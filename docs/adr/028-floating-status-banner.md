# ADR-028: Floating Status Banner (OBD and Observer)

**Status:** Accepted; implemented in `8cd95f8` (2026-10-11); partly device-verified (see IMPLEMENTATION-PLAN section 4). Revised 2026-10-14 (tap-through and hints, see Revision note below).
**Date:** 2026-10-11
**Revised:** 2026-10-14
**Decided By:** Project owner + Claude Code

> **Revision 2026-10-14 (owner decision).** Tapping the body of an expanded chip no longer opens Android system settings. The OBD chip opens the in-app **OBD Settings** screen (`Screen.ObdSettingsScreen`). The Observer chip opens the in-app **Observer Settings** screen (`Screen.ObserverSettingsScreen`). Both screens are outside the banner routes, so the banner hides there, and Back returns to the previous screen. The hint lines and TalkBack wording change to match (Decisions 3, 4, 6, 7, 9). The system Bluetooth and Accessibility settings stay reachable from inside those screens: OBD Settings keeps "Pair a new device" (system Bluetooth), and Observer Settings keeps its accessibility button. The Observer Feed `ServiceBanner` "Open settings" button is unchanged. Collapse behaviour is unchanged. No in-app Bluetooth discovery or PIN entry is added (PRD §2, AGENTS §2). The Pencil frames in `ui-design.pen` still show the old hint text until they are touched up.

## Context

Two background features stop working silently when the driver is not looking at the right screen:

- **The OBD adapter.** OBD telemetry (ADR-004, ADR-012) only runs while the adapter is connected. The OBD state is shown on the Session card and the Trip panel, not on the main screens a driver sees while driving.
- **The Observer accessibility service.** Order capture (ADR-013, ADR-014, ADR-015) only works while `ObserverAccessibilityService` is enabled in system Accessibility settings.

On 2026-10-11 (ERR-010) the Observer service was silently disabled. Two Gojek orders were not captured and no trip auto-started. The existing red `ServiceBanner` on the Observer Feed (polled every second through `ObserverViewModel.isOurServiceEnabled`) would have shown the problem, but only while the Observer screen was open. The gap is the screen-closed case.

An earlier draft of ADR-028 was reverted in commit `339e366` because its premise was wrong: the Observer Feed already had a `ServiceBanner`. This ADR is the corrected scope. It adds a banner that works on the main screens and covers both the OBD adapter and the Observer service.

Constraints:
- No schema change and no Room migration.
- PRD §2 and AGENTS §2 ban in-app Bluetooth discovery and PIN entry. A link to the system Bluetooth settings (reachable from OBD Settings since 2026-10-14) does not break that rule.
- No new bottom-navigation destination. GPS tracking, the always-recording switch and the PRD §12 locked rules stay untouched.

## Decision

1. **Placement (UI).** One overlay, `StatusBannerHost`, is mounted in `MainActivity` over the nav graph. It uses the same host point as `FuelDockHost` (ADR-027), but sits at the **top** of the screen, below the status bar. It always shows when a problem exists. It has two chips: the OBD chip and the Observer chip. Layout and dimensions are in UI-SPEC §5c. No new bottom-nav destination.

2. **Routes.** The banner is shown only on the Track, Sessions, Trips (List) and Settings routes. This is the same set as the fuel dock (ADR-027, UI-SPEC §5b). It is hidden on Details, OBD Settings, Observer Feed and Observer Settings, because those screens have their own status UI or are pushed screens. The two route lists must stay in sync.

3. **OBD chip (amber).** Shown whenever `ObdPollingService.obdUiState` is not `Connected`:
   - `Connected`: chip hidden.
   - `Connecting` and `Retrying`: title "OBD connecting…".
   - `Idle` and `Waiting`: title "OBD not connected".
   - Hint line: "Tap to open OBD Settings" (same for Idle, Waiting, Connecting and Retrying).
   - The chip also shows when the OBD feature is turned off in OBD Settings. Owner decision: always show; the chip is never hidden because OBD is disabled.

4. **Observer chip (red).** Shown when the app's own accessibility service is not enabled. The check matches the package and class, as `ObserverViewModel.isOurServiceEnabled` does. The check is extracted into one shared helper and re-run every second while the app is in the foreground, at the same cadence as the existing Observer Feed `ServiceBanner`. That `ServiceBanner` stays unchanged.
   - Title: "Observer inactive".
   - Hint line: "Orders won't be captured · Tap to open Observer Settings".

5. **Both chips.** When both problems exist, the chips are stacked with the Observer chip on top. When both are healthy, nothing is shown.

6. **Tap-through.** Tapping the body of an expanded chip navigates in-app (revised 2026-10-14; previously deep-linked to system settings):
   - OBD chip: navigates to `Screen.ObdSettingsScreen` (OBD Settings).
   - Observer chip: navigates to `Screen.ObserverSettingsScreen` (Observer Settings).
   - Both screens are outside the banner routes, so the banner hides there. Back returns to the previous screen.
   - The system Bluetooth and Accessibility settings remain reachable from inside those screens (OBD Settings "Pair a new device"; Observer Settings accessibility button). The Observer Feed `ServiceBanner` "Open settings" button still opens `Settings.ACTION_ACCESSIBILITY_SETTINGS` and is unchanged.
   - In-app Bluetooth discovery and PIN entry stay banned (PRD §2, AGENTS §2).

