# CR-0001 — Always-recorded Location Sessions

Status: Accepted / partially implemented UI-first

Last updated: 2026-05-16 13:52:51 +07:00

## Summary

This change request adds always-recorded location sessions and changes the GPS data model so both sessions and trips are based on a shared canonical location log.

This CR is the source of truth for why the change exists. After acceptance, its stable decisions are propagated into:

- `../product/product-baseline.md`
- `../architecture/navigation.md`
- `../architecture/data-model.md`
- `../ui/screen-specification.md`
- `../implementation/source-change-manifest.md`
- `../implementation/migration-plan.md`
- `../implementation/test-plan.md`

## Problem

The existing GPS tracking model primarily treats recorded points as trip-owned path data. A new requirement needs continuous location recording independent from trips, while preserving explicit trip records.

## Decision

Introduce always-recorded sessions and a canonical location log.

- Location points are stored once in an append-only location table.
- Sessions represent ON-to-OFF recording ranges.
- Trips represent explicit start-to-stop ranges over the same log.
- Sessions and trips are related by shared location rows, but they are distinct product concepts.

## Impact Matrix

| Area | Impacted? | Required change | Source-of-truth doc |
|---|---:|---|---|
| Product behavior | Yes | Sessions are not trips; trips become ranges | `product/product-baseline.md` |
| Navigation | Yes | Main nav becomes Session/List/Track/Settings | `architecture/navigation.md` |
| UI screens | Yes | Add Session screen; modify List and Track semantics | `ui/screen-specification.md` |
| Data model | Yes | Add `LocationEntity`, `SessionEntity`; update trips | `architecture/data-model.md` |
| Database migration | Yes | Convert legacy serialized trip paths into location rows | `implementation/migration-plan.md` |
| Services | Yes | Foreground location recorder supports always-recording | `architecture/app-architecture.md` |
| Sync | No | No direct sync behavior change in CR#1 | N/A |
| Auth | No | No auth behavior change in CR#1 | N/A |
| Tests | Yes | Migration, session, trip-boundary tests | `implementation/test-plan.md` |
| Source files | Yes | See manifest | `implementation/source-change-manifest.md` |

## User-facing changes

- Add `Session` as a first-class screen.
- Add always-recording switch in the List screen header.
- Track screen Start/Stop becomes trip-specific.
- Stop trip does not stop always-recording.
- Trip list continues to show trips only.
- Session list shows always-recorded sessions only.

## Behavior changes

- Turning always-recording ON starts a new session.
- Turning always-recording OFF closes the active session using the latest location point.
- If location permission is missing, turning ON requests permission before recording starts.
- If permission is denied, the switch remains OFF.
- Starting a trip while always-recording is OFF automatically starts always-recording.
- Starting a trip uses the next inserted location row as the trip start boundary.
- Stopping a trip uses the latest existing location row as the trip end boundary.
- If a trip stops before any location point is recorded for that trip, do not save the trip.
- Deleting a trip deletes only the trip row; location history remains intact.

## Navigation decision

Current accepted navigation after CR#1:

```text
Session / List / Track / Settings
```

Observer integration remains an open decision.

## Open questions

1. Should Observer become a fifth bottom-nav tab later?
2. Should `List` be renamed to `Trips` after CR#1?
3. Should Sessions eventually have detail maps or remain list-only?
4. Should always-recording switch state eventually survive process death?

---

# Original CR plan content

# TrackLocation Change Requests Plan

Last updated: 2026-05-14 20:18:18 +07:00

## Purpose

This document records change requests separately from rollout phases. Each CR captures the intent and product/data behavior that should remain stable even if implementation phases change later.

`docs/plans.md` remains the main rollout source of truth. This file is the chronological CR source of truth.

## CR#1 - Always-recorded location sessions

### Intent

Allow the app to continuously record location points when the always-recording switch is enabled, without treating those points as a trip by default. Trips become explicit ranges over the shared location log.

### Behavioral Decisions

- Add an always-recording switch to the List screen header, replacing the existing Export pill.
- When the switch is ON, the app records location points into a canonical location table.
- Always-recorded location points are not trips.
- Always-recorded sessions are ON-to-OFF ranges.
- When the switch turns ON, start a new always-recorded session.
- When the switch turns OFF, close the active always-recorded session using the latest recorded location point.
- Always-recording continues in the background through a foreground location service until the user turns it OFF.
- The switch state is session-only and does not persist after process death.
- If location permission is missing, turning the List-screen switch ON requests permission on the List screen and only starts recording after permission is granted.

### Trip Recording Changes

- Trips reference the canonical location table instead of owning their own serialized path as the primary model.
- Each trip stores `startLocationId` and `endLocationId`.
- Location IDs are expected to be generated incrementally and used as inclusive range boundaries.
- Only one trip can be active at a time.
- If always-recording is OFF and the user starts a trip, the app automatically starts a new always-recorded session.
- Starting a trip uses the next inserted location row as the trip start boundary.
- Stopping a trip uses the latest existing location row as the trip end boundary.
- If the user stops a trip before any location point is recorded for that trip, do not save the trip.
- Stopping a trip does not stop always-recording.
- Deleting a trip deletes only the trip row; location history remains intact.

