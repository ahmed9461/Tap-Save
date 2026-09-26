# PROJECT_MEMORY

## Product

**Name:** Tap Save  
**Repository:** `ahmed9461/Tap-Save`  
**Platform:** Android only.  
**Usage model:** Personal/private use.  
**Primary language:** Kotlin.  
**UI:** Jetpack Compose activities; native View for the experimental overlay window.

## Core idea

Tap Save should let the user keep browsing Instagram Reels while a small floating download control remains available. When the current reel is worth keeping, one tap should start saving it locally with as little interruption as possible.

The experience should prioritize:

- speed;
- minimal UI;
- no ads;
- no unnecessary app switching;
- low memory/battery overhead;
- reliability across normal Instagram updates;
- clear fallback behavior when direct detection is unavailable.

## Initial product behavior

- The floating action should appear only in the relevant context, ideally only while Instagram is foregrounded.
- The floating control must be movable and unobtrusive.
- A tap should begin the fastest supported save flow for the currently targeted reel.
- Download progress should be visible without blocking Instagram.
- Completion should be obvious but quiet.
- Saved media should be available in the Android media library / a predictable Tap Save folder.
- Duplicate saves should be prevented or clearly handled.
- A Share → Tap Save flow is the required fallback path.
- Direct current-reel detection is a technical goal, not an excuse to make the app fragile.

## Scope boundaries

### In scope first
- Android.
- Instagram Reels.
- Public or otherwise legitimately accessible media presented to the user.
- Local saving.
- Overlay UI.
- Share target fallback.
- Robust download queue with one or a small number of active jobs.
- Lightweight history/status if useful.

### Explicitly out of scope for v1
- iOS.
- Desktop.
- Accounts/cloud sync.
- Ads.
- Analytics.
- Social features.
- Bulk scraping.
- Private-account bypass.
- DRM/access-control circumvention.
- Automated following/liking/commenting.
- TikTok/X/Facebook support before Instagram is stable.

## Architecture principles

- Separate UI, platform detection, URL/media resolution, download execution, and persistence.
- Instagram-specific logic must live behind a platform adapter/resolver boundary.
- The download engine must be replaceable.
- Prefer official/native Android pathways before UI scraping.
- If an accessibility-based adapter is explored, it must be opt-in, limited to the smallest required surface, and replaceable.
- No server should be required for the initial personal-use version unless local-only approaches prove technically inadequate.
- Prefer on-device processing and storage.

## UX target

The ideal interaction is:

`browse → tap ↓ → progress → ✓ → continue browsing`

No full-screen interstitial, no ad, no unnecessary confirmation for the normal successful path.

## Foundation implementation

- Owner's live-validation device: Samsung Galaxy S22 Ultra SM-S908U1, Android 16. Owner confirmed Usage Access, overlay visibility in Instagram and Share URL receipt. A real public Reel save passed on an API 36 emulator; owner-phone save/playback remains the next gate. Keep it independent from current-Reel identification.

- Single `app` module, namespace/application ID `io.github.ahmed9461.tapsave`; minimum API 29, compile/target API 37. Minimum 29 avoids legacy storage branches; owner-device compatibility still needs verification.
- Pinned versions and their primary sources are in `docs/TECHNICAL_SPIKE.md`; use the wrapper and `docs/BUILDING.md` commands.
- `SharedTargetParser` isolates Instagram URL normalization. Direct Reel links are canonicalized and tracking removed. `/share/reel/` tokens are classified separately; they are not Reel IDs. Never treat the last shared target as the Reel currently on screen.
- Share activity is independent of overlay/usage/notification permissions. The 0.2 development pipeline performs public resolution, cancellable transfer and MediaStore publication. `DGOSAUyC903` passed the live Android save test at `03a5d1f`; this is emulator evidence, not Samsung acceptance or broad Instagram compatibility.
- The optional native overlay has a user-started `specialUse` session and Stop controls. Usage events provide approximate app context only. No accessibility service, clipboard listener, boot receiver, wake lock or analytics is present. Networking is limited to explicit Share save requests.
- Current spike limitations and device evidence belong in `TECHNICAL_SPIKE.md`. Do not call the full phase complete from build/emulator evidence alone.
- Plan 0001 Step 1 passed the clean CI build/unit/lint gate; Step 3 URL reception passed owner-device validation. Keep the plan active until its remaining overlay/current-target and save acceptance gates are met.
- `MediaStoreVideoWriter` uses pending rows and rollback. The Share pipeline adds a single short `dataSync` service, one job checkpoint, exact-target app-owned row reconciliation and deduplication. Build/runtime evidence must distinguish synthetic fixtures, a real public resolver/download and owner-device acceptance.
- CI covers the minimum API 29 and API 35/36 with real emulator integrations. Pending-row cleanup tests must explicitly include pending items in MediaStore queries; a default query can hide a leaked incomplete row.
- Public embed metadata can be a `contextJSON` string inside a `requireLazy` / ServerJS wrapper. Decode bounded JSON literals without executing JavaScript. Keep fixtures faithful to observed response structure; standalone JSON fixtures previously missed this integration defect. Public live tests are explicit, separate from deterministic CI, and stop at restrictions.

## Repository workflow

Agents must follow `AGENTS.md`, the active plan, and the project documentation. Stable decisions belong in `docs/DECISIONS.md`; chronological work belongs in `docs/PROGRESS_LOG.md`.
