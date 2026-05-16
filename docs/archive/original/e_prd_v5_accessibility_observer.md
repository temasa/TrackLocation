# E-PRD v5 — Accessibility Event Observer (AI-Executable)
> Consolidated from review sessions. All sections locked unless marked 🔄 EXTENSIBLE.
> v4 changes: Face authentication promoted to primary login method; Google SSO demoted to automatic fallback only (no manual user choice). UI section updated to reflect face-first auth screens and JSON viewer.
> v5 changes: Removed TARGET_PACKAGE_NAME allowlist filter. Observer now captures ALL accessibility events from ALL packages, excluding a configurable system noise denylist. Package filtering is a UI-layer concern (search/filter), not a capture-layer concern. Idle detection updated accordingly.

---

## 1. 🎯 Final Scope (LOCKED)

```
Purpose:         Developer inspection tool — observe and identify UI events across any app on the device
Capture scope:   ALL packages, excluding configurable system noise denylist
UI Snapshot:     FULL TREE (no depth limit)
Search:          FTS5 (SQLite) / GIN index (Neon)
Sync:            Hybrid (batch size + time-based)
Pagination:      Cursor-based infinite scroll
UI:              Main feed + package filter chip + event detail + JSON viewer
Auth:            Google SSO (identity) + Face authentication (presence)
Users:           One device, multiple drivers (shift-based)
Events:          Device-level (no user association)
```

---

## 2. 🏗️ End-to-End Data Flow

```
AccessibilityEvent
    ↓
Denylist Filter (drop system noise packages)
    ↓
UI Snapshot Extraction (FULL TREE, DFS)
    ↓
Size Enforcement (nodes → snapshot → searchable_text)
    ↓
activityName Extraction
    ↓
ObservedEvent Assembly (UUID v7 id)
    ↓
SQLite (Room) — local-first
    ↓
Sync Queue (hybrid trigger)
    ↓
Batch Upload → RemoteDataSource (V1: Neon direct / V2: API)
    ↓
Jetpack Compose UI (package filter chip + FTS search + infinite scroll)
```

---

## 3. 📦 Module Contracts (STRICT)

### 3.1 Accessibility Observer

**Triggers:**
- `TYPE_WINDOW_STATE_CHANGED`
- `TYPE_WINDOW_CONTENT_CHANGED`

**Capture scope:** ALL packages, except those on the denylist.

**Denylist filter (configurable, default entries):**
```kotlin
val DEFAULT_DENIED_PACKAGES = setOf(
    "android",                          // core OS framework events
    "com.android.systemui",             // status bar, notifications, overlays
    "com.android.launcher3",            // home screen
    "com.google.android.inputmethod.latin", // keyboard
    "com.android.settings",            // settings app
    // 🔄 EXTENSIBLE — user can add/remove entries in Settings
)

fun shouldCapture(event: AccessibilityEvent): Boolean =
    event.packageName != null &&
    event.packageName !in deniedPackages
```

**Design rationale:**
```
The tool's purpose is to help developers identify which accessibility events
a target app emits, and what their UI trees look like. This requires seeing
ALL events across apps — the developer selects the app of interest via the
UI filter chip, not by pre-configuring a single allowlist target.

The denylist exists only to reduce noise from high-frequency system packages
that are never of inspection interest (keyboard, launcher, OS framework).
```

**activityName Extraction:**
```
TYPE_WINDOW_STATE_CHANGED  → use event.className
TYPE_WINDOW_CONTENT_CHANGED → use rootNode.className
Both null → store as null (acceptable)
```

**Output Model:**
```kotlin
data class RawEvent(
    val timestamp: Long,
    val eventType: Int,
    val packageName: String,
    val className: String?
)
```

---

### 3.2 UI Snapshot Extractor (FULL TREE)

**Traversal:** DFS, do NOT skip nodes

**Size Enforcement Order (strict priority):**
```
1. Hard cap at 1000 nodes during traversal
2. After serialization: if uiSnapshotJson > 200KB
   → drop nodes from end (deepest levels first)
   → re-serialize until ≤ 200KB
3. searchable_text hard truncate at 5000 chars after generation
```

