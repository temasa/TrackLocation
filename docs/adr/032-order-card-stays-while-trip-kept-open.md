# ADR-032: Order Card Stays on the Track Screen While a Kept-open Order Trip Is Recording

**Status:** Accepted
**Date:** 2026-10-14
**Decided By:** Project owner + Claude Code

**Amends:** ADR-025 (resolves its open question on the card state during a cancelled order trip), ADR-030 and ADR-031 (Track screen card behaviour only). Stop rules, status values, the snackbar, and the route overlay are unchanged. See the amendment notes in those ADRs.

## Context

ADR-030 and ADR-031 keep an order-owned trip recording after the order has ended in Gojek. This happens when a cancelled order's trip is kept open (ADR-030), or when a `FINISHED` signal arrives farther than 150 m from the drop-off or with unknown drop coordinates (ADR-031).

The Track screen, however, switched back to the normal `TripPanel` as soon as the order ended. `UI-SPEC.md` §3e and ADR-025 recorded this as the proposed default and an open question: whether the order card should be replaced by `TripPanel` in the cancelled state. In the kept-open case the driver is still on the trip, so the screen dropped the order details, phase chip and the Stop trip control the driver needs, and showed a generic panel instead.

## Decision

1. **The order card stays on the Track screen while a kept-open order trip is recording.** This applies to both kept-open cases: a cancelled order (ADR-030), and an early finish (`FINISHED`, ADR-031) that did not auto-stop.

2. **What the card shows.** The card is the same card the driver saw while the order was active. It shows the last known pickup, drop, payment and earnings, and the phase chip reads `Done` (for an early finish) or `Cancelled` (for a cancelled order). It keeps the **Stop trip** control and the trip strip. No new visual state is added.

3. **When the card is shown.** The card stays until the trip stops, or until the pending state clears. Stopping happens through **Stop trip** on the card, the snackbar **Stop** action, or the automatic stop rules of ADR-031 (within 150 m on `FINISHED`). After the trip stops, or the pending state clears, the normal `TripPanel` / READY state returns.

4. **Card-only data source.** The card is driven by a card-only display order: the active order, or, while the trip is recording, the order row whose id is the kept-open pending order (`cancelledPendingId` or `finishedPendingId`). This display order is used only for the card. Route overlay, planned and optimized lines and the legend still follow the **active** order only, and are cleared when the order ends (ADR-016, unchanged).

5. **Stop rules unchanged.** ADR-025, ADR-030 and ADR-031 decide when a trip stops, what status it is saved with, and `orderOffline`. This ADR changes what the Track screen shows, not when the trip stops.

6. **No schema change, no Room migration, no parser change.** The change is in `ShareViewModel` (the new display-order flow) and in the Track screen card branch, which uses it.

## Consequences

### Positive
- The driver keeps the order details and the Stop trip control for as long as the trip is recording.
- The open question in ADR-025 and `UI-SPEC.md` §3e is closed, with no new visual design.
- No schema change, no migration, no observer change, and no change to the stop rules.

### Negative / accepted risks
- **Ended order still looks active:** the card can show an order that Gojek has already closed, until the driver stops the trip. The chip (`Done` or `Cancelled`) and the absence of an active order in Gojek make this clear, but the card is the same one used for live orders.
- **Route overlay is not shown:** the card remains, but the route overlay and planned lines are cleared when the order ends (ADR-016). The driver sees the card without the route.
- **Card depends on the pending id being kept:** if the pending id is cleared early, the card falls back to `TripPanel` even though the trip is still recording. The stop rules are not affected.

## Alternatives Considered

1. **Keep the `TripPanel` fallback in the cancelled and early-finish states:** rejected. It removes the order details and the order-linked Stop trip context while the trip is still recording.
2. **New compact "ended order" card for the kept-open state:** rejected. It would add a new visual state. The owner asked for no new visuals; the existing card with a `Done` or `Cancelled` chip covers the case.
3. **Show the card only for cancelled orders, not for early finishes:** rejected. The early-finish case has the same problem (the driver is still on the trip), and one rule for both is simpler.

## Related ADRs

- [[025-order-trip-status-and-offline-completion]] — amended by this ADR (open question on the card during the cancelled state is resolved).
- [[030-offline-completion-for-all-orders]] — amended by this ADR (card behaviour while a cancelled trip is kept open); stop rules unchanged.
- [[031-early-finish-keeps-trip-recording]] — amended by this ADR (card behaviour while an early-finished trip is kept open); stop rules unchanged.
- [[014-gojek-order-card-takeover]] — the order card and its takeover of the Track screen that this ADR extends.
- [[016-order-route-overlay]] — route overlay and planned lines follow the active order only, unchanged.

## References

- UI-SPEC.md — §3b-order (order card and status chip), §3e (drop-off snackbar, card state during a kept-open trip).
- ARCHITECTURE.md — Gojek Order-Card Takeover Flow (trip end), card state.
- PRD.md — FR-16 (order trip end rules).
- IMPLEMENTATION-PLAN.md — §6 task log (ADR-032 slice).
- Code (pending implementation): `ShareViewModel` (card-only `displayOrder` flow from `activeOrder` and `cancelledPendingId` / `finishedPendingId`), Track screen card branch (`TrackScreen`).
