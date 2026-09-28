# Development build

Use JDK 21 and the Android SDK. Set `JAVA_HOME` and `ANDROID_HOME` (or an untracked `local.properties` with `sdk.dir`). Install SDK platform `platforms;android-37.0` and build tools `36.0.0` using SDK Manager. The checked-in wrapper pins/checksums Gradle; do not install a separate Gradle version.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

On Windows use `gradlew.bat`. The debug APK is `app/build/outputs/apk/debug/app-debug.apk`. CI performs the same gate in a fresh checkout, runs instrumentation on API 29, 35 and 36 emulators, and retains APK/reports/dependency graph for seven days. `scripts/summarize_checks.py` reports actual XML test counts and APK checksum/size; it rejects empty, skipped or failed suites.

With a disposable emulator connected, run `python3 scripts/run_device_tests.py` (the CI runner assumes a Unix wrapper). It builds/installs a separate native fixture using `com.instagram.android` solely to test semantic accessibility controls; it refuses non-emulators and any pre-existing Instagram package. The overlay tests change overlay/usage/notification permissions for Tap Save only. Run these on an emulator; follow the separate manual acceptance checklist on an owner device. The fixture is clearly labelled TEST FIXTURE and is not the real Instagram app. Never install it on an owner device.

The 0.3 development build saves supported public Reel URLs through a public-first native resolver and MediaStore, with optional isolated Instagram login and a separate opt-in current-Reel adapter. See `TECHNICAL_SPIKE.md` for the exact samples and verification boundaries; a successful APK build alone does not prove Instagram compatibility.

For a separate, explicitly requested live test, dispatch the Android workflow on the working branch with `live_reel` set to a canonical public URL (`https://www.instagram.com/reel/<shortcode>/`). The API 36 job first runs the deterministic suite, then invokes three `LiveReelSaveTest` samples (the supplied URL plus `Cop84x6u7CP` and `CDUMkliABpa`) through the actual Share activity/service with no fixture or resolver override. It verifies published video/audio metadata and frame decoding, then deletes only its test output. Live XML reports are kept separately from deterministic counts. Login/challenge/private/unavailable responses fail visibly; the test does not authenticate or retry around restrictions.

Equivalent manual test on a disposable emulator:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=io.github.ahmed9461.tapsave.LiveReelSaveTest \
  -Pandroid.testInstrumentationRunnerArguments.liveReel=https://www.instagram.com/reel/<shortcode>/
```

CI debug signing keys are ephemeral and reserved for disposable tests. Personal deliveries use the persistent release identity and update gate in [SIGNING.md](SIGNING.md). Change only `app/version.properties` for the next release version; never generate a fresh signing key for a routine update.

Arabic and English are Android resources; Arabic device locales use RTL automatically on all supported APIs. Android 13+ also exposes the app-specific language selector through Settings. The selector lists only the two supported languages. No locale library or custom font is bundled. The localization tests cover Arabic resources/plurals and actual Arabic screens via the platform locale API.
