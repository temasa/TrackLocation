---
name: ARCHITECTURE.md
path: docs/ARCHITECTURE.md
description: High-level system architecture, domain model, and design decisions — TrackLocation
---

# System Architecture
## TrackLocation

**Document Version:** 0.31
**Status:** Active (migrated from product-spec.md data/architecture rules)
**Last Updated:** 2026-10-11
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
  ├── phase (pickup/drop-only/finished/cancelled; cancelled added ADR-025, string column, no schema change), firstSeenAt, lastSeenAt, handled (marked once the takeover for that order has been processed)
  └── no FKs to trips/sessions; deduplication key = (pickupAddress, dropAddress)

TripPlannedRouteEntity (trip_planned_route)   ← saved planned route per trip, device-only (ADR-026, DB v15; code in f26bb03)
  ├── tripStartedAt (INTEGER PK) = track.timestamp of the owning trip; NO FK (same convention as observer_trip)
  ├── kind (TEXT NOT NULL) — 'ORDER' | 'NAV' (NAV reserved for ADR-009, not implemented)
  ├── geometry (TEXT NOT NULL) — ORS encoded polyline, precision 5, as returned
  ├── distanceM, durationS (INTEGER) — ORS routes[0].summary
  └── originSource (TEXT 'ACCEPTED' | 'FALLBACK_LIVE'), fetchedAt (INTEGER)

ObdSampleEntity (obd_sample)
  ├── timestampMs, rpm, obdSpeedKmh
  ├── fuelRateLph, mafGramsPerSecond, fuelRateSource
  ├── adapterElapsedMs
  └── gpsSpeedKmh   (REAL?, added DB v13, ADR-024 — GPS speed at poll time; covered distance is re-integrated from it)
  (no FK to trip/session — linked by time-window queries)

SessionEntity (recording_session) — OBD accumulator columns
  ├── obdFuelConsumedL       (REAL, default 0.0) — cumulative L consumed this session (added DB v5)
  └── obdCoveredDistanceKm   (REAL, default 0.0) — cumulative km over the same fuel-covered intervals (added DB v13, ADR-024)
  (the former obdGpsDistanceKm column was dropped in v8, ADR-007)

TrackEntity (trip) — OBD accumulator columns
  ├── obdFuelConsumedL       (REAL, default 0.0) — cumulative L consumed this trip (added DB v5)
  └── obdCoveredDistanceKm   (REAL, default 0.0) — km over the same fuel-covered intervals (added DB v13, ADR-024)

TrackEntity (trip) — Gojek order outcome columns (ADR-025)
  ├── orderStatus            (TEXT, nullable) — 'COMPLETED' | 'CANCELLED'; NULL for manual trips (added DB v14; backfilled 'COMPLETED' where orderLabel IS NOT NULL)
  └── orderOffline           (INTEGER, NOT NULL, default 0) — 1 = trip closed offline (cash order, stopped within 150 m of drop-off); meaningful only with orderStatus = 'COMPLETED' (added DB v14)

FuelPriceEntity (fuel_price)   ← effective-dated price log (added DB v9, ADR-008)
  ├── pricePerLiter (REAL)
  └── effectiveFromMs (Long, @Index) — current price = latest row; completed-trip cost uses the row effective at trip start; a Fill up with a changed price appends a row here (ADR-027)

