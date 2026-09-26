# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation
**Status:** Owner verified Share reception and overlay visibility. The actual Share save pipeline is implemented and entering its build/runtime gate.
**Active plan:** `plans/0001-foundation-and-instagram-spike.md`
**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Implemented

- One Kotlin/Compose application module with a pinned, verified stable toolchain and build/test/lint CI.
- Defensive text share receiver: canonical Reel target, distinct short share link, invalid/ambiguous input recovery. No optional permissions required for Share.
- Native movable overlay experiment with persisted position, explicit foreground session and Stop controls. Optional usage access supplies approximate app context. No accessibility service.
- JVM regression tests and API 29/35/36 instrumentation coverage for the risky entry points/lifecycle; evidence is tracked in `docs/TECHNICAL_SPIKE.md`.

## Verification and limitations

- Clean-checkout debug build, strict lint, 14 JVM tests and 15 instrumentation tests per job passed on API 29/35/36 at `0e3bc84` in CI run `36272869682`, including the non-activity window-context refinement. Zero failures/errors/skips.
- Owner confirmed Usage Access, overlay visibility in Instagram and receipt of `Dc_WBLAuR7M` on the S22 Ultra / Android 16. Drag/rotation, hide/show transitions, lock, OEM interruption and actual phone saving remain unverified.
- New native Share save pipeline: public page/embed metadata, HTTPS transfer, progress/cancellation, pending MediaStore publication, one active job and retry/recovery. New code has not yet passed its CI gate. Direct-current-Reel identity and short-link resolution remain open.
- Owner's second sample `DGOSAUyC903` resolved anonymously through its public embed; desktop download verified a 4,074,976-byte MP4 with H.264 720×1280 video and AAC audio. This proves the resolver candidate, not Android/phone saving.
- Native INTERNET/dataSync permissions added; no embedded engine, credentials, cloud features or analytics.

## Next milestone

Pass the new deterministic and public-Reel Android gates, then deliver a Share-saving APK for the owner to verify on the S22 Ultra. Continue direct-current-Reel work only after that save path succeeds. Keep Plan 0001 active.
