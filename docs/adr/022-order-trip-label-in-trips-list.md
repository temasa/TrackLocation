# ADR-022: Order Trip Label in the Trips List

**Status:** Accepted
**Date:** 2026-10-08
**Decided By:** Project owner + Claude Code

## Context

The Gojek order-card takeover (ADR-015) brings a trip to life when an order is taken and ends it when the order finishes. However, when the driver later reviews the completed trip in the Trips list, there is no visual linkage to the original Gojek order: no label, no earnings summary, no way to know at a glance that this trip was tied to a Gojek order. The trip appears as a generic "Track #N" entry with only the distance, duration, and efficiency metrics visible.

This decision adds a persistent order label and earnings summary to each order-linked trip, stored as a snapshot on the trip row itself (no foreign key to `observer_trip`, consistent with ADR-013 "extraction is device-only, no links"). When a Gojek order trip is completed and persisted, the trip row captures the pickup and drop location names as a label (e.g., "Gojek: Senayan → Blok S") and the earnings in Rupiah (Rp). The Trips list row displays the label and a compact price/net cell (e.g., "Rp28k / Rp19.6k"), allowing the driver to quickly identify and review order-linked work without opening each trip.

---

## Decision

1. **Data model:** Add two nullable columns to the `track` table:
   - `orderLabel TEXT` — snapshot of "Gojek: <pickupName> → <dropName>" (names only, no addresses; truncated with ellipsis if necessary in the UI row).
   - `orderEarningsRp INTEGER` — snapshot of the earnings in Rupiah (integer, e.g., 28000 for Rp28k).
   - Both fields are `NULL` for manual trips and order trips from other apps; only Gojek order trips receive values.

2. **Schema migration:** Room database version 10 → 11 via `MIGRATION_10_11` (inline in `TrackDatabase.kt`):
   ```sql
   ALTER TABLE track ADD COLUMN orderLabel TEXT
   ALTER TABLE track ADD COLUMN orderEarningsRp INTEGER
   ```
   No default values; columns are nullable.

3. **Label format:** "Gojek: <pickupName> → <dropName>" where names are the extracted location names from the order card (not full addresses, which are never stored; customer names are never stored). If a name exceeds ~25 characters (or the row layout width), truncate in the UI row with an ellipsis. The label is a static snapshot captured at trip save time; it does not update if the order row is later pruned (90d/5k retention on `observer_trip`).

4. **Price and net display (in Trips list row, TrackItemRow):**
   - For order trips: two values separated by " / " (e.g., "Rp28k / Rp19.6k" or "Rp850 / Rp−1.2k").
   - First value: `price = orderEarningsRp` (earnings from the order at completion).
   - Second value: `net = orderEarningsRp − fuelCost` where `fuelCost = trip litres (from `track.obdFuelConsumedL`) × effective fuel price` (same price-per-liter as FR-12/ADR-008, priced at trip start). If `obdFuelConsumedL = 0` or no fuel price was in effect at trip start, cost shows `—` and net shows `—`.
   - Compact Rupiah format (same as FR-16 trip strip):
     - < 1,000 → 'Rp850'
     - 1,000–999,999 → 'Rp8.4k' (one decimal)
     - ≥1,000,000 → 'Rp1.2jt'
     - Negative prefixed with '−' (e.g., '−Rp8.4k' if net is negative)
   - For manual trips (no `orderLabel`): no price/net cell in the row (row remains unchanged).

5. **Capture timing:** When the trip is ended and persisted by `ShareViewModel.onTripCtaTap()` (or the auto-end via ADR-015), the latest `observer_trip` row is queried for the order (matched by timing or ID from `orderOwnsTrip` tracking). If found and in a terminal state (phase = `FINISHED`), the `orderLabel` and `orderEarningsRp` are copied from the `observer_trip` row into the newly-saved `track` row. If no order row exists or the match is ambiguous, the trip is saved without order metadata (manual trip behaviour).

6. **Backfill (best-effort):** As part of the MIGRATION_10_11 (or a one-time post-migration step), existing unlabeled `track` rows (pre-ADR-022) are matched against `observer_trip` rows based on timing:
   - For each `track` row, find `observer_trip` rows where the trip's `startedAt` falls within a window around the order's `firstSeenAt`..`lastSeenAt` (suggested window: `firstSeenAt − 2 min` to `lastSeenAt + 5 min`; exact window determined at implementation).
   - If exactly one unambiguous match exists, copy `orderLabel` and `orderEarningsRp` to the `track` row.
   - If multiple matches or the order row was pruned (90d/5k retention expired), leave the `track` row unlabeled (no false linking).
   - This is a best-effort backfill; the driver can manually infer missing labels from the trip's distance/timing if needed.

