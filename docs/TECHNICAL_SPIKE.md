# Foundation spike evidence

Plan: `plans/0001-foundation-and-instagram-spike.md`. Observations dated 2026-09-26.
This is an evidence notebook, not a replacement plan. Device acceptance remains open.

## Compare before committing

| Concern | Smallest useful experiment | Alternatives and tradeoffs | Current boundary |
| --- | --- | --- | --- |
| Floating control | `WindowManager.TYPE_APPLICATION_OVERLAY`, one native View, explicit user-started session | Compose is useful for the activity but adds lifecycle owners to a service-hosted window. Bubbles are conversation-oriented; PiP needs an activity and is not a general overlay. | Native View candidate; permission denial must leave Share usable. |
| Foreground context | `UsageStatsManager.queryEvents` with user-granted usage access, only during an enabled overlay session | No general cross-app foreground callback for ordinary apps. Usage events require polling and can be delayed; accessibility events expose substantially more sensitive content. | Optional usage-access experiment; unknown/locked context hides control. No browsing history persistence. |
| Share fallback | Exported `ACTION_SEND` / `text/plain` activity; validate `EXTRA_TEXT` locally | Clipboard needs focus on modern Android and is neither a reliable live target nor a background channel. Deep links receive explicit URLs but cannot reveal another app's current screen. | Establish explicit shared targets first; never reuse the last share as the current Reel. |
| Current Reel | Compare explicit share URL against package-only usage events | Overlay permission provides a window, not another app's URL. MediaSession metadata is app-dependent. Screen capture/OCR adds consent, latency and privacy exposure without guaranteeing a permalink. Accessibility may inspect visible content, but UI/IDs change. | Direct one-tap identity is unproven. No accessibility service, screen capture, private APIs or authentication workarounds. |
| Download lifecycle | First measure a user-requested public file transfer; compare platform DownloadManager with a short `dataSync` FGS | DownloadManager owns retries/notifications but cannot resolve a Reel page or mux tracks. A foreground service allows direct progress/cancel/MediaStore but needs explicit start conditions and cleanup. UIDT jobs (API 34+) fit long user transfers but add a second path below 34. WorkManager suits deferrable durable work; long-running workers add scheduling/FGS constraints. | No scheduler/engine dependency until a viable resolver and actual transfer semantics are established. |
| Overlay lifetime | Explicit session, ongoing Stop notification, `START_NOT_STICKY`; evaluate `specialUse` FGS | A bound/activity-only service does not survive leaving Tap Save; `dataSync` is not an honest type for an idle overlay. No boot start or blanket battery exemption. | Session-only FGS is an experiment, not a claim of OEM reliability. |
| Storage | `MediaStore.Video`, `RELATIVE_PATH=Movies/Tap Save/`, `IS_PENDING=1` until verified completion | SAF adds a picker; legacy filesystem permissions add complexity; app-private storage is not a gallery result. | API 29 minimum avoids legacy storage branches. Delete pending rows on cancel/failure; reconcile interrupted rows before claiming durable storage. |
| Resolver | Isolated Instagram adapter, bounded anonymous HTTPS against public pages only | A small public-metadata parser has little APK cost but may fail at login/challenge or absent media URLs. Embedded yt-dlp adds a Python/native runtime, extractor updates and possibly FFmpeg/muxing; its feature surface exceeds this app's allowed scope. | Compare with actual public-Reel evidence before adding either. Never retry a login/challenge using credentials or private endpoints. |

Native API documentation establishes capabilities/constraints, not Instagram/OEM acceptance. A real device with Instagram is required for the overlay and actual share flow. Emulator fixtures cannot prove those gates.

## Primary references

