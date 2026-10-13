# ADR-029: Session Overlay Banner (System Overlay Above Other Apps)

**Status:** Accepted (owner-approved 2026-10-13); Fill-up action added 2026-10-13 (Decision 9); strip layout, circle, permission prompt and translucent surface amended 2026-10-13 (Decisions 3, 5, 6, 10); implementation decisions recorded 2026-10-13 (Decisions 1, 4 and 9 amended, Decision 11 added); circle shows fuel-left litres with a level-band ring, not a percentage (Decision 5 amended, owner-visible deviation, 2026-10-13); code in progress (IMPLEMENTATION-PLAN §4, slices 1 to 9); device verification recorded in part on 2026-10-13 (see "Device verification (2026-10-13)"; open items updated)
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

   *Amendment 2026-10-13 (implementation decision, owner-approved):* the overlay is hosted by a separate `OverlayService`, not inside `TrackingService`. It is started only while TrackLocation is in the foreground (Android 12 and later block starting a foreground service from the background), when a session is active and "Display over other apps" is granted. It adds `SYSTEM_ALERT_WINDOW` to the manifest, its own notification channel, and explicit `lifecycle-process` and `lifecycle-service` dependencies at 2.5.1. `targetSdk` stays at 33, where no foreground service type is enforced for this service; a later bump to `targetSdk` 34 needs a type (`specialUse`). This is a future note, not part of this change. `Settings.canDrawOverlays` is re-checked on every show, because the user can revoke the permission at any time.

2. **Visibility.** The overlay is shown only while both of these are true:
   - a recording session is active, and
   - TrackLocation is **not** in the foreground.

   Foreground state is detected with `ProcessLifecycleOwner`. No `UsageStats` permission is used. When the session ends, the overlay is removed.

3. **Strip content and layout.** The strip is one row of five cells, in this order, with no units in the labels:
   - **FUEL LEFT** (ADR-027, session-independent OBD ledger).
   - **RANGE** (ADR-027, rolling-distance RANGE).
   - **FUEL CONS** (the session average km/L and the instant km/L, ADR-007, ADR-024).
   - **COST** (the session total cost, Decision 4).
   - **INCOME** (the session gross and net, Decision 4).

   The row also holds an icon-only **Fill up** button (Decision 9) and the shrink button. The row is 382 px wide, so the overlay has 4 px side gutters on a 390 px screen. The exact layout, sizes and value formats are owned by UI-SPEC §5d and the pen frames in `ui-design.pen`, section "Session Overlay Banner (ADR-029)". This ADR does not repeat them.

4. **Session totals over all orders.** The totals accumulate over all orders in the current session:
   - **Cost** = sum over orders of (trip litres x pump price), using the same rule as FR-16 in `GojekOrderCard`.
   - **Gross** = sum of `earningsRp`.
   - **Net** = gross - cost.
   - An order that lacks OBD litres or earnings is excluded from the totals, and the total is flagged **partial** on the strip.
   - A negative net is shown as a loss.

   *Amendment 2026-10-13 (implementation decisions, owner-approved):*
   - **Cancelled orders** (`track.orderStatus == CANCELLED`) are excluded from cost, gross and net, since they have no earnings. Completed orders, and orders in progress that have data, count per FR-16.
   - **Session-to-order link:** the `track` table has no session id, so the current session's orders are selected by time window: `track.timestamp >= active session startedAt AND orderLabel IS NOT NULL`, through a new read-only DAO query. No schema change and no Room migration. Known limitation: a trip that started before an older open session was adopted can be misattributed.

5. **Shrink and circle.** The strip has a shrink button. Tapping it collapses the strip to a **48 px** draggable circle. A drag moves the circle. A tap (without a drag) restores the strip. The circle has no Fill-up action. The circle position is persisted in DataStore and clamped to the screen bounds, so it stays reachable after rotation or a screen-size change.

   *Amendment 2026-10-13 (owner-visible deviation, implementation):* the collapsed circle shows the **fuel-left litres** (for example `12.4`, with a fuel icon), not a percentage. The ring is coloured by the fuel level band from `FuelBalance.level` (Normal green, Low amber below 6 L, Critical red below 3 L; the same bands as the UI-SPEC §5b Fuel Dock). A percentage needs a tank-capacity setting, which the code does not have and which is a product decision not yet made. The pen circle frames (`Overlay State 4`, `Overlay/Circle`) still draw `62%` and must be updated, or a tank-capacity setting specified, before they are treated as final (UI-SPEC §5d).

