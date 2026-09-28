# PROJECT_MEMORY

## Product

**Name:** Tap Save  
**Repository:** `ahmed9461/Tap-Save`  
**Platform:** Android only.  
**Usage model:** Personal/private use.  
**Primary language:** Kotlin.  
**UI:** Jetpack Compose activities; native View for the floating control.

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

- Owner's live-validation device: Samsung Galaxy S22 Ultra SM-S908U1, Android 16. Owner confirmed Usage Access, overlay visibility in Instagram and Share URL receipt. Owner now reports some actual public Reel saves and failures on others; Instagram 448.0.0.52.84, Arabic. Detailed gallery/quality/lifecycle acceptance remains open. Keep it independent from current-Reel identification.

- One shipping `app` module, `instagram-fixture` for disposable emulator tests only, and a standalone `device-probe` diagnostic package; neither helper is an application dependency. Namespace/application ID `io.github.ahmed9461.tapsave`; minimum API 29, compile/target API 37. Minimum 29 avoids legacy storage branches; owner-device compatibility still needs verification.
- Pinned versions and their primary sources are in `docs/TECHNICAL_SPIKE.md`; use the wrapper and `docs/BUILDING.md` commands.
- `SharedTargetParser` isolates Instagram URL normalization. Direct Reel links are canonicalized and tracking removed. `/share/reel/` tokens are classified separately; they are not Reel IDs. Never treat the last shared target as the Reel currently on screen.
- Share activity is independent of overlay/usage/notification permissions. The native pipeline performs public resolution, cancellable transfer and MediaStore publication. `DGOSAUyC903`, `Cop84x6u7CP` and `CDUMkliABpa` passed anonymous live Android saves at `3e5f0e5` (CI `36419880160`); this is emulator evidence, not Samsung acceptance or broad Instagram compatibility.
- The optional native overlay has a user-started `specialUse` session and Stop controls. Usage events provide approximate app context only. The opt-in accessibility adapter is package-filtered to Instagram and reads semantic nodes only during an explicit user request: at most 12 seconds for controls, 1.5 seconds for owned-sheet cleanup and 3 seconds for focused handoff. No clipboard listener, boot receiver, wake lock or analytics. Both explicit Share and overlay saves use the same transfer service.
- Current spike limitations and device evidence belong in `TECHNICAL_SPIKE.md`. Do not call the full phase complete from build/emulator evidence alone.
- Plan 0001 Step 1 passed the clean CI build/unit/lint gate; Step 3 URL reception passed owner-device validation. Keep the plan active until its remaining overlay/current-target and save acceptance gates are met.
- `MediaStoreVideoWriter` uses pending rows and rollback. The Share pipeline adds a single short `dataSync` service, one job checkpoint, exact-target app-owned row reconciliation and deduplication. Build/runtime evidence must distinguish synthetic fixtures, a real public resolver/download and owner-device acceptance.
- CI covers the minimum API 29 and API 35/36 with real emulator integrations. Pending-row cleanup tests must explicitly include pending items in MediaStore queries; a default query can hide a leaked incomplete row.
- Public embed metadata can be a `contextJSON` string inside a `requireLazy` / ServerJS wrapper. Decode bounded JSON literals without executing JavaScript. Keep fixtures faithful to observed response structure; standalone JSON fixtures previously missed this integration defect. Public live tests are explicit, separate from deterministic CI, and stop at restrictions.

## Repository workflow

Agents must follow `AGENTS.md`, the active plan, and the project documentation. Stable decisions belong in `docs/DECISIONS.md`; chronological work belongs in `docs/PROGRESS_LOG.md`.

## Version 0.3 experiment boundaries

- Public page/embed/post-permalink resolution with an explicit HTML Accept header always precedes optional authenticated requests. Session cookies are read through private IPC from a dedicated `:instagram` WebView process/profile; only the opt-in flag is stored by the main process. No password field scraping, JavaScript bridge, credential logging, third-party downloader, or cookie forwarding to media/CDN hosts. Disconnect disables immediately, cancels a transfer and clears the isolated profile. Actual owner-account login/fallback requires phone validation.
- Direct acquisition never reuses a previously shared URL or stale clipboard. It selects a unique semantic Share control and Copy link control (Arabic/English), then validates a timestamped clipboard handoff. Missing/ambiguous controls, another app, lock, interruption and timeout fail with acquisition codes and the independent Share fallback. The observed Instagram 448 Arabic Copy tile is covered; successive-Reel identity and complete Samsung handoff remain device gates.
- Rank available progressive MP4 variants by advertised dimensions, preserve original bytes/audio, and refresh resolution once for an expired CDN URL. No DASH muxing or promise of Instagram's absolute maximum quality. Diagnostics distinguish auth, rate limit, unavailable metadata, incompatible extraction, network, expired URL and storage errors without response bodies/session/CDN URLs.
- `/share/reel/` tokens are resolved to canonical Reel identities before allocation/deduplication. The checkpoint also retains the sanitized requested link so Share UI can follow normalization.
- The disposable `com.instagram.android` fixture tests Android accessibility/clipboard semantics, not the real Instagram app. The runner refuses non-emulators or an existing Instagram installation. Never install this fixture on the owner's phone or distribute it as the Tap Save APK.

