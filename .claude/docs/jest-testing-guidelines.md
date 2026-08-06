# Vue Component Jest Testing Guidelines

Rules and patterns for writing unit tests with Jest, Vue Test Utils, and Vue 3 Options API in this codebase.

---

## Rule Priority System

- **Priority 1** — Non-negotiable. Violation = immediate fix required before proceeding.
- **Priority 2** — Strict. Violation = fix required before proceeding.
- **Priority 3** — Best practice. Violation = document + fix plan required.

---

## Quick Reference — Rule Index

### Priority 1
| ID | Rule |
|---|---|
| RULE-001 | Test From User Perspective |
| RULE-002 | DOM-Based Assertions Only |
| RULE-003 | No Direct Method Calls |
| RULE-004 | `data-testid` Selectors Exclusively |
| RULE-005 | Check Global Mocks Before Adding |
| RULE-006 | Helper Functions Outside `describe` Blocks |
| RULE-012 | No Data Values Via `wrapper.vm` |

### Priority 2
| ID | Rule |
|---|---|
| RULE-007 | Existence Checks Only for Conditional Elements |
| RULE-008 | Component Stubbing |
| RULE-009 | Large Mock Data in Separate Files |
| RULE-010 | Type-Safe Service Mocking |
| RULE-011 | Type-Safe Mount Helpers (`createWrapper`) |
| RULE-013 | DOM Structure Assertions |
| RULE-014 | Event Assertions |
| RULE-015 | Component Update Synchronization |
| RULE-016 | API Calls and Promises |

### Priority 3
| ID | Rule |
|---|---|
| RULE-017 | Mount vs ShallowMount |
| RULE-018 | Event Bus Mocking |
| RULE-019 | Hardcoded Expected Values |
| RULE-020 | Timer-based Operations |
| RULE-021 | Complex Async Sequences |

---

## Definitions

**Conditional element**: An element is conditional if the component uses `v-if`, `v-show`, or computed properties to control its rendering — based on the component's implementation, not how props are set in a specific test.

**Common props**: Props used in 3 or more test cases in the same file. These MUST be defined as defaults in `createWrapper`.

**Large mock data**: Any mock that exceeds 10 lines when formatted, contains 3+ objects with multiple fields, or reduces readability.

---

## Priority 1 Rules

### RULE-001: Test From User Perspective

Test only what users can observe:
- **Inputs**: clicks, form inputs, props passed to component
- **Outputs**: rendered DOM, emitted events, API calls made

Never test internal methods, internal state, or private implementation details.

### RULE-002: DOM-Based Assertions Only

`wrapper.vm.*` is forbidden except for two specific uses:

```typescript
// ONLY permitted wrapper.vm usages:
await wrapper.vm.$nextTick();                    // synchronization
await child.vm.$emit('event-name', payload);     // simulating child events in parent tests
```

```typescript
// FORBIDDEN
expect(wrapper.vm.isLoading).toBe(false);
expect(wrapper.vm.items).toEqual([...]);
wrapper.vm.$el; wrapper.vm.$refs; wrapper.vm.$data; wrapper.vm.$props;

// CORRECT
expect(wrapper.find('[data-testid="loading"]').exists()).toBe(false);
expect(wrapper.findAll('[data-testid="item"]').length).toBe(2);
```

### RULE-003: No Direct Method Calls

```typescript
// FORBIDDEN
wrapper.vm.handleClick();
wrapper.vm.submitForm();

// CORRECT
await wrapper.find('[data-testid="submit-button"]').trigger('click');
```

### RULE-004: `data-testid` Selectors Exclusively

Every `.find()` and `.findAll()` call must use `[data-testid="..."]` — no CSS classes, element types, IDs, pseudo-selectors, or chained selectors.

```typescript
// FORBIDDEN
wrapper.find('.btn-primary');
wrapper.find('button');
wrapper.find('button').find('[data-testid="icon"]');  // chaining forbidden

// CORRECT
wrapper.find('[data-testid="submit-button"]');
wrapper.find(`[data-testid="item-${id}"]`);
```

**Exception**: `wrapper.findComponent(ChildComponent)` is permitted when testing parent-child interactions (props passed to child, events emitted from child).

**If `data-testid` is missing**: Add it to the component first, then write the test. Do not use other selectors as a workaround.

**Naming convention**: `{component-name}-{element-purpose}`, or `{base-name}-${id}` for repeated elements.

### RULE-005: Check Global Mocks Before Adding

Before adding any mock, check the closest `jest.global-mocks.js` file. Already globally mocked: `$t`, `Btn`, `Modal`, `Dropdown`, `Notification`. Do not duplicate these.

### RULE-006: Helper Functions Outside `describe` Blocks

All helpers (`createWrapper`, `findByTestId`, etc.) must appear after `jest.mock()` calls and before any `describe` block. Never define helpers inside `describe` or `it` blocks.

### RULE-012: No Data Values Via `wrapper.vm`

Never set `wrapper.vm.property = value` anywhere — in `createWrapper`, test bodies, `beforeEach`, or any helper. Pass data via props or trigger the user interaction that produces the state.

```typescript
// FORBIDDEN (anywhere)
wrapper.vm.items = [...];
wrapper.vm.isLoading = false;
Object.assign(wrapper.vm, { data: value });

// CORRECT
const wrapper = await createWrapper({ props: { items: [...] } });
// or trigger the interaction that loads the data
```

---

## Priority 2 Rules

### RULE-007: Existence Checks Only for Conditional Elements

`.exists()` is only valid for elements with `v-if`, `v-show`, or computed rendering. Never check existence of always-present elements.

### RULE-008: Component Stubbing

