# TrackLocation Implementation Plan

This file is the active implementation contract for code assistants and design tools.

It uses a two-track model for every CR/phase:

```text
A. Code Implementation Work
   -> Codex / Claude Code / Cursor / Windsurf

B. UI Specification / Design Handoff Work
   -> Claude Design / Google Stitch / Figma
```

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

1. **`settings.gradle`** — What: add JitPack repository. How: add `maven { url 'https://jitpack.io' }` inside `dependencyResolutionManagement.repositories`
2. **`app/build.gradle`** — What: add OBD library. How: add `com.github.eltonvs:kotlin-obd-api:<version>` (verify Kotlin 1.7.0 compat first; if incompatible, skip library and implement raw AT commands over socket)
3. **`AndroidManifest.xml`** — What: declare BT permissions and OBD service. How: add `BLUETOOTH` + `BLUETOOTH_ADMIN` (maxSdk 30), `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN neverForLocation` (API 31+); add `uses-feature bluetooth required=false`; declare `ObdPollingService` with `foregroundServiceType="connectedDevice"`
4. **Create** `data/roomdb/ObdSampleEntity.kt` — What: define database table for OBD samples. Schema: id, timestampMs, rpm, obdSpeedKmh, fuelRateLph, mafGramsPerSecond, fuelRateSource, adapterElapsedMs
5. **Create** `data/roomdb/ObdSampleDao.kt` — What: DAO for OBD sample CRUD and queries. Methods: `insert(ObdSampleEntity)`, `latestSample(): Flow<ObdSampleEntity?>`, `samplesBetween(startMs, endMs): Flow<List<ObdSampleEntity>>`, `deleteOlderThan(cutoffMs)`
6. **Edit** `data/roomdb/TrackDatabase.kt` — What: wire OBD entity and migration. How: add `ObdSampleEntity` to entities list; bump version 3→4; add `abstract fun obdSampleDao()`; add inline `MIGRATION_3_4` (`CREATE TABLE obd_sample` with all fields); chain migration into `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`
7. **Create** `feature/obd/data/ObdPreferencesDataStore.kt` — What: DataStore for OBD settings and state. DataStore name: `obd_prefs`. Keys (with defaults): `obdServiceEnabled` (bool, false), `obdDeviceMac` (string, ""), `obdPollHz` (int, 2), `obdRetentionDays` (int, 7), `obdRetryMaxSeconds` (int, 120), `obdLastState` (string, "Idle"), `obdLastError` (string, ""), `obdLastSampleTs` (long, 0). Model pattern on `feature/observer/data/ObserverPreferencesDataStore.kt`
8. **Edit** `TrackApp.kt` — What: expose OBD DAO and notification channel. How: add `val obdSampleDao by lazy { TrackDatabase.getDatabase(this).obdSampleDao() }`; in `onCreate`, create notification channel with id `OBD_CHANNEL_ID = "OBD_POLLING"`
9. **Create** `feature/obd/service/ObdPollingService.kt` (shell for Slice 1) — What: service to manage OBD connection. What it exposes: `companion object { val obdUiState = MutableStateFlow<ObdUiState>(ObdUiState.Idle) }`. What it does: handles `ACTION_START`/`ACTION_STOP` intents (no-op stubs for now); calls `startForeground()` with OBD notification; sets up `serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` and tears it down in `onDestroy()`. `ObdUiState` sealed class: `Idle`, `Connecting`, `Connected(rpm, obdSpeedKmh, fuelRateLph, fuelSource, instantKmL, avgKmL, sessionActive)`, `Retrying(attemptSeconds, maxSeconds)`, `Waiting(lastError)`
10. **Create** `screens/settings/obd/ObdSettingsScreen.kt` (shell for Slice 1) — What: Settings UI for OBD. Displays: status card showing `ObdPollingService.obdUiState` (initially "Idle"); Enable toggle (disabled for now, always shows OFF). No other controls yet
11. **Edit** `navigation/Screen.kt` — What: register OBD Settings route. How: add `object ObdSettingsScreen : Screen("obd_settings_screen")`
12. **Edit** `navigation/NavGraph.kt` — What: add OBD route to graph. How: add `composable(Screen.ObdSettingsScreen.route) { ObdSettingsScreen(navController) }`
13. **Edit** `screens/settings/SettingsScreen.kt` — What: add OBD link in TOOLS. How: in TOOLS section after Observer row, add `SettingsRow(title = "OBD", supporting = "ELM327 Bluetooth telemetry", onClick = { navController.navigate(Screen.ObdSettingsScreen.route) })`

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

1. **Edit** `feature/obd/service/ObdPollingService.kt` — What: implement full poll loop and metrics. After successful `socket.connect()`, emit `Connected` and start polling coroutine at `obdPollHz` Hz (`delay(1000L / pollHz)`). Poll sequence: execute `SpeedCommand` (get `obdSpeedKmh`), execute `RPMCommand` (get `rpm`), then fuel fallback chain: try `FuelConsumptionRateCommand` (DIRECT_FUEL_RATE); else try `MassAirFlowCommand` (MAF_DERIVED: `fuelRateLph = maf × 3600 / (14.7 × 750)`); else mark UNAVAILABLE. Calculate EMA instant km/L: `emaKmL = 0.2 × (gpsSpeedKmh / fuelRateLph) + 0.8 × emaKmL`; set to null when GPS speed < 3 km/h or accuracy > 20 m (read from `TrackingService.locationUiState`). Accumulate session fuel: `sessionFuelLiters += fuelRateLph × dtHours`; `sessionDistanceKm` from `TrackingService.locationUiState`. Compute average km/L: `sessionDistanceKm / sessionFuelLiters`. Write `ObdSampleEntity` to DB only when `sessionActive == true`. Retention cleanup: on `ACTION_START` and every 6 h, call `obdSampleDao.deleteOlderThan(now - retentionDays × 86_400_000)`
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

### Pending

| Pre-checks | Work |
|---|---|
| Kotlin compat | Confirm `kotlin-obd-api` compiles on Kotlin 1.7.0; if not, skip library and implement raw AT I/O |

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
