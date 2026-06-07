# Devil 렌즈3 — 디자인 충실성 검증

대상: `ui/Theme.kt`, `ui/Components.kt` (+ 교차확인 `MainActivity.kt`, `LockActivity.kt`)
기준: `design-tokens.md`(R2 확정), `decisions.md` D14~D16·D21

---

## VERDICT: FAIL

색 hex / radius / weight 값 자체는 **AppTheme 에 정의된 범위 안에선 전부 정확**하다(아래 §A 확인). 그러나 design-tokens.md 가 명시한 토큰 다수가 AppTheme/컴포넌트에 **아예 존재하지 않고**, R2 에서 "결정만 하고 코드 미반영"으로 닫았던(D13~D16) 함정이 **다시 재발**했다. 특히 D16 이 "폰트 글리프 폐기"를 명시했는데도 체크 ✓ 가 여전히 Material 폰트 글리프다. 이건 값 불일치가 아니라 결정 위반.

---

## §A 값 대조 — 일치하는 것 (정확함, 트집 없음)

3테마 전부 design-tokens.md 표와 hex/alpha/radius/weight 정확히 일치:

- A: bg #2B1F54→#1A1438→#0D0A1F, card-fill-opaque #332B4F, border #FFF@.16, text #FFF, muted #B8B2D8/#8A85A8, accent #7C6CFF, pressed #6A5AE0, on-accent #FFF, ripple #FFF@.12, destructive #FFF@.45 / #FF6B6B, divider none, checkbox r6(D21), card r24, field r14, weight 300/400/400.
- B: bg #F4F5F7, **bg-lock #ECEEF1**(별도값 반영 ✓), card-fill #FFF, border none, text #23262E, muted #9AA0AD/#B6BAC3, accent #4F6BED, pressed #3F58D8, ripple #4F6BED@.12, destructive #9AA0AD / #E5484D, divider #F0F1F4, checkbox 원형, card r22, field r12, weight 600/500/500.
- C: bg #FFE9D6→#FFD9C2→#FFC9BE, card-fill-opaque #FFF6EF, border #FFF@.55, text #5B3A2E, muted #B07D63/#C9A392, accent #FF8A5C, pressed #F2754A, ripple #FF8A5C@.14, destructive #C9A392 / #E5484D, divider none, checkbox **r12(D21)**, card r26, field r16, weight 700/600/600.

핵심 합격 항목:
- **on-accent.C = #5B3A2E (D15 갈색 ✓)** → `onAccent = Color(0xFF5B3A2E)`, `TodoCheckbox` tint=`t.onAccent`. 정확히 반영 ✓
- **완료 = text×0.60 + 취소선 (D15)** → `itemTextStyle`: `color = t.text.copy(alpha=0.60f)` + `TextDecoration.LineThrough`. 합성색 = 취소선색(데코는 텍스트색 상속) ✓
- **체크박스 미완 외곽선 2dp / 미완색 muted-weak** → `border(2.dp, t.mutedWeak)` ✓
- destructive 평상/확인 2단계 모두 필드화(`destructive`/`destructiveConfirm`) ✓
- divider 토큰화 ✓
- row 터치 `heightIn(min=56.dp)` (MainActivity:163, LockActivity:103) ✓

---

## §B 결함 — 누락된 토큰 (AppTheme 에 필드 자체가 없음)

design-tokens.md "색 — 상태" / "색 — 배경" 표에 있으나 AppTheme data class 에 부재. 전 .kt 파일 grep 결과 어디서도 안 쓰임(`pressed|disabled|shadow|elevation|crossfade|statusBars|trimPath` 0건).

1. **`pressed` (row) 누락** — 토큰 A #FFF@.08 / B #000@.06 / C #5B3A2E@.07. AppTheme 에 필드 없음. (`accentPressed` 는 accent-pressed 토큰으로 별개. row press 오버레이 토큰 아님.) 행 clickable 에 press 표현 없음.
2. **`disabled-text` 누락** — #FFF@.38 등 3색. 필드 없음.
3. **`disabled-fill` 누락** — #FFF@.12 등 3색. 필드 없음.
4. **`card-shadow` 누락 (D14)** — B `0 8 24 rgba(20,20,40,.06)`, C `0 6 18 rgba(180,110,80,.12)`. AppTheme 에 shadow 필드 없고 `TodoCard`(Components.kt:50) 에 elevation/shadow modifier 없음. B/C 카드가 평면.
5. **`focus` (입력 1.5dp) 누락** — 값은 accent 와 동일하나 토큰 명시. MainActivity:137 입력 필드에 focus border(1.5dp) 구현 없음.
6. **`space.*` 미시스템화** — xs/sm/md/lg/xl/2xl(4dp 그리드, D16) 상수 0개. 전부 매직넘버(`16.dp`,`12.dp`,`24.dp` 등 산재). 토큰 시스템화(D16) 미충족.

---

## §C 결함 — "결정했지만 코드에 없음" (R2 결정≠반영 재발)

