# DECISIONS

Durable project decisions are recorded here. Change an existing decision only when evidence justifies it; append a replacement decision rather than silently rewriting history.

## D-001 — Android-first, Android-only initial product

**Status:** Accepted  
**Reason:** The desired overlay/background integration is Android-specific and the app is for personal use. Supporting more platforms would add cost without helping the initial goal.

## D-002 — Kotlin native is the preferred implementation

**Status:** Accepted, subject to technical spike  
**Reason:** The project depends heavily on Android-native permissions, overlays, services, share intents, lifecycle behavior, storage, and potentially accessibility. Kotlin keeps those paths direct and reduces framework overhead.

## D-003 — Instagram Reels only for v1

**Status:** Accepted  
**Reason:** Stabilize the primary experience before adding more adapters.

## D-004 — Share receiver is mandatory

**Status:** Accepted  
**Reason:** Direct current-reel identification can break when Instagram changes. Android sharing provides a simpler recovery path and prevents the entire product from depending on a brittle technique.

## D-005 — Direct current-Reel detection must be replaceable

**Status:** Accepted  
**Reason:** Instagram implementation details can change. Detection must sit behind an interface/adapter rather than leaking into the rest of the app.

## D-006 — AccessibilityService is not the default architecture

**Status:** Accepted  
**Reason:** It is intrusive, sensitive, and potentially fragile. It may be tested for this private-use app only if less invasive mechanisms cannot achieve the desired one-tap flow. Any use must be narrowly scoped and opt-in.

## D-007 — No initial backend

**Status:** Accepted  
**Reason:** The product should stay private, fast, cheap, and simple. On-device functionality is preferred unless technical validation proves a backend is required.

## D-008 — No bypass behavior

**Status:** Accepted  
**Reason:** Tap Save will not be designed to circumvent DRM, private-account permissions, authentication barriers, paywalls, or other access controls.

## D-009 — Dependency restraint

**Status:** Accepted  
**Reason:** A personal background utility benefits from small size and low maintenance. Large engines such as yt-dlp wrappers must earn their inclusion through the technical spike rather than being added by default.

## D-010 — One native application module and scoped-storage baseline

**Status:** Accepted for the foundation
**Decision:** Kotlin/Compose activities in one `app` module; `io.github.ahmed9461.tapsave` application ID and namespace. Minimum API 29 removes legacy shared-storage permission branches; compile/target API 37. Revisit the minimum if the owner's device requires it. Version pins, compatibility rationale and publisher links live in `TECHNICAL_SPIKE.md`.

## D-011 — Explicit share identity before automatic current-Reel identity

**Status:** Accepted
**Decision:** Normalize explicit HTTPS Reel links behind `SharedTargetParser`. Reject ambiguous/unsupported input and strip tracking. A short share token needs redirect resolution and must never become a Reel ID by assumption. No previous share, clipboard contents or usage event may silently stand in for the current Reel.
**Evidence:** Android sharing exposes caller-provided text; overlay/usage APIs do not expose another app's current content URL. Owner confirmed actual Instagram Share reception on the S22 Ultra / Android 16.

## D-012 — User-started overlay and optional usage context experiment

**Status:** Experimental; production choice deferred until device evidence
**Decision:** One native `TYPE_APPLICATION_OVERLAY` window, hosted by a non-sticky `specialUse` foreground session with Stop notification/action. Usage access is optional and isolated; sampling is limited to an unlocked, explicitly started session, at 1.5-second intervals on a worker thread. Screen lock/stop invalidates pending results. No accessibility dependency, boot start or wake lock.
**Tradeoff:** Usage events can lag and multi-window/OEM behavior is uncertain. The control currently directs the user to Share; it cannot identify a Reel. Notifications must be enabled for this experiment so Stop remains discoverable. Share downloads use the independent lifecycle in D-014.
**Review refinement:** API 30+ uses a display-bound window context for non-activity overlay resources/metrics. Attachment tests start from a non-activity context. Notification setup opens system settings on all supported APIs, including after a prior denial.

## D-013 — Validate storage separately from Instagram resolution

