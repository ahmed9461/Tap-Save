# AGENTS.md

## Purpose

This repository is built with AI-assisted development. Agents must work from the repository state, not from assumptions or chat history.

## Required reading order

Before changing code, read:

1. `PROJECT_MEMORY.md`
2. `PROJECT_STATUS.md`
3. `docs/PRODUCT_SPEC.md`
4. `ROADMAP.md`
5. `docs/DECISIONS.md`
6. the current active plan under `plans/`
7. the latest relevant entries in `docs/PROGRESS_LOG.md`

Then inspect the current Git state, recent commits, changed files, build configuration, and tests.

## Working rules

- The repository is the final source of truth.
- Do not repeat completed work.
- Do not create a new plan while an active plan still covers the work.
- Before a meaningful implementation step, challenge the proposed approach and check whether a simpler, safer, more reliable Android-native solution exists.
- Prefer small, testable changes over large rewrites.
- Keep the app lightweight. Do not add libraries without a concrete reason.
- Prefer Android/Kotlin native APIs when they are adequate.
- Keep platform-specific extraction/downloading logic behind interfaces so Instagram changes do not force a rewrite of the app.
- Do not hard-code secrets, session cookies, credentials, or personal account data.
- Do not build mechanisms to bypass DRM, private-account access, paywalls, authentication restrictions, or other access controls.
- Treat AccessibilityService as an optional, narrowly scoped technique rather than a default dependency. Prefer less invasive Android mechanisms whenever possible.
- Any persistent background work must have a clear product reason and comply with Android background-execution rules.
- Do not collect analytics or transmit browsing activity unless explicitly added later.
- Use defensive error handling and cancel work cleanly when the user leaves the relevant flow.
- Keep UI responsive during download/extraction.
- Avoid duplicate downloads.
- Verify behavior on a real Android device whenever the change depends on overlays, shares, accessibility, foreground services, notifications, or media storage.

## Documentation discipline

After each completed milestone:

- update `PROJECT_STATUS.md`;
- append a concise factual entry to `docs/PROGRESS_LOG.md`;
- record durable architecture/product decisions in `docs/DECISIONS.md`;
- update `PROJECT_MEMORY.md` only with stable information that future agents need;
- update the active plan with completed and next steps.

Do not fill documentation with copied chat text or verbose diaries.

## Quality gate

Before calling a phase complete:

1. Build succeeds.
2. Relevant tests pass.
3. No obvious lifecycle/background-service leak exists.
4. Failure states have usable UX.
5. Documentation matches the actual implementation.
6. Re-read the diff critically and simplify anything unnecessarily complex.
