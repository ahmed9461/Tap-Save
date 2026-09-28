# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation

**Status:** Version 0.4 control repair and actual Home/Settings/setup/overlay UI implemented, built and tested. The real Samsung Copy action failure was reproduced and its supported parent action verified. Complete new-app acquisition through focused clipboard and saving still requires phone acceptance.

**Active plan:** `plans/0001-foundation-and-instagram-spike.md`

**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

The owner disconnected the S22 Ultra for personal use before 0.4 installation. No owner app, settings or session was replaced or cleared. Continue without assuming ADB availability.

## Implemented

- Native public-first Reel resolution with page/embed/post-permalink fallbacks, optional isolated Instagram session, best available combined video/audio, expired-link refresh and specific safe failure categories.
- Independent Share → Tap Save path with progress/cancellation, one immediate transfer, pending MediaStore publication under `Movies/Tap Save/`, deduplication and cleanup/recovery.
- Opt-in Instagram-only acquisition after a floating-button tap: semantic controls, supported action/ancestor selection, event-driven bounded readiness/retries, owned-sheet cleanup and a focused fresh-clipboard handoff. No coordinates or background clipboard reading.
- Real Home with one master switch, sequential permission setup and Settings/Advanced. Native circular draggable control has vector, spinner, progress, success/error states and size/opacity/reset settings. No production dependency added.
- Separate disposable emulator fixture and standalone phone control probe; neither ships with Tap Save.

## Verified

- **Actual S22 / Android 16 / Instagram 448.0.0.52.84 Arabic:** Copy's ImageView claims clickable but omits ACTION_CLICK; clicking it returned false. Its enclosing LinearLayout two parents up supports ACTION_CLICK and returned true. This establishes the reported failure and supported dispatch, not full 0.4 clipboard/save acceptance.
- **Final source `e8fbf58`:** [CI `36422892820`](https://github.com/ahmed9461/Tap-Save/actions/runs/36422892820) passed assembly, strict lint, **19 JVM + 44 instrumentation tests on each API 29/35/36**, zero failures/errors/skips. Coverage includes realistic rejected/nested action controls, acquisition/cleanup through HTTP/MediaStore, UI, resolver/session, cancellation and lifecycle/storage regressions. Actual rendered screens and all native floating-control states were visually reviewed.
- **Live Share pipeline at `3e5f0e5`:** [CI `36419880160`](https://github.com/ahmed9461/Tap-Save/actions/runs/36419880160) saved `DGOSAUyC903`, `Cop84x6u7CP` and `CDUMkliABpa` anonymously through production resolver/HTTP/MediaStore on API 36. All three passed audio/video metadata and frame decoding, then removed only their test outputs. Later changes through `e8fbf58` affect system bars/capture/CI only; acquisition/resolver/transfer/storage code is unchanged. This is three-sample evidence, not universal compatibility.
- Owner previously confirmed Usage Access, overlay visibility, Share URL receipt and some public phone saves. Actual account login/session benefit, successive-Reel correctness, Samsung playback/quality, OEM interruption and battery remain open.

## Next milestone

When the phone is available, test 0.4 on Samsung SM-S908U1 / Android 16, Instagram 448 Arabic: direct ↓ on successive Reels including focused clipboard handoff and sheet restoration, independent Share, optional session/Disconnect, audio/quality, progress/cancel/error and overlay lifecycle. Plan 0001 stays active. The temporary `io.github.ahmed9461.tapsave.probe` package remains installed; remove it when practical.

## Device test build

Source `e8fbf5888c4a8414462a514b1a457561aaf9b499`; API 36 artifact `10970244215`; APK **29,855,521 bytes**, SHA-256 `9b7ebc57a5c9e4caec72a5a24d2f684ba97a52ba82c3f3fd8ccbd249b1c4eafc`. APK, checksum, verification metadata and Arabic phone checklist are in task outputs. The verified CI debug signer differs from the installed phone build; Android cannot update it in place, and replacement installation clears app settings/session. No replacement was performed.
