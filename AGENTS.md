---
name: AGENTS.md
description: AI-assistant instructions for TrackLocation (create-project schema + preserved project rules)
---

# AGENTS.md

Instructions for AI Coding Assistants — TrackLocation

**Document Version:** 0.3
**Status:** Active
**Last Updated:** 2026-07-02
**Controlled By:** `docs/DOCUMENT-CONTROL.md`

> Migrated 2026-06-15 to the create-project governance schema. Generic methodology is template-owned; TrackLocation-specific rules are preserved in §2, §3, §5a, and §12.
> 2026-07-02: §5b imported from the `utbk-platform` project's governance rules (user directive — these rules take priority over the generic template where they overlap).

---

## 0. Tool-Based Responsibilities

### Code-editing agents (Claude Code, Codex CLI, Cursor, Windsurf)
- ✅ Work with source code (read, write, edit, create files)
- ✅ Execute git workflows
- ✅ Full project implementation
- ⚠️ Heavy verification (Gradle/test/emulator/device) requires explicit user permission — see §5a

### Codex App / Claude Desktop (planning & design)
- ✅ Planning, architecture discussion, mockups, design directions, non-code exploration
- ❌ DO NOT modify source code, layouts, assets, Gradle files, resources, or project files unless the user explicitly asks for implementation changes

---

## 1. Source of Truth

Priority order:

1. `docs/PRD.md`
2. `docs/IMPLEMENTATION-PLAN.md`
3. Approved architecture docs (`docs/ARCHITECTURE.md`, `docs/adr/`)
4. Source code and tests

Do not silently contradict a higher-priority document. If documents conflict, prefer the active ADR/PRD for product behavior, then ARCHITECTURE, then IMPLEMENTATION-PLAN; if still unclear, raise a blocker (§9) and make the smallest safe change.

---

## 2. Product Boundaries (TrackLocation-specific)

In scope: GPS trip tracking, always-recorded location sessions, the Accessibility Observer, OBD-II telemetry, and the planned registration/auth phases. Out of scope: anything in PRD §8 "Out of Scope".

The AI assistant must **never unilaterally**:

- Change the canonical-location-log-as-source-of-truth model or the sessions-vs-trips separation (PRD §12).
- Remove or degrade existing GPS tracking behavior.
- Add a new bottom-nav destination or move the always-recording switch off the Session screen.
- Introduce BLE OBD, in-app BT discovery/PIN entry, or user-facing Observer-history deletion.

Record gaps and ask for a product decision; do not invent scope that conflicts with locked decisions.

---

## 3. Decisions the Coding Assistant Must Not Invent

Do not independently define:

- Product scope or new product directions (belongs in PRD / a new ADR).
- The locked business rules in PRD §12 (sessions/trips, canonical log, switch placement, retention).
- Navigation/IA changes (e.g., revisiting Observer Option B).
- Data-retention or privacy rules (Observer retention, "raw face images never transmitted").
- Major UI/UX behavior — produce a UI spec/design handoff first (§12 two-track model).

Record gaps and ask for a product decision.

---

## 4. Technical Responsibilities

Propose and maintain:

- **System architecture / domain model / schema** → `docs/ARCHITECTURE.md`
- **Significant technical decisions** → `docs/adr/`
- **Room migrations** for every schema change (never change schema silently)
- **Local setup instructions** → `README.md`
- Keep business logic out of Compose where practical; keep data access behind DAOs/repositories.

---

## 5. Implementation Process

Before coding: read project docs, inspect the repo, identify blocking gaps, propose steps, record assumptions.
During: work incrementally in end-to-end vertical slices; keep docs synchronized; do not silently change product behavior.
After: list assumptions/deviations and remaining work; mark the task **Completed** in `docs/IMPLEMENTATION-PLAN.md §6 Task Log` with `Ended`, `Git Revision`, and a one-line `Verification`; commit source + updated plan together. Then **remind the user to commit before starting the next task.**

---

## 5a. Build, Test & Emulator Permission Gate (TrackLocation-specific — MANDATORY)

