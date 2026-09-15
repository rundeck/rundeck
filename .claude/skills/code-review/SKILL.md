---
name: code-review
description: 'Reviews a pull request diff for Rundeck: correctness, security, project conventions, and over-engineering. One line per finding. Use when reviewing a PR or diff, when asked "review this" or "ready to merge?", and for Copilot code review in this repository.'
---

# Code Review

Findings stay inside the diff; read whatever context you need to judge it.
One line per finding. No praise, no
restating what the diff does. A good diff gets shorter and stays correct.

## Format

`<file>:L<line>: <tag>: <what>. <fix>.`

Tags, in the order to look for them:

- `bug:` wrong behavior, race, null path, leaked resource. Name the failing input.
- `security:` command/SQL injection, XSS, path traversal, secret in code, unescaped user input.
- `convention:` breaks a project rule. Name the rule file.
- `test:` changed code without a unit or API test, or a test that cannot fail.
- `delete:` dead code, commented-out code, speculative flexibility. Replacement: nothing.
- `yagni:` abstraction with one implementation, config nobody sets, layer with one caller.
- `shrink:` same logic, fewer lines. Show the shorter form.

Formatting is Spotless's job (`./gradlew spotlessCheck`). Never comment on it.
Legacy code the diff does not touch is out of scope.

## Conventions

Open the referenced file only when the diff touches its files. Do not repeat its content.

| Diff touches | Check | Source |
|---|---|---|
| `*.groovy`, `*.java` | Javadoc or Groovydoc on new/modified code; Groovy classes `@CompileStatic` (or `@GrailsCompileStatic`), `@CompileDynamic` only per method; no hand-written getters/setters; tests in Spock, never new JUnit | `.claude/docs/development-guidelines.md` |
| `build.gradle` | versions from root `gradle.properties` via `${prop}`, never hardcoded | `CLAUDE.md` |
| Liquibase changelogs | never edit an existing changeset; precondition, rollback, MySQL + PostgreSQL + H2 | `.claude/rules/database-migrations.md` |
| API controllers | OpenAPI annotations, `Since: v<n>`, one capitalized tag, DTOs instead of inline schemas, version bump only for new behavior | `.claude/docs/api-guidelines.md`, `.claude/docs/development-guidelines.md` § API Versioning |
| `*.vue` | Options API, `<style scoped>`, no inline styles, `$t()` for text, `*.spec.ts` exists | `.claude/rules/vue.md` |
| `*.ts`, `*.js`, `*.spec.ts` under `ui-trellis` | a Jest test covers the changed code; spec files follow the Priority 1 and 2 rules | `.claude/rules/jest.md` |
| Selenium specs, page objects | Page Object Model, no `Thread.sleep`, explicit waits | `.claude/rules/selenium.md` |
| Functional and Selenium tests using OkHttp | `RdClient` `do*` responses closed or consumed (base-class `do*` helpers clean up on their own); prefer `get()`/`post()` when only the body matters | `.claude/rules/okhttp-client-response.md` |
| `package.json` | exact versions, no `^` or `~` outside `peerDependencies` (a preemptive `overrides` entry with no lockfile match may stay a range) | `.claude/rules/npm-dependencies.md` |
| New or modified methods or functions | cyclomatic complexity ≤ 25 | `.claude/rules/complexity.md` |

The PR itself: title `[RUN-1234] Description` when there is a ticket, template
sections filled in, scope matches what the title says. Product names in user-facing text:
"Rundeck" (open source), "Runbook Automation" (commercial), never "Rundeck Pro".

## Verdict

Close with one verdict line, last:

- `Ship.` when no `bug`, `security`, `convention` or `test` finding.
- `Fix first: <n> finding(s).` otherwise.

Right before it, add `net: -<N> lines possible.` when any `delete`, `yagni` or `shrink` finding exists.

## Self-check

Before posting: every finding line matches the format with one of the seven tags
right after the location, the verdict is the last line, and `Fix first` counts only `bug`,
`security`, `convention` and `test` findings.

## Boundaries

Review only. Do not apply fixes, approve, or merge. Do not flag CI status:
that is the pipeline's job.
