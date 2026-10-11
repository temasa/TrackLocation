# ADR-028: Observer Service-Off Banner on the Observer Feed

**Status:** Accepted
**Date:** 2026-10-11
**Decided By:** Project owner + Claude Code

## Context

On 2026-10-11 the Observer accessibility service was silently disabled from about 07:22 WIB. Android removes an accessibility service from the enabled list (`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`) on force-stop or on reinstall. Nothing in the app told the user. Two Gojek orders were not captured, and the Track auto-takeover (ADR-014, ADR-015) never fired because the order card was never read.

The Observer Feed already shows a "live" chip, but that chip only reflects the capture-switch preference. It does not reflect whether the system is delivering accessibility events. So the screen looked healthy while nothing was being captured.

Constraints:
- The app cannot re-enable its own accessibility service. Turning it on needs the user to act in system Settings (ARCHITECTURE §5, "Android AccessibilityService: user enables in system Accessibility settings; app deep-links and reflects status only").
- A dead or unbound service cannot report its own state, so the check must not depend on the service being alive.
- No new permission, no schema change, no change to GPS tracking or to the locked rules in PRD §12.
- Compose 1.2-compatible APIs only (Kotlin 1.7.0).

## Decision

1. **Detection.** The app reads `Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES` (a colon-separated list of flattened `ComponentName` strings) and reports the Observer as enabled when the list contains the Observer service. The match accepts both forms of the same component:
   - `com.kolee.tracklocation/.observer.ObserverAccessibilityService` (short form)
   - `com.kolee.tracklocation/com.kolee.tracklocation.observer.ObserverAccessibilityService` (full form)

   Each entry is parsed with `ComponentName.unflattenFromString` and compared case-sensitively. The check reads the system setting only. It does **not** require the service to be alive or bound.
2. **Refresh.** The state is re-read on the Observer Feed screen's `ON_RESUME` lifecycle event, so the banner updates when the user returns from system Settings. The state is held in `ObserverUiState` as a `serviceEnabled` flag and refreshed by `ObserverViewModel.refreshServiceState()`.
3. **Banner.** When the service is **not** enabled, the Observer Feed shows an amber banner (same visual language as the existing `CapturePausedBanner`, UI-SPEC §3c-svc):
   - Placement: below the capture-chip / auto-scroll row and above the filter bar.
   - Copy: "Observer service is OFF – orders won't be captured. Tap to enable."
   - Tap action: opens `Settings.ACTION_ACCESSIBILITY_SETTINGS`.
   - Hidden when the service is enabled.
   - Independent of the capture-switch banner. Both can show at the same time.
   - Implemented as a new composable `ServiceOffBanner` under `feature/observer/presentation/components/`, modelled on `CapturePausedBanner`.
4. **No other behaviour changes.** The capture switch, the allowlist, the feed contents and the Observer's capture logic are unchanged. The banner only reports; it never changes the service or the preference.
5. **No schema change, no new permission, no PRD or ARCHITECTURE change.** No external design handoff: the owner explicitly skipped it, and the banner reuses the existing `CapturePausedBanner` style.

## Consequences

### Positive
- A silent loss of capture is now visible on the screen where the user looks for it, with one tap to fix it.
- The check reads system state, so it works even when the service process is dead, which is exactly the failure case.
- No new permission, no migration, and no change to GPS or to the trip and order logic.

### Negative
- The banner tells the user the service is off but cannot turn it on. The user must still act in system Settings.
- The check runs only while the Observer Feed is in the foreground (on `ON_RESUME`). If the service is disabled while the feed is open and the app is never backgrounded, the banner refreshes only on the next resume. This is acceptable because the feed is a diagnostic screen, not a live alarm.
- Reading `ENABLED_ACCESSIBILITY_SERVICES` relies on a documented system setting. A future Android change to how this list is stored would need a code update.

## Alternatives Considered

1. **Bind-state reported by the service itself (for example, a heartbeat or `onServiceConnected` flag).**
   - Pros: reflects the real running state.
   - Cons: a dead or unbound service cannot report that it is dead. The failure case in this incident is exactly the case where the service does not run. Rejected.
2. **Foreground notification while the service is off.**
   - Pros: visible even outside the Observer Feed.
   - Cons: needs a notification channel and noise for a diagnostic feature; a persistent notification is an extra user-visible surface that the PRD does not ask for. Rejected for this version.
3. **Auto re-enable the service from the app.**
   - Pros: no user step.
   - Cons: not possible without the `WRITE_SECURE_SETTINGS` permission, which a normal app cannot be granted. Rejected. (The manual mitigation used on 2026-10-11 was an `adb shell settings put secure` command, which is not a product path.)

## Related ADRs

- **ADR-013** — Gojek order extraction: the source of the orders that were missed.
- **ADR-014 / ADR-015** — Order card takeover and auto-start trip: the behaviour that did not fire because the service was off.
- **ADR-002** — Capture-switch placement. The capture switch is unchanged; this banner is independent of it.

## References

- ARCHITECTURE §5 (AccessibilityService: user enables in system settings; app reflects status only)
- UI-SPEC §3c-svc (Observer service-off banner)
- IMPLEMENTATION-PLAN §4 slice "Observer Service-Off Banner (ADR-028)"
- ERRORS-LOG ERR-010 (2026-10-11 incident)
