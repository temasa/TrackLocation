---
name: UI-SPEC.md
path: docs/UI-SPEC.md
description: UI Specification — TrackLocation (screens, design system, flows)
---

# UI Specification
## TrackLocation

**Document Version:** 0.5
**Status:** Active (migrated from product-spec.md, DESIGN_SYSTEM.md, CR-0002 UI spec)
**Last Updated:** 2026-07-07
**Owner:** Product Manager / UX Designer
**Controlled By:** `docs/DOCUMENT-CONTROL.md`
**Design Tool:** Google Stitch / Claude Design (external handoff). See `docs/WORKFLOW.md §3`.

> Migrated 2026-06-15 from `docs/product-spec.md` (screen status, nav, UI direction), `docs/DESIGN_SYSTEM.md` (Kinetic Precision tokens), and the CR-0002 UI specification.

---

## 1. UI Principles

Use Jetpack Compose + Material 3. The UI should feel **operational, calm, technical, compact, readable**, and suitable for repeated driver/support usage.

Avoid: marketing-style hero sections, decorative gradients, oversized nested cards, playful visual language, and controls that rely only on color for state.

---

## 2. Navigation & Screen Inventory

### Bottom Navigation

```text
Session / List / Track / Settings
```

- **Session** — always-recorded ON-to-OFF location sessions + always-recording status/control (app start tab).
- **List** — explicit trip history only (no always-recording switch after CR-0002).
- **Track** — start/stop explicit trip ranges.
- **Settings** — unified operational settings; hosts Tools (Observer, OBD).

Observer navigation is **Option B** (accepted 2026-05-18): Observer lives under `Settings → Tools → Observer`; bottom nav stays four tabs. (Alternatives A/C recorded in the ADR.)

### Screen Status

| Screen | Status | Notes |
|---|---|---|
| Session | Current | First-class destination; sessions + always-recording switch + ObdStatusCard |
| List | Current | Trip list only; no always-recording switch after CR-0002; restyled 2026-07-07 for Sessions visual parity (UI-SPEC §3b) |
| Track | Current | Starts/stops explicit trip ranges; glass panel + brand-green CTA; OBD km/L row |
| Settings | Current | Unified operational settings; GENERAL / TOOLS / ABOUT |
| Observer Feed | Current (P1) | `Settings → Tools → Observer`; feed, allowlist overlay, snapshot viewer |
| OBD Settings | Current (P1) | `Settings → Tools → OBD` |
| Observer Event Detail / JSON Viewer | Current | via `SnapshotViewerSheet` (formatted + raw JSON, copy) |
| Registration / Auth Overlay | Planned | Observer/auth phase |

### Core User Flow

```text
Open app → Session tab (toggle always-recording ON)
→ Track tab (Start trip) → drive → Stop trip
→ List tab (review trips) → tap trip → detail path from canonical log
→ Settings → Tools → OBD / Observer for telemetry & diagnostics
```

---

## 3. Always-recording Switch (CR-0002)

Affected screen: **Session**. Component: **Always-recording switch**, placed inside the Session always-recording status area (trailing/right-side), visually connected to the status card. Do not add a nav item; do not place the switch on List.

Required states:

| State | Required UI |
|---|---|
| OFF / inactive | Switch OFF; status `Inactive`; sessions list visible |
| ON / active | Switch ON; status `Active`; active session appears if available |
| Permission required | Turning ON triggers permission flow |
| Permission denied | Switch remains/returns OFF; show helper/snackbar |
| Active trip guard | Switch remains ON; OFF blocked; explain recording is required |
| Auto-started by trip | Switch ON; status Active; no separate visual state |

Recommended copy: Title `Always-recording`; ON `Active` / OFF `Inactive`; ON helper `Recording location sessions in the background.`; OFF helper `Location sessions are not being recorded.`; Permission helper `Location permission is required to start always-recording.`; Guard helper `Always-recording is required while a trip is running.`

---

## 3a. Active Trip Row (List screen — live in-progress trip)

**Decision (2026-07-07):** Mirror the Sessions tab's live-active behaviour on the List/Trips screen. Because there is **no live trip DB row** (a `track` row is written only at Stop — see IMPLEMENTATION-ISSUES #1), the in-progress trip is rendered as a **synthesized live row** derived from `TrackingService.locationUiState`, prepended to "Recent trips" while `isTracking`. On Stop it disappears and the newly-saved finished `Track #N` row takes its place.

