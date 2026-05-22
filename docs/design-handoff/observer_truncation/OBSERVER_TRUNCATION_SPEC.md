# Handoff: Observer Event Truncation Warning

## Overview

When Observer captures accessibility events, it bounds the node tree traversal with safety limits: maximum 200 nodes, maximum depth 10, maximum payload size 40 KB. If any limit is hit during capture, the `truncationMetadata` field records the reason and how many nodes were captured before cutoff.

Currently, `truncationMetadata` is stored in the database but **never shown to the operator**. This handoff adds a **compact warning banner** inside the existing `SnapshotViewerSheet` (the modal shown when the user taps "View window content" on an event card) that surfaces the truncation reason and node count only when truncation occurred.

Goal: let the operator know when an event's window tree was incomplete due to safety limits, without cluttering the UI for events that were fully captured (the common case).

## About the design files

This handoff **does not include HTML/React prototypes or design files**. Instead, it references an existing design pattern already in the TrackLocation app: the amber draft warning banner in `AllowlistBottomSheet`. The new truncation banner should follow that same visual treatment or a refinement of it, as approved by the design tool.

The task is to:
1. Review the 5 provided app screenshots (listed below).
2. Use the existing `AllowlistBottomSheet` amber banner as the visual reference.
3. Create a compact warning banner in the same style inside `SnapshotViewerSheet`.

## Fidelity

**High-fidelity reference:** The existing amber draft banner in `AllowlistBottomSheet` (see screenshot 3 below). Match its color palette, typography, padding, icon treatment, and border radius for consistency.

## Where this feature lives

- **Surface**: `SnapshotViewerSheet` modal (already implemented in `SnapshotViewerSheet.kt`).
- **Position**: Inside the `ContentArea`, **above** the formatted node list or raw JSON text.
- **Visibility**: Only when `truncationMetadata` is non-null and parses to a valid `{ reason, nodesCaptured }` object.
- **Entry path**: Settings → Tools → Observer → tap event card → tap "View window content" → (tap event card that was truncated).

## Screens / views

### 1. Truncation warning banner

**Placement**
- Inside `ContentArea` of the sheet, immediately above the node list (Formatted mode) or code block (Raw JSON mode).
- Spans the full width of `ContentArea`; inherits the 10–16 dp horizontal padding from the content area.

