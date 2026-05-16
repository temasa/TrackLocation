# TrackLocation Documentation Source of Truth

Refactored on: 2026-05-16 13:52:51 +07:00

This documentation set reorganizes the existing TrackLocation documents so product changes, architecture decisions, UI requirements, implementation tasks, and source-code edits are easier to control.

## Source-of-truth hierarchy

1. `docs/product/` — stable product behavior and locked PRD rules.
2. `docs/change-requests/` — chronological product changes, impact analysis, and acceptance state.
3. `docs/architecture/` — current accepted technical structure after accepted CRs are absorbed.
4. `docs/ui/` — current and planned screen specifications.
5. `docs/implementation/` — rollout plan, source-code change manifest, migrations, and test obligations.
6. `docs/status/` — historical progress and status reports. Do not treat this as the primary source of truth.
7. `docs/archive/original/` — unchanged original uploads preserved for audit and comparison.

## Rule for future CRs

Every accepted CR must update these layers in order:

```text
CR document
  -> architecture docs if behavior/model/navigation changes
  -> UI docs if screens/interactions change
  -> implementation source-change manifest
  -> migration/test plans if required
  -> status/progress after the work is completed
```

## Current baseline after refactor

The accepted CR#1 baseline is:

* Main bottom navigation: `Session / List / Track / Settings`.
* Sessions are always-recorded ON-to-OFF location ranges.
* Trips are explicit ranges over a canonical append-only location log.
* Observer remains a planned feature from the Accessibility Observer PRD and requires a later navigation integration decision.

## Start here

* Product overview: `docs/product/product-baseline.md`
* Current navigation truth: `docs/architecture/navigation.md`
* Current data model truth: `docs/architecture/data-model.md`
* Source-code change control: `docs/implementation/source-change-manifest.md`
* Current UI truth: `docs/ui/screen-specification.md`



## Inspiration

https://youtu.be/LND9Cjc7CRM



