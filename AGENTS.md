# AGENTS.md — TrackLocation AI Coding Rules

## Purpose

This file defines stable instructions for AI coding assistants working on the TrackLocation Android project.

Use this file for long-lived project rules. Put the current task, change request, or implementation goal in the prompt when starting Codex, Claude Code, Cursor, Windsurf, or another coding agent.

## Project Context

TrackLocation is an Android Jetpack Compose application evolving from a GPS trip tracker into a broader driver utility with:

- GPS trip tracking
- Always-recorded location sessions
- Accessibility Event Observer
- Local-first storage
- Sync-ready architecture
- Future registration and face-first authentication

The existing GPS tracking feature must remain intact unless the active task or accepted change request explicitly changes it.

## Tool Usage Modes

### Codex App

Use the Codex App primarily for sharpening UI ideas, mockups, design directions, and non-code exploration.

When the user asks to "provide a mockup" or create a screen mockup:

- Produce a visual artifact only, such as an image, design preview, or UI description.
- Do not modify source code, layouts, assets, Gradle files, resources, or project files unless the user explicitly asks for implementation changes.

Implementation changes should be left for Codex CLI or another code-editing agent unless the user clearly requests code edits inside the current tool.

### Codex CLI / Code-Editing Agents

Use Codex CLI, Claude Code, Cursor, Windsurf, or another code-editing agent for actual repository modifications.

Whenever an instruction, task, plan, or change request modifies, creates, deletes, or migrates code files, follow the documentation and progress workflow in this file.

## Documentation Source of Truth

The active documentation is under `docs/`.

Read documentation in this order before implementation:

1. `docs/README.md` or root `README.md` if present
2. `docs/INDEX.md`
3. `docs/product/product-baseline.md`
4. Relevant files in `docs/change-requests/`
5. `docs/architecture/navigation.md`
6. `docs/architecture/data-model.md`
7. `docs/architecture/app-architecture.md`
8. `docs/ui/screen-specification.md`
9. `docs/ui/ui-implementation-plan.md`
10. `docs/implementation/source-change-manifest.md`
11. `docs/implementation/migration-plan.md`
12. `docs/implementation/test-plan.md`
13. `docs/status/status_report.txt`
14. `docs/status/progress.md`

If a listed file does not exist, continue with the available files and document the missing file only if it affects the task.

## Authority Rules

For implementation tasks:

- `docs/implementation/source-change-manifest.md` is the source-code change contract.
- Relevant files in `docs/change-requests/` are the requirement source for accepted change requests.
- `docs/architecture/*.md` files describe the current accepted architecture.
- `docs/ui/*.md` files describe current and planned UI behavior.
- `docs/status/*` files are historical/status logs.
- `docs/archive/original/*` is historical reference only. Do not implement from archived files unless explicitly instructed.

If documents conflict:

1. Prefer the relevant accepted change request for task-specific behavior.
2. Prefer architecture docs for current system structure.
3. Prefer the source-change manifest for concrete source file changes.
4. Treat archived docs as lower priority.
5. If still unclear, document the conflict in `docs/status/status_report.txt` and make the smallest safe implementation.

## Mandatory Workflow Before Code Changes

Before creating, editing, deleting, or migrating source code files:

1. Read the relevant source-of-truth documents.
2. Check `docs/implementation/source-change-manifest.md`.
3. If the source-change manifest does not include a needed file change, update the manifest first.
4. Update `docs/status/status_report.txt`.
5. Record:
   - start timestamp
   - task goal in short and concise form
   - expected affected files
   - current status: `In Progress`

Do not start code edits before the status report is updated.

## Mandatory Workflow After Code Changes

After implementation:

1. Update `docs/status/status_report.txt`.
2. If source code changed, update `docs/status/progress.md`.
3. Record:
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

When listing affected or created code files, include a brief inline comment:

```text
app/src/main/java/.../ExampleFile.kt // Added repository method for session ranges.
```

After completing a task, remind the user to make a git commit before starting another task.

## Progress Tracking Rules

Use `docs/status/status_report.txt` for the active task status.

Use `docs/status/progress.md` for session-level historical progress only when source code changed.

If the task only changes documentation and does not touch code, do not add a source-code progress entry unless the user explicitly asks.

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

You may perform lightweight static inspection, file reads, grep/search, and small local edits without asking, unless the user has instructed otherwise.

If a build, test, or emulator run is not performed, document this clearly in `docs/status/status_report.txt` under "Tests not run / Verification not run".

## Code Change Rules

- Keep existing GPS tracking behavior working unless the active CR explicitly changes it.
- Do not remove existing features unless a CR explicitly says so.
- Keep Compose + Material3 as the UI direction.
- Keep navigation consistent with `docs/architecture/navigation.md`.
- Keep the data model consistent with `docs/architecture/data-model.md`.
- Use Room migrations for database schema changes.
- Do not silently change schema without updating migration docs and tests.
- Keep business logic out of Compose UI where practical.
- Keep data access behind repositories or data source abstractions where practical.
- Do not hardcode secrets, connection strings, credentials, or API keys.
- Do not implement unrelated future phases unless explicitly requested.

## Change Request Implementation Rules

For every change request:

1. Read the CR document first.
2. Read the impact sections:
   - product behavior
   - navigation
   - UI
   - data model
   - migration
   - source-code impact
   - tests
3. Update the source-change manifest before implementation if needed.
4. Implement only the current CR scope unless instructed otherwise.
5. Do not implement future planned phases accidentally.
6. Update status docs before and after implementation.

## Testing Rules

When permission is granted to run verification, prefer the smallest meaningful verification first.

Recommended order:

1. Static/code inspection
2. Unit tests for changed logic
3. Room migration tests if schema changed
4. `:app:compileDebugKotlin`
5. `:app:testDebugUnitTest`
6. `:app:assembleDebug`

If a test/build cannot be run because of machine limits, timeouts, missing SDK, or user instruction, document it clearly in `docs/status/status_report.txt`.

## CR-0001 Guardrails

When working on CR-0001 Always-recorded Location Sessions:

- Treat always-recorded sessions as separate from trips.
- Treat the canonical location log as the source of truth for recorded GPS points.
- Trips should reference location ranges with `startLocationId` and `endLocationId` when the data model migration is implemented.
- Starting a trip may start always-recording if it is not already active.
- Stopping a trip must not stop always-recording.
- Deleting a trip must not delete canonical location history.
- Keep current navigation aligned with the accepted CR navigation unless a later CR supersedes it.

## Accessibility Observer Guardrails

When working on the Accessibility Observer feature:

- Capture all packages except the configurable denylist.
- Package filtering is a UI concern, not a capture-layer allowlist.
- Do not associate observed events with users; events are device-level.
- Do not delete unsynced local events.
- Keep sync logic behind `RemoteDataSource`.
- Do not put Neon logic directly in Compose UI, ViewModels, or the accessibility service.
- Keep JSON viewer full-screen when implemented because snapshots can be large.

## Auth and Privacy Guardrails

When working on registration, face enrollment, authentication, or re-authentication:

- Do not transmit raw face images.
- Face authentication is primary after registration.
- Google SSO is fallback only after face auth fails or cannot run.
- Auth gates UI access only; GPS tracking, accessibility capture, and sync must continue in the background.
- Do not show Google SSO as an equal primary login option after registration.

## Suggested Commit Message Format

At the end of each implementation task, provide one concise suggested commit message, for example:

```text
Implement CR-0001 session navigation baseline
```

