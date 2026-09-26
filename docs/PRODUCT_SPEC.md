# Tap Save — Product Specification

## 1. Product statement

Tap Save is a private Android utility that reduces the friction of saving a video the user is already viewing in a supported app. The first supported experience is Instagram Reels.

The product should feel like a small Android capability layered onto normal browsing, not like a separate downloader application the user must repeatedly open.

## 2. Primary user flow

1. User opens Instagram.
2. User enters Reels.
3. Tap Save exposes a small movable download control.
4. User browses normally.
5. User taps the control on a reel they want to keep.
6. Tap Save resolves the intended media.
7. Download begins without opening a full-screen Tap Save activity.
8. The floating control shows a compact progress state.
9. On success it briefly shows completion.
10. The file is available locally through Android media storage.

## 3. Required fallback flow

If current-reel resolution cannot be performed safely/reliably:

1. User uses Instagram Share.
2. User selects Tap Save.
3. Tap Save receives the shared URL/text.
4. Download starts with minimal interaction.
5. User can return to Instagram immediately.

The fallback is part of the product, not an error-only afterthought.

## 4. Functional requirements

### Overlay
- Movable.
- Small touch target but still usable.
- Does not cover important Instagram controls by default.
- Remembers position.
- Appears only when relevant whenever feasible.
- States: idle, resolving, downloading/progress, success, retry/error.
- Repeated taps must not create duplicate jobs.

### Foreground context
- Detect Instagram foreground state using the least invasive reliable Android mechanism.
- Do not continuously poll at an aggressive frequency.
- Gracefully handle permission denial.

### Media identification
- Use a layered strategy.
- Prefer stable Android-provided/shared data.
- Avoid assuming Instagram view IDs/text will remain constant.
- Any Instagram-specific parsing must be isolated and covered by tests where practical.
- Never attempt to bypass private access, DRM, login barriers, or other access controls.

### Download
- Preserve useful quality.
- Include audio when the source provides it.
- Support cancellation.
- Survive normal UI/activity lifecycle changes.
- Provide progress.
- Use a bounded/reasonable retry policy.
- Avoid redownloading the same target accidentally.

### Storage
- Save through modern Android storage APIs.
- Default collection should be visible to common gallery/file apps.
- Proposed folder: `Movies/Tap Save/`.
- Generate safe filenames.
- Resolve name collisions.

### Privacy
- No analytics.
- No advertising SDK.
- No account system.
- No remote telemetry.
- No upload of browsing history.
- Store only what is needed for app function.

## 5. Non-functional requirements

- Fast startup.
- Low idle CPU.
- Low idle battery impact.
- Small dependency footprint.
- No background loop that wakes the device unnecessarily.
- No network activity unless resolving/downloading user-requested media.
- UI operations must not block the main thread.
- All long-running work must be cancellable or safely resumable where appropriate.

## 6. Technical direction

Preferred starting point:

- Kotlin.
- Jetpack Compose.
- Coroutines/Flow.
- A small local persistence layer only if actually required.
- MediaStore for final media.
- Android foreground service / WorkManager only according to the real duration and semantics of the work; choose after validation rather than using both by default.

Suggested boundaries:

- `ui/`
- `overlay/`
- `platform/`
- `platform/instagram/`
- `resolver/`
- `download/`
- `storage/`
- `data/`

Exact modules/packages should remain simple for a personal app; avoid premature multi-module architecture.

## 7. Success criteria for v1

- The app can save a supported Instagram Reel locally.
- Normal flow requires minimal interaction.
- A reliable Share fallback exists.
- No ads or external tracking.
- The app stays responsive during downloading.
- Common failures produce a clear retry path.
- Background behavior is controlled and battery-conscious.
- Project documentation reflects the shipped behavior.
