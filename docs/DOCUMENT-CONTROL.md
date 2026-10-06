---
name: DOCUMENT-CONTROL.md
path: docs/DOCUMENT-CONTROL.md
description: Document control register and change log — fully generic, ready to use
---

# Document Control Register
## TrackLocation

**Document Version:** 0.1  
**Status:** Active  
**Last Updated:** 2026-10-06  
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
| `docs/PRD.md` | 0.11 | Active (migrated from product-spec.md + change-requests.md) | Product Manager | 2026-10-06 |
| `docs/ARCHITECTURE.md` | 0.13 | Active (migrated from product-spec.md data rules) | Tech Lead | 2026-10-06 |
| `docs/adr/README.md` | 0.4 | Active (15 ADRs indexed) | Tech Lead | 2026-10-06 |
| `docs/adr/001-always-recorded-sessions.md` | 1.0 | Accepted (from CR-0001) | Project owner | 2026-06-15 |
| `docs/adr/002-session-recording-switch.md` | 1.0 | Accepted (from CR-0002) | Project owner | 2026-06-15 |
| `docs/adr/003-observer-navigation-placement.md` | 1.0 | Accepted | Project owner | 2026-06-15 |
| `docs/adr/004-obd-raw-at-io.md` | 1.0 | Accepted | Project owner | 2026-06-15 |
| `docs/adr/006-location-dwell-collapse.md` | 1.0 | Accepted | Project owner | 2026-07-07 |
| `docs/adr/007-unified-fuel-economy-metrics.md` | 1.0 | Accepted | Project owner | 2026-07-07 |
| `docs/adr/008-fuel-price-effective-dated-entity.md` | 1.0 | Accepted | Project owner | 2026-07-07 |
| `docs/adr/009-dual-mode-track-navigation.md` | 1.0 | Accepted | Tech Lead | 2026-07-08 |
| `docs/adr/010-self-learning-route-store.md` | 1.0 | Accepted | Tech Lead | 2026-07-08 |
| `docs/adr/011-routing-engine-adapter.md` | 1.0 | Accepted | Tech Lead | 2026-07-08 |
| `docs/adr/012-obd-accumulation-recording-state.md` | 1.0 | Accepted | Project owner | 2026-07-10 |
| `docs/adr/013-observer-trip-extraction.md` | 1.0 | Accepted | Project owner | 2026-10-05 |
| `docs/adr/014-gojek-order-card-takeover.md` | 1.0 | Accepted (amended by ADR-015) | Project owner | 2026-10-05 |
| `docs/adr/015-order-auto-start-trip.md` | 1.0 | Accepted | Project owner | 2026-10-06 |
| `docs/IMPLEMENTATION-PLAN.md` | 0.16 | Active (migrated from implementation-plan.md + progress.md) | Product Manager / Tech Lead | 2026-10-06 |
| `docs/UI-SPEC.md` | 0.12 | Active (migrated from product-spec.md + DESIGN_SYSTEM.md + CR-0002) | Product Manager / UX Designer | 2026-10-06 |
| `docs/WORKFLOW.md` | 0.1 | Ready to use | Project Team | 2026-06-15 |
| `docs/IMPLEMENTATION-ISSUES.md` | 0.1 | Ready to use (blocker protocol) | Tech Lead | 2026-06-15 |
| `docs/ERRORS-LOG.md` | 0.1 | Active (persistent) | Project Team | 2026-07-10 |
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
| 2026-07-07 | PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN | 0.4/0.3/0.3/0.4 | 0.5/0.4/0.4/0.5 | Added Fuel Cost feature (FR-12): estimated fuel cost (IDR `Rp`) = litres × user-set price on the Session OBD card + Trips active-trip row; inline tap-to-edit price (Save/Cancel) with multi-step in-memory undo/redo; price persisted in `obd_prefs` (`obd_fuel_price_per_liter`), `ObdUiState.Connected` gains `sessionFuelConsumedL`/`tripFuelConsumedL`. No schema change, no ADR. | Claude Code |
| 2026-07-07 | PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN, adr/008 | 0.5/0.4/0.4/0.5/— | 0.6/0.5/0.5/0.6/1.0 | ADR-008 accepted — fuel price as an effective-dated `fuel_price` entity (migration v8→v9): completed-trip cost priced at trip start (non-retroactive), active-trip card split into two metric rows (instant km/L, L/h, trip-avg km/L, cost). Supersedes the FR-12 v1 'no schema change' note. Code pending. | Claude Code |
| 2026-07-08 | docs/adr/009-dual-mode-track-navigation.md, docs/adr/010-self-learning-route-store.md, docs/adr/README.md | — | 0.1/0.1 | Two new ADRs (Proposed) from a grilling session: ADR-009 dual-mode Track screen (follow-a-route navigation as a sub-mode of a trip — one coupled trip↔navigation lifecycle, manual end, mutable destination, 50 m/2-3-fix/15 s deviation recalc, fail-soft, heading-up "navigation perspective" toggle + directional car marker); ADR-010 self-learning local route store (reject caching Google results on ToS grounds; build a derived, rebuildable route store from the user's own GPS traces — Stage A trace-reuse road-ahead + branches, Stage B routable graph deferred). Routing engine, traffic-vs-static ETA, and ghost-route-via-external-API cost left OPEN. Also fixed a stale ADR index (008 was missing). PRD/ARCHITECTURE/UI-SPEC/IMPLEMENTATION-PLAN propagation deferred to ADR acceptance. | Claude Code |
| 2026-07-08 | adr/011, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN, adr/009, adr/README | —/0.6/0.6/0.7/1.0/— | 1.0/0.7/0.7/0.8/1.0/— | ADR-011 accepted — routing engine via connector-adapter: OpenRouteService hosted free tier now, portable to self-hosted OSM by a base-URL swap (`RoutingEngine` port + `OpenRouteServiceAdapter`); static ETA; ORS/OSM attribution; key not hardcoded. Resolves ADR-009's routing-engine + traffic-ETA open questions. ARCHITECTURE §12 RoutingEngine/adapter; UI-SPEC §3d attribution surface. Remaining parked: when to migrate to self-hosted OSM. | Claude Code |
| 2026-07-08 | PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN, adr/009, adr/010, adr/README | 0.6/0.5/0.5/0.6/0.1/0.1 | 0.7/0.6/0.6/0.7/1.0/1.0 | ADR-009 (dual-mode Track navigation) + ADR-010 (self-learning route store) accepted and propagated. PRD: Feature 6, FR-13/FR-14, BR-12…BR-15, Out-of-Scope turn-by-turn. ARCHITECTURE: derived `known_segment`/CellIndex, MIGRATION_9_10 (DB→v10), new §12. UI-SPEC: §3d nav surfaces (design-handoff-pending). Routing engine, traffic-vs-static ETA, and ghost-route external-API cost left OPEN. Code gated (design handoff §12 + build permission §5a). | Claude Code |
| 2026-07-08 | IMPLEMENTATION-PLAN, docs/design-handoff/track_navigation/TRACK_NAVIGATION_SPEC.md | 0.8/— | 0.9/— | Track Navigation design handoff (AGENTS §12) produced: new `TRACK_NAVIGATION_SPEC.md` (7 UI surfaces, states, screenshot checklist, Claude Design prompt); IMPLEMENTATION-PLAN §1/§6 rows; backfilled the ADR-011 task-log revision (`34ffe0a`). Design-track only; no code. | Claude Code |
| 2026-07-10 | adr/012, ARCHITECTURE, IMPLEMENTATION-PLAN, ERRORS-LOG, adr/README | —/0.7/0.9/0.1/0.1 | 1.0/0.8/0.10/0.1/0.1 | ADR-012 accepted — OBD fuel accumulation gates on shared `isAlwaysRecording` state instead of the intent-synced `sessionActive` flag (fixes ERR-005: instant km/L & L/h show but avg km/L / cost / finished-trip fuel blank when OBD reconnects without a fresh ACTION_SESSION_ON). ARCHITECTURE §5 data-flow note updated (samples written while recording; SESSION_ON/OFF advisory); IMPLEMENTATION-PLAN §6 task row + backfilled the design-handoff `---` → `742f9da`; ERRORS-LOG ERR-005; adr/README index. Code gated (build permission §5a). | Claude Code |
| 2026-10-05 | adr/013 (new), adr/README, PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN, DOCUMENT-CONTROL | —/0.1/0.7/0.8/0.7/0.10/0.1 | 1.0/0.2/0.8/0.9/0.8/0.11/0.1 | ADR-013 accepted — Observer Trip Extraction (Gojek pickup/drop, device-only, no FK to trips/sessions, 90d/5k retention). New ADR-013 file. PRD: Feature 3 updated (Gojek extraction), FR-15 new (trip extraction feature), §8 Included/Out-of-Scope updated. ARCHITECTURE: ObserverTripEntity added to entity list, retention note, MIGRATION_10_11 added to schema section, parser + rule table mentioned in Observer component. UI-SPEC: Observer Trip Card (Gojek) added to screen inventory (design pending). IMPLEMENTATION-PLAN: §4 Observer — Gojek Trip Extraction slice (6-step implementation), §6 Task Log row (docs only, 2026-10-05), §1 Change Log entry, Next Step updated, version 0.10→0.11. adr/README: ADR-013 row added, version 0.1→0.2. DOCUMENT-CONTROL: register + change log updated. Code gated (design handoff §12 + build permission §5a). | Claude Code |
| 2026-10-05 | adr/014 (new), adr/README, PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN | —/0.2/0.8/0.9/0.8/0.11 | 1.0/0.3/0.9/0.10/0.9/0.12 | ADR-014 accepted — Gojek Order Card Takeover of the Track Screen: auto-stop trip + order card on Track screen + one-shot foreground launch when order is complete. New ADR-014 file (Context/Decision/Consequences/Alternatives/References). PRD: FR-16 new (Gojek order-card takeover Gojek-scoped exception to FR-13), §8 Included bullet added, version 0.8→0.9. ARCHITECTURE: new Gojek Order-Card Takeover Flow subsection in §5 (Observer-to-tracking link by signal/intent, Android 10+ fallback note), version 0.9→0.10. UI-SPEC: existing "Observer Trip Card (Gojek)" row in screen inventory changed to "Gojek Order Card (Track screen)" with updated description; new §3e subsection (card states, placement, related behavior) added, version 0.8→0.9. IMPLEMENTATION-PLAN: §4 ADR-013 slice extended with ADR-014 steps 7–10 + verification block; §1 Change Log entry v0.12 added; §6 Task Log row added (docs only, 2026-10-05) + backfilled ADR-013 row revision `b79c4f2`; Next Step updated; version 0.11→0.12. adr/README: ADR-014 row added, version count → 14, version 0.2→0.3. Code gated (design handoff §12 + build permission §5a). | Claude Code |
| 2026-10-05 | adr/014, adr/013, PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN | 1.0/1.0/0.9/0.10/0.9/0.12 | 1.0/1.0/0.10/0.11/0.10/0.13 | Docs-only fixes to ADR-014 and related text: removed invented Android names (permission/error constants) and restated the background-activity-start restriction as to-be-verified on the SM-G965F with a two-item fallback list; replaced the placeholder composable name; corrected the flow (foreground launch done by the Observer service; trip persisted by `ShareViewModel.onTripCtaTap()` before `STOP_TRIP`, auto-stop routed through `ShareViewModel`); migration numbering (ADR-013 takes MIGRATION_9_10 DB→v10, ADR-010 renumbered 10→11 when it ships); UI-SPEC §3e design-handoff spec added. Docs only; no code. | Claude Code |
| 2026-10-05 | IMPLEMENTATION-PLAN, ARCHITECTURE, adr/013, DOCUMENT-CONTROL | 0.13/0.11/1.0/0.1 | 0.14/0.12/1.0/0.1 | Code — Gojek order card takeover (ADR-013/014 extension): OrderCardParser + GojekRules table; observer_trip entity/DAO + MIGRATION_9_10 (adds `handled` column + `lastSeenAt` index); OrderTripRecorder in Observer service write path; ShareViewModel auto-stop (one-shot per order); foreground launch + provisional GojekOrderCard on Track screen. IMPLEMENTATION-PLAN: §4 ADR-013 slice updated with implementation status + known risks; §6 new task-log row (code written, unbuilt, design handoff pending) + backfilled Git Revision for ADR-014 wording fix; §1 Change Log entry v0.14 added. ARCHITECTURE: ObserverTripEntity updated with `handled` field; migration sentence updated (includes `handled` column + `lastSeenAt` index mention). adr/013: Storage point updated with `handled` column description. Code written (static, unbuilt); AGENTS.md §5a build/test/device gated. | Claude Code |
| 2026-10-05 | IMPLEMENTATION-PLAN, UI-SPEC, adr/014 | 0.14/0.10/1.0 | 0.15/0.11/1.0 | Code — fix order-card weaknesses: IMPLEMENTATION-PLAN known risks 1–2 marked FIXED, §6 task-log row added, order-card takeover Git Revision backfilled (`329a02a`); UI-SPEC §3e notes the provisional Dismiss control; adr/014 defaults note dismiss-revert and same-address-new-order rule (wording only). Code unbuilt (AGENTS.md §5a). | Claude Code |
| 2026-10-06 | adr/015, adr/README, PRD, ARCHITECTURE, UI-SPEC, IMPLEMENTATION-PLAN | —/0.3/0.10/0.12/0.11/0.15 | 1.0/0.4/0.11/0.13/0.12/0.16 | ADR-015 accepted — Gojek Order Lifecycle Drives the Trip (auto-start at Taken, auto-end at Cleared/Cancelled/Dismissed). New ADR-015 file (Context/Decision/Consequences/Alternatives/Vocabulary/Evidence/References). adr/README: ADR-015 row + index count 14→15, v0.3→0.4. PRD: FR-13 amended (Gojek exception note), FR-16 rewritten (auto-start/auto-end instead of auto-stop), v0.10→0.11. ARCHITECTURE: Gojek Order-Card Takeover Flow section rewritten (TRIP START + TRIP END phases), terminal-state detection added, v0.12→0.13. UI-SPEC: §3e updated (auto-start/auto-end, card states Cleared/Cancelled/Dismissed, while-active note), v0.11→0.12. IMPLEMENTATION-PLAN: §4 new Observer — Gojek Order Auto-Start/End Trip slice (7 implementation steps + verification); §1 change log entry 0.16 added; §6 task-log rows added (docs 2026-10-06 Completed, code 2026-10-06 In Progress); Also documented line updated; v0.15→0.16. Code gated (build permission AGENTS.md §5a). | Claude Code |

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
