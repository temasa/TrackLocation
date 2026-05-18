# TrackLocation Progress

This is the single active status/progress file in the simplified documentation structure.

## Current Session

Status: None active.

## Latest Known State

Last known active implementation session:

- Date: 2026-05-16 20:20:00 +07:00 to 2026-05-16 21:05:00 +07:00
- Branch: `codex`
- Commit: `fe241a7`
- Task: implement CR-0002 Session always-recording switch as the single control surface
- Status: Done
- Verification: verified by user as working as expected

## What Has Been Done

### Tooling / Baseline

- Upgraded Android/Gradle/Kotlin/Compose tooling.
- Migrated remaining Material2 blockers to Material3.
- Addressed SDK 34 foreground service compatibility.
- `:app:compileDebugKotlin` passed in an earlier session.
- `:app:assembleDebug` was attempted earlier and timed out after 6 minutes.

### Documentation Refactor

- Reorganized docs into product, CR, architecture, UI, implementation, status, and archive layers.
- Added current source-of-truth docs and preserved original docs in archive.
- Added/refined AI-agent instructions in `AGENTS.md` and `CLAUDE.md`.

### CR-0001 — Always-recorded Location Sessions

Implemented as of 2026-05-16 18:30:39 +07:00.

Done:

- Added canonical `location_log` rows.
- Added always-recorded `recording_session` rows.
- Added Room DAOs for location ranges and session history.
- Migrated Room from version 1 to 2.
- Converted legacy serialized trip paths into canonical location rows and trip boundaries.
- Updated `TrackingService` so always-recording appends canonical points and sessions.
- Updated trip start/stop so trips are explicit ranges.
- Replaced the Trips/List header Export pill with an always-recording switch during CR-0001.
- Updated Track controls to Start trip/Stop trip semantics.
- Updated trip detail path rendering to resolve from canonical location ranges.
- Connected Sessions screen to real session rows instead of mock UI state.

Verification:

- Verified by user as working as expected.
- `git diff --check` was run and reported only line-ending warnings/no whitespace errors.
- Static searches were run for stale actions/copy and negative letter spacing.
- Gradle build/tests/emulator/device verification were not run because user permission is required.

### CR-0002 — Session Always-recording Switch

Implemented as of 2026-05-16 21:05:00 +07:00.

Done:

- Replaced the Session screen hero mockup with a compact always-recording status card and trailing switch.
- Kept sessions list visible.
- Marked active sessions distinctly.
- Removed always-recording switch and permission flow from the List screen.
- Kept List as trip history UI only.
- Updated CR-0002 docs and UI screen specification to treat Session as the only always-recording control surface.

Verification:

- Verified by user as working as expected.
- No Gradle build, unit tests, emulator, or device verification were run in the verification pass.

## Known Remaining Issues / Risks

- Build/tests/emulator/device verification has not been run.
- Tests listed for CR-0001 were not added in the implementation pass.
- `CheckAndRequestPermissions` remains a shared permission helper and still uses a full-screen prompt style.

## Recommended Next Steps

### Step 1 — Confirm Observer navigation before Observer work

Observer should not be implemented until one navigation option is accepted:

- add Observer as fifth tab
- put Observer under Settings/tools
- redesign into GPS / Track / Observer / Settings

Planning note:

- `docs/implementation-plan.md` was updated on 2026-05-18 to make Observer Phase 1 an implementation-ready contract, pending only the navigation placement decision above.

Decision:

- **Accepted: Option B** (2026-05-18). Observer will live under Settings/tools; bottom navigation remains `Session / List / Track / Settings`.

Scope decision:

