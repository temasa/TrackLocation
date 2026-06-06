# Claude Desktop Prompt — OBD Phase 1 Slice 1 UI Spec

Use this prompt to ask Claude Desktop to create the UI specification document for OBD Phase 1 Slice 1.

## How to use

1. Open Claude Desktop.
2. Copy the prompt below and paste it as your message.
3. Attach the 6 screenshots listed in the **Screenshots to attach** section.
4. Review the output and paste it back into Claude Code for saving.

---

## Prompt

```
Create a UI specification / design handoff document for the OBD Phase 1 Slice 1 screen.

## Context

This is for the TrackLocation Android app (Jetpack Compose + Material 3). The app has 4 bottom-nav tabs: Session / List / Track / Settings.

OBD Phase 1 adds ELM327 Bluetooth Classic telemetry (RPM, speed, fuel rate, km/L). Slice 1 is the entry point — it introduces a new OBD Settings screen reachable via Settings → TOOLS → "OBD" row.

## What to design (Slice 1 only)

### Surface 1 — OBD row in Settings TOOLS section
- Identical `SettingsRow` pattern as the existing Observer row (no new design needed)
- Only decision: which icon to use for OBD (e.g. a car, Bluetooth, or plug icon)

### Surface 2 — ObdSettingsScreen (Idle state only)
This is the main design surface. The screen appears when the user taps the OBD row in Settings.

**Idle state elements:**
- Status card showing current OBD state: "Idle — service not enabled"
- Enable toggle: displayed but **disabled** (user must select a device first before enabling)
- Overall screen layout (top app bar with back arrow, content below)

**No other states needed for Slice 1.** Connecting, Waiting, Connected states are Slice 2 and beyond.

## Visual language to match

The app uses Material 3 with a dark theme. The existing reference patterns are:
- **Observer row in Settings TOOLS** — the `SettingsRow` the OBD row should match
- **Session screen always-recording status card** — the compact card style the OBD status card should follow
- **Settings screen TOOLS section** — the section the OBD row lives in

## Spec document to create

Create the content for `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md` with the following structure:

1. **Overview** — what this handoff covers, what Slice 1 introduces
2. **Fidelity** — reference patterns to match (SettingsRow, always-recording status card)
3. **Where this feature lives** — entry path: Settings → TOOLS → OBD → ObdSettingsScreen
4. **Screens / views**
   - OBD row in TOOLS (icon decision only)
   - ObdSettingsScreen Idle state — layout, status card anatomy, disabled toggle treatment
5. **States** — Idle only (other states are future slices)
6. **Design tokens** — reference existing app tokens; do not invent new ones
7. **Edge cases** — e.g. no device ever paired yet vs device previously paired but service off
8. **Screenshots to attach** — exact list of which screens to capture and what states to show for reference
9. **Design System section** — instructions for the design tool to extract and document the app's design system from the provided screenshots before designing the new screen. This must cover:
   - **Color palette** — background, surface, card, text primary/secondary/tertiary, switch ON/OFF, disabled state tones, icon colors
   - **Typography scale** — screen title, section header, list item title, supporting text, caption; approximate sp sizes and weights
   - **Component patterns** — SettingsRow anatomy (icon, title, supporting text, trailing chevron or switch), status card anatomy (card background, state label, supporting text, trailing toggle), section header treatment
   - **Spacing and padding conventions** — card padding, list item vertical spacing, section spacing, screen horizontal margin
   - **Icon style** — filled vs outlined, size (dp), color treatment
   - **Toggle / switch treatment** — ON color, OFF color, disabled color, thumb style
   - **Elevation / surface treatment** — card elevation, background layering
10. **Handoff Instructions** — self-executing section with:
    - Step-by-step process (capture screenshots → choose tool → extract design system → design screen → submit)
    - Screenshot checklist (which screens, which states — use the list below)
    - Tool recommendations (Claude Design, Google Stitch, or Figma) and when to use each
    - Copy-paste-ready prompt to submit to the design tool that instructs it to first extract the design system from screenshots, then design the new screen

## Screenshots to attach

| # | Screen | State to capture | Purpose |
|---|---|---|---|
| 1 | **Settings screen — full view** | Default state, all sections visible (GENERAL, TOOLS, ABOUT) | Shows overall Settings screen layout, section headers, screen background, top app bar style |
| 2 | **Settings screen — TOOLS section close-up** | Observer row visible with icon, title "Observer", supporting text, trailing chevron | Primary SettingsRow style reference — the OBD row must match this pattern exactly |
| 3 | **Session screen — always-recording OFF** | Switch is OFF, status card shows "Inactive" | Status card style reference (card background, padding, layout) — OBD status card must follow this pattern |
| 4 | **Session screen — always-recording ON** | Switch is ON, status card shows "Active" with active session visible | Shows switch ON color, active state label treatment, card with live data |
| 5 | **Track screen — Trip panel (READY state)** | No active trip, panel shows Start button | Shows disabled/idle state treatment, panel card style, overall screen with content behind panel |
| 6 | **Track screen — Trip panel (LIVE/RECORDING state)** | Active trip, timer running, Stop button visible | Shows active state card with live data, metric cells, overall data-dense card layout |

**Submission order:** Attach screenshots 1 and 2 first (Settings layout + SettingsRow reference), then 3 and 4 (status card reference), then 5 and 6 (ambient context).

## Constraints

- Slice 1 screen is a **shell only** — no device picker, no real BT connection, no preference selectors yet (those are Slice 2)
- Enable toggle must be clearly **disabled/greyed** — user cannot toggle ON until a device is selected (Slice 2)
- Follow Material 3 compact operational styling (same as the Session screen always-recording card)
- Do not invent new color tokens; extract and reuse from screenshots
- Dark theme primary; light theme optional
- The design tool must extract the design system from the screenshots first before producing any new UI

## Reference document to model structure on

Use `docs/design-handoff/observer_truncation/OBSERVER_TRUNCATION_SPEC.md` as the structural template. It shows the expected level of detail, section ordering, screenshot checklist format, and the Handoff Instructions section format.

## Output

Write the full spec document content for `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md`. Do not create the file — output the content only so I can review it before saving.
```

---

## Screenshots checklist

Before submitting, capture these 6 screenshots from the running app:

| # | Screen | State |
|---|---|---|
| 1 | Settings screen | Full view — all sections visible |
| 2 | Settings screen | TOOLS section close-up — Observer row visible |
| 3 | Session screen | Always-recording OFF (Inactive) |
| 4 | Session screen | Always-recording ON (Active, session visible) |
| 5 | Track screen | Trip panel READY state (no active trip) |
| 6 | Track screen | Trip panel LIVE state (active trip, timer running) |
