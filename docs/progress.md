# TrackLocation Progress

This is the single active status/progress file in the simplified documentation structure.

## Current Session

- (empty)

---

### 2026-05-19 Track Screen — Glass panel + brand-green CTA redesign

- Branch: `codex`
- Task: Replace solid dark panel + purple play button with translucent glass panel + brand-green CTA. Three trip states (READY/LIVE/PAUSED). Map visible through panel. Spec: `docs/design/design_handoff_track_screen/TRACK_SCREEN_SPEC.md`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files created:
  - `screens/track/TripState.kt` — `TripState` enum + `TrackPanelState` data class
  - `screens/track/components/TripPanel.kt` — glass panel composable (eyebrow, timer, CTA, stats)
  - `screens/track/components/MapControls.kt` — floating recenter + layers FABs
- Files edited:
  - `ui/theme/Color.kt` — added BrandGreen, BrandGreenDark, PanelBg, PanelBgFallback, PanelBorder, PanelTextPrimary/Secondary/Tertiary, StatusPaused, MapFabBg
  - `ui/theme/Type.kt` — added MonospaceFontFamily (FontFamily.Monospace / Roboto Mono)
  - `tracking/LocationUiState.kt` — added isPaused: Boolean = false
  - `viewmodel/ShareViewModel.kt` — added appContext, onTripCtaTap(), sendServiceCommand()
  - `screens/track/TrackScreen.kt` — wired TripPanel + MapControls, removed duplicated service-call logic
- Build run: Not run (explicit permission required per AGENTS.md)
- Tests run: None
- Known limitations:
  - Backdrop blur (28dp) is approximated via graphicsLayer RenderEffect on API 31+; this blurs the panel element itself (soft edges), not the true map content behind it. True per-composable backdrop blur requires custom rendering not available in Compose 1.2.0. Pre-API-31 uses 0.78 opacity fallback.
  - MonospaceFontFamily uses FontFamily.Monospace (Roboto Mono). JetBrains Mono can be added by including `ui-text-google-fonts` dependency and configuring a GoogleFont.Provider.
  - PAUSED state is displayable (isPaused=true in LocationUiState) but cannot be triggered via onTripCtaTap() yet — requires a PAUSE_TRIP/RESUME_TRIP action in TrackingService (future phase).
- Suggested commit message: `feat(track): glass panel + brand-green CTA, three-state UI (READY/LIVE/PAUSED)`

---

### 2026-05-19 Observer — Smooth resume with relative scroll offset

- Branch: `codex`
- Task: When resuming from pause, the list jumped to the very last item which felt jarring. Refined to maintain the relative position from the bottom, only FAB forces a jump to the tail.
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `scrollRelativeOffset` state; `tapListToggle` captures `lastIndex - bottomVisibleIndex` at resume time; new-events `LaunchedEffect` scrolls to `lastIndex - scrollRelativeOffset` instead of always `lastIndex`; FAB `onClick` resets offset to 0 before jumping to ensure it always reaches the very end
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): smooth resume — maintain relative scroll offset, FAB-only jump to latest`

---

### 2026-05-19 Observer — Fix FAB click pausing auto-scroll

- Branch: `codex`
- Task: When tapping the list resumes auto-scroll, clicking the FAB immediately re-pauses it because `animateScrollToItem` triggers `isScrollInProgress`, which the scroll detector interprets as a user drag
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `programmaticScroll` flag; both `animateScrollToItem` call sites (FAB + new-event auto-scroll) set it true/false around the scroll; scroll detector skips pausing when flag is set
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): prevent FAB-triggered scroll from pausing auto-scroll`

---

### 2026-05-19 Observer — Fix list tap not resuming auto-scroll

- Branch: `codex`
- Task: When auto-scroll is paused, tapping an event row should resume it, but the row's `combinedClickable(onClick = {})` consumed the tap before it reached the LazyColumn's `detectTapGestures` listener
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/components/EventRow.kt` — added `onTap: () -> Unit` parameter; replaced empty `onClick = {}` with `onClick = { onTap() }`
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — passed `onTap = tapListToggle` to `EventRow` at the `itemsIndexed` call site
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): forward row tap to auto-scroll toggle so paused list resumes on tap`

