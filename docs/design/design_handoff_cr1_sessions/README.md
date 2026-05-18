# Handoff: CR#1 — Always-recorded location sessions

## Overview

This bundle implements **CR#1** from `crs_screen_specification.md` for **TrackLocation**, an Android app that records and tracks location-based trips.

CR#1 introduces a new concept — **always-recorded location sessions** — alongside the existing trips concept. A session is a continuous period during which the app records location points in the background, independent of whether the user has started a "trip" on the Track screen.

This handoff covers three screen states:

1. **Sessions — Idle** (empty / switch OFF)
2. **Sessions — Recording** (switch ON, active session pinned, history below)
3. **List — unchanged content, but with the new 4-tab bottom nav**

> **Correction applied in this design vs. the original spec text:**
> The original spec placed the always-recording switch in the **List** screen header (replacing `Export`). The corrected requirement is that the switch lives on the **Sessions** screen header instead. The List screen's `Export` pill remains. The accompanying `crs_screen_specification.md` is included verbatim for context — treat the switch location override above as the authoritative requirement.

## About the Design Files

The files in this bundle are **design references created in HTML/React** — prototypes that demonstrate the intended look and behavior. They are **not production code** to copy directly.

Your task is to recreate these HTML designs in the **TrackLocation Android codebase**, using its established patterns, theme, and component library (Jetpack Compose, XML views, Material 3, whatever the project uses). Hex values, typography, and spacing are documented below so you can map them to your existing design tokens — or extend the token set if needed.

If the codebase has no UI framework decision yet, prefer **Jetpack Compose with Material 3**, since the screens follow a Material-adjacent visual language.

## Fidelity

**High-fidelity.** Pixel-perfect mockups with final colors, typography, spacing, corner radii, and interaction states. Recreate pixel-perfectly using existing theme tokens.

## Files in this bundle

| File | Purpose |
|---|---|
| `CR1 Sessions Redesign.html` | Entry point — opens a pan/zoom canvas with all three artboards side-by-side. Open in a browser to inspect. |
| `cr1-shared.jsx` | Shared design tokens (`T`) and reusable components: `StatusBar`, `PageHeader`, `PillButton`, `RecordingSwitch`, `StatusHero`, `SessionRow`, `BottomNav` |
| `screen-sessions-idle.jsx` | Sessions screen — switch OFF, empty state |
| `screen-sessions-active.jsx` | Sessions screen — switch ON, active session + history |
| `screen-list-updated-nav.jsx` | List screen — unchanged content, with new 4-tab nav |
| `design-canvas.jsx` | Pan/zoom artboard container (presentation-only, not part of the implementation) |
| `crs_screen_specification.md` | Original CR specification document |

---

## Navigation (applies to all screens)

Bottom navigation contains **four primary destinations in this order**:

1. **Session** — new first-class screen, lists always-recorded sessions
2. **List** — trip list (existing screen, content unchanged)
3. **Track** — Start/Stop trip control (existing screen, copy updated, see below)
4. **Settings** — existing screen

The Session destination is a top-level tab, **not** a nested page under List.

### Bottom nav visual

- Container: full-width, `padding: 10px 6px 16px`, `border-top: 1px solid #e7eae6`, background `#f1f4f0`
- Tabs: equal-flex, vertical stack of icon + label
- **Active tab** gets a dark pill behind its icon:
  - Pill: `padding: 8px 22px`, `border-radius: 22px`, `background: #0e2a20`
  - Icon stroke color: `#22c55e` (brand green)
  - Label below pill: `color: #0a0a0a`, `font-weight: 600`
- **Inactive tab**:
  - No pill background, `padding: 8px 14px`
  - Icon and label: `color: #737373`, `font-weight: 500`
- Label font: `Roboto 11px`
- Icon size: `22×22`, stroke width `1.9`

### Icons (24×24 viewBox, outline-style)

- **Session**: filled center dot + two concentric stroked arcs (broadcast/signal glyph)
- **List**: three horizontal lines (top + middle full width, bottom shorter, ~60%)
- **Track**: location pin with a small circle inside
- **Settings**: gear / sun-like glyph (center circle + 8 radial ticks)

---

## Screen 1 — Sessions (Idle / empty state)

### Purpose
Default state when no always-recorded sessions exist yet. Communicates how to start recording.

### Layout (top to bottom)
1. Status bar (system) — 30px tall
2. Page header — eyebrow + title + switch on right
3. Status hero card — dark green, "Idle"
4. **Vertical-centered empty state** in remaining space
5. Bottom nav

