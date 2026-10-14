---
name: UI-SPEC.md
path: docs/UI-SPEC.md
description: UI Specification — TrackLocation (screens, design system, flows)
---

# UI Specification
## TrackLocation

**Document Version:** 0.55
**Status:** Active (migrated from product-spec.md, DESIGN_SYSTEM.md, CR-0002 UI spec)
**Last Updated:** 2026-10-14
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
| Fuel dock (§5b) on Track (all variants), List, Sessions, Settings | Designed in `ui-design.pen` (dock, edit pencils, Fill up / Reset fuel / Fuel price dialogs, dock states; 2026-10-11 update); state thresholds and colours confirmed 2026-10-11; code implemented 2026-10-11 (`a197453`, `36030ef`, `3354689`); build OK; partly device-verified 2026-10-11 | FUEL LEFT / RANGE / PRICE / Fill up above the bottom nav (ADR-027, FR-22). No dock on OBD Settings or the pushed screens. No external handoff needed (designed in the `.pen`) |
| Session overlay banner (§5d) | Designed (approved 2026-10-13); not implemented | System overlay above other apps, shown only while a session is active and TrackLocation is in the background; needs the Display over other apps permission (ADR-029, FR-24). Strip with an inline Fill up form and a draggable collapsed circle. Visual source of truth: `ui-design.pen`, section frame "Session Overlay Banner (ADR-029)". No external handoff. Not a TrackLocation screen, so it adds no route |
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
- **Two stat rows** (OBD-connected): a **base row** — `duration` from `now - tripStartedAt`, `distance (km)` from `distanceInMeters`, `avg speed` = km ÷ elapsed-hours — and an **OBD row** — `cost` (`Rp`, display-only, no tap target — see §4), `L/h` (`fuelRateLph`), `instant km/L` (from `ObdUiState.Connected.instantKmL`; `—` at rest / poor fix, gated like the Session card), and `trip avg km/L` (`tripAvgKmL`). Non-connected: OBD-row values render `—`. The card grows taller to fit the second row. This mirrors the Session OBD card's instant/average separation for a seamless cross-tab experience. Order follows §5a (canonical order); the instant + trip-average km/L may later be combined into one `AVG/INST` pair cell.
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
5. **Recent-trip rows** (`TrackItemRow`) — add a 4th stat, **efficiency**: `track.obdCoveredDistanceKm ÷ obdFuelConsumedL` km/L (ADR-024) when `obdCoveredDistanceKm > 0`, else the legacy `distance(km) ÷ obdFuelConsumedL` for pre-v13 trips; shows `"—"` when `obdFuelConsumedL = 0` (trip recorded without OBD).
6. **Recent-trip rows — cost stat (ADR-008):** add a 5th stat, **cost** (`Rp`) = `obdFuelConsumedL × price effective at the trip's start` (from the `fuel_price` effective-dated log); shows `—` when `obdFuelConsumedL = 0` or no price was in effect at the trip's start. Final completed-row stats, in canonical order (§5a): duration / km / avg speed / cost / average km/L.
7. **Recent-trip rows — two-row metric grid (2026-07-08 fix):** the five stats no longer share a single row. Packing five equal-width columns clipped the values on-device (cost showed only `Rp`, avg speed lost its `h`, duration lost its last digit). `TrackItemRow` now mirrors `ActiveTripRow`'s two-row grid (card height 124→156dp): **base row** = duration / km / avg speed; **fuel row** = cost / average km/L (an empty third cell keeps the columns aligned with the base row). Stat **values** use `MonospaceFontFamily` (tabular digits, ideal for tight numeric columns) at 14sp / Medium — smaller and lighter than the previous 15sp / Bold — with `-0.3sp` tracking and `Ellipsis` overflow; **labels** at 10sp. Data was always persisted correctly; this was a purely visual fix. **Design update (2026-10-09):** the grid renders as §5a metric cells — label above value, unit in the label (`TIME`, `DIST (km)`, `AVG SPEED (km/h)`, `COST (Rp.)`, `AVG FUEL (km/L)`), left-aligned on the light surface (2026-10-11; earlier centred), e.g. `22m15s` / `12.4` / `33.5` / `8.2k` / `8.5`. The value styling above (14sp Medium) is superseded in the design; implemented in Compose 2026-10-11 (static review; device check pending). **Single-row update (2026-10-09, later):** in the design the five stats sit in ONE row per card, in canonical order — TIME, DIST, AVG SPEED, COST, AVG FUEL — instead of the two-row grid; the card is 310 px wide inside its 18 px padding, so the row uses weighted cell widths 48 / 44 / 82 / 52 / 76 with a 2 px gap, label Inter 8 (letter-spacing 0) and value Roboto Mono 600 at 12, which keeps every label on one line. The two-row grid text above is superseded in the design; implemented in Compose 2026-10-11 (static review; device check pending).
8. **Header fuel-price affordance (2026-07-08) — REMOVED / SUPERSEDED 2026-10-11:** the fuel dock PRICE pencil (§5b) is now the price entry point from outside the cost cells. The Trips header fuel-pump icon is no longer in `ui-design.pen`; code removal implemented (`9d01688`; IMPLEMENTATION-PLAN §4 fuel dock slice, step 6). The historic 2026-07-08 spec below no longer applies in the design: the `ListHeader` "Trips" row gains a trailing tappable **fuel-pump icon** (right-aligned, same row as the 40sp label). Tapping opens the existing `FuelCostEditorDialog` (§4). This is the **only persistent entry point** to set/edit the fuel price on the Trips screen — previously the editor was reachable only via the active-trip row's COST cell, which is absent when no trip is running. Icon-only (no price text); rendered from a new `res/drawable` vector (no Compose-1.2 extended-icons dependency).
9. **Completed-trip cost read-out (2026-07-08) — REMOVED / SUPERSEDED 2026-10-11:** the cost cell is display-only and the Toast is removed (§4, ADR-027 Decision 12). Historic spec, kept for reference: tapping the **cost** cell on a completed `TrackItemRow` shows a **Toast** with the historic fuel price applied to that trip — the `fuel_price` row effective at the trip's **start** (`priceEffectiveAt(timestamp)`), price-per-litre only (e.g. "Track #12 fuel price: Rp 12.500 / litre"). **Read-only per ADR-008** — never opens the editable `FuelCostEditorDialog`, so finished trips are never re-costed. When no price was in effect at the trip's start (cost `—`), the Toast reads "No fuel price recorded for this trip." The cost cell's tap is independent of the row's tap-to-open-detail / long-press-to-delete.
10. **Completed-trip cost color alternation (2026-07-08, cosmetic):** on the completed `TrackItemRow` list, the **cost value** alternates between two theme colors — `TripInk` (#0A0A0A) and `TripGreen` (#16A34A) — toggling each time a row's cost differs from the row **above** it. Equal-cost runs (including consecutive `—`) share one color; the color flips at each change boundary, as a visual cue that the applied cost changed between adjacent trips. Purely cosmetic — no data or ordering change; only the cost value's text color is affected (labels and every other stat unchanged).

**Data:** No schema change for §3b's efficiency stat; the cost stat (ADR-008) reads the new `fuel_price` table (migration v8→v9).

**Design handoff:** Reuses the established Sessions/`TrackItemRow` visual language (no new visual design), so no external Claude Design round-trip is required. If externalised: attach the current Trips screen + Sessions screen as reference.

---

## 3b-order. Trips List — Order-Trip Rows (ADR-022, ADR-023)

**What it does:** When a trip is tied to a Gojek order (auto-started by ADR-015, geo-snapshot tracked by ADR-023), the completed trip row in the Trips list displays a three-line header card and a compact price/net cell, allowing the driver to quickly identify order-linked work and see three distinct locations at a glance.

**Display in TrackItemRow (completed trip, order-linked):**

**Three-line header card (ADR-023):** Appears as the row title/subtitle area, replacing the one-line label from ADR-022. The header shows three lines:
- **Line 1 (ACCEPTED):** Place name from reverse-geocoding the trip start GPS fix (e.g., "Senayan", "Jalan Cipete Raya"). One-time reverse-geocoding at trip end (fail-soft if offline). Names only (no full address shown). **Hidden if reverse-geocoding failed or no fix available** — the card then shows two lines (PICKUP and DROP only). For old migrated trips (v11→v12 database migration) that lack a name but have coordinates, the ACCEPTED line initially hidden; a one-time background backfill after app launch will eventually reverse-geocode the name, and the ACCEPTED line will appear on the next Trips list render (ADR-023 Amendment 2026-10-08).
- **Line 2 (PICKUP):** Pickup name from the Gojek order card (e.g., "Mall Pondok Indah").
- **Line 3 (DROP):** Drop name from the Gojek order card (e.g., "Blok S").

Names are extracted location names only (no customer name/phone; full addresses persist but are not displayed). If a name exceeds ~25 characters, truncate in the UI row with an ellipsis (e.g., "PT Multi-Pratama M…"). All three names and addresses are snapshots captured at trip save time; they survive `observer_trip` row pruning (90d/5k retention, ADR-013).

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

**Manual (non-order) trips:** Rows without an order header (manual trips, or order trips that failed to capture PICKUP/DROP names) remain unchanged — no header or price/net cell is shown. The row displays the standard metrics: duration / distance / avg speed / cost / avg km/L (§3b, canonical order §5a).

**Order status chip (ADR-025, 2026-10-10):** Every order-linked completed trip row shows one status chip. It sits on the same line as the row's existing order header (next to the title), not in the metric grid. Manual trips show no chip; the live (in-progress) row shows no chip because its status is not saved until stop.

| Status (stored) | Chip label | Look (provisional) |
|---|---|---|
| `COMPLETED`, `orderOffline = false` | `Completed` | Pill (999px radius, per §5), text and 1dp border in `TripGreen` (#16A34A) |
| `COMPLETED`, `orderOffline = true` | `Completed · Offline` | Same green pill, text reads "Completed · Offline"; the offline text is what carries the meaning (not colour alone) |
| `CANCELLED` | `Cancelled` | Pill, text and border in the existing neutral secondary-text token (no new colour) |

- Chip text is the source of meaning; colour is secondary. Chip touch target is not interactive (display only).
- Chip uses existing tokens and the status-chip shape from §5 (full pill, 999px). No new colour or elevation.
- Missing status (pre-v14 manual trips) → no chip. Pre-v14 order-linked trips show `Completed` (migration backfill).
- Screenshots to attach if this chip is externalised to a design handoff: Trips screen (an order-linked completed row, a manual row) and the Track screen order card (§3e).

**Design note:** No new visual language is introduced — the header is plain text (reuses existing title/label typography), the three-line structure is simple typography stacking, and the price/net cell reuses the existing compact-Rupiah format and cell styling from the active-trip row (FR-12 integration). No external design handoff needed; user explicitly exempted from AGENTS.md §12 for this change (ADR-022, ADR-023).

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
- **Cleared/Dismissed/Cancelled** — Card is replaced by the normal `TripPanel`. The trip ends automatically on Cleared and Dismissed. For any cancelled order, in Gojek, the trip keeps recording until the driver stops it, with the drop-off snackbar below (ADR-030, amending ADR-025). The card's replacement by `TripPanel` in that cancelled state is the proposed default and is an open question (ADR-025).

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
- **COST/NET (Rp.)** — label "COST/NET (Rp.)"; value formatted as "3.5k/21k" (estimated fuel cost then net profit, compact Rupiah without "Rp" prefix, no spaces around "/"). COST = trip litres burned × current fuel price; NET = OrderCard.earningsRp − COST (negative shown as "−8.4k"). Compact amounts: <1,000 → '850'; 1,000–999,999 → '8.4k'; ≥1,000,000 → '1.2jt'. TalkBack reads full Rupiah amounts. Missing data: "—" when OBD off / no fuel / no price; NET "—" when COST is "—" or no earnings.
- **AVG/INST (km/L)** — label "AVG/INST (km/L)"; value formatted as "11.8/12.3" (trip-average km/L then instant km/L, each one decimal, no spaces around "/"). Instant "—" when not moving or no GPS fix. OBD not connected → "—/—"; average "—" until covered distance > 0 and fuel > 0.01 L (ADR-024).

Layout: four cells, all center-aligned; weights TIME 0.7, DIST 0.7, COST/NET 1.4, AVG/INST 1.4; values maxLines=1. Display format (label, value, row styling) follows §5a.

The strip is hidden when the order ends and `TripPanel` returns. Provisional visuals reuse `TripPanel`'s glass-panel style; final design comes from the handoff (§ Handoff Instructions below).

**Drop-off snackbar (ADR-025, amended by ADR-030: any cancelled order)**

Shown on the Track screen only when a Gojek order was cancelled in Gojek (any payment type), the trip is still recording, and the driver comes within 150 m of the order's drop-off point (cached drop coordinates). It is not shown when the drop coordinates are unresolved.

- **Trigger:** first fix within 150 m of the drop-off after the cancel. Shown **once per trip**; it does not reappear if the driver leaves and returns to the point.
- **Text:** "Order cancelled in Gojek. You're at the drop-off. Stop trip?"
- **Action:** one text action, **Stop**. Tapping it runs the normal stop sequence (persist, then stop). Stopping within 150 m of the drop-off saves the trip as `Completed · Offline`; otherwise it saves as `Cancelled` (ADR-025 stop rules).
- **Placement:** bottom of the Track screen, above the fuel dock (§5b), the bottom navigation and the order card/trip panel. Non-blocking; it does not cover the map controls or take focus from the map.
- **Duration:** indefinite. It never auto-dismisses by timer and never auto-stops the trip. Ignoring it does nothing; the normal Stop CTA stays available. Swiping it away also does nothing to the trip.
- **Scope:** shown only while TrackLocation is on screen. There is no system notification, heads-up, or full-screen takeover (by design; a driver outside the app is not prompted).
- **If drop coordinates cannot be resolved:** no snackbar. The trip keeps recording and is saved as `Cancelled` when the driver stops it.
- **Visual:** Material 3 snackbar in the app's existing style; no new colour, no new visual language. Provisional; no separate design handoff requested (AGENTS.md §12: attach the Track screen with the order card as the baseline screenshot if a handoff is produced later).

**Handoff Instructions (Trip strip)**

*Provisional code until the design returns:* the trip strip ships in a PROVISIONAL style reusing `TripPanel` tokens (no new visual language); it is replaced when the design handoff returns.

**Step-by-step process (Claude Design / Google Stitch):**
1. Capture the screenshots in the checklist below from the running app (dark and light if available).
2. Open Claude Design or Google Stitch, attach the screenshots, and paste the prompt below.
3. Generate the trip strip in the existing dark glass-panel visual language for three OBD states: connected (moving with km/L), connected (idle with — / —), and not connected (— / —). Include a trip-paused state.
4. Review against the constraints (glass-panel language, mono numerals, no colour-only meaning, ≥48dp touch targets, Compose 1.2-compatible), then export the design and record it per `docs/WORKFLOW.md §3`.

**Screenshot checklist (attach current-app screens):**
- Track screen with the map and `TripPanel` in READY, LIVE and PAUSED states (shows the glass style).
- Track screen with the current provisional order card and the compact trip strip (LIVE state, OBD connected/moving, fuel price set) showing TIME / DIST / COST/NET / AVG/INST km/L.
- Track screen with the order card and trip strip (OBD not connected) showing "— / —" for COST/NET and km/L.
- Bottom navigation.
- Top app bar, if any.

**Tool recommendation:** Claude Design primary, Google Stitch alternative (external handoff, see `docs/WORKFLOW.md §3`).

**Copy-paste-ready prompt:**

> Design the "Trip Strip" for an Android (Jetpack Compose, Material 3) driver app called TrackLocation. It sits below the Gojek Order Card on the Track screen, inside the same glass panel, displaying live trip metrics while the order is active. Layout: one row with four cells: **TIME** (elapsed, compact format like "2h13m" / "42s" / "3m15s"), **DIST** (km, one decimal), **COST/NET** (label "COST/NET", value "Rp8.4k / Rp28k" = estimated fuel cost then net profit, in compact Rupiah format: <1k 'Rp850', 1k–999k 'Rp8.4k', ≥1M 'Rp1.2jt', negative prefixed with '−'), **AVG/INST km/L** (label "AVG/INST", value "11.8 / 12.3" = trip average then instant, each one decimal). States: (1) OBD connected, moving, price set (all values showing: COST/NET calculated); (2) OBD connected, moving, no price ("— / —" for COST/NET); (3) OBD connected, idle or no GPS fix (instant km/L shows "—", COST/NET calculated if price set else "— / —", average already set); (4) OBD not connected ("— / —" for both COST/NET and km/L); (5) trip paused (all values frozen). Do not show the Stop button in the strip (the card keeps any controls). Keep the existing glass-panel visual language (see attached screenshots), usable in dark and light themes. Use mono numerals for the time/distance/fuel values. Touch targets for any controls ≥48dp. Do not rely on colour alone for state. Compose 1.2-compatible (no EaseInOut, no animation label params, no ModalBottomSheet). Provide all five states in both themes.

**Placement:** Replaces `TripPanel` on the Track screen (where the trip control and metrics normally display) when an extracted Gojek order has all four fields. Card is displayed from pickup phase until order completion; a compact trip strip appears below the card while the trip is live (showing elapsed time, distance, COST/NET, and AVG/INST km/L); `TripPanel` returns after the order ends.

**Related behavior (not visual design):** Automatic trip start when the order card first becomes complete (Taken) and automatic trip end when the order is Cleared or Dismissed (ADR-014, ADR-015; cancel never auto-stops, ADR-030 amending ADR-025); one-shot foreground launch when card is ready; always-recording remains active. While an order is active the trip runs automatically and the card replaces TripPanel (including its Start/Stop control) and carries a compact trip strip. The compact trip strip carries live trip metrics without duplicating the card.

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

> Design the "Gojek Order Card" for an Android (Jetpack Compose, Material 3) driver app called TrackLocation. It replaces the `TripPanel` at the bottom of the Track screen (above the fuel dock and the bottom navigation, over the map) while a Gojek order is active. Fields: a phase chip (Pickup / Drop / Done), pickup name + address, drop name + address, payment method, and earnings in Rp. States: (1) pickup phase shows pickup and drop; (2) drop-only phase shows only drop; (3) finished shows earnings only. Keep the existing glass-panel visual language (see attached screenshots), usable in dark and light themes. Do not rely on colour alone to convey phase or state (use text/icon as well). The phase change must be announced through an accessibility live region. Customer name and phone number are never shown. Provide all three states in both themes.

**Route overlay (provisional, ADR-016, provider ADR-017 — OpenRouteService)**

Two driving routes are drawn on the Track screen's embedded Google Map while a Gojek order is active. Per AGENTS §12 the visuals are **design-handoff-first** — this section records the structure, not final visual specs.

**Visuals (PROVISIONAL styling until handoff):**
- *(2026-10-11: line styles, legend and the saved planned route on trip details are specified in §3j, ADR-026; the planned line is dashed there and its origin is the accepted point, ADR-017 Amendment.)*
- **Planned route:** thin, muted blue-grey polyline (RGB hex `#B0BEC5` or equivalent theme token); width ~4 dp; connects the accepted point (trip-start fix; falls back to the current location if unavailable) → pickup → drop (pickup as a waypoint; ADR-017 Amendment 2026-10-11); drawn below other map elements (lower zIndex). Line style is superseded by §3j (dashed, so the three lines are not told apart by colour alone).
- **Runtime route:** bold orange polyline (RGB hex `#FF9800` or equivalent theme token); width ~6 dp; connects current location → next stop (pickup during PICKUP phase, drop during DROP phase); drawn above the planned route (higher zIndex).
- **Markers:** Pickup and Drop locations receive map markers with titles "Pickup" / "Drop" shown on tap; use standard Material Design marker coloring (default blue for most markers, or custom colors at the designer's discretion).
- **Attribution surface:** While a route is drawn on screen, display '© openrouteservice.org | © OpenStreetMap contributors' as small text. Provisional placement: bottom-start (above the order card) or top-start; final placement from design handoff. Hide attribution when no active route.
- **Existing map elements untouched:** blue recorded-trace polyline, live car marker (heading-rotated per §3i, independent of the navigation perspective), and Google Maps base layer remain unchanged.

**Behavior:**
- **Planned route** fetched once when the order becomes Taken; frozen for the life of the order (never re-fetched).
- **Runtime route** re-fetched on phase change (PICKUP → DROP) or when the driver deviates >~40 m from the polyline; throttled to at most once per 30 s and only after the driver moved ~100 m (prevents jitter).
- **Markers** displayed at their geocoded LatLng coordinates; refresh on phase change.
- **Clear on completion:** Both routes, markers, and route state are cleared when the order ends (FINISHED or CANCELLED), is dismissed, or no active order remains. The map reverts to showing the recorded trace + live car position.
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

> Design the route overlay for the Google Map on TrackLocation's Track screen, displayed while a Gojek driver order is active. Two routes are shown: (a) **Planned route** — thin, receded-looking polyline from the accepted point (trip-start fix) → pickup → drop (pickup as waypoint); shown once at order start and frozen; (b) **Runtime route** — bold, prominent polyline from current location → next stop (pickup in PICKUP phase, drop in DROP phase); updated on phase change or when driver deviates from route, throttled to prevent jitter. Map also shows **Pickup and Drop markers** (standard Material Design map pins with titles "Pickup" / "Drop"). When the order ends, both routes and markers disappear. Existing map layers (recorded blue trace, live car marker, base map) remain unchanged. **Attribution:** '© openrouteservice.org | © OpenStreetMap contributors' must be visible as small text while routes are shown (provisional placement: bottom-start above card or top-start; final placement TBD). Provide styling for both dark and light themes. Planned route should look distinct and receded (e.g., lighter color, dash pattern, or reduced opacity); runtime route should look prominent and current (e.g., bold stroke, saturated orange, or animated pattern). Markers must be visually distinct from the car marker and readable on the map. Do not rely on color alone for meaning. Compose 1.2-compatible (no EaseInOut, no animation label params, no custom shape composition beyond Compose basics). Include both routes visible simultaneously, route-near-car close-up, and zoom-out overview.

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
The old separate `StatsRow` (KM / KM/HR) and `ObdRow` (FUEL / L/H / TRIP AVG) are replaced by a single `CombinedMetricsRow` (a `MetricStrip`, §5a) with up to four cells in one row, all center-aligned, no icons:
- **DIST (km)** — trip distance (`Metric/Distance`).
- **SPEED (km/h)** — current speed (`Metric/Speed`).
- **FLOW (L/h)** *(only when OBD connected)* — current fuel rate; `—` when engine off or no data (`Metric/FuelFlow`).
- **AVG/INST (km/L)** *(only when OBD connected)* — trip-average km/L then instant km/L, e.g. `11.8/12.3`; `—` each when unavailable (`Metric/AvgInst`).

When OBD is not connected only the first two cells are shown.

**Visual design:** the panel keeps the glass-panel surface; the metric cells and row follow §5a (design in `ui-design.pen`, `TripPanel` frame).

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

## 3j. Route Lines: Planned / Optimized / Actual + Trip Details Comparison (2026-10-11) — pending design handoff (ADR-026)

**Purpose:** Tell the three route lines apart at a glance, on the live Track screen and on the trip details map, and show a planned-vs-actual comparison after the trip. Per AGENTS §12 the visuals below are **design-handoff-first**; the values are PROVISIONAL until the design returns, and code must not invent visuals. Applies to order trips; the same surface is reserved for future navigation trips (ADR-009, `kind='NAV'`, not implemented). Prerequisites for code: this handoff approved AND the ORS ToS gate cleared (ADR-026 decision 10).

**Line styles (no colour-only encoding):**

| Line | Meaning | Style | Width (relative to `POLYLINE_WIDTH`) | Lifetime |
|---|---|---|---|---|
| Planned | accepted point → pickup → drop, frozen at takeover | **dashed**, thin, muted blue-grey (`#B0BEC5` or theme token) | ~0.6x | persisted device-only with the trip |
| Optimized | current location → next stop, navigation aid | **solid**, bold, orange (`#FF9800` or theme token) | ~1.4x | memory only, cleared at order end |
| Actual | recorded GPS trace | **solid**, blue (existing trace colour) | 1.0x | location log + `track.pathPoints` |

Dash vs. solid and thin vs. bold carry the meaning in addition to hue, so the lines remain distinguishable for colour-blind users. Draw order, bottom to top: planned, actual, optimized (live screen); planned under actual (details map). Pickup/Drop markers, the car marker (§3i) and the base map are unchanged.

**Legend (compact, dismissible):**
- Three entries: a short line sample plus label "Planned", "Optimized", "Actual".
- At most one line of height; horizontal strip with a small close affordance (>= 48dp touch target for the dismiss control).
- Placement must avoid: the top-start ORS attribution, the order card, and the map controls (Recenter FAB). Provisional: top-end below the status bar, or directly above the order card; final placement from the handoff.
- Dismissal is per-screen-visit (a remembered per-viewer preference is acceptable); it never hides the lines themselves.

**Live Track screen rules:**
- The legend shows only while an order is active (i.e. while at least a planned or optimized line is drawn); hidden otherwise.
- Glanceable while driving: no new interactive element is required to read the map; the legend dismiss is optional.
- Optimized-line behaviour (deviation-based refresh, throttling) is unchanged from §3e; this section only restyles and labels.
- Planned and optimized lines never appear for manual trips.

**Trip details map (DetailsTrackMap / DetailsScreen):**
- Draws the saved planned line (dashed) under the actual line; camera fits the bounds of BOTH lines (planned plus actual), with the existing padding.
- Legend shows two entries here (Planned, Actual); no Optimized entry (it is never saved).
- **Comparison card** below or beside the map (final layout from the handoff), four numbers with units:
  - Planned: `X.X km`
  - Actual: `X.X km`
  - Extra: `+X.X km` / `−X.X km` (signed, actual − planned)
  - Max deviation: `NNN m`
  
  TalkBack strings: "Planned route 12.3 kilometres", "Actual route 13.1 kilometres", "Extra distance plus 0.8 kilometres" (or "minus ..."), "Maximum deviation from plan 240 metres". Values are derived at display time; missing values show `—`.
- **Attribution** '© openrouteservice.org | © OpenStreetMap contributors' is visible whenever the saved plan is drawn (not placed on the top-start corner if it collides with the map controls on this screen; final placement from the handoff).
- **Empty state** (trip recorded before DB v15, plan fetch failed, or manual trip): no planned line, no comparison card; a quiet caption "No planned route saved". The actual line and existing details content are unchanged.

**Compose 1.2-compatible notes:** use maps-compose 2.5.3 `Polyline(pattern = listOf(Dash(...), Gap(...)))` for the dashed planned line (`pattern` is a Google Maps SDK property, not a Compose API); no `ModalBottomSheet`, no animation `label` params, no Compose >= 1.3 APIs. Legend and card are plain Material 3 components using existing "Kinetic Precision" tokens (§5).

**Display-only / no behaviour change:** no change to recording, the canonical location log or trip/session model; no new bottom-nav destination.

**Handoff Instructions (Route lines + comparison)**

**Step-by-step process (Claude Design / Google Stitch / `ui-design.pen`):**
1. Capture the screenshots in the checklist below from the running app (dark and light if available).
2. Open Claude Design or Google Stitch (or the project's `ui-design.pen` through the Pencil tools), attach the screenshots, and paste the prompt below.
3. Refine the three line styles (dash pattern, widths, hues) so planned / optimized / actual are distinguishable without colour alone, over the map in both themes.
4. Design the compact dismissible legend and its placement on the Track screen (clear of the ORS attribution, order card and Recenter FAB).
5. Design the trip details comparison card (four numbers, units, signed extra km), the two-entry legend, the attribution placement and the "No planned route saved" empty state.
6. Include dark and light variants; check contrast over map imagery.
7. Review against the constraints (no colour-only meaning, >= 48dp touch targets, Compose 1.2-compatible), then export and record per `docs/WORKFLOW.md §3`.

**Screenshot checklist (attach current-app screens):**
- Track screen with a live order: planned + optimized + actual lines visible together, Pickup/Drop markers, the order card and ORS attribution.
- Trips list with an order-trip card (§3b-order), to carry the card and list density.
- Trip details map screen (existing actual line, top app bar, current layout below the map).
- Bottom navigation and top app bar.
- Map controls / Recenter FAB (§3f), to place the legend clear of them.
- Light and dark variants if available.

**Tool recommendation:** Claude Design primary (line styling, legend and card variants, dark/light); Google Stitch alternative for quick state variants; `ui-design.pen` for matching the existing design file (external handoff, `docs/WORKFLOW.md §3`).

**Copy-paste-ready prompt:**

> Design the route-line system and the trip comparison view for TrackLocation, an Android (Jetpack Compose 1.2, Material 3, "Kinetic Precision" design system, see attached screenshots) driver app with an embedded Google Map. Three lines exist: (1) PLANNED — the route frozen when a Gojek order was accepted (accepted point to pickup to drop): dashed, thin, receded muted blue-grey; (2) OPTIMIZED — a live navigation aid from the current location to the next stop: solid, bold, orange, drawn on top, only while an order is active; (3) ACTUAL — the recorded GPS trace: solid blue. The lines must be distinguishable without relying on colour alone (dash vs. solid, thin vs. bold). Deliver: (a) the three line styles with widths and dash pattern, for dark and light themes, over map imagery; (b) a compact, dismissible legend with three entries (Planned, Optimized, Actual), at most one line tall, placed on the live Track screen so it does not overlap the top-start "© openrouteservice.org | © OpenStreetMap contributors" attribution, the Gojek order card at the bottom, or the Recenter FAB; it is shown only while an order is active; (c) the trip details map: the saved planned line (dashed) under the actual line, camera fitted to both, a two-entry legend (Planned, Actual), the attribution visible whenever the plan is drawn, and a comparison card with four numbers and units: Planned km, Actual km, Extra km (signed, actual minus planned), Max deviation in metres, plus accessible TalkBack strings; (d) the empty state caption "No planned route saved" for old trips. Constraints: the driver glances at the live map while driving, so no new required interaction; touch targets >= 48dp; do not add bottom-nav destinations; keep the existing glass-panel/card language; output must be implementable with Compose 1.2-era components and maps-compose Polyline dash patterns.

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

Averages remain a **single km/L** per surface (SESSION AVG on the Session card; on the Trip panel, the average half of AVG/INST (km/L), §5a):
- `avg km/L = covered distance ÷ fuel`, both over the same fuel-covered poll intervals (unified session/trip derivation — ADR-024, superseding the displacement-distance definition of ADR-007). SESSION AVG = `session.obdCoveredDistanceKm ÷ obdFuelConsumedL`; live TRIP AVG = `tripCoveredKm ÷ tripFuelL`; completed-trip rows use `track.obdCoveredDistanceKm ÷ obdFuelConsumedL` (legacy `distance ÷ fuel` for pre-v13 trips).
- Shows `—` when covered distance = 0 or fuel ≤ 0.01 L; otherwise the value. Pre-v13 sessions show `—`.
- While idling the average **degrades** (fuel keeps accruing, distance flat) — intended.
- **Session card group (Sessions screen, 2026-10-09):** each recorded-session card shows the session's average km/L (`session.obdCoveredDistanceKm ÷ obdFuelConsumedL`, as SESSION AVG above) and its total fuel consumed (`session.obdFuelConsumedL`, litres, one decimal) as ONE pair cell, `AVG/TOTAL (km/L/L)` (§5a), e.g. `10.4/0.5`. Implemented in Compose 2026-10-11 (static review; device check pending). The same card also shows the session average speed, `AVG SPEED (km/h)` (`session distance ÷ session elapsed hours`, one decimal), right after DIST in the canonical order. **COST row (2026-10-11; design updated in `ui-design.pen`, SessionsScreen, `CostRow` frame on both session cards; code committed `7760d48`, static review; device check pending):** below the five-cell strip each recorded-session card shows a second row with a single left-aligned `COST (Rp.)` cell (§5a exception (5)). Value = `session.obdFuelConsumedL` × the **current** shared fuel price (`FuelPriceController.state.currentPrice`), compact without `Rp` (e.g. `5k`); `—` when the session has no OBD fuel or no price is set. This is a deliberate exception to the completed-trip start-price rule (ADR-008): editing the price re-costs past sessions. The cell is not tappable on the card.

#### Fuel cost (Rp) — Session OBD card + Trips active-trip row + completed trips

- **Value:** `litres × price`, formatted `Rp` with a dot thousands separator and no decimals (e.g. `Rp 12.500`). Shows `—` when litres = 0 or the price is unset.
- **Placement:** Session OBD card — a COST cell below the fuel metrics (below OBD Status). Trips active-trip row — an added COST stat (row goes from 4 to 5 stats; keep the existing stat styling, reflow density handled in code).
- **Display-only, no tap target (decided 2026-10-11, ADR-027 Decision 12):** cost cells (Session OBD card, Trips active-trip row, completed-trip rows, Sessions COST row, Track strips) are read-only. Tapping a cost cell does nothing: no editor and no Toast. The price is edited only from the fuel dock **PRICE pencil** (§5b), which opens the Fuel price dialog: a compact numeric `Rp` editor with **Save** = apply and **Cancel** = discard the in-progress edit. Saving writes the shared current price and both surfaces update. The Trips header fuel-pump icon (§3b #8) was removed from the design on 2026-10-11.
- **Undo / redo:** `↶` reverts to the previous price, `↷` re-applies the undone price; full multi-step within the session; each control is disabled when there is nothing to undo/redo. History is in-memory and resets on app restart; the current price persists.
- **Completed trips (ADR-008):** each completed-trip row also shows cost, priced by the `fuel_price` row effective at the trip's **start**; editing the price later never re-costs finished trips (`—` if no price applied then). The completed-row cost cell is display-only; there is no Toast (§3b #9 removed).
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

## 5a. Metric Display Standard (2026-10-09)

**Source of truth:** the `TrackScreen — Order Active` trip strip (ADR-014/015) in `ui-design.pen`. Reusable components live in the `Metric Components` frame of that file.

**Cell**
- Vertical, 2 px gap between label and value. **Horizontal alignment follows the surface theme (2026-10-11):** on **dark surfaces** (every Track-screen strip: Order Active, Expanded and Strip Only, LIVE and READY `TripPanel`, Alt A) the label and value are centred; on **light surfaces** (List recent-trip rows, List live row, Sessions cards including the COST row) the label and value are left-aligned, so the cell content hugs the cell's left edge and the text is left-aligned. Dark-surface alignment is unchanged. `Metric/Earnings` is the exception and stays right-aligned.
- **Label:** Inter 10, regular weight, UPPERCASE, letter-spacing 0.4, `#FFFFFFC7`. Format `NAME (unit)` with the unit lowercase, e.g. `DIST (km)`. No icons in labels. No unit inside the value.
- **Value:** Roboto Mono 600, 16, letter-spacing -0.3, `#FFFFFF`, `maxLines=1`.

**Row (`MetricStrip`)**
- 1 px `#FFFFFF1F` top separator, 9 px top padding, 6 px gap between cells; cells are centre-aligned on dark surfaces and left-aligned on light surfaces (see Cell).
- Cell widths follow the Order Active strip (58 / 58 / 100 / 100); a pair cell is at least 100 wide.

**Value formats**
- Distance, speed, fuel economy and fuel flow: one decimal place.
- Elapsed time: compact (`2h13m`, `42s`, `3m15s`).
- Finished-trip and session time (same `TIME` label as live elapsed time): `XmYYs` with zero-padded seconds when minutes are shown (`45m12s`, `11m05s`), `42s` under a minute, `2h13m` from one hour.
- Counts (e.g. Points) use a thousands comma: `1,284`.
- Money: compact (`850`, `8.4k`, `1.2jt`), no `Rp` prefix. Exception: Earnings is `Rp24.500`, green `#22C55E`, right-aligned.
- Pairs are written `a/b` with no spaces around `/`. Missing data is `—`; a pair with both parts missing is `—/—`.

**Order (canonical, mandatory on every surface):** TIME → DIST → SPEED → COST/NET (or COST) → FLOW → AVG/INST (or AVG FUEL, or AVG/TOTAL on Sessions cards) → POINTS. A surface that does not show a metric keeps the relative order of the rest. Sessions cards are the documented exception for COST: it sits in its own row below the strip (§5a exception (5), 2026-10-11). Earnings is not part of a strip; it sits in the order card's pay row.

**Components (one per metric type; the label and unit are fixed, an instance may override only the value and the width)**

| Kind | Component | Label | Example value |
|---|---|---|---|
| Standalone | `Metric/Time` | `TIME` | `42m35s` |
| Standalone | `Metric/Distance` | `DIST (km)` | `6.1` |
| Standalone | `Metric/Speed` | `SPEED (km/h)` | `47.2` |
| Standalone | `Metric/FuelFlow` | `FLOW (L/h)` | `4.2` |
| Standalone | `Metric/Earnings` | `EARNINGS` | `Rp24.500` |
| Standalone | `Metric/AvgSpeed` | `AVG SPEED (km/h)` | `33.5` |
| Standalone | `Metric/FuelEconomy` | `AVG FUEL (km/L)` | `8.5` (`—` when no OBD fuel) |
| Standalone | `Metric/Cost` | `COST (Rp.)` | `8.2k` (`—` when no price) |
| Standalone | `Metric/Points` | `POINTS` | `1,284` |
| Pair | `Metric/CostNet` | `COST/NET (Rp.)` | `3.5k/21k` |
| Pair | `Metric/AvgInst` | `AVG/INST (km/L)` | `8.5/6.2` (average first, then instant) |
| Pair | `Metric/FuelAvgTotal` | `AVG/TOTAL (km/L/L)` | `10.4/0.5` (average km/L first, then total fuel in L; `—/—` when no OBD fuel) |

**Label vocabulary (one name per quantity):** `TIME` is used for every elapsed time — live trip, finished trip and session — and `DURATION` is not used. `SPEED (km/h)` is the current speed and `AVG SPEED (km/h)` the average. `AVG/INST (km/L)` is the live pair; a finished trip, which only has the average, shows `AVG FUEL (km/L)`. `FLOW (L/h)` is fuel rate. `COST (Rp.)` is cost alone and `COST/NET (Rp.)` the cost/net pair. On a Sessions card the pair `AVG/TOTAL (km/L/L)` shows the session average fuel economy (km/L) first, then the total fuel consumed (L, one decimal): the average is `—` when covered distance = 0 or fuel ≤ 0.01 L, the total is `—` when no OBD fuel was recorded (§4).

All components nest a shared `Metric/Base`, so a base style change propagates to every type.

**Light surface:** the label, value and separator colours are themed variables (`metric-label`, `metric-value`, `metric-sep`; theme axis `surface`, default `dark`). Light surfaces (List, Sessions) set `theme: {surface: "light"}` on the strip instance: label `#737373`, value `#0A0A0A`, separator `#0000001F`. Inside cards the strip's separator and top padding are turned off. **Alignment (2026-10-11):** light surfaces left-align their cells (label and value start at the cell's left edge); dark surfaces stay centred. The design is already updated in `ui-design.pen` (the five light strips: 2 Sessions cards, 3 List rows); `Metric/Base` is unchanged (centred, the dark default).

**Scope:** Track-screen surfaces (Order Active, Expanded and Strip Only trip strips; LIVE and READY `TripPanel`; Alt A), the List recent-trip rows (§3b) and the Sessions cards (Time / Distance / Avg speed / AVG/TOTAL fuel / Points in the strip, plus a COST row below it, 2026-10-11). Alt B (Dashboard Panel, an exploration) uses the same strip for its stats row: DIST (km) / SPEED (km/h) / AVG/INST (km/L), the pair showing `—/—` until OBD data arrives. **Documented exceptions:** (1) in the Strip Only card the strip uses a 2 px cell gap and a 90 px AVG/INST cell so it clears the expand chevron; (2) the completed-trip cost value keeps the alternating `TripInk`/`TripGreen` colour of §3b #10 as a value-colour override; (3) for the List rows the design uses the §5a value size (Roboto Mono 600, 16) and label-above-value, left-aligned cells on the light surface (2026-10-11; earlier centred), superseding the 14sp Medium value styling of §3b #7 — implemented in Compose 2026-10-11 (static review; device check pending); (4) the List rows and the Sessions cards show five cells in one row with a smaller type size (label 8, value 12, below the usual 10 label size; Sessions cell widths 52 / 46 / 82 / 90 / 48 with a 2 px gap) so the row fits the card width — revisit if on-device legibility is poor; (5) (2026-10-11) on Sessions cards, COST is a separate second row below the five-cell strip, not a sixth strip cell: a single left-aligned `COST (Rp.)` cell (label 8 / value 12, light surface, same cell styling as the strip), full card-content width, with the same 12 px vertical gap used between card sections. This departs from the canonical order (COST normally sits between SPEED and AVG/TOTAL) because the five-cell strip has no room for a sixth cell. The cell is not tappable on the card. Design in `ui-design.pen`; code committed `7760d48` (static review; device check pending).

**Code-side decisions (2026-10-11; implemented in Compose, static review, device check pending):**
- (a) **Strip Only card:** the AVG/INST cell uses weight 1.7 as a stand-in for the 90 px width in exception (1), because the Compose strip lays out cells by relative weight. Revisit if the expand chevron overlaps the cell on device.
- (b) **List live row (`ActiveTripRow`):** ONE row of six compact cells in canonical order TIME / DIST (km) / SPEED (km/h) / COST/NET (Rp.) / FLOW (L/h) / AVG/INST (km/L), weights 44 / 40 / 60 / 46 / 40 / 70. This supersedes the two-row live layout.
- (c) **List finished-trip row:** ONE row of five compact cells TIME / DIST (km) / AVG SPEED (km/h) / COST (Rp.) / AVG FUEL (km/L), weights 48 / 44 / 82 / 52 / 76, card height 132 dp. The order price / net moves to the order header column. The 132 dp height may clip non-header order rows slightly; check on device.
- (d) **Light-surface alignment (2026-10-11; implemented in Compose, static review, device check pending):** cells on light surfaces (List rows, Sessions cards incl. COST row) are left-aligned; dark Track strips stay centred. `MetricCell` derives alignment from its theme (commit `2b64eaf`; IMPLEMENTATION-PLAN §4 How to Verify item 8).

---

## 5b. Fuel Dock (2026-10-11) — designed in `ui-design.pen`; code implemented (static review)

**Behaviour:** ADR-027 and PRD FR-22. This section covers the visual and layout spec only.

**Status:** Dock, edit pencils, the three dialogs (Fill up, Reset fuel, Fuel price) and the dock states are designed directly in `ui-design.pen` (2026-10-11 design update; the dock was first added in commit `f39e1d4`, which also put a dock on OBD Settings; the update removes it). The dialogs were designed in the `.pen` file itself, so no external design handoff is needed. Code implemented 2026-10-11 (`a197453`, `36030ef`, `9d01688`, mount fix `3354689`). `:app:assembleDebug` BUILD SUCCESSFUL; partly device-verified 2026-10-11 (screenshots; per-item status in IMPLEMENTATION-PLAN §4 How to Verify). The state thresholds and colours were confirmed by the project owner on 2026-10-11 (ADR-027 Decision 9).

**Placement:** a persistent dock, full width, **64 px high**, absolutely positioned directly **above the bottom navigation**. It is part of the screen scaffold on the screens listed below, not a per-screen element. Panels and map buttons on Track move up to clear it (see layout shifts).

**Variants:**
- `FuelDock/Dark`: map and Track screens (Track and all Track variants).
- `FuelDock/Light`: list-style screens (List, Sessions, Settings).

**Cells (left to right):**

| Cell | Value | Tap |
|---|---|---|
| FUEL LEFT (L) | Fuel balance, one decimal (e.g. `40.0`); `—` until a Reset | The **pencil** in the same row, right of the value, opens the **Reset fuel** dialog (the cell itself is not the tap target) |
| RANGE (km) | Fuel left × rolling average km/L over the last 1.0 km of moving OBD driving (ADR-027 Decision 5, rev. 1.7), whole km (e.g. `310`); previous value held until the window covers 0.2 km and while stationary; `—` before any value has been shown since app start | None (read-only, no pencil) |
| PRICE (Rp/L) | Current shared fuel price (`FuelPriceController`, same as FR-12) | The **pencil** in the same row, right of the value, opens the **Fuel price** dialog (the FR-12 price editor) |
| Fill up | Green pill button | Opens the **Fill up** dialog |

Edit pencils are 12 px lucide pencil icons in the label colour, one for each of the FUEL LEFT and PRICE cells. Each pencil sits in the same row as its value, immediately to the right of the number (4 px gap, vertically centred), not at the right edge of the cell. The pencil is the tap target. Cells use the §5a cell format (label above value, label `NAME (unit)`, value Roboto Mono). Surface alignment follows §5a: centred on dark, left-aligned on light. Number formats: FUEL LEFT to one decimal, RANGE in whole km, PRICE in the §5a compact Rp format, as in the design; confirm at code time.

**Screens:**

| Screen | Dock | Notes |
|---|---|---|
| Track, and all Track variants (Alt A, Alt B, LIVE, Order (Strip Only), Order Active, Order Active (Expanded)) | Yes, Dark | Layout shifts below |
| List | Yes, Light | |
| Sessions | Yes, Light | |
| Settings | Yes, Light | No live data in the design: FUEL LEFT and RANGE show the Not set placeholder (`—`) |
| OBD Settings | No | Pushed sub-screen (`Settings → Tools → OBD`), no bottom navigation. The dock was briefly placed here in `f39e1d4`; removed in the 2026-10-11 design update |
| Details | No | Pushed screen, no bottom navigation |
| Observer Feed | No | Pushed screen, no bottom navigation |
| Observer Settings | No | Pushed screen, no bottom navigation |

**Layout shifts (to clear the dock):** on Alt A, Alt B and LIVE the trip panel moves up 4 px, and the map buttons and panels move up on those screens. Strip Only is shifted up in the same way. Order Active already had the dock. OBD Settings was reverted to its previous layout (no shift there, since it has no dock).

**Dock states (design frame `Fuel Dock states`, variants of the Light dock):**

| State | When | FUEL LEFT | RANGE |
|---|---|---|---|
| Not set | Before the first Fill up or Reset | `—` | `—` |
| Normal | Fuel left at or above 6 L | e.g. `24.0`, normal colour | e.g. `310`, normal colour |
| Low | Fuel left below 6 L | value in amber `#D97706` | amber `#D97706` |
| Critical | Fuel left below 3 L | value in red `#DC2626` | red `#DC2626` |
| Low confidence | More than 10% of the distance in the active session was not covered by OBD (never shown when no session is active) | value prefixed with `~` (e.g. `~24.0`) | prefixed with `~` (e.g. `~310`) |

The thresholds (6 L, 3 L, 10%) and the colours (amber `#D97706`, red `#DC2626`) were confirmed by the project owner on 2026-10-11 (ADR-027 Decision 9). Normal and Not set keep the normal value colour and the `—` placeholder.

**Fill up dialog (designed, frame `Dialog Column`, FillUpDialog):**
- Title: Fill up.
- Field **Litres added (L)**: required, decimal, must be greater than 0.
- Field **Price (Rp/L) - optional**: pre-filled with the latest shared price. Helper: "Price defaults to the latest. Changing it saves a new price from now."
- Default price kept: no price row is written.
- Price changed: an effective-now price row is appended, the same as a Save in the FR-12 cost editor (ADR-008). The PRICE cell then shows the new price.
- Actions: Cancel / Save.

**Reset fuel dialog (designed, frame `Dialog Column`, ResetFuelDialog):**
- Title **Reset fuel**; field **Fuel in tank now (L)** (required). Helper: "Sets the starting amount. Fuel left counts down from here."
- Saving stores a `RESET` event and restarts FUEL LEFT from that amount.
- Entry point: the pencil in the FUEL LEFT row, right of the value (decided 2026-10-11).
- Actions: Cancel / Save.

**Fuel price dialog (designed, frame `Dialog Column`, FuelPriceDialog):**
- Title **Fuel price**; field **Price (Rp/L)**. Helper: "Applies from now. Finished trips keep their price."
- It is the FR-12 price editor shown as a dialog: Save / Cancel, with undo / redo as in FR-12. Saving appends an effective-now `fuel_price` row (ADR-008).
- Entry point: the pencil in the PRICE row, right of the value. This is the only price entry point on the dock; the Trips header fuel-pump icon is removed (2026-10-11).

**`—` rules:**
- FUEL LEFT shows `—` until one Reset has been done.
- RANGE shows `—` when FUEL LEFT is `—`, or before any value has been shown since app start. The rolling window covers the last 1.0 km of moving driving (speed ≥ 2 km/h; idle polls do not enter it). The average is shown once the window covers at least 0.2 km and its litres are above 0.01 L; otherwise, and while the car is stationary, RANGE holds the previous value. RANGE does not depend on the active session (ADR-027 Decision 5, rev. 1.6 and 1.7).
- Idle (engine on, car stationary, valid OBD fuel rate) lowers FUEL LEFT, and so RANGE (fuel left × the held km/L), with no distance added (ADR-027 Decision 13).
- PRICE shows `—` when no fuel price is set.

---

**Implementation notes (2026-10-11, code `36030ef`):**
- (a) Dark dock background uses `TripHeroBg` (`#173A2D`, opaque) with `PanelBorder`, not the semi-transparent `PanelBg`, because nothing sits behind the dock.
- (b) The Fill up pill uses `#22C55E` (`TripHeroBrandGreen`); `TripGreen` (`#16A34A`) does not match the design.
- (c) Each pencil is drawn at 12 dp, but its clickable box is 28 dp, below the 48 dp touch-target guideline. This is a deliberate deviation to keep the 64 dp dock layout; flagged for owner review.
- (d) Range is shown as a whole number, fuel left with one decimal, and price with dot thousands separators and no "Rp".
- (e) TalkBack: each cell has a contentDescription `LABEL, value`, but child texts are not hidden, so some screen readers may read both. To check on device.
- (f) Fill up writes `pricePerLiter` and a new `fuel_price` row only when the entered price is > 0 and differs from the current price.
- (g) Fill ups made before the first Reset are ignored; fuel left stays `—` until a Reset.
- (h) Litres inputs accept a comma decimal separator.
- (i) Mount point (2026-10-11, fix `3354689`): the dock is mounted in `MainActivity` through `ui/components/FuelDockHost.kt` (Scaffold `bottomBar`, above the bottom navigation). The first code mounted it in `screens/MainScreen.kt`, which no code calls; that file was dead code and was deleted 2026-10-11.
- (j) Reset accepts 0 L (verified on device 2026-10-11: typing 0 enables Save and the dock shows 0.0; the dialog opens empty with a grey 0 hint, Save is disabled while the field is empty).
- (k) Device status (2026-10-11, partial, screenshots): dock on Session, List (light), Track (dark, plain) and Settings (light); absent on OBD Settings and Trip Details. Dialogs and persistence checked; FUEL LEFT Low (amber) and Critical (red) seen on the light and dark docks; the `~` state, RANGE colouring, the changed-price flow, TalkBack and the Alt / Order variants are not verified yet. See IMPLEMENTATION-PLAN §4 How to Verify.
- (l) Contrast, owner review (device 2026-10-11, not changed): the red `#DC2626` FUEL LEFT value on the dark Track dock (`#173A2D`) is readable but has lower contrast than on the light dock. The owner decides whether to adjust it.

## 5c. Floating Status Banner (2026-10-11) — designed in `ui-design.pen`; code implemented `8cd95f8`

**Behaviour:** ADR-028 and PRD FR-23. This section covers the visual and layout spec only.

**Purpose:** Tell the driver, on the main screens, when the OBD adapter is not connected or when the Observer accessibility service is not active. The existing Observer Feed `ServiceBanner` only helps while the Observer screen is open (ERR-010).

**Placement and routes:** one overlay, `StatusBannerHost`, mounted in `MainActivity` over the nav graph (same host point as the fuel dock, §5b). It sits at the **top** of the screen, below the status bar, at x = 16 and about y = 48 px, full width minus the 16 px gutters. It does not sit on the bottom navigation or the fuel dock.
- Shown on: Track (all variants), Sessions, Trips (List) and Settings.
- Hidden on: Details, OBD Settings, Observer Feed and Observer Settings.
- **This route list must stay in sync with the fuel dock list in §5b.** Change both together.

**States (eight, from `ui-design.pen`):**

| # | State | Chips shown | Screen shown on | Where in `ui-design.pen` |
|---|-------|-------------|-----------------|--------------------------|
| 1 | OBD not connected | OBD chip (amber, expanded) | Sessions (light) | "State 1 OBD not connected" (a Track variant on the dark map is also in the file) |
| 2 | OBD connecting | OBD chip ("OBD connecting…") | Sessions (light) | "State 2 OBD connecting" |
| 3 | Observer inactive | Observer chip (red, expanded) | Sessions (light) | "State 3 Observer inactive" |
| 4 | Both problems, stacked | Observer chip on top, OBD chip below | Sessions (light) | "State 4 both stacked"; "State 4 on Track" (dark map) shows the same on Track |
| 5 | All good | None (no banner) | Sessions (light) | "State 5 all good (no banner)" |
| 6 | Both collapsed | Two 44 px icons, Observer on top | Sessions (light) | "State 6 both collapsed (Sessions)" |
| 7 | Observer only, collapsed | One 44 px red eye-off icon | Sessions (light) | "State 7 Observer only collapsed (Sessions)" |
| 8 | Both collapsed | Two 44 px icons, Observer on top | Track (dark map) | "State 8 both collapsed (Track)" |

**OBD state mapping (from `ObdPollingService.obdUiState`):** `Connected` shows no OBD chip. `Connecting` and `Retrying` show "OBD connecting…". `Idle` and `Waiting` show "OBD not connected". The OBD chip also shows when the OBD feature is turned off in OBD Settings (owner decision: always show; no hiding when disabled).

**Chip anatomy and dimensions (expanded):**
- Width 358, height 48, corner radius 14, 1.5 px border, soft shadow.
- Left: state icon (amber Bluetooth-off for OBD, red eye-off for Observer), then a title line and a hint line.
- Right: a divider, then the minimize button (32 px visual, 48 dp hit area).
- When both chips are shown, the Observer chip stacks on top of the OBD chip.

**Colours (tokens):**

| Token | Hex | Use |
|-------|-----|-----|
| warn-bg | `#FFF7E6` | OBD chip background |
| warn-border | `#F5B041` | OBD chip border |
| warn-ink | `#7A4A00` | OBD chip title and hint text |
| warn-accent | `#D97706` | OBD chip icon and collapsed icon |
| err-bg | `#FEECEC` | Observer chip background |
| err-border | `#F08A8A` | Observer chip border |
| err-ink | `#8A1C1C` | Observer chip title and hint text |
| err-accent | `#DC2626` | Observer chip icon and collapsed icon |

**Copy:**

| Chip | Title | Hint line |
|------|-------|-----------|
| OBD (Idle, Waiting) | OBD not connected | Tap to open OBD Settings |
| OBD (Connecting, Retrying) | OBD connecting… | Tap to open OBD Settings |
| Observer | Observer inactive | Orders won't be captured · Tap to open Observer Settings |

**Tap targets:**
- **Body of an expanded chip:** navigates in-app (revised 2026-10-14; previously opened system settings). OBD opens the **OBD Settings** screen (`Screen.ObdSettingsScreen`); Observer opens the **Observer Settings** screen (`Screen.ObserverSettingsScreen`). Both are outside the banner routes, so the banner hides there; Back returns to the previous screen. The system Bluetooth and Accessibility settings stay reachable from inside those screens (OBD Settings "Pair a new device"; Observer Settings accessibility button). The Observer Feed `ServiceBanner` "Open settings" button is unchanged. No in-app Bluetooth discovery or PIN entry.
- **Pen frames:** `ui-design.pen` still shows the old hint text ("Tap to open Bluetooth settings", "Tap to fix") until the StatusBanner frames are touched up. Treat the copy and tap-target text in this section as authoritative.
- **Minimize button:** collapses the banner to icon-only chips.
- **Collapsed icon:** expands the banner again. It does **not** open settings, to avoid accidental taps.

**Collapsed icons:** 44 px circles at the top right, stacked vertically with Observer on top when both are shown. Amber Bluetooth-off for OBD; red eye-off for Observer.

**Collapse memory:**
- The collapsed or expanded choice applies to all screens where the banner is shown.
- It is held in memory and lasts for the app session only. It resets on app restart, so a warning can never stay hidden forever.
- If a problem state changes (for example OBD goes from connected to disconnected, or the Observer service is turned off again), the affected chip re-expands even if it was collapsed.

**Accessibility:**
- Do not rely on colour alone: every chip has an icon and text, and the collapsed icons use shape (Bluetooth-off, eye-off) as well as colour.
- TalkBack reads the title plus the action, for example "OBD not connected. Opens OBD Settings" and "Observer inactive. Orders won't be captured. Opens Observer Settings".
- Collapsed icons read their title and the expand action (wording to confirm at code time).
- Touch targets: collapsed icons 44 px; minimize button 32 px visual with a 48 dp hit area.
- Check at code time that the banner does not hide the Track top controls or the status bar.

**Status:** designed directly in `ui-design.pen` on 2026-10-11 (the "StatusBanner Components" section: Banner/OBD Not Connected, Banner/OBD Connecting, Banner/Observer Inactive, Banner/Collapsed OBD, Banner/Collapsed Observer; and states 1 to 8). No external design handoff is needed, the same as §5b. Code implemented `8cd95f8`; partly device-verified 2026-10-11 (see IMPLEMENTATION-PLAN §4 slice "Floating Status Banner (ADR-028)"). Not tested on device: the OBD Connected state (chip hidden), re-expansion on a real Connected to not connected change, restart reset to expanded, TalkBack wording of the chips and collapsed icons, and a trip or session started with the banner active. Cosmetic: the chips cover the page title area and overlap the top edge of the Session Always-recording card.

---

## 5d. Session Overlay Banner (ADR-029) — designed and approved in `ui-design.pen`; not implemented

**Behaviour:** ADR-029 and PRD FR-24. This section is the visual and layout spec only.

**Purpose:** Show the live session figures (fuel left, range, fuel consumption, cost and income) while the driver uses another app, such as the Gojek driver app, without switching back to TrackLocation.

**Source of truth:** the frames in `ui-design.pen` are the visual source of truth. They are edited through the Pencil MCP only. Section frame: **"Session Overlay Banner (ADR-029)"** (id `GjDVg`). Components:

| Component frame | Id | Component frame | Id |
|-----------------|----|-----------------|----|
| Overlay/Strip Dark | `uBWY3` | Overlay/FillUpForm Dark | `R4Y6qP` |
| Overlay/Strip Light | `vcWqE` | Overlay/FillUpForm Light | `vsy9e` |
| Overlay/Circle Dark | `r9QAZL` | Overlay/MockKeyboard (helper) | `O9Ef71` |
| Overlay/Circle Light | `QxDIj` | Overlay/Backdrop Map (helper) | `u7vww` |

Use the frame names first; the ids are a secondary reference. No external design handoff is used (ADR-029 Decision 8), the same as §5b and §5c.

**Placement and visibility:**
- A **system overlay** drawn above other apps (`TYPE_APPLICATION_OVERLAY`, hosted by a foreground service). It is not part of the in-app nav graph and is not mounted in `MainActivity`. The in-app `StatusBannerHost` (§5c) is unchanged.
- Shown only while **both** a recording session is active **and** TrackLocation is in the background. Removed when the session ends.
- Not shown on any TrackLocation screen, so the route list in §5c and §5b does not apply.
- Design units: frame pixels are treated as dp in code (the design frame is 390 px wide).

**Strip (expanded): one row.** Frame states 1, 2, 3 and 8 (instances ZJeoW, Hd2om, g9vmg and JDbsT).
- One row, **382 px wide** (4 px side gutters on a 390 px screen), **52 px high**. When the optional PARTIAL line is shown below the row, the strip is **72 px high**.
- Inner padding: left 12, right 6, vertical 4. Gap between items: 4 (2026-10-13; never less than 4). No wrapping.
- Order, left to right: **FUEL LEFT | RANGE | FUEL CONS | COST | INCOME | Fill up | shrink button**.

| Cell | Label (no units) | Width | Value | Format |
|------|------------------|-------|-------|--------|
| Fuel left | FUEL LEFT | 48 | Fuel left | `12.4`, litres implied; `—` until a Reset has been set (ADR-027) |
| Range | RANGE | 32 | Range | `310`, km implied; `—` as on the dock (§5b) |
| Fuel consumption | FUEL CONS | 62 | Session average / instant | `12.3/8.1`, km/L implied; no spaces around `/`; `—` when there is no OBD data |
| Cost | COST | 40 | Sum of litres × pump price over orders | Compact, e.g. `1.2jt`, Rp implied |
| Income | INCOME | ~76 (fills the rest) | Gross / net over orders | `8.4k/6.0k`, Rp implied; no spaces around `/` |

- Labels are 9 px. Values are 16.5 px (1.5x, 2026-10-13) bold, tracking 0, in the condensed system face `sans-serif-condensed` (Roboto Condensed in the design). The condensed face is what lets 1.5x fit: the monospace face at 1.5x overflowed the fixed-width cells on device (2026-10-13), and 1.25x monospace (13.75 px) was the earlier fallback. Cell widths 48 / 32 / 62 / 40 with a 4 dp gap (2026-10-13, design frames Overlay/Strip Dark and Overlay/Strip Light in `ui-design.pen`). Do not go below these minimums.
- Compact Rupiah (FR-16): below 1,000 → `850`; 1,000 to 999,999 → `8.4k`; from 1,000,000 → `1.2jt`. A negative value carries a real minus sign `−`.
- Value examples that must not clip: `12.45`, `12.5jt`, `15.0k/−3.5k`.

**Fill up button (in the strip):** icon-only **green 44 × 44 px circle** with a fuel icon. Accessible name "Fill up". Opens the Fill-up form (below). It is not on the collapsed circle.

**Shrink button:** **44 × 44 px hit area** with a **32 px visual**, solid surface fill. Tapping it collapses the strip to the circle. Accessible name "Shrink to circle".

**Surface and text (translucent, so the app below shows through):**
- The strip surface uses the strip-only token `ov-surface-glass`: dark `#0E2A209E` (about 62%), light `#FFFFFFB8` (about 72%).
- Text, the green Fill up button and the shrink button stay **opaque**.
- Text uses **halo text** for contrast: an outer shadow with offset 0, blur 3, colour `ov-halo` (dark theme: black about 70%; light theme: white about 90%).
- Strip-only text tokens: `ov-label` and `ov-value`. Dark: white 94% (label) and 100% (value). Light: `#3F3F3F` (label) and `#0A0A0A` (value).
- The halo is not adaptive. The overlay cannot read the pixels under it without screen capture, which was rejected for privacy.
- **Known weak spots to check on a device:** dark-strip labels over pure white (about 4.5:1); light strip over red (about 4:1); the red net value over mid-grey.

**PARTIAL line (below the row, optional):** shown only when the totals exclude some orders. A bordered **PARTIAL** tag with a warning icon, and the note "Orders without OBD or earnings excluded". The tag and note are text, not colour.

**Net loss:** INCOME shows `15.0k/−3.5k`. The net part is red (`ov-loss`) and has a leading real minus sign. The minus sign is the non-colour cue. The trend-down icon is intentionally not used (no width for it).

**No-data state:** all five cells show a single `—`.

**Collapsed circle.** Frame state 4 (`l1w29`).
- A **48 px circle** with a fuel icon and the **fuel-left litres** (for example `12.4`, litres implied, the same value as the strip's FUEL LEFT cell). A small grip bar at the bottom is the drag affordance.
- **Ring colour = fuel level band** (the same bands as the §5b Fuel Dock, from `FuelBalance.level`): **Normal** green, **Low** amber (`#D97706`) below 6 L, **Critical** red (`#DC2626`) below 3 L. The band is the non-colour cue together with the litres value; the ring is not the only signal. `—` in the circle when there is no Reset yet (as on the strip).
- **Design deviation (owner-visible, 2026-10-13):** the implemented circle shows litres, not a percentage. The pen frames for this circle (`Overlay State 4`, component `Overlay/Circle`) still draw `62%`, because the code has no tank capacity, so a percentage cannot be computed. A percentage needs a tank-capacity setting, which is a product decision not yet made. **The pen circle frames must be updated to litres and level colour (or a tank-capacity setting specified) before this circle is treated as final.** Until then, this paragraph governs and the `62%` drawings and the TalkBack wording below are superseded by it.
- It has **no** Fill-up action. Its only tap restores the strip.
- **Drag:** moves the circle freely. A small touch slop separates a drag from a tap, so a drag never restores the strip.
- **Tap:** restores the strip. It does **not** open the Fill-up form.
- **Clamp and persistence:** the circle is clamped to the screen with an 8 dp margin and stays clear of the status bar and the navigation-bar insets. Its position is persisted (DataStore, ADR-029 Decision 5) and clamped again after rotation or a screen-size change.
- State 4 shows a ghost at the old spot, a move arrow, and the caption "Drag to move · tap to expand". The caption is a design annotation, not UI.

**Fill-up form (inline panel in the overlay).** Frame states 5 (`B56hLY`, keyboard open) and 6 (`woEUQ`, invalid litres). Frames FillUpForm Dark and Light.
- Opens from the strip's Fill up button. It is an inline compact panel, not a separate dialog window.
- Placed in the **upper part of the screen**, with the numeric keyboard below it, so the Save row is never covered.
- Overlay takes input focus while the form is open. When it closes, the overlay returns to pass-through.
- Title: **Fill up**.
- Two side-by-side fields:
  - **Litres added (L)**: required. Focus is placed here when the form opens.
  - **Price (Rp/L) - optional**: pre-filled with the latest price. Changing it saves a new price from now on (ADR-008). Helper text: "Price defaults to the latest. Changing it saves a new price from now."
- **Cancel** and **Save** buttons, **48 px high**.
- **Save is disabled until litres are valid** (greater than 0). The Save label uses **dark text on the green fill**, because white on green has too low a contrast.
- **Error state (invalid litres):** the litres field has a red border and the message "Enter litres greater than 0".
- The form is dismissed by **Cancel**, **Save**, or **shrinking** the overlay.
- A saving spinner icon exists in the component, off by default. There is no dedicated "Saving…" frame.
- A price equal to the latest, or blank or 0, writes no price row. A price above 0 that differs from the latest appends an effective-now price row (ADR-008).
- Keyboard and font scale: the Save and Cancel row stays visible while the keyboard is shown. At the largest system font size the form wraps and scrolls inside the panel rather than clipping Save or Cancel.

**Fill-up TalkBack:** the button reads "Fill up". On open, the form is announced as "Fill up form" and focus goes to the litres field. A disabled Save reads "Save, unavailable" with the reason. On success, a polite live announcement reads "Fill-up saved. Fuel left N litres." On error, the error message is announced politely. The helper and error text are linked to their fields.

**Permission prompt (not granted).** Frame state 7 (`jnz6e`).
- A **centred dialog** over a dimmed backdrop, in the FillUpDialog look. No overlay is drawn while the permission is missing.
- Title: "Show session totals over other apps".
- Body: "Display over other apps is needed to show fuel left, range and session earnings on top of navigation and driver apps while a session is recording." Followed by "Tracking and recording keep working without it."
- Buttons: **Not now** and **Open settings**. Open settings deep-links to the system overlay-permission screen (`Settings.ACTION_MANAGE_OVERLAY_PERMISSION`).
- Shown **once** (one-time prompt). It is not a card on the Session screen.

**States (frame map):**

| # | Frame | Id | What it shows |
|---|-------|----|---------------|
| 1 | Strip normal | `i8qsNW` | All five cells with values, Fill up and shrink |
| 2 | Strip no data | `vWXqS` | All five cells show `—` |
| 3 | Strip negative net + partial | `Z3kpsG` | INCOME `15.0k/−3.5k` (red net, minus sign), PARTIAL line, 72 px strip |
| 4 | Circle collapsed, dragged | `l1w29` | Circle, ghost at old spot, move arrow, design caption |
| 5 | Fill-up form open with keyboard | `B56hLY` | Form in upper area, numeric keyboard below, Save row visible |
| 6 | Fill-up form error, invalid litres | `woEUQ` | Red litres border and "Enter litres greater than 0" |
| 7 | Permission prompt | `jnz6e` | Centred dialog over dimmed backdrop |
| 8 | Light-theme strip | `C4M54` | Light strip (`vcWqE` component) |

**Accessibility:**
- Touch targets: Fill up 44 px; shrink 44 px hit area; collapsed circle 48 px; form buttons 48 px high; the Allow/Open settings button at the normal button size (48 px or more).
- Do not rely on colour alone. Every cell has a text label. The loss uses a minus sign as well as red. The PARTIAL marker is text.
- The strip keeps the value minimums above at the system font scale or cells clip. Check at code time.
- **TalkBack wording (per cell):**
  - Fuel left: "Fuel left 12.4 litres"
  - Range: "Range 310 kilometres"
  - Fuel consumption: "Fuel consumption average 12.3 instant 8.1 kilometres per litre"
  - Cost: "Cost 1.2 million rupiah"
  - Income: "Income gross 8.4 thousand, net 6.0 thousand rupiah". A loss is stated explicitly as "net loss".
  - Fill up button: "Fill up". Shrink button: "Shrink to circle".
  - Collapsed circle: "Fuel left 12.4 litres, level normal, drag to move, double tap to expand" (litres and level band; replaces the earlier percent wording, see the §5d circle deviation note).
- Check at code time that the overlay does not block the status bar or the system back gesture.

**Status:** designed and approved in `ui-design.pen` on 2026-10-13 (section frame "Session Overlay Banner (ADR-029)", eight state frames). Behaviour is recorded in ADR-029 (accepted 2026-10-13, not yet implemented) and PRD FR-24. Not implemented. Items to check on a device once implemented: the contrast weak spots listed under the strip, the halo legibility on both themes, and the TalkBack wording above.

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
- Fuel dock (§5b): designed in `ui-design.pen` (dock, edit pencils, Fill up / Reset fuel / Fuel price dialogs, dock states; 2026-10-11 update, `f39e1d4` added the first dock); state thresholds and colours confirmed 2026-10-11; code implemented 2026-10-11 (`a197453`, `36030ef`, `9d01688`, `3354689`; build OK; partly device-verified 2026-10-11).
- Floating status banner (§5c): designed in `ui-design.pen` 2026-10-11 (OBD not connected, Observer inactive, collapsed icons; eight states); code implemented `8cd95f8` (2026-10-11); partly device-verified 2026-10-11 (not tested: OBD Connected state, restart reset, TalkBack) (ADR-028).
- Session overlay banner (§5d): designed and approved in `ui-design.pen` 2026-10-13 (section frame "Session Overlay Banner (ADR-029)", eight state frames: one-row strip with Fill up, collapsed circle, Fill-up form, permission prompt); not implemented (ADR-029 accepted, FR-24).

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
- **Map Overlays:** The primary tracking interface uses a bottom-anchored persistent card. This card sits at a 16px margin from the fuel dock (§5b) on Track, which sits directly above the bottom navigation, and from the side edges to feel "floating" yet docked.
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
