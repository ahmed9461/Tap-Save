# PROGRESS_LOG

## 2026-09-28 — Arabic and persistent personal signing started

- Owner confirmed normal 0.4 use with no observed problems, then requested Arabic and future updates without uninstalling.
- Moved hard-coded app chrome into Android resources; added 112 Arabic strings, all plural forms, RTL and an Android 13+ app-language settings entry. Kept selectors, resolver/storage and branding/folder identity unchanged.
- Created and locally backed up one RSA-3072/40-year PKCS12 identity with restricted filesystem access; uploaded only sealed encrypted secrets through GitHub's repository secrets API. Public fingerprint is checked in; private material is ignored and excluded from artifacts. Local debug key does not match delivered 0.4, whose ephemeral private key was not retained.
- Added explicit non-debuggable signed builds and a disposable-emulator update/data-retention gate. Build, Arabic visual checks and signed update validation are pending; no phone installation or deletion performed.

## 2026-09-27 — Real public Reel saved through the Android Share pipeline

- Code `03a5d1f`: build/strict lint, 16 JVM + 26 instrumentation tests per API 29/35/36 job passed (CI `36277332969`). Separate live CI `36277332176` saved `DGOSAUyC903` through the real API 36 Share activity/service, verified published video/audio and decoded a frame. No resolver/transport fixture override in that live test.
- The first live test exposed a missing ServerJS-wrapper parser path. Added bounded JSON-string decoding without executing JavaScript and updated fixtures from the observed public structure. No heavy dependency or authenticated endpoint added.
- Controlled tests cover progress, blocked-read cancellation, rollback, interrupted input, activity recreation/closure, duplicates and pending-row reconciliation. Reviewed the diff and corrected cleanup assertions to inspect pending-inclusive collections directly.
- Exported the exact live-tested 0.2 debug APK and verified archive/APK checksums; artifact and size are recorded in `TECHNICAL_SPIKE.md`. Updated memory/status/decisions/Plan 0001. Step 5 development-build exit is met; Samsung save/playback and remaining current-Reel/overlay gates are open. Plan remains active.

## 2026-09-27 — Owner acceptance and actual Share pipeline implementation

- Owner confirmed Usage Access, overlay visibility in Instagram and reception of `Dc_WBLAuR7M` on Samsung SM-S908U1 / Android 16. Overlay tap only shows the Share hint; no phone download has passed yet.
- The first sample was unavailable in this environment's logged-out browser/embed. Owner supplied `DGOSAUyC903` and confirmed incognito playback. Its public embed resolved anonymously; desktop transfer yielded 4,074,976 bytes, H.264 720×1280 + AAC, duration 26.665 seconds (SHA-256 `505790fbc134af30f11bc0e1ef723bf342dfe172c3c2bcf89907afb1ea33d63b`). Raw pages/media stay outside the repository.
- Implemented the native resolver/transfer/service/MediaStore/UI pipeline and tests for actual HTTP behavior, failures, cancellation, activity recreation, duplicates and recovery. Added a separately requested live Android test. New build/runtime gates pending; this is not yet owner-device save acceptance.

## 2026-09-27 — Final automated foundation/spike gate passed

- Final code `0e3bc84`, CI `36272869682`: clean debug build, strict lint, 14 JVM tests and 15 instrumentation tests on each of API 29/35/36; zero failures, errors or skips. Includes non-activity overlay attachment, notification Stop/worker termination and all six MediaStore transaction tests.
- Reviewed complete implementation/diff and resolved dependency graph. No downloader/network/scheduler/accessibility dependency was introduced. API 36 debug APK: 29,547,938 bytes; exact artifact/hash recorded in `TECHNICAL_SPIKE.md`.
- Updated project memory/status/decisions and the existing active plan. Documentation-only follow-up does not change the tested application or build inputs.
- Step 1 is complete. Plan 0001 and Phase 0 remain open for Samsung/Instagram acceptance, direct target identification, public resolution and durable transfer jobs. Owner's public Reel URL is the next live input; Share and overlay investigation remain independent.

## 2026-09-27 — Non-activity overlay review

- API 29/35/36 passed all 14 JVM and 15 instrumentation tests per job at `bc9572e` in CI `36272477074`, including corrected MediaStore assertions.
- Critical review found the attachment test supplied an Activity context while the service does not. Use Android's display-bound window context on API 30+ and exercise attachment from a non-activity context; the updated path needs its own final gate.
- Simplified notification setup to system settings for both first grant and recovery after denial. No additional dependency or background mechanism.

## 2026-09-27 — Storage test correction and expanded compatibility gate

