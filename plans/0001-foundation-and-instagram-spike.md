# Plan 0001 — Foundation and Instagram Technical Spike

**Status:** Active  
**Started:** 2026-09-26

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

**Progress:** Compared native APIs and privacy/coupling tradeoffs in `docs/TECHNICAL_SPIKE.md`. Explicit Share is the baseline; usage events give package context only. An opt-in Instagram-only semantic Share → Copy link adapter is now implemented after owner authorization. It is bounded, inactive between taps, has Arabic/English labels, and uses a focused fresh-clipboard handoff. A disposable fixture tests Android mechanics; real Instagram 448.0.0.52.84 Arabic acceptance remains open (D-015).

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

**Status: Development-build exit met; owner reports some phone saves, broader reliability remains open.** At `03a5d1f`, the real Share activity/service resolved `DGOSAUyC903`, downloaded it anonymously and published a playable video/audio file on API 36 (live CI `36277332176`). The test verified metadata and decoded a frame. Native public-page/embed parsing required support for JSON strings inside ServerJS wrappers; no JavaScript runtime or heavy engine was needed (D-014). This proves one public sample, not broad compatibility. The owner subsequently confirmed some public Reel saves and requested reliability/direct-current-Reel work; that continuation is authorized. Version 0.3 adds optional session fallback, variant selection, short-link normalization and specific diagnostics (D-016).

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

**Progress:** Reviewed the native resolver, service, storage transaction, cleanup assertions, dependency graph and failure UX. Final application/test code at `03a5d1f` passed build/lint, 16 JVM tests and 26 instrumentation tests per API 29/35/36 job (CI `36277332969`), plus one separate live public-Reel save test on API 36 (CI `36277332176`). No production dependency added. Real-device save/overlay acceptance, real Instagram acquisition/session acceptance and battery measurements remain outstanding. Short-link resolution is implemented with controlled redirect tests. Plan stays active; no new plan created.

Before closing this plan:

1. run build/tests/lint;
2. inspect dependency graph for unnecessary weight;
3. inspect background/battery behavior;
4. review failure UX;
5. re-read the implementation for simpler alternatives;
6. update memory/status/decisions/progress;
7. create the next plan only after this one is complete.

## Non-goals

- polished branding;
- multiple social platforms;
- bulk download;
- complex settings;
- cloud/backend;
- account system.

### 2026-09-28 continuation gate

- Implemented: explicit opt-in semantic current-Reel adapter, public-first/session-second native resolution, isolated actual Instagram login page and Disconnect/Clear, best available progressive variant, expired-link refresh, short-link normalization and safe diagnostics.
- In progress: build/lint and API 29/35/36 fixtures, separate live Share saves for `DGOSAUyC903` and `Cop84x6u7CP`. Record final evidence before marking this continuation verified.
- Still required on owner device: production Arabic node tree, successive-Reel correctness, login and session fallback, original audio/quality, Samsung storage UX and OEM interruption. No new plan.
