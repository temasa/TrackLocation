# ADR-002: Session Screen as the Single Always-recording Control Surface

**Status:** Accepted
**Date:** 2026-05-16
**Decided By:** Project owner (CR-0002)

> Migrated 2026-06-15 from CR-0002 in the legacy `docs/change-requests.md`. UI details now live in `docs/UI-SPEC.md §3`.

## Context

After ADR-001, the Session screen showed always-recorded sessions and status, but the primary always-recording control was not where the user inspects that status. The control belonged on the Session screen.

## Decision

Place a compact always-recording switch inside the Session screen always-recording status area (trailing/right-side). The Session screen is the primary always-recording control surface; the List screen remains trip-only and does not expose the switch.

Key behaviors:
- Switch controls the same always-recording state introduced in ADR-001.
- Starting a trip while OFF auto-starts always-recording and reflects ON.
- Stopping a trip does not stop always-recording.
- An active trip guards (blocks) turning always-recording OFF.

## Consequences

### Positive
- Control co-located with status; clearer mental model.
- List screen stays focused on trip history.

### Negative
- Active-trip guard adds a state the UI must explain.

## Alternatives Considered
1. **Keep the switch on the List/Trips header:** control divorced from status; rejected.
2. **Add a dedicated nav destination for recording control:** unnecessary fifth tab; rejected.

## Related ADRs
- [[001-always-recorded-sessions]]

## References
- `docs/PRD.md` §12 (BR-07, BR-08)
- `docs/UI-SPEC.md` §3 (states + copy)
- `docs/IMPLEMENTATION-PLAN.md` Appendix A (CR-0002), Appendix B (2026-05-16)
- Implementation commit `fe241a7`
