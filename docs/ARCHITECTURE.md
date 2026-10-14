---
name: ARCHITECTURE.md
path: docs/ARCHITECTURE.md
description: High-level system architecture, domain model, and design decisions — TrackLocation
---

# System Architecture
## TrackLocation

**Document Version:** 0.42
**Status:** Active (migrated from product-spec.md data/architecture rules)
**Last Updated:** 2026-10-14
**Owner:** Tech Lead
**Controlled By:** `docs/DOCUMENT-CONTROL.md`

> Migrated 2026-06-15 from data-model/behavior rules in `docs/product-spec.md` and CR data-model impacts during create-project schema adoption.

---

## 1. System Overview

TrackLocation is a single-module Android app (Jetpack Compose + Material 3, package `com.kolee.tracklocation`) targeting phones. It records GPS continuously via a foreground `TrackingService`, persists everything locally in Room, and exposes four bottom-nav destinations (`Session / List / Track / Settings`). Two tools live under Settings: an accessibility-event Observer and an ELM327 OBD-II telemetry feature, each backed by its own service. The architecture is local-first and sync-ready (remote access is abstracted behind a `RemoteDataSource` for future phases).

---

## 2. Domain Model

Core entities and their relationships (Room):

### Entity Relationships

```
LocationEntity (location_log)   ← canonical GPS points (source of truth); stationary fixes collapse to one dwell anchor (ADR-006)
  └── referenced by id ranges

SessionEntity (recording_session)
  ├── startedAt / endedAt
  └── resolves path from a location_log range (ON→OFF period)

TrackEntity (trip)
  ├── startLocationId → LocationEntity
  ├── endLocationId   → LocationEntity
  └── distance, duration, timestamp

ObservedEventEntity (observer_event)
  ├── packageName, eventType, activityName
  ├── firstSeenAt, lastSeenAt, repeatCount
  └── textSummary, treeSnapshot, truncationMetadata

AllowlistRuleEntity (allowlist_rule)   ← EXACT / REGEX, packageName-only, case-sensitive

ObserverTripEntity (observer_trip)   ← DERIVED from captured tree snapshots; Gojek-only extraction (ADR-013)
  ├── pickupName, pickupAddress, dropName, dropAddress
  ├── payment (e.g., GoPay/Kartu), earningsRp (integer)
  ├── phase (pickup/drop-only/finished/cancelled; cancelled added ADR-025, string column, no schema change), firstSeenAt, lastSeenAt, handled (marked once the takeover for that order has been processed)
  └── no FKs to trips/sessions; deduplication key = (pickupAddress, dropAddress)

TripPlannedRouteEntity (trip_planned_route)   ← saved planned route per trip, device-only (ADR-026, DB v15; code in f26bb03)
  ├── tripStartedAt (INTEGER PK) = track.timestamp of the owning trip; NO FK (same convention as observer_trip)
  ├── kind (TEXT NOT NULL) — 'ORDER' | 'NAV' (NAV reserved for ADR-009, not implemented)
  ├── geometry (TEXT NOT NULL) — ORS encoded polyline, precision 5, as returned
  ├── distanceM, durationS (INTEGER) — ORS routes[0].summary
  └── originSource (TEXT 'ACCEPTED' | 'FALLBACK_LIVE'), fetchedAt (INTEGER)

ObdSampleEntity (obd_sample)
  ├── timestampMs, rpm, obdSpeedKmh
  ├── fuelRateLph, mafGramsPerSecond, fuelRateSource
  ├── adapterElapsedMs
  └── gpsSpeedKmh   (REAL?, added DB v13, ADR-024 — GPS speed at poll time; covered distance is re-integrated from it)
  (no FK to trip/session — linked by time-window queries)

SessionEntity (recording_session) — OBD accumulator columns
  ├── obdFuelConsumedL       (REAL, default 0.0) — cumulative L consumed this session (added DB v5)
  └── obdCoveredDistanceKm   (REAL, default 0.0) — cumulative km over the same fuel-covered intervals (added DB v13, ADR-024)
  (the former obdGpsDistanceKm column was dropped in v8, ADR-007)

TrackEntity (trip) — OBD accumulator columns
  ├── obdFuelConsumedL       (REAL, default 0.0) — cumulative L consumed this trip (added DB v5)
  └── obdCoveredDistanceKm   (REAL, default 0.0) — km over the same fuel-covered intervals (added DB v13, ADR-024)

TrackEntity (trip) — Gojek order outcome columns (ADR-025)
  ├── orderStatus            (TEXT, nullable) — 'COMPLETED' | 'CANCELLED'; NULL for manual trips (added DB v14; backfilled 'COMPLETED' where orderLabel IS NOT NULL)
  └── orderOffline           (INTEGER, NOT NULL, default 0) — 1 = trip closed offline (cancelled order of any payment type, stopped within 150 m of drop-off; ADR-030); meaningful only with orderStatus = 'COMPLETED' (added DB v14)

FuelPriceEntity (fuel_price)   ← effective-dated price log (added DB v9, ADR-008)
  ├── pricePerLiter (REAL)
  └── effectiveFromMs (Long, @Index) — current price = latest row; completed-trip cost uses the row effective at trip start; a Fill up with a changed price appends a row here (ADR-027)

FuelEventEntity (fuel_event)   ← append-only fuel log for the fuel dock (added DB v16, ADR-027; no FK)
  ├── id (INTEGER PK AUTOINCREMENT)
  ├── kind (TEXT NOT NULL) — 'RESET' | 'FILL_UP'
  ├── litres (REAL NOT NULL) — starting level (RESET) or litres added (FILL_UP)
  ├── pricePerLiter (REAL, nullable) — price entered in Fill up when changed; NULL when the default price was kept
  ├── resetBaselineL (REAL, nullable) — RESET rows only: OBD ledger fuelConsumedL at the moment of the Reset (rev. 1.2 wording: Σ session litres; the ledger value from v17, ADR-027 rev. 1.6); NULL for FILL_UP
  └── createdAtMs (INTEGER NOT NULL)

ObdLedgerEntity (obd_ledger)   ← session-independent OBD running total (added DB v17, ADR-027 rev. 1.6; implemented 2026-10-11, build OK, device checks open; no FK)
  ├── id (INTEGER PK, singleton: one row, id = 1)
  └── fuelConsumedL (REAL NOT NULL, default 0) — cumulative OBD litres: each poll's fuel increment (valid rate 0.1..100 L/h, same gates as ADR-024), including idle (engine on, stationary); one column only
  Written by ObdPollingService on every poll, whether or not a recording session exists and whether or not always-recording is ON (the session accumulators `addObdAccumulator` are unchanged). Seeded by MIGRATION_16_17 from the Σ of the session totals. No `onCreate` seeding: `ObdLedgerDao.addFuel` (abstract DAO, one `@Transaction`) seeds the singleton row if it is missing and adds the increment; reads return 0.0 until the first write.
  Fuel balance (derived, never stored; ADR-027 rev. 1.6): anchor = latest RESET (resetAt = its createdAtMs; resetBaselineL = its baseline); cumulativeNow = obd_ledger.fuelConsumedL; fuelLeft = max(0, anchor.litres + Σ FILL_UP.litres (createdAtMs > resetAt) − max(0, cumulativeNow − anchor.resetBaselineL)); no RESET → `—`. Reads no obd_sample rows, so it survives obd_sample retention pruning. Only OBD-measured litres reduce it (idle included when the OBD rate is valid); OBD disconnected → frozen.
  RANGE (display only, ADR-027 rev. 1.6, window of distance since rev. 1.7) = fuelLeft × ROLLING AVG km/L over the last 1.0 km of moving OBD driving (window covered km ÷ window litres). The window holds per-poll (litres, covered km) pairs for moving intervals only (covered km > 0 and GPS speed ≥ 2 km/h); idle polls do not enter it. It is trimmed while the rest still spans ≥ 1.0 km, kept in memory in `ObdPollingService` (not persisted, cleared on adapter reconnect, lost on restart) and published to the dock through a shared state holder. The average is published only when the window covers ≥ 0.2 km and its litres are > 0.01 L; otherwise the previous published value is held. Before any value has been published since app start RANGE shows `—`. No active session needed. The session average (obdCoveredDistanceKm ÷ obdFuelConsumedL, ADR-024) is unchanged and still shown on the Session card.

KnownSegmentEntity (known_segment)   ← DERIVED from location_log (added DB v10, ADR-010); rebuildable, never source-of-truth
  ├── simplified directed polyline (Douglas–Peucker) + bearingDegrees
  └── indexed by grid cell for road-ahead lookup (CellIndex)
```