**Visual**
- Compact, single-line (or 2-line if reason is long).
- Amber-tinted background (reference: `AllowlistBottomSheet` draft banner background).
- Warning icon (14×14 dp, left-aligned) + reason text (right of icon) + node count (far right).
- Type: Roboto 13 sp, weight 500, color: amber tone (reference screenshot 3).
- Padding: 12 dp vertical, 12 dp left (icon + gap), 12 dp right.
- Border radius: 8–10 dp (consistent with the reference).
- Optional 1 dp amber-tinted border (reference the draft banner's border styling).

**Content**
- Left section: Warning icon (e.g. `Icons.Outlined.Warning` or similar) in amber.
- Center section: `"Tree truncated — {reason_label}"`
  - Reason labels:
    - `node_limit` → "node limit"
    - `depth_limit` → "depth limit"
    - `size_limit` → "size limit"
  - Example full text: `"Tree truncated — node limit (200 nodes captured)"`
- Right section: `"(N nodes captured)"` in a muted tone (e.g. `T.ink2` or `T.ink3`).

**Visibility rules**
- Banner hidden when `truncationMetadata == null`.
- Banner hidden when `truncationMetadata` fails to parse (graceful degradation; no error state).
- Banner visible only when parsing succeeds and a recognized `reason` is found.

**Interaction**
- No tap target. Banner is informational only.

---

## States

### State A: No truncation (normal case)
- `truncationMetadata` is null or absent.
- Banner is not rendered.
- `ContentArea` shows the formatted node list or raw JSON with no overlay or warning.
- ContentArea background: `#fbfcfa` (from reference spec).

### State B: Truncation present — Formatted view
- `truncationMetadata = { reason: "node_limit", nodesCaptured: 200 }`
- Amber warning banner renders above the node list with text `"Tree truncated — node limit (200 nodes captured)"`.
- User can scroll the node list below the banner to see the captured nodes.

### State C: Truncation present — Raw JSON view
- Same `truncationMetadata` as State B.
- User has tapped the "Raw JSON" segment of the mode toolbar.
- Amber warning banner still visible above the JSON code block.
- JSON displays the raw truncated payload (what was successfully captured before the limit).

---

## Design tokens

Use the existing app palette. The amber warning reference is in the `AllowlistBottomSheet` draft banner:

| Token                | Value / Reference                     | Usage                                |
|---|---|---|
| Background           | Amber tint (reference screenshot 3)   | Banner background                    |
| Border               | Amber tint, 1 dp (optional)          | Banner edge                          |
| Icon                 | Amber tone (reference screenshot 3)   | Warning icon fill                    |
| Text                 | Amber tone (darker, reference scr 3)  | Reason label                         |
| Count text           | `T.ink2` or `T.ink3`                  | Node count (secondary)               |
| Padding              | 12 dp vertical, 12 dp horizontal      | Banner inside edges                  |
| Border radius        | 8–10 dp                              | Consistent with design              |

**Type**
- Sans: Roboto 13 sp, weight 500.

**Icon**
- Warning icon: `Icons.Outlined.Warning` or equivalent Material icon in amber.

---

## Interactions & behavior

- **No interaction.** Banner is read-only and informational.
- **Persistence.** Banner appears whenever `truncationMetadata` is present, across both Formatted and Raw JSON modes.
- **Scroll.** Banner is pinned to the top of `ContentArea` and does not scroll with content.

---

## Edge cases

1. **Truncation with parse error in metadata.** If `truncationMetadata` contains invalid JSON or an unrecognized `reason`, the banner is **not rendered** and the sheet displays content normally. No error badge is shown.

2. **Very long reason label.** If the reason text is unexpectedly long (though the 3 known reasons are short), the text wraps to a second line naturally. Banner expands vertically.

3. **Very large node count.** Format as `(200 nodes captured)`, `(999 nodes captured)`, etc. No comma-thousands separators needed; counts are typically under 1000.

---

## Screenshots to attach to the design tool

Capture these from the running Android app. They establish the design language the truncation banner must match.

| # | Screen / State | Purpose |
|---|---|---|
| 1 | **SnapshotViewerSheet — Formatted view, event with no truncation** (e.g. a fresh event with a small tree) | Primary surface: shows `ContentArea` background color (`#fbfcfa`), padding, and the node list layout where the banner will be placed above |
| 2 | **SnapshotViewerSheet — Raw JSON view** (toggle mode on the same sheet) | Second mode: shows the raw JSON view and `ContentArea` appearance; banner must sit above the code text in this mode too |
| 3 | **AllowlistBottomSheet — at least one draft allowlist rule visible (amber draft banner displayed)** | Existing design reference: shows the amber background color, amber text tone, icon style, padding, and border radius for the draft warning banner that this truncation banner should match or refine |
| 4 | **Observer Feed screen — several event cards visible, capture is running** | Ambient styling context: shows event type chips (their colors and tones), card density, padding, type scale, and overall Observer screen treatment for consistency |
| 5 | **Observer Feed screen — at least one event card with "View window content" link visible** | Entry point: confirms the tap flow that leads to the sheet and shows the card styling context |

**Submission order:** Attach screenshots 1 and 2 first (the primary surface); then 3 (the amber reference pattern); then 4 and 5 (ambient context).

---

## State management / code notes

The `truncationMetadata` field is already:
- Captured in the database (via `ObserverAccessibilityService.captureTreeSnapshot()`).
- Persisted in `ObservedEventEntity` as a nullable `String` field.
- Mapped into the domain model `ObservedEvent` as `truncationMetadata: String?`.
- Passed to `SnapshotViewerSheet` as part of the `event` parameter.

Implementation should:
1. Check if `event.truncationMetadata` is non-null.
2. Attempt to parse it as JSON: `Json.decodeFromString<{ reason: String, nodesCaptured: Int }>(truncationMetadata)`.
3. If parsing succeeds, render the banner with the human-readable reason label.
4. If parsing fails or the `reason` is unrecognized, skip the banner (no error state).

No additional ViewModel, state machine, or repository changes are required.

---

## Open questions / assumptions

1. **Banner placement within ContentArea.** Assumed the banner sits above the scrollable node list / JSON, pinned to the top. If the ContentArea has sticky headers or other scroll anchors, confirm the banner is above them.

2. **Refining the amber color.** The reference screenshot 3 shows the existing draft banner's amber tones. Design may choose to use those exact colors or propose a refinement (e.g. a slightly different amber, or a different warning color entirely). If refining, ensure it harmonizes with the Observer color palette (event type chips, status indicators, etc.).

3. **Icon alternative.** The spec assumes `Icons.Outlined.Warning`. If a different icon better matches the design language (e.g. a rounded exclamation circle, or an info icon), substitute as approved.

4. **Multi-line wrapping.** The spec assumes reason text is short (node limit / depth limit / size limit), so wrapping is unlikely. If a future reason label is added that is longer, the banner should wrap naturally with the text shrinking or the banner expanding vertically.

---

## Handoff Instructions

### How to submit to a design tool

1. **Prepare 5 screenshots** from the running app (emulator or device):
   - SnapshotViewerSheet — Formatted view (event with snapshot, no truncation)
   - SnapshotViewerSheet — Raw JSON view (same sheet, toggle to Raw)
   - AllowlistBottomSheet — with a draft rule visible (amber draft banner showing)
   - Observer Feed screen — capture running, several event cards
   - Observer Feed — event card with "View window content" link visible

2. **Choose a design tool:**
   - Claude Design (best for high-fidelity designs matching your exact tokens)
   - Google Stitch (for interactive prototypes + design specs)
   - Figma (if your org uses it; you can iterate directly on the design file)

3. **Submit this prompt + screenshots to the tool:**

```
Design a truncation warning banner for the Observer Snapshot Viewer feature.

Reference spec: [This document — OBSERVER_TRUNCATION_SPEC.md]

Key points:
- Banner appears inside a modal sheet (SnapshotViewerSheet), above the content area
- Only shown when an accessibility event tree was truncated during capture
- Amber tint, matching the existing draft banner style in AllowlistBottomSheet (see screenshot 3)
- No interaction — read-only informational banner
- Must work in both Formatted and Raw JSON viewing modes
- Banner text: "Tree truncated — {reason} ({count} nodes captured)"
  - Reasons: node limit / depth limit / size limit

Please design the banner visual (colors, typography, padding, border radius, icon treatment) based on the existing app screenshots, matching the design language and tokens already in use.

Attached: this spec document + 5 reference screenshots
```

4. **Next:** Once design approves, implement the banner as a private composable inside `SnapshotViewerSheet.kt` (the only file that changes).

---

## Files in this bundle

- **This file** (`OBSERVER_TRUNCATION_SPEC.md`) — the specification, screenshots checklist, and handoff instructions.
- **No design canvas or prototype** — use the existing app screenshots (list above) as the reference.
