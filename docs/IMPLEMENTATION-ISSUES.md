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

*(None currently — design is scaffolded and ready for development)*

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
