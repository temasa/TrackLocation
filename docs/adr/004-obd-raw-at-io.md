# ADR-004: OBD-II via Raw AT I/O (no kotlin-obd-api) + Indirect Speed-Density Fuel

**Status:** Accepted
**Date:** 2026-06-07 (raw AT I/O); 2026-06-08 (speed-density fuel)
**Decided By:** Claude Code (implementation) + project owner

> Migrated 2026-06-15 from the OBD Phase 1 pre-check and live-verification sessions in `implementation-plan.md` / `progress.md`.

## Context

OBD Phase 1 targets ELM327 Bluetooth Classic adapters. The candidate library `kotlin-obd-api` is binary-incompatible with the project's Kotlin 1.7.0 ("metadata 2.3.0/2.1.0, expected 1.7.1"). Separately, the test vehicle (1193 cc gasoline, ISO 15765-4 CAN) exposes **no MAF (0110) and no direct fuel-rate (015E) PID**, so km/L cannot be derived directly.

## Decision

1. **Skip `kotlin-obd-api`; implement raw AT command I/O** over the Bluetooth RFCOMM/SPP socket: ELM327 init (`ATZ/ATE0/ATL0/ATSP0`), Mode-01 PIDs (`010C` RPM, `010D` speed), manual response parsing. One serialized connection coroutine (single-flight) to avoid socket races.
2. **Fuel-rate fallback chain:** `DIRECT(015E) → MAF(0110) → SPEED_DENSITY → UNAVAILABLE`. For no-MAF vehicles, estimate MAF via speed-density from MAP/IAT/RPM + a user-set engine displacement (default 1193 cc).

## Consequences

### Positive
- Works on Kotlin 1.7.0 with no incompatible dependency.
- km/L available even on vehicles lacking MAF/fuel-rate PIDs (~1.08 L/h idle verified realistic).
- Single serialized lifecycle eliminated concurrent-connection socket failures (secure SPP connects first try).

### Negative
- Manual AT parsing assumes standard ELM327 response formatting; clones may vary (mitigated by secure→insecure→reflection fallback).
- Speed-density fuel is an estimate (VE=0.85, gasoline constants), not a measured value.

## Alternatives Considered
1. **Use kotlin-obd-api:** rejected — binary-incompatible with Kotlin 1.7.0.
2. **Report fuel UNAVAILABLE on no-MAF vehicles:** simpler but leaves km/L blank on the test vehicle. Rejected in favor of the estimate.

## Related ADRs
- [[003-observer-navigation-placement]]

## References
- `docs/ARCHITECTURE.md` §4, §8 (fuel formula)
- `docs/IMPLEMENTATION-PLAN.md` Appendix A (OBD Phase 1), Appendix B (2026-06-07/08)
- Commits `3cd1fa8` (pre-check), `bd13fb3` (Slice 3), `211ef56` (live fixes + speed-density)
