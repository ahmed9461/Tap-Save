# Development build

Use JDK 21 and the Android SDK. Set `JAVA_HOME` and `ANDROID_HOME` (or an untracked `local.properties` with `sdk.dir`). Install SDK platform `platforms;android-37.0` and build tools `36.0.0` using SDK Manager. The checked-in wrapper pins/checksums Gradle; do not install a separate Gradle version.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows use `gradlew.bat`. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. CI performs the same gate in a fresh checkout, runs instrumentation on API 29, 35 and 36 emulators, and retains APK/reports/dependency graph for seven days. `scripts/summarize_checks.py` reports actual XML test counts and APK checksum/size; it rejects empty, skipped or failed suites.

With a disposable emulator connected, run `./gradlew :app:connectedDebugAndroidTest`. The overlay tests change overlay/usage/notification permissions for Tap Save only. Run these on an emulator; follow the separate manual acceptance checklist on an owner device. Emulator fixtures do not install or exercise Instagram.

The development build does not yet download Reels. See `TECHNICAL_SPIKE.md` and the active plan for evidence and outstanding device gates. Do not infer Instagram compatibility from a successful APK build.