- CI `36271302584` passed build, 14 JVM tests and lint on both API 35/36 jobs; each device suite failed only the pending-row visibility assertion. The eight share/window/service tests passed on both.
- Fixed the test query to explicitly include pending rows, per the MediaStore API contract. This is also required to prove cancellation/failure deletes pending rows rather than merely hiding them from the query.
- Added API 29 coverage, notification Stop/worker-termination verification and machine-readable test/APK evidence. Full corrected gate pending.
- Dependency report resolves Kotlin stdlib 2.4.20 and only AndroidX/Kotlin support dependencies; no network/download engine was introduced.

## 2026-09-26 — API 35 integration gate passed

- CI `36270467176` completed successfully at `db844a4`: build, JVM tests, lint and eight API 35 share/window/service instrumentation tests.
- Confirmed owner device: Samsung Galaxy S22 Ultra SM-S908U1 on Android 16. Owner will provide a public Reel URL at the live gate and requested continued independent automation; API 36 coverage added.
- Storage is being tested independently with a generated 0.5-second black/silent MP4 fixture. It cannot establish Reel resolution, network job durability or Samsung gallery acceptance.

## 2026-09-26 — Step 1 foundation gate passed

- CI run `36270467176` passed clean-checkout debug assembly, JVM tests and strict lint at `db844a4`.
- Marked Plan 0001 Step 1 complete and updated status/memory. Emulator overlay/share tests remain in progress, and real Instagram/OEM acceptance is still unverified.
- The post-gate review added a regression for late stop events within the same app; that subsequent change still needs the final gate.

## 2026-09-26 — Share and native overlay prototypes

- Added defensive share normalization, distinct short-link handling, permission-independent receiver and honest development states; no media saving is claimed.
- Added optional native overlay, explicit non-sticky foreground session, Stop controls, persisted/clamped position and usage-event context without accessibility.
- Reviewed lifecycle races: screen-lock/stop invalidates in-flight results; context resets stay on the worker; late stop events from a previous activity cannot erase a newer activity in the same app.
- Added JVM regression coverage and API 35 share/window/service instrumentation tests. Clean APK builds succeeded; full current CI/emulator gate remains pending.
- Recorded one anonymous public-page probe (HTTP 200, no direct media metadata), engine tradeoffs and exact device gates. No resolver/downloader/network permission was added.
- Updated memory, status, decisions and Plan 0001; phase and plan remain open.

## 2026-09-26 — Foundation implementation started

- Read required documents in order; inspected clean `main` at `1ab718e`, remote branches and pull refs (none).
- Created `codex/foundation-instagram-spike`; added a minimal single-module Kotlin/Compose app and Gradle/CI gate.
- Compared native integration options and recorded primary documentation/version sources in `TECHNICAL_SPIKE.md`.
- Windows has no Android SDK/JDK on PATH; large local tool downloads stalled and were stopped. Downloaded wrapper was verified against Gradle's published SHA-256.
- Clean CI gate is pending. No phase is complete; no Reel saving or device acceptance is claimed.

## 2026-09-26 — Repository foundation

- Confirmed repository: `ahmed9461/Tap-Save`.
- Repository was empty before initialization.
- Defined Tap Save as an Android-only personal utility.
- Locked Instagram Reels as the v1 platform.
- Defined the target interaction: browse → tap floating save control → download → continue browsing.
- Made Share → Tap Save a required reliability fallback.
- Chose Kotlin/native Android as the preferred direction pending technical validation.
- Recorded privacy, scope, and access-control boundaries.
- Added roadmap, product specification, decision log, idea parking lot, project status, project memory, agent rules, and initial active plan.
- No application code has been written yet.

## 2026-09-28 — Direct acquisition and reliability implementation (verification in progress)

- Recorded owner confirmation of some real phone saves and failures on other public Reels; Instagram 448.0.0.52.84 Arabic. No waiting for additional failing URLs.
- Added opt-in package-limited semantic Share/Copy acquisition and fresh focused clipboard handoff; same foreground save pipeline and overlay progress/cancel/success. Share remains independent.
- Added optional isolated real Instagram login/session, public-first ordered fallbacks, cookie-origin confinement/clear, short-link normalization, highest progressive variant selection, bounded expired-media refresh and specific safe diagnostics.
- Compared native implementation against publisher wrapper/FFmpeg AAR size and initializer/subprocess maintenance. No production dependency added. Added disposable Arabic/English UI fixture and controlled resolver/session/lifecycle regressions; build/emulator validation is running.
- Independently resolved/downloaded/fully decoded a second public Reel, `Cop84x6u7CP`, on desktop; added it to the Android live gate with `DGOSAUyC903`. Three other anonymous sample documents exposed no downloadable metadata. Actual Instagram UI and owner-account login acceptance remain open.