**Pseudocode:**
```kotlin
var nodes = traverseFullTree(root).take(1000)
var json = serialize(nodes)
while (json.size > 200_000) {
    nodes = nodes.dropLast(50)
    json = serialize(nodes)
}
val searchableText = generateSearchableText(nodes).take(5000)
```

**Node Model:**
```kotlin
data class UiNodeSnapshot(
    val text: String?,
    val contentDesc: String?,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean
)
```

---

### 3.3 Searchable Text Generator

```
searchable_text = concat(all text + contentDesc).lowercase()
```

Constraints:
- Remove nulls before concat
- Hard truncate at 5000 chars

---

### 3.4 Observed Event

```kotlin
data class ObservedEvent(
    val id: String = UUID7.generate(),  // time-ordered, fast insert + access
    val timestamp: Long,
    val packageName: String,
    val activityName: String?,          // see 3.1 extraction rules
    val eventType: Int,
    val uiSnapshotJson: String,         // max 200KB
    val searchableText: String,         // max 5000 chars
    val synced: Boolean = false
)
```

---

## 4. 🗄️ SQLite Schema (Room)

```sql
CREATE TABLE observed_events (
    id TEXT PRIMARY KEY,           -- UUID v7
    timestamp INTEGER NOT NULL,
    package_name TEXT NOT NULL,
    activity_name TEXT,
    event_type INTEGER NOT NULL,
    ui_snapshot_json TEXT NOT NULL, -- max 200KB
    searchable_text TEXT,           -- max 5000 chars
    synced INTEGER DEFAULT 0
);
```

**Indexes:**
```sql
CREATE INDEX idx_timestamp ON observed_events(timestamp DESC);
CREATE INDEX idx_synced ON observed_events(synced);
CREATE INDEX idx_synced_timestamp ON observed_events(synced, timestamp); -- retention cleanup query

-- FTS5 for full-text search (replaces LIKE query)
CREATE VIRTUAL TABLE observed_events_fts USING fts5(
    searchable_text,
    content='observed_events',
    content_rowid='rowid'
);
```

**Query System:**
```sql
-- FTS5 query (replaces LIKE '%text%')
SELECT oe.* FROM observed_events oe
JOIN observed_events_fts fts ON oe.rowid = fts.rowid
WHERE fts.searchable_text MATCH :text
ORDER BY oe.timestamp DESC
LIMIT :limit;
```

- Search debounce: 300ms
- Event capture debounce: 200ms

---

## 5. 🔄 Sync Engine (HYBRID)

**Trigger:**
```
unsynced_count >= 50 OR 30 seconds elapsed
```

**Batch size:** 50

**Retry:** exponential backoff (2s → 60s max)

**Local Retention Policy (runs on every successful sync batch):**
```
After each successful batch upload, run cleanup in the same transaction:

DELETE FROM observed_events
WHERE synced = 1
AND timestamp < (
    SELECT MAX(timestamp) FROM observed_events
) - 86400000  -- 24 hours in milliseconds

Rules:
- Anchor is MAX(timestamp) in local DB, NOT the current clock
- Preserves the most recent 24-hour window of captured data,
  regardless of how old that window is in calendar time
- Only synced rows are ever eligible for deletion
- Unsynced rows are NEVER deleted regardless of age
- If all rows fall within the 24h window, nothing is deleted
- Cleanup only runs on successful sync — never on failure or retry

Design intent:
- Guarantees the local DB always has a full day of browsable data
- Protects the case where the device is offline for days —
  the last captured day stays intact until newer data arrives
- Prevents sync retries from wiping data that hasn't safely landed on Neon
```

**Pseudocode:**
```kotlin
suspend fun syncAndCleanup() {
    val batch = db.getUnsynced(limit = 50)
    val result = remote.syncEvents(batch)
    if (result.success) {
        db.markSynced(batch.map { it.id })
        val anchor = db.getMaxTimestamp() ?: return
        db.deleteOldSynced(before = anchor - Duration.ofDays(1).toMillis())
    }
    // on failure: no cleanup, retry via backoff
}
```