6. **One-time permission prompt.** The app asks once for the overlay permission (from Settings or on the first session). The prompt is a one-time **centred dialog** titled "Show session totals over other apps", with the buttons **Not now** and **Open settings**. It is not a card on the Session screen. Open settings deep-links to `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. If the permission is not granted, the overlay does not show. Nothing else changes: tracking, recording and the in-app UI keep working.

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
   - **Amendment 2026-10-13 (implementation decision):** the rule is taken from the FillUpDialog code. Litres: digits and one `.`, must be greater than 0, no upper bound. Price: digits only and optional; it is saved only if it is greater than 0 and different from the latest price (`FuelPriceController.set`, ADR-008). The app has **no numeric price bound**. The fill-up write is extracted from `FuelDockViewModel.fillUp` into a plain class that both the dock and the overlay use. Behaviour is unchanged, and a fill-up before a Reset is stored but ignored for the balance, as in FR-22.
   - **Result on the strip.** On save, fuel left and range update immediately. Both are derived from the stored events and the OBD ledger / rolling window (ADR-027 Decisions 2 and 5), so the strip re-reads them after the save.
   - **Window focus.** The overlay window is normally `FLAG_NOT_FOCUSABLE`, so touches pass through to the app underneath. While the form is open, the window becomes focusable so the keyboard can receive input. It reverts to `FLAG_NOT_FOCUSABLE` when the form closes by Cancel, by Save, or when the strip is shrunk to the circle.
   - **Placement.** The form is a compact panel, positioned so the on-screen keyboard does not cover its input fields. The exact placement is confirmed on the device (see Consequences, Risks).
   - **Entry point only.** This is a second entry point for the ADR-027 Fill up. The in-app Fill up button and dialog (ADR-027 Decision 3) are unchanged, and the strip does not add a Reset or Fuel price action.
   - **Design.** The form is designed in `ui-design.pen` and specified in UI-SPEC §5d, the same as the rest of the banner (Decision 8). No code before that spec exists (AGENTS §12).
   - **Risks (device check needed):** the keyboard may overlap the form; while the form is open the panel can cover the other app's controls (the same overlap risk as the strip, Consequences); and the focus switch on the overlay window must not disturb the app underneath after the form closes.

10. **Translucent strip, halo text, no content-adaptive contrast (owner decision, 2026-10-13).** The strip surface is translucent so the app below stays visible: dark theme `ov-surface-glass` about 62% (`#0E2A209E`), light theme about 72% (`#FFFFFFB8`). The Fill up and shrink buttons stay opaque, and so does all text. Text uses **halo text**: an outer shadow (offset 0, blur 3) in `ov-halo`, for legibility over whatever is underneath.
    - **Not content-adaptive.** The strip does not sample the pixels under it. Reading them needs MediaProjection screen capture, which was considered and rejected (Alternatives Considered, 5). It adds an extra permission and a persistent recording indicator, costs battery, is blocked on secure apps, and raises privacy concerns.
    - **Consequence: contrast is marginal in some cases.** Known weak spots: a dark strip over pure white labels (about 4.5:1); a light strip over red (about 4:1); the red net value over mid-grey. These must be checked on the device (Open items).
    - The visual values are owned by UI-SPEC §5d and `ui-design.pen`.

11. **Strip figures and sources (implementation decision, owner-approved 2026-10-13).**
   - **Instant km/L** uses the same gate as the Track screen: speed above 3 km/h and GPS accuracy at or below 20 m. When the OBD state is not Connected, the instant value shows `—`.
   - **FUEL LEFT and RANGE** come from the `FuelDockViewModel` state. RANGE is held in memory, so it shows `—` after a process restart until 0.2 km is driven.

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
- **Contrast over the app below (Decision 10).** The strip is translucent and not content-adaptive, so text contrast depends on what is under it. It is marginal in some cases (Decision 10). This is a device-check risk.
- **Form covers the other app (Decision 9).** While the form is open, the overlay is focusable and the form sits over the app underneath. The form must close quickly and be easy to cancel. Focus changes must not leave the other app in a bad state.
- **Circle shows litres, not percent (Decision 5, 2026-10-13).** The implemented circle shows litres with a level-band ring. The pen circle frames still show `62%`, so the design and the code differ until the pen frames are updated or a tank-capacity setting is decided. This is an owner-visible deviation, not a silent one.
- **Strip theme follows the system (2026-10-13).** The implemented strip uses the system dark or light theme, and the translucent values of Decision 10 apply per theme.
### Implemented surface (2026-10-13)
- The **Settings** screen has a row **"Session overlay"** (Decision 6 entry point).

## Device verification (2026-10-13)

One owner-requested device run on 2026-10-13: Galaxy S9+ (SM-G965F) over the adb tunnel, with a debug APK built after commits `0ab3b13` (ERR-012) and `14523fc` (ERR-013). The assistant checked the screen through adb screenshots and dumpsys at the owner's request. Save was not pressed, so no data was written.

**Verified on the device:**
- The overlay window shows over the launcher while a session is active and TrackLocation is in the background (strip with FUEL LEFT `0.7`, other cells `—`). No crash after the ERR-012 fix.
- The Fill-up form opens under the status bar with litres, price (prefilled `16130`), Cancel, and a disabled Save.
- The numeric keyboard shows above the Save row. After the ERR-013 fix it appeared by itself on two consecutive opens (one device run).
- Validation shows a red border and "Enter litres greater than 0", and Save stays disabled.
- BACK is two-step: the first press hides the keyboard, the second closes the form.
- An outside touch closes the form, and window focus returns to the app below (touch pass-through).
- Shrink collapses to a circle at the default spot (right edge, about 25% down), with a fuel icon, the litres and a red ring (Critical, 0.7 L below 3 L).
- Dragging the circle moves it without scrolling or launching the app underneath. Tapping it expands the strip without launching the app underneath. Shrinking again returns the circle at the dragged position.

