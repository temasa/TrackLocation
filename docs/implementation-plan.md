# TrackLocation Implementation Plan

This file is the active implementation contract for code assistants and design tools.

It uses a two-track model for every CR/phase:

```text
A. Code Implementation Work
   -> Codex / Claude Code / Cursor / Windsurf

B. UI Specification / Design Handoff Work
   -> Claude Design / Google Stitch / Figma
```

## Current Implementation Summary

| Work item | Code status | UI/design status | Verification |
|---|---|---|---|
| CR-0001 Always-recorded Location Sessions | Implemented | Implemented | Verified by user |
| CR-0002 Session always-recording switch | Implemented | Implemented | Verified by user |
| Observer Phase 1 Local Foundation | Planned | Planned | Not started |
| Observer Phase 2 Inspection UI | Planned | Planned | Not started |
| Observer Phase 3 Filtering + Settings | Planned | Planned | Not started |
| Observer Phase 4 Sync Engine | Planned | Planned | Not started |
| Observer Phase 5 Neon V1 | Planned | Planned | Not started |
| Observer Phase 6 Registration + Face Enrollment | Planned | Planned | Not started |
| Observer Phase 7 Auth + Hardening | Planned | Planned | Not started |

## Immediate Next Step

Proceed to the next planned implementation in this order:

1. Accept an Observer navigation placement decision (required).
2. Freeze Observer Phase 1 scope + data contract (this doc).
3. Then implement Observer Phase 1.

Notes:

- CR-0001 and CR-0002 are verified by the user as working as expected (see `docs/progress.md`).
- Heavy verification (Gradle builds/tests/emulator/device) still requires explicit user permission per `AGENTS.md`.

## CR-0001 — Always-recorded Location Sessions

Status:

- Code implementation: Done.
- UI implementation: Done.
- Verification: Verified by user.

### A. Code Implementation Work

Done:

| Category | Files / modules | Result |
|---|---|---|
| Create | `LocationEntity.kt` | canonical GPS point row |
| Create | `SessionEntity.kt` | always-recording session row |
| Create | `LocationDao.kt` | canonical location queries |
| Create | `SessionDao.kt` | session persistence queries |
| Edit | `TrackDatabase.kt` | added entities and Room v1→v2 migration |
| Edit | `TrackEntity.kt` | added `startLocationId` and `endLocationId` |
| Edit | `TrackDao.kt` | added location-range-aware queries |
| Edit | `TrackingService.kt` | appends canonical points; manages session and trip ranges |
| Edit | `TrackScreen.kt` / running card components | trip-specific Start/Stop semantics |
| Edit | `DetailsScreen.kt` | resolves trip path from canonical location range |
| Edit | navigation files | added Session tab/start destination |

Pending:

| Category | Required work |
|---|---|
| Test | Add migration test for legacy trip path → location rows/trip boundaries |
| Test | Add always-recording ON/OFF session test |
| Test | Add trip boundary tests: start uses next point, stop uses latest point |
| Test | Add trip deletion test proving location rows remain |
| Verify | Compile/build only after user permission |
| Verify | Manual location/session/trip behavior on device/emulator only after user permission |

### B. UI Specification / Design Handoff Work

Done:

- Session top-level destination.
- `Session / List / Track / Settings` bottom nav.
- Session screen for always-recorded sessions.
- Real session rows connected after CR-0001 full implementation.
- Track copy changed toward trip-specific semantics.

No additional design handoff required before verification unless visual defects are found.

## CR-0002 — Session Always-recording Switch

Status:

- Code implementation: Done.
- UI/design handoff: Done.
- Verification: Verified by user.

### A. Code Implementation Work

Done:

| Category | Files / modules | Result |
|---|---|---|
| Edit | `SessionsScreen.kt` | replaced hero mockup with compact status card and trailing switch |
| Edit | `ListScreen.kt` | removed always-recording control and permission flow |
| Edit | `ListContent.kt` | kept trip history UI only and removed switch UI |
| Edit | docs | updated CR and screen specification docs |

Pending:

