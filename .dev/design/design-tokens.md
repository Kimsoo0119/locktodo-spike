# 디자인 토큰 (3테마) — R2 확정

> Round 1·2 결정 반영. blur 폐기(반투명 단일 fill), 대비 WCAG 통과 색, 완료=베이스×0.6.
> 구현: `AppTheme` data class × 3 인스턴스 + `LocalAppTheme` CompositionLocal. 컴포넌트 본문 `when(theme)` 금지 — `LocalAppTheme.current.xxx` 만 읽는다.

## 색 — 배경 / 카드
| 토큰 | A 다크 | B 라이트 | C 따뜻 |
|------|--------|----------|--------|
| `bg` (메인) | gradient 160° #2b1f54→#1a1438→#0d0a1f | #f4f5f7 | gradient 165° #ffe9d6→#ffd9c2→#ffc9be |
| `bg-lock` (잠금 전용) | (메인과 동일) | **#ECEEF1** (야간 눈부심 −4%) | (메인과 동일) |
| `card-fill` | #FFFFFF @ 0.10 | #FFFFFF (불투명) | #FFFFFF @ 0.70 |
| `card-fill-opaque` (합성 등가, 권장) | #332B4F | #FFFFFF | #FFF6EF |
| `card-border` | #FFFFFF @ 0.16 | none | #FFFFFF @ 0.55 |
| `card-shadow` | none | 0 8 24 rgba(20,20,40,.06) | 0 6 18 rgba(180,110,80,.12) |

## 색 — 텍스트 / accent
| 토큰 | A | B | C |
|------|---|---|---|
| `text` | #FFFFFF | #23262E | #5B3A2E |
| `muted-strong` (날짜/라벨) | #B8B2D8 | #9AA0AD | #B07D63 |
| `muted-weak` (보조/empty) | #8A85A8 | #B6BAC3 | #C9A392 |
| `accent` | #7C6CFF | #4F6BED | #FF8A5C |
| `accent-pressed` | #6A5AE0 | #3F58D8 | #F2754A |
| `on-accent` (체크 ✓) | #FFFFFF | #FFFFFF | **#5B3A2E** (흰 ✓는 2.32:1 탈락 → 갈색 4.33:1) |

> 완료 전용 muted 색 토큰은 **삭제**. 완료 = `text` × alpha 0.60 (계산: A 5.43 / B 4.13 / C ~5.0 :1).

## 색 — 상태
| 토큰 | A | B | C |
|------|---|---|---|
| `pressed` (row) | #FFF @ 0.08 | #000 @ 0.06 | #5B3A2E @ 0.07 |
| `ripple` | #FFF @ 0.12 | #4F6BED @ 0.12 | #FF8A5C @ 0.14 |
| `disabled-text` | #FFF @ 0.38 | #23262E @ 0.38 | #5B3A2E @ 0.38 |
| `disabled-fill` | #FFF @ 0.12 | #000 @ 0.10 | #5B3A2E @ 0.10 |
| `focus` (입력 1.5dp) | #7C6CFF | #4F6BED | #FF8A5C |
| `destructive` 평상/확인 | #FFF@0.45 / #FF6B6B | #9AA0AD / #E5484D | #C9A392 / #E5484D |
| `divider` | none | #F0F1F4 | none |

## 완료 항목 (전 테마 공통 규칙)
- 텍스트 = `text` × **alpha 0.60** + 취소선 1.5dp(취소선 색 = 동일 합성색)
- 정렬: 미완(생성순) → 완료(완료시각 desc) 하단 sink

## 간격 (4dp 베이스)
| 토큰 | 값 | 용처 |
|------|-----|------|
| `space.xs` | 4 | 아이콘-텍스트, 취소선 위 |
| `space.sm` | 8 | row 내부 gap |
| `space.md` | 12 | row 세로 패딩 |
| `space.lg` | 16 | 카드 마진/내부 좌우 |
| `space.xl` | 24 | 카드↔시계 |
| `space.2xl` | 32 | 시계 top(+ 세이프에어리어 인셋) |
- row 터치: 시각 패딩과 별개 `heightIn(min=56.dp)`, 체크박스 CenterVertically
- 시계 top = `WindowInsets.statusBars` + `space.xl` (노치/펀치홀 대응, 고정값 금지)

## 타입스케일 (size / weight A·B·C)
| 역할 | size | weight | 비고 |
|------|------|--------|------|
| `clock` | 60sp | 300/600/700 | tnum 고정폭 |
| `clock-ampm` | 20sp | clock | 12h만, muted |
| `date` | 14sp | 400/500/600 | muted-strong |
| `card-label` | 12sp | 600 | "할 일", uppercase 1sp letterSpacing, 전테마 공통 |
| `item` | 16sp | 400/500/600 | 본문 |
| `title` (메인) | 22sp | 700 | |
| `input` | 16sp | 400 | |
| `button` | 15sp | 600 | |
| `empty` | 16sp | 400 | muted-weak |
- 잠금화면 폰트 1.3배 클램프(접근성), weight는 테마 토큰화(A 얇게/C 굵게 개성)

## 체크박스 / radius / 모션
- ✓ vector: viewport 24×24, path `M6 12.5 L10.5 17 L18 7.5`, stroke 2dp(C 2.5), round cap/join, fill none (폰트 글리프 폐기)
- 박스 radius: A 6 / B 원형 / C 12, border 미완 2dp (테마 모양 축 식별성 확보)
- 토글 모션: box scale 0.9→1.0 150ms FastOutSlowIn, ✓ trimPathEnd 0→1 180ms, 취소선/alpha 120ms fade, 역재생 해제. **200ms 초과 금지**
- radius 세트: card A24/B22/C26, field A14/B12/C16, chip A12/B999/C16, checkbox A6/B원형/C12
- 테마 전환: 색 220ms crossfade(animateColorAsState)

## 컴포넌트 메모
- 세그먼트(테마선택): height 36dp, radius chip, 트랙 disabled-fill, 선택칸 = 카드색+accent 1.5dp border+accent 라벨, 3등분
- 빈 상태: 카드 minHeight 96dp, 수직중앙, empty 색 + accent@0.4 체크아이콘 28dp + "할 일이 없어요"
- 카드 오버플로: maxHeight = 화면−시계−인셋, 내부 스크롤 + 하단 8dp fade