- [Overlay window type](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#TYPE_APPLICATION_OVERLAY)
- [Non-activity window context](https://developer.android.com/reference/android/content/Context#createWindowContext(int,android.os.Bundle))
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Receiving shares](https://developer.android.com/develop/ui/compose/sharing/receive)
- [Clipboard restrictions](https://developer.android.com/about/versions/10/privacy/changes#clipboard-data)
- [Foreground service types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Foreground starts and visible-overlay exemption](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [Transfer task comparison](https://developer.android.com/develop/background-work/background-tasks/data-transfer-options)
- [User-initiated data transfer jobs](https://developer.android.com/develop/background-work/background-tasks/uidt)
- [MediaStore ownership and pending writes](https://developer.android.com/training/data-storage/shared/media)
- [Explicit inclusion of pending media in queries](https://developer.android.com/reference/android/provider/MediaStore#QUERY_ARG_MATCH_PENDING)

## Toolchain selection

Verified against primary release documentation and live publisher metadata, rather than inferred version numbers:

- AGP **9.4.1**: stable artifact in Google Maven; [9.4 compatibility](https://developer.android.com/build/releases/agp-9-4-0-release-notes).
- Gradle **9.6.0**: AGP 9.4's documented default/minimum, within Kotlin's supported Gradle range. The live Gradle endpoint reports 9.8.0; avoid that newer, not fully supported Kotlin combination for this spike.
- Kotlin/Compose compiler **2.4.20**: [Kotlin release history](https://kotlinlang.org/docs/releases.html), [compatibility matrix](https://kotlinlang.org/docs/gradle-configure-project.html). Use AGP built-in Kotlin, not the legacy Android Kotlin plugin.
- Compose BOM **2026.09.00**, Activity Compose **1.13.0**: [BOM](https://developer.android.com/develop/ui/compose/bom), [Activity releases](https://developer.android.com/jetpack/androidx/releases/activity), Google Maven stable metadata.
- SDK compile/target **37** (Android 17); build tools **36.0.0** per AGP default; minimum **29** (Android 10) for scoped MediaStore.
- Build JVM: maintained Temurin **21 LTS**; app bytecode target **17**. JDK/Gradle archives are checksum-verified before execution.

Application ID and namespace: `io.github.ahmed9461.tapsave`, based on the repository owner rather than an unowned domain. One `app` module; no DI/navigation/database/network/downloader library at foundation.

Test-only dependencies: JUnit 4.13.2 ([publisher](https://junit.org/junit4/)); AndroidX Test runner 1.7.0 and ext JUnit 1.3.0 (stable Google Maven metadata); Compose UI test version managed by the same BOM. They are not release dependencies.

Core KTX 1.19.1 is explicitly declared for URI/preferences helpers already present transitively through Activity. The stable version was checked in Google's `androidx/core/core-ktx/maven-metadata.xml`. Lint keeps warnings fatal; only the wrapper update suggestion is narrowly excluded because Gradle 9.6.0 is the documented compatible choice. Runtime dependency graph is captured in CI artifacts.

## Public media resolution probe

One anonymous, bounded HTTPS GET to the public Reel URL from the [yt-dlp extractor's Reel test](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/instagram.py) returned HTTP 200, 706,607 bytes and a generic Instagram title. No `og:video`, `video_url`, `playable_url` or `video_versions` fields were present. No cookies, credentials, private endpoints or challenge retries were used. The response is not proof of a login restriction, nor proof that all public Reels fail; it is evidence that a basic public-page metadata parser is insufficient for this sample in this environment. Raw page/header data is not committed.

The [Android yt-dlp wrapper](https://github.com/yausername/youtubedl-android) documents a bundled Python/extractor runtime, ABI-specific packaging, process cancellation/progress callbacks, optional FFmpeg and runtime extractor updates. Its sample storage guidance includes legacy pathways. This needs measured APK/startup costs and a constrained no-auth extraction audit before adoption; neither measurement nor device compatibility was established here. No engine was added. Next experiment: an owner-accessible public Reel on the actual device, public page/redirect path only, stopping at authentication/challenge; then evaluate an isolated public-only extractor if necessary. Resolver and download/storage integration remain open.

## Implemented experiment boundaries

- Share receiver handles `ACTION_SEND` / `text/plain`, bounded input, canonical Reel URLs, distinct redirect tokens and usable invalid/ambiguous states. The development screen explicitly says saving is unavailable. No URL/history is written to disk.
- Overlay grants are independent of Share. A native 56dp window can be dragged; position is saved and clamped on attachment. Tap currently reminds the user to Share. Start/Stop are explicit; no automatic restart.
- Usage events retain only the current package candidate. No high-frequency render loop or screen-off wakeups are scheduled. Actual CPU/battery, delayed events, split-screen and OEM behavior are unmeasured.
- There is deliberately no `INTERNET` permission, resolver, transfer scheduler or fake save progress in this build. Those require a proven supported Reel path first.
- An isolated MediaStore writer is now available to instrumentation, independent of the absent resolver/job layer. It creates a pending row, copies off the UI thread, closes both streams, verifies size/container metadata, and only then publishes; exceptions/cancellation delete its allocated row. Tests use generated media, including truncated input, simulated connection loss, source-close failure and filename collisions. Process-death reconciliation and job deduplication remain unimplemented.

## Device acceptance still required

Foundation milestone: clean-checkout `assembleDebug`, `testDebugUnitTest` and `lintDebug` passed on `db844a4` in [CI run 36270467176](https://github.com/ahmed9461/Tap-Save/actions/runs/36270467176). This is compile/JVM/lint evidence; emulator and device acceptance are separate.

The same run also passed all eight API 35 instrumentation tests: exported share resolution, valid/invalid intent intake, recreation, malformed extras, denied overlay prerequisites, idempotent native-window attachment, session Stop and usage-access revocation. This does not exercise Instagram. The owner's device is a Samsung Galaxy S22 Ultra SM-S908U1 on Android 16; API 36 emulator coverage is being added, with Samsung/Instagram behavior still reserved for live validation.

1. Record device/API, OEM and Instagram version without account identifiers. Deny each optional permission and confirm Share still launches.
2. Grant through Settings, start a session, enter/leave Instagram, open its share sheet, Home and Recents; verify hide/show delays. Repeat in split-screen.
3. Drag to each edge, rotate, stop/restart and confirm saved position remains reachable. Lock/unlock; inspect that sampling stops while locked and resumes without a stale window.
4. Revoke overlay/usage permissions while attached, deny notifications, force-stop the app and stop from the notification. Confirm no orphan window/service and an understandable recovery route.
5. Share a real public Reel directly from Instagram, including any `/share/reel/` form. Compare the displayed canonical URL against the actual intended Reel. ADB/test-fixture shares cannot establish this gate.
6. Resolve/save only permitted public media, verify audio/quality, Samsung gallery visibility, pending-file cleanup, cancellation, duplicates and interrupted transfers. Synthetic MediaStore evidence cannot satisfy this end-to-end saving gate.
