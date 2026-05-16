# TrackLocation UI Screen Specification

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

This file is the current UI source of truth after CR#1. It absorbs the accepted Session/List/Track/Settings baseline and keeps Observer screens marked as planned until their navigation placement is decided.

## Screen Status

| Screen | Status | Notes |
|---|---|---|
| Session | Current after CR#1 | First-class bottom-nav destination |
| List | Current after CR#1 | Trip list only; owns always-recording switch |
| Track | Current, modified by CR#1 | Start/Stop controls trip range, not global recording |
| Settings | Current | Unified settings destination |
| Observer Feed | Planned | Navigation placement unresolved |
| Observer Event Detail | Planned | Depends on Observer Feed |
| JSON Viewer | Planned | Full-screen, no bottom nav |
| Registration Step 1 | Planned | Observer/auth phase |
| Registration Step 2 | Planned | Observer/auth phase |
| Registration Step 3 | Planned | Observer/auth phase |
| Auth Overlay | Planned | App-wide overlay, not a destination |

## Current bottom navigation

```text
Session / List / Track / Settings
```

See `../architecture/navigation.md` for navigation authority.

## 1. Session Screen

Purpose:

Show always-recorded location sessions. These sessions are ON-to-OFF recording periods and are not trips.

Required content:

- Screen title: `Sessions`.
- List of recorded sessions, newest first.
- Active session, if present, pinned or clearly marked at the top.
- Empty state when no sessions exist.

Each session row should show:

- Start time.
- Duration.
- Distance.
- Optional: end time, point count, active status.

Empty state:

- Title: `No sessions recorded yet`.
- Supporting text: sessions appear when always-recording is turned ON from the List screen.

## 2. List Screen

Purpose:

Show explicit trips only.

Required CR#1 changes:

- Header top label: `Trip Tracker`.
- Main title: `Trips` or accepted `List` label.
- Replace prior export pill with compact always-recording switch.

Switch behavior:

- OFF means no always-recorded session is currently active.
- ON means foreground location recorder is active.
- If permission is missing, tapping ON triggers location permission flow.
- If permission is denied, switch remains OFF.

Trip list behavior:

- Continue showing trips only.
- Do not show always-recorded sessions in the trip list.
- Trip metrics are based on the trip's location range.

## 3. Track Screen

Purpose:

Start and stop explicit trip ranges.

CR#1 semantic changes:

- `Start` begins a trip range inside the canonical location log.
- If always-recording is OFF, starting a trip automatically starts always-recording.
- `Stop` ends the trip range but does not stop always-recording.
- Avoid copy like `Stop recording` on this screen.
- Prefer `Start trip` and `Stop trip`.

## 4. Settings Screen

Purpose:

Unified operational settings destination.

Current required sections:

- GPS tracking status and permissions.
- Always-recording/session status if available.
- Data controls as implementation matures.

Planned Observer sections:

- Account.
- Observer service.
- Observer denylist.
- Sync.
- Local Observer data.

## 5. Planned Observer Screens

Observer remains governed by `../product/accessibility-observer-prd.md` and the original screen specification below. Before implementation, decide whether Observer becomes:

1. A fifth bottom-nav tab.
2. A child/tool under Settings.
3. Part of a redesigned app shell.

---

# Original UI Screen Specification Preserved

# TrackLocation UI and Screen Specification

Last updated: 2026-05-14

## Purpose

This document translates the PRD and implementation plan into a UI handoff specification for a product designer or UI AI design tool.

TrackLocation is evolving from a GPS trip tracker into a combined driver utility and accessibility event observer. The existing GPS tracking feature must remain intact. The new Observer feature helps a developer or driver-support operator inspect accessibility events emitted by any app on the Android device.

## Product Positioning

The app should feel like an operational Android tool for drivers and developers:

- Clear, readable, and efficient during repeated use.
- Calm enough for driving-adjacent workflows.
- Dense enough for inspecting technical data without feeling like a consumer landing page.
- Designed for both light and dark theme.
- Optimized first for a 390 x 844 Android viewport, then responsive to common Android phone sizes.

Avoid marketing-style hero sections, decorative gradients, oversized illustrations, and playful styling. The interface should communicate trust, data continuity, and inspection precision.

## Primary Users

- Driver or device operator: starts and stops GPS trips, keeps the app running, completes registration and re-authentication.
- Developer or support operator: observes app accessibility events, filters noisy data, opens event details, inspects UI tree JSON, and checks sync status.

## Navigation Model

Use a single Android app shell with bottom navigation for peer destinations.

Recommended bottom navigation:

- Trips
- Track
- Observer
- Settings

Secondary screens are pushed from their parent:

