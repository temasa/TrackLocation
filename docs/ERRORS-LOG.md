---
name: ERRORS-LOG.md
path: docs/ERRORS-LOG.md
description: Persistent error learning log — captures all errors from compilation, build, deployment, and verification with root cause, resolution, and lessons
---

# Errors Log

## TrackLocation

**Document Version:** 0.1  
**Status:** Active (persistent — never delete)  
**Last Updated:** 2026-06-15 11:44:58  
**Owned By:** Project Team

---

## Purpose

This log is a **permanent learning artifact**. Every error encountered during compilation, build, deployment, or verification is recorded here with its root cause and resolution — so patterns are recognized, mistakes are not repeated, and future sessions have full context on what has already been debugged.

Unlike `PLANNING-LOG.md`, this document is **never deleted**. It grows with the project.

---

## When to Log

Log an entry whenever an error occurs during any of these phases:

| Phase | Examples |
|-------|---------|
| **Compilation** | Type errors, missing imports, syntax errors, annotation processor failures |
| **Build** | Gradle/Maven/npm/Makefile failures, missing dependencies, version conflicts |
| **Deployment** | Docker build failures, server config errors, environment variable mismatches, migration failures |
| **Verification** | Test failures, assertion errors, integration check failures, smoke test errors |

---

## How to Log

1. Add a new entry under **Error Entries** using the template below.
2. Record **Found At** immediately when the error occurs (seconds precision).
3. Fill **Resolved At** once the error is fixed; leave `—` while still open.
4. Add a row to the **Quick Reference Table** above the entries section.
5. After 3+ similar errors appear, add a note to **Patterns & Recurring Issues**.

---

## Quick Reference Table

| ID | Found At | Resolved At | Phase | Sprint/Task | Title | Status |
|----|----------|-------------|-------|-------------|-------|--------|
| ERR-001 | 2024-01-15 10:23:45 | 2024-01-15 10:41:02 | Build | S1-T2 | Example: Gradle JDK version mismatch | Resolved |

*(Add a row here for every new entry — update Resolved At when fixed)*

---

## Error Entries

---

### ERR-001: Example — Gradle JDK version mismatch *(example entry, replace with real errors)*

**Found At:** 2024-01-15 10:23:45  
**Resolved At:** 2024-01-15 10:41:02  
**Phase:** Build  
**Sprint/Task:** S1-T2  
**Environment:** local  
**Status:** Resolved  

**Error Message / Output:**
```
> Task :app:compileDebugKotlin FAILED
e: error: incompatible types: Int cannot be converted to Long
FAILURE: Build failed with an exception.
* What went wrong: Execution failed for task ':app:compileDebugKotlin'.
> Compilation error. See log for more details.
```

**Root Cause:**
`gradle-wrapper.properties` specified Gradle 8.x, but the local JDK was 11; Gradle 8 requires JDK 17+. The mismatch caused the Kotlin compiler to report spurious type errors before the real JDK error surfaced.

**Resolution:**
```bash
# Verified JDK version
java -version   # was 11

# Updated JAVA_HOME to JDK 17 in local .env
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk

# Re-ran build
./gradlew assembleDebug
```

**Lesson Learned:**
Always check JDK version first when the build error looks like a type error but the code is correct. Add JDK version requirement to README. Pin the expected JDK version in `CLAUDE.md` under development environment.

---

*(Replace the example above and add new entries below this line)*

---

## Patterns & Recurring Issues

Populate this section when the same class of error appears 3 or more times.

| Pattern | Phase(s) | Entries | Mitigation |
|---------|----------|---------|------------|
| *(none yet)* | | | |

---

**This log is permanent. Do not delete. Append entries as errors are encountered.**
