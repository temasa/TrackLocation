---
name: AGENTS.md
description: AI-assistant instructions for TrackLocation (create-project schema + preserved project rules)
---

# AGENTS.md

Instructions for AI Coding Assistants — TrackLocation

**Document Version:** 0.2
**Status:** Active
**Last Updated:** 2026-06-15
**Controlled By:** `docs/DOCUMENT-CONTROL.md`

> Migrated 2026-06-15 to the create-project governance schema. Generic methodology is template-owned; TrackLocation-specific rules are preserved in §2, §3, §5a, and §12.

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

## 6. First Vertical Slice

TrackLocation is past its first slice — GPS tracking, sessions (CR-0001/0002), Observer P1, and OBD P1 are implemented and verified. New work follows the per-slice pattern in `docs/IMPLEMENTATION-PLAN.md §4` (each slice: what it does, observable result, how to verify, numbered steps with What/How).

---

## 7. Git Safeguard Workflow

At the start of every file-modifying task, snapshot the working tree so it survives a failed task:

```bash
git stash push -u -m "pre-task: <short description>" && git stash apply
```

Clean tree → `git stash push` prints `No local changes to save` and `&&` skips the apply (expected). On success: `git stash drop stash@{0}`. On failure: `git reset --hard HEAD && git clean -fd` then `git stash apply stash@{0}` and drop. `git clean -fd` is destructive — only on failure recovery.

---

## 8. Documentation Rule

Use one authoritative implementation plan: `docs/IMPLEMENTATION-PLAN.md`. Do not create separate phase-specific plans. Every task-log entry records **commit status** (uncommitted, or committed + branch + revision hash) — this is the single source of truth for branch/revision; do not add a redundant Branch field.

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
