# ADR-021: Debug-only Diagnostic Screen Recorder

**Status:** Accepted
**Date:** 2026-10-07
**Decided By:** Project owner

## Context

During development and debugging, it is valuable to capture video evidence of UI behavior anomalies (e.g., a wrong cost value, incorrect camera orientation, stale text). The developer needs a way to record the screen automatically without manual intervention, then extract clips to analyze frame-by-frame. Existing fallback methods exist (adb screenrecord, Android Quick Settings screen recording), but a built-in app mechanism offers convenience and automatic lifecycle management.

The challenge: Android app screen recording requires `MediaProjection` (the system consent dialog cannot be bypassed), but the payoff is seamless integration and automatic start/stop tied to the app's foreground state. This ADR documents a debug-only implementation that respects debug/release build separation and storage isolation.

## Decision

1. **Debug-only scope:** All code lives in `app/src/debug/` source set (manifest overlays, service, activity hooks). Release builds (`assembleRelease`, `bundleRelease`) contain no recorder classes, service declarations, or permissions. No product feature, no new UI destination, no impact on PRD/UI-SPEC (respects AGENTS.md §2 product-boundary rule).

2. **Consent and activation:** The system `MediaProjection` consent dialog appears once per app process in debug builds (see Amendment 2026-10-07: not on every foreground return). On first launch, the user sees: "TrackLocation wants to record your screen." Tapping "Allow" starts automatic recording; tapping "Deny" skips recording until the app has been backgrounded and returns. The user's choice persists only for the process lifetime (dialog re-appears after a process restart).

3. **Foreground-only lifecycle:** Recording is active only while TrackLocation is in the foreground. When the app moves to background the recorder is **paused** (projection and service stay alive); when it returns the recorder **resumes** with no dialog (see Amendment 2026-10-07). This prevents runaway recording while avoiding repeated consent prompts. *(Superseded in part — see Amendment 2026-10-08: opt-in background capture, OFF by default.)*

4. **Rolling segment storage:** Video is recorded in 2-minute MP4 segments. The app keeps the most recent 5 segments (~10 minutes of video). On segment rollover, the oldest segment is deleted automatically. Segments are stored in the MediaStore under a `Movies/TrackLocation-Diagnostics/` directory. Files are created with `IS_PENDING = true` during write (hidden from the gallery until finalized), then `IS_PENDING = false` when closed (visible). This allows clips to appear in the standard Photos/Gallery app as an album. *(Superseded in part — see Amendment 2026-10-08: segments are 10 minutes and the most recent 6 are kept.)*

5. **Foreground service:** A MediaProjection foreground service (debug manifest only) posts a persistent notification with a "Stop & Keep" action. Tapping this action freezes the current segment set (stops rotation and deletion), allowing the developer to safely examine clips without them disappearing. The notification is required by Android 12+ for foreground services; the "Stop & Keep" action is a convenience feature.

6. **Manual clip review workflow:** Since the AI assistant cannot play video, extracted frames are used for review. On the dev machine:
   - Pull a clip from the device: `adb pull /sdcard/Movies/TrackLocation-Diagnostics/<file>.mp4`
   - Extract frames with ffmpeg (user installs `sudo apt install -y ffmpeg`): `ffmpeg -i clip.mp4 -vf fps=1 frame_%03d.png`
   - Read the PNG frames using the AI assistant's vision capability
   - Describe the problem: "At frame 120, the COST cell shows 'Rp8.4k' but OBD speed was 0; expected '—' per the spec."

7. **Storage isolation and MediaStore API:** Clips are stored via `MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)` with `RELATIVE_PATH = "Movies/TrackLocation-Diagnostics/"`. The app creates and manages files it owns. On API 29+, no storage permission is required (scoped storage). On API 28 (minSdk), `WRITE_EXTERNAL_STORAGE` is declared in the debug manifest only with `maxSdkVersion="28"` so it is not inherited by API 29+ (avoiding permission prompts). Files created by the app are deleted by the app upon segment rotation (legal without permission on API 29+).

8. **Consequences and risks:**
   - **(a) Cloud backup:** Google Photos, Dropbox, or other backup services may automatically upload clips from the TrackLocation-Diagnostics album, leaking location data, trip details, and cost/earnings information. Mitigation: the developer should **disable cloud backup for the album**. The app cannot prevent the backup; it is a user-device setting.
   - **(b) Segment stale windows:** If the developer forgets to tap "Stop & Keep" and the bug appears near the 10-minute boundary, the clip may rotate out before analysis. Mitigation: the notification offers the action; the developer should tap it when a bug is spotted. *(Superseded in part — see Amendment 2026-10-08: the window is 60 minutes, not 10.)*
   - **(c) Battery and CPU cost:** Continuous MediaRecorder codec activity drains battery and uses CPU, which may alter timing and jank behavior under test. This is acceptable for debug-only local verification; the penalty does not apply to release builds.
   - **(d) Post-uninstall orphans:** After uninstall and reinstall, old clips in MediaStore are no longer owned by the app (URI authority changes if the app is reinstalled with a different build ID or on a different device). The old files are not rotated or deleted; manual deletion via Photos/Gallery is required.

