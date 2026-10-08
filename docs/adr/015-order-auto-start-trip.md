# ADR-015: Gojek Order Lifecycle Drives the Trip (auto-start at Taken, auto-end at Cleared/Cancelled)

**Status:** Accepted
**Date:** 2026-10-06
**Decided By:** Project owner + Claude Code

**Amended 2026-10-08:** vocabulary table and wording aligned with code - the trip ends when the order row reaches FINISHED, including at the Selesai screen.

## Context

ADR-014 established automatic stopping of an active trip when a Gojek order card first becomes complete (pickup + drop + payment + earnings). However, this decision creates a semantic gap: the trip auto-stops at order acceptance, but does not auto-start when an order is taken. The driver's actual workflow is: order offered → order taken (driver driving to customer) → pickup → carrying → drop off → finished. The trip should capture the entire engagement lifecycle, starting when the driver commits to the order (Taken phase).

Furthermore, the order card stays visible only until "Selesai" (finished). If the driver cancels the order after accepting it, or dismisses the card manually, TrackLocation leaves the trip running without any visual feedback—creating an inconsistency with the original intent (trip reflects order engagement).

This ADR amends ADR-014 to: (1) auto-start a trip when the order becomes Taken (the card is fully read: pickup, drop, payment, earnings), (2) auto-end the trip when the order is Cleared (Gojek home screen after "Selesai"), Cancelled by the customer, or Dismissed by the user.

---

## Decision

1. **Auto-start the trip at Taken:** When the Gojek parser (ADR-013) first detects a complete order card (pickup, drop, payment, earnings), run the "order ready" signal once per order. If no trip is currently active, `ShareViewModel` sends `START_TRIP`. If a trip is already active (e.g., the driver manually started a trip before viewing the order), the existing trip is kept and tied to the order by setting an in-memory flag `orderOwnsTrip=true`. This preserves manual trip starts while automating the common case. The always-recording session remains ON and unaffected.

2. **Auto-end the trip at Selesai, Cleared, Cancelled, or Dismissed (whichever FINISHED signal arrives first):** The Gojek parser also recognizes terminal states that produce FINISHED:
   - **(a) Selesai screen:** The "Selesai" screen with earnings/summary; represents the normal completion of an order.
   - **(b) Gojek home screen (Cleared):** All four bottom-nav texts present (`Beranda`, `Pendapatan`, `Swadaya`, `Pesan`); co-occurrence with an order button is impossible (verified: 0 overlaps across 1,001 snapshots).
   - **(c) Cancel message (Cancelled):** Text "Oke, sip" together with a message containing "nge-cancel" (customer cancellation).
   
   All produce a `FINISHED` order row in `observer_trip`. `OrderTripRecorder` marks the latest open row `FINISHED` (existing idempotent logic). `ShareViewModel` observes `latestOrderFlow`; when a `FINISHED` row appears and the trip is order-owned (`orderOwnsTrip=true`) and active, it runs the persist-then-stop sequence (`stopActiveTrip`: persist the trip via `ShareViewModel.insertTrack`, then `STOP_TRIP`). Dismiss (user taps the Dismiss button on the provisional order card) also calls `dismiss(id)`, triggering the same stop logic.

3. **Trip is not ended by the 2-hour card-staleness window:** The trip runs until `FINISHED` or manual stop—the 2-hour staleness is for card deduplication only.

4. **Always-recording is untouched (BR-13):** Always-recording remains ON before, during, and after the trip lifecycle. Navigation trips stay manual-end (ADR-009).

5. **In-memory flag survives activity recreation, not process death:** The `orderOwnsTrip` flag is held in `ShareViewModel`'s in-memory state. If the process is killed and relaunched, the flag is lost. On relaunch, the latest order row is queried; if it is still open and `FINISHED` has not arrived, the trip is not auto-ended (manual stop required). This trade-off simplifies the solution and defers cross-process persistence to future phases.

---

## Consequences