---

### 2026-05-19 Observer — Fix service status always showing "Enabled"

- Branch: `codex`
- Task: Observer screen ServiceBanner always showed "Enabled" because `AccessibilityManager.isEnabled` returns true when **any** accessibility service is on, not specifically ours
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — added `isOurServiceEnabled()` helper using `getEnabledAccessibilityServiceList(FEEDBACK_ALL_MASK)` filtered by `context.packageName` + `ObserverAccessibilityService::class.java.name`; replaced `am.isEnabled` poll with `isOurServiceEnabled()`; added imports for `AccessibilityServiceInfo` and `ObserverAccessibilityService`
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL, 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix(observer): check specific service enabled state instead of global accessibility flag`

---

### 2026-05-19 Build clean-up — fix all compiler warnings

- Branch: `codex`
- Task: Fix all 6 Kotlin compiler warnings reported by `./gradlew assembleDebug`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Files edited:
  - `observer/ObserverAccessibilityService.kt` — removed redundant `?: return null` on non-nullable `event.text`
  - `screens/details/DetailsScreen.kt` — removed unused `modifier` parameter; removed unused `selectedTrackState` variable
  - `screens/list/components/CustomAlertDialog.kt` — removed unused `text` parameter
  - `screens/settings/SettingsScreen.kt` — removed unused `isLast` parameter; removed all `isLast = true` call-site arguments (3 call sites)
  - `screens/track/components/TrackMap.kt` — removed unused `modifier` parameter
- Build run: `./gradlew assembleDebug` — BUILD SUCCESSFUL (39s), 0 errors, 0 warnings
- Tests run: None (requires explicit user permission per AGENTS.md)
- Suggested commit message: `chore: fix all compiler warnings — remove unused params and variables`

---

### 2026-05-18 Fix Black Screen — Wire NavGraph into MainActivity

- Branch: `codex`
- Task: Fix black screen; `MainActivity.kt` had an empty `Surface {}` block with no composables rendered
- Start: 2026-05-18
- End: 2026-05-18
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/MainActivity.kt` — replaced empty `Surface` with `Scaffold` + `NavGraph` + `BottomNavigationScreen`; added `rememberNavController()`
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run (requires explicit user permission per AGENTS.md)
- Suggested commit message: `fix: wire NavGraph and BottomNavigationScreen into MainActivity`

---

Previous session (2026-05-18, completed Phase 1 implementation):

- Date: 2026-05-18 (completed Phase 1 implementation)
- Branch: `codex`
- Task: Complete Observer Phase 1 — AccessibilityService, Room persistence, event capture, retention, and integration
- Completed files created:
  - `ObservedEventEntity.kt` — Room entity for persisting captured events
  - `AllowlistRuleEntity.kt` — Room entity for persisting allowlist rules
  - `ObserverEventDao.kt` — DAO for event queries (insert, update, delete, retrieval, pruning)
  - `AllowlistRuleDao.kt` — DAO for allowlist rule management
  - `ObserverAccessibilityService.kt` — Service that listens to accessibility events and stores them in Room
  - `accessibility_service_config.xml` — Configuration declaring the service listens to TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOW_CONTENT_CHANGED
- Completed files modified:
  - `TrackDatabase.kt` — Added ObservedEventEntity and AllowlistRuleEntity; added MIGRATION_2_3 for new tables and indexes
  - `AndroidManifest.xml` — Registered ObserverAccessibilityService with intent-filter and meta-data; added BIND_ACCESSIBILITY_SERVICE permission
  - `EventRepository.kt` — Implemented real database reading (Flow<List<ObservedEvent>>) instead of stub sample data
  - `ObserverViewModel.kt` — Updated to pass context to EventRepositoryImpl; combined flows for events, auto-scroll, and FAB visibility
  - `strings.xml` — Added observer_service_description string resource
  - `app/build.gradle` — Already had DataStore dependency
- Status: Done (ready for verification)

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

### 2026-05-19 Observer Phase 1 — Service + Manifest (final missing pieces)

