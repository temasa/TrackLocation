# TrackLocation — Observer Phase 1 UI Specification

**Platform:** Android · Jetpack Compose · Material 3
**Visual baseline:** Sessions / List / Track screens (existing).
**Scope:** Phase 1 only.

---

## Screens & states covered

1. **Settings** — default (top-level tab)
2. **Observer Feed** — 5 states:
   - A. Running (service ON · capture running · auto-scroll running)
   - B. Service DISABLED (system-controlled)
   - C. Capture PAUSED (app-controlled)
   - D. Auto-scroll PAUSED + transient jump-to-latest FAB visible
   - E. Empty (service ON, capture running, no events received yet)
3. **Allowlist bottom sheet** — 3 states:
   - F. Empty (no rules)
   - G. Rules applied, no draft
   - H. Rules with unapplied draft edits

---

## Information architecture

```
Bottom nav (unchanged): Session · List · Track · Settings
                                                  └── Settings screen
                                                        └── Tools section
                                                              └── Observer  ──▶  Observer Feed
                                                                                   └── Allowlist (bottom sheet)
```

- **Observer is NOT a bottom-nav tab.**
- Entry path: `Settings → Tools → Observer`.
- Back from Observer returns to Settings.
- Allowlist is a modal bottom sheet over the Observer Feed; closing it returns to the feed (does NOT pop the Observer screen).

---

## 1. Settings screen

A new layout to replace the current empty placeholder. Consistent with Sessions/List/Track:

### Layout

```
┌─ system status bar ──────────────────────────────────┐
│                                                       │
│ Settings              ← 40px / 800 / -1.2 tracking   │
│                                                       │
│ GENERAL                                              │
│ ┌─────────────────────────────────────────────────┐ │
│ │ ⏱  Units              Metric (km, km/h)      ›  │ │
│ │ ─────────────────────────────────────────────── │ │
│ │ 🌐  Map style         System default           › │ │
│ │ ─────────────────────────────────────────────── │ │
│ │ 📍  Location permission  Granted while in use  › │ │
│ └─────────────────────────────────────────────────┘ │
│                                                       │
│ TOOLS                                                 │
│ ┌─────────────────────────────────────────────────┐ │
│ │ 📡  Observer                                   › │ │
│ │     Inspect accessibility events captured       │ │
│ │     by the service                              │ │
│ └─────────────────────────────────────────────────┘ │
│                                                       │
│ ABOUT                                                 │
│ ┌─────────────────────────────────────────────────┐ │
│ │ ℹ  About TrackLocation   Version 1.6.2        › │ │
│ └─────────────────────────────────────────────────┘ │
│                                                       │
├─ bottom nav (Settings tab active) ───────────────────┤
```

### Components

- **Page title** "Settings" — Roboto 40 / 800 / letter-spacing −1.2px. No back arrow (top-level tab). No top app bar.
- **Section group**:
  - Section label: Roboto 12 / 600 / uppercase / letter-spacing 1.4px / `#737373`, padded 18px horizontal, 8px below.
  - Group container: white card, `border: 1.5px solid #eef0ec`, `border-radius: 18px`, rows stacked inside.
- **Settings row** (`min-height: 56px`, `padding: 14px 16px`):
  - Leading icon: 36×36 rounded-10 tile, background `#f1f4f0`, ink-colored icon (20×20, stroke 2)
  - **Label**: Roboto 15 / 600 / `#0a0a0a`
  - **Supporting** (optional): Roboto 12.5 / 400 / `#737373`, line-height 1.35, 2px below label
  - **Trailing value** (optional): Roboto 13 / 500 / `#737373`
  - **Trailing chevron**: 18×18, stroke 2, `#a3a3a3`
  - Divider between rows: `1px solid #eef0ec`. None after last row.

### Tools row — "Observer"

- Icon: broadcast/signal glyph (filled center dot + 2 stroked arcs)
- Label: **"Observer"**
- Supporting: **"Inspect accessibility events captured by the service"**
- Tap navigates to Observer Feed.

