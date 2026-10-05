# ADR-014: Gojek Order Card Takeover of the Track Screen (auto-stop trip, order card, foreground)

**Status:** Accepted
**Date:** 2026-10-05
**Decided By:** Project owner + Claude Code

## Context

ADR-013 established device-local trip extraction for Gojek order screens: pickup/drop locations, payment, and earnings are parsed from captured accessibility snapshots and stored in `observer_trip`. This ADR addresses what happens when a Gojek order reaches its full "ready to start" state — the pickup-phase card with all four fields (pickup, drop, payment, earnings).

Currently, if a driver opens the Gojek app and views an order card while a TrackLocation trip is active, TrackLocation keeps recording that trip independently. The driver is in the Gojek app (at that moment) to begin/confirm the order. A product decision has been made to capture this engagement moment: **automatically stop the active trip, show the extracted order card on TrackLocation's Track screen (replacing the normal trip panel), and bring TrackLocation to the foreground** — all triggered once when the card first becomes complete.

This provides the driver with context (pickup/drop/payment/earnings) at the critical order-acceptance moment, without requiring manual app switching. The always-recording session remains active and unaffected.

## Decision

1. **Auto-stop the active trip:** When the Gojek parser from ADR-013 first detects a complete order card (pickup, drop, payment, earnings), stop the active trip **only if a trip is currently active**, by running the same sequence as a manual stop: `ShareViewModel.onTripCtaTap()` persists the trip (`TrackEntity`, via `insertTrack`) and then sends `STOP_TRIP`. A bare `STOP_TRIP` is not enough: `TrackingService.stopTrip()` only stops trip recording and the timer; it does not write the trip row. The always-recording session is never affected — it remains ON even after the trip stops.

2. **Replace the trip card with an order card on the Track screen:** When a complete order card is available and a trip is not active (or was just stopped), the Track screen's `TripPanel` is replaced by an order card (the order card (composable name chosen at implementation)) displaying:
   - Pickup: name + address
   - Drop: name + address
   - Payment method (GoPay, Kartu, etc.)
   - Earnings (Rp)
   - Current order phase (pickup phase / drop-only phase / finished phase)
   
   The order card displays from the pickup phase until the order reaches "Selesai" (finished), after which `TripPanel` returns.

3. **Bring TrackLocation to the foreground:** When the complete order card is first detected, use an intent/activity-launch mechanism to bring `MainActivity` (Track screen) to the foreground. This fires **once per order** (i.e., only on the first snapshot that completes the card for a given pickup+drop pair), not on every state update. The driver is inside the Gojek app at that moment; this foreground jump allows quick context-switching back to TrackLocation without hunting the app switcher.

### Defaults (state them as defaults awaiting confirmation)
- The order card displays from the pickup phase (card has all four fields) until the "Selesai" (finished) phase, then `TripPanel` returns. The card also reverts when the user dismisses it, and a later order with the same addresses is a new order when the previous one is finished or older than 2 hours.
- The foreground jump fires once per order, when the card first has all four fields.
- This feature is device-only: the foreground launch, the trip auto-stop, and the order card are all local decisions with no sync/remote link.
- Customer name, rating, and phone numbers are **never** stored or used (per ADR-013 PII rules).

## Consequences

### Positive
- Automatic trip closure at order acceptance aligns TrackLocation's trip lifecycle with the driver's actual Gojek order engagement (product-desired exception to FR-13 "no auto-arrival").
- Order context (pickup/drop/earnings/payment) is immediately visible on the device, eliminating the need to switch apps mid-engagement.
- The driver has one-shot focus: the app brings itself to the foreground exactly when the order card becomes ready, reducing friction.
- Scope is Gojek-only and device-only: the trip stop is a runtime command (not a stored relation), so no data model or sync complexity.

### Negative / accepted risks
- **(a) Auto-stop exception to FR-13:** Deliberately stopping a trip without user confirmation is a violation of "the user ends manually" (FR-13, ADR-009). However, this is an intentional, Gojek-scoped exception tied to a real business event (order acceptance); it does **not** change the rule for other apps/navigation trips. Navigation trips (ADR-009) and manual trips still require manual end. This exception is documented as FR-16 and kept to Gojek only.

- **(b) Background-activity-start restrictions (Android 10+):** Android 10+ restricts starting activities from the background; whether an accessibility service may do so on the test device (Samsung SM-G965F, Android 10) must be verified on the device. Fallback mechanisms in priority order:
  1. A high-priority full-screen-intent notification (may require a new permission/notification channel, to be decided if needed).
  2. A `SYSTEM_ALERT_WINDOW` overlay.

  The chosen fallback will be documented in ARCHITECTURE and verified on the test device, and may require a PRD/ARCHITECTURE note.
  - **Consequence for auto-stop:** the auto-stop runs in `ShareViewModel`, which exists only while the activity does. If the foreground launch is blocked and `ShareViewModel` does not exist, the trip is stopped when the activity is next created.

