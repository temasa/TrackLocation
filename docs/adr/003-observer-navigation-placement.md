# ADR-003: Observer Navigation Placement (Option B — under Settings/Tools)

**Status:** Accepted
**Date:** 2026-05-18
**Decided By:** Project owner

> Migrated 2026-06-15 from the navigation decision recorded in `product-spec.md` and `implementation-plan.md`.

## Context

The Accessibility Observer needed a home in the app's information architecture without disrupting the accepted four-tab baseline (`Session / List / Track / Settings`).

## Decision

Adopt **Option B**: Observer is accessed from `Settings → Tools → Observer`. Bottom navigation remains four tabs. A future CR may revise this, but Observer implementation proceeds under this placement.

## Consequences

### Positive
- Preserves the 4-tab baseline; minimal churn.
- Groups developer/support tools (Observer, OBD) under Settings.

### Negative
- Observer is less discoverable than a first-class tab.

## Alternatives Considered
1. **Option A — Session / List / Track / Observer / Settings:** Observer first-class, but five tabs crowd the bar. Rejected.
2. **Option C — GPS / Track / Observer / Settings (Session/List inside GPS):** cleaner long-term IA but requires a larger redesign. Deferred.

## Related ADRs
- [[004-obd-raw-at-io]] (the other Settings/Tools feature)

## References
- `docs/UI-SPEC.md` §2
- `docs/IMPLEMENTATION-PLAN.md` Appendix A (Observer Rollout)
