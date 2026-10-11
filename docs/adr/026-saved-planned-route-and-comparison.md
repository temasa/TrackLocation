# ADR-026: Saved Planned Route and Planned-vs-Actual Comparison (device-only)

**Status:** Accepted — conditional on ToS gate (see Open items / gates; no code ships until the gate is cleared)
**Date:** 2026-10-11
**Decided By:** Project owner + Claude Code

## Context

The Track screen can show up to three route lines for an order trip, each with a different lifetime:

- **Planned** — accepted point → pickup → drop, fetched once at order takeover and frozen (ADR-016, origin amended by ADR-017 Amendment 2026-10-11). Today it lives only in memory in `OrderRouteController` and is lost on process death. Because the shipped rule is "routes are display-only, not persisted" (ADR-016, PRD FR-17), the plan can never be compared with what the driver actually drove.
- **Optimized** (runtime) — the navigation aid, current location → next stop, re-fetched on deviation or phase change (ADR-016/017). Temporary by nature.
- **Actual** — the recorded GPS trace, already persisted in the canonical location log and `track.pathPoints`.

The owner wants to review a finished trip: where the plan said to go versus where the car went, with numbers (extra km, max deviation). The three lines must also be told apart on the live Track screen and on the trip details map. Persisting the planned route conflicts with the wording of BR-15 / FR-14 ("third-party routing results are never cached/stored"), so that rule needs an explicit, narrow amendment.

## Decision

1. **New Room table `trip_planned_route`** via `MIGRATION_14_15` (DB version 14 → 15; no exported schema JSON, `exportSchema = false`; migration verified by installing over a copy of the v14 database; ARCHITECTURE updated). Columns:
   - `tripStartedAt` INTEGER PRIMARY KEY — equals `track.timestamp` of the owning trip. **No foreign key** (same convention as `observer_trip`).
   - `kind` TEXT NOT NULL — `'ORDER'` | `'NAV'`.
   - `geometry` TEXT NOT NULL — ORS encoded polyline, precision 5, stored exactly as returned.
   - `distanceM` INTEGER, `durationS` INTEGER — from ORS `routes[0].summary`; the client must now extract them.
   - `originSource` TEXT — `'ACCEPTED'` | `'FALLBACK_LIVE'` (ADR-017 Amendment origin rule).
   - `fetchedAt` INTEGER.

   It is a **separate table, not a column on `track`**: list queries load whole `track` rows and `track` already carries a large `pathPoints` string, so adding geometry there risks CursorWindow and list-performance problems.
