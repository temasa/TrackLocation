# Handoff: Observer Snapshot Viewer

## Overview

Adds a **"View window content"** affordance to each event card in the Observer
Feed (Settings → Tools → Observer). Tapping it opens a **modal bottom sheet**
that shows the event's captured `treeSnapshot` as a readable, formatted
hierarchy with a Raw JSON fallback and a single Copy action.

Goal: let the operator quickly inspect what the active app's accessibility
tree looked like at the moment an event fired — without leaving the feed,
without a separate detail screen, and without losing their scroll position.

## About the design files

The files in this bundle are **design references created in HTML/React** —
prototypes showing intended look and behavior, not production code to lift
verbatim. The task is to **recreate these designs in the TrackLocation Android
codebase using its existing UI conventions** (Jetpack Compose Material 3 by
the look of the existing screens), reusing components and tokens already in
the app.

The HTML mock uses inline-style React and a custom design-canvas; ignore those
mechanics. What matters: layout, hierarchy, copy, color tokens, type scale,
spacing, motion intent, and the state machine described below.

## Fidelity

**High-fidelity.** All colors, type sizes, weights, spacing, border radii,
and copy are intentional. Recreate pixel-equivalent on Android using the
project's existing tokens (the design picks from the same palette already in
use across Sessions / List / Track / Observer screens).

## Where this feature lives

- **Surface**: Observer Feed screen (already implemented).
- **Entry path**: Settings → Tools → Observer.
- **Per event card**: new link/text-button placed trailing-bottom inside the
  card, *below* the activity / snippet rows.

## Screens / views

### 1. Event card — "View window content" link

**Placement**
- Inside the existing event card, trailing-aligned (right edge), one row
  below the activity name (and below the snippet quote box if present).
- Vertical hit target ≥ 44 dp via top/bottom padding.

**Visual**
- Label: `View window content` (preferred). Fallback `View window`.
- Color: brand `greenDark` (`#16a34a` ↔ `T.greenDark` token in the mock).
- Type: Roboto 12.5 sp, weight 600, letter-spacing -0.05.
- Trailing chevron-right icon, 14 dp, current-color, 2.2 stroke.
- No underline; weight + color signal interactivity.

**Visibility rules**
- `hasSnapshot=true` → link visible.
- `hasSnapshot=false` → link **hidden** (preferred — keeps feed scannable).
- Optional disabled state (`No snapshot captured`, ink3 muted, no chevron)
  is shown in artboard B *only* if PM wants list-density parity across cards
  in high-burst captures. Default is to hide.

### 2. Snapshot modal — primary states

A Material 3 **bottom sheet** rising to ~88% of viewport height with a scrim
behind it.

**Anatomy (top → bottom)**

1. **Drag handle** — 36×4 dp, `T.hair` (`#e7eae6`), centered, 4 dp / 8 dp
   padding above/below.
2. **Title row** (padding 0 16 dp 10 dp)
   - Title: `Window content` — Roboto 18 sp / 700 / -0.3 ls, `T.ink`.
   - Subtitle (one line, mono, ellipsize end): `packageName · activityName · lastSeenAt`.
     - `pkg` in `T.ink`, separators in `T.ink4`, rest in `T.ink2`.
     - If `activityName` is missing, collapse to `packageName · lastSeenAt`.
   - Close icon button on the right: 36×36 dp circle, `T.bg` fill,
     `T.hair` 1 dp border, X glyph at 16 dp, 2.2 stroke. Tap = dismiss.
3. **Meta strip** (padding 0 16 dp 12 dp, grid `1.4fr 1fr 1fr [0.7fr]`, 6 dp gap)
   - 3 chips: **Event** (mono, green tone), **First seen** (mono), **Last seen** (mono).
   - 4th chip **Repeat** appears only when `repeatCount > 1` (mono, amber tone,
     value formatted as `×17`); grid reflows to 4 columns.
   - Chip body: 6×10 dp padding, 10 dp radius, 1 dp `T.hair` border. Label
     row: 10 sp / 600 / 0.6 ls uppercase, `T.ink3`. Value row: 12 sp / 600,
     tone-colored, tabular numerals.
4. **Mode toolbar** (padding 8 dp 16 dp 10 dp, 1 dp top border `T.hairSoft`)
   - Segmented control, full-width minus the Copy button. Pill: 32 dp tall,
     `T.bg` fill, 1 dp `T.hair` border, 999 dp radius.
     - Options: `Formatted` (default), `Raw JSON`. Active segment: `T.ink`
       background, white text, 12 sp / 600 / 0.1 ls.
   - **Copy button**, right of the segmented: 32 dp pill, 12 dp horizontal
     padding, 1.5 dp `T.ink` border, white fill, `T.ink` label "Copy", with
     a 14 dp copy glyph. On click → flip to `T.green` fill, white label,
     "Copied" + check glyph, for 1400 ms, then revert.
   - Disabled when content can't be copied (see edge states).
5. **Content area** — fills remaining height, scrollable, `#fbfcfa`
   background in ok state. 1 dp top border `T.hairSoft`.

