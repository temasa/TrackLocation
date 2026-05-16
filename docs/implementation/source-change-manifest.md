# Source Change Manifest

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

This file is the implementation source of truth for source-code changes. Every accepted CR or rollout phase should add a section here before code changes begin.

Each entry must classify source files as:

- Create
- Edit
- Delete
- Migrate
- Test

Historical status/progress files record what happened after implementation. This manifest records what should change before implementation.

---

# CR-0001 — Always-recorded Location Sessions

Status: Implemented in source as of 2026-05-16 18:30:39 +07:00; Gradle/test/device verification pending explicit permission.

## Create

| File | Purpose | Status |
|---|---|---|
| `app/src/main/java/com/kolee/tracklocation/screens/sessions/SessionsScreen.kt` | Sessions UI | Done per status report |
| `app/src/main/res/drawable/ic_session_signal.xml` | Session bottom-nav icon | Done per status report |
| `app/src/main/java/com/kolee/tracklocation/data/roomdb/LocationEntity.kt` | Canonical location row | Done |
| `app/src/main/java/com/kolee/tracklocation/data/roomdb/SessionEntity.kt` | Always-recording session row | Done |
| `app/src/main/java/com/kolee/tracklocation/data/roomdb/SessionDao.kt` | Session persistence queries | Done |
| `app/src/main/java/com/kolee/tracklocation/data/roomdb/LocationDao.kt` | Canonical location queries | Done |

## Edit

| File | Change | Status |
|---|---|---|
| `app/src/main/java/com/kolee/tracklocation/navigation/BottomNavigationScreen.kt` | Change to 4-tab bottom nav: Session/List/Track/Settings | Done per status report |
| `app/src/main/java/com/kolee/tracklocation/navigation/NavGraph.kt` | Add Session as top-level destination/start tab | Done per status report |
| `app/src/main/java/com/kolee/tracklocation/navigation/Screen.kt` | Add Session route metadata/icon reference | Done per status report |
| `app/src/main/res/values/strings.xml` | Add Session tab label | Done per status report |
| `app/src/main/java/com/kolee/tracklocation/ui/theme/Color.kt` | Align color tokens with CR#1 handoff | Done per status report |
| `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackDatabase.kt` | Add Location and Session entities; increment schema version | Done |
| `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackEntity.kt` | Add `startLocationId` and `endLocationId`; preserve summary fields | Done |
| `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackDao.kt` | Add location-range-aware queries | Done |
| `app/src/main/java/com/kolee/tracklocation/tracking/TrackingService.kt` | Append points to canonical location log; support always-recording lifecycle | Done |
| `app/src/main/java/com/kolee/tracklocation/screens/list/ListScreen.kt` and `app/src/main/java/com/kolee/tracklocation/screens/list/components/ListContent.kt` | Replace Export pill with always-recording switch | Done |
| `app/src/main/java/com/kolee/tracklocation/screens/track/TrackScreen.kt` and `app/src/main/java/com/kolee/tracklocation/screens/track/components/RunningCardTime.kt` | Update Start/Stop semantics and copy to trip-specific wording | Done |
| `app/src/main/java/com/kolee/tracklocation/screens/details/DetailsScreen.kt` | Resolve path from location ID range | Done |

## Delete

| File | Reason | Status |
|---|---|---|
| None | CR#1 does not require source deletion yet | N/A |

## Migrate

| Migration | Purpose | Status |
|---|---|---|
| Room migration from legacy trip path schema to canonical location log | Convert serialized trip path points into `LocationEntity` rows and set trip boundaries | Done |

## Tests required

| Test file | Required cases | Status |
|---|---|---|
| `TripMigrationTest.kt` | Legacy trip path converted to location rows and trip boundaries | Not added in this pass; verification pending permission/build capacity |
| `RecordingSessionRepositoryTest.kt` | Always-recording ON/OFF creates and closes sessions | Not added in this pass; verification pending permission/build capacity |
| `TripRecordingBoundaryTest.kt` | Start uses next point; stop uses latest point | Not added in this pass; verification pending permission/build capacity |
| `TripDeletionTest.kt` | Deleting trip keeps location rows | Not added in this pass; verification pending permission/build capacity |
| `SessionsScreenTest.kt` or Compose preview/manual checklist | Empty, active, and history session states | Not added in this pass; verification pending permission/build capacity |

---

# Accessibility Observer Rollout — Planned Source Areas

Status: Planned. Detailed file names may change once implementation begins.

## Phase 1 planned source changes

| Category | Expected files/modules |
|---|---|
| Create | Accessibility service, service config XML, `ObservedEventEntity`, Observer DAO, repository/use cases, Observer feed screen |
| Edit | Manifest, Room database, navigation graph, bottom/app shell after navigation decision |
| Test | Denylist filtering, searchable text generation, snapshot caps, DAO insert/get |

## Phase 2 planned source changes

| Category | Expected files/modules |
|---|---|
| Create | Observer detail screen, JSON viewer screen, detail ViewModel/state |
| Edit | Observer DAO for event by ID and cursor pagination, navigation graph |
| Test | Cursor boundaries, large JSON scroll behavior, metadata extraction |

## Phase 3 planned source changes

| Category | Expected files/modules |
|---|---|
| Create/Edit | FTS5 setup, filter query builder, package/text chip UI, denylist persistence, Settings sections |
| Test | Query composition, scoped package text, denylist CRUD, clear-local confirmation |

## Phase 4+ planned source changes

| Category | Expected files/modules |
|---|---|
| Sync | `RemoteDataSource`, fake remote, sync worker/state, retention cleanup |
| Neon | `NeonDirectDataSource`, secure config, SQL setup scripts |
| Auth | Registration flow, Google Sign-In, CameraX, ML Kit `FaceProcessor`, auth overlay |
| Test | Sync/retry/retention, registration state, auth state, face failure budget, real-device checklists |

## Rule

Before implementing any planned Observer phase, update this manifest with concrete file paths from the actual codebase.
