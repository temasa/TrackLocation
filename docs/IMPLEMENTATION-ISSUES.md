---
name: IMPLEMENTATION-ISSUES.md
path: docs/IMPLEMENTATION-ISSUES.md
description: Blocker protocol and live communication channel — fully generic, ready to use
---

# Implementation Issues

## TrackLocation - Phase 1

**Version:** 0.1  
**Status:** Issue log + blocker protocol  
**Last Updated:** 2026-06-15 11:44:58

---

## Purpose

This document is a **live communication channel** between the implementation agent (Claude Code) and the architect (you) when development discovers contradictions, ambiguities, or edge cases in the locked design docs.

**Use this protocol to avoid:**
- Silent wrong choices (agent guesses on ambiguity)
- Wasted work (agent builds on wrong assumption)
- Rework cycles (architect discovers issue after dev is deep in code)

---

## Protocol

### When to Raise a Blocker

A **blocker** is raised when:
1. Two locked docs contradict each other (e.g., schema says X, API contract says Y)
2. A task requires changing a locked decision (e.g., "schema needs a new field, but Phase 1 is locked")
3. An edge case is not specified (e.g., "what happens if upload fails halfway?")
4. An external dependency is missing (e.g., "API key not configured")

**Do NOT raise a blocker for:**
- Implementation questions (how to structure code, which library, etc.) — agent decides
- Design refinements within scope (adding helper functions, reorganizing files)
- Standard edge cases (null checks, error handling) — agent implements per best practices

---

## Blocker Workflow

### Step 1: Developer (Claude Code) Raises Issue

Create a new section in this file (or append to "Active Issues" below) with:

```markdown
### Issue #N: [Title]

**Sprint/Task:** S#-T#  
**Severity:** Blocker | Warning | Question  
**Status:** Raised | Awaiting Decision | Decided | Resolved  

**Problem:**
[What is blocked? What docs contradict? Quote the relevant sections.]

**Context:**
[Why did this come up now? What task is trying to do?]

**Options:**
1. [Option A] — Pros / Cons
2. [Option B] — Pros / Cons

**Recommendation:**
[Developer's best guess, if any]

**Asked:** 2024-01-15T12:00:00Z  
**Asking:** [Developer name / Claude Code version]  
```

Then:
```bash
git add docs/IMPLEMENTATION-ISSUES.md
git commit -m "[doc-issue] S#-T#: [Short description]"
# HALT — do not continue implementation
```

### Step 2: Architect Reviews & Decides

Review the issue, discuss with team if needed, then decide. Respond by updating the issue:

```markdown
**Decision:**
[Which option? Why?]

**Action:**
- [ ] Update docs/[FILE].md to clarify [CHANGE]
- [ ] Developer resume at [NEXT STEP]

**Decided:** 2024-01-15T13:00:00Z  
**Deciding:** [Architect name]  
**Decision Commit:** [git hash]
```

Then:
```bash
# Make doc updates if needed
git add docs/IMPLEMENTATION-ISSUES.md docs/[AFFECTED_FILE].md
git commit -m "[doc-decision] Issue #N: [Brief outcome]

[Full explanation of decision]
"
```

### Step 3: Developer Resumes

Pull decision commit, verify docs are updated, resume from the specified next step:

```bash
git pull
# Verify docs are consistent
git log --oneline -2  # See decision commit
# Resume work
```

---

## Active Issues

### Issue #1: OBD Phase 2 Slice 2 — no live trip id to accumulate fuel into

**Sprint/Task:** OBD-P2-S2 (trip half — plan Slice 2 steps 2–3)
**Severity:** Blocker
**Status:** Resolved

**Problem:**
IMPLEMENTATION-PLAN.md Slice 2 steps 2–3 say:
> add `var activeTripId` … listen for `ACTION_TRIP_START`/`ACTION_TRIP_STOP` sent by `TrackingService`; on start receive `tripId` and set `activeTripId` … `TrackingService` … when a trip starts, `sendBroadcast(Intent(ACTION_TRIP_START).putExtra("tripId", tripId))`.

This assumes a persisted trip row (with an id) exists while a trip is active. In the actual codebase it does not:
- `TrackingService.startTrip()` / `stopTrip()` never write a `track` row; a trip lives only in `TrackingService.locationUiState` (in-memory) during recording.
- The `track` row is created **at trip stop** by `ShareViewModel.onTripCtaTap()` via `insertTrack(...)`, with an **auto-generated `idx`** (`@PrimaryKey(autoGenerate = true)`).
- Therefore there is no stable `track.idx` during an active trip to pass to `TrackDao.addObdFuel(idx, …)`.

Giving an active trip a live DB id means changing **when/where trip rows are persisted** — locked trip behavior (AGENTS.md §2/§13; do not invent per §3/§9).

**Context:**
The session half of Slice 2 (persisting `recording_session.obdFuelConsumedL`/`obdGpsDistanceKm`) is fully implementable and has been implemented, because `recording_session` rows exist live with a known `String` id. Only the trip half is blocked. The Slice 1 column `track.obdFuelConsumedL` already exists.

