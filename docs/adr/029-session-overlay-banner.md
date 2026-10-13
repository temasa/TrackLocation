# ADR-029: Session Overlay Banner (System Overlay Above Other Apps)

**Status:** Accepted (owner-approved 2026-10-13); Fill-up action added 2026-10-13 (Decision 9); not yet implemented
**Date:** 2026-10-13
**Decided By:** Project owner + Claude Code

## Context

While a recording session is active, the driver often uses another app (navigation, messaging, the Gojek driver app). The session's live figures (fuel left, range, consumption, and the earnings and cost of the orders taken in the session) are only visible inside TrackLocation. The owner wants a compact strip that stays visible on top of other apps during a session, so the driver can see the numbers without switching back.

ADR-028 added a floating status banner, but it is an in-app overlay (`StatusBannerHost`, mounted in `MainActivity`). It disappears as soon as TrackLocation leaves the foreground. It cannot serve this purpose, so this ADR records a separate banner.

Constraints:
- No schema change and no Room migration. The banner displays data that already exists (ADR-027 fuel ledger and rolling RANGE, ADR-007 and ADR-024 consumption, FR-16 order figures). Its only write is the Fill-up action (Decision 9), which goes through the existing ADR-027 fill-up path and the ADR-008 price path.
- GPS tracking, the always-recording switch (ADR-002) and the PRD §12 locked rules stay untouched. No new bottom-navigation destination.
- Drawing over other apps needs the user's explicit permission (`SYSTEM_ALERT_WINDOW`, "Display over other apps"). The app must work without it.

## Decision

1. **Separate overlay, not `StatusBannerHost`.** The session banner is a system overlay drawn above other apps. It is added through `WindowManager` with `TYPE_APPLICATION_OVERLAY` and hosted by a foreground service. It needs the `SYSTEM_ALERT_WINDOW` permission. `StatusBannerHost` (ADR-028) stays in-app only and is not changed.

2. **Visibility.** The overlay is shown only while both of these are true:
   - a recording session is active, and
   - TrackLocation is **not** in the foreground.

   Foreground state is detected with `ProcessLifecycleOwner`. No `UsageStats` permission is used. When the session ends, the overlay is removed.

3. **Strip content.** The strip shows, in this order:
   - **Fuel left** (ADR-027, session-independent OBD ledger).
   - **Range** (ADR-027, rolling-distance RANGE).
   - **Consumption:** the session average km/L and the instant km/L (ADR-007, ADR-024).
   - **Total cost**, **gross** and **net** for the session (Decision 4).

4. **Session totals over all orders.** The totals accumulate over all orders in the current session:
   - **Cost** = sum over orders of (trip litres x pump price), using the same rule as FR-16 in `GojekOrderCard`.
   - **Gross** = sum of `earningsRp`.
   - **Net** = gross - cost.
   - An order that lacks OBD litres or earnings is excluded from the totals, and the total is flagged **partial** on the strip.
   - A negative net is shown as a loss.

5. **Shrink and circle.** The strip has a shrink button. Tapping it collapses the strip to a small draggable circle. Tapping the circle restores the strip. The circle position is persisted in DataStore and clamped to the screen bounds, so it stays reachable after rotation or a screen-size change.

6. **One-time permission prompt.** The app asks once for the overlay permission (from Settings or on the first session). The prompt has a deep link to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. If the permission is not granted, the overlay does not show. Nothing else changes: tracking, recording and the in-app UI keep working.

7. **Boundaries.** The banner is display plus one action: Fill-up (Decision 9). It has no other write path.
   - No schema change, no Room migration, no new store.
   - GPS tracking, the always-recording switch (ADR-002) and PRD §12 are untouched.
   - No new bottom-navigation destination.
   - It does not write to the canonical location log or to the sessions/trips model.

8. **Design source.** The banner is designed in `ui-design.pen` (edited through the Pencil MCP). The visual spec is UI-SPEC §5d. Per the owner directive of 2026-10-13, this replaces the external design-handoff step for this banner.

9. **Fill-up action on the strip (owner decision, 2026-10-13).** The strip has a **Fill-up** action. Tapping it opens a compact inline form inside the overlay, so the driver can record a refuel without switching back to TrackLocation.
   - **Fields:** **Litres added (L)**, required; **Price (Rp/L)**, optional, pre-filled with the latest shared price, with the same helper text as the in-app Fill up dialog. Actions: Cancel / Save.
   - **Same write path, no new store.** Save calls the ADR-027 fill-up use case (ADR-027 Decision 3). If the price was changed, the save also goes through the ADR-008 effective-dated price mechanism (a new effective-now `fuel_price` row). If the price is unchanged, the event stores `pricePerLiter = NULL` and no `fuel_price` row is written. This is exactly the in-app FillUpDialog behaviour. No new store, no schema change, no Room migration, and no new permission (the overlay already needs `SYSTEM_ALERT_WINDOW`).
   - **Validation mirrors FillUpDialog.** Litres: required, decimal, > 0; Save is disabled while invalid. Price: the same rule as the FillUpDialog price field. ADR-027 does not state a numeric bound for the price, so the implementation takes it from the FillUpDialog code.
   - **Result on the strip.** On save, fuel left and range update immediately. Both are derived from the stored events and the OBD ledger / rolling window (ADR-027 Decisions 2 and 5), so the strip re-reads them after the save.
   - **Window focus.** The overlay window is normally `FLAG_NOT_FOCUSABLE`, so touches pass through to the app underneath. While the form is open, the window becomes focusable so the keyboard can receive input. It reverts to `FLAG_NOT_FOCUSABLE` when the form closes by Cancel, by Save, or when the strip is shrunk to the circle.
   - **Placement.** The form is a compact panel, positioned so the on-screen keyboard does not cover its input fields. The exact placement is confirmed on the device (see Consequences, Risks).
   - **Entry point only.** This is a second entry point for the ADR-027 Fill up. The in-app Fill up button and dialog (ADR-027 Decision 3) are unchanged, and the strip does not add a Reset or Fuel price action.
   - **Design.** The form is designed in `ui-design.pen` and specified in UI-SPEC §5d, the same as the rest of the banner (Decision 8). No code before that spec exists (AGENTS §12).
   - **Risks (device check needed):** the keyboard may overlap the form; while the form is open the panel can cover the other app's controls (the same overlap risk as the strip, Consequences); and the focus switch on the overlay window must not disturb the app underneath after the form closes.

