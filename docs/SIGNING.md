# Personal APK signing

All distributed builds from 0.5 use one persistent RSA-3072 signing identity, valid for 40 years. Keep application ID `io.github.ahmed9461.tapsave` and this identity for future updates. Increment `versionCode` and set `versionName` in `app/version.properties` before each delivery. Do not distribute CI debug APKs as updates to the personal release.

The public certificate and SHA-256 fingerprint are checked in under `signing/`. The private PKCS12 keystore and password are excluded from Git. The owner-local copy is `%USERPROFILE%\.android\Tap-Save-signing`; an owner backup is in this task's `outputs/Tap-Save-signing-private`, both restricted to the owner and SYSTEM. Keep a private backup of that folder outside this computer. Neither the private key nor its password belongs in an issue, log or public artifact.

GitHub repository Actions secrets contain `TAP_SAVE_KEYSTORE_BASE64` and `TAP_SAVE_KEYSTORE_PASSWORD`. They were encrypted using GitHub's repository public key before upload. The manual **Personal signed release** workflow loads the existing identity into the runner's temporary directory, checks the resulting APK against the checked-in public fingerprint, tests an update, then deletes temporary signing material. It never generates a new key. Signing secrets are not used by pull-request builds. APK artifacts contain no key/password.

The release build fails without explicit signing configuration; it cannot silently use an ephemeral debug certificate. For a local build with JDK 21 and the documented SDK:

```powershell
$signingDir = Join-Path $env:USERPROFILE '.android\Tap-Save-signing'
$env:TAP_SAVE_KEYSTORE_PATH = Join-Path $signingDir 'tap-save-release.p12'
$env:TAP_SAVE_KEYSTORE_PASSWORD = [IO.File]::ReadAllText((Join-Path $signingDir 'keystore-password.txt'))
try { .\gradlew.bat :app:assembleRelease :app:lintRelease }
finally { Remove-Item Env:TAP_SAVE_KEYSTORE_PASSWORD; Remove-Item Env:TAP_SAVE_KEYSTORE_PATH }
```

The release workflow builds a same-source baseline with the preceding version code and a candidate with the current code. A separately signed test APK seeds settings and an app-owned MediaStore video, then verifies that `adb install -r` retains both in the non-debuggable candidate. This proves future same-key updates; it does not claim migration from an unrelated debug certificate. These tests refuse a physical device or a pre-existing installation.

## One-time move from 0.4

The delivered 0.4 certificate is `e6ebd81995f82aec447d9f16c29c92486e55107d3c6a866c03a238e4aadc3b4f`. Its private key came from an ephemeral CI runner and was not retained. The existing local debug keystore has a different certificate; an APK contains no recoverable private signing key. A new key cannot update that installation or create a valid old-key rotation proof. Moving from 0.4 to this stable identity requires a one-time replacement installation, which resets app settings and the optional local session. No owner app removal/reset is automated. Subsequent releases signed with the retained identity install as updates without that reset.

Sources: [Android signing and update identity](https://developer.android.com/studio/publish/app-signing), [apksigner](https://developer.android.com/tools/apksigner), [GitHub encrypted secrets](https://docs.github.com/en/rest/actions/secrets#create-or-update-a-repository-secret).
