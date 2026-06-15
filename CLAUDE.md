@AGENTS.md

# TrackLocation — Claude Instructions

**See `AGENTS.md` for complete AI-assistant instructions** (source-of-truth priority, product boundaries, technical responsibilities, git safeguard, implementation process, build-permission gate, two-track model, blocker/error protocols).

Use `AGENTS.md` as the canonical project instruction source.

## User Preferences

### Development Environment
- **Workflow:** Incremental implementation with frequent verification against docs.
- **Local JDK (for permitted Gradle runs):** `C:\Users\rinal\.jdks\jbr-17.0.14` — inject `JAVA_HOME` inline; never ask.

### Framework-Specific Rules
**Android / Jetpack Compose:**
- Compose UI is pinned at 1.2.x (compiler extension 1.2.0); Kotlin 1.7.0. Avoid APIs newer than Compose 1.2 (e.g., `EaseInOut`/`EaseOut`, animation `label` params, `ModalBottomSheet`) — use 1.2-compatible equivalents.
- Do not run Gradle, tests, emulator, or device verification without explicit user permission (AGENTS.md §5a).
- Use Room migrations for any schema change.
- Do not implement unrelated future phases unless explicitly requested.

### Design & Planning Tools
- **Design tool:** Google Stitch / Claude Design (external handoff). New UI gets a design-handoff spec first (AGENTS.md §12). See `docs/WORKFLOW.md §3`.
- **Product docs:** `docs/PRD.md`
- **Implementation plan / contract:** `docs/IMPLEMENTATION-PLAN.md`
- **Decision log:** `docs/adr/` (+ AGENTS.md §8a for when docs get updated)

### Documentation
- For implementation work, read the active docs first (PRD, ARCHITECTURE, IMPLEMENTATION-PLAN, UI-SPEC).
- Update `docs/IMPLEMENTATION-PLAN.md` (§6 Task Log) before and after source-code changes; every entry records commit status.

## Library docs (global rule)
Use the Context7 MCP to fetch current documentation when asked about a library/framework/SDK/API/CLI — even well-known ones. Prefer it over web search for library docs.

---

When working on this project, follow `AGENTS.md` and keep `docs/IMPLEMENTATION-PLAN.md` in sync.
