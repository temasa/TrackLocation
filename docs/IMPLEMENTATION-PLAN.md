---
name: IMPLEMENTATION-PLAN.md
path: docs/IMPLEMENTATION-PLAN.md
description: Implementation Plan — TrackLocation (phases, slices, task log, session history)
---

# Implementation Plan
## TrackLocation

**Version:** 0.2
**Status:** Active (migrated from implementation-plan.md + progress.md)
**Last Updated:** 2026-06-15
**Approach:** Incremental end-to-end vertical slices; two-track model (code work + UI design-handoff work) per AGENTS.md §12.

> Migrated 2026-06-15 to the create-project schema. The detailed per-phase/slice contract from the legacy `implementation-plan.md` is preserved verbatim in **Appendix A**. The full session/progress audit log from the legacy `progress.md` is preserved verbatim in **Appendix B**. §6 below is a summarized task log over those sessions.

---

## ▶ Next Step — Start Here

### Current — Observer Phase 2: Truncation banner

**Parallel work:** ✅ OBD Phase 1 is complete and hardware-verified (RPM/speed streaming, km/L calculation, session gating, stability hardening all working on SM-G965F with ELM327).

**Next action:** Submit the truncation-warning spec (`docs/design-handoff/observer_truncation/OBSERVER_TRUNCATION_SPEC.md`) + screenshots to Google Stitch / Claude Design for visual approval, then implement the truncation banner in `SnapshotViewerSheet.kt` with the approved design.

**Claude Code prompt** (paste at repo root; safe to re-paste to resume):
> Read AGENTS.md and docs/IMPLEMENTATION-PLAN.md, then continue Observer Phase 2 (truncation banner). Inspect the repo and the §6 Task Log; resume from the first incomplete item. Do not run Gradle/tests/emulator/device without explicit permission (AGENTS.md §5a). Stop for review at the truncation banner implementation.

### Completed (newest first)

- ✅ OBD Phase 1 — hardware verification complete (2026-06-15, ELM327 streaming verified)
- OBD Phase 1 — live device verification + indirect speed-density fuel (2026-06-08, `211ef56`)
- OBD Phase 1 Slice 3 — full polling loop, live telemetry (2026-06-07, `bd13fb3`)
- OBD Phase 1 Slice 2 — enable toggle + device picker (2026-06-07, `c8a68da`)
- OBD Phase 1 Slice 1 — infrastructure + navigation (2026-06-07, `d089fcf`)
- Observer Phase 1 — verified on device (2026-05-29)
- Observer Phase 2 — cursor pagination (2026-05-22, `ee54e9c`)
- CR-0002 Session always-recording switch (2026-05-16, `fe241a7`)
- CR-0001 Always-recorded sessions + canonical log (2026-05-16, `776cd6e`)

---

## 1. Change Log

| Version | Date | Change |
|---------|------|--------|
| 0.1 | (legacy) | Two-track implementation contract maintained in `implementation-plan.md`. |
| 0.2 | 2026-06-15 | Migrated to create-project schema; legacy plan → Appendix A, progress.md → Appendix B; summarized §6 task log. |

---

## 2. Purpose

Single authoritative plan for TrackLocation development: phases, per-slice breakdown, task log, and tech stack. Replaces the separate `implementation-plan.md` + `progress.md` pair.

---

## 3. Phases Overview

| Phase / Work item | Code status | UI/design status | Verification |
|---|---|---|---|
| CR-0001 Always-recorded Location Sessions | Implemented | Implemented | Verified by user |
| CR-0002 Session always-recording switch | Implemented | Implemented | Verified by user |
| Observer Phase 1 — Local Foundation | Implemented | Implemented | Verified on device (2026-05-29) |
| Observer Phase 2 — Inspection UI | Implemented (pagination + snapshot viewer); truncation code ready | Truncation banner spec drafted, awaiting design | Not fully verified on device |
| OBD Phase 1 — ELM327 telemetry | Implemented (4 slices) | Implemented | Verified on device (2026-06-15): RPM/speed streaming, km/L calculation, session gating, stability tested |
| Observer Phase 3 — Filtering + Settings | Planned | Planned | Not started |
| Observer Phase 4 — Sync Engine + Retention | Planned | Planned | Not started |
| Observer Phase 5 — Neon V1 Remote | Planned | Planned | Not started |
| Observer Phase 6 — Registration + Face Enrollment | Planned | Planned | Not started |
| Observer Phase 7 — Auth + Hardening | Planned | Planned | Not started |

Full detail for each phase/slice (goals, non-goals, numbered steps, per-slice verification, design-handoff tables) is preserved in **Appendix A**.

---

## 4. Detailed Slice Breakdown

The active and historical slice contracts (OBD Phase 1 Slices 1–4, Observer Phases 1–7, CR-0001/0002) live verbatim in **Appendix A — Detailed Phase/Slice Breakdown**. Each slice follows: *what it does → observable result → how to verify → numbered implementation steps (What/How per step)*. New slices must follow that format.

---

## 5. Timeline at a Glance

| Phase | Focus | Deliverable | Status |
|-------|-------|-------------|--------|
| Sessions | CR-0001/0002 | Canonical log + Session control surface | Done |
| Observer P1–P2 | Local capture + inspection | Feed, allowlist, snapshot viewer, pagination | P1 done; P2 in progress |
| OBD P1 | ELM327 telemetry | RPM/speed/fuel, km/L on Session + Trip | Done (live-verified) |
| Observer P3–P7 | Filtering → sync → remote → auth | (see Appendix A) | Planned |

---

## 6. Task Log (summarized)

Status: `Completed` | `In Progress` | `Blocked`. Full narrative for each entry is in **Appendix B**. Commit status is the single source of truth for branch/revision (AGENTS.md §8). A `Git Revision` of `---` is a placeholder to be backfilled with the introducing commit's hash in the next commit (AGENTS.md §8 backfill rule).

| Date | Task | Status | Git Revision | Verification |
|------|------|--------|--------------|--------------|
| 2026-06-15 | Hardware verification complete — OBD Phase 1 (ELM327 streaming) | Completed | `ab16161` | Live on device: RPM/speed streaming verified; km/L calculation functional; connection stable; session gating working; orphan reaper verified on restart |
| 2026-06-15 | Stability — silent-stop + exception hardening: TrackingService sticky-restart resume from open session, serviceScope CoroutineExceptionHandler, SecurityException guard on location updates, 1 ms→1 s timer; launch-time orphan reaper now stale-only (closeStaleActiveSessions, 2 min grace) to avoid racing resume; OBD FGS promotion on SESSION_ON + startForeground guard + null Bluetooth-adapter handling | Completed | `16c9f4e` | Device verified: build OK; app survives force-stop/restart; OBD FGS running; no crashes; 98 MB memory |
| 2026-06-08 | OBD — live device verification + connection bug fixes | Completed | `211ef56` | Live on device: RPM/speed streaming, no crashes; `assembleDebug` OK |
| 2026-06-08 | OBD — capability scan + indirect speed-density fuel estimate | Completed | `211ef56` | Live: ~1.08 L/h idle on 1.2L; source SPEED_DENSITY |
| 2026-06-07 | OBD Slice 3 — build + device verification | Completed | `bd13fb3` | `assembleDebug` OK; installed; no crash on launch |
| 2026-06-07 | OBD Slice 3 — full polling loop + live telemetry | Completed | `bd13fb3` | Static; build pending at the time |
| 2026-06-07 | OBD Phase 1 design spec for Slices 3–4 | Completed | (docs) | Documentation only |
| 2026-06-07 | OBD Slice 2 — enable toggle + device picker | Completed | `c8a68da` | `compileDebugKotlin` OK |
| 2026-06-07 | Fix — ObdSettingsScreen alignment vs mockup | Completed | `760500a` | Static |
| 2026-06-07 | Fix — ObdSampleEntity missing @Index (migration crash) | Completed | `192effd` | Static (root cause fixed) |
| 2026-06-07 | OBD Slice 1 — infrastructure + navigation | Completed | `d089fcf` / `dfc982e` | `compileDebugKotlin` OK |
| 2026-06-07 | OBD pre-check — kotlin-obd-api Kotlin 1.7.0 incompat | Completed | `3cd1fa8` | Decision: raw AT I/O |
| 2026-05-29 | OBD Phase 1 — ELM327 telemetry (docs) | Completed | (docs) | Docs step; source followed |
| 2026-05-29 | Observer Phase 1 — device verification complete | Completed | (multiple) | All P1 features verified on device |
| 2026-05-22 | Observer Phase 2 — cursor pagination | Completed | `ee54e9c` | Static; device verification pending |
| 2026-05-19 | Observer snapshot viewer — per-event modal sheet | Completed | `8ceb530` | `compileDebugKotlin` OK |
| 2026-05-19 | Sessions — indicator animation + header alignment | Completed | `eefb18c` | Static |
| 2026-05-19 | Observer Phase 1 Step 4 — tree snapshot DFS | Completed | (codex) | Static; device run pending |
| 2026-05-19 | Multiple Observer/Track/List fixes + build clean-up | Completed | (codex) | `assembleDebug` OK on several |
| 2026-05-18 | Observer Phase 1 — full implementation + infra | Completed | (codex) | Static; build verification pending |
| 2026-05-16 | CR-0002 — Session always-recording switch | Completed | `fe241a7` | Verified by user |
| 2026-05-16 | CR-0001 — always-recorded sessions + canonical log | Completed | `776cd6e` | Verified by user |
| 2026-05-14 | CR#1 — UI-first Sessions screen + 4-tab nav | Completed | `7ae11b9` | Static |

---

## 7. Current Status

- **PRD:** v0.2
- **DB version:** Room 4 (migrations 1→2, 2→3, 3→4)
- **Active work:** Observer Phase 2 truncation banner (design handoff)
- **Reference docs:** §13

---

## 8. Technical Architecture

### Required Environment / Toolchain
- Local JDK: `C:\Users\rinal\.jdks\jbr-17.0.14` (inject `JAVA_HOME` inline for any permitted Gradle run).
- Kotlin 1.7.0; Compose UI 1.2.x (compiler extension 1.2.0).

### Folder Structure
Single app module `app/` (`com.kolee.tracklocation`): `data/roomdb/`, `feature/obd/`, `feature/observer/`, `observer/`, `tracking/`, `screens/`, `navigation/`, `ui/theme/`. Full package map in Appendix A (Observer Phase 1) and ARCHITECTURE §2/§8.

### Commit Message Format

```
feat: <capability added>

- What: brief summary of what changed
- Why: the user value or constraint that prompted this
- How: technical approach (if non-obvious)

Closes: <task-id>  (e.g., OBD-S3, CR-0002)
```

Doc-only commits use `docs:`; fixes use `fix:`; blocker commits use `[doc-issue]` / `[doc-decision]` (AGENTS.md §9).

### Stack & Domain Model
See `docs/ARCHITECTURE.md` (§4 stack, §2 domain model, §8 schema).

---

## 9. Key Decisions Locked

| Decision | Resolution | Rationale |
|----------|-----------|-----------|
| Observer navigation | Option B — under `Settings → Tools → Observer` | Keeps 4-tab baseline; see `docs/adr/003` |
| OBD library | Raw AT I/O over Bluetooth socket (no kotlin-obd-api) | Library binary-incompatible with Kotlin 1.7.0; see `docs/adr/004` |
| Fuel rate on no-MAF vehicles | Indirect speed-density estimate | Test vehicle exposes no MAF/015E PID |

---

## 10. Success Criteria (current)

1. ✅ Canonical log + sessions + trips behave per PRD §12.
2. ✅ Observer P1 captures, filters, and inspects events on device.
3. ✅ OBD P1 streams RPM/speed and computes km/L (incl. no-MAF vehicles).
4. ⏳ Observer P2 truncation banner approved + implemented.

---

## 11. Dependencies & Risks

- **External:** ELM327 adapter for OBD live verification; physical device for Observer/OBD device tests.
- **Internal:** Compose 1.2.x API ceiling (avoid newer APIs); Room migrations must accompany schema changes.
- **Risk:** device/build verification gated on explicit user permission (AGENTS.md §5a) — some items remain static-inspection only.

---

## 12. What's NOT in current scope

- ❌ Remote sync / Neon / API backend (Observer P4–P5)
- ❌ Registration, face enrollment, auth overlay (Observer P6–P7)
- ❌ BLE OBD; in-app BT discovery/PIN entry

---

## 13. Manual Test Guides & Hardware Verification

Manual test procedures and hardware verification steps live here, alongside the implementation they verify. Each entry covers one feature, integration, or hardware component.

### Template per guide

```
### [Component / Feature Name] — Manual Verification

**Purpose:** <what this verifies>
**Prerequisites:** <hardware, environment, or setup needed>
**Steps:**
1. <step>
2. <step>
**Expected result:** <what success looks like>
**Known limitations / edge cases:** <anything to watch for>
```

### OBD-II Phase 1 — Manual Verification

**Purpose:** Verify ELM327 adapter connects and streams live PIDs into the app.
**Prerequisites:** KONNWEI ELM327 adapter plugged into OBD-II port; 1193cc gasoline car (ISO 15765-4 CAN); app installed with OBD Phase 1 build; Bluetooth paired.
**Steps:**
1. Start the car engine (or key-on for accessory power).
2. Open the app and navigate to the OBD screen.
3. Tap Connect — confirm the adapter connects without error toast.
4. Observe live PID values streaming (RPM, speed, coolant temp, fuel via speed-density).
5. Disconnect and reconnect to verify reconnection works.
6. Kill and relaunch the app; verify it survives sticky restart without crashing.
**Expected result:** PIDs update continuously; no MAF/015E values (adapter not supported); fuel reads via speed-density fallback.
**Known limitations / edge cases:** No MAF sensor on this vehicle — speed-density is the correct fuel path. Sticky restart (OS kill + relaunch) must not throw uncaught exceptions.

---

## 14. Reference Documents

- `docs/PRD.md` — Product requirements
- `docs/ARCHITECTURE.md` — Architecture, domain model, schema
- `docs/UI-SPEC.md` — UI spec + design tokens
- `docs/adr/` — Architecture Decision Records
- `docs/IMPLEMENTATION-ISSUES.md` — Blocker protocol
- `docs/DOCUMENT-CONTROL.md` — Version register

---

## Appendix A — Detailed Phase/Slice Breakdown (migrated verbatim from implementation-plan.md)

## Current Implementation Summary

