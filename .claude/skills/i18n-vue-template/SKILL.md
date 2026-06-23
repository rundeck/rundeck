---
name: i18n-vue-template
description: Internationalize Vue component templates by replacing all hardcoded strings with $t() calls and adding translations to i18n.ts or en_US.js. Use when asked to internationalize, i18n, or translate strings in Vue templates.
allowed-tools: Read, Grep, Glob, Write, Edit, Bash
---

# Internationalize Vue Template Strings

Systematically replace all hardcoded strings in Vue component templates with i18n translation keys.

---

## Phase 1: Audit

1. Read the target Vue component fully
2. Identify all hardcoded strings in:
   - Template: text within HTML tags (`<p>Text</p>`, `<button>Click me</button>`)
   - Template: bare interpolation without `$t()` (`{{ 'hardcoded' }}`)
   - Script: `data()` properties containing display strings
3. **Document the count**: "Found X hardcoded strings to internationalize"

Do not internationalize: CSS class names, HTML attributes, test IDs, prop values, dynamic content from variables.

---

## Phase 2: Add Translation Keys

1. Locate the translation file:
   - Within `ui-trellis`: find the nearest `en_US.js` file
   - Elsewhere: find the nearest `i18n.ts` file
2. Add new keys under an appropriate namespace using camelCase
3. Provide translations for **all supported languages** present in the file (at minimum English)
4. Key naming examples:
   - `"Search for a step"` → `searchForStep`
   - `"Node Steps"` → `nodeSteps`

---

## Phase 3: Replace in Template and Script

- Template text: `<p>Text</p>` → `<p>{{ $t('namespace.key') }}</p>`
- Data properties with display strings → move to `computed`, use `this.$t()`

```js
// Before
data() {
  return { options: [{ name: 'Node Steps', value: 'step' }] }
}

// After
computed: {
  options() {
    return [{ name: this.$t('message.nodeSteps'), value: 'step' }]
  }
}
```

---

## Phase 4: Verify

1. Count remaining hardcoded strings — must be 0
2. Run verification grep:
   ```bash
   grep -E '<(p|button|span)>[^{<]*[A-Z][a-z]+ [a-z]+[^}>]*</(p|button|span)>' [file]
   ```
3. Report: "Internationalization complete: 0 hardcoded strings remaining (from X)"