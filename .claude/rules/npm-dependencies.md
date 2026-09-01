---
globs: ["**/package.json", "**/.npmrc"]
alwaysApply: false
applyTo: "**/package.json,**/.npmrc"
---

# NPM Dependency Pinning Rules

Reference: [NPM Remediation Strategy (Confluence)](https://pagerduty.atlassian.net/wiki/spaces/~62d5e36b3ace3a8e7388b3f1/pages/5023989761/NPM+Remediation+Strategy)

## Mandatory

1. **No caret/tilde ranges** (`^`, `~`) in `dependencies`, `devDependencies`, or `overrides`. Pin the exact version.
2. **`peerDependencies` are the one exception** — keep them as ranges. Pinning a peer breaks consumers; this is intentional, not an oversight.
3. **Every npm project needs its own local `.npmrc`** (sibling to that `package.json`) with:
   ```
   registry=https://npm.artifacts.pd-internal.com/npm/
   @pagerduty:registry=https://npm.artifacts.pd-internal.com/npm/
   save-prefix=""
   ```
   Without it, npm falls back to the user's global config — public registry, caret prefixes — and silently undoes this policy on the next install.
4. **Lockfiles must resolve only through the internal registry.** Zero `registry.npmjs.org` references in `package-lock.json`.
5. **An `overrides` entry with no corresponding lockfile entry** (nothing in the dependency tree currently pulls that package) is a defensive/preemptive override, commonly written as a range on purpose (a floor for if the package is ever introduced transitively). There is no resolved version to pin against — leave it as-is rather than inventing one. Don't treat this as a violation of rule 1.
6. Use `npm ci` (never `npm install`) in CI.

## Before Completing

1. `grep -rn '"\^' package.json` (per touched project) — no matches outside `peerDependencies`.
2. `grep -c 'registry.npmjs.org' package-lock.json` — must be 0.
3. `npm ci` + `npm run test:unit` + production build pass for every touched project.