**Options:**
1. **Insert the `track` row at trip start** (move/duplicate trip persistence into `TrackingService`, keep a stable `activeTripIdx`, finalize on stop; broadcast the idx to OBD). — Pros: matches plan; live incremental accumulation; trip row exists during trip. Cons: changes locked trip lifecycle; risk of empty/orphan trip rows on mid-trip kill (needs reaper); moves creation away from `ShareViewModel`.
2. **Derive trip fuel from `obd_sample` time-window** (no live trip id). Live trip-avg (Slice 4) queries `obdSampleDao.samplesBetween(tripStartMs, now)` and integrates fuel; write the total into `track.obdFuelConsumedL` at stop when `ShareViewModel` creates the row. — Pros: no change to trip lifecycle; reuses persisted samples; matches the original OBD Phase 1 Slice 4 approach; keeps the Slice 1 column meaningful. Cons: column populated at stop rather than incrementally; live avg is a windowed query.
3. **Defer trip accumulation to Slice 4 only**, compute trip-avg in the UI from `obd_sample`, leave `track.obdFuelConsumedL` unused for now. — Pros: smallest change, unblocks immediately. Cons: Slice 1 column unused; postpones the decision.

**Recommendation:** Option 2 — avoids changing locked trip-persistence behavior, reuses the `obd_sample` rows already written during a session, still delivers a live trip-average in Slice 4, and populates `track.obdFuelConsumedL` at trip stop so the column stays meaningful.

**Asked:** 2026-07-02
**Asking:** Claude Code (Opus 4.8)

**Decision:** Option 2 above — **derive trip fuel from `obd_sample`** (no change to trip-persistence lifecycle). Chosen by user 2026-07-02.

**Action taken (2026-07-02, static — unbuilt per AGENTS.md §5a):**
- Added `ObdSampleDao.samplesBetweenOnce(startMs, endMs): List<ObdSampleEntity>` (one-shot suspend variant of `samplesBetween`).
- `ShareViewModel.onTripCtaTap()` now integrates fuel over the trip window `[tripStartedAt, now]` at trip stop (`integrateFuelLiters(...)`, same `0 < dt < 60 s` guard as the service) and writes the total into `TrackEntity.obdFuelConsumedL` when the trip row is created. Injected `obdSampleDao` into `ShareViewModel` + factory. `0.0` when OBD was not connected (no samples).
- Live trip-average km/L **display** is deferred to Slice 4 (Trip screen UI), which will query the same window; that surface is UI and routes through claude.ai/design first.

**Decided:** 2026-07-02
**Deciding:** User (rinaldi.ch@gmail.com)

---

## Resolved Issues

*(Will be populated as implementation proceeds)*

### Template for historical record:

```markdown
### Issue #1: Example Resolved Issue [RESOLVED]

**Sprint/Task:** S1-T1  
**Severity:** Question  

**Problem:**
[Original question]

**Decision:**
[How it was resolved]

**Decided:** 2024-01-15T10:00:00Z  
**Decision Commit:** abc123def456  
```

---

## Communication Rules

**For Developer (Claude Code):**
- Be specific: quote the conflicting doc sections, not "the schema is confusing"
- Provide options: don't just say "this is wrong", suggest resolutions
- Halt immediately: do not work around ambiguity; raise and wait

**For Architect:**
- Respond within 1 work day (or document reason for delay)
- Update the issue with explicit decision + action items
- Commit decision to git (make it part of history)
- If decision requires code review, note it in decision comment

---

## Examples of Blockers vs. Non-Blockers

### ✅ RAISE A BLOCKER

**Blocker:**
```
Issue #1: Data model contradiction

S2-T3 is building the database schema. IMPLEMENTATION-PLAN.md says:
  "9 tables with the following structure..."

But PRD.md §6 says:
  "Content is stored as JSON without schema validation."

QUESTION: Should we use a normalized schema or flexible JSON storage?
PRD §6 is locked; IMPLEMENTATION-PLAN says normalized. This is a fundamental design decision.
```

**Why raise:** Two locked docs contradict on a fundamental design choice.

---

### ❌ DO NOT RAISE A BLOCKER

**Not a blocker (developer decides):**
```
"Should I use UUID or autoincrement IDs for primary keys?"
```
**Why:** Implementation detail, no design doc restriction.

**Not a blocker (best practices):**
```
"What validation should I add for email addresses?"
```
**Why:** Standard engineering, not a design ambiguity.

**Not a blocker (edge case):**
```
"What happens if a user deletes their account?"
```
**Why:** Phase 1 is not covering account deletion; default is soft-delete (standard).

---

## FAQ

**Q: How long does a blocker typically take to resolve?**
A: 1-4 hours. Architect reviews, discusses if needed with team, updates docs, commits. Developer should expect to wait (schedule other work in parallel).

**Q: What if the architect doesn't respond within 24 hours?**
A: Escalate (message architect directly, don't wait). Document the escalation in the issue.

**Q: Can I proceed if I think I know the answer?**
A: No. Even if you're 90% sure, raise it. The 10% risk of rework is worse than 1 hour of waiting.

**Q: What if a blocker affects multiple tasks?**
A: Raise it as soon as discovered (first task that hits it). Resolution applies to all downstream tasks.

**Q: Can I edit this file directly or only via blocker protocol?**
A: Only via blocker protocol + architect decision. This keeps history clean.

---

## Related Documents

- **IMPLEMENTATION-PLAN.md** — Sprint breakdown; see §11 for dependencies & risks
- **PRD.md** — Product requirements (source of truth)
- **AGENTS.md** — §9 summarizes the blocker protocol and points back to this file

---

**Last reviewed:** 2026-06-15 11:44:58 (consistent, no open issues)
