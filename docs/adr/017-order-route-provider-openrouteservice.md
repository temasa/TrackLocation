# ADR-017: Order Route Provider — OpenRouteService (supersedes Google for Gojek order routes)

**Status:** Accepted
**Date:** 2026-10-06
**Decided By:** Project owner + Claude Code

## Context

ADR-016 established order route overlay (planned + runtime routes) for Gojek orders using Google Directions + Geocoding APIs. However, the Google Maps Platform billing account is unavailable for this project: calls to Geocoding and Directions APIs return `REQUEST_DENIED` with "You must enable Billing" error. This blocks the ADR-016 feature from working.

ADR-011 had already chosen OpenRouteService (free tier, no card required) as the provider for ADR-009 navigation (follow-a-route on the Track screen). A re-evaluation shows ORS is viable for order route overlays with documented accuracy caveats and a geocoding fallback ladder to handle Indonesian address ambiguity. Switching to ORS unifies the provider choice, eliminates the billing barrier, and keeps the door open for future migration to self-hosted OSM or other adapters (ADR-011 connector-adapter pattern).

## Decision

1. **Provider switch:** The order route overlay (ADR-016) switches from Google Directions + Geocoding APIs to OpenRouteService (same as ADR-011 for ADR-009 navigation). All other behavior in ADR-016 (planned frozen route, runtime route re-fetch on phase-change/off-route throttled >40m/30s/100m, markers, clear on completion, fail-soft, display-only) remains unchanged.

2. **Routing:** `POST https://api.openrouteservice.org/v2/directions/driving-car` with `Authorization: <api_key>` header, JSON body `{"coordinates": [[lon,lat],...], "radiuses": [-1,...]}`. Response `routes[0].geometry` is an encoded polyline (precision 5) decoded with `android-maps-utils` PolyUtil.decode; `routes[0].summary.distance`/`duration` extracted.

3. **Geocoding:** `GET https://api.openrouteservice.org/geocode/search?text=<place>&boundary.country=ID&size=1&focus.point.lat=<lat>&focus.point.lon=<lon>` (Pelias on OSM). Tested behaviour: landmark/venue names match well (e.g., Pondok Indah Mall); street names match at street level (e.g., Jalan Cipete Raya); long comma-separated Gojek-style addresses (street, sub-district, district, city) often return no result; house numbers fall back to city-level results (confidence ~0.6, unreliable).