Do **not** run a Gradle build, full test suite, emulator, or device run without explicit user permission. This includes `./gradlew assembleDebug`, `./gradlew build`, `./gradlew testDebugUnitTest`, `./gradlew connectedDebugAndroidTest`. The user's laptop is resource-constrained.

Lightweight static inspection, file reads, search, and small local edits are allowed. If a build/test/emulator run is not performed, document this clearly in the task log. Local JDK for any permitted Gradle run: `C:\Users\rinal\.jdks\jbr-17.0.14` (inject `JAVA_HOME` inline).

---

## 5b. Imported Governance Rules (from `utbk-platform`, 2026-07-02 — MANDATORY, overrides conflicting sections above)

These rules were imported at the user's explicit direction and take priority over §5, §6, §8, and §8a where they overlap.

### Coding Permission Rule
Do not write or modify any code before explicitly asking for and receiving the user's permission — this includes creating files, editing existing files, running code-generating commands, or any codebase change. Allowed without permission: reading files, searching, exploring, planning, and explaining. Always present the plan first and wait for explicit approval ("yes", "go ahead", "proceed", "do it", or similar) before touching any code.

### Execution Rule
Once the user approves an action that writes to project documents or writes/modifies code, delegate the execution to a sub-agent using the `haiku` model. The main model (Sonnet) handles planning, analysis, impact assessment, and decision-making only — it never writes files or modifies code directly after the planning phase. Applies to: any file in `docs/`, any source file, any other project file modification.

### Fixing Rule
Before fixing any error, always analyze the root cause first and report it to the user — including what caused it, which file/line, and why — before proceeding with the fix. Once the cause is understood and approved:
- **Simple fix** (single file, obvious/localized cause — typo, wrong variable, missing import, incorrect value) → delegate to a `haiku` sub-agent.
- **Complex fix** (multiple files, architectural issue, requires understanding system state, cascading failures, race conditions, non-obvious root cause) → delegate to a `sonnet` sub-agent.

Never proceed with a fix — simple or complex — without first reporting the cause to the user.

### Commit Rule
Once the user approves a git commit, delegate the execution to a sub-agent using the `haiku` model. The main model drafts the commit message and confirms staged files only; the sub-agent runs `git add`, `git commit`, and (if requested) `git push`. Never run `git commit` directly from the main model.

### Task-Completion Documentation Rule
**Scope: documentation changes only** (does not govern code changes — those follow the Coding Task Rule below). When something recordable is finished — a decision reached (UI, UX, architectural, scope, governance), an error encountered with a lesson learned, or a status/milestone worth logging — do NOT auto-write docs. Follow six steps:

1. **Plan the record** — identify everything worth capturing.
2. **Show what needs recording** — present the actual content to be written, not a vague summary.
3. **Show the routing** — name the destination doc/section per TrackLocation's existing routing table (§8), and **audit all project docs for stale references**: scan `PRD.md`, `ARCHITECTURE.md`, `IMPLEMENTATION-PLAN.md`, `UI-SPEC.md`, `ERRORS-LOG.md`, `IMPLEMENTATION-ISSUES.md`, and `adr/` for existing content the new decision now contradicts or invalidates, even if not a primary routing target.
4. **Wait for approval** — do not write anything until the user approves the plan.
5. **Route the content** — after approval, delegate the writes to a sub-agent (per the Execution Rule).
6. **Commit** — after the documents are written, commit (per the Commit Rule).

Never write docs without showing the plan first; never leave recorded changes uncommitted. This rule never fires on context-window summarization or a silent buffer reset.

### Coding Task Rule
**Scope: code changes only.** Three phases:

- **Phase 1 — Documentation gate.** Verify the intended change is already recorded in project documentation AND those docs are committed. If not documented and committed, halt, inform the user, and offer to run the Task-Completion Documentation Rule first, then resume.
- **Phase 2 — Plan gate.** Present the planned code changes and wait for approval per the Coding Permission Rule.
- **Phase 3 — Execute, then commit.** Delegate the code changes to a sub-agent, preferring the most cost-effective model adequate for complexity (`haiku` for simple/localized, `sonnet` for complex/multi-file/architectural). After completion, always commit (per the Commit Rule). Before committing, check whether the changes form one coherent conceptual change; if not, propose splitting into multiple atomic commits for approval.

