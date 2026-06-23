# Jest Rules

All tests in this file must comply with the **Priority 1** and **Priority 2** rules defined in `.claude/docs/jest-testing-guidelines.md`.

- **Priority 1** (7 rules): violations must be fixed immediately before any other work
- **Priority 2** (9 rules): violations must be fixed before completing the task

If you are unfamiliar with the rules, read the guidelines before making changes.

## Before Completing

1. `npm run test:unit [file]` — all tests must pass
2. `npm run lint` — no new lint or type errors
3. If NOT running the `write-jest-tests` skill: open `.claude/skills/write-jest-tests/VERIFICATION-CHECKLIST.md` and work through it

---

## Maintenance

This file intentionally contains no rule definitions — those live exclusively in `jest-testing-guidelines.md`. If rule content changes, only the guidelines and the checklist need updating.