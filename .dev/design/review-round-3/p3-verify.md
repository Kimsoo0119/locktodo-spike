# P3 에뮬 검증 결과 (2026-06-07, Android 16)

| 항목 | 결과 | 증거 |
|------|------|------|
| 추가 | PASS | groceries/workout 저장 |
| 삭제 + Undo | PASS | ✕ 삭제→스낵바 실행취소→원래 index 복원 |
| 무잠금 체크(G1) | PASS | 잠금 상태(isKeyguardShowing=true)에서 체크 토글, keyguard 유지 |
| 잠금화면 자동표시 | PASS | keyguard 가드 통과 후 SCREEN_ON→LockActivity (잠금 아니면 미표시 확인) |
| 시계 | PASS | 6:59→7:00 갱신, 시스템 12h, 날짜 |
| 3테마 + 즉시 전환 | PASS | 다크↔따뜻, theme=C 저장 영속 |
| 완료 sink + 취소선 | PASS | 완료 하단, 취소선+alpha |

## 발견 결함 (P4 수정 대상)
- **D-SYNC: 메인↔잠금 state 동기화 안 됨** — MainActivity/LockActivity 가 todos 를 remember 초기 1회만 로드. 한 화면 토글이 다른 화면 재개 시 반영 안 됨(저장소는 정확). lifecycle ON_RESUME 재로드 필요.
