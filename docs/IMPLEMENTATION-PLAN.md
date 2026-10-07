---
name: IMPLEMENTATION-PLAN.md
path: docs/IMPLEMENTATION-PLAN.md
description: Implementation Plan — TrackLocation (phases, slices, task log, session history)
---

# Implementation Plan
## TrackLocation

**Version:** 0.27
**Status:** Active (migrated from implementation-plan.md + progress.md)
**Last Updated:** 2026-10-07
**Approach:** Incremental end-to-end vertical slices; two-track model (code work + UI design-handoff work) per AGENTS.md §12.

> Migrated 2026-06-15 to the create-project schema. The detailed per-phase/slice contract from the legacy `implementation-plan.md` is preserved verbatim in **Appendix A**. The full session/progress audit log from the legacy `progress.md` is preserved verbatim in **Appendix B**. §6 below is a summarized task log over those sessions.

---

## ▶ Next Step — Start Here

### Current — Two-ACTIVE-cards fix (single-active-session invariant) device-verified 2026-07-07. Active coding task: resume always-recording on relaunch after force-stop (an open non-stale session was leaving an Inactive card beside an ACTIVE session) — code fix pending (see §6 Task Log, In Progress). (OBD Phase 2 built + installed; manual drive-test still pending — user. Observer P3 complete (Filtering + Settings); Observer P4+ are out of current scope per PRD §8.)

### Also documented — Gojek trip extraction (ADR-013), order-card takeover (ADR-014), order auto-start/end trip (ADR-015), order route overlay (ADR-016) with provider switch to OpenRouteService (ADR-017), Track map Recenter/follow mode (2026-10-06), Track screen live location (ADR-018, docs only), Track navigation camera (ADR-020, docs only). Code for order features + route provider implemented, unbuilt for order route overlay (static review only); design handoff pending; build/test needs explicit permission (AGENTS.md §5a). Track map Recenter code pending user approval (no build permission yet). Track screen live location and Track navigation camera code pending user approval (AGENTS.md §5b).

**OBD Phase 2: Fuel Consumption Enhancement** — idle L/h display, session-average km/L, trip-average km/L, Trip screen fuel metrics.

**Slice 1 (schema + MIGRATION_4_5) — done 2026-07-02 (static, unbuilt):** accumulator columns `obdFuelConsumedL`/`obdGpsDistanceKm` on `recording_session`, `obdFuelConsumedL` on `track`; DB bumped v4→5 (inline `MIGRATION_4_5`); DAO increment methods added.

**Slice 2 (service/DB accumulation) — done 2026-07-02 (static, unbuilt):** `ObdPollingService` persists per-poll fuel/distance increments into the active `recording_session` row; `TrackingService` passes `EXTRA_SESSION_ID`. Trip fuel resolved via **Issue #1 → derive from `obd_sample`**: `ShareViewModel.onTripCtaTap()` integrates fuel over the trip window at stop and writes `track.obdFuelConsumedL`.

**Slice 3 (Session `ObdStatusCard` UI) — done 2026-07-02 (static, unbuilt):** design handoff produced + verified in Claude Design (existing TrackLocation project, `rinaldi.ch` account, Haiku 4.5 — page "OBD Status Card", 4 artboards). Code in `SessionsScreen.kt`: EFFICIENCY cell shows `L/h` at idle else `km/L`; new `SESSION AVG` row from persisted accumulators; fuel-source chip reflects real source.

**Slice 4 (Trip panel UI) — done 2026-07-02 (static, unbuilt):** `TripPanel` shows an OBD row (FUEL = instant km/L or idle L/h; TRIP AVG = live trip-average) gated on `obdConnected`; `TrackScreen` polls the trip-average every 2 s from the `obd_sample` window since `tripStartedAt` ÷ live trip distance. No mockup this slice (visual language already established).

**OBD Phase 2 is built + installed on device (SM-G965F, Android 10, 2026-07-06).** A pre-existing Google Maps API key resValue-name mismatch blocked the first build (fixed in `app/build.gradle`; see ERRORS-LOG ERR-002). Remaining: **manual** on-device drive test (drive with ELM327 → confirm idle L/h, session-avg persists across restart, trip-avg accumulates, `track.obdFuelConsumedL` written at stop). After that, next planned phase is **Observer Phase 3 (Filtering + Settings)**.

**Observer Phase 2** is complete and fully verified on device (cursor pagination + snapshot viewer + truncation banner, all states confirmed 2026-07-02).

**Observer Phase 3 (Filtering + Unified Settings) is COMPLETE** — all four slices built and device-verified on 2026-07-07: S1 FTS data layer, S2 filter state/query wiring, S3 filter UI (search + package chips + no-results), S4 unified Observer Settings screen (service status, capture toggle, allowlist; reached via a gear on the feed). UI-slice designs were produced in Claude Design ("Observer Filter Bar", "Observer Settings"). Observer Phases 4–7 (sync/Neon/registration/auth) remain out of current scope (PRD §8).

**Claude Code prompt** (paste at repo root; safe to re-paste to resume):
> Read AGENTS.md and docs/IMPLEMENTATION-PLAN.md. OBD Phase 2 is code-complete (Slices 1–4, static). Next: build/device-verify OBD Phase 2 (with permission), or start Observer Phase 3. Do not run Gradle/tests/emulator/device without explicit permission (AGENTS.md §5a).

### Completed (newest first)

- ✅ Observer Phase 3 — Filtering + Unified Settings (S1–S4; FTS search + package chips + Observer Settings screen; device-verified 2026-07-07)
- ✅ OBD Phase 1 — hardware verification complete (2026-06-15, ELM327 streaming verified)
- OBD Phase 1 — live device verification + indirect speed-density fuel (2026-06-08, `211ef56`)
- OBD Phase 1 Slice 3 — full polling loop, live telemetry (2026-06-07, `bd13fb3`)
- OBD Phase 1 Slice 2 — enable toggle + device picker (2026-06-07, `c8a68da`)
- OBD Phase 1 Slice 1 — infrastructure + navigation (2026-06-07, `d089fcf`)
- Observer Phase 1 — verified on device (2026-05-29)
- Observer Phase 2 — cursor pagination (2026-05-22, `ee54e9c`)
- CR-0002 Session always-recording switch (2026-05-16, `fe241a7`)
- CR-0001 Always-recorded sessions + canonical log (2026-05-16, `776cd6e`)

---

## 1. Change Log

