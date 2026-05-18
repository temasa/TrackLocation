# AGENTS.md — TrackLocation AI Coding Rules

## Purpose

This file defines stable instructions for AI coding assistants working on the TrackLocation Android project.

Use this file for long-lived project rules. Put the current task, change request, or implementation goal in the prompt when starting Codex, Claude Code, Cursor, Windsurf, or another coding agent.

## Project Context

TrackLocation is an Android Jetpack Compose app evolving from a GPS trip tracker into a broader driver utility with:

- GPS trip tracking
- Always-recorded location sessions
- Accessibility Event Observer
- Local-first storage
- Sync-ready architecture
- Future registration and face-first authentication

The existing GPS tracking feature must remain intact unless the active task or accepted change request explicitly changes it.

## Active Documentation Source of Truth

Use the simplified active docs:

1. `README.md`
2. `docs/product-spec.md`
3. `docs/change-requests.md`
4. `docs/implementation-plan.md`
5. `docs/progress.md`

Archived detailed docs live under:

```text
docs/archive/latest-upload/
```

The archive is historical/reference material only. Do not implement from archived files unless explicitly instructed.

## Authority Rules

For implementation tasks:

- `docs/change-requests.md` is the accepted CR history.
- `docs/product-spec.md` is the current accepted product/UI/navigation/data baseline.
- `docs/implementation-plan.md` is the implementation contract.
- `docs/progress.md` records what actually happened and what remains pending.

If documents conflict:

1. Prefer the active CR section in `docs/change-requests.md` for task-specific behavior.
2. Prefer `docs/product-spec.md` for current accepted baseline.
3. Prefer `docs/implementation-plan.md` for concrete source-code and UI/design handoff work.
4. Treat `docs/archive/latest-upload/*` as lower-priority historical reference.
5. If still unclear, document the conflict in `docs/progress.md` and make the smallest safe change.

## Tool Usage Modes

### Codex App

Use the Codex App primarily for sharpening UI ideas, mockups, design directions, and non-code exploration.

When the user asks to "provide a mockup" or create a screen mockup:

- Produce a visual artifact only, such as an image, design preview, or UI description.
- Do not modify source code, layouts, assets, Gradle files, resources, or project files unless the user explicitly asks for implementation changes.

Implementation changes should be left for Codex CLI or another code-editing agent unless the user clearly requests code edits inside the current tool.

### Codex CLI / Code-Editing Agents

Use Codex CLI, Claude Code, Cursor, Windsurf, or another code-editing agent for repository modifications.

Whenever an instruction, task, plan, or CR modifies, creates, deletes, or migrates code files, follow the progress workflow below.

## Progress Tracking

Use `docs/progress.md` as the single progress file.

Before source-code changes:

1. Update `## Current Session`.
2. Record:
   - start timestamp
   - task goal
   - expected files
   - status: `In Progress`
3. If the implementation plan does not include the required source changes, update `docs/implementation-plan.md` before code edits.

After source-code changes:

1. Move the completed session into `## Task Log`.
2. Record:
   - end timestamp
   - final status: `Done`, `Partially Done`, or `Blocked`
   - files created
   - files edited
   - files deleted
   - migrations added
   - tests added or updated
   - tests run
   - tests not run and why
   - known remaining issues
   - concise suggested commit message
3. Clear or reset `## Current Session`.
4. Remind the user to make a git commit before starting another task.

If the task only changes documentation and does not touch source code, do not add a source-code progress entry unless the user explicitly asks.

## Build, Test, and Emulator Rules

Do not run a Gradle build, full test suite, emulator, or Android device run without explicit user permission.

This includes commands such as:

```text
./gradlew assembleDebug
./gradlew build
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

The user's laptop can be slow and resource constrained. Always ask permission before running heavy build, test, emulator, or device commands.

Lightweight static inspection, file reads, grep/search, and small local edits are allowed unless the user instructs otherwise.

If a build, test, or emulator run is not performed, document this clearly in `docs/progress.md`.

## Code Change Rules

- Keep existing GPS tracking behavior working unless the active CR explicitly changes it.
- Do not remove existing features unless a CR explicitly says so.
- Keep Compose + Material3 as the UI direction.
- Keep navigation consistent with `docs/product-spec.md`.
- Keep data model behavior consistent with `docs/product-spec.md`.
- Use Room migrations for database schema changes.
- Do not silently change schema without updating implementation docs and tests.
- Keep business logic out of Compose UI where practical.
- Keep data access behind repositories or data source abstractions where practical.
- Do not hardcode secrets, connection strings, credentials, or API keys.
- Do not implement unrelated future phases unless explicitly requested.

## Two-Track Work Model

For every CR or implementation phase, distinguish:

1. Code implementation work — for Codex / Claude Code / Cursor.
2. UI specification/design handoff work — for Claude Design / Google Stitch / Figma.

Do not mix design-only handoff instructions with source-code implementation unless explicitly requested.

### UI Screenshot Attachment Rule (Always Follow)

When producing or updating any UI specification/design handoff (including prompts for Google Stitch, Figma, Claude Design, or similar), always suggest which current-app screenshots the user should attach to preserve the app's visual language and interaction patterns.

Guidance:

- Prefer screenshots that establish baseline styling: bottom navigation, top app bar, list density, cards, typography, and any relevant control surfaces.
- If a referenced screen is currently empty/minimal, do not request it as a style reference; instead request other screens that reflect the real UI baseline.
- Tailor the screenshot list to the specific UI change (for example, if changing list behavior, include a representative list screen).

## CR-0001 Guardrails

- Sessions are separate from trips.
- The canonical location log is the source of truth for GPS points.
- Trips reference location ranges with `startLocationId` and `endLocationId`.
- Starting a trip while always-recording is OFF auto-starts always-recording.
- Stopping a trip does not stop always-recording.
- Deleting a trip must not delete canonical location history.

## CR-0002 Guardrails

- The Session screen is the primary always-recording control surface.
- The always-recording switch belongs inside the Session always-recording status area.
- The List screen remains trip-only and should not expose the always-recording switch.
- Starting a trip can auto-start always-recording; the Session switch must reflect ON.
- Stopping a trip does not turn the Session switch OFF.
- If a trip is active, do not allow the user to turn always-recording OFF unless a future CR changes this rule.