### Page Header
- Outer padding: `14px 22px 6px`
- **Eyebrow**: `"Trip Tracker"` — `Roboto 14px / 400 / #737373`
- **Title**: `"Sessions"` — `Roboto 40px / 800 / #0a0a0a`, `letter-spacing: -1.2px`, `line-height: 1.05`
- **Right slot**: `RecordingSwitch` (see component spec below), in OFF state
- Title + right slot are on the same row, `justify-content: space-between`, `align-items: center`

### Status Hero card (Idle variant)
- Margin: `14px 22px 0`
- Padding: `20px 22px`
- Background: `#173a2d` (dark green)
- Border radius: `22px`
- Layout: horizontal flex, `gap: 16px`, `align-items: center`
- **Indicator circle** (left): `56×56`, `border-radius: 28px`, `background: #22513f`
  - Inner dot: `16×16`, `border-radius: 8px`, `background: #9aa9a1` (muted gray-green)
- **Text block** (flex 1):
  - Label: `"Always-recording"` — `Roboto 13px / 400 / rgba(255,255,255,.65)`
  - Status: `"Idle"` — `Roboto 22px / 700 / #ffffff`, `letter-spacing: -0.4px`, `line-height: 1.15`, `margin-top: 2px`

### Empty State (center)
- Container: flex column, vertically centered, `padding: 0 36px`, `gap: 14px`, `text-align: center`
- **Icon container**: `72×72`, `border-radius: 36px`, `background: #ffffff`, `border: 1.5px solid #e7eae6`
  - Inside: broadcast/signal icon, `30×30`, stroke `#a3a3a3`, stroke-width `1.7`
- **Title**: `"No sessions recorded yet"` — `Roboto 19px / 700 / #0a0a0a`, `letter-spacing: -0.2px`
- **Body**: max-width `280px`, `Roboto 14px / 400 / #737373`, `line-height: 1.5`
  > Sessions appear here when always-recording is turned **ON** from the Sessions header.
  - The word `ON` is bold (`font-weight: 600`) and colored `#0a0a0a`.

### State (Idle)
- `recordingOn: false`
- `sessions: []`

---

## Screen 2 — Sessions (Recording, with history)

### Purpose
Default state when one or more sessions exist. The currently active session (if any) pins to the top with a live, animated marker.

### Layout
Same top-down structure as Idle, but the empty state is replaced by a **section caption + scrolling list**.

### Page Header
Identical to Idle, but the `RecordingSwitch` is in **ON** state.

### Status Hero card (Recording variant)
- Same container as Idle.
- **Indicator circle** background changes: `rgba(34, 197, 94, 0.18)` (light green wash)
- **Inner dot**: `background: #22c55e`, plus `box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.22)` (halo)
- **Inner dot pulse animation**:
  ```css
  @keyframes sessPulse { 0%,100% { opacity: 1 } 50% { opacity: .55 } }
  animation: sessPulse 1.6s ease-in-out infinite;
  ```
- Status text: `"Recording"` (replaces `"Idle"`)
- **Live readout line** (only when recording):
  - Below status, `margin-top: 6px`
  - `Roboto 13px / 400 / rgba(255,255,255,.78)`
  - `font-variant-numeric: tabular-nums`
  - Format: `"00:12:34 · 184 points"` — elapsed (HH:MM:SS) + a middle dot + point count
  - **Updates live** every second while recording, if the app has current data.

### Section caption
- Margin: `18px 22px 0`
- Layout: flex row, `justify-content: space-between`, `align-items: baseline`
- **Heading**: `"Recorded sessions"` — `Roboto 17px / 700 / #0a0a0a`, `letter-spacing: -0.2px`
- **Trailing**: `"Newest first"` — `Roboto 13px / 400 / #737373`

### Session List
Scrolling vertical list of `SessionRow` components. The currently active session is the **first row** and uses the active visual treatment.

#### `SessionRow` — full spec
- Margin: `12px 22px 0`
- Padding: `18px 18px 18px 20px`
- Background: `#ffffff`
- Border radius: `20px`
- Border: `1.5px solid`, color depends on active state:
  - Inactive: `#eef0ec`
  - **Active: `#22c55e`**
- Position: `relative`, `overflow: hidden`