FuelEventEntity (fuel_event)   ← append-only fuel log for the fuel dock (added DB v16, ADR-027; no FK)
  ├── id (INTEGER PK AUTOINCREMENT)
  ├── kind (TEXT NOT NULL) — 'RESET' | 'FILL_UP'
  ├── litres (REAL NOT NULL) — starting level (RESET) or litres added (FILL_UP)
  ├── pricePerLiter (REAL, nullable) — price entered in Fill up when changed; NULL when the default price was kept
  ├── resetBaselineL (REAL, nullable) — RESET rows only: cumulative OBD fuel total at the moment of the Reset (rev. 1.2); NULL for FILL_UP
  └── createdAtMs (INTEGER NOT NULL)
  Fuel balance (derived, never stored; ADR-027 rev. 1.2): anchor = latest RESET (resetAt = its createdAtMs; resetBaselineL = its baseline); cumulativeNow = Σ recording_session.obdFuelConsumedL over all session rows, active included (one query/Flow; OBD litres accrue into the active row on every poll via addObdAccumulator); fuelLeft = max(0, anchor.litres + Σ FILL_UP.litres (createdAtMs > resetAt) − max(0, cumulativeNow − anchor.resetBaselineL)); no RESET → `—`. Reads no obd_sample rows, so it survives obd_sample retention pruning. Only OBD-measured litres reduce it; OBD disconnected → frozen.
  RANGE (display only) = fuelLeft × session avg km/L (obdCoveredDistanceKm ÷ obdFuelConsumedL, ADR-024).

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
- `fuel_event` (ADR-027) is append-only and has no FK. The fuel balance is derived from it and the OBD cumulative total (Σ `recording_session.obdFuelConsumedL` over all session rows, active included, compared with the RESET's `resetBaselineL`), never stored as a column.
- `trip_planned_route` (ADR-026) is a separate table (not a `track` column: `track` list queries load whole rows and already carry a large `pathPoints` string). It has no FK; it is deleted with its trip (same DAO transaction path as the `track` delete) plus an app-start orphan sweep (no matching `track.timestamp` AND `fetchedAt` older than 24 h). It is never ingested into the local route store (ADR-010, PRD BR-15/BR-16); the optimized route is never persisted.

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
| Persistence | Room (current DB version 16; `fuel_event` v16 implemented by ADR-027, commit `a197453`) | Local-first storage with migrations |
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

   `ObdUiState.Connected` additionally carries `sessionFuelConsumedL` and `tripFuelConsumedL` (litres) so the Session card, the Trips active-trip row, and the Gojek order-card trip strip can compute fuel cost = litres × effective fuel price (from `fuel_price` table, FR-12/ADR-008).

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

TRIP END (auto-end at Selesai/Cleared/Dismissed; cancel rule per ADR-025):
   → Gojek parser recognizes terminal states:
     (a) Selesai screen = "Selesai" with earnings/summary (normal completion)
     (b) Gojek home screen = all 4 nav texts present (Beranda, Pendapatan, Swadaya, Pesan)
     (c) cancel message = "Oke, sip" + text containing "nge-cancel" → phase=CANCELLED (distinct from FINISHED)
   → (a) and (b) yield OrderCard with phase=FINISHED; (c) yields phase=CANCELLED
     → OrderTripRecorder marks the latest open row (FINISHED or CANCELLED)
     → ShareViewModel observes latestOrderFlow
     → when a terminal phase (FINISHED or CANCELLED) and orderOwnsTrip=true and a trip is live, decide by status:
        - FINISHED → stopActiveTrip(status=COMPLETED, offline=false)
        - CANCELLED, payment not cash ('Tunai' is the cash match, case-insensitive) or missing → stopActiveTrip(status=CANCELLED, offline=false)
        - CANCELLED, cash, drop coordinates resolved → do NOT stop; keep recording; show one drop-off snackbar when within 150 m of drop coordinates (Track screen only; action "Stop")
        - CANCELLED, cash, drop coordinates unresolved → do NOT stop; no snackbar; stop later saves CANCELLED
     → stop of a cash-cancelled trip (snackbar Stop or Stop CTA): within 150 m of drop → COMPLETED + orderOffline=true; otherwise CANCELLED + orderOffline=false
     → stopActiveTrip(): persist the trip (TrackEntity via insertTrack, with orderStatus/orderOffline), then send STOP_TRIP
        (TrackingService.stopTrip() only stops trip recording and the timer; always-recording unaffected)
     → clear orderOwnsTrip=false
   → user-initiated Dismiss on the order card also calls dismiss(id), triggering the same stop logic
   → open risk (ADR-025): a home-screen "Cleared" (FINISHED) written after CANCELLED may end a cash trip early as COMPLETED; verify on device

   → Track screen displays the order card (composable name chosen at implementation) instead of TripPanel (from pickup phase through terminal state)
   → while the trip is live, a compact trip strip sits below the card (same glass panel): TIME (elapsed, compact format), DIST (km), COST / NET (estimated fuel cost = trip fuel from OBD accumulator × effective price; NET = OrderCard.earningsRp − COST), AVG/INST km/L, fed from TrackPanelState + OBD state + FuelPriceController + OrderCard earnings
   → after terminal state, trip strip and card disappear; TripPanel returns

ROUTE (Planned + Optimized/Runtime + Actual, ADR-016, provider ADR-017, persistence ADR-026):
   → OrderRouteController collects activeOrder (from latestOrderFlow) and locationUiState
   → On order takeover (phase → PICKUP):
      Geocode pickup and drop addresses via OpenRouteService Geocoding (once per order, cached in-memory)
      Geocoding fallback ladder (ADR-017 Amendment 2026-10-11): (1) full address, normalized ('Jl.'→'Jalan'), including city, (2) place name + last 1–2 address segments as area hint, (3) house-number-stripped address + progressive trims, (4) street-only last; bare place name alone is not a candidate
      Boundary guard: each request sends boundary.circle (driver's current location, radius 50 km) plus focus.point.*; client-side, any accepted result more than 50 km (MAX_GEOCODE_DISTANCE_KM) from the driver's current location is rejected and falls to the next step; all steps failing draws nothing (fail-soft)
      Reject results: confidence <0.8 unless venue/street layer; locality/localadmin/county/region/macroregion/country layers
      Planned-route origin = accepted point (trip-start fix: activeTripStartLocationId row, else first recorded path point, else current live fix as last resort)
      Fetch planned route (accepted point → pickup → drop via waypoint; during DROP at takeover: accepted point → drop) via OpenRouteService Directions API; fetched once, frozen
      Diagnostics (tags unchanged; no address text, coordinates or key logged): per ladder step order key seen, index accepted, layer, confidence, distance to driver (km) or reason rejected (far / coarse / low-confidence / none); planned/runtime fetch success or failure and point counts; warning if pickup and drop resolve <30 m apart
      Extract routes[0].summary distance/duration along with the geometry (RouteResult)
      Persist at fetch (ADR-026, planned code): write trip_planned_route row (kind 'ORDER', keyed by LocationUiState.tripStartedAt, originSource ACCEPTED or FALLBACK_LIVE) immediately after the fetch succeeds; if tripStartedAt == 0 skip persisting and log
      Hydrate (ADR-026): on process restart, if a row exists for the current trip, restore the frozen plan from it instead of re-planning from a new origin
      Display as dashed thin blue-grey polyline on TrackMap (UI-SPEC §3j)
   → On phase change (PICKUP → DROP) or off-route (>~40 m from polyline):
      Fetch runtime route (current → next stop: pickup or drop) via OpenRouteService Directions API
      Display as solid bold orange polyline above the planned route; in-memory only, never persisted, never fed to the route store (BR-16)
      Throttled: at most once per 30 s and only after driver moved ~100 m
   → Pickup and Drop map markers displayed at their geocoded LatLng coordinates
   → Attribution: '© openrouteservice.org | © OpenStreetMap contributors' visible while routes shown, hidden when none
   → On terminal state (FINISHED or CANCELLED) or dismiss:
      Clear both planned and runtime routes from memory (the persisted planned row stays with its trip), remove markers, hide route state
      TrackMap reverts to recorded trace + live car position only
   → Failure handling: offline or API error → no route drawn, trip continues normally (fail-soft)
   → Trip details (ADR-026): load trip_planned_route by track.timestamp; decode the polyline; draw dashed under the actual line; compute comparison at display time with a pure RouteComparison util, never persisted:
        Planned km = distanceM / 1000; Actual km = track.distance; Extra km = actual − planned (signed);
        Max deviation m = max over actual path points of the distance to the planned polyline
      No row (pre-v15 trip, failed plan, manual trip) → empty state "No planned route saved"; attribution shown whenever the saved plan is drawn
   → Data lifetime (ADR-026): planned = persisted device-only with its trip; optimized = memory only; actual = canonical location log + track.pathPoints
   → External data flow: pickup/drop addresses and current location sent to OpenRouteService (Directions + Geocoding); the boundary circle reuses the driver's current coordinates (no new data class); customer name/phone never sent (ADR-013)
   → Accuracy notes (ADR-017): street-level pins can be metres off on long roads; geocoding may fail for long comma-separated Gojek addresses; an address with no city may geocode wrongly inside the 50 km circle or not at all; fail-soft (no route better than wrong route)
```

**Coupling:** Observer-to-tracking link is by signal/intent only (service isolation rule); the Observer service never calls `TrackingService` methods directly. `TrackingService` must never start/stop `ObserverAccessibilityService`. Foreground launch from a background accessibility service is gated by Android 10+ background-activity-start restrictions; a fallback mechanism (full-screen-intent notification or system alert window) may be required and must be verified on-device (test device: Samsung SM-G965F, Android 10). If the launch is blocked and `ShareViewModel` does not exist, the trip is started when the activity is next created. `orderOwnsTrip` is in-memory; after process death the trip is not auto-ended. Any new permission required will be documented separately.

### Track Screen — Live Location Display (ADR-018, display-only data path)

```
FusedLocationProviderClient → LiveLocationSource (screen-scoped, lifecycle-aware)
   ├── Active only while TrackScreen visible AND app in foreground (ON_START/ON_STOP/RESUMED)
   └── Exposes StateFlow<LiveFix?> (lat/lng, speed, bearing, accuracy, time)

TrackScreen display logic:
   displayLocation = liveLocationSource.collect() ?: trackingService.lastKnownSeed

DISPLAY ONLY (never written to DB):
   ├── Map dot/marker position
   ├── Follow mode camera target
   └── Recenter FAB target

SEPARATE from TrackingService recording path:
   TrackingService.locationUiState → canonical location_log (write path, persisted)
   LiveLocationSource → TrackScreen display (read path, ephemeral)
   
   Result: live dot visible whenever screen open, regardless of recording state.
   Canonical log unchanged (source of truth for sessions/trips/OBD).
```

**Permissions:** Uses existing `ACCESS_FINE_LOCATION` foreground permission; NO background location. If permission not granted, source stays idle and screen uses fallback seed (same as today).

**Battery:** GPS at ~1 Hz only while TrackScreen visible (same cost model as Google Maps/Waze). FusedLocationProviderClient merges requests when both screen and service are active (service already running, so incremental cost small).

**Privacy:** Live fixes never stored or transmitted. Only external call remains OrderRouteController → OpenRouteService (for order routes, ADR-017).

### Debug-only Diagnostic Screen Recorder (ADR-021)

**Purpose:** Automatic on-device video capture for debugging UI anomalies without manual intervention.

**Architecture:**
- **Debug-only source set:** All code in `app/src/debug/` (never compiled into release builds). Debug manifest overlay declares `mediaProjection` foreground service type and `WRITE_EXTERNAL_STORAGE` permission (debug, maxSdkVersion 28; absent from release manifest via source-set isolation).
- **Service:** `DiagnosticRecorderService` extends `MediaProjectionCallback`, uses `MediaRecorder` to encode to MP4 video (no audio, no microphone permission).
- **Lifecycle:** Request `MediaProjection` once per app process (system consent dialog; cannot be bypassed or persisted). On background the live session is **paused** (`MediaRecorder.pause()`; projection, `VirtualDisplay` and foreground service stay alive, notification "Paused - app in background") and on return **resumed** with no dialog. Consent is asked again only with no live session (process restart, system stop, "Stop & keep"). Segment length counts recording time only (handler timer + `setMaxDuration`). See ADR-021 amendment 2026-10-07. **Opt-in background capture** (debug-only, default **OFF**, persisted in debug SharedPreferences): a second notification action "Background: OFF/ON" toggles it. When ON, the recorder is **not** paused on background and keeps capturing the app in front; when OFF, the pause/resume behaviour above applies. See ADR-021 amendment 2026-10-08. Foreground service posts persistent notification with "Stop & Keep" action to freeze segment rotation.
- **Storage:** MediaStore video with `RELATIVE_PATH = "Movies/TrackLocation-Diagnostics/"`. Segments are 10-minute MP4 files; app maintains last 6 (~60 min) (ADR-021 amendment 2026-10-08; previously 2-minute segments, last 5, ~10 min). On rollover, oldest segment deleted automatically. Files marked `IS_PENDING = false` after close, so clips appear in standard Photos/Gallery album.
- **On release builds:** No service, no permissions, no recorder classes. Clean APK, zero overhead.

**Review workflow:** Clips pulled from device (`adb pull`) and frames extracted with ffmpeg (`sudo apt install -y ffmpeg` on dev machine), then read as images for pixel-level analysis. No built-in video playback in the app.

**Consequences:** See ADR-021 §8 (cloud backup risk, segment rotation window, battery/CPU impact, post-uninstall orphans). No impact on canonical GPS recording, trips, sessions, or data model.

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

Room database (`TrackDatabase`), at version **v16** (current; **v16 added by ADR-027**, implemented in commit `a197453`, static review only; ADR-026 extends to v15, code `f26bb03`; ADR-025 extends to v14 2026-10-10; ADR-024 extended to v13 2026-10-09; ADR-023 extended to v12 2026-10-08). 

**Current migrations (all inline in `TrackDatabase.kt`):**
- `MIGRATION_1_2` (legacy serialized trip paths → canonical location rows + trip boundaries)
- `MIGRATION_2_3` (observer_event + allowlist_rule)
- `MIGRATION_3_4` (obd_sample)
- `MIGRATION_4_5` (OBD accumulator columns — `ALTER TABLE recording_session` + `ALTER TABLE track`; the domain "trip" is the physical `track` table)
- `MIGRATION_8_9` (ADR-008, DB→**v9**): `CREATE TABLE fuel_price (id INTEGER PK AUTOINCREMENT, pricePerLiter REAL NOT NULL, effectiveFromMs INTEGER NOT NULL)`, plus an `@Index` on `effectiveFromMs`
- `MIGRATION_9_10` (ADR-013, DB→v10): `CREATE TABLE observer_trip (id INTEGER PK AUTOINCREMENT, pickupName TEXT, pickupAddress TEXT, dropName TEXT, dropAddress TEXT, payment TEXT, earningsRp INTEGER, phase TEXT NOT NULL, firstSeenAt INTEGER NOT NULL, lastSeenAt INTEGER NOT NULL, handled INTEGER NOT NULL DEFAULT 0)` with an `@Index` on `lastSeenAt`
- `MIGRATION_10_11` (ADR-022, DB→v11): `ALTER TABLE track ADD COLUMN orderLabel TEXT` + `ALTER TABLE track ADD COLUMN orderEarningsRp INTEGER`
- `MIGRATION_11_12` (ADR-023, DB→**v12**): Twelve nullable columns added to `track` for order-trip geo snapshot:
  ```sql
  ALTER TABLE track ADD COLUMN acceptedName TEXT
  ALTER TABLE track ADD COLUMN acceptedAddress TEXT
  ALTER TABLE track ADD COLUMN acceptedLat REAL
  ALTER TABLE track ADD COLUMN acceptedLng REAL
  ALTER TABLE track ADD COLUMN pickupName TEXT
  ALTER TABLE track ADD COLUMN pickupAddress TEXT
  ALTER TABLE track ADD COLUMN pickupLat REAL
  ALTER TABLE track ADD COLUMN pickupLng REAL
  ALTER TABLE track ADD COLUMN dropName TEXT
  ALTER TABLE track ADD COLUMN dropAddress TEXT
  ALTER TABLE track ADD COLUMN dropLat REAL
  ALTER TABLE track ADD COLUMN dropLng REAL
  ```
  No default values; all columns nullable.
- `MIGRATION_12_13` (ADR-024, DB→**v13**): covered-distance columns for the fuel-covered-interval average:
  ```sql
  ALTER TABLE recording_session ADD COLUMN obdCoveredDistanceKm REAL NOT NULL DEFAULT 0.0
  ALTER TABLE track ADD COLUMN obdCoveredDistanceKm REAL NOT NULL DEFAULT 0.0
  ALTER TABLE obd_sample ADD COLUMN gpsSpeedKmh REAL
  ```
  Existing rows keep `0.0` / `NULL`; no backfill (pre-v13 trips keep the legacy displayed-distance ÷ fuel value).
- `MIGRATION_13_14` (ADR-025, DB→**v14**): order trip outcome columns on `track`:
  ```sql
  ALTER TABLE track ADD COLUMN orderStatus TEXT
  ALTER TABLE track ADD COLUMN orderOffline INTEGER NOT NULL DEFAULT 0
  UPDATE track SET orderStatus = 'COMPLETED' WHERE orderLabel IS NOT NULL
  ```
  Order-linked trips existing before v14 are marked `COMPLETED` (the earlier cancel outcome was not stored; history not rewritten). Manual trips keep `orderStatus` NULL and show no chip. `orderOffline` defaults to `0`.
- `MIGRATION_14_15` (ADR-026, DB→**v15**, implemented in code `f26bb03`): new table, no change to existing tables:
  ```sql
  CREATE TABLE trip_planned_route (
    tripStartedAt INTEGER NOT NULL PRIMARY KEY,
    kind TEXT NOT NULL,
    geometry TEXT NOT NULL,
    distanceM INTEGER,
    durationS INTEGER,
    originSource TEXT,
    fetchedAt INTEGER
  )
  ```
  No backfill: trips recorded before v15 have no planned route. Exported schema `15.json` is added with the code change. Gate: ORS ToS check (ADR-026 decision 10) before the code ships.
- `MIGRATION_15_16` (ADR-027, DB→**v16**, implemented in commit `a197453`; static review only, not built or run on device): new table, no change to existing tables. (The original scope said v14 → v15; the code is already at v15 via ADR-026, so this is v15 → v16.)
  ```sql
  CREATE TABLE fuel_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    kind TEXT NOT NULL,
    litres REAL NOT NULL,
    pricePerLiter REAL,
    resetBaselineL REAL,
    createdAtMs INTEGER NOT NULL
  )
  ```
  `resetBaselineL` is set only on RESET rows (cumulative OBD fuel total at the Reset, ADR-027 rev. 1.2) and is NULL for FILL_UP. Append-only; no deletes in ADR-027. No exported schema (`TrackDatabase` has `exportSchema = false`); the migration is verified by installing over a copy of the v15 database before shipping.

**Backfill logic for MIGRATION_11_12 (v11→v12):**
- For each `track` row with `orderLabel NOT NULL` (order-linked trips), match against `observer_trip` rows using the window rule: `firstSeenAt − 2 min` to `lastSeenAt + 5 min`, exactly one unambiguous match.
  - If exactly one match exists: copy pickup/drop names and addresses from the `observer_trip` row into `pickupName`, `pickupAddress`, `dropName`, `dropAddress`.
  - If multiple matches, no match, or the order row was pruned: leave pickup/drop fields `NULL` (no false linking).
- Backfill `acceptedLat`/`acceptedLng` from the `location_log` row referenced by `track.startLocationId` when available; leave `NULL` if `startLocationId` is `NULL` or the location row is missing.
- Leave `acceptedName`/`acceptedAddress` as `NULL` (no network call in migration; reverse-geocoding is captured on-demand for new trips).
- Post-migration, new order trips capture all twelve fields when the trip is persisted at stop; accepted location is reverse-geocoded once (fail-soft if offline) via OpenRouteService.

**Post-migration background backfill (ADR-023 Amendment 2026-10-08):**
- A one-time background backfill runs from `ShareViewModel` init (once per app process), processing up to 20 order-linked trips with `NULL` accepted names but present `acceptedLat`/`acceptedLng` coordinates (newest first).
- Each candidate is reverse-geocoded via OpenRouteService (8 s timeout, one retry); success writes `acceptedName`/`acceptedAddress`; failure leaves `NULL` (retried on next app launch).
- Processing is sequential with ~1.2 s delays, never blocking trip data or UI.
- No schema change (uses existing v12 columns); privacy treatment same as new trips (FR-20/ADR-017).

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
| gpsSpeedKmh | Double? | GPS speed at poll time (DB v13, ADR-024); null on pre-v13 rows (contributes 0 covered distance) |

Fuel-rate fallback chain: `DIRECT(015E) → MAF(0110) → SPEED_DENSITY → UNAVAILABLE`. Speed-density estimate: `MAF(g/s) = (RPM × MAP_kPa × VE × Displacement_L × 28.97)/(120 × 8.314 × IAT_K)`, `fuel(L/h) = MAF/(14.7×λ) × 3600/745` (VE=0.85, gasoline; engine displacement is a user pref, default 1193 cc).

**OBD Phase 2 accumulation — unified per ADR-007; averaging definition amended by ADR-024:**
- **One averaging definition (session and trip), per ADR-024 (supersedes the ADR-007 displacement-distance definition):** `avg km/L = Σ covered distance ÷ Σ fuel`, both taken over the same **fuel-covered intervals** — a poll interval with a valid fuel rate (0.1..100 L/h) and `0 < dt < 60 s`. Covered distance per interval = `gpsSpeedKmh` (GPS speed at that poll, stored on each `obd_sample`) × `dt`. Idle intervals (speed 0, fuel > 0) add fuel with 0 distance. Total fuel (and cost) is unchanged.
- **Live value = O(1) incremental cache** (session: `obdFuelConsumedL` + `obdCoveredDistanceKm`, updated together by `SessionDao.addObdAccumulator(sessionId, fuelL, coveredKm)`; trip: in-memory fuel and covered-distance totals since `tripStartedAt`). **Authoritative value at close = re-integrate `obd_sample`** (`ObdFuelMath.integrateFuelLiters` and `integrateCoveredDistanceKm`, same guards; null `gpsSpeedKmh` contributes 0) over the final window and persist it (`TrackingService.stopAlwaysRecording` → session row; `ShareViewModel.onTripCtaTap` → `track.obdFuelConsumedL` and `track.obdCoveredDistanceKm`); `obd_sample` is the single source of truth for fuel, the cache is a disposable live proxy (ADR-007 "Option B" — cache = memoized integral of the canonical rows). Mid-drive restart reseeds both in-memory trip totals by one `obd_sample` integration.
- **Session average** = `session.obdCoveredDistanceKm / session.obdFuelConsumedL` (ADR-024; `—` when covered = 0 or fuel ≤ 0.01 L). Pre-v13 sessions show `—`.
- **Trip average (live)** = `tripCoveredKm / tripFuelL` (in-memory, same guard).
- **Completed-trip average** = `track.obdCoveredDistanceKm / track.obdFuelConsumedL` when `obdCoveredDistanceKm > 0`, else the legacy `track.distance / track.obdFuelConsumedL` for pre-v13 trips (history not rewritten).
- **No plausibility clamp and no accuracy gating on covered distance** (fuel must stay complete for cost; excluding intervals only from distance would re-break the ratio).
- **Display:** idle (fuel accrues, distance flat) correctly degrades the average — relies on the ADR-006 stale-speed fix.
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
