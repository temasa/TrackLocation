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
