# ADR-009: Dual-mode Track Screen — Follow-a-Route Navigation as a Sub-mode of a Trip

**Status:** Accepted
**Date:** 2026-07-08
**Decided By:** Claude Code (grilling session) + project owner

## Context

The Track screen (`screens/track/TrackScreen.kt`) is today a live location tracker: a full-screen Google map (`TrackMap`) plus a floating `TripPanel` whose CTA drives the trip state machine. The project owner wants the same screen to also function as **navigation** when the user inputs a route, without disturbing the locked model (PRD §12: canonical location log is source-of-truth; sessions vs trips separation; the always-recording switch lives on the Session screen; AGENTS §2/§3 forbid the assistant from inventing this scope). Full turn-by-turn is explicitly out of scope. This ADR records the product+behavior decision reached in a grilling session; it is a new product capability and must be recorded before code.

**Note (2026-10-07, ADR-019):** The trip state machine wording "READY → LIVE → PAUSED" in the Context and Alternatives sections is historical. The PAUSED state was removed per ADR-019; the live implementation uses Start/Stop only (READY and LIVE states only).

## Decision

**Framing.** Navigation is a **sub-mode of a trip** — there is no navigation without a trip. The Track screen's Start control offers two paths: **Start trip (track only)** or **Start trip + navigate**. Chosen fidelity = **a route polyline to follow** (route line + remaining distance + ETA), NOT turn-by-turn maneuver guidance.

**Destination input.** Place search with **autocomplete** (Places SDK for Android) → route computed by a routing engine (engine choice is an OPEN question, below).

**Lifecycle (single, coupled).**
- "Start trip + navigate" fires the existing `START_TRIP` action, which already cascades into always-recording/session (`TrackingService.startTrip()` calls `startAlwaysRecording()` when off).
- Navigation and its trip share ONE lifecycle: **ending navigation (manual cancel) ends the trip** (`STOP_TRIP`, which persists the finished trip to the List).
- Ending a trip does **NOT** turn off always-recording — that switch remains owned by the Session screen (PRD §12 / ADR-002 unchanged).
- **No auto-arrival detection** — the user always ends manually.
- **Destination is mutable mid-trip** — re-searching a new place swaps the target/route in place without ending the trip.

**Off-route handling.** Auto-recalculate when the driver deviates: **~50 m threshold, gated by 2–3 consecutive off-route GPS fixes, with a ≥15 s minimum between recalculation calls** (rejects single noisy fixes; caps worst-case API spend).

**Resilience.**
- Process death mid-navigation resumes **both** trip and navigation: the destination is persisted and the route auto-recomputes from the new position on reopen. Process death is NOT treated as "ending navigation," so the trip survives regardless.
- Routing failures **fail soft** — the trip always starts and records track-only; on initial-route failure show a toast and keep recording; recalc failure keeps the last drawn route; resume failure retries quietly then falls back to track-only. A network failure must never cost trip data (canonical-log invariant).

**On-screen display.** Remaining distance + ETA shown alongside the existing live trip metrics.

**"Navigation perspective" map view.** A user-selectable **view toggle** (new control in `MapControls`), fully **decoupled** from trip/navigation state — usable any time (idle, tracking, trip, navigating). ON = map rotates **heading-up** (direction of travel points up) and the camera **follows the car**, with **NO tilt** (stays top-down, just rotated). OFF (default) = north-up, top-down as today.

**Directional car marker.** Replace the static pin with a car/chevron marker that **rotates to the GPS heading**; shown whenever a valid heading exists; holds the last heading below ~3 km/h (GPS bearing is noise at rest). Requires surfacing `bearingDegrees` (already captured at `TrackingService`, persisted on `LocationEntity`) into `LocationUiState`.

## Open Questions (parked at owner's direction)

- **Routing engine:** **Resolved by ADR-011** — OpenRouteService (hosted free tier) via a connector-adapter, portable to self-hosted OSM by a base-URL swap. ETA is therefore static (no live traffic).
- **Traffic-aware vs static ETA:** **Resolved** — static ETA (OpenRouteService has no live traffic; ADR-011). Live traffic would require a future Google adapter.
- **Always-on "ghost road-ahead" via external API** and its ongoing cost — superseded in preference by the self-learning local route store (see ADR-010), which serves road-ahead from the user's own traces for free.
- **Google Maps Platform ToS grey area:** drawing third-party (OSM) routing lines on top of a Google basemap needs verification before shipping.

## Consequences

### Positive
- Delivers "input a route → follow it" while keeping the locked canonical-log / sessions / always-recording model untouched — navigation is additive.
- One coupled trip↔navigation lifecycle is simple to reason about and to verify.
- The heading-up view + directional marker give a familiar navigation feel with minimal, Compose-1.2-safe camera changes (bearing only, no tilt).

### Negative
- New paid API surface (Places autocomplete + routing) unless offset by ADR-010 / a free engine.
- Auto-recalc edges toward turn-by-turn behavior and needs the guardrails above to bound cost/battery.
- New UI (search field, Start-control shape, remaining-distance/ETA placement, perspective toggle, car marker) requires a design handoff first (AGENTS §12) — not invented in code.
- A persisted destination field and resume logic are needed for process-death recovery.

## Alternatives Considered
1. **Full turn-by-turn navigation (maneuver prompts, voice, auto-reroute):** rejected — very large build, fights the Compose-1.2 / resource-constrained setup.
2. **Destination marker + straight-line bearing only:** rejected — too crude to read as "navigation."
3. **Navigation independent of trips (four-combo: trip-only / nav-only / both / neither):** rejected in favor of navigation-as-sub-mode-of-a-trip (one lifecycle owner).
4. **Coupling navigation to also toggle always-recording off on end:** rejected — the always-recording switch is locked to the Session screen (ADR-002).
5. **Making Track the app landing screen / reordering the bottom nav:** raised then parked — a separate, smaller IA decision, not part of this feature.
6. **Camera tilt / 3D perspective:** rejected by owner — heading-up + follow only, no tilt.

## Related ADRs
- [[001-always-recorded-sessions]] (canonical log source-of-truth this must not disturb)
- [[002-session-recording-switch]] (always-recording ownership unchanged)
- [[010-self-learning-route-store]] (free road-ahead / routing alternative feeding this feature)

## References
- PRD §12 (locked business rules), AGENTS §2/§3/§12.
- Code: `screens/track/TrackScreen.kt`, `screens/track/components/TrackMap.kt`, `viewmodel/ShareViewModel.kt` (`onTripCtaTap`), `tracking/TrackingService.kt` (`startTrip`/`stopTrip`, `bearingDegrees`), `tracking/LocationUiState.kt`, `navigation/BottomNavigationScreen.kt`.
