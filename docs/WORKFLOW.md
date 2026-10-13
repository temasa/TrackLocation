---
name: WORKFLOW.md
path: docs/WORKFLOW.md
description: Human-facing development workflow guide — references AGENTS.md for Claude's rules, no duplication
---

# Development Workflow
## TrackLocation

**Document Version:** 0.2  
**Last Updated:** 2026-10-13 (§3 design handoff replaced by Pencil MCP / ui-design.pen)

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

The design tool is set by **Design tool** in `CLAUDE.md`: Pencil MCP, editing the project design file `ui-design.pen`. The external design handoff (Google Stitch / Claude Design / Figma) is retired (owner directive, 2026-10-13). `.pen` files are encrypted, so Claude accesses them only through the Pencil MCP tools.

```
1. You: describe the component and which screen
2. Claude: draws or edits the screen as a frame in ui-design.pen (Pencil MCP)
   — edits the existing frame; does not duplicate or regenerate it from scratch
3. You: review the frame in ui-design.pen
4. Iterate until satisfied
5. You: "Approved" or "Finalize" ← Claude waits for this explicitly
6. Claude: records the frame name(s) as the visual source of truth in docs/UI-SPEC.md
7. Claude: implements in code to match the approved pen frames + builds
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
| Regenerating a screen from scratch instead of editing it in ui-design.pen | Edit the existing frame — regenerating creates a duplicate and loses prior work |
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