**RemoteDataSource abstraction:** 🔄 EXTENSIBLE
```kotlin
interface RemoteDataSource {
    suspend fun syncEvents(events: List<ObservedEvent>): SyncResult
    suspend fun authenticateFace(payload: FaceData): AuthResult
    suspend fun registerUser(googleId: String, faceData: FaceData): RegisterResult
}

// V1 — direct Neon connection
class NeonDirectDataSource : RemoteDataSource { ... }

// V2 — swap with no schema changes required
class ApiDataSource : RemoteDataSource { ... }
```

---

## 6. 📜 Pagination

- Cursor-based using timestamp
- Page size: 50
- Trigger: last 5 items visible

---

## 7. 🎨 UI (Jetpack Compose)

### 7.1 Architecture

```
Single Activity — MainActivity hosts a NavHost (Jetpack Compose Navigation)
All screens are composable destinations within the same NavHost
No fragments — Compose-only
```

### 7.2 Navigation Graph

```
Registration flow (one-time, shown when no google_id in EncryptedSharedPreferences):
  register/step1_google_sso  →  register/step2_face_capture  →  register/step3_success
  (no back stack — user cannot navigate back through registration once completed)

Main flow (post-registration):
  main_feed
    ↓ tap event card
  event_detail/{eventId}
    ↓ tap "View full JSON"
  json_viewer/{eventId}         ← no bottom nav, back returns to event_detail

  main_feed  ←→  settings       (bottom nav, peer destinations, no back stack between them)

Auth overlay:
  Rendered as a full-screen composable layered over the current destination
  Not a NavHost destination — it is a conditional overlay driven by auth state
  Does NOT interrupt navigation state — current destination stays in back stack
  Dismisses automatically on successful auth
```

### 7.3 Screen Specs

**main_feed**
```
Layout (top to bottom):
  1. Top bar: app name + service status indicator (● Active / ● Inactive) + settings icon
  2. Filter area (top-level)
     - Package chip row (horizontally scrollable)
     - Text chip row (horizontally scrollable, appears once ≥ 1 text chip exists)
     - "Add text filter" input affordance
  3. Summary row: total event count badge + sync status badge + "Last synced X ago"
  4. LazyColumn — event cards (infinite scroll, cursor-based)
  5. Bottom navigation bar: Events | Settings

Filter model:

  TOP-LEVEL PACKAGE CHIPS
  - Auto-populated from distinct package_name values in local DB
  - Multi-select — tap to toggle on/off
  - "All" chip always first; selecting it clears all other selections
  - Tapping an already-selected package chip expands an inline scoped text
    input directly beneath that chip
  - Scoped text narrows results within that package only (see query logic)

  TOP-LEVEL TEXT CHIPS
  - User types a term into "Add text filter" input → confirms → becomes a chip
  - Multiple text chips allowed — OR'd together
  - Applied AND across the entire package selection
  - Text chips are independent of package chips (no package scope)

  PER-PACKAGE SCOPED TEXT
  - Triggered by tapping an already-selected package chip
  - Inline text input expands beneath the chip
  - Scoped text replaces the bare package match for that package only
  - A package with scoped text shows as: [com.gojek.app · tarif]
  - A package without scoped text shows as: [com.indriver.app]

Filter query logic:

  -- Build per-package conditions
  pkg_conditions =
    FOR each selected package:
      IF package has scoped text:
        (package_name = :pkg AND searchable_text MATCH :scoped_term)
      ELSE:
        (package_name = :pkg)

  -- Build top-level text condition
  text_condition =
    IF top-level text chips exist:
      (searchable_text MATCH :term1 OR searchable_text MATCH :term2 OR ...)
    ELSE:
      (no condition)

  -- Combine
  WHERE (pkg_condition1 OR pkg_condition2 OR ...)
  AND   text_condition          -- omitted if no top-level text chips

  -- No packages selected + no text chips → show all events (no WHERE clause)
  -- No packages selected + text chips exist → text condition applies across all packages

Full SQL example:
  -- Packages: com.gojek.app (scoped: "tarif"), com.indriver.app (no scope)
  -- Top-level text: "promo", "fare"

  SELECT oe.* FROM observed_events oe
  JOIN observed_events_fts fts ON oe.rowid = fts.rowid
  WHERE (
    (oe.package_name = 'com.gojek.app' AND fts.searchable_text MATCH 'tarif')
    OR
    (oe.package_name = 'com.indriver.app')
  )
  AND (
    fts.searchable_text MATCH 'promo'
    OR fts.searchable_text MATCH 'fare'
  )
  ORDER BY oe.timestamp DESC
  LIMIT :limit;
```