### Positive
- Trip lifecycle now aligns with the driver's actual Gojek order engagement: starts at Taken (commitment), ends at Selesai/Cleared/Cancelled/Dismissed (disengagement).
- The driver has automatic trip bookending without losing manual control: a pre-started trip is preserved; post-start trips are bound to the order.
- Terminal-state detection is deterministic: home-nav texts and cancel-message text are stable anchors, not dependent on timing or screen-read order.
- The trip always ends if the order ends, preventing orphaned trips.
- Scope remains Gojek-only and device-only; no remote linking or sync complexity.

### Negative / accepted risks
- **(a) Drive to customer is included in the trip:** The trip starts at Taken (driving to pickup), not at Pickup (passenger on board). This means the trip includes the drive-to phase, which was not previously recorded. This is intentional (order acceptance marks engagement), but it means trip distance/duration include customer-pickup deadheading. **Accepted:** This is the desired product behavior per ADR-014 and FR-16.

- **(b) App relaunch during an active order can split the trip into two:** If the app is relaunched (e.g., `"Membuka aplikasi driver…"` relaunch screen), the foreground-launch may bring a home screen or intermediate state into view briefly. The order parser may skip a snapshot (home screen briefly shown, then the order card re-appears). If a `FINISHED` row is incorrectly written and then cleared, and a new order with the same pickup/drop appears, two trips may be created instead of one. **Risk accepted:** The re-start/re-ordering is rare; if it occurs, the driver can manually merge trips via the UI or accept two separate trip records. The 2-hour same-address deduplication window mitigates accidental re-matching to a stale old order.

- **(c) Background-activity-start restrictions on Android 10+ (unchanged from ADR-014):** The foreground launch from the accessibility service is verified on the test device (SM-G965F, Android 10) and may require a fallback (full-screen-intent notification or SYSTEM_ALERT_WINDOW overlay). If blocked, the trip still auto-starts via `ShareViewModel` when the activity next becomes active. If `ShareViewModel` is destroyed before `FINISHED` arrives, the trip is not auto-ended (manual stop required).

- **(d) Trip not auto-ended after process death:** If the process is killed after the trip is started but before `FINISHED` is detected, the trip remains open on relaunch. The driver must manually stop it. **Accepted:** Out-of-process persistence of the `orderOwnsTrip` flag is deferred; simpler in-memory state is sufficient for the current phase.

- **(e) Vietnamese/Indonesian UI strings may change:** The terminal-state detection relies on exact text matches ("nge-cancel", "Oke, sip", nav text). Gojek updates may change these strings (versioning, localization). **Accepted:** The parser can be updated when Gojek changes; the fallback is manual trip end.

---

## Alternatives Considered

1. **Auto-end only; keep manual start:** End the trip when the order is Cleared/Cancelled, but require manual start. **Rejected:** Leaves an asymmetry and increases friction; auto-start is the common case and aligns with the order lifecycle.

2. **Trip ends at "Selesai" screen only, not Cleared/Cancelled:** Detect only the "Selesai" button press and skip Cleared/Cancelled signals. **Rejected:** Selesai is kept as the normal end signal, but is not sufficient alone: cancelled orders never reach "Selesai", so cancel and home signals are also necessary to end the trip without leaving orphaned trips.

3. **Out-of-process flag (persist `orderOwnsTrip` to DB):** Survive process death and auto-end the trip on relaunch. **Rejected (deferred):** Adds a new column to a trip or session row, introduces consistency risk (what if the order row is pruned before the trip is ended?), and complicates the schema. In-memory is simpler for now.

---

## Vocabulary Table (Gojek Order States)

| Gojek State | Screen Evidence | Code Enum | Trip Behavior |
|---|---|---|---|
| Offer | (never seen; auto-accept on; parser ignores) | — | — |
| Taken | Card with pickup, drop, payment, earnings; button "Udah di titik jemput" | `OrderPhase.PICKUP` | **AUTO-START** (if no trip active) or keep existing trip |
| Pickup | Driver taps "Udah di titik jemput" button; transition only | — | (no screen change) |
| Carrying | Card without pickup; button "Sampai tujuan"; customer on board | `OrderPhase.DROP` | Trip continues |
| Drop off | Driver taps "Sampai tujuan"; toll dialog then "Trip selesai" summary + "Selesai" button | — | (transition sequence) |
| Finished | "Selesai" screen with earnings/summary; then gone | `OrderPhase.FINISHED` | **AUTO-END** (if orderOwnsTrip=true): the Selesai screen writes FINISHED, which stops the trip; this is the normal end path |
| Cleared | Gojek home screen: 4 nav texts present (`Beranda`, `Pendapatan`, `Swadaya`, `Pesan`) | `OrderPhase.FINISHED` (re-written) | Backstop AUTO-END (if orderOwnsTrip=true): normally a no-op because the Selesai screen already ended the trip and cleared ownership; ends the trip only if Selesai was never observed |
| Cancelled | Cancel message + "Oke, sip" button; text contains "nge-cancel" | `OrderPhase.FINISHED` (written once) | **AUTO-END** (if `orderOwnsTrip=true`) |

