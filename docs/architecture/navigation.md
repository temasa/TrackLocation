# Navigation Specification

Last updated: 2026-05-16 13:52:51 +07:00

## Purpose

This file is the navigation source of truth. When any PRD, CR, UI spec, or rollout plan mentions bottom navigation, this file has priority for the current accepted app shell.

## Current accepted main navigation

After CR#1, the accepted main bottom navigation is:

```text
1. Session
2. List
3. Track
4. Settings
```

## Destination meanings

### Session

Shows always-recorded ON-to-OFF location sessions. Sessions are not trips.

### List

Shows explicit trips only. The List header owns the always-recording switch.

### Track

Starts and stops explicit trip ranges inside the canonical location log. Stopping a trip does not stop always-recording.

### Settings

Unified operational settings destination.

## Previous / superseded navigation assumptions

Older Observer planning documents proposed:

```text
Trips / Track / Observer / Settings
```

This is superseded for the current app baseline by CR#1. The Observer feature remains planned, but its final navigation placement is unresolved.

## Observer navigation options to decide later

| Option | Navigation | Pros | Cons |
|---|---|---|---|
| A | Session / List / Track / Observer / Settings | Observer stays first-class | Five bottom tabs may feel crowded |
| B | Session / List / Track / Settings, Observer under Settings/tools | Keeps four tabs | Observer is less discoverable |
| C | GPS / Track / Observer / Settings, with Session/List inside GPS | Cleaner long-term information architecture | Requires redesign of existing CR#1 nav |

## Rule

Do not implement Observer bottom navigation until this decision is explicitly accepted in a future CR or architecture decision record.
