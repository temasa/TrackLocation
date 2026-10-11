---
name: ERRORS-LOG.md
path: docs/ERRORS-LOG.md
description: Persistent error learning log — captures all errors from compilation, build, deployment, and verification with root cause, resolution, and lessons
---

# Errors Log

## TrackLocation

**Document Version:** 0.7  
**Status:** Active (persistent — never delete)  
**Last Updated:** 2026-10-11  
**Owned By:** Project Team

---

## Purpose

This log is a **permanent learning artifact**. Every error encountered during compilation, build, deployment, or verification is recorded here with its root cause and resolution — so patterns are recognized, mistakes are not repeated, and future sessions have full context on what has already been debugged.

Unlike `PLANNING-LOG.md`, this document is **never deleted**. It grows with the project.

---

## When to Log

Log an entry whenever an error occurs during any of these phases:

| Phase | Examples |
|-------|---------|
| **Compilation** | Type errors, missing imports, syntax errors, annotation processor failures |
| **Build** | Gradle/Maven/npm/Makefile failures, missing dependencies, version conflicts |
| **Deployment** | Docker build failures, server config errors, environment variable mismatches, migration failures |
| **Verification** | Test failures, assertion errors, integration check failures, smoke test errors |

---

## How to Log

1. Add a new entry under **Error Entries** using the template below.
2. Record **Found At** immediately when the error occurs (seconds precision).
3. Fill **Resolved At** once the error is fixed; leave `—` while still open.
4. Add a row to the **Quick Reference Table** above the entries section.
5. After 3+ similar errors appear, add a note to **Patterns & Recurring Issues**.

---

## Quick Reference Table

| ID | Found At | Resolved At | Phase | Sprint/Task | Title | Status |
|----|----------|-------------|-------|-------------|-------|--------|
| ERR-010 | 2026-10-11 | 2026-10-11 (manual mitigation via adb; permanent fix pending ADR-028 banner) | Verification | Observer capture (ADR-013 / ADR-014 / ADR-015) | Observer accessibility service silently disabled (dropped from `ENABLED_ACCESSIBILITY_SERVICES` on force-stop / reinstall) from ~07:22 WIB; two Gojek orders not captured, Track auto-takeover did not fire; Observer Feed "live" chip still showed | Mitigated (manual); banner pending |
| ERR-009 | 2026-10-11 | 2026-10-11 (fix `3354689`) | Verification | Fuel dock (ADR-027) | Fuel dock not rendering — mounted in unreferenced `screens/MainScreen.kt`; fixed by `FuelDockHost` in MainActivity | Resolved |
| ERR-008 | 2026-10-11 | — (code fixed; device check pending) | Verification | Session running cost / fuel accumulator | Running-session COST/fuel reset or stopped accumulating — TrackingService re-wrote its cached session row with REPLACE on every GPS point, clobbering OBD accumulator increments | Code fixed (partial UPDATE); device verification pending |
| ERR-007 | 2026-10-09 | 2026-10-10 (partial: pending a real order) | Verification | Order trip save (multi-instance MainActivity) | Duplicate order-trip cards (3–5 identical rows) — several MainActivity instances each collected the order-end event | Root-cause fix verified on device via takeover repro (single MainActivity, 1 ShareViewModel); end-to-end order check pending |
| ERR-006 | 2026-10-09 | — | Verification | ADR-024 average km/L | SESSION AVG km/L above 700 km/L — GPS distance over the whole session divided by fuel accumulated only while OBD was valid/connected | Open |
| ERR-005 | 2026-07-10 | — | Verification | ADR-012 sessionActive decoupling | OBD connected + polling but `sessionActive=false` → avg km/L / cost / SESSION AVG / obd_sample writes skipped while instant km/L & L/h show | Open |
| ERR-004 | 2026-07-07 | 2026-07-07 | Verification | ADR-007 Slice 1 v7→v8 | Room build crash — `fallbackToDestructiveMigrationFrom(7)` illegal alongside `MIGRATION_6_7` (end version 7); app crashed on launch | Resolved |
| ERR-003 | 2026-07-07 | 2026-07-07 | Verification | ADR-006 dwell collapse | Dwell collapse froze live GPS speed → OBD idle metric wrong (km/L instead of L/h) + phantom session distance | Resolved |
| ERR-002 | 2026-07-06 22:05:00 | 2026-07-06 22:12:25 | Build | OBD Phase 2 deploy | Google Maps API key resValue name mismatch | Resolved |
| ERR-001 | 2024-01-15 10:23:45 | 2024-01-15 10:41:02 | Build | S1-T2 | Example: Gradle JDK version mismatch | Resolved |