**Layout decision:** Keep the existing "Current trip" card (it holds the Start/Stop CTA); ADD the live row below the "Recent trips" header, above the newest finished row. (Alternative "remove the card, live row only" was considered and rejected to keep the Start/Stop control in place.)

**Visual (reuses existing language — no new visual design):** adapts `TrackItemRow` with the Sessions active-row accent:
- Card: `TripSurface`, rounded 18dp, **green border `TripGreen` ~1.5dp** (active accent); same 4-stat footer as a finished row.
- Title: `Trip in progress` (instead of `Track #N`).
- Trailing: **ACTIVE badge** — pulsing green dot + `ACTIVE` label (same treatment as the Sessions `ActiveBadge`).
- **Two stat rows** (OBD-connected): a **base row** — `distance (km)` from `distanceInMeters`, `duration` from `now - tripStartedAt`, `avg speed` = km ÷ elapsed-hours — and an **OBD row** — `instant km/L` (from `ObdUiState.Connected.instantKmL`; `—` at rest / poor fix, gated like the Session card), `L/h` (`fuelRateLph`), `trip avg km/L` (`tripAvgKmL`), and `cost` (`Rp`, tap to edit — see §4). Non-connected: OBD-row values render `—`. The card grows taller to fit the second row. This mirrors the Session OBD card's instant/average separation for a seamless cross-tab experience.
- Not clickable; no long-press delete (not a saved trip yet).
- Reduced-motion: badge pulse uses the same infinite-transition pattern already present on this screen.

**Empty-state rule:** show `EmptyTripsCard` only when there are no finished trips **AND** no active trip; while a trip is active the live row stands in.

**Screenshots if externalised to Claude Design:** List/Trips screen (Recent trips + Current trip card) and the Sessions active row (ACTIVE badge + green border) as reference. No new visual language is introduced (reuses Sessions active-row + `TrackItemRow`), so no external design round-trip is required.

---

## 3b. Trips screen — Sessions visual parity (2026-07-07)

**Decision:** The List/Trips screen is restyled to match the Sessions tab so switching tabs feels continuous. Active-trip data stays as the **first row of the list** (§3a) — no separate live card is added ("list row only" chosen over a duplicate info card).

**Changes:**
1. **Header** — "Trip Tracker" / **Trips** at 40sp ExtraBold, matching Sessions' "Sessions" header (was 36sp Bold).
2. **Current-trip hero card** — keep the Start/Stop CTA and function; align corner radius, padding, and spacing to Sessions' card rhythm.
3. **Remove** the metrics row (Trips / Distance / Hours) and the empty search bar. *(Both were code-only, never spec'd; removed for parity with Sessions' header → card → list structure.)*
4. **"Recent trips / Newest first"** list header — align typography to Sessions' "Recorded sessions" header.
5. **Recent-trip rows** (`TrackItemRow`) — add a 4th stat, **efficiency**: `distance(km) ÷ obdFuelConsumedL` km/L, from the existing `TrackEntity.obdFuelConsumedL`; shows `"—"` when `obdFuelConsumedL = 0` (trip recorded without OBD).
6. **Recent-trip rows — cost stat (ADR-008):** add a 5th stat, **cost** (`Rp`) = `obdFuelConsumedL × price effective at the trip's start` (from the `fuel_price` effective-dated log); shows `—` when `obdFuelConsumedL = 0` or no price was in effect at the trip's start. Final completed-row stats: km / duration / avg speed / average km/L / cost.
7. **Recent-trip rows — two-row metric grid (2026-07-08 fix):** the five stats no longer share a single row. Packing five equal-width columns clipped the values on-device (cost showed only `Rp`, avg speed lost its `h`, duration lost its last digit). `TrackItemRow` now mirrors `ActiveTripRow`'s two-row grid (card height 124→156dp): **base row** = km / duration / avg speed; **fuel row** = average km/L / cost (an empty third cell keeps the columns aligned with the base row). Stat **values** use `MonospaceFontFamily` (tabular digits, ideal for tight numeric columns) at 14sp / Medium — smaller and lighter than the previous 15sp / Bold — with `-0.3sp` tracking and `Ellipsis` overflow; **labels** at 10sp. Data was always persisted correctly; this was a purely visual fix.