### Accessibility

- Each row exposes a single click target with content description `"<Label>, <Supporting>"`.
- Chevron is decorative (`importantForAccessibility=no`).

---

## 2. Observer Feed

The primary Phase 1 surface. Compact, technical, calm. **No dark-green hero card** — Observer is operational, not marketing.

### Layout

```
┌─ system status bar ─────────────────────────────────────┐
│                                                          │
├─ Top app bar (Medium) ──────────────────────────────────┤
│ ← Observer                                       ▽      │  ← Allowlist (filter) icon, badge dot when drafts pending
├─ Service banner (status A) ─────────────────────────────┤
│ ┌────────────────────────────────────────────────────┐ │
│ │ ✓  Service                                          │ │  ← Enabled state (compact)
│ │    Enabled                                          │ │
│ └────────────────────────────────────────────────────┘ │
├─ Capture / Auto-scroll chips (status B + C) ────────────┤
│ ┌──────────────────┐  ┌──────────────────┐             │
│ │ ⏺  Capture        │  │ ↓  Auto-scroll    │            │
│ │    Running        │  │    Running        │            │
│ └──────────────────┘  └──────────────────┘             │
├─ Feed header bar ───────────────────────────────────────┤
│ 124 EVENTS · ALL PACKAGES                        ● LIVE │
├─ Event list (scrollable) ──────────────────────────────┤
│ com.android.chrome       TYPE_VIEW_CLICKED   12:34:56  │
│ .browser.ChromeTabbedActivity                          │
│ ↳ "Search results"                                     │
│ ─────────────────────────────────────────────────────  │
│ ...                                                     │
├─ Bottom nav (Settings tab active) ──────────────────────┤
```

### Top app bar

- Type: Material 3 small/medium top app bar
- Background: `#f1f4f0` (matches body — no contrasting band)
- Back arrow: 24×24 stroke 2, ink, 48dp tap target, pops back to Settings
- Title: **"Observer"** — Roboto 22 / 700 / `#0a0a0a` / letter-spacing −0.2px
- Trailing icon: **Allowlist** (funnel/filter glyph). 48dp tap target.
  - Tap opens Allowlist bottom sheet.
  - Show an 8×8 amber dot at top-right of the icon if there are **unapplied draft** rules. The dot uses `background: #d97706` with a 1.5px border of `#f1f4f0` to punch out from the icon.
  - Content description: `"Allowlist"` (or `"Allowlist, draft changes not applied"` when the dot is shown).

### Status block — three independent indicators

All three are presented separately so users don't confuse them.

#### A. Service state (system-controlled)

**Enabled variant** (compact card):

- Container: white card, `1.5px solid #e7eae6`, `border-radius: 14px`, margin `4px 18px 0`, padding `10px 14px`, flex row gap 12.
- Leading: 26×26 rounded-13 tile, background `rgba(34,197,94,.12)`, green check icon (14×14, stroke 2.6, `#16a34a`).
- Text:
  - Eyebrow "Service" — Roboto 11 / 400 / `#737373`
  - Value "**Enabled**" — Roboto 14 / 600 / `#16a34a`

**Disabled variant** (banner with action):

- Container: `#fef2f2` background, `1.5px solid #fecaca`, `border-radius: 14px`, same margin/padding shape, flex row gap 12 with right-aligned action.
- Leading: 26×26 rounded-13 solid red (`#dc2626`), white warning glyph (`!`).
- Body:
  - Eyebrow "Service" — Roboto 11 / 400 / `#991b1b` @ 85% opacity
  - Value "**Disabled**" — Roboto 14 / 700 / `#991b1b`
  - Helper: **"Enable the accessibility service to start receiving events."** — Roboto 12 / 400 / `#991b1b` @ 80% opacity, line-height 1.35, 4px below value.
- Trailing action: **"Open settings"** button — 36dp tall pill, `background: #991b1b`, `color: #fff`, Roboto 12.5 / 600. Tap launches `ACTION_ACCESSIBILITY_SETTINGS` intent.

