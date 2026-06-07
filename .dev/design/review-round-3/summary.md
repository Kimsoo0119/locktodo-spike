# Round 3 — 통합 수렴 리뷰 (4페르소나 종합)

> 리뷰어: R3 통합 리뷰어 (designer/ux/developer/devil 종합)
> 대상: decisions.md(D1~D19) · design-tokens.md(R2 확정) · SPEC-design.md · R2 devil/developer/designer/ux · 현재 *.kt
> 목표: 잔여 결함 닫기 + 구현 준비 완료 판정
> 작성: 2026-06-07
> 한 줄 결론: 구현 준비 완료. 단 D12 잠금해제 동선 문구 1건은 기술적으로 부정확하므로 R3에서 메커니즘으로 교정(4). 나머지는 토큰 미세조정·테스트 게이트·정책 확정으로 모두 닫힘. 구현 착수를 막는 결함 없음.

---

## 1. 테마 정체성 — blur 폐기 후 A/C가 "색만 다른가" → 색만은 아님, 단 체크박스 모양 축 1개 보강 필요

판정: 충분히 구분됨. "색만 다르다"는 과장. blur를 떼도 A/C는 4개 축에서 갈린다.

| 차별 축 | A 다크 글래스 | C 따뜻 카드 | 구분력 |
|---------|--------------|------------|--------|
| 배경 | 160° 다크 퍼플 그라데이션 (#2b1f54→#0d0a1f) | 165° 웜 코랄 크림 (#ffe9d6→#ffc9be) | 강 (명/암 자체가 반대) |
| accent | 퍼플 #7C6CFF | 코랄 #FF8A5C | 강 |
| weight | clock 300 / item 400 (얇음·우아) | clock 700 / item 600 (굵음·친근) | 강 |
| 카드 처리 | 반투명 흰10% + border, shadow 없음 | 흰70% + border + 웜 shadow | 중 |
| 체크박스 radius | 8dp | 9dp | 무 (1dp 차 = 육안 식별 불가) ← 유일한 결함 |

- B는 원형 체크박스 + divider + 단색 배경 + shadow로 셋 중 가장 독립적 → 문제 없음.
- A vs C의 약점은 체크박스 "모양" 축이 사실상 죽어 있다는 것. design-tokens.md `checkbox A8/C9`는 둘 다 "라운드 사각"으로 읽혀 차이가 없다. SPEC §4가 "테마별 모양"을 정체성 요소로 약속했는데 이 축만 미이행.

R3 보강 (테마별 1개씩):
- A: 체크박스 radius 8→6dp (날카로운 기하학적 = 다크/정밀 인상 강화). stroke 2dp 유지.
- B: 변경 없음 (원형 + divider로 이미 독립).
- C: 체크박스 radius 9→12dp (둥글고 부드러움 = 웜/친근 강화). stroke 2.5dp 유지.
- 결과: 체크박스 radius가 6 / 원형 / 12 로 벌어져 "모양" 축이 실제 구분에 기여. weight(300 vs 700)·shadow(없음 vs 있음)·accent와 합쳐 A/C 정체성 충분.

→ 결론: 정체성 충분. 토큰 1줄(checkbox radius A6/C12) 수정으로 완전 해소. 구현 비블로커.

## 2. BAL (백그라운드 Activity 시작 제한) — SYSTEM_ALERT_WINDOW로 우회됨, 단 OEM 잔여 리스크는 테스트 게이트로 관리

판정: 블로커 아님. 합법 우회 경로 존재 + 테스트 게이트·fallback으로 관리.

기술 사실:
- Android BAL 면제 목록에 "앱이 SYSTEM_ALERT_WINDOW(다른 앱 위에 표시) 권한 보유"가 명시 면제 사유로 있다. 매니페스트가 이미 이 권한을 선언(AndroidManifest.xml:4)하므로, 사용자가 "다른 앱 위에 표시"를 허용하면 ScreenService의 백그라운드 startActivity는 합법적으로 통과한다. FGS(specialUse)도 보조 면제로 작동.
- 진짜 잔여 리스크는 OS BAL이 아니라 OEM 배터리 최적화(Xiaomi/MIUI, Samsung One UI, Oppo/Vivo의 autostart 차단·FGS kill). 이건 코드로 100% 못 막고 화이트리스트 안내로 완화 (ux #14 silent-failure 처리와 동일 경로).

구현+테스트 검증 방법(명확화):
1. D12 keyguard 가드를 먼저 추가 — ScreenService.onReceive에서 KeyguardManager.isKeyguardLocked가 true일 때만 LockActivity 기동. BAL 시도 표면 자체를 잠금 상황으로 좁힘(§2-a "잠금 아닐 때 침입"도 동시 해소).
2. 권한 의존성 실증: SYSTEM_ALERT_WINDOW를 끄고 화면켜기 → 실패하는지, 켜고 → 뜨는지 토글 비교. 면제가 load-bearing임을 증명.
3. 테스트 매트릭스: 에뮬(AOSP API 34/35/36) + 실기 최소 2대(Samsung One UI 1대 + 공격적 OEM 1대). 시나리오 = 잠금→화면OFF→화면ON→LockActivity 등장.
4. 로깅: startActivity 시도/실패 catch 로그. 포그라운드 전이 실패 시 기록.
5. Fallback (OEM 차단 시): Notification.setFullScreenIntent + IMPORTANCE_HIGH 채널로 전체화면 노티 경로 — OS가 백그라운드 전체화면을 허용하는 정식 수단(알람/통화 앱 방식). degradation 경로로 구현(developer N3와 일치).

→ 결론: 합법 우회 경로(SYSTEM_ALERT_WINDOW) 있음 → 동작 보장. OEM 잔여분은 ①keyguard 가드 ②실기 테스트 게이트 ③fullScreenIntent fallback ④autostart 안내로 관리. 비블로커.

## 3. 카드 오버플로 — 정책 확정

판정: 확정. design-tokens.md :79의 "maxHeight + 내부 스크롤 + 하단 fade"를 기반으로 완성:

- 표시 개수 상한 없음. 하드 max 없이 카드 maxHeight = 화면 − 시계 − safeDrawing 인셋 안에서 LazyColumn 내부 스크롤. 잠금화면 카드가 화면을 넘으면 스크롤로 흡수.
- 스크롤 어포던스: 내용이 넘칠 때만 하단 8dp fade-out 그라데이션으로 "더 있음" 암시(designer §G-17).
- 정렬: 미완(생성순) 상단 고정 → 완료(완료시각 desc) 하단 sink(D9). 미완이 항상 위라 글랜서블 핵심 정보가 잘리지 않음.
- 완료 접기 정책 확정 (designer §C-6 R3 보류분): 완료 항목이 3개 초과면 단일 요약 row `완료 {n}개 ▾`로 접는다.
  - 잠금화면: 기본 접힘(미완을 above-the-fold에 우선 노출). 요약 row 탭으로 펼침 — 탭은 체크 토글과 동일하게 잠금해제 없이 동작(잠금 비차단).
  - 메인: 기본 펼침(관리·되돌림이 목적이라 완료도 보여야 함, ux #4·A-3과 일치).
- 메인은 일반 세로 스크롤(상한 없음).

→ 결론: 스크롤(주) + 하단 fade(어포던스) + 완료>3 접기(미완 우선) 로 확정. 비블로커.

## 4. 잠금해제 동선 (D12) — Compose 구현 가능, 단 D12 문구가 기술적으로 부정확 → R3 교정 필수

판정: 의도는 구현 가능. 단 D12의 "카드 밖 제스처는 OS로 패스"는 불투명 Activity에서 물리적으로 불가능하므로 메커니즘을 교정해야 함. 이게 R3에서 닫아야 할 유일한 실질 잔여 결함.

기술 사실:
- 우리 LockActivity는 keyguard 위에 뜬 별개의 불투명 Activity/Task다. 불투명 Activity는 터치를 keyguard 창으로 진짜 통과시킬 수 없다. 통과시키려면 윈도우에 FLAG_NOT_TOUCHABLE을 줘야 하는데 그러면 체크박스 탭도 안 먹는다. 즉 D12의 "카드 밖 제스처 OS 패스(터치 통과)"는 구현 불가능한 문구다. 그대로 두면 구현자가 시도→실패→"갇힘" 화면 출시.
- 하지만 사용자 의도("위로 스와이프=잠금해제")는 다른 메커니즘으로 동일하게 달성 가능하다. 우리가 제스처를 받아 직접 처리한다:

R3 교정 — D12 잠금해제 메커니즘 (Compose 1급 구현):
1. back: BackHandler { finish() } (또는 기본 back) → 우리 Activity finish → 뒤의 OS keyguard 노출 → 거기서 평소대로 PIN/지문/스와이프.
2. 카드 밖 스와이프(상/하): 루트 비카드 영역에 Modifier.pointerInput { detectVerticalDragGestures }, 임계 초과 시 둘 중 하나 호출:
   - finish() (keyguard 드러내기), 또는
   - KeyguardManager.requestDismissKeyguard(activity, callback) (API26+, minSdk27 OK) — showWhenLocked Activity에서 OS 잠금해제 UI(지문/PIN)를 직접 띄우는 정식 API. 성공 시 keyguard 해제. ← 권장(스와이프 한 번에 바로 unlock UI, "갇힘" 체감 0).
3. 지문/생체: 보안 기기에서 OS unlock 후에도 showWhenLocked Activity가 남을 수 있으므로 requestDismissKeyguard 콜백 또는 unlock 감지 시 finish 처리.
4. 카드 안 탭: 체크 토글만 소비(우리가 처리). 카드 밖만 dismiss 제스처.

- 즉 "OS로 터치 패스"가 아니라 "우리가 dismiss 제스처를 감지해 finish()/requestDismissKeyguard() 호출"로 멘탈모델("스와이프=해제")을 보존. ux #10(잠금해제 제스처 삼킴 방지)의 실제 충족 경로.
- 전부 Compose에서 표준 API로 구현 가능. 별도 프레임워크 불요.

→ 결론: 구현 가능. 단 decisions.md D12 문구 "카드 밖 제스처 OS 패스(터치 통과)"를 "카드 밖 swipe/back을 감지해 finish()/requestDismissKeyguard() 호출"로 교정해야 함. 교정하면 비블로커. (교정 없이 직역하면 블로커가 됨 → R3 필수 반영)

## 5. 4페르소나 최종 판정 — 구현 가능 상태인가

| 영역 | 페르소나 | R2 상태 | R3 판정 |
|------|---------|---------|---------|
| 아키텍처(덮기/keyguard 가드) | devil/ux | D12 결정됨 | 결정 완료. 코드에 keyguard 가드 추가는 구현 작업(ScreenService:19, 현재 미반영). 설계 비블로커 |
| 토큰 결정 반영 | devil/designer | D13~D16에서 design-tokens.md 전면 재작성 | blur 제거·대비 통과색·완료 alpha0.6·상태토큰 모두 토큰에 실재. 닫힘 |
| 대비/어포던스 | designer | 완료 alpha0.6(A5.43/B4.13/C~5.0), on-accent.C #5B3A2E 4.33:1, 미완 border 2dp | 수치 실재. 닫힘 |
| 잠금해제 동선 | ux/devil/dev | D12 문구 부정확 | R3에서 메커니즘 교정(4). 교정 후 닫힘 |
| 테마 정체성 | designer | A/C 색만 차이 우려 | checkbox radius A6/C12 1줄 수정으로 닫힘 |
| 검증 지표 순환 | devil | 정정됨(P4 별도 게이트, §9 정량) | 닫힘 |
| BAL/OEM | dev/devil | 미해결 | 우회경로+테스트게이트+fallback 정의(2). 비블로커 |
| 카드 오버플로 | designer | R3 보류 | 정책 확정(3) |

비블로커로 추적할 잔여(구현 중 처리):
- 코드가 아직 R1 프로그래매틱 상태(LockActivity removeAllViews/AppCompat, ScreenService 가드 없음) — 전면 재작성이 구현 첫 작업. 설계 결함 아님. developer §4(테마 parent 교체)·§5(edge-to-edge inset)·§6(시계 state 격리) 순서대로.
- device-protected storage(재부팅 직후) = scope-out 유지(developer §8b). 요구 확정 시 재오픈.
- 테마 전파: 이미 떠 있는 잠금화면은 onStart에서 prefs 재read로만 반영(developer N7). 동시표시 시나리오 없어 허용.
- B 야간 눈부심 = 자동 다크 연동 안 함, 안내 문구만(ux #9, bg-lock #ECEEF1 이미 토큰화).

---

## 구현 준비 완료 선언

구현 준비 완료. 구현 착수를 막는 잔여 설계 결함 없음.

단 착수 전 R3 반영 3건(모두 문서/토큰 1~2줄 수정, 코드 외):
1. D12 문구 교정 — "카드 밖 제스처 OS 패스(터치 통과)" → "카드 밖 swipe/back 감지 → finish()/KeyguardManager.requestDismissKeyguard() 호출". (이것만 직역 위험 → 반드시 반영)
2. 테마 정체성 토큰 — checkbox radius A 8→6 / C 9→12 (모양 축 실효화).
3. 카드 오버플로/BAL 정책 명문화 — 완료>3 접기(잠금 기본접힘/메인 펼침), BAL 테스트 게이트(SYSTEM_ALERT_WINDOW 토글 실증 + 실기 2대 + fullScreenIntent fallback).

이후 구현 순서: ①빌드 셋업(compose plugin 2.0.21/BOM/테마 parent 교체) → ②keyguard 가드 + 잠금해제 동선 → ③토큰 박제(AppTheme×3) → ④컴포넌트(시계 state 격리/커스텀 체크박스/Undo) → ⑤§9 정량 지표 에뮬+실기 회귀.
