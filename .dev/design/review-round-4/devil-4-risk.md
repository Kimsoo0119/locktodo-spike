# Devil's Advocate 렌즈 4 — 잔여 리스크/누락 (2026-06-07)

기본값: 결함 있음. 출시 전 막아야 할 잔여 리스크를 코드 근거로 검증.
검증 대상: `app/src/main/java/com/example/locktodo/*.kt` + `ui/*.kt`, `AndroidManifest.xml`,
`themes.xml`, `decisions.md`, `p3-verify.md`.

판정: **FAIL** — 블로커 3건.

---

## 블로커 (출시 전 반드시 막음)

### B1. D23 fullScreenIntent fallback 미구현 + 권한 없을 때 startActivity 무방비
- 근거: `ScreenService.kt:23-25` — `onReceive` 에서 `context.startActivity(... LockActivity)` 를
  try/catch 없이, `Settings.canDrawOverlays` 체크 없이, fallback 없이 호출.
- D23(decisions.md:74)은 "SYSTEM_ALERT_WINDOW가 BAL 합법 면제 → 통과, **차단 시 fullScreenIntent
  노티 fallback**" 을 게이트로 명시했으나 코드에 fallback이 전혀 없음(`grep fullScreenIntent` = 0건).
- 결과: 오버레이 권한 미허용 상태 또는 OEM이 BAL을 추가 차단하는 기기에서 화면 ON 시
  LockActivity 가 **조용히 안 뜸**. 앱의 핵심 가치(화면 켜면 잠금화면에 할 일)가 무음 실패하고
  사용자에게 어떤 피드백도 없음. 이게 이 앱의 단 하나의 핵심 기능이라 무음 실패 = 출시 불가.
- 최소 조치: onReceive에서 `canDrawOverlays` 분기 → 미허용/실패 시 `fullScreenIntent` 노티로 폴백,
  startActivity는 try/catch.

### B2. 권한 거부/영구거부 동선 없음 — 상태 피드백 0, 게이팅 0
- 근거: `MainActivity.kt:208-217 requestPerms`, `219-221 startSvc`.
  - 권한 요청 후 결과 콜백(`onRequestPermissionsResult`) 없음, 허용 여부를 다시 읽어 UI에 반영하는
    경로 없음. 배너(`MainActivity.kt:128`)는 "화면 켜면 잠금화면 표시" 고정 문구로 권한 상태 무관.
  - "시작" 버튼(`MiniButton("시작")`)은 오버레이 권한 미허용이어도 그대로 눌리고 서비스가 뜸.
    사용자는 다 됐다고 믿지만 B1대로 실제론 LockActivity가 안 뜸.
  - 영구거부(다시 묻지 않음) 시 `requestPermissions` 가 즉시 무시되는데 설정으로 보내는 동선 없음.
- 결과: 권한 미설정 상태가 사용자에게 보이지 않고, 핵심 기능 실패와 결합되어 "왜 안 되는지" 진단
  불가능. B1과 한 쌍.

### B3. D-SYNC 미수정 — 메인↔잠금 state 동기화 안 됨 (p3-verify 기지 결함이 코드에 그대로)
- 근거: `MainActivity.kt:85`, `LockActivity.kt:67` 모두 `remember { TodoStore.load(ctx) }` 로
  최초 1회만 로드. `grep ON_RESUME|onResume|repeatOnLifecycle` = 0건.
- p3-verify.md:14 가 P4 수정 대상으로 명시했으나 코드 미반영. 한 화면에서 토글/추가/삭제 후 다른
  화면 재개 시 옛 목록 표시(저장소는 정확). 잠금화면에서 체크 → 메인 열면 안 바뀐 것처럼 보임.
- 결과: 두 화면을 오가는 기본 사용 흐름에서 항상 재현되는 신뢰 깨짐. 출시 블로커.
- 조치: ON_RESUME(LifecycleEventObserver/lifecycle-runtime-compose)에서 `TodoStore.load` 재호출.

---

## 추후 (스파이크 출시는 가능, 후속 처리)

### L1. 폰트 확대(접근성) sp 클램프 미구현 (D17 위반)
- D17(decisions.md:48) "폰트 1.3배 클램프" 명시. 코드엔 `grep fontScale` = 0건, 모든 `fontSize`가
  raw sp(`ClockHeader` 시계 60sp 포함). 시스템 글꼴 2.0배 사용자는 시계 ~120sp → 잠금화면 클리핑/오버플로.
- 분류: 추후(접근성 사용자 한정). 단 명시 결정 미반영이므로 문서-코드 불일치로 기록.

### L2. 가로모드 세로 고정 미구현 (D17 위반)
- D17 "가로=세로고정". 매니페스트 `grep screenOrientation` = 0건, 코드 잠금 없음.
- LockActivity는 `Column(fillMaxSize)` 안에 시계(60sp)+카드가 세로 적층 → 가로에서 세로 높이 부족 시
  시계가 위로 밀리거나 카드가 잘림. 외곽 Column은 스크롤 없음.
