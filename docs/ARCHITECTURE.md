---
name: ARCHITECTURE.md
path: docs/ARCHITECTURE.md
description: High-level system architecture, domain model, and design decisions — TrackLocation
---

# System Architecture
## TrackLocation

**Document Version:** 0.15
**Status:** Active (migrated from product-spec.md data/architecture rules)
**Last Updated:** 2026-10-06
**Owner:** Tech Lead
**Controlled By:** `docs/DOCUMENT-CONTROL.md`

> Migrated 2026-06-15 from data-model/behavior rules in `docs/product-spec.md` and CR data-model impacts during create-project schema adoption.

---

## 1. System Overview

TrackLocation is a single-module Android app (Jetpack Compose + Material 3, package `com.kolee.tracklocation`) targeting phones. It records GPS continuously via a foreground `TrackingService`, persists everything locally in Room, and exposes four bottom-nav destinations (`Session / List / Track / Settings`). Two tools live under Settings: an accessibility-event Observer and an ELM327 OBD-II telemetry feature, each backed by its own service. The architecture is local-first and sync-ready (remote access is abstracted behind a `RemoteDataSource` for future phases).

---

## 2. Domain Model

Core entities and their relationships (Room):

### Entity Relationships

```
LocationEntity (location_log)   ← canonical GPS points (source of truth); stationary fixes collapse to one dwell anchor (ADR-006)
  └── referenced by id ranges

SessionEntity (recording_session)
  ├── startedAt / endedAt
  └── resolves path from a location_log range (ON→OFF period)

TrackEntity (trip)
  ├── startLocationId → LocationEntity
  ├── endLocationId   → LocationEntity
  └── distance, duration, timestamp

ObservedEventEntity (observer_event)
  ├── packageName, eventType, activityName
  ├── firstSeenAt, lastSeenAt, repeatCount
  └── textSummary, treeSnapshot, truncationMetadata

AllowlistRuleEntity (allowlist_rule)   ← EXACT / REGEX, packageName-only, case-sensitive

ObserverTripEntity (observer_trip)   ← DERIVED from captured tree snapshots; Gojek-only extraction (ADR-013)
  ├── pickupName, pickupAddress, dropName, dropAddress
  ├── payment (e.g., GoPay/Kartu), earningsRp (integer)
  ├── phase (pickup/drop-only/finished), firstSeenAt, lastSeenAt, handled (marked once the takeover for that order has been processed)
  └── no FKs to trips/sessions; deduplication key = (pickupAddress, dropAddress)

ObdSampleEntity (obd_sample)
  ├── timestampMs, rpm, obdSpeedKmh
  ├── fuelRateLph, mafGramsPerSecond, fuelRateSource
  └── adapterElapsedMs   (no FK to trip/session — linked by time-window queries)

SessionEntity (recording_session) — OBD accumulator columns (added DB v5)
  ├── obdFuelConsumedL  (REAL, default 0.0) — cumulative L consumed this session
  └── obdGpsDistanceKm  (REAL, default 0.0) — cumulative GPS km this session

TrackEntity (trip) — OBD accumulator column (added DB v5)
  └── obdFuelConsumedL  (REAL, default 0.0) — cumulative L consumed this trip

FuelPriceEntity (fuel_price)   ← effective-dated price log (added DB v9, ADR-008)
  ├── pricePerLiter (REAL)
  └── effectiveFromMs (Long, @Index) — current price = latest row; completed-trip cost uses the row effective at trip start

KnownSegmentEntity (known_segment)   ← DERIVED from location_log (added DB v10, ADR-010); rebuildable, never source-of-truth
  ├── simplified directed polyline (Douglas–Peucker) + bearingDegrees
  └── indexed by grid cell for road-ahead lookup (CellIndex)
```

### Key Invariants

- The canonical `location_log` is the single source of truth for GPS points (see PRD §12 BR-01/BR-02).
- Sessions and trips are ranges/views over the canonical log; deleting a trip never deletes location rows.
- **Session invariant:** at most one `recording_session` row has `isActive = 1` at any time. Starting always-recording adopts the existing open session rather than creating a duplicate; the launch-time reaper and stop path close open sessions.
- **Recording resumes on restart:** on launch, `MainActivity` reaps only *stale* open sessions (no location point within the ~2-min grace window); a non-stale open session that outlived a force-stop (which `START_STICKY` does not redeliver) is resumed by starting `TrackingService` with `START_RECORDING`, which adopts it — keeping the live service state and the persisted session consistent (no Inactive-card-beside-Active-session mismatch).
- OBD samples and Observer events have **no foreign keys** to trips/sessions; they associate via `samplesBetween(startMs, endMs)` / time windows.
- Observer history is never user-deletable; retention is automatic (7 days / 50,000 rows, whichever is smaller).
- `observer_trip` is retained independently: 90 days / 5,000 rows, whichever is smaller; automatic pruning; no user-facing delete.

