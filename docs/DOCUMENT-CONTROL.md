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
| `docs/PRD.md` | 0.4 | Active (migrated from product-spec.md + change-requests.md) | Product Manager | 2026-07-07 |
| `docs/ARCHITECTURE.md` | 0.3 | Active (migrated from product-spec.md data rules) | Tech Lead | 2026-07-07 |
| `docs/adr/README.md` | 0.1 | Active (4 ADRs indexed) | Tech Lead | 2026-06-15 |
| `docs/adr/001-always-recorded-sessions.md` | 1.0 | Accepted (from CR-0001) | Project owner | 2026-06-15 |
| `docs/adr/002-session-recording-switch.md` | 1.0 | Accepted (from CR-0002) | Project owner | 2026-06-15 |
| `docs/adr/003-observer-navigation-placement.md` | 1.0 | Accepted | Project owner | 2026-06-15 |
| `docs/adr/004-obd-raw-at-io.md` | 1.0 | Accepted | Project owner | 2026-06-15 |
| `docs/adr/006-location-dwell-collapse.md` | 1.0 | Accepted | Project owner | 2026-07-07 |
| `docs/adr/007-unified-fuel-economy-metrics.md` | 1.0 | Accepted | Project owner | 2026-07-07 |
| `docs/IMPLEMENTATION-PLAN.md` | 0.4 | Active (migrated from implementation-plan.md + progress.md) | Product Manager / Tech Lead | 2026-07-07 |
| `docs/UI-SPEC.md` | 0.3 | Active (migrated from product-spec.md + DESIGN_SYSTEM.md + CR-0002) | Product Manager / UX Designer | 2026-07-07 |
| `docs/WORKFLOW.md` | 0.1 | Ready to use | Project Team | 2026-06-15 |
| `docs/IMPLEMENTATION-ISSUES.md` | 0.1 | Ready to use (blocker protocol) | Tech Lead | 2026-06-15 |
| `docs/ERRORS-LOG.md` | 0.1 | Active (persistent) | Project Team | 2026-06-15 |
| `AGENTS.md` | 0.4 | Active (template + preserved project rules + imported utbk-platform governance §5b + back-ported create-project §5c/§8b) | Tech Lead | 2026-07-06 |
| `CLAUDE.md` | 0.2 | Active | Tech Lead | 2026-06-15 |
| `README.md` | 0.2 | Active | Project Team | 2026-06-15 |
| `docs/PLANNING-LOG.md` | 0.1 | Active (temporary) — new, scaffolded from create-project template | Planning/Design Session | 2026-07-06 |
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
| 2026-07-02 | AGENTS.md | 0.2 | 0.3 | Added §5b "Imported Governance Rules" — a full governance rule set (Coding Permission, Execution, Fixing, Commit, Task-Completion Documentation, Coding Task, Verification, Test, Slice Documentation, Subagent Transparency rules) ported from the `utbk-platform` project at the user's explicit direction; these rules override §5/§6/§8/§8a where they conflict. | Claude Code |
| 2026-07-06 | AGENTS.md | 0.3 | 0.4 | Added §5c "Task Tracking Consolidation" and §8b "Decision & Documentation Workflow" — back-ported from the `create-project` skill template; this content predated TrackLocation's 2026-06-15 migration but was not carried over. Also added a `docs/PLANNING-LOG.md` reference to §11. | Claude Code |
| 2026-07-06 | docs/PLANNING-LOG.md | — | 0.1 | New document scaffolded from the `create-project` skill template — temporary planning/design session continuity log for the Two-Track Work Model's design track (AGENTS.md §12). | Claude Code |
| 2026-07-07 | docs/adr/006-location-dwell-collapse.md | — | 0.1 | New ADR (Proposed) — Location Dwell Collapse: collapse stationary GPS fixes into one canonical `location_log` anchor per stop (new `dwellStartTimestamp`/`collapsedCount` columns, Room MIGRATION_6_7). Downstream PRD §12 / ARCHITECTURE / IMPLEMENTATION-PLAN edits pending acceptance. | Claude Code |
| 2026-07-07 | PRD, ARCHITECTURE, IMPLEMENTATION-PLAN, adr/006 | 0.2/0.1/0.2/0.1 | 0.3/0.2/0.3/1.0 | ADR-006 accepted (Location Dwell Collapse). Propagated: PRD §12 (BR-02 amended + new BR-11), ARCHITECTURE §4/§8 (dwell columns `dwellStartTimestamp`/`collapsedCount` + MIGRATION_6_7, DB v7, corrected stale v5 note), IMPLEMENTATION-PLAN §3 phase row + Appendix A two-slice contract. Code pending build permission. | Claude Code |
| 2026-07-07 | PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN, adr/007 | 0.3/0.2/0.2/0.3/— | 0.4/0.3/0.3/0.4/1.0 | ADR-007 accepted — Unified Fuel-Economy Metrics: instant two-cell (km/L + L/h), single averaging derivation (displacement distance ÷ obd_sample fuel, Option B incremental cache, authoritative at close), drop obdGpsDistanceKm via destructive v7→v8. Propagated to PRD/ARCHITECTURE/UI-SPEC/IMPLEMENTATION-PLAN. Code pending. | Claude Code |

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
