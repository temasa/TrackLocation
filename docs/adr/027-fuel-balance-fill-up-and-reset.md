# ADR-027: Fuel Balance, Fill-up and Reset (Fuel Dock)

**Status:** Accepted; code implemented 2026-10-11 (static review only; build and device verification pending)
**Date:** 2026-10-11
**Decided By:** Project owner + Claude Code

## Context

Fuel economy is already measured while OBD is connected (ADR-004, ADR-024): the app integrates the litres consumed and the covered distance, and shows km/L and the session average. What it cannot show is how much fuel is **left in the tank**, and therefore how far the car can still go. The owner wants a persistent dock above the bottom navigation with the fuel left, the range, the current fuel price, and a **Fill up** action.

Constraints:
- OBD measures only the litres consumed while the adapter is connected. The starting level is unknown, so the balance needs an explicit anchor (a **Reset**).
- The fuel price is already a shared, effective-dated entity (ADR-008, FR-12). Fill-up prices must reuse that mechanism, not create a second price store.
- PRD §12 locked rules stay untouched: the canonical location log, the sessions-vs-trips separation, always-recording behaviour, and the bottom navigation (four tabs, no new destination). The fuel balance is a derived view and changes none of them.
- Schema: the codebase is already at Room **v15**. ADR-026 (`MIGRATION_14_15`, `trip_planned_route`) is implemented in commit `f26bb03`. This ADR's migration is therefore **v15 → v16** (`MIGRATION_15_16`), not the v14 → v15 named in the original scope. See Decision 6 and Open items.

## Decision

1. **Fuel dock (UI).** A persistent dock, 64 px high, full width, absolutely positioned directly above the bottom navigation. It has four cells: **FUEL LEFT (L)**, **RANGE (km)**, **PRICE (Rp/L)**, and a green **Fill up** pill button. It is shown on Track (all variants), List, Sessions and Settings. It is **not** shown on Details, OBD Settings, Observer Feed or Observer Settings (pushed sub-screens without the bottom navigation). Full placement, variants, states and layout rules are in UI-SPEC §5b. No new bottom-nav destination.
   - Note: commit `f39e1d4` first placed a dock on OBD Settings too. The 2026-10-11 design update removes it (owner decision); OBD Settings has no dock.

2. **Fuel left is derived, never edited in place.** Using the `fuel_event` log (Decision 6) and the OBD cumulative total:
   - `anchor` = the most recent `RESET` event; `resetAt` = its `createdAtMs`; `resetBaselineL` = its stored baseline (Decision 6).
   - `cumulativeNow` = Σ `recording_session.obdFuelConsumedL` over **all** session rows, the active row included, in one query/Flow. `ObdPollingService` calls `sessionDao.addObdAccumulator(sessionId, fuelIncrementL, …)` on every poll, so the active row already holds the live litres (confirmed in code 2026-10-11). `ObdUiState.sessionFuelConsumedL` is not needed. With OBD disconnected nothing accrues, so the value stays flat.
   - `fuelLeft = max(0, anchor.litres + Σ FILL_UP.litres (createdAtMs > resetAt) − max(0, cumulativeNow − anchor.resetBaselineL))`.
   - Consumption since the Reset is the growth of the cumulative OBD total since the Reset, read from the stored baseline. It does **not** read `obd_sample` and does not depend on retained samples, so the balance survives `obd_sample` retention pruning (1–30 days, `obdRetentionDays`). The inner `max(0, …)` stops a falling total (for example after a session is removed, see Consequences) from adding fuel back.
   - Fill-ups before `resetAt` are superseded by the Reset and do not count.
   - With no `RESET` event, fuel left is `—`.
   - **OBD gating:** only OBD-measured litres reduce the balance. With OBD disconnected, the balance stays frozen at its last value. No estimation from speed, distance or time.
   - The floor at 0 is applied to the displayed value; stored events are never clamped.

3. **Fill up.** The user reports a refuel. The **Fill up** button opens the Fill up dialog. The dialog asks for:
   - **Litres added (L)** (required, decimal, > 0).
   - **Price (Rp/L) - optional**, pre-filled with the latest shared price. Helper text: "Price defaults to the latest. Changing it saves a new price from now."
   - If the price is left at the default, the event is stored with `pricePerLiter = NULL` and **no** `fuel_price` row is written.
   - If the user changes the price, a new effective-now `fuel_price` row is appended, exactly as a Save in the FR-12 cost editor (ADR-008), and the event stores that price.
   - Actions: Cancel / Save.