2. **Write at fetch, not at trip end.** `OrderRouteController` writes the row immediately after the planned fetch succeeds, keyed by `LocationUiState.tripStartedAt`. If `tripStartedAt == 0`, persisting is skipped and logged. On process restart the controller **hydrates** the frozen plan from the table if a row exists for the current trip; it never re-plans from a new origin. This also fixes the current in-memory-only loss on process death.
3. **Lifecycle.** The row is deleted when its trip is deleted (same DAO transaction path as the `track` delete). On app start an **orphan sweep** deletes rows with no matching `track.timestamp` AND `fetchedAt` older than 24 h (an in-progress trip has no `track` row yet). No separate retention cap: the row lives and dies with the user-deletable trip. **No backfill:** trips recorded before v15 show no planned line.
4. **Comparison numbers are derived at display time and never persisted:** Planned km (`distanceM`), Actual km (`track.distance`), Extra km = actual − planned (signed), Max deviation m = max over actual path points of the distance to the planned polyline. Shown on trip details only. No off-route-time metric (out of scope).
5. **BR-15 amendment.** A third-party route MAY be stored device-only as a trip's "planned route" record for display and comparison. It is NEVER ingested into the local route store (ADR-010) and never used to derive or seed routes; the route store derives only from the user's own location log. Geocode results are not stored by this ADR (pickup/drop coordinates are already stored under ADR-023).
6. **Optimized route stays in memory only.** Cleared on order end; never persisted; never fed to the route store. Rerouting stays deviation-based (> ~40 m, throttled 30 s / 100 m) or on phase change — not more frequent.
7. **UI.** The live Track screen differentiates the three lines without relying on colour alone: planned = dashed thin blue-grey, optimized = solid bold orange, actual = solid blue, with a compact dismissible legend. The trip details map draws the saved planned line (dashed) under the actual line, plus the comparison card, the legend and the ORS/OSM attribution; trips without a saved plan show the empty state "No planned route saved". The visual spec lives in UI-SPEC §3j (design handoff first per AGENTS §12; visuals are not invented in code).
8. **Privacy.** No new data leaves the device; ORS calls are unchanged. **Known risk:** the saved polyline together with the stored pickup/drop coordinates is a sensitive location pattern, and the manifest has `android:allowBackup="true"`, so the DB can end up in Google backups. This is an existing condition, not changed by this ADR, and is recorded here as a risk.
9. **`kind='NAV'` is reserved** for ADR-009 navigation trips (same mechanism, same table). Not implemented by this ADR.
10. **GATE — openrouteservice / HeiGIT Terms of Service.** The ToS sections available during research (https://account.heigit.org/info/tos) ban unacceptable use and overburdening and require attribution on the free API, but did **not** state whether storing returned route geometry is permitted. The data is OSM-derived (ODbL attribution applies wherever stored routes are shown). **Action for the project owner:** read the full ToS or contact HeiGIT, and record the finding in this ADR. If storing is NOT permitted, fall back to storing nothing from ORS (display-only, as today) and revisit this ADR. This gate must be cleared before code ships. Tracked in `docs/IMPLEMENTATION-ISSUES.md`.

## Consequences

### Positive
- Post-trip planned-vs-actual comparison (extra km, max deviation) on trip details.
- The frozen plan survives process death (hydrate instead of re-plan from a new origin).
- The three lines are distinguishable live and after the trip.
- Same mechanism is ready for future navigation trips (`kind='NAV'`).

### Negative
- Room migration 14 → 15, a new table with its own DAO, and hydrate/orphan-sweep logic to maintain.
- Licence gate: code cannot ship until the ORS ToS question is answered; if the answer is "not permitted" the feature falls back to display-only.
- Privacy/backup risk (saved route + stored stop coordinates; `allowBackup="true"` unchanged).
- No backfill: trips recorded before v15 have no planned line; only order trips (and later NAV) ever have one.
- BR-15 / FR-14 wording is loosened (narrowly) and a new BR-16 is added for the optimized route.

### Schema impact
Room v14 → v15, `MIGRATION_14_15`: `CREATE TABLE trip_planned_route (...)` as above. No change to `track`, `location_log` or `observer_trip`. Migration verified on a copy of the device DB before shipping (see IMPLEMENTATION-PLAN slice).

### Docs impacted
PRD (FR-17 amended, FR-21 new, BR-15 amended, BR-16 new), ARCHITECTURE (ROUTE section, data model, DB v15), UI-SPEC (new §3j + Handoff Instructions), IMPLEMENTATION-PLAN (§1, §4 slice, §6, Next Step), IMPLEMENTATION-ISSUES (ToS gate), adr/README, adr/016 and adr/017 (pointers), DOCUMENT-CONTROL.

## Alternatives Considered

1. **Add the geometry as a column on `track`.**
   - Pros: no new table or DAO.
   - Cons: list queries load whole `track` rows, and `track` already holds a large `pathPoints` string; CursorWindow and list-performance risk. Rejected.
2. **Store every re-optimized route.**
   - Pros: full history of what the navigation aid advised.
   - Cons: storage growth and no product need; the optimized route is a transient aid. Rejected.
3. **Store only derived numbers (planned km, duration).**
   - Pros: tiny, no geometry licence question.
   - Cons: cannot redraw the plan on the details map and cannot compute max deviation. Rejected.
4. **Keep display-only (status quo).**
   - Pros: no migration, no licence or privacy exposure.
   - Cons: plan lost on process death; no post-trip comparison. Rejected by the owner (remains the fallback if the ToS gate fails).

## Related ADRs

- **ADR-016** (Order Route Overlay) — defines planned and runtime routes; its "not persisted" rule is amended by this ADR for the planned route only.
- **ADR-017** (Order Route Provider: OpenRouteService) — provider, attribution, accepted-point origin (Amendment 2026-10-11); the `originSource` column records which origin was used.
- **ADR-010** (Self-learning Local Route Store) — the saved planned route is never ingested into it.
- **ADR-009** (Dual-mode Track Navigation) — `kind='NAV'` reserved for its trips.
- **ADR-023** (Order Trip Header Card and Geo Snapshot) — pickup/drop coordinates already stored; same no-FK convention family.
- **ADR-025** (Order Trip Status) — latest migration before this one (v14).

## References

- PRD FR-17, FR-21, BR-15, BR-16
- ARCHITECTURE ROUTE section and Database Schema
- UI-SPEC §3j
- IMPLEMENTATION-PLAN §4 slice "Saved Planned Route + Planned-vs-Actual Comparison (ADR-026)"
- IMPLEMENTATION-ISSUES (ToS gate issue)
- openrouteservice / HeiGIT Terms of Service: https://account.heigit.org/info/tos

## Status Summary

- **ADR Accepted:** 2026-10-11 — conditional on the ToS gate below.
- **Docs Updated:** 2026-10-11 — PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN, IMPLEMENTATION-ISSUES, adr/README, adr/016, adr/017, DOCUMENT-CONTROL.
- **Code Implementation:** Not started. Blocked on (1) UI design handoff approval (AGENTS §12) and (2) the ToS gate.
- **Design Handoff:** UI-SPEC §3j written; handoff to Claude Design / Google Stitch pending.

### Open items / gates

- [ ] **ORS / HeiGIT ToS:** project owner to confirm whether storing returned route geometry is permitted and record the finding here. Not cleared as of 2026-10-11. If not permitted: store nothing from ORS and revisit.
- [ ] UI design handoff (UI-SPEC §3j) approved.
- [ ] Privacy/backup risk (`allowBackup="true"`) acknowledged by the owner; mitigation (exclude DB from backup) is a separate decision, not part of this ADR.
