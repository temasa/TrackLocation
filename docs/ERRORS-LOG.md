---
name: ERRORS-LOG.md
path: docs/ERRORS-LOG.md
description: Persistent error learning log — captures all errors from compilation, build, deployment, and verification with root cause, resolution, and lessons
---

# Errors Log

## TrackLocation

**Document Version:** 0.1  
**Status:** Active (persistent — never delete)  
**Last Updated:** 2026-07-07  
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
| ERR-004 | 2026-07-07 | 2026-07-07 | Verification | ADR-007 Slice 1 v7→v8 | Room build crash — `fallbackToDestructiveMigrationFrom(7)` illegal alongside `MIGRATION_6_7` (end version 7); app crashed on launch | Resolved |
| ERR-003 | 2026-07-07 | 2026-07-07 | Verification | ADR-006 dwell collapse | Dwell collapse froze live GPS speed → OBD idle metric wrong (km/L instead of L/h) + phantom session distance | Resolved |
| ERR-002 | 2026-07-06 22:05:00 | 2026-07-06 22:12:25 | Build | OBD Phase 2 deploy | Google Maps API key resValue name mismatch | Resolved |
| ERR-001 | 2024-01-15 10:23:45 | 2024-01-15 10:41:02 | Build | S1-T2 | Example: Gradle JDK version mismatch | Resolved |

*(Add a row here for every new entry — update Resolved At when fixed)*

---

## Error Entries

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
