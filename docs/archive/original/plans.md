# TrackLocation PRD Implementation Plan

Last updated: 2026-05-14 11:08:28 +07:00

## Summary

Implement the full E-PRD v5 Accessibility Observer inside the existing TrackLocation Android app. The app will keep both feature areas:

- GPS tracking remains intact.
- Accessibility Observer is added as a first-class feature.

Phase 1 is intentionally the local accessibility event recording slice, but the target is the whole PRD: observer capture, screen-tree JSON snapshots, local search/filter UI, sync, Neon V1 remote storage, registration, face enrollment, face-first auth, Google SSO fallback, re-auth overlay, unified settings, tests, and real-device hardening.

## Locked Rollout Decisions

- Use the existing `docs/plans.md` as the source of truth for rollout planning.
- Keep the existing GPS tracking feature. Observer is an additional feature, not a replacement.
- Capture all packages by default, excluding only a configurable system-noise denylist.
- Phase 1 stores the final local event shape, including the JSON representation of the accessibility screen tree.
- Keep one Settings destination with clear sections per feature area.
- Build Android app layers before backend/auth where possible.
- Add `RemoteDataSource` early and start with fake/no-op remote behavior before Neon.
- Direct Neon is the V1 remote target, isolated behind `RemoteDataSource`; API backend remains the V2 replacement.
- Face auth target is the PRD custom CameraX + ML Kit landmark flow, not only Android `BiometricPrompt`.
- Auth overlay is app-wide state layered above the current NavHost destination, not a navigation destination.
- Add targeted tests in each phase, then reserve final hardening for full integration and real-device verification.

## Phase 1: Local Accessibility Observer Foundation

### Goal

Create the local capture pipeline so the user can enable the accessibility service and see real captured events in the app.

### User Value

The app becomes useful as a developer inspection tool immediately, without waiting for sync, auth, or backend infrastructure.

### Scope

- Add an `AccessibilityService` listening for:
  - `TYPE_WINDOW_STATE_CHANGED`
  - `TYPE_WINDOW_CONTENT_CHANGED`
- Add manifest service declaration and accessibility service config XML.
- Capture all packages except the default denylist:
  - `android`
  - `com.android.systemui`
  - `com.android.launcher3`
  - `com.google.android.inputmethod.latin`
  - `com.android.settings`
- Skip events with null `packageName`.
- Extract `activityName` using PRD rules:
  - `TYPE_WINDOW_STATE_CHANGED`: `event.className`
  - `TYPE_WINDOW_CONTENT_CHANGED`: `rootNode.className`
- Add DFS screen-tree snapshot extraction from `AccessibilityNodeInfo`.
- Store the final local event shape in Room:
  - `id`
  - `timestamp`
  - `packageName`
  - `activityName`
  - `eventType`
  - `uiSnapshotJson`
  - `searchableText`
  - `synced`
- Enforce PRD data limits:
  - max 1000 nodes during traversal
  - max 200KB `uiSnapshotJson`
  - max 5000 chars `searchableText`
- Add a basic Observer feed:
  - newest events first
  - timestamp
  - package name
  - activity name
  - event type
  - local/sync placeholder
- Add Observer bottom-nav item while preserving existing Trips, Track, and Settings areas.
- Add empty state and service-disabled state with shortcut to Android accessibility settings.

### Out Of Scope

- Polished detail screen.
- Full JSON viewer.
- FTS5 search.
- Advanced filter chips.
- Editable denylist UI.
- Sync, Neon, registration, face auth, and Google SSO.

### Main Implementation Areas

- Accessibility service package/module.
- Room entity, DAO, and database migration/version update.
- Observer repository/use-case layer.
- Basic Compose Observer feed screen.
- Existing bottom navigation and NavHost.

### Tests And Verification

- Unit tests for denylist filtering.
- Unit tests for searchable text generation.
- Unit tests for snapshot node cap and size trimming.
- DAO insert/get tests where feasible.
- Manual real-device check:
  - enable accessibility service
  - open other apps
  - verify events appear in Observer feed
  - verify system denylist noise is reduced

### Exit Criteria

- App compiles.
- Accessibility service can be enabled.
- Events from multiple non-denied packages are stored locally.
- Screen-tree JSON and searchable text are stored with PRD limits.
- Existing GPS tracking navigation remains usable.

## Phase 2: Observer Inspection UI

### Goal

Make captured events inspectable from the app UI.

### User Value

The user can open an event, inspect metadata, preview searchable text, and view the stored UI snapshot.

