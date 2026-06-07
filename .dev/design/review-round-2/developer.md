# 구현 청사진 리뷰 — 안드로이드 엔지니어 (Round 2)

> 리뷰어: 시니어 안드로이드 엔지니어
> 대상: SPEC-design.md / design-tokens.md / decisions.md(R1) / R1 developer.md / 현재 소스
> 전제(R1 확정, 재론 안 함): Jetpack Compose · 글래스=반투명 fill+border · ACTION_TIME_TICK · SharedPreferences · ✕+Undo
> 코드 현황: Kotlin 2.0.21 / AGP 8.7.3 / Gradle 8.14.3 / JDK 17 / minSdk 27 / targetSdk 36, appcompat+viewBinding, `AppTheme` parent=`Theme.AppCompat.Light.NoActionBar`
> 목표: **빌드 가능한 구체 청사진 확정** — 버전 세트·파일 구조·전환 단계·애니/스낵바 구현·미해결 기술 답·R1 미발견 리스크

---

## 0. R2 한눈 요약 (결정)

| 항목 | 확정 |
|------|------|
| Compose 의존성 | compiler plugin `org.jetbrains.kotlin.plugin.compose` **2.0.21**(Kotlin 동일) + `compose-bom` **2024.12.01**(확인 후 최신 2025.x로 올려도 무방) |
| Material3 | **씀(최소)** — Scaffold/SnackbarHost/SwipeToDismissBox만. 색/모양은 커스텀 토큰이 source of truth |
| 두 Activity | `AppCompatActivity` → `ComponentActivity` + `setContent`, **appcompat 의존성 제거**, 매니페스트 테마 parent 교체(§4 리스크) |
| 토큰 | `AppTheme` data class + `staticCompositionLocalOf` (R1 #3 구조 그대로) |
| 시계 | `DisposableEffect`로 TIME_TICK/TIME_CHANGED/TIMEZONE 동적등록, state는 `ClockHeader` **로컬**에 둬 부분 recompose |
| 리스트 | `LazyColumn(items, key=it.id)` + `Modifier.animateItem()` |
| 토글 애니 | `animateColorAsState`(테두리↔accent) + `AnimatedVisibility`/scale(✓) + 햅틱 |
| Undo | Material3 `Scaffold`+`SnackbarHostState`, optimistic 제거 후 `showSnackbar(action=실행취소)` |

---

## 1. Compose 의존성 정확한 버전 세트

### 루트 `build.gradle.kts`
```kotlin
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false  // 추가
}
```
- Kotlin 2.0부터 Compose Compiler는 **별도 Kotlin Gradle 플러그인**으로 분리됐고 **버전을 Kotlin과 정확히 일치**시켜야 한다(2.0.21). 과거 `composeOptions { kotlinCompilerExtensionVersion }` 방식은 더 이상 쓰지 않는다.

### 앱 `app/build.gradle.kts`
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")   // 추가
}

