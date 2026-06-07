# 디자인 결정 로그 (append-only)

> 라운드마다 4페르소나(designer/ux/developer/devil) 리뷰 → 합의/결정을 누적. 덮어쓰지 않는다.

## Round 1 결정 (2026-06-07)

여러 페르소나가 동시에 지적한 항목은 신뢰도 높음으로 우선 채택.

| # | 결정 | 근거(제기 페르소나) |
|---|------|---------------------|
| D1 | **구현 = Jetpack Compose** 확정 (Kotlin 2.0.21 + compose compiler plugin) | dev #1, designer·ux 동의 (커스텀 토큰/모양/애니/취소선 1급 지원, XML 자산 0) |
| D2 | **글래스 blur 폐기 → 반투명 solid fill + 1px border** | designer #2, dev #2, devil #1 (배경이 자체 그라데이션이라 blur 무의미 + RenderEffect API31+ 제약, 27~30 붕괴) |
| D3 | **대비 WCAG AA 사수**: 본문 ≥4.5:1, 큰글씨/컴포넌트 ≥3:1. 완료 표현은 색 죽이기 대신 **취소선 + 불투명도**, muted 색은 대비 충족값으로 토큰 수정 | designer #5, ux #3, devil #3 (현재 완료 1.7~2.3:1) |
| D4 | **잠금화면엔 삭제 없음**(체크·보기만). 삭제는 메인 **✕ 아이콘 + Undo 스낵바** | ux #2, dev #5 (잠금화면 오터치 + 파괴동작 복구 불가) |
| D5 | **체크 토글 즉시 피드백**: 채움 애니메이션 + 햅틱 + 해당 row만 갱신. `removeAllViews` 전체 재렌더 폐기 | ux #1 (잠금화면은 확인 안 되면 재탭/잠금해제) |
| D6 | **터치 영역 = row 전체 ≥56dp** (체크박스 아이콘만 X) | ux #4 |
| D7 | **시계**: 시스템 12/24h 설정 따름, OS 시각과 일치, 부분 갱신(깜빡임 방지), tabular figures, `ACTION_TIME_TICK` onStart/onStop 동적 등록(RECEIVER_NOT_EXPORTED) | dev #3, devil #4 |
| D8 | **상태 토큰 추가**: pressed/ripple, disabled, focus, destructive, on-accent, empty | designer #1 |
| D9 | **완료 항목 하단 sink**, 카드 라벨("오늘 할 일") 전 테마 통일 | designer #4 |
| D10 | **테마 선택 = 메인 상단 세그먼트(즉시 프리뷰)**, prefs 저장 | designer, ux, dev §8 |
| D11 | **정량 완료 기준 추가**(아래 §검증 지표) | devil #5 |