**Data:** No schema change for §3b's efficiency stat; the cost stat (ADR-008) reads the new `fuel_price` table (migration v8→v9).

**Design handoff:** Reuses the established Sessions/`TrackItemRow` visual language (no new visual design), so no external Claude Design round-trip is required. If externalised: attach the current Trips screen + Sessions screen as reference.

---

## 4. OBD UI Surfaces

### Phase 1 (implemented)
- **OBD Settings** (`Settings → Tools → OBD`): Enable toggle, saved-device row + Change, "Pair a new device" (system BT), bonded-device picker, poll-rate (1/2/5 Hz), retention (1–30d), retry cap, engine displacement (default 1193 cc), status display (IDLE/CONNECTING/CONNECTED/RETRYING/WAITING + last error + last sample ts), Reconnect button.
- **ObdStatusCard** (Session screen): Connected/Waiting states, RPM/SPEED/EFFICIENCY, instant km/L, fuel-source chip, Reconnect.
- **OBD metric row** (TripPanel): instant km/L cell, fuel-source chip.

### Phase 2 — Fuel-economy metrics (revised per ADR-007)
Instant fuel economy is shown as **two always-on cells** (no unit toggling), on **both** the Session `ObdStatusCard` and the `TripPanel`:
- **km/L** — shown only when moving (speed > ~3 km/h, good fix); shows `—` at rest.
- **L/h** — always shown when OBD is connected (current fuel rate); `—` when OBD disconnected / engine off (RPM = 0).

Averages remain a **single km/L** per surface (SESSION AVG on the Session card, TRIP AVG on the Trip panel):
- `avg km/L = displayed displacement distance ÷ fuel` (unified session/trip derivation — ADR-007).
- Shows a value once distance > 0.01 km and fuel > 0, else `—`.
- While idling the average **degrades** (fuel keeps accruing, distance flat) — intended.

#### Fuel cost (Rp) — Session OBD card + Trips active-trip row + completed trips

- **Value:** `litres × price`, formatted `Rp` with a dot thousands separator and no decimals (e.g. `Rp 12.500`). Shows `—` when litres = 0 or the price is unset.
- **Placement:** Session OBD card — a COST cell below the fuel metrics (below OBD Status). Trips active-trip row — an added COST stat (row goes from 4 to 5 stats; keep the existing stat styling, reflow density handled in code).
- **Tap to edit:** tapping the cost opens a compact numeric `Rp` price editor with **Save (✓)** = apply and **Cancel (✗)** = discard the in-progress edit. Saving writes the shared current price and both surfaces update.
- **Undo / redo:** `↶` reverts to the previous price, `↷` re-applies the undone price; full multi-step within the session; each control is disabled when there is nothing to undo/redo. History is in-memory and resets on app restart; the current price persists.
- **Completed trips (ADR-008):** each completed-trip row also shows cost, priced by the `fuel_price` row effective at the trip's **start**; editing the price later never re-costs finished trips (`—` if no price applied then).
- **Price model:** the price is an effective-dated entity (`fuel_price` table), not a scalar; Save/Undo/Redo append effective-now rows. Current price = latest row.

---

## 5. Design System — "Kinetic Precision"

Summarized here; the full machine-readable token export (colors, typography, rounded, spacing) is preserved verbatim in **§9 Legacy / Token Export** below (migrated from the retired `docs/DESIGN_SYSTEM.md`).

- **Theme:** dark-forest surface (`#0e150e`), primary green (`#22C55E` active/running), secondary purple (`#A855F7` toggles/triggers), error red. High-contrast utility; color is never the sole state signal.
- **Typography:** Hanken Grotesk; heavy weights (700–800) for titles/primary numbers; tabular figures for time/distance.
- **Shape:** rounded — `rounded-xl` (1.5rem) for main cards, `rounded-md/lg` for controls, full pill for status chips.
- **Spacing:** 20px (1.25rem) global horizontal safe-zone; 16px card gaps; min 72px settings rows; bottom-anchored floating Track card.
- **Elevation:** tonal layers over shadows; hero status card uses dark forest background to pop.
- **Components:** Status Card (hero), SettingsRows (icon + title/support + trailing value/chevron), Session list items (left route-line), minimalist map with floating circular controls, white bottom nav with green active pill.

---

## 6. Responsive Design & Accessibility

