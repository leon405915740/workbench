---
name: "team-collab-coordinator"
description: "Orchestrates multi-agent collaborative development with module ownership, task lifecycle, DoD, CI Gate, and integration coordination. Invoke when user wants multiple agents to work on the same codebase in parallel, or asks for a team collaboration plan for bug fixing, feature development, refactoring, or migration."
---

# Multi-Agent Development Coordinator

A universal multi-agent collaboration framework for structured software development tasks. Mimics a real engineering team's workflow with role-based module ownership. Language/framework agnostic.

## When to Invoke

- User asks to "fix bugs/issues" across the project
- User wants multiple agents to work on the same codebase in parallel
- User wants a structured plan for feature development, refactoring, or migration
- User asks for a "team collaboration plan" or "task breakdown with agents"
- User has a large task that needs to be split across multiple agents

## Task Types

This framework adapts to 5 task types. Pick the matching mode:

| Task Type | Typical Phases | Lite Mode? |
|-----------|---------------|------------|
| **Bug Fix** | Full 10 phases | No |
| **Feature Development** | Skip Phase 1 audit, start from Phase 2 scoping | Optional |
| **Refactor** | Phase 1 + 2 + 7 + 8 + verification | Yes |
| **Migration** (DB/framework/API) | Phase 1 + 2 + 3 + 4 + 8 + rollback plan | No |
| **Audit/Review** | Phase 1 + 7 only (no code changes) | Yes |

### Lite Mode (3 phases)

For small tasks (≤ 3 files, single module), use lite mode:

```
1. Scope & Assign → 2. Parallel Execute → 3. Integrate & Verify
```

Skip Issue lifecycle, CI Gate, QA, DevOps checklist.

---

## Phase 1: Task Scoping (Exploration)

Before planning, understand the scope:

1. **Explore project structure** — modules, packages, dependencies
2. **Read key files** — build config, core business logic, conventions
3. **Identify work items** — for bug fixes: categorize by severity; for features: list acceptance criteria; for refactors: list target files/patterns
4. **Group by module** — NOT by file, by responsibility domain

### Severity Levels (Bug Fix mode)

- **P0** — crash, data loss, security breach
- **P1** — security risk, memory leak, resource exhaustion
- **P2** — stability issue, edge case failure
- **P3** — UX issue, cosmetic, minor inconvenience

### Module Audit (for each module)

```
┌─────────────────────────────────────────┐
│ Module Name                              │
│ (Boundary: Input → Output)               │
│                                          │
│ Owned Files:                             │
│   - file1.kt                             │
│   - file2.kt                             │
│                                          │
│ Interface Contracts:                     │
│   - publicFunA() → returns TypeX         │
│                                          │
│ Work Items:                              │
│   - [P0] ISSUE-001: description          │
│   - [P1] ISSUE-002: description          │
└─────────────────────────────────────────┘
```

---

## Phase 2: Role & Module Definition

### Team Roles

| Role | Writes Code? | Responsibility |
|------|:---:|---------------|
| PM / Requester | ✗ | Priority, acceptance criteria, scope |
| Tech Lead | ✗ | Cross-module impact, interface contracts, P0 Review |
| Integration Coordinator | ✗ | Task assignment, dependency tracking, conflict prevention |
| Module Owner(s) | ✓ | Own entire module end-to-end, no cross-module edits |
| QA | ✗ | Test cases, regression, acceptance |
| DevOps | ✗ | CI config, build, version, release |

> For solo developer + AI agents: one human acts as PM + Tech Lead + Coordinator; AI agents are Module Owners.

### Module Ownership Principle

**One Work Item = One Module Owner**

- Never split one item across multiple modules
- If an item spans modules, split into sub-items (e.g., ITEM-014A, ITEM-014B)
- Module owners must NOT modify files outside their module boundary
- Cross-module communication happens through interface contracts

---

## Phase 3: Task Lifecycle

### State Machine

