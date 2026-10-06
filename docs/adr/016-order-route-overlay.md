# ADR-016: Gojek Order Route Overlay (planned + runtime routes)

**Status:** Accepted (provider superseded by ADR-017; behaviour decisions remain in force)
**Date:** 2026-10-06
**Decided By:** Project owner + Claude Code

## Context

ADR-014 and ADR-015 established the Gojek order-card takeover and automatic trip lifecycle. However, the driver has no visual guidance of the route to pickup and drop while the order is active. The Track screen embeds a Google Map (`TrackMap`) which already draws the recorded GPS trace and the live car position; it can also draw planned and runtime routes to help the driver navigate without launching an external app (or switching away from TrackLocation's integrated metrics: elapsed time, distance, fuel efficiency, OBD telemetry).

This ADR adds two car routes (driving mode) to `TrackMap` during an active Gojek order:
1. **Planned route:** current location → pickup → drop (pickup as a waypoint); fetched once when the order is taken, then frozen.
2. **Runtime route:** current location → next stop (pickup during PICKUP phase, drop during DROP phase); re-fetched on phase change or when the driver deviates >~40 m from the polyline, throttled to at most once per 30 s and only after the driver moved ~100 m.

Pickup and drop locations receive map markers (titled "Pickup" / "Drop"). All route state is cleared on order completion (FINISHED), dismissal, or no active order. Routes are display-only, not persisted, and do not affect the canonical location log or trip/session model (PRD §12).

---

## Decision

1. **Route provider:** Google Directions API + Google Geocoding API. These are chosen over OpenRouteService (ADR-011) because ORS has no Indonesian address-to-coordinates geocoding support and lacks detailed Indonesian road networks. For this Gojek-specific feature, Google's better coverage justifies the billed API use. ADR-011's ORS provider remains the choice for ADR-009 navigation (follow-a-route, planned future). The Maps key (existing `google_maps_key` resValue) must have both Directions and Geocoding APIs enabled; keys are restricted and not hardcoded (per AGENTS §13).

2. **Fetch strategy:**
   - **Planned route:** Fetched once when the order is taken (when `observer_trip` phase transitions from pre-takeover to PICKUP). Frozen for the life of the order (never re-fetched even if the driver teleports; simplifies UX).
   - **Runtime route:** Fetched on phase change (PICKUP → DROP) and when the driver is off-route. "Off-route" is defined as distance > ~40 m from the polyline (using `android-maps-utils` PolyUtil.isLocationOnPath). Throttled to at most once per 30 s and only after the driver moved ~100 m (prevents constant re-fetching due to GPS jitter).

3. **Geocoding:** Because `OrderCard` from ADR-013 contains only address text (no coordinates), addresses must be geocoded to LatLng. Geocoding is done once per order and cached in memory (in `OrderRouteController`). Pickup and drop use the first result returned by the Geocoding API (best-effort).

4. **Privacy:** Only pickup/drop address strings and the driver's current coordinates are sent to Google Services (Directions + Geocoding APIs). Customer name, phone number, payment details, and earnings are never sent (PRD §12 / ADR-013). Extraction and storage of pickup/drop remain device-only; this refinement clarifies that **routing** sends addresses to Google while **extraction/persistence** remain device-only.

5. **Display:**
   - **Planned route:** thin, muted blue-grey polyline (visually receded).
   - **Runtime route:** bold orange polyline drawn above it (active/current).
   - **Markers:** Pickup and Drop receive standard colored map markers (following Material Design color conventions). Titles "Pickup" / "Drop" shown on tap or hover.
   - **Existing visuals untouched:** the blue recorded-trace polyline and car marker remain unchanged.

6. **Architecture:**
   - **New file `feature/observer/trip/route/GoogleRouteClient.kt`:** Handles Directions and Geocoding API calls via `HttpURLConnection` (no external HTTP dependency; uses `org.json` for JSON parsing, already a Gradle dependency). Runs on the IO dispatcher. Returns `RouteResult(polyline: List<LatLng>, distanceMeters: Int, durationSeconds: Int)` and cached-geocode results.
   - **New class `OrderRouteController`:** Owned by `ShareViewModel`, collects `activeOrder` (from `latestOrderFlow` + `observer_trip` phase) and `locationUiState` (current position). Manages state transitions (fetch planned on takeover, fetch runtime on phase change / off-route). Exposes `OrderRouteState` (StateFlow) containing `plannedRoute`, `runtimeRoute`, `pickupLatLng`, `dropLatLng`, and error/retry state.
   - **`TrackMap` parameter update:** Gains optional `plannedRoute: List<LatLng>?`, `runtimeRoute: List<LatLng>?`, `pickupMarker: LatLng?`, `dropMarker: LatLng?` parameters. Draws polylines and markers conditionally.
   - **`TrackScreen` wiring:** Collects `orderRouteState` from `ShareViewModel.orderRouteState` and passes to `TrackMap`.
   - **No schema change:** route state is ephemeral; no Room entity or migration.

7. **Failure handling:**
   - **Offline / API error:** No route is drawn; trip continues normally. The driver can still see the map, recorded trace, and metrics.
   - **Geocoding failure (address not found):** Runtime route not drawn for that order. Planned route not drawn if either pickup or drop geocoding fails.
   - **Retry:** On-demand retry via a "Retry" button in the route error state (optional UI, design handoff pending). No automatic retry loop.

8. **Cost:** Billed per API request (Google Cloud billing). Estimated 3–5 API calls per order:
   - 2 Geocoding calls (pickup + drop addresses) — once per order.
   - 1 Directions call for planned route — once per order.
   - 0–2 Directions calls for runtime route — on phase change and off-route re-fetch.
   - Total: 3–5 calls per typical order (low volume; throttling prevents excessive re-fetch).

9. **Alternatives considered & comparison with ADR-009 navigation:**
   - **OpenRouteService (ADR-011 provider):** No reliable Indonesian address geocoding; weak coverage for Indonesian roads. Rejected for this use case.
   - **Launching external Google Maps app:** Would switch the user away from TrackLocation, losing integrated metrics and trip tracking. Rejected; this ADR keeps the feature embedded.
   - **ADR-009 follow-a-route navigation provider decision:** ADR-009 uses ORS (ADR-011) for flexibility and cost. This ADR overrides the provider choice for the Gojek order feature only, justified by Google's superior Indonesian support. ADR-011 stands for ADR-009 navigation; this is a Gojek-specific exception.

---

## Consequences

### Positive
- Driver has embedded visual guidance (routes + markers) without leaving the app, preserving access to live metrics (elapsed time, distance, fuel efficiency, OBD data).
- Planned and runtime routes provide context for both pre-arrival and in-progress phases.
- Throttling (1 per 30 s, after 100 m moved) prevents excessive API calls and GPS jitter noise.
- Provider choice (Google) aligns with Indonesian road quality and address geocoding capability.

### Negative / Accepted Risks
- **Billed API usage:** 3–5 calls per order incurs Google Cloud charges. Mitigation: throttling + low order volume per user. No per-user quota implemented (simple model).
- **Planned route may differ from Gojek's route:** Both apps use different routing algorithms (Google vs Gojek's internal). The driver may see two different route lines. **Accepted:** best-effort guidance; driver can follow their preferred route or use Gojek's in-app guidance if desired.
- **Geocoding ambiguity:** If an address matches multiple locations, the Geocoding API returns the first result. No disambiguation UI. **Accepted:** Indonesian addresses with street names are usually unambiguous; if incorrect, the driver can ignore the route.
- **Google API key scope:** The same Maps key is used for embedded maps, navigation (future ADR-009 + Google adapter), and route overlay. If the key is compromised, all three features are at risk. Mitigation: API key is restricted (Android app restriction + API restriction) in the Google Cloud Console.
- **Addresses sent to Google:** While pickup/drop extraction remains device-only (PRD §12), routing sends addresses to Google Services. This is a refinement of the "device-only" language to clarify: *extraction/persistence are device-only; routing sends addresses for real-time guidance*. User consent for this external call is implicit in accepting the order (Gojek itself sends addresses to remote servers).