#### B. Capture state (app-controlled) — interactive

Chip at 50% width (left of row). **Tappable.** Looks like a button: solid white card surface, colored border, **filled** icon tile.

- Container: 56dp tall, `border-radius: 16px`, `1.5px solid` border, flex row gap 10, padding `0 14px`. Full tap target with ripple.
- Leading: 28×28 rounded-14 **filled** tile in the tone color, white glyph inside.
- Text:
  - Eyebrow "Capture" — Roboto 11 / 400 / `#737373`
  - Value — Roboto 14 / 600

| State | Border | Tile fill | Glyph | Value color | Value text |
|---|---|---|---|---|---|
| **Running** | `#22c55e` | `#22c55e` | filled circle | `#16a34a` | "Running" |
| **Paused** | `#f5e3b8` | `#d97706` | pause bars | `#92400e` | "Paused" |

Tap toggles capture state.

#### C. Auto-scroll state (UI-only) — display-only

Readout at 50% width (right of row). **NOT tappable.** Looks like a status label — visually distinct from the Capture chip so the user doesn't expect it to be interactive.

- Container: 56dp tall, **`background: transparent`**, **no border**, flex row gap 10, padding `0 14px 0 4px`. **No ripple, no hover, no `clickable` modifier.**
- Leading: 28×28 rounded-14 **outlined** tile — transparent fill, 1.5px border in the tone color, tone-colored glyph inside.
- Eyebrow "Auto-scroll" — Roboto 11 / 500 / `#737373` / **uppercase / letter-spacing 1px** (the uppercase eyebrow further signals "this is a label").
- Value — Roboto 14 / **500** / tone color (weight 500 vs Capture's 600 — a quieter type weight reinforces the readout role).

| State | Tile outline | Glyph | Value color | Value text |
|---|---|---|---|---|
| **Running** | `#22c55e` | down-arrow | `#16a34a` | "Running" |
| **Paused** | `#d97706` | pause bars | `#92400e` | "Paused" |

Updates reactively when the user taps the list / drags the list / unpauses. **Tapping this readout does nothing** — the canonical auto-scroll toggle is tap-in-feed.

**Side-by-side recap of the two visual modes** (this is the key cue):

| | Capture (interactive) | Auto-scroll (display-only) |
|---|---|---|
| Surface | white card | transparent |
| Border | 1.5px solid, colored | none |
| Icon tile | filled, white glyph | outlined, tone-colored glyph |
| Eyebrow | sentence-case | UPPERCASE |
| Value weight | 600 | 500 |
| Ripple | yes | no |
| Compose role | `Button` / `Surface(onClick)` | `Row` (no `clickable`) |

#### Capture-paused sub-banner

When **Capture = Paused**, show an inline notice between the chip row and the feed:

- Margin `4px 18px 0`, padding `8px 12px`, `border-radius: 10px`.
- Background `#fef7e6`, border `1px solid #f5e3b8`.
- Text — Roboto 12 / 400 / `#92400e` / line-height 1.35:
  > **"Capture paused — new events are not being received or stored. Previously recorded events remain visible."**

### Feed header bar

A single sticky row between the status block and the event list:

- Background `#f1f4f0`, border-top/bottom `1px solid #e7eae6`, padding `8px 18px`.
- Left: count + scope — Roboto Mono 10.5 / 400 / `#737373` / uppercase / letter-spacing 1px.
  - Format: `"{N} events · {scope}"` where scope is **"all packages"** (when allowlist empty/no enabled rules) or **"{K} of {total} packages"** (when active rules exist).
- Right: live indicator — colored dot + lowercased label.
  - Capture running → green dot + "live"
  - Capture paused → amber dot + "paused"

### Event row

Compact log-line style, ~58–72dp tall depending on optional snippet.

- Padding `10px 18px`. Bottom border `1px solid #eef0ec`.
- Alternating background: odd rows `#ffffff`, even rows `#f8faf7` (very subtle).
- **Line 1** (flex row, gap 8, align center):
  - **Package name** (primary) — Roboto Mono 12.5 / 600 / `#0a0a0a`, flex 1, ellipsized
  - **Event type chip** — see below
  - **Timestamp** — Roboto Mono 11 / 400 / `#737373`, tabular-nums, format `HH:mm:ss` (24h)
- **Line 2** — Activity / class:
  - Roboto Mono 11.5 / 400 / `#737373`, ellipsized, 3px below line 1.
- **Line 3** — optional text snippet (when accessibility text is available):
  - Indented with `↳` glyph (Roboto Mono 11 / `#a3a3a3`)
  - Snippet: Roboto 12 / italic / `#0a0a0a`, ellipsized, wrapped in straight quotes.

#### Event type chip

Small tonal pill, 2px vertical padding × 8px horizontal, `border-radius: 6px`, Roboto Mono 10.5 / 500. Color-coded by category to aid scanning — never carries meaning alone (the text is always present).

| Type prefix | Background | Text |
|---|---|---|
| `CLICK` / `TOUCH` / `GESTURE` | `#eef6ff` | `#1d4ed8` |
| `SCROLL` / `FOCUS` / `HOVER` | `#f5f3ff` | `#6d28d9` |
| `TEXT` / `SELECTION` / `INPUT` | `#fef7e6` | `#92400e` |
| `WINDOW` / `CONTENT` / `STATE` | `#ecfdf5` | `#047857` |
| `ANNOUNCE` / `NOTIFICATION` | `#fdf2f8` | `#9d174d` |
| (fallback) | `#f3f4f6` | `#374151` |

### Empty state (Variant E)

Shown when capture is running and no events have been received yet. Centered vertically inside the feed area:

- 64×64 rounded-32 tile, background `#f1f4f0`, border `1.5px solid #e7eae6`, ink3 broadcast glyph inside.
- Title: **"Waiting for events"** — Roboto 16 / 700 / `#0a0a0a`.
- Body: **"Captured accessibility events will appear here as soon as they're received."** — Roboto 13 / 400 / `#737373`, max-width 260dp, line-height 1.5.

### Jump-to-latest transient FAB

- Pill button, 40dp tall, padding `0 16px 0 12px`, `border-radius: 20px`.
- Background `#0a0a0a`, text `#ffffff`, Roboto 13 / 600.
- Leading icon: down-arrow (16×16 stroke 2.3).
- Copy: **"Jump to latest"**.
- Position: bottom-center of the feed region, `bottom: 92dp` (above the bottom nav).
- Shadow: `0 6px 18px rgba(0,0,0,.25)`.

**Visibility rules** (per spec):

- Shown ONLY at the moment auto-scroll transitions from **Paused → Running** (i.e. user taps the list to resume).
- Auto-dismisses after **~2 seconds** with a 200ms fade.
- Tapping it scrolls the list to the newest event with a 250ms smooth scroll.
- It does **not** change capture or auto-scroll state.

### Interactions (the heart of Phase 1)

| Gesture | Where | Effect |
|---|---|---|
| Tap | Anywhere in the list area | Toggles auto-scroll **Running ↔ Paused**. Reflected in the auto-scroll chip immediately. |
| Drag / scroll | List | Auto-scroll immediately → **Paused**. Manual scroll begins. |
| Long-press on row | List, **only when auto-scroll is Paused** | Copies to clipboard the single line `"{package} | {activity}"` (best-effort — package alone if activity missing). Show transient snackbar **"Copied"**. No-op when auto-scroll is Running. |
| Tap chip | Capture chip | Toggles capture **Running ↔ Paused**. State persists across app restarts. |
| Tap chip | Auto-scroll chip | **No-op.** The readout's visual treatment (no border, outlined icon, transparent background) signals it isn't interactive. The canonical auto-scroll toggle is tap-in-feed. |
| Tap | Allowlist icon (top app bar) | Opens Allowlist bottom sheet. |
| Tap | "Open settings" (Service disabled banner) | Launches Android Accessibility settings intent. |
| Tap | Back arrow | Pops to Settings. |

### State transitions (auto-scroll)

```
            tap-list / Jump-to-latest tap
   Running ─────────────────────────────► Running
        ▲                                       │
        │ tap-list                       drag / │
        │                                scroll │
        │                                       ▼
   Running ◄───────────────────────────── Paused
            tap-list  (and transient FAB
                       appears ~2s)
```

When **transitioning Paused → Running**, auto-scroll continues **from the current scroll position** — it does NOT jump to the latest event. The transient FAB gives the user a one-tap way to actually jump.

### State variables (Compose)

```kotlin
data class ObserverUiState(
    // Status indicators
    val serviceEnabled: Boolean,                 // sourced from AccessibilityManager
    val captureRunning: Boolean,                 // persisted; survives process death
    val autoScrollRunning: Boolean,              // UI-only; resets to true on screen entry

    // Data
    val events: List<ObservedEvent>,             // newest-last in list order
    val totalEventCount: Int,
    val scope: AllowlistScope,                   // 'AllPackages' | 'FilteredCount(k, total)'

    // Transient
    val showJumpToLatestFab: Boolean,            // window: 2000ms after Paused→Running

    // Allowlist
    val allowlistDraftPending: Boolean,
)
```

### Accessibility

- All status indicators carry **icon AND text** — color is never the sole signal.
- Chips and rows expose merged semantics with content descriptions:
  - Service chip → `"Service, Enabled"` / `"Service, Disabled. Tap to open accessibility settings."`
  - Capture chip → `"Capture, Running. Double-tap to pause."` / `"Capture, Paused. Double-tap to resume."`
  - Auto-scroll chip → `"Auto-scroll, Running."` / `"Auto-scroll, Paused. Tap the list to resume."` (and announce that the chip itself is not actionable)
- Event row content description: `"{package}, {activity}, {type}, at {time}"`.
- Long-press copy emits a polite live-region announcement: `"Copied package and activity"`.
- Capture-paused sub-banner is given `liveRegion = polite` so it announces when it appears.
- Service-disabled banner is given `liveRegion = assertive` (high-priority state change).
- Minimum tap targets: 48dp. Allowlist icon, back arrow, status chips all comply.

---

## 3. Allowlist — bottom sheet overlay

A **compact modal bottom sheet** layered over the Observer Feed so the user can see events while they tune rules.

### Container

- Sheet height: `wrap_content`, capped at **62% of screen height** (the spec calls for "compact, not much space"). Feed remains visible behind.
- Sheet shape: rounded top corners `22px`, white background, drop shadow `0 -10px 30px rgba(0,0,0,.18)`.
- Scrim behind sheet: `rgba(10,10,10,.32)`. Tap on scrim → dismiss (saves drafts but does NOT apply them).
- Grabber: 36×4 rounded-2, `#e7eae6`, centered, 10px from sheet top.

### Header

Padding `4px 18px 12px`. Flex row, space-between.

- Title: **"Allowlist"** — Roboto 17 / 700 / `#0a0a0a` / letter-spacing −0.2px.
- Subtitle: **"Match by package name. Applies to future capture only."** — Roboto 12 / 400 / `#737373`, 2px below title.
- Trailing: **Add rule** button — 36×36 round, `background: #0a0a0a`, white `+` glyph (stroke 2.4). Adds a new draft row at the top.

### Draft banner (state H only)

When any rule has unapplied changes (created/edited/toggled/deleted):

- Margin `0 18px 10px`, padding `8px 12px`, `border-radius: 10px`.
- Background `#fef7e6`, border `1px solid #f5e3b8`.
- Icon: amber warning glyph (14×14, stroke 2.5).
- Text: **"Draft changes not applied"** — Roboto 12.5 / 600 / `#92400e`.

### Rule row

A list of rules inside a bordered container (`1px solid #eef0ec`, `border-radius: 14px`, margin `0 18px`). Each row:

- Padding `10px 16px`. Divider `1px solid #eef0ec` between rows. Last row has none.
- Background `#ffffff` normally; **`#fffdf7`** when the row has draft changes.
- **Draft indicator**: 4×22 amber bar (`#d97706`) on the far left edge of the row, vertically centered, only when the row is a draft.

Row internals (flex row gap 10, center-aligned):

1. **Match-type segmented toggle** (Exact / Regex):
   - Container: 2px-padded pill, `background: #f1f4f0`, `border: 1px solid #e7eae6`, `border-radius: 8px`.
   - Segments: 4×8 padding, `border-radius: 6px`. Active segment: white pill with subtle shadow `0 1px 2px rgba(0,0,0,.06)`, Roboto 11 / 600 / `#0a0a0a`. Inactive: Roboto 11 / 500 / `#737373`.
2. **Pattern text field**:
   - Single-line, 32dp tall, `border: 1px solid #e7eae6` (softer when disabled), `border-radius: 8px`, padding `0 10px`.
   - Text: Roboto Mono 12 / 400 / `#0a0a0a` (or `#a3a3a3` when row disabled).
   - Placeholder when empty: `"com.example.app"` for Exact, `"keyword"` for Regex.
3. **Enable/disable toggle** — 30×18 switch:
   - On: track `#22c55e`, knob `#fff` right.
   - Off: track `#d6d8d4`, knob `#fff` left.
4. **Delete** — 28×28 round transparent button with trash glyph (ink2). Confirmation: in-row swipe-to-undo snackbar or quiet delete (Phase 1: quiet delete; the operation is a draft until Apply).

### Empty state (state F)

Inside the bordered container, replace the rule list with:

- Padding `24px 18px`, center-aligned.
- Title: **"No rules yet"** — Roboto 13.5 / 600 / `#0a0a0a`.
- Body: **"Capturing all packages. Add rules to reduce noise."** — Roboto 12.5 / 400 / `#737373` / line-height 1.45.

### Footer

Padding `12px 18px 18px`. Flex row, space-between.

- **Close** button (left): 44dp tall pill, transparent, `1.5px solid #e7eae6`, Roboto 14 / 500 / `#0a0a0a`. Tap: save drafts → dismiss sheet (does NOT apply).
- **Apply** button (right): 44dp tall pill, Roboto 14 / 600.
  - **Disabled** (no draft): `background: #e1e3df`, `color: #a3a3a3`, `cursor: not-allowed`.
  - **Enabled** (draft pending): `background: #0a0a0a`, `color: #fff`. Tap: commit drafts → clear draft indicators → snackbar **"Allowlist updated"**.

### Matching semantics (per spec)

| Rule kind | Match | Case | Scope |
|---|---|---|---|
| **Exact** | Full-string equality of package name | Case-sensitive | Package name only |
| **Regex** | Substring match (keyword can appear anywhere) | Case-sensitive | Package name only |

Capture behavior:

- **Zero enabled rules** → store all events (no filtering). Show helper text in sheet.
- **Any enabled rule** → store only events whose package matches at least one enabled rule.
- Apply affects **future capture only**. Previously stored rows remain visible regardless of rule changes.

### Interactions

| Gesture | Effect |
|---|---|
| Tap **Add rule** | Insert new empty Exact rule at top, focus the pattern field, mark draft. |
| Tap **Exact / Regex** | Toggle row's match kind. Marks draft. |
| Edit **pattern** | Marks draft. (No live validation in Phase 1.) |
| Tap **enable switch** | Toggles per-rule enabled. Marks draft. |
| Tap **delete** | Removes row. Marks draft. |
| Tap **Apply** | Commits all drafts → clears draft markers + draft banner + top-bar dot. Capture behavior reflects new rules on the next event. |
| Tap **Close** / scrim / back | Persists drafts (so they're still there next time the sheet opens) but does **NOT** apply them. Feed/capture continue using last-applied rules. Top-bar dot remains. |

### Accessibility

- Sheet announces on open: `"Allowlist, 5 rules. Edits are drafts until you apply them."`
- Draft banner is `liveRegion = polite`.
- Each row has merged semantics: `"Rule, {kind}, {pattern}, {enabled|disabled}. Double-tap to edit."`
- Apply button announces enabled/disabled state changes.

---

## Visual tokens

### Color (additions on top of existing palette)

| Token | Hex | Use |
|---|---|---|
| `red` | `#dc2626` | Service-disabled icon background |
| `redDark` | `#991b1b` | Service-disabled text + action button |
| `redBg` | `#fef2f2` | Service-disabled banner |
| `redHair` | `#fecaca` | Service-disabled banner border |
| `amber` | `#d97706` | Paused state icon, draft indicators |
| `amberDark` | `#92400e` | Paused state text, draft banner text |
| `amberBg` | `#fef7e6` | Paused sub-banner, draft banner, draft row tint |
| `amberHair` | `#f5e3b8` | Paused/draft borders |
| `cardAlt` | `#f8faf7` | Even-row alternating event surface |

(All existing tokens — `bg`, `card`, `hair`, `hairSoft`, `green`, `greenDark`, `ink`, `ink2`, `ink3`, `navPill` — are reused unchanged.)

### Typography

| Role | Family | Size | Weight | Tracking |
|---|---|---|---|---|
| Page title | Roboto | 40 | 800 | −1.2 |
| App bar title | Roboto | 22 | 700 | −0.2 |
| Section header | Roboto | 12 | 600 | 1.4 (uppercase) |
| Settings label | Roboto | 15 | 600 | −0.1 |
| Settings supporting | Roboto | 12.5 | 400 | — |
| Sheet title | Roboto | 17 | 700 | −0.2 |
| Chip eyebrow | Roboto | 11 | 400 | — |
| Chip value | Roboto | 14 | 600 | −0.1 |
| Feed header | Roboto Mono | 10.5 | 400 | 1.0 (uppercase) |
| Event package | Roboto Mono | 12.5 | 600 | −0.1 |
| Event activity | Roboto Mono | 11.5 | 400 | −0.1 |
| Event type chip | Roboto Mono | 10.5 | 500 | 0.1 |
| Event timestamp | Roboto Mono | 11 | 400 | — (tabular-nums) |
| Event snippet | Roboto (italic) | 12 | 400 | — |
| Rule pattern field | Roboto Mono | 12 | 400 | — |

### Spacing

Page gutter **18px** on Observer (slightly tighter than the 22px gutter used on Sessions/List/Trips — Observer is denser by design). Settings reverts to the standard pattern with 18px section gutter.

### Radii

- Status chip: 16px
- Service banner card: 14px
- Settings group card: 18px
- Event row: 0 (full-bleed inside the white feed surface)
- Rule row pattern field: 8px
- Bottom sheet top corners: 22px
- Buttons / pills: 22px (or 18px for small)

### Motion

| Element | Spec |
|---|---|
| Auto-scroll Paused → Running transient FAB | Fade in 120ms (ease-out), hold ~2000ms, fade out 200ms (ease-in) |
| Jump-to-latest scroll-to-end | 250ms ease-out (Compose `animateScrollToItem`) |
| Bottom sheet open | Material 3 default (300ms ease-emphasized) |
| Status chip / banner mode swap | Background + text color 180ms ease-in-out |
| Apply button enable/disable | 120ms color transition |

### Assets

All glyphs are inline outline icons (Material Symbols equivalents in parentheses):

- Broadcast/signal (`sensors`) — used for Observer entry + status indicator
- List bars (`list`) — bottom nav
- Pin (`place`) — bottom nav
- Gear (`settings`) — bottom nav
- Funnel/filter (`filter_alt`) — Allowlist trailing icon
- Check (`check`) — Service Enabled
- Warning bang (`error`) — Service Disabled / Draft banner
- Filled circle (`fiber_manual_record`) — Capture Running
- Pause bars (`pause`) — Paused states
- Down-arrow (`arrow_downward`) — Auto-scroll Running / Jump-to-latest
- Trash (`delete`) — Rule delete
- Plus (`add`) — Add rule
- Chevron right (`chevron_right`) — Settings rows
- Clock (`schedule`), globe (`public`), pin (`place`) — Settings General icons
- Info (`info`) — About row

No raster assets, no third-party logos.

---

## Out of scope (Phase 1 — do NOT design)

- Clear/delete observer history UI.
- Quick-add rule from live event row.
- Persisted always-recording switch state mirroring (Sessions CR concern, not Observer).
- Filtering / search inside the feed.
- Session detail screens / point-by-point log browser.

---

## Implementation checklist — interaction rules to verify

### Settings
- [ ] Tools section exists with one row labeled **"Observer"**.
- [ ] Tap on Observer row navigates to Observer Feed.
- [ ] Settings is reachable as the 4th bottom-nav tab.

### Observer Feed — status indicators
- [ ] Service, Capture, and Auto-scroll are presented as **three independent indicators** in three visually distinct slots — never combined into one chip.
- [ ] Each indicator shows **both an icon and a text label** (no color-only state communication).
- [ ] **Service Disabled** shows a clear message + **"Open settings"** action; tapping launches `ACTION_ACCESSIBILITY_SETTINGS`.

### Capture state
- [ ] Capture state is **app-controlled**, toggled by tapping the capture chip.
- [ ] **Pause** stops receiving and storing new events (no processing, no DB writes).
- [ ] **Unpause** resumes capture immediately.
- [ ] Capture state **persists across app restarts**.
- [ ] Capture state is **remembered even if the system service is disabled and re-enabled**.
- [ ] When paused, the inline sub-banner is shown.

### Auto-scroll state
- [ ] Auto-scroll is **UI-only** and never affects capture.
- [ ] **Tap anywhere in the list area** toggles auto-scroll Running ↔ Paused.
- [ ] **Drag/scroll** on the list immediately switches auto-scroll to Paused.
- [ ] While **Paused**, the user can scroll freely up/down.
- [ ] When resuming from Paused, auto-scroll continues from the **current position** — it does NOT jump to latest.
- [ ] A **transient jump-to-latest FAB** appears only briefly (~2 seconds) when transitioning Paused → Running.
- [ ] The FAB does **not** change capture or auto-scroll state.

### Long-press copy
- [ ] Long-press on a row works **only when auto-scroll is Paused**.
- [ ] Long-press copies a single line `"{package} | {activity}"` (best-effort, package first).
- [ ] Long-press is a no-op when auto-scroll is Running.

### Allowlist
- [ ] Allowlist UI is a **modal bottom sheet** layered over the Observer Feed; feed remains visible behind.
- [ ] **Zero enabled rules** → all packages are captured. Empty-state helper text is shown.
- [ ] **Exact** rules → full-string equality, case-sensitive.
- [ ] **Regex** rules → case-sensitive, substring match (keyword can appear anywhere).
- [ ] Matching scope is **package name only**.
- [ ] No quick-add suggestions from the feed.
- [ ] **Apply** affects future capture only; previously stored rows remain visible.
- [ ] Each rule row has: Match-type toggle (Exact/Regex), Pattern field, Enable/Disable, Delete.
- [ ] **Closing the sheet** (Close button, scrim tap, back) **saves drafts but does NOT apply them**.
- [ ] Feed/capture reflect **applied** rules only — draft edits do not affect capture.
- [ ] **"Draft changes not applied"** banner appears in the sheet whenever any draft exists.
- [ ] A small amber dot decorates the Allowlist top-bar icon when drafts are pending.

### Data deletion
- [ ] **No UI** anywhere to clear or delete observer history.

### Accessibility
- [ ] Every status indicator carries an icon + text label (not color-only).
- [ ] Disabled service banner uses `liveRegion = assertive`.
- [ ] Capture-paused and draft-not-applied banners use `liveRegion = polite`.
- [ ] All tap targets ≥ 48dp.
- [ ] Event rows expose meaningful content descriptions; long-press copy announces "Copied".
