# Test Plan

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

This file records tests required by accepted product behavior. It should be updated before implementation and checked after implementation.

# CR-0001 — Always-recorded Location Sessions

## Unit tests

| Area | Required tests |
|---|---|
| Migration | Legacy path strings convert into location rows and trip boundaries |
| Always-recording | ON creates active session; OFF closes session |
| Trip start | Starts always-recording if needed; start boundary uses next inserted point |
| Trip stop | End boundary uses latest existing point; stopping does not stop always-recording |
| Empty trip | Trip is not saved if no point was recorded for it |
| Trip delete | Trip row deleted; location rows remain |
| Session metrics | Duration, distance, and point count are computed or displayed correctly |

## UI/manual tests

| Screen | Required checks |
|---|---|
| Session | Empty state, active session row, history rows |
| List | Always-recording switch ON/OFF, permission denied keeps OFF |
| Track | Copy says Start trip/Stop trip; Stop does not imply global recording stops |
| Trip detail | Path renders from location range |
| Bottom nav | Session/List/Track/Settings order is stable |

# Observer planned tests

Use the phase-level tests from `implementation/rollout-plan.md`. Before implementation, expand them into concrete test files in `source-change-manifest.md`.

Minimum planned Observer test areas:

- Denylist filtering.
- Searchable text generation.
- Snapshot node cap and size trimming.
- DAO insert/get and cursor pagination.
- FTS5 query composition.
- Sync trigger, retry, and retention cleanup.
- Auth state transitions and face failure budget.
- Registration state transitions.
- Real-device accessibility capture.