**Status:** Accepted for storage; D-014 covers job integration
**Decision:** Use MediaStore pending rows in `Movies/Tap Save/` without broad media/storage permissions. A blocking writer runs off the UI thread, verifies copy size when known and video metadata, closes both streams before publication, and deletes its own allocated row on cancellation/failure. No queue/FGS/download engine is selected by this primitive.
**Evidence boundary:** Generated video/audio fixtures can validate Android storage and cleanup on emulators. They cannot prove Instagram resolution, Samsung gallery UX, full-frame media integrity or actual process-death recovery. The job layer uses the normalized target key for deduplication; filename collision safety is separate.
**Validation:** All six storage integration tests passed on API 29/35/36 at `0e3bc84` (CI `36272869682`). Pending-inclusive queries verify cleanup, not just absence from published media.

## D-014 — Native public-document resolver and one immediate transfer

**Status:** Accepted for the Share spike; Samsung acceptance and broader compatibility remain open
**Decision:** Resolve only metadata served in public Instagram Reel documents, matching the requested shortcode. Prefer a muxed progressive MP4; use native HTTPS and MediaStore in one user-started, non-sticky `dataSync` service. One active job, throttled progress, cancellation disconnects active I/O, a ten-minute deadline and 512 MiB cap; no automatic retry/boot/resume. Reconcile app-owned pending rows by exact target and folder on the next explicit attempt. Completed rows prevent repeat downloads.
**Comparison:** DownloadManager would still need a resolver and a separate pending-MediaStore publication/validation stage. WorkManager adds scheduling for work that must begin immediately; UIDT needs a second pre-API-34 path. A Python/yt-dlp/FFmpeg bundle adds native/runtime/update weight without helping this proven progressive-MP4 sample. WebView rendering adds renderer lifetime and cookie handling; the public embed already supplies the required media. None is justified yet.
**Evidence:** `DGOSAUyC903` public embed exposes a matching, non-copyright-blocked video URL; an anonymous desktop transfer returned 4,074,976 bytes with H.264/AAC. The owner independently confirmed logged-out phone playback. No login cookies, internal authenticated endpoints, challenge retries, TLS impersonation or third-party downloader service was used. Restricted/unavailable documents and unsafe redirects must fail visibly; no fallback around those restrictions.
**Android validation:** At `03a5d1f`, API 29/35/36 deterministic gates passed; the separate API 36 live Share test resolved, downloaded and published this Reel, verified video/audio metadata and decoded a frame (CI `36277332176`). The initial live test caught a missing ServerJS-wrapper parser path; bounded JSON-string decoding fixed it without a runtime/engine dependency. Progress/cancellation, blocked I/O interruption, screen recreation/closure, deduplication and pending-row reconciliation passed controlled tests. Actual force-stop/network/storage-pressure behavior and Samsung gallery acceptance remain owner gates.

## D-015 — Opt-in semantic current-Reel acquisition experiment

