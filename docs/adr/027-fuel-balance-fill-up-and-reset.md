# ADR-027: Fuel Balance, Fill-up and Reset (Fuel Dock)

**Status:** Accepted; code implemented 2026-10-11; build OK; device-verified partially (see IMPLEMENTATION-PLAN §4). **Amended 2026-10-11 (Revision 1.6):** consumption comes from a session-independent OBD ledger (Decision 13); Decisions 2, 5 and 6 amended to match. Ledger code not started.
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
   - `cumulativeNow` = the OBD ledger's `fuelConsumedL` (Decision 13, Revision 1.6). It is no longer Σ `recording_session.obdFuelConsumedL`: the ledger is written on every poll whether or not a session exists, so fuel left counts down with no active session and with always-recording OFF. (The Revision 1.2 definition, Σ over all session rows with the active row included, is superseded; it only counted litres while a session existed.) With OBD disconnected nothing accrues, so the value stays flat.
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

5. **Range (rev. 1.6, design change).** `RANGE = fuel left × ROLLING AVG km/L`, where `ROLLING AVG km/L = window covered km ÷ window litres` over the **last 60 seconds** of OBD driving. The window holds the same poll increments as the session accumulators (ADR-024: the same fuel-covered intervals, GPS-covered km, and litres at a valid rate of 0.1 to 100 L/h). It is kept **in memory** in `ObdPollingService`, is not persisted, and is lost on process restart. The rolling value is published to the dock through a small shared state holder (the same pattern as `FuelPriceController`). It does not depend on the session, the ledger or always-recording. The session average (ADR-024) is unchanged and still shown on the Session card.
   - **Stationary hold (assistant default; the owner may override):** when the window's covered km is below 0.05 km (car essentially stopped), or its litres are ≤ 0.01 L, the dock **holds** the last valid rolling km/L instead of showing 0 or `—`. RANGE therefore keeps shrinking as fuel left drops while idling.
   - Before any valid rolling value exists since app start, RANGE shows `—`. RANGE also shows `—` when fuel left is `—`. RANGE is read-only (no pencil).
   - The low-confidence `~` marker (Decision 9) is still computed from the **active** session's covered and total distance, so it stays off when no session is active (unchanged behaviour).
   - *Superseded draft:* an average since the latest Reset (with a lifetime fallback for pre-v17 Resets) was the first rev. 1.6 draft; the owner replaced it with the rolling window (Alternatives 7).

6. **Storage.** New Room table `fuel_event`, via `MIGRATION_15_16` (DB v15 → v16):
   - `id` INTEGER PRIMARY KEY AUTOINCREMENT
   - `kind` TEXT NOT NULL — `'RESET'` | `'FILL_UP'`
   - `litres` REAL NOT NULL — the starting level (RESET) or litres added (FILL_UP)
   - `pricePerLiter` REAL NULL — the price entered when changed in the Fill-up dialog; NULL otherwise
   - `resetBaselineL` REAL NULL — set only on `RESET` rows: the cumulative OBD fuel total (Decision 2) at the moment of the Reset; NULL for `FILL_UP`
   - `createdAtMs` INTEGER NOT NULL
   - **Append-only; no deletes and no edit path in this ADR.** No foreign keys (same convention as `observer_trip` and `trip_planned_route`). The table is not a column on `track` or `recording_session`.
   - Consumption comes from the OBD ledger (`obd_ledger`, Decision 13, Revision 1.6), which is fed by the same OBD increments as the session accumulators (ADR-004 / ADR-012 / ADR-024). The balance adds no new OBD column and does not read `obd_sample`. (Revision 1.2 read Σ `recording_session.obdFuelConsumedL`; superseded by the ledger.)

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

