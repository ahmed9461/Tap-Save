# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation
**Status:** Step 1 foundation complete; share receiver and optional overlay prototype implemented; emulator/device gates in progress.
**Active plan:** `plans/0001-foundation-and-instagram-spike.md`
**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Implemented

- One Kotlin/Compose application module with a pinned, verified stable toolchain and build/test/lint CI.
- Defensive text share receiver: canonical Reel target, distinct short share link, invalid/ambiguous input recovery. No optional permissions required for Share.
- Native movable overlay experiment with persisted position, explicit foreground session and Stop controls. Optional usage access supplies approximate app context. No accessibility service.
- JVM regression tests and API 29/35/36 instrumentation coverage for the risky entry points/lifecycle; evidence is tracked in `docs/TECHNICAL_SPIKE.md`.

## Verification and limitations

- Clean-checkout debug build, all 14 JVM tests and lint passed at `506951e` in CI run `36271302584`.
- Share/window/service instrumentation passed on API 35 and 36. Storage tests exposed a query that excluded pending rows; corrected pending-inclusive assertions and broader API 29/35/36 checks are pending. No real-device Instagram/OEM behavior has been verified.
- This development build does not download media. Current-Reel identity, short-link resolution, a viable public media resolver and transfer/job lifecycle remain open. An isolated MediaStore writer is being verified with synthetic fixtures; it is not connected to the share flow.
- A bounded anonymous public-Reel page probe returned HTTP 200 but no direct video metadata. This is one environmental result, not a general claim about public Reel support.
- No network permission, embedded downloader, credentials, cloud features or analytics have been added.

## Next milestone

Finish the clean CI/emulator gate, then execute the real-device overlay/share checks in `docs/TECHNICAL_SPIKE.md`. Validate a permitted public Reel before choosing the resolver/download lifecycle. Keep Plan 0001 active; do not start UI polish or a new plan.
