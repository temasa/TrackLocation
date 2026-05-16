# TrackLocation Product Baseline

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

TrackLocation is an Android-first driver utility evolving from a GPS trip tracker into a broader operational tool that supports:

1. GPS trip tracking.
2. Always-recorded location sessions.
3. Accessibility event observation for developer/support inspection.

## Current accepted baseline

The current accepted app baseline includes CR#1: Always-recorded Location Sessions.

### GPS and Sessions

- The app maintains a canonical append-only location log.
- Always-recorded sessions are ON-to-OFF periods over the canonical location log.
- Sessions are not trips.
- Trips are explicit ranges over the canonical location log.
- Trip paths are reconstructed from `startLocationId` and `endLocationId` boundaries.
- Deleting a trip does not delete location history.
- If a trip starts while always-recording is OFF, always-recording starts automatically.
- Stopping a trip does not stop always-recording.

### Accessibility Observer

The Accessibility Observer remains a planned product area governed by `accessibility-observer-prd.md`.

Locked Observer principles:

- Capture all packages except configurable system-noise denylist.
- Package filtering is a UI concern, not a capture-layer setting.
- Store screen-tree JSON snapshots locally first.
- Use cursor-based pagination and indexed search.
- Sync is hybrid: batch-count and time-based.
- Auth gates UI only; background capture, sync, and GPS tracking continue.
- Raw face images are never transmitted.
- Unsynced local events are never deleted.

## Current navigation baseline

Current implemented/accepted bottom navigation after CR#1:

```text
Session / List / Track / Settings
```

Observer is planned but not yet integrated into the bottom navigation baseline. See `../architecture/navigation.md`.

## Product documentation rules

- This file describes the current accepted product baseline.
- CR files describe why and when behavior changed.
- Architecture files describe how accepted behavior is implemented conceptually.
- Implementation files describe what source code must change.
- Status files describe what actually happened historically.
