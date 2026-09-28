# Plan 0001 — Foundation and Instagram Technical Spike

**Status:** Active  
**Started:** 2026-09-26

### Owner acceptance and 0.5 continuation

- Owner reports the delivered 0.4 works very well with no observed problems. This establishes normal-use acceptance; targeted session, OEM interruption and battery measurements remain separate.
- Requested continuation: Arabic UI/notifications/errors and a persistent signing identity for future in-place updates. Implement native resources/RTL and platform language settings, preserve the accepted acquisition/download path, and retain one protected signing key. No new plan.
- Before delivery: pass build/unit/lint/device gates, inspect real Arabic screens, verify non-debuggable signing fingerprint and same-key higher-version update with retained settings/media. Document the one-time 0.4 key transition and preserve the owner's installation until they choose to replace it.

## Objective

Prove the risky parts of Tap Save before investing in polished UI. The result of this plan should be a minimal Android development build that demonstrates the reliable save path and provides evidence for the architecture decision.

## Step 1 — Repository and Android foundation

**Status: Complete.** Single-module Compose app, IDs/API baseline, pinned toolchain, checksum-verified wrapper and CI added. Clean-checkout `assembleDebug`, JVM tests and `lintDebug` passed at `db844a4` in CI run `36270467176`. See `docs/TECHNICAL_SPIKE.md` for the comparison and evidence. This completes foundation only; later integration/device gates remain open.

- Inspect all project docs before implementation.
- Create a minimal Kotlin Android application.
- Use Jetpack Compose unless a concrete technical blocker is found.
- Pick package/application IDs deliberately and record them.
- Use current stable Android/Gradle/Kotlin dependencies verified from primary documentation.
- Keep dependencies minimal.
- Establish basic build/test/lint workflow.

### Exit
Debug build succeeds from a clean checkout.

## Step 2 — Overlay spike

**Owner update:** Usage Access and overlay visibility in Instagram passed on Samsung SM-S908U1 / Android 16. The previous Share-hint-only tap was rejected as the normal product behavior; 0.3 now attempts automatic acquisition. The other device acceptance items below remain open.

**Progress:** Optional native window and non-sticky foreground session implemented with persisted drag position, usage-event context, Stop controls, permission checks and screen-lock cleanup. API 29/35/36 lifecycle tests passed, including non-activity window attachment and worker termination. Instagram hide/show transitions, drag/rotation, multi-window and OEM interruption acceptance remain open; do not mark this step complete from initial visibility alone.

Validate:

- overlay permission flow;
- movable floating control;
- persisted position;
- clean attach/detach lifecycle;
- showing/hiding based on relevant foreground context;
- no high-frequency busy polling;
- behavior when permission is denied/revoked.

### Exit
A minimal floating control can be safely used while Instagram is foregrounded and disappears appropriately.

## Step 3 — Share receiver spike

**Status: Complete for URL reception.** Owner confirmed actual Instagram Share reaches `Dc_WBLAuR7M` on Samsung / Android 16, supplementing API 29/35/36 intake/recreation tests. Actual saving is a separate Step 5/6 gate.

Implement the most reliable baseline first:

- register Tap Save as a receiver for shared text/URLs;
- parse a shared Instagram Reel target defensively;
- display/record a normalized target in development mode;
- reject unsupported/invalid input cleanly.

### Exit
Instagram Share → Tap Save reaches a normalized target reliably.

## Step 4 — Current Reel identification research

**Progress:** Compared native APIs and privacy/coupling tradeoffs in `docs/TECHNICAL_SPIKE.md`. Explicit Share is the baseline; usage events give package context only. An opt-in Instagram-only semantic Share → Copy link adapter is implemented after owner authorization. It is bounded, inactive between taps, has Arabic/English labels, and uses a focused fresh-clipboard handoff. Version 0.4 reproduces the actual Instagram 448 Arabic decorative-image rejection and selects the supported containing tile (D-017). The actual floating-button-to-MediaStore path, English/Arabic, nested/rejected actions and cleanup passed controlled API 29/35/36 tests at `e8fbf58`. Complete new-app Samsung clipboard/save and successive-Reel acceptance remain open.

Test approaches in order from least invasive to most invasive.

For each approach record:

- Android APIs used;
- permissions needed;
- reliability;
- battery cost;
- coupling to Instagram UI;
- behavior across app restarts/navigation;
- privacy implications;
- known failure modes.

Do not lock AccessibilityService into the main architecture merely because it can inspect UI. Treat it as an experimental adapter only if necessary.

### Exit
Choose a primary strategy plus fallback strategy and record the choice in `docs/DECISIONS.md`.

## Step 5 — Media resolution/download spike

**Status: Development-build exit met; owner reports some phone saves, broader reliability remains open.** At `03a5d1f`, the real Share activity/service resolved `DGOSAUyC903`, downloaded it anonymously and published a playable video/audio file on API 36 (live CI `36277332176`). The test verified metadata and decoded a frame. Native public-page/embed parsing required support for JSON strings inside ServerJS wrappers; no JavaScript runtime or heavy engine was needed (D-014). This proved one public sample, not broad compatibility. The owner subsequently confirmed some public Reel saves and requested reliability/direct-current-Reel work; that continuation is authorized. Version 0.3 adds public page/embed/post-permalink strategies, optional session fallback, variant selection, short-link normalization and specific diagnostics (D-016). At `411ee70`, live API 36 CI `36354557958` saved three public Reels anonymously with video/audio and frame verification; authenticated-account acceptance remains open.