*Implementation note: the end trigger is the row reaching FINISHED (ShareViewModel end collector). The Selesai screen, the cancel message and the home screen all produce FINISHED, so whichever is observed first ends the trip.*

---

## Evidence from Device Observation

**Device:** Gojek Observer log (read-only pull, 2026-10-06)

- **Normal end:** "Sampai tujuan" button pressed → toll dialog → "Trip selesai" screen → "Selesai" button → home screen appears ~1 second later.
- **Cancelled order:** 08:57 event — cancel message shown with "Oke, sip" button → home screen immediately after; both stored orders remained in `PICKUP` phase (never reached "Selesai").
- **Home-nav co-occurrence:** Across 1,001 Gojek snapshots, the 4 nav texts (`Beranda`, `Pendapatan`, `Swadaya`, `Pesan`) never appeared in the same snapshot as an order button, confirming the home screen and order card are mutually exclusive.

---

## Related ADRs

- **ADR-014** (Gojek Order Card Takeover) — amended by this ADR; auto-stop behavior replaced.
- **ADR-013** (Observer Trip Extraction) — the upstream parser that feeds the complete-card signal and terminal-state detection.
- **ADR-009** (Dual-mode Track Navigation) — manual-end rule for navigation trips (unchanged).

---

## References

- PRD.md — FR-13 (manual trip end, Gojek exception), FR-16 (amended)
- ARCHITECTURE.md — Gojek Order-Card Takeover Flow (updated)
- ADR-014 — Gojek Order Card Takeover of the Track Screen
- ADR-013 — Gojek Trip Extraction (pickup/drop, device-only)
- ADR-009 — Manual-end rule for navigation trips

---

## Implementation Notes (code pending approval)

**Phase 1 — Auto-start signal**
- In `OrderCardParser` / `GojekRules`, after the first detection of a complete card (pickup + drop + payment + earnings), set an in-memory `firstCompleteFlag` per (pickupAddress, dropAddress) key.
- Emit a one-shot "order ready" signal (unchanged from ADR-014 Step 7).

**Phase 2 — Auto-start trip**
- `ShareViewModel` observes the unhandled ready row.
- If no trip is currently active (`!isTracking`), send `START_TRIP`.
- If a trip is active, set `orderOwnsTrip=true` (in-memory; survives activity recreation, not process death).
- Mark row handled.

**Phase 3 — Terminal-state detection**
- Extend `GojekRules` with two new patterns:
  - Home screen: presence of all 4 nav texts (`Beranda`, `Pendapatan`, `Swadaya`, `Pesan`) in the snapshot.
  - Cancel message: presence of "Oke, sip" and a text node containing "nge-cancel".
- When either pattern matches, yield an `OrderCard` with `phase = FINISHED`.

**Phase 4 — Auto-end trip**
- `OrderTripRecorder` marks the latest open row `FINISHED` (existing logic, unchanged).
- `ShareViewModel` observes `latestOrderFlow` (new flow from the DAO; latest order row by lastSeenAt).
- When `FINISHED` appears and `orderOwnsTrip=true` and a trip is live, run `stopActiveTrip()` (persist-then-STOP_TRIP).
- User-initiated Dismiss calls `dismiss(id)` on the order card, which also triggers the stop logic.
- Clear `orderOwnsTrip=false` after stopping.