- Observer Phase 1 must include user-configurable allowlist management with regex keyword/pattern support (add/remove rules; enable/disable rules).
- Accepted detail (2026-05-18): Phase 1 allowlist matching applies to package name only.
- Accepted UX detail (2026-05-18): allowlist rules use `Match type` (`Exact`/`Regex`) + a single `Pattern` field.
- Accepted detail (2026-05-18): allowlist matching is case-sensitive.
- Accepted detail (2026-05-18): regex rules use substring match semantics (keyword can match anywhere in package name).
- Accepted detail (2026-05-18): `Exact` match type uses full-string equality only.
- Accepted detail (2026-05-18): if there are zero enabled allowlist rules, capture stores everything (no filtering).
- Accepted UX detail (2026-05-18): allowlist is optional; when empty, UI hints that all packages are being captured and suggests adding rules to reduce noise.
- Accepted nav/IA detail (2026-05-18): Observer entry path is `Settings -> Tools -> Observer`, and allowlist configuration is a compact overlay panel on top of the Observer feed.
- Accepted UX detail (2026-05-18): tap anywhere in the Observer feed list area toggles auto-scroll pause/resume; long-press copy is enabled only when paused and copies `package + activity` (best-effort).
- Accepted UX detail (2026-05-18): resuming from paused continues from the paused position (no jump to latest); while paused, the list can be freely scrolled.
- Accepted UX detail (2026-05-18): dragging/scrolling while auto-scroll is running immediately pauses and begins manual scrolling.
- Accepted UX detail (2026-05-18): show a transient jump-to-latest FAB that scrolls to the newest event without changing whether auto-scroll is paused or running; the FAB is shown only for a couple of seconds when transitioning from paused to running.
- Accepted content detail (2026-05-18): Phase 1 event cards include event type, and long-press copy (paused only) copies a single line `package | activity` (best-effort), with package first.
- Accepted UX detail (2026-05-18): Observer feed includes a capture pause/resume control (stops receiving/storing new events), separate from UI auto-scroll pause.
- Accepted behavior detail (2026-05-18): pausing capture stops the observer capture loop (no event processing/writes) and unpausing starts it again; this does not change the system AccessibilityService enablement toggle.
- Accepted UX detail (2026-05-18): Phase 1 allowlist overlay does not include quick-add suggestions from the live feed; rules are added manually.
- Accepted UX detail (2026-05-18): allowlist rules support enable, disable, and delete.
- Accepted UX detail (2026-05-18): allowlist edits are staged and applied only when the user presses `Apply` in the overlay; closing the overlay saves draft edits but does not apply them.
- Accepted UX detail (2026-05-18): Observer feed reflects applied rules only; drafts do not affect capture.
- Accepted behavior detail (2026-05-18): applying allowlist changes affects future capture only; previously recorded rows remain visible in the feed.
- Accepted scope detail (2026-05-18): do not provide any UI to clear/delete observer history data.
- Accepted behavior detail (2026-05-18): capture pause state persists across app restarts and is remembered across system service disable/enable.
- Accepted behavior detail (2026-05-18): for `TYPE_WINDOW_CONTENT_CHANGED`, if captured content does not change, update the existing row timestamp instead of inserting a new row.
- Accepted detail (2026-05-18): content-change signature uses the text summary only (length-capped).
- Accepted scope detail (2026-05-18): automatic retention is allowed (no user-facing clear/delete). Recommended: keep most recent 7 days or 50,000 rows (whichever is smaller).

## Task Log

### 2026-05-16 20:20:00 +07:00

- Branch: `codex`
- Commit: `fe241a7`
- Task: implement CR-0002 Session always-recording switch as the single control surface
- End: 2026-05-16 21:05:00 +07:00
- Done:
  - Replaced the Session screen hero mockup with a compact always-recording status card and trailing switch.
  - Kept the sessions list visible and marked active sessions distinctly.
  - Removed the always-recording switch and permission flow from the List screen.
  - Updated the CR-0002 docs and UI screen specification to treat Session as the only control surface for always-recording.
- Verification:
  - Verified by user as working as expected.
  - Did not run Gradle build, unit tests, emulator, or device verification in this verification pass.

### 2026-05-16 20:01:12 +07:00

- Branch: `codex`
- Commit: `fe241a7`
- Commit status: uncommitted working-tree changes
- Task: refine CR-0002 Session switch UI specification from Stitch references
- End: 2026-05-16 20:20:00 +07:00
- Done:
  - Added Stitch design references to the CR-0002 Session UI handoff.
  - Tightened UI handoff copy to match supplied visual treatment.
  - Preserved accepted Session switch behavior, states, and accessibility requirements.
- Verification:
  - Documentation-only change.
  - No build/tests/emulator/device run.

### 2026-05-16 18:01:44 +07:00

- Branch: `codex`
- Commit: `776cd6e`
- Commit status: uncommitted working-tree changes
- Task: fully implement CR-0001 always-recorded sessions and canonical location log
- End: 2026-05-16 18:30:39 +07:00
- Done:
  - Added canonical location/session persistence and migration.
  - Updated tracking service behavior.
  - Updated Track/List/Details/Sessions behavior.
- Verification:
  - Static inspection only.
  - No build/tests/emulator/device run.

### 2026-05-16 17:38:39 +07:00

- Branch: `codex`
- Commit: `776cd6e7230ca77e00310b188032a36209ffc524`
- Commit status: committed as current `codex` HEAD with a clean working tree
- Task: capture latest docs refactor in progress log
- Done:
  - Added task log entry for docs refactor commit.
- Verification:
  - Documentation-only review.

### 2026-05-14 21:05:21 +07:00

- Branch: `codex`
- Commit: `7ae11b9`
- Task: implement CR#1 UI-first Sessions screen and 4-tab bottom navigation
- Done:
  - Added Session destination/start tab.
  - Changed bottom nav to Session/List/Track/Settings.
  - Added UI-only Sessions screen and visual states.
  - Added icon/string/theme changes.
- Verification:
  - Static inspection only.

## Local Build Note

Preferred local JDK path from earlier progress:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
```