**event_detail/{eventId}**
```
Layout:
  1. Top bar: back arrow + "Event detail" + share icon
  2. Metadata card: activityName, packageName, eventType badge, sync status badge
  3. Metadata grid (2 col): timestamp, event ID, node count, snapshot size
  4. Searchable text section: plain text preview (first 300 chars)
  5. UI snapshot section: truncated JSON preview (first 3 lines) + "View full JSON ↗" button
  6. Bottom navigation bar: Events | Settings
```

**json_viewer/{eventId}**
```
Layout:
  1. Top bar: close (X) button + activityName + truncated event ID
  2. Sub-bar: node count + file size + copy-to-clipboard button
  3. Scrollable body: syntax-highlighted JSON with line numbers
     · JSON keys: distinct color
     · String values: distinct color
     · Numbers and booleans: distinct color
     · Monospace font throughout
  4. No bottom navigation bar (modal context)
Dismiss: X returns to event_detail
```

**settings**
```
Layout:
  1. Top bar: "Settings"
  2. Account section:
     - User avatar + name + email
     - Active session status badge
     - Face authentication row: label + "Primary · re-auth every 24h" + toggle (always ON, not disableable)
     - Google SSO fallback row: label + "Used only when face auth fails" + "Auto" badge (not a toggle)
  3. Denylist section:
     - List of currently denied packages (each row: package name + remove button)
     - "Add package" row → inline text input
     - Changes take effect immediately on next captured event
  4. Sync section:
     - Sync status row: synced count + pending count + status badge
     - "Clear local events" row → destructive action, requires confirmation dialog
  5. Bottom navigation bar: Events | Settings
```

### 7.4 Registration Flow (one-time)

```
Triggered when: no google_id found in EncryptedSharedPreferences on app launch
Back navigation: disabled throughout — user must complete or force-quit

Step 1 — Google Sign-In
  Layout:
    - App logo + "Welcome to Accessibility Observer"
    - Brief description of what the app does
    - "Sign in with Google" button (only CTA on this screen)
    - No skip option
  On success: navigate to Step 2
  On failure: show error message, allow retry

Step 2 — Face Capture
  Layout:
    - Step indicator (3 dots, step 2 active)
    - "Capture your face" heading
    - Privacy note: "Only ~1KB of landmark coordinates are sent. Your image never leaves this device."
    - Camera viewfinder with:
        · Face guide oval (dashed border)
        · Corner alignment markers
        · "● Live" indicator (top right)
        · "Align face within the oval" hint (bottom)
    - "Capture & continue" primary button
    - "Back" secondary button → returns to Step 1
  On success: navigate to Step 3
  On failure: show inline error, allow retry (no attempt limit on registration)

Step 3 — Success
  Layout:
    - Step indicator (3 dots, all complete)
    - Success icon (checkmark)
    - "You're all set" heading
    - "Your device is registered. Face authentication is active." body text
    - "Start observing" primary button → navigates to main_feed
    - No back navigation from this screen
```

### 7.5 Auth Overlay (re-auth)

