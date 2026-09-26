# Development build

Use JDK 21 and the Android SDK. Set `JAVA_HOME` and `ANDROID_HOME` (or an untracked `local.properties` with `sdk.dir`). Install SDK platform `platforms;android-37.0` and build tools `36.0.0` using SDK Manager. The checked-in wrapper pins/checksums Gradle; do not install a separate Gradle version.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows use `gradlew.bat`. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. CI performs the same gate in a fresh checkout and retains APK/reports for seven days. At foundation the test task may report NO-SOURCE; that is not a passing integration test.

The development build does not yet download Reels. See `TECHNICAL_SPIKE.md` and the active plan for evidence and outstanding device gates. Do not infer Instagram compatibility from a successful APK build.