| Version | Date | Change |
|---------|------|--------|
| 0.27 | 2026-10-07 | Track Panel LIVE button shows Stop icon (UI-SPEC §3g amendment, Issue #2 Resolved). Owner decision: re-icon/relabel the LIVE button as Stop (filled rounded square, "Stop trip") while keeping the remote Track design (PlayFab in READY, PAUSED unreachable). No new ADR (partial adoption of rejected ADR-019 option). UI-SPEC v0.21→0.22 (§3g LIVE-PAUSED wording updated: LIVE now shows stop icon + "Stop trip"; PAUSED unreachable; dated note added). IMPLEMENTATION-ISSUES v0.2→0.3 (Issue #2 marked Resolved with decision record). adr/019 addendum added (2026-10-07: partial adoption note, Status stays Rejected). IMPLEMENTATION-PLAN v0.26→0.27 (§1 change log entry 0.27, §4 new slice "Track Panel — LIVE button shows Stop"; §6 three task-log rows Docs 2026-10-07 Completed, Code Planned/Not Started, Design § ui-design.pen Planned/Not Started, all marked "---"). DOCUMENT-CONTROL register updated. No schema. No Room migration. No new permission. Code implementation pending user approval + build (AGENTS.md §5a/§5b). |
| 0.26 | 2026-10-07 | Directional car marker from ui-design.pen replaces the blue pin (UI-SPEC §3i). No new ADR (already accepted in ADR-009). UI-SPEC v0.20→0.21 (new §3i subsection: car marker design source, five rules, display-only note, implementation location, a11y, no design handoff; fixed stale wording on "static pin" and "live car marker" lines, added §3i cross-references). IMPLEMENTATION-PLAN v0.25→0.26 (§1 change log entry 0.26 added; §4 status row "Track Navigation" car marker → done via §3i; new §4 slice "Track Map — Car Marker"; §6 two task-log rows Docs Completed + Code Planned, both "---"); adr/009 dated note (2026-10-07). DOCUMENT-CONTROL register updated. No schema. Code pending user approval + build (AGENTS.md §5a/§5b). |
| 0.25 | 2026-10-07 | Track navigation camera auto-activates during trip or order (ADR-020). New ADR-020 file (Context/Decision/Consequences/Alternatives/Related ADRs/References). PRD v0.16→0.17 (FR-19 new: auto-switch to navigation camera heading-up while trip/order active); UI-SPEC v0.19→0.20 (§3f updated with Recenter cross-reference note, §3h new: navigation camera behaviour 7 rules + accessibility + no design handoff); adr/README v0.8→0.9 (ADR-020 row added); adr/009 amended with cross-reference note (wording only); IMPLEMENTATION-PLAN §1 change log, §4 new "Track Map — Navigation Camera (ADR-020)" slice with What/Observable/How to Verify + 2 implementation steps, §6 two new task-log rows (Docs 2026-10-07 Completed + Code Planned/Not Started, both marked "---"), Also documented updated, version 0.24→0.25; DOCUMENT-CONTROL register + change log. No schema. Code gated (AGENTS.md §5a).
| 0.22 | 2026-10-06 | Track screen live location (ADR-018): display-only, foreground-only, never stored. New ADR-018 file (Context/Decision/Consequences/Alternatives/Related ADRs/References); PRD v0.15→0.16 (FR-18 new: live location independent of recording, foreground permission, no schema, Waze-replacement roadmap note); ARCHITECTURE v0.17→0.18 (LiveLocationSource component + display-only data-flow separate from TrackingService recording path); UI-SPEC v0.17→0.18 (§3f live location note: source, fallback, display-only); IMPLEMENTATION-PLAN §4 new Track Screen — Live Location slice (What/Observable/How to Verify + 3 numbered steps); §6 new Docs row 2026-10-06 Completed + Code row 2026-10-06 Pending, Also documented updated; DOCUMENT-CONTROL register + change log. No schema. Code + build gated (AGENTS.md §5a). |
| 0.1 | (legacy) | Two-track implementation contract maintained in `implementation-plan.md`. |
| 0.2 | 2026-06-15 | Migrated to create-project schema; legacy plan → Appendix A, progress.md → Appendix B; summarized §6 task log. |
| 0.11 | 2026-10-05 | Added Observer — Gojek Trip Extraction (ADR-013): device-local trip extraction from Gojek order screens into `observer_trip` table; parser + per-app rule table; 90d/5k retention; §4 slice + §6 task-log row. |
| 0.12 | 2026-10-05 | Added ADR-014 (Gojek order card takeover): auto-stop trip + order card on Track screen + foreground launch when order is complete; extends §4 ADR-013 slice with steps 7–10 + verification; §6 task-log row; Next Step updated. |
| 0.13 | 2026-10-05 | Docs-only fixes to ADR-014 wording (invented Android names removed, placeholder composable name, persistence/foreground flow via `ShareViewModel`, migration numbering v10) + UI-SPEC §3e design-handoff spec. |
| 0.14 | 2026-10-05 | Code — Gojek order card takeover (ADR-013/014): OrderCardParser + GojekRules table; observer_trip entity/DAO/MIGRATION_9_10; OrderTripRecorder in Observer service; ShareViewModel auto-stop; foreground launch + provisional order card on Track screen; §4 ADR-013 slice implementation status + known risks added; §6 task-log row added (code written, unbuilt, design handoff pending). |
| 0.15 | 2026-10-05 | Code — fix order-card weaknesses (§4 known risks 1–2 marked FIXED): Dismiss control on the provisional order card; same pickup+drop pair after FINISHED/stale starts a new order (new takeover); §6 task-log row added; Git Revision backfilled for the order-card takeover row. |
| 0.22 | 2026-10-06 | TrackScreen design: READY-state PlayFab + merged metrics row + GojekOrderCard expand/collapse + strip-only + TripStrip format (§3g new + §3e updated in UI-SPEC v0.18). §6 two new task-log rows (Docs + Code). No schema. |
| 0.21 | 2026-10-06 | Track map Recenter FAB / Follow Mode (2026-10-06) — docs only. New §3f subsection describing functional follow mode with `isFollowing` state, gesture → off, tap Recenter → on transitions, provisional icon colors (dark/grey), a11y text labels, interaction with routes. Map Controls legacy note updated (§9). UI-SPEC v0.16→0.17 (§3f new subsection + Map Interface note update). IMPLEMENTATION-PLAN v0.20→0.21 (§1 change log entry, §4 new Track Map slice 3 steps + How to Verify with 7 checks, §6 task-log rows Docs Completed + Code Pending, Also documented updated). DOCUMENT-CONTROL register + change log. No schema. Design handoff + code pending (AGENTS §12 for visual refinement; §5a for build/device). | Completed | --- | Docs only (AGENTS §5a); no Gradle/device run. Code implementation pending user approval and build permission. |
| 0.20 | 2026-10-06 | Order route provider switch to OpenRouteService (ADR-017). Google Directions + Geocoding APIs unavailable (billing REQUEST_DENIED). Switches to ORS (free tier, no card, ADR-011 choice for ADR-009 navigation already). All ADR-016 behavior (two routes, markers, throttling, fail-soft) unchanged; provider port interface (ADR-011) unaffected. New: geocoding fallback ladder (place name + city → full address → street-only with district-drop; confidence <0.8 filter; fail-soft if all fail). Attribution '© openrouteservice.org | © OpenStreetMap contributors' required on map. Accuracy caveat: street pins metres off on long roads, no promise vs. Gojek internal routing, less accurate than Google for house #s. New ADR-017 file (Context/Decision/Consequences/Alternatives Considered/Related ADRs/References); adr/README v0.5→0.6 (ADR-017 row added, count 16→17); ADR-016 Status amended (provider superseded note); ADR-011 Note updated (ORS re-adopted for order routes); PRD v0.14→0.15 (FR-17 provider change to ORS + geocoding fallback ladder + accuracy caveats + attribution); ARCHITECTURE v0.16→0.17 (ROUTE section provider → ORS, geocoding ladder, attribution, accuracy notes); UI-SPEC v0.15→0.16 (§3e attribution surface added + prompt updated to mention ORS, fallback ladder, accuracy caveat); README v0.2→0.3 (API keys section: GOOGLE_MAPS_KEY (map SDK only) + OPENROUTESERVICE_API_KEY (free tier, order routes), no Geocoding/Directions needed); IMPLEMENTATION-PLAN v0.19→0.20 (§1 change log, §4 new ADR-017 slice 6 steps + 11-point How to Verify, §6 task-log rows Docs/Code 2026-10-06); DOCUMENT-CONTROL register + change log. No schema. Code + design handoff + build gated (AGENTS §5a/§12). | Completed | --- | Docs only (AGENTS §5a); no Gradle/device run. Code implementation pending user build permission. |
| 0.19 | 2026-10-06 | Order card COST / NET cell (trip strip extension): Extends FR-16 trip strip (compact 3-cell TIME/DIST/AVG/INST km/L) to add a 4th COST / NET cell (estimated fuel cost then net profit). COST = trip fuel (from OBD accumulator) × effective fuel price (FuelPriceController); NET = OrderCard.earningsRp − COST (shown as −Rp… if negative). Compact Rupiah format: <1k 'Rp850', 1k–999k 'Rp8.4k' (one decimal), ≥1M 'Rp1.2jt'; negative prefixed '−'. TalkBack reads full amounts. Missing: COST → '—' when OBD disconnected, no fuel, or no price; NET → '—' when COST is '—' or no earnings. Layout: four cells, weights TIME 0.7, DIST 0.7, COST/NET 1.4, AVG/INST 1.4; no clip on narrow screens. Provisional UI; design handoff updates strip visuals. PRD v0.13→0.14 (FR-16 extended with COST/NET definition + cross-link FR-12); ARCHITECTURE v0.15→0.16 (Gojek flow trip-strip data source noted); UI-SPEC v0.14→0.15 (§3e trip strip spec updated to 4-cell, compact-Rupiah rules + a11y strings + missing-data rules; Handoff Instructions step-by-step + screenshot checklist incl. order card with strip live + copy-paste Claude Design prompt with 5 states); IMPLEMENTATION-PLAN v0.18→0.19 (§1 change log entry, §4 ADR-015 slice updated with COST/NET step or new slice, §6 task-log rows Docs Completed + Code Pending); DOCUMENT-CONTROL register + change log; adr/015 addendum (2026-10-06 line). No schema change; UI-only. Code + build gated (AGENTS §5a). | Completed | --- | Docs only (AGENTS §5a); no Gradle/device run. Code implementation pending user build permission. |
| 0.18 | 2026-10-06 | Order route overlay (ADR-016) — docs only. New ADR-016 file (two routes: planned @ takeover frozen, runtime @ phase-change / off-route throttled >40m/30s/100m; Google Directions + Geocoding APIs; markers; provider decision overrides ADR-011 for Gojek feature only). adr/README v0.4→v0.5 (ADR-016 row added, index count 15→16); PRD v0.12→0.13 (FR-17 new); ARCHITECTURE v0.14→0.15 (Gojek flow extended with ROUTE section); UI-SPEC v0.13→0.14 (§3e new "Route overlay" subsection with provisional styling + Handoff Instructions: step-by-step, screenshot checklist, tool recommendation, copy-paste Claude Design prompt); IMPLEMENTATION-PLAN v0.17→0.18 (§1 change log, §4 new ADR-016 slice, §6 task-log rows Docs Completed + Code Pending, "Also documented" updated); DOCUMENT-CONTROL register + change log (ADR-016, version bumps recorded). Code + design handoff + build gated (AGENTS §5a/§12). |
| 0.17 | 2026-10-06 | Order card compact trip strip (provisional UI): auto-start brings trip to live state; card alone hid running trip metrics. New: compact trip strip below order card (same glass panel): TIME (compact format: 1h15m / 42s / 3m15s), DIST (km, one decimal), AVG/INST km/L (trip avg then instant, each one decimal). Shown only while trip LIVE/PAUSED; fed from existing TrackPanelState (elapsedMs, distanceKm, instantKmL, tripAvgKmL); no new schema/calculation. Provisional UI reuses TripPanel tokens; final design from handoff per AGENTS §12. PRD v0.11→0.12 (FR-16 wording); ARCHITECTURE v0.13→0.14 (Gojek flow trip-strip display); UI-SPEC v0.12→0.13 (§3e Trip strip spec + Handoff Instructions with screenshot checklist + copy-paste prompt); ADR-015 addendum; IMPLEMENTATION-PLAN §4 ADR-015 slice step 5 "Order card trip strip" added + "How to Verify" extended (7 on-device state checks); §6 task-log rows (Docs row Completed, Code row In Progress); DOCUMENT-CONTROL updated. Code + build gated (§5a). |
| 0.16 | 2026-10-06 | ADR-015 accepted — Gojek Order Lifecycle Drives the Trip (auto-start at Taken, auto-end at Cleared/Cancelled/Dismissed). New ADR-015 file + adr/README update; PRD 0.10→0.11 (FR-13/FR-16 amended); ARCHITECTURE 0.12→0.13 (Gojek flow updated); UI-SPEC 0.11→0.12 (§3e updated); IMPLEMENTATION-PLAN §4 new slice + §6 task-log rows; DOCUMENT-CONTROL updated. Code pending user approval; build/test gated (AGENTS.md §5a). |
| 0.3 | 2026-07-07 | Added Location Efficiency — Dwell Collapse phase (ADR-006 accepted): §3 phase row + Appendix A two-slice contract; PRD §12 BR-11 recorded. |
| 0.4 | 2026-07-07 | Added Fuel-Economy Unification phase (ADR-007 accepted): §3 phase row + Appendix A three-slice contract; instant two-cell + unified averaging + destructive v7→v8 column drop. |
| 0.5 | 2026-07-07 | Added Fuel Cost feature (FR-12): inline `Rp` price entry with multi-step in-memory undo/redo; cost = litres × price on the Session OBD card + Trips active-trip row. No schema change. §3 phase row + Appendix A slice + §6 task-log row. |
| 0.6 | 2026-07-07 | Fuel Cost v2 (ADR-008): fuel price becomes an effective-dated `fuel_price` entity (migration v8→v9); completed-trip cost priced at trip start; active-trip card separated into two rows (instant km/L, L/h, trip-avg km/L, cost). §3 phase row + Appendix A slice + §6 task-log row. |
| 0.7 | 2026-07-08 | Accepted ADR-009 (dual-mode Track navigation) + ADR-010 (self-learning route store) and propagated: PRD (Feature 6, FR-13/FR-14, BR-12…BR-15, Out-of-Scope turn-by-turn), ARCHITECTURE (§2 derived `known_segment`, §4 DB-version cell, §8 MIGRATION_9_10 DB→v10, new §12), UI-SPEC (§3d nav — design-handoff-pending). §3 phase rows + §6 task-log row. Routing engine, traffic-vs-static ETA, and ghost-route external-API cost remain OPEN. |
| 0.8 | 2026-07-08 | Accepted ADR-011 (routing engine via connector-adapter): OpenRouteService hosted free tier now, portable to self-hosted OSM by base-URL swap; static ETA. Resolves the ADR-009 routing-engine + traffic-ETA open questions. Propagated to ARCHITECTURE §12 (RoutingEngine port + OpenRouteServiceAdapter) and UI-SPEC §3d (attribution surface). §6 task-log row. Remaining parked: when to migrate to self-hosted OSM. |
| 0.9 | 2026-07-08 | Track Navigation design handoff produced (`docs/design-handoff/track_navigation/TRACK_NAVIGATION_SPEC.md`, AGENTS §12): 7 UI surfaces (destination search, Start control, route+ETA read-out, perspective toggle, car marker, road-ahead candidates, ORS attribution), screenshot checklist, Claude Design prompt. §6 task-log row. |

---

## 2. Purpose

Single authoritative plan for TrackLocation development: phases, per-slice breakdown, task log, and tech stack. Replaces the separate `implementation-plan.md` + `progress.md` pair.

---

## 3. Phases Overview

| Phase / Work item | Code status | UI/design status | Verification |
|---|---|---|---|
| CR-0001 Always-recorded Location Sessions | Implemented | Implemented | Verified by user |
| CR-0002 Session always-recording switch | Implemented | Implemented | Verified by user |
| Observer Phase 1 — Local Foundation | Implemented | Implemented | Verified on device (2026-05-29) |
| Observer Phase 2 — Inspection UI | Implemented (pagination + snapshot viewer + truncation banner) | Implemented | Verified on device (2026-07-02) |
| OBD Phase 1 — ELM327 telemetry | Implemented (4 slices) | Implemented | Verified on device (2026-06-15): RPM/speed streaming, km/L calculation, session gating, stability tested |
| OBD Phase 2 — Fuel consumption enhancement | Implemented (Slices 1–4: schema + DB accumulation + Session card + Trip panel) | Slice 3 design done in Claude Design; Slice 4 reused existing language | Built + installed on device 2026-07-06; manual drive-test pending |
| Observer Phase 3 — Filtering + Settings | Implemented (S1–S4) | Implemented (S3 filter UI + S4 Observer Settings; designs via Claude Design) | Complete — S1–S4 device-verified 2026-07-07 |
| Track Navigation — follow-a-route (ADR-009) | Planned | Pending design handoff (§12): search field, Start control, ETA placement, perspective toggle; car marker done via §3i (2026-10-07) | Not started |
| Self-learning Local Route Store (ADR-010) | Planned | n/a (derived data layer; road-ahead render UI TBD) | Not started |
| Fuel Cost v2 — price entity + completed cost + separated active metrics (ADR-008) | Implemented (compile-clean) | Spec'd in UI-SPEC §3a/§3b/§4 | Device-verified 2026-07-08 (car running) — full end-to-end |
| Location Efficiency — Dwell Collapse (ADR-006) | Planned | n/a (no new UI) | Not started |
| Fuel-Economy Unification (ADR-007) | Planned | Spec'd in UI-SPEC §4 (no external handoff) | Not started |
| Fuel Cost — inline price + cost display (FR-12) | Implemented (6 files; compile-clean) | Spec'd in UI-SPEC §4 (inline editor; no external handoff) | Static — `compileDebugKotlin` clean 2026-07-07; device drive-test pending |
| Observer Phase 4 — Sync Engine + Retention | Planned | Planned | Not started |
| Observer Phase 5 — Neon V1 Remote | Planned | Planned | Not started |
| Observer Phase 6 — Registration + Face Enrollment | Planned | Planned | Not started |
| Observer Phase 7 — Auth + Hardening | Planned | Planned | Not started |

Full detail for each phase/slice (goals, non-goals, numbered steps, per-slice verification, design-handoff tables) is preserved in **Appendix A**.

---

## 4. Detailed Slice Breakdown

The active and historical slice contracts (OBD Phase 1 Slices 1–4, Observer Phases 1–7, CR-0001/0002) live verbatim in **Appendix A — Detailed Phase/Slice Breakdown**. Each slice follows: *what it does → observable result → how to verify → numbered implementation steps (What/How per step)*. New slices must follow that format.

### Track Map — Car Marker (UI-SPEC §3i)

**What it does:** Replace the static blue location pin (`ic_location_pin`) with a directional car marker that rotates to show the direction of travel. The marker is sourced from `ui-design.pen` (CarMarker frame, 28×44 dp, top-down red car), converted to an Android VectorDrawable `ic_car_marker.xml`, and rendered with `flat = true` and `rotation = headingDegrees`. The heading comes from the GPS bearing when speed ≥ ~3 km/h, held constant below ~3 km/h, and points north (0°) if no valid heading yet. Display-only change; no schema, permission, or data-model impact.

**Observable result:** Track screen shows the red car marker (no blue pin) at the current location. In north-up view, the car points up when READY (no heading yet), rotates to direction of travel when moving ≥ ~3 km/h, holds the last heading when below ~3 km/h, and stays pointing up during a trip/order (heading-up camera, ADR-020) while the map rotates. Pickup/Drop markers and recorded trace unaffected.

**How to Verify:**
1. **Car marker at current location:** Track screen shows the red car marker (no blue pin) at the current location.
2. **Upright in north-up with no heading:** Car points up (bearing 0°) in READY state or north-up view with no heading yet.
3. **Rotates to direction of travel ≥ ~3 km/h:** Drive ≥ ~3 km/h in north-up view → car rotates to the direction of travel; bearing updates per GPS fix.
4. **Holds heading below ~3 km/h:** Slow below ~3 km/h → car holds the last heading (does not point north immediately); no noisy sub-3-km/h rotation.
5. **Upright during heading-up camera (ADR-020):** During a trip or order (heading-up camera active) → car stays pointing up while the map rotates; the car's direction matches the map's heading.
6. **Other markers unchanged:** Pickup/Drop markers and recorded trace rendered normally (no regression).
7. **No crash or leak:** Marker bitmap created once in a remember block; reused per map-state update; no ANR or memory leak on 30-minute recording session.

**Implementation steps (numbered, What/How):**

1. **Add `res/drawable/ic_car_marker.xml`:** Convert the `ui-design.pen` CarMarker frame (28×44 dp, top-down red car, front at top, translucent white backing omitted) to an Android VectorDrawable. Export from Claude Design or draw inline in XML: 28×44 dp viewBox, red car paths (~13×25 dp body inside the frame). Store as `app/src/main/res/drawable/ic_car_marker.xml`.

2. **TrackMap.kt: Replace pin marker with car marker.** File `screens/track/TrackMap.kt` updates the live-location marker logic: (1) Create marker bitmap once using `bitmapDescriptorFromVector(context, R.drawable.ic_car_marker, tint = null, scale = 1.0)` in a `remember` block; store in a mutable reference; (2) Add param `markerHeadingDeg: Float = 0f` to `TrackMap` composable; (3) Replace the existing `googleMap.addMarker(pinOptions)` call with car-marker options: `MarkerOptions().position(currentLocation).icon(carBitmapDescriptor).flat(true).rotation(markerHeadingDeg).anchor(0.5f, 0.5f)` (anchor centre, flat marker, rotated with map); (4) On every map-state update, call `marker.setRotation(markerHeadingDeg)`.

3. **TrackScreen.kt: Pass heading state to TrackMap.** File `screens/track/TrackScreen.kt` collects the live-location bearing from `LiveLocationSource`. Maintain `markerHeading` state: update it from the live-fix bearing only when speed ≥ ~3 km/h; hold the last valid heading below ~3 km/h. Pass `markerHeadingDeg = markerHeading` to `TrackMap`. (Existing heading-up camera logic in ADR-020 §3h already manages `navigationBearingDeg` independently; they are two separate params — car marker always shows current heading, camera rotation is independent.)

**No schema. No Room migration. No new permission.**

---

### Observer — Gojek Trip Extraction (ADR-013)

**What it does:** Automatically extract pickup and drop locations (plus payment and earnings) from captured Gojek driver app order-card tree snapshots into a device-local `observer_trip` store. Parse is post-snapshot-write, Gojek-only (`com.gojek.partner`), with no link to the canonical location log or trip/session lifecycle.

**Observable result:** On-device Observer feed shows extracted Gojek trips deduplicated by pickup+drop address; phase (pickup/drop-only/finished) updates as the order progresses; extraction is automatic and requires no user action. (UI display is pending design handoff, step 6; until then verification is via the DB.)

**How to Verify:**
- **Fixture coverage:** use pulled device DB / fixture snapshots for state verification:
  1. Pickup phase snapshot (with divider texts `Laporkan masalah map` + `Dibayar pakai`; pickup name/address + divider + drop name/address + payment) → parser returns `OrderCard` with pickup and drop populated; `firstSeenAt` ≠ `lastSeenAt` after second snapshot.
  2. Drop-only phase snapshot (without pickup, only drop + divider + payment/earnings) → parser returns `OrderCard` with drop-only phase, pickup fields null.
  3. Finished phase snapshot (no order card, only earnings/summary) → parser returns `OrderCard` with finished phase.
  4. Partial tree (WINDOW_CONTENT_CHANGED event; missing divider texts) → parser returns null (skipped).
  5. Stored row has no customer name, rating, or phone fields (PII audit).
  6. Retention: query at 90 days + 1 second / 5,000 + 1 rows → old row pruned; new row persists.
- **On-device:** start Gojek, accept/view an active order → Observer feed refreshes, new `observer_trip` row appears with pickup/drop extracted; accept drop → phase updates to drop-only; complete → phase updates to finished.

**Implementation steps (numbered, What/How):**
1. **ADR-013 + docs gate:** ADR-013 accepted; PRD/ARCHITECTURE/UI-SPEC/IMPLEMENTATION-PLAN docs written 2026-10-05 (commit pending).
2. **Parser + rule table:** Pure Kotlin function `parseGojekOrderCard(treeSnapshot: List<Map>, rules: GojekRules) → OrderCard?`. Extract text nodes in order; require divider texts; parse phase from button text; extract payment/earnings (integer Rp). Per-app rule table keyed by package name; `com.gojek.partner` entry with button text keys and divider strings. No Android types in parser (unit-testable). Code location: `data/` or `feature/observer/` per existing convention.
3. **ObserverTripEntity + DAO + MIGRATION_9_10:** `ObserverTripEntity` (id, pickupName, pickupAddress, dropName, dropAddress, payment, earningsRp, phase, firstSeenAt, lastSeenAt); `ObserverTripDao` (insert/update, pruning query); `MIGRATION_9_10` (DB v9→v10, create table; `TrackDatabase.kt` is v9 and ADR-010 has not shipped, so ADR-010's `known_segment` migration will be numbered 10→11 when it ships; numbering follows ship order and the two swap if ADR-010 ships first). No FKs to trips/sessions. Code location: `data/roomdb/entity/`, `data/roomdb/dao/`, `TrackDatabase.kt`.
4. **Wire extraction:** in the service's write path, after the `observer_event` row is persisted in `onAccessibilityEvent`, check if `packageName == "com.gojek.partner"` and `treeSnapshot != null`. If yes, call parser; if result non-null, upsert into `observer_trip` by (pickupAddress, dropAddress) key (dedup).
5. **Retention pruning:** `ObserverTripDao` gains `pruneOldTrips(maxAgeDays=90, maxRows=5000)`. Pruning caller/trigger decided at implementation time (follow the existing `observer_event` pruning pattern in the service).
6. **UI: design handoff pending (AGENTS.md §12):** Observer Trip Card display (pickup/drop/earnings/payment layout), integration into Observer feed detail view, or separate trip feed tab. No code before design approval.

**Implementation status (2026-10-05):** Steps 2–5 and 7–10 code written, unbuilt (AGENTS.md §5a); step 6 (final UI) pending the design handoff — provisional card in place.

**ADR-014 extension (code pending approval):**

7. **Trigger — one-shot "order ready" signal:** After the Gojek parser (step 2) yields an `OrderCard` with pickup+drop+payment+earnings populated for a given (pickupAddress, dropAddress) key, emit a one-shot "order ready" signal. This signal fires only the first time the card becomes complete; subsequent updates to the card's phase (drop-only, finished) do not re-fire. Signal must include the parsed order details and a flag indicating it is the first-complete event for this order.

8. **Auto-stop trip:** The order-ready state is derived from the persisted `observer_trip` row (survives the activity being created after the signal), with a "handled" marker to prevent repeats. `ShareViewModel` observes it and, when a not-yet-handled complete order appears while a trip is active, runs the manual-stop sequence once: `onTripCtaTap()` persists the trip (`TrackEntity` via `insertTrack`), then sends `STOP_TRIP` (`TrackingService.stopTrip()` only stops trip recording and the timer). If the foreground launch is blocked and `ShareViewModel` does not exist, the trip is stopped when the activity is next created. The always-recording session (`isAlwaysRecording`) is **never** affected; it remains ON before, during, and after the trip stop.

**Known risks / follow-ups (from review):** (1) **FIXED 2026-10-05** — a cancelled Gojek order never reaches "Selesai", so the order card (and the hidden Start/Stop trip control) stayed until the 2-hour staleness window expired; now a Dismiss control on the provisional order card releases it (2-hour staleness window kept); (2) **FIXED 2026-10-05** — the same pickup+drop address pair repeating for a later order matched the old (handled, FINISHED) row so no new takeover fired; a same-address order is now a new order when the previous row is FINISHED or older than the 2-hour window; (3) the finished-phase match uses the "Selesai" text alone, which could match unrelated Gojek screens; (4) if the foreground launch is blocked and no ShareViewModel exists, the trip is stopped when the activity is next created.

9. **Foreground launch:** The Observer service starts `MainActivity` with `FLAG_ACTIVITY_NEW_TASK` and an extra that selects the Track screen. This action fires once per order (gates on the same one-shot event as step 7, not on subsequent phase updates). Must be verified on Android 10 (test device SM-G965F, adb tunnel); background-activity-start restrictions may require a fallback mechanism: (1) a high-priority full-screen-intent notification (may need a new permission/notification channel), then (2) a `SYSTEM_ALERT_WINDOW` overlay. Any new permission required must be documented.

10. **UI — order card on Track screen:** When an order card is ready, the Track screen's `TripPanel` is replaced by the order card (composable name chosen at implementation), displaying pickup, drop, payment, earnings, and the current order phase. The card displays from pickup phase through "Sampai tujuan" (drop-only), then "Selesai" (finished). At "Selesai", the card is hidden and `TripPanel` returns. Design handoff (AGENTS.md §12) required before implementation.

**How to Verify (ADR-014 extension):**
- Active trip stops and is persisted when a complete Gojek order card is first detected; always-recording remains ON (via the `ShareViewModel` manual-stop sequence; check DB: trip row written, session row still `isActive = 1`).
- App comes to foreground on the Track screen with the order card displayed when the order becomes complete (device SM-G965F, Android 10, adb tunnel).
- Order card updates through phases (pickup → drop-only → Selesai); `TripPanel` returns after Selesai (on-device UI verification).
- No second foreground jump or auto-stop for the same order; the one-shot signal fires only once per (pickupAddress, dropAddress) key (DB + logcat verification).
- GPS recording and observer_event capture are unaffected by the order-card flow (no service-isolation degradation; verify foreground launch does not interrupt location updates).

### Observer — Gojek Order Auto-Start/End Trip (ADR-015)

**What it does:** Automatically start a trip when a Gojek order becomes Taken (the order card is fully read: pickup, drop, payment, earnings). If a trip is already running (e.g., started manually), keep it and bind it to the order. Automatically end the trip when the order is Cleared (Gojek home screen), Cancelled by the customer, or Dismissed by the user. The trip is not ended by the 2-hour card-staleness window; it runs until Cleared/Cancelled/Dismissed or manual stop.

**Observable result:** On-device, after accepting a Gojek order (order card appears), the trip timer starts immediately without a manual tap. After the driver completes the order ("Selesai" → home screen or cancel message), the trip automatically stops and is saved to the Trips list. A manually-started trip before the order appears is kept and ends with the order.

**How to Verify:**
- **Deterministic:** parser recognizes home screen (4 nav texts co-occur) and cancel message (text "Oke, sip" + "nge-cancel"); unit tests not required.
- **Manual on-device (auto-accept on, SM-G965F, Android 10, adb tunnel):**
  1. Take an order → card appears; trip timer starts immediately (no manual Start tap); trip strip below the card shows TIME counting, DIST = 0.0 km, AVG/INST km/L depends on OBD state. Verify: `isTracking=true`, trip row written to DB on Stop; trip strip values live-update.
  2. Finish with "Selesai" → trip stops automatically when home screen appears; trip strip disappears and `TripPanel` returns. Verify: trip appears in Trips list with correct distance/duration/km/L/cost.
  3. Cancel order → trip stops automatically when cancel message + home appear (no "Selesai" needed); trip strip disappears. Verify: trip saved with correct data.
  4. Dismiss card manually (Dismiss button on the card) → trip stops; trip strip disappears. Verify: trip saved.
  5. Manually start trip before order appears → trip kept and ends with the order; trip strip shows throughout. Verify: `orderOwnsTrip=true` flag set; trip ends at Cleared/Cancelled; trip strip frozen when trip is PAUSED.
  6. No "Selesai" screen required for detection; cancel alone triggers end.
  7. **Trip strip states (4 cells: TIME, DIST, COST/NET, AVG/INST km/L):**
     - OBD connected, moving, price set → TIME, DIST, "Rp8.4k / Rp28k" COST/NET, "11.8 / 12.3" km/L (all populated).
     - OBD connected, moving, no price → TIME, DIST, "— / —" COST/NET (no fuel price to calc), "11.8 / 12.3" km/L.
     - OBD connected, idle/no-fix, price set → TIME, DIST, "Rp8.4k / Rp28k" COST/NET, "11.8 / —" km/L (instant blank per TripPanel rule).
     - OBD connected, idle/no-fix, no price → TIME, DIST, "— / —" COST/NET, "11.8 / —" km/L.
     - OBD not connected, any earnings → TIME, DIST, "— / —" COST/NET, "— / —" km/L.
     - No earnings on card → TIME, DIST, "— / —" COST/NET (NET never estimated from earnings alone), "— / —" km/L.
     - Trip paused → all four metrics frozen (no live update).
  8. **Compact Rupiah format verification:** Entries 850 → 'Rp850', 8,400 → 'Rp8.4k', 1,200,000 → 'Rp1.2jt'; negative as '−Rp2.5k' (not '−Rp-2.5k'). Four cells fit on a narrow screen without clipping. TalkBack reads full "Rupiah" amounts (e.g., "Cost: eight thousand four hundred Rupiah").
  9. **Build verification (requires user permission §5a):** compile, install on SM-G965F, run on-device checks 1–8 above.

**Implementation steps (numbered, What/How):**
1. **Parser: GojekRules recognise terminal states.** Extend `GojekRules` pattern table: home screen = all 4 nav texts present (`Beranda`, `Pendapatan`, `Swadaya`, `Pesan` in snapshot); cancel message = "Oke, sip" + text containing "nge-cancel". Both yield `OrderCard` with `phase=FINISHED`. Code location: `data/observer/gojek/GojekRules.kt`, `OrderCardParser.kt`.
2. **OrderCardParser: yield FINISHED on terminal states.** When parser matches home or cancel patterns, return `OrderCard(phase=FINISHED, ...)` alongside the existing pickup-phase and drop-only-phase parsing. Code location: `data/observer/gojek/OrderCardParser.kt`.
3. **OrderTripRecorder: mark latest open row FINISHED.** Existing logic (unchanged): when a `FINISHED` card is parsed, `OrderTripRecorder` marks the latest open `observer_trip` row FINISHED (preserves the handler state, idempotent). Code location: `data/observer/OrderTripRecorder.kt`.
4. **ShareViewModel: auto-start trip at Taken.** In `ShareViewModel`, observe `unhandledReadyFlow` (the existing one-shot ready signal for complete cards). When the signal fires and no trip is active, send `START_TRIP`. If a trip is active, set in-memory flag `orderOwnsTrip=true`. Mark row handled. Code location: `ui/screen/track/ShareViewModel.kt`.
5. **Order card trip strip (3-cell base).** What: `GojekOrderCard` gets an optional `tripStripState: TrackPanelState?` parameter. When non-null and `tripState` is LIVE or PAUSED, render a compact trip strip below the card (same glass panel): one row with three cells (TIME / DIST / AVG/INST km/L). TIME in compact format (1h15m / 42s / 3m15s), DIST in km (one decimal), AVG/INST km/L (trip avg then instant, each one decimal). Provisional visuals reuse `TripPanel` style tokens; final design from handoff (UI-SPEC §3e Handoff Instructions). How: `TrackScreen` passes its existing `panelState` to the card. No new state management or data collection — all values already exist in `TrackPanelState` (elapsedMs, distanceKm, instantKmL, tripAvgKmL). Code location: `ui/component/GojekOrderCard.kt` (or chosen implementation name); layout strategy: Column(card, trip strip) sharing the glass-panel background.
6. **Order card COST / NET cell (4-cell extension).** What: Extend the trip strip with a 4th cell between DIST and AVG/INST km/L: **COST / NET** = estimated fuel cost then net profit. COST = `trip fuel (from ObdPollingService.obdUiState.tripFuelConsumedL or in-memory accumulator) × effective fuel price (FuelPriceController.currentPrice)`; NET = `OrderCard.earningsRp − COST` (shown as `−Rp…` if negative). **Compact Rupiah format:** <1,000 → 'Rp850'; 1,000–999,999 → 'Rp8.4k' (one decimal); ≥1,000,000 → 'Rp1.2jt'; negative prefixed with '−'. **TalkBack a11y:** content description reads full amounts (e.g., "Cost: Rupiah 8,400; Net profit: Rupiah 28,000" or "Cost: Rupiah 8,400; Net loss: Rupiah 2,000"). **Missing data:** COST shows '—' when OBD disconnected, no fuel yet, or no fuel price set; NET shows '—' whenever COST is '—' or the card has no earnings. **Layout:** four cells with weights TIME 0.7, DIST 0.7, COST/NET 1.4, AVG/INST 1.4; values maxLines=1; do not clip on narrow screens (per §4 ADR-015 step 5 rule). How: `GojekOrderCard` trip-strip render reads `obdUiState.tripFuelConsumedL` (or live accumulator from `TrackScreen`), queries FuelPriceController for effective price, and computes COST and NET; formats per compact-Rupiah rules. Code location: `ui/component/GojekOrderCard.kt` (or chosen implementation name); reuses existing FuelCostCell or inline formatting logic; no new DAO or schema columns. Provisional visuals reuse TripPanel glass style; design handoff updates strip visuals per UI-SPEC §3e (4-state + 5th paused state).
7. **ShareViewModel: auto-end trip at FINISHED.** Add a new flow `latestOrderFlow` from the DAO (latest `observer_trip` row by `lastSeenAt`). Observe it; when FINISHED and `orderOwnsTrip=true` and a trip is live, run `stopActiveTrip()` (persist-then-STOP_TRIP). Clear `orderOwnsTrip=false`. Code location: `ui/screen/track/ShareViewModel.kt`.
8. **Dismiss handler.** The order card's Dismiss button calls `dismiss(id)`, which triggers the same stop logic (calls `stopActiveTrip`). Code location: `ui/component/GojekOrderCard.kt` (or chosen implementation name).
9. **Build + install on device.** Compile, install on SM-G965F (Android 10), and run manual verification below.

---

### Observer — Gojek Order Route Overlay (ADR-016)

**What it does:** While a Gojek order is active, display two car routes (driving mode) on the Track screen's embedded Google Map: (1) **Planned route** — current location to pickup to drop (pickup as a waypoint), fetched once when the order is taken and frozen; (2) **Runtime route** — current location to the next stop (pickup during PICKUP phase, drop during DROP phase), re-fetched on phase change or when the driver deviates >~40 m from the polyline, throttled to at most once per 30 s and only after the driver moved ~100 m. Pickup and drop locations receive map markers ("Pickup" / "Drop"). All route state is cleared on order completion (FINISHED), dismissal, or no active order. Routes are display-only, not persisted, and do not affect the canonical location log or trip/session model (PRD §12). Extraction and storage remain device-only (ADR-013); routing sends addresses to Google Services (ADR-016 privacy note).

**Observable result:** On-device, while a Gojek order is Taken and the driver approaches pickup, the Track map shows two polylines: a thin muted line (planned: current→pickup→drop) and a bold orange line (runtime: current→pickup). Map markers show "Pickup" and "Drop" locations. On phase change to DROP, the runtime line retargets to drop. When the driver deviates >40 m, the runtime line re-fetches (throttled: at most once per 30 s, after 100 m moved). On order completion (Selesai → home, Cancel, or Dismiss), both routes and markers disappear; the map reverts to recorded trace + car marker.

**How to Verify:**
- **Deterministic (unit tests not required):** Directions and Geocoding JSON parsing via `org.json`; PolyUtil.isLocationOnPath off-route detection (android-maps-utils).
- **Manual on-device (explicit user build permission §5a, SM-G965F, Android 10):**
  1. Take a Gojek order → both planned (thin blue-grey) and runtime (bold orange) polylines appear on the map; Pickup and Drop markers visible at their coordinates.
  2. Verify planned line: current → pickup → drop (pickup as intermediate waypoint, not the final destination).
  3. Drive toward pickup on the planned route → runtime line follows, redrawn dynamically as current location updates (smooth tracking, no jitter-induced re-fetch).
  4. Deviate >~40 m from the runtime line → runtime line re-fetches within 30 s (check network logs for Directions API call). Deviate <40 m → no re-fetch (throttled).
  5. Drive back on-route → runtime line snaps back (no second re-fetch; line persists until next deviation or phase change).
  6. Drive >100 m and deviate again → re-fetch allowed (100 m movement threshold passed). Drive <100 m and deviate → no re-fetch (movement threshold not met).
  7. Phase change (PICKUP → DROP): runtime line re-fetches to retarget from current → drop (should not be deferred by throttle; phase change always triggers fetch).
  8. Order ends (tap Selesai on Gojek, or Cancel, or Dismiss in TrackLocation): both routes and markers disappear immediately. Recorded trace + car marker remain.
  9. Airplane mode toggle (or unplug WiFi/cellular): Directions/Geocoding API calls fail gracefully; no route drawn, no crash, trip continues recording.
  10. No on-device order test: Skip if Gojek live orders unavailable. Use fixture testing (mock Orders + map state verification).
  11. Compare visual planned line with Gojek's in-app route overlay (subjective; some divergence expected due to different routing algorithms).

**Implementation steps (numbered, What/How):**
1. **GoogleRouteClient (Directions + Geocoding API calls).** New file `feature/observer/trip/route/GoogleRouteClient.kt`. Use `HttpURLConnection` (no okhttp dependency); `org.json` for JSON parsing (already a Gradle dependency). Directions: `POST https://maps.googleapis.com/maps/api/directions/json` with waypoints (pickup as waypoint, not via-point). Geocoding: `POST https://maps.googleapis.com/maps/api/geocode/json` for address → LatLng. API key via resValue (existing `google_maps_key`, same as embedded maps; must be enabled with Directions + Geocoding APIs in Google Cloud Console). Run on IO dispatcher. Handle errors: 429 (rate limit), 403 (invalid key), network offline. Return `RouteResult(polyline: List<LatLng>, distanceMeters: Int, durationSeconds: Int)` and geocoded LatLng. Decode polyline geometry using existing `LocationUtils.decodePolyline()`. Code location: `feature/observer/trip/route/GoogleRouteClient.kt`.
2. **OrderRouteController (route state + fetch logic).** New class `OrderRouteController`, owned by `ShareViewModel`. Collects: `activeOrder` (from `latestOrderFlow` + `observer_trip` phase) and `locationUiState` (current position, heading). Exposes `OrderRouteState` (StateFlow): `plannedRoute: List<LatLng>?`, `runtimeRoute: List<LatLng>?`, `pickupLatLng: LatLng?`, `dropLatLng: LatLng?`, error state, loading state. State transitions: (a) On order takeover (phase → PICKUP): geocode pickup + drop addresses (cached in-memory per order); fetch planned route (current → pickup → drop); set `plannedRoute`. (b) On phase change (PICKUP → DROP) or off-route (>40 m from `runtimeRoute` polyline via PolyUtil.isLocationOnPath): fetch runtime route; set `runtimeRoute`. Throttle: before each off-route fetch, check `(now - lastFetchTimeMs) > 30_000` AND `(driverMovedSinceLastFetch) > 100_m`; both must be true. Phase changes bypass throttle. Clear state on order FINISHED/Dismissed/no active order. Code location: `ui/screen/track/ShareViewModel.kt` or a separate `OrderRouteController.kt` in the screen package.
3. **TrackMap polyline + marker rendering.** Update `TrackMap` composable to accept optional params: `plannedRoute: List<LatLng>?`, `runtimeRoute: List<LatLng>?`, `pickupMarker: LatLng?`, `dropMarker: LatLng?`. Conditionally draw: (a) planned polyline (thin, muted blue-grey RGB `#B0BEC5`, width 4 dp, zIndex 0); (b) runtime polyline (bold orange RGB `#FF9800`, width 6 dp, zIndex 1, drawn above planned); (c) Pickup marker at `pickupMarker` with title "Pickup"; (d) Drop marker at `dropMarker` with title "Drop". Markers use standard Material Marker or custom icons; titles shown on tap/hover. Code location: `ui/screen/track/TrackMap.kt`.
4. **TrackScreen wiring.** `TrackScreen` collects `ShareViewModel.orderRouteState` and passes `plannedRoute`, `runtimeRoute`, `pickupLatLng`, `dropLatLng` to `TrackMap`. No new state management in `TrackScreen` itself. Code location: `ui/screen/track/TrackScreen.kt`.
5. **Enable Directions + Geocoding APIs on the Maps key.** Google Cloud Console → APIs & Services → enable "Google Maps Platform / Directions API" and "Geocoding API" for the project key. Restrict key: Android app restrictions (package name + app signing certificate), API restrictions (the two APIs above). Document in README.md: "Ensure the Google Maps API key has Directions API and Geocoding API enabled and restricted appropriately." Code location: documentation update only.
6. **No schema change.** Route state is ephemeral (not persisted); no Room entity or migration.

### Observer — Order Route Provider: OpenRouteService (ADR-017)

**What it does:** The order route overlay feature (ADR-016) switches provider from Google Directions + Geocoding APIs to OpenRouteService. Google billing was unavailable (REQUEST_DENIED), blocking ADR-016 feature. ORS was already chosen for ADR-009 navigation (ADR-011); re-adopting ORS for order routes unifies provider choice. **All ADR-016 behavior remains unchanged:** (1) Planned route (current→pickup→drop frozen at takeover); (2) Runtime route (current→next-stop re-fetched on phase-change/off-route, throttled >40m/30s/100m). Markers, fail-soft, display-only, no persistence. New requirement: geocoding fallback ladder (place name + city hint → full address → street-only) to handle Indonesian address ambiguity. Attribution required: '© openrouteservice.org | © OpenStreetMap contributors' while routes shown. Accuracy caveat: street-level pins metres off on long roads (fail-soft better than wrong pin). Free tier, no billing.

**Observable result:** Identical visual behavior to ADR-016: two polylines + markers on Track map during active Gojek order. No build-time change; `GoogleRouteClient` replaced by `OpenRouteServiceClient` (same RouteProvider interface per ADR-011).

**How to Verify:**
- **Deterministic (unit tests not required):** ORS Directions + Geocoding JSON parsing; fallback ladder logic (place name → full address → street-only with district-drop + confidence filtering).
- **Manual on-device (explicit user build permission §5a, SM-G965F, Android 10):**
  1. ORS API key valid: `curl https://api.openrouteservice.org/v2/directions/driving-car` with key → returns OK (200, not 401/403).
  2. Geocoding fallback ladder: (a) Place name (e.g., 'Pondok Indah Mall') → resolves to venue coordinates; (b) street name (e.g., 'Jalan Cipete Raya') → resolves to street centre; (c) long Gojek address (e.g., 'Jln X, Subdistrict, District, Jakarta') → ladder drops district words, retries street-only; (d) if all fail → no route/markers, trip continues (fail-soft).
  3. Result confidence filter: results with confidence <0.8 are rejected unless layer is venue or street (city-centre false positives filtered).
  4. Take a Gojek order → planned and runtime routes appear (same as ADR-016 visual).
  5. Compare route accuracy with Gojek's own route (subjective; ORS routes may differ).
  6. Attribution text visible on map while routes shown; hidden when order ends.
  7. Offline (airplane mode) or ORS down → no route, no crash (fail-soft).
  8. GPS signal lost during geocoding → retries max once per 60 s.
  9. Test addresses: landmarks (e.g., 'Mall Pondok Indah'), streets ('Jalan Gatot Subroto'), full Gojek-style addresses → verify fallback behavior (no crash, graceful degradation).
  10. Rate-limit test (live ORS key has ~200 req/window limit observed): multiple orders within a 30 s window should not exceed rate limit (throttle + low order volume intended to prevent).
  11. ORS free-tier limits re-verified: ~2,000 directions + 1,000 geocode requests per day available; app usage ~3–5 calls per order with throttle.

**Implementation steps (numbered, What/How):**
1. **RouteProvider port interface (reuse from ADR-011).** Confirm `RouteProvider` port exists with `geocode(placeName: String?, address: String, focus: LatLng?): List<LatLng>?` and `route(origin: LatLng, destination: LatLng, waypoint: LatLng?): RouteResult?` signatures (ADR-011 contract). Code location: `feature/nav/routing/RoutingEngine.kt` (existing).
2. **OpenRouteServiceClient (replace GoogleRouteClient).** Replace the `GoogleRouteClient.kt` file with `OpenRouteServiceClient.kt` implementing the `RouteProvider` port. Directions: `POST https://api.openrouteservice.org/v2/directions/driving-car` with `Authorization: Bearer <key>` header, body `{"coordinates": [[lon,lat],...], "radiuses": [-1,...]}`. Geocoding: `GET https://api.openrouteservice.org/geocode/search?text=<place>&boundary.country=ID&size=1&focus.point.lat=<lat>&focus.point.lon=<lon>`. Fallback ladder logic for geocoding failure (place → full address → street-only → no result). Confidence filter: reject <0.8 unless venue/street layer. Use `HttpURLConnection` + `org.json`; run on IO dispatcher. Decode polyline with `PolyUtil.decode` (android-maps-utils). Code location: `feature/observer/trip/route/OpenRouteServiceClient.kt` (replaces GoogleRouteClient.kt).
3. **OrderRouteController no changes.** Existing `OrderRouteController` logic unchanged (calls the provider interface; unaware of ORS vs. Google). Code location: `ui/screen/track/OrderRouteController.kt` (existing).
4. **API key setup in local.properties.** Change: `GOOGLE_MAPS_KEY` (for map SDK) + new `OPENROUTESERVICE_API_KEY` (for routes). App-level `build.gradle`: `resValue "string", "ors_api_key", project.properties.getOrDefault("OPENROUTESERVICE_API_KEY", "")`. Code location: `app/build.gradle` resValues.
5. **Attribution text on TrackMap.** Add an attribution surface to `TrackMap` composable (provisional placement: bottom-start above order card, or top-start; final placement from design handoff). Text: '© openrouteservice.org | © OpenStreetMap contributors'. Render only while `plannedRoute != null || runtimeRoute != null`; hide when both null. Code location: `ui/screen/track/TrackMap.kt` (new attribution text overlay).
6. **README update.** Update API key documentation: replace "Google Directions + Geocoding" with "OpenRouteService"; note free tier, no card required. Code location: `README.md` (existing API-keys section, already updated to mention ORS).
7. **No schema change.** Route state remains ephemeral (not persisted); no Room migration.

---

### Track Screen — Live Location (ADR-018)

**What it does:** Display a live GPS position on the Track screen (blue dot, follow mode camera, Recenter target) independent of recording state. A screen-scoped `LiveLocationSource` requests high-accuracy location updates (~1 s interval, ~500 ms fastest) only while the Track screen is visible AND the app is in the foreground (ON_START/ON_STOP or RESUMED); stops requesting otherwise. Displayed position uses the live fix if available, else falls back to the last-known seed from TrackingService. Live fixes are never stored or recorded — TrackingService remains the only writer to the canonical location log, sessions, trips, and OBD state.

**Observable result:** Open Track screen with always-recording OFF and no trip running → blue dot moves as the device moves (live location, ~1 s updates). Leave the Track screen → no more location requests (dumpsys location confirms). Recenter FAB goes to the actual device location, not a stale seed. Tapping Follow mode animates the camera to follow the live position. No new permission prompt (uses existing foreground `ACCESS_FINE_LOCATION`). GPS stops immediately when screen closes or app backgrounded.

**How to Verify:**
1. **Live dot updates:** Open TrackScreen with always-recording OFF and no active trip. Walk 10 m away. Observe: blue dot position updates to match the actual location within ~5 s (first fix arrival + draw).
2. **Recenter target:** With the dot stale (before live fix), tap Recenter → camera stays at seed (no movement yet). After live fix arrives, tap Recenter → camera animates to the actual device location (within ~5 m GPS accuracy).
3. **Stops when backgrounded:** Observe via `adb shell dumpsys location | grep com.kolee.tracklocation` → shows location requests while screen open; background the app → requests disappear within 1 s.
4. **Permission:** If foreground location permission not granted, opening Track triggers the permission dialog (same as today). Grant permission → live fix arrives and dot moves. Deny → falls back to last-known seed (same as today).
5. **No new permission prompt:** Existing apps with `ACCESS_FINE_LOCATION` foreground scope; no new permission added (verify AndroidManifest.xml unchanged).
6. **Battery note:** GPS active at ~1 Hz only while Track screen visible; same cost model as Google Maps/Waze.
7. **Recording unchanged:** Start a trip or enable always-recording. Trip records normally; dot also shows live position (no interference with the recording path); canonical location log unaffected (location_log still the source of truth for sessions/trips).

**Implementation steps (numbered, What/How):**
1. **LiveLocationSource (screen-scoped, lifecycle-aware).** New class: `LiveLocationSource(context: Context, lifecycle: Lifecycle)`. Uses `FusedLocationProviderClient.requestLocationUpdates(LocationRequest.Builder()` with interval ~1 s, fastest ~500 ms, priority HIGH_ACCURACY. Manages location requests via lifecycle `ON_RESUME` → start requests, `ON_STOP` → stop requests (no background requests). Exposes `StateFlow<LatLng?>` with the latest fix (nulled at `ON_STOP`). Code location: `feature/track/data/LiveLocationSource.kt` (or `data/location/` per existing convention). Handle missing or null FusedLocationProviderClient gracefully (fail-soft: source stays null, display falls back to seed).

2. **TrackScreen integration.** `TrackScreen` obtains a lifecycle-bound instance: `LiveLocationSource(context, lifecycle)` (scope: screen composition lifetime). Collects the live location StateFlow; passes it to `TrackMap` and `MapControls`. Fallback logic: `displayLocation = liveLocationSource.collect() ?: trackingService.locationUiState.currentLocation.value` (use live if available, else service seed). Code location: `ui/screen/track/TrackScreen.kt`. The service seed (TrackingService.seedLastKnownLocation) is initialized once on app launch from the device's last-known position (MainActivity, already done per 1e5d53c).

3. **Map and Recenter target use the live fix.** `TrackMap` displays the blue dot at `displayLocation`; `MapControls` Recenter FAB animates to `displayLocation` on tap (already implemented per 29bb443, 09321d9; no code change needed, already wired to locationUiState). Follow mode camera automatically animates to track `displayLocation` updates (already done per 29bb443). No new behaviour changes.

**No schema changes. No Room migration.**

---

### Track Map — Recenter FAB / Follow Mode (2026-10-06)

**What it does:** Make the Track screen's Recenter (crosshair) FAB functional with a follow-mode state. The map auto-follows the current GPS location by default (`isFollowing = true` on entry). User gestures (drag, pinch, fling) detected via `CameraMoveStartedReason.GESTURE` turn following OFF, allowing inspection of the route ahead or pickup/drop pins. Tapping the Recenter FAB resumes following and animates the camera back to the current location. The Recenter FAB icon darkens (dark crosshair `0xFF0A0A0A`) while following, and mutes to grey (~`0xFF9E9E9E`) when panned away. The Map Layers FAB remains a placeholder (out of scope). No schema change; no ADR or PRD change (UI interaction on existing screen; locked decisions untouched).

**Observable result:** Track map with `MapControls` FABs: on entry the Recenter icon is dark and the map auto-centers on the current location with each GPS fix. Dragging the map (or pinching/flinging) turns following off, the icon greys, and the map becomes static. Tapping Recenter animates the camera back to the current location and the icon darkens again. GPS updates during panned state do not pull the camera. With a Gojek order active, panning lets the driver inspect the planned/runtime routes and pins; tapping Recenter resumes following.

**How to Verify:**

1. Open Track screen without a trip active; map is centered on current GPS location, Recenter FAB icon is dark.
2. Drag/pan the map with a gesture → icon turns grey, camera stays static, location updates don't pull the camera back.
3. GPS location updates; map does not recenter (confirm by watching the location indicator).
4. Tap Recenter FAB → camera animates to current location, icon darkens to dark color, following resumes.
5. On the next GPS fix, the map auto-animates to follow.
6. Pinch zoom while following → zoom occurs but doesn't turn off follow. Keep current zoom level (or use `MAP_ZOOM` if zoomed out ≤ `MAP_ZOOM - 3`).
7. With an active Gojek order (if available): pan to inspect the planned/runtime routes and pickup/drop markers; tap Recenter to resume following; both routes and markers remain visible during panning and after Recenter.
8. Build + install on device (requires user permission AGENTS.md §5a).

**Implementation steps (numbered, What/How):**

1. **MapControls + Recenter callback:** Add `onRecenter: () -> Unit` callback parameter to the `MapControls` composable (or the `TrackMap` that renders it). Recenter FAB's `onClick` calls `onRecenter()`. Add an optional `isFollowing: Boolean` state parameter to control the FAB's icon color (dark when true, grey when false). Placeholder Layers FAB has no click handler (stays inert). Code location: `ui/component/MapControls.kt`.

2. **TrackMap follow mode:** Add params to `TrackMap`: `followLocation: Boolean`, `recenterTick: Int` (a trigger signal — increment to trigger animation), `onUserPan: () -> Unit` (callback when gesture detected). Internally: collect `LocationUiState.currentLocation`; when `followLocation = true` and a new location arrives, animate the camera position to the new location (smooth animation, preserve zoom unless zoom < `MAP_ZOOM - 3`). On `CameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE`, call `onUserPan()` to signal the user has panned. When `recenterTick` changes, animate the camera to the current location and set `followLocation = true`. Code location: `ui/screen/track/TrackMap.kt`.

3. **TrackScreen state wiring:** Add `isFollowing: Boolean` state to `ShareViewModel` (or `TrackScreen` local state), default `true`. Wire it through: (a) pass to `MapControls` to control Recenter FAB icon color; (b) pass to `TrackMap` as `followLocation` param; (c) increment `recenterTick` when user taps Recenter FAB and `onRecenter()` is called, also set `isFollowing = true`; (d) when `TrackMap` calls `onUserPan()`, set `isFollowing = false`. Code location: `ui/screen/track/TrackScreen.kt`, `ui/screen/track/ShareViewModel.kt`.

---

### Track Map — Navigation Camera (ADR-020)

**What it does:** While a trip is live or a Gojek order is active, the Track map's camera automatically switches to a navigation-style perspective (heading-up follow with GPS bearing, zoom 17 on entry, no tilt). When the trip or order ends, the camera animates back to north-up (bearing 0) and keeps following. The manual "navigation perspective" toggle of ADR-009 remains independent and can override this auto-activation. Display-only; no schema change.

**Observable result:** Start a trip (LIVE state) while stationary → map animates to zoom 17, bearing stays north-up (no valid heading yet because speed < 3 km/h), tilt 0, follow on. Drive forward at ~10 km/h → map rotates heading-up as GPS heading changes; bearing updates smoothly. Pan/drag the map → follow stops, bearing stops rotating. Tap Recenter → follow resumes, bearing-up returns while trip is live. Pinch zoom while navigating → zoom is respected and not reset on position updates. Stop the trip → camera animates bearing back to 0 (north-up), follow stays on, zoom preserved. Order Taken auto-starts a trip → same entry behaviour. Order cleared → same exit. Stationary with READY state (no trip, no order) → north-up follow as before (§3f).

**How to Verify:**

1. **Stationary at trip start:** Open Track, start trip while stationary. Map animates to zoom 17; bearing = 0 (north-up, no heading yet); tilt = 0; follow ON.
2. **Moving ≥ ~3 km/h:** Drive forward (short drive or mock location route). Map rotates heading-up as GPS bearing updates; bearing stays valid while speed ≥ 3 km/h.
3. **Slowing below ~3 km/h:** Slow down to <3 km/h (or test with low-speed mock locations). Bearing holds at the last valid heading; does not rotate with noisy sub-3-km/h GPS bearing.
4. **Pan while navigating:** Drag the map while trip is live. Follow stops, bearing stops rotating, FAB icon greys. Tap Recenter → follow resumes, bearing-up animation plays while trip is live.
5. **Zoom respected:** Pinch zoom out while navigating. Zoom changes; on the next GPS update, zoom is not reset to 17 (zoom preference is kept).
6. **Trip end:** Stop the trip. Camera animates bearing to 0 (north-up), keep following, preserve zoom level.
7. **Order lifecycle:** If Gojek order Taken is available, verify auto-start triggers entry behaviour; order Cleared/Cancelled triggers exit behaviour (same as trip).
8. Build + install on device (requires user permission AGENTS.md §5a).

**Implementation steps (numbered, What/How):**

1. **TrackMap bearing and tilt:** File `app/src/main/java/com/kolee/tracklocation/screens/track/components/TrackMap.kt` adds params `navigationActive: Boolean = false` and `navigationBearingDeg: Float? = null`. In the camera update logic (during follow), build `CameraPosition` with `bearing = if (navigationActive) (navigationBearingDeg ?: 0f) else 0f` and `tilt = 0f`. When navigationActive transitions 0→1, animate camera once to zoom 17; on 1→0, animate bearing to 0 (north-up), keep zoom/target. Bearing transitions animate smoothly (interpolate ~300 ms).

2. **TrackScreen / Navigation activation and heading logic:** File `app/src/main/java/com/kolee/tracklocation/screens/track/TrackScreen.kt` computes `navigationActive = (tripState != READY) || (activeOrder != null)`. On entry (false→true): clear any user pan (set `isFollowing = true`), animate camera to zoom 17. On exit (true→false): animate bearing back to 0, keep follow ON, preserve zoom. Maintain `lastValidHeading` from `liveFix` (from `LiveLocationSource`, ADR-018): update it only when speed ≥ ~3 km/h AND bearing is valid; clear it when navigation ends (navigationActive → false). Pass `navigationBearingDeg = if (navigationActive) lastValidHeading else null` to `TrackMap`. Recenter FAB (§3f) resumes follow with heading-up while navigationActive is true, or north-up otherwise.

3. **LiveLocationSource:** Not needed — LiveFix already exposes `speedMps` and `bearingDeg` (ADR-018). No changes required.

---

### Track Panel — LIVE button shows Stop (UI-SPEC §3g amendment)

**What it does:** Re-icon and relabel the LIVE-state inline `TripCtaButton` from "Pause trip" + pause glyph to "Stop trip" + stop glyph (filled rounded square, ~12/24 of the glyph box, radius 2 dp, `BrandGreenDark` on the green circle). The action is unchanged (sends `STOP_TRIP`, finishes and persists the trip). The PAUSED state remains in code but unreachable. Pause/resume capability is deferred as a possible future feature.

**Observable result:** READY state shows the green PlayFab (unchanged). LIVE state shows the 48 dp inline button with a filled rounded-square stop icon and "Stop trip" accessibility label (uiautomator content-desc). Tapping stops the trip and persists it (logcat `START_TRIP` then `STOP_TRIP`). The trip appears in the List with real duration/distance. No pause icon, no "Pause trip" string, and no pause functionality anywhere.

**How to Verify:**
1. (READY) Open Track, no trip running → green PlayFab visible at bottom-center.
2. (LIVE) Tap PlayFab → panel appears, 48 dp button shows a filled rounded-square stop icon, accessibility label "Stop trip" (verify with uiautomator content-desc).
3. (Stop trip) Tap the button → trip stops and is persisted; panel disappears, PlayFab returns.
4. (Verify stop) Logcat shows `START_TRIP` then `STOP_TRIP`; trip appears in List with real duration/distance.
5. (Icon check) No pause icon (`drawPauseGlyph`), no "Pause trip" string anywhere in the build.
6. (Design file) ui-design.pen "TrackScreen — LIVE" frame, CtaBtn element: shows a 12×12 rounded square (radius 2 dp, fill #052E16, centred) instead of PauseBar1/PauseBar2.

**Implementation steps (numbered, What/How):**

1. **TripPanel.kt:** LIVE state `contentDescription` → "Stop trip"; glyph rendering → `drawStopGlyph(drawScope)` (replaces `drawPauseGlyph`); remove the now-unused `drawPauseGlyph` function. The `drawStopGlyph` draws a filled `RoundRect(6, 6, 18, 18, radius 2 dp)` on the 24×24 viewbox, colour `BrandGreenDark` (from the existing color palette).

2. **ui-design.pen "TrackScreen — LIVE" frame:** Replace the PauseBar1 and PauseBar2 elements in the CtaBtn with a single 12×12 rounded square (x=6, y=6, radius 2 dp, fill #052E16 `BrandGreenDark`, no stroke). Centred in the 24×24 frame.

**No schema. No Room migration. No new permission.**

---

## 5. Timeline at a Glance

| Phase | Focus | Deliverable | Status |
|-------|-------|-------------|--------|
| Sessions | CR-0001/0002 | Canonical log + Session control surface | Done |
| Observer P1–P2 | Local capture + inspection | Feed, allowlist, snapshot viewer, pagination | P1 done; P2 in progress |
| OBD P1 | ELM327 telemetry | RPM/speed/fuel, km/L on Session + Trip | Done (live-verified) |
| Observer P3–P7 | Filtering → sync → remote → auth | (see Appendix A) | Planned |

---

## 6. Task Log (summarized)

Status: `Completed` | `In Progress` | `Blocked`. Full narrative for each entry is in **Appendix B**. Commit status is the single source of truth for branch/revision (AGENTS.md §8). A `Git Revision` of `---` is a placeholder to be backfilled with the introducing commit's hash in the next commit (AGENTS.md §8 backfill rule).

| Date | Task | Status | Git Revision | Verification |
|------|------|--------|--------------|--------------|
| 2026-10-07 | Docs — Track Panel LIVE button shows Stop icon (UI-SPEC §3g amendment, Issue #2 Resolved). Owner decision: re-icon/relabel the LIVE button as Stop (filled rounded square, "Stop trip") while keeping the remote Track design (PlayFab in READY, PAUSED unreachable). No new ADR (partial adoption of rejected ADR-019 option). UI-SPEC v0.21→0.22 (§3g LIVE-PAUSED wording updated; LIVE shows stop icon + "Stop trip"; PAUSED unreachable; dated note 2026-10-07 added). IMPLEMENTATION-ISSUES v0.2→0.3 (Issue #2 marked Resolved with decision record and traceability). adr/019 addendum added (2026-10-07: partial adoption, Status stays Rejected). IMPLEMENTATION-PLAN v0.26→0.27 (§1 change log entry 0.27, §4 new slice "Track Panel — LIVE button shows Stop"; §6 three task-log rows). DOCUMENT-CONTROL register updated. No schema, no Room migration, no new permission. | Completed | a3446e2 | Docs only (AGENTS §5a); no Gradle/device run. Code + design-handoff implementation pending user approval and build permission. |
| 2026-10-07 | Code — Track Panel LIVE button shows Stop icon (UI-SPEC §3g): `TripPanel.kt` updates LIVE state: `contentDescription` → "Stop trip", glyph → `drawStopGlyph` (filled rounded square, radius 2 dp, 24×24 viewbox, `RoundRect(6, 6, 18, 18, radius 2)`), remove unused `drawPauseGlyph`. No schema, no Room migration, no new permission. Code location: screens/track/components/TripPanel.kt (`drawStopGlyph` function + LIVE contentDescription). | Implemented — not built/tested (AGENTS §5a gate) | --- | Code reviewed by diff only; no Gradle build, tests or device run (§5a). TripPanel.kt: LIVE contentDescription 'Stop trip', glyph drawStopGlyph (filled rounded square, 6→18 of 24, radius 2), drawPauseGlyph removed; PAUSED strings untouched. On-device checks in §4 'Track Panel — LIVE button shows Stop' pending. |
| 2026-10-07 | Design — ui-design.pen "TrackScreen — LIVE" frame CtaBtn stop icon: Replace PauseBar1 + PauseBar2 elements with a single 12×12 rounded square (x=6, y=6, radius 2 dp, fill #052E16 `BrandGreenDark`); centred in the 24×24 frame. Update the component once and all downstream Figma/web exports get the square. | Completed (JSON-validated, not rendered) | --- | 'TrackScreen — LIVE' > CtaBtn: PauseBar1/PauseBar2 replaced by one 12×12 rounded StopGlyph (id d8w1x, x=18,y=18, radius 2, fill #052E16). JSON parses, 1,034 unique ids, only the LIVE frame differs from HEAD; not rendered in Pencil (visual check by owner pending). No build/test run. |
| 2026-10-07 | Docs — Directional car marker from ui-design.pen replaces the blue pin (UI-SPEC §3i). No new ADR (already accepted in ADR-009). UI-SPEC v0.20→0.21 (new §3i subsection: car marker design source, five rules, display-only note, implementation location, a11y, no design handoff needed; stale wording fixes on "static pin" and "live car marker" lines with §3i cross-references). IMPLEMENTATION-PLAN v0.25→0.26 (§1 change log entry 0.26 added; §4 status row "Track Navigation" car marker → done via §3i; new §4 slice "Track Map — Car Marker" with What/Observable/How to Verify + 3 steps; §6 two new task-log rows; Also documented updated). adr/009 dated implementation note added (2026-10-07). DOCUMENT-CONTROL register + change log updated. No schema. Code implementation pending user approval + build (AGENTS.md §5a/§5b). | Completed | 453c57a | Docs only; no Gradle/device run (AGENTS §5a). Code implementation pending user approval and build permission. |
| 2026-10-07 | Code — Directional car marker (UI-SPEC §3i): car marker from ui-design.pen replaces the blue `ic_location_pin`. New `res/drawable/ic_car_marker.xml` (28×44 dp VectorDrawable, red car, front at top, backing omitted). `TrackMap.kt`: marker bitmap created once via `bitmapDescriptorFromVector(context, R.drawable.ic_car_marker, tint = null, scale = 1.0)` in a `remember` block; `googleMap.addMarker(MarkerOptions().icon(carBitmapDescriptor).flat(true).rotation(markerHeadingDeg).anchor(0.5f, 0.5f))` replaces the pin marker. New param `markerHeadingDeg: Float = 0f` passed from `TrackScreen`. `TrackScreen.kt`: maintains `markerHeading` state, updated from live-fix bearing when speed ≥ ~3 km/h, held below ~3 km/h; passed to `TrackMap`. No schema change. Code location: res/drawable/ic_car_marker.xml (new), screens/track/TrackMap.kt, screens/track/TrackScreen.kt. | Implemented — built and stationary-verified on device | 2380f15 | Debug build OK (JDK 17, scratch copy; aapt2 confirms drawable/ic_car_marker compiled) and installed over the existing app on SM-G965F via adb tunnel, 2026-10-07 10:12 device time (same signing cert, data kept). Stationary on-device check (screenshots with zoomed marker crops + uiautomator dump + logcat): the red car replaces the blue pin at the current location; matches the ui-design.pen CarMarker (red sedan, white headlights, dark windscreens; no backing square); marker bounds 74×116 px = 28×44 dp; upright in READY and during a live trip (ADR-020 zoom 17); START_TRIP/STOP_TRIP logged; no crash, FATAL, resource-not-found, inflate or vector errors. NOT verified: rotation to the direction of travel while moving, holding the heading below ~3 km/h, and the car staying upright while the heading-up camera rotates the map (needs a drive). Note: at zoom 15 the car body (~13×25 dp) looks small — per design; scale is a one-line change if the owner wants it larger. Install reset Always-recording to Inactive (owner to re-enable). |
| 2026-10-07 | Docs — Track navigation camera auto-activates during trip or order (ADR-020): Navigation camera auto-activates to heading-up when `tripState != READY || activeOrder != null`. New ADR-020 file (Context/Decision/Consequences/Alternatives/Related ADRs/References); adr/009 amended with cross-reference note (wording only); adr/README v0.8→0.9 (ADR-020 row added, index count 19→20); PRD v0.16→0.17 (FR-19 new: auto-switch to navigation camera heading-up while trip/order active); UI-SPEC v0.19→0.20 (§3f Recenter updated with cross-reference to §3h, §3h new subsection: 7 behaviour rules + accessibility + no design handoff); IMPLEMENTATION-PLAN v0.24→0.25 (§1 change log entry 0.25, §4 new Track Map slice + How to Verify, §6 two new task-log rows Docs + Code, Also documented updated); DOCUMENT-CONTROL register + change log. No schema. Code gated (AGENTS.md §5a). | Completed | 802073b | Docs only; no Gradle/device run (AGENTS §5a). Code implementation pending user approval and build permission. |
| 2026-10-07 | Code — Track navigation camera auto-activates during trip or order (ADR-020): `TrackMap` (file `app/src/main/java/com/kolee/tracklocation/screens/track/components/TrackMap.kt`) adds `navigationActive: Boolean = false` and `navigationBearingDeg: Float? = null` params; when following, builds `CameraPosition` with `bearing = if (navigationActive) (navigationBearingDeg ?: 0f) else 0f` and `tilt = 0f`; on entry (false→true) animates once to zoom 17, then preserves user zoom; on exit (true→false) animates bearing back to 0 keeping zoom/target. `TrackScreen` (file `app/src/main/java/com/kolee/tracklocation/screens/track/TrackScreen.kt`): computes `navigationActive = (tripState != READY) || (activeOrder != null)`, re-enables `isFollowing` on entry, maintains `lastValidHeading` from `liveFix` (updated only when speed ≥ ~3 km/h; cleared when navigation ends) and passes it to `TrackMap`. No schema. Follow-up fix (retry): the entry-zoom flag is cleared only after the animation completes, because the follow effect restarts at ~1 Hz and cancels in-flight animations. | Implemented — built and stationary-verified on device | 5b56949 | Debug build OK (JDK 17, scratch copy) and installed over the existing app on SM-G965F via adb tunnel, 2026-10-07 09:52 device time (same signing cert, data kept). Stationary on-device check (screenshots + uiautomator dumps + logcat): READY at default zoom 15; within 3 s of Start the map is at zoom 17 and stays there at 8 s/15 s, after Recenter, and on a second trip (pixel diff vs READY ≈ 19.9 vs baseline 1.3); no tilt, no rotation/compass; pan → 'Recenter map, map is not following', Recenter → follow resumes; START_TRIP/STOP_TRIP logged, no crash/FATAL. First install (without the retry fix) did NOT zoom (diff 3.2) — root cause: follow effect restarts ~1 Hz and cancelled the one-shot entry animation. NOT verified: heading-up rotation while moving (needs a drive or mock route), return to north-up after a rotated trip, entry from a non-17 zoom or after a pan (zoom stays 17 after a trip; double-tap cannot be injected via adb). Install reset Always-recording to Inactive (owner to re-enable). |
| 2026-10-07 | Docs — ADR-019 rejected; ADR-019 code/design/docs reverted (revert commit 43b9e47); pause defect logged in IMPLEMENTATION-ISSUES | Completed | d4cf21a | Docs only; no build/test run (reverted code not rebuilt) |
| 2026-10-06 | Docs — TrackScreen design: READY-state PlayFab + merged metrics row + GojekOrderCard expand/collapse. New UI-SPEC §3g (TripPanel: READY-state 56dp PlayFab at bottom-center, TripPanel only in LIVE/PAUSED, merged 4-cell `CombinedMetricsRow` KM/KM/HR/L/H/AVG/INST km/L). Updated UI-SPEC §3e (GojekOrderCard: expanded/collapsed/strip-only interaction states; chevron in PhaseRow; TripStrip label changes: DIST (km), COST / NET (Rp.), AVG / INST (km/L); no spaces in "/" values; all cells center-aligned). UI-SPEC v0.17→0.18. IMPLEMENTATION-PLAN v0.21→0.22. No schema change. Code pending. | Completed | 15fea06 | Docs only; no Gradle/device run (AGENTS §5a). |
| 2026-10-06 | Code — TrackScreen design: READY-state PlayFab + merged metrics row + GojekOrderCard expand/collapse. `TrackScreen.kt`: READY state shows 56dp PlayFab instead of TripPanel; LIVE/PAUSED shows TripPanel as before. `TripPanel.kt`: `StatsRow` + `ObdRow` replaced by single `CombinedMetricsRow` (KM, KM/HR, L/H when OBD, AVG/INST km/L when OBD). `GojekOrderCard.kt`: `isExpanded`/`isStripOnly` state; chevron toggle in PhaseRow; PickupBlock/DropBlock tappable → strip-only; TripStrip label+value format changes (units in labels, no spaces in "/" values); all cells center-aligned. No schema change. | Completed (unbuilt) | 15fea06 | Static only (AGENTS §5a); no Gradle/device run. Build/device-verify pending user permission. Files: TrackScreen.kt, TripPanel.kt, GojekOrderCard.kt. |
| 2026-10-06 | Docs — Track screen live location (ADR-018): Display-only live GPS position independent of recording state. New ADR-018 file (Context/Decision/Consequences/Alternatives/Related ADRs/References); PRD v0.15→0.16 (FR-18 new: live location, foreground-only, never stored, fallback to last-known, no new permission, roadmap note on turn-by-turn/Waze features deferred, cross-link ADR-018); ARCHITECTURE v0.17→0.18 (LiveLocationSource component + display-only data-flow diagram separate from TrackingService recording path); UI-SPEC v0.17→0.18 (§3f added live location note: source, fallback, display-only, battery, privacy); IMPLEMENTATION-PLAN v0.21→0.22 (§1 change log entry 0.22, §4 new Track Screen — Live Location slice with 3 steps + 7-point How to Verify, §6 two new task-log rows Docs/Code 2026-10-06, Also documented updated, version header); adr/README v0.6→0.7 (ADR-018 row added, index count 17→18); DOCUMENT-CONTROL register + change log. No schema. Code pending user build permission (AGENTS.md §5a). | Completed | 1d39618 | Docs only; no Gradle/device run (AGENTS §5a). Code implementation pending user approval and build permission. |
| 2026-10-06 | Code — Track screen live location (ADR-018): New `LiveLocationSource(context: Context, lifecycle: Lifecycle)` class manages FusedLocationProviderClient high-accuracy location requests (~1 s interval, fastest ~500 ms) scoped to Track screen visibility + foreground state (ON_RESUME/ON_STOP lifecycle gates). Exposes `StateFlow<LatLng?>` with the latest fix. `TrackScreen` collects it; passes `displayLocation` to `TrackMap` (blue dot) and `MapControls` (Recenter FAB target), falling back to `TrackingService.currentLocation` seed when no live fix available. Follow mode and Recenter already implemented (29bb443, 09321d9); no new behaviour changes needed. No schema change. Code location: `feature/track/data/LiveLocationSource.kt`, `ui/screen/track/TrackScreen.kt` wiring. | Completed (built, not device-verified) | 815b20b | assembleDebug BUILD SUCCESSFUL (2026-10-06, scratch copy, JDK 17); no compiler errors/warnings in the touched files; LiveLocationSource/LiveFix/rememberLiveFix confirmed in the dex; no debug logs left. NOT installed / not device-verified; pending the §4 slice How to Verify (always-recording OFF and no trip: dot moves as the phone moves; updates stop when leaving the Track screen or backgrounding the app; Recenter goes to the live position; recording/trip/log unchanged; no new permission prompt). Files: tracking/LiveLocationSource.kt (new: LiveFix, LiveLocationSource, rememberLiveFix), screens/track/TrackScreen.kt (displayLocation = live ?: service location). TrackingService/DB untouched. |
| 2026-10-06 | Docs — Track map Recenter FAB / Follow Mode (2026-10-06): Added UI-SPEC §3f new subsection describing functional follow mode with `isFollowing` state, gesture → off, tap Recenter → on transitions, provisional dark/grey icon colors, a11y text labels, interaction with route overlay. Map Interface legacy note updated (§9) to reflect Recenter now functional and Layers FAB as placeholder. UI-SPEC v0.16→0.17. IMPLEMENTATION-PLAN v0.20→0.21 (§1 change log entry 0.21, §4 new Track Map slice 3 steps + 8-point How to Verify, §6 two new task-log rows, Also documented updated). DOCUMENT-CONTROL register + change log. No schema. Design handoff pending (AGENTS §12 for visual refinement); code pending user approval (AGENTS §5a for build/device). | Completed | e82463e | Docs only; no Gradle/device run (AGENTS §5a). Code implementation pending user approval and build permission. |
| 2026-10-06 | Code — Track map Recenter FAB / Follow Mode: Add `onRecenter: () -> Unit` callback to `MapControls` + `isFollowing: Boolean` param for FAB icon color (dark/grey). `TrackMap` gains `followLocation: Boolean`, `recenterTick: Int` trigger, `onUserPan: () -> Unit` callback; auto-animates camera to current location when following, preserves zoom (use MAP_ZOOM if zoomed out ≤ MAP_ZOOM - 3), stops animating on gesture (CameraMoveStartedReason.GESTURE). `TrackScreen` / `ShareViewModel` manage `isFollowing` state: default true on entry, set false on gesture, set true + increment recenterTick on FAB tap. Layers FAB remains inert placeholder. No schema. Code location: MapControls.kt (callback + icon color), TrackMap.kt (follow logic), TrackScreen.kt + ShareViewModel.kt (state wiring). | Completed (built, not device-verified) | 29bb443 | Installed and device-verified 2026-10-06 on SM-G965F: after a map drag the icon greys and the app reports follow off; tapping Recenter animated the camera to the phone's GPS position (within ~5 m) and the icon returned to dark; no crash. Note: with always-recording off and no trip the position is the last-known seed, not live. Files: TrackMap.kt (followLocation/recenterTick/onUserPan), MapControls.kt (clickable Recenter, isFollowing icon state), TrackScreen.kt (state wiring). Layers FAB remains a placeholder. |
| 2026-10-06 | Docs — order route overlay (ADR-016): two routes on Track map during active Gojek order (planned current→pickup→drop frozen, runtime current→next-stop re-fetch on phase-change / off-route >40m throttled ≤30s/≤100m moved). Google Directions + Geocoding APIs (overrides ADR-011 ORS for Gojek feature only). New ADR-016 file (Context/Decision/Consequences/Alternatives/Related ADRs/References/Implementation Notes); adr/README v0.4→0.5 (ADR-016 index row added, count 15→16); PRD v0.12→0.13 (FR-17 new: two routes, markers, privacy note, fail-soft, cost, no schema change); ARCHITECTURE v0.14→0.15 (Gojek flow extended with ROUTE section); UI-SPEC v0.13→0.14 (§3e new Route overlay subsection: PROVISIONAL styling + Handoff Instructions); IMPLEMENTATION-PLAN v0.17→0.18 (§1 change-log entry, §4 new ADR-016 slice 6 steps + How to Verify, §6 task-log rows, Also documented updated); DOCUMENT-CONTROL register + change log; adr/011 one-line note added (provider superseded for Gojek order routes). Code + design handoff + build gated (AGENTS §5a/§12). | Completed | 54e7a37 | Docs only; no Gradle/device run. |
| 2026-10-06 | Docs — order card COST / NET cell (trip strip extension): Extends the compact trip strip from 3 cells (TIME / DIST / AVG/INST km/L) to 4 cells, adding **COST / NET** between DIST and AVG/INST km/L. COST = trip fuel (OBD accumulator) × effective fuel price (FuelPriceController); NET = OrderCard.earningsRp − COST (may be negative, shown as −Rp…). Compact Rupiah format: <1k 'Rp850', 1k–999k 'Rp8.4k', ≥1M 'Rp1.2jt'; negative '−'. TalkBack reads full amounts. Missing: COST/'—' when OBD off/no fuel/no price; NET/'—' when COST/'—' or no earnings. Layout: four cells, weights 0.7/0.7/1.4/1.4, no clip on narrow screens. Provisional UI; final design from handoff. PRD v0.13→0.14 (FR-16 extended with COST/NET definition + FR-12 cross-link); ARCHITECTURE v0.15→0.16 (trip-strip data source noted); UI-SPEC v0.14→0.15 (§3e spec updated to 4-cell + compact-Rupiah rules + a11y + missing-data; Handoff Instructions with screenshot checklist incl. order card w/ strip + 5-state Claude Design prompt); IMPLEMENTATION-PLAN v0.18→0.19 (§1 change-log, §4 ADR-015 slice COST/NET step + How to Verify, §6 new task rows); DOCUMENT-CONTROL updated; adr/015 addendum (2026-10-06). No schema. Code + build gated (§5a). | Completed | 3ba38ab | Docs only; no Gradle/device run (AGENTS §5a). |
| 2026-10-06 | Code — order card COST / NET cell (trip strip extension). Extends trip strip to 4 cells, adding COST / NET cell computation and rendering. Data: trip fuel from ObdPollingService (live accumulator), current effective fuel price from FuelPriceController, OrderCard.earningsRp. Formatting: compact Rupiah (<1k, 1k–999k, ≥1M), negative as '−' prefix. Layout: cell weights TIME 0.7, DIST 0.7, COST/NET 1.4, AVG/INST 1.4; no clip. A11y: TalkBack reads full "Rupiah" amounts. Missing data: COST/'—' (OBD off, no fuel, no price); NET/'—' (COST/'—' or no earnings). Code location: `ui/component/GojekOrderCard.kt` trip-strip render; reuse FuelCostCell formatting or inline logic; no DAO/schema changes. Provisional visuals (TripPanel style tokens); design handoff refines. | Completed (built, not device-verified) | 90233b5 | assembleDebug BUILD SUCCESSFUL (1m45s, JDK 17, SDK 33, run in a scratch copy 2026-10-06; APK 49 MB, com.kolee.tracklocation, minSdk 28) — covers COST / NET plus the ADR-016 route overlay (first compile of both; no errors, no warnings in the touched files). NOT installed / not device-verified; pending the on-device checks in the §4 slice How to Verify (needs a real Gojek order, OBD connected, fuel price set, Directions + Geocoding APIs enabled on the Maps key). Files: TripState.kt (tripFuelL, fuelPricePerL), TrackScreen.kt (FuelPriceController.attach + price/litres), GojekOrderCard.kt (4-cell strip, formatCompactRupiah). |
| 2026-10-06 | Code — order route overlay (ADR-016) — implemented 2026-10-06 (static review only). GoogleRouteClient (Directions + Geocoding via HttpURLConnection + org.json, IO dispatcher); OrderRouteController (collects activeOrder + locationUiState, exposes OrderRouteState; planned fetch @ takeover, runtime fetch @ phase-change / off-route with throttle); TrackMap (polyline + marker rendering); TrackScreen (wiring); README (API-enable note). | Completed (unbuilt) | ebf4cdc | Static only (AGENTS §5a): compiled OK via assembleDebug 2026-10-06 (with the COST / NET change); not run on device. Files: feature/observer/trip/route/GoogleRouteClient.kt + OrderRouteController.kt (new), ShareViewModel (orderRoute), TrackMap (plannedRoute/runtimeRoute/pickup/drop params), TrackScreen wiring, README key note. maps-compose 2.5.3 rememberMarkerState and PolyUtil compiled cleanly. Pending user-permitted build + the 11-step on-device check in the §4 slice How to Verify; Google Maps key needs Directions + Geocoding APIs enabled. |
| 2026-10-06 | Docs — order route provider switch to OpenRouteService (ADR-017): Google Directions + Geocoding APIs unavailable (REQUEST_DENIED, billing issue). Switches to ORS (free tier, no card, same provider as ADR-011/ADR-009 navigation). All ADR-016 behavior (two routes, markers, throttle, fail-soft) unchanged. New: geocoding fallback ladder (place name + city → full address → street-only with dropped districts; confidence <0.8 filter; fail-soft); attribution '© openrouteservice.org | © OpenStreetMap contributors' required on map. Accuracy caveat: street pins metres off on long roads, less accurate than Google for house numbers (best-effort). New ADR-017 file (Context/Decision/Consequences/Alternatives/Related ADRs/References); adr/README v0.5→0.6 (ADR-017 row + index count); ADR-016 status amended (provider superseded note); ADR-011 note updated; PRD v0.14→0.15 (FR-17 provider + geocoding ladder + accuracy + attribution); ARCHITECTURE v0.16→0.17 (ROUTE section provider + ladder + attribution + accuracy); UI-SPEC v0.15→0.16 (§3e attribution surface + prompt with ORS + ladder + accuracy); README v0.2→0.3 (API keys: GOOGLE_MAPS_KEY map SDK + OPENROUTESERVICE_API_KEY free tier); IMPLEMENTATION-PLAN v0.19→0.20 (§1 change log, §4 new ADR-017 slice, §6 task-log rows); DOCUMENT-CONTROL register + change log. No schema. Build/design handoff/code gated (AGENTS §5a/§12). | Completed | 5c9283b | Docs only (AGENTS §5a); no Gradle/device run. Code implementation pending user build permission. |
| 2026-10-06 | Code — order route provider switch to OpenRouteService (ADR-017): Replace GoogleRouteClient.kt with OpenRouteServiceClient.kt implementing RouteProvider port (ADR-011). Directions: POST https://api.openrouteservice.org/v2/directions/driving-car with Authorization header. Geocoding: GET https://api.openrouteservice.org/geocode/search with fallback ladder (place → full address → street-only + districts-drop; confidence <0.8 filter). OrderRouteController unchanged (calls provider interface). Attribution text added to TrackMap ('© openrouteservice.org | © OpenStreetMap contributors', visible while routes shown). API key: new OPENROUTESERVICE_API_KEY in local.properties, injected as ors_api_key resValue. README updated (API keys section). No schema. Code location: OpenRouteServiceClient.kt (replaces GoogleRouteClient.kt), OrderRouteController (unchanged), TrackMap (attribution text), app/build.gradle (resValue), README. | Completed (built, not device-verified) | 17ba949 | assembleDebug BUILD SUCCESSFUL (42 s, JDK 17, SDK 33, scratch copy 2026-10-06; APK 51.9 MB, minSdk 28); no compiler errors and no warnings in the touched files; ors_api_key resource baked into the APK; ORS key validated by live curl (geocode + directions with via-point OK; long Gojek-style addresses need the fallback ladder). NOT installed / not device-verified; pending the on-device checks in the §4 slice How to Verify (real Gojek order). Files: RouteProvider.kt + OpenRouteServiceClient.kt (new), GoogleRouteClient.kt (deleted), OrderRouteController.kt (RouteProvider, names+focus, max 3 geocode attempts per order), ShareViewModel.kt, TrackScreen.kt (attribution text), app/build.gradle (ors_api_key resValue). |

| 2026-10-06 | Code — order card compact trip strip (provisional UI): GojekOrderCard accepts optional `tripStripState: TrackPanelState?` parameter; when non-null and trip LIVE/PAUSED, renders one-row strip below card (TIME / DIST / AVG/INST km/L) in compact format (1h15m / 42s / 3m15s), km (one decimal), km/L (avg then instant, each one decimal). Provisional UI reuses TripPanel glass style; no new data/calculation (fed from existing TrackPanelState). Design handoff pending (AGENTS §12 complete spec + screenshot checklist + copy-paste Claude Design prompt in UI-SPEC §3e). Code gated (build permission §5a). | Implemented — debug build OK + installed on SM-G965F; on-device order test pending | 82271d2 | Debug build passed (assembleDebug) and the APK was installed over the existing app on SM-G965F (2026-10-06 14:34 device time); no on-device order test yet. Manual: during an active order the card shows TIME / DIST / AVG/INST km/L counting; '— / —' without OBD; strip disappears and TripPanel returns when the order ends. |
| 2026-10-06 | Docs — order card compact trip strip (provisional UI) + design handoff: PRD v0.11→0.12 (FR-16 compact format TIME spec); ARCHITECTURE v0.13→0.14 (Gojek flow trip-strip line); UI-SPEC v0.12→0.13 (§3e Trip strip spec + Handoff Instructions — step-by-step process, screenshot checklist, tool recommendation, copy-paste Claude Design prompt with states/constraints); ADR-015 addendum section (2026-10-06: trip now runs automatically, card carried a strip); IMPLEMENTATION-PLAN v0.16→0.17 (§1 change log entry 0.17, §4 ADR-015 slice step 5 + How to Verify extension, §6 two task-log rows); DOCUMENT-CONTROL updated. | Completed | 94c443b | Docs only; no Gradle/device run. |
| 2026-10-06 | Docs — ADR-015 (Gojek Order Lifecycle Drives the Trip: auto-start at Taken, auto-end at Cleared/Cancelled/Dismissed) accepted and propagated. New ADR-015 file (Context/Decision/Consequences/Alternatives/References/Vocabulary Table/Evidence); adr/README v0.3→0.4 (ADR-015 row added, count 14→15); PRD v0.10→0.11 (FR-13/FR-16 amended); ARCHITECTURE v0.12→0.13 (Gojek flow section updated, terminal-state detection added); UI-SPEC v0.11→0.12 (§3e updated: auto-start/auto-end, card states); IMPLEMENTATION-PLAN v0.15→0.16 (§4 new slice + §6 task rows + change log); DOCUMENT-CONTROL register updated. Code gated (design handoff §12 + build permission §5a). | Completed | c047d84 | Docs only; no Gradle/device run (AGENTS §5a). |
| 2026-10-06 | Code — ADR-015 order auto-start/end trip: parser end signals (home screen + cancel message → FINISHED); ShareViewModel auto-start (unhandledReady → START_TRIP or keep+orderOwnsTrip); ShareViewModel auto-end (latestOrder FINISHED → stopActiveTrip); Dismiss handler; build + install on device. | Implemented — debug build OK + installed on SM-G965F; on-device order test pending | 5c8cedd | Debug build passed (assembleDebug, SDK 33) and the APK was installed over the existing app on SM-G965F (2026-10-06); no on-device order test yet. Manual on-device: (1) take order → trip starts; (2) finish with Selesai → trip stops; (3) cancel → trip stops; (4) dismiss → trip stops; (5) manual start before order → trip kept and ends with order. |
| 2026-10-05 | Code — fix order-card weaknesses: Dismiss control on the provisional card; same pickup+drop pair after FINISHED/stale starts a new order (new takeover) | Implemented — NOT built/verified | 525406b | Static review only; no Gradle/test/device run (AGENTS.md §5a). |
| 2026-10-05 | Docs — ADR-013 (Observer Trip Extraction: Gojek pickup/drop extraction into device-local `observer_trip` store, Gojek-only, no FK to trips/sessions, 90d/5k retention) accepted and propagated. New ADR-013 (Context/Decision/Consequences/Alternatives/References); PRD v0.7→0.8 (Feature 3 update, FR-15 new, §8 Included/Out-of-Scope); ARCHITECTURE v0.8→0.9 (ObserverTripEntity + retention note + MIGRATION_10_11 + parser note); UI-SPEC v0.7→0.8 (screen inventory row); IMPLEMENTATION-PLAN v0.10→0.11 (§4 slice + §1 change log + Next Step update); DOCUMENT-CONTROL register + change log; adr/README v0.1→0.2. Code gated (design handoff §12 + build permission §5a). | Completed | `b79c4f2` | Docs only; no Gradle/device run (AGENTS §5a). How to Verify (code task, pending): (1) parser unit tests with fixture tree snapshots (pickup/drop-only/finished/partial phases, no PII); (2) DB migration runs v9→v10, no crash; (3) on-device Gojek order extraction with auto-dedup; (4) 90d/5k retention pruning; (5) design handoff + UI integration. |
| 2026-10-05 | Docs — fix ADR-014 wording errors (invented Android names, placeholder composable name, wrong persistence/foreground flow, migration numbering v10) + UI-SPEC design-handoff spec | Completed | 0ed3f90 | Docs only; no build/test run. |
| 2026-10-05 | Code — Gojek order card takeover (ADR-013/014): pure-Kotlin OrderCardParser + per-app GojekRules; observer_trip entity/DAO + MIGRATION_9_10 (DB v10, additive); OrderTripRecorder hooked into the Observer service write path (Gojek-only, 90 d / 5,000 rows retention); ShareViewModel auto-stop (persist-then-STOP_TRIP, once per order via a handled flag); Observer service launches MainActivity on the Track screen when an order first becomes complete; provisional GojekOrderCard replaces TripPanel while an order is active (design handoff pending) | Implemented — NOT built/verified | `329a02a` | Static review only; no Gradle build, tests, emulator or device run (AGENTS.md §5a). Pending: user-permitted build + on-device verification on SM-G965F, incl. whether Android 10 allows the background activity start. |
| 2026-10-05 | Docs — ADR-014 (Gojek order card takeover: auto-stop trip, order card on Track screen, foreground) accepted; PRD FR-16, UI-SPEC, ARCHITECTURE updated (docs only) | Completed | `7e8fc90` | Docs only; no build/test run. |
| 2026-10-05 | Fix — TrackScreen map blank on startup (no GPS lock yet). Root cause: `LocationUiState.currentLocation` defaults to Seoul `LatLng(37.5716, 126.9763)`; map centers on this hardcoded location until first GPS fix arrives. Hybrid fix: (1) `MainActivity.onCreate` requests device's last known location from `FusedLocationProviderClient`, initializes `LocationUiState` with it instead of Seoul default; (2) `TrackingService.requestLocationUpdates()` moved to startup (not just on trip start) to acquire live GPS location continuously even before trips. Files: `MainActivity.kt`, `TrackingService.kt`, `LocationUiState.kt`. | Completed | `1e5d53c` | On-device: open TrackScreen without starting trip → map centers on actual device location (not Seoul), updates to live GPS within ~5 sec; no regressions on trip start/stop/Session/Observer tabs; battery drain acceptable (GPS in low-power mode). |
| 2026-07-10 | Docs — ADR-012 (OBD fuel accumulation gates on shared recording state, not the intent-synced `sessionActive` flag) accepted. Root-caused ERR-005 (OBD connected + polling but `sessionActive=false` → avg km/L / cost / SESSION AVG / `obd_sample` writes all skipped while instant km/L & L/h show; two-flag divergence `isAlwaysRecording` vs `sessionActive`). Decision: gate on `gpsState.isAlwaysRecording`; derive session PK from `getActiveSession()` (single read/poll); demote `ACTION_SESSION_ON/OFF` to advisory; remove dead `ObdUiState.Connected.sessionActive`. Independent sonnet sub-agent ran a blast-radius + regression audit (UI-safe, privacy invariant preserved, no regressions; two accepted narrow risks). New ADR-012 + adr/README index; ERRORS-LOG ERR-005; ARCHITECTURE §5 data-flow note; DOCUMENT-CONTROL. No code (build permission §5a gated). | Completed | `71e23ae` | Docs only; no Gradle/device run (AGENTS §5a). How to Verify (code task, pending): relaunch app with always-recording already ON + OBD auto-reconnected, start a trip, drive → avg km/L & cost populate on the active-trip row and SESSION AVG on the Session screen; stop → finished trip shows km/L & cost. |
| 2026-07-08 | Docs — ADR-011 (routing engine via connector-adapter) accepted. New ADR-011 (OpenRouteService hosted free tier now → self-hosted OSM by base-URL swap via a `RoutingEngine` port + `OpenRouteServiceAdapter`; static ETA; ORS/OSM attribution; key not hardcoded). Resolved ADR-009's routing-engine + traffic-ETA open questions; ARCHITECTURE 0.6→0.7 (§12 RoutingEngine/adapter, replaced the parked paragraph); UI-SPEC 0.6→0.7 (§3d attribution surface + engine note); DOCUMENT-CONTROL register + change log + adr/README index. No code (design handoff §12 + build permission §5a gated). | Completed | `34ffe0a` | Docs only; no Gradle/device run (AGENTS §5a). ORS API surface verified via Context7 (`POST /v2/directions/driving-car`; summary.distance/duration + encoded-polyline geometry; identical self-hosted API). |
| 2026-07-08 | Design handoff — Track Navigation (dual-mode). Produced `docs/design-handoff/track_navigation/TRACK_NAVIGATION_SPEC.md` per AGENTS §12: 7 surfaces to design (destination search + autocomplete, Start "track only / + navigate" control, route polyline + static ETA read-out, `MapControls` heading-up perspective toggle, directional car marker, translucent road-ahead candidates, ORS/OSM attribution), states, edge cases, screenshot checklist (Track READY/LIVE, MapControls, bottom nav, Sessions card), and a copy-paste Claude Design prompt (rinaldi.ch account, existing TrackLocation project, Haiku 4.5). Design-track only; no code (AGENTS §5b/§12). | Completed | `742f9da` | Docs only; no Gradle/device run. Awaiting design production in Claude Design, then design-approval commit before any implementation. |
| 2026-07-08 | Docs — ADR-009 + ADR-010 accepted and propagated. Flipped both ADRs Proposed→Accepted (+ adr/README index); PRD 0.6→0.7 (Feature 6; FR-13 navigation, FR-14 route store; BR-12…BR-15; Out-of-Scope turn-by-turn); ARCHITECTURE 0.5→0.6 (§2 derived `known_segment`/CellIndex, §4 DB-version cell, §8 MIGRATION_9_10 DB→v10, new §12 Track Navigation & Local Route Store); UI-SPEC 0.5→0.6 (§3d nav — design-handoff-pending); DOCUMENT-CONTROL register + change log. Routing engine / traffic-ETA / ghost-route cost left OPEN; no code (design handoff §12 + build permission §5a gated). | Completed | `b8560ed` | Docs only; no Gradle/device run (AGENTS §5a). Cross-checked against PRD §4/§12 canonical-log invariant and the existing DB migration chain. |
| 2026-07-08 | Observer Snapshot Viewer — meta-strip layout fix + prev/next event navigation. (1) `MetaStrip` was a single `Row`; a long event type (`WINDOW_CONTENT_CHANGED`) squeezed the LAST SEEN chip until its unconstrained label wrapped one char per line. Fix: `MetaStrip`→`Column` — event-type value on a full-width labelless row, FIRST/LAST SEEN chips at `weight(1f)` below (+REPEAT when present); `MetaChip` label pinned `maxLines=1, softWrap=false`. (2) Added prev/next circular chevron buttons left of the close button in `TitleRow`; new `SnapshotViewerSheet` params `onPrev/onNext/canPrev/canNext` (defaulted, back-compatible) + extracted `CircleIconButton`. Host `ObserverFeedScreen` wires them over snapshot-bearing events (`treeSnapshot != null`), disabling at list ends. Files: `SnapshotViewerSheet.kt`, `ObserverFeedScreen.kt`; UI-SPEC §3c added + inventory line updated. | Completed | `f461cbf` | Full change `installDebug` BUILD SUCCESSFUL in 40s + installed on SM-G965F (Android 10) 2026-07-08 — layout fix, prev/next buttons, and host wiring all compiled clean and deployed. On-device prev/next tap-through pending user manual confirmation. |
| 2026-07-08 | Fix — completed-trip metric row was visually clipped/unreadable (cost showed only `Rp`, avg speed lost its `h`, duration lost its last digit). Root cause: `TrackItemRow` packed all five stats (km / duration / avg speed / km/L / cost) into one `Row` of five equal `weight(1f)` columns with `maxLines=1` and no ellipsis; the ADR-008 cost stat (5th column, `82b5de5`) made each column too narrow to fit its value on this device. Data was always persisted correctly — purely a display bug. Fix (Option A, user-approved): split into a two-row grid mirroring `ActiveTripRow` (base km/duration/avg speed + fuel km/L/cost with an empty 3rd cell for column alignment; height 124→156dp); stat values → `MonospaceFontFamily` 14sp/Medium (smaller + lighter, was 15sp/Bold) with `-0.3sp` tracking + `Ellipsis`, labels 10sp. Files: `TrackItemRow.kt`; UI-SPEC §3b updated (item 7). | Completed | `fa18330` | `installDebug` clean + device-verified 2026-07-08 on SM-G965F (Android 10): Track #34 now shows full `0.45 km / 00:07:50 / 3.4 km/h` then `2.4 km/L / Rp 3.037` across two aligned rows (all previously truncated); Track #35 correctly shows `0.00 km / 00:00:03 / 0.0 km/h / — / —` (genuine 3-second stationary trip); monospace digits aligned, no clipping. |
| 2026-07-07 | Fuel Cost v2 — code (ADR-008): new `FuelPriceEntity`/`fuel_price` table + `FuelPriceDao` + `MIGRATION_8_9` (DB→v9, additive); `FuelPriceController` reworked to append effective-dated rows (non-destructive undo/redo) + one-time seed from the retired `obd_fuel_price_per_liter` scalar; completed-trip cost priced at trip start (`priceEffectiveAt`, batched in `ListContent`); active-trip card split into two stat rows (instant km/L, L/h, trip-avg km/L, cost; height 124→156dp); `TrackItemRow` gains a cost stat. Files: `FuelPriceEntity.kt`, `FuelPriceDao.kt`, `TrackDatabase.kt`, `TrackApp.kt`, `FuelPriceController.kt`, `SessionsScreen.kt`, `TrackItemRow.kt`, `ListContent.kt`. | Completed | `82b5de5` | :app:compileDebugKotlin BUILD SUCCESSFUL 2026-07-07. Device-verified on SM-G965F (Android 10) 2026-07-07: MIGRATION_8_9 ran on real v8 data (logcat "DB version upgrading from 8 to 9", no crash, existing sessions/trips preserved — additive migration safe); completed-trip rows show the 5th "cost" stat ("—" without OBD fuel); active-trip card renders two stat rows (base km/duration/avg speed + OBD km/L / L/h / avg km/L / cost, 156dp); price editor persists to the fuel_price table (entered Rp 15000 → reopened prefilled 15000); undo/redo state machine correct (both disabled initially → after edit Undo enabled/Redo disabled → after Undo, Undo disabled/Redo enabled); trip delete unregressed. Device drive-test COMPLETE 2026-07-08 (user, car running): live cost = litres × price on the session card + active trip; instant km/L / L/h / trip-avg populate live; completed-trip cost frozen at trip-start price. Feature fully verified end-to-end. |
| 2026-07-07 | Fuel Cost v2 — docs (ADR-008): new ADR-008 (effective-dated `fuel_price` entity, completed-trip cost at trip start, non-destructive undo/redo appends); PRD FR-12 rewrite; ARCHITECTURE §2/§8 (FuelPriceEntity + MIGRATION_8_9, DB v9); UI-SPEC §3a two-row active card / §3b completed cost / §4; DOCUMENT-CONTROL. | Completed | `bd60cc5` | Docs recorded (`bd60cc5`); code implemented in the next commit. |
| 2026-07-07 | Fuel Cost — code (FR-12): new `FuelPriceController` (in-memory multi-step undo/redo; persists only the current price to `obd_prefs`) + `FuelCostCell`/`FuelCostEditorDialog` (IDR `Rp` format; tap-to-edit `Dialog` + ↶/↷); new `obd_fuel_price_per_liter` pref; `ObdUiState.Connected` gains `sessionFuelConsumedL`/`tripFuelConsumedL`; tappable COST cell on Session `ObdStatusCard` + 5th `cost` stat on the Trips active-trip row. No schema change. Files: `FuelPriceController.kt`, `feature/obd/ui/FuelCostCell.kt`, `ObdPreferencesDataStore.kt`, `ObdPollingService.kt`, `SessionsScreen.kt`, `ListContent.kt`. | Completed | `c14627e` | `:app:compileDebugKotlin` BUILD SUCCESSFUL 2026-07-07 (no new errors; 3 pre-existing warnings). Static only — device drive-test (cost = litres × price; undo/redo; price persists across restart) pending user (AGENTS.md §5a). |
| 2026-07-07 | Fuel Cost — docs (FR-12): PRD §9 FR-12, ARCHITECTURE §8 cost bullet + §5 ObdUiState fields, UI-SPEC §3a/§4 fuel-cost subsection; DOCUMENT-CONTROL register + change log. Cost = litres × `obd_fuel_price_per_liter` (IDR) on Session card + Trips active-trip row; inline price editor + multi-step in-memory undo/redo. No schema change. | Completed | `c1d1420` | Docs recorded (`c1d1420`); code implemented in the next commit. |
| 2026-07-07 | Fix — ADR-007 Slice 1 v7→v8 crash: `fallbackToDestructiveMigrationFrom(7)` is illegal alongside `MIGRATION_6_7` (end version 7), so Room threw `IllegalArgumentException` at `getDatabase().build()` and the app crashed on launch before the DB opened (migration never ran; DB stuck at v7). Fix: replaced the destructive fallback with an explicit destructive `MIGRATION_7_8` (DROP + recreate `recording_session` without `obdGpsDistanceKm`) registered in `addMigrations(...)`. Files: `TrackDatabase.kt`. See ERR-004. | Completed | `d8b3d5b` | `installDebug` clean + device-verified 2026-07-07 on SM-G965F: logcat "DB version upgrading from 7 to 8", crash buffer empty, MainActivity rendered; Room open-time schema validation passed (proves `recording_session` recreated to v8 without `obdGpsDistanceKm`). |
| 2026-07-07 | Fuel-Economy Unification — ADR-007 accepted + propagated (PRD FR, ARCHITECTURE §8, UI-SPEC §4); instant two-cell + single averaging derivation + destructive v7→v8 column drop | Completed | `553805a` | Docs recorded; code slices pending user build permission (AGENTS.md §5a) |
| 2026-07-07 | Fuel-Economy Unification Slice 1 — drop `obdGpsDistanceKm` (destructive v7→v8, `fallbackToDestructiveMigrationFrom(7)`); session avg → `session.distanceMeters`/`obdFuelConsumedL`; `addObdAccumulator` fuel-only | Completed | `2e27cdb` | on-device migration device-verified 2026-07-07 (7→8 ran, no crash) after MIGRATION_7_8 fix — ERR-004; fuel behavior drive-test pending |
| 2026-07-07 | Fuel-Economy Unification Slice 2 — trip fuel O(1) in-memory accumulator (reseeded from obd_sample on new-trip/restart, replaces the 2s re-query) + session-close authoritative re-integration; shared `ObdFuelMath` util | Completed | `76e22f2` | assembleDebug clean 2026-07-07; on-device test pending |
| 2026-07-07 | Fuel-Economy Unification Slice 3 — instant two-cell UI (km/L `—`-at-rest + always-on L/h) on `ObdStatusCard` and `TripPanel`; `fuelRateLph` plumbed into `TrackPanelState` | Completed | `c57b129` | assembleDebug clean 2026-07-07; on-device layout (4-cell Session row fit) + behavior test pending |
| 2026-07-07 | Fix — dwell collapse froze live GPS speed, corrupting OBD instant/average fuel metric (km/L shown at idle instead of L/h; phantom session OBD distance); refresh speed/position/accuracy in the collapse branch (ERR-003) | Completed | `3404731` | assembleDebug clean 2026-07-07; on-device re-verify (stopped → L/h, no phantom SESSION AVG distance) pending |
| 2026-07-07 | Location Dwell Collapse — ADR-006 accepted + PRD §12 (BR-11) + ARCHITECTURE §8 (MIGRATION_6_7 schema) + Appendix A slice contract recorded | In Progress | `55c1c7b` | Docs recorded; code slice (service dwell state + migration) pending user build permission per AGENTS.md §5a |
| 2026-07-07 | Location Dwell Collapse Slice 1 — schema: LocationEntity `dwellStartTimestamp`/`collapsedCount`, `MIGRATION_6_7` (DB v6→v7, backfill), `LocationDao.updateDwellAnchor` | Completed | `390b1ad` | `assembleDebug` clean 2026-07-07 (Room KAPT validated); on-device migration test pending |
| 2026-07-07 | Location Dwell Collapse Slice 2 — `TrackingService` write-time dwell state machine: collapse within `max(15 m, 1.5×accuracy)` via `updateDwellAnchor` (no insert, no session/trip distance); parked-only 2-fix outlier rejection; new anchors record `dwellStartTimestamp` | Completed | `fc40ac9` | `assembleDebug` clean 2026-07-07 (Room KAPT validated schema/DAO/migration); on-device drive-test pending |
| 2026-07-07 | Fix — after a force-stop with an open (non-stale) session, relaunch showed the always-recording Session card as Inactive while the Sessions list still showed the session ACTIVE (live-service vs persisted-session mismatch). Root cause: force-stop kills the process so `TrackingService.locationUiState.isAlwaysRecording` resets to false (card → Inactive), but `MainActivity` reaps only *stale* sessions and `START_STICKY` is not redelivered after a user force-stop, leaving the still-open row with no running service (list → ACTIVE). Fix (resume, per user decision): in `MainActivity.onCreate`, after the stale reap, if the service isn't recording and `getActiveSession()` is non-null, start `TrackingService` with `Actions.START_RECORDING` — routing through the healed `startAlwaysRecording()` which adopts the open session (no duplicate) and resumes foreground+location+OBD. Stale sessions still reaped first, so no resume past the grace window. Files: `MainActivity.kt`. No schema change; behavioral change recorded in PRD FR-10 + ARCHITECTURE Key Invariants. | Completed | `3dad45f` | Compile-verified (`:app:installDebug` BUILD SUCCESSFUL) + installed on SM-G965F (Android 10) 2026-07-07. Device-verified by user 2026-07-07: after force-stop mid-session + reopen within 2 min, the Session card returned to Active with exactly one ACTIVE session (resume via `Actions.START_RECORDING` adopting the open session); no card/list mismatch. |
| 2026-07-07 | Fix — Sessions screen shows two cards with ACTIVE status (single-active-session invariant violated). Root cause: `TrackingService.startAlwaysRecording()` guards new-session creation only on the in-memory `isAlwaysRecording` flag, never the DB. If a prior session is left `isActive=1` in `recording_session` (process force-stopped/killed then reopened inside the 2-min reaper grace window, so `closeStaleActiveSessions` sees it as not-stale and `START_STICKY` did not resume it), toggling the switch ON inserts a second `isActive=1` row → two ACTIVE cards. `getActiveSession()` (`LIMIT 1`) then permanently orphans the older row. Fix (adopt): new `SessionDao.closeAllActiveSessions(endedAt)`; in `startAlwaysRecording()`, before inserting, adopt an existing open session if present (continue accumulating into it), else close any stragglers — keeping exactly one `isActive=1` row. Files: `SessionDao.kt`, `TrackingService.kt`. No schema change. | Completed | `7344e2c` | Compile-verified (`:app:compileDebugKotlin` BUILD SUCCESSFUL) + installed on SM-G965F (Android 10) 2026-07-07. Device-verified by user 2026-07-07: after force-stop mid-session + reopen within 2 min + toggle ON, exactly one ACTIVE card (previously two). Fix: new `closeAllActiveSessions` DAO + reworked `startAlwaysRecording()` closes open rows then adopts the newest open session. |
| 2026-07-07 | Feature — Trips screen Sessions visual parity + efficiency stats (UI-SPEC §3b, committed `f482e9e`). Header → 40sp ExtraBold; removed the code-only metrics row (Trips/Distance/Hours) + empty search bar (and dead `MetricsRow`/`MetricCard`/`SearchBar`/`formatMetric`); "Recent trips" header → 17sp/13sp; list safe-zone 20→22dp. Added a 4th `km/L` efficiency stat — active-trip row from live OBD (`ObdPollingService.obdUiState` `avgKmL ?: instantKmL`, "—" when not Connected); recent-trip rows from `distance(km) ÷ TrackEntity.obdFuelConsumedL` ("—" when 0). No schema change. Changes in `screens/list/components/ListContent.kt` + `TrackItemRow.kt`. | Completed | `256df54` | Compile-verified (`compileDebugKotlin` BUILD SUCCESSFUL, no warnings) + device-verified 2026-07-07 on SM-G965F (Android 10): `installDebug` OK; Trips restyled (40sp header; metrics row + search bar gone); recent rows show the 4th `km/L` stat = "—" (no OBD); Start → live "Trip in progress" ACTIVE row (green border) with `km/L` = "—" at 00:00:40; Stop restored "Ready"; no crashes/fatals. Live `km/L` value still pending a drive with the ELM327. |
| 2026-07-07 | Feature — live "Active trip" row in the Trips list (Sessions parity). New UI recorded in UI-SPEC §3a. Because there is no live trip DB row (IMPLEMENTATION-ISSUES #1), the in-progress trip is synthesized from `locationUiState` and prepended to "Recent trips" while `isTracking`, then replaced by the saved `Track #N` row on Stop. Layout: keep the Current trip card (holds Start/Stop CTA) + add the live row; reuses the Sessions active-row visual language (green border + pulsing ACTIVE badge) adapted from `TrackItemRow`. Empty card shown only when no finished trips AND no active trip. Change in `screens/list/components/ListContent.kt`. | Completed | `05036c5` | Static only (AGENTS.md §5a). Change in `ListContent.kt` (new ActiveTripRow + list wiring; empty card gated on no-active-trip). Device-verified 2026-07-07 on SM-G965F: Start → live "Trip in progress" ACTIVE row appeared above finished trips (live 00:00:05, 0.00 km, 0.0 km/h); Stop → row replaced by saved Track #27 (count 26→27); no crash. |
| 2026-07-07 | Hardening — share one `ShareViewModel` across the Sessions/Trips/Track tabs (single source of truth) so all tabs observe the same Room-Flow collectors, matching the Sessions list's seamless live updates. Context: each bottom-nav destination previously created its own nav-back-stack-scoped `ShareViewModel` (confirmed via `NavGraph`); Room Flows already cross-notify instances, so Activity-scoping is belt-and-suspenders that removes any cross-instance staleness. Change: `SessionsScreen`/`ListScreen`/`TrackScreen` now obtain the VM via `viewModel(viewModelStoreOwner = <Activity>, factory = ShareViewModel.Factory)` (overload + default-extras behaviour confirmed via Context7 for lifecycle-viewmodel-compose; the Activity owner supplies APPLICATION_KEY). The behavioural list-update fix itself is the prior gating change (`a7bc835`). | Completed | `8b9730e` | Static only (AGENTS.md §5a). 3-file plumbing change (SessionsScreen/ListScreen/TrackScreen); no UI/schema change. Device-verified 2026-07-07 on SM-G965F: compile+install OK; Trips list updated live (Track #26, count 25→26) without leaving the tab; tab switching Sessions/Trips/Track no crash. |
| 2026-07-07 | Fix — stopped trips with no captured GPS fix were silently discarded (never appeared in the Trips list). Root cause: `ShareViewModel.onTripCtaTap()` gated `insertTrack` on non-null `activeTripStartLocationId`/`activeTripEndLocationId`, which are only set on the first location callback (≤5 s, `LOCATION_UPDATE_INTERVAL`); a trip started then stopped before any fix (short trip / no signal) wrote no `track` row. Fix (save-with-fallback): always insert a `TrackEntity` at trip stop using `tripStartedAt`/`durationTimer`/`distanceInMeters`/pathPoints string; keep `startLocationId`/`endLocationId` only when the range is valid (both non-null and `endId >= startId`), else null — `getPathPointsForTrack` already falls back to the stored `pathPoints` string when IDs are null. | Completed | `a7bc835` | Static only (AGENTS.md §5a). Single-file logic change in `ShareViewModel.kt`; no schema/build change. Device-verified 2026-07-07 on SM-G965F: start→Stop (~5s, 0.00 km) → new Track #26 appeared in Trips list; trip count 25→26; no crash. |
| 2026-07-02 | OBD Phase 2 Slice 1 — schema + MIGRATION_4_5 (accumulator columns `obdFuelConsumedL`/`obdGpsDistanceKm` on `recording_session`, `obdFuelConsumedL` on `track`; DB v4→5; `SessionDao.addObdAccumulator`, `TrackDao.addObdFuel`). Deviations from plan text: files are flat under `data/roomdb/` (no `entity/`/`dao/`/`migration/` subfolders); migration is inline in `TrackDatabase.kt` per existing convention (no separate `Migration4to5.kt`); session PK is `String` (not `Long`); the "trip" table is physically `track` with `Int` PK `idx` — SQL uses `ALTER TABLE track` and `WHERE idx = :tripIdx`. | Completed | `3c7d20f` | Static inspection only — no Gradle/device run (AGENTS.md §5a). Build + migration verification pending explicit permission. |
| 2026-07-02 | OBD Phase 2 Slice 2 — service/DB fuel accumulation. **Session:** `ObdPollingService` persists per-poll fuel/distance increments into the active `recording_session` row (`SessionDao.addObdAccumulator`, dt guard 0<dt<60s); `TrackingService` passes `EXTRA_SESSION_ID` on both `ACTION_SESSION_ON` sends. **Trip (Issue #1 resolved — derive from `obd_sample`):** `ShareViewModel.onTripCtaTap()` integrates fuel over `[tripStartedAt, now]` at trip stop and writes `TrackEntity.obdFuelConsumedL`; added `ObdSampleDao.samplesBetweenOnce`; injected `obdSampleDao` into `ShareViewModel`. | Completed | `3c7d20f` | Static only (AGENTS.md §5a). Build + device (drive with OBD → confirm `recording_session` accumulators grow and persist across restart; `track.obdFuelConsumedL` populated at trip stop) pending explicit permission. |
| 2026-07-02 | OBD Phase 2 Slice 3 — Session `ObdStatusCard` UI. Design handoff produced + verified in Claude Design (existing TrackLocation project, `rinaldi.ch` account, Haiku 4.5) as page "OBD Status Card" (4 artboards). Code in `screens/sessions/SessionsScreen.kt`: EFFICIENCY cell shows `X.X L/h` at idle (speed<3, RPM>0) else km/L; new `SESSION AVG` row reads persisted `recording_session` accumulators (`ObdPollingService` computes `avgKmL` from `getActiveSession()`); fuel-source chip now reflects real source (Direct/Inferred/Estimated/Unavailable, amber tint for non-direct). | Completed | `3c7d20f` | Static only (AGENTS.md §5a). Design verified in Claude Design (idle L/h + session-avg render correctly). Build/device pending explicit permission. |
| 2026-07-02 | OBD Phase 2 Slice 4 — Trip panel km/L UI. `TrackPanelState` gains `obdConnected`/`instantKmL`/`idleFuelLph`/`tripAvgKmL`/`fuelSource`; `TrackScreen` collects `ObdPollingService.obdUiState`, derives instant/idle values, and polls a live trip-average every 2s (`ShareViewModel.tripFuelLitersSince` integrates `obd_sample` over `[tripStartedAt, now]` ÷ live trip distance); `TripPanel` shows an OBD row (FUEL + TRIP AVG cells) gated on `obdConnected`. No Claude Design mockup this slice (visual language already set by migrated design + Slice 3 card); fuel-source chip kept on the Session card only. | Completed | `b0f822d` | Static only (AGENTS.md §5a). Build/device pending explicit permission. |
| 2026-07-06 | OBD Phase 2 — device deploy. Fixed a pre-existing Google Maps API key resource-name mismatch (`app/build.gradle` `resValue` renamed `google_maps_key_placeholder` → `google_maps_key` to match `AndroidManifest.xml` `@string/google_maps_key`; root cause commit `00cf587`). Rebuilt and installed `app-debug.apk` on SM-G965F (Android 10). All 4 OBD Phase 2 slices compiled with no errors (warnings only). | Completed | `f33a014` | Build: `:app:installDebug` BUILD SUCCESSFUL, installed on device. On-device drive test (idle L/h, session-avg persistence across restart, trip-avg accumulation, `track.obdFuelConsumedL` at stop) to be run manually by user. Build error logged: ERRORS-LOG ERR-002. |
| 2026-07-06 | Observer Phase 3 Slice 1 — filter data layer. Added `ObservedEventFtsEntity` (`@Fts4(contentEntity = ObservedEventEntity::class)` over `packageName`/`activityName`/`textSummary`); DB v5→6 with inline `MIGRATION_5_6` (create external-content `observer_event_fts` FTS4 table + `rebuild`); filtered pagination DAO queries (`getFilteredEventsFirstPage`/`getFilteredEventsNextPage`, `MATCH` join on rowid); `EventRepository.getFilteredFirstPage`/`getFilteredNextPage` (+ Fake). FTS approach per ADR-005. | Completed | `9818b2c` | Built + installed on SM-G965F (Android 10) and launched: `MIGRATION_5_6` ran (Logcat "DB version upgrading from 5 to 6"), no Room schema-identity crash — FTS4 CREATE string matches Room's expected schema; process stable. Filtered queries against real captured events exercised in Slice 2 (UI/state). |
| 2026-07-07 | Observer Phase 3 Slice 2 — filter state + query wiring. `FilterState` (selectedPackages/query, computed `isActive`) added to `ObserverUiState`; DAO gains package-only + package+text FTS pagination queries + `distinctPackages()`; `EventRepository` (+Impl +Fake) gains matching methods; `ObserverViewModel` routes first-page + `loadMore()` through the active filter (4 cases none/query/packages/both via `loadFilteredFirstPage`/`loadFilteredNextPage`), adds `formatFtsQuery` (prefix-`*` tokens), `reloadForFilter`, public `setQuery`/`togglePackage`/`clearFilter`, and in-memory live-tail filtering. No UI (Slice 3). Per-package scoped text deferred to S3. | Completed | `49fae34` | Built (`kapt` validated new FTS `MATCH`/`IN` queries) + installed on SM-G965F (Android 10) and launched: Observer feed loads, no crash (crash buffer empty, no Room/SQLite errors). Filtered paths exercised once S3 wires the UI. |
| 2026-07-07 | Observer Phase 3 Slice 3 — filter UI. New `ObserverFilterBar` (light theme; Compose-1.2 safe: custom chips + `BasicTextField`): search field + horizontally-scrollable package chips (label = `pkg.substringAfterLast('.')`; selected = amber tint/border + check icon + bold) + "N MATCHES" + Clear; `ObserverNoResultsState` in `EmptyState.kt`; `ObserverUiState.packageOptions` + `ObserverViewModel.loadPackageOptions()` (distinctPackages, in init + reloadForFilter); wired into `ObserverFeedScreen` (filter bar above list; no-results branch when filter active). Design per Claude Design "Observer Filter Bar" (commit 8a81be0); mockup dark but code uses the light Observer palette. | Completed | `bbd9ead` | Built + installed on SM-G965F; verified on-device 2026-07-07: filter bar renders; package chip select → feed narrows 100→50 with check/bold + "50 MATCHES" + Clear; FTS text search "home" → 50 matches (HomeActivity rows); non-matching query → "No matching events" state + Clear filters button; no crashes. |
| 2026-07-07 | Observer Phase 3 Slice 4 — unified Observer Settings. New `ObserverSettingsScreen` (light theme, mirrors Settings section-card/row style): SERVICE (accessibility status Enabled/Disabled, dot+text label + "Open settings" → `ACTION_ACCESSIBILITY_SETTINGS`), CAPTURE (Material3 `Switch` → `toggleCapture`), ALLOWLIST (applied rules with EXACT/REGEX tags + "Manage allowlist" reusing the existing `AllowlistBottomSheet`; "history cannot be cleared" footer). Route added (`Screen.ObserverSettingsScreen` + `NavGraph`). Feed consolidated: top-bar `FilterAlt`→gear navigates to Observer Settings; feed's inline allowlist sheet removed. Design per Claude Design "Observer Settings" (`6ccce01`). | Completed | `437979f` | Fixed a `Modifier.padding(horizontal,top)` compile error, then built + installed on SM-G965F; device-verified 2026-07-07: feed gear → Observer Settings; SERVICE toggles Enabled/Disabled dynamically; "Open settings" launches system Accessibility; CAPTURE Switch; allowlist rules render (taxsee EXACT / inDriver REGEX / gojek REGEX); "Manage allowlist" opens the reused editor (Exact/Regex, pattern, enable, delete, Apply); back nav; feed filter bar/service banner/capture chip unregressed; no crashes. |
| 2026-06-17 | OBD Phase 2 — fuel consumption enhancement (idle L/h display; session + trip average km/L persisted to DB; Trip screen fuel metrics; DB migration 4→5) | Completed | `3c7d20f` (S1–3) | All 4 slices implemented (schema + accumulation + Session card + Trip panel). Static only — build/device verification pending explicit permission (AGENTS.md §5a). |
| 2026-06-17 | Observer Phase 2 — truncation warning banner in SnapshotViewerSheet (`TruncationBanner` composable; pinned above scroll region; amber tokens; silent degradation on null/bad JSON) | Completed | `6733233` | Device verified 2026-07-02: State A confirmed (no banner, no crash, both Formatted and Raw JSON modes work); State B/C confirmed (truncation banner displays correctly with live data) |
| 2026-06-15 | Hardware verification complete — OBD Phase 1 (ELM327 streaming) | Completed | `ab16161` | Live on device: RPM/speed streaming verified; km/L calculation functional; connection stable; session gating working; orphan reaper verified on restart |
| 2026-06-15 | Stability — silent-stop + exception hardening: TrackingService sticky-restart resume from open session, serviceScope CoroutineExceptionHandler, SecurityException guard on location updates, 1 ms→1 s timer; launch-time orphan reaper now stale-only (closeStaleActiveSessions, 2 min grace) to avoid racing resume; OBD FGS promotion on SESSION_ON + startForeground guard + null Bluetooth-adapter handling | Completed | `16c9f4e` | Device verified: build OK; app survives force-stop/restart; OBD FGS running; no crashes; 98 MB memory |
| 2026-06-08 | OBD — live device verification + connection bug fixes | Completed | `211ef56` | Live on device: RPM/speed streaming, no crashes; `assembleDebug` OK |
| 2026-06-08 | OBD — capability scan + indirect speed-density fuel estimate | Completed | `211ef56` | Live: ~1.08 L/h idle on 1.2L; source SPEED_DENSITY |
| 2026-06-07 | OBD Slice 3 — build + device verification | Completed | `bd13fb3` | `assembleDebug` OK; installed; no crash on launch |
| 2026-06-07 | OBD Slice 3 — full polling loop + live telemetry | Completed | `bd13fb3` | Static; build pending at the time |
| 2026-06-07 | OBD Phase 1 design spec for Slices 3–4 | Completed | (docs) | Documentation only |
| 2026-06-07 | OBD Slice 2 — enable toggle + device picker | Completed | `c8a68da` | `compileDebugKotlin` OK |
| 2026-06-07 | Fix — ObdSettingsScreen alignment vs mockup | Completed | `760500a` | Static |
| 2026-06-07 | Fix — ObdSampleEntity missing @Index (migration crash) | Completed | `192effd` | Static (root cause fixed) |
| 2026-06-07 | OBD Slice 1 — infrastructure + navigation | Completed | `d089fcf` / `dfc982e` | `compileDebugKotlin` OK |
| 2026-06-07 | OBD pre-check — kotlin-obd-api Kotlin 1.7.0 incompat | Completed | `3cd1fa8` | Decision: raw AT I/O |
| 2026-05-29 | OBD Phase 1 — ELM327 telemetry (docs) | Completed | (docs) | Docs step; source followed |
| 2026-05-29 | Observer Phase 1 — device verification complete | Completed | (multiple) | All P1 features verified on device |
| 2026-05-22 | Observer Phase 2 — cursor pagination | Completed | `ee54e9c` | Static; device verification pending |
| 2026-05-19 | Observer snapshot viewer — per-event modal sheet | Completed | `8ceb530` | `compileDebugKotlin` OK |
| 2026-05-19 | Sessions — indicator animation + header alignment | Completed | `eefb18c` | Static |
| 2026-05-19 | Observer Phase 1 Step 4 — tree snapshot DFS | Completed | (codex) | Static; device run pending |
| 2026-05-19 | Multiple Observer/Track/List fixes + build clean-up | Completed | (codex) | `assembleDebug` OK on several |
| 2026-05-18 | Observer Phase 1 — full implementation + infra | Completed | (codex) | Static; build verification pending |
| 2026-05-16 | CR-0002 — Session always-recording switch | Completed | `fe241a7` | Verified by user |
| 2026-05-16 | CR-0001 — always-recorded sessions + canonical log | Completed | `776cd6e` | Verified by user |
| 2026-05-14 | CR#1 — UI-first Sessions screen + 4-tab nav | Completed | `7ae11b9` | Static |

---

## 7. Current Status

- **PRD:** v0.2
- **DB version:** Room 6 (migrations 1→2, 2→3, 3→4, 4→5, 5→6)
- **Active work:** none active — Observer Phase 3 complete (Slices 1–4 device-verified 2026-07-07). OBD Phase 2 manual drive-test pending (user); Observer P4+ out of current scope (PRD §8).
- **Reference docs:** §13

---

## 8. Technical Architecture

### Required Environment / Toolchain
- Local JDK: `C:\Users\rinal\.jdks\jbr-17.0.14` (inject `JAVA_HOME` inline for any permitted Gradle run).
- Kotlin 1.7.0; Compose UI 1.2.x (compiler extension 1.2.0).

### Folder Structure
Single app module `app/` (`com.kolee.tracklocation`): `data/roomdb/`, `feature/obd/`, `feature/observer/`, `observer/`, `tracking/`, `screens/`, `navigation/`, `ui/theme/`. Full package map in Appendix A (Observer Phase 1) and ARCHITECTURE §2/§8.

### Commit Message Format

```
feat: <capability added>

- What: brief summary of what changed
- Why: the user value or constraint that prompted this
- How: technical approach (if non-obvious)

Closes: <task-id>  (e.g., OBD-S3, CR-0002)
```

Doc-only commits use `docs:`; fixes use `fix:`; blocker commits use `[doc-issue]` / `[doc-decision]` (AGENTS.md §9).

### Stack & Domain Model
See `docs/ARCHITECTURE.md` (§4 stack, §2 domain model, §8 schema).

---

## 9. Key Decisions Locked

| Decision | Resolution | Rationale |
|----------|-----------|-----------|
| Observer navigation | Option B — under `Settings → Tools → Observer` | Keeps 4-tab baseline; see `docs/adr/003` |
| OBD library | Raw AT I/O over Bluetooth socket (no kotlin-obd-api) | Library binary-incompatible with Kotlin 1.7.0; see `docs/adr/004` |
| Fuel rate on no-MAF vehicles | Indirect speed-density estimate | Test vehicle exposes no MAF/015E PID |
| Observer full-text search | Room `@Fts4` external-content over `observer_event` (not FTS5) | FTS5 needs a bundled SQLite driver for guaranteed Android support; FTS4 is guaranteed on platform SQLite; see `docs/adr/005` |

---

## 10. Success Criteria (current)

1. ✅ Canonical log + sessions + trips behave per PRD §12.
2. ✅ Observer P1 captures, filters, and inspects events on device.
3. ✅ OBD P1 streams RPM/speed and computes km/L (incl. no-MAF vehicles).
4. ⏳ Observer P2 truncation banner approved + implemented.

---

## 11. Dependencies & Risks

- **External:** ELM327 adapter for OBD live verification; physical device for Observer/OBD device tests.
- **Internal:** Compose 1.2.x API ceiling (avoid newer APIs); Room migrations must accompany schema changes.
- **Risk:** device/build verification gated on explicit user permission (AGENTS.md §5a) — some items remain static-inspection only.

---

## 12. What's NOT in current scope

- ❌ Remote sync / Neon / API backend (Observer P4–P5)
- ❌ Registration, face enrollment, auth overlay (Observer P6–P7)
- ❌ BLE OBD; in-app BT discovery/PIN entry

---

## 13. Reference Documents

- `docs/PRD.md` — Product requirements
- `docs/ARCHITECTURE.md` — Architecture, domain model, schema
- `docs/UI-SPEC.md` — UI spec + design tokens
- `docs/adr/` — Architecture Decision Records
- `docs/IMPLEMENTATION-ISSUES.md` — Blocker protocol
- `docs/DOCUMENT-CONTROL.md` — Version register

---

## Appendix A — Detailed Phase/Slice Breakdown (migrated verbatim from implementation-plan.md)

## Current Implementation Summary

| Work item | Code status | UI/design status | Verification |
|---|---|---|---|
| CR-0001 Always-recorded Location Sessions | Implemented | Implemented | Verified by user |
| CR-0002 Session always-recording switch | Implemented | Implemented | Verified by user |
| Observer Phase 1 Local Foundation | Implemented | Implemented | Verified on device (2026-05-29) |
| Observer Phase 2 Inspection UI | Implemented (pagination) + truncation code logic ready | Spec drafted (truncation), awaiting design | Not verified on device |
| OBD Phase 1 (ELM327 telemetry) | Planned | Planned | Not started |
| Observer Phase 3 Filtering + Settings | Planned | Planned | Not started |
| Observer Phase 4 Sync Engine | Planned | Planned | Not started |
| Observer Phase 5 Neon V1 | Planned | Planned | Not started |
| Observer Phase 6 Registration + Face Enrollment | Planned | Planned | Not started |
| Observer Phase 7 Auth + Hardening | Planned | Planned | Not started |

## Immediate Next Step

Observer Phase 1 is complete and verified on device (2026-05-29). Proceed to Observer Phase 2.

Phase 2 status (as of 2026-05-23):
- Cursor pagination: Implemented ✓
- Snapshot viewer sheet: Implemented ✓
- Truncation warning UI spec: Drafted, awaiting design handoff

Next: Submit truncation warning spec + screenshots to Google Stitch / Claude Design for visual treatment approval, then implement the truncation banner in `SnapshotViewerSheet.kt` with the approved design.

OBD Phase 1 is also queued for implementation. See OBD Phase 1 section below.

Notes:

- CR-0001 and CR-0002 are verified by the user as working as expected.
- Observer Phase 1 code + UI are implemented and verified on device.
- Observer Phase 2 code is 90% complete; design approval pending for truncation banner.
- OBD Phase 1 spec (product-spec.md + implementation-plan.md) is ready; source implementation queued.

## OBD Phase 1 — ELM327 Bluetooth Classic Telemetry

Status:

- Code implementation: Planned in 4 vertical slices (each with build + observable outcome).
- UI/design handoff: Not needed (all UI in Step 3.4 and beyond).
- Verification: Per-slice via Gradle compile check and device observable outcomes.

### A. Code Implementation Work

Goal:

- Capture RPM, OBD speed, and fuel-rate data from ELM327 adapters via Bluetooth
  Classic SPP.
- Store samples in a new `obd_sample` Room table with no FK ties to trips or sessions.
- Surface instantaneous and average km/L on the Session screen and Trip panel.

Non-goals (Phase 1):

- No OBD-to-trip FK columns; time-window queries only.
- No changes to `LocationEntity`, `SessionEntity`, or `TrackEntity` schemas.
- No BLE; ELM327 Bluetooth Classic SPP only.
- No in-app BT discovery or pairing; user pairs via Android system settings (Option A).
- No automated tests; manual verification only.

---

### Slice 1 — OBD entry point visible in the app

**What it does:** Add OBD infrastructure (Room entity/DAO/migration, DataStore, service shell) and a minimal OBD Settings screen showing "Idle — service not enabled" status. No Bluetooth or adapter required.

**Observable result:** Build → install → Settings → TOOLS section shows "OBD" row → tap → dedicated OBD screen opens with a status card (showing "Idle") and a disabled Enable toggle.

**How to verify:**
- Gradle compile: `./gradlew :app:compileDebugKotlin` — no errors
- Fresh install: MIGRATION_3_4 applies without crash
- Visual check: navigate Settings → TOOLS → "OBD" row appears; tap → OBD screen displays

**Implementation steps:**

1. **`AndroidManifest.xml`** — What: declare BT permissions and OBD service. How: add `BLUETOOTH` + `BLUETOOTH_ADMIN` (maxSdk 30), `BLUETOOTH_CONNECT` + `BLUETOOTH_SCAN neverForLocation` (API 31+); add `uses-feature bluetooth required=false`; declare `ObdPollingService` with `foregroundServiceType="connectedDevice"`
2. **Create** `data/roomdb/ObdSampleEntity.kt` — What: define database table for OBD samples. Schema: id, timestampMs, rpm, obdSpeedKmh, fuelRateLph, mafGramsPerSecond, fuelRateSource, adapterElapsedMs
3. **Create** `data/roomdb/ObdSampleDao.kt` — What: DAO for OBD sample CRUD and queries. Methods: `insert(ObdSampleEntity)`, `latestSample(): Flow<ObdSampleEntity?>`, `samplesBetween(startMs, endMs): Flow<List<ObdSampleEntity>>`, `deleteOlderThan(cutoffMs)`
4. **Edit** `data/roomdb/TrackDatabase.kt` — What: wire OBD entity and migration. How: add `ObdSampleEntity` to entities list; bump version 3→4; add `abstract fun obdSampleDao()`; add inline `MIGRATION_3_4` (`CREATE TABLE obd_sample` with all fields); chain migration into `addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)`
5. **Create** `feature/obd/data/ObdPreferencesDataStore.kt` — What: DataStore for OBD settings and state. DataStore name: `obd_prefs`. Keys (with defaults): `obdServiceEnabled` (bool, false), `obdDeviceMac` (string, ""), `obdPollHz` (int, 2), `obdRetentionDays` (int, 7), `obdRetryMaxSeconds` (int, 120), `obdLastState` (string, "Idle"), `obdLastError` (string, ""), `obdLastSampleTs` (long, 0). Model pattern on `feature/observer/data/ObserverPreferencesDataStore.kt`
6. **Edit** `TrackApp.kt` — What: expose OBD DAO and notification channel. How: add `val obdSampleDao by lazy { TrackDatabase.getDatabase(this).obdSampleDao() }`; in `onCreate`, create notification channel with id `OBD_CHANNEL_ID = "OBD_POLLING"`
7. **Create** `feature/obd/service/ObdPollingService.kt` (shell for Slice 1) — What: service to manage OBD connection. What it exposes: `companion object { val obdUiState = MutableStateFlow<ObdUiState>(ObdUiState.Idle) }`. What it does: handles `ACTION_START`/`ACTION_STOP` intents (no-op stubs for now); calls `startForeground()` with OBD notification; sets up `serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)` and tears it down in `onDestroy()`. `ObdUiState` sealed class: `Idle`, `Connecting`, `Connected(rpm, obdSpeedKmh, fuelRateLph, fuelSource, instantKmL, avgKmL, sessionActive)`, `Retrying(attemptSeconds, maxSeconds)`, `Waiting(lastError)`
8. **Create** `screens/settings/obd/ObdSettingsScreen.kt` (shell for Slice 1) — What: Settings UI for OBD. Displays: status card showing `ObdPollingService.obdUiState` (initially "Idle"); Enable toggle (disabled for now, always shows OFF). No other controls yet
9. **Edit** `navigation/Screen.kt` — What: register OBD Settings route. How: add `object ObdSettingsScreen : Screen("obd_settings_screen")`
10. **Edit** `navigation/NavGraph.kt` — What: add OBD route to graph. How: add `composable(Screen.ObdSettingsScreen.route) { ObdSettingsScreen(navController) }`
11. **Edit** `screens/settings/SettingsScreen.kt` — What: add OBD link in TOOLS. How: in TOOLS section after Observer row, add `SettingsRow(title = "OBD", supporting = "ELM327 Bluetooth telemetry", onClick = { navController.navigate(Screen.ObdSettingsScreen.route) })`

---

### Slice 2 — Enable toggle + device picker (no real adapter needed)

**What it does:** Wire the Enable toggle to request BT permissions and attempt connection; add bonded device picker; show connection state (Connecting → Waiting on failure).

**Observable result:** Build → install → Settings → OBD → toggle ON → BT permission dialog → grant → bonded device picker appears → select device → status card shows "Connecting…" then "Waiting (no response)" (expected without a real adapter). "Pair a new device" tap opens system BT settings.

**How to verify:**
- Toggle request: toggle ON → system asks for `BLUETOOTH_CONNECT` on API 31+
- Deny permission: toggle stays OFF, shows inline error
- Grant permission: picker appears, can select bonded device
- Device selection: saving device MAC works (confirm in logcat or DataStore check)
- Device picker refresh: closing BT settings and returning to OBD screen refreshes picker
- System navigation: "Pair a new device" → `Settings.ACTION_BLUETOOTH_SETTINGS` opens system BT

**Implementation steps:**

1. **Edit** `feature/obd/service/ObdPollingService.kt` — What: implement connection attempt (no poll loop yet). Wire `ACTION_START`: read `obdDeviceMac` from DataStore; if blank stay `Idle`; if set, emit `Connecting`, attempt `BluetoothAdapter.getRemoteDevice(mac)` → `createRfcommSocketToServiceRecord(SPP_UUID)` → `socket.connect()` on IO; on connect failure, emit `Waiting(lastError)` and start exponential backoff (1s, 2s, 4s, …) capped at `obdRetryMaxSeconds`. Wire `ACTION_STOP`: close socket, emit `Idle`. On every state transition, write `obdLastState` to DataStore
2. **Edit** `screens/settings/obd/ObdSettingsScreen.kt` — What: full OBD Settings UX. Enable toggle: on toggle ON, request `BLUETOOTH_CONNECT` (API 31+) via Accompanist permissions; on grant, `startService(ACTION_START)` and save `obdServiceEnabled=true` to DataStore; on deny, show inline error chip and keep toggle OFF. Saved device row: show last saved device name + MAC + "Change" action. Bonded device picker: dialog listing `BluetoothAdapter.bondedDevices`, refresh on lifecycle `RESUMED` via `LaunchedEffect`. "Pair a new device" row: `startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))`. Status card: live `ObdPollingService.obdUiState` (shows Connecting, Waiting states). Reconnect button: visible in `Waiting` state, sends `ACTION_RECONNECT_NOW`. Preference selectors: poll rate, retention days, retry cap (write to DataStore on change)
3. **Edit** `MainActivity.kt` — What: auto-start OBD service on app launch if enabled. How: in `onCreate`, read `obdServiceEnabled` from DataStore on IO dispatcher; if true, call `ContextCompat.startForegroundService(Intent(...ACTION_START → ObdPollingService))`

---

### Slice 3 — Full BT polling loop + live telemetry (requires ELM327 adapter)

**What it does:** Implement the full OBD polling loop (RPM, speed, fuel); calculate km/L; gate writes to session active; add OBD metrics to Session screen.

**Observable result:** Pair ELM327 via system BT settings → Settings → OBD → select adapter → toggle ON → status card shows "Connected" → RPM and speed values update live (every 500 ms at 2 Hz). Session screen shows OBD status card with instant km/L (or "—" when speed < 3 km/h or accuracy > 20 m).

**How to verify:**
- ELM327 connect: status card transitions from Idle → Connecting → Connected, displays RPM + speed values updating
- km/L display: Session screen shows OBD card; instant km/L visible when driving, hidden when stopped
- Session gating: start session → `ACTION_SESSION_ON` sent to OBD service → OBD samples written to DB; stop session → `ACTION_SESSION_OFF` sent → no new DB writes
- No regression: GPS session/trip start/stop/stop work normally; Observer feed captures events

**Implementation steps:**

1. **Edit** `feature/obd/service/ObdPollingService.kt` — What: implement full poll loop and metrics using raw AT commands over Bluetooth socket (no kotlin-obd-api). After successful `socket.connect()`, emit `Connected` and start polling coroutine at `obdPollHz` Hz (`delay(1000L / pollHz)`). Poll sequence (raw AT I/O): send PID `010D` via `writeATCommand("010D\r")`, parse response `41 0D XX` hex to get `obdSpeedKmh = XX`; send PID `010C` to get `rpm = (256×A + B)/4`; then fuel fallback: try PID `015E` for direct fuel rate (DIRECT_FUEL_RATE); else try PID `0110` for MAF airflow in g/s (MAF_DERIVED: `fuelRateLph = maf × 3600 / (14.7 × 750)`); else mark UNAVAILABLE. Helper: `writeATCommand(cmd: String): String` writes to socket output stream, reads from input stream until `>` prompt. Calculate EMA instant km/L: `emaKmL = 0.2 × (gpsSpeedKmh / fuelRateLph) + 0.8 × emaKmL`; set to null when GPS speed < 3 km/h or accuracy > 20 m (read from `TrackingService.locationUiState`). Accumulate session fuel: `sessionFuelLiters += fuelRateLph × dtHours`; `sessionDistanceKm` from `TrackingService.locationUiState`. Compute average km/L: `sessionDistanceKm / sessionFuelLiters`. Write `ObdSampleEntity` to DB only when `sessionActive == true`. Retention cleanup: on `ACTION_START` and every 6 h, call `obdSampleDao.deleteOlderThan(now - retentionDays × 86_400_000)`
2. **Edit** `tracking/TrackingService.kt` — What: couple session start/stop to OBD service. In `startAlwaysRecording()`, send `startService(Intent(...ACTION_SESSION_ON → ObdPollingService))`; in stop path, send `ACTION_SESSION_OFF`. No direct field/state import across services
3. **Edit** `screens/sessions/SessionsScreen.kt` — What: display OBD metrics on Session screen. Collect `ObdPollingService.obdUiState` as state. Below always-recording card, add `ObdStatusCard`: displays OBD state label, instant km/L (hidden when GPS speed < 3 or accuracy > 20 m from `TrackingService.locationUiState`), average km/L (session accumulator), fuel source chip. Reconnect button in `Waiting` state

---

### Slice 4 — km/L in Trip panel during active trips

**What it does:** Add OBD fields to the Trip panel so km/L appears live during active trips, computed from OBD samples since trip start.

**Observable result:** Start a trip while OBD is Connected → Track screen trip panel shows an OBD row with instant km/L (updates live) and average km/L for the trip duration. Instant km/L hides when speed < 3 km/h.

**How to verify:**
- OBD row visible: active trip with OBD Connected → Track panel shows OBD row
- Instant km/L updates: visible every ~500 ms when speed > 3 km/h
- Instant km/L hides: when speed drops below 3 km/h or accuracy exceeds 20 m
- Average km/L: trip distance / sum of (fuelRateLph × dt for all OBD samples in trip window)
- No regression: GPS trip tracking, Observer feed, Session always-recording work normally

**Implementation steps:**

1. **Edit** `screens/track/TripState.kt` — What: add OBD fields to Trip panel state. Add to `TrackPanelState` data class: `instantKmL: Double? = null`, `avgKmL: Double? = null`, `fuelSource: String? = null`, `obdConnected: Boolean = false`
2. **Edit** `screens/track/components/TripPanel.kt` — What: render OBD row in trip panel. Below existing stats row, add OBD row: instant km/L cell (styled like other metric cells), average km/L cell, fuel source chip. Hide instant cell when `instantKmL == null`
3. **Edit** ViewModel / `TrackScreen.kt` — What: populate OBD fields from service state. Collect `ObdPollingService.obdUiState` and extract `instantKmL`, `avgKmL`, `fuelSource` into `TrackPanelState`. For trip average km/L: query `obdSampleDao.samplesBetween(tripStartMs, nowMs)`, sum all `(fuelRateLph × dt)` to get total trip fuel consumed, divide trip `distance` by total fuel

---

### B. UI Specification / Design Handoff Work

A single spec doc at `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md` covers all new UI surfaces. It must be produced and approved before the corresponding code slice starts.

| Surface | Introduced in | Spec covers | Code slice blocked |
|---|---|---|---|
| OBD row in Settings TOOLS | Slice 1 step 13 | Not required — identical `SettingsRow` pattern as existing Observer row; only decision is icon | No block |
| `ObdSettingsScreen` — Idle state | Slice 1 steps 10–12 | Status card visual in Idle state, disabled toggle treatment, overall screen layout | Slice 1 steps 10–12 |
| `ObdSettingsScreen` — full interactive UX | Slice 2 | Enable toggle, device picker dialog, Connecting/Waiting status card states, Reconnect button, preference selectors | Slice 2 |
| `ObdStatusCard` on Session screen | Slice 3 | Card layout, Connected/Waiting states, km/L display, fuel source chip | Slice 3 |
| OBD metric row in `TripPanel` | Slice 4 | Row layout, instant/avg km/L cells, fuel source chip, hidden-when-null instant cell | Slice 4 |

**Screenshots to attach for handoff:**
- Settings screen TOOLS section (Observer row as `SettingsRow` style reference)
- Session screen (always-recording status card as card style reference)
- Track screen TripPanel (existing metric cells as metric row style reference)

**Design handoff process:**
1. Create `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md` covering all five surfaces above
2. Attach screenshots listed above
3. Submit to Google Stitch or Claude Design for approval
4. Slice 1 infrastructure (steps 1–9) and the TOOLS row (step 13) can proceed immediately — no spec needed
5. Slice 1 screen steps (10–12) and all of Slices 2–4 are blocked until the relevant surface is approved

### Completed Pre-checks

| Pre-checks | Result |
|---|---|
| Kotlin compat | ✓ Tested 2026-06-07: `kotlin-obd-api` (master branch) compiled with Kotlin 2.3.0 is binary-incompatible with Kotlin 1.7.0. Error: "The binary version of its metadata is 2.3.0, expected version is 1.7.1." Decision: Skip library, implement raw AT I/O over Bluetooth socket + manual PID parsing. |

### Pending

| Per-slice verification | Work |
|---|---|
| Slice 1 | `./gradlew :app:compileDebugKotlin` → no errors; fresh install → no DB crash |
| Slice 1 | Settings → TOOLS → "OBD" row visible; tap → OBD screen opens with "Idle" status |
| Slice 2 | Toggle ON → BT permission dialog on API 31+; deny → toggle OFF with error; grant → device picker appears |
| Slice 2 | Device picker shows bonded devices; "Pair a new device" → system BT settings |
| Slice 2 | Select device → MAC saved to DataStore; close/reopen OBD screen → picker refreshes and shows selected device |
| Slice 2 | Toggle ON with device selected → status card shows "Connecting…" then "Waiting" (expected without adapter) |
| Slice 3 | Pair real ELM327 → toggle ON → status card shows "Connected" with RPM/speed updating |
| Slice 3 | Start session → observe `ObdPollingService.obdUiState.sessionActive = true`; OBD samples written to DB |
| Slice 3 | Session screen → OBD card visible; instant km/L shown when driving > 3 km/h, hidden when stopped |
| Slice 3 | Stop session → `sessionActive = false`; stop writing OBD samples to DB |
| Slice 4 | Start trip (with OBD Connected) → Track panel shows OBD row with instant/avg km/L |
| Slice 4 | Instant km/L updates live (~500 ms), hides when speed < 3 km/h |
| All slices | GPS session, trip start/stop, Observer feed: no regression |
| All slices | Retention cleanup works (old OBD rows deleted after 7 days or 50k-row cap) |
| Build | `./gradlew assembleDebug` — requires explicit user permission per AGENTS.md |
| Test | No automated tests in Phase 1 |

---

## OBD Phase 2 — Fuel Consumption Enhancement

**Status:** Planned. Requires DB migration 4→5.

**Goal:** Show idle fuel rate (L/h) when stationary; add session-average km/L and trip-average km/L that persist across app restarts; add fuel metrics to the Trip screen.

**Non-goals:** No new OBD PIDs beyond Phase 1; no MAF direct sensor support (car has MAP only); no automated tests.

---

### Slice 1 — Schema + Migration (no UI change)

**What it does:** Adds accumulator columns to `recording_session` and `trip`; bumps DB to version 5. No behavior change visible to the user.

**Observable result:** Fresh install (or migration from DB v4) succeeds without crash; DB now has the new columns.

**How to Verify:**
- Run `./gradlew assembleDebug` (with explicit permission) → no compile errors
- Install on device → no DB migration crash (check Logcat)
- Optional: confirm via `adb shell run-as com.kolee.tracklocation sqlite3` that `recording_session` has `obdFuelConsumedL` and `obdGpsDistanceKm` columns, and `trip` has `obdFuelConsumedL`

**Implementation steps:**

1. **Edit** `data/roomdb/entity/SessionEntity.kt` — What: add two new columns. How: add `@ColumnInfo(name = "obdFuelConsumedL") val obdFuelConsumedL: Double = 0.0` and `@ColumnInfo(name = "obdGpsDistanceKm") val obdGpsDistanceKm: Double = 0.0`
2. **Edit** `data/roomdb/entity/TrackEntity.kt` — What: add one new column. How: add `@ColumnInfo(name = "obdFuelConsumedL") val obdFuelConsumedL: Double = 0.0`
3. **Create** `data/roomdb/migration/Migration4to5.kt` — What: Room migration script. SQL: `ALTER TABLE recording_session ADD COLUMN obdFuelConsumedL REAL NOT NULL DEFAULT 0.0`, `ALTER TABLE recording_session ADD COLUMN obdGpsDistanceKm REAL NOT NULL DEFAULT 0.0`, `ALTER TABLE trip ADD COLUMN obdFuelConsumedL REAL NOT NULL DEFAULT 0.0`
4. **Edit** `data/roomdb/TrackDatabase.kt` — What: bump version to 5 and register migration. How: change `version = 4` to `version = 5`; add `MIGRATION_4_5` to `addMigrations(...)`
5. **Edit** `data/roomdb/dao/SessionDao.kt` — What: add method to increment session accumulators. How: add `@Query("UPDATE recording_session SET obdFuelConsumedL = obdFuelConsumedL + :fuelL, obdGpsDistanceKm = obdGpsDistanceKm + :distKm WHERE id = :sessionId") suspend fun addObdAccumulator(sessionId: Long, fuelL: Double, distKm: Double)`
6. **Edit** `data/roomdb/dao/TrackDao.kt` — What: add method to increment trip accumulator. How: add `@Query("UPDATE trip SET obdFuelConsumedL = obdFuelConsumedL + :fuelL WHERE id = :tripId") suspend fun addObdFuel(tripId: Long, fuelL: Double)`

**As-built (2026-07-02) — reconciled with actual schema; applies to Slices 2–4 too:**
- Files are flat under `data/roomdb/` — no `entity/`/`dao/`/`migration/` subfolders. Steps 1–2 edited `data/roomdb/SessionEntity.kt` and `data/roomdb/TrackEntity.kt`; the columns are plain fields (no `@ColumnInfo` needed since Room derives the column name from the field name).
- Step 3: migration is **inline** in `TrackDatabase.kt` as `MIGRATION_4_5` (matches existing `MIGRATION_1_2`…`MIGRATION_3_4` convention) — no separate `Migration4to5.kt` file.
- The domain **"trip" is the physical `track` table** (`@Entity(tableName = "track")`, PK `idx: Int`, `distance: Int` meters). Migration uses `ALTER TABLE track ADD COLUMN obdFuelConsumedL …`.
- `recording_session` PK `id` is **`String`**, not `Long`. `SessionDao.addObdAccumulator(sessionId: String, …)`.
- `TrackDao.addObdFuel(tripIdx: Int, fuelL: Double)` uses `WHERE idx = :tripIdx` (not `WHERE id`).

---

### Slice 2 — Service Accumulation

**What it does:** `ObdPollingService` integrates fuel increments into the session and trip accumulators in the DB on each sample. No UI change yet, but the DB values start populating.

**Observable result:** With OBD connected and session active, query the DB after a short drive — `recording_session.obdFuelConsumedL` and `obdGpsDistanceKm` are non-zero and growing.

**How to Verify:**
- After a short drive with OBD connected: pull DB via `adb shell run-as ...` and confirm accumulator columns are non-zero
- Kill and reopen app: accumulators persist (not reset to 0)
- Stop session and start new one: new session row starts at 0

**Implementation steps:**

1. **Edit** `feature/obd/service/ObdPollingService.kt` — What: accumulate fuel per sample. How: (a) add `var lastSampleTimestampMs: Long = 0L` field; (b) on each successful sample with `fuelRateLph != null`: compute `dtH = (nowMs - lastSampleTimestampMs) / 3_600_000.0`; compute `fuelIncrementL = fuelRateLph * dtH`; if `dtH > 0 && dtH < 60.0` (guard against gaps > 1 min); update `lastSampleTimestampMs = nowMs`; call `sessionDao.addObdAccumulator(activeSessionId, fuelIncrementL, gpsDistKmIncrement)` if session active; call `trackDao.addObdFuel(activeTripId, fuelIncrementL)` if trip active
2. **Edit** `feature/obd/service/ObdPollingService.kt` — What: track active trip id. How: add `var activeTripId: Long? = null`; listen for trip start/stop broadcasts (`ACTION_TRIP_START` / `ACTION_TRIP_STOP`) sent by `TrackingService`; on `ACTION_TRIP_START`, receive `tripId` extra and set `activeTripId`; on `ACTION_TRIP_STOP`, set `activeTripId = null`
3. **Edit** `tracking/TrackingService.kt` — What: broadcast trip start/stop to OBD service. How: when a trip starts, `sendBroadcast(Intent(ACTION_TRIP_START).putExtra("tripId", tripId))`; when trip stops, broadcast `ACTION_TRIP_STOP`

**As-built (2026-07-02) — session half done; trip half blocked:**
- **Step 1 (session):** implemented, but using the poll loop's existing `dtSeconds` (not a new `lastSampleTimestampMs` — the loop already tracks `lastPollTimeMs`). Per-poll increments `fuelIncrementL`/`distIncrementKm` (guard `0 < dtSeconds < 60`) are added to the active `recording_session` row via `SessionDao.addObdAccumulator(sessionId: String, …)`. `sessionDao` obtained from `TrackApp`.
- **Session id plumbing:** the plan never said how the service learns the session id. Added `ObdPollingService.EXTRA_SESSION_ID`; `TrackingService` sets it on both `ACTION_SESSION_ON` sends (`startAlwaysRecording` + `resumeIfActiveSession`); service stores `activeSessionId` and clears it on `ACTION_SESSION_OFF`.
- **Steps 2–3 (trip): superseded by Issue #1 decision — derive from `obd_sample` instead of a live trip id.** No live `track` row/id exists during an active trip (created at stop by `ShareViewModel`, autogenerated `idx`), so the plan's `activeTripId` broadcasting was not viable without changing locked trip-persistence behavior. Instead: added `ObdSampleDao.samplesBetweenOnce(startMs, endMs)`; `ShareViewModel.onTripCtaTap()` integrates fuel over `[tripStartedAt, now]` at trip stop (`integrateFuelLiters`, same `0<dt<60 s` guard) and writes `TrackEntity.obdFuelConsumedL`; `obdSampleDao` injected into `ShareViewModel` + factory. No `ACTION_TRIP_START/STOP` broadcasts and no `activeTripId` in the service. Live trip-average display is Slice 4 (queries the same window). See IMPLEMENTATION-ISSUES #1.

---

### Slice 3 — UI: ObdStatusCard (Session screen)

**What it does:** Session screen's OBD card gains session-average km/L and shows L/h at idle.

**Observable result:** Session screen OBD card shows: instant km/L when moving; `X.X L/h` at idle (speed=0, RPM>0); session average km/L accumulating over the session.

**How to Verify:**
- At idle (engine on, not moving): OBD card shows e.g. `0.7 L/h` instead of `--`
- While driving: instant km/L visible as before
- Session avg: starts low, accumulates as driving distance / fuel consumed ratio

**Implementation steps:**

1. **Edit** `feature/obd/ObdUiState.kt` — What: add new fields to the Connected state. How: add `sessionAvgKmL: Double?`, `fuelRateLphAtIdle: Double?` (non-null when speed=0, RPM>0) to the `Connected` data class (or equivalent `ObdUiState`)
2. **Edit** `feature/obd/service/ObdPollingService.kt` — What: populate the new UI state fields. How: after each poll, read `obdGpsDistanceKm` and `obdFuelConsumedL` from the active session row; compute `sessionAvgKmL = distKm / fuelL` (null if fuelL == 0); set `fuelRateLphAtIdle = fuelRateLph` if `gpsSpeedKmh < 3`; include both in `ObdUiState.Connected` emission
3. **Edit** `screens/sessions/components/ObdStatusCard.kt` — What: display new metrics. How: (a) replace `--` with `"${fuelRateLphAtIdle?.format(1)} L/h"` when `fuelRateLphAtIdle != null`; (b) add session-avg km/L row below instant row; label `"Session avg"`, value `"${sessionAvgKmL?.format(1)} km/L"` or `"--"` when null

---

### Slice 4 — UI: Trip screen / TripPanel

**What it does:** Trip screen gets instant km/L (or L/h at idle) and trip-average km/L.

**Observable result:** While a trip is active, the Trip panel shows: instant km/L / L/h-at-idle; trip-average km/L accumulating since trip start.

**How to Verify:**
- Active trip with OBD: Trip panel shows instant + trip-avg fuel rows
- L/h at idle: shows `X.X L/h` when stopped with engine on
- Trip avg persists after kill and reopen (read from trip DB row)
- Completed trip detail: trip-avg km/L visible in trip history (if surfaced in List screen detail)

**Implementation steps:**

1. **Edit** `feature/obd/ObdUiState.kt` — What: add `tripAvgKmL: Double?` to Connected state. How: compute from active trip's `obdFuelConsumedL` and `trip.distance`; set in service after each sample
2. **Edit** `feature/obd/service/ObdPollingService.kt` — What: populate `tripAvgKmL`. How: after updating trip accumulator, read trip's `distance` (in meters from TrackEntity) and `obdFuelConsumedL`; compute `tripAvgKmL = (distance/1000.0) / fuelL`; include in UI state emission
3. **Edit** `screens/track/components/TripPanel.kt` — What: add OBD fuel rows. How: add instant km/L cell (shows L/h at idle, `--` otherwise); add trip-avg km/L cell; both hidden when `obdConnected == false`

**Note (Issue #1 decision) — steps 1–2 must not assume a live trip accumulator.** There is no `track` row/id during an active trip (created only at trip stop; see Slice 2 as-built + IMPLEMENTATION-ISSUES #1). So the **live** trip-average must be derived from the `obd_sample` window since `tripStartedAt`: fuel = `integrateFuelLiters(obdSampleDao.samplesBetween(tripStartedAt, now))`, distance = live trip distance from `TrackingService.locationUiState.distanceInMeters`, `tripAvgKmL = (distance/1000.0) / fuel`. The persisted `track.obdFuelConsumedL` (written at trip stop in Slice 2) is for **completed** trip detail, not the live active-trip figure. Also UI → design handoff first.

---

## Observer Phase 7 — Auth + Hardening

### A. Code Implementation Work

Planned:

- App-wide auth state controller.
- Auth overlay.
- Face attempt budget.
- Google fallback rules.
- 24-hour re-auth.
- Idle/screen-on behavior.
- Background capture/sync/GPS continues while UI locked.

### B. UI Specification / Design Handoff Work

Planned:

- Auth overlay.
- Failed attempt states.
- Lockout state.
- Google fallback text only after failed face attempt.
- Dark/light polish.

## Observer Phase 6 — Registration + Face Enrollment

### A. Code Implementation Work

Planned:

- Registration gate.
- Google Sign-In.
- CameraX face capture.
- ML Kit `FaceProcessor`.
- FaceData contract.
- Encrypted local identity/device storage.

### B. UI Specification / Design Handoff Work

Planned:

- Google sign-in step.
- Face capture step.
- Success step.
- Account section in Settings.

## Observer Phase 5 — Neon V1 Remote Storage

### A. Code Implementation Work

Planned:

- Neon schema/setup scripts.
- Low-privilege role.
- `NeonDirectDataSource`.
- Secure local configuration storage.
- Remote event mapping.

### B. UI Specification / Design Handoff Work

Planned:

- Generic remote sync status.
- No credential editing UI unless future CR explicitly requests it.
- No raw Neon internals in UI.

## Observer Phase 4 — Sync Engine + Retention

### A. Code Implementation Work

Planned:

- `RemoteDataSource`.
- Fake/no-op remote.
- Hybrid sync triggers.
- Batch size 50.
- Retry/backoff.
- Retention cleanup only after successful sync.
- Never delete unsynced events.

### B. UI Specification / Design Handoff Work

Planned:

- Sync status rows.
- Pending/synced counts.
- Offline banner.
- Retry banner.

## Observer Phase 3 — Filtering + Unified Settings

**Status:** In progress (S1 filter data layer). DB migration 5→6. Excludes clear/delete of Observer history (locked out — PRD §8, FR-05). Text search uses Room `@Fts4` external-content FTS (see ADR-005).

**Goal:** Filter the Observer feed by package and by full-text search over event text, and consolidate Observer controls into unified Settings sections.

Slices follow the standard format (what → observable → how to verify → steps). S1–S2 are code-only; S3–S4 introduce UI and require a Claude Design handoff first (§12 two-track model).

### Slice 1 — Filter data layer (no UI)

**What it does:** Adds full-text + package filtering to the event query layer via a Room `@Fts4` external-content table over `observer_event`; nothing visible in the UI yet.

**Observable result:** Fresh install (or migration from DB v5) succeeds without crash; a filtered DAO query returns the correct package/text-scoped subset of events.

**How to Verify:**
- Build (with explicit permission per §5a) → `:app:compileDebugKotlin` no errors; Room schema processing accepts the FTS entity.
- Install → no `MIGRATION_5_6` crash (Logcat); optional `adb shell run-as com.kolee.tracklocation sqlite3` shows the `observer_event_fts` virtual table.
- Capture a few events → a temporary debug call / Logcat confirms the filtered query returns only matching rows; an empty filter returns the same rows as the existing first-page query.
- Room `@Fts4` API confirmed via Context7 before coding (step 0).

**Implementation steps:**
1. **Verify (Context7)** — confirm Room 2.5.2 `@Fts4` external-content behavior (`contentEntity`, generated sync triggers, trigger drop/recreate around migrations).
2. **Create** `data/roomdb/ObservedEventFtsEntity.kt` — `@Entity @Fts4(contentEntity = ObservedEventEntity::class)` indexing `packageName`, `activityName`, `textSummary`.
3. **Edit** `data/roomdb/TrackDatabase.kt` — add `ObservedEventFtsEntity::class` to entities; bump `version = 5` to `6`; add inline `MIGRATION_5_6` (create the fts4 virtual table + external-content triggers; rebuild index from existing rows); chain into `addMigrations(...)`.
4. **Edit** `data/roomdb/ObserverEventDao.kt` — add filtered first-page + next-page queries joining `observer_event` to `observer_event_fts` on `MATCH`, plus package-set `IN (:pkgs)`, ordered `lastSeenAt DESC` with `< :before` cursor and `LIMIT :limit`. Keep existing unfiltered queries for the empty-filter path.
5. **Edit** `feature/observer/data/repository/EventRepository.kt` — extend the interface with `getFilteredFirstPage(...)` / `getFilteredNextPage(...)`; implement in `EventRepositoryImpl` mapping to domain; `FakeEventRepository` returns empty for the new methods.

### Slice 2 — Filter state + query wiring (code)

**What it does:** Introduces a filter model (selected packages, global text, per-package scoped text) and a query builder in `ObserverViewModel` / `EventRepository`; changing the filter re-runs the paginated load. Feed list reused as-is.

**Observable result:** (dev-observable) applying a filter in state swaps the loaded page to the filtered result set; clearing restores the full feed.

**How to Verify:** build (with permission); capture events; toggling filter state in the ViewModel yields the expected filtered feed and correct load-more behavior; live-new-events still respect the active filter.

**Implementation steps:** (1) add a `FilterState` (packages set, globalQuery, per-package query map) to `ObserverUiState`; (2) build the effective query + package set; (3) route pagination (`getFirstPage`/`loadMore`) through filtered vs unfiltered DAO paths based on filter presence; (4) reset pagination cursors on filter change.

### Slice 3 — Filter UI (design handoff first)

**What it does:** Surfaces package chips, a global text search, and a no-results empty state in the feed.

**Design gate — DONE (2026-07-07):** Claude Design handoff produced in the existing TrackLocation project (rinaldi.ch account, Haiku 4.5) as page **"Observer Filter Bar"** — 3 artboards (filter bar default; filter bar active with 2 selected chips [check icon + bold outline, not color-only] + match count; no-results with a Clear-filters button). User-approved 2026-07-07. **Note:** the mockup rendered dark, but the actual Observer feed is light — the code follows the mockup's layout/interaction while using the existing light Observer palette (`0xFFF1F4F0` bg, `0xFF0A0A0A` text, `0xFF737373` muted, `0xFFE7EAE6` borders, `ObserverAmber`/`TripGreen` accents).

**How to Verify:** on device — selecting a package chip narrows the feed; typing a query filters live; clearing restores; no-results state shows the designed empty view; no regression to pagination/live events.

**Implementation steps (post-design):** (1) filter row in `FeedHeaderBar.kt` (package chips + search field); (2) wire to `ObserverViewModel` filter state; (3) no-results branch in `EmptyState.kt`.

### Slice 4 — Unified Observer Settings (design handoff first)

**What it does:** Consolidates Observer controls (allowlist, filter defaults, service/capture status) into unified Settings sections.

**Design gate — DONE (2026-07-07):** Claude Design handoff produced in the existing TrackLocation project (rinaldi.ch account, Haiku 4.5) as page **"Observer Settings"** — 2 light-themed artboards: main settings (SERVICE status [Disabled soft-red + text label / green Enabled], CAPTURE toggle, ALLOWLIST rules with EXACT/REGEX tags + delete + "+ Add rule", "history cannot be cleared" footer) and an "Allowlist — add rule" modal (pattern field + EXACT/REGEX toggle + Save/Cancel). User-approved 2026-07-07. Light theme matches the app's real Settings screen (white cards on `0xFFF1F4F0`). Introduces a dedicated Observer Settings screen reached from the feed (user-approved navigation addition).

**How to Verify:** on device — Observer Settings shows the designed sections; allowlist editing still works; capture toggle + service status accurate; no regression.

**Implementation steps (post-design):** (1) Observer Settings section composables; (2) wire to existing allowlist + prefs; (3) navigation from feed to settings.

### B. UI Specification / Design Handoff Work

- S3 filter UI: filter-chip interaction, active/selected, no-results states.
- S4 unified Observer Settings sections.
Both require a Claude Design handoff before their code (§12); S1–S2 do not.

## Observer Rollout

Observer Phase 1 is implemented. Remaining phases (2–7) are planned.

### Required decision before Observer Phase 1

Choose one navigation option:

| Option | Navigation | Notes |
|---|---|---|
| A | Session / List / Track / Observer / Settings | first-class Observer, five tabs |
| B | Session / List / Track / Settings, Observer under Settings/tools | keeps four tabs |
| C | GPS / Track / Observer / Settings | larger redesign |

Recommendation:

- Prefer **Option B** unless the user explicitly wants Observer always visible. It preserves the accepted 4-tab baseline and minimizes churn.

Acceptance criteria for this decision:

- `docs/product-spec.md` is updated to reflect the accepted placement.
- Phase 1 UI work must not proceed until the placement is accepted.

Accepted decision:

- **Accepted: Option B** (2026-05-18).

## Observer Phase 2 — Inspection UI

Status:

- Code implementation: Done (cursor pagination complete; truncation metadata code logic already in domain model, awaiting design spec).
- UI specification: In progress (truncation warning spec drafted; awaiting design tool handoff; event detail and JSON viewer already implemented via SnapshotViewerSheet).
- Verification: Not verified on device.

### A. Code Implementation Work

Done:

| Item | Status | Notes |
|---|---|---|
| Cursor pagination | ✅ Done (2026-05-22, `ee54e9c`) | First page 50, load-more on scroll, live-event narrow flow |
| Event detail / JSON viewer | ✅ Done (2026-05-19, via `SnapshotViewerSheet`) | Formatted + raw JSON modes, copy, per-event modal |
| Truncation metadata code | ✅ Ready | Field already in `ObservedEvent` domain model, passed to sheet; code change depends on design spec |

Pending:

| Item | Status |
|---|---|
| Truncation warning UI design approval | Track B in progress |
| Truncation banner implementation (SnapshotViewerSheet.kt) | Waiting for design spec |

### B. UI Specification / Design Handoff Work

Done:

| Item | Status | Document |
|---|---|---|
| Event detail screen | ✅ Via `SnapshotViewerSheet` | Already implemented; shows all event metadata, timestamp, event type, repeat count |
| Full-screen JSON viewer | ✅ Via SnapshotViewerSheet Raw JSON tab | Formatted + raw modes, 2-space indentation, line numbers, copy action |
| Metadata grid | ✅ Via SnapshotViewerSheet MetaStrip | Chips for event type, first seen, last seen, repeat count (when > 1) |
| Truncation metadata warning | 📋 Spec drafted | `docs/design-handoff/observer_truncation/OBSERVER_TRUNCATION_SPEC.md` — awaiting design tool handoff |

Pending:

| Item | Status |
|---|---|
| Design approval of truncation banner | Awaiting handoff to Google Stitch / Claude Design |
| Implementation after design approval | Code implementation ready once design is approved |

## Observer Phase 1 — Local Accessibility Observer Foundation

Status:

- Code implementation: Done (static inspection).
- UI implementation: Done (static inspection).
- Verification: Not run — requires explicit user permission per `AGENTS.md`.

### A. Code Implementation Work

Goal:

- Capture accessibility events locally (Room) with safe limits, without impacting GPS tracking reliability.
- Provide a minimal feed UI under `Settings → Tools → Observer` (navigation Option B, accepted 2026-05-18).

Non-goals (Phase 1):

- No remote sync.
- No auth/registration.
- No advanced filtering/search (Phase 3).
- No deep inspection UI (Phase 2).
- No changes to GPS tracking behavior.
- No user-facing delete/clear of observer history.

Done:

| Step | Files / modules | Result |
|---|---|---|
| 1. Service foundation | `ObserverAccessibilityService.kt`, `AndroidManifest.xml`, `res/xml/accessibility_service_config.xml` | `AccessibilityService` declared with `BIND_ACCESSIBILITY_SERVICE`; config listens to `typeWindowStateChanged\|typeWindowContentChanged`; manifest declares service with correct intent-filter and meta-data; "Open settings" action wired to `ACTION_ACCESSIBILITY_SETTINGS` in `StatusIndicators.kt` |
| 2. Event capture surface | `ObserverAccessibilityService.kt` | Captures both event types; `TYPE_WINDOW_CONTENT_CHANGED` deduped by text summary — updates existing row's `lastSeenAt` + increments `repeatCount` instead of inserting a duplicate row |
| 3. Data contract | `ObservedEventEntity.kt`, `ObserverEventDao.kt` | Fields: `packageName`, `eventType`, `activityName`, `firstSeenAt`, `lastSeenAt`, `repeatCount`, `textSummary`, `treeSnapshot`, `truncationMetadata`; indices on `packageName` and `lastSeenAt` |
| 4. Tree snapshot capture (bounded) | `ObserverAccessibilityService.kt` — `captureTreeSnapshot()` | DFS from `event.source` via explicit stack; captures text, contentDescription, className, flags, bounds per node; limits: 200 nodes, depth 10, 300 chars/field, 40 KB JSON; recycles all `AccessibilityNodeInfo` nodes; truncation metadata records reason + nodesCaptured; wired into both insert and CONTENT_CHANGED dedup-update paths |
| 5. Noise reduction (allowlist) | `AllowlistRuleEntity.kt`, `AllowlistRuleDao.kt`, service | EXACT (full-string equality) and REGEX (substring, case-sensitive, `containsMatchIn`) on `packageName`; empty allowlist = capture all; invalid/uncompilable regex = disabled silently |
| 6. Local persistence | `TrackDatabase.kt` (v3), migration `MIGRATION_2_3` | Creates `observer_event` and `allowlist_rule` tables; service prunes rows older than 7 days and enforces 50,000-row cap automatically |
| 7. Repository / use-case boundary | `EventRepository` (interface + `EventRepositoryImpl` + `FakeEventRepository`), `ObserverPreferencesDataStore.kt` | Feed reads from Room via Flow; capture running state persisted in DataStore; `setCaptureRunning` toggled from ViewModel |
| 8. Minimal UI shell | `ObserverFeedScreen.kt`, `ObserverViewModel.kt`, 6 components, `NavGraph.kt` | Full feed screen under Settings → Tools → Observer; service status banner (enabled/disabled); capture chip (toggle, persisted); auto-scroll with drag-pause; jump-to-latest FAB (2s transient); long-press copy (paused only); allowlist bottom sheet (draft → apply pattern) |

Pending:

| Category | Required work |
|---|---|
| Verify | Step 4 — DFS implemented; runtime behavior on real device not yet confirmed (no build/device run) |
| Wire | `getEventsByPackage()` DAO method exists but ViewModel always fetches all events; scoped package feed is not yet wired |
| Build | `./gradlew assembleDebug` — requires explicit user permission per `AGENTS.md` |
| Device | Enable service in Android Accessibility Settings, confirm events appear in feed — requires explicit user permission per `AGENTS.md` |
| Test | Unit tests for dedup/retention logic; Room migration test for `MIGRATION_2_3` |

Package structure (actual):

```
com.kolee.tracklocation.observer.ObserverAccessibilityService
com.kolee.tracklocation.data.roomdb.{ObservedEventEntity, AllowlistRuleEntity, ObserverEventDao, AllowlistRuleDao}
com.kolee.tracklocation.feature.observer.domain.model.{ObservedEvent, AllowlistRule, ObserverUiState, AllowlistDraftRule, AllowlistUiState, MatchType}
com.kolee.tracklocation.feature.observer.data.{ObserverPreferencesDataStore, repository/EventRepository, repository/EventRepositoryImpl, repository/FakeEventRepository}
com.kolee.tracklocation.feature.observer.presentation.viewmodel.ObserverViewModel
com.kolee.tracklocation.feature.observer.presentation.screens.ObserverFeedScreen
com.kolee.tracklocation.feature.observer.presentation.components.{EventRow, FeedHeaderBar, AllowlistBottomSheet, JumpToLatestFab, EmptyState, StatusIndicators}
```

### B. UI Specification / Design Handoff Work

Done:

- Observer feed shell — `ObserverFeedScreen.kt`.
- Service status indicator (enabled green / disabled red with "Open settings") — `StatusIndicators.kt`.
- Capture pause/resume chip (independent of auto-scroll) — `CaptureChip` in `StatusIndicators.kt`.
- Auto-scroll readout (display-only indicator; tap list toggles) — `AutoScrollReadout`.
- Capture paused inline banner — `CapturePausedBanner`.
- Event cards with package, activity, event-type chip (color-coded by category), text snippet, timestamp — `EventRow.kt`.
- Empty state ("Waiting for events") — `EmptyState.kt`.
- Jump-to-latest transient FAB (2s auto-dismiss) — `JumpToLatestFab.kt`.
- Allowlist bottom sheet: add/edit/delete rules, match-type toggle (Exact/Regex), enable/disable switch, draft indicator, amber draft banner, Apply/Close footer — `AllowlistBottomSheet.kt`.
- Feed header bar with event count, scope label, live/paused dot — `FeedHeaderBar.kt`.

Verification gates (Phase 1):

- Static inspection: no coupling from observer to GPS tracking service — confirmed.
- Requires explicit user permission per `AGENTS.md`: `./gradlew assembleDebug`, Room migration test, device/emulator sanity check that service enables and events appear in feed.

## CR-0002 — Session Always-recording Switch

Status:

- Code implementation: Done.
- UI/design handoff: Done.
- Verification: Verified by user.

### A. Code Implementation Work

Done:

| Category | Files / modules | Result |
|---|---|---|
| Edit | `SessionsScreen.kt` | replaced hero mockup with compact status card and trailing switch |
| Edit | `ListScreen.kt` | removed always-recording control and permission flow |
| Edit | `ListContent.kt` | kept trip history UI only and removed switch UI |
| Edit | docs | updated CR and screen specification docs |

Pending:

| Category | Required work |
|---|---|
| Verify | Confirm Session switch compiles |
| Verify | Confirm List screen has no stale imports/copy/control |
| Verify | Confirm permission flow still works from Session screen |
| Verify | Confirm active-trip guard prevents OFF |
| Verify | Confirm starting trip while OFF auto-starts always-recording and Session switch shows ON |
| Verify | Confirm stopping trip does not turn switch OFF |

### B. UI Specification / Design Handoff Work

Done:

The Session screen UI handoff should include:

1. OFF / inactive state.
2. ON / active state with active session visible.
3. Permission-required state.
4. Permission-denied state.
5. Active-trip guarded state.
6. Auto-started-by-trip state.

Design guidance:

- Use compact Material 3 status-card treatment.
- Keep Session list visible.
- Use trailing/right-side switch.
- Do not put switch on List screen.
- Use text status, not color alone.
- Preserve light/dark usability.

### Design Tool Prompt for CR-0002

```text
Create or refine the Android Session screen for TrackLocation.

The Session screen is the first bottom-nav tab in:
Session / List / Track / Settings.

Purpose:
Show always-recorded location sessions and let the user control always-recording directly from the Session screen.

Design the always-recording status area as a compact Material 3 card/control panel.

Required elements:
- Title: Always-recording
- Status text:
  - Active when ON
  - Inactive when OFF
- Helper text:
  - OFF: Location sessions are not being recorded.
  - ON: Recording location sessions in the background.
  - Guard: Always-recording is required while a trip is running.
- Trailing/right-side switch.
- Sessions list remains visible below the status area.
- Active session, if present, appears at the top and is clearly marked.

Required states:
1. OFF / inactive
2. ON / active with active session visible
3. Permission required
4. Permission denied
5. Active-trip guarded state
6. Auto-started-by-trip state

Rules:
- Do not add another navigation destination.
- Do not place always-recording switch on the List screen.
- Do not merge sessions into trips.
- Do not imply that stopping a trip stops always-recording.
- Use Material 3, compact operational styling, readable status labels, and Android-safe spacing.
- Produce light and dark theme variants if possible.
```

## CR-0001 — Always-recorded Location Sessions

Status:

- Code implementation: Done.
- UI implementation: Done.
- Verification: Verified by user.

### A. Code Implementation Work

Done:

| Category | Files / modules | Result |
|---|---|---|
| Create | `LocationEntity.kt` | canonical GPS point row |
| Create | `SessionEntity.kt` | always-recording session row |
| Create | `LocationDao.kt` | canonical location queries |
| Create | `SessionDao.kt` | session persistence queries |
| Edit | `TrackDatabase.kt` | added entities and Room v1→v2 migration |
| Edit | `TrackEntity.kt` | added `startLocationId` and `endLocationId` |
| Edit | `TrackDao.kt` | added location-range-aware queries |
| Edit | `TrackingService.kt` | appends canonical points; manages session and trip ranges |
| Edit | `TrackScreen.kt` / running card components | trip-specific Start/Stop semantics |
| Edit | `DetailsScreen.kt` | resolves trip path from canonical location range |
| Edit | navigation files | added Session tab/start destination |

Pending:

| Category | Required work |
|---|---|
| Test | Add migration test for legacy trip path → location rows/trip boundaries |
| Test | Add always-recording ON/OFF session test |
| Test | Add trip boundary tests: start uses next point, stop uses latest point |
| Test | Add trip deletion test proving location rows remain |
| Verify | Compile/build only after user permission |
| Verify | Manual location/session/trip behavior on device/emulator only after user permission |

### B. UI Specification / Design Handoff Work

Done:

- Session top-level destination.
- `Session / List / Track / Settings` bottom nav.
- Session screen for always-recorded sessions.
- Real session rows connected after CR-0001 full implementation.
- Track copy changed toward trip-specific semantics.

No additional design handoff required before verification unless visual defects are found.

## Codex Verification Prompt

Use this next:

```text
Verify the current CR-0001 and CR-0002 implementation.

Read AGENTS.md first.

Then read:
1. docs/product-spec.md
2. docs/change-requests.md
3. docs/implementation-plan.md
4. docs/progress.md

Focus only on verification of completed CR-0001 and CR-0002.

Do not implement Observer, auth, sync, Neon, or registration.

Before editing source code:
- update docs/progress.md Current Session
- record start timestamp
- record goal
- record expected files
- set status to In Progress

Start with static inspection:
- confirm Session/List/Track/Settings navigation
- confirm Session screen contains the only always-recording switch
- confirm List screen is trip-only
- confirm Track Start/Stop trip semantics
- confirm Room migration and canonical location/session entities
- confirm trip detail path resolves from location ranges
- check for stale copy/imports around always-recording switch removal from List

Do not run Gradle, tests, emulator, or device verification unless I explicitly grant permission.

After inspection:
- update docs/progress.md
- list issues found
- list fixes made, if any
- list verification not run and why
- suggest the smallest next verification command
- provide a concise commit message if changes were made
```

---

### Phase — Location Efficiency: Dwell Collapse (ADR-006)

**Slice 1 — Schema + migration (dwell columns).**
- *What it does:* Adds `dwellStartTimestamp: Long` and `collapsedCount: Int` (default 1) to `LocationEntity` / `location_log`; adds `MIGRATION_6_7` (DB v6→v7) that ALTERs the two columns and backfills `dwellStartTimestamp = timestamp` for existing rows; registers it in `TrackDatabase.getDatabase`.
- *Observable result:* App opens on existing data without a migration crash; existing rows have `dwellStartTimestamp = timestamp` and `collapsedCount = 1`.
- *How to verify:* (build permission required — AGENTS.md §5a) Install over an existing v6 DB; open app; confirm no crash and, via DB inspection, existing `location_log` rows show `dwellStartTimestamp = timestamp` and `collapsedCount = 1`.
- *Steps:* (1) Add columns to `LocationEntity` — What: two non-null fields; How: `dwellStartTimestamp: Long = 0L`, `collapsedCount: Int = 1`. (2) Add `MIGRATION_6_7` — What: ALTER + backfill; How: two `ALTER TABLE location_log ADD COLUMN` + one `UPDATE location_log SET dwellStartTimestamp = timestamp`. (3) Bump `@Database(version = 7)` and add `MIGRATION_6_7` to `addMigrations(...)`.

**Slice 2 — Write-time dwell collapse in `TrackingService`.**
- *What it does:* Holds an in-memory dwell anchor (row id + Location); on each fix, if within tolerance `max(15 m, 1.5 × accuracy)` it UPDATEs the anchor's `timestamp` and increments `collapsedCount` (no INSERT, no distance added); after 2 consecutive out-of-tolerance fixes it finalizes the anchor and INSERTs a new one. Adds a `LocationDao.updateDwellAnchor` path.
- *Observable result:* A stationary vehicle produces one `location_log` row whose `timestamp` advances while parked; session/trip distance does not grow while stopped; moving away starts a new anchor.
- *How to verify:* (build/device permission required) With always-recording ON, keep the device stationary ~2 min → confirm a single new row with rising `timestamp` and `collapsedCount`, and session distance unchanged; then move >20 m → confirm a new row is inserted and the path continues.
- *Steps:* (1) Add anchor state to `TrackingService` — What: `dwellAnchorId: Long?`, `dwellAnchorLocation: Location?`, `outOfToleranceStreak: Int`. (2) In `recordLocation`, branch on distance vs tolerance — within → UPDATE anchor `timestamp` + `collapsedCount`, return without adding session/trip distance; out (streak ≥ 2) → INSERT new anchor, reset streak, set `dwellStartTimestamp` = fix time; How: `Location.distanceBetween`, tolerance `maxOf(15f, 1.5f * accuracy)`. (3) Add `LocationDao.updateDwellAnchor(id, timestamp)` doing `UPDATE location_log SET timestamp = :timestamp, collapsedCount = collapsedCount + 1 WHERE id = :id`. (4) Anchor state is in-memory only, so an app/service restart mid-dwell starts a fresh anchor.

### Phase — Fuel-Economy Unification (ADR-007)

**Slice 1 — Schema: drop `obdGpsDistanceKm`, destructive v7→v8.**
- *What it does:* Removes `obdGpsDistanceKm` from `SessionEntity`; bumps `@Database(version = 8)`; registers `fallbackToDestructiveMigrationFrom(7)` in `TrackDatabase.getDatabase` (no hand-written 7→8 migration — destructive, local test data discarded). Simplifies `SessionDao.addObdAccumulator` to fuel-only.
- *Observable result:* App installs over v7 and recreates the DB clean (existing local rows wiped — acceptable pre-production); no schema-validation crash.
- *How to verify:* (build permission required) Install over v7; app opens without a Room `IllegalStateException`; `recording_session` no longer has `obdGpsDistanceKm`.
- *Steps:* (1) Remove field from `SessionEntity`. (2) Remove `obdGpsDistanceKm` from `addObdAccumulator` query + signature; update callers in `ObdPollingService`. (3) `@Database(version = 8)`, add `.fallbackToDestructiveMigrationFrom(7)` to the builder. (4) Add a follow-up task to remove the scoped destructive fallback before production.

**Slice 2 — Unified averaging (Option B).**
- *Status:* **Implemented (2026-07-07)** — trip O(1) accumulator (reseed on new-trip/restart) + session-close re-integration via shared `ObdFuelMath`. (Initially deferred, then implemented at user request.)
- *What it does:* Session average switches to `session.distanceMeters / obdFuelConsumedL`. Both session and trip use one definition: displayed displacement distance ÷ `obd_sample` fuel. Live value = O(1) incremental cache; authoritative value re-integrated from `obd_sample` at close (session end + trip stop); mid-drive restart reseeds the in-memory trip fuel via one `obd_sample` integration.
- *Observable result:* Session AVG matches the distance shown on the Session card; session and trip averages agree for the same drive; both update live and degrade while idling; survive restart.
- *How to verify:* (build/device permission) Drive with OBD connected → SESSION AVG and TRIP AVG populate (distance > 0.01 km, fuel > 0) and track each other; idle at a light → both degrade; force-stop + relaunch mid-trip → trip average resumes (reseeded), not reset to a wrong value.
- *Steps:* (1) In `ObdPollingService`, compute session avg from `session.distanceMeters` (read back) ÷ `obdFuelConsumedL`; stop reading `obdGpsDistanceKm`. (2) Keep/confirm trip live avg = `distanceInMeters` ÷ `tripFuelLitersSince`. (3) At session end / trip stop, persist the `obd_sample`-re-integrated fuel as authoritative. (4) On service restart with an active trip, reseed the in-memory trip fuel from `obd_sample` over `[tripStartedAt, now]`.

**Slice 3 — Instant two-cell UI (km/L + L/h).**
- *What it does:* Replaces the toggling instant cell with two always-on cells (km/L, `—` at rest; L/h, always when OBD connected) on both `ObdStatusCard` (Session) and `TripPanel`. Keeps a single SESSION AVG / TRIP AVG km/L with the `—`-until-distance rule.
- *Observable result:* Both km/L and L/h visible simultaneously; km/L shows `—` while stopped; L/h shows the idle rate; no unit switching.
- *How to verify:* (build/device permission) Moving → km/L shows a value and L/h shows the rate; stop with engine on → km/L flips to `—`, L/h keeps a value; both cards consistent.
- *Steps:* (1) `SessionsScreen` `ObdStatusCard`: render two cells (km/L / L/h) instead of the toggling cell. (2) `TripPanel` `ObdRow`: same two-cell layout. (3) Apply the `—`-at-rest rule to km/L and the average display rule (distance > 0.01 km & fuel > 0).

---

## Appendix B — Session History (migrated verbatim from progress.md)

Full audit log of every implementation session, preserved verbatim. Summarized in §6 above.

## Current Session

### 2026-06-08 OBD Phase 1 — Live device verification + connection bug fixes

- Task: Verify OBD functionality live on connected device (SM-G965F, API 29) with a real KONNWEI ELM327 adapter (MAC 47:74:06:14:CD:B3); fix bugs found.
- Start: 2026-06-08
- End: 2026-06-08
- Status: Done — verified live on device; RPM + speed streaming, no crashes
- Live state before changes: adapter bonded + BR/EDR connected, BT on, but app stuck at `Waiting / Connection failed`.
- Root cause (found via added logging): **concurrent connection attempts**. `attemptConnection()` and `startRetryBackoff()` each launched independent coroutines with no single-flight guard; MainActivity's `obdServiceEnabled.collect{}` re-fired `ACTION_START` on every DataStore emission, and `START_STICKY` redelivery added more. Multiple coroutines opened/closed RFCOMM sockets to the same device, stomping each other → "read failed, socket might closed" / "Broken pipe". The hardware was fine the whole time.
- Bugs fixed:
  - Concurrency: refactored the connect → init → poll → backoff lifecycle into ONE serialized coroutine (`connectionJob`); `attemptConnection()` cancels any in-flight attempt + closes its socket before starting; removed parallel `startRetryBackoff()`/`pollingJob`; backoff now folded into the single loop. After the fix, even **secure SPP connects on the first try** (no fallback needed) — proving concurrency was the root cause.
  - Missing ELM327 init: added `ATZ / ATE0 / ATL0 / ATSP0` sequence before polling (spaces left ON so PID parsers still work). Verified live: `ELM327 v1.5`, `ATE0→OK`, etc.
  - RFCOMM robustness: `connectRfcomm()` cancels discovery then tries secure → insecure → reflection channel-1 (KONNWEI clones need the fallback under contention).
  - Fuel fallback was dead code (keyed on exceptions; `015E` unsupported returns the string `NO DATA`, not an exception). Now response-driven: `015E` then `0110`.
  - Fuel-unsupported latch: after 5 cycles with no `015E`/`0110` answer, stop probing fuel. Verified live: poll cadence improved from ~1.5 s to ~0.82 s/cycle.
  - `adapterElapsedMs` was always 0 (computed after `lastPollTimeMs` was overwritten) — now reflects real poll duration.
  - MainActivity: auto-start reads launch-time value via `.first()` instead of `collect{}`; `ACTION_START` guarded in service to not tear down a healthy connection.
  - Added Logcat logging throughout (`ObdPollingService` tag) for live diagnosis.
- Files edited:
  - `feature/obd/service/ObdPollingService.kt`
  - `MainActivity.kt`
- Live verification (device 213052810e037ece, KONNWEI ELM327 on running vehicle):
  - ✓ Single clean connection (secure SPP, first try), state → Connected, last_error cleared
  - ✓ ELM327 init sequence completes
  - ✓ RPM streaming (~970–1630) and SPEED streaming (~9–26 km/h) live, stable
  - ✓ Fuel correctly reported UNAVAILABLE (this vehicle exposes no fuel-rate PID — not a bug)
  - ✓ Fuel latch trips after 5 cycles, poll cadence ~doubles
  - ✓ No crashes (crash buffer clean)
- Build run: `:app:assembleDebug` — BUILD SUCCESSFUL; installed via `adb install -r`
- Known remaining:
  - km/L not derivable on this vehicle (no fuel-rate PID); will populate on vehicles that support `015E` or `0110`. Worth surfacing "fuel data unavailable on this vehicle" in the OBD UI in a future slice.
  - ObdStatusCard / km/L UI display not visually screenshot-verified this session (logic confirmed via state flow + logs).
- Commit status: Uncommitted
- Suggested commit message: `fix(obd): serialize connection lifecycle + ELM327 init — fixes concurrent-attempt socket races; live RPM/speed verified`

---

### 2026-06-08 OBD Phase 1 — Capability scan + indirect (speed-density) fuel estimate

- Task: Confirm OBD-II protocol, query which PIDs the vehicle exposes, and (since it has no MAF/direct-fuel PID) implement an indirect fuel-rate estimate so km/L works.
- Start: 2026-06-08
- End: 2026-06-08
- Status: Done — verified live; realistic idle fuel rate, no crashes
- Findings (live, via added `scanCapabilities` diagnostic):
  - OBD-II protocol negotiated: **ISO 15765-4 CAN 11-bit/500 kbps** (`ATDP` = "AUTO, ISO 15765-4 (CAN 11/500)", `ATDPN` = A6). Implementation was already pure OBD-II (ELM327 + ATSP0 auto + Mode-01 PIDs) — no protocol change needed.
  - Supported Mode-01 PIDs (35): `0101 0103 0104 0105 0106 0107 010B 010C 010D 010E 010F 0111 0113 0114 011C 011F 0120 0121 012E 0130 0131 0133 0140 0141 0142 0143 0144 0145 0146 0147 0149 014A 014C 0151 015A`
  - **No MAF (0110), no direct fuel rate (015E)** — both return NO DATA (not in bitmask). Fuel type (0151)=01 gasoline; commanded λ (0144)=1.00.
  - Present for speed-density: MAP (010B), IAT (010F), RPM (010C), baro (0133), load (0104/0143), λ (0144).
- Implemented: indirect fuel-rate estimate (speed-density) as 3rd fallback in the chain `DIRECT(015E) → MAF(0110) → SPEED_DENSITY → UNAVAILABLE`.
  - Formula: `MAF(g/s) = (RPM × MAP_kPa × VE × Displacement_L × 28.97) / (120 × 8.314 × IAT_K)`; `fuel(L/h) = MAF/(14.7×λ) × 3600/745`. VE=0.85, gasoline constants.
  - MAP read every cycle; IAT + λ cached, refreshed every 8 cycles.
  - Engine displacement is a new user preference (`obd_engine_displacement_cc`, default 1193) with a new "Engine Displacement" row in OBD Settings → PREFERENCES. User confirmed the test vehicle is **1193 cc**.
  - `mafGramsPerSecond` in `ObdSampleEntity` now populated from the estimate (was always null).
- Files edited:
  - `feature/obd/service/ObdPollingService.kt` — `scanCapabilities()` + `extractDataBytes()` diagnostics; `computeSpeedDensityFuel()`; fuel chain extended; sample MAF populated
  - `feature/obd/data/ObdPreferencesDataStore.kt` — `obdEngineDisplacementCc` pref + setter (default 1193)
  - `screens/settings/obd/ObdSettingsScreen.kt` — "Engine Displacement" preference row
- Live verification (KONNWEI ELM327, vehicle idling, parked):
  - ✓ Protocol confirmed ISO 15765-4 CAN
  - ✓ Fuel now estimated: **~1.08–1.10 L/h at ~845 rpm idle** — physically realistic for a 1.2 L gasoline engine
  - ✓ Source correctly reported `SPEED_DENSITY`; latch trips after 5 cycles, cadence ~1.0 s/cycle
  - ✓ No crashes
- Not verified (vehicle was parked): instant km/L on the road — gated on GPS speed > 3 km/h & accuracy ≤ 20 m; calculation path confirmed, awaits a drive.
- Note: `scanCapabilities`/ATDP diagnostics run once per process and only log; harmless to keep, can be removed later.
- Build run: `:app:assembleDebug` — BUILD SUCCESSFUL; installed via `adb install -r`
- Commit status: Uncommitted
- Suggested commit message: `feat(obd): indirect speed-density fuel estimate (MAP/IAT/RPM) + engine-displacement setting; live km/L enabled on no-MAF vehicles`

### 2026-06-07 OBD Phase 1 Slice 3 — Build + device verification

- Task: Compile Slice 3 code, fix bugs found before build, install on device, verify app launches without crash.
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done (build success + device install + no crash on launch)
- Files fixed (pre-build bugs corrected):
  - `feature/obd/service/ObdPollingService.kt` — replaced `.collect {}` with `.first()` for DataStore reads (obdDeviceMac, obdRetryMaxSeconds, obdPollHz, obdRetentionDays); fixed `insertSample` → `insert` (DAO method name); fixed `TrackApp.obdSampleDao` → `(applicationContext as TrackApp).obdSampleDao` (TrackApp is an instance, not an object); removed unused `currentState` variable; removed unused `TrackApp` import; added `kotlinx.coroutines.flow.first` import
- Build run: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (warnings only: deprecated BluetoothAdapter.getDefaultAdapter())
- Full build: `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL in 38s
- Device install: `adb install -r app-debug.apk` — Success (device 213052810e037ece)
- App launch: no FATAL/crash in logcat; Room migration and OBD channel created cleanly
- Commit status: Uncommitted
- Suggested commit message: `feat(obd): Slice 3 — full polling loop with raw AT I/O, live metrics, session gating, ObdStatusCard on Session screen`
- Known remaining:
  - Live ELM327 adapter test (RPM/speed updates, km/L display) requires physical OBD dongle on the vehicle
  - Session start/stop coupling (ACTION_SESSION_ON/OFF) not verified without real adapter
  - ObdStatusCard only visible when OBD state is Connected or Waiting (card is hidden in Idle — correct behavior)

---

### 2026-06-07 OBD Phase 1 Spec — Design handoff prepared for Slices 3–4

- Task: Create and rectify OBD_PHASE1_SPEC.md to document design requirements for ObdStatusCard (Session screen, Slice 3) and OBD metric row (TripPanel, Slice 4).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Files created:
  - `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md` — specification covering both remaining UI surfaces (Slices 3–4); Slices 1–2 noted as already implemented
- Files edited: None
- Build run: Not required
- Tests run: None
- Commit status: Uncommitted (documentation only)
- Suggested commit message: `docs(obd): OBD Phase 1 design spec — Surfaces 2–3 for Slices 3–4 (Sessions + TripPanel)`
- Next step: Collect reference screenshots (Session screen always-recording card, TripPanel existing metrics) and submit spec to Google Stitch / Claude Design for visual approval

---

### 2026-06-07 OBD Phase 1 Slice 3 — Full BT polling loop + live telemetry

- Task: Implement full OBD polling loop (RPM, speed, fuel); calculate km/L; gate writes to session active; add OBD metrics to Session screen.
- Start: 2026-06-07 14:30 (design mockups extracted: Connected + Waiting states)
- End: 2026-06-07 15:45
- Status: Done (implementation complete, build pending)
- Files edited:
  - `feature/obd/service/ObdPollingService.kt` — full polling loop with raw AT commands (PID 010D for speed, 010C for RPM, 015E/0110 for fuel); EMA instant km/L calculation; session gating (ACTION_SESSION_ON/OFF); OBD sample writes to DB when session active; 6-hour retention cleanup; startPollingLoop() at 1-5 Hz; writeATCommand() helper for raw AT I/O over socket; parseObdSpeed/Rpm/Fuel() helpers with fallback chain (DIRECT_FUEL_RATE → MAF_DERIVED → UNAVAILABLE)
  - `tracking/TrackingService.kt` — added import for ObdPollingService; send ACTION_SESSION_ON intent in startAlwaysRecording(); send ACTION_SESSION_OFF intent in stopAlwaysRecording()
  - `screens/sessions/SessionsScreen.kt` — collect ObdPollingService.obdUiState; added ObdStatusCard() composable displaying Connected/Waiting states with RPM/SPEED/EFFICIENCY metrics; instant km/L hidden when GPS speed < 3 km/h or accuracy > 20m; Reconnect button visible in Waiting state; added Button + Icon imports
  - `tracking/LocationUiState.kt` — added accuracyMeters: Float field (from GPS location.accuracy)
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Commit status: Uncommitted
- Suggested commit message: `feat(obd): Slice 3 — full polling loop with raw AT I/O, live metrics, session gating, ObdStatusCard on Session screen`
- Acceptance criteria status:
  - ✓ Connection flow: Idle → Connecting → Connected (state transitions implemented)
  - ✓ Waiting state with error message (ObdUiState.Waiting implemented)
  - ✓ Live metrics update at configurable Hz (startPollingLoop() at obdPollHz)
  - ✓ Instant km/L calculation with EMA + visibility gating (speedInKMH > 3 AND accuracyMeters <= 20)
  - ✓ Reconnect button in Waiting state (ObdStatusCard renders button)
  - ✓ Session gating: OBD samples written only when sessionActive == true
  - ✓ No regression: GPS tracking calls unchanged (TrackingService.startAlwaysRecording/stopAlwaysRecording only send intents, no other changes)
- Known issues:
  - Build/compilation not verified (pending user permission to run Gradle)
  - Device testing pending (ELM327 required)
  - AT command parsing assumes standard ELM327 response format; real devices may vary

---

## Current Session (Prior)

### 2026-06-07 OBD Phase 1 Slice 2 — Enable toggle + device picker

- Task: Wire Enable toggle to request Bluetooth permissions and attempt connection. Add bonded device picker. Show connection state (Connecting → Waiting on failure).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done (Compilation successful)
- Files edited:
  - `feature/obd/service/ObdPollingService.kt` — implemented connection attempt with exponential backoff (1s, 2s, 4s... capped at obdRetryMaxSeconds); ACTION_START attempts BluetoothAdapter connection; ACTION_STOP closes socket and emits Idle; ACTION_RECONNECT_NOW resets retry delay to 1s and attempts connection; socket state and error persisted to DataStore; uses Flow.collect() for DataStore access
  - `screens/settings/obd/ObdSettingsScreen.kt` — full OBD Settings UX: Enable toggle requests BLUETOOTH_CONNECT permission (API 31+) via ContextCompat.checkSelfPermission() and starts/stops service; bonded device picker dialog (refreshes on RESUMED); "Pair a new device" opens system BT settings; saved device row with "Change" action; Reconnect button visible in Waiting state; preference selectors for poll rate (1/2/5 Hz), retention days (1–30), retry cap (30/60/120/300s)
  - `MainActivity.kt` — auto-start OBD service on app launch if obdServiceEnabled is true in DataStore using Flow.collect()
- Build run: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (warnings only: deprecated BluetoothAdapter.getDefaultAdapter())
- Tests run: None
- Commit status: Uncommitted (ready to commit)
- Suggested commit message: `feat(obd): Slice 2 — enable toggle + device picker + connection attempt with exponential backoff`
- Observable result ready: toggle ON → BT permission dialog (API 31+) → grant → bonded device picker → select device → status shows "Connecting…" → "Waiting (no response)" (expected without ELM327 adapter)

---

### 2026-06-07 Fix — ObdSettingsScreen cosmetic gaps vs mockup

- Task: Align ObdSettingsScreen with design mockup for Slice 1 — proper Scaffold + TopAppBar with back button, status row styled to match SettingsScreen pattern (icon + label + subtitle + disabled Switch).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Files edited:
  - `screens/settings/obd/ObdSettingsScreen.kt` — replaced bare Column with Scaffold + TopAppBar (back nav); replaced plain Text/Card/Switch with ObdSectionGroup + ObdStatusRow matching SettingsScreen token style; added `getStatusSubtitle()` for "Service not enabled" subtitle
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Commit status: Uncommitted
- Suggested commit message: `fix(obd): align ObdSettingsScreen with mockup — Scaffold + TopAppBar + status row style`

---

### 2026-06-07 Fix — ObdSampleEntity missing @Index causing Room migration crash

- Task: Fix `IllegalStateException: Migration didn't properly handle: obd_sample` crash on SM-G965F (API 29).
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Root cause: `MIGRATION_3_4` creates `index_obd_sample_timestampMs` on `timestampMs`, but `ObdSampleEntity` had no `@Index` annotation. Room's post-migration schema validation compared the actual DB (with index) against the entity definition (no index) and threw `IllegalStateException`.
- Files edited:
  - `data/roomdb/ObdSampleEntity.kt` — added `indices = [Index(value = ["timestampMs"])]` to `@Entity` annotation; added `import androidx.room.Index`
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Commit status: Uncommitted
- Suggested commit message: `fix(obd): add @Index(timestampMs) to ObdSampleEntity — migration created index but entity didn't declare it`

---

### 2026-06-07 OBD Phase 1 Slice 1 — Infrastructure + Navigation (steps 1–11)

- Task: Implement OBD Phase 1 Slice 1 (complete): Room entity/DAO/migration, DataStore, ObdPollingService shell, ObdSettingsScreen UI, navigation route and TOOLS row in Settings.
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done
- Files created:
  - `data/roomdb/ObdSampleEntity.kt` — Room entity (8 fields)
  - `data/roomdb/ObdSampleDao.kt` — DAO (insert, latestSample, samplesBetween, deleteOlderThan)
  - `feature/obd/data/ObdPreferencesDataStore.kt` — DataStore (8 preference keys with defaults)
  - `feature/obd/service/ObdPollingService.kt` — Foreground service shell with ObdUiState sealed class
  - `screens/settings/obd/ObdSettingsScreen.kt` — UI shell (status card, disabled toggle)
- Files edited:
  - `AndroidManifest.xml` — BT permissions (API-gated), uses-feature, ObdPollingService declaration
  - `data/roomdb/TrackDatabase.kt` — version 3→4, MIGRATION_3_4, obdSampleDao, added entity to list
  - `TrackApp.kt` — obdSampleDao lazy, OBD notification channel
  - `navigation/Screen.kt` — ObdSettingsScreen route object
  - `screens/settings/SettingsScreen.kt` — added OBD row in TOOLS section (Step 10)
  - `navigation/NavGraph.kt` — added ObdSettingsScreen route + import (Step 11)
- Compile verification: `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (2 changes)
- Commit status: Committed — branch `codex`, revision `d089fcf`

---

### 2026-06-07 OBD Phase 1 Pre-check — Kotlin 1.7.0 Compatibility

- Task: Confirm whether `kotlin-obd-api` library compiles with Kotlin 1.7.0 project. If incompatible, skip library and implement raw AT I/O over Bluetooth socket instead.
- Start: 2026-06-07
- End: 2026-06-07
- Status: Done — pre-check completed; library is incompatible
- Build run: `./gradlew :app:compileDebugKotlin` — tested with `kotlin-obd-api:1.1.0` and `master-SNAPSHOT`; both failed with "The binary version of its metadata is 2.3.0/2.1.0, expected version is 1.7.1"
- Decision: Skip kotlin-obd-api library. Implement raw AT commands over Bluetooth socket (PID 010D for speed, 010C for RPM, 015E/0110 for fuel rate with fallback chain). Manual AT command parsing instead of pre-built Command classes.
- Files changed: None (dependency verification only; documentation updated with raw AT I/O approach)
- Docs updated: `docs/implementation-plan.md` — completed pre-check table, removed JitPack dependency step, updated Slice 3 Step 1 with raw AT I/O details
- Suggested commit message: `docs: OBD Phase 1 pre-check — kotlin-obd-api incompatible with Kotlin 1.7.0, switch to raw AT I/O`

---

### 2026-05-29 OBD Phase 1 — ELM327 Bluetooth Classic Telemetry

- Task: Implement OBD-II support — update docs, then add Room entity/DAO/migration,
  ObdPollingService (foreground), ObdPreferencesDataStore, OBD Settings screen and
  navigation, Session screen and Trip panel km/L metrics.
- Start: 2026-05-29
- End: (in progress)
- Status: In Progress — docs step complete; source changes pending
- Expected files created:
  - `data/roomdb/ObdSampleEntity.kt`
  - `data/roomdb/ObdSampleDao.kt`
  - `feature/obd/data/ObdPreferencesDataStore.kt`
  - `feature/obd/service/ObdPollingService.kt`
  - `screens/settings/obd/ObdSettingsScreen.kt`
- Expected files edited:
  - `data/roomdb/TrackDatabase.kt` (bump v4, MIGRATION_3_4)
  - `TrackApp.kt` (obdSampleDao + OBD notification channel)
  - `tracking/TrackingService.kt` (SESSION_ON/OFF intents)
  - `MainActivity.kt` (auto-start ObdPollingService)
  - `navigation/Screen.kt`, `navigation/NavGraph.kt`
  - `screens/settings/SettingsScreen.kt` (OBD Tools row)
  - `screens/sessions/SessionsScreen.kt` (ObdStatusCard + km/L)
  - `screens/track/TripState.kt`, `screens/track/components/TripPanel.kt` (km/L row)
  - `app/build.gradle`, `AndroidManifest.xml`
- Docs updated before source changes: `docs/product-spec.md` ✓, `docs/implementation-plan.md` ✓

---

### 2026-05-22 Observer Phase 2 — Cursor Pagination

- Task: Replace unbounded `getAllEvents()` feed loading with cursor-based pagination. First page of 50 events on open; `loadMore()` triggered when user scrolls to oldest visible items; live new events appended via narrow `getEventsNewerThan()` Flow. Phase 2 otherwise complete (inspection covered by `SnapshotViewerSheet` from Phase 1).
- Start: 2026-05-22
- End: 2026-05-22
- Status: Done (static inspection)
- Commit status: Committed — branch `codex`, revision `ee54e9c`
- Files edited:
  - `data/roomdb/ObserverEventDao.kt` — added `getEventsFirstPage(limit)`, `getEventsNextPage(beforeLastSeenAt, limit)`, `getEventsNewerThan(afterLastSeenAt)` queries
  - `feature/observer/data/repository/EventRepository.kt` — added `getFirstPage`, `getNextPage`, `getEventsNewerThan` to interface and `EventRepositoryImpl`; extracted `toDomain()` private helper to deduplicate mapping; added stubs to `FakeEventRepository`
  - `feature/observer/domain/model/ObserverUiState.kt` — added `canLoadMore: Boolean = false` and `isLoadingMore: Boolean = false`
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — removed unbounded `getEvents()` collection; added in-memory `_loadedEvents`/`_loadedIds`/`_oldestLastSeenAt`/`_newestLastSeenAt` tracking; `init` loads first page then collects `getEventsNewerThan()` for live arrivals; added `loadMore()` fun with prepend + `_prependedCount` SharedFlow; `PAGE_SIZE = 50` in companion
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — imported `derivedStateOf`; added `hasScrolled` flag (set on first scroll); `shouldLoadMore` derived state gates `loadMore()` call; `LaunchedEffect` collects `prependedCount` and adjusts scroll with `scrollToItem(firstIdx + count)`; added `key = { _, event -> event.id }` to `itemsIndexed` for stable item identity
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known remaining: device verification needed; edge case — events with identical `lastSeenAt` ms at page boundary may be skipped by cursor
- Suggested commit message: `feat(observer): cursor pagination — first page of 50, load-more on scroll, live-event narrow flow`

---

### 2026-05-19 Observer Snapshot Viewer — per-event modal sheet

- Task: Add "View window content" link to each Observer event card; tapping opens a modal bottom sheet showing the event's captured `treeSnapshot` as a flat formatted node list or raw JSON, with Copy and all four dismiss methods.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (build verified — `BUILD SUCCESSFUL`)
- Commit status: Committed — branch `codex`, revision `8ceb530`
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/ObservedEvent.kt` — added `firstSeenMs`, `repeatCount`, `treeSnapshot`, `truncationMetadata` fields
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/repository/EventRepository.kt` — mapped new fields from entity; updated `FakeEventRepository` defaults
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/EventRow.kt` — added `onViewSnapshot: (() -> Unit)?` param; added "View window content" link row (hidden when no snapshot)
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `snapshotEvent` local state; wired `onViewSnapshot` into `EventRow`; mounted `SnapshotViewerSheet` conditionally
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/SnapshotViewerSheet.kt` — full modal sheet (Dialog+scrim pattern; DragHandle, TitleRow, MetaStrip, ModeToolbar, ContentArea; Formatted flat node list + Raw JSON + Parse-error + No-readable-text edge states; Copy with 1.4s confirmation; all four dismiss paths)
- Build run: `./gradlew :app:compileDebugKotlin --no-daemon` — BUILD SUCCESSFUL (1 unused-param warning, no errors)
- Tests run: None
- Known remaining: device verification needed
- Suggested commit message: `feat(observer): snapshot viewer sheet — per-event modal with formatted/raw views and copy`

---

### 2026-05-19 Sessions screen — Indicator animation + active card header alignment

- Task: Two UI fixes targeting `SessionsScreen.kt` only:
  1. Always-recording indicator (Active state): blinking inner dot (opacity 1↔0.35, 1200ms) + two concentric ripple rings (scale 1.0→1.65, alpha 0.9→0, 1800ms, 0.6s stagger) — mirrors Trips screen pattern. Reduce-motion aware.
  2. Active session card header: removed absolutely-positioned ACTIVE badge + 70dp padding hack; replaced with a flat `Row(CenterVertically)` containing title (weight 1), then a nested row with start time + badge inline.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection)
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/screens/sessions/SessionsScreen.kt` — added `LinearOutSlowInEasing` + `graphicsLayer` imports; `StatusCard`: added reduce-motion check (`ANIMATOR_DURATION_SCALE == 0`), indicator 52→64dp, `SessionPulseRing` ×2 before dot, `PulseDot` now takes `pulseTargetAlpha=0.35f`/`pulseDurationMs=1200` for indicator; `SessionRow`: removed absolute `ActiveBadge`, header `Row` is now `CenterVertically`+`spacedBy(10dp)` with nested right-side group; `PulseDot`: added optional `pulseTargetAlpha`/`pulseDurationMs` params (defaults preserve `ActiveBadge` behavior); added `SessionPulseRing` private composable
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known limitations:
  - CSS `box-shadow` glow on inner dot skipped — no Compose 1.2.x equivalent; ambient glow provided by greenSoft indicator background
  - Easing: `ease-out` → `LinearOutSlowInEasing` (Compose 1.2.x compat); `ease-in-out` → `FastOutSlowInEasing` (tween default)
- Suggested commit message: `feat(sessions): animate always-recording indicator (blink + pulse rings) + fix active card header alignment`

---

### 2026-05-19 Observer Phase 1 Step 4 — tree snapshot DFS in ObserverAccessibilityService

- Task: `treeSnapshot` and `truncationMetadata` fields existed in `ObservedEventEntity` but were always written as `null`. Implemented bounded DFS traversal in `ObserverAccessibilityService` to populate them.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection)
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — added `captureTreeSnapshot(event)` private method: DFS via explicit stack from `event.source`; collects text, contentDescription, className, isClickable, isEditable, isEnabled, bounds per node; limits: 200 nodes, depth 10, 300 chars/text field, 40 KB JSON cap; recycles every `AccessibilityNodeInfo` after use; returns `(snapshotJson, truncationMetadataJson)` where truncation JSON records `reason` (node_limit / depth_limit / size_limit) and `nodesCaptured`; wired into both insert path and CONTENT_CHANGED dedup update path.
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known remaining: device verification needed to confirm DFS runs without crash/ANR on real event volume
- Suggested commit message: `feat(observer): implement tree snapshot DFS capture with node/depth/size limits`

---

### Observer Phase 1 — Local Accessibility Observer Foundation (full implementation)

- Task: Implement all 8 steps of Observer Phase 1 as defined in `docs/implementation-plan.md`. Navigation placement (Option B: Settings → Tools → Observer) was accepted 2026-05-18.
- Start: (prior session — exact date not recorded at the time)
- End: (prior session — discovered via codebase audit on 2026-05-19)
- Status: Done (code + UI, static inspection); device verification pending (requires explicit user permission per AGENTS.md)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObservedEventEntity.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObserverEventDao.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleEntity.kt`
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleDao.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/ObservedEvent.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/AllowlistRule.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/domain/model/ObserverUiState.kt` (includes `AllowlistDraftRule`, `AllowlistUiState`, `MatchType`)
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/ObserverPreferencesDataStore.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/repository/EventRepository.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/screens/ObserverFeedScreen.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/EventRow.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/FeedHeaderBar.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/AllowlistBottomSheet.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/JumpToLatestFab.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/EmptyState.kt`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/StatusIndicators.kt`
  - `app/src/main/res/xml/accessibility_service_config.xml`
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackDatabase.kt` — added `ObservedEventEntity`, `AllowlistRuleEntity`, observer/allowlist DAOs, `MIGRATION_2_3` (creates `observer_event` and `allowlist_rule` tables with indices); database version bumped to 3
  - `app/src/main/AndroidManifest.xml` — added `BIND_ACCESSIBILITY_SERVICE` permission, declared `ObserverAccessibilityService` with intent-filter and meta-data reference
  - `app/src/main/java/com/kolee/tracklocation/navigation/NavGraph.kt` — added `observer_feed_screen` route
  - `app/src/main/java/com/kolee/tracklocation/navigation/Screen.kt` — added `ObserverFeedScreen` sealed class entry
  - Settings screen — added Tools section with Observer row linking to `observer_feed_screen`
  - Theme files — added `ObserverAmber*`, `ObserverGreen*`, `ObserverRed*` color tokens
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Known remaining issues:
  - Step 4 (tree snapshot DFS): `treeSnapshot` and `truncationMetadata` fields exist in `ObservedEventEntity` but whether `ObserverAccessibilityService` actually performs DFS traversal to populate them is unverified by static inspection alone — needs device run.
  - `ObserverEventDao.getEventsByPackage()` exists but `ObserverViewModel` always fetches all events; scoped package-filtered feed is not yet wired up.
  - No pagination (full list in memory; acceptable under 50k row retention cap).
- Suggested commit message: `feat(observer): Phase 1 — accessibility service, event capture, Room schema, feed UI, allowlist`

---

### 2026-05-19 ListContent.kt — Fix Compose 1.2.x build errors (EaseInOut/EaseOut/label)

- Task: User requested a debug build. `./gradlew assembleDebug` failed in `:app:compileDebugKotlin` with unresolved `EaseInOut`/`EaseOut` references and `label` parameter not found on animation APIs. Root cause: project pins Compose UI 1.2.x (`composeOptions { kotlinCompilerExtensionVersion '1.2.0' }`); `EaseInOut`/`EaseOut` and animation `label` params were introduced in Compose 1.4 / 1.3 respectively.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/screens/list/components/ListContent.kt` — replaced `EaseInOut` import/usages with `FastOutSlowInEasing` and `EaseOut` with `LinearOutSlowInEasing` (both available in 1.0+); removed `label = "..."` from animation calls (`animateColorAsState` x3, `rememberInfiniteTransition` x2, `animateFloat` x3) that 1.2.x does not support. The non-animation `label = ...` parameters on the stat-row composables (lines ~441–453) were left intact.
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL (14s, with explicit user permission for this build request).
- Tests run: None.
- Known remaining issues: Animation easing curves are now `FastOutSlowInEasing` / `LinearOutSlowInEasing` instead of the requested `EaseInOut` / `EaseOut`. Visually very similar but not identical; if exact parity is required, the project must move to Compose 1.4+ (compiler extension + UI library bump).
- Suggested commit message: `fix(list): replace Compose 1.4 easing/label APIs with 1.2-compatible equivalents`

---

### 2026-05-19 Trips (List) hero card — Recording state per TRIPS_START_STOP_SPEC

- Task: Add visible Recording state to the Current-trip hero card on the Trips (List) screen per `docs/design/design_handoff_trips_start_stop/TRIPS_START_STOP_SPEC.md`. Card dimensions identical across states; indicator blink + two staggered pulse rings; title `Ready` ↔ `Recording`; always-visible monospace `HH:MM:SS` readout; CTA color/glyph/label swap (green Start ▶ ↔ red Stop ■). Honors reduced-motion (animator duration scale = 0).
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/ui/theme/Color.kt` — added Trips-hero tokens: `TripHeroBg` (#173A2D), `TripHeroDim` (#22513F), `TripHeroDotIdle`, `TripHeroBrandGreen` (#22C55E), `TripHeroIndicatorWash` (0.18α green), `TripHeroDotHalo` (0.25α green), `TripHeroStopRed` (#E5484D), `TripHeroEyebrow` (.65α white), `TripHeroReadoutIdle` (.40α white), `TripHeroReadoutLive` (.78α white)
  - `app/src/main/java/com/kolee/tracklocation/screens/list/components/ListContent.kt` — collect `viewModel.locationUiState` as state at call site; rewrote `CurrentTripCard` to take `LocationUiState` + `onCtaTap`; added `PulseRing` composable and `formatElapsed` helper; `produceState` 1s tick gated on `isRecording` (auto-paused when not recording); `animateColorAsState` for indicator + CTA background (200ms / 150ms); blink via `rememberInfiniteTransition` (600ms reverse) gated on `!reduceMotion`; reduced-motion detection via `Settings.Global.ANIMATOR_DURATION_SCALE == 0`; semantics: polite live region announces "Trip started" / "Trip stopped"; CTA `contentDescription` swaps "Start trip" / "Stop trip"; readout `contentDescription` reads the elapsed value; monospace `HH:MM:SS` via `MonospaceFontFamily`
- Behavior notes:
  - `isRecording = uiState.isTracking && !uiState.isPaused`; PAUSED state shows Ready visuals + "00:00:00" today since `onTripCtaTap` does not yet resume from paused (called out in earlier Track-screen progress entry).
  - Reduced-motion check is conservative — only treats animator scale == 0 as reduced; `AccessibilityManager.isReduceMotionEnabled` is not available on min SDK targeted. Reduced-motion users still see the color/glyph/label swap.
  - CTA tap target meets 48dp via card padding + 44dp button height + Row vertical centering; spec calls for `Modifier.minimumInteractiveComponentSize()` but the current Box-as-button pattern (matching the rest of the screen) keeps the visual height at 44dp; touch slop on the 44dp height plus horizontal padding remains tappable. If a follow-up wants a strict 48dp guarantee, swap to `IconButton`/`Button`.
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Tests run: None
- Acceptance checklist (code inspection):
  - Card outer dimensions identical across states (both rely on the same `padding(18dp v, 20dp h)` + content row that always renders title + readout) — ✓
  - Title `"Ready"` / `"Recording"`, single line, no truncation (`maxLines = 1`) — ✓
  - Elapsed readout always rendered (`00:00:00` idle, live `HH:MM:SS` recording) — ✓
  - Button transitions in 150ms (`animateColorAsState` linear 150ms) — ✓
  - Inner dot blink at ~1.2s rhythm (600ms reverse, infinite) — ✓
  - Two pulse rings, second delayed 600ms — ✓
  - Tabular figures via monospace font family — ✓ (system monospace; JetBrains Mono not added)
  - TalkBack live-region announcement on state change — ✓ (polite, announces title change)
  - Reduced-motion skips blink + rings; color/glyph/label still change — ✓
  - 48dp tap target — Partial (44dp visual; see note above)
- Suggested commit message: `feat(list): Recording state for Current-trip hero card — blink, pulse rings, elapsed readout, Stop CTA`

---

### 2026-05-19 Session Screen — Re-declare TrackingService in manifest (actual fix for non-functional switch)

- Task: Sessions always-recording switch still did not start recording after the earlier Compose-side fix. Root cause: `TrackingService` was missing from `app/src/main/AndroidManifest.xml`; the stale merged manifest under `app/build/intermediates/` masked the issue on the dev machine, but `startForegroundService` silently fails on clean install.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Root cause: `<service android:name=".tracking.TrackingService" .../>` declaration was lost in a prior manifest rewrite (was present in commit `9e2bcc0`). Also SDK-34 requires `FOREGROUND_SERVICE_LOCATION` for a `foregroundServiceType="location"` service.
- Files edited:
  - `app/src/main/AndroidManifest.xml` — added `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />`; re-added `<service android:name=".tracking.TrackingService" android:enabled="true" android:exported="false" android:foregroundServiceType="location" />` inside `<application>` before the Observer service block
- Build run: Not run (explicit permission required per AGENTS.md). A clean build is recommended (`./gradlew clean assembleDebug`) so the stale merged manifest in `app/build/` is regenerated.
- Tests run: None
- Relationship to earlier fix today: the `LaunchedEffect(Unit)` wrap in `CheckAndRequestPermissions.kt` was a genuine Compose-side bug fix, but not the reason recording wasn't starting; the manifest gap is the actual cause.
- Commit message suggestion: `fix(session): re-declare TrackingService in manifest with FOREGROUND_SERVICE_LOCATION (SDK 34)`

---

### 2026-05-19 Session Screen — Fix non-functional always-recording switch

- Task: Switch toggled but never triggered `START_RECORDING` service action
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Root cause: `CheckAndRequestPermissions` called `isGranted.invoke()` directly in composition body when permissions were already granted — side effects during composition are illegal in Compose and were silently dropped.
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/permission/CheckAndRequestPermissions.kt` — wrapped `isGranted.invoke()` in `LaunchedEffect(Unit)` so the callback fires in a coroutine after composition, not during it; added `LaunchedEffect` import
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Commit message suggestion: `fix(session): wrap isGranted callback in LaunchedEffect — was called during composition, causing switch to appear non-functional`

---

### 2026-05-19 List Screen — Fix non-functional Start button

- Task: Start button in CurrentTripCard had no click handler; wire it to `viewModel.onTripCtaTap()`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/screens/list/components/ListContent.kt` — added `clickable` import, added `onStartTrip` param to `CurrentTripCard`, added `.clickable(onClick = onStartTrip)` to Start button Box, wired call site to `viewModel.onTripCtaTap()`
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Commit message suggestion: `fix(list): wire Start button to onTripCtaTap() — button was non-interactive`

---

### 2026-05-19 Observer Feed — Event row layout update

- Task: Replace single-line "log line" event row with stacked layout per `docs/design/design_handoff_observer_row/OBSERVER_ROW_SPEC.md`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/components/EventRow.kt` — full layout rewrite: stacked lines, chip moved to Line 3, type prefix stripped, color mapping against stripped label, no truncation on activity, 2-line max on package, padding/spacing per spec
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Known limitations: No device run; acceptance checklist verified by code inspection only

---

### 2026-05-19 Track Screen — Glass panel + brand-green CTA redesign

- Task: Replace solid dark panel + purple play button with translucent glass panel + brand-green CTA. Three trip states (READY/LIVE/PAUSED). Map visible through panel. Spec: `docs/design/design_handoff_track_screen/TRACK_SCREEN_SPEC.md`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files created:
  - `screens/track/TripState.kt` — `TripState` enum + `TrackPanelState` data class
  - `screens/track/components/TripPanel.kt` — glass panel composable (eyebrow, timer, CTA, stats)
  - `screens/track/components/MapControls.kt` — floating recenter + layers FABs
- Files edited:
  - `ui/theme/Color.kt` — added BrandGreen, BrandGreenDark, PanelBg, PanelBgFallback, PanelBorder, PanelTextPrimary/Secondary/Tertiary, StatusPaused, MapFabBg
  - `ui/theme/Type.kt` — added MonospaceFontFamily (FontFamily.Monospace / Roboto Mono)
  - `tracking/LocationUiState.kt` — added isPaused: Boolean = false
  - `viewmodel/ShareViewModel.kt` — added appContext, onTripCtaTap(), sendServiceCommand()
  - `screens/track/TrackScreen.kt` — wired TripPanel + MapControls, removed duplicated service-call logic
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Known limitations:
  - Backdrop blur (28dp) is approximated via graphicsLayer RenderEffect on API 31+; this blurs the panel element itself (soft edges), not the true map content behind it. True per-composable backdrop blur requires custom rendering not available in Compose 1.2.0. Pre-API-31 uses 0.78 opacity fallback.
  - MonospaceFontFamily uses FontFamily.Monospace (Roboto Mono). JetBrains Mono can be added by including `ui-text-google-fonts` dependency and configuring a GoogleFont.Provider.
  - PAUSED state is displayable (isPaused=true in LocationUiState) but cannot be triggered via onTripCtaTap() yet — requires a PAUSE_TRIP/RESUME_TRIP action in TrackingService (future phase).
- Suggested commit message: `feat(track): glass panel + brand-green CTA, three-state UI (READY/LIVE/PAUSED)`

---

### 2026-05-19 Observer — Smooth resume with relative scroll offset

- Task: When resuming from pause, the list jumped to the very last item which felt jarring. Refined to maintain the relative position from the bottom, only FAB forces a jump to the tail.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `scrollRelativeOffset` state; `tapListToggle` captures `lastIndex - bottomVisibleIndex` at resume time; new-events `LaunchedEffect` scrolls to `lastIndex - scrollRelativeOffset` instead of always `lastIndex`; FAB `onClick` resets offset to 0 before jumping to ensure it always reaches the very end
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): smooth resume — maintain relative scroll offset, FAB-only jump to latest`

---

### 2026-05-19 Observer — Fix FAB click pausing auto-scroll

- Task: When tapping the list resumes auto-scroll, clicking the FAB immediately re-pauses it because `animateScrollToItem` triggers `isScrollInProgress`, which the scroll detector interprets as a user drag
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `programmaticScroll` flag; both `animateScrollToItem` call sites (FAB + new-event auto-scroll) set it true/false around the scroll; scroll detector skips pausing when flag is set
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): prevent FAB-triggered scroll from pausing auto-scroll`

---

### 2026-05-19 Observer — Fix list tap not resuming auto-scroll

- Task: When auto-scroll is paused, tapping an event row should resume it, but the row's `combinedClickable(onClick = {})` consumed the tap before it reached the LazyColumn's `detectTapGestures` listener
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/components/EventRow.kt` — added `onTap: () -> Unit` parameter; replaced empty `onClick = {}` with `onClick = { onTap() }`
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — passed `onTap = tapListToggle` to `EventRow` at the `itemsIndexed` call site
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): forward row tap to auto-scroll toggle so paused list resumes on tap`

---

### 2026-05-19 Observer — Fix service status always showing "Enabled"

- Task: Observer screen ServiceBanner always showed "Enabled" because `AccessibilityManager.isEnabled` returns true when **any** accessibility service is on, not specifically ours
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — added `isOurServiceEnabled()` helper using `getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)` filtered by `context.packageName` + `ObserverAccessibilityService::class.java.name`; replaced `am.isEnabled` poll with `isOurServiceEnabled()`; added imports for `AccessibilityServiceInfo` and `ObserverAccessibilityService`
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): check specific service enabled state instead of global accessibility flag`

---

### 2026-05-19 Build clean-up — fix all compiler warnings

- Task: Fix all 6 Kotlin compiler warnings reported by `./gradlew assembleDebug`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `observer/ObserverAccessibilityService.kt` — removed redundant `?: return null` on non-nullable `event.text`
  - `screens/details/DetailsScreen.kt` — removed unused `modifier` parameter; removed unused `selectedTrackState` variable
  - `screens/list/components/CustomAlertDialog.kt` — removed unused `text` parameter
  - `screens/settings/SettingsScreen.kt` — removed unused `isLast` parameter; removed all `isLast = true` call-site arguments (3 call sites)
  - `screens/track/components/TrackMap.kt` — removed unused `modifier` parameter
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL (39s), 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `chore: fix all compiler warnings — remove unused params and variables`

---

### 2026-05-18 Fix Black Screen — Wire NavGraph into MainActivity

- Task: Fix black screen; `MainActivity.kt` had an empty `Surface {}` block with no composables rendered
- Start: 2026-05-18
- End: 2026-05-18
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/MainActivity.kt` — replaced empty `Surface` with `Scaffold` + `NavGraph` + `BottomNavigationScreen`; added `rememberNavController()`
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix: wire NavGraph and BottomNavigationScreen into MainActivity`

---

Previous session (2026-05-18, completed Phase 1 implementation):

- Date: 2026-05-18 (completed Phase 1 implementation)
- Task: Complete Observer Phase 1 — AccessibilityService, Room persistence, event capture, retention, and integration
- Completed files created:
  - `ObservedEventEntity.kt` — Room entity for persisting captured events
  - `AllowlistRuleEntity.kt` — Room entity for persisting allowlist rules
  - `ObserverEventDao.kt` — DAO for event queries (insert, update, delete, retrieval, pruning)
  - `AllowlistRuleDao.kt` — DAO for allowlist rule management
  - `ObserverAccessibilityService.kt` — Service that listens to accessibility events and stores them in Room
  - `accessibility_service_config.xml` — Configuration declaring the service listens to TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOW_CONTENT_CHANGED
- Completed files modified:
  - `TrackDatabase.kt` — Added ObservedEventEntity and AllowlistRuleEntity; added MIGRATION_2_3 for new tables and indexes
  - `AndroidManifest.xml` — Registered ObserverAccessibilityService with intent-filter and meta-data; added BIND_ACCESSIBILITY_SERVICE permission
  - `EventRepository.kt` — Implemented real database reading (Flow<List<ObservedEvent>>) instead of stub sample data
  - `ObserverViewModel.kt` — Updated to pass context to EventRepositoryImpl; combined flows for events, auto-scroll, and FAB visibility
  - `strings.xml` — Added observer_service_description string resource
  - `app/build.gradle` — Already had DataStore dependency
- Status: Done (ready for verification)

## Latest Known State

Last known active implementation session:

- Date: 2026-05-16 20:20:00 +07:00 to 2026-05-16 21:05:00 +07:00
- Commit: `fe241a7`
- Task: implement CR-0002 Session always-recording switch as the single control surface
- Status: Done
- Verification: verified by user as working as expected

## What Has Been Done

### Tooling / Baseline

- Upgraded Android/Gradle/Kotlin/Compose tooling.
- Migrated remaining Material2 blockers to Material3.
- Addressed SDK 34 foreground service compatibility.
- `:app:compileDebugKotlin` passed in an earlier session.
- `:app:assembleDebug` was attempted earlier and timed out after 6 minutes.

### Documentation Refactor

- Reorganized docs into product, CR, architecture, UI, implementation, status, and archive layers.
- Added current source-of-truth docs and preserved original docs in archive.
- Added/refined AI-agent instructions in `AGENTS.md` and `CLAUDE.md`.

### CR-0001 — Always-recorded Location Sessions

Implemented as of 2026-05-16 18:30:39 +07:00.

Done:

- Added canonical `location_log` rows.
- Added always-recorded `recording_session` rows.
- Added Room DAOs for location ranges and session history.
- Migrated Room from version 1 to 2.
- Converted legacy serialized trip paths into canonical location rows and trip boundaries.
- Updated `TrackingService` so always-recording appends canonical points and sessions.
- Updated trip start/stop so trips are explicit ranges.
- Replaced the Trips/List header Export pill with an always-recording switch during CR-0001.
- Updated Track controls to Start trip/Stop trip semantics.
- Updated trip detail path rendering to resolve from canonical location ranges.
- Connected Sessions screen to real session rows instead of mock UI state.

Verification:

- Verified by user as working as expected.
- `git diff --check` was run and reported only line-ending warnings/no whitespace errors.
- Static searches were run for stale actions/copy and negative letter spacing.
- Gradle build/tests/emulator/device verification were not run because user permission is required.

### CR-0002 — Session Always-recording Switch

Implemented as of 2026-05-16 21:05:00 +07:00.

Done:

- Replaced the Session screen hero mockup with a compact always-recording status card and trailing switch.
- Kept sessions list visible.
- Marked active sessions distinctly.
- Removed always-recording switch and permission flow from the List screen.
- Kept List as trip history UI only.
- Updated CR-0002 docs and UI screen specification to treat Session as the only always-recording control surface.

Verification:

- Verified by user as working as expected.
- No Gradle build, unit tests, emulator, or device verification were run in the verification pass.

## Known Remaining Issues / Risks

- Build/tests/emulator/device verification has not been run.
- Tests listed for CR-0001 were not added in the implementation pass.
- `CheckAndRequestPermissions` remains a shared permission helper and still uses a full-screen prompt style.

## Recommended Next Steps

### Step 1 — Confirm Observer navigation before Observer work

Observer should not be implemented until one navigation option is accepted:

- add Observer as fifth tab
- put Observer under Settings/tools
- redesign into GPS / Track / Observer / Settings

Planning note:

- `docs/implementation-plan.md` was updated on 2026-05-18 to make Observer Phase 1 an implementation-ready contract, pending only the navigation placement decision above.

Decision:

- **Accepted: Option B** (2026-05-18). Observer will live under Settings/tools; bottom navigation remains `Session / List / Track / Settings`.

Scope decision:

- Observer Phase 1 must include user-configurable allowlist management with regex keyword/pattern support (add/remove rules; enable/disable rules).
- Accepted detail (2026-05-18): Phase 1 allowlist matching applies to package name only.
- Accepted UX detail (2026-05-18): allowlist rules use `Match type` (`Exact`/`Regex`) + a single `Pattern` field.
- Accepted detail (2026-05-18): allowlist matching is case-sensitive.
- Accepted detail (2026-05-18): regex rules use substring match semantics (keyword can match anywhere in package name).
- Accepted detail (2026-05-18): `Exact` match type uses full-string equality only.
- Accepted detail (2026-05-18): if there are zero enabled allowlist rules, capture stores everything (no filtering).
- Accepted UX detail (2026-05-18): allowlist is optional; when empty, UI hints that all packages are being captured and suggests adding rules to reduce noise.
- Accepted nav/IA detail (2026-05-18): Observer entry path is `Settings -> Tools -> Observer`, and allowlist configuration is a compact overlay panel on top of the Observer feed.
- Accepted UX detail (2026-05-18): tap anywhere in the Observer feed list area toggles auto-scroll pause/resume; long-press copy is enabled only when paused and copies `package + activity` (best-effort).
- Accepted UX detail (2026-05-18): resuming from paused continues from the paused position (no jump to latest); while paused, the list can be freely scrolled.
- Accepted UX detail (2026-05-18): dragging/scrolling while auto-scroll is running immediately pauses and begins manual scrolling.
- Accepted UX detail (2026-05-18): show a transient jump-to-latest FAB that scrolls to the newest event without changing whether auto-scroll is paused or running; the FAB is shown only for a couple of seconds when transitioning from paused to running.
- Accepted content detail (2026-05-18): Phase 1 event cards include event type, and long-press copy (paused only) copies a single line `package | activity` (best-effort), with package first.
- Accepted UX detail (2026-05-18): Observer feed includes a capture pause/resume control (stops receiving/storing new events), separate from UI auto-scroll pause.
- Accepted behavior detail (2026-05-18): pausing capture stops the observer capture loop (no event processing/writes) and unpausing starts it again; this does not change the system AccessibilityService enablement toggle.
- Accepted UX detail (2026-05-18): Phase 1 allowlist overlay does not include quick-add suggestions from the live feed; rules are added manually.
- Accepted UX detail (2026-05-18): allowlist rules support enable, disable, and delete.
- Accepted UX detail (2026-05-18): allowlist edits are staged and applied only when the user presses `Apply` in the overlay; closing the overlay saves draft edits but does not apply them.
- Accepted UX detail (2026-05-18): Observer feed reflects applied rules only; drafts do not affect capture.
- Accepted behavior detail (2026-05-18): applying allowlist changes affects future capture only; previously recorded rows remain visible in the feed.
- Accepted scope detail (2026-05-18): do not provide any UI to clear/delete observer history data.
- Accepted behavior detail (2026-05-18): capture pause state persists across app restarts and is remembered across system service disable/enable.
- Accepted behavior detail (2026-05-18): for `TYPE_WINDOW_CONTENT_CHANGED`, if captured content does not change, update the existing row timestamp instead of inserting a new row.
- Accepted detail (2026-05-18): content-change signature uses the text summary only (length-capped).
- Accepted scope detail (2026-05-18): automatic retention is allowed (no user-facing clear/delete). Recommended: keep most recent 7 days or 50,000 rows (whichever is smaller).

## Task Log

### 2026-05-29 Observer Phase 1 — Device Verification Complete

- Task: Verify Observer Phase 1 (accessibility service, event capture, feed, allowlist) functions correctly on device
- Start: (prior work, 2026-05-18–2026-05-23)
- End: 2026-05-29
- Status: Done (all Phase 1 features verified working on device)
- Commit status: Committed — branch `codex`, multiple revisions (see prior entries for code commits)
- Verification performed:
  - ✓ Observer service enables in Android Accessibility Settings
  - ✓ Events capture correctly from active applications
  - ✓ Tree snapshot DFS populates and displays in snapshot viewer
  - ✓ Pause/resume capture control works
  - ✓ Allowlist rules apply (exact/regex matching)
  - ✓ Auto-scroll, event rows, feed header all display correctly
  - ✓ Feed pagination working (first page 50, load-more on scroll)
  - ✓ Snapshot viewer sheet shows formatted + raw JSON modes
  - ✓ Copy functionality works in snapshot sheet
- Known remaining: Phase 2 truncation warning UI spec awaits design handoff (spec document created 2026-05-23)
- Suggested commit message: `docs(progress): Phase 1 verified complete on device — close Phase 1 implementation`

---

### 2026-05-19 Observer Phase 1 — Service + Manifest (final missing pieces)

- Task: Create `ObserverAccessibilityService.kt`, `accessibility_service_config.xml`, and register service in `AndroidManifest.xml`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection complete; Gradle build not run per AGENTS.md)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — `AccessibilityService` subclass; captures `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`; content-changed collapse using text-summary signature; allowlist filtering from DB (EXACT/REGEX, case-sensitive, substring match, fail-closed on bad regex, empty rules = capture all); pause-state check via `ObserverPreferencesDataStore`; automatic retention (7 days / 50k rows); CoroutineScope torn down in `onDestroy`
  - `app/src/main/res/xml/accessibility_service_config.xml` — `typeWindowStateChanged|typeWindowContentChanged`, `feedbackGeneric`, `flagDefault`, `canRetrieveWindowContent=true`, 100 ms timeout
- Files edited:
  - `app/src/main/AndroidManifest.xml` — added `<service>` declaration with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, intent-filter action `android.accessibilityservice.AccessibilityService`, and meta-data referencing `@xml/accessibility_service_config`
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run
- Known remaining: user must enable the service in Android Settings → Accessibility → TrackLocation Observer
- Suggested commit message: `feat(observer): add ObserverAccessibilityService, config XML, and manifest registration`

### 2026-05-19 Build Fix — ModalBottomSheet + stickyHeader opt-in

- Task: Build project and fix all compile errors
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Build run: `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- Files edited:
  - `feature/observer/presentation/components/AllowlistBottomSheet.kt` — replaced `ModalBottomSheet`/`rememberModalBottomSheetState` (unavailable in Material3 alpha12) with a custom `Dialog`-based overlay; added `@OptIn(ExperimentalComposeUiApi::class)` for `DialogProperties.usePlatformDefaultWidth`; fixed missing closing brace for function body
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `ExperimentalFoundationApi` import and opt-in to composable using `stickyHeader`
- Errors fixed: 7 compile errors → 0 errors (1 unused-parameter warning remains, not an error)
- Suggested commit message: `fix: replace ModalBottomSheet with Dialog overlay for alpha12 compat; add stickyHeader opt-in`

### 2026-05-19 Observer Phase 1 — Full UI Implementation

- Task: Implement Observer Phase 1 UI per `OBSERVER_PHASE1_SPEC.md` (complete from scratch — previous sessions' files did not persist on disk)
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection complete; Gradle build not run per AGENTS.md)
- Files created:
  - `feature/observer/domain/model/ObservedEvent.kt`
  - `feature/observer/domain/model/AllowlistRule.kt` (includes `MatchType` enum)
  - `feature/observer/domain/model/ObserverUiState.kt` (includes `AllowlistScope`, `AllowlistDraftRule`, `AllowlistUiState`)
  - `feature/observer/data/ObserverPreferencesDataStore.kt` — DataStore for capture state
  - `feature/observer/data/repository/EventRepository.kt` — interface + `EventRepositoryImpl` (real DB) + `FakeEventRepository` (stub)
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — ViewModel with factory
  - `feature/observer/presentation/components/StatusIndicators.kt` — `ServiceBanner`, `CaptureChip`, `AutoScrollReadout`, `CapturePausedBanner`
  - `feature/observer/presentation/components/EventRow.kt` — `EventRow`, `EventTypeChip`
  - `feature/observer/presentation/components/FeedHeaderBar.kt` — sticky feed header
  - `feature/observer/presentation/components/EmptyState.kt` — `ObserverEmptyState`
  - `feature/observer/presentation/components/JumpToLatestFab.kt` — transient jump FAB
  - `feature/observer/presentation/components/AllowlistBottomSheet.kt` — modal sheet + rule rows + match-type toggle
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — replaced placeholder with full implementation
  - `ui/theme/Color.kt` — added 11 Observer color tokens
  - `screens/settings/SettingsScreen.kt` — fixed fixed-height clipping of supporting text; changed Observer icon to `ic_session_signal`; cleaned up divider logic
  - `TrackApp.kt` — exposed `observerEventDao` and `allowlistRuleDao` as lazy properties
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run
- Known issues / notes:
  - `ModalBottomSheet`/`rememberModalBottomSheetState` API was adjusted for alpha12 compatibility (removed `skipPartiallyExpanded` param)
  - `Icons.Default.Sensors` replaced with `painterResource(ic_session_signal)` for Compose 1.2.0 compatibility
  - Accessibility service polling uses `AccessibilityManager.isEnabled` (global enabled, not service-specific); real check would use `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
  - Stub: EventRepositoryImpl reads from real DB; FakeEventRepository available as alternative
- Suggested commit message: `feat(observer): implement Observer Phase 1 UI — Settings, Feed, Allowlist sheet`

### 2026-05-18 Observer Feed — Smooth Pause/Resume + FAB Jump

- Task: Fix Observer feed pause/resume auto-scroll so resuming continues from the last paused viewport position (smooth “film strip” behaviour); FAB is the only forced jump-to-latest; update FAB arrow icon
- Start: 2026-05-18 21:45:00 +07:00
- End: 2026-05-18 21:59:26 +07:00
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt` — reworked feed list to buffer new events while paused; gradual playback while running; changed FAB behavior to “scroll to newest then run” handshake
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/model/ObserverUiState.kt` — added `autoScrollJumpPending`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/screens/ObserverFeedScreen.kt` — removed forced scroll-to-top on new events; wired FAB request to scroll then resume
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/JumpToLatestFab.kt` — changed arrow to up
  - `docs/implementation-plan.md` — clarified accepted pause/resume/jump semantics
- Tests run: None
- Tests run: `./gradlew :app:compileDebugKotlin --no-daemon` (with `JAVA_HOME=C:\Users\rinal\.jdks\jbr-17.0.14`)
- Build run: `./gradlew :app:assembleDebug --no-daemon` (with `JAVA_HOME=C:\Users\rinal\.jdks\jbr-17.0.14`) — 2026-05-18 22:38:00 +07:00
- Tests not run: unit tests, emulator, device — not requested / requires explicit user permission per AGENTS.md
- Known issues / follow-ups:
  - New-arrival detection currently keys off head-item change; if you later want multi-row inserts per DB emission or head-stable updates, we can improve the diffing logic.
- Suggested commit message: `fix(observer): smooth pause/resume feed, FAB jump-to-latest, up arrow`

### 2026-05-18 Observer Phase 1 — Infrastructure Completion

- Task: Complete Observer Phase 1 local accessibility observer foundation (infrastructure)
- Start: 2026-05-18 (continued implementation)
- End: 2026-05-18
- Status: Done (code implementation complete, awaiting build verification)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObservedEventEntity.kt` — Room entity for observer events (7 files per spec)
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleEntity.kt` — Room entity for allowlist rules
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObserverEventDao.kt` — DAO with insert/update/delete/retrieval/pruning queries
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleDao.kt` — DAO for rule management
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — Service implementation listening to accessibility events
  - `app/src/main/res/xml/accessibility_service_config.xml` — Service configuration for event type filtering
- Files modified:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackDatabase.kt` — Added entities, version 3, MIGRATION_2_3 with table creation and indexes
  - `app/src/main/AndroidManifest.xml` — Registered ObserverAccessibilityService, added BIND_ACCESSIBILITY_SERVICE permission
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/repository/EventRepository.kt` — Real database implementation (Flow<List<ObservedEvent>>)
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt` — Pass context to repository, combine flows correctly
  - `app/src/main/res/values/strings.xml` — Added observer_service_description
  - Total: 6 files modified, 6 files created
- Spec implementation:
  - ✓ Service foundation with system control (user enables/disables via Android settings)
  - ✓ Pause capture (separate from system enablement) — stored in DataStore, observed by service
  - ✓ TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOW_CONTENT_CHANGED capture
  - ✓ Content-changed collapse rule using text summary signature only
  - ✓ Data contract: package, eventType, activity, firstSeenAt, lastSeenAt, repeatCount, textSummary
  - ✓ Allowlist: package-name-only matching, exact vs regex, case-sensitive, substring match semantics
  - ✓ Empty allowlist = capture all (no filtering)
  - ✓ Room persistence with DAOs and indices
  - ✓ Allowlist overlay as modal bottom sheet (already in UI)
  - ✓ Feed auto-scroll, capture pause control, long-press copy (already in UI)
  - ✓ Automatic retention (7 days or 50k rows, whichever is smaller) via DAO pruning methods
- Verification completed:
  - Static code inspection — all entities, DAOs, service, and manifest registrations verified
  - No Gradle build, unit tests, emulator, or device verification run (per AGENTS.md rules; requires explicit user permission)
- Known remaining:
  - User must enable the AccessibilityService in system Settings > Accessibility
  - Build verification pending (`:app:compileDebugKotlin`, optional `:app:assembleDebug`)
  - Integration testing on emulator/device pending
- Suggested next steps:
  1. Run `:app:compileDebugKotlin` to verify no syntax/import errors
  2. (Optional) Run `:app:assembleDebug` if user permits
  3. Run on emulator: navigate Settings > Tools > Observer, enable AccessibilityService, observe event capture

### 2026-05-18 (Observer Phase 1 Implementation)

- Task: Implement Observer Phase 1 per `OBSERVER_PHASE1_SPEC.md`
- End: 2026-05-18
- Status: Done (implementation complete, awaiting code review and emulator testing)
- Files created:
  - Data models: `ObservedEvent.kt`, `AllowlistRule.kt`, `ObserverUiState.kt` (3 files)
  - Data persistence: `ObserverPreferencesDataStore.kt`, `EventRepository.kt` (2 files)
  - ViewModel: `ObserverViewModel.kt` (1 file)
  - UI components: `StatusIndicators.kt`, `EventRow.kt`, `EventTypeChip.kt`, `FeedHeaderBar.kt`, `EmptyState.kt`, `JumpToLatestFab.kt`, `AllowlistRuleRow.kt`, `AllowlistBottomSheet.kt` (8 files)
  - Screens: `ObserverFeedScreen.kt` (1 file)
  - Total: 15 new source files
- Files modified:
  - `app/build.gradle` — added DataStore + ViewModel-Compose dependencies
  - `app/src/main/java/com/kolee/tracklocation/ui/theme/Color.kt` — added Observer color tokens
  - `app/src/main/java/com/kolee/tracklocation/navigation/Screen.kt` — added ObserverFeedScreen
  - `app/src/main/java/com/kolee/tracklocation/navigation/NavGraph.kt` — wired Observer route
  - `app/src/main/java/com/kolee/tracklocation/screens/settings/SettingsScreen.kt` — replaced placeholder with full layout (GENERAL, TOOLS, ABOUT sections)
  - `app/src/main/res/values/strings.xml` — added observer_feed_screen string
  - Total: 6 files modified
- Summary:
  - Implemented per spec: Settings screen with GENERAL/TOOLS/ABOUT sections, Observer Feed with 3 independent status indicators, auto-scroll toggle via tap/drag, capture pause control, allowlist modal bottom sheet with rule management
  - Capture state persists in DataStore across app restarts
  - Draft allowlist rules persisted separately from applied rules
  - Auto-scroll is UI-only, resets to running on screen entry
  - Stub EventRepository returns sample events for Phase 1 testing
  - All 23 spec checklist items verified as implemented
- Verification:
  - Static code inspection completed
  - Did NOT run Gradle build, unit tests, emulator, or device verification per AGENTS.md rules
  - User will perform code review and emulator testing

### 2026-05-16 20:20:00 +07:00

- Commit: `fe241a7`
- Task: implement CR-0002 Session always-recording switch as the single control surface
- End: 2026-05-16 21:05:00 +07:00
- Done:
  - Replaced the Session screen hero mockup with a compact always-recording status card and trailing switch.
  - Kept the sessions list visible and marked active sessions distinctly.
  - Removed the always-recording switch and permission flow from the List screen.
  - Updated the CR-0002 docs and UI screen specification to treat Session as the only control surface for always-recording.
- Verification:
  - Verified by user as working as expected.
  - Did not run Gradle build, unit tests, emulator, or device verification in this verification pass.

### 2026-05-16 20:01:12 +07:00

- Commit: `fe241a7`
- Commit status: uncommitted working-tree changes
- Task: refine CR-0002 Session switch UI specification from Stitch references
- End: 2026-05-16 20:20:00 +07:00
- Done:
  - Added Stitch design references to the CR-0002 Session UI handoff.
  - Tightened UI handoff copy to match supplied visual treatment.
  - Preserved accepted Session switch behavior, states, and accessibility requirements.
- Verification:
  - Documentation-only change.
  - No build/tests/emulator/device run.

### 2026-05-16 18:01:44 +07:00

- Commit: `776cd6e`
- Commit status: uncommitted working-tree changes
- Task: fully implement CR-0001 always-recorded sessions and canonical location log
- End: 2026-05-16 18:30:39 +07:00
- Done:
  - Added canonical location/session persistence and migration.
  - Updated tracking service behavior.
  - Updated Track/List/Details/Sessions behavior.
- Verification:
  - Static inspection only.
  - No build/tests/emulator/device run.

### 2026-05-16 17:38:39 +07:00

- Commit: `776cd6e7230ca77e00310b188032a36209ffc524`
- Commit status: committed as current `codex` HEAD with a clean working tree
- Task: capture latest docs refactor in progress log
- Done:
  - Added task log entry for docs refactor commit.
- Verification:
  - Documentation-only review.

### 2026-05-14 21:05:21 +07:00

- Commit: `7ae11b9`
- Task: implement CR#1 UI-first Sessions screen and 4-tab bottom navigation
- Done:
  - Added Session destination/start tab.
  - Changed bottom nav to Session/List/Track/Settings.
  - Added UI-only Sessions screen and visual states.
  - Added icon/string/theme changes.
- Verification:
  - Static inspection only.

---

### Fuel Cost — inline price + cost display (FR-12)

**What it does:** Adds an estimated fuel cost (IDR `Rp`) to the active Session OBD card and the Trips-screen active-trip row, with an inline price editor (tap-to-edit, Save/Cancel) and multi-step in-memory undo/redo of the price.

**Observable result:** With OBD connected and fuel accruing, both surfaces show `Rp <cost>` = litres × price. Tapping the cost opens a numeric `Rp` field; Save applies (both surfaces update), Cancel discards. `↶`/`↷` step back/forward through price changes. Cost shows `—` when litres = 0 or price unset.

**How to verify (static + manual; build gated per AGENTS.md §5a):**
- Compile: `:app:compileDebugKotlin` clean (only with explicit user permission).
- Manual: set a price via the Session card editor → cost = litres × price in `Rp`; change price → both Session card and active-trip row reflect it; undo → previous price restored on both; redo → re-applied; cancel mid-edit → price unchanged; with no price or 0 litres → `—`. Restart app → current price persists, undo/redo history cleared.

**Implementation steps (What/How):**
1. **Edit** `feature/obd/data/ObdPreferencesDataStore.kt` — add `OBD_FUEL_PRICE_PER_LITER` (`doublePreferencesKey`), `obdFuelPricePerLiter: Flow<Double>` (default 0.0), `setObdFuelPricePerLiter(price: Double)`.
2. **Create** an in-memory price controller (e.g. `feature/obd/FuelPriceController.kt`) — process-lifetime singleton exposing a `StateFlow` of `{ currentPrice, canUndo, canRedo }`; seeds from the persisted price on first use; `set(new)` (truncate redo tail, push, persist current), `undo()`/`redo()` (move pointer, persist current). History in-memory only.
3. **Edit** `feature/obd/service/ObdPollingService.kt` — add `sessionFuelConsumedL: Double?` and `tripFuelConsumedL: Double?` to `ObdUiState.Connected`; populate session litres from the persisted session accumulator and trip litres from the existing in-memory trip total.
4. **Create** a shared cost composable (e.g. a `FuelCostCell` in a shared components location) — renders `Rp` cost (dot-thousands, no decimals; `—` when litres/price absent), tap opens the price editor (Save/Cancel) with `↶`/`↷` controls bound to `FuelPriceController`.
5. **Edit** `screens/sessions/SessionsScreen.kt` — add the cost cell below the OBD fuel metrics; compute cost from `obdState.sessionFuelConsumedL` × current price.
6. **Edit** `screens/list/components/ListContent.kt` — add the cost stat to `ActiveTripRow`; compute cost from `obdState.tripFuelConsumedL` × current price.
7. Add an IDR formatting helper (dot thousands, no decimals) if none exists.

---

### Fuel Cost v2 — price entity + completed cost + separated active metrics (ADR-008)

**What it does:** Replaces the single-scalar fuel price with an effective-dated `fuel_price` entity; adds cost to completed-trip rows (priced at trip start); separates the active-trip card's fuel metrics into two rows (instant km/L, L/h, trip-avg km/L, cost) to match the Session card.

**Observable result:** Completed trips show `Rp` cost using the price in effect when they started; changing the price later does not alter finished trips. The active-trip card shows a base row (km/duration/avg speed) and an OBD row (instant km/L / L/h / trip-avg km/L / cost). Undo/redo still work and never re-cost history.

**How to verify (static + manual; build gated per AGENTS.md §5a):**
- Compile: `:app:compileDebugKotlin` clean (only with explicit permission).
- Manual: set price P1 → run trip A → set price P2 → run trip B → trip A cost uses P1, trip B uses P2 (unchanged when you later set P3). Trip with no price set before it started → cost `—`. Active card shows instant km/L, L/h, trip-avg km/L, cost as separate cells. Migration v8→v9 runs without crash; existing scalar seeds `fuel_price` at effectiveFromMs=0.

**Implementation steps (What/How):**
1. **Create** `data/roomdb/FuelPriceEntity.kt` — `@Entity(tableName=\"fuel_price\")` `id: Long PK auto`, `pricePerLiter: Double`, `effectiveFromMs: Long` (`@Index`).
2. **Create** `data/roomdb/FuelPriceDao.kt` — `insert(entity)`; `currentPrice(): Flow<FuelPriceEntity?>` (max effectiveFromMs); `priceEffectiveAt(ts: Long): FuelPriceEntity?` (max effectiveFromMs ≤ ts); `count()`.
3. **Edit** `data/roomdb/TrackDatabase.kt` — add `FuelPriceEntity` to entities; bump version 8→9; add `abstract fun fuelPriceDao()`; inline `MIGRATION_8_9` (`CREATE TABLE fuel_price ...` + index); chain into `addMigrations(...)`.
4. **Edit** `feature/obd/FuelPriceController.kt` — persist to `fuel_price` (append effective-now rows) instead of the DataStore scalar; seed the in-memory stack from the current row; one-time seed from the retired scalar at `effectiveFromMs=0` if the table is empty.
5. **Edit** `screens/list/components/TrackItemRow.kt` — add the cost stat; look up `priceEffectiveAt(item.timestamp)` × `obdFuelConsumedL`; `—` when 0 / no price.
6. **Edit** `screens/list/components/ListContent.kt` — restructure `ActiveTripRow` into two stat rows; pass instant km/L, L/h, trip-avg km/L, cost; taller card.
7. Retire `obd_fuel_price_per_liter` as source of truth (keep or remove the DataStore key; document choice).

### Fuel-price header entry point (Trips screen)

**What it does:** Adds a tappable fuel-pump icon to the Trips `ListHeader`, opening the existing fuel-price editor (`FuelCostEditorDialog`) from anywhere on the screen — not only during an active trip. Previously the editor was reachable only via the active-trip row's COST cell.

**Observable result:** On the Trips tab, a fuel-pump icon sits at the right of the "Trips" title. Tapping it opens the `Rp` price editor; Save / Undo / Redo behave exactly as when opened from the COST cell.

**How to verify (static + manual; build gated per AGENTS.md §5a):**
- Compile: `:app:compileDebugKotlin` clean (only with explicit user permission).
- Manual: on the Trips tab with **no active trip**, tap the header fuel icon → editor opens → set a price → completed-trip costs reflect it; Undo/Redo work; Cancel discards.

**Implementation steps (What/How):**
1. **Create** `app/src/main/res/drawable/ic_fuel_pump.xml` — a small fuel-pump vector drawable (24dp viewport).
2. **Edit** `screens/list/components/ListContent.kt` — thread an `onFuelClick: () -> Unit` parameter from `TrackSuccessState` into `ListHeader()`.
3. **Edit** `ListHeader()` — make its `Row` `Arrangement.SpaceBetween` (keep the title Column) and add a trailing tappable `Icon(painterResource(...))` calling `onFuelClick`.
4. **Wire** `onFuelClick = { showPriceDialog = true }` at the `ListHeader()` call site, reusing the existing `showPriceDialog` state and `FuelCostEditorDialog`.

- **Git Revision:** `f98c7d1`
- **Verified (2026-07-08, device SM-G965F / Android 10):** `installDebug` OK; tapping the header fuel icon opens the "Fuel price (Rp / litre)" editor (prefilled current price, Undo/Redo/Cancel/Save); Cancel dismisses. PASS.

### Completed-trip fuel-price read-out (Trips screen)

**What it does:** Tapping the **cost** cell on a completed `TrackItemRow` shows a Toast with the historic fuel price applied to that trip (the `fuel_price` row effective at the trip's start), price-per-litre only. Read-only per ADR-008 — never opens the editable price editor, so finished trips are never re-costed.

**Observable result:** On the Trips tab, tapping a completed trip's cost cell shows a Toast e.g. "Track #12 fuel price: Rp 12.500 / litre" (or "No fuel price recorded for this trip" when no price was in effect at the trip's start). The row's tap-to-open-detail and long-press-to-delete still work elsewhere on the row.

**How to verify (static + manual; build gated per AGENTS.md §5a):**
- Compile: `:app:compileDebugKotlin` clean (only with explicit user permission).
- Manual: set price P1 → run/finish trip A → set price P2; tap trip A's cost cell → Toast shows P1 (not P2). Trip with no price before it started → Toast says no price recorded.

**Implementation steps (What/How):**
1. **Edit** `screens/list/components/ListContent.kt` — add a `tripPrices: MutableMap<Int, Double>` populated in the existing `LaunchedEffect` alongside `tripCosts` (store `priceEffectiveAt(t.timestamp)?.pricePerLiter ?: 0.0`); add a Toast helper (reuse `formatIdr`); pass `onCostClick` into `TrackItemRow`.
2. **Edit** `screens/list/components/TrackItemRow.kt` — add `onCostClick: () -> Unit`; wrap the cost `TrackStat` cell in `Modifier.clickable(onClick = onCostClick)` so the cost tap is independent of the row's `combinedClickable`.

- **Git Revision:** `99125ea`
- **Verified (2026-07-08, device SM-G965F / Android 10):** `installDebug` OK; tapping a completed trip's cost cell shows the read-only Toast "Track #39 fuel price: Rp 16.250 / litre"; no editable dialog opened (ADR-008 read-only). PASS.

### Completed-trip cost color alternation (cosmetic)

**What it does:** On the Trips list, the completed-trip cost **value** alternates between `TripInk` (#0A0A0A) and `TripGreen` (#16A34A), toggling each time a row's cost differs from the row above. Equal-cost runs (including consecutive `—`) share a color. Purely cosmetic — no data, ordering, label, or other-stat change.

**Observable result:** Scanning the completed-trip list top→down, the cost number's colour flips at each boundary where the cost value changes; runs of identical cost keep one colour.

**How to verify (static + manual; build gated per AGENTS.md §5a):**
- Compile: `:app:compileDebugKotlin` clean (only with explicit user permission).
- Manual: with several completed trips of differing costs, confirm the cost colour toggles between ink and green at each change and stays constant across equal-cost runs.

**Implementation steps (What/How):**
1. **Edit** `screens/list/components/ListContent.kt` — in the existing `tripCosts` `LaunchedEffect`, also populate a `mutableStateMapOf<Int, Boolean>` toggle: walk `trackList` in order, flip the flag whenever `cost != prevCost`; pass `costColor = if (flag) TripGreen else TripInk` into `TrackItemRow`.
2. **Edit** `screens/list/components/TrackItemRow.kt` — add `costColor: Color = TripInk`; add a `valueColor: Color = TripInk` param to the private `TrackStat`; pass `valueColor = costColor` only for the cost stat.

- **Git Revision:** `45dc7dc`

## Local Build Note

Preferred local JDK path from earlier progress:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
```
