# ADR-021: Debug-only Diagnostic Screen Recorder

**Status:** Accepted
**Date:** 2026-10-07
**Decided By:** Project owner

## Context

During development and debugging, it is valuable to capture video evidence of UI behavior anomalies (e.g., a wrong cost value, incorrect camera orientation, stale text). The developer needs a way to record the screen automatically without manual intervention, then extract clips to analyze frame-by-frame. Existing fallback methods exist (adb screenrecord, Android Quick Settings screen recording), but a built-in app mechanism offers convenience and automatic lifecycle management.

The challenge: Android app screen recording requires `MediaProjection` (the system consent dialog cannot be bypassed), but the payoff is seamless integration and automatic start/stop tied to the app's foreground state. This ADR documents a debug-only implementation that respects debug/release build separation and storage isolation.

## Decision

1. **Debug-only scope:** All code lives in `app/src/debug/` source set (manifest overlays, service, activity hooks). Release builds (`assembleRelease`, `bundleRelease`) contain no recorder classes, service declarations, or permissions. No product feature, no new UI destination, no impact on PRD/UI-SPEC (respects AGENTS.md §2 product-boundary rule).

2. **Consent and activation:** The system `MediaProjection` consent dialog appears once per app launch in debug builds. On first app launch, the user sees: "TrackLocation wants to record your screen." Tapping "Allow" starts automatic recording; tapping "Deny" skips recording for that session. Recording does not restart after the consent-deny action. The user's choice persists only for this session (dialog re-appears on next app launch).

3. **Foreground-only lifecycle:** Recording is active only while TrackLocation is in the foreground (app `ON_RESUME` state). Recording stops when the app moves to background (`ON_PAUSE`); recording resumes with a fresh consent dialog when the app returns to foreground. This prevents runaway recording and respects the spirit of the MediaProjection consent model.

4. **Rolling segment storage:** Video is recorded in 2-minute MP4 segments. The app keeps the most recent 5 segments (~10 minutes of video). On segment rollover, the oldest segment is deleted automatically. Segments are stored in the MediaStore under a `Movies/TrackLocation-Diagnostics/` directory. Files are created with `IS_PENDING = true` during write (hidden from the gallery until finalized), then `IS_PENDING = false` when closed (visible). This allows clips to appear in the standard Photos/Gallery app as an album.

5. **Foreground service:** A MediaProjection foreground service (debug manifest only) posts a persistent notification with a "Stop & Keep" action. Tapping this action freezes the current segment set (stops rotation and deletion), allowing the developer to safely examine clips without them disappearing. The notification is required by Android 12+ for foreground services; the "Stop & Keep" action is a convenience feature.

6. **Manual clip review workflow:** Since the AI assistant cannot play video, extracted frames are used for review. On the dev machine:
   - Pull a clip from the device: `adb pull /sdcard/Movies/TrackLocation-Diagnostics/<file>.mp4`
   - Extract frames with ffmpeg (user installs `sudo apt install -y ffmpeg`): `ffmpeg -i clip.mp4 -vf fps=1 frame_%03d.png`
   - Read the PNG frames using the AI assistant's vision capability
   - Describe the problem: "At frame 120, the COST cell shows 'Rp8.4k' but OBD speed was 0; expected '—' per the spec."

7. **Storage isolation and MediaStore API:** Clips are stored via `MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)` with `RELATIVE_PATH = "Movies/TrackLocation-Diagnostics/"`. The app creates and manages files it owns. On API 29+, no storage permission is required (scoped storage). On API 28 (minSdk), `WRITE_EXTERNAL_STORAGE` is declared in the debug manifest only with `maxSdkVersion="28"` so it is not inherited by API 29+ (avoiding permission prompts). Files created by the app are deleted by the app upon segment rotation (legal without permission on API 29+).

8. **Consequences and risks:**
   - **(a) Cloud backup:** Google Photos, Dropbox, or other backup services may automatically upload clips from the TrackLocation-Diagnostics album, leaking location data, trip details, and cost/earnings information. Mitigation: the developer should **disable cloud backup for the album**. The app cannot prevent the backup; it is a user-device setting.
   - **(b) Segment stale windows:** If the developer forgets to tap "Stop & Keep" and the bug appears near the 10-minute boundary, the clip may rotate out before analysis. Mitigation: the notification offers the action; the developer should tap it when a bug is spotted.
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
- **Segment rollover:** 5 segments (10 min) auto-rotate; developer can "Stop & Keep" to freeze when needed.
- **Zero-permission release builds:** Release builds contain no recorder code or permissions (clean APK, no security surface).
- **Video + frame extraction workflow:** ffmpeg + image reading on the dev machine allows pixel-level analysis without in-app playback.

### Negative / Accepted Risks
- **Cloud backup leakage:** User must manually disable backup for the album to prevent data exfiltration (no in-app control).
- **Segment rotation window:** Developer must tap "Stop & Keep" before the 10-minute window closes; otherwise, the clip may be deleted.
- **Battery/CPU drain:** MediaRecorder continuously encodes; impacts test timing and battery (debug-only, acceptable).
- **Post-uninstall orphans:** Old clips remain in MediaStore after uninstall; manual cleanup required.
- **Consent re-prompt per launch:** User sees the system dialog again on the next app launch (this is Android's design; cannot bypass).

## Alternatives Considered

1. **adb screenrecord (no-code fallback):** `adb shell screenrecord /sdcard/video.mp4`. Requires manual dev-machine command per session; not automatic. Kept as a fallback if in-app recorder is unavailable or broken.

2. **Android Quick Settings screen recording:** User toggles "Record screen" in Quick Settings; auto-stops per timer or manual toggle. Requires user action per session; no app-side code. Kept as a fallback.

3. **In-app floating REC button:** Visible debug-only FAB to start/stop recording. Rejected: adds visual noise, violates "no new UI destination" rule (AGENTS.md §2), increases cognitive load.

4. **App-private `Android/data/` storage:** Clips stored in `app-specific external files directory` are not visible in the Gallery. Rejected: defeats the purpose (developer wants quick Gallery access to inspect clips).

5. **Persistent recording (no lifecycle ties):** Recording continues even when the app is backgrounded. Rejected: wastes battery, may not be legal without persistent notification (already added for MediaProjection service), increases risk of unintended data capture.

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
