# ADR-0002 — Observer Capture Scope Uses Denylist, Not Allowlist

Date: 2026-05-16 13:52:51 +07:00

## Status

Accepted from Accessibility Observer PRD v5

## Context

The Observer feature is intended to help identify which accessibility events and UI trees are emitted by apps on the device. A single target allowlist would prevent discovery of unknown package/activity/event behavior.

## Decision

Capture all packages except configurable high-noise system packages.

Package filtering is a UI/query concern, not a capture-layer setting.

## Consequences

Positive:

- Developer can switch between target apps without losing events.
- Unknown package/activity behavior remains discoverable.
- UI filtering is instant and reversible.

Tradeoffs:

- Requires good default denylist.
- Requires local filtering/search to manage event volume.
- Requires retention/sync safeguards.
