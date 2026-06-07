# LockTodo — 원페이저

> 잠금화면을 풀지 않고 할 일을 보고 바로 체크하는 안드로이드 앱

## 한 줄
화면을 켜면 잠금화면 위에 **시계 + 오늘 할 일**이 떠서, **잠금 해제 없이** 체크할 수 있습니다.

## 문제
일반 투두 앱은 폰을 풀고 → 앱을 찾아 열어야 확인·체크가 됩니다. 잠금화면 위젯은 탭하면 결국 잠금 해제를 요구합니다.

## 해결
`showWhenLocked` 액티비티를 **잠금 상태에서만** 잠금화면 위에 띄우고, 그 안의 체크박스를 **잠금 해제 없이** 토글합니다. 기능은 추가 / 삭제(+실행취소) / 체크로 좁혀 군더더기를 없앴습니다.

## 데모
![잠금화면 시계+체크](docs/screenshots/04-lock-checked.png)

## 기술
- Kotlin + Jetpack Compose, `AppTheme` 토큰 시스템(3테마: 다크/라이트/따뜻)
- `KeyguardManager` 가드 + `SYSTEM_ALERT_WINDOW`(막히면 `fullScreenIntent` fallback)
- 시계 `ACTION_TIME_TICK` 부분 갱신, WCAG AA 대비 통과 색
- minSdk 27 / targetSdk 36 (Android 16)

## 만든 방식
4개 페르소나(디자이너·UX·엔지니어·Devil's Advocate) 디자인 리뷰를 여러 라운드 돌려 수렴 → Compose 리팩터링 → Devil's Advocate 4-pass로 "결정이 코드에 반영됐는지"까지 검증. 전 과정은 [`.dev/design/`](.dev/design/).

## 링크
- 저장소: https://github.com/Kimsoo0119/locktodo-spike
- 상세: [README.md](README.md) · 설계/결정: [.dev/design/decisions.md](.dev/design/decisions.md)
