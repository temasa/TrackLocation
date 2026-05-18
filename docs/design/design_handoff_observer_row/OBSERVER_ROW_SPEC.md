# Observer Event Row — UI Update

A focused UI change to the **event row** in the Observer Feed screen of TrackLocation (Android · Jetpack Compose · Material 3).

**Scope: row layout only.** Do not touch the top app bar, status indicators (Service banner, Capture/Auto-scroll chips), feed header, allowlist bottom sheet, or navigation. The rest of the Observer Feed screen is unchanged.

> **About these files.** `screen-observer.jsx` is a web prototype showing the row's visual reference — it contains the full Observer Feed for context, but you only need to recreate the `EventRow` component. Use `Observer Phase 1.html` in a browser to see the design live; pan/zoom to the running-state artboard.

## What's changing

The row was previously a single-line "log line" with the package, type chip, and time on one row, and the activity below it. Both the package and activity were ellipsized — long fully-qualified names disappeared behind `…`. Users couldn't tell apart events from packages like `com.sec.android.…`.

The new layout is **stacked**, with each piece of metadata on its own line and free to wrap. No truncation.

## New row layout (top → bottom)

Row container padding: **10dp horizontal × 10dp vertical**.
Row container background alternates: odd rows `#FFFFFF`, even rows `#F8FAF7`.
Bottom border: `1dp solid #EEF0EC`.

```
┌─────────────────────────────────────────────────────────────┐
│ com.sec.android.app.launcher                      05:12:13 │  ← Line 1
│ android.widget.ListView                                    │  ← Line 2
│ [WINDOW_CONTENT_CHANGED]                                   │  ← Line 3
│ ↳ "Recent apps"                                            │  ← Line 4 (optional)
└─────────────────────────────────────────────────────────────┘
```

### Line 1 — Package + timestamp (row, top-aligned, gap 10dp)

- **Package** (`Modifier.weight(1f)`):
  - Roboto Mono, **12.5sp**, weight **600**, color **`#0A0A0A`**, line-height 1.3, letter-spacing -0.1sp.
  - **Word-break at any character** so a long package wraps cleanly (not by hyphenation rules).
  - Clamp to **2 lines maximum** (`maxLines = 2`, `overflow = TextOverflow.Ellipsis`).
- **Timestamp** (no shrink):
  - Roboto Mono, **11sp**, weight 400, color **`#737373`**.
  - Tabular figures.
  - Format `HH:mm:ss` (24h).

### Line 2 — Activity / class (full width)

- Top margin: **3dp**.
- Roboto Mono, **11.5sp**, weight 400, color **`#737373`**, line-height 1.4, letter-spacing -0.1sp.
- **Word-break at any character**. **No `maxLines` cap** — long names like `com.kolee.tracklocation.MainActivity` must wrap onto a second line, never ellipsize.
- **Omit this line entirely** when `activity == null`.

### Line 3 — Event type chip (left-aligned)

- Top margin: **6dp**.
- Chip:
  - Padding **2dp vertical × 7dp horizontal**.
  - Corner radius **5dp**.
  - Roboto Mono, **10sp**, weight **600**, letter-spacing **0.15sp**.
- **Strip the `TYPE_` prefix** from the displayed text:
  - Show `WINDOW_CONTENT_CHANGED`, not `TYPE_WINDOW_CONTENT_CHANGED`.
  - Show `VIEW_CLICKED`, not `TYPE_VIEW_CLICKED`.
  - **Keep the full original type string** in any clipboard payload — only the visible label is shortened.

#### Tonal color mapping

Apply by **prefix of the stripped string** (do the regex against the displayed label, after `TYPE_` is removed). Both background and text color are set per category.

| Prefix matches | Background | Text |
|---|---|---|
| `CLICK` · `TOUCH` · `GESTURE` | `#EEF6FF` | `#1D4ED8` |
| `SCROLL` · `FOCUS` · `HOVER` | `#F5F3FF` | `#6D28D9` |
| `TEXT` · `SELECTION` · `INPUT` | `#FEF7E6` | `#92400E` |
| `WINDOW` · `CONTENT` · `STATE` | `#ECFDF5` | `#047857` |
| `ANNOUNCE` · `NOTIFICATION` | `#FDF2F8` | `#9D174D` |
| (fallback) | `#F3F4F6` | `#374151` |

### Line 4 (optional) — Snippet

- Render only when a snippet string is present.
- Top margin: **6dp**.
- Layout: row, baseline-aligned, gap **6dp**.
- Leading glyph `↳`: Roboto Mono, 11sp, color **`#A3A3A3`**.
- Snippet text: Roboto, **12sp**, italic, color **`#0A0A0A`**, single line, ellipsized, wrapped in straight double quotes `"…"`.

## Constraints — keep these working

- **Long-press copy** on a row continues to copy `"{package} | {activity}"` (package first, best-effort) — only when **auto-scroll is Paused**. Don't change the gating logic; just keep the long-press modifier attached to the new container.
- **Row content description** still uses the **full original type string** (with `TYPE_` prefix), not the stripped label. Accessibility cares about precision. Recommended format:
  > `"{package}, {activity}, {fullType}, at {time}"`

## Acceptance checklist

After running on a device with the existing accessibility-event stream:

- [ ] No package name is truncated unless the row genuinely needs more than 2 lines.
- [ ] A long activity name like `com.kolee.tracklocation.MainActivity` wraps onto a second line — not ellipsized.
- [ ] Event chips show `WINDOW_CONTENT_CHANGED`, `WINDOW_STATE_CHANGED`, `VIEW_CLICKED`, etc., **without** the `TYPE_` prefix.
- [ ] Chip colors follow the tonal mapping (e.g. `WINDOW_*` is green, `VIEW_CLICKED` is blue).
- [ ] Snippet rows (when present) still render as a single italicized line below the chip, with the `↳` leader.
- [ ] Long-press while auto-scroll is paused still copies `"{package} | {activity}"` to the clipboard.
- [ ] Row background alternates between `#FFFFFF` and `#F8FAF7`.
- [ ] No other part of the Observer screen has changed.

## Visual reference

- `screen-observer.jsx` — the HTML/React component implementing this layout. The `EventRow` and `EventTypeChip` functions are the relevant ones; the rest of the file is the surrounding screen for context.
- `Observer Phase 1.html` — open in a browser and look at the "A · Service ON, Capture Running, Auto-scroll Running" artboard for the row in its normal state.