## 미해결 / 추가 검증 (다음 라운드에서 답할 것)
- **자체 시계 vs OS 시계 중복**(devil #2): 우리 LockActivity가 OS 잠금화면을 덮으므로 우리 시계 필요. 단 "덮는 방식이 옳은가 / OS 잠금화면 위 패널이 아니라 전체 대체가 맞나"를 R2에서 정리.
- **3테마 YAGNI**(devil): 사용자가 3테마 명시로 유지. 단 유지보수 부담은 AppTheme data class + CompositionLocal 토큰화로 완화(구조 분기 최소화) — R2에서 분기 비용 구체화.
- 재부팅 직후 첫 잠금해제 전 표시 = device-protected storage 필요 여부(범위 확인).
- 가로 모드 / 폰트 크기 확대(접근성) 대응 범위.
- 라이트 테마(B) 야간 눈부심 — 시스템 다크모드 연동 여부.

## 검증 지표 (D11, 측정 가능)
- 색 대비: 본문 ≥4.5:1, 큰글씨(≥24sp)/컴포넌트 외곽선 ≥3:1 — 토큰 쌍마다 계산 통과
- 터치 영역: 모든 인터랙션 row ≥56dp
- 시계: 1분 갱신 시 전체 깜빡임 없음(부분 갱신)
- 테마 전환: 메인에서 A/B/C 선택 시 메인·잠금화면 즉시 반영
- 기능 회귀: 추가/삭제(+Undo)/체크 동작 유지(에뮬 실측)

## Round 2 결정 (2026-06-07)

R1 결함 해소 판정 + 구체값 확정. devil R2가 "결정≠반영"(토큰 미수정)을 치명으로 지적 → 이번에 design-tokens.md 실제 수정.

| # | 결정 | 근거 |
|---|------|------|
| D12 | **아키텍처 = 잠금 상태에서만 전체 대체(full-bleed) 표시 + 잠금해제 동선 보존**. ScreenService 에 `KeyguardManager.isKeyguardLocked` 가드 추가(잠금 아니면 LockActivity 안 띄움). LockActivity 는 OS 잠금화면을 덮되 back/스와이프로 dismiss 가능, 카드 밖 제스처는 OS 로 패스(갇힘 방지) | devil R2 #1(코드 근거: ScreenService 가드 없음), ux #3·designer §F-15 |
| D13 | **결정을 실제 산출물에 반영**: design-tokens.md 를 R2 확정값으로 전면 재작성(blur 제거, 대비 통과색, 상태토큰, 완료=베이스×0.6). mockups.html 의 blur 도 구현 시 제거 | devil R2 #2 ("토큰이 결정 안 따름") |
| D14 | **글래스 fill 확정**: A fill #FFF@.10/border #FFF@.16, C fill #FFF@.70/border #FFF@.55+shadow, 불투명 등가(A #332B4F, C #FFF6EF) 병기 | designer R2 §A |
| D15 | **완료 = text×alpha 0.60 + 취소선 1.5dp** 전테마 통일, 완료전용 muted 삭제. **on-accent.C = #5B3A2E**(코랄 위 갈색 ✓ 4.33:1) | designer R2 §B·C |
| D16 | **토큰 시스템화**: 4dp 그리드, 타입/weight 스케일, 체크 벡터 path, radius 세트, muted 2분리(strong/weak), 세이프에어리어 인셋 기반 시계 top | designer R2 §D·E·G |
| D17 | **라벨 "오늘 할 일" → "할 일"**(날짜 없는 모델이라 자정 후 거짓말 방지), 잠금해제 제스처 패스, 가로=세로고정, 폰트 1.3배 클램프 | ux R2 #5·#4 |
| D18 | **Compose 셋업 확정**: compiler plugin 2.0.21 + compose-bom 2024.12.01 + activity-compose 1.9.3. **AppTheme parent 를 Material(android:Theme.Material.Light.NoActionBar)로 교체**(appcompat 제거 시 빌드깨짐 주의). **edge-to-edge 강제**(targetSdk36) → safeDrawing inset. 시계 state 는 ClockHeader 로컬 remember + DisposableEffect(TIME_TICK, RECEIVER_NOT_EXPORTED) | dev R2 #1·#2·#3 |
| D19 | 토글=animateColorAsState+scaleIn, row 전체 clickable(체크박스 단독 금지), Undo=Scaffold+SnackbarHost 원래 index 복원(메인 전용) | dev R2 #5 |

## R1 결함 해소 판정 (devil R2)
- ✅ 닫힘: 시계 깜빡임(부분 갱신 D7/D18), 구현 스택(Compose)
- 🔧 R2에서 닫음: 아키텍처(D12), 토큰 미반영(D13~D16), 라벨 거짓말(D17), 대비(토큰 수정)
- ⏳ R3 확인: 테마 정체성(blur 폐기 시 A/C 색만 차이 — 배경 그라데이션+체크모양+accent로 차별화 유지 검증), 완료 기준 순환참조 제거(아래)

## 완료 기준 정정 (순환참조 제거, devil R2 #5)
- "devil 4-pass 통과"는 완료 기준이 아니라 **별도 최종 게이트**(P4). 작업 완료 기준 = §9 정량 지표(대비 토큰 계산 통과 / row≥56dp / 시계 무깜빡임 / 테마전환 즉시 / 추가·삭제·체크 에뮬 회귀) 충족.

## R2 미해결 (R3에서)
- 재부팅 직후 표시(device-protected storage) = spike scope-out 권장, 요구 확정 시 재오픈
- BAL(백그라운드 Activity 시작 제한) targetSdk36/OEM 차단 가능성 → 에뮬+실기 회귀 필수
- 카드 오버플로 max 표시 개수 정책, 완료 5개 초과 접기

## Round 3 결정 (2026-06-07) — 수렴/구현 준비 완료

통합 4페르소나 리뷰 → "구현 준비 완료" 판정. 잔여 3건 반영.

| # | 결정 | 근거 |
|---|------|------|
| D20 | **D12 문구 교정**: "카드 밖 제스처를 OS로 패스(터치 통과)"는 불투명 Activity에서 물리적 불가(통과 주면 체크도 안 먹음). → 카드 밖 swipe/back을 **우리가 감지해 `finish()` 또는 `KeyguardManager.requestDismissKeyguard()`(API26+)** 호출. 멘탈모델(스와이프=해제) 유지, Compose 표준으로 구현 | R3 #4 (직역 시 유일 블로커) |
| D21 | **체크박스 radius A 8→6 / C 9→12** (1dp차는 식별 불가 → 모양 축 실효화) | R3 #1 |
| D22 | **카드 오버플로**: 표시 상한 없음, maxHeight=화면−시계−인셋 내부 LazyColumn 스크롤+하단 fade. **완료>3개면 "완료 {n}개 ▾" 접기**(잠금=기본접힘 미완우선, 메인=펼침) | R3 #3 |
| D23 | **BAL 테스트 게이트**: SYSTEM_ALERT_WINDOW가 BAL 합법 면제라 통과. keyguard 가드 추가 후 권한 토글로 면제 실증 + 에뮬 회귀, 차단 시 fullScreenIntent 노티 fallback | R3 #2 |

## 디자인 리뷰 종료 선언
- R1(4페르소나) → R2(4페르소나) → R3(통합) 로 수렴 완료. 구현을 막는 설계 결함 없음.
- 사용자 완료기준의 "4번째 루프 = devil review 전부 패스"는 **구현 산출물 대상 최종 게이트(P4)**로 배치(R2 순환참조 제거와 정합).
- 구현 전 코드 패치 대상(문서와 어긋난 곳): ScreenService(keyguard 가드 없음), LockActivity(AppCompat/removeAllViews/해제동선 없음) → P2에서 Compose 전면 재작성.

## P2 구현 계획
1. Compose 셋업(D18): compiler plugin 2.0.21, compose-bom 2024.12.01, activity-compose 1.9.3, AppTheme parent → Material
2. 테마 시스템: AppTheme data class ×3 + LocalAppTheme CompositionLocal (토큰 1:1, when(theme) 금지)
3. LockActivity → ComponentActivity setContent: 시계(ClockHeader 로컬 remember + TIME_TICK) + 카드 + 체크(토글 애니) + 잠금해제 동선(D20)
4. MainActivity → Compose: 테마 세그먼트 + 입력/추가 + 목록(✕ 삭제 + Undo 스낵바)
5. ScreenService: keyguard 가드(D12) + fullScreenIntent fallback 준비
6. edge-to-edge inset, 폰트 클램프, 완료 sink/접기

## Round 4 — devil 4-pass 결과 (전부 FAIL) + 수정 목록

핵심: "결정≠반영" 재발 — 토큰을 design-tokens.md 에 박았으나 구현 코드가 절반만 반영(devil-3). devil 4렌즈 전원 FAIL.

### 수정 대상 (1차 = 블로커/HIGH, 2차 = MED, 3차 = 추후)
**1차(블로커):**
- FIX1 D-SYNC: Main/Lock 에 lifecycle ON_RESUME 재로드(DisposableEffect+LocalLifecycleOwner)
- FIX2 BAL fallback: ScreenService canDrawOverlays 체크 + try/catch + fullScreenIntent 노티
- FIX3 ✓ 커스텀 path: Components Canvas drawPath(M6 12.5 L10.5 17 L18 7.5), Icons.Check 폐기
- FIX4 권한 동선: 시작 전 권한 상태 체크, 미허용 안내(최소 Toast)
- FIX5 id 중복: TodoStore.add id = maxId+1
- FIX6 가로 고정: manifest 두 Activity portrait
- FIX7 startForeground/receiver 순서·try, teardown 안전

**2차(MED):**
- FIX8 토글 row 점프: LazyColumn animateItem
- FIX9 D22 완료>3 접기
- FIX10 누락 토큰: pressed/disabled/focus/card-shadow + space 그리드 상수화
- FIX11 card-label "할 일"(전테마), empty(메인), clock-ampm 분리
- FIX12 세이프에어리어 시계 top(statusBars), 카드 동적 높이+fade
- FIX13 테마전환 220ms crossfade(animateColorAsState)
- FIX14 D20 swipe-dismiss/requestDismissKeyguard, 카드내부 탭 전파 차단

**3차(추후/저위험):**
- 폰트 sp 클램프, 긴텍스트 maxLines/ellipsis, 햅틱, 재부팅 BOOT_COMPLETED

리뷰 원본: review-round-4/devil-{1-function,2-arch,3-design,4-risk}.md
