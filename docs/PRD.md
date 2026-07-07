---
name: PRD.md
path: docs/PRD.md
description: Product Requirements Document — TrackLocation
---

# Product Requirements Document
## TrackLocation

**Document Version:** 0.3
**Status:** Active (migrated from product-spec.md + change-requests.md)
**Created:** 2026-06-15
**Last Updated:** 2026-07-07
**Owner:** Project Team
**Controlled By:** `docs/DOCUMENT-CONTROL.md`

> Migrated 2026-06-15 from `docs/product-spec.md` (product baseline) and `docs/change-requests.md` (accepted decisions) during create-project schema adoption. Per-decision rationale now lives in `docs/adr/`; implementation status lives in `docs/IMPLEMENTATION-PLAN.md`.

---

## 1. Product Vision

TrackLocation is an Android-first driver utility evolving from a GPS trip tracker into a broader operational tool for drivers and support/developer use. The long-term direction is a local-first, sync-ready companion that continuously records location, lets users define explicit trips over that record, observes accessibility events for developer/support inspection, and surfaces live vehicle telemetry (OBD-II) — eventually gated behind registration and face-first authentication.

---

## 2. Product Problem

The app originally treated recorded location data mainly as trip-owned data. The product needs continuous location recording independent from trips, while still allowing users to define explicit trips. Drivers and support staff also need lightweight operational tooling (event inspection, vehicle telemetry) without a marketing-style consumer experience.

---

## 3. Product Objectives

The product must help users:

- Continuously record location into a canonical, append-only log independent of trips.
- Define, view, and manage explicit trips as ranges over that canonical log.
- Inspect accessibility events locally for developer/support purposes (Observer).
- View live vehicle telemetry (RPM, speed, fuel/efficiency) from an ELM327 OBD-II adapter.
- Operate calmly and reliably during repeated driver/support usage.

---

## 4. Core Product Principle

> The canonical location log is the single source of truth for GPS points. Sessions and trips are views/ranges over it — never owners of it. The UI stays operational, calm, technical, compact, and readable.

---

## 5. Target Users

### 5.1 Primary User

The driver — uses the app on Android during/around driving to record location, run trips, and read live efficiency/telemetry. Needs glanceable, high-contrast, touch-accurate UI.

### 5.2 Secondary User

The developer / support operator — uses the Observer to inspect accessibility events and snapshots for diagnostics, and OBD telemetry for vehicle data.

---

## 6. High-Level Features

### Feature 1: GPS Trip Tracking
- **What it does:** Start/stop explicit trip ranges over the canonical location log; resolve trip paths from location ranges.
- **User benefit:** Records and reviews discrete journeys without losing continuous history.

### Feature 2: Always-recorded Location Sessions
- **What it does:** ON-to-OFF always-recording sessions captured into the canonical location log; controlled from the Session screen switch.
- **User benefit:** Continuous location history independent of any trip.

### Feature 3: Accessibility Event Observer (developer/support)
- **What it does:** Local-first capture of accessibility events with allowlist filtering, tree snapshots, feed UI, and inspection.
- **User benefit:** On-device diagnostics of app/screen activity without remote dependencies.

### Feature 4: OBD-II Telemetry (ELM327 Bluetooth Classic)
- **What it does:** Polls RPM, speed, and fuel-rate from an ELM327 adapter; surfaces instantaneous km/L, L/h at idle, and session/trip average km/L on the Session screen and Trip panel.
- **User benefit:** Live vehicle efficiency and engine data tied to sessions and trips; idle fuel rate visible when stationary.

### Feature 5 (Planned): Registration + Face-first Authentication
- **What it does:** Registration gate, Google Sign-In, CameraX/ML Kit face enrollment, app-wide auth overlay.
- **User benefit:** Protects access while background capture/sync/GPS continue.

---

## 7. MVP Definition

### Delivered (verified)

- ✅ CR-0001 Always-recorded Location Sessions [Phase: Sessions]
- ✅ CR-0002 Session always-recording switch [Phase: Sessions]
- ✅ Observer Phase 1 — Local foundation (verified on device 2026-05-29)
- ✅ Observer Phase 2 — Inspection UI (pagination + snapshot viewer + truncation banner; verified on device 2026-07-02)
- ✅ OBD Phase 1 — ELM327 telemetry (live RPM/speed verified on device 2026-06-08; indirect speed-density fuel estimate)
- ❌ OBD Phase 2 — Fuel consumption enhancement (idle L/h display; session + trip average km/L; Trip panel fuel metrics)

### In progress / planned

- ✅ Observer Phase 3 — Filtering + Unified Settings (FTS text search + package-chip filtering + dedicated Observer Settings screen; device-verified 2026-07-07; see ADR-005)
- ❌ Observer Phases 3–7 (filtering, sync engine, Neon V1, registration + face enrollment, auth + hardening)

### Rationale

Sessions + trips + Observer P1 + OBD P1 form the working operational core. Sync, remote storage, and auth are deferred to later phases to keep the local-first baseline stable.

### Timeline

**Status:** Ongoing roadmap (no fixed external deadline recorded).

---

## 8. Product Scope

### Included

- Canonical append-only location log; sessions and trips as ranges over it.
- Session screen as the primary always-recording control surface.
- Observer under `Settings → Tools → Observer` (navigation Option B, accepted 2026-05-18).
- OBD-II under `Settings → Tools → OBD`; ELM327 Bluetooth Classic SPP only.
- Compose + Material 3 UI.

