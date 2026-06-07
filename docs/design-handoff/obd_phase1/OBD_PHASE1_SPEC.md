# OBD Phase 1 Design Specification

**Status:** Partial — Slices 1–2 implemented; Slices 3–4 awaiting design approval  
**Last updated:** 2026-06-07  
**Reference mockup:** `obd_phase1_mockup.png`  
**Implemented code:** `screens/settings/obd/ObdSettingsScreen.kt` (commit pending)

---

## Overview

This specification defines UI surfaces **not yet implemented** in OBD Phase 1 (ELM327 Bluetooth Classic telemetry).

- **Slices 1–2** (ObdSettingsScreen) are **already implemented in code** (see [Implemented Surfaces](#implemented-surfaces) below)
- **Slices 3–4** (Session screen + Trip panel) are **planned and awaiting design approval** (see [Surfaces Awaiting Design](#surfaces-awaiting-design) below)

**Design direction:** Material 3, dark theme primary, inline status indicators, glass panel patterns (consistent with Track screen), compact operational styling.

---

## Implemented Surfaces

### ✅ Slice 1 & 2: ObdSettingsScreen

**Status:** Implemented in code (not yet committed)  
**Navigation path:** Settings → Tools → OBD  
**Code file:** `screens/settings/obd/ObdSettingsScreen.kt`

The OBD Settings screen is **fully implemented** with all Slice 1 and Slice 2 features:

- ✓ Custom top bar (back button, "OBD Settings" title)
- ✓ Status card (icon, status label, subtitle, toggle switch)
- ✓ Status states: Idle, Connecting, Connected, Waiting, Retrying
- ✓ Device section (selected device name + MAC + "Change" button)
- ✓ "Pair a new device" row → opens system Bluetooth settings
- ✓ Device picker dialog (bonded devices list, select + dismiss)
- ✓ Preference sections (Poll Rate, Retention, Retry Cap)
- ✓ Preference dialogs (radio-button selections per preference)
- ✓ Reconnect button (visible in Waiting state)
- ✓ Permission error messaging (location permission required)
- ✓ Conditional visibility (Device + Preferences shown only when enabled)

**Visual reference:**
See `obd_phase1_mockup.png` for the overall layout pattern. The implementation follows Material 3 semantics with white rounded cards on a light background, section headers, and blue accent links.

**Design approval:** Not needed — already implemented and working.

---

### ⚠️ Surface 1: OBD Row in Settings TOOLS Section

**Introduced in:** Slice 1, Step 13 (SettingsScreen.kt edit)  
**Navigation path:** Settings → Tools  
**Status:** Planned (not yet in code)  
**Specification requirement:** None required

This surface uses an **existing pattern** — the same `SettingsRow` Composable as the Observer row in the TOOLS section. No new visual treatment required.

**Expected rendering:**
```
[icon] OBD                    [>]
       ELM327 Bluetooth telemetry
```

**Reference:** Observer row in current Settings screen TOOLS section.

---

## Surfaces Awaiting Design

---

## Surface 2: ObdStatusCard on Session Screen (Slice 3)

**Introduced in:** Slice 3, Step 3 (SessionsScreen.kt edit)  
**Navigation path:** Session screen (bottom-nav first tab)  
**Specification requirement:** Yes, new card component  
**Design approval needed:** Before Slice 3 code begins

### User Goal

Display live OBD metrics (RPM, speed, fuel efficiency) on the Session screen while always-recording. Provides instant feedback on vehicle telemetry during active sessions.

### Layout & Structure

**Placement:** Below the always-recording status card on Session screen (see reference screenshot)

**Card container:**
- Material3 Card or elevated surface
- Corner radius: 12dp (material default)
- Elevation: 1–2dp (subtle, less than status card)
- Padding: 16dp horizontal, 12dp vertical

**Header row (always visible):**
- Left: OBD icon (20dp)
- Center: "OBD Telemetry" or "Vehicle Data" (title, 14sp medium weight)
- Right: Status indicator (dot + label)
  - Connected: green dot + "Connected"
  - Connecting: amber dot + "Connecting…"
  - Waiting: red dot + "Waiting"
  - Idle: gray dot + "Idle"

**Metrics section (content area):**

Three metric cells in a 2-column grid or 3-column row (TBD in design review):

1. **RPM Display**
   - Label: "RPM" (12sp, secondary color, all-caps)
   - Value: 4-digit number (18sp, monospace, primary color) or "—" if unavailable
   - Unit: none (numeric only)
   - Example: `2850`

2. **Speed Display**
   - Label: "Speed" (12sp)
   - Value: 3-digit number + unit (18sp, monospace)
   - Unit: "km/h"
   - Example: `65 km/h`

3. **Instant km/L Display**
   - Label: "Efficiency" or "km/L" (12sp)
   - Value: 1–2 decimal places (16sp, monospace)
   - Unit: "km/L"
   - Visibility: **Hidden when**:
     - GPS speed < 3 km/h (stopped)
     - GPS accuracy > 20 m (poor signal)
     - No valid fuel rate available
   - Shows "—" or empty when hidden
   - Example: `8.5 km/L` (when visible)

**Fuel Source Chip:**
- Small badge below or inline with metrics
- Shows fuel rate source: "Direct (OBD)" or "Inferred (MAF)" or "Unavailable"
- Background: subtle tinted surface (amber for inferred, neutral for direct)
- Text: 11sp, secondary color
- Visibility: shown only when source is known

**Reconnect Button (Waiting state only):**
- Material3 OutlinedButton or TextButton
- Label: "Reconnect"
- Taps → sends ACTION_RECONNECT_NOW intent
- Visible only in Waiting state; hidden in Connected/Idle/Connecting

### State Examples

**Connected state:**
```
┌─────────────────────────────┐
│ ⊙ OBD Telemetry  ● Connected│
├─────────────────────────────┤
│  RPM         Speed   km/L    │
│  2850        65      8.5     │
│              km/h    km/L    │
│                              │
│ Direct (OBD) ─────────────── │
└─────────────────────────────┘
```

**Waiting state (with Reconnect):**
```
┌─────────────────────────────┐
│ ⊙ OBD Telemetry  ● Waiting  │
├─────────────────────────────┤
│  RPM         Speed   km/L    │
│  —           —       —       │
│  Last error: No response     │
│                              │
│         [RECONNECT]          │
└─────────────────────────────┘
```

**Idle state:**
```
┌─────────────────────────────┐
│ ⊙ OBD Telemetry  ○ Idle     │
├─────────────────────────────┤
│  RPM         Speed   km/L    │
│  —           —       —       │
│  Enable in Settings          │
└─────────────────────────────┘
```

### Metrics Visibility Rules

- **RPM & Speed**: always shown (numeric or "—")
- **Instant km/L**: hidden if GPS speed < 3 km/h OR GPS accuracy > 20 m
- **Fuel source chip**: shown only when fuel rate is available and valid

### Material3 Tokens

- Card background: `surfaceContainer` or `surface` + 1dp border
- Title: `onSurface` (87%)
- Metric label: `onSurfaceVariant` (60%), 12sp all-caps
- Metric value: `onSurface` (87%), 18sp monospace, medium weight
- Status dot color: green (connected), amber (connecting), red (waiting), gray (idle)
- Button: Material3 OutlinedButton (primary color)

### Spacing

- Card padding: 16dp horizontal, 12dp vertical
- Header bottom margin: 12dp
- Metric cell spacing: 12dp (grid gap)
- Chip top margin: 8dp
- Button top margin: 12dp

---

## Surface 3: OBD Metric Row in TripPanel (Slice 4)

**Introduced in:** Slice 4 (Track screen, during active trip)  
**Navigation path:** Track screen (third bottom-nav tab)  
**Specification requirement:** Yes, new metric row within existing TripPanel  
**Design approval needed:** Before Slice 4 code begins

### User Goal

Display live OBD-derived km/L metrics during an active trip, allowing the driver to monitor fuel efficiency in real-time alongside distance and elapsed time.

### Layout & Structure

**Placement:** Inside the TripPanel (glass panel at bottom of Track screen)

**Row structure** (similar to existing metric cells in Trip panel):

Add a new metric row **below existing stats** (distance, elapsed time), labeled:

```
┌─────────────────────────────┐
│ OBD Telemetry              │
├─────────────────────────────┤
│ Instant km/L      Avg km/L  │
│    8.5            12.3      │
│                   (trip avg)│
│ Direct (OBD)              ● │
└─────────────────────────────┘
```

**Column 1: Instant km/L**
- Label: "Instant" (12sp, secondary)
- Value: 1–2 decimals (16sp, monospace, primary) or "—"
- Visibility: **hidden when**:
  - GPS speed < 3 km/h
  - GPS accuracy > 20 m
  - No valid fuel rate
- Shows "—" when conditions not met

**Column 2: Average km/L (trip duration)**
- Label: "Avg" or "Trip Avg" (12sp, secondary)
- Value: 1–2 decimals (16sp, monospace)
- Formula: total trip distance / total trip fuel consumed (from OBD samples in trip window)
- Visibility: always shown (numeric or "—" if no valid data)

**Fuel source indicator (trailing):**
- Dot or small icon + label
- "Direct" (OBD PID 015E) or "Inferred" (MAF-derived) or "—"
- Right-aligned, 11sp secondary color
- Tappable → brief tooltip or snackbar explaining source

### Example States

**Driving (instant > 3 km/h):**
```
Instant km/L      Avg km/L
   8.5             12.3
```

**Stopped (instant hidden):**
```
Instant km/L      Avg km/L
   —               12.3
```

**No data:**
```
Instant km/L      Avg km/L
   —               —
```

### Regression Prevention

- No changes to existing metric cells (distance, elapsed)
- OBD row is additive only
- If OBD service is disconnected, row shows "—" across all values
- Observer feed, trip start/stop, Session always-recording: no visual or behavioral impact

### Material3 Tokens

- Row background: transparent (inherits TripPanel glass background)
- Label: `onSurface` (60%), 12sp
- Value: `onSurface` (87%), 16sp monospace, medium weight
- Source indicator: `onSurface` (50%), 11sp
- Accent dot: green (Connected), gray (Idle/Disconnected)

### Spacing

- Row vertical padding: 8dp (top/bottom)
- Column spacing: 24dp
- Label–value spacing: 4dp (vertical)
- Source indicator: right-aligned, 8dp from edge

---

## Handoff Instructions

### Step 1: Reference Existing Screens

Before design tool handoff, provide the following **reference screenshots** to preserve visual language:

1. **Session screen** — showing always-recording status card (card container, text layout, switch placement)
2. **Track screen TripPanel** — showing existing metric cells (distance, elapsed time) and spacing pattern
3. **Settings TOOLS section** — showing Observer row (reference for OBD Settings screen styling already implemented)

### Step 2: Design Tool Submission (Google Stitch / Claude Design)

**Tool:** Google Stitch or Claude Design  
**Prompt template:**

```
Create the Android OBD Phase 1 UI for TrackLocation — Slices 3–4.

Reference spec: OBD_PHASE1_SPEC.md (Surfaces 2–3)
OBD Settings screen (Slices 1–2) already implemented; focus on these two new surfaces:

1. OBD Status Card — Session screen (Slice 3)
   - Live metrics: RPM, Speed, instant km/L
   - Status indicator dot (Connected/Waiting/Idle) + label
   - Reconnect button (visible in Waiting state only)
   - Fuel source chip (Direct/Inferred/Unavailable)
   - Instant km/L hidden when: GPS speed < 3 km/h OR accuracy > 20 m

2. OBD Metric Row — Trip Panel (Slice 4)
   - Two metric columns: Instant km/L, Trip Avg km/L
   - Visibility: instant hidden when speed < 3 km/h, always show avg
   - Fuel source indicator (right-aligned, small)
   - Consistent styling with existing metric cells (distance, elapsed time)

Reference screenshots (attached):
- Session screen (always-recording card as styling baseline)
- Track TripPanel (existing metric cells as layout baseline)

Requirements:
- Material 3, dark + light theme variants
- Compact operational styling, monospace numerics
- Visibility rules: instant km/L conditional, avg always shown
- Accessibility: semantic roles, color + text, tested with TalkBack
- Glass panel / card consistency with existing screens
```

### Step 3: Approval & Integration

1. **Design approval:** Get visual feedback on both surfaces and state variants
2. **Address feedback:** Update this spec if design changes are requested
3. **Mark approved:** Once approved, update progress:
   - `docs/progress.md` → note design spec approved for Slices 3–4
   - Slice 3 code can proceed

---

## Dependencies & Blockers

| Slice | Status | Blocker |
|---|---|---|
| 1 | ✅ Implemented | None — code committed |
| 2 | ✅ Implemented | None — code ready to commit |
| 3 | 🔄 Planned | Design approval (this spec, Surfaces 2) |
| 4 | 🔄 Planned | Design approval (this spec, Surface 3) |

---

## Design Review Checklist (Surfaces 2–3 Only)

- [ ] ObdStatusCard layout and state variants reviewed
- [ ] Metric visibility rules (instant km/L conditional) understood
- [ ] Reconnect button placement and styling approved
- [ ] Fuel source chip design approved
- [ ] TripPanel metric row styling consistent with existing metrics
- [ ] Dark and light theme variants approved
- [ ] Spacing and padding match Material 3 guidelines
- [ ] Accessibility guidance understood (semantic roles, color + text)
- [ ] Reference screenshots reviewed
- [ ] Ready for Slice 3–4 code implementation

---

**Version:** 1.1 (Rectified)  
**Created:** 2026-06-07  
**Updated:** 2026-06-07 (removed Slices 1–2 implemented surfaces, focused on Slices 3–4)  
**Next step:** Collect reference screenshots (Session screen + TripPanel) and submit Surfaces 2–3 to design tool (Google Stitch / Claude Design).