13. **OBD ledger: session-independent consumption (amendment, Revision 1.6, 2026-10-11).**
   - **Problem found:** fuel left and RANGE stopped updating when no recording session was active. `ObdPollingService` writes each poll's litres and covered km only to the active session (`sessionDao.addObdAccumulator`, ADR-024), and fuel left was derived from Σ session litres. Driving with always-recording OFF therefore changed nothing on the dock.
   - **Decision (owner, 2026-10-11, option 1):** add an **OBD ledger**: a Room table `obd_ledger` with one singleton row (`id = 1`): `fuelConsumedL REAL` (cumulative OBD litres, one column). `ObdPollingService` adds every poll's increments to the ledger unconditionally: with or without an active session, and with always-recording ON or OFF. The increments and the fuel-covered interval rule are the same as for the session accumulators (ADR-024). The per-session and per-trip accumulators are unchanged (FR-08, FR-12; session and trip cost unchanged). Implementation note: today the increment computation sits inside `if (gpsState.isAlwaysRecording)` in `ObdPollingService`; it must be moved or duplicated so that the ledger write (and the rolling window) run whether or not always-recording is ON.
   - **Fuel left:** the formula in Decision 2 is unchanged, but its cumulative total now comes from the ledger. A new Reset stores the ledger's `fuelConsumedL` as `resetBaselineL` (the `resetBaselineL` semantics are unchanged).
   - **RANGE:** as in Decision 5 (a 1-minute rolling OBD window kept in memory, not read from the ledger). RANGE no longer needs an active session.
   - **Idle counts (owner addition, 2026-10-11).** Fuel left also falls while the engine is ON and the car is stationary, as long as the OBD adapter is connected and reports a valid fuel rate (0.1 to 100 L/h, the ADR-024 rule). Idle litres (`fuelRate × dt`) are added to `fuelConsumedL` and to the rolling window, with **0** km. Idle therefore lowers the ledger's average km/L and so lowers RANGE. This is intended.
   - **Only OBD-measured litres reduce the balance.** With the adapter disconnected the balance stays frozen (FR-22 unchanged). No GPS or speed estimation is added.
   - **Migration (Room v16 → v17, `MIGRATION_16_17`):**
     - Create `obd_ledger` and seed the singleton row with the current Σ `recording_session.obdFuelConsumedL`, so existing Reset baselines (`resetBaselineL`) stay valid.
     - Fresh installs: the singleton row must exist (created on database create, or inserted if missing on first write).
   - **Rejected alternatives** (see Alternatives 5 to 7): an automatic daily session (conflicts with PRD §12, FR-02, FR-10, BR-07 and BR-08, and leaves gaps when the process dies); attributing orphan fuel to the latest closed session (corrupts session statistics and cost); an average since the latest Reset for RANGE (rejected by the owner in favour of the 1-minute rolling window; see Alternatives 7).

## Consequences

### Positive
- The driver sees fuel left, range and the current price on every main screen, with one tap to record a refuel.
- The balance is derived from an append-only log: it is auditable and needs no backfill of an in-place value.
- Reuses the FR-12 / ADR-008 price path and the ADR-024 session average; no new OBD integral.
- Revision 1.6: fuel left and RANGE follow OBD driving whether or not a recording session is active (Decision 13).
- Revision 1.6: fuel left also counts down at idle (engine on, car stationary, valid OBD fuel rate). This is recorded as intended (Decision 13).

### Negative
- The balance is only as good as the OBD integral and the user's Reset. Long disconnected periods freeze the balance rather than estimate it, so the displayed value can lag reality.
- Append-only means a mistyped Fill up or Reset cannot be deleted in this version; the user corrects it with a later Reset.
- Room migration 15 → 16 and a new DAO to maintain.
- The dialogs and states were designed directly in the `.pen` file (no external handoff). The state thresholds and colours were confirmed by the owner 2026-10-11.
- **Session removal risk (known limitation):** the baseline assumes `recording_session.obdFuelConsumedL` values are never removed. If a persisted session driven after a Reset is deleted, the cumulative total falls, consumption is understated, and fuel left reads higher than reality by up to that session's litres until the next Reset. The `max(0, …)` clamp only prevents a negative consumption term; it does not restore the lost litres. Today no code path deletes `recording_session` rows (`SessionDao` has no delete method and no UI delete was found), so this is a risk for any future delete path, not a current behaviour. Any future session-delete feature must either keep the litres or re-anchor the Reset. (Revision 1.6: the ledger is a separate running total, so deleting a session row does not lower it. This risk no longer affects fuel left or RANGE; it still applies to session-level figures.)
- **Ledger and session figures can differ slightly (Revision 1.6):** the ledger adds each poll's increments and is not re-integrated at session close, while the stored session litres are re-integrated from `obd_sample` at close (Assumptions). See Open items.
- **Disconnected OBD:** while OBD is disconnected nothing accrues, so the balance is frozen (Decision 2), not estimated.
- **Idle and RANGE (Revision 1.6):** with the engine on and the car stationary, fuel left keeps falling. RANGE uses the last valid 1-minute rolling average (Decision 5), held while stationary, so RANGE keeps shrinking as fuel left drops. This follows from the owner's idle rule (Decision 13).