Compare practical on-device options, including a lightweight direct resolver and, only if justified, an embedded maintained downloader engine.

Measure/inspect:

- APK/app size impact;
- initialization cost;
- dependency maintenance;
- Instagram compatibility;
- progress reporting;
- cancellation;
- error quality;
- audio/video muxing needs;
- architecture/security impact.

Do not implement access-control bypasses.

### Exit
At least the Share flow can resolve one supported Reel and save it locally in a development build, or the exact blocker is documented with evidence and the next viable design is selected.

## Step 6 — Storage and job lifecycle

**Progress:** One user-started dataSync transfer service, progress/cancellation, activity-independent execution, a single-job checkpoint, app-owned pending cleanup and published-target deduplication. API 29/35/36 tests passed exact HTTP-to-MediaStore bytes, interrupted/truncated input, blocked-read cancellation, recreation/closing the Share activity, deduplication and pending-row reconciliation. Live API 36 Reel saving also passed. Samsung gallery/playback, actual force-stop and storage/network pressure acceptance remain open.

- Save through modern Android storage APIs.
- Target `Movies/Tap Save/` unless testing shows a better user-visible location.
- Ensure incomplete files are not presented as complete.
- Confirm cancellation/cleanup.
- Add deduplication key/strategy.
- Validate network interruption behavior.
- Choose foreground-service vs WorkManager behavior based on measured job semantics.

### Exit
A requested download is robust across ordinary activity lifecycle changes and appears correctly in local media storage.

## Step 7 — Review

**Progress:** Reviewed the native resolver, service, storage transaction, cleanup assertions, dependency graph, failure UX and rendered actual UI. Final 0.4 application/test code at `e8fbf58` passed build/strict lint, 19 JVM tests and 44 instrumentation tests per API 29/35/36 job (CI `36422892820`). Three separate live public-Reel save tests passed on API 36 at `3e5f0e5` (CI `36419880160`); subsequent changes only affect system bars, capture and CI. No production dependency added. Complete Samsung acquisition/save, real account session, OEM lifecycle and battery measurements remain outstanding. Plan stays active; no new plan created.

Before closing this plan:

1. run build/tests/lint;
2. inspect dependency graph for unnecessary weight;
3. inspect background/battery behavior;
4. review failure UX;
5. re-read the implementation for simpler alternatives;
6. update memory/status/decisions/progress;
7. create the next plan only after this one is complete.

## Non-goals

- polished branding beyond the owner's explicitly requested actual Home/Settings/setup and floating-control refinement;
- multiple social platforms;
- bulk download;
- complex settings;
- cloud/backend;
- account system.

### 2026-09-28 continuation gate

- Implemented: explicit opt-in semantic current-Reel adapter, public-first/session-second native resolution, isolated actual Instagram login page and Disconnect/Clear, best available progressive variant, expired-link refresh, short-link normalization and safe diagnostics.
- Verified at `411ee70`: build/strict lint; 18 JVM + 39 instrumentation tests per API 29/35/36, zero failures/errors/skips; three separate real anonymous Share saves for `DGOSAUyC903`, `Cop84x6u7CP` and `CDUMkliABpa`. Exact APK/hash and CI links are recorded in `PROJECT_STATUS.md` and `docs/TECHNICAL_SPIKE.md`. Direct current-Reel evidence uses a controlled Android UI fixture, not the production Instagram app.
- The later 0.4 continuation below establishes the production Arabic Copy node/action tree. Still required on owner device: complete focused handoff, successive-Reel correctness, login and session fallback, original audio/quality, Samsung storage UX and OEM interruption. No new plan.

### 2026-09-28 owner bug and production UI continuation

- Owner rejected the 0.3 `COPY_ACTION_FAILED` behavior and development control panel. Direct acquisition is a confirmed target-device bug; preceding synthetic passes do not close it.
- Completed implementation: live node/action inspection, refreshed semantic selectors with actionable ancestors and bounded event-driven readiness retries, owned-sheet cleanup, focused clipboard activity; actual Home/master switch, sequential setup, Settings/Advanced and native vector/progress overlay. Share remains independent.
- Completed live control gate: the actual Arabic Copy image rejected ACTION_CLICK; its supported containing tile accepted ACTION_CLICK. The production selector follows the supported parent and stops at the observed share-row boundary. The owner disconnected before 0.4 installation; complete new-app Samsung clipboard/MediaStore acceptance remains required.
- Completed automated/visual gate: `e8fbf58` passed assembly/strict lint, 19 JVM + 44 instrumentation tests per API 29/35/36 with zero failures/errors/skips; real activity and native overlay captures reviewed. Unchanged save pipeline passed three anonymous live Share saves at `3e5f0e5`. Tested 0.4 APK, checksum, evidence metadata and Arabic phone checklist delivered; exact source/artifact hashes in `PROJECT_STATUS.md` and `docs/TECHNICAL_SPIKE.md`.
- Next: full ↓ → fresh current-Reel link → saved matching media on Samsung, including automatic sheet restoration, then session/quality/lifecycle/battery acceptance. No phone installation or session reset occurred. Keep Plan 0001 active.
