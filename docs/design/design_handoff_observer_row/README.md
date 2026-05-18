# Observer Event Row — Claude Code Handoff

A small, focused UI update for the **event row** in the Observer Feed screen.

## What's in this bundle

| File | Purpose |
|---|---|
| **`OBSERVER_ROW_SPEC.md`** | The authoritative spec — layout, sizes, colors, chip rules, accessibility, acceptance checklist. Read this first. |
| `Observer Phase 1.html` | Live HTML prototype. Open in a browser to see the row in context. |
| `screen-observer.jsx` | The React/HTML reference for `EventRow` + `EventTypeChip`. Look here when the spec is unclear. |
| `observer-shared.jsx` | Shared tokens (colors, fonts). |
| Other `.jsx` files | The rest of the Observer screen (status bar, chips, app bar) — included for visual context only. Do **not** port these; they're already implemented. |

## Suggested Claude Code prompt

Paste this at your repo root:

> Update the **event row composable** for the Observer Feed screen in this Jetpack Compose + Material 3 app. Read `docs/design/design_handoff_observer_row/OBSERVER_ROW_SPEC.md` end-to-end before writing any code — it is the authoritative spec.
>
> **Scope:** row layout only. Do not touch the top app bar, status indicators (Service banner, Capture/Auto-scroll chips), feed header, allowlist bottom sheet, or navigation. Find the existing row Composable (likely `ObserverEventRow` or `EventRow` under the observer feature package) and replace its layout with the stacked one in the spec.
>
> Key rules that are easy to get wrong:
> - The chip's displayed text **strips the `TYPE_` prefix** (`WINDOW_CONTENT_CHANGED`, not `TYPE_WINDOW_CONTENT_CHANGED`). The full string stays in the row's content description and in any clipboard payload.
> - Activity name **wraps onto multiple lines**, no `maxLines` cap. Long fully-qualified names like `com.kolee.tracklocation.MainActivity` must not ellipsize.
> - Package name uses `word-break: break-all` semantics (break at any character) and is clamped to **2 lines max**.
> - Long-press copy must keep working — copies `"{package} | {activity}"` only when auto-scroll is paused.
>
> The `.jsx` and `.html` files in the same folder are visual references. Use them to disambiguate spacing/visuals. Do not port DOM markup — implement in Compose.
>
> Before you start, list the file(s) you plan to edit. Then implement. When finished, walk through the **Acceptance checklist** at the bottom of the spec and confirm each item.
