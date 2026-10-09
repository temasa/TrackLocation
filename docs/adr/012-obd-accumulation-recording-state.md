# ADR-012: OBD Fuel Accumulation Gates on Shared Recording State (not the intent-synced session flag)

**Status:** Accepted
**Date:** 2026-07-10
**Decided By:** Claude Code (implementation) + project owner

## Context

`ObdPollingService` gates ALL trip/session fuel accumulation, `obd_sample` writes, and retention pruning on a private boolean `sessionActive`, set true only on an `ACTION_SESSION_ON` intent from `TrackingService` (false on `ACTION_SESSION_OFF`). The OBD connection lifecycle is independent: `ACTION_START` (fired by `MainActivity` every launch) and `ACTION_RECONNECT_NOW` (Reconnect buttons) connect and poll without touching `sessionActive`, and `onStartCommand` has no null-intent recovery. So a fresh/restarted-and-reconnected service can be polling — showing instant km/L and L/h (computed OUTSIDE the gate) — while `sessionActive == false`, in which case avg km/L, active-trip cost, SESSION AVG, and `obd_sample` recording are all silently skipped. Completed-trip km/L and cost re-integrate `obd_sample`, so the finished trip is blank too.

Observed live (drive, 2026-07-10): instant km/L + L/h displayed; avg km/L + cost stayed "—". Root cause is a two-flag divergence — `TrackingService.isAlwaysRecording` (authoritative, UI-synced) vs `ObdPollingService.sessionActive` (best-effort, intent-synced). See ERR-005. An independent impact analysis (blast radius + regression checklist) confirmed the accumulation math is correct; every failure is state-wiring.

## Decision

1. **Gate on the authoritative shared state.** Replace `if (sessionActive)` in the poll loop with `if (gpsState.isAlwaysRecording)`, where `gpsState = TrackingService.locationUiState.value` is already read each poll. It cannot drift from the UI, so no cross-service intent sync.
2. **Derive the session PK from the DB.** Read `sessionDao.getActiveSession()` once per poll and reuse for both `addObdAccumulator(sid, …)` and the `avgKmL`/`sessionFuelConsumedL` computation, instead of the intent-delivered `activeSessionId`. Guard `sid != null`.
3. **Demote `ACTION_SESSION_ON`/`OFF` to advisory** — retained only for foreground-service promotion, no longer the accumulation source of truth.
4. **Remove the dead `ObdUiState.Connected.sessionActive` field** (no UI reads it).

## Consequences

### Positive
- avg km/L, cost, SESSION AVG, and finished-trip fuel are consistent whenever recording is on — closes the observed bug and the whole class of connect-order / service-restart races.
- Removes an entire cross-service synchronization dependency.

### Negative / accepted risks
- One `getActiveSession()` Room read per poll (mitigated: single read reused for both consumers; prior code already read it once for `avgKmL`).
- Narrow session-start race: `closeAllActiveSessions` + `insertSession` are not atomic, so `getActiveSession()` could briefly return null (guarded — skips that tick) or a soon-to-close row (sub-second, single poll). Accepted as a known narrow window.

## Out of Scope (follow-ups surfaced by the impact analysis)
- ~~Partial-connect over-optimism: fuel burned while OBD was disconnected mid-trip isn't recovered, but GPS distance includes it → that trip's avg km/L reads slightly high.~~ **Resolved 2026-10-09 by [[024-avg-km-per-liter-over-fuel-covered-intervals]]** (averages now use covered distance over the same fuel-covered intervals).
- `stopAlwaysRecording()` calls `stopTrip()` directly, possibly ending a trip without saving a `TrackEntity`.
- Process kill mid-trip abandons trip state (`resumeIfActiveSession` restores session, not trip).
- Dead write-only fields `sessionDistanceKm`/`sessionFuelLiters` in `ObdPollingService`.

## Alternatives Considered
1. **Null-intent recovery that rehydrates `sessionActive` from the DB** — rejected: keeps two flags synced by best effort; papers over the race.
2. **Re-assert `ACTION_SESSION_ON` on every OBD connect** — rejected: still intent-dependent and order-sensitive.

## Related ADRs
- [[007-unified-fuel-economy-metrics]], [[008-fuel-price-effective-dated-entity]].

## References
- ERRORS-LOG ERR-005. `ObdPollingService.kt` (poll loop ~L414–495, onStartCommand ~L96–130), `TrackingService.kt` (isAlwaysRecording setter L82–88), `MainActivity.kt:66`, `SessionDao.getActiveSession`.
