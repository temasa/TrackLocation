# ADR-010: Self-learning Local Route Store (reuse own GPS traces for free road-ahead)

**Status:** Proposed
**Date:** 2026-07-08
**Decided By:** Claude Code (grilling session) + project owner

## Context

The navigation feature (ADR-009) relies on billed routing APIs. To cost-optimize Google API usage, the owner proposed two ideas: (1) save/cache queried route results and build our own routes from them; (2) in tracking mode always save the traveled path and build our own routes from that. These are NOT equivalent:

- **Idea 1** — persisting **Google** Directions results to build a routing dataset — **violates Google Maps Platform terms** (verified against Google Maps Platform policies: pre-fetching/caching/storing content is prohibited except `place_id`; deriving an independent routing dataset from Google content is disallowed).
- **Idea 2** — building a route store from the user's **own GPS traces** — is legally clean (it is the user's own location data, not Google content) and the raw material is **already captured** in the canonical location log (`LocationEntity`, persisted while always-recording).

For a personal app the owner drives the same corridors repeatedly, so their own traces cover most roads they actually use.

## Decision

- **Reject Idea 1** (caching Google-derived results to build routes) on ToS grounds.
- **Adopt Idea 2**, staged, as a **derived, simplified local route store built FROM the canonical location log.** The canonical log remains source-of-truth and is left untouched (PRD §12 safe); the derived store is fully rebuildable from it.

### Stage A (now) — trace reuse for road-ahead + branches on familiar roads
- **Derived Room entities:** `KnownSegmentEntity` (a simplified directed polyline + its bearing) and a `CellIndex` (grid cell → segment ids). Traces are line-simplified (Douglas–Peucker) so the store stays small and fast, unlike the unbounded raw log.
- **Matching:** grid-cell binning (~30 m cells) + **bearing tolerance ±30°** to recognise "been here, heading this way" and reject the opposite direction of the same road.
- **Branches = "all routes ahead":** where past traces diverge through a junction, render **all** previously-driven continuations as translucent candidate polylines. This realises the owner's earlier "draw all possible routes where the car is heading" request — which Google's Directions/Roads APIs cannot do — but only for roads actually driven before.
- **Ingest:** on session/trip end, in the **background**, incremental and idempotent (`TraceIngester`) — never mid-drive, so the live map never stutters.
- **Query:** a `RoutePredictor` domain service (current LatLng + bearing → 0..N ahead polylines) behind a `LocalRouteRepository`; business logic kept out of Compose (AGENTS §4).
- **Fallback:** genuinely-new road (no match) → the external routing engine (parked, see ADR-009) or nothing. Once driven, that road is free thereafter.

### Stage B (deferred — its own future ADR)
Full self-learning **routable graph**: map-match + merge overlapping traces, detect intersections, build a topology, and run pathfinding (A*/Dijkstra) for point-to-point routing over the user's own network. Not built until Stage A proves its value.

## Consequences

### Positive
- Near-zero ongoing routing cost for the repeated corridors the user actually drives; external API only for genuinely-new roads, and only once each.
- Delivers the "all roads ahead" branch view for free — a feature no Google API provides.
- Fully additive to the locked canonical log; the store is derived and rebuildable, so it can never corrupt source-of-truth data.

### Negative
- Only knows roads the user has driven; cold-start is empty; first drive of any new road still needs an external API.
- Building a store from raw GPS needs map-matching/simplification/merging — non-trivial engineering (more so for Stage B).
- Prediction quality depends on GPS accuracy; a new derived store to maintain and occasionally rebuild.

## Alternatives Considered
1. **Idea 1 — cache Google Directions results to build routes:** rejected — Google Maps Platform ToS prohibits caching/storing content (except `place_id`) and building a derivative routing dataset.
2. **Live-scan the raw `LocationEntity` log on every GPS tick:** rejected — unbounded and slow; a simplified derived index is far cheaper.
3. **Build the full routable graph (Stage B) first:** rejected — that's the hard 80% before the easy 20% proves its worth; stage it.

## Related ADRs
- [[001-always-recorded-sessions]] (the canonical log this store derives from, unchanged)
- [[006-location-dwell-collapse]] (affects trace density feeding this store)
- [[009-dual-mode-track-navigation]] (the navigation feature this serves; its parked routing-engine choice is the fallback)

## References
- Verified: Google Maps Platform caching/storage policy (Places/Directions) — content not cacheable except `place_id`.
- Code: `data/roomdb/LocationEntity.kt` + `LocationDao` (the trace corpus), `data/roomdb/TrackDatabase.kt` (where derived entities/migration would live).
