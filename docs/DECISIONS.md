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
**Evidence:** Android sharing exposes caller-provided text; overlay/usage APIs do not expose another app's current content URL. Actual Instagram share acceptance remains a device gate.

## D-012 — User-started overlay and optional usage context experiment

**Status:** Experimental; production choice deferred until device evidence
**Decision:** One native `TYPE_APPLICATION_OVERLAY` window, hosted by a non-sticky `specialUse` foreground session with Stop notification/action. Usage access is optional and isolated; sampling is limited to an unlocked, explicitly started session, at 1.5-second intervals on a worker thread. Screen lock/stop invalidates pending results. No accessibility dependency, boot start or wake lock.
**Tradeoff:** Usage events can lag and multi-window/OEM behavior is uncertain. The control currently directs the user to Share; it cannot identify a Reel. Notifications must be enabled for this experiment so Stop remains discoverable. Download lifecycle/engine choices stay open.
**Review refinement:** API 30+ uses a display-bound window context for non-activity overlay resources/metrics. Attachment tests start from a non-activity context. Notification setup opens system settings on all supported APIs, including after a prior denial.

## D-013 — Validate storage separately from Instagram resolution

**Status:** Accepted for the storage spike; job integration remains open
**Decision:** Use MediaStore pending rows in `Movies/Tap Save/` without broad media/storage permissions. A blocking writer runs off the UI thread, verifies copy size when known and video metadata, closes both streams before publication, and deletes its own allocated row on cancellation/failure. No queue/FGS/download engine is selected by this primitive.
**Evidence boundary:** Generated video/audio fixtures can validate Android storage and cleanup on emulators. They cannot prove Instagram resolution, Samsung gallery UX, full-frame media integrity or process-death recovery. Deduplication belongs to the future job layer using the normalized target key; filename collision safety is separate.
**Validation:** All six storage integration tests passed on API 29/35/36 at `0e3bc84` (CI `36272869682`). Pending-inclusive queries verify cleanup, not just absence from published media.
