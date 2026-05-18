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

