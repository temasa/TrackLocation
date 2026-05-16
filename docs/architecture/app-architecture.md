# App Architecture

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

This file describes high-level module boundaries for the TrackLocation app after CR#1 and before full Observer implementation.

## Architectural principles

- Android-first.
- Jetpack Compose + Material3 UI.
- Single Activity with Compose navigation.
- Local-first persistence with Room.
- Foreground service for ongoing location capture.
- Clear separation between GPS/session/trip logic and Observer logic.
- Remote integrations must be isolated behind data-source abstractions.

## Current feature areas

### GPS Location Recorder

Responsible for collecting location points into the canonical location log.

Responsibilities:

- Start foreground recording.
- Stop foreground recording when always-recording is turned OFF.
- Continue recording while a trip is active.
- Expose active recording/session state to UI.

### Sessions

Responsible for ON-to-OFF always-recorded ranges.

Responsibilities:

- Create session when always-recording starts.
- Close session when always-recording stops.
- Show newest sessions first.
- Keep sessions distinct from trips.

### Trips

Responsible for explicit travel ranges.

Responsibilities:

- Start trip range.
- Stop trip range.
- Store trip summaries.
- Reconstruct trip details from location boundaries.

### Observer

Planned feature area. Governed by the Accessibility Observer PRD.

Responsibilities when implemented:

- Accessibility event capture.
- UI snapshot extraction.
- Local Room storage.
- FTS5 search/filtering.
- Sync behind `RemoteDataSource`.
- Registration and face-first auth.

## Layering guidance

```text
Compose UI
  -> ViewModel / UI state
  -> Use cases / repositories
  -> Room DAOs + services + remote data sources
```

Do not place database, Neon, accessibility service, or foreground service logic directly in composables.

## Remote boundary

All remote upload/auth behavior must go through `RemoteDataSource`.

Do not place Neon-specific details in:

- Compose UI
- ViewModels
- Accessibility service
- GPS service
