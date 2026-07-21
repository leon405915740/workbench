---
name: "team-collab-coordinator"
description: "Orchestrates multi-agent collaborative bug fixing with module ownership, Issue lifecycle, DoD, CI Gate, and integration coordination. Invoke when user wants to fix bugs/issues using a structured team workflow with multiple agents, or asks for a team collaboration plan for codebase improvements."
---

# Team Collaboration Coordinator

A reusable multi-agent collaboration framework for structured bug fixing and codebase improvements. Mimics a real software company's development workflow with role-based module ownership.

## Overview

This skill provides a complete process for coordinating multiple AI agents (or developers) to fix bugs and improve code quality in a structured, trackable way. It enforces:

- **Module-based ownership** (not file-based) to minimize conflicts
- **Issue lifecycle** with 8 states (Todo → In Progress → PR Opened → CI Passed → Code Review → QA Verified → Ready Release → Done)
- **Definition of Done (DoD)** with tiered requirements
- **CI Gate** for merge blocking
- **Integration Coordinator** for conflict prevention

## When to Invoke

- User asks to "review and fix bugs" across the entire project
- User wants a "team collaboration plan" for code improvements
- User asks multiple agents to work on the same codebase in parallel
- User wants a structured approach to a large number of issues/bugs

## Phase 1: Codebase Audit (Exploration)

Before planning, conduct a thorough audit of the project:

1. **Explore project structure** — understand modules, packages, dependencies
2. **Read key files** — build config, manifest, core business logic
3. **Identify issues** — categorize by severity: Severe / High / Medium / Low
4. **Group issues by module** — NOT by file, by responsibility domain

### Audit Modules

For each module, identify:
- **Boundary**: Input → Output contract
- **Files owned**: All files the module is responsible for
- **Issues found**: List with severity and file:line references

## Phase 2: Role & Module Definition

### Team Roles

| Role | Writes Code? | Responsibility |
|------|:---:|---------------|
| PM | ✗ | Priority, acceptance criteria, version planning |
| Tech Lead | ✗ | Cross-module impact, interface contracts, P0 Review |
| Integration Coordinator | ✗ | Task assignment, dependency tracking, conflict prevention |
| Module Owner(s) | ✓ | Own entire module end-to-end, no cross-module edits |
| QA | ✗ | Test cases, regression, acceptance |
| DevOps | ✗ | CI config, build, Lint, version, APK |

### Module Ownership Principle

**One Issue = One Module Owner**

- Never split one Issue across multiple modules
- If an Issue spans modules, split into sub-Issues (e.g., ISSUE-014A, ISSUE-014B)
- Module owners must NOT modify files outside their module boundary
- Cross-module communication happens through interface contracts

### Module Boundary Definition

For each module, define:

```
┌─────────────────────────────────────────┐
│ Module Name                              │
│ (Boundary: Input → Output)               │
│                                          │
│ Owned Files:                             │
│   - file1.kt                             │
│   - file2.kt                             │
│   - ...                                  │
│                                          │
│ Interface Contracts:                     │
│   - publicFunA() → returns TypeX         │
│   - publicFunB() → returns TypeY         │
│                                          │
│ Prohibited:                              │
│   - Do NOT modify files of other modules │
└─────────────────────────────────────────┘
```

## Phase 3: Issue Lifecycle

### State Machine

```
Create Issue (from audit/user report/monitoring)
            │
            ▼
    ┌───────────────┐
    │   Todo        │
    └───────────────┘
            │ developer picks up
            ▼
    ┌───────────────┐
    │ In Progress   │
    └───────────────┘
            │ PR created
            ▼
    ┌───────────────┐
    │  PR Opened    │
    └───────────────┘
            │ CI auto-triggered
            ▼
    ┌───────────────┐
    │  CI Passed    │
    └───────────────┘
            │ CI green → Review
            ▼
    ┌───────────────┐
    │ Code Review   │
    └───────────────┘
            │ Approved
            ▼
    ┌───────────────┐
    │  QA Verified  │
    └───────────────┘
            │ QA pass
            ▼
    ┌───────────────┐
    │Ready Release  │
    └───────────────┘
            │ merged
            ▼
    ┌───────────────┐
    │     Done      │
    └───────────────┘
```

### Rollback Rules

- CI Failed → auto rollback to `In Progress`
- Code Review Rejected → rollback to `In Progress`
- QA Failed → rollback to `In Progress`

### Issue Table Format

| Issue ID | Module | Title | Priority | Status | Owner | Files |
|----------|--------|-------|----------|--------|-------|-------|

Priority levels: **P0** (crash/data loss) → **P1** (security/memory leak) → **P2** (stability) → **P3** (experience)

## Phase 4: Definition of Done (DoD)

### Universal DoD (All Priorities)

- [ ] Code complete: matches requirements, no TODO/temp code
- [ ] Unit tests pass: coverage >= 80% for new/modified code
- [ ] Lint pass: no ERROR
- [ ] CI pass: build + lint + unit test all green
- [ ] Code Review passed:
  - P0: Tech Lead + another module owner (dual approve)
  - P1/P2/P3: at least 1 approve
- [ ] No merge conflicts with release branch
- [ ] Logging: key paths include structured logs
- [ ] Sensitive data check: no API keys, PII in code/logs

