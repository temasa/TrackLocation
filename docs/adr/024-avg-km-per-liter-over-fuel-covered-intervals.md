# ADR-024: Average km/L over Fuel-covered Intervals

**Status:** Accepted
**Date:** 2026-10-09
**Decided By:** Project owner + Claude Code

## Context

The Session screen "SESSION AVG" km/L was observed above 700 km/L. Root cause (`ObdPollingService.kt` ~L457-461): `avgKmL = session.distanceMeters / 1000 ÷ session.obdFuelConsumedL`.

- **Numerator** — GPS distance over the WHOLE always-on session, accumulated by `TrackingService` regardless of OBD.
- **Denominator** — fuel accumulated only while OBD is connected, a valid fuel rate exists, and the poll gap is < 60 s.

Distance driven without matching fuel (OBD connected late in the session, reconnect gaps, null-fuel stretches, poll gaps >= 60 s) inflates the ratio; the only guard was fuel > 0.01 L. The same mismatch exists for the live trip average (`gpsState.distanceInMeters / tripFuelLiters`) and for completed-trip rows (`track.distance / track.obdFuelConsumedL` in `TrackItemRow`). ADR-012 listed this as an out-of-scope "partial-connect over-optimism" follow-up; this ADR resolves it.

## Decision

1. **Same-interval definition.** Average km/L = Σ covered distance ÷ Σ fuel, where BOTH sums are taken over the same "fuel-covered intervals": a poll interval in which OBD returned a valid fuel rate (0.1..100 L/h) and `0 < dt < 60 s`. Covered distance per interval = `gpsSpeedKmh` (GPS speed at that poll) × `dt`. Idle intervals (speed 0, fuel > 0) legitimately add fuel with 0 distance. Total fuel (and therefore cost, ADR-008) is unchanged.
2. **Schema — Room DB v12 → v13 via `MIGRATION_12_13`:**
   - `ALTER TABLE recording_session ADD COLUMN obdCoveredDistanceKm REAL NOT NULL DEFAULT 0.0`
   - `ALTER TABLE track ADD COLUMN obdCoveredDistanceKm REAL NOT NULL DEFAULT 0.0`
   - `ALTER TABLE obd_sample ADD COLUMN gpsSpeedKmh REAL` (nullable; GPS speed at poll time so covered distance can be re-integrated from canonical rows).
3. **Live path.** `SessionDao.addObdAccumulator(sessionId, fuelL, coveredKm)` updates both columns in one atomic UPDATE. `ObdPollingService` also keeps an in-memory trip covered-distance accumulator beside `tripFuelLiters`, reseeded from `obd_sample` (new `ObdFuelMath.integrateCoveredDistanceKm(samples)`, same guards as `integrateFuelLiters`; samples with null `gpsSpeedKmh` contribute 0) on a new trip and on a mid-trip process restart. Each `obd_sample` row now stores `gpsSpeedKmh`.
4. **Authoritative at close (ADR-007 "Option B" preserved: `obd_sample` is canonical, accumulators are memoized).** `TrackingService.stopAlwaysRecording` re-integrates both fuel and covered distance into the session row; `ShareViewModel.onTripCtaTap` writes `track.obdFuelConsumedL` and `track.obdCoveredDistanceKm`.
5. **Display.**
   - Session SESSION AVG = `session.obdCoveredDistanceKm ÷ session.obdFuelConsumedL`; "—" when covered = 0 or fuel <= 0.01 L.
   - Live trip avg (Track panel / order card / Trips active row) = `tripCoveredKm ÷ tripFuelL`, same guard.
   - Completed trip rows: km/L = `track.obdCoveredDistanceKm ÷ track.obdFuelConsumedL` when `obdCoveredDistanceKm > 0`, else the legacy `distance ÷ obdFuelConsumedL` for pre-v13 trips (history is not rewritten; no backfill).
   - Pre-v13 sessions show "—" (only the active session shows an average, so this is moot).
   - No accuracy gating is applied to covered distance: fuel must stay complete for cost, and excluding intervals only from distance would re-break the ratio.
6. **No plausibility clamp** is added: the fix removes the cause; a clamp would hide future bugs.

## Consequences

### Positive
- Numerator and denominator come from the same intervals, so the average cannot be inflated by distance that has no matching fuel.
- Closes the ADR-012 "partial-connect over-optimism" follow-up for session, live-trip and completed-trip averages.
- Total fuel and cost (ADR-008) are unchanged.
- `obd_sample` stays the canonical source; accumulators remain memoized and are re-derived at close (ADR-007 Option B).

### Negative / accepted risks
- New DB version and migration (v12 → v13); three new columns.
- The average now differs slightly from "displayed distance ÷ fuel" by design (it is OBD-covered distance).
- Pre-v13 trips keep the legacy `distance ÷ fuel` behaviour; history is not rewritten.
- Supersedes ADR-007's "one averaging definition" bullet (average = displacement ÷ fuel) and the matching ARCHITECTURE session/trip average definitions; the rest of ADR-007 stands.

## Alternatives Considered
1. **(B) Sanity cap + higher minimum-fuel threshold only.** Rejected: hides the symptom, leaves the numerator/denominator mismatch.
2. **(C) Derive the session average by integrating `obd_sample` per poll.** Rejected: no schema change but O(n) work per poll; heavier than two memoized accumulators.
3. **Use the GPS `distanceMeters` delta per interval instead of speed × dt.** Rejected: couples the average to location-log cadence and does not align with poll intervals.

## Related ADRs
- [[007-unified-fuel-economy-metrics]], [[012-obd-accumulation-recording-state]], [[008-fuel-price-effective-dated-entity]].

## References
- ERRORS-LOG ERR-006. `ObdPollingService.kt` (~L457-461 session average), `TrackingService.stopAlwaysRecording`, `ShareViewModel.onTripCtaTap`, `TrackItemRow`, `ObdFuelMath`, `SessionDao`.