**Formatted view (default)** — recursive tree:
- Indent: 14 dp per depth.
- Each node renders as a row group:
  - Tree glyph (`▸`, `├`, `└`) in `T.ink4`.
  - Class line: short class name (last segment of FQN) in mono, followed by
    full FQN in parentheses, `T.ink4`. Both 12 sp.
  - `text:` line (if present): label "text: " in `T.greenDark` 500, value
    in `T.ink` 600 inside a pill `rgba(34,197,94,0.10)` with 4 dp horizontal
    padding and 3 dp radius. Quoted with `"`.
  - `desc:` line (if present): label "desc: " in `T.ink3`, value in italic
    `T.ink2`. Quoted with `"`.
- Padding around the whole tree: 10 dp 14 dp 24 dp.

**Raw JSON view** — pretty-printed (`JSON.stringify(snapshot, null, 2)`):
- Two-column row: 32 dp gutter for line numbers (right-aligned, `T.ink4`,
  tabular numerals) + the JSON line.
- Mono 11.5 sp / line-height 1.55, `T.ink` for code.
- Horizontal padding 16 dp; wrap with `word-break: break-all` only for
  pathologically long values, otherwise preserve whitespace.

**Copy action**
- Always copies the content currently visible in the active mode.
- Formatted mode → copies a plain-text rendering of the tree (one node per
  line, depth indented with two spaces, prefixed with class name; text and
  desc on their own indented lines). On Android, use Compose's
  `LocalClipboardManager` / `ClipboardManager.setPrimaryClip`.
- Raw mode → copies the raw JSON string as-is.
- Show the "Copied" confirmation in the button (~1.4 s).

### 3. Snapshot modal — edge states

**Parse / render error**
- Trigger: snapshot is non-empty but JSON parse fails or formatted render
  throws.
- Modal still opens. Header + meta strip unchanged.
- Content area shows an inline message card:
  - 14 dp margin, 16 dp padding, 14 dp radius,
    `rgba(220,38,38,0.10)` background, `rgba(220,38,38,0.22)` 1 dp border.
  - 32×32 dp icon tile (white fill, light red border, alert glyph in `T.red`).
  - Title: `Unable to render snapshot` — 14 sp / 700, `T.red`.
  - Body: 12.5 sp `T.ink2`, lh 1.4 — *"The captured node tree could not be
    parsed. The raw payload is still available — switch to Raw JSON to copy
    it."*
  - Hint chip below: mono 11 sp `T.ink3` in a `1 dp dashed T.hair` pill on
    white — surfaces the parser error message verbatim.
- Formatted tab: Copy disabled.
- Raw JSON tab: re-enables Copy so the raw payload can be exported (truncate
  display to ~280 chars + `…truncated` for sanity — full content still
  copies).

**No readable text**
- Trigger: snapshot parses successfully but contains no `text` or
  `contentDescription` anywhere in the tree.
- Same inline message card pattern, neutral tone (no red).
- Title: `No readable text found` — 14 sp / 700 `T.ink2`.
- Body: *"The captured tree contains nodes but no text or
  content-description fields. This commonly happens for canvas-rendered
  surfaces or fully-icon UIs."*
- Copy stays disabled in both modes.

**Missing snapshot** — handled at card level (link hidden); modal is not
opened.

## Interactions & behavior

### Open
- Tap event card's "View window content" link → modal sheet animates up
  from bottom, scrim fades in.
- Compose: `ModalBottomSheet` with `skipPartiallyExpanded = true`, or
  custom sheet at `sheetState.expand()` ≈ 88% height.

### Dismiss
1. Header close (X) tap.
2. System Back press.
3. Tap outside the sheet (scrim tap).
4. Swipe-down on the drag handle (Compose default for `ModalBottomSheet`).

