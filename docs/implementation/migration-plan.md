# Migration Plan

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

This file records database and data migration requirements caused by accepted CRs and rollout phases.

# CR-0001 — Legacy Trip Paths to Canonical Location Log

## Goal

Preserve existing saved trips while migrating from trip-owned serialized path data to a canonical location log.

## Required migration behavior

1. Create `LocationEntity` table.
2. Create `SessionEntity` table.
3. Add `startLocationId` and `endLocationId` to trip storage.
4. For each legacy trip with serialized path points:
   - Parse the old path string.
   - Generate one location row per point.
   - Assign generated timestamps by distributing points across the old trip duration.
   - Set the trip's `startLocationId` to the first generated row.
   - Set the trip's `endLocationId` to the last generated row.
5. Preserve trip summary fields for fast list rendering.
6. Do not create sessions for legacy trips unless a future CR explicitly requires it.

## Edge cases

| Case | Expected handling |
|---|---|
| Legacy trip has no path points | Keep trip if product wants historical record, but boundaries remain null; otherwise document deletion rule before implementing |
| Legacy trip has one point | Start and end boundaries point to the same location row |
| Legacy duration missing/zero | Use original trip timestamp for all generated points |
| Path parse failure | Preserve trip row, mark migration issue if an error-reporting mechanism exists |

## Verification

- Existing trips remain visible in List.
- Existing trip details can render paths from the new location table.
- Migration is idempotent under Room migration test conditions.

# Observer migrations

Observer migrations remain planned and governed by `product/accessibility-observer-prd.md` and `implementation/rollout-plan.md`.

Expected later migrations:

- Add `observed_events` table.
- Add indexes for timestamp and sync state.
- Add FTS5 table/triggers or DAO-managed FTS updates.
- Add denylist persistence if denylist is stored in Room.