```
Create Task
    │
    ▼
┌───────────┐    assign     ┌──────────────┐    PR created   ┌────────────┐
│   Todo    │ ──────────►   │ In Progress  │ ──────────►    │ PR Opened  │
└───────────┘               └──────────────┘                └────────────┘
                                                                  │
                                                        CI auto   │
                                                                  ▼
┌───────────┐   merge    ┌──────────────┐  QA pass   ┌────────────┐
│   Done    │ ◄────────  │ Ready Release│ ◄────────  │ QA Verified│
└───────────┘            └──────────────┘            └────────────┘
                               ▲                          │
                               │    Approved              │
                          ┌────────────┐                  │
                          │Code Review │ ◄────────────────┘
                          └────────────┘
                               ▲
                          ┌────────────┐
                          │ CI Passed  │
                          └────────────┘
```

### Rollback Rules

- CI Failed → rollback to `In Progress`
- Code Review Rejected → rollback to `In Progress`
- QA Failed → rollback to `In Progress`

### Task Table Format

| Task ID | Module | Title | Priority | Status | Owner | Files |
|---------|--------|-------|----------|--------|-------|-------|

---

## Phase 4: Definition of Done (DoD)

### Universal DoD (All Priorities)

- [ ] Code complete: matches requirements, no TODO/temp code
- [ ] Unit tests pass: coverage ≥ 80% for new/modified code
- [ ] Lint pass: no ERROR
- [ ] CI pass: build + lint + unit test all green
- [ ] Code Review passed (see Phase 6 review matrix)
- [ ] No merge conflicts with target branch
- [ ] Logging: key paths include structured logs
- [ ] Sensitive data check: no API keys, PII in code/logs

### P0 Extra DoD

- [ ] QA manual verification passed
- [ ] Boundary tests passed (empty, negative, extreme values)
- [ ] Regression tests passed (core flows no degradation)
- [ ] Rollback plan documented

### Migration Extra DoD

- [ ] All version jump paths tested
- [ ] Data integrity verified (pre/post count match)
- [ ] Failure strategy confirmed (user prompt or rollback)
- [ ] Backup taken before migration

---

## Phase 5: CI Gate

```
Push branch → Create PR → CI auto-trigger
                                │
                    ┌───────────┴───────────┐
                    │ 1. assemble (build)    │
                    │ 2. lint                │
                    │ 3. unit test           │
                    │ 4. code quality check  │
                    └───────────────────────┘
                                │
                    ┌───────┴───────┐
                    ▼               ▼
                 Failed         Passed
                    │               │
                    ▼               ▼
                Block PR    Enter Review
                               │
                               ▼
                           Merge
```

**Any CI step failure blocks the PR. No exceptions.**

---

## Phase 6: Code Review

### Review Matrix

| Priority | Reviewer 1 (Required) | Reviewer 2 (Required) |
|----------|----------------------|----------------------|
| P0 | Tech Lead | Another module owner |
| P1 | Tech Lead or Coordinator | Any module owner |
| P2/P3 | Any two developers | — |

### Review Checklist

- [ ] Follows language style guide
- [ ] No force-unwrap (`!!` in Kotlin, `!` in TS/Dart, `unwrap()` in Rust)
- [ ] No global scope misuse
- [ ] New key paths have structured logging
- [ ] No unrelated adjacent code changes (precision editing)
- [ ] Unit tests updated, naming: `[method]_[scenario]_[expected]`
- [ ] Correct dispatcher/thread usage
- [ ] Sensitive data sanitized
- [ ] DB operations have transaction protection (if applicable)
- [ ] No unexpected interface contract changes

### Comment Tags

```
[Block]    Must fix, blocks approve
[Suggest]  Should fix, but doesn't block
[Question] Needs clarification from author
[Good]     Good practice, worth noting
```

---

## Phase 7: Integration Coordinator

### Responsibilities (Does NOT write code)

