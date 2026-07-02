---
name: ARCHITECTURE.md
path: docs/ARCHITECTURE.md
description: High-level system architecture, domain model, and design decisions — TrackLocation
---

# System Architecture
## TrackLocation

**Document Version:** 0.1
**Status:** Active (migrated from product-spec.md data/architecture rules)
**Last Updated:** 2026-06-15
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
LocationEntity (location_log)   ← canonical append-only GPS points (source of truth)
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

ObdSampleEntity (obd_sample)
  ├── timestampMs, rpm, obdSpeedKmh
  ├── fuelRateLph, mafGramsPerSecond, fuelRateSource
  └── adapterElapsedMs   (no FK to trip/session — linked by time-window queries)

SessionEntity (recording_session) — OBD accumulator columns (added DB v5)
  ├── obdFuelConsumedL  (REAL, default 0.0) — cumulative L consumed this session
  └── obdGpsDistanceKm  (REAL, default 0.0) — cumulative GPS km this session

TrackEntity (trip) — OBD accumulator column (added DB v5)
  └── obdFuelConsumedL  (REAL, default 0.0) — cumulative L consumed this trip
```

### Key Invariants

- The canonical `location_log` is the single source of truth for GPS points (see PRD §12 BR-01/BR-02).
- Sessions and trips are ranges/views over the canonical log; deleting a trip never deletes location rows.
- OBD samples and Observer events have **no foreign keys** to trips/sessions; they associate via `samplesBetween(startMs, endMs)` / time windows.
- Observer history is never user-deletable; retention is automatic (7 days / 50,000 rows, whichever is smaller).

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
| Persistence | Room (current DB version 5) | Local-first storage with migrations |
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
   → ObdSampleEntity (Room)            ← written only when session active (ACTION_SESSION_ON/OFF)
```

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

Room database (`TrackDatabase`), current version **5** (code at v5 as of OBD Phase 2 Slice 1, 2026-07-02; not yet built/device-verified per AGENTS.md §5a). Migrations (all inline in `TrackDatabase.kt`): `MIGRATION_1_2` (legacy serialized trip paths → canonical location rows + trip boundaries), `MIGRATION_2_3` (observer_event + allowlist_rule), `MIGRATION_3_4` (obd_sample), `MIGRATION_4_5` (OBD accumulator columns — `ALTER TABLE recording_session` + `ALTER TABLE track`; the domain "trip" is the physical `track` table).

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

**OBD Phase 2 accumulation (Slices 1–2 done, static; UI Slices 3–4 planned):**
- **Session (live accumulation):** `ObdPollingService` integrates each poll's fuel/distance (`delta-t × fuelRateLph / 3 600 000`, guard `0 < dt < 60 s`) into the active `recording_session` row via `SessionDao.addObdAccumulator`; `TrackingService` passes the session id (`EXTRA_SESSION_ID`) on session start/resume. Session average km/L = `obdGpsDistanceKm / obdFuelConsumedL`. Survives app/service restarts.
- **Trip (derived from samples — Issue #1):** a `track` row has no id until the trip stops, so fuel is **not** accumulated live. At trip stop, `ShareViewModel.onTripCtaTap()` integrates the `obd_sample` rows over `[tripStartedAt, now]` (`ObdSampleDao.samplesBetweenOnce`) and writes the total into `track.obdFuelConsumedL`. Trip average km/L = `track.distance_m/1000 / track.obdFuelConsumedL` for completed trips; the live active-trip figure (Slice 4) queries the same window against live trip distance. `SessionDao.addObdAccumulator` is used; `TrackDao.addObdFuel` (added in Slice 1) is currently unused under this approach.
- At idle (speed = 0, RPM > 0), the UI shows the instantaneous fuel rate as L/h instead of "--" (Slices 3–4).

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

## Reference Documents

- `docs/PRD.md` — Product requirements and features
- `docs/IMPLEMENTATION-PLAN.md` — Sprint/slice breakdown and tech stack summary
- `docs/adr/` — Architecture Decision Records (detailed decisions)
- `AGENTS.md` §4 — Technical responsibilities
- `docs/WORKFLOW.md` — Development workflow

---

**Last Reviewed:** 2026-06-15
**Next Review:** After Observer Phase 2 completion