### Verification Rule
**Scope: confirming a change works.** Applies after a coding task or fix, and whenever verification is requested. Delegate verification to a sub-agent, preferring the most cost-effective model adequate for complexity (`haiku` for deterministic checks, `sonnet` when interpreting behavior against intent).
- **Deterministic checks** (tests, build, type-check, lint) — the sub-agent runs them and reports pass/fail directly; pass/fail is objective.
- **Interpretive verification** (behavior matches intent, UI correctness, subtle regressions) — the sub-agent executes and returns raw evidence (output, logs, screenshots); **the main model always interprets and declares the final pass/fail** — a sub-agent must never be the final judge here.

Report outcomes faithfully: if a check fails, say so with the output; never label a partial or ambiguous result as "verified."

### Test Rule
**Scope: automated tests.** Tests are written or updated only when the user explicitly requests them; not required by default. Writing/modifying tests is a coding task (follows the Coding Task Rule). Running tests is verification (follows the Verification Rule).

### Slice Documentation Rule (adapted from utbk-platform's Sprint Documentation Rule)
Before a slice starts, its entry in `docs/IMPLEMENTATION-PLAN.md §4` must have a "How to Verify" block written (already required by §6 below). Before a slice is marked complete in the §6 Task Log, if the slice maps to tracked user stories, a US→Task traceability table must be filled in immediately after the User Stories table:

| US # | User Story (short) | Implemented by |
|------|--------------------|----------------|
| US-01 | ... | Task ID(s) |

Backfilling for already-completed slices is handled via the Task-Completion Documentation Rule.

### Subagent Transparency Rule
Whenever delegating to a sub-agent, always explicitly state the model being used in the response — e.g. "Delegating to a haiku sub-agent" or "Running a sonnet sub-agent for this." Never spawn a sub-agent silently without naming the model.

---

## 6. First Vertical Slice

TrackLocation is past its first slice — GPS tracking, sessions (CR-0001/0002), Observer P1, and OBD P1 are implemented and verified. New work follows the per-slice pattern in `docs/IMPLEMENTATION-PLAN.md §4` (each slice: what it does, observable result, how to verify, numbered steps with What/How).

**Slice verification rule (mandatory):** Every slice definition must include a "How to Verify" block if the slice is verifiable. All verification detail — steps, expected output, hardware prerequisites, manual setup — goes inline in that block. Never put verification content in a separate section or file. A reader must be able to read one slice entry and know both what was built and how to confirm it works.

---

## 7. Git Safeguard Workflow

At the start of every file-modifying task, snapshot the working tree so it survives a failed task:

```bash
git stash push -u -m "pre-task: <short description>" && git stash apply
```

Clean tree → `git stash push` prints `No local changes to save` and `&&` skips the apply (expected). On success: `git stash drop stash@{0}`. On failure: `git reset --hard HEAD && git clean -fd` then `git stash apply stash@{0}` and drop. `git clean -fd` is destructive — only on failure recovery.

---

## 8. Documentation Rule

Use one authoritative implementation plan: `docs/IMPLEMENTATION-PLAN.md`. Do not create separate phase-specific plans. Every task-log entry records **commit status** in the `Git Revision` column — this is the single source of truth for branch/revision; do not add a redundant Branch field.

**The `docs/` directory is a closed set.** Never create a new `.md` file in `docs/` or at the project root. Route all new content into the existing governance doc whose purpose matches:

| Content type | Correct home |
|---|---|
| Build / deploy / compile / runtime errors | `docs/ERRORS-LOG.md` |
| Manual test procedures, hardware verification | inline in the slice definition (§4 "How to Verify") or task Verification column (§6) |
| Verification steps for a task or slice | `docs/IMPLEMENTATION-PLAN.md §6` (Verification column) or §4 "How to Verify" |
| Product decisions, scope, requirements | `docs/PRD.md` |
| UI behavior, screen specs, interaction notes | `docs/UI-SPEC.md` |
| Architecture, domain model, tech stack | `docs/ARCHITECTURE.md` |
| Significant technical decisions | `docs/adr/` (new ADR file — the only permitted new file in docs) |
| Blockers and contradictions | `docs/IMPLEMENTATION-ISSUES.md` |

