# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation
**Status:** Owner reports some successful public Reel saves on the S22 Ultra. Version 0.3 adds opt-in current-Reel acquisition and optional local Instagram session fallbacks; automated verification is in progress. Real Instagram Arabic UI and authenticated-account acceptance remain open.
**Active plan:** `plans/0001-foundation-and-instagram-spike.md`
**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Implemented

- One shipping Kotlin/Compose application module (plus a separate disposable UI fixture) with a pinned, verified stable toolchain and build/test/lint CI.
- Defensive text share receiver: canonical Reel target, distinct short share link, invalid/ambiguous input recovery. No optional permissions required for Share.
- Native movable overlay experiment with persisted position, explicit foreground session and Stop controls. Optional usage access supplies approximate app context. An opt-in, Instagram-only accessibility adapter now attempts Share → Copy link on a tap; no idle tree reads.
- JVM regression tests and API 29/35/36 instrumentation coverage for the risky entry points/lifecycle; evidence is tracked in `docs/TECHNICAL_SPIKE.md`.

## Verification and limitations

- Clean-checkout debug build, strict lint, 16 JVM tests and 26 instrumentation tests per job passed on API 29/35/36 at `03a5d1f` in CI `36277332969`. Zero failures/errors/skips. Tests include actual HTTP transfer, cancellation during blocked reads, pending cleanup, activity recreation/closure, duplicate prevention and recovery reconciliation.
- Owner confirmed Usage Access, overlay visibility in Instagram and receipt of `Dc_WBLAuR7M` on the S22 Ultra / Android 16. Drag/rotation, hide/show transitions, lock, OEM interruption remain unverified; owner reports some actual public phone saves, with failures on other public Reels.
- Native Share save pipeline: public page/embed metadata, HTTPS transfer, progress/cancellation, pending MediaStore publication, one active job and retry/recovery. Short share links now resolve before media identity/storage allocation; direct acquisition is implemented but not yet accepted against the production Instagram UI.
- Live CI `36277332176` saved `DGOSAUyC903` through the real Share activity/service on API 36, with no resolver/transport override. Published output passed video/audio metadata, nonzero size/duration and frame decode checks; the test then removed its own output. Desktop download independently verified a 4,074,976-byte H.264 720×1280/AAC MP4. Neither establishes Samsung gallery/playback acceptance.
- Native INTERNET/dataSync permissions added; no embedded extraction engine, cloud features or analytics. Optional Instagram login uses an isolated native WebView profile, with local session cookies and Disconnect/Clear.

## Next milestone

Complete the 0.3 build/lint/emulator and three-sample live gates, then test on Samsung SM-S908U1 / Android 16, Instagram 448.0.0.52.84 in Arabic: direct ↓ on successive Reels, independent Share, public resolution, optional login fallback, quality/audio, cancellation/success/errors and Disconnect. Do not equate synthetic accessibility controls with Instagram compatibility. Plan 0001 remains active.