7. **Privacy:** The driver has explicitly accepted storing pickup/drop location names and earnings in the `track` table (user-deletable trips) as part of this decision, even though `observer_trip` rows are auto-pruned and non-deletable (ADR-013). Customer name and phone are still never stored (ADR-013 rule preserved).

8. **No foreign key:** Consistent with ADR-013, there is no FK from `track` to `observer_trip`. The label and earnings are a snapshot copied at save time; later pruning of the order row does not affect the persisted trip record.

---

## Consequences

### Positive
- Driver can quickly identify order-linked trips in the Trips list without opening each one.
- Earnings and net profit are visible at a glance for Gojek work, allowing the driver to review efficiency and payment accuracy.
- Snapshot approach avoids a foreign-key dependency, keeping the data model simple and resilient to order retention pruning.
- Privacy boundary is respected: names and earnings are stored, but customer PII is not; the driver controls trip deletion.

### Negative / accepted risks
- **(a) Backfill ambiguity:** If two orders occur within ~10 minutes (e.g., drop-off, then immediately accept a new order before the old one is pruned), the backfill window may match the wrong order. **Accepted:** A best-effort backfill is sufficient for existing data; new trips always capture the correct order via live `orderOwnsTrip` tracking (ADR-015). The driver can manually correct a mislabeled trip by editing the label (future enhancement).

- **(b) Snapshot stale if order details change:** If Gojek updates the pickup/drop names after the trip is saved (unlikely but possible), the trip record shows the old names. **Accepted:** Gojek order names are final before the driver accepts the order (Taken phase), so this risk is minimal.

- **(c) Order row pruned before trip deleted:** If the order row reaches the 90-day/5k-row retention limit before the driver deletes the trip, the trip still displays the label (snapshot). **Accepted:** This is the desired behavior — the trip record is independent and durable; the driver can always see the label even if the order extraction is pruned.

---

## Alternatives Considered

1. **Store a foreign key to `observer_trip`:** Join the trip to the order at display time (Trips list queries would include LEFT JOIN). **Rejected:** Breaks ADR-013's "no FK" design and adds query complexity; if the order row is pruned the trip shows `NULL` (orphaned visual). Snapshot approach is simpler.

2. **Compute net at display time from persisted `track.obdFuelConsumedL` and current fuel price:** Net = earnings − (current price × litres). **Rejected:** Net should reflect the price at trip completion, not the current price (same reasoning as FR-12/ADR-008 completed-trip pricing). Snapshot of both earnings and fuel cost is clearest. Since cost is still time-windowed (trip litres × effective price at trip start), the simplest approach is to compute net at display time from persisted earnings and the stored fuel cost, or to persist both values.

3. **Persist net alongside earnings (three columns: orderLabel, orderEarningsRp, orderNetRp):** **Rejected:** Net is derived (earnings − fuel cost) and fuel cost is already persisted in `track.obdFuelConsumedL` and the `fuel_price` table (ADR-008); computing net at display time is sufficient and avoids data redundancy. The trip record is immutable after completion, so the net will not change.

---

## Related ADRs

- **ADR-013** (Observer Trip Extraction) — Gojek extraction without FK to trips; the upstream data source.
- **ADR-015** (Gojek Order Lifecycle Drives the Trip) — auto-start/auto-end behaviour; captures the order at trip end.
- **ADR-008** (Fuel Price as Effective-Dated Entity) — completed-trip pricing model; net = earnings − (fuel litres × price at trip start).
- **FR-12** (Fuel Cost, PRD) — cost definition for trips; same compact Rupiah format.
- **FR-16** (Gojek order-card takeover, PRD) — trip strip on order card includes COST/NET; same format applied to Trips list rows.

---

## References

- PRD.md — FR-12 (fuel cost, compact Rupiah), FR-15 (trip extraction device-only), FR-16 (order-card takeover, snapshot label/earnings), FR-20 (new; order trip label in Trips list)
- ARCHITECTURE.md — `track` schema, MIGRATION_10_11, backfill window definition
- UI-SPEC.md — Trips list row (TrackItemRow) for order trips: label + price/net display
- IMPLEMENTATION-PLAN.md — new slice "Order trip label in Trips list (ADR-022)" with verification and implementation steps

---

---

## Presentation Supersession (2026-10-08)

**ADR-023 (Order Trip Header Card)** supersedes the one-line row header presentation of this ADR. The label "Gojek: <pickupName> → <dropName>" is replaced by a three-line header:
- **ACCEPTED** — place name from reverse-geocoding the trip start GPS fix
- **PICKUP** — Gojek pickup name
- **DROP** — Gojek drop name

The **orderLabel** and **orderEarningsRp** columns (this ADR's decision) remain stored and used for backfilling; the three-line display (ADR-023) supersedes their one-line presentation in the Trips list row. The price/net cell from ADR-022 is retained unchanged.

---

**Amends:** None (new decision).

**Supersedes:** None (presentation superseded by ADR-023 for row header display; data columns remain).