### Out of Scope (current)

- New navigation items beyond `Session / List / Track / Settings`.
- BLE OBD adapters; in-app BT discovery/PIN entry (system-settings pairing only).
- User-facing clear/delete of Observer history.
- Remote sync/backend, Neon, registration, and auth (later phases).

---

## 9. Functional Requirements

- **FR-01:** Location rows are the source of truth for recorded GPS points and are never deleted when a trip is deleted.
- **FR-02:** Turning always-recording ON starts a new session; OFF closes the active session (unless an active trip requires recording).
- **FR-03:** Trips are explicit ranges using `startLocationId`/`endLocationId`; empty trips are not saved.
- **FR-04:** Starting a trip while always-recording is OFF auto-starts always-recording and the Session switch reflects ON.
- **FR-05:** Observer captures only packages matching a user-configured allowlist (empty allowlist = capture all); history is not user-deletable.
- **FR-06:** OBD samples are stored only while a session is active; trips/sessions link to samples via time-window queries (no FKs).
- **FR-07:** When OBD is connected, speed = 0, and RPM > 0 (idle), the fuel consumption display shows the instantaneous fuel flow rate in L/h rather than "--".
- **FR-08:** Session average km/L is the ratio of cumulative GPS distance to cumulative fuel consumed since the session started; it persists across app restarts and resets when a new session begins.
- **FR-09:** Trip average km/L is the ratio of trip GPS distance to cumulative fuel consumed since the trip started; it persists to the trip record.
- **FR-10:** Always-recording persists across app restarts. If an open session survives a force-stop/kill and is still recent (a location point within the ~2-minute launch grace window), the app resumes recording on next launch — the Session status card returns to Active and the session keeps accumulating. Sessions idle beyond the grace window are closed on launch and stay stopped.

---

## 10. Non-Functional Requirements

- Local-first storage; sync-ready architecture (remote behind `RemoteDataSource`).
- Privacy: raw face images are never transmitted; unsynced local events are never deleted.
- Reliability: Observer/OBD must not degrade GPS tracking reliability.
- UI: operational/calm/technical/compact/readable; do not rely on color alone for state; light + dark usable.

---

## 11. Delivery Phases

### Phase — Sessions (delivered)
CR-0001 always-recorded sessions + canonical log; CR-0002 Session-screen switch as the single control surface.

### Phase — Observer (in progress)
P1 local foundation (done, verified) → P2 inspection UI (in progress) → P3 filtering/settings → P4 sync engine → P5 Neon V1 → P6 registration + face enrollment → P7 auth + hardening.

### Phase — OBD-II (Phase 1 delivered; Phase 2 planned)
- **Phase 1 (done):** ELM327 Bluetooth Classic telemetry: RPM/speed/fuel, instant km/L on Session + Trip panel; indirect speed-density fuel estimate for no-MAF vehicles.
- **Phase 2 (planned):** Idle L/h display; session average km/L (persisted to `recording_session`); trip average km/L (persisted to `trip`); Trip screen fuel metrics. DB migration 4→5.

---

## 12. Locked Business Rules

Inviolable constraints (migrated from CR-0001/CR-0002 guardrails and product-spec rules):

- **BR-01:** Sessions are separate from trips.
- **BR-02:** The canonical location log is the source of truth for GPS points; while stationary it stores one collapsed dwell anchor per stop rather than raw jitter fixes (see BR-11, ADR-006).
- **BR-03:** Trips reference location ranges with `startLocationId` and `endLocationId`.
- **BR-04:** Starting a trip while always-recording is OFF auto-starts always-recording.
- **BR-05:** Stopping a trip does not stop always-recording.
- **BR-06:** Deleting a trip must not delete canonical location history.
- **BR-07:** The Session screen is the primary always-recording control surface; the List screen remains trip-only and must not expose the always-recording switch.
- **BR-08:** If a trip is active, the user cannot turn always-recording OFF (guarded) unless a future CR changes this rule.
- **BR-09:** Observer history is not user-deletable/clearable; unsynced local events are never deleted.
- **BR-10:** OBD samples are stored only while a session is active.
- **BR-11:** During a stop, consecutive GPS fixes within tolerance `max(15 m, 1.5 × accuracy)` collapse into a single `location_log` anchor row: `timestamp` tracks the last confirmed-still fix (departure) and `dwellStartTimestamp` the first (arrival); `collapsedCount` counts the folded fixes and raw intra-dwell fixes are not retained. Movement is confirmed only after 2 consecutive out-of-tolerance fixes (single-outlier rejection); collapsed fixes add no session/trip distance. (ADR-006)

---

## Reference Documents

- `docs/IMPLEMENTATION-PLAN.md` — Implementation phases, slices, and task log
- `docs/UI-SPEC.md` — User interface specification and design system
- `docs/ARCHITECTURE.md` — System architecture, domain model, data model
- `docs/adr/` — Architecture Decision Records (CR-0001, CR-0002, navigation, OBD approach)
- `docs/DOCUMENT-CONTROL.md` — Version register and change log

---

**Status:** Active. Migrated from the legacy product-spec + change-requests docs; refine as new CRs are accepted.
