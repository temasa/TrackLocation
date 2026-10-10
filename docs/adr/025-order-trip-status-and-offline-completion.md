# ADR-025: Order Trip Status and Offline Completion

**Status:** Accepted
**Date:** 2026-10-10
**Decided By:** Project owner + Claude Code

**Amends:** ADR-015 (cancel no longer unconditionally auto-stops the trip; cash-cancelled orders keep the trip until the driver stops it). See ADR-015 "Amendment 2026-10-10".

## Context

ADR-015 ends an order-owned trip as soon as the Gojek order reaches a terminal state (Selesai, Cleared, Cancelled, Dismissed) and saves it with no outcome. The Trips list therefore cannot tell a normal completed order from a cancelled one, and a cancelled cash order can end before the driver has actually reached the drop-off, where the job may still be finished offline.

The product owner decided:

- Every order-owned trip card shows a status chip. Statuses are **Completed**, **Completed · Offline**, and **Cancelled**. Manual trips (no order) show no chip.
- The status is persisted on the trip row at save time (not derived at display time), because `observer_trip` rows are pruned after 90 days / 5,000 rows (ADR-013) while the trip must keep its outcome (same reasoning as the ADR-023 geo snapshot).
- The offline rule applies **only to cash orders**. Non-cash orders behave as before, with a Cancelled status.

## Decision

1. **Status model.** `track.orderStatus` is `COMPLETED`, `CANCELLED`, or NULL (manual trips and pre-v14 manual trips). `track.orderOffline` is a boolean flag that is meaningful only when `orderStatus = COMPLETED`. "Offline" is a flag on the Completed status, not a third status value.

2. **Observer phase.** `OrderCardParser` yields a new phase `CANCELLED` for the cancel message (`Oke, sip` + text containing `nge-cancel`). It is distinct from `FINISHED`. The `observer_trip.phase` column is a string, so no observer schema change is needed. The Selesai screen (`FINISHED`) and the Gojek home screen "Cleared" (`FINISHED`) are unchanged. Every place that treats only `FINISHED` as terminal (trip end, route clear, card hide, dismiss) must also treat `CANCELLED` as terminal.

3. **Cash-order detection.** Payment is cash when `observer_trip.payment` equals `Tunai` (case-insensitive). The match string lives in `GojekRules` / `OrderCardRules` next to the other Gojek strings. A missing or non-cash payment is treated as not cash.

4. **Stop rules.**

   | Trigger | Auto-stop? | Status saved | `orderOffline` |
   |---|---|---|---|
   | Normal finish (Selesai / Drop off → `FINISHED`) | Yes (ADR-015) | `COMPLETED` | `false` |
   | Home screen "Cleared" (`FINISHED`) | Yes (ADR-015) | `COMPLETED` | `false` |
   | Cancel (`CANCELLED`), payment not cash or missing | Yes, immediately | `CANCELLED` | `false` |
   | Cancel (`CANCELLED`), payment cash, drop coordinates unresolved | No, the trip keeps recording; no snackbar | `CANCELLED` at the driver's eventual stop | `false` |
   | Cancel (`CANCELLED`), payment cash, drop coordinates resolved | No, the trip keeps recording until the driver stops it | see row below | see row below |
   | Stop of a cash-cancelled trip (snackbar **Stop** or the normal Stop CTA), ended within 150 m of the drop-off | Driver stop | `COMPLETED` | `true` |
   | Stop of a cash-cancelled trip, ended farther than 150 m from the drop-off | Driver stop | `CANCELLED` | `false` |

   "Within 150 m" is measured by great-circle distance from the last known fix at the moment of the stop, against the cached drop coordinates (the geocoded LatLng already used for the drop marker, ADR-016 / ADR-017).

5. **Drop-off snackbar (cash-cancelled trips only).** When the driver first comes within 150 m of the drop-off point after a cash cancel, the Track screen shows one non-blocking snackbar: "Order cancelled in Gojek. You're at the drop-off. Stop trip?" with action **Stop**. It appears at most once per trip. Ignoring it does nothing, and the normal Stop CTA remains available. The app never auto-closes the trip. The snackbar appears only while TrackLocation is on screen; no system notification or takeover is used (by design). See UI-SPEC §3e.

6. **Status chip.** Shown on the order-linked completed trip rows in the Trips list (UI-SPEC §3b-order). The live (in-progress) trip row shows no chip, because the status is written at save time. Variants: `Completed`, `Completed · Offline`, `Cancelled`.

7. **Schema: Room DB v13 → v14 via `MIGRATION_13_14`.**
   - `ALTER TABLE track ADD COLUMN orderStatus TEXT` (nullable; `'COMPLETED'` or `'CANCELLED'`)
   - `ALTER TABLE track ADD COLUMN orderOffline INTEGER NOT NULL DEFAULT 0`
   - Backfill: `UPDATE track SET orderStatus = 'COMPLETED' WHERE orderLabel IS NOT NULL`

   Migration backfill rule: existing order-linked trips (`orderLabel IS NOT NULL`) are marked `COMPLETED`. Manual trips keep NULL and show no chip.