*(Add a row here for every new entry — update Resolved At when fixed)*

---

## Error Entries

---

### ERR-010: Observer service silently disabled — two Gojek orders missed, no visible warning

**Found At:** 2026-10-11 ~20:00 WIB (found after the fact; the service had been off from ~07:22 WIB)  
**Resolved At:** 2026-10-11 (manual mitigation via adb; the permanent mitigation is the Observer service-off banner, ADR-028, not yet implemented)  
**Phase:** Verification  
**Sprint/Task:** Observer capture for Gojek orders (ADR-013, ADR-014, ADR-015); Observer Feed (ADR-028)  
**Environment:** Galaxy S9+ SM-G965F, Android 10, adb tunnel (`ANDROID_ADB_SERVER_PORT=5038`)  
**Status:** Mitigated (manual); permanent fix pending  

**Error Message / Output:**
No crash and no build error. Two Gojek orders were not captured, and the Track auto-takeover (ADR-014 / ADR-015) never fired. The Observer Feed still showed its normal "live" chip, so nothing on screen indicated that capture was off.

**Root Cause:**
Android removes an accessibility service from `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` on force-stop or on reinstall. The Observer service was dropped at about 07:22 WIB, and capture stopped silently. The app never checked whether the service was enabled. The Observer Feed "live" chip reflects only the capture-switch preference, not whether accessibility events are delivered, so the screen looked healthy.

**Resolution:**
- **Manual, 2026-10-11:** the Observer component was re-added with `adb shell settings put secure enabled_accessibility_services <list>`, keeping the `rk.android.app.lockscreen` service enabled.
- **Permanent (decided, not yet implemented):** ADR-028 adds an amber banner on the Observer Feed when the Observer service is not in the enabled list, with a tap that opens Accessibility settings. Code is pending approval; the slice is in IMPLEMENTATION-PLAN §4 "Observer Service-Off Banner (ADR-028)".

**Lesson Learned:**
1. A feature that depends on a system-granted service must show that service's own state. A status chip has to reflect the real dependency, not only a user preference. Otherwise the screen can look healthy while the feature is dead.
2. Re-check Observer enablement after every `adb install -r` and every force-stop. Both can silently drop the accessibility service.
3. For any silent capture loss, ask what the user sees when the dependency is off. If the answer is "nothing", add a visible state before shipping.

---

### ERR-009: Fuel dock not rendering — mounted in an unreferenced composable (`screens/MainScreen.kt`)

**Found At:** 2026-10-11 (first device run; time not recorded)  
**Resolved At:** 2026-10-11 (fix `3354689`)  
**Phase:** Verification  
**Sprint/Task:** Fuel dock, Fill up and Reset (ADR-027) first on-device run  
**Environment:** local (`:app:assembleDebug`, `adb install -r` over existing data, Galaxy S9+ SM-G965F, Android 10)  
**Status:** Resolved  

**Error Message / Output:**
No crash and no build error. After install the dock did not appear on any screen. Found by the device run, not by the build or by static review.

**Root Cause:**
`FuelDock` was mounted inside `screens/MainScreen.kt`, but no code calls `MainScreen`; the real app shell is `MainActivity.setContent`. The static review checked the dock composable, its imports and its Compose 1.2 APIs, but never checked that the host composable is referenced from the entry point.

