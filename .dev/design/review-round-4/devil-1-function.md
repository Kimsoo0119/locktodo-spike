# Devil's Advocate 렌즈 1 — 기능 정합성 (Round 4)

대상: `MainActivity.kt` `LockActivity.kt` `ScreenService.kt` `TodoStore.kt` `ui/Components.kt`
기준: `decisions.md` (D1~D23, P2 구현계획), `review-round-3/p3-verify.md`
방법: 코드 정독. 칭찬 없음, 결함만.

---

## VERDICT: FAIL

기본 CRUD(추가/삭제/Undo/토글) 로직 자체는 코드상 정확하나, **Round 3에서 결정·계획된 기능(D22 완료 접기, D23/P2.5 fullScreenIntent fallback)이 코드에 아예 없다.** 또한 ux R2가 지적한 "토글 시 row 점프"가 방지되지 않았다. p3-verify의 PASS 항목은 거짓은 아니지만 검증 범위가 결정 사항을 다 덮지 못했다.

---

## 결함 목록

### F1 [HIGH · 결정 미구현] D22 "완료 접기" 양 화면 모두 없음
- 근거: `MainActivity.kt:159-189`, `LockActivity.kt:84-92`. 두 화면 모두 `todos.sortedBy { it.done }` 후 전부 렌더만 한다.
- D22(`decisions.md:73`)는 "완료>3개면 '완료 {n}개 ▾' 접기(잠금=기본접힘 미완우선, 메인=펼침)" 를 명시했고, P2 구현계획 6번(`decisions.md:87`)에도 "완료 sink/접기"로 들어가 있다.
- 코드 전역 grep 결과 `접기`/`완료 N개`/`fold`/`collapse` 0건. sink(정렬)만 구현되고 **접기는 미구현**.
- 영향: 완료 항목이 누적되면 잠금화면 카드가 미완 항목을 아래로 밀어내림(D22가 막으려던 정확히 그 증상). 잠금화면 "기본접힘 미완우선" 의도 위반.

### F2 [MED · ux R2 미방지] 토글 직후 즉시 재정렬 — row 점프, placement 애니 없음
- 근거: `MainActivity.kt:159-161, 167`, `LockActivity.kt:84-89`. 토글 시 `TodoStore.toggle` → `todos = load()` → `sortedBy { it.done }` 가 즉시 다시 평가되어 해당 row 가 곧바로 목록 끝(또는 위)으로 점프.
- `items(..., key = { it.id })` 로 아이템 식별자는 있으나 `Modifier.animateItem()`(placement 애니) 미적용 → 점프가 애니 없이 순간 이동. grep `animateItem` 0건.
- ux R2가 지적한 "정렬로 row 가 손가락 밑에서 움직이는 문제" 에 대한 코드상 방지책 없음. 인접 row 가 탭 직후 위로 끌려 올라와 연속 오탭 위험.

### F3 [MED · 잠재 크래시] 할 일 id = `System.currentTimeMillis()` 충돌 시 LazyColumn key 중복
- 근거: `TodoStore.kt:34` (`Todo(System.currentTimeMillis(), ...)`), 소비처 `MainActivity.kt:161` / `LockActivity.kt:86` 의 `key = { it.id }`.
- 같은 밀리초에 2건 추가되면 동일 id → Compose `items` 의 중복 key 로 런타임 예외. 수동 탭으로는 재현 난이도 높지만, id 생성이 시간 의존이라 구조적으로 유일성이 보장되지 않음. 카운터/UUID 권장.
- (참고: Undo 의 `insert(removed, idx)` 는 기존 id 를 재사용하므로 Undo 자체로는 충돌 없음 — 정상.)

### F4 [MED · 결정 미구현] D23/P2.5 fullScreenIntent BAL fallback 없음
- 근거: `ScreenService.kt:23-25` 는 SCREEN_ON 수신 시 곧장 `startActivity(... NEW_TASK)`. 알림(`buildNotification` L46-57)은 fullScreenIntent/ contentIntent 없는 단순 LOW 채널.
- D23(`decisions.md:74`)·P2 5번(`decisions.md:86`)이 "차단 시 fullScreenIntent 노티 fallback 준비"를 명시했으나 미구현. targetSdk36/OEM 에서 백그라운드 Activity 시작이 막히면 잠금화면이 안 뜨고 대체 경로가 없다.
- p3-verify "잠금화면 자동표시 PASS"(Android 16 에뮬)는 BAL 면제가 통한 단일 경로만 증명. fallback 부재를 가리지 못함.