```
Rendered over current destination (not a nav destination)
Triggered by: auth_required = true (see §9.5)
Background: blurred + dimmed current screen content

Layout:
  - Face-id icon with ring + pulse animation (● Ready to scan)
  - "Verify your identity" heading
  - "Session expired after 24 hours. Look at the camera to unlock." body
  - "Scan face to unlock" primary button (only CTA)
  - Divider
  - Attempt counter: "X of 3 attempts remaining"
  - After ≥ 1 failed attempt only: "Having trouble? Sign in with Google instead"
    (low-prominence text link, NOT a button)

Dismiss: successful auth only — overlay cannot be manually dismissed
```

### 7.6 UI States

```
main_feed loading    → skeleton shimmer cards (no chip row or search bar interaction)
main_feed empty      → "No events captured yet" + hint to open the target app
main_feed no results → "No results for [query]" + clear query affordance
sync error           → non-blocking banner below top bar: "Sync failed — retrying in Xs"
offline              → non-blocking banner below top bar: "Offline — events stored locally"
auth locked          → auth overlay (§7.5) layered over current screen
```

---

## 8. ⚠️ Performance Safeguards

```
Max nodes per snapshot:   1000 (hard cap during traversal)
Max snapshot size:        200KB (post-serialization trim)
Max searchable_text:      5000 chars (hard truncate)
Event capture debounce:   200ms
Search debounce:          300ms
```

---

## 9. 🔐 Authentication

### 9.1 Auth Model Overview

```
Two layers — strict priority order:
1. Face Auth     → PRIMARY (presence + identity, on every login)
2. Google SSO    → AUTOMATIC FALLBACK ONLY (system-triggered, not user-chosen)

Design intent:
- Face auth is ALWAYS attempted first. Google SSO is never offered as an equal option.
- Google SSO surfaces automatically only when face auth cannot complete (see 9.4).
- The UI must not present Google SSO as a visible primary action — it appears only
  as a low-prominence fallback hint after face auth fails or is unavailable.

Auth gates UI access ONLY.
Event capture and sync run uninterrupted regardless of auth state.
```

### 9.2 Registration (one-time per user)

```
1. Google SSO → obtain google_id + email
2. Capture face frame → ML Kit extracts landmarks + euler angles
3. FaceProcessor → produces FaceData (normalized landmarks, euler angles)
4. Send FaceData to backend (raw image NEVER transmitted)
5. Backend → generates embedding → stores encrypted (AES-256)
6. Backend → returns { success: true, google_id } only
7. Device → stores google_id locally (EncryptedSharedPreferences)
8. Face template NEVER returned to device
```

### 9.3 Login (normal — face, PRIMARY)

```
1. On app open or re-auth trigger → launch face scan immediately (no choice screen)
2. Device captures face frame
3. FaceProcessor → produces FaceData
4. Send FaceData to backend
5. Backend → extracts landmarks → cosine similarity vs stored template
6. Passive liveness check (backend):
   - Landmark variance check (static photo = perfectly still)
   - Euler angle plausibility check (flat photo = no rotation)
7. If score ≥ 0.85 + liveness pass → { authenticated: true, google_id } → unlock UI
8. If score < 0.85 OR liveness fail → trigger automatic Google SSO fallback (see 9.4)
   DO NOT present Google SSO as a user-selectable option at this point
```

### 9.4 Login (automatic fallback — Google SSO)

```
Trigger conditions (system-initiated, never user-initiated):
- Cosine similarity < 0.85 after all face attempts exhausted (see 9.6)
- Two users similarity > 0.90 (flagged ambiguous pair)
- Network unavailable during face auth
- Camera hardware unavailable or permission denied

Fallback behaviour:
- Device silently attempts Google SSO cached token first
- If cached token valid → authenticate with google_id, unlock UI
- If cached token expired → launch Google SSO OAuth flow
- If both fail → queue re-auth, UI remains locked, capture continues

UI rule:
- Google SSO fallback MUST NOT be shown as a button or primary CTA
- It may appear ONLY as a low-prominence text hint, e.g.:
    "Having trouble? Sign in with Google instead"
  and only after ≥ 1 face auth attempt has already failed in the current session
```

