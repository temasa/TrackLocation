# Handoff: Track Navigation (dual-mode) — follow-a-route

## Overview

The Track screen keeps live location tracking and gains a follow-a-route navigation mode (ADR-009), a decoupled heading-up "navigation perspective" map view, a directional car marker, and translucent road-ahead candidates from the local route store (ADR-010). Routing uses OpenRouteService via a connector-adapter (ADR-011); ETA is **static** (no live traffic). This handoff specifies the NEW UI surfaces to design; it does not change the tracking/trip data model.

## About the design files

No HTML/React prototypes. This references the existing TrackLocation Track screen (full-screen map + `TripPanel` + `MapControls`) and the "Kinetic Precision" design language. Design in Claude Design against the provided app screenshots.

## Fidelity

High-fidelity, matching the existing Track screen visual language (`TripPanel` card, `MapControls` FABs, Trip color tokens). **Compose 1.2 / Material 3 only** — no APIs newer than Compose 1.2 (no `ModalBottomSheet`; reuse existing sheet/dialog patterns).

## Where this feature lives

- **Surface:** Track screen (`screens/track/TrackScreen.kt`), on top of the full-screen `GoogleMap`.
- **Entry:** destination search + Start control on the Track screen; the perspective toggle in `MapControls`.

## Screens / views (surfaces to design)

### 1. Destination search + autocomplete
- Entry affordance on the Track screen (e.g. a search pill/bar) that opens place-search with autocomplete.
- Autocomplete results list (place name + secondary address line).
- States: empty (no query), typing (results), no-results, selected. Collapses after selection so it doesn't obscure the live map.

### 2. Start control — track-only vs track + navigate
- The existing `TripPanel` CTA drives trip start/stop. Extend it so starting a trip offers two paths: "Start trip" (track only) and "Start trip + navigate" (when a destination is set).
- Design how the two present (a primary CTA + secondary "+ navigate", or one CTA whose label reflects whether a destination is set). Reuse `TripPanel` READY/LIVE/PAUSED language.

### 3. Route line + read-out (navigating)
- Route polyline to the destination, visually distinct from the solid blue live path.
- Remaining distance + **static** ETA read-out placed alongside the existing `TripPanel` metrics; label ETA as approximate ("~18 min").
- A cancel/end affordance (ending navigation ends the trip per ADR-009) + a destination marker.

### 4. Navigation-perspective toggle (MapControls)
- New control in `MapControls` (alongside recenter + layers) toggling heading-up + follow ON/OFF; default OFF (north-up).
- Design active/inactive states consistent with the existing FAB styling. Decoupled from trip/nav state.

### 5. Directional car marker
- Replaces the static blue pin: a car/chevron marker that rotates to heading. Optionally distinct "moving" vs "at rest (last heading)" appearance.

### 6. Road-ahead candidates (ADR-010)
- Translucent polylines for previously-driven continuations (incl. branches at junctions), visually subordinate to the live path and the active route. Specify color/opacity/width.

### 7. Routing attribution
- Unobtrusive, visible "© openrouteservice.org | © OpenStreetMap contributors" wherever a route is shown (ADR-011 requirement).

## States