- 분류: 추후(스파이크 주 사용은 세로). 단 L4와 겹쳐 가로에서 깨짐.

### L3. D22(카드 오버플로/완료 접기/하단 fade) 미구현 + 카드 높이 하드코딩
- D22(decisions.md:73): maxHeight=화면−시계−인셋 동적, 하단 fade, 완료>3개 접기.
- 코드: `LockActivity.kt:85 LazyColumn(Modifier.heightIn(max = 480.dp))` — **480dp 하드코딩**.
  fade 없음, 완료 접기 없음(`grep 접기|fade|collaps` = 0건).
- 리스크: 소형/가로 화면에서 480dp가 가용 높이를 초과 → 카드가 시계를 밀어내거나 화면 밖. 스크롤은
  되지만 시계 클리핑 가능. 대형 화면은 공간 낭비.
- 분류: 추후. 단 동적 높이로 바꾸지 않으면 L2와 합쳐 가로/소형 기기 시각 붕괴.

### L4. 긴 텍스트 maxLines/ellipsis 없음
- `MainActivity.kt:170`, `LockActivity.kt:110` 의 `Text(todo.text, ...)` 에 maxLines/overflow 없음
  (`grep maxLines|overflow` = 0건). 매우 긴 할 일 1건이 row를 세로로 길게 차지. 카드는 스크롤되므로
  치명은 아니나 의도된 말줄임/제한 없음.
- 분류: 추후.

### L5. 햅틱 미구현 (D5 위반)
- D5(decisions.md:15) "체크 토글 즉시 피드백: 채움 애니 + **햅틱** + row 갱신". 코드에 채움 애니
  (`TodoCheckbox` animateColorAsState/scaleIn)는 있으나 `grep haptic|performHapticFeedback` = 0건.
- 분류: 추후(피드백 품질). 명시 결정 미반영으로 기록.

### L6. 메인 화면 빈 상태 없음
- `LockActivity` 는 `EmptyState()`(todos.isEmpty) 처리하나 `MainActivity` 목록은 빈 상태 분기 없음
  (`MainActivity.kt:159-189`). 할 일 0이면 빈 공간만. 입력창이 있어 치명은 아님.
- 분류: 추후.

### L7. 재부팅 후 동선 (decisions: scope-out, 확인)
- BOOT_COMPLETED 리시버 없음(`grep BOOT_COMPLETED` = 0건) → 재부팅 후 서비스 자동 재시작 안 됨,
  사용자가 앱 열어 "시작" 다시 눌러야 함.
- `TodoStore` 는 기본(credential-encrypted) SharedPreferences → 첫 잠금해제 전 Direct Boot 구간에선
  읽기 불가(빈 목록). decisions.md:26,61 가 scope-out 권장한 항목과 일치.
- 분류: 추후(명시 scope-out). 요구 확정 시 재오픈.

### L8. ScreenService registerReceiver 플래그 불일치 (저위험)
- `ScreenService.kt:34` `registerReceiver(screenReceiver, IntentFilter(ACTION_SCREEN_ON))` 는
  RECEIVER_EXPORTED/NOT_EXPORTED 플래그 없이 호출. `Components.kt:91` ClockHeader는
  `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` 로 올바르게 함.
- ACTION_SCREEN_ON은 protected system broadcast라 targetSdk34+ 플래그 강제에서 면제 → 크래시 안 함.
  단 코드 일관성/방어 차원에서 ContextCompat + 플래그로 통일 권장.
- 분류: 추후(저위험).

---

## 검증 지표(decisions §검증지표) 대비 코드 미충족
- 햅틱(D5): 미충족(L5)
- 폰트 클램프(D17): 미충족(L1)
- 가로 고정(D17): 미충족(L2)
- D22 완료 접기/fade/동적 높이: 미충족(L3)
- 메인↔잠금 즉시 반영(테마 전환 지표는 충족, **할 일 state 전환은 미충족**): D-SYNC(B3)
- 충족 확인: 색 대비(토큰 R2값), row≥56dp(heightIn min 56/48dp 적용), 시계 부분 갱신(ClockHeader
  now state 격리), 추가/삭제+Undo/체크(p3-verify PASS)

---

## VERDICT: FAIL

블로커:
1. B1 — D23 fullScreenIntent fallback 미구현 + 권한 미허용/BAL 차단 시 startActivity 무음 실패
2. B2 — 권한 거부/영구거부 동선·상태 피드백·게이팅 전무 (B1과 한 쌍, 진단 불가)
3. B3 — D-SYNC 메인↔잠금 state 미동기화 (p3-verify 기지 결함이 코드에 그대로)
