# LockTodo — 작업 지침 / 진행 상황

> 잠금화면 위에서 할 일을 보고 잠금 해제 없이 체크하는 안드로이드 앱. 상세는 [SPEC.md](./SPEC.md).
> GitHub: https://github.com/Kimsoo0119/locktodo-spike

## 범위 (딱 이것만)
할 일 **추가 / 삭제 / 체크**. 날짜·기한·시계·우선순위 등 부가 속성 일체 없음.

## 최대 리스크 = 골든 게이트 G1 (코드보다 먼저 증명)
> **G1. 에뮬레이터 잠금 상태에서 화면을 켜면, 앱 화면이 자동으로 잠금화면 위에 뜨고, 잠금 해제 없이 체크박스 토글이 동작한다.**

G1 통과가 본 개발 진입 조건. 실패 시 표시 방식(full-screen intent 등) 재설계.

## 아키텍처 (확정, 리서치 근거 SPEC §3)
- 상주 **ForegroundService** → `ACTION_SCREEN_ON` 동적 수신 (정적 등록 불가)
- 화면 ON → **LockActivity** 실행. `SYSTEM_ALERT_WINDOW` 권한으로 백그라운드 Activity 시작 제한(BAL) 우회
- LockActivity: `setShowWhenLocked(true)` + `setTurnScreenOn(true)`, 할 일 목록 + 체크박스(무잠금 토글)
- 권한은 사용자 수동 허용 전제

## 진행 상황
- [x] SPEC.md 작성, Gradle 골격 + app 빌드 설정 (커밋 2개, 푸시됨)
- [ ] AndroidManifest.xml (권한·서비스 type·LockActivity showWhenLocked)
- [ ] MainActivity(권한 요청+서비스 시작+추가/삭제) / LockActivity(목록+체크) / ScreenService(foreground+screen-on)
- [ ] res/layout, res/values(테마=Theme.AppCompat)
- [ ] 빌드 `./gradlew assembleDebug` → 에뮬 설치 → **G1 검증**(adb logcat·screencap)
- [ ] README 원페이저 + 데모 스크린샷

## 환경
- 에뮬레이터: `Medium_Phone_API_36.1` (Android 16). 기동: `$ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_36.1`
- adb: `/opt/homebrew/bin/adb` · SDK: `~/Library/Android/sdk` (platforms android-36, build-tools 36.x)
- Gradle wrapper 8.14.3, AGP 8.7.3, Kotlin 2.0.21, JDK 17
- 검증 도구는 **adb** (chrome-devtools 아님 — 그건 웹용)

## 규칙
- **커밋 author 에 claude/🤖 미포함**. Co-Authored-By 넣지 않음.
- **구현 과정 전부 커밋** (제출 요건). 단계마다 커밋 → 푸시.
- 외부 산출물(README·커밋 메시지)은 존댓말, 동사형, 수사 금지.
- README 는 루트 원페이저. 데모는 `adb exec-out screencap -p > shot.png`.
- 빌드 에러는 임시 우회로 덮지 말고 근본 해결(버전 핀). 프레임워크 API 추측 금지 — 불확실하면 문서/소스 확인.