**Resolution:**
New `ui/components/FuelDockHost.kt` (dock, the three dialogs and the route-to-theme mapping) mounted in `MainActivity`'s Scaffold `bottomBar` as `Column { FuelDockHost; BottomNavigationScreen }` (commit `3354689`). `MainScreen.kt` was restored to its pre-dock content, was dead code, and was deleted 2026-10-11 (dead code removed; no call sites). After a rebuild and reinstall the dock rendered on Session, List, Track and Settings.

**Lesson Learned:**
1. Before mounting UI into a host composable, grep for its call sites. A host with no caller is dead code, and anything mounted in it never renders.
2. Static review must confirm the host is reachable from the entry point (MainActivity), not only that the new code compiles.
3. Run an early device smoke test of a new visible element right after the first build, before writing the rest of the feature.

---

### ERR-008: Running-session cost/fuel reset on every GPS point — cached session row written back with REPLACE

**Found At:** 2026-10-11 (device observation on the Sessions screen + code analysis)  
**Resolved At:** — (code fixed 2026-10-11; device verification pending)  
**Phase:** Verification  
**Sprint/Task:** Session running cost / fuel accumulators (TrackingService cache write-back vs ObdPollingService increments)  
**Environment:** on-device drive (user), SM-G965F Android 10 (running-session COST on the Sessions screen)  
**Status:** Code fixed — device verification PENDING  

**Error Message / Output:**
No crash. Observed: the running-session COST (litres × price) periodically reset or stopped accumulating while OBD was polling; the session average km/L was also affected.

**Root Cause:**
`TrackingService.updateActiveSession()` kept a cached `SessionEntity` and wrote it back with `insertSession` (Room `@Insert`, REPLACE) on every location point. `ObdPollingService` increments `obdFuelConsumedL` / `obdCoveredDistanceKm` directly in the database through `SessionDao.addObdAccumulator`. Each REPLACE overwrote the whole row with the stale accumulator values held in the cache, clobbering the fuel and covered-distance totals written since the cache was last refreshed. The close paths (session start insert, `stopAlwaysRecording` re-integration from `obd_sample`) were not affected.

**Resolution:**
Added `SessionDao.updateSessionProgress(...)`, a partial `UPDATE` of only `startLocationId`, `endLocationId`, `distanceMeters`, `durationMillis`, `pointCount` and `isActive`. `updateActiveSession()` now calls it, so the OBD accumulator columns are never written from the service cache. No schema change, no Room migration. Note: the unused `SessionDao.updateSession` (`@Update`) would have the same hazard if it were used on a cached session. Static review only — not built, not unit-tested, not device-verified (AGENTS.md §5a, no permission given).

**Lesson Learned:**
Never write back a full row from a cached entity when another writer increments columns of that same row in the database. Use a targeted `UPDATE` that names only the columns this writer owns. A REPLACE insert or an `@Update` of a stale copy silently reverts concurrent counters, and the symptom appears only intermittently. Device check still required: the running-session COST should keep rising across GPS points.

---

### ERR-007: Duplicate order-trip cards — several MainActivity instances each saved the same order trip

**Found At:** 2026-10-09 (device inspection: 3, 4 and 5 identical order-trip rows; manual trips never duplicated)  
**Resolved At:** 2026-10-10 (partial: pending a real order)  
**Phase:** Verification  
**Sprint/Task:** Order trip save path (ShareViewModel order-end collector) / Observer takeover  
**Environment:** on-device use (user), debug build, Trips list  
**Status:** Root-cause fix verified on device via takeover repro (single MainActivity, 1 ShareViewModel); end-to-end order check pending  

**Error Message / Output:**
No crash. Trips list showed the same order trip 3–5 times. Heap dumps showed `ShareViewModel` count 1 → 2 after a takeover launch.

