# ADR-011: Routing Engine via Connector-Adapter (OpenRouteService now, portable to self-hosted OSM)

**Status:** Accepted
**Date:** 2026-07-08
**Decided By:** Claude Code (planning) + project owner

## Context

ADR-009 (dual-mode Track navigation) needs a routing engine to compute road routes for destinations not covered by the ADR-010 local route store. The engine choice was left open. Constraints: a personal, cost-sensitive Indonesia app; ongoing billed Google API use is undesirable; and ADR-010 already serves road-ahead from the user's own traces for free, so the external engine is only called for genuinely-new roads (low volume). The owner chose OpenRouteService (hosted free tier) now, with a firm requirement that a later migration to self-hosted OSM be trivial — achieved via a connector-adapter (ports-and-adapters) design.

## Decision

- **Port interface `RoutingEngine`** (domain-facing): `suspend fun route(origin: LatLng, destination: LatLng, options): RouteResult` returning `RouteResult(polyline: List<LatLng>, distanceMeters: Int, durationSeconds: Int)`. Navigation code depends only on this port, never on a concrete provider.
- **First connector `OpenRouteServiceAdapter`:** calls `POST /v2/directions/driving-car` (JSON) with API-key auth; maps `routes[0].summary.distance`/`duration` and decodes `routes[0].geometry` (an encoded polyline — reuse the app's existing polyline decoding).
- **Portability contract:** a self-hosted ORS instance exposes the identical `/v2/directions/driving-car` API, so porting hosted→self-hosted is a **base-URL + auth swap inside the same adapter** (configuration, not a rewrite). A different engine (OSRM/Valhalla/Google) is a **new adapter** implementing the same port; navigation code is untouched.
- **Static ETA:** ORS has no live-traffic durations, so ETA is a static estimate. This supersedes the provisional traffic-aware ETA noted in ADR-009. A future Google adapter could restore live traffic.
- **Secrets:** the ORS API key is not hardcoded (AGENTS §13) — supplied via resource/`local.properties`/build config, like the existing Maps key.
- **Attribution:** ORS/OSM require visible attribution ("© openrouteservice.org | © OpenStreetMap contributors") wherever routes are shown; recorded as a UI-SPEC surface.

## Consequences

### Positive
- Zero routing cost now (free tier, kept low by ADR-010); no server to run yet.
- Provider is swappable behind one interface — self-hosted ORS = config change; OSRM/Valhalla/Google = an isolated new adapter (no one-way door).
- Encoded-polyline geometry reuses the app's existing decode path.

### Negative
- Static ETA (no live traffic) until/unless a traffic-capable adapter is added.
- Free tier has a daily cap (≈2,000 directions/day — verify current); heavy new-road exploration could approach it (mitigated by ADR-010).
- Adds an HTTP client + JSON mapping dependency and a UI attribution requirement.

## Alternatives Considered
1. **Google Directions/Places:** live traffic + existing key, but paid and ToS-restrictive on caching; deferred (can return later as an adapter if live traffic is wanted).
2. **Self-hosted OSM now (OSRM/Valhalla):** free/unlimited but requires standing up + maintaining a server; deferred — the adapter makes this a later, low-cost migration and the base for ADR-010 Stage B.
3. **Hardwire ORS with no abstraction:** rejected — the owner explicitly wants easy portability; a port/adapter avoids a one-way door.

## Related ADRs
- [[009-dual-mode-track-navigation]] (the navigation feature this serves; resolves its routing-engine open question)
- [[010-self-learning-route-store]] (serves road-ahead free; this engine is the fallback for roads not yet driven)

## References
- Verified: OpenRouteService `POST /v2/directions/{profile}` (JSON: `summary.distance`/`duration`, encoded-polyline `geometry`); identical API on self-hosted instances (`/ors/v2/directions/...`).
- Code (planned): `feature/nav/routing/RoutingEngine.kt` (port) + `OpenRouteServiceAdapter.kt`; reuse `LocationUtils` polyline decode.