**Active badge** (only when `active=true`):
- Positioned `top: 14px, right: 16px`
- Layout: inline flex, `gap: 6px`, `padding: 4px 10px 4px 8px`
- Background: `rgba(34, 197, 94, 0.12)`
- Border radius: `999px`
- Inside: pulsing green dot (`7×7`, `#22c55e`, same `sessPulse` animation) + label
- Label: `"ACTIVE"` — `Roboto 11px / 600 / #16a34a`, `letter-spacing: 0.3px`, `text-transform: uppercase`

**Row inner layout** (horizontal flex, `gap: 14px`, `align-items: flex-start`):
- **Marker gutter** (`width: 14px`, `padding-top: 5px`, flex column, `align-items: center`):
  - Top dot: `12×12`, `border-radius: 6px`, `border: 2.5px solid <accent>`, `background: #fff`
  - Vertical line: `width: 2.5px`, `flex: 1`, `min-height: 32px`, `background: <accent>`, `margin-top: 2px`, `border-radius: 2px`
  - **Accent color**: `#22c55e` when active, `#16a34a` when inactive
- **Body** (flex 1, `min-width: 0`):
  - **Top row**: flex space-between, `align-items: baseline`, `gap: 12px`
    - **Title**: `"Session #<n>"` — `Roboto 19px / 700 / #0a0a0a`, `letter-spacing: -0.2px`
    - **Start time** (right): `Roboto 13px / 400 / #737373`, `font-variant-numeric: tabular-nums`
      - When active, give it `padding-right: 70px` to clear the badge.
      - Format: `"Today, 08:19"`, `"Wed, 07:02"`, `"Tue, 18:41"`
  - **Hairline divider**: `height: 1px`, `background: #eef0ec`, `margin: 14px 0 14px -34px` (the negative left margin extends it through the marker gutter — important visual detail)
  - **Metric grid**: `display: grid`, `grid-template-columns: 1fr 1fr 1fr`, `gap: 4px`
    - Three `Metric` cells: **duration**, **distance**, **points**

#### `Metric` cell
- **Value**: `Roboto 17px / 700 / #0a0a0a`, `letter-spacing: -0.2px`, `line-height: 1`, `font-variant-numeric: tabular-nums`
  - Duration format: `HH:MM:SS` (e.g. `00:12:34`, `01:14:22`)
  - Distance format: `X.XX km` (two decimals, e.g. `1.84 km`)
  - Points format: integer (e.g. `184`, `1862`)
- **Unit label**: `Roboto 12px / 400 / #737373`, `margin-top: 6px`
  - Strings: `"duration"`, `"distance"`, `"points"`

### Sample data (for the mock)
| Active | # | Start | Duration | Distance | Points |
|:--:|:--:|---|---|---|---|
| ✅ | 7 | Today, 08:19 | 00:12:34 | 1.84 km | 184 |
|  | 6 | Wed, 07:02   | 01:14:22 | 9.42 km | 1862 |
|  | 5 | Tue, 18:41   | 00:41:08 | 3.07 km | 742 |

### Optional row metadata (per spec, "if space allows")
- End time, number of recorded points (already shown), active status (already shown).

---

## Screen 3 — List (unchanged content + new 4-tab nav)

The List screen is **not** part of the change beyond its bottom nav. It's included so the developer can see how the four-tab nav lives across both screens. All visual details (hero card, stat cards, trip rows) are exactly as in the existing app.

Key elements documented for completeness:

### Page Header
- Eyebrow: `"Trip Tracker"`
- Title: `"Trips"` (same Roboto 40/800 treatment)
- Right slot: **`Export` pill button** (kept — do not replace with switch)

### `PillButton` (Export)
- `height: 44px`, `padding: 0 22px`, `border-radius: 22px`
- `background: #ffffff`, `border: 1.5px solid #e7eae6`
- Text: `Roboto 15px / 500 / #0a0a0a`