### 9.5 Re-authentication (every 24 hours)

```
On auth expiry:
→ Set auth_required = true
→ Continue all background processes uninterrupted
→ Begin idle detection polling

Idle condition (both must be true):
→ Screen OFF for ≥ 5 minutes
→ No accessibility events captured for ≥ 5 minutes (any package)

On idle confirmed:
→ Queue re-auth for next screen ON event

Grace period:
→ If idle never detected within 2 hours → force re-auth on next screen ON

On screen ON with auth_required = true:
→ Show re-auth prompt (UI locked/blurred)
→ Capture and sync continue in background
→ On success → unlock UI, reset 24h timer
```

### 9.6 Auth Failure Handling

```
Face auth attempt budget per session: 3
After each failure → show attempt count ("2 of 3 attempts remaining")
After 3 failures → 30 second lockout, then trigger automatic Google SSO fallback (9.4)
After 5 total failures → force Google SSO OAuth flow (device credential)
Never delete local data on auth failure
Never interrupt capture or sync on failure

UI rules during failure:
- NEVER show Google SSO as a retry option between face attempts
- ONLY show Google SSO hint text after ≥ 1 failed attempt in current session
- Hint text: "Having trouble? Sign in with Google instead" (low-prominence, not a button)
```

### 9.7 Device Incompatibility

```
BiometricPrompt with BIOMETRIC_STRONG or DEVICE_CREDENTIAL
Ensures no device is blocked by hardware limitations
```

---

## 10. 🧠 Face Processing Architecture 🔄 EXTENSIBLE

### 10.1 Abstraction Layer

```kotlin
// Contract — never bypass this interface
interface FaceProcessor {
    fun extractFaceData(frame: Bitmap): FaceData?
}

// V1 — ML Kit bundled (free, on-device)
class MLKitFaceProcessor : FaceProcessor {
    override fun extractFaceData(frame: Bitmap): FaceData? { ... }
}

// V2 — MediaPipe or custom algorithm (drop-in replacement)
class CustomFaceProcessor : FaceProcessor {
    override fun extractFaceData(frame: Bitmap): FaceData? { ... }
}
```

### 10.2 FaceData Contract (version-stable)

```kotlin
data class FaceData(
    // Landmarks — 10 normalized points (V1), 478 points (V2 MediaPipe)
    val landmarks: List<NormalizedPoint>,

    // Passive liveness signals
    val headEulerAngleX: Float,       // up/down rotation
    val headEulerAngleY: Float,       // left/right rotation
    val headEulerAngleZ: Float,       // tilt rotation

    // Active liveness hooks — null in V1
    val livenessChallenge: ChallengeType? = null,
    val challengeResponse: ChallengeResponse? = null,

    // Metadata
    val frameTimestamp: Long,
    val imageWidth: Int,
    val imageHeight: Int
)

data class NormalizedPoint(val x: Float, val y: Float)

enum class ChallengeType {
    NONE,       // V1 — passive only
    BLINK,      // V2
    TURN_HEAD,  // V2
    SMILE       // V3
}
```

### 10.3 Library Roadmap

```
V1: ML Kit Face Detection (bundled)
    com.google.mlkit:face-detection:16.x.x
    On-device, no network, no cost
    10 landmark points

V2: MediaPipe Face Landmarker (open source, Google)
    Drop-in via FaceProcessor interface
    478 landmark points
    No changes outside FaceProcessor implementation

V3: Custom TFLite / OpenCV + dlib
    Full algorithm ownership
    Same interface contract
```

### 10.4 Payload to Backend

```
Sent:     FaceData as JSON (~1KB per request)
Never:    Raw image, face template, embedding
```

---

## 11. 🗄️ Neon PostgreSQL Schema

### 11.1 Tables

