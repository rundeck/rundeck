# Jest Test Verification Checklist

Execute gates sequentially after writing tests. Each gate must fully pass before proceeding.

For the meaning of any rule, see `.claude/docs/jest-testing-guidelines.md`.

---

## Gate 1 — Priority 1 (fix required, restart gate on any failure)

### 1.1 User perspective [RULE-001]
For each test, confirm it verifies only inputs (clicks, props) or outputs (DOM, events, API calls).
- [ ] No test accesses internal methods or state
- **RESULT**: [PASS] or [FAIL — tests at lines X]

### 1.2 `wrapper.vm` usage [RULE-002]
Enumerate every `wrapper.vm.*` and `child.vm.*` instance with line number.
- [ ] Each is only `$nextTick()` or `child.vm.$emit()`
- **RESULT**: [PASS] or [FAIL — violations at lines X, Y]

### 1.3 No direct method calls [RULE-003]
Find all `wrapper.vm.methodName()` calls.
- [ ] Zero direct method calls — only `$nextTick()` permitted
- **RESULT**: [PASS] or [FAIL — lines X]

### 1.4 `data-testid` selectors [RULE-004]
Enumerate every `.find(` and `.findAll(` call.
- [ ] Each uses `[data-testid="..."]` or `findComponent()` — no CSS / element types / IDs / chaining
- **RESULT**: [PASS] or [FAIL — violations at lines X, Y, Z]

### 1.5 Global mocks [RULE-005]
- Closest `jest.global-mocks.js` path: ___________
- Items mocked globally: ___________
- [ ] No local mocks duplicate global ones
- **RESULT**: [PASS] or [FAIL — duplicates found]

### 1.6 Helper placement [RULE-006]
Enumerate all function definitions with location.
- [ ] All helpers are after `jest.mock()` calls and before any `describe` block
- **RESULT**: [PASS] or [FAIL — helpers inside describe at lines X]

### 1.7 No `wrapper.vm` data setting [RULE-012]
Scan createWrapper, test bodies, hooks, and all helpers.
- [ ] Zero `wrapper.vm.property = value` or `Object.assign(wrapper.vm, ...)`
- **RESULT**: [PASS] or [FAIL — assignments at lines X]

**Gate 1 result**: All 7 steps [PASS] → proceed. Any [FAIL] → fix and restart from 1.1.

---

## Gate 2 — Priority 2 (fix required, restart gate on any failure)

### 2.1 Conditional `.exists()` [RULE-007]
Enumerate every `.exists()` call. For each, verify the element has `v-if`/`v-show`/computed in the component.
- [ ] Zero `.exists()` on always-present elements
- **RESULT**: [PASS] or [FAIL — lines X, Y]

### 2.2 Component stubbing [RULE-008]
List all child components. Confirm stubbing decision is appropriate for each.
- **RESULT**: [PASS] or [FAIL — inappropriate stubbing for X]

### 2.3 Mock data size [RULE-009]
Find all inline mock data. Flag any exceeding 10 lines, 3+ objects, or hurting readability.
- [ ] No large mocks inline — extracted to `./mocks/`
- **RESULT**: [PASS] or [FAIL — inline at lines X]

### 2.4 Type-safe mocks [RULE-010]
Find all service mocks.
- [ ] Each uses `jest.Mocked<typeof Service>` with typed return values — no `as any`
- **RESULT**: [PASS] or [FAIL — untyped at lines X]

### 2.5 `createWrapper` structure [RULE-011]
- [ ] Named `createWrapper`
- [ ] Props used in 3+ tests defined as defaults
- [ ] `options.props` override supported
- **RESULT**: [PASS] or [FAIL]

### 2.6 Assertion types [RULE-013]
Confirm all assertions use `.text()`, `.attributes()`, `.classes()`, `.exists()`, or `.emitted()`.
- [ ] No `wrapper.vm.*` in assertions
- **RESULT**: [PASS] or [FAIL — lines X]

### 2.7 Event payloads [RULE-014]
For each test that triggers an event, confirm payload is verified — not just emission.
- [ ] All event assertions include payload
- **RESULT**: [PASS] or [FAIL — missing payload at lines X]

### 2.8 Component synchronisation [RULE-015]
After every `setProps()` or state change, confirm `await wrapper.vm.$nextTick()` follows.
- **RESULT**: [PASS] or [FAIL — missing sync at lines X]

### 2.9 Promise handling [RULE-016]
After every API-triggering interaction, confirm sequence: trigger → `flushPromises()` → `$nextTick()` → assert.
- **RESULT**: [PASS] or [FAIL — missing flushPromises at lines X]

**Gate 2 result**: All 9 steps [PASS] → proceed. Any [FAIL] → fix and restart from 2.1.

---

## Gate 3 — Priority 3 (document warnings, does not block)

### 3.1 Mount strategy [RULE-017]
Confirm `mount` vs `shallowMount` is appropriate and consistent within the file.
- **RESULT**: [PASS] or [WARNING + fix plan]

### 3.2 Event bus mocking [RULE-018]
If component uses event bus: confirm it is mocked with `on/off/emit` jest.fn() methods.
- **RESULT**: [PASS], [WARNING + fix plan], or [N/A]

### 3.3 Hardcoded values [RULE-019]
Confirm expected values in assertions are hardcoded, not assigned to variables.
- **RESULT**: [PASS] or [WARNING + fix plan]

### 3.4 Timer handling [RULE-020]
If component uses timers: confirm `jest.useFakeTimers()` / `advanceTimersByTime()` / `useRealTimers()`.
- **RESULT**: [PASS], [WARNING + fix plan], or [N/A]

### 3.5 Async sequencing [RULE-021]
Confirm complex async tests follow: trigger → `flushPromises()` → `$nextTick()` → assert.
- **RESULT**: [PASS] or [WARNING + fix plan]

**Gate 3 result**: Document all warnings. Execute fix plans or get explicit deferral approval. Proceed to Gate 4.

---

## Gate 4 — Final

### 4.1 Tests pass
```bash
npm run test:unit [file]
npx tsc --noEmit
```
- [ ] All tests pass, no TypeScript errors, no console errors
- **RESULT**: [PASS] or [FAIL]

### 4.2 Edge case coverage
Confirm applicable cases are tested: empty arrays, null/undefined props, API errors, loading states, boundary conditions.
- **RESULT**: [PASS] or [FAIL — missing: X, Y]

### 4.3 Conditional rendering coverage
List all `v-if`/`v-show` elements in the component. Confirm each has a test for true and false state.
- **RESULT**: [PASS] or [FAIL — untested: X, Y]

**Gate 4 result**: All 3 steps [PASS] → complete Final Certification.

---

## Final Certification

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

## Maintenance

Each gate step maps to one rule in `jest-testing-guidelines.md`. If a rule is added or removed from the guidelines, add or remove the corresponding gate step here. Steps must not explain rules — only check them.
