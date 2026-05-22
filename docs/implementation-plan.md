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
| Observer Phase 1 Local Foundation | Implemented | Implemented | Not verified on device |
| Observer Phase 2 Inspection UI | Implemented (pagination) + truncation code logic ready | Spec drafted (truncation), awaiting design | Not verified on device |
| Observer Phase 3 Filtering + Settings | Planned | Planned | Not started |
| Observer Phase 4 Sync Engine | Planned | Planned | Not started |
| Observer Phase 5 Neon V1 | Planned | Planned | Not started |
| Observer Phase 6 Registration + Face Enrollment | Planned | Planned | Not started |
| Observer Phase 7 Auth + Hardening | Planned | Planned | Not started |

## Immediate Next Step

Observer Phase 1 is implemented. Proceed to device verification, then Observer Phase 2.

1. Grant explicit permission for `./gradlew assembleDebug` and device run to verify Phase 1 on device.
2. Verify tree snapshot DFS population in `ObserverAccessibilityService` on a real device.
3. Then plan and implement Observer Phase 2 (Inspection UI).

Notes:

- CR-0001 and CR-0002 are verified by the user as working as expected (see `docs/progress.md`).
- Observer Phase 1 code + UI are implemented (static inspection confirmed); device verification not yet run.
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

## Observer Rollout

Observer Phase 1 is implemented. Remaining phases (2–7) are planned.

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

Status:

- Code implementation: Done (static inspection).
- UI implementation: Done (static inspection).
- Verification: Not run — requires explicit user permission per `AGENTS.md`.

### A. Code Implementation Work

Goal:

- Capture accessibility events locally (Room) with safe limits, without impacting GPS tracking reliability.
- Provide a minimal feed UI under `Settings → Tools → Observer` (navigation Option B, accepted 2026-05-18).

Non-goals (Phase 1):

- No remote sync.
- No auth/registration.
- No advanced filtering/search (Phase 3).
- No deep inspection UI (Phase 2).
- No changes to GPS tracking behavior.
- No user-facing delete/clear of observer history.

Done:

| Step | Files / modules | Result |
|---|---|---|
| 1. Service foundation | `ObserverAccessibilityService.kt`, `AndroidManifest.xml`, `res/xml/accessibility_service_config.xml` | `AccessibilityService` declared with `BIND_ACCESSIBILITY_SERVICE`; config listens to `typeWindowStateChanged\|typeWindowContentChanged`; manifest declares service with correct intent-filter and meta-data; "Open settings" action wired to `ACTION_ACCESSIBILITY_SETTINGS` in `StatusIndicators.kt` |
| 2. Event capture surface | `ObserverAccessibilityService.kt` | Captures both event types; `TYPE_WINDOW_CONTENT_CHANGED` deduped by text summary — updates existing row's `lastSeenAt` + increments `repeatCount` instead of inserting a duplicate row |
| 3. Data contract | `ObservedEventEntity.kt`, `ObserverEventDao.kt` | Fields: `packageName`, `eventType`, `activityName`, `firstSeenAt`, `lastSeenAt`, `repeatCount`, `textSummary`, `treeSnapshot`, `truncationMetadata`; indices on `packageName` and `lastSeenAt` |
| 4. Tree snapshot capture (bounded) | `ObserverAccessibilityService.kt` — `captureTreeSnapshot()` | DFS from `event.source` via explicit stack; captures text, contentDescription, className, flags, bounds per node; limits: 200 nodes, depth 10, 300 chars/field, 40 KB JSON; recycles all `AccessibilityNodeInfo` nodes; truncation metadata records reason + nodesCaptured; wired into both insert and CONTENT_CHANGED dedup-update paths |
| 5. Noise reduction (allowlist) | `AllowlistRuleEntity.kt`, `AllowlistRuleDao.kt`, service | EXACT (full-string equality) and REGEX (substring, case-sensitive, `containsMatchIn`) on `packageName`; empty allowlist = capture all; invalid/uncompilable regex = disabled silently |
| 6. Local persistence | `TrackDatabase.kt` (v3), migration `MIGRATION_2_3` | Creates `observer_event` and `allowlist_rule` tables; service prunes rows older than 7 days and enforces 50,000-row cap automatically |
| 7. Repository / use-case boundary | `EventRepository` (interface + `EventRepositoryImpl` + `FakeEventRepository`), `ObserverPreferencesDataStore.kt` | Feed reads from Room via Flow; capture running state persisted in DataStore; `setCaptureRunning` toggled from ViewModel |
| 8. Minimal UI shell | `ObserverFeedScreen.kt`, `ObserverViewModel.kt`, 6 components, `NavGraph.kt` | Full feed screen under Settings → Tools → Observer; service status banner (enabled/disabled); capture chip (toggle, persisted); auto-scroll with drag-pause; jump-to-latest FAB (2s transient); long-press copy (paused only); allowlist bottom sheet (draft → apply pattern) |

Pending:

| Category | Required work |
|---|---|
| Verify | Step 4 — DFS implemented; runtime behavior on real device not yet confirmed (no build/device run) |
| Wire | `getEventsByPackage()` DAO method exists but ViewModel always fetches all events; scoped package feed is not yet wired |
| Build | `./gradlew assembleDebug` — requires explicit user permission per `AGENTS.md` |
| Device | Enable service in Android Accessibility Settings, confirm events appear in feed — requires explicit user permission per `AGENTS.md` |
| Test | Unit tests for dedup/retention logic; Room migration test for `MIGRATION_2_3` |

Package structure (actual):

```
com.kolee.tracklocation.observer.ObserverAccessibilityService
com.kolee.tracklocation.data.roomdb.{ObservedEventEntity, AllowlistRuleEntity, ObserverEventDao, AllowlistRuleDao}
com.kolee.tracklocation.feature.observer.domain.model.{ObservedEvent, AllowlistRule, ObserverUiState, AllowlistDraftRule, AllowlistUiState, MatchType}
com.kolee.tracklocation.feature.observer.data.{ObserverPreferencesDataStore, repository/EventRepository, repository/EventRepositoryImpl, repository/FakeEventRepository}
com.kolee.tracklocation.feature.observer.presentation.viewmodel.ObserverViewModel
com.kolee.tracklocation.feature.observer.presentation.screens.ObserverFeedScreen
com.kolee.tracklocation.feature.observer.presentation.components.{EventRow, FeedHeaderBar, AllowlistBottomSheet, JumpToLatestFab, EmptyState, StatusIndicators}
```

### B. UI Specification / Design Handoff Work

Done:

- Observer feed shell — `ObserverFeedScreen.kt`.
- Service status indicator (enabled green / disabled red with "Open settings") — `StatusIndicators.kt`.
- Capture pause/resume chip (independent of auto-scroll) — `CaptureChip` in `StatusIndicators.kt`.
- Auto-scroll readout (display-only indicator; tap list toggles) — `AutoScrollReadout`.
- Capture paused inline banner — `CapturePausedBanner`.
- Event cards with package, activity, event-type chip (color-coded by category), text snippet, timestamp — `EventRow.kt`.
- Empty state ("Waiting for events") — `EmptyState.kt`.
- Jump-to-latest transient FAB (2s auto-dismiss) — `JumpToLatestFab.kt`.
- Allowlist bottom sheet: add/edit/delete rules, match-type toggle (Exact/Regex), enable/disable switch, draft indicator, amber draft banner, Apply/Close footer — `AllowlistBottomSheet.kt`.
- Feed header bar with event count, scope label, live/paused dot — `FeedHeaderBar.kt`.

Verification gates (Phase 1):

- Static inspection: no coupling from observer to GPS tracking service — confirmed.
- Requires explicit user permission per `AGENTS.md`: `./gradlew assembleDebug`, Room migration test, device/emulator sanity check that service enables and events appear in feed.

## Observer Phase 2 — Inspection UI

Status:

- Code implementation: Done (cursor pagination complete; truncation metadata code logic already in domain model, awaiting design spec).
- UI specification: In progress (truncation warning spec drafted; awaiting design tool handoff; event detail and JSON viewer already implemented via SnapshotViewerSheet).
- Verification: Not verified on device.

### A. Code Implementation Work

Done:

| Item | Status | Notes |
|---|---|---|
| Cursor pagination | ✅ Done (2026-05-22, `ee54e9c`) | First page 50, load-more on scroll, live-event narrow flow |
| Event detail / JSON viewer | ✅ Done (2026-05-19, via `SnapshotViewerSheet`) | Formatted + raw JSON modes, copy, per-event modal |
| Truncation metadata code | ✅ Ready | Field already in `ObservedEvent` domain model, passed to sheet; code change depends on design spec |

Pending:

| Item | Status |
|---|---|
| Truncation warning UI design approval | Track B in progress |
| Truncation banner implementation (SnapshotViewerSheet.kt) | Waiting for design spec |

### B. UI Specification / Design Handoff Work

Done:

| Item | Status | Document |
|---|---|---|
| Event detail screen | ✅ Via `SnapshotViewerSheet` | Already implemented; shows all event metadata, timestamp, event type, repeat count |
| Full-screen JSON viewer | ✅ Via SnapshotViewerSheet Raw JSON tab | Formatted + raw modes, 2-space indentation, line numbers, copy action |
| Metadata grid | ✅ Via SnapshotViewerSheet MetaStrip | Chips for event type, first seen, last seen, repeat count (when > 1) |
| Truncation metadata warning | 📋 Spec drafted | `docs/design-handoff/observer_truncation/OBSERVER_TRUNCATION_SPEC.md` — awaiting design tool handoff |

Pending:

| Item | Status |
|---|---|
| Design approval of truncation banner | Awaiting handoff to Google Stitch / Claude Design |
| Implementation after design approval | Code implementation ready once design is approved |

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
