# TrackLocation

**Document Version:** 0.2
**Status:** Active development
**Last Updated:** 2026-06-15
**Controlled By:** `docs/DOCUMENT-CONTROL.md`

---

## Project Overview

TrackLocation is an Android (Jetpack Compose + Material 3) driver utility evolving from a GPS trip tracker into a broader operational tool. It records location continuously into a canonical, append-only log, lets users define explicit trips over that log, observes accessibility events locally for developer/support inspection, and surfaces live ELM327 OBD-II telemetry (RPM, speed, fuel/efficiency). Architecture is local-first and sync-ready; later phases add registration and face-first authentication.

---

## Documentation

This project is governed by an integrated set of documents:

1. **`AGENTS.md`** — AI-assistant instructions, responsibilities, guardrails.
2. **`CLAUDE.md`** — Claude Code instructions and user preferences.
3. **`docs/PRD.md`** — Product requirements (source of truth).
4. **`docs/ARCHITECTURE.md`** — System architecture, domain model, data model.
5. **`docs/adr/`** — Architecture Decision Records.
6. **`docs/IMPLEMENTATION-PLAN.md`** — Authoritative implementation plan, slices, and task log.
7. **`docs/UI-SPEC.md`** — UI principles, screen inventory, design system.
8. **`docs/WORKFLOW.md`** — Human-facing development workflow.
9. **`docs/IMPLEMENTATION-ISSUES.md`** — Blocker protocol.
10. **`docs/ERRORS-LOG.md`** — Persistent error learning log.
11. **`docs/DOCUMENT-CONTROL.md`** — Version register and change log.

---

## Project Structure

```text
TrackLocation/
├── AGENTS.md                 # AI-assistant responsibilities
├── CLAUDE.md                 # Claude Code instructions
├── README.md                 # This file
├── app/                      # Android app module (com.kolee.tracklocation)
├── docs/
│   ├── PRD.md                # Product requirements
│   ├── ARCHITECTURE.md       # Architecture & data model
│   ├── adr/                  # Architecture Decision Records
│   ├── IMPLEMENTATION-PLAN.md# Plan, slices, task log, session history
│   ├── UI-SPEC.md            # UI spec + design tokens
│   ├── WORKFLOW.md           # Workflow guide
│   ├── IMPLEMENTATION-ISSUES.md
│   ├── DOCUMENT-CONTROL.md
│   ├── ERRORS-LOG.md
│   ├── design/               # Design handoff artifacts (per feature)
│   ├── design-handoff/       # OBD + Observer truncation specs
│   └── archive/              # Historical reference (do not implement from)
```

---

## Getting Started

1. **Read `AGENTS.md`** — responsibilities, source-of-truth priority, git safeguard, build-permission gate, blocker protocol.
2. **Skim the docs** — PRD → ARCHITECTURE → IMPLEMENTATION-PLAN → UI-SPEC.
3. **Build** — Android Gradle project; local JDK `C:\Users\rinal\.jdks\jbr-17.0.14`. Heavy Gradle/test/emulator runs require explicit user permission (AGENTS.md §5a).

## Document Maintenance

All documents are controlled by `docs/DOCUMENT-CONTROL.md`. When a document changes: update it, bump its version, update `Last Updated`, add a change-log entry, and commit with a clear message (see IMPLEMENTATION-PLAN §8 commit format).

## Workflow Highlights

- **Source of truth** (AGENTS.md §1): PRD → IMPLEMENTATION-PLAN → architecture docs → source.
- **Two-track model** (AGENTS.md §12): code work vs. UI design-handoff work, kept separate.
- **Blocker protocol** (AGENTS.md §9 / IMPLEMENTATION-ISSUES.md): raise and halt on doc contradictions.
- **Git safeguard** (AGENTS.md §7): snapshot before every file-modifying task.
- **Decision log** (`docs/adr/`): record significant decisions as ADRs.

---

## Legacy / Unsorted (from original README.md)

Original tutorial reference and learning checklist that seeded the project:

- Source video: https://youtu.be/LND9Cjc7CRM
- Topics: Jetpack Compose (Android App); transfer data between screens; tracking user's current location on Google Map; location permission; Navigation; Room Database; Flow, `mutableStateOf`; LazyColumn (= RecyclerView); foreground tracking service; timer duration; ElevatedCard.