| Category | Required work |
|---|---|
| Verify | Confirm Session switch compiles |
| Verify | Confirm List screen has no stale imports/copy/control |
| Verify | Confirm permission flow still works from Session screen |
| Verify | Confirm active-trip guard prevents OFF |
| Verify | Confirm starting trip while OFF auto-starts always-recording and Session switch shows ON |
| Verify | Confirm stopping trip does not turn switch OFF |

### B. UI Specification / Design Handoff Work

Done:

The Session screen UI handoff should include:

1. OFF / inactive state.
2. ON / active state with active session visible.
3. Permission-required state.
4. Permission-denied state.
5. Active-trip guarded state.
6. Auto-started-by-trip state.

Design guidance:

- Use compact Material 3 status-card treatment.
- Keep Session list visible.
- Use trailing/right-side switch.
- Do not put switch on List screen.
- Use text status, not color alone.
- Preserve light/dark usability.

### Design Tool Prompt for CR-0002

```text
Create or refine the Android Session screen for TrackLocation.

The Session screen is the first bottom-nav tab in:
Session / List / Track / Settings.

Purpose:
Show always-recorded location sessions and let the user control always-recording directly from the Session screen.

Design the always-recording status area as a compact Material 3 card/control panel.

Required elements:
- Title: Always-recording
- Status text:
  - Active when ON
  - Inactive when OFF
- Helper text:
  - OFF: Location sessions are not being recorded.
  - ON: Recording location sessions in the background.
  - Guard: Always-recording is required while a trip is running.
- Trailing/right-side switch.
- Sessions list remains visible below the status area.
- Active session, if present, appears at the top and is clearly marked.

Required states:
1. OFF / inactive
2. ON / active with active session visible
3. Permission required
4. Permission denied
5. Active-trip guarded state
6. Auto-started-by-trip state

Rules:
- Do not add another navigation destination.
- Do not place always-recording switch on the List screen.
- Do not merge sessions into trips.
- Do not imply that stopping a trip stops always-recording.
- Use Material 3, compact operational styling, readable status labels, and Android-safe spacing.
- Produce light and dark theme variants if possible.
```

## Observer Rollout — Planned

Observer remains planned. Do not begin until CR-0001/CR-0002 are verified and Observer navigation placement is accepted.

### Required decision before Observer Phase 1

Choose one navigation option:

| Option | Navigation | Notes |
|---|---|---|
| A | Session / List / Track / Observer / Settings | first-class Observer, five tabs |
| B | Session / List / Track / Settings, Observer under Settings/tools | keeps four tabs |
| C | GPS / Track / Observer / Settings | larger redesign |

Recommendation:

- Prefer **Option B** unless the user explicitly wants Observer always visible. It preserves the accepted 4-tab baseline and minimizes churn.

Acceptance criteria for this decision:

- `docs/product-spec.md` is updated to reflect the accepted placement.
- Phase 1 UI work must not proceed until the placement is accepted.

Accepted decision:

- **Accepted: Option B** (2026-05-18).

## Observer Phase 1 — Local Accessibility Observer Foundation

### A. Code Implementation Work

Goal:

- Capture accessibility events locally (Room) with safe limits, without impacting GPS tracking reliability.
- Provide a minimal feed UI (only after navigation placement is accepted).
  Navigation (accepted Option B): `Settings -> Tools -> Observer`.

Non-goals (Phase 1):

- No remote sync.
- No auth/registration.
- No advanced filtering/search (Phase 3).
- No deep inspection UI (Phase 2).
- No changes to GPS tracking behavior.
- No user-facing delete/clear of observer history.

Planned, in buildable order:

1. Service foundation
   Add an `AccessibilityService` implementation dedicated to observer capture. Add the manifest service declaration. Add accessibility service config XML with the minimum required capabilities.
   Service enablement notes:
   - The user enables/disables the AccessibilityService in Android system accessibility settings (the app cannot enable it programmatically).
   - The app provides a deep-link/action to open the relevant system settings screen when the service is disabled.
   Pause capture semantics:
   - When the user pauses capture from the Observer feed, the observer capture loop stops (no event processing / no DB writes). When unpaused, the capture loop starts again.
   - This pause/resume does not change the system accessibility enablement toggle; it controls whether the enabled service actively records.