| Work item | Code status | UI/design status | Verification |
|---|---|---|---|
| CR-0001 Always-recorded Location Sessions | Implemented | Implemented | Verified by user |
| CR-0002 Session always-recording switch | Implemented | Implemented | Verified by user |
| Observer Phase 1 Local Foundation | Implemented | Implemented | Verified on device (2026-05-29) |
| Observer Phase 2 Inspection UI | Implemented (pagination) + truncation code logic ready | Spec drafted (truncation), awaiting design | Not verified on device |
| OBD Phase 1 (ELM327 telemetry) | Planned | Planned | Not started |
| Observer Phase 3 Filtering + Settings | Planned | Planned | Not started |
| Observer Phase 4 Sync Engine | Planned | Planned | Not started |
| Observer Phase 5 Neon V1 | Planned | Planned | Not started |
| Observer Phase 6 Registration + Face Enrollment | Planned | Planned | Not started |
| Observer Phase 7 Auth + Hardening | Planned | Planned | Not started |

## Immediate Next Step

Observer Phase 1 is complete and verified on device (2026-05-29). Proceed to Observer Phase 2.

Phase 2 status (as of 2026-05-23):
- Cursor pagination: Implemented ✓
- Snapshot viewer sheet: Implemented ✓
- Truncation warning UI spec: Drafted, awaiting design handoff

Next: Submit truncation warning spec + screenshots to Google Stitch / Claude Design for visual treatment approval, then implement the truncation banner in `SnapshotViewerSheet.kt` with the approved design.

OBD Phase 1 is also queued for implementation. See OBD Phase 1 section below.

Notes:

- CR-0001 and CR-0002 are verified by the user as working as expected.
- Observer Phase 1 code + UI are implemented and verified on device.
- Observer Phase 2 code is 90% complete; design approval pending for truncation banner.
- OBD Phase 1 spec (product-spec.md + implementation-plan.md) is ready; source implementation queued.

## OBD Phase 1 — ELM327 Bluetooth Classic Telemetry

Status:

- Code implementation: Planned in 4 vertical slices (each with build + observable outcome).
- UI/design handoff: Not needed (all UI in Step 3.4 and beyond).
- Verification: Per-slice via Gradle compile check and device observable outcomes.

### A. Code Implementation Work

Goal:

- Capture RPM, OBD speed, and fuel-rate data from ELM327 adapters via Bluetooth
  Classic SPP.
- Store samples in a new `obd_sample` Room table with no FK ties to trips or sessions.
- Surface instantaneous and average km/L on the Session screen and Trip panel.

Non-goals (Phase 1):

- No OBD-to-trip FK columns; time-window queries only.
- No changes to `LocationEntity`, `SessionEntity`, or `TrackEntity` schemas.
- No BLE; ELM327 Bluetooth Classic SPP only.
- No in-app BT discovery or pairing; user pairs via Android system settings (Option A).
- No automated tests; manual verification only.

---

### Slice 1 — OBD entry point visible in the app

**What it does:** Add OBD infrastructure (Room entity/DAO/migration, DataStore, service shell) and a minimal OBD Settings screen showing "Idle — service not enabled" status. No Bluetooth or adapter required.

**Observable result:** Build → install → Settings → TOOLS section shows "OBD" row → tap → dedicated OBD screen opens with a status card (showing "Idle") and a disabled Enable toggle.

**How to verify:**
- Gradle compile: `./gradlew :app:compileDebugKotlin` — no errors
- Fresh install: MIGRATION_3_4 applies without crash
- Visual check: navigate Settings → TOOLS → "OBD" row appears; tap → OBD screen displays

**Implementation steps:**

1. **`AndroidManifest.xml`** — What: declare BT permissions and OBD service. How: add `BLUETOOTH` + `BLUETOOTH_ADMIN` (maxSdk 30), `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN neverForLocation` (API 31+); add `uses-feature bluetooth required=false`; declare `ObdPollingService` with `foregroundServiceType="connectedDevice"`
2. **Create** `data/roomdb/ObdSampleEntity.kt` — What: define database table for OBD samples. Schema: id, timestampMs, rpm, obdSpeedKmh, fuelRateLph, mafGramsPerSecond, fuelRateSource, adapterElapsedMs
3. **Create** `data/roomdb/ObdSampleDao.kt` — What: DAO for OBD sample CRUD and queries. Methods: `insert(ObdSampleEntity)`, `latestSample(): Flow<ObdSampleEntity?>`, `samplesBetween(startMs, endMs): Flow<List<ObdSampleEntity>>`, `deleteOlderThan(cutoffMs)`
4. **Edit** `data/roomdb/TrackDatabase.kt` — What: wire OBD entity and migration. How: add `ObdSampleEntity` to entities list; bump version 3→4; add `abstract fun obdSampleDao()`; add inline `MIGRATION_3_4` (`CREATE TABLE obd_sample` with all fields); chain migration into `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`
5. **Create** `feature/obd/data/ObdPreferencesDataStore.kt` — What: DataStore for OBD settings and state. DataStore name: `obd_prefs`. Keys (with defaults): `obdServiceEnabled` (bool, false), `obdDeviceMac` (string, ""), `obdPollHz` (int, 2), `obdRetentionDays` (int, 7), `obdRetryMaxSeconds` (int, 120), `obdLastState` (string, "Idle"), `obdLastError` (string, ""), `obdLastSampleTs` (long, 0). Model pattern on `feature/observer/data/ObserverPreferencesDataStore.kt`
6. **Edit** `TrackApp.kt` — What: expose OBD DAO and notification channel. How: add `val obdSampleDao by lazy { TrackDatabase.getDatabase(this).obdSampleDao() }`; in `onCreate`, create notification channel with id `OBD_CHANNEL_ID = "OBD_POLLING"`
7. **Create** `feature/obd/service/ObdPollingService.kt` (shell for Slice 1) — What: service to manage OBD connection. What it exposes: `companion object { val obdUiState = MutableStateFlow<ObdUiState>(ObdUiState.Idle) }`. What it does: handles `ACTION_START`/`ACTION_STOP` intents (no-op stubs for now); calls `startForeground()` with OBD notification; sets up `serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` and tears it down in `onDestroy()`. `ObdUiState` sealed class: `Idle`, `Connecting`, `Connected(rpm, obdSpeedKmh, fuelRateLph, fuelSource, instantKmL, avgKmL, sessionActive)`, `Retrying(attemptSeconds, maxSeconds)`, `Waiting(lastError)`
8. **Create** `screens/settings/obd/ObdSettingsScreen.kt` (shell for Slice 1) — What: Settings UI for OBD. Displays: status card showing `ObdPollingService.obdUiState` (initially "Idle"); Enable toggle (disabled for now, always shows OFF). No other controls yet
9. **Edit** `navigation/Screen.kt` — What: register OBD Settings route. How: add `object ObdSettingsScreen : Screen("obd_settings_screen")`
10. **Edit** `navigation/NavGraph.kt` — What: add OBD route to graph. How: add `composable(Screen.ObdSettingsScreen.route) { ObdSettingsScreen(navController) }`
11. **Edit** `screens/settings/SettingsScreen.kt` — What: add OBD link in TOOLS. How: in TOOLS section after Observer row, add `SettingsRow(title = "OBD", supporting = "ELM327 Bluetooth telemetry", onClick = { navController.navigate(Screen.ObdSettingsScreen.route) })`

---

### Slice 2 — Enable toggle + device picker (no real adapter needed)

**What it does:** Wire the Enable toggle to request BT permissions and attempt connection; add bonded device picker; show connection state (Connecting → Waiting on failure).

**Observable result:** Build → install → Settings → OBD → toggle ON → BT permission dialog → grant → bonded device picker appears → select device → status card shows "Connecting…" then "Waiting (no response)" (expected without a real adapter). "Pair a new device" tap opens system BT settings.

**How to verify:**
- Toggle request: toggle ON → system asks for `BLUETOOTH_CONNECT` on API 31+
- Deny permission: toggle stays OFF, shows inline error
- Grant permission: picker appears, can select bonded device
- Device selection: saving device MAC works (confirm in logcat or DataStore check)
- Device picker refresh: closing BT settings and returning to OBD screen refreshes picker
- System navigation: "Pair a new device" → `Settings.ACTION_BLUETOOTH_SETTINGS` opens system BT

**Implementation steps:**

1. **Edit** `feature/obd/service/ObdPollingService.kt` — What: implement connection attempt (no poll loop yet). Wire `ACTION_START`: read `obdDeviceMac` from DataStore; if blank stay `Idle`; if set, emit `Connecting`, attempt `BluetoothAdapter.getRemoteDevice(mac)` → `createRfcommSocketToServiceRecord(SPP_UUID)` → `socket.connect()` on IO; on connect failure, emit `Waiting(lastError)` and start exponential backoff (1s, 2s, 4s, …) capped at `obdRetryMaxSeconds`. Wire `ACTION_STOP`: close socket, emit `Idle`. On every state transition, write `obdLastState` to DataStore
2. **Edit** `screens/settings/obd/ObdSettingsScreen.kt` — What: full OBD Settings UX. Enable toggle: on toggle ON, request `BLUETOOTH_CONNECT` (API 31+) via Accompanist permissions; on grant, `startService(ACTION_START)` and save `obdServiceEnabled=true` to DataStore; on deny, show inline error chip and keep toggle OFF. Saved device row: show last saved device name + MAC + "Change" action. Bonded device picker: dialog listing `BluetoothAdapter.bondedDevices`, refresh on lifecycle `RESUMED` via `LaunchedEffect`. "Pair a new device" row: `startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))`. Status card: live `ObdPollingService.obdUiState` (shows Connecting, Waiting states). Reconnect button: visible in `Waiting` state, sends `ACTION_RECONNECT_NOW`. Preference selectors: poll rate, retention days, retry cap (write to DataStore on change)
3. **Edit** `MainActivity.kt` — What: auto-start OBD service on app launch if enabled. How: in `onCreate`, read `obdServiceEnabled` from DataStore on IO dispatcher; if true, call `ContextCompat.startForegroundService(Intent(...ACTION_START → ObdPollingService))`

---

### Slice 3 — Full BT polling loop + live telemetry (requires ELM327 adapter)

**What it does:** Implement the full OBD polling loop (RPM, speed, fuel); calculate km/L; gate writes to session active; add OBD metrics to Session screen.

**Observable result:** Pair ELM327 via system BT settings → Settings → OBD → select adapter → toggle ON → status card shows "Connected" → RPM and speed values update live (every 500 ms at 2 Hz). Session screen shows OBD status card with instant km/L (or "—" when speed < 3 km/h or accuracy > 20 m).

**How to verify:**
- ELM327 connect: status card transitions from Idle → Connecting → Connected, displays RPM + speed values updating
- km/L display: Session screen shows OBD card; instant km/L visible when driving, hidden when stopped
- Session gating: start session → `ACTION_SESSION_ON` sent to OBD service → OBD samples written to DB; stop session → `ACTION_SESSION_OFF` sent → no new DB writes
- No regression: GPS session/trip start/stop/stop work normally; Observer feed captures events

**Implementation steps:**

1. **Edit** `feature/obd/service/ObdPollingService.kt` — What: implement full poll loop and metrics using raw AT commands over Bluetooth socket (no kotlin-obd-api). After successful `socket.connect()`, emit `Connected` and start polling coroutine at `obdPollHz` Hz (`delay(1000L / pollHz)`). Poll sequence (raw AT I/O): send PID `010D` via `writeATCommand("010D\r")`, parse response `41 0D XX` hex to get `obdSpeedKmh = XX`; send PID `010C` to get `rpm = (256×A + B)/4`; then fuel fallback: try PID `015E` for direct fuel rate (DIRECT_FUEL_RATE); else try PID `0110` for MAF airflow in g/s (MAF_DERIVED: `fuelRateLph = maf × 3600 / (14.7 × 750)`); else mark UNAVAILABLE. Helper: `writeATCommand(cmd: String): String` writes to socket output stream, reads from input stream until `>` prompt. Calculate EMA instant km/L: `emaKmL = 0.2 × (gpsSpeedKmh / fuelRateLph) + 0.8 × emaKmL`; set to null when GPS speed < 3 km/h or accuracy > 20 m (read from `TrackingService.locationUiState`). Accumulate session fuel: `sessionFuelLiters += fuelRateLph × dtHours`; `sessionDistanceKm` from `TrackingService.locationUiState`. Compute average km/L: `sessionDistanceKm / sessionFuelLiters`. Write `ObdSampleEntity` to DB only when `sessionActive == true`. Retention cleanup: on `ACTION_START` and every 6 h, call `obdSampleDao.deleteOlderThan(now - retentionDays × 86_400_000)`
2. **Edit** `tracking/TrackingService.kt` — What: couple session start/stop to OBD service. In `startAlwaysRecording()`, send `startService(Intent(...ACTION_SESSION_ON → ObdPollingService))`; in stop path, send `ACTION_SESSION_OFF`. No direct field/state import across services
3. **Edit** `screens/sessions/SessionsScreen.kt` — What: display OBD metrics on Session screen. Collect `ObdPollingService.obdUiState` as state. Below always-recording card, add `ObdStatusCard`: displays OBD state label, instant km/L (hidden when GPS speed < 3 or accuracy > 20 m from `TrackingService.locationUiState`), average km/L (session accumulator), fuel source chip. Reconnect button in `Waiting` state

---

### Slice 4 — km/L in Trip panel during active trips

**What it does:** Add OBD fields to the Trip panel so km/L appears live during active trips, computed from OBD samples since trip start.

**Observable result:** Start a trip while OBD is Connected → Track screen trip panel shows an OBD row with instant km/L (updates live) and average km/L for the trip duration. Instant km/L hides when speed < 3 km/h.

**How to verify:**
- OBD row visible: active trip with OBD Connected → Track panel shows OBD row
- Instant km/L updates: visible every ~500 ms when speed > 3 km/h
- Instant km/L hides: when speed drops below 3 km/h or accuracy exceeds 20 m
- Average km/L: trip distance / sum of (fuelRateLph × dt for all OBD samples in trip window)
- No regression: GPS trip tracking, Observer feed, Session always-recording work normally

**Implementation steps:**

1. **Edit** `screens/track/TripState.kt` — What: add OBD fields to Trip panel state. Add to `TrackPanelState` data class: `instantKmL: Double? = null`, `avgKmL: Double? = null`, `fuelSource: String? = null`, `obdConnected: Boolean = false`
2. **Edit** `screens/track/components/TripPanel.kt` — What: render OBD row in trip panel. Below existing stats row, add OBD row: instant km/L cell (styled like other metric cells), average km/L cell, fuel source chip. Hide instant cell when `instantKmL == null`
3. **Edit** ViewModel / `TrackScreen.kt` — What: populate OBD fields from service state. Collect `ObdPollingService.obdUiState` and extract `instantKmL`, `avgKmL`, `fuelSource` into `TrackPanelState`. For trip average km/L: query `obdSampleDao.samplesBetween(tripStartMs, nowMs)`, sum all `(fuelRateLph × dt)` to get total trip fuel consumed, divide trip `distance` by total fuel