4. **Reset.** Sets the starting fuel level and restarts the balance from it. The dialog asks for litres (required) and stores a `RESET` event whose `resetBaselineL` is the cumulative OBD total at the moment of the Reset (Decision 2 definition). **Entry point (decided 2026-10-11):** the pencil beside the FUEL LEFT value opens the **Reset fuel** dialog. The dialog has the title **Reset fuel**, the field **Fuel in tank now (L)** and the helper text "Sets the starting amount. Fuel left counts down from here." Actions: Cancel / Save.

5. **Range.** `RANGE = fuel left × SESSION AVG km/L`, where `SESSION AVG = session.obdCoveredDistanceKm ÷ session.obdFuelConsumedL` (ADR-024). Range uses the **current session's** average, not a per-trip average. It shows `—` when fuel left is `—`, or when the session has no average yet (covered distance 0, or fuel ≤ 0.01 L). A new session starts at `—`. RANGE is read-only (no pencil).

6. **Storage.** New Room table `fuel_event`, via `MIGRATION_15_16` (DB v15 → v16):
   - `id` INTEGER PRIMARY KEY AUTOINCREMENT
   - `kind` TEXT NOT NULL — `'RESET'` | `'FILL_UP'`
   - `litres` REAL NOT NULL — the starting level (RESET) or litres added (FILL_UP)
   - `pricePerLiter` REAL NULL — the price entered when changed in the Fill-up dialog; NULL otherwise
   - `resetBaselineL` REAL NULL — set only on `RESET` rows: the cumulative OBD fuel total (Decision 2) at the moment of the Reset; NULL for `FILL_UP`
   - `createdAtMs` INTEGER NOT NULL
   - **Append-only; no deletes and no edit path in this ADR.** No foreign keys (same convention as `observer_trip` and `trip_planned_route`). The table is not a column on `track` or `recording_session`.
   - Consumption comes from the existing OBD fuel totals (ADR-004 / ADR-012 / ADR-024): Σ `recording_session.obdFuelConsumedL` over all session rows, the active row included (Decision 2). The balance adds no new OBD column and does not read `obd_sample`.

7. **PRICE cell and its pencil.** The PRICE cell shows the current shared price from `FuelPriceController` (same source as FR-12). The pencil beside PRICE opens a **Fuel price** dialog that is the FR-12 price editor (Save / Cancel, undo / redo, effective-now row; finished trips keep their price). It is the same editor shown as a dialog, not a second price store or a second price editor. Helper text: "Applies from now. Finished trips keep their price."

8. **Edit pencils.** The tap target is the pencil icon, not the whole cell. A small pencil (12 px, lucide pencil, label colour) sits in the same row as the FUEL LEFT value, immediately to the right of the number (4 px gap, vertically centred), and likewise for the PRICE value. Pencils are not placed at the cell's right edge. The FUEL LEFT pencil opens Reset fuel; the PRICE pencil opens Fuel price. RANGE has no pencil. The Fill up pill opens the Fill up dialog.

9. **Dock states (confirmed by the owner 2026-10-11).** The design file (`ui-design.pen`, frame `Fuel Dock states`, variants of the Light dock) shows these states. The thresholds and colours below are confirmed by the project owner on 2026-10-11:
   - **Not set:** before the first Fill up or Reset; FUEL LEFT and RANGE show `—`.
   - **Normal:** e.g. `24.0` L, `310` km, in the normal value colour.
   - **Low:** fuel left below 6 L; the FUEL LEFT value and RANGE are amber, `#D97706`.
   - **Critical:** fuel left below 3 L; FUEL LEFT and RANGE are red, `#DC2626`.
   - **Low confidence:** values are prefixed with `~` (e.g. `~24.0`, `~310`) when more than 10% of the distance in the session was not covered by OBD.