2. Event capture surface
   Capture `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`. Decide and document the capture policy for high-frequency content-changed events. Recommended: throttle + dedupe (for example, time window + stable key) to avoid runaway writes.
   Accepted decision (2026-05-18) for `TYPE_WINDOW_CONTENT_CHANGED`:
   - If a new content-changed event arrives but the relevant captured content signature has not changed, do not insert a new row. Instead update the existing row's timestamp (and optionally increment a repeat counter).
   - Accepted content signature (2026-05-18): use **text summary only** (length-capped) to decide whether content changed.
3. Normalize observed context (data contract)
   Fields:
   - package name (required)
   - event type (required)
   - activity/class name (best-effort)
   - firstSeenAt (required, epoch millis)
   - lastSeenAt (required, epoch millis)
   - optional repeatCount (best-effort; for collapsed content-changed events)
   - optional text summary (best-effort, length-capped)
4. Tree snapshot capture (bounded)
   Capture a serialized view-tree snapshot (JSON) with strict limits (max nodes, max depth, max string lengths, max total payload size). Store truncation metadata so Phase 2 UI can explain partial snapshots.
5. Noise reduction (allowlist)
   Use a user-configured allowlist to decide what gets stored. Persist allowlist locally and allow the user to configure it in Phase 1 (UI under Settings/tools). Allowlist entries support regex keywords/patterns.
   Accepted decision (2026-05-18): allowlist matching applies to **package name only** in Phase 1.
   Accepted decision (2026-05-18): matching is **case-sensitive**.
   Accepted decision (2026-05-18): regex rules use **substring match** semantics. The pattern may match anywhere in the package name (start, middle, or end). Users can use `^` and `$` for full-string matching when needed.
   Behavior:
   - If the allowlist has zero enabled rules, capture stores everything (no filtering is applied).
   - A package is captured when it matches at least one enabled rule.
   Rule types:
   - Exact match: full-string equality only (for example, `packageName == pattern`), e.g. `com.example.app`.
   - Regex match: treat as a Kotlin `Regex` pattern and use substring matching (for example, `Regex(pattern).containsMatchIn(packageName)`).
   Examples:
   - Pattern `maps` matches `com.google.android.apps.maps` and `com.example.mapshelper`.
   - Pattern `^com\\.google\\.` matches only packages starting with `com.google.`.
   Recommended defaults:
   - Start with a broad regex rule (or keep the list empty) during early bring-up; refine rules to reduce noise as needed.
   - Do not include quick-add suggestions from the live feed in Phase 1; rules are added manually.
   UI guidance (Phase 1):
   - Treat allowlist as optional configuration.
   - When there are zero enabled rules, show helper text such as: `Allowlist is empty. Capturing all packages. Add rules to reduce noise.`
   Regex safety:
   - Cap pattern length.
   - Validate/compile before saving/enabling.
   - Fail closed: invalid regex rules are disabled and do not capture.
6. Local persistence
   Add Room entities/DAOs for observed events and (optional) package stats rollups for fast feed rendering. Ensure schema changes are migration-safe (Room migration required).
   Retention limit (accepted 2026-05-18):
   - Implement automatic retention to cap local observer storage growth (no user-facing clear/delete UI).
   - Recommended limit: keep the most recent 7 days of observer events OR 50,000 rows (whichever is smaller). Oldest rows are pruned automatically.
7. Repository / use-case boundary
   Add repository APIs for enabling/disabling capture (service running state), reading the feed (paged/cursor-ready shape), and reading a single event by id.
