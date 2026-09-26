# ROADMAP

## Phase 0 — Foundation and technical spike

Goal: prove the risky Android/Instagram integration pieces before building the polished app.

- Establish Android project structure.
- Validate overlay permission and movable floating control.
- Validate detection of Instagram foreground state with the least invasive reliable mechanism.
- Implement Android share target for Instagram links.
- Compare current-Reel identification approaches.
- Compare media resolution/download engine options.
- Validate MediaStore saving and reliable progress/cancellation.
- Record findings and choose the smallest viable architecture.

Exit condition: a development build can reliably accept an Instagram Reel through at least the share fallback and save it locally, while the overlay lifecycle is proven.

## Phase 1 — Core Tap Save flow

- Refine floating button.
- Connect current-target identification to resolver/downloader.
- Show progress in compact overlay state.
- Add success/failure/retry states.
- Prevent duplicate downloads.
- Keep browsing uninterrupted.

Exit condition: the normal happy path feels like one action from Instagram to a saved local video.

## Phase 2 — Reliability

- Handle app/process recreation.
- Handle network loss and retry.
- Handle invalid/expired links.
- Handle storage errors.
- Add bounded queue behavior.
- Add download history only if it improves recovery/usefulness.
- Test rapid repeated taps and reel switching.

Exit condition: no lost state or confusing duplicate work under common failure scenarios.

## Phase 3 — UX and device polish

- Refine onboarding for required permissions.
- Make overlay position persistent.
- Add size/opacity/location preferences only if useful.
- Optimize battery/memory behavior.
- Verify Samsung and at least one non-Samsung Android environment if available.
- Finalize app icon and visual identity.

## Phase 4 — Personal release

- Signed release build.
- Minimal release notes.
- Reproducible build instructions.
- Backup/export any local configuration only if needed.
- Final security/privacy review.

## Later, only after Instagram is stable

- Consider additional platforms through the same adapter architecture.
- Add only one platform at a time.
- Do not expand scope at the cost of the simple one-tap experience.