- Mobile-first; fluid grid; touch targets sized for in-motion use (≥48dp goal; some legacy cards at 44dp — see task log).
- Reduced-motion aware (animations gated on `ANIMATOR_DURATION_SCALE == 0`).
- Status communicated via text + icon, not color alone; light + dark usable.
- TalkBack live-region announcements on trip/session state changes.

---

## 7. Current UI Status

- Session/List/Track/Settings: implemented and verified.
- Observer Phase 1 UI: implemented, verified on device (2026-05-29).
- Observer Phase 2: snapshot viewer + pagination done; truncation banner spec drafted, awaiting design approval.
- OBD Phase 1 UI: implemented (Settings, ObdStatusCard, TripPanel row); km/L logic verified live.

---

## 8. Design References

Existing handoff artifacts retained on disk:

- `docs/design/design_handoff_cr1_sessions/`
- `docs/design/design_handoff_observer_phase1/`
- `docs/design/design_handoff_observer_row/`
- `docs/design/design_handoff_observer_snapshot/`
- `docs/design/design_handoff_track_screen/`
- `docs/design-handoff/obd_phase1/OBD_PHASE1_SPEC.md`
- `docs/design-handoff/observer_truncation/OBSERVER_TRUNCATION_SPEC.md`

Design tool: Google Stitch / Claude Design (record new project/file URLs here as they are created).

---

## Reference Documents

- `docs/PRD.md` — Product requirements
- `docs/IMPLEMENTATION-PLAN.md` — Implementation plan and task log
- `docs/DOCUMENT-CONTROL.md` — Version register

---

## 9. Legacy / Token Export (migrated verbatim from docs/DESIGN_SYSTEM.md)

The "Kinetic Precision" design token export, preserved verbatim from the retired `docs/DESIGN_SYSTEM.md`. Treat this as the machine-readable source for theme tokens.