---

## 3. Architectural Principles

- **Single source of truth:** canonical location log; sessions/trips derive from it.
- **Local-first, sync-ready:** all data in Room; remote behind `RemoteDataSource` (Neon V1 is a direct datasource; an API backend is the V2 replacement path).
- **Service isolation:** `TrackingService` (GPS), `ObserverAccessibilityService` (events), and `ObdPollingService` (telemetry) are independent; Observer/OBD must not degrade GPS reliability. `TrackingService` must not start/stop `ObdPollingService` (they communicate via intent actions only).
- **Business logic out of Compose:** data access behind DAOs/repositories where practical.

---

## 4. Technology Stack

| Layer | Technology | Rationale |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Accepted product UI direction; compose-pinned at Compose UI 1.2.x (compiler extension 1.2.0) |
| Language | Kotlin 1.7.0 | Project baseline (note: incompatible with `kotlin-obd-api` — see ADR/OBD approach) |
| Persistence | Room (current DB version 10) | Local-first storage with migrations |
| Preferences | DataStore | Observer + OBD settings/state |
| GPS | Foreground `TrackingService` (`foregroundServiceType="location"`) | Continuous always-recording |
| Telemetry | `ObdPollingService` (`foregroundServiceType="connectedDevice"`) + raw AT I/O over Bluetooth RFCOMM/SPP | ELM327 Bluetooth Classic |
| Accessibility | `ObserverAccessibilityService` (`BIND_ACCESSIBILITY_SERVICE`) | Event capture |
| Build | Gradle + JDK `C:\Users\rinal\.jdks\jbr-17.0.14` | Local toolchain |

---

## 5. Key Integration Points

- **Bluetooth Classic (ELM327):** RFCOMM/SPP socket; pairing via Android system settings (`Settings.ACTION_BLUETOOTH_SETTINGS`, Option A). `BLUETOOTH_CONNECT` requested on first Enable toggle (API 31+). Raw AT command I/O (`ATZ/ATE0/ATL0/ATSP0` init, Mode-01 PIDs) — no third-party OBD library.
- **Android AccessibilityService:** user enables in system Accessibility settings; app deep-links and reflects status only.
- **Remote (future):** `RemoteDataSource` abstraction; Neon V1 direct, API backend V2.

### Data flow (OBD telemetry)

```
ELM327 adapter → RFCOMM/SPP socket → ObdPollingService (poll @1–5 Hz, raw AT)
   → ObdUiState (StateFlow)            → Session screen / Trip panel (km/L, RPM, speed)
   → ObdSampleEntity (Room)            ← written only while recording (TrackingService.isAlwaysRecording; ADR-012 — SESSION_ON/OFF now advisory)
```

   `ObdUiState.Connected` additionally carries `sessionFuelConsumedL` and `tripFuelConsumedL` (litres) so the Session card and the Trips active-trip row can compute fuel cost = litres × `obd_fuel_price_per_liter`.

### Gojek Order-Card Takeover Flow (ADR-014, amended by ADR-015, extended by ADR-016)