### Mode switch
- Tap a segment of the segmented control → instant content swap (no slide
  animation; the meta strip and header don't change).
- Persist the user's choice **only for the session** — reset to Formatted
  each time the sheet opens.

### Copy
- Tap Copy → write to clipboard → swap button to "Copied" + check (green
  fill) → revert after 1400 ms → show a Toast/Snackbar at the bottom of
  the host screen confirming "Copied to clipboard" (optional but
  recommended for accessibility/TalkBack feedback).

### Scroll
- Content area is the only scrolling region. Header + meta strip + toolbar
  are pinned.
- Don't auto-scroll-into-view on mode swap.

## State management

Required state inside the sheet host:

```kotlin
data class SnapshotSheetState(
    val event: ObserverEvent,           // package, activity, type, firstSeen, lastSeen, repeatCount
    val snapshot: TreeSnapshot?,        // parsed; null if parse error
    val rawJson: String,                // always available, for the Raw view + parse-error fallback
    val parseError: String? = null,     // exception message; null when parsed ok
    val mode: ContentMode = ContentMode.Formatted,
    val copied: Boolean = false,        // transient 1.4s flag
)

enum class ContentMode { Formatted, Raw }

// derived:
val hasReadableText: Boolean
    get() = snapshot != null && snapshot.anyNodeHas { it.text != null || it.contentDesc != null }

val canCopy: Boolean
    get() = when {
        parseError != null -> mode == ContentMode.Raw     // raw fallback
        !hasReadableText   -> false                       // empty
        else               -> true
    }
```

State transitions:
- Sheet opens with `mode = Formatted, copied = false`.
- `onModeChange(m)` → update `mode`. No data fetch.
- `onCopy()` → write to clipboard based on `mode`, set `copied = true`,
  launch coroutine to flip back after 1400 ms.
- `onDismiss()` → unmount sheet, drop state.

Snapshot data: events already carry `treeSnapshot` (per the brief). On
sheet open, attempt `Json.decodeFromString<TreeSnapshot>(rawJson)` once and
cache the result + any error in the state.

## Design tokens

Pulled from the existing app palette (already used across Sessions, List,
Track, and the Observer Phase 1 screens):

| Token            | Value                | Usage                                 |
|------------------|----------------------|---------------------------------------|
| `T.bg`           | `#f1f4f0`            | App bg, chip-bg neutral, close button |
| `T.card`         | `#ffffff`            | Sheet surface, list card              |
| `T.hair`         | `#e7eae6`            | 1 dp borders                          |
| `T.hairSoft`     | `#eef0ec`            | Internal dividers                     |
| `T.ink`          | `#0a0a0a`            | Primary text                          |
| `T.ink2`         | `#737373`            | Secondary text                        |
| `T.ink3`         | `#a3a3a3`            | Tertiary text                         |
| `T.ink4`         | `#cdd1cb`            | Quaternary text / tree glyphs         |
| `T.green`        | `#22c55e`            | Active capture, copied confirmation   |
| `T.greenDark`    | `#16a34a`            | Link color, ok-tone text              |
| `T.red`          | `#dc2626`            | Errors                                |
| `OBS.warnBg`     | `rgba(217,119,6,.10)`| Repeat-count chip                     |
| `OBS.errBg`      | `rgba(220,38,38,.10)`| Parse-error message bg                |
| Green text pill  | `rgba(34,197,94,.10)`| Highlight for tree `text:` values     |

**Type**
- Sans: Roboto. Sizes used: 11 / 11.5 / 12 / 12.5 / 13 / 14 / 18 sp.
- Mono: Roboto Mono. Same scale; `letter-spacing: -0.1` on dense rows.

**Spacing scale**: 4 / 6 / 8 / 10 / 12 / 14 / 16 / 22 dp.

**Radii**
- Sheet top corners: 22 dp.
- Pills (segmented, copy button): 999 dp.
- Cards/chips: 10–14 dp.
- Icon tiles: 8 dp.

**Motion**
- Sheet open/close: standard Compose `ModalBottomSheet` motion.
- Copy success: instant swap + revert after 1400 ms.
- Mode swap: no transition (immediate).

## Assets

No raster assets. All icons are stroked SVGs in the mock — implement with
Compose `Icon(painter = rememberVectorPainter(...))` or `Material Icons`:
- Close → `Icons.Outlined.Close`
- Copy → `Icons.Outlined.ContentCopy`
- Check (copied) → `Icons.Outlined.Check`
- Chevron-right (link affordance) → `Icons.Outlined.ChevronRight`
- Alert (parse error) → `Icons.Outlined.ErrorOutline`
- Empty (no text) → `Icons.Outlined.TextSnippet` or `Icons.Outlined.Description`

## Open questions / assumptions

1. **Tree shape.** Mock assumes nodes are `{cls, id?, text?, desc?, children[]}`.
   If the actual `treeSnapshot` is a flat list of text-lines, swap the
   recursive tree renderer for a flat list using the same row styling for
   `text:` / `desc:` — metadata strip and toolbar stay identical.
2. **Truncation.** Snapshots are bounded server-side. If a capture is
   truncated, the design suggests a small mono badge after the title
   (e.g. `truncated · 47 KB / 64 KB`). Not implemented in the mock — need
   the size + cap from eng to render it correctly.
3. **Subtitle ellipsize.** Single mono line, ellipsize from the right.
   Long packages keep the front visible; activity falls off first, then
   `lastSeen` — acceptable because `lastSeen` is repeated in the meta strip.
4. **Copy label.** Stays "Copy" regardless of mode — the segmented control
   already communicates what will be copied.
5. **TalkBack.** Sheet should announce title on open, link reads as
   "View window content, button".

## Files in this bundle

- `Observer Snapshot Viewer.html` — the design canvas. Open in a browser
  to see all artboards (cards, modal states, edge cases, notes).
- `sheet-snapshot.jsx` — the modal sheet React component (reference impl).
- `screen-observer-snap.jsx` — Observer Feed screen with the new link.
- `screen-observer-feed.jsx` — base feed (already-shipped Phase 1).
- `obs-shared.jsx` — Observer-specific tokens + StateChip + EventRow base.
- `cr1-shared.jsx` — app-wide tokens (`T`), StatusBar, BottomNav.
- `design-canvas.jsx` — canvas wrapper (ignore — design-tool plumbing).
