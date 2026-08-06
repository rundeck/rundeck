# Agent Conventions

Guidelines for how agents should behave when working on this codebase.

## Agent Behavior Patterns

### Verification Before Completion
- **NEVER** consider a task complete without running verification
- **ALWAYS** verify build succeeds locally: `./gradlew build -x check`
  - Typical duration: 4-8 minutes
  - Full test suite can take over 1 hour — CI will run it

### Double-Check Code Standards
- Before any response, verify your code matches project standards
- If it doesn't match, restart and fix it

### Commit Only with Confirmation
- Never commit changes without user confirmation

## Saving Artifacts

| Type | Path |
|------|------|
| Plans & implementation proposals | `.claude/artifacts/plans/` |
| Investigation & analysis reports | `.claude/artifacts/reports/` |
| Temporary / scratch files | `.claude/artifacts/tmp/` |
| Handoff notes for other agents | `.claude/artifacts/handoff/` |

Name files with a kebab-case description and today's date, e.g. `plan-runner-refactor-2026-04-02.md`.