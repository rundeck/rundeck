# Claude Code Configuration

This directory contains the Claude Code AI configuration for the rundeck OSS repo.

## Structure

```
.claude/
├── CLAUDE.md          # Main index — loaded at the start of every session
├── CONTRIBUTING.md    # How to maintain and extend this configuration
├── docs/              # Reference documentation
├── rules/             # Context-aware rules (auto-loaded by file pattern)
├── skills/            # Workflow automation skills
└── artifacts/         # Agent-generated outputs (plans, reports, tmp)
```

## Quick Start

- **CLAUDE.md** is loaded automatically — it contains the index of all docs, rules, and skills
- **Rules** load automatically when you edit matching file types (e.g., Vue/TS/JS files in `ui-trellis` → jest rules)
- **Skills** are invoked with `/skill-name` in the prompt

## Rule Files Are Shared Across Three Tools

| Tool | Reads | Field |
|---|---|---|
| Claude Code | `.claude/rules/*.md` | `globs`, `alwaysApply` |
| Cursor | `.cursor/rules/*.mdc` (symlinks to `.claude/rules/*.md`) | `globs`, `alwaysApply` |
| GitHub Copilot | `.github/instructions/*.instructions.md` (symlinks to `.claude/rules/*.md`) | `applyTo` |

New rule file checklist:
- Keep `globs` and `applyTo` in sync.
- Add `.github/instructions/<name>.instructions.md` symlink for Copilot.
- Add `.cursor/rules/<name>.mdc` symlink for Cursor — extension must be `.mdc`, not `.md`.
- Always-apply rules (`complexity.md`) skip the Copilot symlink — no `applyTo` means "not applied automatically" in Copilot, so there's no way to express "always apply" there.

## For Contributors

If you add new docs, rules, or skills, update `CLAUDE.md` to reference them.

See `CONTRIBUTING.md` for conventions and file structure guidelines.
