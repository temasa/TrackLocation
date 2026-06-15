---
name: DOCUMENT-CONTROL.md
path: docs/DOCUMENT-CONTROL.md
description: Document control register and change log — fully generic, ready to use
---

# Document Control Register
## TrackLocation

**Document Version:** 0.1  
**Status:** Active  
**Last Updated:** 2026-06-15 11:44:58  
**Owner:** Product Manager  
**Controlled By:** This file

---

## 1. Purpose

This file is the single control register for all project documents. It records:

- Current document versions
- Document ownership
- Document status
- Change history across all controlled files

When any controlled document changes, update both:

1. The version header inside that document
2. The change log in this file

---

## 2. Current Document Register

| Document | Current Version | Status | Owner | Last Updated |
|---|---:|---|---|---|
| `docs/PRD.md` | 0.2 | Active (migrated from product-spec.md + change-requests.md) | Product Manager | 2026-06-15 |
| `docs/ARCHITECTURE.md` | 0.1 | Active (migrated from product-spec.md data rules) | Tech Lead | 2026-06-15 |
| `docs/adr/README.md` | 0.1 | Active (4 ADRs indexed) | Tech Lead | 2026-06-15 |
| `docs/adr/001-always-recorded-sessions.md` | 1.0 | Accepted (from CR-0001) | Project owner | 2026-06-15 |
| `docs/adr/002-session-recording-switch.md` | 1.0 | Accepted (from CR-0002) | Project owner | 2026-06-15 |
| `docs/adr/003-observer-navigation-placement.md` | 1.0 | Accepted | Project owner | 2026-06-15 |
| `docs/adr/004-obd-raw-at-io.md` | 1.0 | Accepted | Project owner | 2026-06-15 |
| `docs/IMPLEMENTATION-PLAN.md` | 0.2 | Active (migrated from implementation-plan.md + progress.md) | Product Manager / Tech Lead | 2026-06-15 |
| `docs/UI-SPEC.md` | 0.2 | Active (migrated from product-spec.md + DESIGN_SYSTEM.md + CR-0002) | Product Manager / UX Designer | 2026-06-15 |
| `docs/WORKFLOW.md` | 0.1 | Ready to use | Project Team | 2026-06-15 |
| `docs/IMPLEMENTATION-ISSUES.md` | 0.1 | Ready to use (blocker protocol) | Tech Lead | 2026-06-15 |
| `docs/ERRORS-LOG.md` | 0.1 | Active (persistent) | Project Team | 2026-06-15 |
| `AGENTS.md` | 0.2 | Active (template + preserved project rules) | Tech Lead | 2026-06-15 |
| `CLAUDE.md` | 0.2 | Active | Tech Lead | 2026-06-15 |
| `README.md` | 0.2 | Active | Project Team | 2026-06-15 |
| `docs/DOCUMENT-CONTROL.md` | 0.1 | Active | Product Manager | 2026-06-15 |

**Retired in migration (content folded into the docs above):** `docs/product-spec.md` → PRD + ARCHITECTURE + UI-SPEC; `docs/change-requests.md` → adr/001–002 (+ PRD §12); `docs/progress.md` → IMPLEMENTATION-PLAN §6 + Appendix B; `docs/DESIGN_SYSTEM.md` → UI-SPEC §5/§9.

---

## 3. Versioning Rules

Use semantic-style document versions:

- **Major (`1.0`, `2.0`)** — approved baseline or major scope/structure change
- **Minor (`0.1`, `0.2`, `1.1`)** — meaningful content update
- **Patch (`0.2.1`, `1.1.1`)** — small correction with no requirement or scope impact

Draft documents normally begin at `0.1`.

---

## 4. Change Log

| Date | Document | From | To | Change Summary | Changed By |
|---|---|---:|---:|---|---|
| 2026-06-15 11:44:58 | (all) | — | 0.1 | Scaffolded from generic project-initialization template via the create-project skill. | Template / Project Team |
| 2026-06-15 | PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN, AGENTS, CLAUDE, README | 0.1 | 0.2/0.1 | **Full adoption migration:** routed legacy `product-spec.md`, `change-requests.md`, `implementation-plan.md`, `progress.md`, `DESIGN_SYSTEM.md` content into the template schema; created ADR-001…004 from CRs/decisions; retired the five legacy docs. Nothing dropped (full audit preserved in IMPLEMENTATION-PLAN Appendix B). | Claude Code |

---

## 5. Controlled-Document Update Procedure

Update `DOCUMENT-CONTROL.md` only when at least one controlled document is actually changed.

A controlled-document change includes:

- Content added, removed, or revised
- Requirement, scope, rule, or status changed
- Document version changed
- Document ownership changed
- A new controlled document added
- A controlled document retired or renamed

Do not update the change log for:

- Discussions that do not modify a document
- Downloading or packaging files
- Regenerating the ZIP without document changes
- Reviewing a document without editing it
- Re-uploading an unchanged file

When a controlled document changes:

1. Update that document.
2. Increment that document's version.
3. Update its `Last Updated` value.
4. Update the current document register in this file.
5. Add one change-log row for that changed document.

Increment the version of `DOCUMENT-CONTROL.md` only when its own structure, rules, or governance content changes (not for routine change-log additions from other documents).

---

## 6. Change-Control Principle

> `DOCUMENT-CONTROL.md` records real document changes only. It does not record conversations, file transfers, downloads, or packaging operations.
