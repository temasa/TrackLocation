# OBD Phase 2 Design Specification

**Status:** Slice 3 (Session `ObdStatusCard`) — design produced + verified in Claude Design and implemented in code (2026-07-02). Slice 4 (Trip panel) pending.
**Last updated:** 2026-07-02
**Reference:** OBD Phase 1 spec `../obd_phase1/OBD_PHASE1_SPEC.md`
**Code baseline:** `ObdStatusCard` (private composable) in `screens/sessions/SessionsScreen.kt`

---

## Overview

OBD Phase 2 adds **fuel-consumption metrics** to the OBD surfaces. Schema + service accumulation (Slices 1–2) are implemented; this spec covers the **UI change** for:

- **Slice 3 — Session `ObdStatusCard`** (this doc's focus)
- **Slice 4 — Trip panel** (deferred; will extend `../obd_phase1/OBD_PHASE1_SPEC.md` Surface 3)

**This is a modification of an already-shipped card, not a new screen.** The card already renders RPM / Speed / Efficiency (instant km/L) and a status indicator in Connected and Waiting states. Design work is limited to the two additions below and must preserve the current visual language (dark glass card: `TripSurface`@70% container, `TripInk` primary text, `TripMuted` labels, `TripGreen` accent, 16dp radius).

---

## Surface: `ObdStatusCard` — Phase 2 additions (Slice 3)

**Navigation path:** Session screen (bottom-nav first tab), below the always-recording status card.

### Current card (baseline — do not redesign)
- Header row: signal icon + "OBD Status" (left), status label "Connected"/"Waiting"/"—" (right).
- Metrics row (Connected): three cells — **RPM**, **SPEED**, **EFFICIENCY** (instant km/L; shows "—" when GPS speed < 3 km/h or accuracy > 20 m).
- A fuel-source chip button ("DIRECT (OBD)").
- Waiting state: dashed metrics + Reconnect.

### Change 1 — Idle fuel rate (L/h) in the EFFICIENCY cell
When the vehicle is **idling** (engine on, GPS speed < 3 km/h, RPM > 0) the EFFICIENCY cell currently shows "—". Instead, show the **instantaneous fuel rate in L/h** (e.g. `0.7 L/h`).
- Moving (speed ≥ 3 km/h, accuracy ≤ 20 m): show `X.X km/L` (unchanged).
- Idle (speed < 3 km/h, RPM > 0): show `X.X L/h`.
- No fuel data / engine off: show "—".
- Question for design: keep one cell that swaps unit (km/L ↔ L/h), or relabel the cell caption ("EFFICIENCY" → "L/H" at idle)? Preference: **swap the value + unit, keep the caption "EFFICIENCY"** to avoid layout shift.

### Change 2 — Session-average km/L
Add a **session-average km/L** metric (persisted, accumulates over the whole always-recording session, survives app restart).
- Label: "SESSION AVG", value `X.X km/L`, or "—" when no fuel consumed yet.
- Placement options for design review:
  - **(A, preferred)** a second metrics row below RPM/SPEED/EFFICIENCY with a single "SESSION AVG" cell (keeps the 3-up top row intact); or
  - (B) a fourth cell wrapped into the existing row.

### Fuel-source chip (cleanup, in-scope)
The chip currently hardcodes "DIRECT (OBD)". It should reflect the real source: `Direct (OBD)` / `Inferred (MAF)` / `Estimated` (speed-density) / `Unavailable`. Neutral tint for direct, amber tint for inferred/estimated.

### States to design
1. **Connected — moving:** EFFICIENCY = `8.5 km/L`; SESSION AVG = `11.2 km/L`; source chip.
2. **Connected — idle:** EFFICIENCY = `0.7 L/h`; SESSION AVG = `11.2 km/L`.
3. **Connected — no fuel data yet:** EFFICIENCY = "—"; SESSION AVG = "—".
4. **Waiting:** unchanged (dashed + Reconnect).

### Tokens / spacing (match existing card)
- Container `TripSurface`@0.7, radius 16dp, inner padding 16dp.
- Metric label 11sp Medium `TripMuted` all-caps; value 18sp Bold `TripInk`.
- New row top margin 8–12dp; keep cell horizontal spacing 20dp.

---

## Handoff Instructions

### Screenshots to attach (preserve visual language)
1. **Session screen with the OBD card visible in Connected state** — the primary baseline (card container, RPM/SPEED/EFFICIENCY cells, status label, source chip).
2. **Session screen always-recording status card** — card style/spacing baseline above the OBD card.
(Skip empty/idle screens — the card only matters when Connected/Waiting.)

### Tool: Claude Design (claude.ai/design)
Use the existing **TrackLocation** project (do not create a new one).

**Copy-paste prompt:**

```
TrackLocation — Android (Jetpack Compose, Material 3, dark theme). Modify the existing
"OBD Status" card on the Session screen — do NOT redesign it, keep the current dark glass
card style (translucent surface, green accent, 16dp radius, RPM/SPEED/EFFICIENCY cells).

Add two fuel-consumption elements for OBD Phase 2:

1) Idle fuel rate in the EFFICIENCY cell:
   - Moving (GPS speed >= 3 km/h): show "X.X km/L" (as today).
   - Idle (GPS speed < 3 km/h, engine RPM > 0): show "X.X L/h" instead of "—".
   - No fuel data / engine off: show "—".
   Keep the caption "EFFICIENCY"; only the value+unit swaps, so there is no layout shift.

2) Session-average km/L:
   - New metric "SESSION AVG" = "X.X km/L" (or "—" when no fuel consumed yet).
   - Preferred: a second metrics row below the RPM/SPEED/EFFICIENCY row containing the
     SESSION AVG cell, so the 3-up top row stays intact.

Also: the fuel-source chip should reflect the real source — "Direct (OBD)", "Inferred (MAF)",
"Estimated" (speed-density), or "Unavailable"; neutral tint for Direct, amber tint for
Inferred/Estimated.

Produce these states: Connected-moving, Connected-idle, Connected-no-data, and confirm the
Waiting state is unchanged. Match the attached screenshots' card styling, typography, and
spacing exactly. Provide dark theme (primary) and a light variant.
```

### After approval
- Note approval in `docs/IMPLEMENTATION-PLAN.md` (Slice 3), then implement in `ObdStatusCard` within `SessionsScreen.kt`.
- Code approach: reuse `ObdUiState.Connected.fuelRateLph` for idle L/h; compute session-average from the persisted `recording_session.obdFuelConsumedL`/`obdGpsDistanceKm` (Slice 2) rather than the in-memory `avgKmL` so it survives restarts.

---

**Version:** 1.0
**Created:** 2026-07-02