```sql
-- Users
CREATE TABLE users (
    id TEXT PRIMARY KEY,              -- UUID v7
    google_id TEXT UNIQUE NOT NULL,
    email TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now(),
    is_active BOOLEAN DEFAULT true
);

-- Devices
CREATE TABLE devices (
    id TEXT PRIMARY KEY,              -- UUID v7
    device_fingerprint TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- Device-User sessions (multi-driver per device)
CREATE TABLE device_user_sessions (
    id TEXT PRIMARY KEY,              -- UUID v7
    device_id TEXT REFERENCES devices(id),
    user_id TEXT REFERENCES users(id),
    started_at TIMESTAMPTZ DEFAULT now(),
    ended_at TIMESTAMPTZ,             -- null = currently active
    is_active BOOLEAN DEFAULT true
);

-- Face templates (history preserved, never exposed to device)
CREATE TABLE face_templates (
    id TEXT PRIMARY KEY,              -- UUID v7
    user_id TEXT REFERENCES users(id),
    embedding BYTEA NOT NULL,         -- AES-256 encrypted float vector
    is_active BOOLEAN DEFAULT true,   -- one active per user
    created_at TIMESTAMPTZ DEFAULT now(),
    superseded_at TIMESTAMPTZ,        -- set when replaced
    liveness_version TEXT DEFAULT 'v1_passive'  -- extensibility hook
);

-- Observed events (device-level, no user association)
CREATE TABLE observed_events (
    id TEXT PRIMARY KEY,              -- UUID v7, generated on device
    device_id TEXT REFERENCES devices(id),
    timestamp TIMESTAMPTZ NOT NULL,
    package_name TEXT NOT NULL,
    activity_name TEXT,
    event_type INTEGER NOT NULL,
    ui_snapshot_json JSONB NOT NULL,  -- queryable, compressed
    searchable_text TEXT,
    created_at TIMESTAMPTZ DEFAULT now()
);
```

### 11.2 Indexes

```sql
-- observed_events
CREATE INDEX idx_oe_device_timestamp
    ON observed_events(device_id, timestamp DESC);
CREATE INDEX idx_oe_fts
    ON observed_events USING gin(to_tsvector('english', searchable_text));

-- face_templates
CREATE INDEX idx_ft_user_active
    ON face_templates(user_id, is_active);

-- device_user_sessions
CREATE INDEX idx_dus_device_active
    ON device_user_sessions(device_id, is_active);
```

### 11.3 Remote Auth (V1 — Direct Neon)

```sql
-- Dedicated low-privilege role
CREATE ROLE app_device WITH LOGIN PASSWORD '<strong_password>';
GRANT INSERT, SELECT ON ALL TABLES IN SCHEMA public TO app_device;
-- NO UPDATE, NO DELETE, NO DROP
```

```
Connection string: stored in EncryptedSharedPreferences (never hardcoded)
TLS: enforced by Neon default
```

### 11.4 Remote Auth (V2 — API Layer)

```
Swap NeonDirectDataSource → ApiDataSource
Auth: JWT or service token replaces DB credentials on device
No schema changes required
```

---

## 12. 🚨 Failure Handling

```
Null root node    → skip event entirely
DB write fail     → retry once, then discard
Sync fail         → exponential backoff (2s → 60s), retry later
Face auth fail    → see Section 9.6
Network unavail   → face auth fallback to Google SSO cached token
Snapshot > 200KB  → trim deepest nodes until compliant
```

---

## 13. 🔒 Security Summary

```
Raw face image:        NEVER transmitted
Face template/embed:   NEVER returned to device, AES-256 at rest
Connection string:     EncryptedSharedPreferences only
Neon role:             INSERT + SELECT only, no destructive ops
TLS:                   Enforced on all Neon connections
Event data:            Device-level only, no PII association
Auth scope:            UI access only — capture/sync always run
```

---

## 14. 📋 Decisions Log