**Status:** Android mechanics verified on API 29/35/36; real Instagram acceptance pending.
**Decision:** The owner reports Usage Access works but does not identify the Reel, and explicitly authorized a narrow accessibility adapter. Android's package/usage APIs provide no cross-app Reel URL; Share remains the dependable explicit input. After ↓ only, an Instagram-package-filtered service finds unique visible semantic Share and Copy link controls, with Arabic/English labels, depth/node limits and an eight-second deadline. No coordinates, contacts, message actions, idle tree scanning or screen capture. Ambiguity/navigation/lock/timeout fail visibly.
**Clipboard constraint:** Android 10+ blocks background clipboard reads. A brief focused Tap Save dialog validates the clip timestamp against the just-issued Copy action, parses the fresh single Reel link, starts the existing service, and returns to Instagram. This can briefly show Instagram's share sheet and Tap Save's handoff; it is an experiment toward one-tap saving, not a claim of an invisible API. No stale link reuse. See [Android clipboard rules](https://developer.android.com/about/versions/10/privacy/changes#clipboard-data) and [accessibility service APIs](https://developer.android.com/guide/topics/ui/accessibility/views/service).
**Boundary:** The service is explicitly opt-in and optional for Share. Android mechanics use a disposable native fixture; production Instagram 448.0.0.52.84 Arabic labels/navigation remain a phone gate. D-012's Share-hint-only tap describes the prior build, not the intended normal behavior.

## D-016 — Public-first resolution with optional local session

**Status:** Public resolution verified with three live Android samples; account/session reliability still requires owner validation.
**Decision:** Keep native bounded page, embed and post-permalink strategies with an explicit HTML Accept header; only after all fail may an enabled local session retry those documents. Explicit private/copyright/challenge restrictions and rate limits stop. Public success never consults the session. Use Instagram's actual login page in a separate `:instagram` process with a dedicated WebView data suffix; no password collection, JS bridge, browser-cookie import or external credential service. Private same-UID IPC supplies a bounded cookie header to HTTPS `www.instagram.com` only, never to CDN/other redirect origins. Main-process WebView is disabled. Disconnect disables immediately, cancels the current transfer and clears cookies/storage/cache in that profile. Backup remains disabled. See [WebView profile isolation](https://developer.android.com/reference/android/webkit/WebView#setDataDirectorySuffix(java.lang.String)).
**Quality/lifecycle:** Select the largest advertised matching progressive MP4 variant across a document, preserving the original video/audio bytes. No transcoding or DASH muxer. Exactly one re-resolution is allowed for a rejected/expired CDN URL; there is no retry loop around rate limits/access restrictions. Pending publication/cancellation/deduplication remain native. Diagnostic category/stage/status are persisted without page bodies, cookies or signed media URLs.
**Engine comparison:** The maintained [Android wrapper](https://github.com/yausername/youtubedl-android) and [yt-dlp Instagram extractor](https://github.com/yt-dlp/yt-dlp/blob/master/yt_dlp/extractor/instagram.py) provide wider format/API handling and updates, but require Python/native ABI packaging and more integration maintenance. Measured publisher AARs and startup-path inspection are recorded in `TECHNICAL_SPIKE.md`. A desktop differential test found an extra engine success on `CDUMkliABpa`; explicit HTML negotiation and the public permalink let the native resolver reproduce it without an engine. Three anonymous progressive samples are now live-gate inputs; no heavy engine is added. Reconsider from concrete failing samples that demonstrate a benefit, including APK/startup/cancellation and session containment tests. D-014's no-session decision is superseded only by this explicitly optional legitimate login path; access-control bypasses remain excluded.

**D-015/D-016 validation update (2026-09-28):** Source `411ee70` passed build/lint and 18 JVM + 39 instrumentation tests per API 29/35/36 in CI `36354556280`, including the actual Arabic-fixture floating button through MediaStore and controlled optional-session/clear/origin tests. Separate live CI `36354557958` saved all three public samples through production Share/HTTP/storage, verifying audio/video and frame decoding. No owner-account login or real Instagram UI acceptance is claimed. APK growth over 0.2 is 71,293 bytes (about 69.6 KiB) with no production dependency change; this is an unminified debug artifact comparison, not a release benchmark.

## D-017 — Repair action dispatch and expose a usable native app

**Status:** Implementation under verification; phone Copy action failure is confirmed by the owner.
**Acquisition:** A visible clickable node is not proof that its action is accepted. Prefer semantic resource IDs and advertised action labels/content descriptions; use Arabic/English text as fallback, refresh nodes, then try that control's actionable ancestor chain. Retry rejected actions within a bounded event-driven request and restore only the sheet opened by that request. Keep a bounded last-request local metadata trace, with no screen text, link/clipboard contents or credentials. The clipboard is read only in a visible focused activity; target-device focus/freshness remains a required check.
**UI:** Use existing Compose Material and native vector/Canvas APIs, with no production dependency added. Home has one active switch, setup status and latest save; setup guides one permission at a time. Settings holds bounded size/opacity/reset, fixed best-progressive quality and MediaStore location, optional connection, permissions and Advanced diagnostics. No fake quality/location alternatives, new history database or permanent developer control panel.
