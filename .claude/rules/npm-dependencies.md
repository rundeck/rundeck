---
globs: ["**/package.json", "**/.npmrc"]
alwaysApply: false
applyTo: "**/package.json,**/.npmrc"
---

# NPM Dependency Pinning Rules

## Why

The npm ecosystem has seen repeated large-scale supply chain attacks: the September 2025 compromise of `chalk`, `debug`, and 16 other packages with a combined 2.6 billion weekly downloads (attacker phished a maintainer's npm account and published malicious versions that hijacked crypto transactions — see [Wiz](https://www.wiz.io/blog/widespread-npm-supply-chain-attack-breaking-down-impact-scope-across-debug-chalk) and [Aikido](https://www.aikido.dev/blog/npm-debug-and-chalk-packages-compromised)), and the ongoing "Shai-Hulud" self-propagating worm that has trojanized 1,300+ package versions since late 2025 by stealing publish credentials and re-infecting every package a compromised maintainer can push to (see [Unit 42](https://unit42.paloaltonetworks.com/npm-supply-chain-attack/) and [Elastic Security Labs](https://www.elastic.co/security-labs/shai-hulud-chaindrop-npm-supply-chain)).

Caret/tilde ranges (`^1.2.3`, `~1.2.3`) mean a `npm install` — or any lockfile regeneration — can silently pull in a newer, compromised version of a transitive dependency without a code review noticing. Exact pinning plus a lockfile that only resolves to a known-good version closes that window: a compromised release can't be installed until someone deliberately bumps the pin.

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