---

## Alternatives Considered

1. **No route overlay; rely on Gojek's in-app navigation:** Driver uses Gojek's own route display. Simpler (no API, no cost), but user must switch apps or split attention. **Rejected:** ADR-014/015 intent is to embed order context in TrackLocation; visual guidance fits.

2. **Use OpenRouteService (ADR-011):** Avoid dual providers (ORS for navigation, Google for order routes). Simpler governance. **Rejected:** ORS lacks Indonesian coverage; using a weaker provider for this use case contradicts the goal (reliable guidance).

3. **Geocode addresses once at extraction time (ADR-013):** Store LatLng in the `observer_trip` entity instead of caching in-memory. **Rejected (deferred):** Requires schema change (MIGRATION_10_11 → MIGRATION_11_12) and ties routing to extraction. In-memory caching keeps them independent; deferred to a later enhancement if persistence is desired.

4. **Hardwire Google; no abstraction:** Simplify code by calling Google APIs directly without a port/adapter. **Rejected:** Contradicts ADR-011's portability principle. Keeps the door open for a future ORS-Indonesian-geocoding shim or OSM integration without code changes.

---

## Related ADRs

- **ADR-011** (Routing Engine via Connector-Adapter) — this ADR overrides the provider for the Gojek order feature only; ADR-011 still governs ADR-009 navigation (future).
- **ADR-013** (Observer Trip Extraction) — provides the `observer_trip` row with pickup/drop addresses; this ADR consumes that data.
- **ADR-014** (Gojek Order Card Takeover) — this ADR adds visual guidance to the order-card takeover flow.
- **ADR-015** (Gojek Order Lifecycle Drives the Trip) — routes are cleared on order completion (FINISHED) or dismissal.
- **ADR-009** (Dual-mode Track Navigation) — navigation uses ORS (ADR-011); routes on Gojek order use Google (this ADR).