### Key Invariants

- The canonical `location_log` is the single source of truth for GPS points (see PRD §12 BR-01/BR-02).
- Sessions and trips are ranges/views over the canonical log; deleting a trip never deletes location rows.
- **Session invariant:** at most one `recording_session` row has `isActive = 1` at any time. Starting always-recording adopts the existing open session rather than creating a duplicate; the launch-time reaper and stop path close open sessions.
- **Recording resumes on restart:** on launch, `MainActivity` reaps only *stale* open sessions (no location point within the ~2-min grace window); a non-stale open session that outlived a force-stop (which `START_STICKY` does not redeliver) is resumed by starting `TrackingService` with `START_RECORDING`, which adopts it — keeping the live service state and the persisted session consistent (no Inactive-card-beside-Active-session mismatch).
- OBD samples and Observer events have **no foreign keys** to trips/sessions; they associate via `samplesBetween(startMs, endMs)` / time windows.
- Observer history is never user-deletable; retention is automatic (7 days / 50,000 rows, whichever is smaller).
- `observer_trip` is retained independently: 90 days / 5,000 rows, whichever is smaller; automatic pruning; no user-facing delete.
- `fuel_event` (ADR-027) is append-only and has no FK. The fuel balance is derived from it and the OBD ledger (`obd_ledger.fuelConsumedL`, compared with the RESET's `resetBaselineL`), never stored as a column. From v17 the ledger, not Σ `recording_session.obdFuelConsumedL`, is the cumulative source (ADR-027 rev. 1.6), so the balance and RANGE do not need an active session.
- `trip_planned_route` (ADR-026) is a separate table (not a `track` column: `track` list queries load whole rows and already carry a large `pathPoints` string). It has no FK; it is deleted with its trip (same DAO transaction path as the `track` delete) plus an app-start orphan sweep (no matching `track.timestamp` AND `fetchedAt` older than 24 h). It is never ingested into the local route store (ADR-010, PRD BR-15/BR-16); the optimized route is never persisted.

---

## 3. Architectural Principles

- **Single source of truth:** canonical location log; sessions/trips derive from it.
- **Local-first, sync-ready:** all data in Room; remote behind `RemoteDataSource` (Neon V1 is a direct datasource; an API backend is the V2 replacement path).
- **Service isolation:** `TrackingService` (GPS), `ObserverAccessibilityService` (events), and `ObdPollingService` (telemetry) are independent; Observer/OBD must not degrade GPS reliability. `TrackingService` must not start/stop `ObdPollingService` (they communicate via intent actions only).
- **Business logic out of Compose:** data access behind DAOs/repositories where practical.

---

## 4. Technology Stack

| Layer | Technology | Rationale |
|---|---|---|
| UI | Jetpack Compose + Material 3 | Accepted product UI direction; compose-pinned at Compose UI 1.2.x (compiler extension 1.2.0) |
| Language | Kotlin 1.7.0 | Project baseline (note: incompatible with `kotlin-obd-api` — see ADR/OBD approach) |
| Persistence | Room (current code DB version 17; `fuel_event` v16 implemented by ADR-027, commit `a197453`; DB v17 with `obd_ledger` implemented by ADR-027 rev. 1.6, 2026-10-11, build OK, device checks open) | Local-first storage with migrations |
| Preferences | DataStore | Observer + OBD settings/state |
| GPS | Foreground `TrackingService` (`foregroundServiceType="location"`) | Continuous always-recording |
| Telemetry | `ObdPollingService` (`foregroundServiceType="connectedDevice"`) + raw AT I/O over Bluetooth RFCOMM/SPP | ELM327 Bluetooth Classic |
| Accessibility | `ObserverAccessibilityService` (`BIND_ACCESSIBILITY_SERVICE`) | Event capture |
| Build | Gradle + JDK `C:\Users\rinal\.jdks\jbr-17.0.14` | Local toolchain |

---

## 5. Key Integration Points

- **Bluetooth Classic (ELM327):** RFCOMM/SPP socket; pairing via Android system settings (`Settings.ACTION_BLUETOOTH_SETTINGS`, Option A). `BLUETOOTH_CONNECT` requested on first Enable toggle (API 31+). Raw AT command I/O (`ATZ/ATE0/ATL0/ATSP0` init, Mode-01 PIDs) — no third-party OBD library.
- **Android AccessibilityService:** user enables in system Accessibility settings; app deep-links and reflects status only.
- **Remote (future):** `RemoteDataSource` abstraction; Neon V1 direct, API backend V2.

### Data flow (OBD telemetry)

```
ELM327 adapter → RFCOMM/SPP socket → ObdPollingService (poll @1–5 Hz, raw AT)
   → ObdUiState (StateFlow)            → Session screen / Trip panel (km/L, RPM, speed)
   → ObdSampleEntity (Room)            ← written only while recording (TrackingService.isAlwaysRecording; ADR-012 — SESSION_ON/OFF now advisory)
```

   `ObdUiState.Connected` additionally carries `sessionFuelConsumedL` and `tripFuelConsumedL` (litres) so the Session card, the Trips active-trip row, and the Gojek order-card trip strip can compute fuel cost = litres × effective fuel price (from `fuel_price` table, FR-12/ADR-008).

### Floating Status Banner (ADR-028, implemented)

```
ObdPollingService.obdUiState (StateFlow)  ──┐
                                             ├→ StatusBannerHost (MainActivity overlay, top)
Observer-enabled helper (shared, 1 s poll    ─┘     ├── OBD chip:      Connecting/Retrying → connecting; Idle/Waiting → not connected; Connected → hidden
while foregrounded; extracted from            │      └── Observer chip: service not enabled → "Observer inactive"; enabled → hidden
ObserverViewModel.isOurServiceEnabled)        └── Collapse state: in-memory only (session), not persisted
```

- **Host:** `StatusBannerHost` is mounted in `MainActivity` over the nav graph, at the top (below the status bar), the same host point as `FuelDockHost` (ADR-027). It is shown only on the Track, Sessions, Trips (List) and Settings routes (route list kept in sync with the fuel dock, UI-SPEC §5b).
- **Classes (as built, `8cd95f8`):** `StatusBannerHost` (`ui/components`) renders the chips and the collapsed icons. `StatusBannerViewModel` is activity-scoped and holds the `collapsed`, `obdProblem` and `observerProblem` StateFlows. Its `startPolling()` loops over `isObserverServiceEnabled` every 1 s; polling starts on ON_START and stops on ON_STOP through a `LifecycleEventObserver`. The observer is used because `lifecycle-runtime-ktx` 2.3.1 has no `repeatOnLifecycle`. The shared check is `observer/ObserverServiceStatus.kt` (`isObserverServiceEnabled(context)`), also used by `ObserverViewModel`.
- **Inputs:** the OBD state is read from `ObdPollingService.obdUiState` (no new OBD flow). The Observer state comes from one shared helper extracted from `ObserverViewModel.isOurServiceEnabled`; the Observer Feed `ServiceBanner` keeps using the same check, unchanged.
- **Polling:** the Observer check re-runs every second only while the app is in the foreground.
- **Taps:** the OBD chip opens `Settings.ACTION_BLUETOOTH_SETTINGS`; the Observer chip opens `Settings.ACTION_ACCESSIBILITY_SETTINGS`. Both are system settings intents; no in-app Bluetooth code is added.
- **State:** the collapsed or expanded choice and the "re-expand on problem change" rule are held in memory only. They reset on app restart and are not written to Room or DataStore.
- **Schema:** no Room change. The banner reads existing state and writes nothing.
- **Service isolation:** the banner does not call `TrackingService`, `ObdPollingService` methods or `ObserverAccessibilityService` methods; it only observes state. GPS tracking is unaffected.

### Session Overlay Banner (ADR-029, planned — not implemented)

A system overlay drawn above other apps while a recording session is active. It is separate from the in-app `StatusBannerHost` (ADR-028), which is not changed.

```
Session state (session active / ended)  ──┐
ProcessLifecycleOwner (app foreground?) ──┤→ SessionOverlayService (foreground service)
                                          │     ├── visibility gate: session active AND app in background
Session aggregator (session-scoped Flow)──┤     ├── WindowManager view, TYPE_APPLICATION_OVERLAY
  ├── fuel left (ADR-027 obd_ledger)      │     │     ├── strip: fuel left · range · fuel cons (avg / inst km/L) · cost · income (gross / net), one row
  ├── range (ADR-027 rolling RANGE)       │     │     └── circle (shrunk): draggable, position in DataStore
  ├── avg / inst km/L (ADR-007, ADR-024)  │     └── permission gate: Settings.canDrawOverlays
  └── per-order cost / gross / net (FR-16)┘
```

- **Permission:** `SYSTEM_ALERT_WINDOW` ("Display over other apps"). `Settings.canDrawOverlays` is checked before the view is added. The app asks once, from Settings or on the first session (ADR-029 Decision 6), with a deep link to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. If the permission is not granted, no overlay is added and tracking and the in-app UI are unchanged.
- **Service and window:** a foreground service hosts the overlay view through `WindowManager` with `TYPE_APPLICATION_OVERLAY`. It starts when a session becomes active and stops when the session ends, and the view is removed at that point. It needs a notification channel (name and wording not yet decided, ADR-029 open items). The service does not call or change `TrackingService`.
- **Visibility gate:** the overlay is visible only while a session is active AND the app process is in the background. `ProcessLifecycleOwner` ON_START hides it and ON_STOP shows it. No `UsageStats` permission is used.
- **Session aggregator:** a session-scoped flow that reads existing data only and adds no store. Fuel left and RANGE come from the session-independent OBD ledger and the rolling-distance RANGE (ADR-027). Consumption comes from the session average and instant km/L (ADR-007, ADR-024). Per order, over all orders of the current session: cost = trip litres × pump price (the same rule as FR-16 in `GojekOrderCard`), gross = `earningsRp`, net = gross − cost. An order that lacks OBD litres or earnings is excluded from the totals and the strip shows a **partial** flag. A negative net is shown as a loss.
- **Surface and text:** the strip surface is translucent and its text uses halo text for contrast (UI-SPEC §5d). This is a pure UI styling choice: the overlay does not capture or sample the screen behind it, and no new component is added.
- **Circle position:** the shrunk circle's x and y are persisted in **DataStore Preferences**. They are clamped to the screen bounds on read and after a rotation or screen-size change, so the circle stays reachable. Tapping the circle restores the strip at the same place.
- **Fill-up action (owner decision 2026-10-13):** the strip has a **Fill-up** action that opens an inline form inside the overlay: litres (required, greater than 0) and price (optional, pre-filled with the current effective price). **Save** calls the existing ADR-027 fill-up use case, which records the event in the session-independent OBD ledger. A changed price calls the existing ADR-008 price save path, which appends one effective-now `fuel_price` row; an unchanged price writes no row. Invalid litres are rejected in the form and nothing is written. The overlay adds no store, schema, repository or permission beyond `SYSTEM_ALERT_WINDOW`.
- **Focusable toggle:** the overlay view is normally `FLAG_NOT_FOCUSABLE`, so touches pass through to the app below and the keyboard is not needed. While the Fill-up form is open, the service clears `FLAG_NOT_FOCUSABLE` on the same view with `WindowManager.updateViewLayout`, so the soft keyboard can receive input. The flag is set again when the form closes by Cancel, when Save succeeds, when the strip is shrunk, and when the view is removed (session end) or hidden by the visibility gate. The shrunk circle is never focusable.
- **Form placement:** the keyboard does not resize an overlay window (`adjustResize` does not apply to `TYPE_APPLICATION_OVERLAY`), so the form is top-anchored in the upper part of the screen and kept short enough to stay clear of the soft keyboard. The keyboard's top edge is not known in advance; the exact layout is decided at implementation and confirmed on device (plan How to Verify). If a top-anchored form still cannot stay clear of the keyboard on the test device, raise a blocker (AGENTS §9).
- **Reuse and live update:** no new store. The strip's fuel left and range are read from the same ledger flow as the session aggregator, so a saved fill-up updates both values immediately, without reopening the overlay. The event is also visible in the in-app Fuel Dock history, because both paths write to the same ledger.
- **Schema: none.** No Room entity, no DAO, no migration (ADR-029 Decision 7). The overlay is display-only except for the Fill-up action, whose only writes are the fill-up event and the optional price row through the existing ADR-027 and ADR-008 paths. It does not write to the canonical location log or the sessions/trips model. PRD §12 locked rules are not changed.
- **Design:** the visual spec is UI-SPEC §5d; the `ui-design.pen` frames are pending (no external handoff).
- **Relation to `StatusBannerHost` (ADR-028):** two different surfaces. `StatusBannerHost` is in-app, mounted in `MainActivity`, and disappears when TrackLocation leaves the foreground. The session overlay is a system window that is shown only when TrackLocation is in the background. They do not share a view or a host.

### Gojek Order-Card Takeover Flow (ADR-014, amended by ADR-015, extended by ADR-016)

```
ObserverAccessibilityService (write path)
   → after observer_event insert & observer_trip upsert

TRIP START (auto-start at Taken):
   → Gojek parser yields complete OrderCard (pickup+drop+payment+earnings)
   → emit "order ready" signal (one-shot per (pickupAddress, dropAddress))
     → the Observer service starts MainActivity (FLAG_ACTIVITY_NEW_TASK + an extra selecting the Track screen)
        → foreground launch from the background: to be verified on Android 10
   → order-ready state is derived from the persisted observer_trip row (survives the activity being created after the signal)
     → ShareViewModel observes it
     → if no trip is live, send START_TRIP
     → if a trip is live, keep it and set in-memory flag orderOwnsTrip=true
     → mark row handled

TRIP END (auto-end at Selesai/Cleared/Dismissed; cancel never auto-stops, per ADR-030 amending ADR-025; early finish far from the drop keeps recording, per ADR-031):
   → Gojek parser recognizes terminal states:
     (a) Selesai screen = "Selesai" with earnings/summary (normal completion)
     (b) Gojek home screen = all 4 nav texts present (Beranda, Pendapatan, Swadaya, Pesan)
     (c) cancel message = "Oke, sip" + text containing "nge-cancel" → phase=CANCELLED (distinct from FINISHED)
   → (a) and (b) yield OrderCard with phase=FINISHED; (c) yields phase=CANCELLED
     → OrderTripRecorder marks the latest open row (FINISHED or CANCELLED)
     → ShareViewModel observes latestOrderFlow
     → when a terminal phase (FINISHED or CANCELLED) and orderOwnsTrip=true and a trip is live, decide by status:
        - FINISHED, drop coordinates resolved, within 150 m of drop (DROP_OFF_RADIUS_M) → stopActiveTrip(status=COMPLETED, offline=false) (unchanged)
        - FINISHED, drop coordinates resolved, farther than 150 m → do NOT stop; keep recording as an early-finish pending trip (ADR-031); show one drop-off snackbar when first within 150 m ("Order finished in Gojek. You're at the drop-off. Stop trip?"; Track screen only; action "Stop")
        - FINISHED, drop coordinates unresolved → do NOT stop; no snackbar; the driver ends the trip manually
     → early-finish pending state (ADR-031, in memory only): pending order id + drop coordinates or null; generalizes the cancel pending state (cancelledPendingId / cancelledDrop / dropWatchJob / dropOffChannel); the drop-off event carries a kind (CANCELLED or FINISHED_EARLY) so TrackScreen picks the snackbar wording; the existing drop-off proximity watch is reused; while a kept-open trip exists, the order-end collector ignores rows other than the pending order's id
     → a new order accepted (Taken) while a kept-open trip is recording is ignored for trip purposes: no trip start, no adoption of the new order
     → manual stop of a kept-open trip (Stop CTA, snackbar Stop, order card Stop trip): status=COMPLETED; orderOffline=true only if within 150 m of drop coordinates (ADR-031)
        - CANCELLED, any payment type, drop coordinates resolved → do NOT stop; keep recording; show one drop-off snackbar when within 150 m of drop coordinates (Track screen only; action "Stop")
        - CANCELLED, any payment type, drop coordinates unresolved → do NOT stop; no snackbar; stop later saves CANCELLED
     → stop of a cancelled trip (snackbar Stop or Stop CTA): within 150 m of drop → COMPLETED + orderOffline=true; otherwise CANCELLED + orderOffline=false (ADR-030; payment is not read)
     → stopActiveTrip(): persist the trip (TrackEntity via insertTrack, with orderStatus/orderOffline), then send STOP_TRIP
        (TrackingService.stopTrip() only stops trip recording and the timer; always-recording unaffected)
     → clear orderOwnsTrip=false
   → the order card's **Stop trip** control (formerly labelled Dismiss) stops the trip directly, regardless of distance, using the same status rule as the Stop CTA for that trip (ADR-030 for cancelled, ADR-031 for early finish), and then closes the order row (ADR-031; formerly it only set the row FINISHED via dismiss(id))
   → open risk (ADR-025, resolved 2026-10-10): a FINISHED written after CANCELLED is ignored (CANCELLED rows are terminal); device confirmation remains

   → Track screen displays the order card (composable name chosen at implementation) instead of TripPanel (from pickup phase through terminal state)
   → while the trip is live, a compact trip strip sits below the card (same glass panel): TIME (elapsed, compact format), DIST (km), COST / NET (estimated fuel cost = trip fuel from OBD accumulator × effective price; NET = OrderCard.earningsRp − COST), AVG/INST km/L, fed from TrackPanelState + OBD state + FuelPriceController + OrderCard earnings
   → after terminal state, trip strip and card disappear; TripPanel returns

ROUTE (Planned + Optimized/Runtime + Actual, ADR-016, provider ADR-017, persistence ADR-026):
   → OrderRouteController collects activeOrder (from latestOrderFlow) and locationUiState
   → On order takeover (phase → PICKUP):
      Geocode pickup and drop addresses via OpenRouteService Geocoding (once per order, cached in-memory)
      Geocoding fallback ladder (ADR-017 Amendment 2026-10-11): (1) full address, normalized ('Jl.'→'Jalan'), including city, (2) place name + last 1–2 address segments as area hint, (3) house-number-stripped address + progressive trims, (4) street-only last; bare place name alone is not a candidate
      Boundary guard: each request sends boundary.circle (driver's current location, radius 50 km) plus focus.point.*; client-side, any accepted result more than 50 km (MAX_GEOCODE_DISTANCE_KM) from the driver's current location is rejected and falls to the next step; all steps failing draws nothing (fail-soft)
      Reject results: confidence <0.8 unless venue/street layer; locality/localadmin/county/region/macroregion/country layers
      Planned-route origin = accepted point (trip-start fix: activeTripStartLocationId row, else first recorded path point, else current live fix as last resort)
      Fetch planned route (accepted point → pickup → drop via waypoint; during DROP at takeover: accepted point → drop) via OpenRouteService Directions API; fetched once, frozen
      Diagnostics (tags unchanged; no address text, coordinates or key logged): per ladder step order key seen, index accepted, layer, confidence, distance to driver (km) or reason rejected (far / coarse / low-confidence / none); planned/runtime fetch success or failure and point counts; warning if pickup and drop resolve <30 m apart
      Extract routes[0].summary distance/duration along with the geometry (RouteResult)
      Persist at fetch (ADR-026, planned code): write trip_planned_route row (kind 'ORDER', keyed by LocationUiState.tripStartedAt, originSource ACCEPTED or FALLBACK_LIVE) immediately after the fetch succeeds; if tripStartedAt == 0 skip persisting and log
      Hydrate (ADR-026): on process restart, if a row exists for the current trip, restore the frozen plan from it instead of re-planning from a new origin
      Display as dashed thin blue-grey polyline on TrackMap (UI-SPEC §3j)
   → On phase change (PICKUP → DROP) or off-route (>~40 m from polyline):
      Fetch runtime route (current → next stop: pickup or drop) via OpenRouteService Directions API
      Display as solid bold orange polyline above the planned route; in-memory only, never persisted, never fed to the route store (BR-16)
      Throttled: at most once per 30 s and only after driver moved ~100 m
   → Pickup and Drop map markers displayed at their geocoded LatLng coordinates
   → Attribution: '© openrouteservice.org | © OpenStreetMap contributors' visible while routes shown, hidden when none
   → On terminal state (FINISHED or CANCELLED) or dismiss:
      Clear both planned and runtime routes from memory (the persisted planned row stays with its trip), remove markers, hide route state
      TrackMap reverts to recorded trace + live car position only
   → Failure handling: offline or API error → no route drawn, trip continues normally (fail-soft)
   → Trip details (ADR-026): load trip_planned_route by track.timestamp; decode the polyline; draw dashed under the actual line; compute comparison at display time with a pure RouteComparison util, never persisted:
        Planned km = distanceM / 1000; Actual km = track.distance; Extra km = actual − planned (signed);
        Max deviation m = max over actual path points of the distance to the planned polyline
      No row (pre-v15 trip, failed plan, manual trip) → empty state "No planned route saved"; attribution shown whenever the saved plan is drawn
   → Data lifetime (ADR-026): planned = persisted device-only with its trip; optimized = memory only; actual = canonical location log + track.pathPoints
   → External data flow: pickup/drop addresses and current location sent to OpenRouteService (Directions + Geocoding); the boundary circle reuses the driver's current coordinates (no new data class); customer name/phone never sent (ADR-013)
   → Accuracy notes (ADR-017): street-level pins can be metres off on long roads; geocoding may fail for long comma-separated Gojek addresses; an address with no city may geocode wrongly inside the 50 km circle or not at all; fail-soft (no route better than wrong route)
```

**Coupling:** Observer-to-tracking link is by signal/intent only (service isolation rule); the Observer service never calls `TrackingService` methods directly. `TrackingService` must never start/stop `ObserverAccessibilityService`. Foreground launch from a background accessibility service is gated by Android 10+ background-activity-start restrictions; a fallback mechanism (full-screen-intent notification or system alert window) may be required and must be verified on-device (test device: Samsung SM-G965F, Android 10). If the launch is blocked and `ShareViewModel` does not exist, the trip is started when the activity is next created. `orderOwnsTrip` is in-memory; after process death the trip is not auto-ended. Any new permission required will be documented separately.

### Track Screen — Live Location Display (ADR-018, display-only data path)

```
FusedLocationProviderClient → LiveLocationSource (screen-scoped, lifecycle-aware)
   ├── Active only while TrackScreen visible AND app in foreground (ON_START/ON_STOP/RESUMED)
   └── Exposes StateFlow<LiveFix?> (lat/lng, speed, bearing, accuracy, time)

TrackScreen display logic:
   displayLocation = liveLocationSource.collect() ?: trackingService.lastKnownSeed

DISPLAY ONLY (never written to DB):
   ├── Map dot/marker position
   ├── Follow mode camera target
   └── Recenter FAB target

SEPARATE from TrackingService recording path:
   TrackingService.locationUiState → canonical location_log (write path, persisted)
   LiveLocationSource → TrackScreen display (read path, ephemeral)
   
   Result: live dot visible whenever screen open, regardless of recording state.
   Canonical log unchanged (source of truth for sessions/trips/OBD).
```

**Permissions:** Uses existing `ACCESS_FINE_LOCATION` foreground permission; NO background location. If permission not granted, source stays idle and screen uses fallback seed (same as today).

**Battery:** GPS at ~1 Hz only while TrackScreen visible (same cost model as Google Maps/Waze). FusedLocationProviderClient merges requests when both screen and service are active (service already running, so incremental cost small).

**Privacy:** Live fixes never stored or transmitted. Only external call remains OrderRouteController → OpenRouteService (for order routes, ADR-017).

### Debug-only Diagnostic Screen Recorder (ADR-021)

**Purpose:** Automatic on-device video capture for debugging UI anomalies without manual intervention.

**Architecture:**
- **Debug-only source set:** All code in `app/src/debug/` (never compiled into release builds). Debug manifest overlay declares `mediaProjection` foreground service type and `WRITE_EXTERNAL_STORAGE` permission (debug, maxSdkVersion 28; absent from release manifest via source-set isolation).
- **Service:** `DiagnosticRecorderService` extends `MediaProjectionCallback`, uses `MediaRecorder` to encode to MP4 video (no audio, no microphone permission).
- **Lifecycle:** Request `MediaProjection` once per app process (system consent dialog; cannot be bypassed or persisted). On background the live session is **paused** (`MediaRecorder.pause()`; projection, `VirtualDisplay` and foreground service stay alive, notification "Paused - app in background") and on return **resumed** with no dialog. Consent is asked again only with no live session (process restart, system stop, "Stop & keep"). Segment length counts recording time only (handler timer + `setMaxDuration`). See ADR-021 amendment 2026-10-07. **Opt-in background capture** (debug-only, default **OFF**, persisted in debug SharedPreferences): a second notification action "Background: OFF/ON" toggles it. When ON, the recorder is **not** paused on background and keeps capturing the app in front; when OFF, the pause/resume behaviour above applies. See ADR-021 amendment 2026-10-08. Foreground service posts persistent notification with "Stop & Keep" action to freeze segment rotation.
- **Storage:** MediaStore video with `RELATIVE_PATH = "Movies/TrackLocation-Diagnostics/"`. Segments are 10-minute MP4 files; app maintains last 6 (~60 min) (ADR-021 amendment 2026-10-08; previously 2-minute segments, last 5, ~10 min). On rollover, oldest segment deleted automatically. Files marked `IS_PENDING = false` after close, so clips appear in standard Photos/Gallery album.
- **On release builds:** No service, no permissions, no recorder classes. Clean APK, zero overhead.

**Review workflow:** Clips pulled from device (`adb pull`) and frames extracted with ffmpeg (`sudo apt install -y ffmpeg` on dev machine), then read as images for pixel-level analysis. No built-in video playback in the app.

**Consequences:** See ADR-021 §8 (cloud backup risk, segment rotation window, battery/CPU impact, post-uninstall orphans). No impact on canonical GPS recording, trips, sessions, or data model.

---

## 6. Security & Authorization Model

- **Current:** no auth; all data local. Permissions: `FOREGROUND_SERVICE_LOCATION`, location, `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` (API 31+) / `BLUETOOTH`+`BLUETOOTH_ADMIN` (≤30), `BIND_ACCESSIBILITY_SERVICE`.
- **Planned (Observer Phases 6–7):** registration gate + Google Sign-In + CameraX/ML Kit face enrollment; app-wide auth overlay; 24-hour re-auth; face attempt budget with Google fallback. Auth gates UI only — background capture, sync, and GPS continue. **Raw face images are never transmitted.**

---

## 7. Data Flow Diagram

```
GPS fix → TrackingService → location_log (canonical)
   ├── session ON/OFF → recording_session rows
   └── trip start/stop → TrackEntity (startLocationId..endLocationId)
Trip/Session detail screens resolve path from location_log ranges.
```

---

## 8. Database Schema

Room database (`TrackDatabase`), at version **v17** in code (current; **v17 added by ADR-027 rev. 1.6**: new `obd_ledger` table (one column), `MIGRATION_16_17`, implemented 2026-10-11, `assembleDebug` OK and installed over existing data without a migration error (FUEL LEFT after the migration not yet read on device); **v16 added by ADR-027**, implemented in commit `a197453`; `assembleDebug` OK and run on a Galaxy S9+ over the existing v15 data on 2026-10-11 without a migration error (schema comparison not done); ADR-026 extends to v15, code `f26bb03`; ADR-025 extends to v14 2026-10-10; ADR-024 extended to v13 2026-10-09; ADR-023 extended to v12 2026-10-08). 

**Current migrations (all inline in `TrackDatabase.kt`):**
- `MIGRATION_1_2` (legacy serialized trip paths → canonical location rows + trip boundaries)
- `MIGRATION_2_3` (observer_event + allowlist_rule)
- `MIGRATION_3_4` (obd_sample)
- `MIGRATION_4_5` (OBD accumulator columns — `ALTER TABLE recording_session` + `ALTER TABLE track`; the domain "trip" is the physical `track` table)
- `MIGRATION_8_9` (ADR-008, DB→**v9**): `CREATE TABLE fuel_price (id INTEGER PK AUTOINCREMENT, pricePerLiter REAL NOT NULL, effectiveFromMs INTEGER NOT NULL)`, plus an `@Index` on `effectiveFromMs`
- `MIGRATION_9_10` (ADR-013, DB→v10): `CREATE TABLE observer_trip (id INTEGER PK AUTOINCREMENT, pickupName TEXT, pickupAddress TEXT, dropName TEXT, dropAddress TEXT, payment TEXT, earningsRp INTEGER, phase TEXT NOT NULL, firstSeenAt INTEGER NOT NULL, lastSeenAt INTEGER NOT NULL, handled INTEGER NOT NULL DEFAULT 0)` with an `@Index` on `lastSeenAt`
- `MIGRATION_10_11` (ADR-022, DB→v11): `ALTER TABLE track ADD COLUMN orderLabel TEXT` + `ALTER TABLE track ADD COLUMN orderEarningsRp INTEGER`
- `MIGRATION_11_12` (ADR-023, DB→**v12**): Twelve nullable columns added to `track` for order-trip geo snapshot:
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
  No default values; all columns nullable.
- `MIGRATION_12_13` (ADR-024, DB→**v13**): covered-distance columns for the fuel-covered-interval average:
  ```sql
  ALTER TABLE recording_session ADD COLUMN obdCoveredDistanceKm REAL NOT NULL DEFAULT 0.0
  ALTER TABLE track ADD COLUMN obdCoveredDistanceKm REAL NOT NULL DEFAULT 0.0
  ALTER TABLE obd_sample ADD COLUMN gpsSpeedKmh REAL
  ```
  Existing rows keep `0.0` / `NULL`; no backfill (pre-v13 trips keep the legacy displayed-distance ÷ fuel value).
- `MIGRATION_13_14` (ADR-025, DB→**v14**): order trip outcome columns on `track`:
  ```sql
  ALTER TABLE track ADD COLUMN orderStatus TEXT
  ALTER TABLE track ADD COLUMN orderOffline INTEGER NOT NULL DEFAULT 0
  UPDATE track SET orderStatus = 'COMPLETED' WHERE orderLabel IS NOT NULL
  ```
  Order-linked trips existing before v14 are marked `COMPLETED` (the earlier cancel outcome was not stored; history not rewritten). Manual trips keep `orderStatus` NULL and show no chip. `orderOffline` defaults to `0`.
- `MIGRATION_14_15` (ADR-026, DB→**v15**, implemented in code `f26bb03`): new table, no change to existing tables:
  ```sql
  CREATE TABLE trip_planned_route (
    tripStartedAt INTEGER NOT NULL PRIMARY KEY,
    kind TEXT NOT NULL,
    geometry TEXT NOT NULL,
    distanceM INTEGER,
    durationS INTEGER,
    originSource TEXT,
    fetchedAt INTEGER
  )
  ```
  No backfill: trips recorded before v15 have no planned route. No exported schema JSON is added (`TrackDatabase` has `exportSchema = false`); the migration is verified by installing over a copy of the v14 database before the code ships. Gate: ORS ToS check (ADR-026 decision 10) before the code ships.
- `MIGRATION_15_16` (ADR-027, DB→**v16**, implemented in commit `a197453`; `assembleDebug` OK and run on a Galaxy S9+ over the existing v15 data on 2026-10-11 without a migration error; schema comparison and a copy-based install not done): new table, no change to existing tables. (The original scope said v14 → v15; the code is already at v15 via ADR-026, so this is v15 → v16.)
  ```sql
  CREATE TABLE fuel_event (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    kind TEXT NOT NULL,
    litres REAL NOT NULL,
    pricePerLiter REAL,
    resetBaselineL REAL,
    createdAtMs INTEGER NOT NULL
  )
  ```
  `resetBaselineL` is set only on RESET rows (cumulative OBD fuel total at the Reset, ADR-027 rev. 1.2) and is NULL for FILL_UP. Append-only; no deletes in ADR-027. No exported schema (`TrackDatabase` has `exportSchema = false`); the migration is verified by installing over a copy of the v15 database before shipping.

- `MIGRATION_16_17` (ADR-027 rev. 1.6, DB→**v17**, implemented 2026-10-11):
  ```sql
  CREATE TABLE obd_ledger (
    id INTEGER PRIMARY KEY NOT NULL,
    fuelConsumedL REAL NOT NULL DEFAULT 0
  );
  INSERT INTO obd_ledger (id, fuelConsumedL)
    SELECT 1, COALESCE(SUM(obdFuelConsumedL), 0) FROM recording_session;
  ```
  The seed keeps existing Reset baselines (`resetBaselineL`) valid. No change to `fuel_event`. A fresh install has no seed row until the first write (`ObdLedgerDao.addFuel` seeds it if missing; reads return 0.0). No other table changes. Verification status (2026-10-11): installed over existing data without a migration error; the seeded FUEL LEFT value was not read on device; the copy-based check (install over a copy of the v16 database) is not done.

**Backfill logic for MIGRATION_11_12 (v11→v12):**
- For each `track` row with `orderLabel NOT NULL` (order-linked trips), match against `observer_trip` rows using the window rule: `firstSeenAt − 2 min` to `lastSeenAt + 5 min`, exactly one unambiguous match.
  - If exactly one match exists: copy pickup/drop names and addresses from the `observer_trip` row into `pickupName`, `pickupAddress`, `dropName`, `dropAddress`.
  - If multiple matches, no match, or the order row was pruned: leave pickup/drop fields `NULL` (no false linking).
- Backfill `acceptedLat`/`acceptedLng` from the `location_log` row referenced by `track.startLocationId` when available; leave `NULL` if `startLocationId` is `NULL` or the location row is missing.
- Leave `acceptedName`/`acceptedAddress` as `NULL` (no network call in migration; reverse-geocoding is captured on-demand for new trips).
- Post-migration, new order trips capture all twelve fields when the trip is persisted at stop; accepted location is reverse-geocoded once (fail-soft if offline) via OpenRouteService.

**Post-migration background backfill (ADR-023 Amendment 2026-10-08):**
- A one-time background backfill runs from `ShareViewModel` init (once per app process), processing up to 20 order-linked trips with `NULL` accepted names but present `acceptedLat`/`acceptedLng` coordinates (newest first).
- Each candidate is reverse-geocoded via OpenRouteService (8 s timeout, one retry); success writes `acceptedName`/`acceptedAddress`; failure leaves `NULL` (retried on next app launch).
- Processing is sequential with ~1.2 s delays, never blocking trip data or UI.
- No schema change (uses existing v12 columns); privacy treatment same as new trips (FR-20/ADR-017).

`location_log` columns (per ADR-006 dwell collapse): `id` (PK), `timestamp` (last confirmed-still fix / departure), `dwellStartTimestamp` (arrival; set once at insert, never bumped), `collapsedCount` (fixes folded into the anchor, default 1), `latitude`, `longitude`, `accuracyMeters?`, `speedMetersPerSecond?`, `bearingDegrees?`, `altitudeMeters?`. `MIGRATION_5_6` (ADR-005) added the `observer_event_fts` FTS4 index; `MIGRATION_6_7` (ADR-006) adds `dwellStartTimestamp`/`collapsedCount` and backfills `dwellStartTimestamp = timestamp`. DB version → 7.

`obd_sample` table:

| Column | Type | Notes |
|---|---|---|
| id | Long PK auto | |
| timestampMs | Long | `System.currentTimeMillis()` at insert; `@Index` |
| rpm | Int? | |
| obdSpeedKmh | Int? | stored for comparison; GPS speed canonical for km/L |
| fuelRateLph | Double? | direct, MAF-derived, or speed-density estimate |
| mafGramsPerSecond | Double? | raw or estimated MAF |
| fuelRateSource | String | `DIRECT_FUEL_RATE` / `MAF_DERIVED` / `SPEED_DENSITY` / `UNAVAILABLE` |
| adapterElapsedMs | Long? | round-trip time to adapter |
| gpsSpeedKmh | Double? | GPS speed at poll time (DB v13, ADR-024); null on pre-v13 rows (contributes 0 covered distance) |

Fuel-rate fallback chain: `DIRECT(015E) → MAF(0110) → SPEED_DENSITY → UNAVAILABLE`. Speed-density estimate: `MAF(g/s) = (RPM × MAP_kPa × VE × Displacement_L × 28.97)/(120 × 8.314 × IAT_K)`, `fuel(L/h) = MAF/(14.7×λ) × 3600/745` (VE=0.85, gasoline; engine displacement is a user pref, default 1193 cc).

**OBD Phase 2 accumulation — unified per ADR-007; averaging definition amended by ADR-024:**
- **One averaging definition (session and trip), per ADR-024 (supersedes the ADR-007 displacement-distance definition):** `avg km/L = Σ covered distance ÷ Σ fuel`, both taken over the same **fuel-covered intervals** — a poll interval with a valid fuel rate (0.1..100 L/h) and `0 < dt < 60 s`. Covered distance per interval = `gpsSpeedKmh` (GPS speed at that poll, stored on each `obd_sample`) × `dt`. Idle intervals (speed 0, fuel > 0) add fuel with 0 distance. Total fuel (and cost) is unchanged.
- **Live value = O(1) incremental cache** (session: `obdFuelConsumedL` + `obdCoveredDistanceKm`, updated together by `SessionDao.addObdAccumulator(sessionId, fuelL, coveredKm)`; trip: in-memory fuel and covered-distance totals since `tripStartedAt`). **Authoritative value at close = re-integrate `obd_sample`** (`ObdFuelMath.integrateFuelLiters` and `integrateCoveredDistanceKm`, same guards; null `gpsSpeedKmh` contributes 0) over the final window and persist it (`TrackingService.stopAlwaysRecording` → session row; `ShareViewModel.onTripCtaTap` → `track.obdFuelConsumedL` and `track.obdCoveredDistanceKm`); `obd_sample` is the single source of truth for fuel, the cache is a disposable live proxy (ADR-007 "Option B" — cache = memoized integral of the canonical rows). Mid-drive restart reseeds both in-memory trip totals by one `obd_sample` integration.
- **Session average** = `session.obdCoveredDistanceKm / session.obdFuelConsumedL` (ADR-024; `—` when covered = 0 or fuel ≤ 0.01 L). Pre-v13 sessions show `—`.
- **Trip average (live)** = `tripCoveredKm / tripFuelL` (in-memory, same guard).
- **Completed-trip average** = `track.obdCoveredDistanceKm / track.obdFuelConsumedL` when `obdCoveredDistanceKm > 0`, else the legacy `track.distance / track.obdFuelConsumedL` for pre-v13 trips (history not rewritten).
- **No plausibility clamp and no accuracy gating on covered distance** (fuel must stay complete for cost; excluding intervals only from distance would re-break the ratio).
- **Display:** idle (fuel accrues, distance flat) correctly degrades the average — relies on the ADR-006 stale-speed fix.
- **Fuel cost (ADR-008):** `cost = litres × price`. Litres = session live `obdFuelConsumedL` (Session card) / in-memory trip total (active-trip row) / stored `track.obdFuelConsumedL` (completed row). Price is a first-class effective-dated entity `FuelPriceEntity(id, pricePerLiter, effectiveFromMs)` in the new `fuel_price` table (**MIGRATION_8_9**, DB→v9): current price = the row with max `effectiveFromMs`; Save/Undo/Redo append effective-now rows (non-destructive). Active session/trip use the current price; a **completed trip** uses the price effective at its **start** (`fuel_price` row with max `effectiveFromMs ≤ track.timestamp`, else `—`). Seed: a one-time code migration inserts the retired `obd_fuel_price_per_liter` scalar at `effectiveFromMs=0`. The `FuelPriceController` in-memory stack drives undo/redo; the DataStore scalar is deprecated as source of truth.

---

## 9. Deployment Architecture

Single Android APK; no backend in current phases. Future: Neon Postgres (V1 direct via low-privilege role), then an API backend (V2).

---

## 10. Error Handling Strategy

- Services log to Logcat (`ObdPollingService`, etc.); OBD surfaces IDLE/CONNECTING/CONNECTED/RETRYING/WAITING + last error in the UI state.
- OBD reconnect: exponential backoff (1s, 2s, 4s…) capped at `obdRetryMaxSeconds`, then WAITING until manual reconnect.
- Build/runtime errors and resolutions are recorded in `docs/ERRORS-LOG.md`.

---

## 11. Future Improvements

- Observer Phases 3–5: FTS5 filtering, hybrid sync engine (batch + time), Neon V1 remote storage.
- Observer Phases 6–7: registration, face enrollment, auth overlay + hardening.
- Surface "fuel data unavailable on this vehicle" in OBD UI for no-fuel-PID vehicles.

---

## 12. Track Navigation & Local Route Store (ADR-009 / ADR-010)

**Dual-mode Track screen (ADR-009).** The Track screen keeps live tracking and adds follow-a-route navigation as a **sub-mode of a trip** (never independent). "Start trip + navigate" reuses the existing `START_TRIP` path (which already starts always-recording); ending navigation ends the trip via `STOP_TRIP` but never stops always-recording. Off-route recalculation fires at ~50 m, gated by 2–3 consecutive off-route fixes and ≥15 s between calls. Routing failures fail soft (trip always records track-only). `LocationUiState` gains a `bearingDegrees` (heading) field, surfaced from the `Location.bearing` already captured in `TrackingService`/`LocationEntity`.

**"Navigation perspective" camera.** A decoupled, user-toggled map view that sets `CameraPosition.bearing = heading` and follows the vehicle (heading-up), with **no tilt** (Compose-Maps 1.2-safe). A directional car marker uses `Marker(rotation = heading)`.

**Local route store (ADR-010).** New derived components (business logic out of Compose, data behind repository per §3): `TraceIngester` (on session/trip end, background — simplify + segment the new `location_log` slice into `known_segment` rows), `RoutePredictor` (current LatLng + bearing → 0..N candidate ahead-polylines from the grid-cell index), and `LocalRouteRepository` (DAO wrapper). The store is derived and rebuildable; the canonical `location_log` is never modified. `TrackDatabase` moves to **v10** via `MIGRATION_9_10`.

**Routing engine (ADR-011).** For roads not covered by the local store, routing goes through a `RoutingEngine` **port** (`route(origin, dest, opts) → RouteResult(polyline, distanceMeters, durationSeconds)`) with a first `OpenRouteServiceAdapter` connector (`POST /v2/directions/driving-car`, API-key auth, encoded-polyline geometry reused by the existing decode). Ports-and-adapters keeps the provider swappable: self-hosted ORS is a base-URL swap in the same adapter; OSRM/Valhalla/Google are new adapters. ETA is **static** (ORS has no live traffic). The ORS key is not hardcoded (§13); ORS/OSM attribution is displayed wherever routes appear.

**Still open (parked):** *when* to migrate to self-hosted OSM (deliberately deferred). Stage B (a full routable graph over the user's own network) is deferred to its own future ADR.

## Reference Documents

- `docs/PRD.md` — Product requirements and features
- `docs/IMPLEMENTATION-PLAN.md` — Sprint/slice breakdown and tech stack summary
- `docs/adr/` — Architecture Decision Records (detailed decisions)
- `AGENTS.md` §4 — Technical responsibilities
- `docs/WORKFLOW.md` — Development workflow

---

**Last Reviewed:** 2026-06-15
**Next Review:** After Observer Phase 2 completion
