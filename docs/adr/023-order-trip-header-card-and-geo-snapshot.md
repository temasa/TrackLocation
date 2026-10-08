# ADR-023: Order Trip Header Card — Accepted/Pickup/Drop + Saved Geo Snapshot

**Status:** Accepted
**Date:** 2026-10-08
**Decided By:** Project owner + Claude Code

## Context

ADR-022 introduced an order label and earnings snapshot to order-linked trips in the Trips list, allowing the driver to quickly identify order-linked work at a glance. However, the one-line "Gojek: <pickupName> → <dropName>" label compresses the three distinct locations (where the driver was when the order was accepted, the pickup location, and the drop location) into two names and offers no persistence of geo-coordinates for later offline reference or analysis.

This decision extends ADR-022 with a three-line header card that displays:
1. **ACCEPTED** — the place name where the driver was when the order was accepted (reverse-geocoded from the trip start GPS fix)
2. **PICKUP** — the Gojek pickup name
3. **DROP** — the Gojek drop name

All three fields are persisted on the trip row as name + full address + latitude/longitude pairs, providing a rich geo snapshot independent of the retained `observer_trip` row (which is auto-pruned at 90d/5k per ADR-013). The reverse geocoding of the accepted location is performed once when the trip stops, with fail-soft behavior: if the reverse geocode fails or is offline, the accepted line is omitted but the trip is still saved with the pickup/drop details intact.

---

## Decision

1. **Three-line header display (Trips list, order-linked trips):** When a completed order-linked trip is shown in the Trips list, the row header displays three lines:
   - **ACCEPTED:** place name of the location where the driver was when the order was accepted (reverse-geocoded from `track.startLocationId` GPS fix)
   - **PICKUP:** pickup name from the Gojek order card
   - **DROP:** drop name from the Gojek order card
   
   Names only are displayed; addresses are never shown in the row (they are persisted but kept private). If the accepted name is unavailable (reverse geocode failed, no fix, offline), line 1 is hidden and the card shows only PICKUP and DROP (two lines). The card is a display-only enhancement to ADR-022's one-line label.

2. **Data model:** Add twelve nullable columns to the `track` table for geo-snapshot persistence:
   - `acceptedName TEXT` — place name from reverse-geocoding the trip start GPS fix (one-time attempt)
   - `acceptedAddress TEXT` — full address from the reverse-geocode result
   - `acceptedLat REAL` — latitude of the trip start fix
   - `acceptedLng REAL` — longitude of the trip start fix
   - `pickupName TEXT` — pickup name from the Gojek order card (sourced from `observer_trip`)
   - `pickupAddress TEXT` — full address of the pickup location
   - `pickupLat REAL` — latitude from the geocoding result (cached by OrderRouteController per ADR-016/017)
   - `pickupLng REAL` — longitude from the geocoding result
   - `dropName TEXT` — drop name from the Gojek order card
   - `dropAddress TEXT` — full address of the drop location
   - `dropLat REAL` — latitude from the geocoding result
   - `dropLng REAL` — longitude from the geocoding result
   
   All twelve columns are nullable. They are captured when the trip is persisted at stop; for order-linked trips with complete Gojek data, they are backfilled from the order card and reverse-geocode result.

3. **Schema migration:** Room database version 11 → 12 via **MIGRATION_11_12** (inline in `TrackDatabase.kt`):
   ```sql
   ALTER TABLE track ADD COLUMN acceptedName TEXT
   ALTER TABLE track ADD COLUMN acceptedAddress TEXT
   ALTER TABLE track ADD COLUMN acceptedLat REAL
   ALTER TABLE track ADD COLUMN acceptedLng REAL
   ALTER TABLE track ADD COLUMN pickupName TEXT
   ALTER TABLE track ADD COLUMN pickupAddress TEXT
   ALTER TABLE track ADD COLUMN pickupLat REAL
   ALTER TABLE track ADD COLUMN pickupLng REAL
   ALTER TABLE track ADD COLUMN dropName TEXT
   ALTER TABLE track ADD COLUMN dropAddress TEXT
   ALTER TABLE track ADD COLUMN dropLat REAL
   ALTER TABLE track ADD COLUMN dropLng REAL
   ```
   No default values; all columns are nullable.