**Root Cause:**
`ObserverAccessibilityService.launchTrackTakeover()` starts `MainActivity` with `NEW_TASK | REORDER_TO_FRONT | SINGLE_TOP`. `SINGLE_TOP` reuses the instance only when it is the top activity of the task. In debug builds `DiagnosticConsentActivity` (ADR-021) and the system MediaProjection consent dialog sit above `MainActivity`, so Android created a new `MainActivity` and the old ones stayed alive with live `ShareViewModel`s. Each `ShareViewModel` runs its own order-end collector, so one order finish saved the same trip once per live instance. The earlier per-instance `stopTripMutex` (commit a28b0aa, see IMPLEMENTATION-PLAN "Fix: duplicate trip card") could not serialise across instances, so it did not fix this.

**Resolution:**
Two changes, pending device verification:
1. `MainActivity` declared `android:launchMode="singleTask"` in `AndroidManifest.xml`, so there is one activity instance and one `ShareViewModel`.
2. The stop-trip guard in `ShareViewModel` is process-wide: the `Mutex` moved to the companion object, plus a `lastPersistedTripStart` early return so the same order trip cannot be inserted twice even if more than one `ShareViewModel` exists.

Verified 2026-10-10 on SM-G965F (debug build): three takeover launches with the consent dialog pending kept a single `MainActivity` record; heap dump showed `ShareViewModel` 1 and `MainActivity` 1 (before the fix: 2 and 3). The end-to-end order-finish path is not yet verified.

Data cleanup 2026-10-10: the 9 duplicate Trips rows (idx 30, 31, 33–35, 37–40) were deleted on device via the app's long-press delete. Kept idx 31, 32, 40 (plus unrelated new trip idx 41). Afterwards the track table had 32 rows, no repeated trip-start timestamps, and `PRAGMA integrity_check` = ok.

Debug-only side effect: a pending consent dialog above `MainActivity` is closed on takeover.

**Lesson Learned:**
- `SINGLE_TOP` is not single-instance. Use `singleTask` when an activity must be unique.
- A ViewModel scoped to an Activity multiplies with the number of Activity instances. Any collector that performs an app-wide side effect (persisting a trip) must not live in an Activity-scoped ViewModel without a process-wide guard.
- A per-instance lock does not protect against duplicate work across instances. Put the guard at the scope where the side effect happens.

---

### ERR-006: SESSION AVG km/L above 700 — numerator and denominator accumulated over different intervals

**Found At:** 2026-10-09 (user observation of the Session screen showing SESSION AVG > 700 km/L)  
**Resolved At:** —  
**Phase:** Verification  
**Sprint/Task:** ADR-024 — average km/L over fuel-covered intervals  
**Environment:** on-device use (user), Session screen OBD card  
**Status:** Open — root cause identified; fix documented in ADR-024, code pending  

**Error Message / Output:**
No crash. Session screen "SESSION AVG" displayed values above 700 km/L (physically impossible).

**Root Cause:**
`ObdPollingService.kt` (~L457-461) computes `avgKmL = session.distanceMeters/1000 ÷ session.obdFuelConsumedL`. The numerator is GPS distance over the WHOLE always-on session (accumulated by `TrackingService` regardless of OBD). The denominator is fuel accumulated only while OBD is connected, a valid fuel rate exists (0.1..100 L/h) and the poll gap is < 60 s. Distance driven without matching fuel (OBD connected late, reconnect gaps, null-fuel stretches, gaps >= 60 s) inflates the ratio; the only guard was fuel > 0.01 L. The same mismatch exists in the live trip average (`gpsState.distanceInMeters / tripFuelLiters`) and in completed-trip rows (`track.distance / track.obdFuelConsumedL` in `TrackItemRow`). Previously noted as the out-of-scope "partial-connect over-optimism" in ADR-012.

**Resolution:**
Pending (docs-first). Per ADR-024: accumulate covered distance (`gpsSpeedKmh` x dt) over the same fuel-covered poll intervals as fuel (new columns `obdCoveredDistanceKm` on `recording_session` and `track`, `gpsSpeedKmh` on `obd_sample`; Room v12 -> v13 `MIGRATION_12_13`), and compute every average as covered distance ÷ fuel. No plausibility clamp.