```
ObserverAccessibilityService (write path)
   → after observer_event insert & observer_trip upsert

TRIP START (auto-start at Taken):
   → Gojek parser yields complete OrderCard (pickup+drop+payment+earnings)
   → emit "order ready" signal (one-shot per (pickupAddress, dropAddress))
     → the Observer service starts MainActivity (FLAG_ACTIVITY_NEW_TASK + an extra selecting the Track screen)
        → foreground launch from the background: to be verified on Android 10
   → order-ready state is derived from the persisted observer_trip row (survives the activity being created after the signal)
     → ShareViewModel observes it
     → if no trip is live, send START_TRIP
     → if a trip is live, keep it and set in-memory flag orderOwnsTrip=true
     → mark row handled

TRIP END (auto-end at Cleared/Cancelled/Dismissed):
   → Gojek parser recognizes terminal states:
     (a) Gojek home screen = all 4 nav texts present (Beranda, Pendapatan, Swadaya, Pesan)
     (b) cancel message = "Oke, sip" + text containing "nge-cancel"
   → both yield OrderCard with phase=FINISHED
     → OrderTripRecorder marks the latest open row FINISHED
     → ShareViewModel observes latestOrderFlow
     → when FINISHED and orderOwnsTrip=true and a trip is live, run stopActiveTrip():
        persist the trip (TrackEntity via insertTrack), then send STOP_TRIP
        (TrackingService.stopTrip() only stops trip recording and the timer; always-recording unaffected)
     → clear orderOwnsTrip=false
   → user-initiated Dismiss on the order card also calls dismiss(id), triggering the same stop logic

   → Track screen displays the order card (composable name chosen at implementation) instead of TripPanel (from pickup phase through terminal state)
   → while the trip is live, a compact trip strip sits below the card (same glass panel): TIME (elapsed, compact format), DIST (km), AVG/INST km/L, fed from TrackPanelState
   → after terminal state, trip strip and card disappear; TripPanel returns

ROUTE (Planned + Runtime, ADR-016):
   → OrderRouteController collects activeOrder (from latestOrderFlow) and locationUiState
   → On order takeover (phase → PICKUP):
      Geocode pickup and drop addresses via Google Geocoding API (once per order, cached in-memory)
      Fetch planned route (current → pickup → drop) via Google Directions API
      Display as thin muted blue-grey polyline on TrackMap
   → On phase change (PICKUP → DROP) or off-route (>~40 m from polyline):
      Fetch runtime route (current → next stop: pickup or drop) via Google Directions API
      Display as bold orange polyline above the planned route
      Throttled: at most once per 30 s and only after driver moved ~100 m
   → Pickup and Drop map markers displayed at their geocoded LatLng coordinates
   → On terminal state (FINISHED) or dismiss:
      Clear both planned and runtime routes, remove markers, hide route state
      TrackMap reverts to recorded trace + live car position only
   → Failure handling: offline or API error → no route drawn, trip continues normally (fail-soft)
   → External data flow: pickup/drop addresses and current location sent to Google (Directions + Geocoding APIs); customer name/phone never sent (ADR-013)
```

**Coupling:** Observer-to-tracking link is by signal/intent only (service isolation rule); the Observer service never calls `TrackingService` methods directly. `TrackingService` must never start/stop `ObserverAccessibilityService`. Foreground launch from a background accessibility service is gated by Android 10+ background-activity-start restrictions; a fallback mechanism (full-screen-intent notification or system alert window) may be required and must be verified on-device (test device: Samsung SM-G965F, Android 10). If the launch is blocked and `ShareViewModel` does not exist, the trip is started when the activity is next created. `orderOwnsTrip` is in-memory; after process death the trip is not auto-ended. Any new permission required will be documented separately.

---

## 6. Security & Authorization Model

- **Current:** no auth; all data local. Permissions: `FOREGROUND_SERVICE_LOCATION`, location, `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` (API 31+) / `BLUETOOTH`+`BLUETOOTH_ADMIN` (≤30), `BIND_ACCESSIBILITY_SERVICE`.
- **Planned (Observer Phases 6–7):** registration gate + Google Sign-In + CameraX/ML Kit face enrollment; app-wide auth overlay; 24-hour re-auth; face attempt budget with Google fallback. Auth gates UI only — background capture, sync, and GPS continue. **Raw face images are never transmitted.**

---

## 7. Data Flow Diagram

```
GPS fix → TrackingService → location_log (canonical)
   ├── session ON/OFF → recording_session rows
   └── trip start/stop → TrackEntity (startLocationId..endLocationId)
Trip/Session detail screens resolve path from location_log ranges.
```

---

## 8. Database Schema

