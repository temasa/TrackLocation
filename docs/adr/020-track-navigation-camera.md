# ADR-020: Track Map Navigation Camera Auto-activates During Trip or Order

**Status:** Accepted
**Date:** 2026-10-07
**Decided By:** Project owner

## Context

The Track screen displays a map with a blue-dot marker and follow-mode camera (ADR-018). Navigation sub-mode on a trip can draw a route to follow (ADR-009, planned). The camera currently remains north-up (bearing 0) and centered on the live location regardless of whether the user is actively navigating. 

Drivers using Google Maps, Waze, and similar navigation apps expect the map to automatically switch to a heading-up perspective (compass heading points upward, map follows the car) when navigation is active. This improves route awareness and reduces the need to manually rotate the map.

The project owner wants this auto-switching behavior while preserving the manual perspective toggle of ADR-009 (which remains a user-selectable control, not driven by trip state).

## Decision

1. **Auto-activation trigger:** The map camera automatically switches to **navigation perspective** (heading-up, follow-on, bearing = GPS heading, tilt = 0) whenever `navigationActive = (tripState != READY) || (activeOrder != null)` becomes true.

2. **Entry behavior:** On entry to navigation mode:
   - Follow is (re)enabled (any previous user pan is cleared).
   - Camera animates once to **zoom 17**; afterwards the user's own zoom changes (pinch/zoom) are respected and not reset.
   - Bearing = GPS heading (if available); tilt = 0 (heading-up, top-down).
   - Target = current live location fix.

3. **Heading validity:** Use the live-fix bearing only when the fix has a bearing AND speed ≥ ~3 km/h (GPS-reported speed, same threshold as ADR-009 directional-marker display). Below ~3 km/h, hold the last valid heading. If no valid heading exists yet (e.g., stationary at startup), remain north-up (bearing 0).

4. **Manual override:** Any user gesture on the map (pan or pinch) stops following and stops auto-rotation (same as ADR-018 / UI-SPEC §3f). The user's zoom is retained. Tapping the Recenter FAB resumes follow **with heading-up while navigationActive is true**, or north-up otherwise. The manual "navigation perspective" toggle of ADR-009 is independent and unaffected by this auto-activation.

5. **Exit behavior:** When navigationActive becomes false (trip ended or order cleared), the camera animates bearing back to 0 (north-up), keeps follow on, and preserves the current zoom.

6. **Display-only:** No change to recording state, the canonical location log, trips/sessions, schema, or route overlays (ADR-016/017). No new control/button; the perspective toggle of ADR-009 remains planned. Turn-by-turn guidance, car-marker rotation, and camera offset are out of scope. Compose 1.2 / maps-compose 2.5.3 compatible (CameraPosition bearing/tilt only).

7. **Rationale:** The Maps SDK has no built-in navigation mode. The Navigation SDK is commercial and turn-by-turn is out of scope (PRD §8). Tilt remains rejected (ADR-009 item 6). This change provides a UX improvement (auto heading-up on entry) without requiring a full navigation SDK.

## Consequences

### Positive
- **Familiar navigation feel:** Drivers recognize the heading-up perspective auto-activating when a trip/order starts, matching Google Maps/Waze behavior.
- **Reduced interaction:** Users need not manually toggle perspective; the map switches on trip start.
- **Non-intrusive:** Manual toggle from ADR-009 is unaffected; users can still switch to north-up if preferred while a trip is active.
- **Zoom-sensible:** Zoom is set once on entry and thereafter respects user gestures; doesn't reset on every position update.
- **Display-only:** Recording, canonical log, and trip/session model are untouched (ADR-001/002 locked rules intact).

### Negative / Accepted Risks
- **Heading jitter below 3 km/h:** Holding the last heading below ~3 km/h means the map may not rotate smoothly in slow/tight maneuvers. Acceptable because drivers do not rely on heading at crawling speeds (ARG).
- **No fallback heading:** If the fix never provides a bearing (e.g., GPS-weak location), the map stays north-up. Acceptable (same as ADR-009 directional-marker behavior).
- **Zoom-17 assumption:** The initial zoom level is hard-coded at 17. If the user prefers a wider view, they must pinch out (and the zoom is then preserved). No auto-adjustment. Acceptable for predictability.

## Alternatives Considered

1. **Manual toggle only (current):** Leaves the camera in north-up mode and requires the user to tap the perspective-toggle button to switch. Rejected: poor UX for navigation (adds friction).

2. **Always heading-up while trip is live (considered):** Would override the manual toggle completely. Rejected: conflicts with ADR-009 design (manual toggle is a separate control for times when navigation is not active).

3. **Auto-activation + auto-deactivation on manual toggle (considered):** Pressing the toggle while a trip is live would turn off heading-up and resume north-up. Rejected: confusing (user's manual toggle would be overridden when the trip ends, fighting user intent).

4. **Auto-activation via external Navigation SDK (rejected):** Turn-by-turn and feature parity with Google Maps would require the commercial Navigation SDK. Out of scope (PRD §8). This ADR is for heading-up only.

5. **Tilt + perspective (rejected):** Adding tilt to the camera (ADR-009 item 6) is rejected; this ADR uses tilt = 0 only.

## Related ADRs

- **ADR-009** (Dual-mode Track Navigation) — the manual perspective toggle (not driven by trip state; this ADR adds auto-activation).
- **ADR-018** (Track Screen Live Location) — provides the bearing / heading data and live-fix prerequisites.
- **ADR-015** (Gojek Order Lifecycle Drives the Trip) — orders auto-start a trip, which triggers auto-activation.

## References

- CameraPosition + CameraUpdateFactory (Google Maps SDK for Android): bearing, tilt, target, zoom.
- ADR-009 (perspective toggle details, directional marker threshold ≥ ~3 km/h).
- ADR-018 (LiveLocationSource, speed/bearing validity).
- UI-SPEC §3f (follow mode and Recenter behavior).
- UI-SPEC §3h (this feature's UI spec).
- PRD §8 (out-of-scope: full turn-by-turn, commercial Navigation SDK).