**Lesson Learned:**
Accumulate the numerator and the denominator of a ratio over the same intervals. A ratio of two independently gated accumulators is a bug class (see also ERR-003, ERR-005, which also corrupted the OBD averages). Do not hide such a bug with a sanity clamp; fix the cause.

---

### ERR-005: OBD fuel accumulation silently gated off — connected but `sessionActive=false`

**Found At:** 2026-07-10 (drive observation + code analysis)  
**Resolved At:** —  
**Phase:** Verification  
**Sprint/Task:** ADR-012 — OBD accumulation gate decoupling  
**Environment:** on-device drive (user), SM-G965F Android 10 (metrics observed on the Trips active-trip row)  
**Status:** Open — root cause identified; fix documented in ADR-012, code pending  

**Error Message / Output:**
No crash. Observed: on the active-trip row, instant `km/L` and `L/h` display live values while `avg km/L` and `cost` stay `—` for the whole drive; completed-trip `km/L`/`cost` also blank.

**Root Cause:**
`ObdPollingService` gates all fuel accumulation, `obd_sample` inserts, and retention pruning on the private `sessionActive` flag, set only by `ACTION_SESSION_ON` from `TrackingService`. The OBD connection lifecycle (`ACTION_START` from `MainActivity` every launch, `ACTION_RECONNECT_NOW` from Reconnect buttons) is independent and never sets `sessionActive`, and `onStartCommand` has no null-intent recovery. So a fresh/restarted-and-reconnected `ObdPollingService` polls with `sessionActive=false` even though `TrackingService.isAlwaysRecording` (the UI-authoritative flag) is true — a two-flag divergence across two services synced only by best-effort intents. Instant km/L and L/h are computed OUTSIDE the `if (sessionActive)` block so they still show; avg km/L, cost, SESSION AVG, and sample writes are inside it, so they're skipped. Finished-trip km/L/cost re-integrate `obd_sample`, so with no samples written they're blank too. An independent impact analysis confirmed the accumulation math is correct — the fault is entirely state-wiring.

**Resolution:**
Pending (docs-first). Per ADR-012: gate the accumulation/sample-recording block on `gpsState.isAlwaysRecording` (already read each poll from `TrackingService.locationUiState`) instead of `sessionActive`; derive the session PK from `sessionDao.getActiveSession()` (single read/poll, reused); demote `ACTION_SESSION_ON/OFF` to foreground-promotion advisories; remove the dead `ObdUiState.Connected.sessionActive` field. Code + device re-verification to follow.

**Lesson Learned:**
Never gate one service's behavior on a private flag that mirrors another service's state via fire-and-forget intents — the two drift on independent lifecycles (connect order, single-service restart, START_STICKY null redelivery). Read the authoritative shared state directly. When a value has a "live per-poll" source and an "accumulated/gated" source, verify BOTH paths are fed by the same signal; a symptom where the instant value shows but the average doesn't points straight at a gate the instant path bypasses.

---

### ERR-001: Example — Gradle JDK version mismatch *(example entry, replace with real errors)*

**Found At:** 2024-01-15 10:23:45  
**Resolved At:** 2024-01-15 10:41:02  
**Phase:** Build  
**Sprint/Task:** S1-T2  
**Environment:** local  
**Status:** Resolved  

**Error Message / Output:**
```
> Task :app:compileDebugKotlin FAILED
e: error: incompatible types: Int cannot be converted to Long
FAILURE: Build failed with an exception.
* What went wrong: Execution failed for task ':app:compileDebugKotlin'.
> Compilation error. See log for more details.
```

**Root Cause:**
`gradle-wrapper.properties` specified Gradle 8.x, but the local JDK was 11; Gradle 8 requires JDK 17+. The mismatch caused the Kotlin compiler to report spurious type errors before the real JDK error surfaced.

**Resolution:**
```bash
# Verified JDK version
java -version   # was 11

# Updated JAVA_HOME to JDK 17 in local .env
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk

# Re-ran build
./gradlew assembleDebug
```