### Scope

- Add `observer_detail/{eventId}` destination.
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
- Add searchable text preview.
- Add truncated JSON preview.
- Add `observer_json/{eventId}` destination:
  - no bottom nav
  - close/back returns to event detail
  - line-numbered monospace JSON
  - copy-to-clipboard action
  - basic syntax coloring if practical within current UI stack
- Add cursor-based pagination by timestamp with page size 50.
- Trigger next page when the last 5 items become visible.

### Out Of Scope

- FTS5 full-text search.
- Advanced filter chip query model.
- Remote sync.
- Auth.

### Main Implementation Areas

- Observer detail ViewModel/state.
- JSON viewer composable.
- Cursor paging DAO queries.
- Navigation graph updates.

### Tests And Verification

- Unit tests for cursor query boundaries.
- Unit tests for node count and snapshot size metadata.
- Manual UI check for long JSON, large snapshots, and back behavior.

### Exit Criteria

- User can navigate feed -> detail -> JSON viewer -> detail.
- Large JSON remains scrollable and does not break layout.
- Pagination loads older events without duplicates.

## Phase 3: Advanced Filtering + Unified Settings

### Goal

Implement the PRD's full local filtering model and move Observer controls into the existing Settings destination.

### User Value

The user can reduce noise, focus on target apps, compare packages, and manage local observer behavior.

### Scope

- Add FTS5 support for `searchable_text`.
- Add 300ms debounced search.
- Add package chip row:
  - `All` chip clears package selection
  - auto-populated from distinct package names in local DB
  - multi-select packages
- Add top-level text filter chips:
  - multiple terms allowed
  - OR'd together
  - AND'd across selected package conditions
- Add scoped per-package text filters:
  - tapping an already-selected package expands inline input
  - scoped text narrows only that package condition
  - display format like `com.gojek.app - tarif`
- Implement PRD query composition:
  - package conditions OR'd together
  - scoped text applies inside its package condition
  - top-level text condition AND'd with package conditions
  - no package and no text means show all events
- Extend the existing Settings screen with clear sections:
  - GPS tracking settings/status
  - Observer service/status
  - Observer denylist
  - Observer local data
- Add editable denylist:
  - list packages
  - remove package
  - add package inline
  - changes apply to the next captured event
- Add clear local observer events with confirmation dialog.

### Out Of Scope

- Sync status backed by real remote sync.
- Account/auth settings.
- Neon.

### Main Implementation Areas

- FTS5 table/triggers or DAO-managed FTS updates.
- Filter state model and query builder.
- Observer feed UI chips and inputs.
- Settings screen sectioning.
- Denylist persistence.

### Tests And Verification

- Unit tests for query/filter composition.
- Unit tests for top-level text OR behavior.
- Unit tests for scoped package text behavior.
- Unit tests for denylist CRUD.
- Manual UI check for chip wrapping/scrolling and no-result states.

### Exit Criteria

- User can filter by package, global text chips, scoped package text, or combinations.
- Denylist edits affect future captured events.
- Clearing local observer events requires confirmation and empties Observer feed.
- Existing GPS settings remain accessible.

## Phase 4: Sync Engine + Retention

### Goal

Add the local sync state machine and retention behavior behind a remote abstraction.

### User Value

Events are prepared for reliable upload while local data remains protected from premature deletion.

### Scope

- Add `RemoteDataSource` contract:
  - `syncEvents(events)`
  - auth/register methods can be stubbed until later phases
- Add `FakeRemoteDataSource` or no-op implementation for local development.
- Add hybrid sync triggers:
  - unsynced count >= 50
  - or 30 seconds elapsed
- Add batch size 50.
- Add retry with exponential backoff from 2s to 60s max.
- Mark rows as synced only after successful remote result.
- Run retention cleanup after successful sync:
  - delete only synced rows
  - anchor to `MAX(timestamp)` in local DB
  - preserve latest 24-hour captured window
  - never delete unsynced rows
- Add observer sync status state for Settings:
  - pending count
  - synced count
  - current sync status
  - last synced time
- Keep capture and UI usable when sync fails.

### Out Of Scope

- Real Neon connection.
- Google SSO.
- Face auth.

### Main Implementation Areas

- Sync worker/scheduler or app-managed coroutine service.
- Observer repository sync methods.
- DAO methods for unsynced batches, mark synced, max timestamp, cleanup.
- Settings sync status section.

### Tests And Verification

