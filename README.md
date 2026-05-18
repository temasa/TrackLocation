# TrackLocation Simplified Documentation

Updated from latest uploaded docs: 2026-05-16 21:05:00 +07:00

This package simplifies the active documentation into four living docs plus AI-agent instructions.

## Active files

```text
AGENTS.md
CLAUDE.md
docs/product-spec.md
docs/change-requests.md
docs/implementation-plan.md
docs/progress.md
```

## Purpose of each file

| File | Purpose |
|---|---|
| `docs/product-spec.md` | Current accepted product, navigation, UI, data, and planned Observer behavior |
| `docs/change-requests.md` | Chronological CR history and accepted behavioral changes |
| `docs/implementation-plan.md` | Two-track implementation plan: code handoff and UI/design handoff |
| `docs/progress.md` | What has been done, current status, verification state, and next steps |
| `AGENTS.md` | Stable rules for Codex, Claude Code, Cursor, Windsurf, and other coding agents |
| `CLAUDE.md` | Claude Code wrapper that imports `AGENTS.md` |

## Archived original upload

The latest uploaded detailed docs are preserved under:

```text
docs/archive/latest-upload/
```

Do not treat archived files as active source of truth unless explicitly needed for audit/comparison.

## Current accepted baseline

The current app baseline is:

```text
Session / List / Track / Settings
```

Current accepted CRs:

- CR-0001: Always-recorded Location Sessions.
- CR-0002: Session always-recording switch as the single control surface.

## Latest implementation state

- CR-0001 has been implemented in source as of 2026-05-16 18:30:39 +07:00.
- CR-0002 has been implemented in source as of 2026-05-16 21:05:00 +07:00.
- CR-0001 and CR-0002 have now been verified by the user and are working as expected.

## Recommended next action

Proceed to the next planned implementation after confirming the Observer navigation decision in `docs/implementation-plan.md`.
