# TrackLocation Product Specification

Last updated from latest uploaded docs: 2026-05-16 21:05:00 +07:00

## Purpose

TrackLocation is an Android-first driver utility evolving from a GPS trip tracker into a broader operational tool that supports:

1. GPS trip tracking.
2. Always-recorded location sessions.
3. Planned Accessibility Event Observer for developer/support inspection.

## Current Accepted Baseline

Accepted CRs:

- CR-0001: Always-recorded Location Sessions.
- CR-0002: Session always-recording switch.

Current bottom navigation:

```text
Session / List / Track / Settings
```

Observer is planned but not yet integrated into bottom navigation.

## Current Screen Status

| Screen | Status | Notes |
|---|---|---|
| Session | Current | First-class destination; shows sessions and always-recording switch |
| List | Current | Trip list only; no always-recording switch after CR-0002 |
| Track | Current | Starts/stops explicit trip ranges |
| Settings | Current | Unified operational settings destination |
| Observer Feed | Planned | Accessed via `Settings -> Tools -> Observer` (Option B accepted 2026-05-18) |
| Observer Event Detail | Planned | Depends on Observer Feed |
| JSON Viewer | Planned | Full-screen, no bottom nav |
| Registration | Planned | Observer/auth phase |
| Auth Overlay | Planned | App-wide overlay, not a nav destination |

## GPS and Sessions

### Canonical Location Log

The app maintains a canonical append-only location log for GPS points.

Rules:

- Location rows are the source of truth for recorded GPS points.
- Location rows are not deleted when a trip is deleted.
- Sessions and trips both resolve paths from location ranges.

### Sessions

Always-recorded sessions are ON-to-OFF periods over the canonical location log.

Rules:

- Sessions are not trips.
- A session starts when always-recording turns ON.
- A session ends when always-recording turns OFF, unless an active trip prevents turning it OFF.
- Active sessions are shown clearly on the Session screen.
- Session rows can store summary metrics for fast rendering.

### Trips

Trips are explicit user-declared ranges over the canonical location log.

Rules:

- Trips use `startLocationId` and `endLocationId`.
- Starting a trip while always-recording is OFF automatically starts always-recording.
- Stopping a trip stops the trip range only.
- Stopping a trip does not stop always-recording.
- Empty trips are not saved.
- Deleting a trip deletes only the trip row; canonical location history remains.

## CR-0002 Always-recording Control

The Session screen is the primary always-recording control surface.

Rules:

- The switch is inside the Session screen always-recording status area.
- The switch controls the same always-recording state introduced by CR-0001.
- The List screen remains trip-only and does not expose a separate always-recording switch.
- ON means always-recording is active.
- OFF means always-recording is inactive, except when a trip is active and recording is required.
- If permission is missing, turning ON triggers the existing location permission flow.
- If permission is denied, the switch remains or returns OFF.
- If a trip is active, turning OFF is blocked/guarded and the UI explains that always-recording is required while a trip is running.
- Starting a trip while OFF auto-starts always-recording and the Session switch reflects ON.
- Stopping a trip does not turn the switch OFF.

## Current Navigation Rules

Main bottom navigation:

```text
1. Session
2. List
3. Track
4. Settings
```

Destination meanings:

- **Session**: always-recorded ON-to-OFF location sessions and always-recording status/control.
- **List**: explicit trip history only.
- **Track**: start/stop explicit trip ranges.
- **Settings**: unified operational settings.

Superseded older Observer nav assumption:

```text
Trips / Track / Observer / Settings
```

Observer navigation is accepted as Option B (Observer under Settings/tools). A future CR can revise this, but Observer implementation may proceed under this placement.

Possible future options:

| Option | Navigation | Tradeoff |
|---|---|---|
| A | Session / List / Track / Observer / Settings | Observer is first-class; five tabs may be crowded |
| B | Session / List / Track / Settings, with Observer under Settings/tools | Keeps four tabs; Observer less discoverable |
| C | GPS / Track / Observer / Settings, with Session/List inside GPS | Cleaner long-term IA; requires redesign |

Accepted Observer navigation placement:

- **Accepted: Option B** (2026-05-18). Observer is accessed from Settings/tools; bottom navigation remains `Session / List / Track / Settings`.

Observer entry path (Option B):

