# PROJECT_STATUS

## Current state

**Phase:** Foundation / technical validation  
**Status:** Planning initialized; implementation has not started.  
**Active plan:** `plans/0001-foundation-and-instagram-spike.md`

## Confirmed product choices

- Product name: Tap Save.
- Android-only.
- Personal use.
- Instagram Reels is the first platform.
- Main UX: floating one-tap save control while browsing.
- No ads, analytics, login system, or cloud dependency in the initial product.
- Share-to-Tap-Save is a required fallback.

## Not yet decided

These must be validated before locking the architecture:

- exact minimum Android version;
- most reliable way to identify the current Reel without creating a fragile dependency on Instagram UI internals;
- whether any AccessibilityService functionality is necessary at all;
- final media resolver/downloader implementation;
- whether an embedded yt-dlp-compatible engine is acceptable for device size, startup cost, and maintenance;
- foreground-service strategy for downloads on current Android versions;
- exact overlay behavior on Samsung/other OEMs.

## Next milestone

Complete the technical spike in the active plan and record evidence for:

1. overlay lifecycle;
2. foreground-app detection;
3. share receiver;
4. current Reel identification options;
5. direct download/resolution options;
6. media storage and background-download behavior.

Do not begin broad UI polishing before these risks are validated.