<!-- BEGIN DESIGN_SYSTEM.md (verbatim) -->
```
---
name: Kinetic Precision
colors:
  surface: '#0e150e'
  surface-dim: '#0e150e'
  surface-bright: '#333b33'
  surface-container-lowest: '#091009'
  surface-container-low: '#161d16'
  surface-container: '#1a221a'
  surface-container-high: '#242c24'
  surface-container-highest: '#2f372e'
  on-surface: '#dce5d9'
  on-surface-variant: '#bccbb9'
  inverse-surface: '#dce5d9'
  inverse-on-surface: '#2a322a'
  outline: '#869585'
  outline-variant: '#3d4a3d'
  surface-tint: '#4ae176'
  primary: '#4be277'
  on-primary: '#003915'
  primary-container: '#22c55e'
  on-primary-container: '#004b1e'
  inverse-primary: '#006e2f'
  secondary: '#ddb7ff'
  on-secondary: '#490080'
  secondary-container: '#6f00be'
  on-secondary-container: '#d6a9ff'
  tertiary: '#95d4ba'
  on-tertiary: '#003829'
  tertiary-container: '#7ab8a0'
  on-tertiary-container: '#004937'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6bff8f'
  primary-fixed-dim: '#4ae176'
  on-primary-fixed: '#002109'
  on-primary-fixed-variant: '#005321'
  secondary-fixed: '#f0dbff'
  secondary-fixed-dim: '#ddb7ff'
  on-secondary-fixed: '#2c0051'
  on-secondary-fixed-variant: '#6900b3'
  tertiary-fixed: '#b0f0d6'
  tertiary-fixed-dim: '#95d3ba'
  on-tertiary-fixed: '#002117'
  on-tertiary-fixed-variant: '#0b513d'
  background: '#0e150e'
  on-background: '#dce5d9'
  surface-variant: '#2f372e'
typography:
  display-lg:
    fontFamily: Hanken Grotesk
    fontSize: 48px
    fontWeight: '800'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '500'
    lineHeight: 24px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-caps:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '600'
    lineHeight: 16px
    letterSpacing: 0.08em
  support-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '400'
    lineHeight: 16px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-padding: 1.25rem
  stack-gap-sm: 0.5rem
  stack-gap-md: 1rem
  stack-gap-lg: 1.5rem
  section-margin: 2rem
---

## Brand & Style

The design system is engineered for a high-performance utility application, balancing technical precision with effortless legibility. The aesthetic is **Modern Corporate** with a strong emphasis on **Functional Minimalism**. It aims to evoke a sense of reliability and real-time responsiveness.

Key characteristics include:
- **High-Contrast Utility:** Prioritizing rapid information retrieval through bold typography and stark tonal shifts.
- **Systematic Clarity:** Using a structured grid and consistent iconography to guide the user through complex data sets.
- **Action-Oriented Accents:** Utilizing a vibrant, saturated palette for state changes and active tracking to differentiate between "monitoring" and "recording" modes.

## Colors

The color strategy uses a light-gray foundation for the overall application frame to reduce glare while maintaining high contrast. 

- **Primary Green (#22C55E):** Used strictly for "Active" or "Running" states, indicators, and success confirmations.
- **Secondary Purple (#A855F7):** Reserved for toggles and specific interactive triggers to provide a clear visual departure from the status-driven green.
- **Deep Forest (#064E3B):** A specialized surface color for "Hero" status cards, providing a dark-mode container that anchors the main dashboard.
- **Neutral Scale:** Uses a precise range of cool grays to differentiate between primary labels (Black) and metadata/support text (Gray-500).

## Typography

The system utilizes **Hanken Grotesk** for its technical yet approachable grotesque qualities. 

- **Weighting:** Heavy weights (700-800) are used for screen titles and primary status numbers to ensure they are the first things a user sees.
- **Numerical Data:** Tabular figures are preferred for time and distance tracking to prevent "jumping" layouts during active updates.
- **Hierarchy:** Secondary metadata (like "duration" or "distance" labels under numbers) uses a smaller, lighter weight with increased line height to maintain a clean appearance.

## Layout & Spacing

This design system utilizes a **Fluid Grid** model optimized for mobile-first interactions.

- **Safe Zones:** A 20px (1.25rem) horizontal margin is maintained globally for all container elements.
- **Card Spacing:** Lists of recorded sessions use a 16px vertical gap.
- **Map Overlays:** The primary tracking interface uses a bottom-anchored persistent card. This card sits at a 16px margin from the bottom navigation and side edges to feel "floating" yet docked.
- **Information Density:** Settings and lists use a generous vertical height (min 72px for rows) to ensure high touch-accuracy during movement.

## Elevation & Depth

Depth is primarily communicated through **Tonal Layers** rather than heavy shadows.

- **Base Layer:** The global background is a very light gray (#F8FAF9).
- **Surface Layer:** White cards (#FFFFFF) sit on top of the base with a subtle, 2px stroke or a very soft, high-blur shadow (8% opacity) to provide separation.
- **Hero Elevation:** The Status Card uses a high-contrast dark background (#064E3B) to visually "pop" forward from the light background.
- **Interactive Elements:** Buttons and toggles use color fills rather than elevation to signify state.

## Shapes

The shape language is consistently **Rounded**, creating a modern and friendly feel for a technical tool.

- **Main Containers:** Large cards and the primary status overlay use a 1.5rem (`rounded-xl`) corner radius.
- **Interactive Components:** Toggles, secondary buttons, and icon containers use a 0.5rem (`rounded-md`) to 1rem (`rounded-lg`) radius.
- **Status Indicators:** Small "Live" or "Active" chips utilize a fully rounded pill shape (999px) for immediate recognition as a status tag.

## Components

### Status Card (Hero)
The primary dashboard element. It features a dark forest green background with high-contrast white text. Integrated controls (like the Play/Pause button) are placed on the far right for thumb accessibility.

### SettingsRows
Standardized rows for navigation and configuration.
- **Left:** Leading icon in a soft-gray square container.
- **Center:** Bold title with support description text below.
- **Right:** Trailing value (gray text) and a chevron-right icon.
- **Divider:** 1px hairline stroke between grouped items.

### List Items (Sessions)
Cards that encapsulate trip data. They use a vertical "Route Line" on the left margin (Primary Green) to visually link the session to the concept of a path. 

### Map Interface
The map uses a minimalist, light-themed tile provider. Interactive controls (Recenter, Layers) are grouped as floating circular white buttons on the right side of the screen.

### Bottom Navigation
A persistent white bar with a subtle top border. The active state is indicated by a soft green pill-shaped background behind the icon and bolded text labels.
```
<!-- END DESIGN_SYSTEM.md (verbatim) -->

---

**Status:** Active. Migrated from legacy product-spec + DESIGN_SYSTEM + CR-0002 UI spec.