### Data Model Direction

- Add a `LocationEntity` table as the append-only canonical location log.
- Add a `SessionEntity` table for always-recorded sessions.
- Keep trip summary fields such as timestamp, distance, and duration for fast list rendering.
- Update trip storage to reference location ranges by `startLocationId` and `endLocationId`.
- Details screens should resolve trip path points by querying the location range.
- Session screens should resolve session metrics by querying or storing session summaries over the location range.

### Migration Direction

- Add an explicit Room migration from the existing schema.
- Preserve existing saved trips.
- Convert legacy serialized trip path points into generated location rows.
- Assign generated location timestamps by evenly distributing points across the old trip duration.
- Set each migrated trip's `startLocationId` and `endLocationId` from the generated location rows.

### Navigation And Scope

- Add a new Sessions screen to display always-recorded sessions.
- Bottom navigation order becomes:
  - Session
  - List
  - Track
  - Settings
- Raw non-trip location points do not need a separate point-by-point browser in this CR.

### Testing Expectations

- Test migration from legacy trip path strings into location rows and trip references.
- Test always-recording ON/OFF session creation and closing.
- Test trip start while always-recording is OFF automatically starts recording.
- Test trip start boundary uses the next inserted point.
- Test trip stop boundary uses the latest existing point.
- Test that empty trips are not saved.
- Test that trip deletion keeps location rows.
- Test that trip and session paths can be reconstructed from location ID ranges.



---

# Original CR screen specification content

# TrackLocation Change Requests Screen Specification

Last updated: 2026-05-14 20:18:18 +07:00

## Purpose

This document records UI and interaction requirements for change requests. It is separate from implementation planning so the screen design can be refined independently.

`docs/ui_screen_specification.md` remains the broader screen specification. This file records CR-specific screen changes.

## CR#1 - Always-recorded location sessions

### Navigation

Bottom navigation contains four primary destinations in this order:

- Session
- List
- Track
- Settings

The Session destination is a first-class screen, not a nested subpage under List.

### List Screen Header

Replace the existing `Export` pill in the List header with an always-recording switch.

Header content:

- Top label: `Trip Tracker`
- Main title: `Trips`
- Right-side control: always-recording switch

Switch behavior:

- OFF means no always-recorded session is currently active.
- ON means the foreground location recorder is active.
- If permission is missing, tapping ON triggers the location permission flow before recording starts.
- If permission is denied, the switch remains OFF.

Recommended switch text treatment:

- Keep the control compact in the header.
- Use a clear active/inactive visual state.
- Do not add a large explanatory card in this CR unless the compact header control proves unclear.

### Session Screen

Purpose:

Show a list of always-recorded location sessions. These sessions are not trips, but they represent periods where the app continuously recorded location points.

Primary content:

- Screen title: `Sessions`
- List of recorded sessions, newest first.
- Empty state when no sessions exist.

Each session row should show:

- Start time.
- Duration.
- Distance.

Optional row metadata if space allows:

- End time.
- Number of recorded points.
- Active status for the currently open session.

Empty state:

- Title: `No sessions recorded yet`
- Supporting text should explain that sessions appear when always-recording is turned ON from the Trips screen.

Active session state:

- If a session is currently active, show it at the top of the list.
- Mark it clearly as active.
- Duration and distance should update while recording if the app has current data available.

### Track Screen

The Track screen keeps the existing Start/Stop trip interaction, but its meaning changes:

- Start begins a trip range inside the canonical location log.
- If always-recording is not active, Start automatically starts it.
- Stop ends the trip range but does not stop always-recording.

UI implications:

- The Start/Stop control can remain visually similar to the current design.
- The running card should reflect trip state, not global always-recording state.
- Avoid implying that Stop disables background location recording.

Recommended copy adjustment:

- Use trip-specific wording such as `Start trip` and `Stop trip` where practical.
- Avoid generic `Start recording` on the Track screen because always-recording is controlled from the List header.

### Trip List And Details

Trip list behavior:

- Continue showing trips only.
- Do not show always-recorded sessions in the trip list.
- Trip metrics should remain based on the trip's location range.

Trip details behavior:

- Draw the trip path from location rows between `startLocationId` and `endLocationId`.
- Existing detail-map visual direction can remain unchanged.

### Permission Flow

Permission can be requested from the List screen when the user turns the switch ON.

Expected states:

- Permission not requested: switch is available; tapping ON starts request flow.
- Permission granted: switch turns ON and recording starts.
- Permission denied: switch remains OFF.
- Permission permanently denied: show existing permission/settings guidance if available.

### Out Of Scope For CR#1 UI

- Raw point-by-point location log browser.
- Session detail map.
- Session delete/edit actions.
- Persisted always-recording switch state after process death.
- Complex filtering/search for sessions.