Room database (`TrackDatabase`), at version **v7** (ADR-006 dwell-collapse shipped + built 2026-07-07); **ADR-007 moves it to v8** via a *destructive* migration (`fallbackToDestructiveMigrationFrom(7)`) that drops the now-unused `obdGpsDistanceKm` column (local test data discarded; no production data yet). Migrations (all inline in `TrackDatabase.kt`): `MIGRATION_1_2` (legacy serialized trip paths → canonical location rows + trip boundaries), `MIGRATION_2_3` (observer_event + allowlist_rule), `MIGRATION_3_4` (obd_sample), `MIGRATION_4_5` (OBD accumulator columns — `ALTER TABLE recording_session` + `ALTER TABLE track`; the domain "trip" is the physical `track` table). ADR-008 adds **MIGRATION_8_9** (DB→**v9**): `CREATE TABLE fuel_price (id INTEGER PK AUTOINCREMENT, pricePerLiter REAL NOT NULL, effectiveFromMs INTEGER NOT NULL)`, plus an `@Index` on `effectiveFromMs`. **ADR-010 adds a migration for `known_segment` (originally planned as MIGRATION_9_10, DB→v10; renumbered 10→11 if ADR-013 ships first, see below):** `CREATE TABLE known_segment` (a derived, simplified directed-polyline store) plus a grid-cell index for road-ahead lookup — derived from and fully rebuildable from `location_log`, never source-of-truth. **ADR-013 adds MIGRATION_9_10 (DB→v10):** `CREATE TABLE observer_trip (id INTEGER PK AUTOINCREMENT, pickupName TEXT, pickupAddress TEXT, dropName TEXT, dropAddress TEXT, payment TEXT, earningsRp INTEGER, phase TEXT NOT NULL, firstSeenAt INTEGER NOT NULL, lastSeenAt INTEGER NOT NULL, handled INTEGER NOT NULL DEFAULT 0)` with an `@Index` on `lastSeenAt` — `TrackDatabase.kt` is currently v9 and ADR-010 has not shipped, so ADR-013 work takes the next free version (MIGRATION_9_10, DB→v10); ADR-010's `known_segment` migration will then be numbered 10→11 when it ships. Numbering follows ship order; the two swap if ADR-010 ships first.

`location_log` columns (per ADR-006 dwell collapse): `id` (PK), `timestamp` (last confirmed-still fix / departure), `dwellStartTimestamp` (arrival; set once at insert, never bumped), `collapsedCount` (fixes folded into the anchor, default 1), `latitude`, `longitude`, `accuracyMeters?`, `speedMetersPerSecond?`, `bearingDegrees?`, `altitudeMeters?`. `MIGRATION_5_6` (ADR-005) added the `observer_event_fts` FTS4 index; `MIGRATION_6_7` (ADR-006) adds `dwellStartTimestamp`/`collapsedCount` and backfills `dwellStartTimestamp = timestamp`. DB version → 7.

`obd_sample` table:

| Column | Type | Notes |
|---|---|---|
| id | Long PK auto | |
| timestampMs | Long | `System.currentTimeMillis()` at insert; `@Index` |
| rpm | Int? | |
| obdSpeedKmh | Int? | stored for comparison; GPS speed canonical for km/L |
| fuelRateLph | Double? | direct, MAF-derived, or speed-density estimate |
| mafGramsPerSecond | Double? | raw or estimated MAF |
| fuelRateSource | String | `DIRECT_FUEL_RATE` / `MAF_DERIVED` / `SPEED_DENSITY` / `UNAVAILABLE` |
| adapterElapsedMs | Long? | round-trip time to adapter |

Fuel-rate fallback chain: `DIRECT(015E) → MAF(0110) → SPEED_DENSITY → UNAVAILABLE`. Speed-density estimate: `MAF(g/s) = (RPM × MAP_kPa × VE × Displacement_L × 28.97)/(120 × 8.314 × IAT_K)`, `fuel(L/h) = MAF/(14.7×λ) × 3600/745` (VE=0.85, gasoline; engine displacement is a user pref, default 1193 cc).

**OBD Phase 2 accumulation — unified per ADR-007:**
- **One averaging definition (session and trip):** `avg km/L = displacement distance ÷ fuel`. Distance = Σ `distanceBetween` over adjacent `location_log` samples = the **displayed** distance (`session.distanceMeters` for the session, trip `distanceInMeters`/`track.distance` for the trip). Fuel = Σ `fuelRate × dt` over `obd_sample` in `[startedAt, now]`.
- **Live value = O(1) incremental cache** (session: `obdFuelConsumedL`; trip: in-memory total since `tripStartedAt`). **Authoritative value at close = re-integrate `obd_sample`** over the final window and persist it; `obd_sample` is the single source of truth for fuel, the cache is a disposable live proxy (ADR-007 "Option B" — cache = memoized integral of the canonical rows). Mid-drive restart reseeds the in-memory trip total by one `obd_sample` integration.
- **Session average** = `session.distanceMeters / obdFuelConsumedL` (was `obdGpsDistanceKm / obdFuelConsumedL`). The `obdGpsDistanceKm` column is **dropped** (destructive v7→v8) and no longer read.
- **Trip average** = `distanceInMeters / (obd_sample fuel over [tripStartedAt, now])`, computed live; at Stop `ShareViewModel.onTripCtaTap` writes the re-integrated total into `track.obdFuelConsumedL`.
- **Display:** the average shows once distance > 0.01 km and fuel > 0, else `—`. Idle (fuel accrues, distance flat) correctly degrades the average — relies on the ADR-006 stale-speed fix.
- **Fuel cost (ADR-008):** `cost = litres × price`. Litres = session live `obdFuelConsumedL` (Session card) / in-memory trip total (active-trip row) / stored `track.obdFuelConsumedL` (completed row). Price is a first-class effective-dated entity `FuelPriceEntity(id, pricePerLiter, effectiveFromMs)` in the new `fuel_price` table (**MIGRATION_8_9**, DB→v9): current price = the row with max `effectiveFromMs`; Save/Undo/Redo append effective-now rows (non-destructive). Active session/trip use the current price; a **completed trip** uses the price effective at its **start** (`fuel_price` row with max `effectiveFromMs ≤ track.timestamp`, else `—`). Seed: a one-time code migration inserts the retired `obd_fuel_price_per_liter` scalar at `effectiveFromMs=0`. The `FuelPriceController` in-memory stack drives undo/redo; the DataStore scalar is deprecated as source of truth.

