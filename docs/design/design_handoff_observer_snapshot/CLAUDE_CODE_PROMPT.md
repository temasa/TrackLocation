# Prompt for Claude Code

Paste this into Claude Code at the root of the TrackLocation repo. The
`design_handoff_observer_snapshot/` folder should sit somewhere readable
(repo root or `docs/`).

---

I'm implementing a new feature in the TrackLocation Android app: a
**snapshot viewer modal** for the Observer Feed. Each accessibility event
card gets a new **"View window content"** link, and tapping it opens a
Material 3 bottom sheet that renders the event's captured `treeSnapshot`.

Full design spec, state machine, tokens, and reference files are in
`design_handoff_observer_snapshot/README.md` — please read that first,
along with the HTML/JSX references in the same folder. They are
**design references, not code to copy**. Recreate them in our existing
Jetpack Compose codebase using the patterns already established in the
Observer screens.

## What I need you to do

1. **Audit the repo first.**
   - Find the existing Observer Feed screen and the event card composable.
   - Locate the `TreeSnapshot` (or equivalent) model on the event payload,
     and confirm its actual shape. The mock assumes
     `{cls, id?, text?, desc?, children[]}`; verify and adapt.
   - Identify the project's color tokens, type scale, and any existing
     ModalBottomSheet / segmented-control / pill-button patterns. Reuse
     them — do not invent new tokens unless the design demands a color
     the palette lacks.

2. **Implement the card link** (`View window content`) per spec section
   "Event card". Default to the *hide-when-missing* behavior. Wire the
   tap to a `onViewSnapshot(event)` callback exposed by the feed.

3. **Implement the snapshot bottom sheet** as a Composable —
   `SnapshotSheet(state, onModeChange, onCopy, onDismiss)`. Hoist state
   into the feed's ViewModel. Cover all four states:
   - `Ok` — Formatted tree (default) + Raw JSON toggle + Copy.
   - `ParseError` — inline error card; Copy disabled in Formatted,
     re-enabled in Raw JSON for the raw payload export.
   - `NoReadableText` — inline neutral message; Copy disabled in both
     modes.
   - (Missing snapshot is handled at the card level.)

4. **State machine.** See `## State management` in the README. Key
   invariants:
   - Mode resets to `Formatted` every time the sheet opens.
   - `copied = true` flips back to `false` after 1400 ms.
   - Sheet dismisses on: close button, system Back, scrim tap, swipe-down.

5. **Copy behavior.** Always copies what's currently rendered:
   - Formatted → plain-text tree (one node per line, two-space indent
     per depth, with `text:` and `desc:` on their own indented lines).
   - Raw → raw JSON string.
   Use `LocalClipboard` / `ClipboardManager.setPrimaryClip`. Surface a
   short Snackbar/Toast confirmation for TalkBack users.

6. **Tests / preview.**
   - Compose previews for each state (Ok-Formatted, Ok-Raw,
     ParseError-Formatted, ParseError-Raw, NoReadableText, RepeatCount).
   - A unit test for the "is there any readable text in this tree" helper.
   - A unit test for the formatted plain-text serializer (snapshot test
     against a small fixture tree).

7. **Don't change**: the rest of the Observer Feed layout, the
   service/capture/auto-scroll status chips, the allowlist sheet, or the
   bottom nav. This feature is additive.

## Acceptance

- Tapping "View window content" on any card with a non-empty snapshot
  opens the modal, populated with that event's metadata + tree.
- All four content states render per spec.
- Copy works in every state where it's enabled, and is correctly
  disabled where the spec says so.
- Sheet dismisses via all four documented methods.
- Snapshot is re-parsed on each open (don't cache across sheets — the
  underlying event may have been mutated by aggregation).
- No regressions in the existing Observer Feed.

## Open items I want your read on after the audit

Before you build, please reply with:

1. The actual shape of `treeSnapshot` you found in the repo — does it
   match the mock, or do we need to adapt the Formatted renderer?
2. Whether you found existing ModalBottomSheet / SegmentedButton patterns
   in this app to reuse, or whether you'll introduce them.
3. Any tokens (color/type/spacing) in the spec that don't already exist
   in our `Theme.kt` / `Tokens.kt` — flag them so I can OK additions.
4. Whether snapshots can ever be truncated server-side (see open
   question #2 in the README) and where the cap is defined.

Then proceed once I confirm.
