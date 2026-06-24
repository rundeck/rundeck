# Project Conventions

## Documentation

Detailed conventions and guidance organized by topic:

### Architecture & Setup
- **`.claude/docs/architecture.md`** — Project structure, module organization, technology stack
- **`.claude/docs/build-commands.md`** — All build, test, and plugin commands with examples
- **`.claude/docs/file-locations.md`** — Where to find controllers, services, components, etc.

### Development
- **`.claude/docs/development-guidelines.md`** — Code standards, Groovy conventions, Git workflows
- **`.claude/docs/code-formatting.md`** — Code formatting rules, Spotless configuration
- **`.claude/docs/plugin-development.md`** — Plugin creation, CI, publishing

### Testing
- **`.claude/docs/testing-guidelines.md`** — Complete testing guide (unit, API, functional, integration)
- **`.claude/docs/jest-testing-guidelines.md`** — Frontend testing (Jest + Vue), Priority 1 & 2 rules
- **`.claude/docs/selenium-best-practices.md`** — E2E testing, Page Object patterns, wait strategies

### API & Agent
- **`.claude/docs/api-guidelines.md`** — OpenAPI specs, DTOs, i18n
- **`.claude/docs/agent-conventions.md`** — Agent behavior patterns, artifact paths, verification requirements

## Code Conventions

### Compilation Mode
Use `@CompileStatic` on all classes (or `@GrailsCompileStatic` for Grails artifacts). Use `@CompileDynamic` only on specific methods requiring dynamic typing.

### Documentation
All new/modified code requires Javadoc or Groovydoc comments.

### Property Access
Groovy auto-generates getters/setters — do NOT implement them explicitly unless custom logic is needed.

### Security
Avoid command injection, XSS, SQL injection. Use parameterized queries and escape user input.

### Simplicity
Only make changes that are directly requested or clearly necessary. Don't add features, refactor code, or make "improvements" beyond what was asked.

## Dependency Management

- All dependency versions defined in root `gradle.properties`
- Reference in `build.gradle` using `${propertyName}` syntax
- Never hardcode version numbers in build files

## Git & PR Conventions

### PR Title
Format: `[RUN-XXXX] Description` when linked to an issue (recommended), or a clear description.

## Rules

Context-aware rules that load automatically based on file patterns:

- **`.claude/rules/selenium.md`** — Selenium test requirements
- **`.claude/rules/jest.md`** — Jest test compliance (references `jest-testing-guidelines.md`)
- **`.claude/rules/vue.md`** — Vue component standards (Options API, scoped styles, component placement)
- **`.claude/rules/database-migrations.md`** — Database migration rules (Liquibase, multi-DB support)
- **`.claude/rules/okhttp-client-response.md`** — OkHttp response cleanup in functional/Selenium tests

## Skills

Available skills for common workflows:

**Development:**
- **`create-code`** — Generate Java/Groovy/TypeScript code following Rundeck standards
- **`create-api-endpoint`** — Create REST API endpoints with OpenAPI annotations and versioning
- **`create-test`** — Create backend tests (Spock unit, API, functional)
- **`write-jest-tests`** — Create frontend tests (Jest + Vue Test Utils)
- **`create-plugin`** — Create Rundeck plugins with proper build configuration
- **`i18n-vue-template`** — Internationalize Vue component templates (replace hardcoded strings with `$t()`)

**Documentation:**
- **`generate-entity-context`** — Generate standardized CONTEXT.md files for features

**Workflows:**
- **`cve-remediation`** — Verify and fix security vulnerabilities (CVEs)
- **`backport-pr`** — Cherry-pick a merged PR onto a release/maintenance branch
- **`onboard-contributor`** — Guide a new contributor through the repo setup and conventions

Use the Skill tool to invoke: `Skill(skill_name="skill-name")`

## Critical Rules

1. **Always verify build succeeds locally** before considering work complete: `./gradlew build -x check` (4-8 min)
2. **Never modify database migrations** without explicit approval (see `.claude/rules/database-migrations.md`)
3. **Always verify work** — don't claim completion without running verification commands

## Compaction Note

When compacting, preserve the documentation structure above and the critical rules.