---

## 9. Deployment Architecture

Single Android APK; no backend in current phases. Future: Neon Postgres (V1 direct via low-privilege role), then an API backend (V2).

---

## 10. Error Handling Strategy

- Services log to Logcat (`ObdPollingService`, etc.); OBD surfaces IDLE/CONNECTING/CONNECTED/RETRYING/WAITING + last error in the UI state.
- OBD reconnect: exponential backoff (1s, 2s, 4s…) capped at `obdRetryMaxSeconds`, then WAITING until manual reconnect.
- Build/runtime errors and resolutions are recorded in `docs/ERRORS-LOG.md`.

---

## 11. Future Improvements

- Observer Phases 3–5: FTS5 filtering, hybrid sync engine (batch + time), Neon V1 remote storage.
- Observer Phases 6–7: registration, face enrollment, auth overlay + hardening.
- Surface "fuel data unavailable on this vehicle" in OBD UI for no-fuel-PID vehicles.

---

## 12. Track Navigation & Local Route Store (ADR-009 / ADR-010)

**Dual-mode Track screen (ADR-009).** The Track screen keeps live tracking and adds follow-a-route navigation as a **sub-mode of a trip** (never independent). "Start trip + navigate" reuses the existing `START_TRIP` path (which already starts always-recording); ending navigation ends the trip via `STOP_TRIP` but never stops always-recording. Off-route recalculation fires at ~50 m, gated by 2–3 consecutive off-route fixes and ≥15 s between calls. Routing failures fail soft (trip always records track-only). `LocationUiState` gains a `bearingDegrees` (heading) field, surfaced from the `Location.bearing` already captured in `TrackingService`/`LocationEntity`.

**"Navigation perspective" camera.** A decoupled, user-toggled map view that sets `CameraPosition.bearing = heading` and follows the vehicle (heading-up), with **no tilt** (Compose-Maps 1.2-safe). A directional car marker uses `Marker(rotation = heading)`.

**Local route store (ADR-010).** New derived components (business logic out of Compose, data behind repository per §3): `TraceIngester` (on session/trip end, background — simplify + segment the new `location_log` slice into `known_segment` rows), `RoutePredictor` (current LatLng + bearing → 0..N candidate ahead-polylines from the grid-cell index), and `LocalRouteRepository` (DAO wrapper). The store is derived and rebuildable; the canonical `location_log` is never modified. `TrackDatabase` moves to **v10** via `MIGRATION_9_10`.

**Routing engine (ADR-011).** For roads not covered by the local store, routing goes through a `RoutingEngine` **port** (`route(origin, dest, opts) → RouteResult(polyline, distanceMeters, durationSeconds)`) with a first `OpenRouteServiceAdapter` connector (`POST /v2/directions/driving-car`, API-key auth, encoded-polyline geometry reused by the existing decode). Ports-and-adapters keeps the provider swappable: self-hosted ORS is a base-URL swap in the same adapter; OSRM/Valhalla/Google are new adapters. ETA is **static** (ORS has no live traffic). The ORS key is not hardcoded (§13); ORS/OSM attribution is displayed wherever routes appear.

**Still open (parked):** *when* to migrate to self-hosted OSM (deliberately deferred). Stage B (a full routable graph over the user's own network) is deferred to its own future ADR.

## Reference Documents

- `docs/PRD.md` — Product requirements and features
- `docs/IMPLEMENTATION-PLAN.md` — Sprint/slice breakdown and tech stack summary
- `docs/adr/` — Architecture Decision Records (detailed decisions)
- `AGENTS.md` §4 — Technical responsibilities
- `docs/WORKFLOW.md` — Development workflow

---

**Last Reviewed:** 2026-06-15
**Next Review:** After Observer Phase 2 completion