- Tracking, no destination, perspective OFF (today's view + directional car marker + optional road-ahead candidates).
- Tracking, perspective ON (heading-up, follow; car marker appears to point up).
- Destination set, not yet started.
- Navigating (route line + ETA read-out + cancel).
- Off-route / recalculating (brief indicator; route refreshes at ~50 m off, gated).
- Routing failed (fail-soft: trip records track-only; a toast/notice "couldn't find a route").

## Design tokens

Use existing "Kinetic Precision" tokens (the `Trip*` colors in `ui/theme`). Route line: a distinct accent (not the blue live path). Road-ahead candidates: low-opacity neutral/accent. Attribution: muted caption.

## Interactions & behavior

- Search → select → Start (+ navigate). Perspective toggle is independent of nav/trip.
- Destination is mutable mid-trip (re-search swaps the route in place).
- Manual end = ends the trip. No auto-arrival.

## Edge cases

- No destination + Start = plain track-only trip.
- Routing failure = record track-only + a notice (never blocks recording).
- Stationary = car marker holds last heading; perspective stays heading-up on the last heading.
- Perspective ON while idle with no heading yet = north-up until the first heading.

## Screenshots to attach to the design tool

| # | Screen / State | Purpose |
|---|---|---|
| 1 | **Track screen — live map + `TripPanel` (READY)** | Primary surface; trip-metrics layout where the ETA read-out will sit |
| 2 | **Track screen — trip LIVE (`TripPanel` with OBD row)** | Where remaining distance/ETA coexist with existing metrics |
| 3 | **`MapControls` (recenter/layers FABs)** | Style reference for the new perspective toggle |
| 4 | **Bottom navigation** | Visual language / type scale |
| 5 | **Sessions status card / OBD card** | Color + typography language for consistency |

**Submission order:** 1 and 2 first (primary surface), then 3 (control style), then 4 and 5 (ambient context).

## State management / code notes

- `LocationUiState` gains a `bearingDegrees` (heading) field, surfaced from the `Location.bearing` already captured in `TrackingService`/`LocationEntity`.
- Camera: `CameraPosition.bearing = heading`, **no tilt** (Compose-Maps 1.2 safe); follows the vehicle.
- Marker: `Marker(rotation = heading)` with a car/chevron drawable.
- Routing behind a `RoutingEngine` port (ADR-011) with an `OpenRouteServiceAdapter`; encoded-polyline geometry reuses the existing decode path.
- No change to the canonical location log or the trip data model.

## Open questions / assumptions

1. Search placement — a top overlay pill vs a launched search sheet.
2. Start control — one CTA whose label changes vs two distinct buttons.
3. Road-ahead candidates — always shown when heading is valid (ADR-010) vs de-emphasized when perspective is OFF (design's call).
4. Attribution placement — corner caption vs within the read-out.

## Handoff Instructions

### How to submit to Claude Design

1. **Prepare the 5 screenshots** above from the running app.
2. **Tool:** Claude Design — sign in with **rinaldi.ch@gmail.com**, use the **existing "TrackLocation" project** (do NOT create a new project), and the **cheapest model (Haiku 4.5)**.
3. **Submit this prompt + the 5 screenshots:**

```
Design the Track Navigation (dual-mode) UI for the TrackLocation Android app (Jetpack Compose, Material 3, "Kinetic Precision" language). Match the existing Track screen (map + TripPanel + MapControls) in the attached screenshots.

Design these surfaces:
1. Destination search + autocomplete (entry on the Track screen; results list; empty/typing/no-results/selected states).
2. A Start control offering "Start trip (track only)" vs "Start trip + navigate", reusing the TripPanel READY/LIVE CTA language.
3. Navigating view: a route polyline distinct from the solid live path + a remaining-distance and static ETA read-out placed with the existing TripPanel metrics ("~18 min") + a cancel/end affordance + destination marker.
4. A "navigation perspective" toggle in MapControls (heading-up + follow, no tilt), default off; active/inactive states matching the existing FAB style.
5. A directional car marker that rotates to heading (replaces the static pin).
6. Translucent "road-ahead" candidate lines (previously-driven branches), subordinate to the live path and active route.
7. An unobtrusive "© openrouteservice.org | © OpenStreetMap contributors" attribution wherever a route shows.

Constraints: Compose 1.2 / Material 3 only (no ModalBottomSheet or newer APIs); do not disturb the existing TripPanel trip metrics; ETA is static (no live traffic).

Attached: this spec document + 5 reference screenshots.
```

4. **Next:** once the design is approved, **record it (commit) before any code** (AGENTS §5b/§12); implement behind the `RoutingEngine` port; do not run Gradle/device without explicit permission (§5a).

## Files in this bundle

- **This file** (`TRACK_NAVIGATION_SPEC.md`) — specification, screenshot checklist, and handoff instructions.
- **No design canvas or prototype** — use the existing app screenshots as the reference.
