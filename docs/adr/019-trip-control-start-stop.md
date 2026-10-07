# ADR-019: Trip Control — Start ⇄ Stop Only (No Pause/Resume)

**Status:** Accepted
**Date:** 2026-10-07
**Decided By:** Project owner + Claude Code

## Context

The Track screen's TripPanel CTA implements a three-state trip state machine: READY → LIVE → PAUSED. In practice, the PAUSED state was unreachable (nothing set `isPaused`; resume was listed as a "future phase" in ADR-009). On-device verification (2026-10-07, SM-G965F) found three usability issues:

1. The LIVE button was labelled "Pause" and displayed a pause glyph, but actually sent STOP_TRIP (not pause).
2. The PAUSED state was unreachable because no service action or user gesture set the pause flag; the app never rendered the pause+resume pattern.
3. When READY (not recording), the TripPanel displayed the elapsed time and distance from the previous trip, leaving stale stats on screen until Start was tapped again.

This state machine was redundant (pause is not needed in the current workflow) and confusing (the UI promised pause but delivered stop). The decision is to simplify to a two-state design: READY and LIVE only.

## Decision

1. **TripState enum:** Remove the `PAUSED` state. Keep `READY` (idle, no trip active) and `LIVE` (trip recording actively).

2. **LIVE state CTA:** 
   - Display: filled rounded square glyph (stop icon, replaces the pause icon)
   - Label: "Stop trip" (accessibility)
   - Behavior: tapping sends `STOP_TRIP` (unchanged)

3. **READY state CTA:**
   - Display: play glyph (unchanged)
   - Label: "Start trip" (unchanged)
   - Behavior: tapping sends `START_TRIP` (unchanged)

4. **READY state display reset:**
   - When entering READY (whenever no trip is active), reset the TripPanel display to:
     - Elapsed: `00:00:00` (reset to zero, not the previous trip's value)
     - Distance: `0.00 km` (reset to zero, not the previous trip's value)
     - Speed: live speed (unchanged; always displays current speed)
   - The finished trip is already persisted when Stop is tapped; no schema or DAO change.
   - This is **display-only:** the finished trip's actual duration and distance are saved before entering READY; the panel just shows fresh zeroes for the next trip.

5. **Related systems unchanged:**
   - Always-recording switch (Session screen) — unchanged (ADR-002)
   - Canonical location log — unchanged (ADR-001)
   - Sessions vs trips separation — unchanged (PRD §12)

## Consequences

### Positive
- **Simpler state machine:** Two states instead of three, all transitions explicit and used (no unreachable PAUSED state).
- **UI-action alignment:** The button glyph and label ("Stop trip", square icon) now match the actual action (STOP_TRIP), eliminating the pause-vs-stop confusion.
- **Cleaner UX on restart:** New trips begin with zeroed elapsed/distance instead of showing stale metrics from the previous trip. The driver sees a clean `00:00:00 / 0.00 km` before hitting Start.
- **No schema or database change:** The finished trip is persisted before entering READY; the reset is a display-only UI state refresh.

### Negative / Accepted Risks
- **No pause/resume feature:** If the driver needs to interrupt a trip mid-journey (e.g., break), the only option is Stop (which persists the trip) or manually start a new trip later. This trade-off was accepted per the current product definition (PRD §12, no pause requirement).
- **Visual break from common patterns:** Some mapping apps offer pause; TrackLocation's simpler Start/Stop is intentional and aligns with the trip-as-atomic-unit model (sessions and trips are journeys, not fragmented).

## Alternatives Considered

1. **Keep PAUSED and implement resume:** Add a working pause state and resume button. Rejected: adds complexity; resume was deferred indefinitely in ADR-009; the current product definition does not require it (PRD §12).

2. **Keep the three-state machine with fixes:** Fix the label/icon to match the action ("Stop trip" on LIVE) but keep PAUSED defined. Rejected: does not address the PAUSED unreachability or the stale-metrics-on-READY display issue; leaves dead code.

3. **Chosen: Start/Stop only with READY reset:** Simplest, cleanest; aligns UI, label, action, and display state.

## Related ADRs

- **ADR-001** (Always-recorded Location Sessions) — session control unchanged.
- **ADR-002** (Session Recording Switch) — always-recording remains on the Session screen.
- **ADR-009** (Dual-mode Track Navigation) — paused-state wording superseded (no longer relevant; see Supersedes note below).
- **ADR-015** (Gojek Order Lifecycle) — trip strip is shown only while the trip is LIVE (PAUSED no longer exists).

## References

- PRD.md §12 (locked rules: canonical log, sessions vs trips; no pause requirement listed).
- UI-SPEC.md §3f (TripPanel spec; updated to READY/LIVE only).
- ARCHITECTURE.md (TripState enum; updated to remove PAUSED).
- On-device observation (2026-10-07, SM-G965F): pause button labelled but nonfunctional; READY shows stale stats.

---

## Status Summary

- **ADR Accepted:** 2026-10-07 — Trip control simplified to Start/Stop; PAUSED state removed.
- **Docs Updated:** 2026-10-07 — ADR-009 (Context note: PAUSED wording superseded), ADR-015 (trip strip shown LIVE only), UI-SPEC (READY/LIVE only, READY reset, stop glyph), ARCHITECTURE (TripState enum), IMPLEMENTATION-PLAN (new slice + task log rows), adr/README (ADR-019 index), DOCUMENT-CONTROL (register + change log).
- **Code Implementation:** Pending user approval and build permission (AGENTS.md §5a).

---

## Supersedes

- **ADR-009 Context:** The wording "READY → LIVE → PAUSED" trip state machine is superseded by this ADR. The paused-state wording and references in ADR-009's context and alternatives are historical; the live implementation uses Start/Stop only per ADR-019.
- **ADR-015 trip strip:** The trip strip (compact TIME/DIST/COST/NET/AVG/INST km/L metrics) is shown only while the trip is LIVE. PAUSED no longer exists, so the "trip paused → frozen" behavior is moot.

---

**Privacy & UX Note:** The decision to remove pause/resume keeps trip recording simple: once started, a trip runs continuously until Stop (no intermediate frozen states, no implicit resume logic). This aligns with the one-trip-per-order model (ADR-015) and the always-recording background sessions (ADR-001), reducing driver confusion and data fragmentation.