- Unit tests for sync trigger thresholds.
- Unit tests for retry/backoff decisions.
- Unit tests for retention cleanup anchored to max local timestamp.
- Unit tests proving unsynced rows are never deleted.

### Exit Criteria

- Fake successful sync marks events as synced.
- Failed sync leaves rows unsynced and schedules retry.
- Cleanup runs only after success and preserves the latest local 24-hour event window.

## Phase 5: Neon V1 Remote Storage

### Goal

Connect the sync abstraction to the PRD's V1 direct Neon remote storage.

### User Value

Observed events can be persisted remotely while the Android app remains local-first.

### Scope

- Create Neon schema from PRD:
  - `users`
  - `devices`
  - `device_user_sessions`
  - `face_templates`
  - remote `observed_events`
- Add indexes:
  - device/timestamp index for observed events
  - GIN text search index
  - active face template index
  - active device session index
- Add low-privilege V1 direct database role:
  - `INSERT`
  - `SELECT`
  - no destructive privileges
- Add generated local `device_id` / device fingerprint storage.
- Store remote connection configuration in EncryptedSharedPreferences, never hardcoded.
- Implement `NeonDirectDataSource` behind `RemoteDataSource`.
- Upload observed events as device-level data with no user association.
- Keep `ApiDataSource` as the documented V2 replacement path.

### Out Of Scope

- Production API backend.
- Full auth flows.
- Face enrollment.

### Main Implementation Areas

- SQL migration files or documented setup scripts.
- Neon client/driver selection.
- Secure local configuration storage.
- Remote event mapping.
- Sync error mapping.

### Tests And Verification

- Integration test or manual verification against a test Neon database.
- Verify low-privilege role cannot update/delete/drop.
- Verify uploaded event IDs remain device-generated UUID v7 values.
- Verify JSON payload lands as JSONB.

### Exit Criteria

- App can sync a batch to Neon through `RemoteDataSource`.
- Local-first behavior still works offline.
- Direct Neon details are isolated from UI, ViewModels, and accessibility service code.

## Phase 6: Registration + Face Enrollment

### Goal

Implement one-time registration using Google identity first, then face enrollment.

### User Value

The app can identify the registered driver/account and enroll face data without sending raw images.

### Scope

- Add one-time registration gate when no `google_id` exists in EncryptedSharedPreferences.
- Add registration NavHost flow:
  - `register/step1_google_sso`
  - `register/step2_face_capture`
  - `register/step3_success`
- Disable back navigation through completed registration states where required by PRD.
- Add Google Sign-In for identity:
  - obtain `google_id`
  - obtain email/profile fields needed for account section
- Add CameraX face capture screen.
- Add ML Kit bundled face detection.
- Add `FaceProcessor` abstraction:
  - `MLKitFaceProcessor` V1
  - interface stable for MediaPipe/custom V2/V3
- Add `FaceData` contract:
  - normalized landmarks
  - Euler angles
  - liveness challenge hooks
  - frame metadata
- Send `FaceData` JSON only.
- Never transmit raw face image.
- Register user/device/session/template through `RemoteDataSource`.
- Add account section in Settings:
  - user avatar/name/email where available
  - active session status
  - face auth primary label
  - Google SSO fallback auto label

### Out Of Scope

- Full re-auth overlay.
- Face login failure budget.
- Google fallback during login.
- Active liveness beyond V1 passive signals.

### Main Implementation Areas

- Registration state machine.
- Google Sign-In integration.
- CameraX preview/capture.
- ML Kit face processor.
- Remote registration contract.
- EncryptedSharedPreferences account/device storage.

### Tests And Verification

- Unit tests for registration state transitions.
- Unit tests for `FaceProcessor` output normalization where possible.
- Manual device test for Google Sign-In.
- Manual device test for camera permission, preview, face detection, and enrollment.

### Exit Criteria

- Fresh install routes to registration.
- Google identity is obtained before face enrollment.
- FaceData is produced without sending raw image.
- Successful registration stores local identity and opens main app.

## Phase 7: Auth, Re-auth + Production Hardening

### Goal

Complete the PRD's face-first login, Google fallback, re-auth, failure handling, and production verification.

### User Value

The app protects UI access while capture and sync continue uninterrupted.

### Scope

- Add app-wide auth state controller.
- Render auth overlay above the current NavHost destination.
- Do not make auth overlay a navigation destination.
- Gate UI access only:
  - accessibility capture continues
  - GPS tracking continues
  - sync continues