---

### B. UI Specification / Design Handoff Work

A single spec doc at `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md` covers all new UI surfaces. It must be produced and approved before the corresponding code slice starts.

| Surface | Introduced in | Spec covers | Code slice blocked |
|---|---|---|---|
| OBD row in Settings TOOLS | Slice 1 step 13 | Not required — identical `SettingsRow` pattern as existing Observer row; only decision is icon | No block |
| `ObdSettingsScreen` — Idle state | Slice 1 steps 10–12 | Status card visual in Idle state, disabled toggle treatment, overall screen layout | Slice 1 steps 10–12 |
| `ObdSettingsScreen` — full interactive UX | Slice 2 | Enable toggle, device picker dialog, Connecting/Waiting status card states, Reconnect button, preference selectors | Slice 2 |
| `ObdStatusCard` on Session screen | Slice 3 | Card layout, Connected/Waiting states, km/L display, fuel source chip | Slice 3 |
| OBD metric row in `TripPanel` | Slice 4 | Row layout, instant/avg km/L cells, fuel source chip, hidden-when-null instant cell | Slice 4 |

**Screenshots to attach for handoff:**
- Settings screen TOOLS section (Observer row as `SettingsRow` style reference)
- Session screen (always-recording status card as card style reference)
- Track screen TripPanel (existing metric cells as metric row style reference)

**Design handoff process:**
1. Create `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md` covering all five surfaces above
2. Attach screenshots listed above
3. Submit to Google Stitch or Claude Design for approval
4. Slice 1 infrastructure (steps 1–9) and the TOOLS row (step 13) can proceed immediately — no spec needed
5. Slice 1 screen steps (10–12) and all of Slices 2–4 are blocked until the relevant surface is approved

### Completed Pre-checks

| Pre-checks | Result |
|---|---|
| Kotlin compat | ✓ Tested 2026-06-07: `kotlin-obd-api` (master branch) compiled with Kotlin 2.3.0 is binary-incompatible with Kotlin 1.7.0. Error: "The binary version of its metadata is 2.3.0, expected version is 1.7.1." Decision: Skip library, implement raw AT I/O over Bluetooth socket + manual PID parsing. |

### Pending

| Per-slice verification | Work |
|---|---|
| Slice 1 | `./gradlew :app:compileDebugKotlin` → no errors; fresh install → no DB crash |
| Slice 1 | Settings → TOOLS → "OBD" row visible; tap → OBD screen opens with "Idle" status |
| Slice 2 | Toggle ON → BT permission dialog on API 31+; deny → toggle OFF with error; grant → device picker appears |
| Slice 2 | Device picker shows bonded devices; "Pair a new device" → system BT settings |
| Slice 2 | Select device → MAC saved to DataStore; close/reopen OBD screen → picker refreshes and shows selected device |
| Slice 2 | Toggle ON with device selected → status card shows "Connecting…" then "Waiting" (expected without adapter) |
| Slice 3 | Pair real ELM327 → toggle ON → status card shows "Connected" with RPM/speed updating |
| Slice 3 | Start session → observe `ObdPollingService.obdUiState.sessionActive = true`; OBD samples written to DB |
| Slice 3 | Session screen → OBD card visible; instant km/L shown when driving > 3 km/h, hidden when stopped |
| Slice 3 | Stop session → `sessionActive = false`; stop writing OBD samples to DB |
| Slice 4 | Start trip (with OBD Connected) → Track panel shows OBD row with instant/avg km/L |
| Slice 4 | Instant km/L updates live (~500 ms), hides when speed < 3 km/h |
| All slices | GPS session, trip start/stop, Observer feed: no regression |
| All slices | Retention cleanup works (old OBD rows deleted after 7 days or 50k-row cap) |
| Build | `./gradlew assembleDebug` — requires explicit user permission per AGENTS.md |
| Test | No automated tests in Phase 1 |

## Observer Phase 7 — Auth + Hardening

### A. Code Implementation Work

Planned:

- App-wide auth state controller.
- Auth overlay.
- Face attempt budget.
- Google fallback rules.
- 24-hour re-auth.
- Idle/screen-on behavior.
- Background capture/sync/GPS continues while UI locked.

### B. UI Specification / Design Handoff Work

Planned:

- Auth overlay.
- Failed attempt states.
- Lockout state.
- Google fallback text only after failed face attempt.
- Dark/light polish.

## Observer Phase 6 — Registration + Face Enrollment

### A. Code Implementation Work

Planned:

- Registration gate.
- Google Sign-In.
- CameraX face capture.
- ML Kit `FaceProcessor`.
- FaceData contract.
- Encrypted local identity/device storage.

### B. UI Specification / Design Handoff Work

Planned:

- Google sign-in step.
- Face capture step.
- Success step.
- Account section in Settings.

## Observer Phase 5 — Neon V1 Remote Storage

### A. Code Implementation Work

Planned:

- Neon schema/setup scripts.
- Low-privilege role.
- `NeonDirectDataSource`.
- Secure local configuration storage.
- Remote event mapping.

### B. UI Specification / Design Handoff Work

Planned:

- Generic remote sync status.
- No credential editing UI unless future CR explicitly requests it.
- No raw Neon internals in UI.

## Observer Phase 4 — Sync Engine + Retention

### A. Code Implementation Work

Planned:

- `RemoteDataSource`.
- Fake/no-op remote.
- Hybrid sync triggers.
- Batch size 50.
- Retry/backoff.
- Retention cleanup only after successful sync.
- Never delete unsynced events.

### B. UI Specification / Design Handoff Work

Planned:

- Sync status rows.
- Pending/synced counts.
- Offline banner.
- Retry banner.

## Observer Phase 3 — Filtering + Unified Settings

### A. Code Implementation Work

Planned:

- FTS5 support.
- Filter query builder.
- Package chips.
- Global text chips.
- Scoped per-package text filters.
Note: clearing/deleting observer history is not included (explicitly disallowed by current decisions).

### B. UI Specification / Design Handoff Work

Planned:

- Filter chip interaction design.
- No-results states.
- Observer Settings sections.

## Observer Rollout

Observer Phase 1 is implemented. Remaining phases (2–7) are planned.

### Required decision before Observer Phase 1

Choose one navigation option:

| Option | Navigation | Notes |
|---|---|---|
| A | Session / List / Track / Observer / Settings | first-class Observer, five tabs |
| B | Session / List / Track / Settings, Observer under Settings/tools | keeps four tabs |
| C | GPS / Track / Observer / Settings | larger redesign |

Recommendation:

- Prefer **Option B** unless the user explicitly wants Observer always visible. It preserves the accepted 4-tab baseline and minimizes churn.

Acceptance criteria for this decision:

- `docs/product-spec.md` is updated to reflect the accepted placement.
- Phase 1 UI work must not proceed until the placement is accepted.

Accepted decision:

- **Accepted: Option B** (2026-05-18).

## Observer Phase 2 — Inspection UI

Status:

- Code implementation: Done (cursor pagination complete; truncation metadata code logic already in domain model, awaiting design spec).
- UI specification: In progress (truncation warning spec drafted; awaiting design tool handoff; event detail and JSON viewer already implemented via SnapshotViewerSheet).
- Verification: Not verified on device.

### A. Code Implementation Work

Done:

| Item | Status | Notes |
|---|---|---|
| Cursor pagination | ✅ Done (2026-05-22, `ee54e9c`) | First page 50, load-more on scroll, live-event narrow flow |
| Event detail / JSON viewer | ✅ Done (2026-05-19, via `SnapshotViewerSheet`) | Formatted + raw JSON modes, copy, per-event modal |
| Truncation metadata code | ✅ Ready | Field already in `ObservedEvent` domain model, passed to sheet; code change depends on design spec |

Pending:

| Item | Status |
|---|---|
| Truncation warning UI design approval | Track B in progress |
| Truncation banner implementation (SnapshotViewerSheet.kt) | Waiting for design spec |

### B. UI Specification / Design Handoff Work

Done:

| Item | Status | Document |
|---|---|---|
| Event detail screen | ✅ Via `SnapshotViewerSheet` | Already implemented; shows all event metadata, timestamp, event type, repeat count |
| Full-screen JSON viewer | ✅ Via SnapshotViewerSheet Raw JSON tab | Formatted + raw modes, 2-space indentation, line numbers, copy action |
| Metadata grid | ✅ Via SnapshotViewerSheet MetaStrip | Chips for event type, first seen, last seen, repeat count (when > 1) |
| Truncation metadata warning | 📋 Spec drafted | `docs/design-handoff/observer_truncation/OBSERVER_TRUNCATION_SPEC.md` — awaiting design tool handoff |

Pending:

| Item | Status |
|---|---|
| Design approval of truncation banner | Awaiting handoff to Google Stitch / Claude Design |
| Implementation after design approval | Code implementation ready once design is approved |

## Observer Phase 1 — Local Accessibility Observer Foundation

Status:

- Code implementation: Done (static inspection).
- UI implementation: Done (static inspection).
- Verification: Not run — requires explicit user permission per `AGENTS.md`.

### A. Code Implementation Work

Goal:

- Capture accessibility events locally (Room) with safe limits, without impacting GPS tracking reliability.
- Provide a minimal feed UI under `Settings → Tools → Observer` (navigation Option B, accepted 2026-05-18).

Non-goals (Phase 1):

- No remote sync.
- No auth/registration.
- No advanced filtering/search (Phase 3).
- No deep inspection UI (Phase 2).
- No changes to GPS tracking behavior.
- No user-facing delete/clear of observer history.

Done:

| Step | Files / modules | Result |
|---|---|---|
| 1. Service foundation | `ObserverAccessibilityService.kt`, `AndroidManifest.xml`, `res/xml/accessibility_service_config.xml` | `AccessibilityService` declared with `BIND_ACCESSIBILITY_SERVICE`; config listens to `typeWindowStateChanged\|typeWindowContentChanged`; manifest declares service with correct intent-filter and meta-data; "Open settings" action wired to `ACTION_ACCESSIBILITY_SETTINGS` in `StatusIndicators.kt` |
| 2. Event capture surface | `ObserverAccessibilityService.kt` | Captures both event types; `TYPE_WINDOW_CONTENT_CHANGED` deduped by text summary — updates existing row's `lastSeenAt` + increments `repeatCount` instead of inserting a duplicate row |
| 3. Data contract | `ObservedEventEntity.kt`, `ObserverEventDao.kt` | Fields: `packageName`, `eventType`, `activityName`, `firstSeenAt`, `lastSeenAt`, `repeatCount`, `textSummary`, `treeSnapshot`, `truncationMetadata`; indices on `packageName` and `lastSeenAt` |
| 4. Tree snapshot capture (bounded) | `ObserverAccessibilityService.kt` — `captureTreeSnapshot()` | DFS from `event.source` via explicit stack; captures text, contentDescription, className, flags, bounds per node; limits: 200 nodes, depth 10, 300 chars/field, 40 KB JSON; recycles all `AccessibilityNodeInfo` nodes; truncation metadata records reason + nodesCaptured; wired into both insert and CONTENT_CHANGED dedup-update paths |
| 5. Noise reduction (allowlist) | `AllowlistRuleEntity.kt`, `AllowlistRuleDao.kt`, service | EXACT (full-string equality) and REGEX (substring, case-sensitive, `containsMatchIn`) on `packageName`; empty allowlist = capture all; invalid/uncompilable regex = disabled silently |
| 6. Local persistence | `TrackDatabase.kt` (v3), migration `MIGRATION_2_3` | Creates `observer_event` and `allowlist_rule` tables; service prunes rows older than 7 days and enforces 50,000-row cap automatically |
| 7. Repository / use-case boundary | `EventRepository` (interface + `EventRepositoryImpl` + `FakeEventRepository`), `ObserverPreferencesDataStore.kt` | Feed reads from Room via Flow; capture running state persisted in DataStore; `setCaptureRunning` toggled from ViewModel |
| 8. Minimal UI shell | `ObserverFeedScreen.kt`, `ObserverViewModel.kt`, 6 components, `NavGraph.kt` | Full feed screen under Settings → Tools → Observer; service status banner (enabled/disabled); capture chip (toggle, persisted); auto-scroll with drag-pause; jump-to-latest FAB (2s transient); long-press copy (paused only); allowlist bottom sheet (draft → apply pattern) |

Pending:

| Category | Required work |
|---|---|
| Verify | Step 4 — DFS implemented; runtime behavior on real device not yet confirmed (no build/device run) |
| Wire | `getEventsByPackage()` DAO method exists but ViewModel always fetches all events; scoped package feed is not yet wired |
| Build | `./gradlew assembleDebug` — requires explicit user permission per `AGENTS.md` |
| Device | Enable service in Android Accessibility Settings, confirm events appear in feed — requires explicit user permission per `AGENTS.md` |
| Test | Unit tests for dedup/retention logic; Room migration test for `MIGRATION_2_3` |

Package structure (actual):

```
com.kolee.tracklocation.observer.ObserverAccessibilityService
com.kolee.tracklocation.data.roomdb.{ObservedEventEntity, AllowlistRuleEntity, ObserverEventDao, AllowlistRuleDao}
com.kolee.tracklocation.feature.observer.domain.model.{ObservedEvent, AllowlistRule, ObserverUiState, AllowlistDraftRule, AllowlistUiState, MatchType}
com.kolee.tracklocation.feature.observer.data.{ObserverPreferencesDataStore, repository/EventRepository, repository/EventRepositoryImpl, repository/FakeEventRepository}
com.kolee.tracklocation.feature.observer.presentation.viewmodel.ObserverViewModel
com.kolee.tracklocation.feature.observer.presentation.screens.ObserverFeedScreen
com.kolee.tracklocation.feature.observer.presentation.components.{EventRow, FeedHeaderBar, AllowlistBottomSheet, JumpToLatestFab, EmptyState, StatusIndicators}
```

### B. UI Specification / Design Handoff Work

Done:

- Observer feed shell — `ObserverFeedScreen.kt`.
- Service status indicator (enabled green / disabled red with "Open settings") — `StatusIndicators.kt`.
- Capture pause/resume chip (independent of auto-scroll) — `CaptureChip` in `StatusIndicators.kt`.
- Auto-scroll readout (display-only indicator; tap list toggles) — `AutoScrollReadout`.
- Capture paused inline banner — `CapturePausedBanner`.
- Event cards with package, activity, event-type chip (color-coded by category), text snippet, timestamp — `EventRow.kt`.
- Empty state ("Waiting for events") — `EmptyState.kt`.
- Jump-to-latest transient FAB (2s auto-dismiss) — `JumpToLatestFab.kt`.
- Allowlist bottom sheet: add/edit/delete rules, match-type toggle (Exact/Regex), enable/disable switch, draft indicator, amber draft banner, Apply/Close footer — `AllowlistBottomSheet.kt`.
- Feed header bar with event count, scope label, live/paused dot — `FeedHeaderBar.kt`.

Verification gates (Phase 1):

- Static inspection: no coupling from observer to GPS tracking service — confirmed.
- Requires explicit user permission per `AGENTS.md`: `./gradlew assembleDebug`, Room migration test, device/emulator sanity check that service enables and events appear in feed.

## CR-0002 — Session Always-recording Switch

Status:

- Code implementation: Done.
- UI/design handoff: Done.
- Verification: Verified by user.

### A. Code Implementation Work

Done:

| Category | Files / modules | Result |
|---|---|---|
| Edit | `SessionsScreen.kt` | replaced hero mockup with compact status card and trailing switch |
| Edit | `ListScreen.kt` | removed always-recording control and permission flow |
| Edit | `ListContent.kt` | kept trip history UI only and removed switch UI |
| Edit | docs | updated CR and screen specification docs |

Pending:

| Category | Required work |
|---|---|
| Verify | Confirm Session switch compiles |
| Verify | Confirm List screen has no stale imports/copy/control |
| Verify | Confirm permission flow still works from Session screen |
| Verify | Confirm active-trip guard prevents OFF |
| Verify | Confirm starting trip while OFF auto-starts always-recording and Session switch shows ON |
| Verify | Confirm stopping trip does not turn switch OFF |

### B. UI Specification / Design Handoff Work

Done:

The Session screen UI handoff should include:

1. OFF / inactive state.
2. ON / active state with active session visible.
3. Permission-required state.
4. Permission-denied state.
5. Active-trip guarded state.
6. Auto-started-by-trip state.

Design guidance:

- Use compact Material 3 status-card treatment.
- Keep Session list visible.
- Use trailing/right-side switch.
- Do not put switch on List screen.
- Use text status, not color alone.
- Preserve light/dark usability.

### Design Tool Prompt for CR-0002

```text
Create or refine the Android Session screen for TrackLocation.

The Session screen is the first bottom-nav tab in:
Session / List / Track / Settings.

Purpose:
Show always-recorded location sessions and let the user control always-recording directly from the Session screen.

Design the always-recording status area as a compact Material 3 card/control panel.

Required elements:
- Title: Always-recording
- Status text:
  - Active when ON
  - Inactive when OFF
- Helper text:
  - OFF: Location sessions are not being recorded.
  - ON: Recording location sessions in the background.
  - Guard: Always-recording is required while a trip is running.
- Trailing/right-side switch.
- Sessions list remains visible below the status area.
- Active session, if present, appears at the top and is clearly marked.

Required states:
1. OFF / inactive
2. ON / active with active session visible
3. Permission required
4. Permission denied
5. Active-trip guarded state
6. Auto-started-by-trip state

Rules:
- Do not add another navigation destination.
- Do not place always-recording switch on the List screen.
- Do not merge sessions into trips.
- Do not imply that stopping a trip stops always-recording.
- Use Material 3, compact operational styling, readable status labels, and Android-safe spacing.
- Produce light and dark theme variants if possible.
```

## CR-0001 — Always-recorded Location Sessions

Status:

- Code implementation: Done.
- UI implementation: Done.
- Verification: Verified by user.

### A. Code Implementation Work

Done:

| Category | Files / modules | Result |
|---|---|---|
| Create | `LocationEntity.kt` | canonical GPS point row |
| Create | `SessionEntity.kt` | always-recording session row |
| Create | `LocationDao.kt` | canonical location queries |
| Create | `SessionDao.kt` | session persistence queries |
| Edit | `TrackDatabase.kt` | added entities and Room v1→v2 migration |
| Edit | `TrackEntity.kt` | added `startLocationId` and `endLocationId` |
| Edit | `TrackDao.kt` | added location-range-aware queries |
| Edit | `TrackingService.kt` | appends canonical points; manages session and trip ranges |
| Edit | `TrackScreen.kt` / running card components | trip-specific Start/Stop semantics |
| Edit | `DetailsScreen.kt` | resolves trip path from canonical location range |
| Edit | navigation files | added Session tab/start destination |

Pending:

| Category | Required work |
|---|---|
| Test | Add migration test for legacy trip path → location rows/trip boundaries |
| Test | Add always-recording ON/OFF session test |
| Test | Add trip boundary tests: start uses next point, stop uses latest point |
| Test | Add trip deletion test proving location rows remain |
| Verify | Compile/build only after user permission |
| Verify | Manual location/session/trip behavior on device/emulator only after user permission |

### B. UI Specification / Design Handoff Work

Done:

- Session top-level destination.
- `Session / List / Track / Settings` bottom nav.
- Session screen for always-recorded sessions.
- Real session rows connected after CR-0001 full implementation.
- Track copy changed toward trip-specific semantics.

No additional design handoff required before verification unless visual defects are found.

## Codex Verification Prompt

Use this next:

```text
Verify the current CR-0001 and CR-0002 implementation.

Read AGENTS.md first.

Then read:
1. docs/product-spec.md
2. docs/change-requests.md
3. docs/implementation-plan.md
4. docs/progress.md

Focus only on verification of completed CR-0001 and CR-0002.

Do not implement Observer, auth, sync, Neon, or registration.

Before editing source code:
- update docs/progress.md Current Session
- record start timestamp
- record goal
- record expected files
- set status to In Progress

Start with static inspection:
- confirm Session/List/Track/Settings navigation
- confirm Session screen contains the only always-recording switch
- confirm List screen is trip-only
- confirm Track Start/Stop trip semantics
- confirm Room migration and canonical location/session entities
- confirm trip detail path resolves from location ranges
- check for stale copy/imports around always-recording switch removal from List

Do not run Gradle, tests, emulator, or device verification unless I explicitly grant permission.

After inspection:
- update docs/progress.md
- list issues found
- list fixes made, if any
- list verification not run and why
- suggest the smallest next verification command
- provide a concise commit message if changes were made
```

---

## Appendix B — Session History (migrated verbatim from progress.md)

Full audit log of every implementation session, preserved verbatim. Summarized in §6 above.

## Current Session

### 2026-06-08 OBD Phase 1 — Live device verification + connection bug fixes

- Task: Verify OBD functionality live on connected device (SM-G965F, API 29) with a real KONNWEI ELM327 adapter (MAC 47:74:06:14:CD:B3); fix bugs found.
- Start: 2026-06-08
- End: 2026-06-08
- Status: Done — verified live on device; RPM + speed streaming, no crashes
- Live state before changes: adapter bonded + BR/EDR connected, BT on, but app stuck at `Waiting / Connection failed`.
- Root cause (found via added logging): **concurrent connection attempts**. `attemptConnection()` and `startRetryBackoff()` each launched independent coroutines with no single-flight guard; MainActivity's `obdServiceEnabled.collect{}` re-fired `ACTION_START` on every DataStore emission, and `START_STICKY` redelivery added more. Multiple coroutines opened/closed RFCOMM sockets to the same device, stomping each other → "read failed, socket might closed" / "Broken pipe". The hardware was fine the whole time.
- Bugs fixed:
  - Concurrency: refactored the connect → init → poll → backoff lifecycle into ONE serialized coroutine (`connectionJob`); `attemptConnection()` cancels any in-flight attempt + closes its socket before starting; removed parallel `startRetryBackoff()`/`pollingJob`; backoff now folded into the single loop. After the fix, even **secure SPP connects on the first try** (no fallback needed) — proving concurrency was the root cause.
  - Missing ELM327 init: added `ATZ / ATE0 / ATL0 / ATSP0` sequence before polling (spaces left ON so PID parsers still work). Verified live: `ELM327 v1.5`, `ATE0→OK`, etc.
  - RFCOMM robustness: `connectRfcomm()` cancels discovery then tries secure → insecure → reflection channel-1 (KONNWEI clones need the fallback under contention).
  - Fuel fallback was dead code (keyed on exceptions; `015E` unsupported returns the string `NO DATA`, not an exception). Now response-driven: `015E` then `0110`.
  - Fuel-unsupported latch: after 5 cycles with no `015E`/`0110` answer, stop probing fuel. Verified live: poll cadence improved from ~1.5 s to ~0.82 s/cycle.
  - `adapterElapsedMs` was always 0 (computed after `lastPollTimeMs` was overwritten) — now reflects real poll duration.
  - MainActivity: auto-start reads launch-time value via `.first()` instead of `collect{}`; `ACTION_START` guarded in service to not tear down a healthy connection.
  - Added Logcat logging throughout (`ObdPollingService` tag) for live diagnosis.
- Files edited:
  - `feature/obd/service/ObdPollingService.kt`
  - `MainActivity.kt`
- Live verification (device 213052810e037ece, KONNWEI ELM327 on running vehicle):
  - ✓ Single clean connection (secure SPP, first try), state → Connected, last_error cleared
  - ✓ ELM327 init sequence completes
  - ✓ RPM streaming (~970–1630) and SPEED streaming (~9–26 km/h) live, stable
  - ✓ Fuel correctly reported UNAVAILABLE (this vehicle exposes no fuel-rate PID — not a bug)
  - ✓ Fuel latch trips after 5 cycles, poll cadence ~doubles
  - ✓ No crashes (crash buffer clean)
- Build run: `:app:assembleDebug` — BUILD SUCCESSFUL; installed via `adb install -r`
- Known remaining:
  - km/L not derivable on this vehicle (no fuel-rate PID); will populate on vehicles that support `015E` or `0110`. Worth surfacing "fuel data unavailable on this vehicle" in the OBD UI in a future slice.
  - ObdStatusCard / km/L UI display not visually screenshot-verified this session (logic confirmed via state flow + logs).
- Commit status: Uncommitted
- Suggested commit message: `fix(obd): serialize connection lifecycle + ELM327 init — fixes concurrent-attempt socket races; live RPM/speed verified`

---

### 2026-06-08 OBD Phase 1 — Capability scan + indirect (speed-density) fuel estimate

- Task: Confirm OBD-II protocol, query which PIDs the vehicle exposes, and (since it has no MAF/direct-fuel PID) implement an indirect fuel-rate estimate so km/L works.
- Start: 2026-06-08
- End: 2026-06-08
- Status: Done — verified live; realistic idle fuel rate, no crashes
- Findings (live, via added `scanCapabilities` diagnostic):
  - OBD-II protocol negotiated: **ISO 15765-4 CAN 11-bit/500 kbps** (`ATDP` = "AUTO, ISO 15765-4 (CAN 11/500)", `ATDPN` = A6). Implementation was already pure OBD-II (ELM327 + ATSP0 auto + Mode-01 PIDs) — no protocol change needed.
  - Supported Mode-01 PIDs (35): `0101 0103 0104 0105 0106 0107 010B 010C 010D 010E 010F 0111 0113 0114 011C 011F 0120 0121 012E 0130 0131 0133 0140 0141 0142 0143 0144 0145 0146 0147 0149 014A 014C 0151 015A`
  - **No MAF (0110), no direct fuel rate (015E)** — both return NO DATA (not in bitmask). Fuel type (0151)=01 gasoline; commanded λ (0144)=1.00.
  - Present for speed-density: MAP (010B), IAT (010F), RPM (010C), baro (0133), load (0104/0143), λ (0144).
- Implemented: indirect fuel-rate estimate (speed-density) as 3rd fallback in the chain `DIRECT(015E) → MAF(0110) → SPEED_DENSITY → UNAVAILABLE`.
  - Formula: `MAF(g/s) = (RPM × MAP_kPa × VE × Displacement_L × 28.97) / (120 × 8.314 × IAT_K)`; `fuel(L/h) = MAF/(14.7×λ) × 3600/745`. VE=0.85, gasoline constants.
  - MAP read every cycle; IAT + λ cached, refreshed every 8 cycles.
  - Engine displacement is a new user preference (`obd_engine_displacement_cc`, default 1193) with a new "Engine Displacement" row in OBD Settings → PREFERENCES. User confirmed the test vehicle is **1193 cc**.
  - `mafGramsPerSecond` in `ObdSampleEntity` now populated from the estimate (was always null).
- Files edited:
  - `feature/obd/service/ObdPollingService.kt` — `scanCapabilities()` + `extractDataBytes()` diagnostics; `computeSpeedDensityFuel()`; fuel chain extended; sample MAF populated
  - `feature/obd/data/ObdPreferencesDataStore.kt` — `obdEngineDisplacementCc` pref + setter (default 1193)
  - `screens/settings/obd/ObdSettingsScreen.kt` — "Engine Displacement" preference row
- Live verification (KONNWEI ELM327, vehicle idling, parked):
  - ✓ Protocol confirmed ISO 15765-4 CAN
  - ✓ Fuel now estimated: **~1.08–1.10 L/h at ~845 rpm idle** — physically realistic for a 1.2 L gasoline engine
  - ✓ Source correctly reported `SPEED_DENSITY`; latch trips after 5 cycles, cadence ~1.0 s/cycle
  - ✓ No crashes
- Not verified (vehicle was parked): instant km/L on the road — gated on GPS speed > 3 km/h & accuracy ≤ 20 m; calculation path confirmed, awaits a drive.
- Note: `scanCapabilities`/ATDP diagnostics run once per process and only log; harmless to keep, can be removed later.
- Build run: `:app:assembleDebug` — BUILD SUCCESSFUL; installed via `adb install -r`
- Commit status: Uncommitted
- Suggested commit message: `feat(obd): indirect speed-density fuel estimate (MAP/IAT/RPM) + engine-displacement setting; live km/L enabled on no-MAF vehicles`

### 2026-06-07 OBD Phase 1 Slice 3 — Build + device verification

- Task: Compile Slice 3 code, fix bugs found before build, install on device, verify app launches without crash.
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done (build success + device install + no crash on launch)
- Files fixed (pre-build bugs corrected):
  - `feature/obd/service/ObdPollingService.kt` — replaced `.collect {}` with `.first()` for DataStore reads (obdDeviceMac, obdRetryMaxSeconds, obdPollHz, obdRetentionDays); fixed `insertSample` → `insert` (DAO method name); fixed `TrackApp.obdSampleDao` → `(applicationContext as TrackApp).obdSampleDao` (TrackApp is an instance, not an object); removed unused `currentState` variable; removed unused `TrackApp` import; added `kotlinx.coroutines.flow.first` import