10. **Out of this ADR:** tank capacity, undo/delete of fuel events, fuel-level alerts beyond the display states above, any change to GPS tracking, the canonical log, sessions/trips, or the navigation structure.
11. **Trips header price control removed (2026-10-11).** The fuel-pump icon added to the Trips `ListHeader` on 2026-07-08 (UI-SPEC §3b #8; historic IMPLEMENTATION-PLAN slice) is removed from the design. The PRICE pencil on the fuel dock is the price entry point from outside the cost cells; the Fuel price dialog is the same FR-12 editor. Code removal is pending (IMPLEMENTATION-PLAN §4 fuel dock slice, step 6).
12. **COST cells are display-only (decided 2026-10-11).** The COST cells (Session OBD card, Trips active-trip row, completed-trip rows, Sessions COST row, Track strips) have no tap target: tapping one does nothing (no editor, no Toast). The read-only Toast on completed-trip cost cells (the 2026-07-08 read-out, UI-SPEC §3b #9) is removed. The PRICE pencil on the fuel dock (Decision 7) is the single price entry point, and the Fuel price dialog is the only UI that edits the price. The ADR-008 model is unchanged (effective-dated rows; a completed trip uses the price in effect at its start); only its UI entry points are amended (see the ADR-008 amendment 2026-10-11). Code removal is pending (IMPLEMENTATION-PLAN §4 fuel dock slice, step 6).

## Consequences

### Positive
- The driver sees fuel left, range and the current price on every main screen, with one tap to record a refuel.
- The balance is derived from an append-only log: it is auditable and needs no backfill of an in-place value.
- Reuses the FR-12 / ADR-008 price path and the ADR-024 session average; no new OBD integral.

### Negative
- The balance is only as good as the OBD integral and the user's Reset. Long disconnected periods freeze the balance rather than estimate it, so the displayed value can lag reality.
- Append-only means a mistyped Fill up or Reset cannot be deleted in this version; the user corrects it with a later Reset.
- Room migration 15 → 16 and a new DAO to maintain.
- The dialogs and states were designed directly in the `.pen` file (no external handoff). The state thresholds and colours were confirmed by the owner 2026-10-11.
- **Session removal risk (known limitation):** the baseline assumes `recording_session.obdFuelConsumedL` values are never removed. If a persisted session driven after a Reset is deleted, the cumulative total falls, consumption is understated, and fuel left reads higher than reality by up to that session's litres until the next Reset. The `max(0, …)` clamp only prevents a negative consumption term; it does not restore the lost litres. Today no code path deletes `recording_session` rows (`SessionDao` has no delete method and no UI delete was found), so this is a risk for any future delete path, not a current behaviour. Any future session-delete feature must either keep the litres or re-anchor the Reset.
- **Disconnected OBD:** while OBD is disconnected nothing accrues, so the balance is frozen (Decision 2), not estimated.

### Schema impact
Room v15 → v16, `MIGRATION_15_16`: `CREATE TABLE fuel_event (...)` as in Decision 6, including `resetBaselineL REAL` (nullable, RESET rows only). No change to `track`, `recording_session`, `location_log`, `observer_trip`, `fuel_price` or `trip_planned_route`. No exported schema (`TrackDatabase` has `exportSchema = false`); the migration is verified by installing over a copy of the v15 database before shipping (IMPLEMENTATION-PLAN slice).

### Docs impacted
PRD (FR-22 new, FR-12 cross-reference), ARCHITECTURE (`fuel_event` entity, balance derivation, DB v16 planned), UI-SPEC (§5b new, §2 status row, §7, position text in §3d, §3e and §5a-adjacent Map Overlays note), IMPLEMENTATION-PLAN (§1, §4 slice, §6, Next Step), adr/README, DOCUMENT-CONTROL.

## Alternatives Considered

1. **Store a mutable "fuel in tank" value and update it on every poll.**
   - Pros: a single number, no derivation.
   - Cons: no audit trail, hard to correct, and it hides the Reset and Fill-up history. Rejected.
2. **No Reset: estimate the starting level from tank capacity.**
   - Pros: no user step before the first reading.
   - Cons: tank capacity is not known or stored, and an estimate would be invented data. Rejected for this version.
3. **Fill-up only, with fuel left = sum of fill-ups − consumption (no Reset).**
   - Pros: no separate Reset dialog.
   - Cons: the balance drifts from the start of the app's history and cannot be re-anchored. Rejected.
4. **Store the price on the Fill-up event always.**
   - Pros: the event is self-contained.
   - Cons: duplicates the fuel price log and makes the default case ambiguous. Rejected; the price row is written only on change.

## Related ADRs

- **ADR-004 / ADR-012** — OBD fuel integral and its accumulation gates; the only consumption source for the balance.
- **ADR-007 / ADR-024** — km/L and the session average used for RANGE.
- **ADR-008** — effective-dated fuel price; fill-up price changes append rows through it.
- **ADR-026** — `MIGRATION_14_15` (`trip_planned_route`, implemented in `f26bb03`); this ADR's migration follows it as v15 → v16.

## References

- PRD FR-12, FR-22 (new); PRD §12 locked rules
- ARCHITECTURE §2 (`fuel_event`), §8 (`MIGRATION_15_16`)
- UI-SPEC §5b (Fuel Dock)
- IMPLEMENTATION-PLAN §4 slice "Fuel Dock, Fill-up and Reset (ADR-027)"
- `ui-design.pen` frames `FuelDock/Dark`, `FuelDock/Light`, `Fuel Dock states`, `Dialog Column` (FillUpDialog, ResetFuelDialog, FuelPriceDialog)

## Assumptions and open points (recorded, not decided here)

- **Dock states:** thresholds 6 L (low), 3 L (critical), 10% uncovered distance (low confidence), and the amber `#D97706` / red `#DC2626` colours were confirmed by the owner on 2026-10-11 (Decision 9).
- **`pricePerLiter` NULL when the default price is kept:** the event then refers to the fuel price row in effect at its `createdAtMs`. Chosen by the assistant.
- **Cumulative OBD total (rev. 1.2):** `cumulativeNow` = Σ `recording_session.obdFuelConsumedL` over all session rows, active included (Decision 2). Chosen by the assistant. Confirmed in code 2026-10-11: `ObdPollingService` writes each poll's litres to the active row via `addObdAccumulator`, so no separate live value is read. ADR-012 gating applies to that accumulator. To verify on device.
- **Session close re-integration (rev. 1.2):** at session close the stored `obdFuelConsumedL` is re-integrated from `obd_sample` (ADR-007; `TrackingService` `reintegratedFuelL`). If that value differs from the value accumulated during the drive, `cumulativeNow` shifts once by the difference at close. Expected to be small; recorded, not decided here. To verify on device.
- **Number formats:** FUEL LEFT to one decimal (e.g. `24.0`) and RANGE in whole km (e.g. `310`), as in the design; PRICE uses the §5a compact Rp format. Confirm at code time.

## Open items / gates

- [x] **Owner confirmed the dock state thresholds (6 L / 3 L / 10%) and the amber `#D97706` / red `#DC2626` colours on 2026-10-11** (Decision 9).
- [ ] **Code check, live litres (to verify on device):** `ObdPollingService` accumulates each poll's litres into the active `recording_session.obdFuelConsumedL` (`addObdAccumulator`), so the cumulative total is Σ over all session rows, active included (Decision 2; Assumptions). To verify on device.
- [ ] **Code gate (before merge):** verify that no session-delete path exists (Consequences).
- [ ] **Migration ordering:** if ADR-026 code ships to v15 before this ADR, this migration is v15 → v16 (as written). If the owner wants both changes in one release, they may be combined into one migration; decide before code.
- [ ] Owner decides whether a fuel-event correction path (edit or delete) is needed later; out of scope here.

## Status Summary

- **ADR Accepted:** 2026-10-11.
- **Docs Updated:** 2026-10-11 — PRD (FR-22; FR-12 v0.26), ARCHITECTURE, UI-SPEC (§5b; §4 and §3b #9 v0.41), IMPLEMENTATION-PLAN (v0.56), adr/README, ADR-008 (amendment 2026-10-11, UI entry points), DOCUMENT-CONTROL.
- **Revision 1.2 (2026-10-11):** Decision 2 and Decision 6 now use the stored `resetBaselineL` (Reset writes the cumulative OBD total), replacing the earlier "OBD integral since the Reset" wording; Decision 9 thresholds and colours confirmed by the owner; session-removal and OBD-disconnect risks recorded. ARCHITECTURE v0.30, IMPLEMENTATION-PLAN v0.57, UI-SPEC v0.42 updated to match; PRD unchanged (v0.26, FR-22 does not name samples or thresholds).
- **Revision 1.3 (2026-10-11):** code implemented (see Code Implementation). Status line updated; the live-litres check and session-close re-integration are marked to verify on device. ARCHITECTURE v0.31, IMPLEMENTATION-PLAN v0.58, UI-SPEC v0.43 updated to match.
- **Design:** dock, edit pencils, Fill up / Reset fuel / Fuel price dialogs and dock states designed in `ui-design.pen` (2026-10-11 update; `f39e1d4` added the dock first). OBD Settings has no dock. Reset entry point decided. Trips header fuel-pump icon removed from the design (Decision 11); COST cells display-only, no tap and no Toast (Decision 12); code removal implemented (`9d01688`).
- **Code Implementation:** implemented 2026-10-11 in commits `a197453` (data layer), `36030ef` (dock UI) and `9d01688` (removals); static review only. No build, test or device run performed; build and device verification pending.
