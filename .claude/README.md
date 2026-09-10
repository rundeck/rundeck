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

Files in `rules/` are read by three different AI coding tools, each via a different mechanism:

| Tool | How it reads `rules/` | Frontmatter it uses |
|---|---|---|
| **Claude Code** | Reads `.claude/rules/*.md` directly | `description`, `globs`, `alwaysApply` |
| **Cursor** | `.cursor/rules/*.mdc` are individual symlinks back to `.claude/rules/*.md` | `globs`, `alwaysApply` |
| **GitHub Copilot** | `.github/instructions/*.instructions.md` are individual symlinks, one per rule file, back to `.claude/rules/` | `applyTo` (comma-separated glob string) |

When adding or editing a rule file's scope, keep `globs` (array) and `applyTo` (equivalent comma-joined string) in sync. When adding a brand-new scoped rule file:
- Create its `.github/instructions/<name>.instructions.md` symlink for Copilot — not automatic.
- Create its `.cursor/rules/<name>.mdc` symlink for Cursor — also not automatic. **Must use the `.mdc` extension**, not `.md` — Cursor only discovers `.mdc` files under `.cursor/rules`; a `.md` file there (even with correct frontmatter) is silently ignored.

Always-apply rules (no path scoping, e.g. `complexity.md`) are not symlinked into `.github/instructions/`, since Copilot's `.instructions.md` format has no equivalent to "always apply" (a file with no `applyTo` is not applied automatically).

## For Contributors

If you add new docs, rules, or skills, update `CLAUDE.md` to reference them.

See `CONTRIBUTING.md` for conventions and file structure guidelines.
