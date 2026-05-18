# Track Screen UI — Implementation Spec

This document specifies the new Track screen UI for **TrackLocation** (Android · Jetpack Compose · Material 3). It replaces the existing dark-card panel design with a translucent glass overlay matching the rest of the app's green design system.

The bundled `screen-track.jsx` is a high-fidelity visual reference (HTML prototype) — not production code. Recreate the design in Compose using the values in this document.

---

## Summary of changes vs. current implementation

| Current | New |
|---|---|
| Solid dark panel `#0a0a0a` | **Translucent glass panel** with brand-green tint + backdrop blur |
| Purple play button (`#8b5cf6`-ish) | **Brand-green play button** (`#22c55e`), matches Sessions hero indicator |
| "Ready for trip" / 00:00:00 sans-serif headline | **Monospace timer** (JetBrains Mono / Roboto Mono) — clearly reads as elapsed time |
| Emoji icons (🏃 ⚡) for metrics | **Outline SVG icons** matching the design system |
| Single state shown | **Three explicit states**: Ready · Live · Paused |

The map should be **visible through the panel** at all times so the user keeps their sense of place.

---

## Layout

```
┌─────────────────────────────────────────────────┐
│                                                 │
│           [ Map fills entire screen ]           │
│                                                 │
│                                  ┌─┐            │
│                                  │↻│  ← recenter│ Map controls
│                                  └─┘            │ (right side,
│                                  ┌─┐            │  above panel)
│                                  │◇│  ← layers  │
│                                  └─┘            │
│                                                 │
│  ┌───────────────────────────────────────────┐  │
│  │ ● TRIP IN PROGRESS                        │  │ ← Translucent
│  │                                           │  │   glass panel
│  │ 00:23:17                            ▶︎    │  │   (insets 14dp)
│  │ ─────────────────────────────────────     │  │
│  │ 📍 KM            ⚡ KM/HR                │  │
│  │  3.84              11.7                   │  │
│  └───────────────────────────────────────────┘  │
│                                                 │
├─ Bottom nav (Track tab active) ────────────────┤
```

### Panel placement
- Positioned at the bottom of the map area, **above** the bottom nav.
- Inset from the screen edges: **14dp left, right, and bottom**.
- Width: fills remaining horizontal space (no fixed width).

---

## Panel container

| Property | Value |
|---|---|
| Background | `rgba(20, 48, 38, 0.48)` (dark green tint at 48% opacity) |
| Border | `1px solid rgba(255, 255, 255, 0.12)` |
| Border radius | **18dp** |
| Padding | **12dp vertical, 14dp horizontal** |
| Shadow | `0 14dp 36dp rgba(10, 20, 16, 0.28)` |
| **Backdrop blur** | **`blur(28dp) saturate(180%)`** — required for legibility |

> **Compose implementation tip:** Use `Modifier.blur()` on the underlying map / `RenderEffect.createBlurEffect` (API 31+) for the backdrop blur, or composite using `BlurMaskFilter` on lower APIs. For pre-31 fallback, increase the panel opacity to `0.78` to maintain text legibility without blur.

### Three states

The panel reflects one of three trip states. Only the eyebrow text, timer value, status dot, metrics, and CTA glyph change — the container is identical.

| State | Eyebrow text | Status dot | Timer | Distance | Speed | CTA glyph |
|---|---|---|---|---|---|---|
| `READY` | `Ready for trip` | inactive (gray, 45% white, no glow) | `00:00:00` | `0.00` | `0.00` | ▶ (play) |
| `LIVE` | `Trip in progress` | **green `#22c55e`**, pulsing (1.6s ease-in-out infinite), with halo `0 0 0 3dp rgba(34,197,94,.22)` | running e.g. `00:23:17` | running e.g. `3.84` | running e.g. `11.7` | ⏸ (pause) |
| `PAUSED` | `Trip paused` | amber `#f5a524`, no pulse, no halo | frozen e.g. `00:08:42` | frozen e.g. `1.42` | `0.00` | ▶ (resume) |

