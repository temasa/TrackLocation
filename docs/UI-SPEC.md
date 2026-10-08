---
name: UI-SPEC.md
path: docs/UI-SPEC.md
description: UI Specification — TrackLocation (screens, design system, flows)
---

# UI Specification
## TrackLocation

**Document Version:** 0.24
**Status:** Active (migrated from product-spec.md, DESIGN_SYSTEM.md, CR-0002 UI spec)
**Last Updated:** 2026-10-08
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
| Observer Event Detail / JSON Viewer | Current | via `SnapshotViewerSheet` (formatted + raw JSON, copy, prev/next event nav) |
| Gojek Order Card (Track screen) + compact trip strip | Planned | Replaces `TripPanel` on the Track screen when a Gojek order has full info; displays the card with a compact trip strip underneath (elapsed, distance, COST/NET, AVG/INST km/L) while the trip is live (ADR-014, ADR-015); design handoff pending (AGENTS.md §12). Screenshots to attach: Track screen with map + `TripPanel` (all 3 states), bottom nav, a Session/List card for glass-panel style |
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
8. **Header fuel-price affordance (2026-07-08):** the `ListHeader` "Trips" row gains a trailing tappable **fuel-pump icon** (right-aligned, same row as the 40sp label). Tapping opens the existing `FuelCostEditorDialog` (§4). This is the **only persistent entry point** to set/edit the fuel price on the Trips screen — previously the editor was reachable only via the active-trip row's COST cell, which is absent when no trip is running. Icon-only (no price text); rendered from a new `res/drawable` vector (no Compose-1.2 extended-icons dependency).
9. **Completed-trip cost read-out (2026-07-08):** tapping the **cost** cell on a completed `TrackItemRow` shows a **Toast** with the historic fuel price applied to that trip — the `fuel_price` row effective at the trip's **start** (`priceEffectiveAt(timestamp)`), price-per-litre only (e.g. "Track #12 fuel price: Rp 12.500 / litre"). **Read-only per ADR-008** — never opens the editable `FuelCostEditorDialog`, so finished trips are never re-costed. When no price was in effect at the trip's start (cost `—`), the Toast reads "No fuel price recorded for this trip." The cost cell's tap is independent of the row's tap-to-open-detail / long-press-to-delete.
10. **Completed-trip cost color alternation (2026-07-08, cosmetic):** on the completed `TrackItemRow` list, the **cost value** alternates between two theme colors — `TripInk` (#0A0A0A) and `TripGreen` (#16A34A) — toggling each time a row's cost differs from the row **above** it. Equal-cost runs (including consecutive `—`) share one color; the color flips at each change boundary, as a visual cue that the applied cost changed between adjacent trips. Purely cosmetic — no data or ordering change; only the cost value's text color is affected (labels and every other stat unchanged).

**Data:** No schema change for §3b's efficiency stat; the cost stat (ADR-008) reads the new `fuel_price` table (migration v8→v9).

**Design handoff:** Reuses the established Sessions/`TrackItemRow` visual language (no new visual design), so no external Claude Design round-trip is required. If externalised: attach the current Trips screen + Sessions screen as reference.

---

## 3b-order. Trips List — Order-Trip Rows (ADR-022)

**What it does:** When a trip is tied to a Gojek order (auto-started by ADR-015, labeled/tracked by ADR-022), the completed trip row in the Trips list displays the order label and a compact price/net cell, allowing the driver to quickly identify order-linked work at a glance.

**Display in TrackItemRow (completed trip, order-linked):**

**Label:** Appears as part of the row title or subtitle (implementation detail TBD in code; placement might be below "Track #N" or replacing the generic title): "Gojek: <pickupName> → <dropName>" (e.g., "Gojek: Senayan → Blok S"). Names are extracted location names only (no full addresses; no customer name/phone). If a name exceeds ~25 characters, truncate in the UI row with an ellipsis (e.g., "Gojek: PT Multi-Pratama M… → …"). The label is a snapshot captured at trip save time; it survives `observer_trip` row pruning (90d/5k retention, ADR-013).

**Price / Net cell:** Appears in the row's second metric row (where completed trips show avg km/L / cost, §3b #7) or as a dedicated cell. Shows two values separated by " / ":
- First: `price = orderEarningsRp` (earnings from the order at completion).
- Second: `net = price − fuelCost` where `fuelCost = trip.obdFuelConsumedL × effectiveFuelPrice(trip.startedAt)` (priced at trip start per ADR-008).

**Compact Rupiah format** (same as FR-16 order-card trip strip):
- < 1,000 → 'Rp850'
- 1,000–999,999 → 'Rp8.4k' (one decimal)
- ≥1,000,000 → 'Rp1.2jt'
- Negative prefixed with '−' (e.g., '−Rp8.4k' if net is negative)
- Example: "Rp28k / Rp19.6k" or "Rp850 / Rp−1.2k"

**Missing data:** Shows '— / —' when `trip.obdFuelConsumedL = 0` or no fuel price was in effect at trip start.

**Manual (non-order) trips:** Rows without an order label (manual trips, or order trips that failed to capture a label) remain unchanged — no label or price/net cell is shown. The row displays the standard metrics: distance / duration / avg speed / avg km/L / cost (§3b).

**Design note:** No new visual language is introduced — the label is plain text (reuses existing title/label typography), and the price/net cell reuses the existing compact-Rupiah format and cell styling from the active-trip row (FR-12 integration). No external design handoff needed; user explicitly exempted from AGENTS.md §12 for this change (ADR-022).

---

## 3c. Observer Snapshot Viewer sheet (2026-07-08)

The `SnapshotViewerSheet` bottom-sheet (opened by tapping a snapshot-bearing Observer feed row) shows the captured accessibility node tree for one event.

**Meta strip — layout fix (2026-07-08).** The EVENT / FIRST SEEN / LAST SEEN chips previously shared one `Row`; a long event type (e.g. `WINDOW_CONTENT_CHANGED`) squeezed the last chip until its label wrapped one character per line (vertical `L-A-S-T-S-E-E-N`). The strip is now a `Column`: **row 1** is the event-type value alone in a full-width bordered box (no "EVENT" label); **row 2** holds FIRST SEEN and LAST SEEN chips at equal `weight(1f)`, with REPEAT appended when `repeatCount > 1`. Chip labels are pinned to `maxLines = 1, softWrap = false` so a squeezed label can never wrap vertically again.

**Prev / Next event navigation (2026-07-08).** The title row gains two circular chevron buttons immediately left of the close button. They move the sheet to the previous (older) / next (newer) event **without leaving the overlay**, iterating only over snapshot-bearing events (`treeSnapshot != null`) so every target renders real content. Buttons render disabled/greyed at the ends of the list (and whenever the host supplies no handler). Sheet exposes `onPrev/onNext/canPrev/canNext` (defaulted → back-compatible); wired in `ObserverFeedScreen` over `uiState.events`.

---

## 3d. Track Navigation (dual-mode) — pending design handoff (ADR-009)

New Track-screen UI for follow-a-route navigation. Per AGENTS §12 the visuals are **design-handoff-first** — this section records *what* surfaces are needed, not final visual specs (a design handoff is produced before code):

- **Destination search** — place-search field with autocomplete on the Track screen; entry point to "Start trip + navigate".
- **Start control** — offers "Start trip (track only)" vs "Start trip + navigate"; reuses the existing `TripPanel` CTA language.
- **Route + read-out** — route polyline to follow; remaining distance + ETA shown alongside existing trip metrics.
- **Navigation-perspective toggle** — new control in `MapControls` (heading-up + follow, no tilt); decoupled from trip/nav state; default off (north-up).
- **Directional car marker** — rotates to GPS heading (replaces the static pin); holds last heading at rest. (implemented per §3i)
- **Road-ahead candidates** — translucent polylines for previously-driven continuations (ADR-010), visually distinct from the solid live path.
- **Routing attribution** — visible "© openrouteservice.org | © OpenStreetMap contributors" wherever a route is shown (ADR-011 requirement).

Screenshots to attach for the handoff: current Track map + `TripPanel`, `MapControls`, bottom nav, Sessions card (for visual language). **Engine:** OpenRouteService (ADR-011) — ETA is static (no live traffic).

---

## 3e. Gojek Order Card (Track screen) — pending design handoff (ADR-014, ADR-015)

New Track-screen UI surface to display extracted Gojek order card details when a complete order is ready. Per AGENTS §12 the visuals are **design-handoff-first** — this section records the structure, not final visual specs. Visual design will be produced in a design handoff before implementation.

**Card states:**
- **Pickup phase** — Order card displays: pickup name + address, drop name + address, payment method, earnings (Rp).
- **Drop-only phase** — Pickup details no longer shown; card displays drop + payment + earnings.
- **Cleared/Cancelled/Dismissed** — Card is replaced by the normal `TripPanel`; the trip ends automatically.

**Dismiss:** The provisional order card includes a Dismiss control so a cancelled order (which never reaches the finished phase) cannot hide the trip Start/Stop control; the final placement/visual is part of the pending design handoff.

**Card interactions (expand/collapse + strip-only) — 2026-10-06**

Three interaction states for the card (excluding the Dismiss control):
- **Expanded** (default): both pickup and drop show name + address; a chevron-up (▲) button in the PhaseRow collapses the card.
- **Collapsed**: both pickup and drop show name only (no address); chevron-down (▼) in the PhaseRow expands. Payment/earnings and the trip strip remain visible in both states.
- **Strip-only**: tapping either the PickupBlock or DropBlock enters this mode — the card body is hidden and only the trip strip is shown. Tapping the trip strip exits strip-only and returns to collapsed state. This lets the driver free most of the map for navigation once they know the route.

**Trip strip (compact, provisional)**

A one-row strip appears below the order card while the trip is live (tripState is LIVE or PAUSED). The strip contains four cells:
- **TIME** — elapsed duration in compact format (e.g., "2h13m", "42s", "3m15s"); updated live.
- **DIST (km)** — label "DIST (km)"; value is the distance with one decimal place, no unit in the value (e.g., "12.5"); updated live.
- **COST / NET (Rp.)** — label "COST / NET (Rp.)"; value formatted as "3.5k/21k" (estimated fuel cost then net profit, compact Rupiah without "Rp" prefix, no spaces around "/"). COST = trip litres burned × current fuel price; NET = OrderCard.earningsRp − COST (negative shown as "−8.4k"). Compact amounts: <1,000 → '850'; 1,000–999,999 → '8.4k'; ≥1,000,000 → '1.2jt'. TalkBack reads full Rupiah amounts. Missing data: "—" when OBD off / no fuel / no price; NET "—" when COST is "—" or no earnings.
- **AVG / INST (km/L)** — label "AVG / INST (km/L)"; value formatted as "11.8/12.3" (trip-average km/L then instant km/L, each one decimal, no spaces around "/"). Instant "—" when not moving or no GPS fix. OBD not connected → "—/—"; average "—" until distance > 0.01 km and fuel > 0.

Layout: four cells, all center-aligned; weights TIME 0.7, DIST 0.7, COST/NET 1.4, AVG/INST 1.4; values maxLines=1.

The strip is hidden when the order ends and `TripPanel` returns. Provisional visuals reuse `TripPanel`'s glass-panel style; final design comes from the handoff (§ Handoff Instructions below).

**Handoff Instructions (Trip strip)**

*Provisional code until the design returns:* the trip strip ships in a PROVISIONAL style reusing `TripPanel` tokens (no new visual language); it is replaced when the design handoff returns.

**Step-by-step process (Claude Design / Google Stitch):**
1. Capture the screenshots in the checklist below from the running app (dark and light if available).
2. Open Claude Design or Google Stitch, attach the screenshots, and paste the prompt below.
3. Generate the trip strip in the existing dark glass-panel visual language for three OBD states: connected (moving with km/L), connected (idle with — / —), and not connected (— / —). Include a trip-paused state.
4. Review against the constraints (glass-panel language, mono numerals, no colour-only meaning, ≥48dp touch targets, Compose 1.2-compatible), then export the design and record it per `docs/WORKFLOW.md §3`.

**Screenshot checklist (attach current-app screens):**
- Track screen with the map and `TripPanel` in READY, LIVE and PAUSED states (shows the glass style).
- Track screen with the current provisional order card and the compact trip strip (LIVE state, OBD connected/moving, fuel price set) showing TIME / DIST / COST / NET / AVG/INST km/L.
- Track screen with the order card and trip strip (OBD not connected) showing "— / —" for COST/NET and km/L.
- Bottom navigation.
- Top app bar, if any.

**Tool recommendation:** Claude Design primary, Google Stitch alternative (external handoff, see `docs/WORKFLOW.md §3`).

**Copy-paste-ready prompt:**

> Design the "Trip Strip" for an Android (Jetpack Compose, Material 3) driver app called TrackLocation. It sits below the Gojek Order Card on the Track screen, inside the same glass panel, displaying live trip metrics while the order is active. Layout: one row with four cells: **TIME** (elapsed, compact format like "2h13m" / "42s" / "3m15s"), **DIST** (km, one decimal), **COST / NET** (label "COST / NET", value "Rp8.4k / Rp28k" = estimated fuel cost then net profit, in compact Rupiah format: <1k 'Rp850', 1k–999k 'Rp8.4k', ≥1M 'Rp1.2jt', negative prefixed with '−'), **AVG/INST km/L** (label "AVG/INST", value "11.8 / 12.3" = trip average then instant, each one decimal). States: (1) OBD connected, moving, price set (all values showing: COST/NET calculated); (2) OBD connected, moving, no price ("— / —" for COST/NET); (3) OBD connected, idle or no GPS fix (instant km/L shows "—", COST/NET calculated if price set else "— / —", average already set); (4) OBD not connected ("— / —" for both COST/NET and km/L); (5) trip paused (all values frozen). Do not show the Stop button in the strip (the card keeps any controls). Keep the existing glass-panel visual language (see attached screenshots), usable in dark and light themes. Use mono numerals for the time/distance/fuel values. Touch targets for any controls ≥48dp. Do not rely on colour alone for state. Compose 1.2-compatible (no EaseInOut, no animation label params, no ModalBottomSheet). Provide all five states in both themes.

**Placement:** Replaces `TripPanel` on the Track screen (where the trip control and metrics normally display) when an extracted Gojek order has all four fields. Card is displayed from pickup phase until order completion; a compact trip strip appears below the card while the trip is live (showing elapsed time, distance, COST/NET, and AVG/INST km/L); `TripPanel` returns after the order ends.

**Related behavior (not visual design):** Automatic trip start when the order card first becomes complete (Taken) and automatic trip end when the order is Cleared, Cancelled or Dismissed (ADR-014, ADR-015); one-shot foreground launch when card is ready; always-recording remains active. While an order is active the trip runs automatically and the card replaces TripPanel (including its Start/Stop control) and carries a compact trip strip. The compact trip strip carries live trip metrics without duplicating the card.

**Handoff Instructions (AGENTS.md §12 template)**

*Provisional code until the design returns:* the code ships a PROVISIONAL card reusing `TripPanel` tokens (no new visual language); it is replaced when the design handoff returns.

*(a) Step-by-step process (Claude Design / Google Stitch):*
1. Capture the screenshots in (b) from the running app (dark and light if available).
2. Open Claude Design or Google Stitch, attach the screenshots, and paste the prompt in (d).
3. Generate the three card states (pickup phase, drop-only phase, finished) for dark and light themes.
4. Review against the constraints (glass-panel language, no colour-only meaning, no customer name/phone), then export the design and record it per `docs/WORKFLOW.md §3`.

*(b) Screenshot checklist (attach current-app screens):*
- Track screen with the map and `TripPanel` in READY, LIVE and PAUSED states.
- Bottom navigation.
- Top app bar, if any.
- The Session or List glass/section card, for the card style.

*(c) Tool recommendation:* Claude Design or Google Stitch (external handoff, see `docs/WORKFLOW.md §3`); Stitch is suited to quick state variants, Claude Design to matching the existing "Kinetic Precision" system.

*(d) Copy-paste-ready prompt:*

> Design the "Gojek Order Card" for an Android (Jetpack Compose, Material 3) driver app called TrackLocation. It replaces the `TripPanel` at the bottom of the Track screen (above the bottom navigation, over the map) while a Gojek order is active. Fields: a phase chip (Pickup / Drop / Done), pickup name + address, drop name + address, payment method, and earnings in Rp. States: (1) pickup phase shows pickup and drop; (2) drop-only phase shows only drop; (3) finished shows earnings only. Keep the existing glass-panel visual language (see attached screenshots), usable in dark and light themes. Do not rely on colour alone to convey phase or state (use text/icon as well). The phase change must be announced through an accessibility live region. Customer name and phone number are never shown. Provide all three states in both themes.

**Route overlay (provisional, ADR-016, provider ADR-017 — OpenRouteService)**

Two driving routes are drawn on the Track screen's embedded Google Map while a Gojek order is active. Per AGENTS §12 the visuals are **design-handoff-first** — this section records the structure, not final visual specs.

**Visuals (PROVISIONAL styling until handoff):**
- **Planned route:** thin, muted blue-grey polyline (RGB hex `#B0BEC5` or equivalent theme token); width ~4 dp; connects current location → pickup → drop (pickup as a waypoint); drawn below other map elements (lower zIndex).
- **Runtime route:** bold orange polyline (RGB hex `#FF9800` or equivalent theme token); width ~6 dp; connects current location → next stop (pickup during PICKUP phase, drop during DROP phase); drawn above the planned route (higher zIndex).
- **Markers:** Pickup and Drop locations receive map markers with titles "Pickup" / "Drop" shown on tap; use standard Material Design marker coloring (default blue for most markers, or custom colors at the designer's discretion).
- **Attribution surface:** While a route is drawn on screen, display '© openrouteservice.org | © OpenStreetMap contributors' as small text. Provisional placement: bottom-start (above the order card) or top-start; final placement from design handoff. Hide attribution when no active route.
- **Existing map elements untouched:** blue recorded-trace polyline, live car marker (heading-rotated per §3i, independent of the navigation perspective), and Google Maps base layer remain unchanged.

**Behavior:**
- **Planned route** fetched once when the order becomes Taken; frozen for the life of the order (never re-fetched).
- **Runtime route** re-fetched on phase change (PICKUP → DROP) or when the driver deviates >~40 m from the polyline; throttled to at most once per 30 s and only after the driver moved ~100 m (prevents jitter).
- **Markers** displayed at their geocoded LatLng coordinates; refresh on phase change.
- **Clear on completion:** Both routes, markers, and route state are cleared when the order ends (FINISHED), is dismissed, or no active order remains. The map reverts to showing the recorded trace + live car position.
- **Failure gracefully:** If the OpenRouteService API fails (offline, rate limit, invalid address, geocoding fallback exhausted, etc.), no route is drawn; the trip continues recording normally with no other disruption.

**Handoff Instructions (Route overlay)**

*Provisional code until the design returns:* the route visuals ship in PROVISIONAL styling (simple polyline colors + Material markers); they are refined when the design handoff returns.

**Step-by-step process (Claude Design / Google Stitch):**
1. Capture the screenshots in the checklist below from the running app (dark and light if available), showing the map with the provisional routes.
2. Open Claude Design or Google Stitch, attach the screenshots, and paste the prompt below.
3. Refine the planned-route styling (receded visual; could use dashing, opacity, or a different hue to signal "planned vs. runtime").
4. Refine the runtime-route styling (prominent, current; could use bolder stroke, brighter hue, or an animated dash pattern).
5. Design the Pickup/Drop markers (use existing Material marker style or a custom marker graphic; must be visually distinct from the car marker).
6. Include both dark and light theme variants; test readability over the map.
7. Review against the constraints (no colour-only meaning, ≥48dp tap targets for markers, Compose 1.2-compatible), then export and record per `docs/WORKFLOW.md §3`.

**Screenshot checklist (attach current-app screens):**
- Track screen with the map, provisional routes (planned + runtime visible simultaneously), and Pickup/Drop markers visible.
- Track screen with an active Gojek order card (shows the context the routes sit in).
- Zoom levels: one with the full route visible, one with close-up route detail near the car marker.
- Bottom navigation, top app bar, MapControls (if present).
- Light and dark theme variants if available.

**Tool recommendation:** Claude Design primary (route styling refinement, multi-state variants); Google Stitch alternative (external handoff, see `docs/WORKFLOW.md §3`).

**Copy-paste-ready prompt:**

> Design the route overlay for the Google Map on TrackLocation's Track screen, displayed while a Gojek driver order is active. Two routes are shown: (a) **Planned route** — thin, receded-looking polyline from current location → pickup → drop (pickup as waypoint); shown once at order start and frozen; (b) **Runtime route** — bold, prominent polyline from current location → next stop (pickup in PICKUP phase, drop in DROP phase); updated on phase change or when driver deviates from route, throttled to prevent jitter. Map also shows **Pickup and Drop markers** (standard Material Design map pins with titles "Pickup" / "Drop"). When the order ends, both routes and markers disappear. Existing map layers (recorded blue trace, live car marker, base map) remain unchanged. **Attribution:** '© openrouteservice.org | © OpenStreetMap contributors' must be visible as small text while routes are shown (provisional placement: bottom-start above card or top-start; final placement TBD). Provide styling for both dark and light themes. Planned route should look distinct and receded (e.g., lighter color, dash pattern, or reduced opacity); runtime route should look prominent and current (e.g., bold stroke, saturated orange, or animated pattern). Markers must be visually distinct from the car marker and readable on the map. Do not rely on color alone for meaning. Compose 1.2-compatible (no EaseInOut, no animation label params, no custom shape composition beyond Compose basics). Include both routes visible simultaneously, route-near-car close-up, and zoom-out overview.

**Related behavior (not visual design):** Automatic route fetches via OpenRouteService Directions API + Geocoding (free tier, no billing; ADR-017 re-adopts ORS from ADR-011); one-shot Geocoding with fallback ladder for Indonesian addresses (place name + city hint → full address → street-only with dropped districts; rejects low-confidence city-centre pins), cached in-memory per order; throttled runtime re-fetch; fail-soft if API is offline (no route drawn, trip continues). Addresses are sent to OpenRouteService (Directions + Geocoding); customer name/phone never sent (ADR-013, privacy: extraction/persistence device-only; routing sends addresses for guidance). Planned route may differ slightly from Gojek's own route (best-effort; no promise of 100% alignment). Street-level geocoding can be metres off on long roads (best-effort; user can follow Gojek's route if preferred). (ADR-016, ADR-017)

---

## 3f. Track Map — Recenter FAB / Follow Mode (2026-10-06)

**What it does:** Adds functional follow mode to the Track screen's map, driven by an `isFollowing` state flag. The map auto-animates the camera to follow the current location while following is ON. User gestures (drag/pinch/fling) turn following OFF, allowing the user to inspect the map or the route ahead. Tapping the Recenter FAB turns following back ON and animates to the current location. The Recenter FAB visual state reflects whether following is active (dark crosshair icon while following; muted grey icon when panned away).

**States:**

1. **Following** — `isFollowing = true` (default on entry). Camera auto-animates to the current location, preserving the user's current zoom level (or defaulting to `MAP_ZOOM` if zoomed out ≤ MAP_ZOOM - 3). Map is read-only (no user pan).
2. **Panned Away** — `isFollowing = false`. Camera is static; the user has manually panned/pinched to inspect the route ahead or look at pickup/drop pins. Tapping the Recenter FAB resumes following.

**Transitions:**

- **Gesture → off:** User initiates a gesture (drag/pinch/fling) detected via `CameraMoveStartedReason.GESTURE` in the `CameraPositionState` callback; `isFollowing` → false, FAB icon → grey, and the camera stops auto-updating.
- **Tap Recenter → on:** User taps the Recenter FAB; `isFollowing` → true, FAB icon → dark, and the camera animates to the current location.

**Visual state (PROVISIONAL):**

- **Recenter FAB:**
  - **Following (dark):** Crosshair icon dark (`0xFF0A0A0A`), fully opaque. Content description: 'Recenter map'.
  - **Panned away (muted grey):** Crosshair icon muted grey (~`0xFF9E9E9E`), fully opaque. Content description: 'Recenter map, map is not following' (a11y hint that the tap would resume following).
  - Minimum touch target: 44dp FAB (note: below 48dp guideline; existing design).
- **Map layers FAB:** Remains an inert placeholder (explicitly out of scope; a future decision).

**Accessibility:** TalkBack users hear 'Recenter map' or 'Recenter map, map is not following' depending on state, so state is communicated via text as well as icon color. Screen-reader users can tap Recenter to resume following after panning.

**Location source (ADR-018):** The blue dot, follow mode camera, and Recenter target all use a **live location fix** from a screen-scoped `LiveLocationSource` that requests high-accuracy location updates (~1 s interval) only while the Track screen is open and the app is in the foreground. Fallback while awaiting the first live fix (typically ~5 s after screen open): the last-known position seed from TrackingService. Live fixes are never stored (display-only; the canonical location log is unchanged). This ensures the Track screen behaves like Google Maps/Waze (position always reflects actual device location) regardless of recording state.

**Interaction with routes:** While a Gojek order is active, the driver can pan away from the current location to inspect the planned or runtime route and the pickup/drop markers. The following state remains independent of the route display; panning turns following off, and tapping Recenter resumes it. While a trip/order is active Recenter resumes heading-up follow (§3h).

**Handoff Instructions (Recenter FAB visual state — PROVISIONAL, pending design handoff)**

*Provisional code until the design returns:* the FAB icons and colour states ship in provisional styling (dark/grey swaps); they are refined when the design handoff returns.

**Step-by-step process (Claude Design / Google Stitch):**
1. Open Claude Design or Google Stitch and attach the screenshots in the checklist below.
2. Review the Recenter FAB in both states: dark (following) and grey (panned away).
3. Confirm the crosshair icon is legible at 44dp and the color contrast is sufficient in light and dark themes.
4. Generate refined icon styling and color specifications if needed (e.g., stronger/softer grey, different icon weight or baseline).
5. Export the design and record it per `docs/WORKFLOW.md §3`.

**Screenshot checklist (attach current-app screens):**
- Track map with `MapControls` in the **Following** state (dark Recenter + inert Layers FAB; map centered on current location, live location updates visible).
- Track map with `MapControls` in the **Panned Away** state (grey Recenter + inert Layers FAB; camera has been manually dragged/pinched away from the current location, showing the route ahead or pickup/drop pins).
- Light and dark theme variants if available.

**Tool recommendation:** Claude Design primary (icon styling, color refinement, both-themes variants); Google Stitch alternative (external handoff, see `docs/WORKFLOW.md §3`).

**Copy-paste-ready prompt:**

> Design the "Recenter FAB" visual state for the Track map in TrackLocation (Android, Jetpack Compose, Material 3). The FAB has two states: (1) **Following** — dark crosshair icon (`0xFF0A0A0A` provisional) showing the camera is auto-following the current GPS location; tap does nothing. (2) **Panned Away** — muted grey crosshair icon (`~0xFF9E9E9E` provisional) showing the camera is static (user has panned/pinched to inspect the map); tap Recenter animates the camera back to the current location and resumes following. Provide both states in light and dark themes. Confirm the icon is legible at 44dp (below the 48dp Material guideline, existing design). Use simple geometric crosshair (no fill, outline only) or a plus-sign variant suitable for 44dp. Do not rely on color alone to convey state (text TalkBack labels distinguish the states). Provide SVG or vector export suitable for an Android `res/drawable` vector drawable.

---

## 3i. Track Map — Car Marker (2026-10-07)

**What it does:** Replace the current static blue `ic_location_pin` location marker with a directional car marker that rotates to indicate the direction of travel. The marker is sourced from the `ui-design.pen` design file (CarMarker frame, id NtE0e, 28×44 dp, top-down car, front at top) and converted to an Android `VectorDrawable`. The body is black (`#000000`) and both window panels are white (`#FFFFFF`); preserve the existing accent details and dark wheels/trim. The design's translucent white backing rectangle is omitted. The car rotates flat on the map (anchored centre, `flat = true`, `rotation = heading`) via the heading from the most-recent GPS fix (bearing) when speed ≥ ~3 km/h; below that speed the marker holds the last valid heading; if no valid heading yet, it points up (0°, north). The marker is a **display-only** change; no schema, permission, or data-model changes.

**Rules:**
1. **Replaces the blue pin:** The Track map's current-location marker (today `ic_location_pin`) is replaced by the car marker on entry to the Track screen, or when a live location fix arrives.
2. **Flat marker with heading rotation:** The marker is rendered `flat = true` (rotates with the map, not the device), `rotation = headingDegrees`, `anchor = (0.5, 0.5)` (centre). Heading comes from the live-fix bearing when speed ≥ 3 km/h, held constant below 3 km/h, points north (0°) if no valid heading yet.
3. **Other markers unchanged:** Pickup/Drop markers (for Gojek orders), the recorded trace polyline, route overlays, and the Recenter FAB control remain unchanged.
4. **Display-only:** No schema change, no permission change, no data model change. The canonical location log and live location fix (ADR-018) are unaffected.
5. **Visual body:** The visible car body is ~13×25 dp inside the 28×44 dp frame boundary (as designed), with black body fill (`#000000`) and white window panels (`#FFFFFF`); preserve the existing accent details and dark wheels/trim.

**Implementation location:** Marker bitmap created once, stored in a remember block; reused for every map-state update (no per-update bitmap regeneration). Rendered via `bitmapDescriptorFromVector(context, R.drawable.ic_car_marker, tint = null, scale = 1.0)`, wired to `GoogleMap.addMarker(MarkerOptions().flat(true).rotation(markerHeadingDeg).anchor(0.5f, 0.5f))`.

**Accessibility:** The marker is a decorative map affordance (no interactive controls), so TalkBack treats it as part of the map background. No new accessibility announcement needed.

**Design handoff:** No design handoff needed — design already exists in `ui-design.pen` (CarMarker frame). Implementation converts the frame directly to `ic_car_marker.xml`.

---

## 3g. Track screen — TripPanel READY-state FAB + merged metrics row (2026-10-06)

**What changed:** The `TripPanel` component is now hidden when no trip is running. A large green PlayFab replaces it in the READY state so the map is unobstructed and the start action is prominent. In the LIVE and PAUSED states the panel returns as before, with the inline CTA button (pause / resume) inside the panel.

**READY state (no active trip, no active order):**
- `TripPanel` is hidden (not rendered).
- A **56 dp green circular FAB** (background `BrandGreen`, play glyph in `BrandGreenDark`) is shown at the bottom-center of the map, with a 14 dp bottom padding (matching the panel padding).
- Tapping the FAB starts the trip (calls `onTripCtaTap()`). Accessibility content description: "Start trip".

**LIVE / PAUSED states:**
- `TripPanel` is shown at bottom-center (same position as before, 14 dp padding all sides).
- The PlayFab is hidden.
- **LIVE:** The existing inline `TripCtaButton` (48 dp) inside `TimerCtaRow` shows a **stop icon** (filled rounded square glyph, ~12/24 of the glyph box, radius 2 dp, `BrandGreenDark` on the green circle), accessibility label **"Stop trip"**. Tapping finishes and persists the trip (sends `STOP_TRIP`).
- **PAUSED:** The inline `TripCtaButton` shows a resume/play icon and label "Resume trip" (not currently reachable; pause/resume is a deferred future feature). No pause functionality is implemented.

**Merged metrics row (4 cells):**
The old separate `StatsRow` (KM / KM/HR) and `ObdRow` (FUEL / L/H / TRIP AVG) are replaced by a single `CombinedMetricsRow` with up to four cells in one row:
- **KM** — trip distance, left-aligned, existing icon + distance value.
- **KM/HR** — current speed, center-aligned, existing icon + speed value.
- **L/H** *(only when OBD connected)* — current fuel rate in L/h; "—" when engine off or no data.
- **AVG/INST km/L** *(only when OBD connected)* — trip-average km/L then instant km/L, formatted "avg/inst" (e.g. "11.8/12.3"); "—" each when unavailable.

When OBD is not connected only the first two cells are shown (identical to the previous StatsRow).

**Visual design:** PROVISIONAL — reuses existing glass-panel and metric-cell styles; refined by a future design handoff (AGENTS.md §12).

**Note (2026-10-07):** The LIVE button now shows a stop icon and label "Stop trip" (replaces the earlier pause/resume design and resolves Issue #2 in IMPLEMENTATION-ISSUES.md). The PAUSED state remains in code but is unreachable. Pause/resume capability is deferred as a possible future feature.

---

## 3h. Track Map — Navigation Camera (2026-10-07)

**What it does:** While a trip is live or a Gojek order is active, the Track map's camera automatically switches to a navigation-style perspective (heading-up follow with bearing from GPS, zoom 17 on entry, no tilt). When the trip or order ends, the camera animates back to north-up follow and keeps the current zoom. The manual "navigation perspective" toggle of ADR-009 remains independent and can override this auto-activated state.

**Behaviour rules:**

1. **Activation trigger:** `navigationActive = (tripState != READY) || (activeOrder != null)`.
2. **Entry behavior (navigationActive becomes true):** Follow is re-enabled (clearing any previous pan), camera animates once to zoom 17, then respects user zoom gestures. Bearing = GPS heading (if valid, see rule 4); tilt = 0.
3. **During active navigation:** Target on the live location fix (ADR-018 `LiveLocationSource`), bearing = GPS heading, tilt = 0.
4. **Heading validity:** Use the live-fix bearing only when the fix has a bearing AND speed ≥ ~3 km/h. Below ~3 km/h, hold the last valid heading. If no valid heading yet, remain north-up (bearing 0).
5. **Manual override (pan/pinch):** Any user gesture on the map (pan or pinch) stops following and auto-rotation (same as ADR-018 / §3f). The user's zoom is retained. Recenter FAB resumes follow with heading-up while navigationActive, or north-up otherwise.
6. **Exit behavior (navigationActive becomes false):** Camera animates bearing back to 0 (north-up), keeps follow on, preserves current zoom.
7. **Display-only:** No change to recording, canonical location log, trips/sessions, schema, or route overlays (ADR-016/017). No new control/button; manual perspective toggle (ADR-009) stays planned and independent. Turn-by-turn, car-marker rotation, and camera offset remain out of scope.

**Accessibility:** Camera orientation is a display affordance (map only); no new controls or motion-sensitive animations beyond the existing camera animations (smooth bearing interpolation, zoom animation). Screen-reader focus and semantic meaning are unchanged.

**Design handoff:** No new visual surface or controls required. Camera behavior uses only the existing CameraPosition bearing/tilt properties (Compose 1.2 / maps-compose 2.5.3 compatible). No design handoff needed.

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
- **Tap to edit:** tapping the cost opens a compact numeric `Rp` price editor with **Save (✓)** = apply and **Cancel (✗)** = discard the in-progress edit. Saving writes the shared current price and both surfaces update. On the Trips screen the editor is also openable from the header fuel-pump icon (§3b #8), not only the COST cell.
- **Undo / redo:** `↶` reverts to the previous price, `↷` re-applies the undone price; full multi-step within the session; each control is disabled when there is nothing to undo/redo. History is in-memory and resets on app restart; the current price persists.
- **Completed trips (ADR-008):** each completed-trip row also shows cost, priced by the `fuel_price` row effective at the trip's **start**; editing the price later never re-costs finished trips (`—` if no price applied then). Tapping a completed row's cost cell shows the applied price (read-only Toast, §3b #9).
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
The map uses a minimalist, light-themed tile provider. Interactive controls (Recenter, Layers) are grouped as floating circular white buttons on the right side of the screen. **Recenter FAB is now functional (§3f):** taps Recenter to resume following the current location. **Layers FAB remains an inert placeholder** (see §3f for details).

### Bottom Navigation
A persistent white bar with a subtle top border. The active state is indicated by a soft green pill-shaped background behind the icon and bolded text labels.
```
<!-- END DESIGN_SYSTEM.md (verbatim) -->

---

**Status:** Active. Migrated from legacy product-spec + DESIGN_SYSTEM + CR-0002 UI spec.
