# ADR-031: Early Finish Keeps the Trip Recording (Gojek FINISHED stops only near the drop-off)

**Status:** Accepted
**Date:** 2026-10-14
**Decided By:** Project owner + Claude Code

**Amends:** ADR-030 (offline completion for cancelled orders; the FINISHED rows of the stop table are no longer unconditional), ADR-015 (order lifecycle drives the trip: the FINISHED auto-end rule, decision 2), and ADR-025 (order trip status and offline completion; the stop rules for FINISHED). Everything else in ADR-030, ADR-015 and ADR-025 is unchanged. See the amendment notes in those ADRs.

**Amendment 2026-10-14 (ADR-032):** a kept-open early-finished trip keeps the order card on the Track screen (instead of `TripPanel`) until the trip stops. The stop rules in this ADR are unchanged. See ADR-032.

## Context

ADR-015 auto-ends an order-owned trip when Gojek reports the order as `FINISHED` (Selesai screen, or Gojek home screen "Cleared"). ADR-030 kept cancelled orders recording until the driver stops them, but still stopped every `FINISHED` trip immediately.

On 2026-10-14 this caused a real loss. Trip #52 (Zea house to Soekarno-Hatta T2) was auto-stopped after 34 minutes and 4.5 km, because the driver closed the order early in Gojek (Selesai, then the Gojek home screen) while still about 60 km from the drop-off. The always-on location log shows the actual ride continued for about 1 h 45 min outside any trip.

The stop decision cannot rely on the order state alone. Gojek's FINISHED signal is also emitted when the driver closes the app flow early, so the trip's position at that moment matters. The drop coordinates are needed for that check, but they are only cached in memory (`cachedGeo`). In the owner's data on 2026-10-14, 13 of 27 order trips had no saved drop coordinates.

## Decision

1. **A Gojek-originated FINISHED no longer always stops an order-owned running trip.** The signals are the Selesai button and the Gojek home screen "Cleared" state. The rule applies to **all** orders, regardless of payment type. The stop decision is made from the driver's position.

2. **Rule table.** Position is the last real GPS fix. Radius is 150 m (`DROP_OFF_RADIUS_M`), great-circle distance to the cached drop coordinates.

   | Situation | Trip behaviour | Snackbar | Status saved | `orderOffline` |
   |---|---|---|---|---|
   | FINISHED, drop coordinates known, within 150 m | Auto-stop (ADR-015) | None | `COMPLETED` | `false` |
   | FINISHED, drop coordinates known, farther than 150 m | Keeps recording | Shown once, the first time the driver comes within 150 m (see below) | Not saved yet | Not saved yet |
   | FINISHED, drop coordinates unknown (not cached) | Keeps recording | None | Not saved yet | Not saved yet |
   | Driver taps **Stop trip** on the order card, or the Stop CTA, or snackbar **Stop** | Always stops, regardless of distance | n/a | Per the manual-stop row below | Per the manual-stop row below |

   The snackbar text, when shown, is: "Order finished in Gojek. You're at the drop-off. Stop trip?" with action "Stop". It uses the same component, placement and duration as the ADR-025 cancel snackbar. It is on-screen only and never auto-stops the trip.

3. **Manual stop of a kept-open trip.** The status is saved as `COMPLETED`. `orderOffline` is `true` if the driver is within 150 m of the drop-off at the moment of the stop, otherwise `false`. If the drop coordinates are unknown, `orderOffline` is `false`.

4. **A new order does not adopt or stop a kept-open trip.** If a new order is accepted (Taken) while an early-finished trip is still recording, the new order is ignored for trip purposes. No new trip is started and the existing trip is not adopted. The existing trip keeps recording until the driver stops it. The order card and overlay for the new order still show.

5. **Cancel behaviour (ADR-030) is unchanged.** A `FINISHED` signal that arrives after `CANCELLED` is still ignored.

6. **No schema change and no Room migration.** `orderStatus` and `orderOffline` already exist.

## Consequences

### Positive
- The driver's actual ride is no longer cut off when Gojek is closed early. The trip keeps recording until the driver is at the drop-off or stops it.
- One rule for every order, whatever the payment type.
- No schema change, no migration, no observer change.

### Negative / accepted risks
- **Drop coordinates unknown (about half of orders in the owner's data on 2026-10-14):** a normal Selesai at the destination no longer auto-stops the trip. The driver must tap Stop trip. This is accepted by product decision.
- **Forgotten kept-open trip:** it keeps accumulating distance and time until the driver stops it. This is the same accepted risk as ADR-030 cancels.
- **Snackbar only while the app is on screen** (same as ADR-025): if the driver is in another app when they reach the drop-off, nothing prompts them.
- **Historical rows are not rewritten:** trips already auto-stopped on FINISHED stay as they were saved.

## Alternatives Considered

1. **Keep the current rule (stop on every FINISHED):** rejected. It cuts the trip off when Gojek is closed early, as in trip #52.
2. **Geocode the stored drop address on demand when the coordinates are unknown:** deferred, not rejected outright. It would reduce the "unknown drop" case. It can be added later without changing the rest of this decision.
3. **Let the next accepted order adopt the open trip:** rejected. It mixes two rides in one trip.
4. **Idle-timeout auto-stop (stop after a period without movement):** not chosen. It adds a new timing rule that the owner did not ask for.

## Related ADRs

- [[015-order-auto-start-trip]] — amended by this ADR (FINISHED auto-end rule, decision 2).
- [[025-order-trip-status-and-offline-completion]] — amended by this ADR (FINISHED stop rules); the status model, chip, and snackbar wording are unchanged.
- [[030-offline-completion-for-all-orders]] — amended by this ADR (FINISHED rows of the stop table); cancel rules unchanged.

## References

- ARCHITECTURE.md — `track` columns (`orderOffline`), Gojek Order-Card Takeover Flow (trip end).
- UI-SPEC.md — §3e (drop-off snackbar), Track screen card states (Stop trip label).
- PRD.md — FR-16 (order trip end rules).
- Code (pending implementation): `ShareViewModel` (FINISHED branch, snackbar, stop rules, new-order handling), `GojekRules` / `OrderCardRules` (FINISHED signal handling), `cachedGeo` (drop coordinates).
