# ADR-0001 — Canonical Location Log

Date: 2026-05-16 13:52:51 +07:00

## Status

Accepted

## Context

CR#1 introduced always-recorded sessions. The previous model of trip-owned serialized path points is not sufficient because sessions and trips must share the same recorded location history.

## Decision

Use an append-only canonical location log. Sessions and trips reference location rows by inclusive `startLocationId` and `endLocationId` boundaries.

## Consequences

Positive:

- Sessions and trips can share location data.
- Trip deletion does not destroy raw location history.
- Migration path from legacy trips is explicit.
- Future session detail maps can reuse existing location rows.

Tradeoffs:

- Requires database migration.
- Requires careful boundary handling when starting/stopping trips.
- Requires retention/data cleanup policy later if raw location history grows large.
