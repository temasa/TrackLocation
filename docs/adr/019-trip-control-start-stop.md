# ADR-019: Trip Control Is Start/Stop Only (READY Panel Shows Zeros)

**Status:** Rejected
**Date:** 2026-10-07
**Decided By:** Project owner

## Context

On-device verification (2026-10-07, SM-G965F) found three defects on the Track screen trip panel:
- The LIVE button is labelled "Pause trip" with a pause glyph, but it sends `STOP_TRIP`.
- PAUSED can never be entered: nothing sets `isPaused`, and resume was deferred as a "future phase".
- The READY panel showed the previous trip's elapsed time and distance.

## Proposal

Make the trip control Start and Stop only (stop glyph, "Stop trip" label), remove the PAUSED state, and zero the READY panel.

## Decision

**Rejected.** The owner chose the remote Track design (UI-SPEC §3g): READY hides the TripPanel and shows the PlayFab; LIVE/PAUSED show the TripPanel with an inline pause/resume CTA; the metrics row is merged.

## Consequences

- Pause/resume behaviour remains an open gap, tracked as Issue #2 in `IMPLEMENTATION-ISSUES.md` ("Trip panel LIVE button is labelled/iconed Pause but sends STOP_TRIP; PAUSED unreachable").
- The READY-stale-values symptom is moot while the READY panel is hidden.

## References

The implementation was built and verified on device, then reverted. It is recoverable from history: 168ef8e (docs), 47f9258 (code), 8d68895 (pen frame), 45a32b0 (task log).