- Build run: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (warnings only: deprecated BluetoothAdapter.getDefaultAdapter())
- Full build: `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL in 38s
- Device install: `adb install -r app-debug.apk` — Success (device 213052810e037ece)
- App launch: no FATAL/crash in logcat; Room migration and OBD channel created cleanly
- Commit status: Uncommitted
- Suggested commit message: `feat(obd): Slice 3 — full polling loop with raw AT I/O, live metrics, session gating, ObdStatusCard on Session screen`
- Known remaining:
  - Live ELM327 adapter test (RPM/speed updates, km/L display) requires physical OBD dongle on the vehicle
  - Session start/stop coupling (ACTION_SESSION_ON/OFF) not verified without real adapter
  - ObdStatusCard only visible when OBD state is Connected or Waiting (card is hidden in Idle — correct behavior)

---

### 2026-06-07 OBD Phase 1 Spec — Design handoff prepared for Slices 3–4

- Task: Create and rectify OBD_PHASE1_SPEC.md to document design requirements for ObdStatusCard (Session screen, Slice 3) and OBD metric row (TripPanel, Slice 4).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Files created:
  - `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md` — specification covering both remaining UI surfaces (Slices 3–4); Slices 1–2 noted as already implemented
- Files edited: None
- Build run: Not required
- Tests run: None
- Commit status: Uncommitted (documentation only)
- Suggested commit message: `docs(obd): OBD Phase 1 design spec — Surfaces 2–3 for Slices 3–4 (Sessions + TripPanel)`
- Next step: Collect reference screenshots (Session screen always-recording card, TripPanel existing metrics) and submit spec to Google Stitch / Claude Design for visual approval

---

### 2026-06-07 OBD Phase 1 Slice 3 — Full BT polling loop + live telemetry

- Task: Implement full OBD polling loop (RPM, speed, fuel); calculate km/L; gate writes to session active; add OBD metrics to Session screen.
- Start: 2026-06-07 14:30 (design mockups extracted: Connected + Waiting states)
- End: 2026-06-07 15:45
- Status: Done (implementation complete, build pending)
- Files edited:
  - `feature/obd/service/ObdPollingService.kt` — full polling loop with raw AT commands (PID 010D for speed, 010C for RPM, 015E/0110 for fuel); EMA instant km/L calculation; session gating (ACTION_SESSION_ON/OFF); OBD sample writes to DB when session active; 6-hour retention cleanup; startPollingLoop() at 1-5 Hz; writeATCommand() helper for raw AT I/O over socket; parseObdSpeed/Rpm/Fuel() helpers with fallback chain (DIRECT_FUEL_RATE → MAF_DERIVED → UNAVAILABLE)
  - `tracking/TrackingService.kt` — added import for ObdPollingService; send ACTION_SESSION_ON intent in startAlwaysRecording(); send ACTION_SESSION_OFF intent in stopAlwaysRecording()
  - `screens/sessions/SessionsScreen.kt` — collect ObdPollingService.obdUiState; added ObdStatusCard() composable displaying Connected/Waiting states with RPM/SPEED/EFFICIENCY metrics; instant km/L hidden when GPS speed < 3 km/h or accuracy > 20m; Reconnect button visible in Waiting state; added Button + Icon imports
  - `tracking/LocationUiState.kt` — added accuracyMeters: Float field (from GPS location.accuracy)
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Commit status: Uncommitted
- Suggested commit message: `feat(obd): Slice 3 — full polling loop with raw AT I/O, live metrics, session gating, ObdStatusCard on Session screen`
- Acceptance criteria status:
  - ✓ Connection flow: Idle → Connecting → Connected (state transitions implemented)
  - ✓ Waiting state with error message (ObdUiState.Waiting implemented)
  - ✓ Live metrics update at configurable Hz (startPollingLoop() at obdPollHz)
  - ✓ Instant km/L calculation with EMA + visibility gating (speedInKMH > 3 AND accuracyMeters <= 20)
  - ✓ Reconnect button in Waiting state (ObdStatusCard renders button)
  - ✓ Session gating: OBD samples written only when sessionActive == true
  - ✓ No regression: GPS tracking calls unchanged (TrackingService.startAlwaysRecording/stopAlwaysRecording only send intents, no other changes)
- Known issues:
  - Build/compilation not verified (pending user permission to run Gradle)
  - Device testing pending (ELM327 required)
  - AT command parsing assumes standard ELM327 response format; real devices may vary

---

## Current Session (Prior)

### 2026-06-07 OBD Phase 1 Slice 2 — Enable toggle + device picker

- Task: Wire Enable toggle to request Bluetooth permissions and attempt connection. Add bonded device picker. Show connection state (Connecting → Waiting on failure).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done (Compilation successful)
- Files edited:
  - `feature/obd/service/ObdPollingService.kt` — implemented connection attempt with exponential backoff (1s, 2s, 4s... capped at obdRetryMaxSeconds); ACTION_START attempts BluetoothAdapter connection; ACTION_STOP closes socket and emits Idle; ACTION_RECONNECT_NOW resets retry delay to 1s and attempts connection; socket state and error persisted to DataStore; uses Flow.collect() for DataStore access
  - `screens/settings/obd/ObdSettingsScreen.kt` — full OBD Settings UX: Enable toggle requests BLUETOOTH_CONNECT permission (API 31+) via ContextCompat.checkSelfPermission() and starts/stops service; bonded device picker dialog (refreshes on RESUMED); "Pair a new device" opens system BT settings; saved device row with "Change" action; Reconnect button visible in Waiting state; preference selectors for poll rate (1/2/5 Hz), retention days (1–30), retry cap (30/60/120/300s)
  - `MainActivity.kt` — auto-start OBD service on app launch if obdServiceEnabled is true in DataStore using Flow.collect()
- Build run: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (warnings only: deprecated BluetoothAdapter.getDefaultAdapter())
- Tests run: None
- Commit status: Uncommitted (ready to commit)
- Suggested commit message: `feat(obd): Slice 2 — enable toggle + device picker + connection attempt with exponential backoff`
- Observable result ready: toggle ON → BT permission dialog (API 31+) → grant → bonded device picker → select device → status shows "Connecting…" → "Waiting (no response)" (expected without ELM327 adapter)

---

### 2026-06-07 Fix — ObdSettingsScreen cosmetic gaps vs mockup

- Task: Align ObdSettingsScreen with design mockup for Slice 1 — proper Scaffold + TopAppBar with back button, status row styled to match SettingsScreen pattern (icon + label + subtitle + disabled Switch).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Files edited:
  - `screens/settings/obd/ObdSettingsScreen.kt` — replaced bare Column with Scaffold + TopAppBar (back nav); replaced plain Text/Card/Switch with ObdSectionGroup + ObdStatusRow matching SettingsScreen token style; added `getStatusSubtitle()` for "Service not enabled" subtitle
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Commit status: Uncommitted
- Suggested commit message: `fix(obd): align ObdSettingsScreen with mockup — Scaffold + TopAppBar + status row style`

---

### 2026-06-07 Fix — ObdSampleEntity missing @Index causing Room migration crash

- Task: Fix `IllegalStateException: Migration didn't properly handle: obd_sample` crash on SM-G965F (API 29).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Root cause: `MIGRATION_3_4` creates `index_obd_sample_timestampMs` on `timestampMs`, but `ObdSampleEntity` had no `@Index` annotation. Room's post-migration schema validation compared the actual DB (with index) against the entity definition (no index) and threw `IllegalStateException`.
- Files edited:
  - `data/roomdb/ObdSampleEntity.kt` — added `indices = [Index(value = ["timestampMs"])]` to `@Entity` annotation; added `import androidx.room.Index`
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Commit status: Uncommitted
- Suggested commit message: `fix(obd): add @Index(timestampMs) to ObdSampleEntity — migration created index but entity didn't declare it`

---

### 2026-06-07 OBD Phase 1 Slice 1 — Infrastructure + Navigation (steps 1–11)

- Task: Implement OBD Phase 1 Slice 1 (complete): Room entity/DAO/migration, DataStore, ObdPollingService shell, ObdSettingsScreen UI, navigation route and TOOLS row in Settings.
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Files created:
  - `data/roomdb/ObdSampleEntity.kt` — Room entity (8 fields)
  - `data/roomdb/ObdSampleDao.kt` — DAO (insert, latestSample, samplesBetween, deleteOlderThan)
  - `feature/obd/data/ObdPreferencesDataStore.kt` — DataStore (8 preference keys with defaults)
  - `feature/obd/service/ObdPollingService.kt` — Foreground service shell with ObdUiState sealed class
  - `screens/settings/obd/ObdSettingsScreen.kt` — UI shell (status card, disabled toggle)
- Files edited:
  - `AndroidManifest.xml` — BT permissions (API-gated), uses-feature, ObdPollingService declaration
  - `data/roomdb/TrackDatabase.kt` — version 3→4, MIGRATION_3_4, obdSampleDao, added entity to list
  - `TrackApp.kt` — obdSampleDao lazy, OBD notification channel
  - `navigation/Screen.kt` — ObdSettingsScreen route object
  - `screens/settings/SettingsScreen.kt` — added OBD row in TOOLS section (Step 10)
  - `navigation/NavGraph.kt` — added ObdSettingsScreen route + import (Step 11)
- Compile verification: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (2 changes)
- Commit status: Committed — branch `codex`, revision `d089fcf`

---

### 2026-06-07 OBD Phase 1 Pre-check — Kotlin 1.7.0 Compatibility

- Task: Confirm whether `kotlin-obd-api` library compiles with Kotlin 1.7.0 project. If incompatible, skip library and implement raw AT I/O over Bluetooth socket instead.
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done — pre-check completed; library is incompatible
- Build run: `./gradlew :app:compileDebugKotlin` — tested with `kotlin-obd-api:1.1.0` and `master-SNAPSHOT`; both failed with "The binary version of its metadata is 2.3.0/2.1.0, expected version is 1.7.1"
- Decision: Skip kotlin-obd-api library. Implement raw AT commands over Bluetooth socket (PID 010D for speed, 010C for RPM, 015E/0110 for fuel rate with fallback chain). Manual AT command parsing instead of pre-built Command classes.
- Files changed: None (dependency verification only; documentation updated with raw AT I/O approach)
- Docs updated: `docs/implementation-plan.md` — completed pre-check table, removed JitPack dependency step, updated Slice 3 Step 1 with raw AT I/O details
- Suggested commit message: `docs: OBD Phase 1 pre-check — kotlin-obd-api incompatible with Kotlin 1.7.0, switch to raw AT I/O`

---

### 2026-05-29 OBD Phase 1 — ELM327 Bluetooth Classic Telemetry

- Task: Implement OBD-II support — update docs, then add Room entity/DAO/migration,
  ObdPollingService (foreground), ObdPreferencesDataStore, OBD Settings screen and
  navigation, Session screen and Trip panel km/L metrics.
- Start: 2026-05-29
- End: (in progress)
- Status: In Progress — docs step complete; source changes pending
- Expected files created:
  - `data/roomdb/ObdSampleEntity.kt`
  - `data/roomdb/ObdSampleDao.kt`
  - `feature/obd/data/ObdPreferencesDataStore.kt`
  - `feature/obd/service/ObdPollingService.kt`
  - `screens/settings/obd/ObdSettingsScreen.kt`
- Expected files edited:
  - `data/roomdb/TrackDatabase.kt` (bump v4, MIGRATION_3_4)
  - `TrackApp.kt` (obdSampleDao + OBD notification channel)
  - `tracking/TrackingService.kt` (SESSION_ON/OFF intents)
  - `MainActivity.kt` (auto-start ObdPollingService)
  - `navigation/Screen.kt`, `navigation/NavGraph.kt`
  - `screens/settings/SettingsScreen.kt` (OBD Tools row)
  - `screens/sessions/SessionsScreen.kt` (ObdStatusCard + km/L)
  - `screens/track/TripState.kt`, `screens/track/components/TripPanel.kt` (km/L row)
  - `app/build.gradle`, `AndroidManifest.xml`
- Docs updated before source changes: `docs/product-spec.md` ✓, `docs/implementation-plan.md` ✓

---

### 2026-05-22 Observer Phase 2 — Cursor Pagination

- Task: Replace unbounded `getAllEvents()` feed loading with cursor-based pagination. First page of 50 events on open; `loadMore()` triggered when user scrolls to oldest visible items; live new events appended via narrow `getEventsNewerThan()` Flow. Phase 2 otherwise complete (inspection covered by `SnapshotViewerSheet` from Phase 1).
- Start: 2026-05-22
- End: 2026-05-22
- Status: Done (static inspection)
- Commit status: Committed — branch `codex`, revision `ee54e9c`
- Files edited:
  - `data/roomdb/ObserverEventDao.kt` — added `getEventsFirstPage(limit)`, `getEventsNextPage(beforeLastSeenAt, limit)`, `getEventsNewerThan(afterLastSeenAt)` queries
  - `feature/observer/data/repository/EventRepository.kt` — added `getFirstPage`, `getNextPage`, `getEventsNewerThan` to interface and `EventRepositoryImpl`; extracted `toDomain()` private helper to deduplicate mapping; added stubs to `FakeEventRepository`
  - `feature/observer/domain/model/ObserverUiState.kt` — added `canLoadMore: Boolean = false` and `isLoadingMore: Boolean = false`
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — removed unbounded `getEvents()` collection; added in-memory `_loadedEvents`/`_loadedIds`/`_oldestLastSeenAt`/`_newestLastSeenAt` tracking; `init` loads first page then collects `getEventsNewerThan()` for live arrivals; added `loadMore()` fun with prepend + `_prependedCount` SharedFlow; `PAGE_SIZE = 50` in companion
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — imported `derivedStateOf`; added `hasScrolled` flag (set on first scroll); `shouldLoadMore` derived state gates `loadMore()` call; `LaunchedEffect` collects `prependedCount` and adjusts scroll with `scrollToItem(firstIdx + count)`; added `key = { _, event -> event.id }` to `itemsIndexed` for stable item identity
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known remaining: device verification needed; edge case — events with identical `lastSeenAt` ms at page boundary may be skipped by cursor
- Suggested commit message: `feat(observer): cursor pagination — first page of 50, load-more on scroll, live-event narrow flow`

---

### 2026-05-19 Observer Snapshot Viewer — per-event modal sheet

