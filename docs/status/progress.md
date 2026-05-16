# Project Progress

Refactor note added: 2026-05-16 13:52:51 +07:00

The documentation set has been reorganized into product, change-request, architecture, UI, implementation, status, and archive sections. Original progress content is preserved below.

---

# Project Progress

Last updated: 2026-05-13 13:49:37 +07:00

## Task Log

### 2026-05-14 21:05:21 +07:00

- Branch: `codex`
- Commit: `7ae11b9`
- Commit status: changes are currently uncommitted working-tree changes on top of this commit
- Task: implement CR#1 UI-first Sessions screen and 4-tab bottom navigation
- End: 2026-05-14 21:09:23 +07:00
- Done:
  - Added a new top-level Session destination and made it the app start tab.
  - Rebuilt the bottom navigation as a custom 4-tab Compose bar in the order Session, List, Track, Settings.
  - Created a UI-only Sessions screen that toggles between the handoff Idle and Recording visual states with local mock state.
  - Added the Recording hero, OFF/ON switch, empty state copy, sample active/history session rows, and pulse accents.
  - Added a Session icon vector and a Session string resource.
  - Updated TrackLocation theme colors to match the CR#1 handoff tokens.
- Verification:
  - Ran `git diff --check`; it reported only existing line-ending warnings and no whitespace errors.
  - Did not run Gradle build, tests, or emulator per project instruction requiring permission first.

### 2026-05-13 13:49:37 +07:00

- Branch: `codex`
- Commit: `af23652`
- Commit status: changes are currently uncommitted working-tree changes on top of this commit
- Task: documented project progress in `docs/progress.md`
- Done:
  - Created a compact project status file for future handoff/reference.
  - Recorded current project status, recent SDK 34 and Material3 migration work, verification results, latest event, known remaining work, next steps, and improvement suggestions.
  - Added this dated task log section so future project changes can be tracked with date/time, branch, and commit.

### 2026-05-13 13:52:00 +07:00

- Branch: `codex`
- Commit: `af23652`
- Commit status: changes are currently uncommitted working-tree changes on top of this commit
- Task: added project working rule for commits
- Done:
  - Added a standing rule: after changes are made to the project folder, remind the user to make a git commit before starting another task.

## Project Working Rules

- After making changes to files in this project folder, remind the user to make a git commit before starting another task.
- Include the current branch and commit in future progress/task log updates when possible.

### 2026-05-13 earlier session

- Branch: `codex`
- Commit: `af23652`
- Commit status: changes are currently uncommitted working-tree changes on top of this commit
- Task: fix SDK 34 and Material3 migration errors
- Done:
  - Upgraded Gradle wrapper from `7.5` to `8.7`.
  - Upgraded Android Gradle Plugin from `7.4.2` to `8.5.2`.
  - Upgraded Kotlin Gradle plugin from `1.7.0` to `1.9.24`.
  - Upgraded Compose compiler from `1.2.0` to `1.5.14`.
  - Switched Compose dependencies to the Compose BOM.
  - Updated Java/Kotlin target from `1.8` to `17`.
  - Migrated remaining Material2 theme/component API usage to Material3.
  - Updated Accompanist permissions API usage.
  - Added `android.permission.FOREGROUND_SERVICE_LOCATION` for SDK 34 foreground location service compatibility.
  - Changed tracking service startup to `ContextCompat.startForegroundService(...)`.
- Verification:
  - `:app:compileDebugKotlin` passed.
  - `:app:assembleDebug` was attempted but timed out after 6 minutes on this machine.

## Current Project Status

This Android project is a Jetpack Compose location tracking app. It uses:

- Compose UI with Material3
- Google Maps Compose
- Fused Location Provider
- Room database
- A foreground `TrackingService` for location tracking
- Bottom navigation for list, tracking, and settings screens

The recent work focused on upgrading the project from SDK 33 to SDK 34 and migrating from Material2 to Material3.

## What Was Done

The build/toolchain was upgraded:

- Gradle wrapper: `7.5` -> `8.7`
- Android Gradle Plugin: `7.4.2` -> `8.5.2`
- Kotlin Gradle plugin: `1.7.0` -> `1.9.24`
- Compose compiler: `1.2.0` -> `1.5.14`
- Java/Kotlin target: `1.8` -> `17`
- Compose dependencies now use the Compose BOM
- Room was upgraded to `2.6.1`
- Accompanist permissions/system UI versions were upgraded

Material3 migration was completed for the remaining compile blockers:

- Theme moved from Material2 `colors`, `darkColors`, and `lightColors` to Material3 `colorScheme`, `darkColorScheme`, and `lightColorScheme`
- Typography moved from Material2 names such as `body1` and `h6` to Material3 names such as `bodyLarge` and `titleLarge`
- Shapes moved to Material3 `Shapes`
- Remaining Material2 `Card`, `Text`, and `CircularProgressIndicator` usages were replaced with Material3 equivalents
- `TrackItemRow` now uses Material3 `CardDefaults`

SDK 34 foreground service compatibility was addressed:

