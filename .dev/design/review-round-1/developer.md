# 디자인 스펙 구현 리뷰 — 안드로이드 엔지니어 (Round 1)

> 리뷰어 페르소나: 시니어 안드로이드 엔지니어
> 대상: `.dev/design/SPEC-design.md`, `design-tokens.md`, `mockups.html`
> 코드 베이스: Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.14.3 / minSdk 27 / targetSdk 36(Android 16), ViewBinding on, 의존성 core-ktx·appcompat 뿐
> 관점: "디자인이 예쁘냐"가 아니라 **이 스펙을 안드로이드에서 실제로 구현할 수 있냐, 어떤 API/비용이 드냐**

---

## 결론 먼저 (SPEC §8 열린 질문에 대한 답)

| 열린 질문 | 결론 | 근거(요약) |
|-----------|------|-----------|
| Compose vs XML+Material3 | **Jetpack Compose** | 3개 테마가 Material 표준에서 완전히 벗어난 커스텀 토큰이라 Material3 의 이점(M3 컴포넌트·dynamic color)을 못 쓴다. 이미 코드가 프로그래매틱이라 XML 레이아웃에 투자한 자산이 없음. 커스텀 체크박스 모양·취소선·글래스·스와이프 삭제가 Compose 에서 전부 1급 지원. Kotlin 2.0.21 이라 Compose Compiler 플러그인 적용이 깔끔(아래 #1) |
| 글래스(backdrop blur) | **실시간 backdrop blur 불필요** — 반투명 fill + 1px 보더로 동일하게 보임 | 배경이 우리가 그린 **부드러운 그라데이션**이라 blur 할 고주파 디테일이 없음. blur 했을 때와 반투명 fill 이 시각적으로 거의 동일(아래 #4) |
| 삭제 인터랙션 | **✕ 아이콘을 기본, 스와이프는 보조** | 메인에서만. 잠금화면은 삭제 없음(SPEC §4). 스와이프 단독은 발견성(discoverability) 낮음(아래 #7) |
| 테마 선택 위치 | **메인 상단 세그먼트(A/B/C)** | 화면 3개뿐인 spike 에 설정 화면 분리는 과함. 즉시 프리뷰가 선택의 핵심 가치(아래 #8) |
| 시계 포맷 | **시스템 설정을 따른다(12/24h 자동)** + 날짜는 `LocalizedDate`/`SimpleDateFormat("M월 d일 EEEE")` | 잠금화면 시계가 OS 설정과 다르면 위화감. `TextClock` 또는 `DateFormat.is24HourFormat()` 로 분기(아래 #6) |

---

## 1. 구현 방식: Jetpack Compose 추천 (확정)

**근거**

- **테마가 Material 을 벗어난다.** design-tokens.md 의 A/B/C 는 배경 그라데이션·글래스 카드·테마별 체크박스 "모양"(A/C 라운드 사각, B 원형)까지 다르다. 이건 Material3 의 `ColorScheme`/`Shapes` 로 표현되는 범위를 넘고, 오히려 Material3 컴포넌트(`Checkbox`, `Card`)의 기본 룩을 일일이 override 해야 해서 손해다. → Material3 의존성의 이점이 거의 없음. (Compose 를 쓰되 `material3` 대신 `foundation` 중심으로 가도 됨)
- **dynamic color 는 비목표.** SPEC §2 에서 다크모드 자동 전환·다국어를 명시적으로 제외했고 테마는 수동 선택이다. Material3 의 최대 셀링포인트인 Material You dynamic color(Android 12+)를 안 쓴다. → Material3 채택 이유가 더 약해짐.
- **현재 UI 가 이미 프로그래매틱.** XML 레이아웃 자산이 0 이라 XML 로 가도 새로 다 써야 한다. 마이그레이션 비용이 Compose 와 동일선상.
- **커스텀 위젯이 Compose 에서 압도적으로 쉽다.** 토큰 기반 체크박스(외곽선↔accent 채움+✓), 취소선+muted, 카드 radius/글래스, 빈 상태, 스와이프 삭제 전부 `Modifier` + `Canvas`/`Box` 조합으로 선언형 처리. XML 이면 커스텀 drawable(selector/shape) + 커스텀 View + RecyclerView + ItemTouchHelper 로 코드량이 훨씬 많다.
- **버전 호환 OK.** Kotlin 2.0 부터 Compose Compiler 가 `org.jetbrains.kotlin.plugin.compose` Kotlin Gradle 플러그인으로 통합됨. 현재 Kotlin 2.0.21 이라 **플러그인 버전을 Kotlin 과 동일하게(2.0.21) 맞추면 끝.** AGP 8.7.3 / Gradle 8.14.3 / JDK 17 모두 최신 Compose BOM 과 호환. Compose 의 minSdk 요구는 21 이라 minSdk 27 문제 없음.

**비용(정직하게)**

- 의존성 추가: `androidx.activity:activity-compose`, Compose BOM + `ui`, `foundation`, (선택) `material3`, `ui-tooling-preview`/`ui-tooling`(debug). appcompat-only 대비 의존성·APK 증가는 있으나 R8 minify 적용 시 수 MB 수준. spike 에선 무시 가능.
- 빌드 설정: `buildFeatures { compose = true }` 추가, `buildFeatures { viewBinding }` 는 더 이상 불필요(제거 가능).
- 빌드 시간 소폭 증가(Compose Compiler). 체감 수준 아님.

> **확인 필요**: 회사/개인 CI 가 있다면 Compose Compiler 플러그인 추가 후 첫 빌드만 한번 돌려 캐시/버전 충돌 없는지 확인. 로컬 단독 spike 면 불필요.

**XML+Material3 를 택해야 하는 유일한 시나리오**: 팀에 Compose 경험이 전무하고 학습 비용을 못 감수할 때. 그 외엔 이 프로젝트 특성상 Compose 가 명확히 우위.

---

## 2. `LockActivity` 를 `AppCompatActivity` → `ComponentActivity` 로 (Compose 채택 시)

- Compose 는 `ComponentActivity.setContent {}` 면 충분. `AppCompatActivity` 의 AppCompat 테마 inflate 가 잠금화면 위 투명/엣지투엣지 구성에 오히려 방해될 수 있다.
- `setShowWhenLocked(true)` / `setTurnScreenOn(true)` 는 `Activity` 메서드(API 27+)라 `ComponentActivity` 에서도 그대로 동작. 현재 코드(`LockActivity.kt:18-21`)와 매니페스트 `android:showWhenLocked`(이미 선언됨) 유지.
- 엣지투엣지: 시계를 노치 아래 상단에 두려면 `enableEdgeToEdge()` + `WindowInsets` 로 status bar inset 처리. 현재 하드코딩 패딩(`setPadding(50,120,50,50)`)은 기기별로 깨지므로 inset 기반으로 교체.

---

## 3. 테마 시스템: Compose 커스텀 Theme(`CompositionLocal`)로 구현

design-tokens.md 를 **그대로 코드 토큰으로 1:1 매핑**하는 게 핵심. 권장 구조:

```kotlin
enum class AppThemeId { A_DARK_GLASS, B_LIGHT, C_WARM }

@Immutable
data class AppTheme(
    val backgroundBrush: Brush,        // A/C 그라데이션, B 단색
    val cardColor: Color,              // 반투명/흰
    val cardBorder: Color?,            // A 만 1px 보더
    val accent: Color,                 // 체크 채움
    val textColor: Color,
    val mutedColor: Color,             // 완료/날짜
    val checkboxShape: Shape,          // A/C RoundedCornerShape, B CircleShape
    val checkboxUncheckedBorder: Color,
    val cardRadius: Dp,                // A/C 24~26, B 22
    val divider: Color?,               // B 만
)

val LocalAppTheme = staticCompositionLocalOf<AppTheme> { error("no theme") }
```

- 3개 인스턴스를 `object AppThemes { val A = AppTheme(...); val B = ...; val C = ... }` 로 토큰값 박아넣고, 루트에서 `CompositionLocalProvider(LocalAppTheme provides selected) { ... }`.
- 한 컴포넌트(`TodoRow`, `TodoCard`, `ClockHeader`)가 `LocalAppTheme.current` 만 읽어 렌더 → SPEC §5 "한 컴포넌트가 토큰만 바꿔 렌더" 요구 정확히 충족.
- **Material3 의 `MaterialTheme` 은 쓰지 않거나 최소만.** 위 커스텀 토큰이 source of truth. (Material 컴포넌트의 기본 색에 끌려가면 테마 일관성 깨짐)
- 그라데이션은 `Brush.linearGradient(colorStops, start, end)`. 목업의 `160deg`/`165deg` 각도는 `start`/`end` Offset 으로 환산(확인 필요: 정확한 각도→Offset 변환은 구현 시 시각 확인 권장. 픽셀 단위로 정밀할 필요는 없음).

> XML 로 갔다면: `themes.xml` 에 3개 style + 커스텀 `attr`(`?attr/accentColor` 등) 정의 → 액티비티에서 `setTheme()` → drawable selector 가 attr 참조. 동작은 하지만 토큰 1개 추가할 때마다 attr 선언+3 style+drawable 동기화라 유지보수가 무겁다. Compose 권장 이유의 핵심.

---

## 4. 글래스(backdrop blur): "진짜 blur" 는 대부분 불필요 — 가장 중요한 발견

목업의 `backdrop-filter:blur(20px/8px)` 를 안드로이드에서 곧이곧대로 재현하려고 하면 함정에 빠진다. 안드로이드 API 정리:

| API | 동작 | 버전 | 우리 경우 적합? |
|-----|------|------|----------------|
| `Modifier.blur()` (Compose) / `View.setRenderEffect()` + `RenderEffect.createBlurEffect` | **그 뷰/컴포저블 "자기 자신"의 픽셀**을 블러 | API 31+ (이하에선 blur 미적용·no-op) | ❌ 카드 자기 픽셀만 흐림. "뒤 배경을 비춰 흐리는" backdrop 아님 |
| `Window.setBackgroundBlurRadius()` | **윈도우 뒤 전체**를 블러 | API 31+ + 기기 플래그(`ro.surface_flinger.supports_background_blur`) + translucent window | △ 윈도우 전체 단위. 카드 한 장만 선택적 블러 불가. 기기 미지원 시 무효 |
| `WindowManager.LayoutParams` `BLUR_BEHIND` / dim | 윈도우 뒤 dim/blur | API 31+ | △ 위와 동일 |

**핵심**: HTML `backdrop-filter` 같은 "요소 단위 backdrop blur" 는 안드로이드에 1급 API 가 없다. 그런데 —

- **우리 배경은 실제 배경화면이 아니라 우리가 그린 부드러운 그라데이션(A/C)·단색(B)이다.** 부드러운 그라데이션에는 블러로 뭉갤 고주파 디테일이 없어서, **blur 한 결과와 "반투명 흰색 fill + 1px 보더" 가 시각적으로 거의 동일**하다.
- 따라서 **A/C 글래스는 `Color.White.copy(alpha=.10/.7)` fill + 보더로 구현하면 목업과 사실상 구분 불가**. 진짜 RenderEffect 블러를 쓸 필요가 없다. → 구현 단순화 + API 31 분기 제거 + 모든 기기 동일 룩.

**진짜 backdrop 가 필요한 유일한 경우**: 잠금화면 배경을 우리 그라데이션이 아니라 **사용자의 실제 배경화면**으로 보여주고 그 위에 글래스 카드를 띄우고 싶을 때. 그땐 윈도우를 translucent 로 만들고 `Window.setBackgroundBlurRadius()` 사용 — 단 **API 31+ 이고 기기 지원 플래그가 있어야 하며**, 미지원 기기 fallback(반투명 fill)을 반드시 준비. 현재 SPEC(§3-1 "배경은 선택 테마 그라데이션")은 우리 그라데이션을 그리는 방향이므로 **이 복잡도는 불필요**.

**개선점**: SPEC/토큰에서 "blur" 라는 단어가 구현자에게 RenderEffect 강제로 읽히지 않도록, "글래스 = 반투명 fill + 보더(블러는 선택)" 로 명시 권장.

---

## 5. 잠금화면 제약 (showWhenLocked) 점검

- **현재 동작 구조는 유효.** `ScreenService`(`ScreenService.kt`)가 `ACTION_SCREEN_ON` 동적 수신 → `LockActivity` 를 `FLAG_ACTIVITY_NEW_TASK` 로 기동. `showWhenLocked`(매니페스트+코드 둘 다 선언됨)로 keyguard 위 표시. 디자인 변경과 무관하게 유지.
- **테마 적용은 잠금화면에서도 자유롭다.** showWhenLocked 액티비티도 일반 액티비티라 Compose/색/그라데이션 다 적용 가능. 제약은 "잠금 해제가 필요한 동작(보안 화면 진입 등)"이지, 렌더링이 아니다.
- **저장소 주의(엣지)**: SharedPreferences 기본은 **Credential-Encrypted(CE) 저장소**라, **기기 재부팅 후 한 번도 잠금 해제 안 한 상태(Direct Boot 이전)** 에서는 접근 불가 → 그 타이밍에 잠금화면이 떠도 todo 를 못 읽을 수 있다. 일반적 "잠갔다 켜는" 사이클(이미 한번 해제함)에선 문제 없음. spike 면 무시 가능하나, 재부팅 직후 표시까지 보장하려면 `createDeviceProtectedStorageContext()` 기반 저장 필요. (확인 필요: 실제 요구 범위인지)
- **잠금 위에서 체크 토글**은 우리 앱 자체 저장소 쓰기라 잠금 상태와 무관하게 동작(CE 접근 가능한 정상 사이클 기준). 현재 `LockActivity.kt:50-53` 토글 로직 유지 가능.

---

## 6. 시계 1분 갱신: `ACTION_TIME_TICK` 또는 `TextClock`

두 가지 방법, 둘 다 검증된 패턴:

**(a) XML 잔재 없이 Compose 라면 — BroadcastReceiver `ACTION_TIME_TICK`**
- `Intent.ACTION_TIME_TICK` 는 **매 분(분 경계)마다** 시스템이 브로드캐스트. SPEC "1분마다 갱신" 에 정확히 부합.
- **반드시 동적 등록**: `ACTION_TIME_TICK` 는 **매니페스트 정적 등록 불가**(시스템이 막음). 액티비티 `onStart`/`onResume` 에서 `registerReceiver`, `onStop`/`onPause` 에서 `unregisterReceiver`(누수 방지). 함께 `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` 도 구독해 수동 시간변경/시간대 반영.
- targetSdk 36 에서 동적 등록 시 `RECEIVER_NOT_EXPORTED` 플래그 필요(시스템 브로드캐스트라 사실상 무해하지만 API 가 요구). `ContextCompat.registerReceiver(..., ContextCompat.RECEIVER_NOT_EXPORTED)` 사용.
- Compose 에선 `DisposableEffect` 로 등록/해제 + `mutableStateOf<LocalTime>` 갱신.

**(b) 더 간단한 대안 — `TextClock` 위젯**
- `TextClock`(API 17+)은 OS 가 자동으로 분 단위 갱신·시간대·12/24h 까지 처리. `format12Hour`/`format24Hour` 지정.
- 단점: View 라 Compose 에선 `AndroidView {}` 로 감싸야 함. 순수 Compose 일관성은 (a)가 나음. **XML 로 갔다면 TextClock 이 가장 적은 코드.**

**시계 포맷(§8 답)**: `android.text.format.DateFormat.is24HourFormat(context)` 로 **시스템 설정을 따른다.** 잠금화면 시계가 OS 시계와 다르면 위화감이 크다. 날짜는 `"M월 d일 EEEE"`(예: 6월 7일 토요일) 로컬 포맷. (목업이 "9:41" 12h 로 그려져 있지만 이는 예시일 뿐, 시스템 설정 우선이 맞음.)

> **주의**: SPEC/목업의 "9:41" 고정 시각은 디자인 예시. 구현은 실제 현재 시각. 명시 권장.

---

## 7. 삭제 인터랙션(§8 답): ✕ 아이콘 기본 + 스와이프 보조

- **잠금화면(LockActivity)엔 삭제 없음**(SPEC §4 "우측 삭제(메인만)"). 잠금 위에서 실수 삭제 방지 차원에서도 옳음.
- 메인: **작은 ✕ 아이콘이 기본.** 스와이프 단독은 발견성이 낮아(아무 안내 없으면 기능 존재를 모름) spike 사용성 테스트에서 불리.
- 스와이프는 보조로 추가 가능: Compose `material3` 의 `SwipeToDismissBox` 가 1급 지원(별도 라이브러리 불필요). XML 이면 RecyclerView + `ItemTouchHelper`.
- **둘 다** 구현 시 비용은 Compose 기준 낮음. 다만 round-1 범위에선 ✕ 아이콘만으로 확정하고 스와이프는 "여유 되면" 정도 권장.

---

## 8. 테마 선택 UI(§8 답): 메인 상단 세그먼트, 선택 즉시 프리뷰

- 화면 3개뿐인 spike 에 설정 화면을 따로 파는 건 과함. **메인 상단에 A/B/C 세그먼트(또는 3 칩).**
- 선택 즉시 메인 자체가 그 테마로 다시 그려져야(즉시 프리뷰) 선택의 의미가 산다. Compose `mutableStateOf(themeId)` + `LocalAppTheme` 교체로 recomposition 한 방.
- 잠금화면은 다음 표시 때 저장된 값 반영(아래 #9).

---

## 9. 저장(SharedPreferences) 충분성 + 테마 저장

- **todo 데이터**: 항목 수가 적은 잠금화면 todo 라 `SharedPreferences` + JSON(현재 `TodoStore.kt`) 으로 **충분**. DataStore/Room 은 spike 단계 과투자.
- **테마 선택값**: 같은 prefs 에 `theme` 키 하나(`"A"/"B"/"C"` 또는 enum name) 추가하면 끝. `MainActivity` 와 `LockActivity` 가 같은 prefs(`"locktodo"`) 를 읽으므로 일관 반영.
- **개선점(현 코드)**: `TodoStore` 의 매 호출 `load→mutate→save` 전체 직렬화는 항목 적어 OK 지만, 같은 prefs 파일에 todo·theme 혼재 시 키 분리(`KEY_TODOS`, `KEY_THEME`) 명확히. 동시쓰기 경쟁은 단일 프로세스라 사실상 없음.
- **반응형 동기화 한계(허용 가능)**: 메인·잠금이 동시에 떠서 한쪽 변경을 다른 쪽이 실시간 반영하는 시나리오는 없음(잠금화면은 보일 때 `onResume`/표시 시점에 `load`). SPEC 요구상 문제 없음.

---

## 10. 그 외 코드 레벨 개선점(디자인 구현과 직접 연관)

1. **하드코딩 패딩/사이즈 제거**: `MainActivity`/`LockActivity` 의 `setPadding(40,60,...)`, `setPadding(50,120,...)` 는 px 단위라 dpi 별로 깨짐. Compose `dp` 또는 inset 기반으로 교체. (디자인 토큰의 radius/패딩 dp 값과 직결)
2. **체크박스를 표준 `CheckBox` 에서 커스텀으로**: 현재 `LockActivity` 의 안드로이드 기본 `CheckBox`(`LockActivity.kt:46`)는 테마별 모양(라운드 사각/원형)·accent 채움+흰 ✓ 를 못 낸다. Compose 커스텀 `Box`(테두리↔채움) + ✓ 아이콘으로 교체 필요.
3. **완료 취소선**: Compose `TextStyle(textDecoration = TextDecoration.LineThrough)` + muted color. 현재 텍스트 prefix(`"✓ "/"○ "`)·기본 CheckBox 방식 제거.
4. **터치 영역 ≥48dp**(토큰 명시): row `Modifier.heightIn(min = 48.dp)` + 체크박스/✕ 의 `Modifier.minimumInteractiveComponentSize()` 또는 충분한 clickable 패딩.
5. **빈 상태**: SPEC §3-1 "오늘 할 일이 없어요". 현재 잠금은 "할 일이 없습니다"(`LockActivity.kt:42`), 메인은 빈 상태 처리 없음 — 문구 통일·메인에도 추가.
6. **리스트 렌더**: 현재 `forEach` + `removeAllViews` 전체 재생성. Compose `LazyColumn(items, key = { it.id })` + `animateItem()` 으로 토글/삭제 시 부드러운 전환(디자인 품질 직결).

---

## 종합 권고

- **Compose 로 간다.** Kotlin 2.0.21 라 도입 장벽 낮고, 3 커스텀 테마·커스텀 체크박스·스와이프·애니메이션이 전부 1급 지원. Material3 는 안 쓰거나 최소.
- **글래스는 반투명 fill + 보더로 충분.** 우리 그라데이션 배경 위에선 진짜 RenderEffect 블러가 시각적으로 무의미하고 API 31 분기·기기 호환 리스크만 늘린다. (실제 배경화면 위 블러를 원하면 그때만 `Window.setBackgroundBlurRadius` + 기기 fallback)
- **시계는 시스템 12/24h 따라가고**, `ACTION_TIME_TICK` 동적 수신(매니페스트 등록 불가, `RECEIVER_NOT_EXPORTED`)로 1분 갱신.
- **저장은 SharedPreferences 유지**, 테마 키 하나 추가. (재부팅 직후 표시까지 보장 필요하면 device-protected storage 검토 — 확인 필요)
- **삭제 = ✕ 아이콘 기본(메인만), 테마 선택 = 메인 상단 세그먼트 즉시 프리뷰.**

### "확인 필요"로 남긴 것
- 그라데이션 각도(160/165deg)→Compose Offset 정밀 변환은 구현 시 시각 확인 권장(픽셀 정밀 불요).
- 재부팅 직후(첫 잠금해제 전) 잠금화면 표시까지 보장이 실제 요구인지 → 맞다면 device-protected storage 필요.
- Compose Compiler 플러그인 추가 후 첫 빌드 1회 확인(CI 있을 경우).