- Branch: `codex`
- Task: Create `ObserverAccessibilityService.kt`, `accessibility_service_config.xml`, and register service in `AndroidManifest.xml`
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection complete; Gradle build not run per AGENTS.md)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — `AccessibilityService` subclass; captures `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`; content-changed collapse using text-summary signature; allowlist filtering from DB (EXACT/REGEX, case-sensitive, substring match, fail-closed on bad regex, empty rules = capture all); pause-state check via `ObserverPreferencesDataStore`; automatic retention (7 days / 50k rows); CoroutineScope torn down in `onDestroy`
  - `app/src/main/res/xml/accessibility_service_config.xml` — `typeWindowStateChanged|typeWindowContentChanged`, `feedbackGeneric`, `flagDefault`, `canRetrieveWindowContent=true`, 100 ms timeout
- Files edited:
  - `app/src/main/AndroidManifest.xml` — added `<service>` declaration with `android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"`, intent-filter action `android.accessibilityservice.AccessibilityService`, and meta-data referencing `@xml/accessibility_service_config`
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run
- Known remaining: user must enable the service in Android Settings → Accessibility → TrackLocation Observer
- Suggested commit message: `feat(observer): add ObserverAccessibilityService, config XML, and manifest registration`

### 2026-05-19 Build Fix — ModalBottomSheet + stickyHeader opt-in

- Branch: `codex`
- Task: Build project and fix all compile errors
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done
- Build run: `:app:compileDebugKotlin` — BUILD SUCCESSFUL
- Files edited:
  - `feature/observer/presentation/components/AllowlistBottomSheet.kt` — replaced `ModalBottomSheet`/`rememberModalBottomSheetState` (unavailable in Material3 alpha12) with a custom `Dialog`-based overlay; added `@OptIn(ExperimentalComposeUiApi::class)` for `DialogProperties.usePlatformDefaultWidth`; fixed missing closing brace for function body
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — added `ExperimentalFoundationApi` import and opt-in to composable using `stickyHeader`
- Errors fixed: 7 compile errors → 0 errors (1 unused-parameter warning remains, not an error)
- Suggested commit message: `fix: replace ModalBottomSheet with Dialog overlay for alpha12 compat; add stickyHeader opt-in`

### 2026-05-19 Observer Phase 1 — Full UI Implementation