android {
    // ...
    buildFeatures {
        compose = true        // 추가
        // viewBinding = true  // 제거 (Compose 전환 후 미사용)
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")               // Scaffold/Snackbar/SwipeToDismiss
    implementation("androidx.compose.ui:ui-graphics")                    // Brush/Color (보통 ui가 끌어옴)
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    // implementation("androidx.appcompat:appcompat:1.7.0")  // 제거 (§4 참고)
    implementation("androidx.core:core-ktx:1.13.1")                      // ContextCompat.registerReceiver 유지
    testImplementation("junit:junit:4.13.2")
}
```

근거/주의:
- **BOM과 Kotlin은 독립**이다. compose-bom은 androidx.compose.* "런타임" 아티팩트 버전만 정렬하고, Kotlin 호환은 compiler plugin이 담당한다. 따라서 BOM은 비교적 자유롭게 최신을 써도 된다. `2024.12.01`은 Kotlin 2.0.x 시대 안정 조합. **확인 필요**: 빌드 시점 최신 2025.x BOM이 있으면 올려도 무방(런타임만 바뀜).
- `activity-compose 1.9.3`은 `ComponentActivity.setContent` / `enableEdgeToEdge` 제공. **확인 필요**: BOM 2025.x로 올리면 activity-compose도 1.10.x 권장.
- material3을 넣지만 **`MaterialTheme` 색을 컴포넌트 룩의 기준으로 쓰지 않는다.** Snackbar/Scaffold/SwipeToDismissBox의 "동작 골격"만 빌려오고, 표면 색·라운드는 우리 토큰으로 override. (R1 #1 "foundation 중심" 유지)

---

## 2. 패키지 / 파일 구조

```
com.example.locktodo
├─ MainActivity.kt            ComponentActivity → setContent { MainScreen() }
├─ LockActivity.kt            ComponentActivity → setContent { LockScreen() }
├─ ScreenService.kt           (변경 없음 — SCREEN_ON 수신·기동)
├─ data/
│   ├─ Todo.kt                data class Todo(id, text, done)
│   ├─ TodoStore.kt           SharedPreferences(JSON) — KEY_TODOS/KEY_THEME 키 분리
│   └─ ThemePrefs.kt          theme 읽기/쓰기 (TodoStore와 같은 prefs 파일)
├─ theme/
│   ├─ AppTheme.kt            @Immutable data class AppTheme + AppThemeId enum
│   ├─ AppThemes.kt           object { val A,B,C } 토큰 박제 (design-tokens.md 1:1)
│   └─ LocalAppTheme.kt       staticCompositionLocalOf + AppThemeProvider 컴포저블
└─ ui/
    ├─ MainScreen.kt          Scaffold(snackbarHost) + 테마 세그먼트 + 입력 + LazyColumn
    ├─ LockScreen.kt          배경 Brush + ClockHeader + TodoCard
    ├─ ClockHeader.kt         시:분 + 날짜, TIME_TICK DisposableEffect (state 로컬)
    ├─ TodoCard.kt            카드 컨테이너(토큰 radius/fill/border)
    ├─ TodoRow.kt             체크박스+텍스트(취소선) (+메인만 ✕)
    └─ ThemeCheckbox.kt       커스텀 체크박스(테두리↔accent 채움+✓), animateColorAsState
```
- `data/`·`theme/`·`ui/` 3분리로 토큰(소유) ← UI(소비) 의존 방향 단방향. SPEC §5 "한 컴포넌트가 토큰만 바꿔 렌더" 충족.
- 현재 `TodoStore.kt`는 패키지 루트에 있음 → `data/`로 이동(import 경로만 변경, 로직 유지). `Todo` data class도 분리.

---

## 3. 토큰: `AppTheme` data class + CompositionLocal (구체화)

R1 #3 스켈레톤을 design-tokens.md 값으로 박제. 핵심 추가 결정:

```kotlin
@Immutable
data class AppTheme(
    val id: AppThemeId,
    val backgroundBrush: Brush,        // A/C linearGradient, B SolidColor(#f4f5f7)
    val cardColor: Color,              // A White.copy(.10) / B White / C White.copy(.7)
    val cardBorder: Color?,            // A White.copy(.14), B·C null (B는 shadow로 분리)
    val cardElevation: Dp,             // B 8.dp(shadow), A·C 0.dp
    val accent: Color,
    val onAccent: Color,               // ✓ 색 (전 테마 White) — D8 on-accent 토큰
    val textColor: Color,
    val mutedColor: Color,             // 완료/날짜 (D3 대비 충족값)
    val checkboxShape: Shape,          // A RoundedCornerShape(8), C (9), B CircleShape
    val checkboxUncheckedBorder: Color,
    val cardRadius: Dp,                // A/C 24.dp, B 22.dp
    val divider: Color?,               // B #f0f1f4, A·C null
)

val LocalAppTheme = staticCompositionLocalOf<AppTheme> { error("AppTheme not provided") }
```
- `staticCompositionLocalOf` 선택 이유: 테마는 화면 단위로 "통째 교체"되지 그 안에서 부분 변경되지 않는다. `compositionLocalOf`(부분 무효화 추적)보다 **읽기 오버헤드 없는 static**이 맞다. 교체 시엔 Provider의 `provides` 인스턴스가 통째 바뀌어 하위 전체 recompose — 이게 "테마 즉시 반영"의 정확한 메커니즘.
- 그라데이션 각도: 목업 `160deg`/`165deg`는 `Brush.linearGradient`의 start/end Offset으로 환산. CSS 0deg=위쪽·시계방향, Compose Offset은 좌상단 원점. **확인 필요**: 각도→Offset 정밀 환산은 구현 시 시각 확인(픽셀 정밀 불요, R1과 동일). 실용적으로 `Offset(0f,0f)→Offset(0f,height)` 대각 근사로 시작 후 미세조정.
- design-tokens.md의 muted가 두 값(예 A `#8a85a8`/`#b8b2d8`)인데 **완료텍스트용·날짜용 용도가 다름**. D3(대비 ≥4.5:1) 충족 확인 후 `mutedCompleted`/`mutedDate`로 **토큰을 둘로 쪼개라**. 한 값으로 합치면 한쪽이 대비 미달날 수 있음 → §검증과 직결.

---

## 4. ComponentActivity 전환 + 테마 parent (★ R1 미발견 리스크)

R1은 "ComponentActivity로 가라"까지만 말하고 **매니페스트 테마 의존을 빠뜨렸다.** 실제 전환 단계:

1. `class LockActivity : ComponentActivity()` / `class MainActivity : ComponentActivity()`.
2. `onCreate`에서 `enableEdgeToEdge()` 후 `setContent { AppThemeProvider(selected) { LockScreen()/MainScreen() } }`.
3. `setShowWhenLocked(true)`/`setTurnScreenOn(true)`는 `Activity` 메서드(API27+)라 그대로(LockActivity).
4. **appcompat 의존성 제거 가능** — 두 Activity 다 AppCompat 미사용.
5. **그러나 매니페스트 `android:theme="@style/AppTheme"`의 parent가 `Theme.AppCompat.Light.NoActionBar`**. appcompat 의존성을 빼면 이 parent 리소스가 사라져 **빌드/런타임 리소스 not found**. 반드시 parent 교체:
   ```xml
   <!-- res/values/themes.xml -->
   <style name="AppTheme" parent="android:Theme.Material.Light.NoActionBar">
       <item name="android:statusBarColor">@android:color/transparent</item>
       <item name="android:navigationBarColor">@android:color/transparent</item>
       <item name="android:windowBackground">@android:color/transparent</item>
   </style>
   ```
   - AppCompat이 아닌 플랫폼 `android:Theme.Material.*`을 쓰면 appcompat 없이 동작. windowBackground 투명은 LockScreen이 직접 그라데이션을 그리므로 흰 플래시 방지.

**리스크 등급**: 높음(빠뜨리면 빌드 깨짐). R1 결론에 누락돼 있었음.

---

## 5. 엣지투엣지 강제 (★ R1 미발견 리스크)

- **targetSdk 35(Android 15)+부터 edge-to-edge가 기본 강제**다. targetSdk 36이므로 `enableEdgeToEdge()`를 호출하든 안 하든 **콘텐츠가 status/navigation bar 뒤로 그려진다.** R1은 inset 처리를 "노치 아래 두려면" 정도로만 언급했지만, 36에선 **선택이 아니라 필수**다.
- 대응: 최상위 컨테이너에 `Modifier.windowInsetsPadding(WindowInsets.safeDrawing)` 또는 `Scaffold`(contentPadding 자동). LockScreen은 배경 그라데이션은 풀블리드로 깔고, 시계/카드만 `safeDrawing` 패딩.
- 현재 하드코딩 `setPadding(50,120,...)`(px)는 제거 → inset 기반. (R1 #10-1과 연결)
- **확인 필요**: showWhenLocked로 keyguard 위에 뜰 때 일부 OEM에서 insets 보고가 0이거나 다르게 올 수 있음 → 에뮬+실기 1대 시각 확인 권장.

---

## 6. 시계 부분 갱신 (Compose state, OS 시각 일치)

`ClockHeader`에 **state를 로컬로 가둬** 분당 이 컴포저블만 recompose(전체 무깜빡):

```kotlin
@Composable
fun ClockHeader() {
    val context = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }  // 즉시 seed
    DisposableEffect(Unit) {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) { now = System.currentTimeMillis() }
        }
        val f = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)       // 분 경계
            addAction(Intent.ACTION_TIME_CHANGED)    // 수동 시간변경
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, r, f, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(r) }
    }
    // 포맷은 시스템 12/24h·로케일 준수
    val timeText = remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
    val dateText = remember(now) { /* "M월 d일 EEEE" SimpleDateFormat(Locale.getDefault()) */ }
    Text(timeText, style = TextStyle(fontFeatureSettings = "tnum"))  // tabular
    Text(dateText)
}
```

미해결 답:
- **자체 시계가 OS와 시각 일치 보장하는 법**: (1) state seed를 `System.currentTimeMillis()`로 즉시(첫 프레임부터 정확), (2) 표시값을 매 tick마다 다시 `currentTimeMillis()`에서 읽음(자체 카운팅 누적오차 없음), (3) 포맷을 `android.text.format.DateFormat.getTimeFormat(context)`로 — **시스템 12/24h 설정·로케일을 OS와 동일 규칙으로** 적용. `ACTION_TIME_TICK`은 분 경계에 시스템이 쏘므로 OS 시계와 같은 순간에 갱신 → 분 단위 어긋남 없음.
- `RECEIVER_NOT_EXPORTED`: targetSdk 34+ 동적 등록 시 export 플래그 필수(시스템 브로드캐스트라 NOT_EXPORTED로 충분).
- **DisposableEffect가 onStart/onStop 생명주기와 맞나**: setContent 컴포지션은 Activity가 STARTED 동안 유효. LockActivity가 화면에 떠 있을 때만 ClockHeader가 살아있어 register/unregister가 자연히 onStart/onStop과 정렬됨(별도 lifecycle 콜백 불필요). 백그라운드로 가면 컴포지션 일시정지 → 리소스 누수 없음.

---

## 7. LazyColumn 항목 구조 + 토글 애니 + Undo

### LazyColumn
```kotlin
LazyColumn {
    items(todos, key = { it.id }) { todo ->
        TodoRow(
            todo = todo,
            showDelete = isMain,
            modifier = Modifier.animateItem(),   // 삭제/재정렬 시 부드러운 이동
            onToggle = { vm.toggle(todo.id) },
            onDelete = { vm.deleteWithUndo(todo.id) },
        )
    }
    if (todos.isEmpty()) item { EmptyState() }   // "오늘 할 일이 없어요"
}
```
- `key = it.id` 필수 — 없으면 토글/삭제 시 위치 기반 매칭이 깨져 애니·상태 튐.
- **D9 완료항목 하단 sink**: ViewModel에서 `sortedBy { it.done }`로 정렬해 LazyColumn에 전달 → 완료 시 `animateItem()`이 하단 이동을 애니메이션.
- **확인 필요**: 항목 이동 Modifier 이름이 BOM 버전마다 다름. 최신은 `Modifier.animateItem()`(구 `animateItemPlacement()`는 deprecated). 선택한 BOM(2024.12.01)에선 `animateItem()` 사용 — 빌드 시 시그니처 확인.

### 커스텀 체크박스 토글 애니 (R1 #10-2 구체화)
```kotlin
@Composable
fun ThemeCheckbox(checked: Boolean, theme: AppTheme) {
    val fill by animateColorAsState(if (checked) theme.accent else Color.Transparent, label="fill")
    val border by animateColorAsState(
        if (checked) theme.accent else theme.checkboxUncheckedBorder, label="border")
    Box(Modifier.size(24.dp).clip(theme.checkboxShape).background(fill)
            .border(2.dp, border, theme.checkboxShape)) {
        AnimatedVisibility(checked, enter = scaleIn(), exit = scaleOut()) {
            Icon(Icons.Default.Check, null, tint = theme.onAccent)  // ✓ on-accent
        }
    }
}
```
- `animateColorAsState`로 테두리→채움 전환, `scaleIn/Out`으로 ✓ 등장. D5 "채움 애니메이션".
- 햅틱(D5): row `onToggle`에서 `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` 또는 view `performHapticFeedback`. 잠금화면에서도 햅틱 가능(권한 불요).
- **터치 영역 D6 ≥56dp**: 체크박스 아이콘은 24dp지만 **row 전체에 `Modifier.heightIn(min=56.dp).clickable{onToggle}`** → 행 어디를 눌러도 토글. 체크박스 자체엔 clickable 걸지 말 것(이중 타깃 방지).
- **완료 취소선**: `Text(style = LocalTextStyle.current.copy(textDecoration = TextDecoration.LineThrough, color = theme.mutedCompleted))` (D3).

### Undo 스낵바 (메인 전용)
```kotlin
val snackbar = remember { SnackbarHostState() }
val scope = rememberCoroutineScope()
Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { pad -> /* ... */ }

fun deleteWithUndo(id: Long) {
    val removed = vm.removeOptimistic(id)          // 즉시 목록에서 제거(메모리 + prefs)
    scope.launch {
        val res = snackbar.showSnackbar("삭제됨", actionLabel = "실행취소",
                       duration = SnackbarDuration.Short)
        if (res == SnackbarResult.ActionPerformed) vm.restore(removed)  // 원위치 복원
    }
}
```
- LockScreen엔 삭제 없음(D4) → Scaffold/Snackbar는 MainScreen에만.
- 복원 시 **원래 위치 유지**: `removeOptimistic`이 index도 같이 보관, `restore`가 그 index에 insert. id만 재추가하면 정렬상 맨 끝으로 가서 위화감.
- **확인 필요**: 빠른 연속 삭제 시 스낵바 1개만 보이고 직전 것은 자동 dismiss → 그 항목은 영구 삭제 확정. 이 동작이 spike 허용 범위인지(보통 OK).

---

## 8. decisions.md 미해결 — 기술 답

### (a) 자체 시계 vs OS / "전체 대체가 맞나"(devil #2)
- **기술 사실**: 우리는 keyguard를 "대체"하는 게 아니라 `SCREEN_ON` 수신 → showWhenLocked **풀스크린 Activity를 keyguard 위에 띄우는** 방식이다. OS 잠금화면은 여전히 그 아래 존재(우리 Activity를 내리면 다시 보임). 즉 **덮기**지 대체가 아니다.
- 대안 비교: SYSTEM_ALERT_WINDOW **오버레이 윈도우**(권한 이미 보유)로 패널만 띄우는 방법도 있으나, (1) 오버레이는 입력 포커스/IME·접근성 충돌이 잦고 (2) showWhenLocked Activity가 키가드 위 표시·turnScreenOn까지 1급 지원이라 **현 Activity 방식이 기술적으로 더 견고**. → **Activity 풀스크린 유지** 권장. 우리 시계는 우리 Activity가 OS 잠금화면 시계를 가리므로 **필요**(자체 그려야 함).

### (b) 재부팅 직후 표시 = device-protected storage 범위
- **기술 사실**: SharedPreferences 기본은 **CE(Credential-Encrypted) 저장소** → 재부팅 후 **첫 잠금해제 전(Direct Boot 구간)** 접근 불가. 또한 BroadcastReceiver/Service도 `android:directBootAware="true"` 가 아니면 그 구간에 **동작 자체를 안 함** → SCREEN_ON 수신도 안 돼 LockActivity가 아예 안 뜸.
- **결론(범위 결정)**: "콜드 부팅 후 첫 해제 전에도 todo 표시"를 요구로 잡으면 **3중 작업** 필요 — ① `TodoStore`를 `createDeviceProtectedStorageContext()` 기반으로, ② `ScreenService`·`LockActivity`에 `android:directBootAware="true"`, ③ 기존 CE 데이터 DE로 1회 마이그레이션. DE는 사용자 자격증명으로 암호화 안 됨(보안 약화) — todo가 민감정보 아니라 수용 가능.
- **권장**: spike 범위에선 **CE 유지 + 한계 문서화**(이미 한 번 해제한 일반 사이클은 정상). 실제로 "재부팅 직후"가 핵심 시나리오로 확정되면 그때 위 3중 작업. → **scope-out 권장, 요구 확정 시 재오픈.**

### (c) 가로 모드 / 폰트 확대(접근성) Compose 대응
- **폰트 확대**: Compose는 `sp` 단위가 `fontScale`을 자동 반영. 시계 56~64sp가 확대되면 한 줄을 넘을 수 있으므로 `ClockHeader` 텍스트에 `maxLines=1` + `autoSize`(또는 `style` 고정 + 컨테이너 `wrapContentWidth`). **sp 상한을 막지 말 것**(접근성 위반). 리스트는 LazyColumn이라 길어져도 스크롤로 흡수.
- **가로 모드**: 잠금화면 가로는 시계+카드가 세로로 안 들어옴 → 두 선택지. ① **LockActivity를 portrait 고정**(`android:screenOrientation="portrait"` 또는 코드) — spike에 가장 단순, 잠금화면은 보통 세로 사용. ② 가로 지원하려면 LockScreen 루트를 `Column` 대신 가로에서 `Row`(시계 좌/리스트 우)로 `LocalConfiguration.orientation` 분기. → **권장: LockActivity portrait 고정, MainActivity는 세로 스크롤이라 가로도 자동 OK.**
- **B 라이트 야간 눈부심 / 시스템 다크모드 연동**: 비목표(§2 "다크모드 자동 전환 없음", 테마 수동). `isSystemInDarkTheme()` **안 씀**. → 연동 안 함 확정. (야간에 어두운 게 필요하면 사용자가 테마 A 선택.)

---

## 9. R1이 놓친 새 구현 리스크 (신규만)

| # | 리스크 | 등급 | 대응 |
|---|--------|------|------|
| N1 | **매니페스트 테마 parent가 AppCompat** — appcompat 제거 시 리소스 not found로 빌드/런타임 깨짐 | 높음 | §4: parent를 `android:Theme.Material.*`로 교체 + 투명 바 |
| N2 | **targetSdk 36 edge-to-edge 강제** — inset 미처리 시 콘텐츠가 시스템바 뒤로 그려짐 | 높음 | §5: `safeDrawing` 패딩/Scaffold, px 패딩 제거 |
| N3 | **백그라운드 Activity 실행 제한(BAL)** — `ScreenService`가 SCREEN_ON 받아 background에서 Activity 기동. Android 10+ BAL 규제가 14/15에서 강화. targetSdk 36에서 일부 OEM/상황에 차단 가능 | 중(기존 동작이라 잠재) | showWhenLocked+FGS 조합이 예외로 보통 통과하나 **에뮬+실기 회귀 필수**. 차단 시 fullScreenIntent 알림 fallback 검토 |
| N4 | **`Modifier.animateItem()` API 명칭 BOM 의존** — 구 `animateItemPlacement` deprecated | 낮음 | 선택 BOM에서 시그니처 확인(§7) |
| N5 | **시계 state 배치 실수 시 전체 recompose** — time state를 root에 두면 분당 화면 전체 recompose(깜빡임은 없지만 낭비, D7 "부분 갱신" 위반 소지) | 중 | §6: state를 `ClockHeader` 내부 `remember`로 격리 |
| N6 | **showWhenLocked 위 insets 비정상 보고**(일부 OEM 0/오프셋) | 중(확인 필요) | 실기 1대 시각 확인, 비정상 시 status bar 높이 fallback |
| N7 | **테마 즉시 반영의 잠금화면 측 한계** — 메인에서 테마 변경해도 이미 떠 있는 LockActivity는 자동 갱신 안 됨. 다음 표시 때 prefs 재로딩으로만 반영(R1 #9 명시). 동시 표시 시나리오 없으니 허용이나 **테스트 시 "메인 바꾸고 잠금 다시 켜야 반영"** 임을 검증 스크립트에 명시 | 낮음 | prefs를 LockActivity `onStart`/컴포지션 진입 시 read |
| N8 | **Compose 첫 프레임 흰 플래시** — windowBackground 미투명 시 잠금화면 그라데이션 전 흰 깜빡 | 낮음 | §4 `windowBackground=transparent` |
| N9 | **`Notification.Builder`(deprecated) FGS + targetSdk 36** — 기존 ScreenService 코드. NotificationCompat 권장(직접 디자인 무관하나 같이 정리) | 낮음 | 선택적 NotificationCompat 전환 |

---

## 10. 검증 지표 매핑(구현 관점)

- 대비(D3/D11): muted를 `mutedCompleted`/`mutedDate` 2토큰으로 쪼개 **쌍마다** 계산(§3). 완료는 취소선+불투명도 병행.
- 터치 ≥56dp: row `heightIn(min=56.dp).clickable`(§7), 체크박스 단독 clickable 금지.
- 무깜빡 시계: state 격리(§6, N5)로 ClockHeader만 recompose.
- 테마 즉시 반영: 메인은 `staticCompositionLocalOf` 교체로 즉시(§3), 잠금은 다음 표시(N7).
- 기능 회귀: 추가/삭제(+Undo)/체크 — optimistic+restore(§7), 에뮬 실측.

---

## 종합 권고 (R2)

1. **빌드 셋업**: compose plugin 2.0.21 + BOM 2024.12.01 + activity-compose 1.9.3, viewBinding/appcompat 제거, **매니페스트 테마 parent를 플랫폼 Material로 교체**(N1, 빠지면 깨짐).
2. **edge-to-edge는 targetSdk 36에서 강제** — safeDrawing inset 필수(N2), px 패딩 전면 제거.
3. **시계 state는 ClockHeader 로컬에** 격리해 부분 recompose + `currentTimeMillis()` 재읽기로 OS 시각 일치(§6).
4. **재부팅 직후 표시는 scope-out 권장**(CE 유지+한계 문서화), 요구 확정 시에만 DE+directBootAware 3중 작업(§8b).
5. **가로=LockActivity portrait 고정, 폰트확대=sp 상한 미설정+시계 maxLines/autoSize, 다크모드 연동 안 함**(§8c).
</content>
</invoke>