- Task: Add "View window content" link to each Observer event card; tapping opens a modal bottom sheet showing the event's captured `treeSnapshot` as a flat formatted node list or raw JSON, with Copy and all four dismiss methods.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (build verified — `BUILD SUCCESSFUL`)
- Commit status: Committed — branch `codex`, revision `8ceb530`
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/ObservedEvent.kt` — added `firstSeenMs`, `repeatCount`, `treeSnapshot`, `truncationMetadata` fields
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/repository/EventRepository.kt` — mapped new fields from entity; updated `FakeEventRepository` defaults
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/EventRow.kt` — added `onViewSnapshot: (() -> Unit)?` param; added "View window content" link row (hidden when no snapshot)
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `snapshotEvent` local state; wired `onViewSnapshot` into `EventRow`; mounted `SnapshotViewerSheet` conditionally
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/SnapshotViewerSheet.kt` — full modal sheet (Dialog+scrim pattern; DragHandle, TitleRow, MetaStrip, ModeToolbar, ContentArea; Formatted flat node list + Raw JSON + Parse-error + No-readable-text edge states; Copy with 1.4s confirmation; all four dismiss paths)
- Build run: `./gradlew :app:compileDebugKotlin --no-daemon` — BUILD SUCCESSFUL (1 unused-param warning, no errors)
- Tests run: None
- Known remaining: device verification needed
- Suggested commit message: `feat(observer): snapshot viewer sheet — per-event modal with formatted/raw views and copy`

---

### 2026-05-19 Sessions screen — Indicator animation + active card header alignment

- Task: Two UI fixes targeting `SessionsScreen.kt` only:
  1. Always-recording indicator (Active state): blinking inner dot (opacity 1↔0.35, 1200ms) + two concentric ripple rings (scale 1.0→1.65, alpha 0.9→0, 1800ms, 0.6s stagger) — mirrors Trips screen pattern. Reduce-motion aware.
  2. Active session card header: removed absolutely-positioned ACTIVE badge + 70dp padding hack; replaced with a flat `Row(CenterVertically)` containing title (weight 1), then a nested row with start time + badge inline.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection)
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/screens/sessions/SessionsScreen.kt` — added `LinearOutSlowInEasing` + `graphicsLayer` imports; `StatusCard`: added reduce-motion check (`ANIMATOR_DURATION_SCALE == 0`), indicator 52→64dp, `SessionPulseRing` ×2 before dot, `PulseDot` now takes `pulseTargetAlpha=0.35f`/`pulseDurationMs=1200` for indicator; `SessionRow`: removed absolute `ActiveBadge`, header `Row` is now `CenterVertically`+`spacedBy(10dp)` with nested right-side group; `PulseDot`: added optional `pulseTargetAlpha`/`pulseDurationMs` params (defaults preserve `ActiveBadge` behavior); added `SessionPulseRing` private composable
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known limitations:
  - CSS `box-shadow` glow on inner dot skipped — no Compose 1.2.x equivalent; ambient glow provided by greenSoft indicator background
  - Easing: `ease-out` → `LinearOutSlowInEasing` (Compose 1.2.x compat); `ease-in-out` → `FastOutSlowInEasing` (tween default)
- Suggested commit message: `feat(sessions): animate always-recording indicator (blink + pulse rings) + fix active card header alignment`

---

### 2026-05-19 Observer Phase 1 Step 4 — tree snapshot DFS in ObserverAccessibilityService

- Task: `treeSnapshot` and `truncationMetadata` fields existed in `ObservedEventEntity` but were always written as `null`. Implemented bounded DFS traversal in `ObserverAccessibilityService` to populate them.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection)
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — added `captureTreeSnapshot(event)` private method: DFS via explicit stack from `event.source`; collects text, contentDescription, className, isClickable, isEditable, isEnabled, bounds per node; limits: 200 nodes, depth 10, 300 chars/text field, 40 KB JSON cap; recycles every `AccessibilityNodeInfo` after use; returns `(snapshotJson, truncationMetadataJson)` where truncation JSON records `reason` (node_limit / depth_limit / size_limit) and `nodesCaptured`; wired into both insert path and CONTENT_CHANGED dedup update path.
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known remaining: device verification needed to confirm DFS runs without crash/ANR on real event volume
- Suggested commit message: `feat(observer): implement tree snapshot DFS capture with node/depth/size limits`

---

### Observer Phase 1 — Local Accessibility Observer Foundation (full implementation)

- Task: Implement all 8 steps of Observer Phase 1 as defined in `docs/implementation-plan.md`. Navigation placement (Option B: Settings → Tools → Observer) was accepted 2026-05-18.
- Start: (prior session — exact date not recorded at the time)
- End: (prior session — discovered via codebase audit on 2026-05-19)
- Status: Done (code + UI, static inspection); device verification pending (requires explicit user permission per AGENTS.md)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObservedEventEntity.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObserverEventDao.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleEntity.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleDao.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/ObservedEvent.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/AllowlistRule.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/ObserverUiState.kt` (includes `AllowlistDraftRule`, `AllowlistUiState`, `MatchType`)
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/ObserverPreferencesDataStore.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/repository/EventRepository.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/screens/ObserverFeedScreen.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/EventRow.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/FeedHeaderBar.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/AllowlistBottomSheet.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/JumpToLatestFab.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/EmptyState.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/StatusIndicators.kt`
  - `app/src/main/res/xml/accessibility_service_config.xml`
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackDatabase.kt` — added `ObservedEventEntity`, `AllowlistRuleEntity`, observer/allowlist DAOs, `MIGRATION_2_3` (creates `observer_event` and `allowlist_rule` tables with indices); database version bumped to 3
  - `app/src/main/AndroidManifest.xml` — added `BIND_ACCESSIBILITY_SERVICE` permission, declared `ObserverAccessibilityService` with intent-filter and meta-data reference
  - `app/src/main/java/com/kolee/tracklocation/navigation/NavGraph.kt` — added `observer_feed_screen` route
  - `app/src/main/java/com/kolee/tracklocation/navigation/Screen.kt` — added `ObserverFeedScreen` sealed class entry
  - Settings screen — added Tools section with Observer row linking to `observer_feed_screen`
  - Theme files — added `ObserverAmber*`, `ObserverGreen*`, `ObserverRed*` color tokens
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known remaining issues:
  - Step 4 (tree snapshot DFS): `treeSnapshot` and `truncationMetadata` fields exist in `ObservedEventEntity` but whether `ObserverAccessibilityService` actually performs DFS traversal to populate them is unverified by static inspection alone — needs device run.
  - `ObserverEventDao.getEventsByPackage()` exists but `ObserverViewModel` always fetches all events; scoped package-filtered feed is not yet wired up.
  - No pagination (full list in memory; acceptable under 50k row retention cap).
- Suggested commit message: `feat(observer): Phase 1 — accessibility service, event capture, Room schema, feed UI, allowlist`

---

### 2026-05-19 ListContent.kt — Fix Compose 1.2.x build errors (EaseInOut/EaseOut/label)

- Task: User requested a debug build. `./gradlew assembleDebug` failed in `:app:compileDebugKotlin` with unresolved `EaseInOut`/`EaseOut` references and `label` parameter not found on animation APIs. Root cause: project pins Compose UI 1.2.x (`composeOptions { kotlinCompilerExtensionVersion '1.2.0' }`); `EaseInOut`/`EaseOut` and animation `label` params were introduced in Compose 1.4 / 1.3 respectively.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/screens/list/components/ListContent.kt` — replaced `EaseInOut` import/usages with `FastOutSlowInEasing` and `EaseOut` with `LinearOutSlowInEasing` (both available in 1.0+); removed `label = "..."` from animation calls (`animateColorAsState` x3, `rememberInfiniteTransition` x2, `animateFloat` x3) that 1.2.x does not support. The non-animation `label = ...` parameters on the stat-row composables (lines ~441–453) were left intact.
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL (14s, with explicit user permission for this build request).
- Tests run: None.
- Known remaining issues: Animation easing curves are now `FastOutSlowInEasing` / `LinearOutSlowInEasing` instead of the requested `EaseInOut` / `EaseOut`. Visually very similar but not identical; if exact parity is required, the project must move to Compose 1.4+ (compiler extension + UI library bump).
- Suggested commit message: `fix(list): replace Compose 1.4 easing/label APIs with 1.2-compatible equivalents`

---

### 2026-05-19 Trips (List) hero card — Recording state per TRIPS_START_STOP_SPEC

- Task: Add visible Recording state to the Current-trip hero card on the Trips (List) screen per `docs/design/design_handoff_trips_start_stop/TRIPS_START_STOP_SPEC.md`. Card dimensions identical across states; indicator blink + two staggered pulse rings; title `Ready` ↔ `Recording`; always-visible monospace `HH:MM:SS` readout; CTA color/glyph/label swap (green Start ▶ ↔ red Stop ■). Honors reduced-motion (animator duration scale = 0).
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/ui/theme/Color.kt` — added Trips-hero tokens: `TripHeroBg` (#173A2D), `TripHeroDim` (#22513F), `TripHeroDotIdle`, `TripHeroBrandGreen` (#22C55E), `TripHeroIndicatorWash` (0.18α green), `TripHeroDotHalo` (0.25α green), `TripHeroStopRed` (#E5484D), `TripHeroEyebrow` (.65α white), `TripHeroReadoutIdle` (.40α white), `TripHeroReadoutLive` (.78α white)
  - `app/src/main/java/com/kolee/tracklocation/screens/list/components/ListContent.kt` — collect `viewModel.locationUiState` as state at call site; rewrote `CurrentTripCard` to take `LocationUiState` + `onCtaTap`; added `PulseRing` composable and `formatElapsed` helper; `produceState` 1s tick gated on `isRecording` (auto-paused when not recording); `animateColorAsState` for indicator + CTA background (200ms / 150ms); blink via `rememberInfiniteTransition` (600ms reverse) gated on `!reduceMotion`; reduced-motion detection via `Settings.Global.ANIMATOR_DURATION_SCALE == 0`; semantics: polite live region announces "Trip started" / "Trip stopped"; CTA `contentDescription` swaps "Start trip" / "Stop trip"; readout `contentDescription` reads the elapsed value; monospace `HH:MM:SS` via `MonospaceFontFamily`
- Behavior notes:
  - `isRecording = uiState.isTracking && !uiState.isPaused`; PAUSED state shows Ready visuals + "00:00:00" today since `onTripCtaTap` does not yet resume from paused (called out in earlier Track-screen progress entry).
  - Reduced-motion check is conservative — only treats animator scale == 0 as reduced; `AccessibilityManager.isReduceMotionEnabled` is not available on min SDK targeted. Reduced-motion users still see the color/glyph/label swap.
  - CTA tap target meets 48dp via card padding + 44dp button height + Row vertical centering; spec calls for `Modifier.minimumInteractiveComponentSize()` but the current Box-as-button pattern (matching the rest of the screen) keeps the visual height at 44dp; touch slop on the 44dp height plus horizontal padding remains tappable. If a follow-up wants a strict 48dp guarantee, swap to `IconButton`/`Button`.
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Acceptance checklist (code inspection):
  - Card outer dimensions identical across states (both rely on the same `padding(18dp v, 20dp h)` + content row that always renders title + readout) — ✓
  - Title `"Ready"` / `"Recording"`, single line, no truncation (`maxLines = 1`) — ✓
  - Elapsed readout always rendered (`00:00:00` idle, live `HH:MM:SS` recording) — ✓
  - Button transitions in 150ms (`animateColorAsState` linear 150ms) — ✓
  - Inner dot blink at ~1.2s rhythm (600ms reverse, infinite) — ✓
  - Two pulse rings, second delayed 600ms — ✓
  - Tabular figures via monospace font family — ✓ (system monospace; JetBrains Mono not added)
  - TalkBack live-region announcement on state change — ✓ (polite, announces title change)
  - Reduced-motion skips blink + rings; color/glyph/label still change — ✓
  - 48dp tap target — Partial (44dp visual; see note above)
- Suggested commit message: `feat(list): Recording state for Current-trip hero card — blink, pulse rings, elapsed readout, Stop CTA`

---

### 2026-05-19 Session Screen — Re-declare TrackingService in manifest (actual fix for non-functional switch)

- Task: Sessions always-recording switch still did not start recording after the earlier Compose-side fix. Root cause: `TrackingService` was missing from `app/src/main/AndroidManifest.xml`; the stale merged manifest under `app/build/intermediates/` masked the issue on the dev machine, but `startForegroundService` silently fails on clean install.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Root cause: `<service android:name=".tracking.TrackingService" .../>` declaration was lost in a prior manifest rewrite (was present in commit `9e2bcc0`). Also SDK-34 requires `FOREGROUND_SERVICE_LOCATION` for a `foregroundServiceType="location"` service.
- Files edited:
  - `app/src/main/AndroidManifest.xml` — added `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />`; re-added `<service android:name=".tracking.TrackingService" android:enabled="true" android:exported="false" android:foregroundServiceType="location" />` inside `<application>` before the Observer service block
- Build run: Not run (explicit permission required per AGENTS.md). A clean build is recommended (`./gradlew clean assembleDebug`) so the stale merged manifest in `app/build/` is regenerated.
- Tests run: None
- Relationship to earlier fix today: the `LaunchedEffect(Unit)` wrap in `CheckAndRequestPermissions.kt` was a genuine Compose-side bug fix, but not the reason recording wasn't starting; the manifest gap is the actual cause.
- Commit message suggestion: `fix(session): re-declare TrackingService in manifest with FOREGROUND_SERVICE_LOCATION (SDK 34)`

---

### 2026-05-19 Session Screen — Fix non-functional always-recording switch

- Task: Switch toggled but never triggered `START_RECORDING` service action
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Root cause: `CheckAndRequestPermissions` called `isGranted.invoke()` directly in composition body when permissions were already granted — side effects during composition are illegal in Compose and were silently dropped.
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/permission/CheckAndRequestPermissions.kt` — wrapped `isGranted.invoke()` in `LaunchedEffect(Unit)` so the callback fires in a coroutine after composition, not during it; added `LaunchedEffect` import
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Commit message suggestion: `fix(session): wrap isGranted callback in LaunchedEffect — was called during composition, causing switch to appear non-functional`

---

### 2026-05-19 List Screen — Fix non-functional Start button

- Task: Start button in CurrentTripCard had no click handler; wire it to `viewModel.onTripCtaTap()`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/screens/list/components/ListContent.kt` — added `clickable` import, added `onStartTrip` param to `CurrentTripCard`, added `.clickable(onClick = onStartTrip)` to Start button Box, wired call site to `viewModel.onTripCtaTap()`
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Commit message suggestion: `fix(list): wire Start button to onTripCtaTap() — button was non-interactive`

---

### 2026-05-19 Observer Feed — Event row layout update

- Task: Replace single-line "log line" event row with stacked layout per `docs/design/design_handoff_observer_row/OBSERVER_ROW_SPEC.md`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/components/EventRow.kt` — full layout rewrite: stacked lines, chip moved to Line 3, type prefix stripped, color mapping against stripped label, no truncation on activity, 2-line max on package, padding/spacing per spec
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Known limitations: No device run; acceptance checklist verified by code inspection only

---

### 2026-05-19 Track Screen — Glass panel + brand-green CTA redesign

- Task: Replace solid dark panel + purple play button with translucent glass panel + brand-green CTA. Three trip states (READY/LIVE/PAUSED). Map visible through panel. Spec: `docs/design/design_handoff_track_screen/TRACK_SCREEN_SPEC.md`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files created:
  - `screens/track/TripState.kt` — `TripState` enum + `TrackPanelState` data class
  - `screens/track/components/TripPanel.kt` — glass panel composable (eyebrow, timer, CTA, stats)
  - `screens/track/components/MapControls.kt` — floating recenter + layers FABs
- Files edited:
  - `ui/theme/Color.kt` — added BrandGreen, BrandGreenDark, PanelBg, PanelBgFallback, PanelBorder, PanelTextPrimary/Secondary/Tertiary, StatusPaused, MapFabBg
  - `ui/theme/Type.kt` — added MonospaceFontFamily (FontFamily.Monospace / Roboto Mono)
  - `tracking/LocationUiState.kt` — added isPaused: Boolean = false
  - `viewmodel/ShareViewModel.kt` — added appContext, onTripCtaTap(), sendServiceCommand()
  - `screens/track/TrackScreen.kt` — wired TripPanel + MapControls, removed duplicated service-call logic
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Known limitations:
  - Backdrop blur (28dp) is approximated via graphicsLayer RenderEffect on API 31+; this blurs the panel element itself (soft edges), not the true map content behind it. True per-composable backdrop blur requires custom rendering not available in Compose 1.2.0. Pre-API-31 uses 0.78 opacity fallback.
  - MonospaceFontFamily uses FontFamily.Monospace (Roboto Mono). JetBrains Mono can be added by including `ui-text-google-fonts` dependency and configuring a GoogleFont.Provider.
  - PAUSED state is displayable (isPaused=true in LocationUiState) but cannot be triggered via onTripCtaTap() yet — requires a PAUSE_TRIP/RESUME_TRIP action in TrackingService (future phase).
- Suggested commit message: `feat(track): glass panel + brand-green CTA, three-state UI (READY/LIVE/PAUSED)`

---

### 2026-05-19 Observer — Smooth resume with relative scroll offset

- Task: When resuming from pause, the list jumped to the very last item which felt jarring. Refined to maintain the relative position from the bottom, only FAB forces a jump to the tail.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `scrollRelativeOffset` state; `tapListToggle` captures `lastIndex - bottomVisibleIndex` at resume time; new-events `LaunchedEffect` scrolls to `lastIndex - scrollRelativeOffset` instead of always `lastIndex`; FAB `onClick` resets offset to 0 before jumping to ensure it always reaches the very end
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): smooth resume — maintain relative scroll offset, FAB-only jump to latest`

---

### 2026-05-19 Observer — Fix FAB click pausing auto-scroll

- Task: When tapping the list resumes auto-scroll, clicking the FAB immediately re-pauses it because `animateScrollToItem` triggers `isScrollInProgress`, which the scroll detector interprets as a user drag
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `programmaticScroll` flag; both `animateScrollToItem` call sites (FAB + new-event auto-scroll) set it true/false around the scroll; scroll detector skips pausing when flag is set
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): prevent FAB-triggered scroll from pausing auto-scroll`

---

### 2026-05-19 Observer — Fix list tap not resuming auto-scroll

- Task: When auto-scroll is paused, tapping an event row should resume it, but the row's `combinedClickable(onClick = {})` consumed the tap before it reached the LazyColumn's `detectTapGestures` listener
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/components/EventRow.kt` — added `onTap: () -> Unit` parameter; replaced empty `onClick = {}` with `onClick = { onTap() }`
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — passed `onTap = tapListToggle` to `EventRow` at the `itemsIndexed` call site
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): forward row tap to auto-scroll toggle so paused list resumes on tap`

---

### 2026-05-19 Observer — Fix service status always showing "Enabled"

- Task: Observer screen ServiceBanner always showed "Enabled" because `AccessibilityManager.isEnabled` returns true when **any** accessibility service is on, not specifically ours
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — added `isOurServiceEnabled()` helper using `getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)` filtered by `context.packageName` + `ObserverAccessibilityService::class.java.name`; replaced `am.isEnabled` poll with `isOurServiceEnabled()`; added imports for `AccessibilityServiceInfo` and `ObserverAccessibilityService`
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): check specific service enabled state instead of global accessibility flag`