8. **ADR-015 amended** (see ADR-015 "Amendment 2026-10-10"). The rule that cancel always auto-stops is replaced by the table in decision 4 for cash orders. The Selesai, Cleared, and Dismiss rules are unchanged.

9. **Unchanged:** always-recording stays ON (BR-13). The 2-hour card-staleness window still does not end a trip. Navigation trips stay manual-end (ADR-009). Route clearing (ADR-016) also runs on `CANCELLED`.

## Consequences

### Positive
- The Trips list shows the outcome of each order (Completed / Completed · Offline / Cancelled) instead of treating every ended order alike.
- A cash order cancelled in Gojek can still be completed offline, with the trip kept until the driver decides, and recorded as such.
- No observer schema change: `phase` is already a string column.
- The status is persisted, so it survives `observer_trip` pruning and process restarts.

### Negative / accepted risks
- **Open risk (verify on device):** the Gojek home screen ("Cleared") may appear immediately after the cancel message. ADR-015 device evidence shows the home screen follows the cancel dialog within about a second. If the home screen writes `FINISHED` onto the cancelled order row during an offline (cash) trip, the trip would stop early as `COMPLETED` with `orderOffline = false`, bypassing the snackbar and the 150 m check. **Resolved 2026-10-10:** a `FINISHED` after `CANCELLED` is ignored, because `CANCELLED` rows are terminal (not open) and the recorder only updates open rows. Related owner decisions: the normal `TripPanel` is shown during a cash-cancelled trip; the snackbar is on-screen only; the live row shows no chip; unresolved drop coordinates keep the trip recording and it is saved `CANCELLED`. Device confirmation remains (IMPLEMENTATION-PLAN §4 step 11).
- **Snackbar only while the app is on screen:** if the driver is in Gojek or another app when they reach the drop-off, nothing prompts them. The trip keeps recording until the driver opens TrackLocation and stops it. This is accepted by design.
- **Pre-v14 history is not distinguishable:** migrated order trips that were auto-stopped on cancel are backfilled as `COMPLETED`, because the old cancel outcome was not stored. History is not rewritten.
- **Drop-coordinate dependency:** the 150 m rule needs resolved drop coordinates. If geocoding failed, a cash-cancelled trip is never offered the snackbar and is saved as `CANCELLED` at stop.
- **Detection strings:** the cancel message and `Tunai` match depend on exact Gojek text (ADR-015 risk (e)). A changed string silently falls back to the non-cash path (stop immediately, Cancelled).

## Alternatives Considered

1. **Third status value `COMPLETED_OFFLINE`:** rejected. Offline describes how a completed job was closed, not a different outcome. A flag keeps the status set to two values and matches the product wording "Completed · Offline".
2. **Derive the status at display time from `observer_trip.phase`:** rejected. Observer rows are pruned (90 days / 5,000 rows, ADR-013), so the outcome would be lost.
3. **Auto-stop the cash-cancelled trip and ask afterwards:** rejected. The trip would stop while the driver may still be driving to the drop-off, losing the distance and the 150 m evidence.
4. **System notification or full-screen prompt for the drop-off:** rejected by product decision (no system notification or takeover). The snackbar is the only prompt.
5. **Convert `phase` to a Room enum column:** rejected. It would be a schema change for no gain; the string column already carries `CANCELLED`.

## Related ADRs

- [[015-order-auto-start-trip]] — amended by this ADR (cancel rule).
- [[014-gojek-order-card-takeover]] — the takeover and card that this ADR extends.
- [[013-observer-trip-extraction]] — `observer_trip` phase values and pruning window.
- [[016-order-route-overlay]] / [[017-order-route-provider-openrouteservice]] — drop coordinates and route clearing on terminal state.
- [[022-order-trip-label-in-trips-list]] / [[023-order-trip-header-card-and-geo-snapshot]] — order-linked trip rows, where the chip is shown.

## References

- PRD.md — FR-16 (amended, cancel rule and chip), FR-20 (Trips list order row).
- ARCHITECTURE.md — Gojek Order-Card Takeover Flow (trip end), `track` columns, DB v14 / `MIGRATION_13_14`.
- UI-SPEC.md — §3b-order (status chip), §3e (drop-off snackbar).
- IMPLEMENTATION-PLAN.md — §4 "Order Trip Status + Offline Completion (ADR-025)" (How to Verify), §6 task log.
- Code (pending approval): `OrderCardParser`, `OrderCardRules` / `GojekRules`, `OrderTripRecorder`, `ShareViewModel` (stop rules, snackbar), `TrackDatabase` (`MIGRATION_13_14`), `TrackEntity`, `TrackItemRow`.
