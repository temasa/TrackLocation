# TrackLocation UI Implementation Plan

Last updated: 2026-05-14

## Summary

This plan breaks down implementation of `docs/ui_screen_specification.md` and aligns it with the rollout phases in `docs/plans.md`.

The UI work should be delivered incrementally:

- Keep GPS tracking intact.
- Add Observer as a first-class bottom navigation destination.
- Build basic usable Observer screens before advanced filtering.
- Add Settings sections when the underlying feature area exists.
- Defer registration, face enrollment, and auth overlay UI until the auth phases.
- Keep `docs/plans.md` as the product rollout source of truth; keep this file as the UI implementation companion.

When implementation modifies or creates code files, follow the project rule in `AGENTS.md`: create/update `docs/status_report.txt` before code changes, mark the task done with timestamp, list affected code files with inline comments, and provide a concise commit message.

## Phase 1: Basic Observer Feed Shell

Goal: add the minimum UI surface needed to see captured accessibility events.

Implement:

- Add `Observer` to bottom navigation alongside existing Trips/List, Track, and Settings.
- Add `observer_feed` as a new Compose destination.
- Preserve existing GPS trip list, track map, trip detail, and settings navigation.
- Add Observer top area:
  - title: `Observer`
  - accessibility service status: `Active` or `Inactive`
  - shortcut/action to open Android accessibility settings when service is inactive
- Add basic event list:
  - newest events first
  - timestamp
  - package name
  - activity name
  - event type
  - local/sync placeholder
- Add feed states:
  - loading
  - empty: `No events captured yet`
  - service disabled with clear enable action
- Keep visual styling simple and Material3-compatible.

Do not implement yet:

- event detail
- JSON viewer
- advanced filters
- editable denylist
- real sync status
- registration/auth UI

## Phase 2: Observer Inspection UI

Goal: make each captured event inspectable.

Implement:

- Add `observer_detail/{eventId}` destination.
- Add event detail top bar:
  - back arrow
  - title: `Event detail`
  - optional copy/share action
- Add metadata card:
  - activity name
  - package name
  - event type badge
  - sync status badge
- Add metadata grid:
  - timestamp
  - event ID
  - node count
  - snapshot size
- Add searchable text preview:
  - first 300 characters
  - copy action if practical
- Add UI snapshot preview:
  - first 3 JSON lines
  - `View full JSON` action
- Add `observer_json/{eventId}` destination:
  - full-screen route
  - no bottom navigation
  - close/back returns to event detail
  - activity name when available
  - truncated event ID
  - node count
  - file size
  - copy-to-clipboard action
  - scrollable monospace JSON
  - line numbers
  - basic syntax coloring if practical
- Add cursor paging UI behavior:
  - page size 50
  - request next page when last 5 items become visible
  - avoid duplicate rows during pagination.

States:

- detail loading
- event not found
- malformed or missing snapshot
- JSON copy confirmation
- pagination loading older events
- pagination end reached

## Phase 3: Advanced Filtering and Unified Settings

Goal: implement the UI spec's local filtering model and organize feature controls in Settings.

Observer Feed:

- Add package chip row:
  - `All` chip first
  - `All` clears selected package filters
  - package chips auto-populated from distinct package names
  - multi-select package chips
  - selected package tap opens scoped text input
  - scoped package display format: `package.name · term`
- Add top-level text filter row:
  - user can add text terms
  - terms become removable chips
  - multiple text chips are visible and horizontally scrollable
- Add add-text-filter affordance:
  - compact input or action row
  - submit creates a text chip
  - empty submit is ignored
- Add summary row:
  - total matching events
  - sync placeholder/status badge
  - last synced placeholder if sync is not active yet
- Add filter states:
  - no filters: show all events
  - package-only results
  - text-only results
  - combined package + text results
  - no results with clear-filter action
- Ensure long package names do not break layout.

Settings:

- Replace placeholder Settings with grouped sections.
- Add GPS tracking section:
  - tracking status
  - permission/status row if available
  - keep any existing GPS controls accessible
- Add Observer service section:
  - accessibility service status
  - open Android accessibility settings action
  - capture scope note: all packages except denylist
- Add Observer denylist section:
  - current denied package list
  - remove action per row
  - inline add package input
  - helper text: changes apply to next captured event
- Add Observer local data section:
  - clear local observer events row
  - destructive confirmation dialog before clearing

Do not implement yet:

- account section backed by real registration
- auth settings backed by real auth
- Neon-specific sync details

## Phase 4: Sync Status UI

Goal: connect real sync state to the UI without blocking local inspection.

Observer Feed:

- Replace sync placeholders with real sync state.
- Add non-blocking banners below the top bar:
  - `Offline - events stored locally`
  - `Sync failed - retrying in Xs`
- Keep feed scrolling, filtering, and detail navigation usable during sync errors.

Settings:

- Add Sync section with:
  - pending count
  - synced count
  - current sync status
  - last synced time
  - retry/backoff state when relevant
- Do not expose low-level transport details in this phase.

States:

- idle
- syncing
- synced
- pending
- offline
- failed/retrying

## Phase 5: Neon V1 Remote UI Integration

Goal: reflect Neon-backed sync state without exposing sensitive internals.

Implement:

- Extend Settings Sync section with:
  - remote configured/unconfigured status
  - last successful upload
  - pending upload count
  - concise sync error details
- Keep Neon connection strings, credentials, schema names, and raw database errors out of UI.
- Keep Observer Feed status generic: synced, pending, offline, retrying, failed.

