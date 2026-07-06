# ADR-005: Observer Full-Text Search via Room @Fts4 External-Content (not FTS5)

**Status:** Proposed  
**Date:** 2026-07-06  
**Decided By:** Claude Code (implementation) + project owner

## Context

Observer Phase 3 adds text + package filtering over the `observer_event` table (Room 2.5.2, `androidx.room`; minSdk 28). The Phase 3 planning stub loosely said "FTS5 support." Two SQLite full-text options exist:

- **FTS4** — available on Android's platform (system) SQLite across all supported API levels; no bundled SQLite driver required.
- **FTS5** — per the Room reference, FTS5 availability "is based on the driver used for the database; for Android specifically the `androidx.sqlite.driver.bundled.BundledSQLiteDriver` supports FTS5." The project uses the default platform SQLite (no bundled driver), so FTS5 is not guaranteed across devices/OEMs.

Both Room `@Fts4` and `@Fts5` support `contentEntity` "external content" mode, in which the FTS table indexes but does not duplicate the source rows, and Room auto-generates triggers to keep the FTS table in sync with the content table. Room drops these triggers before running migrations and recreates them afterward.

## Decision

Use **Room `@Fts4` in external-content mode** over `observer_event`:

1. Add `ObservedEventFtsEntity` annotated `@Fts4(contentEntity = ObservedEventEntity::class)` indexing the searchable text columns (`packageName`, `activityName`, `textSummary`).
2. Register it on `TrackDatabase`, bump the DB version 5 to 6, and add an inline `MIGRATION_5_6` that creates the FTS virtual table (Room-generated `CREATE VIRTUAL TABLE ... USING fts4` plus the external-content sync triggers), matching the existing inline-migration convention in `TrackDatabase.kt`.
3. Filtered queries use `observer_event_fts MATCH :query` joined back to `observer_event`, combined with package-set filtering and the existing `lastSeenAt` cursor pagination. Empty filters fall back to the existing non-FTS queries.

## Consequences

### Positive
- Full-text search works on all supported devices using platform SQLite; no bundled SQLite driver dependency added (avoids APK size + build changes).
- External-content mode avoids duplicating event text; Room manages sync triggers automatically.
- Keeps the existing cursor-pagination and live-new-events design intact; filtering layers on top.

### Negative
- FTS4 lacks some FTS5 features (e.g., BM25 ranking, richer tokenizer options); acceptable for substring/keyword filtering of an event feed.
- The 5→6 migration must create the FTS table and rebuild the index from existing rows so pre-existing events are searchable (Room drops/recreates external-content triggers around migrations).

## Alternatives Considered
1. **FTS5 (with BundledSQLiteDriver):** richer ranking/tokenizers, but requires adding and wiring the bundled SQLite driver — a larger dependency/build change not justified for feed filtering.
   - Pros: BM25 ranking, better tokenizers.
   - Cons: extra dependency + build/config change; APK size. Rejected.
2. **Plain `LIKE '%term%'` (no FTS):** simplest, no schema change.
   - Pros: zero migration.
   - Cons: full table scans, no tokenization; degrades as the event log grows. Rejected as primary approach (may remain a fallback for very short queries).

## Related ADRs
- [[003-observer-navigation-placement]]
- [[004-obd-raw-at-io]]

## References
- Room FTS reference: https://developer.android.com/reference/kotlin/androidx/room/Fts4 (external-content `contentEntity`; FTS5 driver caveat)
- `docs/IMPLEMENTATION-PLAN.md` Appendix A (Observer Phase 3 slices)
- `docs/ARCHITECTURE.md` (Observer schema)
- Room 2.5.2; `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObservedEventEntity.kt`