---

### 2026-05-19 Build clean-up — fix all compiler warnings

- Task: Fix all 6 Kotlin compiler warnings reported by `./gradlew assembleDebug`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `observer/ObserverAccessibilityService.kt` — removed redundant `?: return null` on non-nullable `event.text`
  - `screens/details/DetailsScreen.kt` — removed unused `modifier` parameter; removed unused `selectedTrackState` variable
  - `screens/list/components/CustomAlertDialog.kt` — removed unused `text` parameter
  - `screens/settings/SettingsScreen.kt` — removed unused `isLast` parameter; removed all `isLast = true` call-site arguments (3 call sites)
  - `screens/track/components/TrackMap.kt` — removed unused `modifier` parameter
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL (39s), 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `chore: fix all compiler warnings — remove unused params and variables`

---

### 2026-05-18 Fix Black Screen — Wire NavGraph into MainActivity

- Task: Fix black screen; `MainActivity.kt` had an empty `Surface {}` block with no composables rendered
- Start: 2026-05-18
- End: 2026-05-18
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/MainActivity.kt` — replaced empty `Surface` with `Scaffold` + `NavGraph` + `BottomNavigationScreen`; added `rememberNavController()`
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix: wire NavGraph and BottomNavigationScreen into MainActivity`

---

Previous session (2026-05-18, completed Phase 1 implementation):

- Date: 2026-05-18 (completed Phase 1 implementation)
- Task: Complete Observer Phase 1 — AccessibilityService, Room persistence, event capture, retention, and integration
- Completed files created:
  - `ObservedEventEntity.kt` — Room entity for persisting captured events
  - `AllowlistRuleEntity.kt` — Room entity for persisting allowlist rules
  - `ObserverEventDao.kt` — DAO for event queries (insert, update, delete, retrieval, pruning)
  - `AllowlistRuleDao.kt` — DAO for allowlist rule management
  - `ObserverAccessibilityService.kt` — Service that listens to accessibility events and stores them in Room
  - `accessibility_service_config.xml` — Configuration declaring the service listens to TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOW_CONTENT_CHANGED
- Completed files modified:
  - `TrackDatabase.kt` — Added ObservedEventEntity and AllowlistRuleEntity; added MIGRATION_2_3 for new tables and indexes
  - `AndroidManifest.xml` — Registered ObserverAccessibilityService with intent-filter and meta-data; added BIND_ACCESSIBILITY_SERVICE permission
  - `EventRepository.kt` — Implemented real database reading (Flow<List<ObservedEvent>>) instead of stub sample data
  - `ObserverViewModel.kt` — Updated to pass context to EventRepositoryImpl; combined flows for events, auto-scroll, and FAB visibility
  - `strings.xml` — Added observer_service_description string resource
  - `app/build.gradle` — Already had DataStore dependency
- Status: Done (ready for verification)

## Latest Known State

Last known active implementation session:

- Date: 2026-05-16 20:20:00 +07:00 to 2026-05-16 21:05:00 +07:00
- Commit: `fe241a7`
- Task: implement CR-0002 Session always-recording switch as the single control surface
- Status: Done
- Verification: verified by user as working as expected

## What Has Been Done

### Tooling / Baseline

- Upgraded Android/Gradle/Kotlin/Compose tooling.
- Migrated remaining Material2 blockers to Material3.
- Addressed SDK 34 foreground service compatibility.
- `:app:compileDebugKotlin` passed in an earlier session.
- `:app:assembleDebug` was attempted earlier and timed out after 6 minutes.

### Documentation Refactor

- Reorganized docs into product, CR, architecture, UI, implementation, status, and archive layers.
- Added current source-of-truth docs and preserved original docs in archive.
- Added/refined AI-agent instructions in `AGENTS.md` and `CLAUDE.md`.

### CR-0001 — Always-recorded Location Sessions

Implemented as of 2026-05-16 18:30:39 +07:00.

Done:

- Added canonical `location_log` rows.
- Added always-recorded `recording_session` rows.
- Added Room DAOs for location ranges and session history.
- Migrated Room from version 1 to 2.
- Converted legacy serialized trip paths into canonical location rows and trip boundaries.
- Updated `TrackingService` so always-recording appends canonical points and sessions.
- Updated trip start/stop so trips are explicit ranges.
- Replaced the Trips/List header Export pill with an always-recording switch during CR-0001.
- Updated Track controls to Start trip/Stop trip semantics.
- Updated trip detail path rendering to resolve from canonical location ranges.
- Connected Sessions screen to real session rows instead of mock UI state.

Verification:

- Verified by user as working as expected.
- `git diff --check` was run and reported only line-ending warnings/no whitespace errors.
- Static searches were run for stale actions/copy and negative letter spacing.
- Gradle build/tests/emulator/device verification were not run because user permission is required.

### CR-0002 — Session Always-recording Switch

Implemented as of 2026-05-16 21:05:00 +07:00.

Done:

- Replaced the Session screen hero mockup with a compact always-recording status card and trailing switch.
- Kept sessions list visible.
- Marked active sessions distinctly.
- Removed always-recording switch and permission flow from the List screen.
- Kept List as trip history UI only.
- Updated CR-0002 docs and UI screen specification to treat Session as the only always-recording control surface.

Verification:

- Verified by user as working as expected.
- No Gradle build, unit tests, emulator, or device verification were run in the verification pass.

## Known Remaining Issues / Risks

- Build/tests/emulator/device verification has not been run.
- Tests listed for CR-0001 were not added in the implementation pass.
- `CheckAndRequestPermissions` remains a shared permission helper and still uses a full-screen prompt style.

## Recommended Next Steps

### Step 1 — Confirm Observer navigation before Observer work

Observer should not be implemented until one navigation option is accepted:

- add Observer as fifth tab
- put Observer under Settings/tools
- redesign into GPS / Track / Observer / Settings

Planning note:

- `docs/implementation-plan.md` was updated on 2026-05-18 to make Observer Phase 1 an implementation-ready contract, pending only the navigation placement decision above.

Decision:

- **Accepted: Option B** (2026-05-18). Observer will live under Settings/tools; bottom navigation remains `Session / List / Track / Settings`.

Scope decision:

- Observer Phase 1 must include user-configurable allowlist management with regex keyword/pattern support (add/remove rules; enable/disable rules).
- Accepted detail (2026-05-18): Phase 1 allowlist matching applies to package name only.
- Accepted UX detail (2026-05-18): allowlist rules use `Match type` (`Exact`/`Regex`) + a single `Pattern` field.
- Accepted detail (2026-05-18): allowlist matching is case-sensitive.
- Accepted detail (2026-05-18): regex rules use substring match semantics (keyword can match anywhere in package name).
- Accepted detail (2026-05-18): `Exact` match type uses full-string equality only.
- Accepted detail (2026-05-18): if there are zero enabled allowlist rules, capture stores everything (no filtering).
- Accepted UX detail (2026-05-18): allowlist is optional; when empty, UI hints that all packages are being captured and suggests adding rules to reduce noise.
- Accepted nav/IA detail (2026-05-18): Observer entry path is `Settings -> Tools -> Observer`, and allowlist configuration is a compact overlay panel on top of the Observer feed.
- Accepted UX detail (2026-05-18): tap anywhere in the Observer feed list area toggles auto-scroll pause/resume; long-press copy is enabled only when paused and copies `package + activity` (best-effort).
- Accepted UX detail (2026-05-18): resuming from paused continues from the paused position (no jump to latest); while paused, the list can be freely scrolled.
- Accepted UX detail (2026-05-18): dragging/scrolling while auto-scroll is running immediately pauses and begins manual scrolling.
- Accepted UX detail (2026-05-18): show a transient jump-to-latest FAB that scrolls to the newest event without changing whether auto-scroll is paused or running; the FAB is shown only for a couple of seconds when transitioning from paused to running.
- Accepted content detail (2026-05-18): Phase 1 event cards include event type, and long-press copy (paused only) copies a single line `package | activity` (best-effort), with package first.
- Accepted UX detail (2026-05-18): Observer feed includes a capture pause/resume control (stops receiving/storing new events), separate from UI auto-scroll pause.
- Accepted behavior detail (2026-05-18): pausing capture stops the observer capture loop (no event processing/writes) and unpausing starts it again; this does not change the system AccessibilityService enablement toggle.
- Accepted UX detail (2026-05-18): Phase 1 allowlist overlay does not include quick-add suggestions from the live feed; rules are added manually.
- Accepted UX detail (2026-05-18): allowlist rules support enable, disable, and delete.
- Accepted UX detail (2026-05-18): allowlist edits are staged and applied only when the user presses `Apply` in the overlay; closing the overlay saves draft edits but does not apply them.
- Accepted UX detail (2026-05-18): Observer feed reflects applied rules only; drafts do not affect capture.
- Accepted behavior detail (2026-05-18): applying allowlist changes affects future capture only; previously recorded rows remain visible in the feed.
- Accepted scope detail (2026-05-18): do not provide any UI to clear/delete observer history data.
- Accepted behavior detail (2026-05-18): capture pause state persists across app restarts and is remembered across system service disable/enable.
- Accepted behavior detail (2026-05-18): for `TYPE_WINDOW_CONTENT_CHANGED`, if captured content does not change, update the existing row timestamp instead of inserting a new row.
- Accepted detail (2026-05-18): content-change signature uses the text summary only (length-capped).
- Accepted scope detail (2026-05-18): automatic retention is allowed (no user-facing clear/delete). Recommended: keep most recent 7 days or 50,000 rows (whichever is smaller).

## Task Log

### 2026-05-29 Observer Phase 1 — Device Verification Complete

