# Foundation spike evidence

Plan: `plans/0001-foundation-and-instagram-spike.md`. Observations dated 2026-09-26/27.
This is an evidence notebook, not a replacement plan. Device acceptance remains open.

## Compare before committing

| Concern | Smallest useful experiment | Alternatives and tradeoffs | Current boundary |
| --- | --- | --- | --- |
| Floating control | `WindowManager.TYPE_APPLICATION_OVERLAY`, one native View, explicit user-started session | Compose is useful for the activity but adds lifecycle owners to a service-hosted window. Bubbles are conversation-oriented; PiP needs an activity and is not a general overlay. | Native View candidate; permission denial must leave Share usable. |
| Foreground context | `UsageStatsManager.queryEvents` with user-granted usage access, only during an enabled overlay session | No general cross-app foreground callback for ordinary apps. Usage events require polling and can be delayed; accessibility events expose substantially more sensitive content. | Optional usage-access experiment; unknown/locked context hides control. No browsing history persistence. |
| Share fallback | Exported `ACTION_SEND` / `text/plain` activity; validate `EXTRA_TEXT` locally | Clipboard needs focus on modern Android and is neither a reliable live target nor a background channel. Deep links receive explicit URLs but cannot reveal another app's current screen. | Establish explicit shared targets first; never reuse the last share as the current Reel. |
| Current Reel | Compare explicit share URL against package-only usage events | Overlay permission provides a window, not another app's URL. MediaSession metadata is app-dependent. Screen capture/OCR adds consent, latency and privacy exposure without guaranteeing a permalink. Accessibility may inspect visible content, but UI/IDs change. | Direct one-tap identity is unproven. No accessibility service, screen capture, private APIs or authentication workarounds. |
| Download lifecycle | One immediate user-requested public file transfer in a short `dataSync` FGS | DownloadManager owns retries/notifications but still needs a resolver and separate MediaStore validation/publication. UIDT adds a second path below API 34. WorkManager adds scheduling for an immediate, bounded request. | Native single transfer selected for the spike (D-014); no scheduler/engine dependency. |
| Overlay lifetime | Explicit session, ongoing Stop notification, `START_NOT_STICKY`; evaluate `specialUse` FGS | A bound/activity-only service does not survive leaving Tap Save; `dataSync` is not an honest type for an idle overlay. No boot start or blanket battery exemption. | Session-only FGS is an experiment, not a claim of OEM reliability. |
| Storage | `MediaStore.Video`, `RELATIVE_PATH=Movies/Tap Save/`, `IS_PENDING=1` until verified completion | SAF adds a picker; legacy filesystem permissions add complexity; app-private storage is not a gallery result. | API 29 minimum avoids legacy storage branches. Delete pending rows on cancel/failure; reconcile interrupted rows before claiming durable storage. |
| Resolver | Isolated Instagram adapter, bounded anonymous HTTPS against public Reel pages/embeds | A small public-metadata parser has little APK cost but can fail at login/challenge or absent media URLs. Embedded yt-dlp adds a Python/native runtime, extractor updates and possibly FFmpeg/muxing. WebView adds a renderer and cookie lifecycle. | Native parser selected from the public embed experiment below. Never retry restrictions using credentials or private endpoints. |

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
- Build JVM: maintained Temurin **21 LTS**; app bytecode target **17**. The checked-in wrapper JAR and Gradle distribution checksum were verified against Gradle's publisher records; CI installs the JDK through the pinned setup action.

Application ID and namespace: `io.github.ahmed9461.tapsave`, based on the repository owner rather than an unowned domain. One `app` module; no DI/navigation/database/network/downloader library at foundation.