### Verification checkpoint and native resolver refinement

At `82f650b`, CI `36352369407` passed build/strict lint, 18 JVM and 37 instrumentation tests on API 29/35/36. Arabic/English semantic acquisition, stale-link rejection, ambiguity/other-app rejection, real native transfer/storage after acquisition, optional session broker/clear and resolver regressions passed. This is synthetic UI evidence, not production Instagram acceptance.

A maintained yt-dlp desktop comparison resolved an additional public sample (`CDUMkliABpa`). The minimal native reproduction was an explicit HTML Accept header and public post permalink; no login, API query or TLS impersonation was needed. Added that third public strategy before session use, modern prefetch-format regression, public/session success indication and a third live Android sample. Strengthened the UI gate to press the actual floating button through an accessibility action, then verify the saved bytes; this final source revision is awaiting rerun.

### Final integration gate repair

The stronger overlay test passed on API 35 but exposed timing issues on API 29/36: an accessibility node can become stale during context-driven window reattachment, and rapid test teardown/start with permission changes could leave a foreground-start deadline outstanding. Tests now re-query stale nodes until the tap is accepted, stop/revoke only after worker/notification termination, and wait for the application's real Stop action rather than a temporary system notification. The service itself now fulfils `startForeground` before stopping when a permission changes after the caller's check. A regression deliberately revokes Usage Access before service startup and observes the foreground deadline. These fixes require a new full gate; earlier successes do not cover them.

### Device-log diagnosis of cleanup assertions

Captured API 36 logcat identified the cleanup false positive: Android's automatic notification group summary reused numeric ID 1 with a different tag. Tests selected it as the overlay Stop notification (no actions) and counted it as a surviving overlay after a successful save. Notification assertions now identify the app's untagged ID 1, so they still require the actual overlay notification and worker to disappear. Tests also await accessibility disconnect and the fixture's real visible window instead of fixed launch sleeps; the disposable emulator is held awake. The earlier foreground-start permission-race guard is retained with its regression. No system group notification is cancelled or treated as an app service leak.

## 2026-09-28 — Version 0.3 automated/live milestone verified

- Final application/test source `411ee70d79b48e779b0a06a6a7e03b292eee4d05` passed build/strict lint and 18 JVM + 39 instrumentation tests on each of API 29/35/36, zero failures/errors/skips (CI `36354556280`). This includes actual floating-button activation through Arabic fixture acquisition/HTTP/MediaStore, English acquisition, negative identity cases, foreground-start revocation, resolver/session/cancellation/storage regressions.
- Separate live API 36 CI `36354557958` passed all three public Share saves (`DGOSAUyC903`, `Cop84x6u7CP`, `CDUMkliABpa`) with real resolver/HTTP/storage, video/audio metadata and frame decoding; no logged-in account or transport override. Exact case XML and artifact checksums were inspected.
- Delivered tested 0.3 APK from artifact `10943980658`: 29,709,819 bytes, SHA-256 `155ab82f2376960a11d27d33e87a62d25f9b4788d626f796a8d9131e14524ce3`. Archive hash `6784f4a6a5c702a3208d05afbc0a2fef85dcecd5b8b3e9cba66588c67bb50943`. APK/checksum and six-path phone checklist saved in task outputs. Its debug certificate differs from the previous delivered build; replacement-install consequences are documented.
- Critically reviewed the final adapter/session/resolver boundaries, dependency diff and CI failures. Native HTML negotiation reproduced the useful maintained-engine result without adding a runtime; final APK growth versus 0.2 is about 69.6 KiB. Corrected test readiness and notification identity rather than cancelling system summaries or relaxing lifecycle checks. Final source diff whitespace check passed.
- Updated memory/status/decisions/spike/active plan. Plan 0001 remains active: production Instagram 448.0.0.52.84 Arabic current-Reel identity, owner authentication/session benefit, Samsung quality/gallery and OEM/battery/force-stop behavior need phone acceptance. No additional failing URLs were required to complete this implementation/verification milestone.

## 2026-09-28 — Target-device Copy failure and actual UI repair started

- Owner reproduced `COPY_ACTION_FAILED` with Arabic Copy link visible on S22 Ultra/Android 16/Instagram 448.0.0.52.84. Confirmed ADB access and versions. Standard UIAutomator dump could not become idle on Instagram; built a separate, no-network ADB diagnostic package to inspect control IDs/properties/supported actions without the idle wait or changes to either app's data.
- Replaced the first-clickable-node assumption with semantic scoring, advertised actions, refreshed ancestor dispatch, bounded retries, explicit cleanup and safe per-stage diagnostics. Added delayed/rejected/nested action fixtures and focused-clipboard checks; these are pending verification, not target-device success claims.
- Replaced the actual main control panel with Home, sequential setup and Settings/Advanced; added persisted button size/opacity/reset and a native circular vector/progress control. No production dependency changes. Build, regression and visual/device checks are next.