- Trip detail opens from Trips.
- Observer event detail opens from Observer.
- JSON viewer opens from Observer event detail and should not show bottom navigation.
- Registration is a one-time flow before the main app.
- Auth overlay is not a destination. It layers over the current screen and preserves the user's place.

## Global App Shell

### Bottom Navigation

Use clear icon plus label items. Keep labels short and stable:

- Trips: trip history/list icon.
- Track: location pin or route icon.
- Observer: activity/list-inspection icon.
- Settings: gear icon.

Selected state should be obvious without using overly saturated color. Include enough touch target space for one-handed Android use.

### Status Patterns

Use small badges for system state:

- Active
- Inactive
- Synced
- Pending
- Offline
- Retrying
- Locked

Badges should be compact and scannable. Use color plus text, not color alone.

## Screen Inventory

### 1. Trips

Existing feature area. This screen shows completed GPS trips and high-level trip history.

Design intent:

- Trip journal plus operational record.
- The user can quickly understand recent trips, totals, and current trip availability.

Required content:

- Top app header: "Trips" or "Trip Tracker".
- Current trip panel with clear Start or Continue action where relevant.
- Dashboard metrics:
  - number of trips
  - total distance
  - total hours
- Search/filter row for trips, places, or dates.
- Recent trip cards with:
  - start location
  - end location
  - trip date/time
  - distance in km
  - duration
  - average speed
- Bottom navigation with Trips selected.

States:

- Loading: skeleton trip cards.
- Empty: no trips recorded yet, with a clear route to start tracking.
- Error: non-blocking data load message.

Reference:

- Existing mockup assets live in `design/`.
- Preserve the clean light/dark mobile direction from the trip list mockups.

### 2. Track

Existing live GPS tracking screen.

Design intent:

- Full-screen map-first utility.
- Controls should be easy to reach and impossible to confuse.

Required content:

- Map as the main surface.
- Current route/path visualization.
- Current location indicator.
- Bottom running card or control panel.
- Start/stop primary action.
- Live stats:
  - elapsed time
  - distance
  - current or average speed where available
- Permission state when location permission is missing.

States:

- Permission required.
- Ready to start.
- Actively tracking.
- Paused/stopping.
- Location unavailable.

Interaction notes:

- The tracking control must remain visible and reachable.
- Stopping a trip should clearly communicate that the trip will be saved.
- Avoid placing critical controls under system navigation or map attribution.

### 3. Observer Feed

New first-class feature. This is the main accessibility event inspection screen.

Design intent:

- Developer inspection feed.
- Fast scanning of event stream.
- Powerful filtering without hiding how filters combine.

Required content from top to bottom:

- Top bar:
  - title: "Observer"
  - service status indicator: Active or Inactive
  - shortcut to Settings or service configuration
- Service-disabled banner when accessibility service is off:
  - concise message
  - action to open Android accessibility settings
- Filter area:
  - package chip row
  - top-level text filter chip row
  - add text filter input affordance
- Summary row:
  - total matching events
  - sync status badge
  - last synced time
- Event feed:
  - newest first
  - infinite scroll
  - page size expectation: 50
- Bottom navigation with Observer selected.

Package chip behavior:

- "All" chip always appears first.
- Package chips are generated from captured package names.
- Package chips support multi-select.
- Selecting "All" clears selected packages.
- Tapping a selected package opens an inline scoped text input for that package.
- A package with scoped text displays like `com.gojek.app · tarif`.
- A package without scoped text displays like `com.indriver.app`.

Text filter behavior:

- User can add one or more global text filter chips.
- Global text chips are OR'd together.
- Global text filters apply across selected packages.
- Scoped package text only narrows that package's events.

Event card content:

- Package name.
- Activity name, if available.
- Event type badge.
- Timestamp, relative and/or absolute.
- Sync state.
- Searchable text preview, one to two lines.
- Visual affordance that the card opens detail.

States:

- Loading: skeleton cards; filters disabled or skeletonized.
- Empty capture: "No events captured yet" with hint to open a target app.
- No results: show active filters and clear-filter action.
- Offline: non-blocking banner: "Offline - events stored locally".
- Sync error: non-blocking banner: "Sync failed - retrying in Xs".
- Service inactive: clear action to enable the accessibility service.

### 4. Observer Event Detail

Opened from an event card.

Design intent:

- Structured event inspection.
- Metadata first, large technical payload second.

Required content from top to bottom:

- Top bar:
  - back arrow
  - title: "Event detail"
  - share/copy action if useful
- Metadata card:
  - activity name
  - package name
  - event type badge
  - sync status badge
