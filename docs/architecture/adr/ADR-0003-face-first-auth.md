# ADR-0003 — Face-first Authentication With Google SSO Fallback

Date: 2026-05-16 13:52:51 +07:00

## Status

Accepted from Accessibility Observer PRD v5

## Context

The app may run on shared driver devices. It needs fast presence/identity verification without interrupting background capture and sync.

## Decision

Face authentication is primary after registration. Google SSO is identity registration first and automatic fallback only after face auth cannot complete or fails under defined conditions.

Auth gates UI only. GPS tracking, Observer capture, and sync continue in the background.

## Consequences

Positive:

- Better shared-device accountability.
- Less chance users bypass face verification.
- Background data continuity is preserved.

Tradeoffs:

- Requires CameraX + face processing.
- Requires careful fallback and lockout UI.
- Requires privacy-safe face data handling.
