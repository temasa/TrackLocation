# CR-0002 — Session always-recording switch

Status: Accepted / documentation-only specification

Last updated: 2026-05-16 20:01:12 +07:00

## Summary

Add a compact always-recording switch to the Session screen inside the always-recording status area. This is a visible control for the same always-recording state introduced in CR-0001, not a new setting, destination, or separate state source.

CR-0002 builds on CR-0001. It does not rewrite CR-0001 and does not change the accepted bottom navigation:

```text
Session / List / Track / Settings
```

## Problem

After CR-0001, the Session screen shows always-recorded sessions and recording status, but the primary always-recording control is outside the screen where users inspect that status. The Session screen should let users control always-recording directly where they see the current session state.

## Decision

Place an always-recording switch inside the Session screen always-recording status area.

- The Session screen switch controls the same always-recording state introduced in CR-0001.
- The switch is compact and status-oriented.
- The switch is visually connected to the current always-recording status label or card.
- The switch is not a large explanatory card unless an existing design pattern requires that treatment.
- The Session list remains visible and must not be moved or removed.
- No new bottom navigation item is added.

## Impact Matrix

| Area | Impacted? | Required change | Source-of-truth doc |
|---|---:|---|---|
| Product behavior | Yes | Session screen can start/stop always-recording through shared state | `product/product-baseline.md` |
| Navigation | No | Keep Session/List/Track/Settings | `architecture/navigation.md` |
| UI screens | Yes | Add switch inside Session always-recording status area | `ui/screen-specification.md` |
| Data model | No | No schema change | N/A |
| Database migration | No | No migration | N/A |
| Services | No new service | Existing foreground location recorder remains the controlled recorder | `architecture/app-architecture.md` |
| Sync | No | No direct sync behavior change | N/A |
| Auth | No | No auth behavior change | N/A |
| Tests | Yes | Add UI/state synchronization expectations | `implementation/test-plan.md` |
| Source files | Future implementation only | No source code changes in this documentation task | `implementation/source-change-manifest.md` when implementation begins |

## User-facing changes

- The Session screen shows an always-recording switch inside the always-recording status area.
- ON means always-recording is active.
- OFF means always-recording is inactive unless a trip is actively running and requires always-recording to stay active.
- The user can start always-recording directly from the Session screen when location permission is granted.
- If location permission is missing, turning the switch ON triggers the existing location permission flow.
- If permission is denied, the switch remains or returns OFF.

## Behavior changes

- Turning the Session switch ON starts or keeps active the foreground location recorder.
- Turning the Session switch OFF stops always-recording and closes the active always-recorded session, unless prevented by an active trip rule.
- If no trip is active, OFF closes the active always-recorded session using the existing CR-0001 session closing behavior.
- If a trip is active, do not allow turning always-recording OFF unless later product docs explicitly allow it.
- Prefer keeping the switch ON during a blocked OFF attempt and showing helper copy or a snackbar explaining that recording is required while a trip is active.
- Starting a trip while always-recording is OFF automatically starts always-recording.
- Auto-starting always-recording from trip start must update the Session switch to ON.
- Stopping a trip does not stop always-recording.

## Interaction with CR-0001

CR-0001 remains intact:

- Always-recorded sessions are separate from trips.
- The canonical location log remains the source of truth for recorded GPS points.
- Starting a new trip automatically activates always-recording if it is currently OFF.
- Stopping a trip does not stop always-recording.
- Deleting a trip must not delete canonical location history.
- The Session screen displays always-recorded sessions.
- Bottom navigation remains `Session / List / Track / Settings`.

CR-0002 only adds a Session-screen control for the existing always-recording state.

## State synchronization rules

- All always-recording controls share one underlying always-recording state.
- The Session switch and any other always-recording controls must display the same ON/OFF state.
- ON means the foreground location recorder is active and an always-recorded session is active or being started.
- OFF means no always-recorded session is currently active, except when a trip-active guard prevents deactivation.
- If the user turns ON from the Session screen, other always-recording controls must reflect ON.
- If the user turns OFF from any allowed control, all always-recording controls must reflect OFF after the active session closes.
- If the user starts a trip from Track while always-recording is OFF, always-recording starts automatically and all always-recording controls, including the Session switch, must reflect ON.
- Manual ON and trip-triggered ON must not be presented as different states.

## CR-0002 UI specification

Affected screen:

- Session

Component:

- Always-recording switch

Placement:

- Inside the always-recording status area.
- Visually connected to the current status label or card.
- Right-side or trailing placement is preferred when space allows.
- Do not add another top-level navigation item.
- Do not move or remove the Session list.

Recommended layout:

- Status area title: `Always-recording`
- Status value:
  - `Active` when switch is ON
  - `Inactive` when switch is OFF
- Trailing control: switch
- Supporting text:
  - OFF: `Location sessions are not being recorded.`
  - ON: `Recording location sessions in the background.`
  - Active trip guard: `Required while a trip is running.`

Interaction rules:

- User turns switch ON:
  - If location permission is granted, always-recording starts.
  - If location permission is missing, trigger the existing location permission flow.
  - If permission is denied, switch remains or returns OFF.
- User turns switch OFF:
  - If no trip is active, always-recording stops and the active session closes.
  - If a trip is active, keep the switch ON and explain that recording is required while a trip is active.
- User starts a trip from Track while always-recording is OFF:
  - Always-recording starts automatically.
  - Session screen switch shows ON.
- User stops a trip:
  - Trip stops.
  - Always-recording remains ON.
  - Session screen switch remains ON.

Visual guidance:

- Use existing Material 3 styling.
- Keep the control operational and compact.
- Use clear text status, not color alone.
- Ensure the switch meets Android touch target guidance.
- Do not create a marketing-style hero section.
- Do not duplicate the same control multiple times on the Session screen.
- Preserve existing CR-0001 Session screen visual direction.

States:

| State | Required UI |
|---|---|
| OFF / inactive | Switch OFF; status `Inactive`; Sessions list remains visible |
| ON / active | Switch ON; status `Active`; active session appears at top if available |
| Permission required | Switch attempt triggers permission; denied permission leaves switch OFF |
| Trip active | Switch ON; OFF interaction blocked or guarded; helper copy explains why |
| Auto-started by trip | Switch ON; status `Active`; no visual distinction from manual ON |

## Testing expectations

- Verify the Session screen switch appears inside the always-recording status area.
- Verify ON starts or keeps active the foreground location recorder when permission is granted.
- Verify OFF stops always-recording and closes the active session when no trip is active.
- Verify permission denial leaves or returns the switch OFF.
- Verify an active trip prevents turning always-recording OFF and explains why.
- Verify starting a trip while always-recording is OFF auto-starts always-recording.
- Verify trip-triggered auto-start updates the Session switch and all other always-recording controls to ON.
- Verify stopping a trip does not stop always-recording and does not turn the Session switch OFF.
- Verify all always-recording controls share one source of truth.

## Out of scope

- New navigation items.
- Rewriting CR-0001.
- Data model or Room migration changes.
- Persisting always-recording switch state after process death beyond CR-0001 behavior.
- Observer, Accessibility, auth, or sync behavior changes.
- Source code changes in this documentation-only task.

## Open questions

1. Should the List-screen switch remain long-term, or should the Session screen become the only always-recording control in a future CR?
2. Should always-recording state eventually survive process death?

## UI handoff

Design-ready Session screen UI guidance for this CR lives in [`CR-0002-session-always-recording-switch-ui.md`](./CR-0002-session-always-recording-switch-ui.md).