Do not add:

- Neon administration screens
- manual credential editing UI unless a later task explicitly requires it
- remote query/search UI

## Phase 6: Registration and Face Enrollment UI

Goal: add one-time registration before the main app.

Navigation:

- Add registration flow shown when no local `google_id` exists:
  - `register/step1_google_sso`
  - `register/step2_face_capture`
  - `register/step3_success`
- Disable back navigation where required by the PRD.
- Successful registration opens main app.

Step 1: Google Sign-In:

- App logo or simple product mark.
- Title: `Welcome to Accessibility Observer` or final product name.
- Short explanation of registration purpose.
- Only CTA: `Sign in with Google`.
- Retryable error state.
- No skip action.

Step 2: Face Capture:

- Step indicator with step 2 active.
- Heading: `Capture your face`.
- Privacy note: `Only landmark coordinates are sent. Your image never leaves this device.`
- CameraX viewfinder.
- Face guide oval.
- Corner alignment markers.
- live/alignment status.
- Primary CTA: `Capture & continue`.
- Secondary back action only before completed registration.
- States:
  - camera permission required
  - camera unavailable
  - face not detected
  - face aligned
  - capturing
  - capture failed

Step 3: Success:

- Complete step indicator.
- Success icon.
- Heading: `You're all set`.
- Body: `Your device is registered. Face authentication is active.`
- Primary CTA: `Start observing`.
- No back navigation.

Settings:

- Add Account section:
  - avatar or initials
  - name/email when available
  - active session badge
  - face authentication row: `Primary · re-auth every 24h`
  - Google SSO fallback row: `Used only when face auth fails` with `Auto` badge

## Phase 7: Auth Overlay and Final UI Hardening

Goal: complete face-first UI locking and final UI polish.

Auth overlay:

- Render overlay above current NavHost destination.
- Do not make overlay a navigation destination.
- Preserve current screen and back stack.
- Blur and dim current screen behind overlay.
- Block interaction with background UI.
- Dismiss only after successful authentication.

Overlay content:

- Face scan icon/ring.
- Status text: `Ready to scan`.
- Heading: `Verify your identity`.
- Body: `Session expired after 24 hours. Look at the camera to unlock.`
- Primary CTA: `Scan face to unlock`.
- Attempt counter: `X of 3 attempts remaining`.
- Low-prominence Google fallback text only after at least one failed face attempt:
  - `Having trouble? Sign in with Google instead`

Hard rules:

- Do not show Google SSO as a primary button after registration.
- Do not show Google fallback before a failed face attempt unless face auth cannot start.
- Overlay gates UI only.
- GPS tracking, accessibility capture, and sync continue in the background.
- Unlock returns to the previous screen.

Final polish:

- Review Observer Feed for chip wrapping, long package names, no-results state, offline/sync banners.
- Review Event Detail for long IDs, missing activity names, large searchable text, and snapshot metadata.
- Review JSON Viewer for long payloads, horizontal scrolling, copy feedback, and no bottom nav.
- Review Settings for section grouping, destructive confirmation, and dark theme.
- Review Registration and Auth Overlay for back behavior, permission states, and accessibility labels.

## Shared UI Components

Prefer small reusable Compose components where they reduce duplication:

- `StatusBadge`
- `ObserverEventCard`
- `PackageFilterChip`
- `TextFilterChip`
- `MetadataGridItem`
- `SettingsSection`
- `SettingsRow`
- `DestructiveConfirmationDialog`
- `JsonLineRow`
- `EmptyState`
- `NonBlockingBanner`

Use existing project patterns first. Add abstractions only when multiple screens need the same behavior or styling.

## Test and Verification Checklist

Phase 1:

- App compiles.
- Bottom nav reaches Trips/List, Track, Observer, Settings.
- Existing GPS tracking navigation still works.
- Observer loading, empty, and service-disabled states render.

Phase 2:

- Feed opens detail.
- Detail opens JSON viewer.
- JSON viewer hides bottom navigation.
- Back returns JSON viewer -> detail -> feed.
- Large JSON remains scrollable and does not break layout.
- Pagination loads older events without duplicates.

Phase 3:

- Package chips toggle correctly.
- `All` clears package selection.
- Scoped package input appears for selected packages.
- Global text chips add/remove correctly.
- Combined filters show correct empty/no-results states.
- Settings denylist add/remove works.
- Clear local observer events requires confirmation.

Phase 4:

- Pending/synced counts render.
- Offline banner appears without blocking feed.
- Retry banner updates with retry timing.
- Sync errors do not block filtering or detail navigation.

Phase 6:

- Fresh install routes to registration.
- Google sign-in screen has no skip action.
- Face capture permission and failure states render.
- Success screen opens main app.
- Account section displays registered user state.

Phase 7:

- Auth overlay appears above current destination.
- Overlay cannot be manually dismissed.
- Google fallback text appears only under PRD conditions.
- Unlock returns to previous screen.
- GPS tracking, accessibility capture, and sync continue while UI is locked.

## Assumptions

- `docs/plans.md` remains the rollout source of truth.
- `docs/ui_screen_specification.md` remains the designer-facing screen spec.
- This file is the implementation companion for UI work only.
- Existing `List` navigation may be visually renamed to `Trips`, but GPS trip behavior must remain intact.
- Compose + Material3 remain the UI implementation stack.
- Visual refinement can be incremental, with final polish concentrated in Phase 7.