**Not verified yet:**
- Circle position after a service or app restart (DataStore).
- Drag clamp at the screen edges.
- Save: fuel left and range update, and the price row (`fuel_price`).
- Strip totals with real orders: COST, INCOME, PARTIAL and a loss.
- The permission dialog and the Settings row (the overlay permission was already granted on the device).
- Readability over a busy app (Decision 10 contrast checks).
- First-session start when the permission is granted from the dialog.
- Behaviour after the session ends (overlay disappears).

**Observed deviations:**
- FUEL CONS shows `—/—` in the no-data state; the design is a single `—` (open item below).
- The circle shows litres, not a percentage (known; Decision 5 amendment).

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
5. **Content-adaptive strip (MediaProjection screen capture, Decision 10).**
   - Pros: the strip could pick an opaque or tinted surface from the pixels under it, so contrast would be reliable.
   - Cons: an extra permission, a persistent screen-recording indicator, extra battery and CPU, secure apps block the capture, and it raises a privacy concern about what is on the driver's screen. Rejected. The strip uses a translucent surface with halo text instead (Decision 10).

## Related ADRs

- **ADR-007 / ADR-024:** instant and session-average km/L shown on the strip.
- **ADR-008:** the effective-dated fuel price. The strip's Fill-up form uses its mechanism for a changed price (Decision 9).
- **ADR-027:** fuel left and the session-independent OBD ledger; rolling-distance RANGE.
- **ADR-028:** the in-app floating status banner. It is not changed; this ADR is the separate system overlay.
- **ADR-012:** OBD accumulation gates that feed the litres used in the totals.

## References

- PRD FR-16 (order cost and earnings rule); PRD §2 and §12 (boundaries and locked rules)
- ADR-027 (fuel dock, ledger, RANGE; Decision 3 Fill-up dialog); ADR-008 (effective-dated price); ADR-028 (in-app status banner)
- UI-SPEC §5d (System Overlay Banner, including the Fill-up form and the permission prompt); `ui-design.pen` (edited through the Pencil MCP)
- Android: `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE` (toggled while the Fill-up form is open)
- `ui-design.pen` is the design source for this banner; UI-SPEC §5d is the visual spec.
- Android: `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`, `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`, `ProcessLifecycleOwner`
- IMPLEMENTATION-PLAN §4 (slice "Session Overlay Banner (ADR-029)", added 2026-10-13; code in progress)

## Open items

- [ ] Implementation is in progress (2026-10-13). The slice with its "How to Verify" block is in IMPLEMENTATION-PLAN §4; one device run on 2026-10-13 covered part of the checks (see "Device verification (2026-10-13)"); the rest are open.
- [ ] Refresh interval of the background figures (battery trade-off) is not decided here.
- [ ] Exact behaviour when a session starts while the permission is missing (prompt timing: Settings only, or also the first session) is recorded as decided by the owner in Decision 6, but the prompt wording is fixed in UI-SPEC §5d (2026-10-13).
- [ ] Notification channel name and wording for the foreground service are not decided here.
- [x] **Fill-up form design (Decision 9):** closed 2026-10-13. Designed in `ui-design.pen` (frames "Overlay State 5" and "Overlay State 6") and specified in UI-SPEC §5d.
- [ ] **Fill-up device checks (Decision 9), reduced 2026-10-13:** the keyboard above the form and the focus return after an outside touch are verified (see "Device verification (2026-10-13)"). Remaining: Save records the fill-up and the strip updates fuel left and range (plan slice 9, item 14); a changed price appends one `fuel_price` row and an unchanged price writes none (item 15); pass-through after Cancel and after shrink, checked as separate cases (item 16).
- [ ] **Contrast device check (Decision 10):** on the test device, check the three weak spots (dark strip over pure white labels; light strip over red; red net value over mid-grey) and the halo legibility in both themes. If one fails, change the design (`ui-design.pen` and UI-SPEC §5d) before changing the code.
- [x] **Price bound for the overlay form (Decision 9):** closed 2026-10-13. The app has no numeric price bound; the overlay rule mirrors FillUpDialog (Decision 9 amendment).
- [ ] **Circle shows litres, not percent (needs tank capacity decision):** the pen circle frames (`Overlay State 4`, `Overlay/Circle`) still show `62%`. Either update them to litres and level colour, or specify a tank-capacity setting (product decision) before the circle is treated as final. Decision 5 amendment, UI-SPEC §5d.
- [ ] **FUEL CONS no-data deviation (2026-10-13):** in the no-data state the strip shows `—/—` for FUEL CONS. The design (UI-SPEC §5d, and plan slice 9 item 19) is a single `—`. Decide: change the code to a single `—`, or amend the design. This must be settled before plan item 19 can pass.
