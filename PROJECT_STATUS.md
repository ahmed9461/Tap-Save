# PROJECT_STATUS

## Current state

**Status:** 2026-10-01 reliability repair in progress after renewed owner failure reports. Connected S22 has signed 0.5.0, all required permissions and a newly owner-connected local Instagram session. Earlier normal-use acceptance does not close this regression.

- Real floating-button acquisition and a session-backed save of `DcIAoWLNkXa` succeeded on the phone. Subsequent attempts reproduced `FRESH_REEL_LINK_MISSING` after accepted Copy actions.
- A separate focused diagnostic, leaving the share sheet open, observed a fresh different Reel link 538 ms after the copy probe started. The production sequence had sent Back about 190 ms after its Copy action. A repair now waits for a fresh focused clipboard result before closing its owned sheet; automated/signed/phone validation is pending.
- Candidate 0.5.1/code 6 adds bounded structural download diagnostics. No passwords, session cookies, response bodies or signed media URLs are logged. Existing owner media and app data are preserved. No new production dependency.

**Active plan:** `plans/0001-foundation-and-instagram-spike.md`

**Review:** Draft PR #1, branch `codex/foundation-instagram-spike`.

## Version 0.5

- All 112 translatable app strings have Arabic resources, including setup, Settings, Share, notifications, errors, login chrome and accessibility labels; setup counts cover Arabic plural forms. Brand/folder identity and diagnostic codes remain stable.
- Arabic follows device locale on API 29+, and Android 13+ offers a per-app language selector from Settings. Actual RTL Home/setup/Settings captures were reviewed. No dependency or custom font added.
- Non-debuggable release uses one protected RSA-3072 signing identity and an increasing version code from `app/version.properties`. Encrypted GitHub secrets and two protected owner-local copies retain the key. Only the public certificate/fingerprint is in Git. Never distribute ephemeral debug APKs as personal updates; see `docs/SIGNING.md`.
- Instagram acquisition, resolver, transfer and storage behavior accepted in 0.4 is unchanged apart from localized presentation.

## Verification

- Source `902ceae1fe65822958191c3732a88a368c3864bc`: [Android CI 36443683479](https://github.com/ahmed9461/Tap-Save/actions/runs/36443683479) passed build, strict lint, **19 JVM + 46 instrumentation tests on each API 29/35/36**, zero failures/errors/skips. Arabic resources/plurals passed across the matrix; native per-app locale/recreation and actual RTL UI were exercised on API 35/36.
- [Signed release CI 36443689015](https://github.com/ahmed9461/Tap-Save/actions/runs/36443689015) passed release assembly/lint, signature/package/version/non-debuggable checks, and two phases of the API 36 update test. A same-key baseline at version code 4 was replaced by code 5 using `adb install -r`; settings and exact app-owned video bytes survived. Baseline and candidate use the same source; this proves future same-key updates, not migration from the old unrelated debug key.
- The earlier live public samples and owner 0.4 acceptance remain recorded in `docs/TECHNICAL_SPIKE.md`; no new Instagram network/account acceptance is claimed for this localization/signing change.

## Delivery and remaining boundaries

Version 0.5.0, code 5, **23,211,148 bytes**, SHA-256 `e901ddc1c3de5d0db174c2acaa7b3b4ec3793018ca02770fc6897d98b73c2225`. Certificate SHA-256 `c047a8350f478a6dd72b4000b40a958dac992cbbeb3294a17e8a12bf0ed521ae`. APK/checksum/evidence, Arabic screenshots and installation notes are in task outputs; private key backup is separate and access-restricted.

The old 0.4 private CI key was not retained and cannot be recovered from its APK; its certificate differs from the new key. Moving to 0.5 requires one replacement installation with app settings/session reset. Later releases signed with this retained identity update in place. No owner-device install/uninstall/reset occurred during this task.

Normal owner use is accepted; detailed actual account-session benefit/clear, OEM interruption/force-stop and battery measurements remain separate unmeasured gates. Arabic visual acceptance on the S22 can be checked after installation. Plan 0001 stays active; no new plan was created. Temporary standalone probe removal remains deferred until the phone is available.
