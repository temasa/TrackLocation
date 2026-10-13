# ADR-030: Offline Completion for All Cancelled Orders (not cash-only)

**Status:** Accepted
**Date:** 2026-10-13
**Decided By:** Project owner + Claude Code

**Amends:** ADR-025 decisions 3 (cash-order detection) and 4 (stop rules, cancel rows), and ADR-015 cancel rule (Amendment 2026-10-10). Everything else in ADR-025 and ADR-015 is unchanged. See ADR-025 "Amended 2026-10-13 by ADR-030".

## Context

ADR-025 limited the cancel-keeps-recording behaviour to cash orders (payment `Tunai`). A cancelled non-cash order was still stopped immediately, which could end the trip before the driver reached the drop-off, where the job might still be finished offline.

The payment type does not describe whether the driver is still driving to the drop-off. The cancel signal is the same for every order, so the offline rule should not depend on payment. The product owner decided to apply the same rule to every order.

## Decision

1. **Cancel never auto-stops.** When a Gojek order reaches `CANCELLED`, the order-owned trip keeps recording for every order, regardless of payment type. The trip is never auto-stopped on cancel.

2. **Drop-off snackbar for all cancelled orders.** When the driver first comes within 150 m of the resolved drop-off coordinates after a cancel, the Track screen shows one non-blocking snackbar (ADR-025 decision 5 wording and placement, unchanged). It appears at most once per trip, and only when the drop coordinates are resolved.

3. **Stop rules (all orders).**

   | Trigger | Auto-stop? | Status saved | `orderOffline` |
   |---|---|---|---|
   | Normal finish (Selesai / Drop off → `FINISHED`) | Yes (ADR-015) | `COMPLETED` | `false` |
   | Home screen "Cleared" (`FINISHED`) | Yes (ADR-015) | `COMPLETED` | `false` |
   | Cancel (`CANCELLED`), any payment type | No, the trip keeps recording; snackbar when within 150 m of resolved drop coordinates | see the stop row below | see the stop row below |
   | Cancel (`CANCELLED`), drop coordinates unresolved | No, the trip keeps recording; no snackbar | `CANCELLED` at the driver's eventual stop | `false` |
   | Stop of a cancelled trip (snackbar **Stop** or the normal Stop CTA), ended within 150 m of the drop-off | Driver stop | `COMPLETED` | `true` |
   | Stop of a cancelled trip, ended farther than 150 m from the drop-off | Driver stop | `CANCELLED` | `false` |

   "Within 150 m" is measured as in ADR-025 decision 4 (great-circle distance from the last known fix at the moment of the stop, against the cached drop coordinates).

4. **Payment is not used for this decision.** The `Tunai` payment match and the `cashPaymentLabel` constant are removed from code. ADR-025 decision 3 (cash-order detection) no longer applies. The stored payment value is not changed by this ADR; it is only no longer read for the completion decision.

5. **Unchanged:** the chip variants (`Completed`, `Completed · Offline`, `Cancelled`), FINISHED-after-CANCELLED ignored (ADR-025 Consequences, open risk resolved 2026-10-10), always-recording stays ON (BR-13), the 2-hour staleness window does not end a trip, and route clearing on `CANCELLED` (ADR-016). No schema change and no Room migration: `orderStatus` and `orderOffline` already exist (DB v14).

## Consequences

### Positive
- One rule for every order. The driver's offline completion is recorded the same way whatever the payment type.
- A cancelled order whose driver reaches the drop-off is saved as `Completed · Offline` whether it was paid in cash or not.
- The payment match string is one less Gojek detection string to keep in sync with Gojek's text (ADR-015 risk (e)).
- No schema change, no migration, no observer change.

### Negative / accepted risks
- **Cancelled order, driver never reaches the drop-off:** the trip keeps running until the driver stops it manually. The app does not stop it on cancel. It is saved as `CANCELLED` (`orderOffline = false`) at that stop. This is accepted by product decision. The trip may record extra driving time or distance until the driver notices, but the saved outcome is correct.
- **Snackbar only while the app is on screen** (ADR-025 risk, unchanged): if the driver is in another app when they reach the drop-off, nothing prompts them.
- **Drop-coordinate dependency** (ADR-025 risk, now for all orders): if geocoding failed, a cancelled trip is never offered the snackbar and is saved as `CANCELLED` at stop.
- **Historical rows are not rewritten:** non-cash cancelled trips saved before this ADR were stopped immediately and stay as they were saved.

## Alternatives Considered

1. **Keep the cash-only rule (ADR-025 as written):** rejected by product decision. Payment type does not tell whether the driver is still heading to the drop-off.
2. **Auto-stop non-cash cancels and keep only cash cancels recording:** rejected by product decision. This is the current ADR-025 rule, and it leaves a non-cash cancelled order able to end before the drop-off.
3. **Auto-stop all cancels after a timeout or a distance threshold:** not chosen. It would add a new timing rule that the owner did not ask for, and the driver can still stop manually.

## Related ADRs

- [[015-order-auto-start-trip]] — amended by this ADR (cancel rule).
- [[025-order-trip-status-and-offline-completion]] — amended by this ADR (decisions 3 and 4); the status model, chip, and snackbar wording are unchanged.
- [[013-observer-trip-extraction]] — `observer_trip` phase values (`CANCELLED`).
- [[016-order-route-overlay]] — route clearing on terminal state, unchanged.
- [[022-order-trip-label-in-trips-list]] / [[023-order-trip-header-card-and-geo-snapshot]] — order-linked trip rows where the chip is shown.

## References

- PRD.md — FR-16 (cancel rule and chip, amended 2026-10-13).
- ARCHITECTURE.md — `track` columns (`orderOffline`), Gojek Order-Card Takeover Flow (trip end).
- UI-SPEC.md — §3e (drop-off snackbar, all cancelled orders), Track screen card states.
- IMPLEMENTATION-PLAN.md — §4 "Order Trip Status + Offline Completion (ADR-025)" (device verification step, now for all orders), §6 task log.
- Code (pending implementation): `GojekRules` / `OrderCardRules` (remove `cashPaymentLabel` and the `Tunai` match), `ShareViewModel` (cancel branch, snackbar, stop rules).