### Schema impact
Room v15 → v16, `MIGRATION_15_16`: `CREATE TABLE fuel_event (...)` as in Decision 6, including `resetBaselineL REAL` (nullable, RESET rows only). No change to `track`, `recording_session`, `location_log`, `observer_trip`, `fuel_price` or `trip_planned_route`. No exported schema (`TrackDatabase` has `exportSchema = false`); the migration is verified by installing over a copy of the v15 database before shipping (IMPLEMENTATION-PLAN slice).

Revision 1.6 (planned, not yet in code): Room v16 → v17, `MIGRATION_16_17`: new table `obd_ledger` (one singleton row with one column, `fuelConsumedL`, seeded from Σ `recording_session.obdFuelConsumedL`). No change to `fuel_event` or other tables. See Decision 13 and ARCHITECTURE §8.

### Docs impacted
PRD (FR-22 new, FR-12 cross-reference; Revision 1.6 FR-22 amended for the ledger and RANGE), ARCHITECTURE (`fuel_event` entity, `obd_ledger` entity, balance derivation, DB v16 current / v17 planned), UI-SPEC (§5b new, §2 status row, §7, position text in §3d, §3e and §5a-adjacent Map Overlays note), IMPLEMENTATION-PLAN (§1, §4 slice, §6, Next Step), adr/README, DOCUMENT-CONTROL.

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
5. **Automatic daily session, so consumption counts with no session (Revision 1.6).**
   - Pros: reuses the existing session accumulators.
   - Cons: conflicts with PRD §12 and FR-02, FR-10, BR-07 and BR-08 (session model), and leaves gaps when the process dies. Rejected.
6. **Attribute orphan fuel (drive with no session) to the latest closed session (Revision 1.6).**
   - Pros: no new table.
   - Cons: corrupts the session statistics and session cost. Rejected.
7. **RANGE from the average since the latest Reset (Revision 1.6, first draft).**
   - Rejected by the owner in favour of the 1-minute rolling window (Decision 5, Revision 1.6).

## Related ADRs

- **ADR-004 / ADR-012** — OBD fuel integral and its accumulation gates; the same gates feed the OBD ledger (Decision 13) that is the consumption source for the balance.
- **ADR-007 / ADR-024** — km/L and the session average; RANGE now uses a 1-minute rolling OBD average (Decision 5, Revision 1.6). The session average is unchanged.
- **ADR-008** — effective-dated fuel price; fill-up price changes append rows through it.
- **ADR-026** — `MIGRATION_14_15` (`trip_planned_route`, implemented in `f26bb03`); this ADR's migration follows it as v15 → v16.

## References

- PRD FR-12, FR-22 (new); PRD §12 locked rules
- ARCHITECTURE §2 (`fuel_event`, `obd_ledger`), §8 (`MIGRATION_15_16`, `MIGRATION_16_17`)
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
- [ ] **OBD ledger code and device check (Revision 1.6, not started):** verify that the ledger counts with no active session and always-recording OFF, keeps counting after a force-stop, and freezes with OBD disconnected; verify the v16 → v17 migration over existing data (IMPLEMENTATION-PLAN §4 "OBD Fuel Ledger (ADR-027 amendment)").
- [ ] **Ledger versus session re-integration (Revision 1.6, open):** at session close the stored session litres are re-integrated from `obd_sample` (Assumptions); the ledger is not re-integrated, so the two can differ by a small amount. Whether the ledger should follow the re-integrated value is not decided here.
- [ ] **Calibration (Revision 1.6, optional):** compare the litres shown by the dock over a tank with the litres of a real fill-up.
- [ ] **Stationary hold (Revision 1.6, assistant default; the owner may override):** when the rolling window covers less than 0.05 km or less than or equal to 0.01 L, RANGE holds the last valid km/L instead of showing 0 or `—`. Before any valid value exists since app start, RANGE shows `—`. The window is in memory only and is lost on process restart.

- [x] **Reset accepts 0 L (verified on device 2026-10-11):** typing 0 enables Save and the dock shows 0.0; the dialog opens empty with a grey `0` hint, and Save is disabled only while the field is empty.

## Status Summary