### F5 [LOW · 결정 부분구현] D20 dismiss — tap-outside + back 만, swipe 없음
- 근거: `LockActivity.kt:73`(카드 밖 탭→`finish()`), back 은 ComponentActivity 기본 finish.
- D20(`decisions.md:71`)은 "카드 밖 swipe/back" 및 `requestDismissKeyguard()`(API26+) 를 거론했으나, swipe 제스처 감지·requestDismissKeyguard 호출 모두 없음. 멘탈모델(스와이프=해제)은 미충족.

### F6 [LOW] LockActivity 카드 높이 하드코딩
- 근거: `LockActivity.kt:85` `LazyColumn(Modifier.heightIn(max = 480.dp))`. D22 가 정한 "maxHeight = 화면−시계−인셋" 계산식이 아니라 고정 480dp. 작은 화면/큰 폰트에서 시계와 겹치거나 잘릴 수 있음.

### F7 [LOW · D7 불일치] ClockHeader 리시버 등록 수명
- 근거: `ui/Components.kt:82` `DisposableEffect(Unit)`. D7(`decisions.md:17`)/D18 은 "onStart/onStop 동적 등록"을 명시했으나 실제로는 컴포지션 진입/이탈 기준이라 Activity STOPPED 동안에도 TIME_TICK 리시버 유지(배터리). 시계 동작 자체는 정상(거짓 PASS 아님).

### F8 [LOW] 잠금화면 시계 탭도 dismiss
- 근거: `LockActivity.kt:69-78`. dismiss `clickable` 이 `fillMaxSize` Column 전체(시계 포함)에 걸려 있고, 시계는 클릭 미소비 → 시계를 탭해도 화면이 닫힘. "카드 밖 빈 영역 탭=dismiss" 주석 의도보다 범위가 넓음.

---

## 정확하게 동작하는 부분(거짓 PASS 검증)

확증 편향 방지용 — 아래는 코드로 확인해 **결함 아님**:
- **추가**: `MainActivity.kt:150-153` trim 후 `isNotEmpty` 가드로 빈/공백 입력 차단. 정상.
- **삭제+Undo index 복원**: `MainActivity.kt:173-180`. `idx = todos.indexOfFirst{id}` 는 raw 저장순(=`load()` 순서) 기준이고 `insert(removed, idx)` 가 같은 raw 위치에 복원 → 저장순 보존. 표시순(sorted)과는 별개지만 렌더 시 재정렬되므로 시각적으로도 올바름. `coerceIn(0,size)` 로 OOB 방어. p3-verify "원래 index 복원 PASS" **참**.
- **토글 멱등성**: `TodoStore.kt:40-41` 는 flip(=toggle 의미상 정상), 탭 1회=1토글. id 매칭만 변경, 나머지 order/필드 보존. 정상.
- **keyguard 가드**: `ScreenService.kt:21-22` `isKeyguardLocked` 가드 존재 → 잠금 아닐 때 미표시. p3-verify "잠금 아니면 미표시 PASS" **참**.
- **테마 영속/전환**: `MainActivity.kt:69-74` 즉시 state 갱신+prefs 저장, `LockActivity.kt:55` 1회 로드(읽기전용 화면이라 적합). 정상.

## p3-verify 종합 판정
- 표에 적힌 7개 PASS 행은 각각 **검증한 범위 내에서는 거짓 PASS 아님**.
- 다만 표가 **D22 접기 / D23 fallback / 토글 row 점프**를 검증 항목으로 두지 않았다 → 거짓 PASS는 아니되 **검증 커버리지가 결정사항(D20·D22·D23)을 다 덮지 못함**. "구현 준비 완료" 선언 대비 실제 구현 누락(F1·F4)을 잡지 못한 것이 한계.
- 알려진 D-SYNC 외 추가 기능 결함: **F1·F4(결정 미구현)**, **F2(ux 미방지)**, **F3(잠재 크래시)** 존재.
