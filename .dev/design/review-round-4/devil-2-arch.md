# Devil's Advocate — 렌즈 2: 아키텍처 / 생명주기 / 상태관리

리뷰 대상: LockActivity.kt, MainActivity.kt, ScreenService.kt, TodoStore.kt, ui/Components.kt, ui/Theme.kt, AndroidManifest.xml
기준 문서: decisions.md (D12/D18/D20/D23), review-round-3/p3-verify.md

---

## VERDICT: FAIL

핵심: P3 에서 이미 발견된 **D-SYNC 가 라운드4(P4) 코드에 여전히 미수정**. 추가로 D20(dismiss)·D17(orientation)·D23(fallback) 가 "결정은 했으나 코드 미반영" 상태. 칭찬 없이 결함만 나열한다.

---

## 결함 목록

### [A1] (HIGH) D-SYNC 미수정 — 메인↔잠금 state 가 lifecycle 재로드 없음
- 근거: `MainActivity.kt:85` `var todos by remember { mutableStateOf(TodoStore.load(ctx)) }`, `LockActivity.kt:67` 동일 패턴.
- 두 화면 모두 `remember` 로 **컴포지션당 1회만** 스토어에서 로드한다. Activity 가 `onResume` 로 돌아올 때 `setContent` 컴포지션은 재생성되지 않으므로, 다른 화면에서 토글/추가/삭제한 결과가 반영되지 않는다. (스토어 자체는 정확 — p3-verify.md:14 와 동일 진단.)
- 시나리오: LockActivity 에서 체크 → 잠금해제 후 MainActivity 복귀 → 체크 반영 안 됨(살아있던 컴포지션이 stale state 유지). 반대 방향도 동일.
- **이건 P4 에서 닫기로 한 결함인데 코드에 손이 안 갔다.** 전체 소스에 `LifecycleEventObserver`/`repeatOnLifecycle`/`ON_RESUME` 가 한 곳도 없음(grep 0건).
- 올바른 수정(코드 근거 기반):
  ```kotlin
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
      val obs = LifecycleEventObserver { _, e ->
          if (e == Lifecycle.Event.ON_RESUME) todos = TodoStore.load(ctx)
      }
      lifecycleOwner.lifecycle.addObserver(obs)
      onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
  }
  ```
  MainActivity·LockActivity 양쪽 `todos` 선언 직후에 추가. (스토어가 SoT 이므로 이걸로 충분, rememberSaveable 불필요.)

### [A2] (MED) D20 dismiss 동선 미완 — swipe/requestDismissKeyguard 부재, "스와이프=해제" 멘탈모델 미구현
- 근거: `LockActivity.kt:57` `onDismiss = { finish() }`, `LockActivity.kt:73` 외곽 Column `.clickable { onDismiss() }`.
- D20(decisions.md:71)은 "카드 밖 swipe/back 을 우리가 감지해 `finish()` **또는** `KeyguardManager.requestDismissKeyguard()`(API26+) 호출, 스와이프=해제 멘탈모델 유지"를 요구. 실제 구현은:
  - **swipe 제스처 감지 전혀 없음** (pointerInput/draggable 0건). 오직 **탭**만 `clickable` 로 처리.
  - `requestDismissKeyguard()` 호출 없음. 탭하면 `finish()` 만 → showWhenLocked Activity 가 닫히며 **OS 키가드가 그대로 다시 드러남(여전히 잠김)**. 즉 "스와이프 한 번에 해제" 모델이 아니라 "탭하면 잠금화면으로 되돌아감"이다.
- "finish 가 잠금해제 동선을 막나?" → **막지는 않는다**(탭→키가드 노출→정상 스와이프 해제 가능, SCREEN_ON 재발화 없어 재진입 루프도 없음). 그러나 **D20 이 약속한 UX 와 다르다**. 최소한 swipe 감지 + `requestDismissKeyguard()` 가 빠졌다.
- 부수 결함: 외곽 Column 의 `clickable` 가 카드 내부 **빈 영역 탭까지 흡수**한다. `TodoCard` Box(`Components.kt:56`)는 pointerInput 이 없어 터치를 소비하지 않으므로, 카드 안 여백을 탭하면 이벤트가 부모 Column 으로 전파되어 `onDismiss()` 가 호출된다(의도치 않은 닫힘).