4. **Accepted location capture:** When the trip ends and is persisted by `ShareViewModel.onTripCtaTap()`, the accepted location is captured once:
   - Source: trip `startLocationId` GPS fix from the `location_log` row (latitude, longitude, timestamp).
   - Action: call OpenRouteService Reverse Geocoding API (`/geocode/reverse?lat=<lat>&lon=<lon>&size=1`) once per trip.
   - Retry policy: one automatic retry on failure (timeout, rate limit, invalid input).
   - Failure is fail-soft: if the reverse geocode fails or times out, the accepted name/address remain `NULL`; the trip is saved immediately with its pickup/drop details intact (trip save never waits on or depends on the geocoding network call).
   - Attribution: reuse the existing ORS attribution '© openrouteservice.org | © OpenStreetMap contributors' (ADR-017).

5. **Pickup and drop location capture:** Pickup and drop names come from the Gojek `observer_trip` row (already available at trip end per ADR-022). The pickup and drop addresses and coordinates are sourced from the OrderRouteController's cached geocoding results (ADR-016/017):
   - Pickup/drop lat/lng are the result of the ORS Geocoding API call made when the order entered the pickup phase (AdapterOrderRouteController per ADR-017 §4 geocoding ladder).
   - If the order's geocoding failed entirely (all four fallback steps exhausted, fail-soft per ADR-017), pickup/drop lat/lng remain `NULL` for that trip.
   - Pickup/drop address is cached from the last successful geocode result; if no geocoding attempt succeeded, address remains `NULL`.
   - No additional geocoding calls are added; pickup/drop geo data is a read of the already-cached OrderRouteController state at trip stop.

6. **Backfill (v11→v12 migration, best-effort):** For existing `track` rows (pre-ADR-023) with `orderLabel NOT NULL` (i.e., order-linked trips), backfill is performed:
   - **pickupName/pickupAddress/dropName/dropAddress:** Match each order-linked `track` row against `observer_trip` rows using the same window rule as ADR-022 (matching window: `firstSeenAt − 2 min` to `lastSeenAt + 5 min`, exactly one unambiguous match). Copy pickup/drop names and addresses from the matched `observer_trip` row into the `track` row. If multiple matches or no match or the order row was pruned, leave the fields `NULL` (no false linking).
   - **acceptedLat/acceptedLng:** Backfill from the `location_log` row referenced by the `track.startLocationId` when available. Copy lat/lng from that location row.
   - **acceptedName/acceptedAddress:** Do not backfill (no network call in migration). Leave `NULL` for all v11→v12 rows.
   - Rationale: addresses and lat/lng are offline-available data (addresses stored in `observer_trip`, location coordinates in `location_log`); accepted geocoding requires the network and is acceptable to defer (captured on demand for new trips). This minimizes migration time and avoids indefinite waits on network calls.

7. **Display logic (Trips list, TrackItemRow):** When rendering an order-linked trip row (with `orderLabel NOT NULL`):
   - If `acceptedName != NULL`, display line 1: "ACCEPTED  <acceptedName>"
   - Always display line 2: "PICKUP  <pickupName>"
   - Always display line 3: "DROP  <dropName>"
   - If `acceptedName == NULL` (reverse geocode failed, offline, or v11→v12 backfilled), show two-line header (PICKUP and DROP only).
   - Three-line or two-line header replaces the one-line label from ADR-022; the price/net cell remains unchanged.

8. **Privacy & control:**
   - The driver's location at trip start (accepted GPS fix) is sent to OpenRouteService for reverse geocoding for the first time in this feature (see ADR-017 amendment below).
   - Pickup/drop addresses were already persisted by ADR-013 (observer_trip); this decision adds lat/lng coordinates and extends their storage to the trip row.
   - Customer name and phone numbers are still never stored (ADR-013 rule preserved).
   - All geo data is stored in user-deletable `track` rows; deleting a trip deletes all associated geo snapshot.
   - No remote sync in current scope; local-only persistence.

9. **No foreign key:** Consistent with ADR-013 and ADR-022, there are no foreign keys from `track` to `observer_trip`. The header names and addresses are snapshots copied at save time; later pruning of the order row does not affect the persisted trip record.

---

## Consequences

