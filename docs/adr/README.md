---
name: ADR README
path: docs/adr/README.md
description: Architecture Decision Records index and template
---

# Architecture Decision Records (ADRs)

This directory contains records of significant architectural decisions made during the development of TrackLocation. Each ADR documents the context, decision made, consequences, and alternatives considered.

---

## Purpose

ADRs serve three functions:

1. **Decision Log:** Record why architectural choices were made, not just what they are
2. **Future Reference:** When someone asks "why did we do this?", the answer is here
3. **Onboarding:** New team members understand the reasoning behind system design

---

## When to Create an ADR

Create an ADR when:
- Choosing between two significant technical approaches
- Making a decision that affects multiple components
- Adopting a new technology or framework
- Establishing a new pattern or convention

---

## Format

Each ADR follows this template:

```markdown
# ADR-NNN: [Title of Decision]

**Status:** Proposed | Accepted | Superseded  
**Date:** YYYY-MM-DD  
**Decided By:** [Name]

## Context
What problem or question prompted this decision? What constraints exist?

## Decision
What did we decide? Be clear and specific.

## Consequences
What are the implications of this decision?

### Positive
- ...

### Negative
- ...

## Alternatives Considered
1. **Option A:** ...
   - Pros: ...
   - Cons: ...

2. **Option B:** ...
   - Pros: ...
   - Cons: ...

## Related ADRs
Links to other related decisions (if any).

## References
Links to docs, code, or external resources supporting this decision.
```

---

## How to Add an ADR

1. Create a new file: `docs/adr/NNN-title-in-kebab-case.md`
2. Use the template above
3. Set status to "Proposed"
4. Add row to the index below
5. Commit with message: `docs: propose ADR-NNN [title]`
6. After review and acceptance, update status to "Accepted"

---

## ADR Index

| # | Title | Status | Date |
|---|-------|--------|------|
| [001](001-always-recorded-sessions.md) | Always-recorded Location Sessions + Canonical Location Log | Accepted | 2026-05-16 |
| [002](002-session-recording-switch.md) | Session Screen as the Single Always-recording Control Surface | Accepted | 2026-05-16 |
| [003](003-observer-navigation-placement.md) | Observer Navigation Placement (Option B) | Accepted | 2026-05-18 |
| [004](004-obd-raw-at-io.md) | OBD-II via Raw AT I/O + Indirect Speed-Density Fuel | Accepted | 2026-06-07 |
| [005](005-observer-fts.md) | Observer Full-Text Search via Room @Fts4 External-Content | Proposed | 2026-07-06 |
| [006](006-location-dwell-collapse.md) | Location Dwell Collapse (canonical log stores one anchor per stop) | Accepted | 2026-07-07 |
| [007](007-unified-fuel-economy-metrics.md) | Unified Fuel-Economy Metrics (instant two-cell + single averaging derivation) | Accepted | 2026-07-07 |
| [008](008-fuel-price-effective-dated-entity.md) | Fuel Price as an Effective-Dated Entity | Accepted | 2026-07-07 |
| [009](009-dual-mode-track-navigation.md) | Dual-mode Track Screen — Follow-a-Route Navigation | Proposed | 2026-07-08 |
| [010](010-self-learning-route-store.md) | Self-learning Local Route Store | Proposed | 2026-07-08 |

---

## ADR Lifecycle

- **Proposed:** New ADR, not yet accepted
- **Accepted:** Decision made and approved; impacts architecture going forward
- **Superseded:** Decision was made but later replaced (link to new ADR); keep for historical context

---

## Key Rule: Claude Creates ADRs During Implementation

**Governance note:** When Claude (the AI assistant) makes significant architectural decisions during implementation, Claude documents them as ADRs. ADRs start as "Proposed"; you review and update status to "Accepted" when approved.

This captures decision rationale in real-time during development, reducing post-hoc documentation burden.

---

**Last Updated:** 2026-07-08