**Verification (detailed in task log)**
- Deterministic (unit tests not required): parser recognizes home screen and cancel-message text.
- Manual on-device with auto-accept on:
  1. Take an order → card appears, trip timer starts immediately (START_TRIP).
  2. Finish with "Selesai" → trip stops and appears in Trips list (FINISHED → AUTO-END).
  3. Cancel order → trip stops when Gojek shows cancel message + home screen (FINISHED → AUTO-END).
  4. Dismiss card manually → trip stops (dismiss() → AUTO-END).
  5. Manually start trip before order appears → trip is kept and ends with the order (orderOwnsTrip flag).
  6. No "Selesai" screen needed; detection works without tapping it.

---

**Amends:** ADR-014 (auto-stop behavior replaced by auto-start + auto-end), PRD FR-13/FR-16 (Gojek exception clarified).

---

## Addendum (2026-10-06): Track screen shows the order card with a compact trip strip (3-cell base)

**Context:** With auto-start, the trip begins running immediately when the order becomes Taken. The order card alone hid the running trip (its elapsed time, distance, and efficiency), making the trip invisible to the driver while it was live. Decision: display a compact trip strip below the order card, inside the same glass panel, showing: TIME (elapsed in compact format: 1h15m / 42s / 3m15s), DIST (km, one decimal), AVG/INST km/L (average then instant, each one decimal). The strip is shown only while the trip is live (tripState LIVE or PAUSED); it disappears when the order ends and `TripPanel` returns.

**Change:** The order card now carries the trip strip as a sub-component (fed from the existing `TrackPanelState` — elapsedMs, distanceKm, instantKmL, tripAvgKmL already available). No new calculation, no schema change, no new data model. Provisional UI reuses `TripPanel`'s glass-panel style; final design comes from a design handoff (AGENTS.md §12). This addendum does not change the auto-start/auto-end decision itself — only clarifies the visual composition of the Track screen during active orders.

**Traceability:** Updated PRD FR-16, UI-SPEC §3e (trip strip spec + handoff instructions), ARCHITECTURE Gojek Order-Card Takeover Flow (trip strip display + data feed), IMPLEMENTATION-PLAN §4 ADR-015 slice (step 5 "Order card trip strip" + "How to Verify" extension).

## Addendum (2026-10-06): Trip strip gains COST / NET cell (4-cell extension)

**Context:** The compact trip strip now displays a 4th cell **COST / NET** between DIST and AVG/INST km/L. COST = estimated fuel cost so far = trip fuel (from OBD accumulator) × effective fuel price (FuelPriceController / fuel_price table, FR-12/ADR-008). NET = net profit so far = OrderCard.earningsRp − COST (may be negative, shown as −Rp…). Estimate caveat: fuel price and litres are estimates (real consumption varies by engine/conditions).

**Change:** Extends the trip strip to 4 cells with layout weights TIME 0.7, DIST 0.7, COST/NET 1.4, AVG/INST 1.4 (no clip on narrow screens). Compact Rupiah format: <1,000 → 'Rp850'; 1,000–999,999 → 'Rp8.4k' (one decimal); ≥1,000,000 → 'Rp1.2jt'. Negative prefixed with '−'. TalkBack content description reads full amounts (like the existing fuelDesc). Missing data: COST shows '—' when OBD disconnected, no litres burned yet, or no fuel price set. NET shows '—' whenever COST is '—' or the card has no earnings (never estimated from earnings alone). Provisional UI (TripPanel-derived style tokens); final design from handoff per AGENTS.md §12. No schema change; no new ADR (UI addition on existing data — fuel cost calculation already exists per ADR-008, trip fuel accumulation per Phase 2, OrderCard earnings read from observer_trip).

**Traceability:** Updated PRD v0.13→0.14 (FR-16 extended with COST/NET definition + FR-12 cross-link), ARCHITECTURE v0.15→0.16 (Gojek flow trip-strip data feed noted), UI-SPEC v0.14→0.15 (§3e trip strip spec updated to 4-cell + compact-Rupiah rules + a11y + missing-data; Handoff Instructions with screenshot checklist + 5-state Claude Design prompt), IMPLEMENTATION-PLAN v0.18→0.19 (§1 change-log 0.19, §4 ADR-015 slice step 6 COST/NET + How to Verify extension, §6 task-log rows Docs+Code), DOCUMENT-CONTROL register + change-log.