### Positive
- Driver can see three distinct locations (accepted, pickup, drop) at a glance in the Trips list, enriching the trip record.
- Geo coordinates are persisted for offline reference, analysis, or future features (e.g., map-view playback with address labels).
- Fail-soft approach ensures trips are always saved even if reverse geocoding fails; the driver is never blocked on network latency.
- Snapshot approach avoids foreign-key dependency and makes the trip data durable across order-row pruning.
- Pickup/drop geocoding is reused from OrderRouteController (no additional API calls for new trips).

### Negative / Accepted Risks
- **(a) Reverse-geocode accuracy for accepted location:** Street-level pins can be metres off on long roads (same as ADR-017 pickup/drop accuracy). The accepted name may be imprecise (e.g., "Jalan Cipete Raya" instead of the specific street number). **Accepted:** One reverse-geocode attempt per trip is sufficient; the driver can see the coordinates and refer to a map if precision is critical.
  
- **(b) Accepted geocoding on network failure:** If the driver is offline when the trip ends, the accepted name/address remain `NULL` and line 1 is hidden. **Accepted:** Fail-soft is preferred (trip always saves). The driver's trip still shows PICKUP and DROP. Future enhancement: background sync could attempt the reverse-geocode later once online.

- **(c) Backfill without accepted geocoding:** v11→v12 migration does not populate `acceptedName`/`acceptedAddress` for old trips. **Accepted:** Accepted geocoding requires the network and is not suitable for a migration step. New trips will have accepted locations once the feature ships.

- **(d) Third-party geocoding not cached:** Unlike pickup/drop, accepted location reverse-geocoding happens at trip stop, not at geocoding time. No caching in OrderRouteController needed. **Accepted:** Trip stop is the only time accepted location is known; a single reverse-geocode per trip is minimal overhead.

---

## Alternatives Considered

