---
name: onboard-contributor
description: Guide a new contributor through the rundeck OSS repo. Use when someone is getting started with the project, asking how to build/test/contribute, or setting up their development environment.
---

# Onboard Contributor

Guides new contributors (external or internal) through the rundeck OSS development environment.

## When to Use

- New contributor asking how to get started
- Questions about building or running the project
- Questions about how to contribute a plugin, bug fix, or feature
- Setting up a development environment

## Process

### Phase 1: Environment Check

Check the contributor's environment:

```bash
java -version   # Must be Java 17
node -v         # Check .nvmrc for required version
./gradlew --version
```

If Java is wrong: suggest `jenv`, `sdkman`, or `asdf`.
If Node is wrong: suggest `nvm use` (after checking `.nvmrc`).

### Phase 2: Project Overview

Explain the structure:

```
rundeck/
├── rundeckapp/       Main Grails application (controllers, services, Vue frontend)
├── core/             Core Java library (plugin interfaces, shared utilities)
├── functional-test/  Selenium and API functional tests
├── grails-webhooks/  Webhooks plugin
└── gradle.properties All dependency versions defined here
```

Key points:
- **Backend**: Grails 7 / Spring Boot 3 / Groovy 4 — use `@CompileStatic` on all classes
- **Frontend**: Vue 3 + TypeScript in `rundeckapp/grails-spa/packages/ui-trellis/`
- **Tests**: Spock for backend, Jest for frontend, Selenium for E2E
- **Migrations**: Liquibase in `rundeckapp/grails-app/migrations/` — never modify existing ones

### Phase 3: First Build

```bash
# Full build, skip tests for speed (~4-8 min)
./gradlew build -x check

# If it fails, try cleaning first
./gradlew clean && ./gradlew build -x check
```

### Phase 4: Running Tests

```bash
# Backend unit tests
./gradlew test

# Frontend unit tests
UI=rundeckapp/grails-spa/packages/ui-trellis
npm run --prefix "$UI" ci:test:unit

# Specific test class
./gradlew test --tests "com.example.MySpec"
```

### Phase 5: Making a Contribution

**For a bug fix:**
1. Write a failing test that reproduces the bug (Spock for backend, Jest for frontend)
2. Fix the bug
3. Verify the test now passes
4. Run `./gradlew build -x check` to verify compilation

**For a new plugin:**
- Use the `create-plugin` skill

**For a new API endpoint:**
- Use the `create-api-endpoint` skill

**PR conventions:**
- Include tests for all new behavior
- All PRs must pass CI before merge
- See `CONTRIBUTING.md` in the repo root for the full contribution guide

### Phase 6: Code Standards Summary

| Standard | Rule |
|---|---|
| Groovy classes | `@CompileStatic` required (or `@GrailsCompileStatic` for Grails artifacts) |
| Tests | Spock only — no new JUnit |
| Vue components | Options API default, scoped styles, `data-testid` in tests |
| DB migrations | Never modify existing — create new ones |
| Strings in Vue | Always use `$t()` — no hardcoded English |
| OkHttp responses | Must be closed or body consumed |

## Checklist

- [ ] Java 17 confirmed
- [ ] Node.js version matches `.nvmrc`
- [ ] `./gradlew build -x check` passes
- [ ] Contributor knows where controllers, services, Vue components live
- [ ] Contributor understands test requirements (Spock, Jest, Selenium)
- [ ] Contributor knows the plugin development path (if applicable)
