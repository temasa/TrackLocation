# ADR-007: Unified Fuel-Economy Metrics (instant two-cell + single averaging derivation)

**Status:** Accepted
**Date:** 2026-07-07
**Decided By:** Claude Code (implementation) + project owner

## Context

Fuel economy is shown two ways and they are inconsistent today. The **instant** metric toggles units (km/L moving, L/h idle) in one cell — users found the switching confusing. The **averages** use two code paths and two *different distance definitions*: session avg = `obdGpsDistanceKm` (Σ speed×dt) ÷ `obdFuelConsumedL`; trip avg = `distanceInMeters` (displacement) ÷ `obd_sample` fuel. The session average's distance does not even match the distance shown on its own card, and session vs trip can diverge for the same drive.

The split was pragmatic: a session has a live DB row to accumulate into from the start; a trip has none until stop (FR-03: empty trips not saved), so it must derive from `obd_sample`. PRD §12 (BR-02) makes `location_log` the canonical source of truth; `obd_sample` is the canonical OBD stream (BR-10, recorded only during sessions). ADR-006 dwell-collapse reduced `location_log` row counts, making range-based distance integration cheap.

## Decision

### Instant metric — two always-on cells (Session card + Trip panel)
- **km/L**, shown only when moving (speed > ~3 km/h with a good fix); `—` at rest.
- **L/h**, always shown (current `fuelRateLph`).
- No unit toggling. Averages remain a single km/L.

### Average metric — one derivation for both session and trip
`avg km/L = displacement distance ÷ fuel`, over the range:
- **distance** = Σ `distanceBetween` over adjacent `location_log` samples = the *displayed* distance (`session.distanceMeters` / trip `distanceInMeters`). Chosen for consistency with the on-screen distance (eye-checkable), accepting slight curve under-read; not speed-integral.
- **fuel** = Σ `fuelRate × dt` over `obd_sample` in the time window `[startedAt, now]`.
- **Live value** = O(1) incremental cache (session: `obdFuelConsumedL` column; trip: in-memory total since `tripStartedAt`).
- **Authoritative at close** = re-integrate `obd_sample` over the final window and persist that. `obd_sample` is the single source of truth for fuel; the live cache is a disposable proxy (Option B: cache = memoized integral of the canonical rows).
- **Mid-drive restart**: reseed the in-memory trip total by one `obd_sample` integration, then continue incrementally.
- Displays a value once distance > 0.01 km (and fuel > 0), else `—`.
- **Idle**: fuel accrues, distance flat → the average degrades correctly (relies on the ADR-006 stale-speed fix).

### Schema
`obdGpsDistanceKm` becomes unused (session average switches to `session.distanceMeters`). Platform SQLite below API 34 lacks `ALTER … DROP COLUMN`, so a clean drop uses a **destructive** migration **v7→v8** scoped via `fallbackToDestructiveMigrationFrom(7)` (local test data discarded; acceptable pre-production). Follow-up task: remove even the scoped fallback before shipping.

## Consequences

### Positive
- One definition; session and trip averages agree and each matches its own displayed distance.
- `obd_sample` is the single fuel source of truth; persisted/at-close numbers cannot drift from the canonical rows (self-healing at close/restart).
- Live UI stays O(1) on a constrained device; expensive integration happens only at close/restart, not per tick.
- Instant metric is unambiguous (km/L and L/h side by side).

### Negative
- Distance uses displacement (adjacent-sample straight lines) → slightly under-reads on curves vs speed-integral. Accepted for consistency with the displayed distance.
- Destructive v7→v8 migration wipes local data (fine while testing); scoped fallback + a pre-production removal task mitigate the footgun.
- Live cache and at-close re-integration can differ by a rounding hair; the re-integrated value wins and is stored.

## Alternatives Considered
1. **Pure recompute from raw rows every refresh (Option A):** maximally canonical but O(N) per tick — rejected for perf on the constrained device (Option B gives identical numbers at O(1)).
2. **Keep the split accumulators:** rejected — inconsistent distances, drift risk, session avg ≠ shown distance.
3. **Speed-integral distance:** marginally truer path length but does not match the displayed distance — rejected for consistency.
4. **Data-preserving v7→v8 rebuild to drop the column:** rejected while testing — unnecessary risk/effort; destructive is justified with no production data.

## Related ADRs
- [[006-location-dwell-collapse]] (makes range distance cheap; provides the stale-speed fix the idle-degradation relies on)
- [[004-obd-raw-at-io]] (fuelRate source; unchanged — this ADR revises only aggregation)

## References
- `docs/ARCHITECTURE.md` §8 (OBD Phase 2 accumulation — revised by this ADR)
- `ObdPollingService` (fuel/distance integration), `ShareViewModel.onTripCtaTap` (trip close), `SessionsScreen`/`TripPanel` (display)
- PRD §12 BR-02/BR-10, FR-03
