# IDEAS

This file is a parking lot for useful ideas. Items here are not commitments and must not silently expand scope.

## Strong candidates after the core works

- Long-press the floating button to expose a tiny secondary menu.
- Optional “audio only” action.
- Optional preferred quality setting.
- Small recent-download list for retry/open/share.
- Brief haptic feedback on accepted tap.
- Progress ring around the floating button.
- Temporary hide/snooze overlay gesture.
- Auto-hide overlay when keyboard or sensitive system UI is visible.
- Remember overlay location separately for portrait/landscape.
- Optional download-complete notification with Open / Share.
- Detect an already-downloaded target and show `✓` instead of starting again.

## Reliability ideas

- Resolver chain with explicit result types rather than exceptions as control flow.
- Fingerprint/canonical ID per media target to deduplicate.
- Structured local diagnostic log that contains no sensitive content and can be exported manually for debugging.
- Feature flags for experimental Instagram adapters.
- Keep the Share receiver usable even if experimental direct detection is disabled.

## Future platform architecture

If more platforms are ever added, expose something conceptually similar to:

- `PlatformDetector`
- `SharedTargetParser`
- `MediaResolver`
- `DownloadEngine`

Do not create abstractions until the Instagram implementation proves what they actually need.

## Ideas intentionally deferred

- Built-in browser.
- Feed scraping.
- Automatic mass download.
- Cloud sync.
- Account login.
- Social feed.
- Video editor.
- Watermark remover.
- AI features.

These do not serve the initial one-tap personal utility goal.