4. **Geocoding fallback ladder (required to improve reliability):**
   1. Place name (pickupName/dropName) + city/area hint → search with `text=<name>&focus=driver_location`
   2. Full address (as-provided) → search with `text=<address>&focus=driver_location`
   3. Normalize 'Jl.' → 'Jalan', progressively drop district/sub-district words; retry
   4. Street name only (if multi-part address) → `text=<street>&focus=driver_location`
   - Reject results whose `layer` is `locality`, `localadmin`, `county`, `region`, `macroregion`, or `country` (misleading city-centre pins are worse than none).
   - Reject results with `confidence < 0.8` unless `layer` is `venue` or `street`.
   - Bias all queries with `focus.point.lat=driver_current_lat`, `focus.point.lon=driver_current_lon` (driver's current location).
   - If all four ladder steps fail: no route/markers are drawn for that order (fail-soft), retry once per 60 s max, and at most 3 geocode attempts per order (quota guard: the ladder makes up to 6 requests per address, so an unmatchable address must not retry forever; after 3 failed attempts no route is drawn for that order).

5. **API key storage:** `OPENROUTESERVICE_API_KEY` in `local.properties` (user-provided). Injected as a string resource (e.g., `ors_api_key` resValue in `app/build.gradle`), never hardcoded or logged. The Google Maps key is retained for the map SDK only; Geocoding/Directions are no longer used.

6. **Architecture:**
   - Small `RouteProvider` port interface: `suspend fun geocode(placeName: String?, address: String, focus: LatLng?): List<LatLng>?` and `suspend fun route(origin: LatLng, destination: LatLng, waypoint: LatLng?): RouteResult?` (same as ADR-011's port).
   - `OpenRouteServiceClient` implementation replacing `GoogleRouteClient` (not adding a second provider).
   - `OrderRouteController` (unchanged) passes place names + current location to geocoding (per ADR-016 §2 Geocoding decision).

7. **Accuracy caveats (documented for user expectations):**
   - Street-level pins can be several hundred metres off on long streets.
   - Routes may differ from Gojek's own internal routing (best-effort; not a promise of alignment).
   - Less accurate than Google for house numbers and niche landmark names.
   - Future upgrade path: HERE (backend) or self-hosted ORS (portability).

8. **Attribution (required by ORS + OpenStreetMap):** While a route is on screen, display '© openrouteservice.org | © OpenStreetMap contributors' as small text (provisional placement: bottom-start above the order card, or top-start; final placement from design handoff). Attribution must be visible whenever routes are shown and hidden when no active route.

9. **Free-tier limits (to be re-verified during implementation):** Observed rate-limit header ~200 requests / window. Roughly 2,000 directions + 1,000 geocode requests per day available. App usage: ~3–5 calls per order with throttling — ample headroom.

10. **Privacy:** Only pickup/drop place name + address strings and the driver's current coordinates (for geocoding focus) are sent to ORS; customer name/phone never sent (ADR-013). Display-only; not persisted; canonical location log untouched.

11. **Cost:** Free tier, no billing required.

## Consequences

### Positive
- Eliminates Google billing dependency and REQUEST_DENIED errors; feature is now workable.
- Unifies provider choice across ADR-009 (navigation) and ADR-016 (order routes) — both use ORS.
- Connector-adapter pattern (ADR-011) keeps the door open for future provider migration (HERE, self-hosted ORS) with no code changes outside the adapter.
- Geocoding fallback ladder improves reliability for Indonesian addresses compared to ORS's bare-API default.
- Free tier and no standing costs.

### Negative / Accepted Risks
- **Accuracy lower than Google** for house numbers and complex addresses; addressed by fallback ladder and fail-soft (no route is better than a wrong route).
- **Street-level pins can be metres off** on long roads; user can ignore incorrect pin and follow Gojek's own route.
- **Geocoding may fail entirely** for long Gojek-style comma-separated addresses; mitigated by ladder (progressively trim components) and fail-soft (trip records even if no route shows).
- **Free-tier rate limits** (~200 req/window); mitigated by throttling and low order volume per typical user. No per-user quota implemented (acceptable for this scale).
- **Attribution surface required** on the map adds UI complexity (small text region); necessary to comply with ORS + OSM license.

## Alternatives Considered

1. **Remain with Google (enable billing or find alternative key).** Rejected: project constraints prevent card-based billing; no alternative Google key available.
2. **Use HERE (alternative paid provider).** Deferred: better address quality, but requires separate signup and card. Viable as a future adapter (same RouteProvider interface).
3. **OSRM or Nominatim public servers (free OSM-backed).** Rejected: public server usage policies forbid commercial app use; self-hosted instances required (infrastructure burden).
4. **Android Geocoder (Google-backed, on-device).** Rejected: unreliable in testing (flaky results); Google-backed ties to the billing problem.
5. **Mapbox (card + geocoding storage terms).** Rejected: storage terms restrict caching of geocoding results; usage model incompatible.

## Related ADRs

- **ADR-011** (Routing Engine via Connector-Adapter) — establishes the RouteProvider port interface; this ADR re-adopts ORS for Gojek order routes (ADR-016 had chosen Google).
- **ADR-016** (Gojek Order Route Overlay) — defines behavior (planned + runtime routes, markers, throttling, fail-soft); this ADR changes only the provider.
- **ADR-013** (Observer Trip Extraction) — provides pickup/drop addresses used by this ADR's geocoding.
- **ADR-015** (Gojek Order Lifecycle Drives the Trip) — manages order phases (PICKUP/DROP/FINISHED); routes key off these phases.
- **ADR-009** (Dual-mode Track Navigation) — also uses ORS via ADR-011 (same provider, different use case).

## References

- OpenRouteService Directions API: https://openrouteservice.org/docs/
- OpenRouteService Geocoding (Pelias): https://openrouteservice.org/docs/#/reference/Geocoding/Structured%20Geocoding
- android-maps-utils PolyUtil: https://github.com/googlemaps/android-maps-utils/blob/master/library/src/main/java/com/google/maps/android/PolyUtil.java
- ADR-011 (RouteProvider interface + OpenRouteServiceAdapter pattern)
- ADR-016 (Behavior design: planned + runtime routes, markers, throttling)
- PRD.md FR-17 (feature requirement, now with ORS provider)
- ARCHITECTURE.md Gojek Order flow (ROUTE section updated with ORS details)
- UI-SPEC.md §3e Route overlay (attribution surface)

---

## Status Summary

- **ADR Accepted:** 2026-10-06 — Supersedes the Google provider choice in ADR-016; all other ADR-016 behavior (planned frozen, runtime throttled re-fetch, fail-soft) unchanged.
- **Docs Updated:** 2026-10-06 — PRD (FR-17 provider + accuracy caveat), ARCHITECTURE (ROUTE section geocoding ladder + ORS details), UI-SPEC (§3e attribution surface + handoff note), README (API key setup), IMPLEMENTATION-PLAN (new slice), adr/016 (status note), adr/011 (pointer note), adr/README (ADR-017 index row), DOCUMENT-CONTROL (register + change log).
- **Code Implementation:** Pending user build permission (AGENTS.md §5a).
- **Design Handoff:** Attribution placement subject to design review per AGENTS.md §12.

---

**Amends:** ADR-016 (provider change from Google to ORS; behavior decisions in ADR-016 remain in force).