9. **No alternatives in scope:**
   - adb screenrecord (fallback): works without code, still requires explicit dev-machine command, cannot be automatic; kept as a fallback.
   - Android Quick Settings screen recording (fallback): user must toggle in Quick Settings per-session, no code; kept as a fallback.
   - In-app floating REC button (rejected): violates the "debug-only, no new UI" rule; visible to the developer and adds visual noise.
   - App-private storage under `Android/data/` (rejected): clips stored there are not visible in the standard Gallery/Photos app, defeating the purpose (developer wants to inspect clips quickly).

## Consequences

### Positive
- **Automatic capture:** No manual button tap or adb command needed; recording is tied to the app's foreground lifecycle.
- **Standard Gallery integration:** Clips appear in the Photos/Gallery app as a TrackLocation-Diagnostics album; no need for a file manager.
- **Segment rollover:** 5 segments (10 min) auto-rotate; developer can "Stop & Keep" to freeze when needed. *(Superseded in part — see Amendment 2026-10-08: 6 segments of 10 min, ~60 min.)*
- **Zero-permission release builds:** Release builds contain no recorder code or permissions (clean APK, no security surface).
- **Video + frame extraction workflow:** ffmpeg + image reading on the dev machine allows pixel-level analysis without in-app playback.

### Negative / Accepted Risks
- **Cloud backup leakage:** User must manually disable backup for the album to prevent data exfiltration (no in-app control).
- **Segment rotation window:** Developer must tap "Stop & Keep" before the 10-minute window closes; otherwise, the clip may be deleted. *(Superseded in part — see Amendment 2026-10-08: the window is 60 minutes.)*
- **Battery/CPU drain:** MediaRecorder continuously encodes; impacts test timing and battery (debug-only, acceptable).
- **Post-uninstall orphans:** Old clips remain in MediaStore after uninstall; manual cleanup required.
- **Consent re-prompt per process:** User sees the system dialog again only when there is no live session (process restart, system-stopped projection, or "Stop & keep"); this is Android's design and cannot be bypassed.

## Alternatives Considered

1. **adb screenrecord (no-code fallback):** `adb shell screenrecord /sdcard/video.mp4`. Requires manual dev-machine command per session; not automatic. Kept as a fallback if in-app recorder is unavailable or broken.

2. **Android Quick Settings screen recording:** User toggles "Record screen" in Quick Settings; auto-stops per timer or manual toggle. Requires user action per session; no app-side code. Kept as a fallback.

3. **In-app floating REC button:** Visible debug-only FAB to start/stop recording. Rejected: adds visual noise, violates "no new UI destination" rule (AGENTS.md §2), increases cognitive load.

4. **App-private `Android/data/` storage:** Clips stored in `app-specific external files directory` are not visible in the Gallery. Rejected: defeats the purpose (developer wants quick Gallery access to inspect clips).

5. **Persistent recording (no lifecycle ties):** Recording continues even when the app is backgrounded. Rejected: wastes battery, may not be legal without persistent notification (already added for MediaProjection service), increases risk of unintended data capture. *(Superseded in part for an opt-in mode — see Amendment 2026-10-08: background capture is OFF by default and is a debug-only toggle.)*

## Related ADRs

None. This is a debug-only feature with no domain-model or product impact.

## References

- Android MediaProjection API: `android.media.projection.MediaProjection`, `MediaProjectionManager`, `MediaRecorder.setNextOutputFile()`.
- Android MediaStore: `MediaStore.Video.Media`, `IS_PENDING`, `RELATIVE_PATH` (API 29+), scoped storage.
- Android Foreground Service types: `mediaProjection` (requires manifest declaration, notification, and persistent foreground service; available on API 31+; on API 30 and below, general foreground service type used as fallback).
- Storage permissions: `WRITE_EXTERNAL_STORAGE` (API 28 and below), `maxSdkVersion` attribute, scoped storage (API 29+).
- ffmpeg: command-line video processing tool; user installs `sudo apt install -y ffmpeg` on dev machine.
- AGENTS.md §2 (product boundaries — debug feature does not violate scope).
- AGENTS.md §5a (build/device verification requires explicit user permission; applies to code implementation, not docs).

## Implementation note (2026-10-07)

Built entirely in `app/src/debug/`, with no `app/src/main` edits. Deviations from the text above:

- **Hook:** a debug-manifest `ContentProvider` registers `ActivityLifecycleCallbacks` (no `MainActivity` change). A transparent consent activity is launched once per foreground session; denial is not re-asked until the app has been backgrounded (1.5 s debounce so rotation does not count).
- **Rollover:** not `setNextOutputFile()` (it triggers on file size, not duration). On `MAX_DURATION_REACHED` a new `MediaRecorder` and MediaStore entry are created, the `VirtualDisplay` is re-pointed with `setSurface()`, then the old segment is stopped and finalized (sub-second gap possible).
- **"Stop & keep":** finalizes the current segment, stops recording and sets a process-lifetime freeze flag (no deletions until the app process restarts); a separate notification reports the kept state.
- **API 28:** files go to `Movies/TrackLocation-Diagnostics` via `File` + `MediaScannerConnection`; `WRITE_EXTERNAL_STORAGE` must be granted manually (no runtime request); without it the recorder posts a notification and does not record.
- Segments left `IS_PENDING` by a process kill are not cleaned up on the next start.
- Superseded in part by the Amendment below: the controller no longer stops the service on background (it pauses it) and consent is asked once per process, not once per foreground session.