- Added `android.permission.FOREGROUND_SERVICE_LOCATION`
- Changed tracking service start from `context.startService(...)` to `ContextCompat.startForegroundService(...)`

Accompanist permissions API changes were fixed:

- Replaced old `hasPermission` and `shouldShowRationale` access with `permission.status.isGranted` and `permission.status.shouldShowRationale`

## Verification Done

The emulator and physical device were not used because the machine is slow.

Successful verification:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
.\gradlew.bat :app:compileDebugKotlin --no-daemon --max-workers=1
```

Result:

- `:app:kaptDebugKotlin` passed
- `:app:compileDebugKotlin` passed
- Kotlin source now compiles after the SDK 34 and Material3 migration fixes

The earlier KAPT blocker was fixed. The original error was:

```text
IllegalAccessError: KaptJavaCompiler cannot access com.sun.tools.javac.main.JavaCompiler
```

That was caused by the old Kotlin/KAPT toolchain running on a modern JDK.

## Last Thing That Happened

`compileDebugKotlin` passed successfully.

Then `assembleDebug` was attempted:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1
```

It timed out after 6 minutes on this machine. No concrete build failure report was produced. The Gradle daemon was stopped afterward to avoid leaving CPU-heavy build processes running.

## Known Remaining Items

- Run `:app:assembleDebug` again when the machine can tolerate a longer build.
- Replace deprecated `Icons.Default.ArrowBack` with the AutoMirrored icon.
- Replace deprecated `accompanist-systemuicontroller` later with AndroidX edge-to-edge APIs.
- Clean up existing unused parameters in some composables.
- No runtime behavior was verified on emulator or physical device.

## What Should Be Done Next

Recommended next steps, in order:

1. Re-run full debug assembly when the machine has enough time:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
.\gradlew.bat :app:assembleDebug --no-daemon --max-workers=1
```

2. If `assembleDebug` passes, run unit tests:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
.\gradlew.bat :app:testDebugUnitTest --no-daemon --max-workers=1
```

3. Fix the remaining compile warnings:

- In `TopBar.kt`, replace deprecated `Icons.Default.ArrowBack` with `Icons.AutoMirrored.Filled.ArrowBack`.
- Remove or use unused `modifier` parameters in `DetailsScreen.kt` and `TrackMap.kt`.
- Remove or implement the unused `text` parameter in `CustomAlertDialog.kt`.

4. Replace deprecated Accompanist system UI handling:

- Current warning says `accompanist-systemuicontroller` is deprecated.
- Suggested direction: use AndroidX edge-to-edge APIs from Activity instead.
- This is not a compile blocker, but it is worth doing before polishing the SDK 34 migration.

5. Later, when device/emulator verification is acceptable, test runtime behavior:

- First launch permission request flow.
- Notification permission on Android 13+.
- Foreground location tracking start/stop.
- Notification appears while tracking.
- Track saving to Room after stop.
- List screen displays saved tracks.
- Detail map displays saved path points.

## Improvement Suggestions

Short-term improvements:

- Add a stable Gradle/JDK note to project setup docs so future command-line builds use `C:\Users\rinal\.jdks\jbr-17.0.14`.
- Add a small unit test around `LocationUtils.pathPointsToString` and `LocationUtils.stringToPathPoints`.
- Add tests for `TimeUtilFormatter.getTime` because duration formatting is easy to break.
- Consider moving permission UI state out of composable-local dialog state so permission dialogs behave more predictably after recomposition.

Medium-term improvements:

- Replace `Timer` in `TrackingService` with coroutine-based timing for cleaner lifecycle control.
- Review Android 14 foreground service behavior carefully before publishing.
- Consider migrating Room from `kapt` to `ksp` later to reduce build overhead.
- Centralize dependency versions using a Gradle version catalog.
- Add a lightweight CI command that runs `:app:compileDebugKotlin` and unit tests only, avoiding emulator/device work.

Longer-term improvements:

- Separate tracking/domain logic from Compose screens to make the app easier to test.
- Add a repository layer between `ShareViewModel` and Room.
- Add UI state models for list, details, and tracking screens.
- Consider handling process death and service recovery for active tracking sessions.

## Important Local Notes

- Use this JDK path for command-line Gradle on this machine:

```powershell
$env:JAVA_HOME='C:\Users\rinal\.jdks\jbr-17.0.14'
```

- The Android Studio JBR at `C:\Program Files\Android\Android Studio\jbr` was incomplete and failed with missing `lib\jvm.cfg`.
- Another Android Studio JBR existed at `C:\Program Files\Android\Android Studio1\jbr`, but the user-local JDK path above was more reliable.

## Session: CR Documentation for Always-recorded Location Sessions

Start: 2026-05-14 20:18:18 +07:00

End: 2026-05-14 20:19:14 +07:00

### Changes

- Created `docs/crs_plan.md` to record CR#1 implementation intent and behavior decisions separately from rollout phases.
- Created `docs/crs_screen_specification.md` to record CR#1 UI and interaction requirements separately from implementation planning.
- Documented CR#1 as `Always-recorded location sessions`.

### Verification

- Documentation-only change. No build or emulator run.
