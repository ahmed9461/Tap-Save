# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation
**Status:** Share resolution/download/MediaStore saving passed the live Android 16 gate. Samsung save/playback acceptance is next; current-Reel identification remains separate.
**Active plan:** `plans/0001-foundation-and-instagram-spike.md`
**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Implemented

- One Kotlin/Compose application module with a pinned, verified stable toolchain and build/test/lint CI.
- Defensive text share receiver: canonical Reel target, distinct short share link, invalid/ambiguous input recovery. No optional permissions required for Share.
- Native movable overlay experiment with persisted position, explicit foreground session and Stop controls. Optional usage access supplies approximate app context. No accessibility service.
- JVM regression tests and API 29/35/36 instrumentation coverage for the risky entry points/lifecycle; evidence is tracked in `docs/TECHNICAL_SPIKE.md`.

## Verification and limitations

- Clean-checkout debug build, strict lint, 16 JVM tests and 26 instrumentation tests per job passed on API 29/35/36 at `03a5d1f` in CI `36277332969`. Zero failures/errors/skips. Tests include actual HTTP transfer, cancellation during blocked reads, pending cleanup, activity recreation/closure, duplicate prevention and recovery reconciliation.
- Owner confirmed Usage Access, overlay visibility in Instagram and receipt of `Dc_WBLAuR7M` on the S22 Ultra / Android 16. Drag/rotation, hide/show transitions, lock, OEM interruption and actual phone saving remain unverified.
- Native Share save pipeline: public page/embed metadata, HTTPS transfer, progress/cancellation, pending MediaStore publication, one active job and retry/recovery. Direct-current-Reel identity and short-link resolution remain open.
- Live CI `36277332176` saved `DGOSAUyC903` through the real Share activity/service on API 36, with no resolver/transport override. Published output passed video/audio metadata, nonzero size/duration and frame decode checks; the test then removed its own output. Desktop download independently verified a 4,074,976-byte H.264 720×1280/AAC MP4. Neither establishes Samsung gallery/playback acceptance.
- Native INTERNET/dataSync permissions added; no embedded engine, credentials, cloud features or analytics.

## Next milestone

Verify the supplied 0.2 development APK saves the public Reel on the S22 Ultra, appears under Movies/Tap Save and plays with audio. Then continue the independent direct-current-Reel experiments and remaining overlay acceptance. Keep Plan 0001 active.