1. **Assign tasks** to module owners based on module boundaries
2. **Track dependencies** — upstream modules must complete first
3. **Conflict prevention** — detect if two modules modify the same file
4. **Progress tracking** — produce standup summary
5. **Pre-merge validation** — verify interface consistency, version numbers

### Standup Format

```markdown
## [Date] Standup

### Progress
| Task ID | Module | Status | Owner | Blocker |
|---------|--------|--------|-------|---------|

### Risks
- ⚠️ [risk description]
  - Mitigation: [action]

### Next Steps
- [item]
```

### Conflict Detection

When two modules modify the same file:
- Same module → module owner resolves internally
- Cross-module → coordinator intervenes, reassigns file ownership or serializes

---

## Phase 8: Parallel Execution Batches

### Batching Strategy

- **Batch 1**: P0 / critical tasks, all modules in parallel
- **Batch 2**: P1/P2 tasks, all modules in parallel
- **Batch 3**: P3 tasks + integration verification
- Each batch: launch agents → wait for completion → run integration check

### Parallel Agent Limits

| Scenario | Max Parallel Agents |
|----------|:---:|
| No file dependencies | 3-5 |
| Same file modifications | 1 (serialize) |
| Tool-call intensive | 2-3 |
| P0 emergency fix | 1-2 |

### Agent Prompt Template

For each module agent, provide:

```
You are the [Module Name] module owner. Complete these tasks in order:

## Project Context
[Project path, tech stack, key conventions]

## Key Rules
- [Project-specific coding rules]
- Must run [build command] to verify
- Must run [test command] to verify
- Must run [lint command] to verify

## Task-[ID]: [Title] [Priority]
**Objective**: [description with file:line]
**Files to modify**: [list]
**Implementation**: [specific changes]
**Acceptance**: [testable criteria]

## Deliverable
Return: files modified, line numbers, change summary, build/test results.
```

---

## Phase 9: QA Test Cases

Design test cases covering:

| Case ID | Task | Scenario | Steps | Expected |
|---------|------|----------|-------|----------|

Must cover:
- Core fix/feature verification (the specific change)
- Boundary conditions (zero, negative, extreme values, empty input)
- Regression (core flows still work)
- Security (no sensitive data exposure)

---

## Phase 10: Release Checklist

- [ ] Version code/name incremented
- [ ] CHANGELOG updated
- [ ] Release build succeeds
- [ ] All P0 tasks = Done
- [ ] CI target branch = green
- [ ] No open hotfix branches

---

## Execution Flow Summary

### Full Mode (Bug Fix / Migration)

```
1. Scope & audit → identify all work items
2. Define modules & boundaries → assign ownership
3. Create task table with priorities & states
4. Launch Batch 1 (P0) — parallel agents
5. Standup → track progress, detect conflicts
6. Launch Batch 2 (P1/P2) — parallel agents
7. Launch Batch 3 (P3 + integration)
8. QA verification
9. Final build + lint + test
10. Produce delivery report
```

### Lite Mode (Refactor / Small Feature)

```
1. Scope & assign → define modules, assign owners
2. Parallel execute → launch agents, wait for completion
3. Integrate & verify → build + test + review
```

---

## Adaptation Guide

This framework is language/framework agnostic. Adapt by:

| Stack | Build | Lint | Test |
|-------|-------|------|------|
| Android/Kotlin | `./gradlew assembleDebug` | `ktlint` / `detekt` | `./gradlew testDebugUnitTest` |
| iOS/Swift | `xcodebuild build` | `SwiftLint` | `xcodebuild test` |
| Web/TypeScript | `npm run build` | `eslint` / `prettier` | `jest` / `vitest` |
| Python | `python -m build` | `ruff` / `mypy` | `pytest` |
| Go | `go build ./...` | `golangci-lint` | `go test ./...` |
| Rust | `cargo build` | `clippy` | `cargo test` |
| Java | `mvn compile` / `gradlew build` | `checkstyle` / `spotbugs` | `mvn test` / `gradlew test` |