- `Settings -> Tools -> Observer` opens the Observer feed.
- The Observer feed provides an in-context entry point to configure the allowlist.
- Accepted UX (2026-05-18): allowlist configuration is a compact overlay panel on top of the Observer feed (so users can see live events while tuning rules).
- Accepted UX (2026-05-18): Observer feed includes a capture pause/resume control that stops receiving/storing new events; this is separate from UI auto-scroll pause.
- Service note: the user enables/disables the AccessibilityService in Android system accessibility settings. The app can only deep-link to settings and reflect status.
- Pause capture behavior: pausing capture stops the observer capture loop (no processing/writes) while leaving the system AccessibilityService enablement unchanged. Unpausing starts capture again.
- Accepted behavior (2026-05-18): capture pause state persists across app restarts and is remembered across system service disable/enable.
- Accepted UX (2026-05-18): tap anywhere in the Observer feed list area toggles auto-scroll (running vs paused). Long-press copy is enabled only when paused; long-press on an event copies a single line `package | activity` (best-effort), with package first.
- Accepted UX (2026-05-18): when resuming from paused, auto-scroll continues from the paused position (does not jump to latest). While paused, the user can drag/scroll the list freely.
- Accepted UX (2026-05-18): if auto-scroll is running and the user drags/scrolls the list, it immediately pauses and allows manual scrolling.
- Accepted UX (2026-05-18): show a transient jump-to-latest FAB that scrolls to the newest event without changing whether auto-scroll is paused or running; the FAB is shown only for a couple of seconds when transitioning from paused to running.
- Phase 1 event cards include: package name, activity/class (when available), event type, and timestamp.
- Accepted collapse rule (2026-05-18): for `TYPE_WINDOW_CONTENT_CHANGED`, if captured content has not changed, do not create a new row; update the existing row's timestamp (and optionally a repeat counter).
- Accepted content signature (2026-05-18): content-change detection uses the text summary only (length-capped).

## Planned Accessibility Observer Principles

Observer remains planned and is governed by the original Accessibility Observer PRD preserved in `docs/archive/latest-upload/docs/product/accessibility-observer-prd.md`.

Locked principles:

- Capture only packages that match a user-configured allowlist (to reduce noise).
- Package filtering is a UI concern, not a capture-layer setting.
- Allowlist configuration is a Phase 1 user-facing setting under Settings/tools (it affects capture by excluding packages from being stored unless they match the allowlist).
- Allowlist rules support regex keywords/patterns.
- Allowlist rule UX (Phase 1): each rule has a `Match type` toggle (`Exact` vs `Regex`) and a single `Pattern` field.
- Allowlist rule actions (Phase 1): enable, disable, and delete rules.
- Accepted UX (2026-05-18): allowlist overlay uses an explicit `Apply` button; rule edits are staged and only take effect when `Apply` is pressed. Closing/dismissing the overlay saves draft edits but does not apply them.
- Accepted UX (2026-05-18): Observer feed reflects applied allowlist rules only; draft (unapplied) edits do not affect capture.
- Accepted behavior (2026-05-18): applying new allowlist rules affects future capture only. Previously recorded events remain visible in the feed because it reflects persisted local history.
- Accepted decision (2026-05-18): `Exact` match means full-string equality only (not substring/contains).
- Accepted decision (2026-05-18): Phase 1 does not include quick-add suggestions from the live feed; allowlist rules are added manually.
- Accepted decision (2026-05-18): Phase 1 allowlist matching applies to **package name only**.
- Accepted decision (2026-05-18): Phase 1 allowlist matching is **case-sensitive**.
- Accepted decision (2026-05-18): regex rules use **substring match** semantics (keyword can appear at start/middle/end of package name). Users can use `^`/`$` for strict matching.
- If the allowlist has zero enabled rules, Observer capture stores everything (no filtering is applied).
- Allowlist is optional. When empty, show a hint like: `Capturing all packages. Add allowlist rules to reduce noise.`
- Store screen-tree JSON snapshots locally first.
- Use FTS5 locally and GIN remotely for search.
- Use cursor-based infinite scroll.
- Sync is hybrid: batch-count and time-based.
- Remote access goes behind `RemoteDataSource`.
- Direct Neon is V1; API backend is V2 replacement path.
- Auth gates UI only; background capture, sync, and GPS tracking continue.
- Raw face images are never transmitted.
- Unsynced local events are never deleted.
- Observer history is not user-deletable/clearable (no clear-data action).
- Accepted retention (2026-05-18): automatic retention limits local observer storage growth (no user-facing clear/delete). Recommended: keep most recent 7 days or 50,000 rows (whichever is smaller).

## UI Direction

Use Android Jetpack Compose + Material 3.

The UI should feel:

- operational
- calm
- technical
- compact
- readable
- suitable for repeated driver/support usage

Avoid:

- marketing-style hero sections
- decorative gradients
- oversized nested cards
- playful visual language
- controls that rely only on color for state

## Documentation Rule

This file describes the current accepted product baseline. CR history lives in `docs/change-requests.md`. Implementation and handoff work lives in `docs/implementation-plan.md`. Progress and verification state live in `docs/progress.md`.