- Metadata grid:
  - timestamp
  - event ID
  - node count
  - snapshot size
- Searchable text section:
  - plain text preview, first 300 characters
  - optional copy action
- UI snapshot section:
  - JSON preview, first 3 lines
  - primary action: "View full JSON"
- Bottom navigation may remain visible here if implementation keeps this as a regular detail destination. JSON viewer must hide bottom navigation.

States:

- Loading event.
- Event not found.
- Snapshot missing or malformed.
- Copy/share confirmation.

### 5. JSON Viewer

Opened from Observer Event Detail.

Design intent:

- Full-screen technical inspection mode.
- Designed for long JSON up to roughly 200KB.

Required content:

- Top bar:
  - close X or back button
  - activity name if available
  - truncated event ID
- Sub-bar:
  - node count
  - file size
  - copy-to-clipboard button
- Body:
  - scrollable JSON
  - monospace font
  - line numbers
  - basic syntax coloring:
    - keys
    - string values
    - numbers/booleans/null
  - comfortable horizontal and vertical scrolling for long lines
- No bottom navigation.

Dismissal:

- Close returns to Observer Event Detail.
- Android back also returns to Observer Event Detail.

### 6. Settings

Unified settings destination for GPS tracking, Observer, Sync, and Account/Auth.

Design intent:

- Operational configuration surface.
- Group by feature area so the app does not feel like several unrelated tools.

Required sections:

- Account
  - user avatar or initials
  - name and email when available
  - active session badge
  - face authentication row: "Primary · re-auth every 24h"
  - face auth toggle is always on or presented as non-disableable
  - Google SSO fallback row: "Used only when face auth fails" with "Auto" badge
- GPS tracking
  - tracking permission/status
  - current tracking state
  - any trip retention or data controls if needed
- Observer service
  - accessibility service status
  - open Android accessibility settings action
  - capture scope explanation: all packages except denylist
- Observer denylist
  - list denied packages
  - remove package action per row
  - add package inline input
  - helper text: changes apply to the next captured event
- Sync
  - pending count
  - synced count
  - current sync status
  - last synced time
  - retry/backoff state when relevant
- Local data
  - clear local observer events action
  - destructive confirmation dialog before clearing

States:

- Signed in.
- Registration incomplete.
- Sync healthy.
- Sync failing/retrying.
- Offline.
- Observer service disabled.
- Destructive confirmation.

### 7. Registration Step 1: Google Sign-In

Shown only when no local `google_id` exists.

Design intent:

- Identity first, then face enrollment.
- No skip path.

Required content:

- App logo or simple product mark.
- Title: "Welcome to Accessibility Observer" or updated app name if branding changes.
- Short description of capture purpose.
- Primary CTA: "Sign in with Google".
- Error state with retry.

Rules:

- Google sign-in is the only CTA on this screen.
- Back navigation through registration is disabled by product rule.

### 8. Registration Step 2: Face Capture

Design intent:

- Camera enrollment with strong privacy reassurance.
- Make alignment state obvious.

Required content:

- Step indicator with step 2 active.
- Heading: "Capture your face".
- Privacy note: "Only landmark coordinates are sent. Your image never leaves this device."
- Camera viewfinder.
- Face guide oval.
- Corner alignment markers.
- Live indicator.
- Instruction hint, such as "Align face within the oval".
- Primary CTA: "Capture & continue".
- Secondary action to return to Step 1 only before registration is completed.

States:

- Camera permission required.
- Camera unavailable.
- Face not detected.
- Face aligned.
- Capturing.
- Capture failed, retry available.

### 9. Registration Step 3: Success

Required content:

- Step indicator complete.
- Success icon.
- Heading: "You're all set".
- Body: "Your device is registered. Face authentication is active."
- Primary CTA: "Start observing".

Rules:

- No back navigation from this screen.
- CTA opens the main app.

### 10. Auth Overlay

App-wide overlay shown when re-authentication is required.

Design intent:

- Lock UI access without losing current navigation state.
- Face-first. Google must not look like an equal login option.

Overlay behavior:

- Current screen remains behind overlay.
- Background is dimmed and blurred.
- Capture, sync, and GPS tracking continue in the background.
- Overlay cannot be manually dismissed.
- Successful auth removes overlay and returns to the previous screen.

Required content:

- Face ID icon or scanning ring.
- Status text: "Ready to scan" or equivalent.
- Heading: "Verify your identity".
- Body: "Session expired after 24 hours. Look at the camera to unlock."
- Primary CTA: "Scan face to unlock".
- Attempt counter: "X of 3 attempts remaining".
- After at least one failed attempt only:
  - low-prominence text link: "Having trouble? Sign in with Google instead"