7. **Collapse.** Every expanded chip has a minimize button on its right, after a divider. The button is 32 px visually with a 48 dp hit area.
   - Tapping the minimize button shrinks the banner to icon-only circular chips, 44 px each, at the top right. The icons are amber Bluetooth-off for OBD and red eye-off for Observer. When both are collapsed they stack vertically, Observer on top.
   - Tapping the main body of an expanded chip opens the in-app OBD Settings or Observer Settings screen (Decision 6, revised 2026-10-14).
   - Tapping a collapsed icon expands it again. It does **not** open settings, to avoid accidental taps.

8. **Collapse memory.** The collapsed or expanded choice applies to all screens. It is held in memory and lasts for the app session only. It resets on app restart, so a warning can never stay hidden forever. If a problem state changes (for example OBD goes from connected to disconnected, or the Observer service is turned off again), the affected chip re-expands even if it was collapsed.

9. **Accessibility.**
   - Do not rely on colour alone. Each chip has an icon and text.
   - TalkBack reads the title plus the action, for example "OBD not connected. Opens OBD Settings" or "Observer inactive. Orders won't be captured. Opens Observer Settings".
   - Touch targets are at least 44 px. Collapsed icons are 44 px. The minimize button is 32 px visually with a 48 dp hit area.

10. **Boundaries.** No schema change, no Room migration, no new bottom-nav destination. GPS tracking, the always-recording switch (ADR-002) and the PRD §12 locked rules are untouched. The banner is display-only and does not change the canonical location log or the sessions/trips model.

11. **Design.** The banner was designed directly in `ui-design.pen` on 2026-10-11 (the "StatusBanner Components" section and states 1 to 8). No external design handoff is needed. The visual spec is UI-SPEC §5c.

## Consequences

### Positive
- The driver sees an OBD or Observer problem on the main screens, not only on the Observer screen. The ERR-010 gap (service off, screen closed) is covered.
- One shared Observer-enabled helper replaces the duplicated check. The Observer Feed `ServiceBanner` keeps its behaviour.
- Tap-through to the in-app OBD or Observer Settings screen shortens the fix to one tap.
- No data-model change; the feature is display-only.

### Negative
- The OBD chip can be visible for a long time when the user does not use OBD. Owner decision: it is always shown while not connected, even when the OBD feature is off. The collapse control is the mitigation.
- The banner takes vertical space at the top of four screens. Layout must be checked so it does not hide the Track top controls or the status bar.
- A 1-second foreground poll of the accessibility state runs whenever the banner's routes are visible. This is the same cost the Observer Feed already pays, but it now runs on more screens.
- The collapse memory is session-only. After an app restart the banner is expanded again, even if the user had collapsed it.

## Alternatives Considered

1. **Persistent notification.**
   - Pros: visible outside the app, survives screen changes.
   - Cons: not visible in-app, easy to ignore or dismiss, and needs notification permission handling. Rejected.
2. **Per-screen inline banners.**
   - Pros: each screen controls its own layout.
   - Cons: duplicated code on every screen, and easy to miss on a screen the driver does not open. Rejected.

## Related ADRs

- **ADR-027:** same host point (`FuelDockHost`) and the same route set; the banner sits at the top, the dock at the bottom.
- **ADR-004 / ADR-012:** OBD connection state and its accumulation gates; the source of the OBD chip state.
- **ADR-013 / ADR-015:** Observer order capture and auto-start that depend on the accessibility service.
- **ADR-002:** the always-recording switch stays on the Session screen; the banner does not change it.

## References

- ERR-010 (`docs/ERRORS-LOG.md`): Observer accessibility service silently disabled; two Gojek orders not captured, no auto-start (2026-10-11).
- PRD FR-23 (new); PRD §2, §12
- UI-SPEC §5c (Floating Status Banner), §5b (Fuel Dock route set)
- ARCHITECTURE §5 (Floating Status Banner data path)
- IMPLEMENTATION-PLAN §4 slice "Floating Status Banner (ADR-028)"
- `ui-design.pen`: "StatusBanner Components" section and states 1 to 8

## Open items

- [ ] Not tested on device (IMPLEMENTATION-PLAN §4 slice "Floating Status Banner (ADR-028)", How to Verify items 4, 5, 8, 10, 16, 19, 20): the OBD Connected state hides the chip (no adapter was available); re-expansion on a real Connected to not connected transition (needs a live adapter); app restart resets the collapse to expanded; TalkBack wording of the chips and collapsed icons; a trip or session started with the banner active.
- [ ] Observed once, not reproduced: in the first pass of the device run the banner re-expanded on List, Track and Settings after being collapsed, around an OBD state flicker (connecting to not connected). A clean second pass kept it collapsed. Watch for spurious re-expansion on OBD state changes; no bug filed.
- [ ] Cosmetic (by design of a floating overlay): the chips cover the page title area, and the stacked chips overlap the top edge of the Session screen's Always-recording card.
