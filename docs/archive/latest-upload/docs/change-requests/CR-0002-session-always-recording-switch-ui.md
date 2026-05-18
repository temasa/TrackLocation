# CR-0002 UI Specification — Session Always-recording Switch

## Status
Documented / ready for design handoff.

## Related CR
Reference CR-0002 behavior/product document: [`CR-0002-session-always-recording-switch.md`](./CR-0002-session-always-recording-switch.md).

## Purpose
Explain that the Session screen should let the user control always-recording directly from the same area where always-recording status is displayed.

## Design Reference
Use the provided Stitch reference screens as the visual target for layout, spacing, control density, and status-card treatment:

- `screenweb application/stitch/projects/4372301068817999806/screens/39f014e484e844e6b1626d3158ec144b`
- `screenweb application/stitch/projects/4372301068817999806/screens/02b3e9041a5548fa8af0075b7b2a080c`

The references should guide visual composition and hierarchy, but the accepted CR-0002 behavior rules still apply.

## Target Screen
Session screen.

## UI Change Summary
Add an always-recording switch inside the always-recording status area.

## Component
Always-recording switch.

## Placement
- Place the switch inside the Session screen always-recording status area.
- The switch should be visually connected to the current always-recording status label.
- Prefer a trailing/right-side switch placement if the existing layout supports it.
- Do not create a separate settings section.
- Do not create a new navigation destination.
- Do not remove or hide the sessions list.
- Do not place an always-recording switch on the List screen.

## Recommended Layout
Status card / status area:
- Title: Always-recording
- Status label:
  - Active when ON
  - Inactive when OFF
- Short helper text:
- OFF: Location sessions are not being recorded.
- ON: Recording location sessions in the background.
- The List screen remains trip-only and does not expose this control.
- Trailing switch:
  - OFF = inactive
  - ON = active

## States

### 1. OFF / Inactive
- Switch is OFF.
- Status text: Inactive.
- Helper text: Location sessions are not being recorded.
- Session history remains visible.
- Empty state remains visible if no sessions exist.

### 2. ON / Active
- Switch is ON.
- Status text: Active.
- Helper text: Recording location sessions in the background.
- If an active session exists, show it at the top of the sessions list.
- Active session should be clearly marked.

### 3. Permission Required
- User attempts to turn switch ON.
- Existing location permission flow is triggered.
- UI should communicate that location permission is needed to start always-recording.
- Do not mark the switch ON until permission is granted.

### 4. Permission Denied
- Switch remains OFF.
- Status remains Inactive.
- Show short non-blocking helper text or snackbar:
  Location permission is required to start always-recording.

### 5. Active Trip Guard
- If a trip is currently active, always-recording must stay ON.
- The switch should remain ON.
- If the user tries to turn it OFF, block the action.
- Recommended feedback:
  Always-recording is required while a trip is running.
- Do not allow UI copy to imply that stopping a trip will stop always-recording.

### 6. Auto-started By Trip
- If the user starts a trip while always-recording is OFF, always-recording starts automatically.
- The Session screen switch must show ON.
- Status must show Active.
- Do not visually distinguish manual ON from trip-triggered ON unless needed for clarity.

## Interaction Rules
- Tapping switch ON starts always-recording only after permission is granted.
- Tapping switch OFF stops always-recording and closes the active always-recorded session only when no trip is active.
- Starting a trip automatically turns this switch ON.
- Stopping a trip does not turn this switch OFF.
- All always-recording controls must share one source of truth.

## Copy Guidance
Use concise operational copy.

Recommended labels:
- Title: Always-recording
- ON status: Active
- OFF status: Inactive
- ON helper: Recording location sessions in the background.
- OFF helper: Location sessions are not being recorded.
- Permission helper: Location permission is required to start always-recording.
- Active trip guard: Always-recording is required while a trip is running.

Avoid:
- "Start recording" on the Session screen if it can be confused with starting a trip.
- "Stop recording" if it can be confused with stopping a trip.
- Long explanatory paragraphs.

## Visual Guidance
- Use existing TrackLocation Material 3 visual language.
- Match the Stitch references with a compact, card-like status area that feels operational, not promotional.
- Keep the switch visually anchored to the status label and helper text.
- Use a trailing/right-side switch when the layout allows it.
- Keep the Session list visible and primary.
- Use status text plus switch state; do not rely on color alone.
- Avoid marketing-style cards, decorative gradients, or oversized hero sections.
- The status area should read like a control panel with clear state, concise copy, and minimal ornament.

## Accessibility Requirements
- Switch touch target must be at least 48dp.
- Switch must have a clear content description, for example:
  - Always-recording active
  - Always-recording inactive
- Status must be communicated with text, not color alone.
- Guarded/blocked OFF action must provide readable feedback.
- Layout must work on small Android phone widths.
- Ensure the referenced design remains legible in both light and dark themes.

## What Must Not Change
- Do not remove the Session screen.
- Do not remove the sessions list.
- Do not change bottom navigation.
- Do not merge sessions into trips.
- Do not change the behavior where starting a trip automatically activates always-recording.
- Do not make stopping a trip stop always-recording.

## Design Deliverables For Google Stitch / Claude Design
Generate or update Session screen designs for:
1. OFF / inactive state
2. ON / active state with active session visible
3. Permission-required state
4. Permission-denied state
5. Active-trip guarded state
6. Auto-started-by-trip state

For each state, include:
- Android mobile layout
- Material 3 styling
- component annotations
- status text
- switch placement
- helper/snackbar copy where relevant
- light and dark theme if supported

Acceptance criteria:
- The UI CR file exists under `docs/change-requests/`.
- The file clearly specifies switch placement inside the Session screen always-recording status area.
- The file clearly defines ON, OFF, permission, denied, active-trip guard, and auto-started-by-trip states.
- The file is directly usable by Google Stitch or Claude Design.
- `docs/ui/screen-specification.md` references this UI CR file if appropriate.
- `docs/status/status_report.txt` and `docs/status/progress.md` are updated.
- No source code is changed.