If no existing doc fits the content, raise a blocker (§9) — do not create a new file.

**Revision-hash backfill rule.** A commit's hash cannot be written into a file that is part of that same commit (the content would change the hash). So:

1. When committing a task, put `---` in the `Git Revision` column (a "backfill me" placeholder — not `uncommitted`, which reads as a permanent state), and commit source + plan together.
2. In the **next** commit, replace that `---` with the short hash of the commit that *introduced the row* (i.e. the commit containing the task's work — usually the immediately preceding commit).
3. `---` must never survive in `HEAD` across more than one commit. Treat a lingering `---` as an unfinished task-log entry.

---

## 8a. Doc-Impact Rule

When the user states a decision, feature, change, or constraint — analyze impact and update all affected docs **before** writing code. Distinguish proactively: a new **product decision** → PRD and/or a new ADR; an **implementation/fix** → IMPLEMENTATION-PLAN (§4 slices / §6 task log). Typical candidates: `docs/PRD.md`, `docs/UI-SPEC.md`, `docs/IMPLEMENTATION-PLAN.md`, `docs/ARCHITECTURE.md`, `docs/adr/`, `AGENTS.md`/`docs/WORKFLOW.md`. Show impact analysis, get approval, update, then proceed. Document-first is automatic and proactive.

---

## 9. Blocker Protocol

If implementation finds a contradiction/ambiguity/missing decision in locked docs, **halt and raise a blocker** — do not guess. Full protocol in `docs/IMPLEMENTATION-ISSUES.md`: developer writes the issue and commits `[doc-issue] …`, halts; architect updates docs, commits `[doc-decision] …`; developer resumes.

---

## 10. Error Logging Protocol

Errors during compilation, build, deployment, or verification are permanent learning artifacts. Search `docs/ERRORS-LOG.md` before debugging; log every error (Found At, Resolved At, root cause, resolution, lessons); after 3+ similar, add a pattern entry.

---

## 11. Reference Documents

- `docs/PRD.md` — product requirements (source of truth #1)
- `docs/ARCHITECTURE.md` — architecture, domain model, data model
- `docs/adr/` — Architecture Decision Records
- `docs/IMPLEMENTATION-PLAN.md` — slices, task log, commit format
- `docs/UI-SPEC.md` — UI designs, screen inventory, design system
- `docs/IMPLEMENTATION-ISSUES.md` — blocker channel
- `docs/ERRORS-LOG.md` — error learning log
- `docs/DOCUMENT-CONTROL.md` — version register
- `docs/WORKFLOW.md` — human-facing workflow

---

## 12. Two-Track Work Model (TrackLocation-specific)

For every CR or implementation phase, distinguish:

1. **Code implementation work** → Codex CLI / Claude Code / Cursor / Windsurf.
2. **UI specification / design handoff work** → Claude Design / Google Stitch / Figma.

Do not mix design-only handoff instructions with source-code implementation unless explicitly requested. New UI needs a design-handoff spec **first**; don't invent visual design directly in code.

### UI Screenshot Attachment Rule (always follow)

When producing/updating any UI spec or design handoff (Google Stitch, Figma, Claude Design), always suggest which current-app screenshots to attach to preserve the visual language. Prefer baseline-establishing screens (bottom nav, top app bar, list density, cards, typography, relevant control surfaces). Skip empty/minimal screens; tailor the list to the specific change.

### Design handoff spec template

Every handoff spec includes a **Handoff Instructions** section: step-by-step process, screenshot checklist, tool recommendations, and a copy-paste-ready prompt.

---

## 13. Code Change Guardrails (TrackLocation-specific)

- Keep existing GPS tracking working unless an accepted decision explicitly changes it.
- Do not remove existing features unless a decision (PRD/ADR) explicitly says so.
- Keep Compose + Material 3; keep navigation and data-model behavior consistent with PRD/UI-SPEC/ARCHITECTURE.
- Use Room migrations for schema changes; update ARCHITECTURE + tests when schema changes.
- Do not hardcode secrets/credentials/keys.
- Do not implement unrelated future phases unless explicitly requested.
