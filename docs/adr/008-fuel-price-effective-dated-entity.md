# ADR-008: Fuel Price as an Effective-Dated Entity (completed-trip cost + separated active metrics)

**Status:** Accepted
**Date:** 2026-07-07
**Decided By:** Claude Code (implementation) + project owner

## Context

FR-12 introduced a fuel cost (`cost = litres × price`) shown on the active Session card and the Trips active-trip row, backed by a single current price in a DataStore scalar (`obd_fuel_price_per_liter`) with in-memory undo/redo (the first FR-12 cut, commit `c14627e`). Two new requirements break that model:

1. **Completed trips must show cost too**, and their cost must not silently change when the price is edited later — i.e. each finished trip must be priced by the value in effect at the time it happened.
2. A single scalar cannot answer "what was the price when trip N ran?" — it only knows the current value.

The project owner directed that price be stored as **its own entity**, separate from the trip and session rows (not a per-trip column snapshot).

## Decision

### `fuel_price` table (new entity)
`FuelPriceEntity(id: Long PK auto, pricePerLiter: Double, effectiveFromMs: Long @Index)` — an append-only, effective-dated log of price changes.
- **Current price** = the row with the greatest `effectiveFromMs`.
- **Save / Undo / Redo each append a new row** with `effectiveFromMs = now` and the chosen value. Undo/redo are therefore **non-destructive**: they move the *current* price for future costs but never rewrite an already-effective price, so completed-trip costs never change retroactively. The in-memory `FuelPriceController` stack still drives which value gets applied.

### Pricing rules
- **Active session / active trip** → `litres × currentPrice`.
- **Completed trip** → `litres × price effective at trip START` = the `fuel_price` row with the greatest `effectiveFromMs ≤ TrackEntity.timestamp` (which is `tripStartedAt`). If no such row exists (price first set after the trip ran), cost shows `—`.

### Migration & seed
Room **MIGRATION_8_9** (DB v8→v9) creates the empty `fuel_price` table. A one-time code seed (repository init) inserts the existing `obd_fuel_price_per_liter` DataStore value with `effectiveFromMs = 0` when the table is empty and the scalar > 0, so pre-existing trips receive a baseline price. The DataStore scalar is retired as the source of truth (may remain as a disposable cache).

### UI
- **Active-trip card** → two stat rows: base (km / duration / avg speed) + OBD row (instant km/L / L/h / trip-average km/L / cost); card grows taller to fit.
- **Completed-trip row** → adds a **cost** stat (km / duration / avg speed / average km/L / cost).

## Consequences

### Positive
- Completed-trip costs are historically correct and never re-cost when the price changes.
- Price is a first-class, queryable entity; no per-trip column bloat.
- Undo/redo remain, reconciled as non-destructive appends.

### Negative
- A schema change + migration (v8→v9) and one extra small query per completed-row render (cheap; can be batched).
- Superseding wording: the FR-12 v1 claim of "no schema change / price not persisted" (commit `c14627e`) is now obsolete.

## Alternatives Considered
1. **Per-trip price column on `TrackEntity` (snapshot at close):** rejected — owner wanted price as a separate entity, and a column duplicates the price onto every trip.
2. **Keep the DataStore scalar, live re-price completed trips:** rejected — editing the price would retroactively change historical costs.
3. **Price at trip *close* instead of start:** rejected per owner — trip-start is the chosen effective point.

## Related ADRs
- [[007-unified-fuel-economy-metrics]] (the fuel pipeline this cost sits on; this ADR revises only pricing + persistence + the active-row layout).

## References
- PRD FR-12 (updated), ARCHITECTURE §2/§8, UI-SPEC §3a/§3b/§4.
- `FuelPriceEntity`/`fuel_price` DAO, `FuelPriceController`, `TrackItemRow` (completed cost), `ListContent.ActiveTripRow` (two-row layout), `SessionsScreen.ObdStatusCard`.