- **(c) Driver-distraction trade-off:** The foreground jump takes the Gojek app out of the driver's hand mid-order-acceptance. Mitigated by firing only once per order (not on every state update). The driver can immediately back-switch to Gojek or dismiss the card.

- **(d) Observer reliability:** The Observer service must not degrade GPS reliability. The Observer-to-tracking link is by signal/intent only (service isolation rule in ARCHITECTURE), not shared state. The auto-stop is one-shot per order and does not introduce polling or repeating operations.

- **(e) UI placement changed:** The earlier UI-SPEC (draft) proposed showing the order card on the Observer feed/detail. This decision moves the card to the Track screen, replacing `TripPanel`. A design handoff spec is required first (AGENTS.md §12); no visual design is defined yet.

## Alternatives Considered

1. **Show the card only on the Observer feed:** The original UI-SPEC placement. Order details are available to the driver via the Observer tab (`Settings → Tools → Observer`), but the driver must navigate there manually. **Rejected by the project owner** because it lacks the engagement moment and doesn't reduce app-switching friction.

2. **Stack the order card above TripPanel without stopping the trip:** Display both the trip and the order card on Track, keeping the trip active. **Rejected by the project owner** because it leaves the trip running while the driver is in the Gojek app, creating a semantic mismatch (the trip was never meant to cover the order-acceptance moment alone).

3. **Manual stop only:** Record the extraction and display the card, but let the user tap Stop to end the trip. **Rejected by the project owner** because it requires active user intervention at the critical moment and increases friction when the driver is context-switching to Gojek.

## Out of Scope (follow-ups)

- Extending order-card takeover to other ride/delivery apps (Grab, Indriver, etc.) — Gojek only for now; stored in PRD §8 Out-of-Scope.
- Linking the extracted order to the TrackLocation trip (no foreign key, no retroactive association) — device-local inspection only.
- Cloud sync of order card data or the auto-stop decision — device-only, never transmitted.
- Navigation trips (ADR-009): navigation end is always manual, no auto-stop exception.

## Related ADRs

- ADR-013 (Observer trip extraction) — the upstream parser and `observer_trip` store that feeds this feature.
- ADR-009 (dual-mode Track navigation, FR-13) — the manual-end rule that this Gojek-scoped exception does not change.
- ARCHITECTURE (service isolation, Observer/tracking link by signal/intent only) — the principle ensuring this feature does not degrade GPS tracking.

## References

- PRD.md — FR-13 (manual trip end, no auto-arrival), FR-16 (Gojek order-card takeover, Gojek-scoped exception)
- ARCHITECTURE.md — service isolation, Observer/tracking link by signal/intent only
- ADR-013 — Gojek trip extraction, `observer_trip` store, parser, rule table
- ADR-009 — FR-13, navigation, manual end

---

## Implementation Notes (planned; code pending approval)

**Phase 1 — Trigger & Signal**
- In the Observer service write path (after `observer_trip` upsert in ADR-013 step 4), add a one-shot "order ready" signal when a Gojek card is parsed and first has pickup+drop+payment+earnings for a given (pickupAddress, dropAddress) key.
- The signal is fired once per order; subsequent updates to phase (drop-only, finished) do not re-fire.

**Phase 2 — Auto-stop**
- The order-ready state is derived from the persisted `observer_trip` row (so it survives the activity being created after the signal), with a "handled" marker to prevent repeats.
- `ShareViewModel` observes it and, when a not-yet-handled complete order appears while a trip is active, runs the manual-stop sequence once (persist the trip via `onTripCtaTap()` logic, then `STOP_TRIP`). `TrackingService.stopTrip()` itself only stops trip recording and the timer.
- The always-recording session (`isAlwaysRecording`) is never touched — it remains active.

**Phase 3 — Foreground Launch**
- The Observer service itself starts `MainActivity` with `FLAG_ACTIVITY_NEW_TASK` and an extra that selects the Track screen.
- Whether this is allowed from the background must be verified on Android 10 (SM-G965F, adb tunnel) and may require a fallback (full-screen-intent notification, or a `SYSTEM_ALERT_WINDOW` overlay).

**Phase 4 — UI Integration**
- Track screen detects when an order card is available and displays the order card (composable name chosen at implementation) instead of `TripPanel`.
- Card displays from pickup phase through "Sampai tujuan" (drop-only), then "Selesai" (finished).
- After "Selesai", `TripPanel` returns (no persisted link to the order).
- Design handoff (AGENTS.md §12) required before code.

**Verification (to be detailed in the code task)**
- Active trip stops and is persisted (via the `ShareViewModel` manual-stop sequence) when a complete order card is detected; always-recording remains ON.
- App comes to foreground on Track with the order card displayed (device SM-G965F, Android 10, adb tunnel).
- Card updates through phases (pickup → drop-only → finished); `TripPanel` returns after "Selesai".
- No second foreground jump for the same order (one-shot per order).
- GPS recording is unaffected (no service-isolation degradation).
