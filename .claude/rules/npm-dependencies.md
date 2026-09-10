---
globs: ["**/package.json", "**/.npmrc"]
alwaysApply: false
applyTo: "**/package.json,**/.npmrc"
---

# NPM Dependency Pinning Rules

## Why

The npm ecosystem has seen repeated large-scale supply chain attacks: the September 2025 compromise of `chalk`, `debug`, and 16 other packages with a combined 2.6 billion weekly downloads (attacker phished a maintainer's npm account and published malicious versions that hijacked crypto transactions — see [Wiz](https://www.wiz.io/blog/widespread-npm-supply-chain-attack-breaking-down-impact-scope-across-debug-chalk) and [Aikido](https://www.aikido.dev/blog/npm-debug-and-chalk-packages-compromised)), and the ongoing "Shai-Hulud" self-propagating worm that has trojanized 1,300+ package versions since late 2025 by stealing publish credentials and re-infecting every package a compromised maintainer can push to (see [Unit 42](https://unit42.paloaltonetworks.com/npm-supply-chain-attack/) and [Elastic Security Labs](https://www.elastic.co/security-labs/shai-hulud-chaindrop-npm-supply-chain)).

Caret/tilde ranges (`^1.2.3`, `~1.2.3`) mean a `npm install` — or any lockfile regeneration — can silently pull in a newer, compromised version of a transitive dependency without a code review noticing. Exact pinning plus a lockfile that stays untouched unless deliberately updated closes that window.

## Mandatory

1. **No caret/tilde ranges** (`^`, `~`) in `dependencies`, `devDependencies`, or `overrides`. Pin the exact version.
2. **`peerDependencies` are the one exception** — keep them as ranges. Pinning a peer breaks consumers; this is intentional, not an oversight.
3. **If a project's `.npmrc` configures a registry**, it should also set `save-prefix=""` — otherwise a plain `npm install <pkg>` falls back to the default caret prefix and quietly undoes the pinning on the next install.
4. **A lockfile's resolved URLs must match whatever registry that project's `.npmrc` points to.** Mixed origins in `package-lock.json` (e.g. some packages resolving from a different host than others) usually mean the lockfile was regenerated without the project's own `.npmrc` active — regenerate it with the right config instead of hand-editing hosts.
5. **An `overrides` entry with no corresponding lockfile entry** (nothing in the dependency tree currently pulls that package) is a defensive/preemptive override, commonly written as a range on purpose (a floor for if the package is ever introduced transitively). There is no resolved version to pin against — leave it as-is rather than inventing one. Don't treat this as a violation of rule 1.
6. Use `npm ci` (never `npm install`) in CI.

## Before Completing

1. `grep -rn '"\^' package.json` (per touched project) — no matches outside `peerDependencies`.
2. `npm ci` + `npm run test:unit` + production build pass for every touched project.
