# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation
**Status:** Step 1 foundation complete; share, optional overlay and isolated storage prototypes pass the automated gate. Real-device and Reel-resolution gates remain open.
**Active plan:** `plans/0001-foundation-and-instagram-spike.md`
**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Implemented

- One Kotlin/Compose application module with a pinned, verified stable toolchain and build/test/lint CI.
- Defensive text share receiver: canonical Reel target, distinct short share link, invalid/ambiguous input recovery. No optional permissions required for Share.
- Native movable overlay experiment with persisted position, explicit foreground session and Stop controls. Optional usage access supplies approximate app context. No accessibility service.
- JVM regression tests and API 29/35/36 instrumentation coverage for the risky entry points/lifecycle; evidence is tracked in `docs/TECHNICAL_SPIKE.md`.

## Verification and limitations

- Clean-checkout debug build, strict lint, 14 JVM tests and 15 instrumentation tests per job passed on API 29/35/36 at `0e3bc84` in CI run `36272869682`, including the non-activity window-context refinement. Zero failures/errors/skips.
- Coverage includes share intake/recreation, window attachment, service Stop/permission revocation, worker termination, pending video/audio publication, cancellation/failure cleanup and filename collision safety. No real-device Instagram/OEM behavior has been verified.
- This development build does not download media. Current-Reel identity, short-link resolution, a viable public media resolver and transfer/job lifecycle remain open. The isolated MediaStore writer was verified with synthetic fixtures; it is not connected to the share flow.
- A bounded anonymous public-Reel page probe returned HTTP 200 but no direct video metadata. This is one environmental result, not a general claim about public Reel support.
- No network permission, embedded downloader, credentials, cloud features or analytics have been added.

## Next milestone

Execute the independent real-device overlay/share checks in `docs/TECHNICAL_SPIKE.md` on the owner's Galaxy S22 Ultra / Android 16. The owner will supply a public unauthenticated Reel URL. Validate that path before choosing the resolver/download lifecycle. Keep Plan 0001 active; do not start UI polish or a new plan.