Stub child components when they have external dependencies or add debugging complexity. Do not stub when testing parent-child interaction is the goal.

```typescript
// Simple stub
mount(Component, { global: { stubs: { 'child-component': true } } });

// Custom stub with template
mount(Component, {
  global: {
    stubs: {
      'modal-component': {
        template: '<div data-testid="modal-stub"><slot></slot></div>',
        methods: { open: jest.fn(), close: jest.fn() }
      }
    }
  }
});
```

### RULE-009: Large Mock Data in Separate Files

Extract to `./mocks/filename.ts` when mock data exceeds 10 lines, contains 3+ objects with multiple fields, or hurts readability.

### RULE-010: Type-Safe Service Mocking

```typescript
jest.mock('@/services/api');
const mockedApi = ApiService as jest.Mocked<typeof ApiService>;

beforeEach(() => {
  mockedApi.getItems.mockResolvedValue({ success: true, data: [] } as ApiResponse<Item[]>);
});
```

### RULE-011: `createWrapper` Helper Structure

```typescript
interface MountOptions {
  props?: Record<string, any>;
  slots?: Record<string, string | Function>;
  stubs?: Record<string, boolean | object>;
  mocks?: Record<string, any>;
}

const createWrapper = async (options: MountOptions = {}) => {
  const wrapper = mount(ComponentName, {
    props: {
      isActive: true,       // common prop (used in 3+ tests)
      userName: 'Test User', // common prop
      ...options.props
    },
    global: {
      stubs: { 'complex-child': true, ...options.stubs },
      mocks: { ...options.mocks }
    }
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};
```

### RULE-013: DOM Structure Assertions

```typescript
expect(element.text()).toBe('Expected Text');
expect(element.text()).toContain('Partial');
expect(element.attributes().href).toBe('/expected-url');
expect(element.classes()).toContain('active');
expect(element.classes('active')).toBe(true);
```

### RULE-014: Event Assertions

Always verify the payload, not just that the event was emitted:

```typescript
expect(wrapper.emitted()).toHaveProperty('selected');
expect(wrapper.emitted('selected')).toHaveLength(1);
expect(wrapper.emitted('selected')[0][0]).toEqual({ id: 2, name: 'Item 2' });
```

### RULE-015: Component Update Synchronization

`await wrapper.vm.$nextTick()` after every state change or `setProps()` call.

### RULE-016: API Calls and Promises

`await flushPromises()` after triggering anything that makes an API call. Sequence: trigger → `flushPromises()` → `$nextTick()` → assert.

---

## Priority 3 Rules

### RULE-017: Mount vs ShallowMount
Prefer `mount`. Use `shallowMount` when isolating from all children. Be consistent within a file.

### RULE-018: Event Bus Mocking
Mock event buses with `{ on: jest.fn(), off: jest.fn(), emit: jest.fn() }`.

### RULE-019: Hardcoded Expected Values
Hardcode expected strings and numbers in assertions directly. Do not assign them to variables.

### RULE-020: Timer-based Operations
Use `jest.useFakeTimers()` / `jest.advanceTimersByTime()` / `jest.useRealTimers()`.

### RULE-021: Complex Async Sequences
Proper order: trigger → `flushPromises()` → `$nextTick()` → assert.

---

## File Organization

```
src/components/Button/Button.vue
src/components/Button/tests/Button.spec.ts   ✅
src/components/tests/Button.spec.ts          ❌ wrong level
```

**Import order**:
1. Vue Test Utils
2. Component under test
3. Supporting components
4. Mock data
5. `jest.mock()` calls

---

## Required Test Structure Template

```typescript
import { mount, flushPromises } from '@vue/test-utils';
import ComponentName from '../ComponentName.vue';
import { mockData } from './mocks/data';

jest.mock('@/services/api', () => ({ apiMethod: jest.fn() }));

const createWrapper = async (options = {}) => {
  const wrapper = mount(ComponentName, {
    props: { defaultProp: 'value', ...options.props },
    global: {
      stubs: { 'child-component': true, ...options.stubs },
      mocks: { ...options.mocks }
    }
  });
  await wrapper.vm.$nextTick();
  return wrapper;
};

const findByTestId = (wrapper, testId) => wrapper.find(`[data-testid="${testId}"]`);

describe('ComponentName', () => {
  beforeEach(() => { jest.clearAllMocks(); });

  describe('Feature', () => {
    it('should do something', async () => {
      const wrapper = await createWrapper({ props: { testProp: true } });
      await findByTestId(wrapper, 'button').trigger('click');
      await flushPromises();
      expect(findByTestId(wrapper, 'result').text()).toBe('Expected');
    });
  });
});
```

---

## Required Edge Cases

When applicable, tests MUST cover:
- Empty arrays/collections (if component accepts them as props)
- Null/undefined nullable props
- API failure states
- Loading states during async operations
- Boundary conditions (max/min values)

---

## Conflict Resolution

1. Priority 1 always overrides Priority 2 or 3
2. Same-priority conflicts: choose the option that better supports testing from the user's perspective
3. Undocumented scenarios: apply the core principle — test via DOM assertions and `data-testid` selectors

---

## Maintenance

This is the source of truth for all 21 rules — their meaning, rationale, and examples. Two other files mirror parts of it and must stay in sync when rules change:

- **`jest.md` rule** mirrors the *conclusions* (what's forbidden/required). If a rule is added, removed, or its constraint changes: update the Forbidden/Required lists in `jest.md`.
- **`VERIFICATION-CHECKLIST.md`** maps each rule to a verification step. If a rule is added or removed: add or remove the corresponding gate step in the checklist.

The skill (`write-jest-tests/SKILL.md`) references this file by name only — no changes needed there when rules evolve.