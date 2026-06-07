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
