# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation
**Status:** Owner reports some successful public Reel saves on the S22 Ultra. Version 0.3 adds opt-in current-Reel acquisition and optional local Instagram session fallbacks; build/lint, API 29/35/36 integrations and three anonymous live saves passed at `411ee70`. Real Instagram Arabic UI and authenticated-account acceptance remain open.
**Active plan:** `plans/0001-foundation-and-instagram-spike.md`
**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Implemented

- One shipping Kotlin/Compose application module (plus a separate disposable UI fixture) with a pinned, verified stable toolchain and build/test/lint CI.
- Defensive text share receiver: canonical Reel target, distinct short share link, invalid/ambiguous input recovery. No optional permissions required for Share.
- Native movable overlay experiment with persisted position, explicit foreground session and Stop controls. Optional usage access supplies approximate app context. An opt-in, Instagram-only accessibility adapter now attempts Share → Copy link on a tap; no idle tree reads.
- JVM regression tests and API 29/35/36 instrumentation coverage for the risky entry points/lifecycle; evidence is tracked in `docs/TECHNICAL_SPIKE.md`.

## Verification and limitations

- Version 0.3 clean-checkout build, strict lint, **18 JVM + 39 instrumentation tests per API 29/35/36 job** passed at `411ee70` in CI `36354556280`, with zero failures/errors/skips. Coverage includes actual floating-button activation on the Arabic fixture through HTTP/MediaStore, English acquisition, stale/ambiguous/other-app rejection, session cookie boundaries/clear, resolver fallbacks, expiry refresh, cancellation and lifecycle/storage recovery.
- Owner confirmed Usage Access, overlay visibility in Instagram and receipt of `Dc_WBLAuR7M` on the S22 Ultra / Android 16. Drag/rotation, hide/show transitions, lock, OEM interruption remain unverified; owner reports some actual public phone saves, with failures on other public Reels.
- Native Share save pipeline: public page/embed/post-permalink metadata, HTTPS transfer, progress/cancellation, pending MediaStore publication, one active job and retry/recovery. Short share links now resolve before media identity/storage allocation; direct acquisition is implemented but not yet accepted against the production Instagram UI.
- Live CI `36354557958` at `411ee70` saved **DGOSAUyC903, Cop84x6u7CP and CDUMkliABpa** through the real Share activity/service on API 36, anonymously and without resolver/transport overrides. Each published output passed video/audio metadata, nonzero size/duration and frame decoding; tests removed only their own outputs. This is three-sample live Android evidence, not universal Instagram compatibility or Samsung acceptance.
- Native INTERNET/dataSync permissions added; no embedded extraction engine, cloud features or analytics. Optional Instagram login uses an isolated native WebView profile, with local session cookies and Disconnect/Clear.

## Next milestone

Test the verified 0.3 development APK on Samsung SM-S908U1 / Android 16, Instagram 448.0.0.52.84 in Arabic: direct ↓ on successive Reels, independent Share, public resolution, optional login fallback, quality/audio, cancellation/success/errors and Disconnect. Do not equate synthetic accessibility controls with Instagram compatibility. Plan 0001 remains active.

## Verified device build

Source `411ee70d79b48e779b0a06a6a7e03b292eee4d05`; live-job artifact `10943980658`; APK **29,709,819 bytes**, SHA-256 `155ab82f2376960a11d27d33e87a62d25f9b4788d626f796a8d9131e14524ce3`. No production dependency added. This CI debug signer differs from the previous delivered build; replacing that development installation clears its app settings/session. APK, checksum and phone checklist are in the task outputs.
