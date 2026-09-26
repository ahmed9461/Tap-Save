# Codex Start Prompt — Tap Save

Continue work on the repository `ahmed9461/Tap-Save`.

Do not re-explain the project and do not copy repository documentation into your response. Treat the repository as the source of truth.

Before making changes, read in order:

1. `AGENTS.md`
2. `PROJECT_MEMORY.md`
3. `PROJECT_STATUS.md`
4. `docs/PRODUCT_SPEC.md`
5. `ROADMAP.md`
6. `docs/DECISIONS.md`
7. `docs/BRAND_IDENTITY.md`
8. the active plan
9. the latest relevant entries in `docs/PROGRESS_LOG.md`

Then inspect the repository, Git state, recent commits, and current toolchain.

The active plan is:

`plans/0001-foundation-and-instagram-spike.md`

Work from that plan. Do not create another plan until it is completed.

Start with the Android foundation and the technical spike, not polished UI. Before locking any architecture, critically compare the simplest reliable native Android approach for the floating control, foreground-app awareness, Share → Tap Save fallback, current-Reel identification, background download lifecycle, and local media storage.

Important constraints:

- Android only.
- Personal-use app.
- Kotlin/native Android preferred.
- Instagram Reels first.
- Keep dependencies and background work minimal.
- The ideal experience is browse → tap ↓ → progress → ✓ → keep browsing.
- Share → Tap Save must remain a reliable fallback.
- Do not make AccessibilityService a default dependency unless the spike proves less invasive approaches are insufficient.
- Keep Instagram-specific logic replaceable.
- Do not implement DRM/private-account/authentication/access-control bypasses.
- Do not add ads, analytics, accounts, cloud sync, or unrelated features.
- Use current stable dependency/tool versions verified from primary documentation rather than guessing version numbers.
- Test each risky integration instead of assuming it works.
- After every completed milestone, update the relevant project memory/status/decision/progress/active-plan files.
- Review your own diff critically before moving to the next step and simplify anything that is unnecessarily complex.

Begin now with the first unfinished step of the active plan and continue as far as can be safely verified in the current environment.