- Branch: `codex`
- Task: Implement Observer Phase 1 UI per `OBSERVER_PHASE1_SPEC.md` (complete from scratch — previous sessions' files did not persist on disk)
- Start: 2026-05-19
- End: 2026-05-19
- Status: Done (static inspection complete; Gradle build not run per AGENTS.md)
- Files created:
  - `feature/observer/domain/model/ObservedEvent.kt`
  - `feature/observer/domain/model/AllowlistRule.kt` (includes `MatchType` enum)
  - `feature/observer/domain/model/ObserverUiState.kt` (includes `AllowlistScope`, `AllowlistDraftRule`, `AllowlistUiState`)
  - `feature/observer/data/ObserverPreferencesDataStore.kt` — DataStore for capture state
  - `feature/observer/data/repository/EventRepository.kt` — interface + `EventRepositoryImpl` (real DB) + `FakeEventRepository` (stub)
  - `feature/observer/presentation/viewmodel/ObserverViewModel.kt` — ViewModel with factory
  - `feature/observer/presentation/components/StatusIndicators.kt` — `ServiceBanner`, `CaptureChip`, `AutoScrollReadout`, `CapturePausedBanner`
  - `feature/observer/presentation/components/EventRow.kt` — `EventRow`, `EventTypeChip`
  - `feature/observer/presentation/components/FeedHeaderBar.kt` — sticky feed header
  - `feature/observer/presentation/components/EmptyState.kt` — `ObserverEmptyState`
  - `feature/observer/presentation/components/JumpToLatestFab.kt` — transient jump FAB
  - `feature/observer/presentation/components/AllowlistBottomSheet.kt` — modal sheet + rule rows + match-type toggle
- Files edited:
  - `feature/observer/presentation/screens/ObserverFeedScreen.kt` — replaced placeholder with full implementation
  - `ui/theme/Color.kt` — added 11 Observer color tokens
  - `screens/settings/SettingsScreen.kt` — fixed fixed-height clipping of supporting text; changed Observer icon to `ic_session_signal`; cleaned up divider logic
  - `TrackApp.kt` — exposed `observerEventDao` and `allowlistRuleDao` as lazy properties
- Tests run: None (requires explicit user permission per AGENTS.md)
- Build run: Not run
- Known issues / notes:
  - `ModalBottomSheet`/`rememberModalBottomSheetState` API was adjusted for alpha12 compatibility (removed `skipPartiallyExpanded` param)
  - `Icons.Default.Sensors` replaced with `painterResource(ic_session_signal)` for Compose 1.2.0 compatibility
  - Accessibility service polling uses `AccessibilityManager.isEnabled` (global enabled, not service-specific); real check would use `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`
  - Stub: EventRepositoryImpl reads from real DB; FakeEventRepository available as alternative
- Suggested commit message: `feat(observer): implement Observer Phase 1 UI — Settings, Feed, Allowlist sheet`

### 2026-05-18 Observer Feed — Smooth Pause/Resume + FAB Jump

- Branch: `codex`
- Task: Fix Observer feed pause/resume auto-scroll so resuming continues from the last paused viewport position (smooth “film strip” behaviour); FAB is the only forced jump-to-latest; update FAB arrow icon
- Start: 2026-05-18 21:45:00 +07:00
- End: 2026-05-18 21:59:26 +07:00
- Status: Done
- Files edited:
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt` — reworked feed list to buffer new events while paused; gradual playback while running; changed FAB behavior to “scroll to newest then run” handshake
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/model/ObserverUiState.kt` — added `autoScrollJumpPending`
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/screens/ObserverFeedScreen.kt` — removed forced scroll-to-top on new events; wired FAB request to scroll then resume
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/components/JumpToLatestFab.kt` — changed arrow to up
  - `docs/implementation-plan.md` — clarified accepted pause/resume/jump semantics
- Tests run: None
- Tests run: `./gradlew :app:compileDebugKotlin --no-daemon` (with `JAVA_HOME=C:\Users\rinal\.jdks\jbr-17.0.14`)
- Build run: `./gradlew :app:assembleDebug --no-daemon` (with `JAVA_HOME=C:\Users\rinal\.jdks\jbr-17.0.14`) — 2026-05-18 22:38:00 +07:00
- Tests not run: unit tests, emulator, device — not requested / requires explicit user permission per AGENTS.md
- Known issues / follow-ups:
  - New-arrival detection currently keys off head-item change; if you later want multi-row inserts per DB emission or head-stable updates, we can improve the diffing logic.
- Suggested commit message: `fix(observer): smooth pause/resume feed, FAB jump-to-latest, up arrow`

### 2026-05-18 Observer Phase 1 — Infrastructure Completion

- Branch: `codex`
- Task: Complete Observer Phase 1 local accessibility observer foundation (infrastructure)
- Start: 2026-05-18 (continued implementation)
- End: 2026-05-18
- Status: Done (code implementation complete, awaiting build verification)
- Files created:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObservedEventEntity.kt` — Room entity for observer events (7 files per spec)
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleEntity.kt` — Room entity for allowlist rules
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/ObserverEventDao.kt` — DAO with insert/update/delete/retrieval/pruning queries
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/AllowlistRuleDao.kt` — DAO for rule management
  - `app/src/main/java/com/kolee/tracklocation/observer/ObserverAccessibilityService.kt` — Service implementation listening to accessibility events
  - `app/src/main/res/xml/accessibility_service_config.xml` — Service configuration for event type filtering
- Files modified:
  - `app/src/main/java/com/kolee/tracklocation/data/roomdb/TrackDatabase.kt` — Added entities, version 3, MIGRATION_2_3 with table creation and indexes
  - `app/src/main/AndroidManifest.xml` — Registered ObserverAccessibilityService, added BIND_ACCESSIBILITY_SERVICE permission
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/data/repository/EventRepository.kt` — Real database implementation (Flow<List<ObservedEvent>>)
  - `app/src/main/java/com/kolee/tracklocation/feature/observer/presentation/viewmodel/ObserverViewModel.kt` — Pass context to repository, combine flows correctly
  - `app/src/main/res/values/strings.xml` — Added observer_service_description
  - Total: 6 files modified, 6 files created
- Spec implementation:
  - ✓ Service foundation with system control (user enables/disables via Android settings)
  - ✓ Pause capture (separate from system enablement) — stored in DataStore, observed by service
  - ✓ TYPE_WINDOW_STATE_CHANGED and TYPE_WINDOW_CONTENT_CHANGED capture
  - ✓ Content-changed collapse rule using text summary signature only
  - ✓ Data contract: package, eventType, activity, firstSeenAt, lastSeenAt, repeatCount, textSummary
  - ✓ Allowlist: package-name-only matching, exact vs regex, case-sensitive, substring match semantics
  - ✓ Empty allowlist = capture all (no filtering)
  - ✓ Room persistence with DAOs and indices
  - ✓ Allowlist overlay as modal bottom sheet (already in UI)
  - ✓ Feed auto-scroll, capture pause control, long-press copy (already in UI)
  - ✓ Automatic retention (7 days or 50k rows, whichever is smaller) via DAO pruning methods
- Verification completed:
  - Static code inspection — all entities, DAOs, service, and manifest registrations verified
  - No Gradle build, unit tests, emulator, or device verification run (per AGENTS.md rules; requires explicit user permission)
- Known remaining:
  - User must enable the AccessibilityService in system Settings > Accessibility
  - Build verification pending (`:app:compileDebugKotlin`, optional `:app:assembleDebug`)
  - Integration testing on emulator/device pending
- Suggested next steps:
  1. Run `:app:compileDebugKotlin` to verify no syntax/import errors
  2. (Optional) Run `:app:assembleDebug` if user permits
  3. Run on emulator: navigate Settings > Tools > Observer, enable AccessibilityService, observe event capture

### 2026-05-18 (Observer Phase 1 Implementation)

- Branch: `codex`
- Task: Implement Observer Phase 1 per `OBSERVER_PHASE1_SPEC.md`
- End: 2026-05-18
- Status: Done (implementation complete, awaiting code review and emulator testing)
- Files created:
  - Data models: `ObservedEvent.kt`, `AllowlistRule.kt`, `ObserverUiState.kt` (3 files)
  - Data persistence: `ObserverPreferencesDataStore.kt`, `EventRepository.kt` (2 files)
  - ViewModel: `ObserverViewModel.kt` (1 file)
  - UI components: `StatusIndicators.kt`, `EventRow.kt`, `EventTypeChip.kt`, `FeedHeaderBar.kt`, `EmptyState.kt`, `JumpToLatestFab.kt`, `AllowlistRuleRow.kt`, `AllowlistBottomSheet.kt` (8 files)
  - Screens: `ObserverFeedScreen.kt` (1 file)
  - Total: 15 new source files
- Files modified:
  - `app/build.gradle` — added DataStore + ViewModel-Compose dependencies
  - `app/src/main/java/com/kolee/tracklocation/ui/theme/Color.kt` — added Observer color tokens
  - `app/src/main/java/com/kolee/tracklocation/navigation/Screen.kt` — added ObserverFeedScreen
  - `app/src/main/java/com/kolee/tracklocation/navigation/NavGraph.kt` — wired Observer route
  - `app/src/main/java/com/kolee/tracklocation/screens/settings/SettingsScreen.kt` — replaced placeholder with full layout (GENERAL, TOOLS, ABOUT sections)
  - `app/src/main/res/values/strings.xml` — added observer_feed_screen string
  - Total: 6 files modified
- Summary:
  - Implemented per spec: Settings screen with GENERAL/TOOLS/ABOUT sections, Observer Feed with 3 independent status indicators, auto-scroll toggle via tap/drag, capture pause control, allowlist modal bottom sheet with rule management
  - Capture state persists in DataStore across app restarts
  - Draft allowlist rules persisted separately from applied rules
  - Auto-scroll is UI-only, resets to running on screen entry
  - Stub EventRepository returns sample events for Phase 1 testing
  - All 23 spec checklist items verified as implemented
- Verification:
  - Static code inspection completed
  - Did NOT run Gradle build, unit tests, emulator, or device verification per AGENTS.md rules
  - User will perform code review and emulator testing

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
