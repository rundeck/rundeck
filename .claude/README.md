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
- **Rules** load automatically when you edit matching file types (e.g., `*.spec.ts` → jest rules)
- **Skills** are invoked with `/skill-name` in the prompt

## For Contributors

If you add new docs, rules, or skills, update `CLAUDE.md` to reference them.

See `CONTRIBUTING.md` for conventions and file structure guidelines.