## Latest verified continuation

- Owner update after installing 0.4: the app works very well and no problems were observed. Accept this as owner confirmation of normal use; do not infer dedicated authentication, battery or stress-test results. The owner next requested Arabic and a persistent signing identity. This continuation stays under Plan 0001.
- Version 0.5 adds Android Arabic resources/RTL and the Android 13+ per-app language settings entry. Personal releases must use the existing protected PKCS12 identity and GitHub signing secrets documented in `docs/SIGNING.md`, never ephemeral debug keys. Keep the checked-in public certificate and application ID; increment `app/version.properties`. Source `902ceae` passed build/strict lint, 19 JVM + 46 instrumentation tests per API 29/35/36 (CI `36443683479`), with native Arabic UI/recreation evidence on API 35/36. Signed CI `36443689015` verified non-debuggable signature/version and version-code 4 → 5 replacement with retained settings/media; no phone install/reset occurred. The new stable key requires one initial replacement of the old unrelated debug-key installation; future updates reuse this identity.

- New owner evidence (2026-09-28): 0.3 direct acquisition fails `COPY_ACTION_FAILED` with Arabic Copy link visibly open. Earlier plain-Button fixtures did not represent a rejected click or nested action tile. The S22 Ultra was connected by ADB for scoped control inspection, then disconnected for personal use before 0.4 installation. Do not assume it remains available. No owner app/session was replaced or cleared.
- Live S22 inspection isolated the cause: `com.instagram.android:id/button` is an ImageView with the Copy link content description and `clickable=true`, but no supported ACTION_CLICK. Calling it returned false. Its enclosing LinearLayout two levels up advertises ACTION_CLICK and returned true. `id/label` supplies a separate non-clickable Arabic label in the same tile. Use supported actions and stop ancestor traversal at `id/direct_external_reshare_row`; do not click the overall bottom sheet. This proves the real control dispatch, not yet the new app's complete clipboard/save flow.
- Version 0.4 uses real Home/Settings/sequential setup screens and a native circular vector/progress control. Home has one master switch; optional session, permission management and bounded size/opacity/reset controls live in Settings. Safe local diagnostics live under Advanced. Share needs none of the overlay/acquisition permissions. The separate temporary phone probe has no activity, network permission or background service; remove package `io.github.ahmed9461.tapsave.probe` when the phone is next available.

- Source `e8fbf5888c4a8414462a514b1a457561aaf9b499`: CI `36422892820` passed build/strict lint, 19 JVM and 44 instrumentation tests on each of API 29/35/36, zero failures/errors/skips. Coverage includes the actual floating button through fixture acquisition/HTTP/MediaStore, rejected decorative-image actions, supported ancestors, delayed readiness, failure cleanup, fresh/stale/ambiguous/other-app cases and actual Home/setup/Settings. Rendered activity and native overlay states were visually reviewed.
- Live CI `36419880160` passed three real anonymous Share saves with audio/video metadata and decoded frames at `3e5f0e5`. Subsequent changes through `e8fbf58` affect system bars, rendering capture and CI only; acquisition/resolver/transfer/storage code is unchanged. Delivered 0.4 APK: 29,855,521 bytes, SHA-256 `9b7ebc57a5c9e4caec72a5a24d2f684ba97a52ba82c3f3fd8ccbd249b1c4eafc`. Its CI debug signer differs from the installed phone build; replacement would clear app settings/session. No production dependency added; Plan 0001 remains active for complete Samsung acquisition, owner session, OEM and battery acceptance.
- Android automatic notification group summaries may reuse a numeric ID with a different tag. Lifecycle tests must identify the actual untagged overlay notification and worker, not assume ID alone identifies it. A foreground-service start must fulfil its foreground contract before stopping after a prerequisite changes between caller check and service startup.

### Current personal release

Version 0.5.0/code 5: 23,211,148 bytes, SHA-256 `e901ddc1c3de5d0db174c2acaa7b3b4ec3793018ca02770fc6897d98b73c2225`. Stable certificate SHA-256 `c047a8350f478a6dd72b4000b40a958dac992cbbeb3294a17e8a12bf0ed521ae`. Keep this identity, increase `app/version.properties` and use the manual signed-release workflow; do not generate a replacement key. The owner accepted 0.4 normal use after the prior phone-control repair. Future work must preserve that working pipeline and distinguish optional-session/OEM/battery measurements from that general acceptance.
