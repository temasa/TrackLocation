# ADR-015: Gojek Order Lifecycle Drives the Trip (auto-start at Taken, auto-end at Cleared/Cancelled)

**Status:** Accepted
**Date:** 2026-10-06
**Decided By:** Project owner + Claude Code

## Context

ADR-014 established automatic stopping of an active trip when a Gojek order card first becomes complete (pickup + drop + payment + earnings). However, this decision creates a semantic gap: the trip auto-stops at order acceptance, but does not auto-start when an order is taken. The driver's actual workflow is: order offered → order taken (driver driving to customer) → pickup → carrying → drop off → finished. The trip should capture the entire engagement lifecycle, starting when the driver commits to the order (Taken phase).

Furthermore, the order card stays visible only until "Selesai" (finished). If the driver cancels the order after accepting it, or dismisses the card manually, TrackLocation leaves the trip running without any visual feedback—creating an inconsistency with the original intent (trip reflects order engagement).

This ADR amends ADR-014 to: (1) auto-start a trip when the order becomes Taken (the card is fully read: pickup, drop, payment, earnings), (2) auto-end the trip when the order is Cleared (Gojek home screen after "Selesai"), Cancelled by the customer, or Dismissed by the user.

---

## Decision

1. **Auto-start the trip at Taken:** When the Gojek parser (ADR-013) first detects a complete order card (pickup, drop, payment, earnings), run the "order ready" signal once per order. If no trip is currently active, `ShareViewModel` sends `START_TRIP`. If a trip is already active (e.g., the driver manually started a trip before viewing the order), the existing trip is kept and tied to the order by setting an in-memory flag `orderOwnsTrip=true`. This preserves manual trip starts while automating the common case. The always-recording session remains ON and unaffected.

2. **Auto-end the trip at Cleared, Cancelled, or Dismissed:** The Gojek parser also recognizes two terminal states:
   - **(a) Gojek home screen (Cleared):** All four bottom-nav texts present (`Beranda`, `Pendapatan`, `Swadaya`, `Pesan`); co-occurrence with an order button is impossible (verified: 0 overlaps across 1,001 snapshots).
   - **(b) Cancel message (Cancelled):** Text "Oke, sip" together with a message containing "nge-cancel" (customer cancellation).
   
   Both produce a `FINISHED` order row in `observer_trip`. `OrderTripRecorder` marks the latest open row `FINISHED` (existing idempotent logic). `ShareViewModel` observes `latestOrderFlow`; when a `FINISHED` row appears and the trip is order-owned (`orderOwnsTrip=true`) and active, it runs the persist-then-stop sequence (`stopActiveTrip`: persist the trip via `ShareViewModel.insertTrack`, then `STOP_TRIP`). Dismiss (user taps the Dismiss button on the provisional order card) also calls `dismiss(id)`, triggering the same stop logic.

3. **Trip is not ended by the 2-hour card-staleness window:** The trip runs until `FINISHED` or manual stop—the 2-hour staleness is for card deduplication only.

4. **Always-recording is untouched (BR-13):** Always-recording remains ON before, during, and after the trip lifecycle. Navigation trips stay manual-end (ADR-009).

5. **In-memory flag survives activity recreation, not process death:** The `orderOwnsTrip` flag is held in `ShareViewModel`'s in-memory state. If the process is killed and relaunched, the flag is lost. On relaunch, the latest order row is queried; if it is still open and `FINISHED` has not arrived, the trip is not auto-ended (manual stop required). This trade-off simplifies the solution and defers cross-process persistence to future phases.

---

## Consequences

### Positive
- Trip lifecycle now aligns with the driver's actual Gojek order engagement: starts at Taken (commitment), ends at Cleared/Cancelled/Dismissed (disengagement).
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

2. **Trip ends at "Selesai" screen only, not Cleared/Cancelled:** Continue the existing ADR-014 behavior and detect only the explicit "Selesai" button press. **Rejected:** Does not handle cancellation (trip would run indefinitely or until staleness timeout); cancelled orders never reach "Selesai", leaving orphaned trips.

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
| Finished | "Selesai" screen with earnings/summary; then gone | `OrderPhase.FINISHED` | (no auto-action yet; waits for Cleared) |
| Cleared | Gojek home screen: 4 nav texts present (`Beranda`, `Pendapatan`, `Swadaya`, `Pesan`) | `OrderPhase.FINISHED` (re-written) | **AUTO-END** (if `orderOwnsTrip=true`) |
| Cancelled | Cancel message + "Oke, sip" button; text contains "nge-cancel" | `OrderPhase.FINISHED` (written once) | **AUTO-END** (if `orderOwnsTrip=true`) |

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