**Lesson Learned:**
Always check JDK version first when the build error looks like a type error but the code is correct. Add JDK version requirement to README. Pin the expected JDK version in `CLAUDE.md` under development environment.

---

*(Replace the example above and add new entries below this line)*

---

### ERR-004: Room build crash — `fallbackToDestructiveMigrationFrom(7)` collides with `MIGRATION_6_7`

**Found At:** 2026-07-07  
**Resolved At:** 2026-07-07  
**Phase:** Verification  
**Sprint/Task:** ADR-007 Slice 1 (Fuel-Economy Unification) — on-device verification  
**Environment:** local (Gradle :app:installDebug, device SM-G965F Android 10)  
**Status:** Resolved  

**Error Message / Output:**
```
java.lang.IllegalArgumentException: Inconsistency detected. A Migration was supplied to
addMigration(Migration... migrations) that has a start or end version equal to a start
version supplied to fallbackToDestructiveMigrationFrom(int... startVersions). Start version: 7
	at androidx.room.RoomDatabase$Builder.build(RoomDatabase.kt:1265)
	at com.kolee.tracklocation.data.roomdb.TrackDatabase$Companion.getDatabase(TrackDatabase.kt:265)
```
Crashed on launch (MainActivity.onCreate → first DAO access) before the database ever opened; the v7→v8 upgrade never ran (device DB stayed at user_version 7 with obdGpsDistanceKm still present).

**Root Cause:**
Room forbids registering a migration whose start OR end version equals a start version passed to `fallbackToDestructiveMigrationFrom(...)`. ADR-006 added `MIGRATION_6_7` (end version = 7); ADR-007 Slice 1 added `fallbackToDestructiveMigrationFrom(7)` (start version = 7). The two collide at version 7, so `RoomDatabase.Builder.build()` throws. ADR-006's `MIGRATION_6_7` and ADR-007's destructive fallback are mutually exclusive — the ADR-007 mechanism was infeasible as written. KAPT schema validation passes at build time; this is a runtime builder check, so it only surfaces on device.

**Resolution:**
Replaced `.fallbackToDestructiveMigrationFrom(7)` with an explicit destructive `MIGRATION_7_8` in `TrackDatabase.kt`: `DROP TABLE IF EXISTS recording_session` then recreate it without `obdGpsDistanceKm` (matching the v8 `SessionEntity`), registered in `addMigrations(...)`. Device-verified: logcat "DB version upgrading from 7 to 8", crash buffer empty, MainActivity rendered, Room open-time schema validation passed.

**Lesson Learned:**
`fallbackToDestructiveMigrationFrom(N)` cannot coexist with any registered migration ending at N — use an explicit N→N+1 migration instead. When one ADR adds a migration ending at version N and another adds a destructive fallback from N, they conflict; check cross-ADR migration interactions. The conflict is a runtime `build()` check invisible to KAPT — always launch on device after changing the Room builder config.

---

### ERR-002: Google Maps API key resValue name mismatch — resource linking failed

**Found At:** 2026-07-06 22:05:00  
**Resolved At:** 2026-07-06 22:12:25  
**Phase:** Build  
**Sprint/Task:** OBD Phase 2 — device deploy  
**Environment:** local (Gradle :app:installDebug, device SM-G965F Android 10)  
**Status:** Resolved  

**Error Message / Output:**
```
> Task :app:processDebugResources FAILED
> Android resource linking failed
AndroidManifest.xml:69: error: resource string/google_maps_key (aka com.kolee.tracklocation:string/google_maps_key) not found.
error: failed processing manifest.
```

**Root Cause:**
Commit `00cf587` ("fix(security): move Google Maps API key to local.properties") injected the key via `resValue "string", "google_maps_key_placeholder", mapsKey` in `app/build.gradle`, but `AndroidManifest.xml` still references `@string/google_maps_key`. The generated resource name (`google_maps_key_placeholder`) did not match the manifest reference (`google_maps_key`), so resource linking could not resolve `string/google_maps_key`. The key value itself was present in `local.properties`; only the resource name was wrong.

