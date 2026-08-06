---
name: write-jest-tests
description: Write Vue component unit tests. Use when the user asks to write unit tests, write Jest tests, write Vue tests, add tests to a component, or test a Vue component.
allowed-tools: Read, Grep, Glob, Write, Edit, Bash
---

# Write Jest Tests

Reference: `.claude/docs/jest-testing-guidelines.md` — 21 rules with rationale and examples

---

## Phase 1: Read Guidelines

Read `.claude/docs/jest-testing-guidelines.md` completely before writing any code.

---

## Phase 2: Write Tests

1. Read the component under test fully
2. Check the closest `jest.global-mocks.js` before adding any mock
3. Create test file at `[component-dir]/tests/[ComponentName].spec.ts`
4. Write tests following all rules in the guidelines
5. Run `npm run test:unit [file]` — fix until all pass

---

## Phase 3: Verify Compliance

Open `VERIFICATION-CHECKLIST.md` (in this skill's folder) and execute Gates 1–4 sequentially.

- **Gate 1** (Priority 1 — 7 steps): fix all violations, restart Gate 1 if any fail
- **Gate 2** (Priority 2 — 9 steps): fix all violations, restart Gate 2 if any fail
- **Gate 3** (Priority 3 — 5 steps): document warnings + fix plans; does not block
- **Gate 4** (Final — 3 steps): tests pass, edge cases covered, conditional rendering tested

For every enumeration step: list ALL instances with line numbers — no sampling.

---

## Phase 4: Certify

```
COMPLIANCE CERTIFICATION
Test File: _________________
Component: _________________
Gate 1 (Priority 1): [PASSED]
Gate 2 (Priority 2): [PASSED]
Gate 3 (Priority 3): [PASSED / WARNINGS: ...]
Gate 4 (Final):      [PASSED]
RESULT: COMPLIANT
```

---

**Done:** [test file created, N tests, all gates passed]
**Artifacts:** [path to test file]
**Next:** [deferred P3 fixes, or "none"]

---

## Maintenance

This file orchestrates — it does not define rules or verification steps. If referenced file names change, update Phase 1 and Phase 3 references only.