1. **Reverse-geocode accepted location in the background after trip saves:** Start a background job to attempt reverse geocoding after the trip persists (so trip save doesn't block). **Rejected:** Async complexity; if the job never completes or crashes, the trip remains missing its accepted name. Fail-soft within the trip-save transaction is simpler and guarantees either full success or all-but-accepted.

2. **Compute accepted name at display time from persisted lat/lng:** Store `acceptedLat`/`acceptedLng` but fetch the name on-demand when the Trips list is rendered. **Rejected:** Network call at display time creates UI jank and data inconsistency (two renders of the same row with different names). Pre-computed name is more reliable.

3. **Cache all pickup/drop geocoding results on disk during the order phase (not just in-memory):** ADR-017 caches results in-memory in OrderRouteController. **Rejected:** OrderRouteController exists for the trip lifetime; caching to disk adds complexity and is not needed for the order-trip use case. In-memory cache suffices if the order is dropped/dismissed before the app terminates; on process death the order state is lost anyway (acceptable per ADR-015 in-memory orderOwnsTrip).

4. **Prompt the user to confirm accepted location capture:** "Reverse geocoding your location now — allow?" before geocoding. **Rejected:** Adds UX friction; fail-soft async approach is better (user is already stopping the trip; one-shot network call is acceptable).

---

## Related ADRs

- **ADR-013** (Observer Trip Extraction) — Gojek extraction without FK to trips; the upstream data source for pickup/drop names and addresses.
- **ADR-015** (Gojek Order Lifecycle Drives the Trip) — auto-start/auto-end trip; provides the trip endpoint where accepted location is captured.
- **ADR-022** (Order Trip Label in Trips List) — one-line label and earnings snapshot; this ADR extends the header to three lines + geo snapshot.
- **ADR-017** (Order Route Provider: OpenRouteService) — establishes ORS as the routing/geocoding provider; this ADR reuses ORS reverse-geocoding for accepted location.
- **ADR-016** (Gojek Order Route Overlay) — caches pickup/drop geocoding results; this ADR reads that cache at trip stop.

---

## References

- PRD.md — FR-20 (order trip label in Trips list, now extended to three-line header + geo snapshot), FR-17 (privacy refinement: driver coordinates sent to ORS reverse-geocode)
- ARCHITECTURE.md — `track` schema v12, MIGRATION_11_12, backfill window definition, OrderRouteController caching
- UI-SPEC.md — Trips list row (TrackItemRow) for order trips: three-line header (ACCEPTED/PICKUP/DROP) display
- IMPLEMENTATION-PLAN.md — new slice "Order Trip Header Card and Geo Snapshot (ADR-023)" with verification and implementation steps
- ADR-017 (amended) — note that reverse-geocoding now sends driver coordinates to ORS

---

**Amends:** ADR-022 (presentation section: note the three-line header supersedes the one-line label in the row display; label and earnings still apply), ADR-017 (privacy: driver coordinates sent to ORS reverse-geocode for accepted location).

**Supersedes:** None.

---

## Amendment 2026-10-08: One-time Accepted-Place Backfill for Migrated Order Trips

**Context:** Device check post-v12 migration showed that migrated order trips (v11→v12) have `acceptedLat`/`acceptedLng` coordinates (6 of 7 test trips; one trip lacks `startLocationId`) but `acceptedName` and `acceptedAddress` remain `NULL`. The ACCEPTED header line is therefore hidden for them, showing only PICKUP and DROP (two-line header). This amendment defines a one-time background backfill process to reverse-geocode the accepted location for these old trips once the app is online, so the ACCEPTED line eventually appears for backfilled rows.

**Decision:**

1. **Backfill process (run from ShareViewModel init):** When `ShareViewModel` is created for the first time in the app process (singleton instance), a one-time background backfill coroutine is launched. It scans the `track` table for candidates and processes them with no UI blocking.

2. **Candidate selection:** `track` rows matching all of:
   - `orderLabel NOT NULL` (order-linked trip)
   - `acceptedName IS NULL AND acceptedAddress IS NULL` (not yet reverse-geocoded)
   
   Candidates are ordered newest-first (by `id` descending). Maximum 20 trips processed per backfill run (per app launch).

3. **Point resolution:** For each candidate, the accepted location point is:
   - `(acceptedLat, acceptedLng)` when both are present
   - Fallback: the first point from the trip's stored `pathPoints` string (parse, extract lat/lng, and persist those as `acceptedLat`/`acceptedLng`)
   - If no point available, skip the row (leave `acceptedName`/`acceptedAddress` `NULL`)

4. **Reverse-geocoding:** Call OpenRouteService reverse-geocode API on the point (same ORS client/key as new trips). Timeout: 8 seconds. Retry: one automatic retry on failure. Success: write `acceptedName` and `acceptedAddress` to the `track` row. Failure: leave both `NULL`; the row is retried on the next app launch. Never block trip data or UI.

5. **Throttling:** Sequential processing with ~1.2 second delays between API calls (gentle rate-limiting to avoid ORS quota strain).

6. **Privacy:** The backfill sends the start coordinates of PAST order trips (user-approved GPS data already in the local log) to OpenRouteService once each. No customer data. Same privacy treatment as new trips (FR-20/ADR-017 reverse-geocoding refinement).

7. **Schema:** No new migration; the `acceptedLat`/`acceptedLng`/`acceptedName`/`acceptedAddress` columns already exist from ADR-023 main decision.

8. **Display impact:** As rows are backfilled on the server side (after each API success), the Trips list re-renders; the ACCEPTED line appears for those rows on subsequent view refreshes. Rows with no backfill candidate remain two-line (PICKUP/DROP only).

9. **Correction to ADR-023 main decision:** The statement in ADR-023 §6 "Do not backfill" for `acceptedName`/`acceptedAddress` is amended: old trips do not get backfilled in the migration itself (v11→v12), but they do get a separate one-time background backfill after launch, making the statement "Leave `NULL` for all v11→v12 rows" accurate for the migration but now followed by "Backfill is deferred to a post-launch background process" (this amendment).

**Consequences:**

- **Positive:** Old order trips gradually gain the ACCEPTED location name over time (after the app resumes online); the three-line header appears for backfilled rows, enriching the Trips list display retrospectively.
- **Negative/Accepted:** (a) Backfill is async and can take several app launches to complete all rows (acceptable; retry on next launch). (b) If ORS is persistently offline, rows remain with `NULL` accepted names (acceptable; fail-soft, trip data unaffected). (c) No new schema or migration (acceptable; uses existing v12 columns).

---
