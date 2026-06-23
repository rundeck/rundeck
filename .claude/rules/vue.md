# Vue Component Rules

## 1. API Style — Options API by Default

Use **Options API** unless a feature is only available via Composition API (e.g., `provide`/`inject` with typed generics, composable reuse across components).

- `data()`, `computed`, `methods`, `watch`, `mounted` etc. are the standard
- `setup()` is permitted only when the feature genuinely requires it — do not use it for stylistic preference
- **Never mix** Options API and Composition API arbitrarily within a component; if `setup()` is needed, keep it minimal and return values consumed by the Options API body

```vue
<!-- Preferred -->
export default defineComponent({
  data() { ... },
  computed: { ... },
  methods: { ... },
})

<!-- Only when composition is genuinely needed -->
export default defineComponent({
  setup() {
    const foo = ref(null)
    return { foo }
  },
  computed: { ... },
})
```

## 2. Styles — Scoped, No Inline

- All `<style>` blocks **must** use the `scoped` attribute: `<style scoped lang="scss">`
- **No inline styles** (`style="..."`) in templates — use scoped classes or CSS variables instead
- Exception: truly dynamic values that cannot be expressed as a class (e.g., computed pixel offsets) must be extracted to a `computed` property returning a style object, not written inline in the template

```vue
<!-- Forbidden -->
<div style="margin-top: 10px">

<!-- Acceptable for dynamic values -->
<div :style="dynamicOffset">
// in computed:
dynamicOffset() { return { marginTop: this.offset + 'px' } }

<!-- Preferred in all other cases -->
<div class="my-offset">
```

## 3. Component Placement

Place components in the correct layer — this keeps the dependency graph clean:

| Layer | Location | Purpose |
|---|---|---|
| **lib** | `rundeckapp/grails-spa/packages/ui-trellis/src/library/components/` | Atomic, reusable UI (buttons, inputs, modals). No domain knowledge. |
| **app** | `rundeckapp/grails-spa/packages/ui-trellis/src/app/` | Feature components. Domain-aware. |

- A `lib` component must not import from `app`
- When unsure: if it has no domain logic, it belongs in `lib`

## 4. Every Component Must Have a Test Suite

Every `.vue` file must have a corresponding `*.spec.ts` test file.

- If none exists, create one using the **`write-jest-tests`** skill before considering work on that component complete
- Tests live alongside the component or in a sibling `__tests__/` directory
- See `.claude/rules/jest.md` for test compliance rules

---

## Additional Rules

### Component Declaration

- Always use `export default defineComponent({ ... })` — never a bare object export
- Always set the `name:` property to match the filename in PascalCase (e.g. `name: "JobStatusBadge"`)
- Register components **locally** in the `components` option — global registration is not permitted

### i18n — No Hardcoded Strings

- All user-visible strings must go through `$t('key')` — never hardcode English text in templates or scripts
- Do not concatenate translation keys dynamically (e.g. `` $t(`period.label.${x}`) ``) unless the full key set is finite, documented, and present in the message catalogue
- If you find hardcoded strings in a component you are touching, use the **`i18n-vue-template`** skill to resolve them before completing the task

### Store and Service Access

Prefer the following order — more explicit and testable patterns rank higher:

1. `inject(InjectionKey)` — preferred when a typed injection key exists
2. `getRundeckContext().rootStore` — acceptable for general store access
3. `window._rundeck` — **forbidden**; untestable and bypasses the service layer

### Props and Emits

- All props must be typed — use the `type` option or `PropType<>`
- Mark props `required: true` or supply a sensible `default`
- Declare all emitted events in the `emits` option

### Timer and Listener Cleanup

- Any `setInterval`, `setTimeout`, or event listener registered in `mounted` or `created` **must** be cleared in `unmounted`
- Failing to clean up causes memory leaks that are difficult to diagnose

```ts
mounted() {
  this._timer = setInterval(this.poll, 5000)
},
unmounted() {
  clearInterval(this._timer)
},
```

### Forbidden Patterns

- **`$forceUpdate()`** — if you need this, the component's reactive state is modelled incorrectly; fix the state instead
- **`document.querySelector` / `document.getElementById`** inside a component — use `this.$refs` or a template ref
- **Inline styles** — covered in rule 2; repeated here because it is the most common violation

### Third-Party UI Components

Before using a third-party UI component (PrimeVue, Bootstrap, etc.) in a feature component, follow this decision flow:

1. **Check `lib` for an existing wrapper** — if one exists, use it; do not import the third-party directly
2. **If no wrapper exists** — check whether the library's stylesheet is already imported globally in the project
   - If yes: use the third-party component directly and import the required styles
   - If no: stop and ask the user how to proceed before importing anything

### Template Hygiene

- Never put `v-if` and `v-for` on the same element — use a wrapping `<template>` for the `v-if`
- Always provide a stable `:key` on `v-for` — prefer an entity id over array index
- Use `v-show` for frequent visibility toggling; use `v-if` when the branch is rarely rendered

---

## Before Completing

1. `npm run lint` — no new lint or type errors introduced
2. `npm run test:unit [file]` — component test suite exists and passes
3. If the component has no test file, run the **`write-jest-tests`** skill