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
