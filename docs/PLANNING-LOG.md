---
name: PLANNING-LOG.md
path: docs/PLANNING-LOG.md
description: Temporary planning log — tracks active feedback and work during Claude desktop planning sessions
---

# Planning Log
## TrackLocation

**Document Version:** 0.1  
**Status:** Active (temporary)  
**Last Updated:** 2026-07-08  
**Owned By:** Planning/Design Session

---

## Purpose

This log is a **temporary working document** for tracking active feedback and planning work during Claude desktop sessions. It serves as:

1. **Session continuity** — When a planning session closes or is interrupted, the next session can resume from where work left off.
2. **Active feedback tracker** — Shows which feedback items are currently being worked on and their status.
3. **Work-in-progress snapshots** — Captures interim decisions and pending items before they're finalized.

---

## Lifecycle

- **Created** — When planning begins in Claude desktop.
- **Updated** — Throughout the planning session as feedback is worked on.
- **Deleted** — Once all feedback items are resolved and closed.

---

## How to Use

### Active Feedback Items

Track each open feedback item with its status:

| ID | Title | Status | Owner | Last Updated | Notes |
|----|-------|--------|-------|--------------|-------|
| NAV-01 | Dual-mode Track: follow-a-route navigation (ADR-009) | pending-review | Planning Session | 2026-07-08 | Behavior tree settled; routing engine + traffic-ETA + ghost-cost OPEN; UI needs design handoff (§12) |
| NAV-02 | Self-learning local route store (ADR-010) | pending-review | Planning Session | 2026-07-08 | Idea 1 (cache Google) rejected on ToS; Idea 2 adopted staged (A now, B later) |

**Status options:**
- `open` — Identified but not yet started
- `in-progress` — Currently being worked on
- `pending-review` — Work complete, awaiting review
- `closed` — Resolved and finalized

### Session Notes

Use this section to track interim decisions, blockers, and context for the next session:

- **Last session ended at:** (date and time)
- **Current context:** (what was being worked on)
- **Blockers:** (any obstacles to resume)
- **Next steps:** (what to do when resuming)

---

## Session Notes

**Last session ended at:** 2026-07-08
**Current context:** Grilling session on dual-mode Track navigation + a self-learning route store to cost-optimize routing APIs. Two ADRs written (009, 010), both Proposed.
**Blockers:** Owner parked the routing-engine choice (Google vs free hosted OSM vs self-hosted OSM), traffic-aware vs static ETA, and the always-on ghost-route external-API cost.
**Next steps:** On ADR-009/010 acceptance, propagate to PRD §12 (new capability), ARCHITECTURE (derived route store, LocationUiState heading field), IMPLEMENTATION-PLAN §4/§6 (slices), and UI-SPEC + a design handoff for the search field / Start control / ETA placement / perspective toggle / car marker. Resolve the parked routing-engine question.

---

**This document is temporary. Delete once all feedback is resolved.**