Rules:

- Do not show Google SSO as a primary button.
- Do not show Google fallback before at least one failed face attempt.
- After repeated failure, show lockout state and fallback flow per PRD.

## Visual System Direction

### Tone

Use an Android Material 3 direction with restrained, operational styling.

Recommended qualities:

- high readability
- compact cards
- clear information hierarchy
- subtle dividers
- technical but approachable data surfaces
- light and dark parity

Avoid:

- one-note purple/blue gradient themes
- decorative blobs or abstract backgrounds
- marketing hero sections
- oversized rounded cards nested inside other cards
- text that explains UI controls instead of labeling them directly

### Color

Use a small semantic palette:

- Primary: action and selected navigation.
- Surface: screen background and cards.
- Success: active/synced.
- Warning: pending/retrying.
- Error: failed/destructive.
- Neutral: metadata and secondary text.

Every status must include a text label, not only a color.

### Typography

Use a native Android-friendly sans-serif, preferably Material default typography.

Use monospace only for:

- event IDs
- package names where useful
- JSON viewer
- technical metadata values

Do not scale font size by viewport width. Keep headings modest inside dashboards, cards, and technical panels.

### Layout

Target viewport for design outputs:

- 390 x 844 mobile.

Also account for:

- small Android phones around 360 px wide
- larger phones around 430 px wide
- system navigation and status bars
- dark theme

Cards should be compact with radius around 8 dp unless the design system already defines otherwise.

## Accessibility Requirements

- Minimum touch target: 48 x 48 dp.
- Text contrast should meet WCAG AA.
- Do not rely on color alone for state.
- Support long package names without layout breakage.
- Support long event IDs by truncating middle or wrapping deliberately.
- JSON viewer must remain usable with large payloads.
- Auth overlay must be screen-reader coherent and block interaction with background controls.

## Handoff Deliverables Requested From Designer or UI AI Tool

Produce light and dark variants for these screens:

- Trips
- Track
- Observer Feed
- Observer Event Detail
- JSON Viewer
- Settings
- Registration Step 1
- Registration Step 2
- Registration Step 3
- Auth Overlay over at least one main screen

For each screen, include:

- default state
- loading/empty/error states where specified
- component annotations for cards, chips, badges, and controls
- spacing and typography tokens if available
- Android-safe handling of status bar and navigation bar

## Suggested UI AI Prompt

Use this prompt when handing off to a UI AI designer app:

```text
Create a complete Android mobile UI specification and visual screen set for TrackLocation, a driver utility that combines GPS trip tracking with an Accessibility Event Observer for developer inspection.

Design for a 390 x 844 Android viewport in both light and dark theme. The app should feel operational, calm, technical, and efficient. Use Material 3 style, compact cards, clear badges, readable typography, and restrained color. Avoid marketing-style hero sections, decorative gradients, nested cards, and playful styling.

Main bottom navigation: Trips, Track, Observer, Settings.

Create these screens:
1. Trips: trip history dashboard with current trip action, metrics, search/filter row, recent trip cards, empty/loading states.
2. Track: full-screen map with route path, current location, bottom tracking control card, start/stop action, live stats, permission state.
3. Observer Feed: accessibility event feed with service status, package filter chips, text filter chips, add text filter input, summary row, sync/offline banners, event cards, loading/empty/no-result states.
4. Observer Event Detail: metadata card, event type and sync badges, metadata grid, searchable text preview, JSON preview, View full JSON action.
5. JSON Viewer: full-screen technical viewer with close button, event ID, node count, file size, copy action, line-numbered monospace JSON with syntax coloring, no bottom navigation.
6. Settings: grouped sections for Account, GPS tracking, Observer service, Observer denylist, Sync, and Local data. Include destructive clear-data confirmation.
7. Registration Step 1: Google sign-in only.
8. Registration Step 2: face capture with camera viewfinder, face guide oval, privacy note, capture action, camera/face states.
9. Registration Step 3: success state and Start observing action.
10. Auth Overlay: dimmed/blurred current screen behind, face-first re-auth, attempt counter, Google fallback only as low-prominence text after a failed face attempt.

Important product rules:
- GPS tracking remains a first-class feature.
- Observer captures all packages except a configurable denylist.
- Package filtering is a viewing concern in the Observer Feed, not a capture setting.
- JSON can be large, so JSON Viewer must be full-screen and scrollable.
- Face authentication is primary. Google SSO is only automatic fallback and must not be presented as an equal primary login option after registration.
- Auth overlay gates UI only; background GPS tracking, event capture, and sync continue.

Deliver polished light and dark screens with component annotations, states, and spacing guidance suitable for Android implementation.
```