### Eyebrow row (top of panel)
- Layout: horizontal row, `gap: 7dp`, items vertically centered.
- **Dot**: 7×7dp circle, `border-radius: 4dp`. Color per state (above). Pulse animation = opacity 1 → 0.55 → 1, duration 1.6s, ease-in-out, infinite, applied only in LIVE state.
- **Text**: JetBrains Mono / Roboto Mono, 10sp, weight 600, letter-spacing `1.2sp`, **UPPERCASE**, color `rgba(255,255,255,.78)`.

### Timer + CTA row
- Layout: horizontal row, `space-between`, `gap: 12dp`, items vertically centered.
- Top margin from eyebrow: `4dp`.
- **Timer**:
  - JetBrains Mono / Roboto Mono, **30sp**, weight 600, letter-spacing `-0.8sp`, line-height 1, color `#ffffff`.
  - `font-variant-numeric: tabular-nums` (use a tabular-figures font feature so digits don't shift).
  - Format `HH:MM:SS` (e.g. `00:23:17`).
- **CTA button** (`TripCta`):
  - **48×48dp** circle.
  - Background **`#22c55e`** (brand green).
  - Shadow: `0 8dp 18dp -6dp rgba(34,197,94,.55)` + inner shadow `0 -2dp 0 rgba(0,0,0,.12)` (subtle inset bottom).
  - Border: none.
  - Glyph color: **`#0a2218`** (very dark green — contrasts on the green button).
  - Glyph size: 18×18dp for play, 16×16dp for pause.
  - Glyphs:
    - **Play / Resume**: filled right-pointing triangle, `M 8 5 L 20 12 L 8 19 Z` in a 24×24 viewBox.
    - **Pause**: two rounded rects, `x=6 y=5 w=4.5 h=14 rx=1.2` and `x=13.5 y=5 w=4.5 h=14 rx=1.2`.
  - Content description per state: `"Start trip"`, `"Pause trip"`, `"Resume trip"`.

### Stats row (bottom of panel)
- Top margin: `10dp` from CTA row.
- Padding-top: `9dp`.
- Border-top: `1dp solid rgba(255, 255, 255, 0.12)` (hairline divider).
- Layout: **2-column grid**, `1fr 1fr`, gap `6dp`, items baseline-aligned. Left cell is left-aligned; right cell is **right-aligned**.

#### Metric cell

Two vertically-stacked elements per cell:

1. **Unit row** (top): horizontal flex, gap `5dp`, color `rgba(255,255,255,.78)`.
   - Icon: 14×14dp, outline-style, stroke 2.2dp, stroke-linecap round, stroke-linejoin round.
     - **Distance**: pin glyph — `M12 22s7-7.5 7-13a7 7 0 1 0-14 0c0 5.5 7 13 7 13z` + inner circle `cx=12 cy=9 r=2.4`.
     - **Speed**: lightning bolt — `M13 2 4 14h7l-1 8 9-12h-7l1-8z`.
   - Unit label: Roboto, 10sp, letter-spacing `0.4sp`, **UPPERCASE**. Strings: `"KM"`, `"KM/HR"`.
2. **Value** (bottom):
   - Top margin from unit row: `2dp`.
   - JetBrains Mono / Roboto Mono, **16sp**, weight 600, letter-spacing `-0.3sp`, line-height 1, color `#ffffff`, tabular-nums.
   - Distance format: 2 decimal places (e.g. `3.84`).
   - Speed format: 1 decimal place (e.g. `11.7`).

---

## Map controls (above the panel)

Two floating circular buttons stacked vertically on the right side of the map.

| Property | Value |
|---|---|
| Size | 44×44dp |
| Shape | circle (`border-radius: 22dp`) |
| Background | `rgba(255, 255, 255, 0.92)` |
| Backdrop blur | `blur(10dp)` |
| Shadow | `0 4dp 12dp rgba(0, 0, 0, 0.12)` |
| Icon color | `#0a0a0a` |
| Icon size | 20×20dp, stroke 2 |

Position:
- Right edge: 16dp from screen edge
- Bottom edge: 240dp from screen bottom (clears the panel)
- Gap between the two buttons: 10dp

Buttons:
1. **Recenter** — crosshair glyph (circle + 4 short ticks). Content description: `"Recenter map"`.
2. **Layers** — three stacked diamonds. Content description: `"Map layers"`.

(Both stub for now — Phase 1 of this UI change only needs the visuals; behavior can be wired later.)

---

## Bottom nav

**Unchanged** — keep the existing 4-tab nav (Session · List · Track · Settings) with the Track tab in its active state. The new panel sits **above** the nav with the 14dp inset.

---

## Required state from ViewModel

```kotlin
data class TrackPanelState(
    val tripState: TripState,             // READY | LIVE | PAUSED
    val elapsed: Duration,                // formatted HH:MM:SS
    val distanceKm: Double,               // formatted with 2 decimals
    val speedKmh: Double,                 // formatted with 1 decimal
)

enum class TripState { READY, LIVE, PAUSED }
```

CTA tap behavior:
- `READY` → start trip → transitions to `LIVE`.
- `LIVE` → pause trip → transitions to `PAUSED`.
- `PAUSED` → resume trip → transitions to `LIVE`.

(Behavior is out of scope for this UI change — just wire the click handler to `viewModel.onTripCtaTap()`.)

---

## Accessibility

- Container: `Modifier.semantics { contentDescription = "Trip panel" }` not strictly required if children are labeled.
- Live status announcements (`liveRegion = polite`):
  - On state change to LIVE: `"Trip started"`.
  - On state change to PAUSED: `"Trip paused at {elapsed}"`.
- Timer: `contentDescription = "Elapsed time, $elapsed"` (e.g. `"Elapsed time, 23 minutes 17 seconds"`).
- Metric cells: `contentDescription = "$value $unit"` (e.g. `"3.84 kilometers"`, `"11.7 kilometers per hour"`).
- CTA button: see content descriptions above. Minimum 48×48dp tap target — the visual 48dp circle hits this exactly.
- Color is never the sole signal: state is also conveyed in the eyebrow text.

---

## Design tokens used

| Token | Hex |
|---|---|
| `brand.green` | `#22c55e` |
| `brand.green.dark` (ink-on-green) | `#0a2218` |
| `panel.bg` | `rgba(20, 48, 38, 0.48)` |
| `panel.border` | `rgba(255, 255, 255, 0.12)` |
| `panel.divider` | `rgba(255, 255, 255, 0.12)` |
| `panel.text.primary` | `#ffffff` |
| `panel.text.secondary` | `rgba(255, 255, 255, 0.78)` |
| `panel.text.tertiary` | `rgba(255, 255, 255, 0.45)` |
| `status.paused` | `#f5a524` |
| `map.fab.bg` | `rgba(255, 255, 255, 0.92)` |

Fonts:
- Sans body: **Roboto** (system).
- Monospace numerals: **JetBrains Mono** (preferred) or **Roboto Mono** as fallback. Add to project resources if not already present.

---

## Acceptance checklist

- [ ] Map fills the entire viewport behind the panel; the user can see roads/features through the panel area.
- [ ] Panel uses backdrop blur (or 0.78 opacity fallback on pre-API-31 devices).
- [ ] Panel is inset 14dp from left, right, and bottom.
- [ ] Timer renders in a monospace font with tabular figures (digits don't shift width).
- [ ] CTA button is **48dp** brand-green, with the **dark-green glyph**, in the right side of the timer row.
- [ ] CTA glyph changes between Play / Pause based on state.
- [ ] Three states (READY / LIVE / PAUSED) each render correctly with the right eyebrow text, status dot color, dot pulse, frozen-vs-live numbers, and CTA glyph.
- [ ] Stats row shows two cells: distance (left, left-aligned) and speed (right, right-aligned), separated by a hairline above.
- [ ] No emoji icons — outline SVG icons only.
- [ ] No purple anywhere.
- [ ] Bottom nav is unchanged; Track tab is in its active visual state.
- [ ] Map controls (recenter, layers) float above the panel on the right, with the glassy white treatment.