- **ADR Accepted:** 2026-10-11.
- **Docs Updated:** 2026-10-11 — PRD (FR-22; FR-12 v0.26), ARCHITECTURE, UI-SPEC (§5b; §4 and §3b #9 v0.41), IMPLEMENTATION-PLAN (v0.56), adr/README, ADR-008 (amendment 2026-10-11, UI entry points), DOCUMENT-CONTROL.
- **Revision 1.2 (2026-10-11):** Decision 2 and Decision 6 now use the stored `resetBaselineL` (Reset writes the cumulative OBD total), replacing the earlier "OBD integral since the Reset" wording; Decision 9 thresholds and colours confirmed by the owner; session-removal and OBD-disconnect risks recorded. ARCHITECTURE v0.30, IMPLEMENTATION-PLAN v0.57, UI-SPEC v0.42 updated to match; PRD unchanged (v0.26, FR-22 does not name samples or thresholds).
- **Revision 1.3 (2026-10-11):** code implemented (see Code Implementation). Status line updated; the live-litres check and session-close re-integration are marked to verify on device. ARCHITECTURE v0.31, IMPLEMENTATION-PLAN v0.58, UI-SPEC v0.43 updated to match.
- **Design:** dock, edit pencils, Fill up / Reset fuel / Fuel price dialogs and dock states designed in `ui-design.pen` (2026-10-11 update; `f39e1d4` added the dock first). OBD Settings has no dock. Reset entry point decided. Trips header fuel-pump icon removed from the design (Decision 11); COST cells display-only, no tap and no Toast (Decision 12); code removal implemented (`9d01688`).
- **Revision 1.4 (2026-10-11):** build OK (`assembleDebug`). Device run (partial, screenshots): the dock was not rendering because it was mounted in the unused `screens/MainScreen.kt`; fix `3354689` mounts `FuelDockHost` in MainActivity. Dock, Reset / Fill up / Fuel price dialogs, persistence and the v15 → v16 migration over existing data verified; OBD, driving, changed-price and TalkBack checks still open (IMPLEMENTATION-PLAN §4). ARCHITECTURE v0.32, IMPLEMENTATION-PLAN v0.59, UI-SPEC v0.44, ERRORS-LOG v0.5 (ERR-009) updated to match.
- **Revision 1.5 (2026-10-11):** correction of the Reset-to-0 L item in Revision 1.4. That item was a misread of the first device screenshots: the Reset dialog opens with an empty field showing a grey `0` hint, and Save is disabled only while the field is empty. `ResetFuelDialog` accepts any value >= 0 (`valid = litres != null && litres >= 0.0`), so there is no deviation and no code change. Reset accepts 0 L (verified on device 2026-10-11: typing 0 enables Save and the dock shows 0.0). Further device checks (screenshots): Reset 5 gave FUEL LEFT 5.0 in amber `#D97706` (Low); Reset 2 gave 2.0 in red `#DC2626` on the light and dark docks; Reset 0 gave 0.0 in red (Critical). RANGE colouring was not exercised (RANGE stayed `—`). Owner review: the red value on the dark dock has modest contrast (UI-SPEC §5b note (l)). IMPLEMENTATION-PLAN v0.60 and UI-SPEC v0.45 updated to match; DOCUMENT-CONTROL v0.7.
- **Code Implementation:** implemented 2026-10-11 in commits `a197453` (data layer), `36030ef` (dock UI), `9d01688` (removals) and `3354689` (mount fix); build OK; partial device verification.
- **Revision 1.6 (2026-10-11):** OBD ledger amendment (Decision 13). Decision 2 (consumption source = ledger), Decision 5 (RANGE = 1-minute rolling OBD window, in memory), Decision 6 (`resetCoveredBaselineKm` removed in the design change), Alternatives 5 to 7, Consequences, Related ADRs, Open items and this summary updated. Docs only; ledger code not started. Planned Room v16 → v17 (`MIGRATION_16_17`). PRD FR-22 v0.28, ARCHITECTURE v0.36, UI-SPEC v0.49, IMPLEMENTATION-PLAN v0.64, adr/README v1.8 and DOCUMENT-CONTROL v0.12 updated to match. Revision 1.1 to 1.5 wording above is kept as history. **Design change 2026-10-11 (owner, supersedes option (a)):** RANGE uses a 1-minute rolling OBD window (in memory; Decision 5). The ledger has one column (`fuelConsumedL`); `resetCoveredBaselineKm` and its ALTER TABLE are dropped. Any 'since the latest Reset' RANGE wording in this revision is superseded.
