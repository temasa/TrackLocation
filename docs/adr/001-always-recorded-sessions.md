# ADR-001: Always-recorded Location Sessions + Canonical Location Log

**Status:** Accepted
**Date:** 2026-05-16
**Decided By:** Project owner (CR-0001)

> Migrated 2026-06-15 from CR-0001 in the legacy `docs/change-requests.md`.

## Context

The app originally treated recorded location data mainly as trip-owned data. The product needs continuous location recording independent from trips, while still allowing users to define explicit trips.

## Decision

Introduce always-recorded location sessions backed by a canonical, append-only location log. Trips become explicit ranges over that log:

- Canonical `location_log` rows are the source of truth for GPS points.
- `recording_session` rows capture ON→OFF always-recording periods.
- `TrackEntity` gains `startLocationId` / `endLocationId`; trips resolve their path from location ranges.
- Room migration v1→v2 converts legacy serialized trip paths into canonical location rows + trip boundaries.

## Consequences

### Positive
- Continuous location history independent of trips.
- Deleting a trip never loses location history (only the trip row is removed).
- Clean separation enables sessions and trips to both derive from one source.

### Negative
- Schema migration complexity (legacy path conversion).
- Two concepts (sessions vs trips) users must understand.

## Alternatives Considered
1. **Keep trip-owned location data:** simpler, but cannot record continuously outside trips. Rejected.
2. **Separate logs for sessions and trips:** duplicates data, breaks single-source-of-truth. Rejected.

## Related ADRs
- [[002-session-recording-switch]] (control surface for this feature)

## References
- `docs/PRD.md` §12 (BR-01…BR-06)
- `docs/ARCHITECTURE.md` §2, §8
- `docs/IMPLEMENTATION-PLAN.md` Appendix A (CR-0001), Appendix B (2026-05-16)
- Implementation commit `776cd6e`
