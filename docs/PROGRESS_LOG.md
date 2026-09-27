# PROGRESS_LOG

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