### P0 Extra DoD

- [ ] QA manual verification passed
- [ ] Boundary tests passed (empty, negative, extreme values)
- [ ] Regression tests passed (core flows no degradation)
- [ ] CHANGELOG updated
- [ ] Architecture decision documented (if applicable)

### Database Migration Extra DoD

- [ ] All version jump paths tested
- [ ] Data integrity verified (pre/post count match)
- [ ] Failure strategy confirmed (user prompt or rollback)

## Phase 5: CI Gate

### Pipeline

```
Push branch → Create PR → CI auto-trigger
                                │
                    ┌───────────┴───────────┐
                    │ 1. assemble (build)    │
                    │ 2. lint                │
                    │ 3. unit test            │
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

### Failure = Block

Any CI step failure blocks the PR. No exceptions.

## Phase 6: Code Review

### Review Matrix

| Priority | Reviewer 1 (Required) | Reviewer 2 (Required) |
|----------|----------------------|----------------------|
| P0 | Tech Lead | Another module owner |
| P1 | Tech Lead or Coordinator | Any module owner |
| P2/P3 | Any two developers | — |

### Review Checklist

- [ ] Follows language style guide
- [ ] No force-unwrap (`!!` in Kotlin, `!` in TypeScript/Dart)
- [ ] No global scope misuse
- [ ] New key paths have structured logging
- [ ] No unrelated adjacent code changes (precision editing)
- [ ] Unit tests updated, naming convention: `[method]_[scenario]_[expected]`
- [ ] Correct dispatcher/thread usage
- [ ] Sensitive data sanitized
- [ ] Database operations have transaction protection (if applicable)
- [ ] No unexpected interface contract changes

### Comment Tags

```
[Severe]   Must fix, blocks approve
[Suggest]  Should fix, but doesn't block
[Question] Needs clarification from author
[Good]     Good practice, worth noting
```

## Phase 7: Integration Coordinator

### Responsibilities (Does NOT write code)

1. **Assign Issues** to module owners based on module boundaries
2. **Track dependencies** — upstream modules must complete first
3. **Conflict prevention** — detect if two modules modify the same file
4. **Daily standup** — produce progress summary
5. **Pre-merge validation** — verify interface consistency, version numbers, CHANGELOG

### Daily Standup Format

```markdown
## [Date] Standup

### Progress
| Issue ID | Module | Status | Owner | Blocker |
|----------|--------|--------|-------|---------|

### Risks
- ⚠️ [risk description]
  - Mitigation: [action]

### Next Day Plan
- [item]
```

### Conflict Detection

When two modules modify the same file:
- Same module → module owner resolves internally
- Cross-module → coordinator intervenes, reassigns file ownership

## Phase 8: Parallel Execution Batches

### Batching Strategy

- **Batch 1**: P0 issues, all modules in parallel (max 3 agents at a time)
- **Batch 2**: P1/P2 issues, all modules in parallel
- **Batch 3**: P3 issues + integration verification
- Each batch: launch agents → wait for completion → run integration check

### Agent Prompt Template

For each module agent, provide:

```
You are the [Module Name] module owner. Fix these Issues in order:

## Project Context
[Project path, tech stack, key conventions]

## Key Rules
- [Project-specific coding rules]
- Must run [build command] to verify
- Must run [test command] to verify
- Must run [lint command] to verify

## Issue-[ID]: [Title] [Priority]
**Problem**: [description with file:line]
**Files to modify**: [list]
**Implementation**: [specific changes]
**Acceptance**: [testable criteria]

## Deliverable
Return: files modified, line numbers, change summary, build/test results.
```

## Phase 9: QA Test Cases

Design test cases covering:

| Case ID | Issue | Scenario | Steps | Expected |
|---------|-------|----------|-------|----------|

Must cover:
- Core fix verification (the specific bug)
- Boundary conditions (zero, negative, extreme values, empty input)
- Regression (core flows still work)
- Security (no sensitive data exposure)

## Phase 10: DevOps Release Checklist

- [ ] Version code incremented
- [ ] Version name updated
- [ ] CHANGELOG updated
- [ ] Release build succeeds
- [ ] All P0 Issues = Done
- [ ] CI main branch = green
- [ ] No open hotfix branches

## Execution Flow Summary

```
1. Audit codebase → identify all issues
2. Define modules & boundaries → assign ownership
3. Create Issue table with priorities & states
4. Launch Batch 1 (P0) — max 3 agents parallel
5. Daily standup — track progress, detect conflicts
6. Launch Batch 2 (P1/P2) — max 3 agents parallel
7. Launch Batch 3 (P3 + integration)
8. Final verification — build + lint + test
9. Produce delivery report with all changes
```

## Adaptation Guide

This framework is language/framework agnostic. Adapt by:

- **Android/Kotlin**: Use `assembleDebug`, `lint`, `testDebugUnitTest`, `ktlint`, `detekt`
- **iOS/Swift**: Use `xcodebuild test`, `SwiftLint`, `Danger`
- **Web/TypeScript**: Use `npm run build`, `eslint`, `jest`, `prettier`
- **Python**: Use `pytest`, `mypy`, `ruff`, `black`
- **Go**: Use `go build`, `go vet`, `go test`, `golangci-lint`