- Implement face-first login:
  - launch face scan immediately on auth requirement
  - send `FaceData`
  - unlock on backend success
- Implement fallback rules:
  - Google SSO fallback only after face attempts fail or face auth cannot complete
  - cached Google token first
  - OAuth flow only when cached token is unavailable/expired
  - no primary Google SSO button
  - low-prominence hint only after at least one failed face attempt
- Implement attempt budget:
  - 3 face attempts per session
  - attempt counter in overlay
  - 30 second lockout after 3 failures
  - force Google OAuth after 5 total failures
- Implement 24-hour re-auth:
  - set `auth_required = true` on expiry
  - detect idle: screen off >= 5 minutes and no accessibility events >= 5 minutes
  - queue re-auth for next screen-on
  - force re-auth on next screen-on if idle not detected within 2 hours
- Add compatibility fallback:
  - `BiometricPrompt` with `BIOMETRIC_STRONG` or `DEVICE_CREDENTIAL` where custom face flow cannot run
- Complete UI states:
  - feed loading
  - feed empty
  - no results
  - sync error banner
  - offline banner
  - auth locked overlay
- Add final polish for JSON viewer, settings, observer feed, and auth overlay.

### Out Of Scope

- Replacing Neon V1 with API V2, unless security requirements change before release.

### Main Implementation Areas

- Auth state controller.
- Auth overlay composables.
- Screen-on/idle detection.
- Remote auth methods.
- Failure handling and fallback orchestration.
- End-to-end test fixtures and manual verification scripts/checklists.

### Tests And Verification

- Unit tests for auth state transitions.
- Unit tests for face failure budget and fallback trigger conditions.
- Unit tests for 24-hour expiry and idle decision logic.
- Integration/manual tests:
  - background capture continues while UI locked
  - sync continues while UI locked
  - GPS tracking continues while UI locked
  - unlock returns to previous screen
  - Google fallback appears only under PRD conditions
  - offline behavior does not lose data
- Run:
  - `:app:compileDebugKotlin`
  - `:app:testDebugUnitTest`
  - `:app:assembleDebug` when machine capacity allows
- Real-device checks:
  - accessibility capture
  - CameraX + ML Kit
  - Google Sign-In
  - Neon sync
  - auth overlay
  - screen off/on re-auth flow

### Exit Criteria

- Full PRD behavior is implemented or explicitly documented as deferred.
- Capture, tracking, sync, and auth can run together without breaking each other.
- All phase-level tests pass.
- Real-device verification checklist is completed.

## Public Interfaces And Data Changes

- Add local Room `ObservedEventEntity` mapped to `observed_events`.
- Add local FTS5 table for `searchable_text`.
- Add Observer DAO methods:
  - insert event
  - get event by ID
  - paged latest events
  - filtered/search events
  - distinct package names
  - unsynced batch
  - mark synced
  - max timestamp
  - delete old synced
  - clear observer events
  - denylist CRUD if denylist is stored in Room
- Add Observer navigation destinations:
  - `observer_feed`
  - `observer_detail/{eventId}`
  - `observer_json/{eventId}`
- Add registration destinations:
  - `register/step1_google_sso`
  - `register/step2_face_capture`
  - `register/step3_success`
- Add Android accessibility service permission/config.
- Add CameraX and ML Kit dependencies in the registration phase.
- Add Google Sign-In dependency in the registration phase.
- Add EncryptedSharedPreferences or equivalent secure storage for:
  - `google_id`
  - device ID/fingerprint
  - Neon V1 connection configuration if direct Neon is enabled
  - auth/session metadata
- Add `RemoteDataSource` implementations:
  - `FakeRemoteDataSource`
  - `NeonDirectDataSource`
  - future `ApiDataSource`
- Add `FaceProcessor` implementations:
  - `MLKitFaceProcessor`
  - future MediaPipe/custom processors

## Implementation Rules

- Keep the existing GPS tracking feature intact unless a later task explicitly asks to change it.
- Keep Observer code separated enough that service capture, local persistence, UI, sync, and auth are not tangled together.
- Do not put Neon logic directly in Compose UI, ViewModels, or the accessibility service.
- Do not make Google SSO a primary login option after registration; face auth remains primary.
- Do not associate observed events with users; events remain device-level.
- Do not delete unsynced local events.
- Do not transmit raw face images.
- During implementation in Codex CLI, follow project rules:
  - create/update `docs/status_report.txt` before modifying or creating code files
  - list affected code files with brief inline comments
  - mark the task done with timestamp
  - provide a concise commit message