| # | Decision | Rationale |
|---|---|---|
| 1 | UUID v7 for all IDs | Time-ordered = fast B-tree insert + access, safe for distributed sync |
| 2 | Event capture debounce 200ms | Prevents flooding from rapid accessibility event bursts |
| 3 | Search debounce 300ms | Standard UX typing debounce |
| 4 | activityName from event.className (STATE_CHANGED) or rootNode.className (CONTENT_CHANGED) | Most reliable extraction per event type |
| 5 | Size enforcement priority: nodes → snapshot → searchable_text | Ensures no constraint conflict |
| 6 | FTS5 (SQLite) + GIN (Neon) replacing LIKE | Leading wildcard LIKE doesn't use indexes |
| 7 | Auth gates UI only, capture/sync uninterrupted | Driver data continuity during re-auth |
| 8 | Re-auth on idle detection, not immediate expiry | Avoids interrupting active driver sessions |
| 9 | Face auth PRIMARY, Google SSO automatic fallback only | Face = faster, more secure, shift-appropriate. SSO parity would reduce face adoption and weaken presence guarantees. Google SSO visible only as low-prominence hint after ≥ 1 failed face attempt. |
| 10 | Face template history preserved | Audit trail, recovery from re-registration |
| 11 | FaceProcessor abstraction layer | ML Kit → MediaPipe → custom, zero downstream changes |
| 12 | ML Kit bundled (free) for V1 | On-device, no cost, extensible via interface |
| 13 | Device sends FaceData not raw image | Privacy + bandwidth, ~1KB vs ~500KB |
| 14 | Passive liveness V1, active hooks defined | Extensible without redesign |
| 15 | One device multiple drivers via device_user_sessions | Shift-based driver workflow |
| 16 | Events are device-level, no user association | Simplifies schema, no PII linkage |
| 17 | RemoteDataSource abstraction | Direct Neon → API layer swap with no schema changes |
| 18 | JSONB for ui_snapshot_json in Neon | Queryable + compressed vs plain TEXT |
| 19 | JSON viewer as full-screen modal | uiSnapshotJson can reach 200KB — inline expansion would break list layout and overflow scroll containers |
| 20 | Google SSO shown as text hint, not button | Button-level parity would undermine face-first design intent and make SSO the path of least resistance |
| 21 | Denylist (not allowlist) for package filtering at capture layer | Tool purpose is inspection across apps — allowlist to a single package prevents discovering which events an unknown app emits. Denylist removes only irreducible system noise (keyboard, launcher, OS framework) that is never of inspection interest. |
| 22 | Package filter as UI chip, not capture config | Filtering by package is a viewing concern, not a data concern. The developer may want to switch between apps mid-session without losing prior events. Capture-layer filtering would require restart; UI-layer filtering is instant and reversible. |
| 23 | Local retention window anchored to MAX(timestamp), not current clock | If the device is offline for days, anchoring to `now` would delete the only data the developer has. Anchoring to the latest event guarantees a full browsable day is always present regardless of calendar time. |
| 24 | Retention cleanup piggybacks on successful sync, not a separate job | Avoids a second scheduled worker. More importantly, ties deletion to confirmed Neon landing — data is never removed from local DB until it is safe on the server. |
| 25 | Unsynced rows are never deleted | Prevents data loss on prolonged offline periods. The device is the source of truth until Neon confirms receipt. |
| 26 | Single Activity with NavHost (no fragments) | Follows Jetpack Compose best practice. Single Activity eliminates fragment lifecycle complexity; NavHost manages the back stack declaratively. |
| 27 | Auth overlay is not a NavHost destination | Re-auth must not clear or replace the current screen. Layering a composable overlay preserves back stack state and allows instant dismiss on success without a navigation event. |
| 28 | Registration back navigation disabled | Partial registration (google_id without face template, or vice versa) leaves the backend in an inconsistent state. Force-completion prevents this. |
| 29 | Two-level filter model: package chips (OR) + scoped text (AND within package) + top-level text chips (OR, AND'd across packages) | Supports real-world comparison use case — e.g. observe Gojek events containing "tarif" alongside InDriver events containing "fare", further narrowed by a top-level term. Single-select chip + single search bar cannot express this. |
| 30 | Google SSO first in registration, face capture second | Identity (google_id) must exist on the backend before a face template can be associated to it. Reversing the order would require a temporary anonymous record. |
