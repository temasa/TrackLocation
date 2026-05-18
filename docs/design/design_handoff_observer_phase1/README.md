# Observer Phase 1 — Implementation Handoff

This bundle is the **design package for CR: Observer Phase 1** in the **TrackLocation** Android app (Jetpack Compose + Material 3).

It contains a clickable HTML prototype + a detailed UI specification document.

> **About these files.** The `.html` and `.jsx` files are **design references** — prototypes that demonstrate the intended look and behavior in a browser. They are **not production code**. Your task is to recreate them in the TrackLocation codebase using **Jetpack Compose + Material 3** and the project's existing theme tokens (colors, type scale, spacing). Lift exact values from `OBSERVER_PHASE1_SPEC.md` — that document is authoritative.

## Fidelity

**High-fidelity.** Final colors, typography, spacing, corner radii, motion durations, and interaction rules are all locked. Recreate pixel-for-pixel.

## What's in this bundle

| File | Purpose | When to read |
|---|---|---|
| **`OBSERVER_PHASE1_SPEC.md`** | The authoritative implementation spec — screen-by-screen layout, every component spec, all state transitions, design tokens, motion specs, accessibility, and an end-of-doc checklist. | **Always read first and last.** |
| `Observer Phase 1.html` | Live clickable prototype showing all 8 artboards on a pan/zoom canvas. Open in a browser. | When you need to *see* a layout the spec describes. |
| `observer-shared.jsx` | Shared tokens (`OB`), small primitives: status bar, app bar, page title, status chip, service banner, bottom nav, phone shell. | When implementing reusable Compose components — read for visual precision. |
| `screen-settings.jsx` | Settings screen with the new `Tools → Observer` row. | Settings screen pass. |
| `screen-observer.jsx` | Observer Feed screen — 5 state variants in one component (`running`, `service-disabled`, `capture-paused`, `autoscroll-paused`, `empty`). Includes `EventRow`, `EventTypeChip`, empty state, transient jump-to-latest FAB. | Observer Feed pass. |
| `sheet-allowlist.jsx` | Allowlist modal bottom sheet — 3 state variants (`empty`, `rules-clean`, `rules-draft`). Includes `RuleRow`. | Allowlist pass. |
| `design-canvas.jsx` | Pan/zoom canvas component used only by the HTML preview. **Not part of the implementation.** | Ignore. |

## How to use this bundle with Claude Code

### 1. Drop the bundle into your Android repo

```bash
unzip design_handoff_observer_phase1.zip -d docs/design/
# Result: docs/design/design_handoff_observer_phase1/
```

(Or put it anywhere accessible — `docs/design/`, `art/specs/`, etc. The exact location doesn't matter; just keep all files together.)

### 2. Optional but recommended: preview the design

Open `Observer Phase 1.html` in any browser to see the live mockups before you start coding. Use the canvas's pan/zoom and "focus" overlay to inspect any single artboard fullscreen.

### 3. Suggested Claude Code prompt

Paste this into Claude Code at the repo root:

> Read `docs/design/design_handoff_observer_phase1/OBSERVER_PHASE1_SPEC.md` end-to-end before writing any code. It is the authoritative spec for **Observer Phase 1**, a new feature in this Jetpack Compose + Material 3 app.
>
> Scope of this change:
> 1. **Settings screen** — replace the current placeholder with the new layout (page title + section groups + rows). Add a **Tools** section with one row labeled **"Observer"** that navigates to the Observer Feed screen.
> 2. **Observer Feed screen** — a new top-level destination reachable only from `Settings → Tools → Observer`. It is **not** a bottom-nav tab. The bottom nav stays at Session / List / Track / Settings.
> 3. **Allowlist modal bottom sheet** — a new sheet opened from the Observer Feed's top-app-bar trailing icon.
>
> Implementation rules:
> - Use Jetpack Compose with Material 3 components. Pull colors and type from the existing project theme; extend the theme with any new tokens listed in the spec's "Visual tokens" section.
> - Reuse existing icons/components where they match. Map line-icon glyphs in the spec to Material Symbols (the spec lists each glyph's Material name in parentheses).
> - Capture state must persist across process restarts (DataStore is fine). Auto-scroll state is in-memory only.
> - The `.html` / `.jsx` files are visual references — do not copy DOM markup. Look at them to disambiguate spacing/visuals when the spec is unclear.
>
> Deliverable shape:
> - Add a `feature/observer` module (or package — match the project's convention).
> - Wire navigation: `Settings → Observer Feed`. Allowlist is a `ModalBottomSheet` over the feed.
> - Persist capture state. Stub the events repository if the accessibility-event capture pipeline doesn't exist yet — return a fake stream so the UI is testable end-to-end.
>
> Before you start, list which files you plan to create/edit and which existing files (theme, navigation graph, settings screen) you need to read. Then implement.
>
> When finished, walk through the **Implementation checklist** at the end of `OBSERVER_PHASE1_SPEC.md` and confirm each item.

### 4. Iterating after the first pass

When you (or Claude Code) want a specific tweak, point Claude back at a specific section of the spec or a specific artboard label, e.g.:
- *"Adjust the Observer Feed event row alternating background — see `OBSERVER_PHASE1_SPEC.md` § "Event row" — odd rows should use #f8faf7."*
- *"Match artboard D (auto-scroll paused) for the jump-to-latest FAB position and timing."*

## Decisions worth flagging (vs. previous designs)

- **Observer breaks the dark-green hero pattern.** Sessions/List use a big dark green hero card. Observer does NOT — the spec calls for "compact operational look, technical and calm, no decorative gradients, no hero marketing sections." Instead Observer uses a compact 3-indicator status block + a dense Roboto Mono event log. The rest of the visual language (palette, nav, radii, type scale) matches the rest of the app.
- **Three status indicators are physically separated** — Service gets a full-width banner (because the disabled state needs an action button), Capture + Auto-scroll are 50/50 chips below. Each shows **icon + text**, never color-only.
- **Auto-scroll chip is display-only.** The canonical toggle is *tap-in-feed* per the spec. Showing it as a tappable chip would create two ways to do the same thing.
- **Allowlist entry point:** funnel icon in Observer's top app bar, with a small amber dot when draft rules are pending — so the user knows even after closing the sheet.
- **Long-press copy on event rows** is gated to `auto-scroll = Paused` per spec, and copies `"package | activity"` (package first, best-effort).
- **No history-clearing UI** anywhere, per spec.

## Acceptance

The end-of-document **Implementation checklist** in `OBSERVER_PHASE1_SPEC.md` is the acceptance criteria. Treat it as the test plan for code review.