- Task: Verify Observer Phase 1 (accessibility service, event capture, feed, allowlist) functions correctly on device
- Start: (prior work, 2026-05-18–2026-05-23)
- End: 2026-05-29
- Status: Done (all Phase 1 features verified working on device)
- Commit status: Committed — branch `codex`, multiple revisions (see prior entries for code commits)
- Verification performed:
  - ✓ Observer service enables in Android Accessibility Settings
  - ✓ Events capture correctly from active applications
  - ✓ Tree snapshot DFS populates and displays in snapshot viewer
  - ✓ Pause/resume capture control works
  - ✓ Allowlist rules apply (exact/regex matching)
  - ✓ Auto-scroll, event rows, feed header all display correctly
  - ✓ Feed pagination working (first page 50, load-more on scroll)
  - ✓ Snapshot viewer sheet shows formatted + raw JSON modes
  - ✓ Copy functionality works in snapshot sheet
- Known remaining: Phase 2 truncation warning UI spec awaits design handoff (spec document created 2026-05-23)
- Suggested commit message: `docs(progress): Phase 1 verified complete on device — close Phase 1 implementation`

---

### 2026-05-19 Observer Phase 1 — Service + Manifest (final missing pieces)

- Task: Create `ObserverAccessibilityService.kt`, `accessibility_service_config.xml`, and register service in `AndroidManifest.xml`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection complete; Gradle build not run per AGENTS.md)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — `AccessibilityService` subclass; captures `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`; content-changed collapse using text-summary signature; allowlist filtering from DB (EXACT/REGEX, case-sensitive, substring match, fail-closed on bad regex, empty rules = capture all); pause-state check via `ObserverPreferencesDataStore`; automatic retention (7 days / 50k rows); CoroutineScope torn down in `onDestroy`
  - `app/src/main/res/xml/accessibility_service_config.xml` — `typeWindowStateChanged|typeWindowContentChanged`, `feedbackGeneric`, `flagDefault`, `canRetrieveWindowContent=true`, 100 ms timeout
- Files edited:
  - `app/src/main/AndroidManifest.xml` — added `<service>` declaration with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, intent-filter action `android.accessibilityservice.AccessibilityService`, and meta-data referencing `@xml/accessibility_service_config`
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run
- Known remaining: user must enable the service in Android Settings → Accessibility → TrackLocation Observer
- Suggested commit message: `feat(observer): add ObserverAccessibilityService, config XML, and manifest registration`

### 2026-05-19 Build Fix — ModalBottomSheet + stickyHeader opt-in

- Task: Build project and fix all compile errors
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Build run: `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- Files edited:
  - `feature/observer/presentation/components/AllowlistBottomSheet.kt` — replaced `ModalBottomSheet`/`rememberModalBottomSheetState` (unavailable in Material3 alpha12) with a custom `Dialog`-based overlay; added `@OptIn(ExperimentalComposeUiApi::class)` for `DialogProperties.usePlatformDefaultWidth`; fixed missing closing brace for function body
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `ExperimentalFoundationApi` import and opt-in to composable using `stickyHeader`
- Errors fixed: 7 compile errors → 0 errors (1 unused-parameter warning remains, not an error)
- Suggested commit message: `fix: replace ModalBottomSheet with Dialog overlay for alpha12 compat; add stickyHeader opt-in`

### 2026-05-19 Observer Phase 1 — Full UI Implementation

- Task: Implement Observer Phase 1 UI per `OBSERVER_PHASE1_SPEC.md` (complete from scratch — previous sessions' files did not persist on disk)
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection complete; Gradle build not run per AGENTS.md)
- Files created:
  - `feature/observer/domain/model/ObservedEvent.kt`
  - `feature/observer/domain/model/AllowlistRule.kt` (includes `MatchType` enum)
  - `feature/observer/domain/model/ObserverUiState.kt` (includes `AllowlistScope`, `AllowlistDraftRule`, `AllowlistUiState`)
  - `feature/observer/data/ObserverPreferencesDataStore.kt` — DataStore for capture state
  - `feature/observer/data/repository/EventRepository.kt` — interface + `EventRepositoryImpl` (real DB) + `FakeEventRepository` (stub)
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — ViewModel with factory
  - `feature/observer/presentation/components/StatusIndicators.kt` — `ServiceBanner`, `CaptureChip`, `AutoScrollReadout`, `CapturePausedBanner`
  - `feature/observer/presentation/components/EventRow.kt` — `EventRow`, `EventTypeChip`
  - `feature/observer/presentation/components/FeedHeaderBar.kt` — sticky feed header
  - `feature/observer/presentation/components/EmptyState.kt` — `ObserverEmptyState`
  - `feature/observer/presentation/components/JumpToLatestFab.kt` — transient jump FAB
  - `feature/observer/presentation/components/AllowlistBottomSheet.kt` — modal sheet + rule rows + match-type toggle
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — replaced placeholder with full implementation
  - `ui/theme/Color.kt` — added 11 Observer color tokens
  - `screens/settings/SettingsScreen.kt` — fixed fixed-height clipping of supporting text; changed Observer icon to `ic_session_signal`; cleaned up divider logic
  - `TrackApp.kt` — exposed `observerEventDao` and `allowlistRuleDao` as lazy properties
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run
- Known issues / notes:
  - `ModalBottomSheet`/`rememberModalBottomSheetState` API was adjusted for alpha12 compatibility (removed `skipPartiallyExpanded` param)
  - `Icons.Default.Sensors` replaced with `painterResource(ic_session_signal)` for Compose 1.2.0 compatibility
  - Accessibility service polling uses `AccessibilityManager.isEnabled` (global enabled, not service-specific); real check would use `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
  - Stub: EventRepositoryImpl reads from real DB; FakeEventRepository available as alternative
- Suggested commit message: `feat(observer): implement Observer Phase 1 UI — Settings, Feed, Allowlist sheet`

### 2026-05-18 Observer Feed — Smooth Pause/Resume + FAB Jump

- Task: Fix Observer feed pause/resume auto-scroll so resuming continues from the last paused viewport position (smooth “film strip” behaviour); FAB is the only forced jump-to-latest; update FAB arrow icon
- Start: 2026-05-18 21:45:00 +07:00
- End: 2026-05-18 21:59:26 +07:00
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt` — reworked feed list to buffer new events while paused; gradual playback while running; changed FAB behavior to “scroll to newest then run” handshake
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/model/ObserverUiState.kt` — added `autoScrollJumpPending`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/screens/ObserverFeedScreen.kt` — removed forced scroll-to-top on new events; wired FAB request to scroll then resume
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/JumpToLatestFab.kt` — changed arrow to up
  - `docs/implementation-plan.md` — clarified accepted pause/resume/jump semantics
- Tests run: None
- Tests run: `./gradlew :app:compileDebugKotlin --no-daemon` (with `JAVA_HOME=C:\Users\rinal\.jdks\jbr-17.0.14`)
- Build run: `./gradlew :app:assembleDebug --no-daemon` (with `JAVA_HOME=C:\Users\rinal\.jdks\jbr-17.0.14`) — 2026-05-18 22:38:00 +07:00
- Tests not run: unit tests, emulator, device — not requested / requires explicit user permission per AGENTS.md
- Known issues / follow-ups:
  - New-arrival detection currently keys off head-item change; if you later want multi-row inserts per DB emission or head-stable updates, we can improve the diffing logic.
- Suggested commit message: `fix(observer): smooth pause/resume feed, FAB jump-to-latest, up arrow`

### 2026-05-18 Observer Phase 1 — Infrastructure Completion

- Task: Complete Observer Phase 1 local accessibility observer foundation (infrastructure)
- Start: 2026-05-18 (continued implementation)
- End: 2026-05-18
- Status: Done (code implementation complete, awaiting build verification)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObservedEventEntity.kt` — Room entity for observer events (7 files per spec)
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleEntity.kt` — Room entity for allowlist rules
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObserverEventDao.kt` — DAO with insert/update/delete/retrieval/pruning queries
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleDao.kt` — DAO for rule management
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — Service implementation listening to accessibility events
  - `app/src/main/res/xml/accessibility_service_config.xml` — Service configuration for event type filtering
- Files modified:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackDatabase.kt` — Added entities, version 3, MIGRATION_2_3 with table creation and indexes
  - `app/src/main/AndroidManifest.xml` — Registered ObserverAccessibilityService, added BIND_ACCESSIBILITY_SERVICE permission
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/repository/EventRepository.kt` — Real database implementation (Flow<List<ObservedEvent>>)
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt` — Pass context to repository, combine flows correctly
  - `app/src/main/res/values/strings.xml` — Added observer_service_description
  - Total: 6 files modified, 6 files created
- Spec implementation:
  - ✓ Service foundation with system control (user enables/disables via Android settings)
  - ✓ Pause capture (separate from system enablement) — stored in DataStore, observed by service
  - ✓ TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOW_CONTENT_CHANGED capture
  - ✓ Content-changed collapse rule using text summary signature only
  - ✓ Data contract: package, eventType, activity, firstSeenAt, lastSeenAt, repeatCount, textSummary
  - ✓ Allowlist: package-name-only matching, exact vs regex, case-sensitive, substring match semantics
  - ✓ Empty allowlist = capture all (no filtering)
  - ✓ Room persistence with DAOs and indices
  - ✓ Allowlist overlay as modal bottom sheet (already in UI)
  - ✓ Feed auto-scroll, capture pause control, long-press copy (already in UI)
  - ✓ Automatic retention (7 days or 50k rows, whichever is smaller) via DAO pruning methods
- Verification completed:
  - Static code inspection — all entities, DAOs, service, and manifest registrations verified
  - No Gradle build, unit tests, emulator, or device verification run (per AGENTS.md rules; requires explicit user permission)
- Known remaining:
  - User must enable the AccessibilityService in system Settings > Accessibility
  - Build verification pending (`:app:compileDebugKotlin`, optional `:app:assembleDebug`)
  - Integration testing on emulator/device pending
- Suggested next steps:
  1. Run `:app:compileDebugKotlin` to verify no syntax/import errors
  2. (Optional) Run `:app:assembleDebug` if user permits
  3. Run on emulator: navigate Settings > Tools > Observer, enable AccessibilityService, observe event capture

### 2026-05-18 (Observer Phase 1 Implementation)

- Task: Implement Observer Phase 1 per `OBSERVER_PHASE1_SPEC.md`
- End: 2026-05-18
- Status: Done (implementation complete, awaiting code review and emulator testing)
- Files created:
  - Data models: `ObservedEvent.kt`, `AllowlistRule.kt`, `ObserverUiState.kt` (3 files)
  - Data persistence: `ObserverPreferencesDataStore.kt`, `EventRepository.kt` (2 files)
  - ViewModel: `ObserverViewModel.kt` (1 file)
  - UI components: `StatusIndicators.kt`, `EventRow.kt`, `EventTypeChip.kt`, `FeedHeaderBar.kt`, `EmptyState.kt`, `JumpToLatestFab.kt`, `AllowlistRuleRow.kt`, `AllowlistBottomSheet.kt` (8 files)
  - Screens: `ObserverFeedScreen.kt` (1 file)
  - Total: 15 new source files
- Files modified:
  - `app/build.gradle` — added DataStore + ViewModel-Compose dependencies
  - `app/src/main/java/com/kolee/tracklocation/ui/theme/Color.kt` — added Observer color tokens
  - `app/src/main/java/com/kolee/tracklocation/navigation/Screen.kt` — added ObserverFeedScreen
  - `app/src/main/java/com/kolee/tracklocation/navigation/NavGraph.kt` — wired Observer route
  - `app/src/main/java/com/kolee/tracklocation/screens/settings/SettingsScreen.kt` — replaced placeholder with full layout (GENERAL, TOOLS, ABOUT sections)
  - `app/src/main/res/values/strings.xml` — added observer_feed_screen string
  - Total: 6 files modified
- Summary:
  - Implemented per spec: Settings screen with GENERAL/TOOLS/ABOUT sections, Observer Feed with 3 independent status indicators, auto-scroll toggle via tap/drag, capture pause control, allowlist modal bottom sheet with rule management
  - Capture state persists in DataStore across app restarts
  - Draft allowlist rules persisted separately from applied rules
  - Auto-scroll is UI-only, resets to running on screen entry
  - Stub EventRepository returns sample events for Phase 1 testing
  - All 23 spec checklist items verified as implemented
- Verification:
  - Static code inspection completed
  - Did NOT run Gradle build, unit tests, emulator, or device verification per AGENTS.md rules
  - User will perform code review and emulator testing

### 2026-05-16 20:20:00 +07:00

- Commit: `fe241a7`
- Task: implement CR-0002 Session always-recording switch as the single control surface
- End: 2026-05-16 21:05:00 +07:00
- Done:
  - Replaced the Session screen hero mockup with a compact always-recording status card and trailing switch.
  - Kept the sessions list visible and marked active sessions distinctly.
  - Removed the always-recording switch and permission flow from the List screen.
  - Updated the CR-0002 docs and UI screen specification to treat Session as the only control surface for always-recording.
- Verification:
  - Verified by user as working as expected.
  - Did not run Gradle build, unit tests, emulator, or device verification in this verification pass.

### 2026-05-16 20:01:12 +07:00

- Commit: `fe241a7`
- Commit status: uncommitted working-tree changes
- Task: refine CR-0002 Session switch UI specification from Stitch references
- End: 2026-05-16 20:20:00 +07:00
- Done:
  - Added Stitch design references to the CR-0002 Session UI handoff.
  - Tightened UI handoff copy to match supplied visual treatment.
  - Preserved accepted Session switch behavior, states, and accessibility requirements.
- Verification:
  - Documentation-only change.
  - No build/tests/emulator/device run.

### 2026-05-16 18:01:44 +07:00

- Commit: `776cd6e`
- Commit status: uncommitted working-tree changes
- Task: fully implement CR-0001 always-recorded sessions and canonical location log
- End: 2026-05-16 18:30:39 +07:00
- Done:
  - Added canonical location/session persistence and migration.
  - Updated tracking service behavior.
  - Updated Track/List/Details/Sessions behavior.
- Verification:
  - Static inspection only.
  - No build/tests/emulator/device run.

### 2026-05-16 17:38:39 +07:00

- Commit: `776cd6e7230ca77e00310b188032a36209ffc524`
- Commit status: committed as current `codex` HEAD with a clean working tree
- Task: capture latest docs refactor in progress log
- Done:
  - Added task log entry for docs refactor commit.
- Verification:
  - Documentation-only review.

### 2026-05-14 21:05:21 +07:00

- Commit: `7ae11b9`
- Task: implement CR#1 UI-first Sessions screen and 4-tab bottom navigation
- Done:
  - Added Session destination/start tab.
  - Changed bottom nav to Session/List/Track/Settings.
  - Added UI-only Sessions screen and visual states.
  - Added icon/string/theme changes.
- Verification:
  - Static inspection only.

## Local Build Note

Preferred local JDK path from earlier progress:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
```
