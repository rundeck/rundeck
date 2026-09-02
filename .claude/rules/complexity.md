# Cyclomatic Complexity Rule (agents only, informative for humans)

The project has deterministic complexity checking enabled (CodeNarc for Groovy,
ESLint `complexity` for TS/Vue in ui-trellis), threshold **25**, informative only —
nothing blocks builds or PRs. See RUN-4839.

## Mandatory for NEW or MODIFIED code only

1. Any method/function you **add or modify** must have cyclomatic complexity ≤ 25.
   If your change would push a method over the threshold, split it before completing.
2. Do **NOT** refactor pre-existing violations unless explicitly asked — the legacy
   baseline (99 violations) is tracked separately.
3. Never block or fail work because of legacy violations. This rule constrains what
   agents produce, not what already exists.

## How to check

- Backend: `./gradlew codenarcComplexity` — report in `build/reports/codenarc/complexity.html`
- Frontend: `npm run lint` in ui-trellis (rule active in `.eslintrc.js`)

## Before Completing

If you touched Groovy or TS/Vue production code, run the relevant check above and
confirm your changes introduced no NEW violations (compare against the baseline).