7. **체크 ✓ 가 폰트 글리프 — D16 직접 위반.** design-tokens.md "체크박스" §: *"✓ vector: viewport24×24, path `M6 12.5 L10.5 17 L18 7.5`, stroke 2dp(C 2.5), round cap/join, fill none (폰트 글리프 폐기)"*. 그러나 Components.kt:73 `Icon(Icons.Default.Check, ...)` = Material 폰트 글리프 그대로. 커스텀 벡터 path 미구현. **결정 명시문구를 정면 위반.**
8. **체크박스 stroke C 2.5dp 차등 없음** — 토큰 stroke 2dp(C 2.5). 글리프라 stroke 개념 부재 + border 도 전테마 균일 2dp. C 개성 축(stroke) 실효 안 됨.
9. **세이프에어리어 인셋 시계 top 누락 (D16, space.2xl)** — 토큰 *"시계 top = WindowInsets.statusBars + space.xl … 고정값 금지"*. `ClockHeader`(Components.kt:79) 에 statusBars 인셋·top 패딩 전무. 노치/펀치홀 미대응.
10. **테마 전환 220ms crossfade 누락** — 토큰 *"테마 전환: 색 220ms crossfade(animateColorAsState)"*. 컴포넌트는 `t.text` 등 색을 직접 읽음(crossfade 래핑 없음). MainActivity:70 / LockActivity:56 도 `CompositionLocalProvider` 로 즉시 교체. 전환 시 색 점프.
11. **토글 box scale 0.9→1.0 누락** — 토큰 *"box scale 0.9→1.0 150ms"*. Components.kt:65 는 `fill` **색 fade**(animateColorAsState 150ms)만. 박스 스케일 애니 없음.
12. **✓ trimPathEnd 0→1 미구현** — 토큰 *"✓ trimPathEnd 0→1 180ms"*. 글리프라 path trim 불가 → `scaleIn(180)/scaleOut(120)` 으로 대체(§7 글리프 결함의 파생).
13. **clock-ampm 20sp muted 누락** — 토큰 `clock-ampm` 20sp/muted/12h. `ClockHeader` 는 `DateFormat.getTimeFormat` 결과를 단일 Text 60sp 로 렌더 → AM/PM 이 60sp 본문에 섞임, 20sp muted 분리 없음.
14. **card-label "할 일" 누락** — 토큰 `card-label` 12sp/600/uppercase 1sp letterSpacing 전테마 공통. `TodoCard` 및 어느 화면에도 "할 일" 라벨 렌더 없음.
15. **빈 상태 체크아이콘 누락** — 토큰 *"empty 색 + accent@0.4 체크아이콘 28dp + 문구"*. `EmptyState`(Components.kt:91) 는 minHeight 96dp ✓ + Text 만. accent@0.4 28dp 아이콘 없음.
16. **카드 오버플로 스펙 미충족** — 토큰 *"maxHeight = 화면−시계−인셋, 내부 스크롤 + 하단 8dp fade"*. LockActivity:85 `LazyColumn(heightIn(max=480.dp))` = 화면기반 아닌 고정 매직넘버, 하단 8dp fade 없음. MainActivity 목록은 maxHeight 자체 없음.

---

## 요약 표

| # | 항목 | 토큰/결정 | 코드 | 판정 |
|---|------|-----------|------|------|
| 1 | pressed(row) | 3색 | AppTheme 필드 없음 | 누락 |
| 2 | disabled-text | 3색 | 없음 | 누락 |
| 3 | disabled-fill | 3색 | 없음 | 누락 |
| 4 | card-shadow(D14) | B/C 그림자 | TodoCard elevation 없음 | 누락 |
| 5 | focus 1.5dp | accent값 | 입력 focus border 없음 | 누락 |
| 6 | space.* 토큰 | 4dp 그리드 | 매직넘버 산재 | 미시스템화 |
| 7 | ✓ 커스텀 벡터 | "폰트 글리프 폐기"(D16) | Icons.Default.Check | **결정 위반** |
| 8 | stroke C 2.5 | 차등 | 균일 2dp | 누락 |
| 9 | 시계 top 인셋 | statusBars+xl(D16) | 패딩 없음 | 누락 |
| 10 | 테마 220ms crossfade | animateColorAsState | 즉시 교체 | 누락 |
| 11 | box scale 0.9→1.0 | 150ms | 색 fade만 | 누락 |
| 12 | ✓ trimPathEnd | 180ms | scaleIn 대체 | 누락 |
| 13 | clock-ampm 20sp muted | 분리 | 60sp 인라인 | 누락 |
| 14 | card-label "할 일" | 12sp/600/uppercase | 없음 | 누락 |
| 15 | empty 체크아이콘 28dp | accent@0.4 | Text만 | 누락 |
| 16 | 카드 오버플로 | 화면기반+8dp fade | 고정480/fade없음 | 미충족 |

값 일치(§A)는 통과, 그러나 **토큰 6개 미정의 + 결정사항 10건 미반영(그중 1건은 명시 결정 정면 위반)** 로 FAIL.
