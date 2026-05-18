# TrackLocation Change Requests

This file is the chronological CR source of truth. Each accepted CR should also be propagated into `docs/product-spec.md` and `docs/implementation-plan.md`.

---

# CR-0001 — Always-recorded Location Sessions

## Status

Implemented in source as of 2026-05-16 18:30:39 +07:00.

Verification pending:

- Gradle build not run.
- Unit tests not run.
- Emulator/device verification not run.
- Project rule requires explicit user permission before heavy verification.

## Problem

The app originally treated recorded location data mainly as trip-owned data. The product needs continuous location recording independent from trips, while still allowing users to define explicit trips.

## Decision

Introduce always-recorded location sessions and a canonical location log.

Trips become explicit ranges over the canonical location log.

## User-facing Changes

- Add Session as a first-class bottom-nav destination.
- Use bottom navigation:

```text
Session / List / Track / Settings
```

- Show always-recorded sessions separately from trips.
- Keep trip list focused on explicit trips only.
- Track Start/Stop controls trip range only.
- Stopping a trip does not stop always-recording.

## Behavior Changes

- Turning always-recording ON starts a new always-recorded session.
- Turning always-recording OFF closes the active session.
- Always-recording records location points into the canonical location log.
- Starting a trip while always-recording is OFF automatically starts always-recording.
- Starting a trip uses the next inserted location row as the trip start boundary.
- Stopping a trip uses the latest existing location row as the trip end boundary.
- Empty trips are not saved.
- Deleting a trip does not delete location rows.

## Data Model Impact

Create:

- `LocationEntity`
- `SessionEntity`
- `LocationDao`
- `SessionDao`

Update:

- `TrackEntity` to add `startLocationId` and `endLocationId`.
- `TrackDatabase` to add entities and schema migration.
- `TrackDao` to support location-range-aware queries.

## Migration Impact

Room migration from version 1 to 2 converts legacy serialized trip paths into canonical location rows and trip boundaries.

## UI Impact

- Add Session screen.
- Add Session bottom-nav item.
- Make Session the app start tab.
- Update Track wording to trip-specific start/stop semantics.
- Trip detail path resolves from canonical location ranges.

## Code Implementation Status

Done according to latest progress log:

- canonical `location_log` rows
- `recording_session` rows
- DAOs for location ranges and session history
- Room v1→v2 migration
- `TrackingService` always-recording/session behavior
- Track control semantics
- trip detail path resolution
- real Sessions screen data connection

## Test / Verification Status

Not completed yet:

- build verification
- unit tests
- migration tests
- emulator/device verification

---

# CR-0002 — Session Always-recording Switch

## Status

Implemented in source as of 2026-05-16 21:05:00 +07:00.

Verification pending:

- Gradle build not run.
- Unit tests not run.
- Emulator/device verification not run.
- Project rule requires explicit user permission before heavy verification.

## Problem

After CR-0001, the Session screen shows always-recorded sessions and status, but the primary always-recording control was not located directly where the user inspects that status. The Session screen should let users control always-recording directly.

## Decision

Place a compact always-recording switch inside the Session screen always-recording status area.

The Session screen is the primary always-recording control surface.

The List screen remains trip-only and does not expose the always-recording switch.

## User-facing Changes

- Session screen shows an always-recording switch inside the always-recording status area.
- ON means always-recording is active.
- OFF means always-recording is inactive unless an active trip requires recording.
- Turning ON starts always-recording after permission is granted.
- Turning OFF stops always-recording and closes the active session only when no trip is active.
- List screen no longer exposes always-recording control.

## Behavior Changes

- Session switch controls the same always-recording state introduced in CR-0001.
- Starting a trip while always-recording is OFF auto-starts always-recording.
- Auto-start updates the Session switch to ON.
- Stopping a trip does not stop always-recording.
- Active trip prevents turning always-recording OFF.
- Manual ON and trip-triggered ON are not visually treated as different states.

## UI Specification

Affected screen:

- Session

Component:

- Always-recording switch

Placement:

- Inside Session always-recording status area.
- Visually connected to the current status label/card.
- Right-side/trailing placement preferred.
- Do not add a new nav item.
- Do not move/remove sessions list.
- Do not place always-recording switch on List.

Required states:

| State | Required UI |
|---|---|
| OFF / inactive | Switch OFF; status `Inactive`; sessions list visible |
| ON / active | Switch ON; status `Active`; active session appears if available |
| Permission required | Turning ON triggers permission flow |
| Permission denied | Switch remains/returns OFF; show helper/snackbar |
| Active trip guard | Switch remains ON; OFF blocked; explain recording is required |
| Auto-started by trip | Switch ON; status Active; no separate visual state required |

Recommended copy:

- Title: `Always-recording`
- ON status: `Active`
- OFF status: `Inactive`
- ON helper: `Recording location sessions in the background.`
- OFF helper: `Location sessions are not being recorded.`
- Permission helper: `Location permission is required to start always-recording.`
- Guard helper: `Always-recording is required while a trip is running.`

## Source Impact

Edited:

- `SessionsScreen.kt`
- `ListScreen.kt`
- `ListContent.kt`
- `docs/change-requests/CR-0002-session-always-recording-switch-ui.md`
- `docs/change-requests/CR-0002-session-always-recording-switch.md`
- `docs/ui/screen-specification.md`
- `docs/implementation/source-change-manifest.md`
- `docs/status/status_report.txt`

In the simplified docs, this impact is represented in `docs/implementation-plan.md` and `docs/progress.md`.

## Out of Scope

- New navigation item.
- Rewriting CR-0001.
- New data model or Room migration.
- Observer, Accessibility, auth, or sync behavior.
- Persisting always-recording switch state after process death beyond CR-0001 behavior.

---

# Template for Next CR

```md
# CR-000X — Short Title

## Status
Proposed / Accepted / In Progress / Implemented / Superseded

## Problem

## Decision

## User-facing Changes

## Behavior Changes

## Impact Matrix

| Area | Impacted? | Required change |
|---|---:|---|
| Product behavior | Yes/No | |
| Navigation | Yes/No | |
| UI/screens | Yes/No | |
| Data model | Yes/No | |
| Database migration | Yes/No | |
| Services/workers | Yes/No | |
| Sync/backend | Yes/No | |
| Auth/security | Yes/No | |
| Tests | Yes/No | |
| Code implementation | Yes/No | |
| UI/design handoff | Yes/No | |

## Code Implementation Work

## UI Specification / Design Handoff Work

## Tests / Verification

## Out of Scope

## Open Questions
```