8. Minimal UI shell (gated by nav decision)
   Add Observer feed destination only after navigation placement is accepted. Phase 1 feed requirements: shows service status (enabled/disabled), shows empty/loading states, shows basic event cards (package, time range via lastSeenAt, event type, activity, small snippet).
   Feed must include an in-context entry point to allowlist configuration. Accepted decision (2026-05-18): allowlist configuration is an overlay panel on top of the Observer feed (compact modal bottom sheet), so users can watch events while tuning rules.
   Capture control (Phase 1):
   - Provide a separate control to pause/resume capture (stops receiving/storing new events). This is distinct from auto-scroll pause (UI-only).
   - When capture is paused, the feed remains readable and continues showing previously stored rows, but no new rows are appended until capture is resumed.
   State model (Phase 1):
   - Service state: `Enabled` or `Disabled` (system-controlled).
   - Capture state: `Running` or `Paused` (app-controlled; only meaningful when service is enabled).
   - UI auto-scroll state: `Running` or `Paused` (UI-only; does not affect capture).
   Accepted persistence (2026-05-18):
   - Persist capture state so a user-paused capture stays paused across application/process restarts until explicitly resumed.
   - If the system disables the AccessibilityService, remember the last capture state and restore it when the service is re-enabled (i.e., do not reset to Running).
   Feed interaction (Phase 1):
   - Tap anywhere in the feed list area toggles auto-scroll (running vs paused).
   - Drag/scroll behavior (accepted 2026-05-18): if auto-scroll is running and the user drags/scrolls the list, auto-scroll immediately switches to paused and the user begins manual scrolling.
   - Resume semantics (accepted 2026-05-18): when resuming from paused, auto-scroll continues from the current paused position (does not jump to the latest event).
   - While paused, the user can freely drag/scroll the list up and down to inspect older events.
   - Jump-to-latest (accepted 2026-05-18): show a transient FAB that scrolls to the latest event without changing whether auto-scroll is running or paused. The FAB is shown only for a couple of seconds when transitioning from paused to running.
   - Long-press copy is enabled only when auto-scroll is paused: long-press on an event copies a single line `package | activity` (best-effort). Put package first so it can be pasted into allowlist rules with minimal edits.

### B. UI Specification / Design Handoff Work

Planned (only after navigation placement is accepted):

- Observer feed shell.
- Service status indicator (enabled/disabled).
- Capture pause/resume control (separate from UI auto-scroll pause).
- Service disabled state with deep-link to Android accessibility settings (action-only, no complex flows).
- Basic event cards.
- Loading/empty states.
- Allowlist configuration UI (Phase 1): add/remove rules and enable/disable rules. Each rule has a `Match type` toggle (`Exact` or `Regex`) and a single `Pattern` field (no special prefixes).
  Each rule supports: enable/disable, and delete.
  IA note: allowlist configuration is presented as a compact overlay panel on top of the feed (modal bottom sheet). The feed remains visible behind the panel.
  Apply semantics (accepted 2026-05-18):
  - Edits are staged and only take effect when the user presses an explicit `Apply` button in the overlay.
  - Closing/dismissing the overlay saves the staged edits as a draft, but does not apply them to capture until `Apply` is pressed.
  - The overlay should indicate when there are unapplied draft changes (e.g., `Draft changes not applied`).
  - The Observer feed and capture behavior reflect applied rules only. Draft changes do not affect capture until `Apply` is pressed.
  - Applying new allowlist rules affects future capture only. Previously stored events remain visible because the feed is backed by the persisted local table, not a live filter.

Event card layout (Phase 1):

- Primary: package name.
- Secondary: activity/class (when available) and event type.
- Metadata: timestamp.

UI acceptance criteria (Phase 1):

- Feed remains readable in high-volume scenarios (list perf baseline).
- Cards do not attempt to render huge JSON.
- The screen clearly explains when snapshots are truncated or unavailable.
- Allowlist changes take effect without requiring an app restart (service observes updates or reloads safely).
- Allowlist overlay panel is compact and does not obscure the feed entirely (user can still see events updating behind it).
- Feed supports a user-controlled pause/resume of auto-scroll, and package copy from paused events.
- Feed provides a jump-to-latest FAB and long-press copy (paused only) as a single line `package | activity` (best-effort).
- Capture can be paused/resumed independently of auto-scroll; pausing capture stops new persisted rows until resumed.
- Accepted UX detail: while paused, the viewport freezes completely and newly arriving events do not shift the visible list; on resume, the feed continues moving from the current viewport position without jumping; the jump-to-latest FAB is the only control that forces a jump to the newest position.