## Consequences

### Positive
- The driver sees fuel, range, consumption and session earnings while using other apps during a session.
- The driver can record a refuel from the overlay, without leaving the other app (Decision 9).
- Reuses existing data (ADR-027 ledger, ADR-024 averages, FR-16 cost rule). No new OBD integral and no new store.
- Works without the permission: if it is not granted, the banner stays hidden and the app behaves as before.
- The shrink and circle controls keep the screen usable while driving.

### Negative
- **Permission friction.** The driver must grant "Display over other apps", and the app must explain why.
- **Foreground service and notification.** A foreground service needs a persistent notification, and its channel and wording must be handled.
- **Battery and CPU.** The service refreshes the figures while a session runs in the background. Refresh rate must be kept low.
- **Overlap with other apps' UI.** The strip can cover controls in other apps, such as navigation or the order app. The shrink and drag controls are the mitigation.
- **Partial totals.** Orders without OBD litres or earnings are left out, so the session totals can understate the real figures. The "partial" flag makes this visible.
- **Keyboard overlap (Decision 9).** The Fill-up form needs the soft keyboard. The keyboard can cover the input fields if the panel is placed badly. This is a device-check risk.
- **Form covers the other app (Decision 9).** While the form is open, the overlay is focusable and the form sits over the app underneath. The form must close quickly and be easy to cancel. Focus changes must not leave the other app in a bad state.

## Alternatives Considered

1. **Reuse `StatusBannerHost` (ADR-028).**
   - Pros: no new service, no new permission, and the existing component is reused.
   - Cons: it is in-app only and disappears when TrackLocation leaves the foreground, which defeats the purpose. Rejected.
2. **Persistent notification.**
   - Pros: no overlay permission needed, and it is visible outside the app.
   - Cons: a notification cannot show a live multi-metric strip, cannot be dragged, and is easy to ignore. Rejected.
3. **Fill-up by opening TrackLocation and using the existing dialog (Decision 9).**
   - Pros: no focus change on the overlay and no new overlay UI.
   - Cons: it takes the driver out of the other app, which defeats the purpose of the strip. Rejected.
4. **One-tap preset amounts on the strip (Decision 9).**
   - Pros: the fastest possible refuel, with no keyboard.
   - Cons: it needs preset or "Full" semantics, which are not defined and not chosen. Rejected for this version.

## Related ADRs

- **ADR-007 / ADR-024:** instant and session-average km/L shown on the strip.
- **ADR-008:** the effective-dated fuel price. The strip's Fill-up form uses its mechanism for a changed price (Decision 9).
- **ADR-027:** fuel left and the session-independent OBD ledger; rolling-distance RANGE.
- **ADR-028:** the in-app floating status banner. It is not changed; this ADR is the separate system overlay.
- **ADR-012:** OBD accumulation gates that feed the litres used in the totals.

## References

- PRD FR-16 (order cost and earnings rule); PRD §2 and §12 (boundaries and locked rules)
- ADR-027 (fuel dock, ledger, RANGE; Decision 3 Fill-up dialog); ADR-008 (effective-dated price); ADR-028 (in-app status banner)
- UI-SPEC §5d (System Overlay Banner, including the Fill-up form once specified); `ui-design.pen` (edited through the Pencil MCP)
- Android: `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE` (toggled while the Fill-up form is open)
- `ui-design.pen` is the design source for this banner; UI-SPEC §5d is the visual spec.
- Android: `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`, `ProcessLifecycleOwner`
- IMPLEMENTATION-PLAN §4 (slice to be added when implementation starts)

## Open items

- [ ] Implementation is not started. A slice with a "How to Verify" block must be added to IMPLEMENTATION-PLAN §4 before code work begins.
- [ ] Refresh interval of the background figures (battery trade-off) is not decided here.
- [ ] Exact behaviour when a session starts while the permission is missing (prompt timing: Settings only, or also the first session) is recorded as decided by the owner in Decision 6, but the wording of the prompt is not fixed here.
- [ ] Notification channel name and wording for the foreground service are not decided here.
- [ ] **Fill-up form design (Decision 9):** the form has no spec or `ui-design.pen` frame yet. It must be added to UI-SPEC §5d and the design before code starts.
- [ ] **Fill-up device checks (Decision 9):** verify the keyboard does not cover the form on the test device; verify the overlay returns to pass-through (`FLAG_NOT_FOCUSABLE`) after Cancel, Save and shrink; verify fuel left and range update on the strip after Save; verify the other app is not disturbed by the focus change.
- [ ] **Price bound for the overlay form (Decision 9):** the price rule is taken from FillUpDialog. ADR-027 does not state a numeric bound, so confirm it against the code at implementation time.
