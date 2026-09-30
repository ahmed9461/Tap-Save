# PROJECT_STATUS

## Current state

**Status:** 0.5.1 reliability repair installed and verified on the owner's Samsung Galaxy S22 Ultra / Android 16 / Instagram 448.0.0.52.84 Arabic (2026-10-01).

- Reproduced accepted Copy followed by `FRESH_REEL_LINK_MISSING`: early Back could cancel Instagram's asynchronous clipboard write. The adapter now keeps its sheet alive through focused fresh-link capture, waits for that reader to close, then restores the Reel before saving. No background clipboard read, coordinates or stale-link reuse.
- Six different controlled phone saves passed: four successive floating-button saves, actual native Instagram Share/chooser for a fifth, and resolution-stage cancel/Retry for a sixth. Each matching MediaStore file passed full H.264/AAC decoding at 720×1280 with non-silent audio.
- One resolved publicly without consulting the session; five used the owner's optional local session after public metadata was missing. Cancellation left no target row or transfer service. A repeated direct tap returned `ALREADY_SAVED` and the same row.
- Same-key 0.5.0 → 0.5.1 update retained Arabic/setup permissions, local session and original media metadata. Final explicit pending-media query was empty. Temporary diagnostic package removed; normal overlay remains enabled.
- Advanced diagnostics retain one bounded structural acquisition/download trace. No passwords, cookies, screen captions, document bodies or signed media URLs are logged. No new production dependency.

**Active plan:** `plans/0001-foundation-and-instagram-spike.md`

**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Retained Arabic UI and signing

- All 112 translatable app strings have Arabic resources, including setup, Settings, Share, notifications, errors, login chrome and accessibility labels; setup counts cover Arabic plural forms. Brand/folder identity and diagnostic codes remain stable.
- Arabic follows device locale on API 29+, and Android 13+ offers a per-app language selector from Settings. Actual RTL Home/setup/Settings captures were reviewed. No dependency or custom font added.
- Non-debuggable release uses one protected RSA-3072 signing identity and an increasing version code from `app/version.properties`. Encrypted GitHub secrets and two protected owner-local copies retain the key. Only the public certificate/fingerprint is in Git. Never distribute ephemeral debug APKs as personal updates; see `docs/SIGNING.md`.
- Native public-first/session-second resolution, original progressive MP4 audio and MediaStore publication are retained. The acquisition timing repair and local diagnostics are the 0.5.1 changes.

## Verification

- Delivered application source `edb902f6c5afa9b2b30efbe07e4d7a8d1641a260`: [Android CI 36789484387](https://github.com/ahmed9461/Tap-Save/actions/runs/36789484387) passed assembly, strict lint, **19 JVM + 48 instrumentation tests on each API 29/35/36**, zero failures/errors/skips, including asynchronous Copy and prior lifecycle/storage/Arabic gates.
- [Live CI 36789595846](https://github.com/ahmed9461/Tap-Save/actions/runs/36789595846), same source: three independent real anonymous Share saves (`DGOSAUyC903`, `Cop84x6u7CP`, `CDUMkliABpa`), each with audio/video metadata and a decoded frame. No account or transport override.
- [Signed CI 36789479757](https://github.com/ahmed9461/Tap-Save/actions/runs/36789479757), same source: non-debuggable signature/package/version checks and API 36 same-key code 5 → 6 replacement retaining settings and exact media bytes.
- An extra probe-only commit's API 35 run failed before acquisition: seven fixture cases could not see their launched window; logcat showed a UiAutomation callback on a dead thread. A test-only runner keeps one non-suppressing automation policy across Compose/accessibility tests. Source `23c384c` then passed [matrix 36791713644](https://github.com/ahmed9461/Tap-Save/actions/runs/36791713644), again 19 JVM + 48 instrumentation tests per API 29/35/36 with zero failures/errors/skips, and [signed update 36791751852](https://github.com/ahmed9461/Tap-Save/actions/runs/36791751852). Report archive integrity and individual result XML/update phases were checked. Shipping app source and installed APK are unchanged.

## Delivery and remaining boundaries

Installed version 0.5.1/code 6, **23,211,148 bytes**, SHA-256 `c9a77a15dd1ec76eb78a74e80aead3ed345889bb4263fed874f674d08a190105`. Retained certificate SHA-256 `c047a8350f478a6dd72b4000b40a958dac992cbbeb3294a17e8a12bf0ed521ae`. APK/checksum/evidence are in task outputs. Reuse this identity and increase the version code for future releases.

Six controlled phone samples and three anonymous emulator samples prove these paths, not universal Instagram compatibility. Best quality means the largest matching progressive MP4 exposed by the successful document; no DASH muxing. Actual account clear remains untested to preserve the owner's session. OEM interruption/force-stop, storage/network pressure, battery and broader Instagram variants remain open. Plan 0001 stays active; no new plan.