Verification gates (Phase 1):

- Static inspection: ensure no coupling from observer to GPS tracking service.
- Optional (requires explicit user permission per `AGENTS.md`): `:app:compileDebugKotlin`, targeted unit tests for throttling/dedupe logic, Room migration test(s) for the new tables, and device/emulator sanity check that service can be enabled and events appear.

## Observer Phase 2 — Inspection UI

### A. Code Implementation Work

Planned:

- Event detail route.
- Event detail state/ViewModel.
- JSON viewer route.
- Cursor pagination.

### B. UI Specification / Design Handoff Work

Planned:

- Event detail screen.
- Full-screen JSON viewer.
- Metadata grid.
- Large JSON handling.
- Back behavior.

## Observer Phase 3 — Filtering + Unified Settings

### A. Code Implementation Work

Planned:

- FTS5 support.
- Filter query builder.
- Package chips.
- Global text chips.
- Scoped per-package text filters.
Note: clearing/deleting observer history is not included (explicitly disallowed by current decisions).

### B. UI Specification / Design Handoff Work

Planned:

- Filter chip interaction design.
- No-results states.
- Observer Settings sections.

## Observer Phase 4 — Sync Engine + Retention

### A. Code Implementation Work

Planned:

- `RemoteDataSource`.
- Fake/no-op remote.
- Hybrid sync triggers.
- Batch size 50.
- Retry/backoff.
- Retention cleanup only after successful sync.
- Never delete unsynced events.

### B. UI Specification / Design Handoff Work

Planned:

- Sync status rows.
- Pending/synced counts.
- Offline banner.
- Retry banner.

## Observer Phase 5 — Neon V1 Remote Storage

### A. Code Implementation Work

Planned:

- Neon schema/setup scripts.
- Low-privilege role.
- `NeonDirectDataSource`.
- Secure local configuration storage.
- Remote event mapping.

### B. UI Specification / Design Handoff Work

Planned:

- Generic remote sync status.
- No credential editing UI unless future CR explicitly requests it.
- No raw Neon internals in UI.

## Observer Phase 6 — Registration + Face Enrollment

### A. Code Implementation Work

Planned:

- Registration gate.
- Google Sign-In.
- CameraX face capture.
- ML Kit `FaceProcessor`.
- FaceData contract.
- Encrypted local identity/device storage.

### B. UI Specification / Design Handoff Work

Planned:

- Google sign-in step.
- Face capture step.
- Success step.
- Account section in Settings.

## Observer Phase 7 — Auth + Hardening

### A. Code Implementation Work

Planned:

- App-wide auth state controller.
- Auth overlay.
- Face attempt budget.
- Google fallback rules.
- 24-hour re-auth.
- Idle/screen-on behavior.
- Background capture/sync/GPS continues while UI locked.

### B. UI Specification / Design Handoff Work

Planned:

- Auth overlay.
- Failed attempt states.
- Lockout state.
- Google fallback text only after failed face attempt.
- Dark/light polish.

## Codex Verification Prompt

Use this next:

```text
Verify the current CR-0001 and CR-0002 implementation.

Read AGENTS.md first.

Then read:
1. docs/product-spec.md
2. docs/change-requests.md
3. docs/implementation-plan.md
4. docs/progress.md

Focus only on verification of completed CR-0001 and CR-0002.

Do not implement Observer, auth, sync, Neon, or registration.

Before editing source code:
- update docs/progress.md Current Session
- record start timestamp
- record goal
- record expected files
- set status to In Progress

Start with static inspection:
- confirm Session/List/Track/Settings navigation
- confirm Session screen contains the only always-recording switch
- confirm List screen is trip-only
- confirm Track Start/Stop trip semantics
- confirm Room migration and canonical location/session entities
- confirm trip detail path resolves from location ranges
- check for stale copy/imports around always-recording switch removal from List

Do not run Gradle, tests, emulator, or device verification unless I explicitly grant permission.

After inspection:
- update docs/progress.md
- list issues found
- list fixes made, if any
- list verification not run and why
- suggest the smallest next verification command
- provide a concise commit message if changes were made
```