### Real Instagram Copy action reproduced and isolated

- Installed only the task-created diagnostic package on the connected S22; Tap Save and Instagram data were untouched. The current Arabic share tree contained 224 nodes and two semantic Copy matches: a non-clickable TextView label and a clickable ImageView without an advertised click action.
- The diagnostic at `dfa686c` reproduced `legacy_image_ACTION_CLICK=false`, followed by `supported_parent=2 ACTION_CLICK=true` on the containing LinearLayout. Resource hierarchy: `id/button` → `id/button_container` → tile → `id/direct_external_reshare_row`. No coordinates, raw captions, contact names or clipboard contents were recorded.
- Production dispatch now uses the supported-action chain, bounded by the observed share row. Added a fixture with both the Arabic label and misleading clickable image, preserving the real missing-action condition. Build/strict lint passed the preceding code revision after removing a per-frame RectF allocation; final instrumentation and full phone acquisition/clipboard/save remain in progress.

### Verification follow-up

- At `a67e87b`, API 29 passed all 19 JVM/44 instrumentation cases, but screenshot collection after AGP uninstalled the app failed. API 35/36 exposed an offscreen first-run Share option at 320 dp; pinned it below the scroll area. Screenshots now use AGP's additional test output collector before uninstall, including the actual overlay's visual states.
- The new missing-action/ancestor/readiness/cleanup/focused-clipboard regressions passed across these jobs. One API 36 cleanup failed after the cold emulator's initial MediaProvider volume scan removed a newly published row; logs show the concurrent scan and uniqueness conflict. The disposable runner now warms MediaProvider and waits for its initial scanner to finish; publication/read/deletion assertions remain strict.
- The owner disconnected the phone for personal use. Full 0.4 S22 clipboard/acquisition/save and Samsung visual acceptance remain pending; no replacement installation or session reset was performed.
- The first scanner-query readiness guard did not terminate on the emulator images and prevented instrumentation from starting (`36418902734`); replaced it with the provider's AOSP test-only `wait_for_idle` shell call. This setup is confined to disposable CI devices, not the production app.
- Source `3e5f0e5` passed build/lint, 19 JVM and 44 instrumentation tests on API 29/35/36 (`36419875027`); the separate live run `36419880160` also passed. Visual inspection caught dark system-bar icons against the dark app and stale display captures taken before a new frame. Set explicit dark-system-bar styles and changed test captures to redraw-aware Compose/PixelCopy APIs, including the real native overlay window. Final visual/source gates follow this small refinement.

## 2026-09-28 — Version 0.4 final build and visual gate

- Source `e8fbf5888c4a8414462a514b1a457561aaf9b499` passed assembly/strict lint, 19 JVM + 44 instrumentation tests on each API 29/35/36, zero failures/errors/skips (CI `36422892820`). Reviewed actual Home/setup/Settings and native floating idle/busy/progress/success/error captures after redraw; no production dependency added.
- Real S22 Copy failure was reproduced: decorative ImageView returned false for ACTION_CLICK; supported containing tile returned true. Production dispatch now follows supported actions within the observed share row. Regression tests exercise rejected/nested controls, readiness, cleanup and the actual overlay through MediaStore. This proves the fix mechanism; the owner disconnected before full 0.4 phone testing.
- Live CI `36419880160` at `3e5f0e5` passed three anonymous public Share saves (`DGOSAUyC903`, `Cop84x6u7CP`, `CDUMkliABpa`) with audio/video metadata and frame decoding. Later source changes affect system bars/capture/CI only; acquisition/resolver/transfer/storage code is unchanged.
- Delivered 0.4 APK from final API 36 artifact `10970244215`: 29,855,521 bytes, SHA-256 `9b7ebc57a5c9e4caec72a5a24d2f684ba97a52ba82c3f3fd8ccbd249b1c4eafc`, with checksum, verification metadata and Arabic device checklist. Archive integrity and signer checked. CI debug key differs from the installed phone build; replacement consequences documented, no owner installation/session reset performed.
- Updated memory/status/decisions/spike and active plan. Full Samsung focused handoff/current-Reel save, account session, quality, OEM lifecycle and battery remain open. Temporary standalone diagnostic package cleanup waits until the phone is available. Plan 0001 remains active.
