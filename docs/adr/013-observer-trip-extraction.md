# ADR-013: Observer Trip Extraction (Gojek pickup/drop, device-only)

**Status:** Accepted
**Date:** 2026-10-05
**Decided By:** Project owner + Claude Code

## Context

The Gojek driver app (`com.gojek.partner`, `com.gojek.driver.home.view.HomeActivity`) displays pickup and drop location details on order cards within its UI. These details (pickup place name + address; drop name + address; payment method; earnings; the card also shows the customer name and rating, which are deliberately not stored) are already captured by the Observer accessibility service as tree snapshots in `observer_event.treeSnapshot` (JSON array of text nodes). Currently, this data is never extracted — only raw event storage and inspection exist.

A demand exists for device-local trip extraction: parse the order card structure from captured Gojek UI snapshots, deduplicate trips by pickup+drop address pairs, persist phase (pickup/drop-only/finished), and retain independently from the 7-day/50,000-row observer_event pruning.

Per ARCHITECTURE (Observer events have no foreign keys to trips/sessions), `observer_trip` has no FKs either. Observer data is derived for inspection only; it does not drive location recording or trip lifecycle decisions.

## Decision

1. **Parser:** a pure Kotlin function (`treeSnapshot: List<Map> → OrderCard?`) post-processes stored tree snapshots. It collects text nodes in order, requires delimiter texts `Laporkan masalah map` and `Dibayar pakai` to signal a valid order card (partial snapshots from TYPE_WINDOW_CONTENT_CHANGED events are common; skip them), and extracts:
   - Phase (from button text): `Udah di titik jemput` (pickup phase; both pickup and drop shown), `Sampai tujuan` (drop-only phase; drop shown only), `Selesai` (trip finished; earnings shown).
   - **Pickup phase:** pickup name/address are the two text nodes before the first divider; drop name/address are the two nodes after it.
   - **Drop-only phase:** drop name/address are the two nodes before the divider (pickup no longer shown).
   - **Finished phase:** only earnings shown (phase marker).
   - Payment (text after `Dibayar pakai`, e.g., GoPay/Kartu).
   - Earnings (text after `Pendapatan`, parsed as integer; e.g., `Rp36.400` → 36400).

2. **App-specific rule table:** UI strings are localized and subject to change. Gojek parsing rules live in a small per-app configuration table keyed by package name (`com.gojek.partner`), **not** hardcoded in parser logic. Allows future rule additions for other apps without altering the parser.

3. **Storage (migration numbering):** `TrackDatabase.kt` is v9 and ADR-010 has not shipped, so this work takes **MIGRATION_9_10 (DB→v10)**; ADR-010's `known_segment` migration will then be numbered 10→11 when it ships. Numbering follows ship order; the two swap if ADR-010 ships first.
   New Room table `observer_trip` (pickupName, pickupAddress, dropName, dropAddress, payment, earningsRp, phase, firstSeenAt, lastSeenAt), separate from `observer_event`. Retention: 90 days OR 5,000 rows, whichever is smaller; automatic pruning; no user-facing delete option (consistent with FR-05, Observer history not user-deletable).

4. **Deduplication:** key a trip by `(pickupAddress, dropAddress)`. Update the row's phase and lastSeenAt each time the same order appears in a new event. Persist pickup+drop when first seen in pickup phase; skip premature drop-only snapshots from earlier resets.

5. **No links to sessions/trips:** `observer_trip` has **no foreign keys** to `recording_session` or `track`. Observer data is device-local, inspection-only, and decoupled from the canonical location log (consistent with the ARCHITECTURE "no foreign keys" rule).

6. **No PII stored:** customer name, rating, and phone numbers are **never** stored, even if visible in the captured tree snapshot.

7. **Extraction trigger:** run the parser post-snapshot-write, only for `com.gojek.partner`. Run the parser in the service's write path right after the `observer_event` row is persisted (in `onAccessibilityEvent`), only for `com.gojek.partner`. Parser and rule table remain pure Kotlin outside the service class.

8. **Gojek-only scope:** Gojek is the only supported app for now. Future multi-app support is out of current scope (documented in PRD §8 Out-of-Scope).

## Consequences

### Positive
- Gojek driver order context (pickup/drop locations, payment, earnings) is automatically extracted and retained on-device for inspection without remote sync or user-facing logic.
- Deduplication by address keeps trip storage bounded (5,000-row cap).
- Pure Kotlin parser decouples extraction from Android service lifecycle; testable and reusable.
- App-specific rule table allows future multi-app expansion without parser changes.
- Retention lifecycle is independent; Observer event pruning does not delete trip records.

### Negative / accepted risks
- **Limited to tree snapshots:** extraction depends on visible UI text in captured accessibility snapshots. If Gojek's UI changes (button text, card layout, node order), the parser may silently return null (skipping that event). Mitigation: update the rule table when Gojek UI strings change.
- **Deduplication latency:** a trip is first stored only when pickup phase is detected. Drop-only transitions (which may appear first if the capture starts after pickup) are skipped until the pickup phase re-appears (when the driver scrolls or the session continues). Accepted: users can manually inspect the full event history if needed.
- **No cascading to trips/sessions:** the extracted trip is separate from the canonical trip lifecycle. Drivers do not see Gojek orders linked to TrackLocation trips on the UI (out of current scope; could be added in a later UI feature).

## Alternatives Considered

1. **Put app-specific parsing logic inside the ObserverAccessibilityService class:** rejected because it couples Gojek-specific logic to the accessibility-service capture path, increasing complexity and risk to GPS/Observer reliability (per the ARCHITECTURE rule that Observer/OBD must not degrade GPS reliability). The chosen design only calls an external pure function from the service's write path.

2. **Store parsed fields in observer_event** (no separate table): rejected because `observer_event` is pruned at 7 days / 50,000 rows, and Gojek trips should persist at 90 days / 5,000 rows (different lifecycle). Separate retention is intentional.

3. **Generic multi-app rule engine** (condition trees, dynamic config): rejected as out of scope. Gojek-only parsing with a simple per-app table is sufficient for now.

## Out of Scope (follow-ups)

- Trip extraction for apps other than Gojek (stored in PRD §8 Out-of-Scope current).
- UI feature to link Gojek orders to TrackLocation trips (would require deduplication by time overlap or user selection).
- Cloud sync of `observer_trip` data (device-only per this ADR).
- Customer-facing PII exposure risk analysis (design assumption: Observer is for developer/support inspection only, not shared with end users).

## Related ADRs

- Related: ADR-005 (Observer FTS index) — the FTS index covers `observer_event` only, not `observer_trip`.

## References

- ARCHITECTURE.md — no foreign keys rule; Observer retention.
- PRD.md — FR-15 (new); §8 Out-of-Scope (trip extraction for other apps).
- Device data: SM-G965F, 2026-10-05: Gojek has 2 observed event types (TYPE_WINDOW_STATE_CHANGED 69 rows, TYPE_WINDOW_CONTENT_CHANGED 613 rows); 112 rows contain order-card text patterns; only HomeActivity reports real activity name, CONTENT_CHANGED rows report widget class names because they record the changed view's class.
