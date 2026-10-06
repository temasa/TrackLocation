# ADR-018: Track Screen Shows Live Location (Display-Only, Independent of Recording)

**Status:** Accepted
**Date:** 2026-10-06
**Decided By:** Project owner + Claude Code

## Context

The Track screen displays the blue dot and related location UI (follow mode, Recenter target, order route overlay) by reading `TrackingService.locationUiState.currentLocation`, which only updates while the service is running (always-recording ON or a trip is live). When nothing records, the dot remains fixed at the last-known position seeded once at launch (`MainActivity.seedLastKnownLocation`), making the dot stale and the Recenter target incorrect after the user moves.

Observation (2026-10-06, on-device): with tracking=false and always-recording=false, the blue dot stayed at the startup seed location; tapping Recenter moved the camera to the stale position, not the actual device location.

The project owner wants to use the Track screen as a Waze/Google Maps replacement, so the display must always reflect the live device position, regardless of recording state.

## Decision

1. **New `LiveLocationSource` (screen-scoped, lifecycle-aware):** Requests FusedLocationProviderClient high-accuracy updates (~1 s interval, ~500 ms fastest) **ONLY** while the Track screen is visible **AND** the app is in the foreground (ON_START/ON_STOP or RESUMED). Stops requesting otherwise. Exposes a `StateFlow` of the latest fix (lat/lng, speed, bearing, accuracy, time).

2. **TrackScreen uses the live fix for DISPLAY:** Map dot/marker, follow mode, Recenter target. Fallback while no live fix has arrived: the last-known seed from TrackingService (today's behaviour). The Seoul placeholder is never shown or used.

3. **Display-only: live fixes are NEVER stored.** TrackingService remains the only writer of the canonical location log, sessions, trips, OBD accumulation, and Gojek order auto-start/end. No schema change, no Room migration. PRD §12 locked rules untouched (canonical location log; sessions vs trips; always-recording switch stays on the Session screen).

4. **Permissions:** No new permission required. Uses the existing `ACCESS_FINE_LOCATION` foreground permission. **NO background location.** If permission is not granted, the Track screen behaves as today (permission flow is shown) and the source stays idle.

5. **Battery:** GPS at ~1 Hz only while the Track screen is open (same as Google Maps/Waze). When the tracking service is also running, FusedLocationProviderClient merges the requests, so the extra cost is small.

6. **OrderRouteController unchanged in this slice:** Keeps reading the service location (an active order auto-starts a trip so the service is running). May adopt the live source later.

7. **Out of scope / roadmap note:** The owner wants the Track screen to eventually replace Google Maps/Waze. Turn-by-turn navigation (heading-up perspective + speed readout) is still listed in PRD Out of Scope and ADR-009 (follow-a-route, planned) is not built. This ADR is only the live-position prerequisite. Heading-up, perspective, speed readout, and any directions require a separate product decision (PRD/ADR-009 revisit). Recorded here as a roadmap note, not implemented.

## Consequences

### Positive
- **Live dot whenever visible:** The Track screen now shows the actual device location in real time, making it usable as a standalone map replacement (Waze-like experience).
- **Accurate Recenter target:** Tapping Recenter animates to the true current position, not a stale seed.
- **No canonical-log pollution:** Recording state and display state are cleanly separated. Live fixes are never written, so the canonical location log remains the authoritative source of truth for all recorded journeys.
- **Battery reasonable:** GPS only while screen open; request merging with the service when both are active (already recording).
- **No schema change:** Simple data-flow addition; zero DB migration risk.

### Negative / Accepted Risks
- **Battery cost while screen open:** ~1 s polling interval (same as Google Maps) drains faster than when idle; mitigated by stopping requests immediately when the screen is not visible.
- **Slight divergence between live and recorded trace:** When parked (recording active), the dot (live at 1 Hz, no dwell collapse) and the recorded trace (dwell-collapsed by the service) may differ slightly in position after a short park. Negligible in practice (metres, user-expected in any mapping app).
- **Permission flow on first open:** If foreground location permission is not yet granted, the Track screen triggers the permission dialog (same as today); user must grant to see live position.
- **Fallback delay:** On first open, the screen shows the last-known seed until the first live fix arrives (~5 s in open air); this is expected and matches Google Maps/Waze behaviour.

## Alternatives Considered

1. **Do nothing (stale dot when idle).** Status quo; rejected: does not meet the Waze-replacement goal.

2. **One-shot fix on Recenter tap (dot still stale between taps).** Lighter implementation; rejected: poor UX (dot remains stale while navigating; confusing for a maps app).

3. **Live while visible (chosen).** Full live position; reasonable battery cost (GPS only while screen open); UX matches Waze/Google Maps.

4. **Force the always-recording service ON whenever Track is open (rejected).** Would write to the canonical log and change the always-recording switch semantics; violates PRD §12 locked rules (canonical log is source of truth, sessions/trips are derived views). Rejected.

## Related ADRs

- **ADR-001** (Always-recorded Location Sessions) — canonical log model; this ADR keeps it intact.
- **ADR-002** (Session Recording Switch) — always-recording switch control; this ADR does not change it.
- **ADR-009** (Dual-mode Track Navigation) — planned follow-a-route feature; this ADR is a prerequisite (live position needed for routing).
- **ADR-016** (Gojek Order Route Overlay) — order routes use OrderRouteController, which still reads the service location; this ADR does not change that.
- **ADR-017** (Order Route Provider) — OpenRouteService; this ADR does not affect routing.

## References

- FusedLocationProviderClient API: https://developers.google.com/location-context/fused-location-provider
- Google Maps / Waze reference: Real-time position display, location request lifecycle tied to foreground visibility.
- PRD.md §12 (locked rules: canonical log as source of truth, sessions/trips as views).
- ARCHITECTURE.md (domain model, Track/Gojek flow).
- UI-SPEC.md §3f (Track map, Recenter FAB, follow mode).
- TrackingService (canonical location writer, singleton).

---

## Status Summary

- **ADR Accepted:** 2026-10-06 — Live-position display as a display-only data path, independent of recording.
- **Docs Updated:** 2026-10-06 — PRD (FR-18 new), ARCHITECTURE (LiveLocationSource component + data-flow), UI-SPEC (§3f update: live fix note), IMPLEMENTATION-PLAN (§4 new slice, §6 task rows), adr/README (ADR-018 index), DOCUMENT-CONTROL (register + change log).
- **Code Implementation:** Pending user build permission (AGENTS.md §5a).
- **Design Handoff:** No new UI surfaces; provisional icon colors already in UI-SPEC §3f (dark/grey Recenter FAB).

---

**Privacy:** The app reads location while the Track screen is open without recording it. Nothing is stored or transmitted (the only external call remains OpenRouteService for order routes, ADR-017).

