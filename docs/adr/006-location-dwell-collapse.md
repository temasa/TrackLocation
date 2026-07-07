# ADR-006: Location Dwell Collapse (canonical log stores one anchor per stop)

**Status:** Accepted
**Date:** 2026-07-07
**Decided By:** Claude Code (implementation) + project owner

## Context

GPS fixes jitter within their accuracy tolerance while the vehicle is stationary. Today every fix inserts a new `location_log` row (`TrackingService.recordLocation`) with no displacement filter, so a parked vehicle produces a stream of near-duplicate rows. Worse, `updateActiveSession` adds the inter-fix jitter distance to `session.distanceMeters` on every fix, inflating recorded distance while stationary ("phantom distance"). PRD §12 BR-02 locks the canonical log as the source of truth, so changing what it stores requires a recorded decision.

Verified precondition: **no consumer joins `location_log` by timestamp** — sessions and trips read it only by id range (`LocationDao.getLocationsByRangeOnce`, used in `ShareViewModel.getPathPointsForTrack`), and `obd_sample` is never joined to it. So a single row spanning a long dwell breaks nothing downstream.

## Decision

Collapse stationary fixes at **write time**. The service holds an in-memory **dwell anchor** (row id + position). For each incoming fix:

- **Within tolerance** `max(15 m, 1.5 × accuracy)` → do not insert; `UPDATE` the anchor row's `timestamp` to this fix's time and increment `collapsedCount`. No session/trip distance is added.
- **Outside tolerance, confirmed by 2 consecutive out-of-radius fixes** (single-outlier rejection) → finalize the anchor, `INSERT` a new row as the new anchor, resume normal per-fix logging while moving.

Two new `LocationEntity` / `location_log` columns:

- `dwellStartTimestamp: Long` — set once at anchor insert (arrival), never bumped.
- `collapsedCount: Int` (default 1) — number of fixes folded into the anchor.

`timestamp` becomes "last confirmed still" (departure); together with `dwellStartTimestamp` a stop retains both arrival and departure without storing raw intra-dwell fixes. Anchor position stays fixed (no drift). Schema change → Room `MIGRATION_6_7`, DB version 7 (backfill `dwellStartTimestamp = timestamp` for existing rows). On app/service restart mid-dwell the anchor is lost and the next fix starts a fresh anchor (consistent with existing resume-on-restart behavior).

## Consequences

### Positive
- Storage during stops drops from N rows to 1.
- Phantom distance eliminated — parked jitter no longer inflates session/trip distance.
- A stop renders as a single clean dwell vertex; arrival + departure + fix count preserved.

### Negative
- Raw intra-dwell fixes are not retained → cannot later re-tune the tolerance against historical stops, and cannot distinguish a dead stop from a slow crawl below tolerance.
- `session.durationMillis` is not refreshed during a collapsed dwell until movement resumes (it is recomputed on the next real insert). Acceptable; can be bumped on the dwell UPDATE if desired.
- Adds an `UPDATE` DAO path and in-memory anchor state to `TrackingService`.
- Collapse must still refresh live speed/position/accuracy into `locationUiState` during a dwell (without inserting a row or adding GPS distance); otherwise OBD's time-integrated distance and its idle km/L-vs-L/h detection read a stale frozen speed — a regression found and fixed 2026-07-07 (see `docs/ERRORS-LOG.md`).

## Alternatives Considered

1. **Raw-as-truth + collapse only on read/render.** Pros: nothing ever lost, fully re-tunable. Cons: zero storage savings — defeats the stated goal. Rejected.
2. **Plain `LocationRequest.smallestDisplacement`.** Pros: trivial. Cons: silently drops jitter fixes and loses the last-seen timestamp — the exact thing we want to keep. Rejected.

## Related ADRs
- [[001-always-recorded-sessions]] (canonical log)

## References
- `docs/PRD.md` §12 (BR-02; new BR-11 on acceptance)
- `docs/ARCHITECTURE.md` §4, §8 (schema + migration on acceptance)
- `TrackingService.recordLocation`, `ShareViewModel.getPathPointsForTrack`