Test-only dependencies: JUnit 4.13.2 ([publisher](https://junit.org/junit4/)); AndroidX Test runner 1.7.0 and ext JUnit 1.3.0 (stable Google Maven metadata); Compose UI test version managed by the same BOM. They are not release dependencies.

Core KTX 1.19.1 is explicitly declared for URI/preferences helpers already present transitively through Activity. The stable version was checked in Google's `androidx/core/core-ktx/maven-metadata.xml`. Lint keeps warnings fatal; only the wrapper update suggestion is narrowly excluded because Gradle 9.6.0 is the documented compatible choice. Runtime dependency graph is captured in CI artifacts.

## Public media resolution probe

One anonymous, bounded HTTPS GET to the public Reel URL from the [yt-dlp extractor's Reel test](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/instagram.py) returned HTTP 200, 706,607 bytes and a generic Instagram title. No `og:video`, `video_url`, `playable_url` or `video_versions` fields were present. No cookies, credentials, private endpoints or challenge retries were used. The response is not proof of a login restriction, nor proof that all public Reels fail; it is evidence that a basic public-page metadata parser is insufficient for this sample in this environment. Raw page/header data is not committed.

The [Android yt-dlp wrapper](https://github.com/yausername/youtubedl-android) documents a bundled Python/extractor runtime, ABI-specific packaging, process cancellation/progress callbacks, optional FFmpeg and runtime extractor updates. Its sample storage guidance includes legacy pathways. No engine was added: the subsequent public embed experiment supports a native progressive-MP4 path without runtime/muxing dependencies.

### 2026-09-27 — Owner sample and public embed

- Owner verified Share reception of `Dc_WBLAuR7M`, Usage Access and overlay visibility on Samsung SM-S908U1 / Android 16. This original Reel was unavailable in the development environment's logged-out browser and embed; this is environment-specific, not a claim that it is universally unavailable.
- Owner supplied `DGOSAUyC903` after confirming incognito playback without login. Its public embed returned matching `contextJSON` / `gql_data.shortcode_media`, `copyright_blocked=false` and a progressive `video_url`. The same request succeeded with the application's exact User-Agent and without cookies. Tracking parameters were discarded.
- Anonymous desktop transfer produced **4,074,976 bytes**, SHA-256 `505790fbc134af30f11bc0e1ef723bf342dfe172c3c2bcf89907afb1ea33d63b`. FFprobe verified **H.264 720×1280, AAC, 26.665375 seconds**; a full FFmpeg decode completed without errors. Metadata advertised a larger size, so claims here use the actual file dimensions.
- A different reference sample exposed `copyright_blocked=true`; probing stopped. No authenticated/private endpoint, cookie import, TLS impersonation, third-party downloader or restriction workaround was used. Raw pages and expiring CDN URLs stay out of the repository.
- Desktop evidence proves a resolver candidate. The separately dispatched Android live gate tests the actual native parser, HTTPS implementation, Share service and MediaStore; Samsung gallery acceptance remains a distinct owner gate.

## Implemented experiment boundaries

- Share receiver handles `ACTION_SEND` / `text/plain`, bounded input, canonical Reel URLs, distinct redirect tokens and usable invalid/ambiguous states. A valid canonical Reel starts saving while the activity is visible; progress, Cancel, retry, completion and Open video are available. Short share-token resolution remains open.
- Overlay grants are independent of Share. A native 56dp window can be dragged; position is saved and clamped on attachment. Tap currently reminds the user to Share. Start/Stop are explicit; no automatic restart.
- Usage events retain only the current package candidate. No high-frequency render loop or screen-off wakeups are scheduled. Actual CPU/battery, delayed events, split-screen and OEM behavior are unmeasured.
- Native HTTPS is restricted to validated public Instagram document paths and media CDN host boundaries, including every redirect. Documents are bounded to 3 MiB and transfers to 512 MiB. Private/copyright-blocked/unavailable content fails without an alternate authenticated route.
- One user-started, non-sticky `dataSync` service owns a worker, throttled progress and cancellation. Activity recreation/leaving does not restart or cancel it; Cancel is explicit in the activity/notification. No queue, automatic retries/resume, wake lock or boot work. A ten-minute deadline bounds each request.
- The MediaStore writer creates a pending row under `Movies/Tap Save/`, copies off the UI thread, closes both streams, verifies size/video and expected audio metadata, then publishes. Failure/cancellation rolls back its own row. The latest job checkpoint stores only canonical target/status/output URI; expiring CDN URLs are never persisted. Before an explicit retry, exact-target app-owned pending rows are reconciled and already-published outputs prevent repeat downloads. Process death does not silently restart work.

## Automated verification

Foundation milestone: clean-checkout `assembleDebug`, `testDebugUnitTest` and `lintDebug` passed on `db844a4` in [CI run 36270467176](https://github.com/ahmed9461/Tap-Save/actions/runs/36270467176). This is compile/JVM/lint evidence; emulator and device acceptance are separate.

The final code gate [CI run 36272869682](https://github.com/ahmed9461/Tap-Save/actions/runs/36272869682) passed at `0e3bc84051cbf28c7203d9cdb4c6140405c19559` on API 29/35/36: build, strict lint, 14 JVM tests and 15 instrumentation tests per job, with zero failures/errors/skips. This includes the non-activity window-context refinement.

- Share: manifest resolution, actual valid/invalid intent intake, activity recreation and malformed extras.
- Overlay: denied prerequisites, native-window attachment/removal from a non-activity context, explicit session Stop, usage-access revocation and notification Stop with worker termination.
- Storage: exact synthetic video/audio bytes, pending-to-published transition, cancellation/retry, interrupted/truncated input, non-video rejection, source-close failure and non-overwriting filename collisions.
- Dependency graph: Kotlin stdlib resolves to 2.4.20. AndroidX/Compose support dependencies remain; no embedded engine, database, HTTP library or scheduler dependency was added. The unminified debug APK is about 28.2 MiB; this is not a release-size measurement.

An earlier storage test queried only non-pending media. Explicit pending inclusion corrected both the visibility assertion and the cleanup oracle, which would otherwise miss leaked incomplete rows. XML counts, APK checksum/size and the runtime dependency graph are retained in each CI artifact. Local Windows tool downloads stalled; build/emulator evidence comes from GitHub Actions, not a local Android SDK.

API 36 artifact: `android-api-36-debug-and-reports`, artifact ID `10916063553`. Its debug APK is 29,547,938 bytes, SHA-256 `6431018b6a54390f8a96bfadad45b7a9547ca2a2e5cd43d76e5956ae9ed8ed73`. Artifacts expire after seven days; source and wrapper remain reproducible inputs. Debug signing varies between CI jobs, so these hashes identify one exact build rather than a release signing identity.

## Device acceptance still required

The owner's device is a Samsung Galaxy S22 Ultra SM-S908U1 on Android 16. The emulator suite does not exercise Instagram or Samsung-specific behavior. Keep these sessions independent: Share needs no optional permissions; the overlay/current-target investigation must not reuse a previous Share target.

1. Record device/API, OEM and Instagram version without account identifiers. Deny each optional permission and confirm Share still launches.
2. Grant through Settings, start a session, enter/leave Instagram, open its share sheet, Home and Recents; verify hide/show delays. Repeat in split-screen.
3. Drag to each edge, rotate, stop/restart and confirm saved position remains reachable. Lock/unlock; inspect that sampling stops while locked and resumes without a stale window.
4. Revoke overlay/usage permissions while attached, deny notifications, force-stop the app and stop from the notification. Confirm no orphan window/service and an understandable recovery route.
5. Share a real public Reel directly from Instagram, including any `/share/reel/` form. Compare the displayed canonical URL against the actual intended Reel. ADB/test-fixture shares cannot establish this gate.
   The owner will supply the unauthenticated public URL at this gate. Direct-current-Reel identity is still absent: separately assess what the least invasive APIs expose while switching Reels; do not infer success from an overlay appearing or introduce accessibility without evidence.
6. Resolve/save only permitted public media, verify audio/quality, Samsung gallery visibility, pending-file cleanup, cancellation, duplicates and interrupted transfers. Synthetic MediaStore evidence cannot satisfy this end-to-end saving gate.