---

## References

- PRD.md — Feature 3 (Accessibility Observer with Gojek order extraction), FR-17 (new: Gojek order route overlay)
- ARCHITECTURE.md — Gojek Order-Card Takeover Flow (extended with ROUTE step)
- UI-SPEC.md — §3e (order card + route overlay styling + handoff instructions)
- IMPLEMENTATION-PLAN.md — §4 (ADR-016 slice with implementation steps + How to Verify)
- Google Directions API — `https://developers.google.com/maps/documentation/directions`
- Google Geocoding API — `https://developers.google.com/maps/documentation/geocoding`
- android-maps-utils — `PolyUtil.isLocationOnPath(point, polyline, geodesic, toleranceMeters)`

---

## Implementation Notes (code pending approval)

**Phase 1 — GoogleRouteClient (HTTP + JSON)**
- Directions API: `POST https://maps.googleapis.com/maps/api/directions/json` (API key via query param or header).
- Geocoding API: `POST https://maps.googleapis.com/maps/api/geocode/json`.
- Use `HttpURLConnection` (no okhttp/Retrofit dependency), `org.json` for parsing.
- Handle 429 (rate limit), 403 (invalid key), and network errors gracefully.
- Decode polyline geometry using the existing `LocationUtils.decodePolyline()` (reuse).

**Phase 2 — OrderRouteController**
- Collect `activeOrder` from `ShareViewModel.latestOrderFlow` (phase + addresses).
- Collect `locationUiState.currentLocation` for off-route detection.
- State: `OrderRouteState(plannedRoute, runtimeRoute, pickupLatLng, dropLatLng, error, isLoading)`.
- On order takeover (phase → PICKUP): geocode pickup + drop; fetch planned route; set `plannedRoute`.
- On phase change (PICKUP → DROP) or off-route: fetch runtime route; set `runtimeRoute`.
- Throttle: `lastFetchTimeMs`, `lastMovementMs`, `lastMovementDistance`; check all three conditions before fetching.

**Phase 3 — TrackMap polyline + marker rendering**
- Planned polyline: `Polyline(points = plannedRoute, color = Color(0xFFB0BEC5), width = 4.dp)`.
- Runtime polyline: `Polyline(points = runtimeRoute, color = Color(0xFFFF9800), width = 6.dp, zIndex = 1)`.
- Markers: `Marker(position = pickupLatLng, title = "Pickup")`, `Marker(position = dropLatLng, title = "Drop")`.
- All conditional on non-null values.

**Phase 4 — TrackScreen wiring**
- `ShareViewModel.orderRouteState` exposed as a StateFlow.
- `TrackScreen` collects and passes to `TrackMap`.

**Phase 5 — Enable APIs on the Maps key**
- Google Cloud Console → APIs & Services → enable "Maps SDK for Android", "Directions API", "Geocoding API" for the project.
- Restrict key: Android app restrictions (app signature + package name), API restrictions (the three APIs above).
- Document in README.md: "Enable Directions API and Geocoding API for the Maps key used by this app."

**Verification (detailed in task log)**
- Deterministic (unit tests not required): Directions/Geocoding JSON parsing.
- On-device (explicit user permission §5a):
  1. Take a Gojek order → planned route line appears (pickup → drop with intermediate waypoint), Pickup and Drop markers show.
  2. Runtime line targets current → pickup (PICKUP phase).
  3. Drive on-route → runtime line follows, no re-fetch (throttled).
  4. Deviate >40 m from route → runtime re-fetches (≤1 per 30 s, ≤1 per 100 m moved).
  5. Order reaches DROP phase → runtime retargets to drop.
  6. Order ends (Cancel/Selesai/Dismiss) → both routes disappear, TripPanel returns.
  7. Offline or API error → no route drawn, trip continues normally.
  8. Airplane mode toggle → no crash, graceful error handling.
  9. Compare planned line with Gojek's in-app route overlay visually (subjective; expected divergence acceptable).
  10. Verify addresses sent to Google only (check network logs / traffic sniffing for sensitive data).

---

## Status Summary

- **ADR Created:** 2026-10-06 — Accepted
- **Code Implementation:** Pending user approval and build permission (AGENTS.md §5a)
- **Design Handoff:** Route overlay styling to be refined per UI-SPEC §3e handoff instructions
- **Related Commits:** None yet (ADR-016 docs-only; code pending)

---

**Amends:** ADR-011 (provider decision limited to ADR-009; ADR-016 overrides for Gojek order routes); PRD (new FR-17).