### [A3] (MED) 화면 회전 미고정 — D17(가로=세로고정) 미반영 + 입력값 유실
- 근거: `AndroidManifest.xml:15-22`(MainActivity), `:24-30`(LockActivity) 둘 다 `android:screenOrientation` 없음. `:configChanges` 도 없음.
- D17(decisions.md:48) "가로=세로고정" 결정이 manifest 에 안 들어갔다. 회전 시 Activity 가 정상 재생성된다.
- 재생성 자체는 todos 는 스토어 reload 로 살아남지만, `MainActivity.kt:86` `var input by remember { mutableStateOf(TextFieldValue(""))}` 는 `remember`(saveable 아님)라 **회전하면 입력 중이던 텍스트가 날아간다**.
- 수정: 두 Activity 에 `android:screenOrientation="portrait"` 추가(D17 준수). 회전을 허용할 거면 `input` 을 `rememberSaveable` 로.

### [A4] (MED) D23 fullScreenIntent fallback 미구현 — 오버레이 권한 없으면 무동작
- 근거: `ScreenService.kt:23` 백그라운드 BroadcastReceiver 에서 `context.startActivity(... FLAG_ACTIVITY_NEW_TASK)`.
- Android 12+ BAL 제한상 이 startActivity 는 SYSTEM_ALERT_WINDOW(오버레이) 권한이 **실제 부여돼 있어야** 면제된다. 권한 미부여 시 startActivity 가 조용히 무시되고, **아무 일도 안 일어난다**. D23(decisions.md:74)은 "차단 시 fullScreenIntent 노티 fallback"을 요구했으나 코드에 fullScreenIntent 가 없다(`buildNotification()` 은 평범한 LOW 채널 노티, `Components`/Service 어디에도 `setFullScreenIntent` 0건).
- 결과: 권한 토글을 안 한 사용자/기기에서 잠금화면 표시 기능이 침묵 실패. 진단 로그도 없음.

### [A5] (LOW) ScreenService teardown 시 unregisterReceiver 2차 크래시 가능
- 근거: `ScreenService.kt:32` `startForeground(...)` → `:34` `registerReceiver(...)` 순서. `:41` onDestroy 가 무조건 `unregisterReceiver`.
- `startForeground` 가 throw 하면(예: Android 12+ ForegroundServiceStartNotAllowed) registerReceiver 전에 서비스가 정리되고, onDestroy 의 `unregisterReceiver` 가 **등록 안 된 receiver** 에 대해 `IllegalArgumentException` 으로 2차 크래시.
- 수정: registerReceiver 를 startForeground 보다 먼저 두거나, 등록 여부 플래그 가드/try-catch.

---

## 검증 통과(결함 아님) — 명시 요청 항목

- **ClockHeader DisposableEffect 누수**: `Components.kt:82-93` registerReceiver(ctx)↔onDispose unregister 짝 정확, 익명 receiver 정적 참조 없음 → **누수 없음. PASS**.
- **RECEIVER_NOT_EXPORTED 정확성**: `Components.kt:91` TIME_TICK/TIME_CHANGED/TIMEZONE_CHANGED 는 전부 **system protected broadcast** 이고, protected 시스템 브로드캐스트는 NOT_EXPORTED receiver 에도 정상 전달된다 → **플래그 선택 올바름. PASS**. (단 minor: DisposableEffect 는 leave-composition 시에만 dispose 하므로 Activity 가 stop 상태로 떠 있는 동안에도 분당 state 갱신이 돈다. LockActivity 는 단명이라 무시 가능.)
- **ScreenService keyguard 가드(D12)**: `ScreenService.kt:21-22` `isKeyguardLocked` 체크로 비잠금 시 미표시 → **D12 정합. PASS**.
- **START_STICKY 정합**: `ScreenService.kt:37` 재시작 시 intent=null 이어도 intent 미사용이라 NPE 없음, onCreate 재실행으로 receiver/foreground 재설정됨 → **PASS**.
- **회전/프로세스 죽음 시 todos 복원**: SharedPreferences 가 SoT, 재생성 시 reload → **todos 는 복원 OK**(유실되는 건 A3 의 input 텍스트뿐).
- **테마 동기화**: LockActivity 는 SCREEN_ON 마다 새로 생성되며 `LockActivity.kt:55` 에서 스토어의 테마를 매번 로드 → 잠금화면은 메인 테마 변경을 다음 표시에 반영. **PASS**.

---

## 요약
- 치명: **A1 D-SYNC 미수정**(P4 에서 닫기로 한 핵심 결함이 코드 반영 0).
- 결정-구현 괴리: **A2(D20 dismiss)**, **A3(D17 orientation)**, **A4(D23 fallback)** — 모두 decisions.md 에 적혔으나 코드 없음.
- 견고성: **A5** unregister 2차 크래시.
- 통과: ClockHeader 누수 없음 / RECEIVER_NOT_EXPORTED 올바름 / keyguard 가드 정합 / START_STICKY 정합 / todos 영속 복원.