## Amendment (2026-10-07) — consent once per process

**Decision (owner-approved):** The consent dialog must not appear on every return to the foreground. Ask once per app process; keep the `MediaProjection`, `VirtualDisplay` and foreground service alive when the app is backgrounded and **pause** the `MediaRecorder` (`pause()`, API 24+) instead of stopping it; on return **resume** (`resume()`) with no dialog. The dialog appears again only when there is no live session: app process restarted/killed, the system stopped the projection, or the user tapped "Stop & keep". After "Stop & keep", a denial or a system stop, the app does not re-prompt until it has been backgrounded and returns.

**Why:** Android cannot persist MediaProjection consent, and reusing the consent result token is forbidden on API 34+ (one `getMediaProjection` per token). Keeping the one projection alive is the only way to avoid repeated prompts.

**How (debug source set only):** the lifecycle controller (1.5 s background debounce, unchanged) calls the live service instance directly (`pause()` / `resume()`; a backgrounded app may not start services). "Session live" is the service's static instance, set once a projection is held and cleared in `onDestroy`; if no instance exists on return, the consent flow runs. Pause/resume/rollover all run on the main thread and so serialize. `pause()`/`resume()` failures (`IllegalStateException`) fall back to finalizing the segment and opening a new one. Segment length counts recording time only: a handler timer armed for the remaining recording time (stopped while paused) backs up `setMaxDuration`, whose treatment of paused time is undocumented; at worst segments are shorter, never longer than 2 min. A paused segment is resumed momentarily before `stop()` when a session ends, so it is finalized as a normal playable file; empty/failed clips are deleted as before.

**Consequences:** a persistent "Paused - app in background" notification while the app is backgrounded; nothing is recorded while backgrounded; the dialog still appears on process restart, system stop or after "Stop & keep". On-device behavior is unverified at the time of writing.

## Amendment 2026-10-08 — opt-in background capture and 6 x 10-minute retention

**Status:** Accepted (debug-only). **Decided by:** Project owner. No PRD, UI-SPEC or product impact: the change stays in `app/src/debug/`, and release builds still contain no recorder code. Code is not yet implemented; device verification is pending (AGENTS.md §5a/§5b).

**Context:** The recorder must capture the Gojek Partner / GoSend order screens so their Accessibility events can be identified. Under the foreground-only lifecycle (§3 and the 2026-10-07 amendment), the recorder pauses as soon as TrackLocation is backgrounded, so it cannot see those screens. Retaining only ~10 minutes of video also leaves too short a window to stop the recording after a bug is spotted.

**Decision:**

1. **Opt-in background capture (supersedes §3 and the rejected alternative #5, for this opt-in mode only):** Add a background-capture mode that is **OFF by default**. The setting is persisted in the debug SharedPreferences and toggled by a second action on the recorder's persistent notification, labelled "Background: OFF/ON".
   - **ON:** the recorder does **not** pause when TrackLocation goes to background. It keeps capturing whatever app is in front.
   - **OFF:** behaviour is unchanged from the 2026-10-07 amendment (pause on background, resume on return with no dialog).
   - MediaProjection consent is still requested once per app process, and the persistent notification still offers "Stop & Keep".
2. **Retention (supersedes §4):** Segments are **10 minutes** (was 2) and the most recent **6** are kept (was 5), giving about **60 minutes** of rolling video (was about 10).
3. The other mechanics (MediaStore layout, `IS_PENDING` handling, "Stop & Keep" freeze behaviour) are unchanged.

**Consequences:**

- **Positive:** The developer can capture other apps' screens, such as Gojek Partner order screens, and can review about an hour of history after a bug.
- **Negative / Accepted Risks (new):**
  - **(a) Privacy:** Clips in background-capture mode may contain passenger names, addresses and chat messages from other apps. The cloud-backup warning in §8(a) applies more strongly. The developer must **disable cloud backup for the `Movies/TrackLocation-Diagnostics` album**; the app cannot enforce this.
  - **(b) Secure windows:** Apps that set `FLAG_SECURE` render as black frames in the clip. This is **unverified for Gojek Partner**; the capture may be blank for that app.
  - **(c) Process kill:** A process kill loses the in-progress segment (up to 10 minutes). It is left `IS_PENDING` and is not cleaned up on the next start (consistent with the 2026-10-07 implementation note).
  - **(d) Storage and battery:** Sixty minutes of video uses more storage and more battery and CPU than about ten minutes. This is accepted for debug-only use.
  - **(e) Stale window:** "Stop & Keep" must now be tapped within **60 minutes** (was 10) of the moment to be analysed.
- **Unchanged:** Consent is asked once per process; release builds have no recorder classes or permissions; no schema or Room migration; no change to GPS tracking, sessions or trips.
