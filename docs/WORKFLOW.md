---
name: WORKFLOW.md
path: docs/WORKFLOW.md
description: Human-facing development workflow guide — references AGENTS.md for Claude's rules, no duplication
---

# Development Workflow
## TrackLocation

**Document Version:** 0.1  
**Last Updated:** 2026-06-15 11:44:58

Human-facing guide for working with Claude across the full development cycle. Claude's behavioral rules (audit log format, commit format, git safeguard, blocker protocol) live in `AGENTS.md` — this doc references them rather than repeating them.

---

## 1. Product Workflow

### PRD → Plan → Sprint → Ship

```
1. Write / update docs/PRD.md
2. Create sprint tasks in docs/IMPLEMENTATION-PLAN.md
3. Tell Claude: "Start sprint N"
4. Claude implements task by task
   — after each task: marks Completed in IMPLEMENTATION-PLAN.md §6 (with verification) + commits
   — on blocker: halts and writes to docs/IMPLEMENTATION-ISSUES.md
5. You resolve any blockers, Claude resumes
6. Sprint done → sprint-close commit → next sprint
```

**Resuming after session reset:**
> *"Resume from where we left off"*

Claude reads `docs/IMPLEMENTATION-PLAN.md` to find the last completed task and continues.

---

## 2. When to Update Docs (Decision Workflow)

See AGENTS.md §8b for the full protocol. **Trigger language:**

- "Add X" / "I want to implement X" / "Let's do X"
- Post-grill-me ("OK, I'm going with this")
- Any UI, UX, or architectural decision made verbally

**Claude will:**
1. Show you impact analysis (which docs change, what sections)
2. Wait for your approval
3. Update all affected docs
4. Confirm before proceeding to code

---

## 3. UI Design Workflow

Design mode is set by **Design tool** in `CLAUDE.md`. Two paths:

### Mode A — Claude as designer (no external tool needed)

Claude renders mockups inline using HTML/SVG (`show_widget` or `/prototype` skill).

```
1. You: describe the screen or component
2. Claude: renders a visual mockup inline in the chat
3. You: give feedback — iterate until satisfied
4. You: "Approved" or "Finalize" ← Claude waits for this explicitly
5. Claude: records the agreed design in docs/UI-SPEC.md
6. Claude: implements in code + builds
   — after task: marks Completed in IMPLEMENTATION-PLAN.md §6 (with verification) + commits
```

### Mode B — External design tool (Google Stitch, Figma, etc.)

Claude proposes/edits designs in the chosen tool; the link is recorded in `docs/UI-SPEC.md §8`.

```
1. You: describe the component and which screen
2. Claude: proposes/edits the design in your chosen tool
   — edits the existing screen; does not regenerate it from scratch
3. You: review in the design tool (link recorded in UI-SPEC.md §8)
4. Iterate until satisfied
5. You: "Approved" or "Finalize" ← Claude waits for this explicitly
6. Claude: updates docs/UI-SPEC.md with the agreed design
7. Claude: implements in code + builds
   — after task: marks Completed in IMPLEMENTATION-PLAN.md §6 (with verification) + commits
```

**Resuming after session reset:**
> *"Continue UI work — load UI-SPEC.md"*

---

## 4. Git Safeguard Workflow

Before starting any task that modifies files, create a snapshot of your work:

```bash
git stash push -u -m "pre-task: <description>" && git stash apply
```

**Why:** This backs up any unfinished work so it survives if the task fails. See `AGENTS.md` §7 for full protocol.

---

## 5. Common Mistakes

| Mistake | Fix |
|---------|-----|
| Regenerating a screen from scratch instead of editing it (Mode B) | Edit the existing screen — regenerating creates a duplicate and loses prior work |
| Not naming which screen to edit | Always say the screen name |
| Leaving a task uncommitted | Every task ends with a commit — see AGENTS.md §5 |
| Resolving a blocker inline | Write it to IMPLEMENTATION-ISSUES.md first — see AGENTS.md §9 |
| Not saying "approved" on a design or decision | Claude won't document or implement until explicit confirmation |
| Updating only one doc when decision affects many | Show impact analysis first, update all affected docs together |
| Vague verification notes ("it works") | Be specific: "npm test passes", "tested in browser", "Docker builds" |

---

## 6. Reference Documents

| Doc | Owns |
|-----|------|
| `AGENTS.md` | Claude's behavioral rules — commit discipline, git safeguard, blocker protocol, error logging |
| `docs/ARCHITECTURE.md` | System architecture, domain model, tech stack, integrations, security |
| `docs/adr/` | Architecture Decision Records (decisions, alternatives, consequences) |
| `docs/PRD.md` | Product requirements and scope |
| `docs/IMPLEMENTATION-PLAN.md` | Sprint plan, task log (with verification), commit format |
| `docs/UI-SPEC.md` | UI design decisions and prototype reference |
| `docs/IMPLEMENTATION-ISSUES.md` | Blocker communication channel |
| `docs/ERRORS-LOG.md` | Persistent error learning log |