### Current-trip hero card
- Same `#173a2d` background, `border-radius: 22px`, `margin: 14px 22px 0`, `padding: 18px 20px`
- Layout: flex row, `gap: 14px`, `align-items: center`
- Indicator: `52×52`, `border-radius: 26px`, `background: #22513f`, with `14×14` green dot inside (`#22c55e`)
- Text block: label `"Current trip"` (13px / rgba(255,255,255,.65)) + title `"Ready to record"` (19/700/#fff)
- **Start button**: `height: 44px`, `padding: 0 22px`, `border-radius: 22px`, `background: #22c55e`, `color: #fff`, `font-weight: 600`, `font-size: 15px`

### Stat row
- Three equal-width white cards, `gap: 10px`, `padding: 14px 22px 0`
- Each: `padding: 18px 16px`, `border-radius: 18px`, `border: 1.5px solid #eef0ec`
- Value: `Roboto 30px / 800 / <color>`, `letter-spacing: -0.6px`, `line-height: 1`, `font-variant-numeric: tabular-nums`
- Unit label: `Roboto 14px / 400 / #737373`, `margin-top: 8px`
- Colors:
  | Stat | Value | Color |
  |---|---|---|
  | Trips | `5` | `#16a34a` (green) |
  | Distance | `52.4` | `#2563eb` (blue) |
  | Hours | `3.3` | `#d97706` (amber) |

### Trip rows
Identical structure to `SessionRow` (without the active-state path), with these label changes:
- Title: `"Track #<n>"` (e.g. `Track #4`, `Track #5`)
- Top-right meta: date + time like `"Wed, 07:15"`
- Metric grid: `<n> km` / `HH:MM:SS` / `<n> km/h` with labels `km` / `duration` / `avg speed`

---

## Components — Cross-cutting

### `RecordingSwitch` — full spec

Compact, header-sized control. Use this exact treatment.

- **Wrapper button**: `height: 44px`, `padding: 0 8px 0 14px`, `border-radius: 22px`
- **Background**: `#ffffff`
- **Border**: `1.5px solid`
  - OFF: `#e7eae6`
  - ON: `#22c55e`
- **Layout**: inline flex, `align-items: center`, `gap: 10px`
- **Label** (left of toggle): `Roboto 13px / 600`, `letter-spacing: 0.2px`
  - OFF text: `"OFF"`, color `#737373`
  - ON text: `"ON"`, color `#16a34a`
- **Toggle track**: `38×22`, `border-radius: 11px`
  - OFF: `background: #d6d8d4`
  - ON: `background: #22c55e`
  - Track color animates with `transition: background 0.2s`
- **Toggle knob**: `18×18`, `border-radius: 9px`, `background: #ffffff`, `box-shadow: 0 1px 3px rgba(0,0,0,0.25)`
  - Position: `top: 2px`, `left: 2px` when OFF, `left: 18px` when ON
  - Animates with `transition: left 0.2s`

#### Behavior (from spec)
- **OFF** → no always-recorded session is active.
- **ON** → foreground location recorder is active.
- **Permission flow when tapping ON**:
  - Permission not requested → trigger system location-permission flow before recording starts.
  - Permission granted → switch turns ON, recording starts immediately.
  - Permission denied → switch stays OFF.
  - Permission permanently denied → show existing permission/settings guidance dialog.
- Switch state **does not need to persist after process death** (out of scope for CR#1).

---

## Interactions & Animations

### Pulse animation
Used on (1) the active session-row badge dot, and (2) the Recording hero's inner dot.

```css
@keyframes sessPulse {
  0%, 100% { opacity: 1; }
  50%      { opacity: 0.55; }
}
/* applied as: animation: sessPulse 1.6s ease-in-out infinite; */
```

### Switch toggle
- Knob `left` and track `background` both transition over `0.2s` (default ease).

### Live recording readout
- Elapsed time updates every second.
- Point count updates whenever a new point is recorded.
- Both are inside the dark green hero card while `recordingOn === true`.

### Tab switching
- No bespoke transition required — defer to the platform's standard tab-change behavior.

---

## State Management

Minimum state for the Sessions screen:

```ts
type Session = {
  id: string;
  index: number;            // user-facing #
  startedAt: Date;
  endedAt: Date | null;     // null while active
  durationMs: number;       // computed live for active session
  distanceKm: number;       // computed live for active session
  pointCount: number;
  isActive: boolean;
};

type SessionsState = {
  recordingOn: boolean;     // drives the RecordingSwitch
  sessions: Session[];      // sorted newest-first
  permissionState:
    | 'not-requested'
    | 'granted'
    | 'denied'
    | 'permanently-denied';
};
```

State transitions:
- Switch tap (currently OFF) → check `permissionState`:
  - `not-requested` → request → on grant: open new active session, set `recordingOn = true`. On deny: set `permissionState = 'denied'`, switch stays OFF.
  - `granted` → open new active session, `recordingOn = true`.
  - `denied` | `permanently-denied` → switch stays OFF, show guidance.
- Switch tap (currently ON) → close active session (`endedAt = now`, `isActive = false`), set `recordingOn = false`.
- New location point received while a session is active → increment `pointCount`, recompute `distanceKm`, `durationMs`.

---

## Design Tokens

### Colors

| Token | Hex | Use |
|---|---|---|
| `bg` | `#f1f4f0` | App background |
| `card` | `#ffffff` | Card / row background |
| `hair` | `#e7eae6` | Card borders, dividers |
| `hairSoft` | `#eef0ec` | Inner divider hairline |
| `hero` | `#173a2d` | Dark green hero card |
| `heroDim` | `#22513f` | Hero card inner indicator (idle) |
| `ink` | `#0a0a0a` | Primary text |
| `ink2` | `#737373` | Secondary text |
| `ink3` | `#a3a3a3` | Tertiary (empty-state icon) |
| `green` | `#22c55e` | Active / brand green |
| `greenDark` | `#16a34a` | Trip count, active label |
| `blue` | `#2563eb` | Distance stat |
| `amber` | `#d97706` | Hours stat |
| `red` | `#dc2626` | Reserved for error/destructive |
| `navPill` | `#0e2a20` | Active bottom-nav pill background |

### Typography

All text uses **Roboto** (system default on Android). Weights used: 400, 500, 600, 700, 800.

| Role | Size | Weight | Tracking | Color |
|---|---|---|---|---|
| H1 page title | 40px | 800 | -1.2px | `ink` |
| H2 section heading | 19px | 700 | -0.2px | `ink` |
| H3 stat value (large) | 30px | 800 | -0.6px | varies |
| Hero status | 22px | 700 | -0.4px | white |
| Metric value | 17px | 700 | -0.2px | `ink` |
| Stat unit label | 14px | 400 | — | `ink2` |
| Eyebrow / body | 14px | 400 | — | `ink2` |
| Date / meta | 13px | 400 | — | `ink2` |
| Hero label / live readout | 13px | 400 | — | white α |
| Switch label | 13px | 600 | 0.2px | varies |
| Metric unit | 12px | 400 | — | `ink2` |
| Nav label | 11px | 500/600 | — | varies |
| Active badge | 11px | 600 | 0.3px uppercase | `greenDark` |

Tabular numbers (`font-variant-numeric: tabular-nums`) on every duration, distance, point-count, and time-of-day value.

### Spacing

Page outer gutter: **22px**. Vertical rhythm uses **6 / 8 / 10 / 12 / 14 / 18 / 22** px.

### Border radius

- Buttons / pills / switch: `22px` (≈ height/2)
- Cards / rows: `20px`
- Stat cards: `18px`
- Hero card: `22px`
- Status indicator circles: `50%`
- Small chips / badges: `999px`

### Shadows
- Switch knob: `0 1px 3px rgba(0,0,0,0.25)`
- Recording hero dot halo (when ON): `box-shadow: 0 0 0 6px rgba(34,197,94,0.22)` (used as a ring, not a drop shadow)

---

## Assets

All icons are inline SVGs in the source files — **no raster assets**. They are simple line/outline icons (stroke 1.7–1.9px) that can be replaced with the project's icon library (e.g. Material Symbols outlined: `sensors`, `list`, `place`, `settings`).

There are no photos, illustrations, or third-party logos.

---

## Acceptance Checklist

- [ ] Bottom nav has four tabs in order: **Session, List, Track, Settings**.
- [ ] Session is a top-level destination — not nested under List.
- [ ] List screen header still shows the **Export** pill (no switch).
- [ ] Sessions screen header shows eyebrow + title + **always-recording switch**.
- [ ] Switch OFF → header shows "OFF" label and gray track; hero shows "Idle".
- [ ] Switch ON → header shows "ON" label (green) and green track; hero shows "Recording" with pulsing dot and live elapsed/point counter.
- [ ] Tapping switch ON with no permission triggers the system location-permission flow before recording.
- [ ] If permission is denied or permanently denied, switch stays OFF.
- [ ] Active session pins to top of list with green border + animated "ACTIVE" badge.
- [ ] Session list is sorted newest-first.
- [ ] Empty state copy and styling match spec when no sessions exist.
- [ ] Track screen retains existing Start/Stop UI but uses trip-specific copy (`Start trip` / `Stop trip` where practical).
- [ ] Trip list (List screen) is unaffected — it shows only trips, not sessions.

---

## Out of Scope (per CR#1 spec)

- Raw point-by-point location log browser
- Session detail map screen
- Session delete / edit actions
- Persisting always-recording state across process death
- Complex filtering or search on the sessions list
