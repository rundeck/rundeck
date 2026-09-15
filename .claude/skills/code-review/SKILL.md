---
name: code-review
description: Reviews a pull request diff for Rundeck: correctness, security, project conventions, and over-engineering. One line per finding. Use when reviewing a PR or diff, when asked "review this" or "ready to merge?", and for Copilot code review in this repository.
---

# Code Review

Review the diff, not the codebase. One line per finding. No praise, no
restating what the diff does. A good diff gets shorter and stays correct.

## Format

`<file>:L<line>: <tag> <what>. <fix>.`

Tags, in the order to look for them:

- `bug:` wrong behavior, race, null path, leaked resource. Name the failing input.
- `security:` command/SQL injection, XSS, path traversal, secret in code, unescaped user input.
- `convention:` breaks a project rule. Name the rule file.
- `test:` changed logic without a test, or a test that cannot fail.
- `delete:` dead code, commented-out code, speculative flexibility. Replacement: nothing.
- `yagni:` abstraction with one implementation, config nobody sets, layer with one caller.
- `shrink:` same logic, fewer lines. Show the shorter form.

Formatting is Spotless's job (`./gradlew spotlessCheck`). Never comment on it.
Legacy code the diff does not touch is out of scope.

## Conventions

Open the rule file only when the diff touches its files. Do not repeat its content.

| Diff touches | Check | Source |
|---|---|---|
| `*.groovy` | `@CompileStatic` (or `@GrailsCompileStatic`) on classes, `@CompileDynamic` only per method; Groovydoc on new/modified code; no hand-written getters/setters; Spock, never new JUnit | `.claude/docs/development-guidelines.md` |
| `build.gradle` | versions from root `gradle.properties` via `${prop}`, never hardcoded | `CLAUDE.md` |
| Liquibase changelogs | never edit an existing changeset; precondition, rollback, MySQL + PostgreSQL + H2 | `.claude/rules/database-migrations.md` |
| API controllers | OpenAPI annotations, `Since: v<n>`, one capitalized tag, DTOs instead of inline schemas, version bump only for new behavior | `.claude/docs/api-guidelines.md` |
| `*.vue` | Options API, `<style scoped>`, no inline styles, `$t()` for text, `*.spec.ts` exists | `.claude/rules/vue.md` |
| `*.spec.ts` | Priority 1 and 2 rules | `.claude/rules/jest.md` |
| Selenium specs, page objects | Page Object Model, no `Thread.sleep`, explicit waits | `.claude/rules/selenium.md` |
| Functional tests using OkHttp | every bare `Response` closed or consumed | `.claude/rules/okhttp-client-response.md` |
| `package.json` | exact versions, no `^` or `~` outside `peerDependencies` | `.claude/rules/npm-dependencies.md` |
| New or modified methods | cyclomatic complexity ≤ 25 | `.claude/rules/complexity.md` |

The PR itself: title `[RUN-1234] Description` when there is a ticket, template
sections filled in, scope matches what the title says. Product names in user-facing text:
"Rundeck" (open source), "Runbook Automation" (commercial), never "Rundeck Pro".

## Verdict

End with one line:

- `Ship.` when no `bug`, `security`, `convention` or `test` finding.
- `Fix first: <n> finding(s).` otherwise.

Add `net: -<N> lines possible.` when any `delete`, `yagni` or `shrink` finding exists.

## Boundaries

Review only. Do not apply fixes, approve, or merge. Do not flag CI status:
that is the pipeline's job.