**Resolution:**
Renamed the generated resource in `app/build.gradle` line 29 to match the manifest reference:
```groovy
resValue "string", "google_maps_key", mapsKey
```
Re-ran `:app:installDebug` → BUILD SUCCESSFUL, installed on SM-G965F.

**Lesson Learned:**
When moving a value into a build-injected `resValue`, the resource name must stay identical to every `@string/...` reference in the manifest and layouts. A rename silently breaks `processDebugResources` (resource linking), not Kotlin compilation — so it will not surface until an actual build/install is run. Grep every `@string/<name>` reference before renaming an injected resource.

---

### ERR-003: Dwell collapse froze live GPS speed → OBD idle metric wrong (km/L instead of L/h) + phantom session distance

**Found At:** 2026-07-07  
**Resolved At:** 2026-07-07  
**Phase:** Verification  
**Sprint/Task:** ADR-006 dwell collapse on-device verification  
**Environment:** local (Gradle :app:installDebug, device SM-G965F Android 10)  
**Status:** Resolved  

**Error Message / Output:**
Identified by code analysis during ADR-006 dwell-collapse verification (prompted by a question about how fuel consumption is handled while the car is stopped) — NOT observed as a runtime failure. Predicted incorrect behavior: while parked during an active trip, the instant metric would show `km/L` instead of `L/h` at idle, and SESSION AVG would accrue phantom OBD distance, because both derive from a GPS speed value the collapse branch no longer refreshes.

**Root Cause:**
In `TrackingService.recordLocation`, the dwell collapse branch called `database.locationDao.updateDwellAnchor(anchorId, dwellTs)` and then `return@launch` before `updateTripState(location, insertedId)`. The early return skipped the UI-state update in `updateTripState`, which normally refreshes `locationUiState.speedInKMH`, `currentLocation`, and `accuracyMeters` on every location fix. So when a car parked during an active trip, the speed froze at the last moving value. `ObdPollingService` reads `locationUiState.speedInKMH` for:
1. **Time-integrated distance:** `distIncrementKm = gpsSpeedKmh × dt / 3600`, accruing to `session.obdGpsDistanceKm` even when speed was stale
2. **Idle detection:** `if (speed < 3 km/h && RPM > 0) show L/h else show km/L`, so a stale non-zero speed fooled the check into displaying km/L instead of L/h

Result: stopped car showed km/L (wrong), accumulated phantom OBD distance (wrong), SESSION AVG km/L was corrupted.

**Resolution:**
Added an explicit UI-state refresh in the collapse branch before `return@launch` (lines 359–365 in `TrackingService.kt`):
```kotlin
_locationUiState.update { state ->
    state.copy(
        currentLocation = LatLng(currentLocation.latitude, currentLocation.longitude),
        speedInKMH = kmh(currentLocation),
        accuracyMeters = currentLocation.accuracy
    )
}
```
This refresh updates live consumers (`speedInKMH ≈ 0` for a parked car) without inserting a DB row or adding to trip/session distance. After the fix, `assembleDebug` is clean (Room KAPT + Kotlin compile); on-device re-verification (stopped → L/h; no phantom SESSION AVG distance) is still PENDING.

**Lesson Learned:**
A write-time filter that skips the persistence path (e.g., collapse, dedup, sampling) can also skip live UI-state updates that OTHER subsystems depend on. When filtering writes, keep two concerns separate: (1) "what goes to the DB" and (2) "what live-state consumers read". The live signal must keep flowing even when you suppress the stored row. Always trace all readers of a StateFlow before adding early-return branches.

---

## Patterns & Recurring Issues

Populate this section when the same class of error appears 3 or more times.

| Pattern | Phase(s) | Entries | Mitigation |
|---------|----------|---------|------------|
| *(none yet)* | | | |

---

**This log is permanent. Do not delete. Append entries as errors are encountered.**
