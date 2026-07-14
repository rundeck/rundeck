# Contributing to the AI Setup

Guide for maintaining and extending the Claude Code configuration in this repository.

---

## Directory Structure

```
.claude/
├── docs/           # Reference documentation (human-authored)
├── rules/          # Context-aware rules (auto-loaded by glob patterns)
├── skills/         # Workflow automation skills
├── artifacts/      # Agent-generated outputs
```

---

## Creating New Skills

### Skill Structure

Each skill lives in `.claude/skills/<skill-name>/` with a `SKILL.md` file:

```
.claude/skills/my-skill/
├── SKILL.md          # Main skill definition (required)
├── checklist.md      # Verification checklist (optional)
└── examples/         # Example files (optional)
```

### SKILL.md Format

```markdown
---
name: my-skill
description: One-line description of what the skill does
---

# Skill Name

## When to Use
- Describe when this skill should be invoked

## Process
### Phase 1: ...
### Phase 2: ...

## Checklist
- [ ] Verification items
```

### Guidelines

- **Keep skills under 500 lines** — move checklists and examples to separate files
- **One skill, one responsibility** — split complex workflows into multiple skills
- **Reference docs, don't duplicate** — point to `.claude/docs/` files instead of repeating content
- **Include verification** — every skill should have a way to verify its output
- **Test with the agent** — run the skill and verify it produces correct results

### Naming Conventions

- Use kebab-case: `create-plugin`, `backport-pr`
- Prefix with action verb: `create-`, `build-`, `generate-`, `validate-`
- Name should describe what it does, not where it's used

---

## Creating New Rules

### Rule Structure

Rules live in `.claude/rules/` and auto-load based on file patterns:

```markdown
---
description: Rule description
globs:
  - "**/*.spec.ts"
  - "**/*.spec.js"
alwaysApply: false
---

# Rule Name

## Mandatory
1. Rule 1
2. Rule 2

## Before Completing
Verification steps
```

### Guidelines

- **Rules enforce constraints** — use for things that MUST be followed (not suggestions)
- **Keep rules short** — reference docs files for detailed guidance
- **Use specific globs** — only load rules when relevant files are being edited
- **Don't duplicate docs** — rules point to docs, docs contain the details

### When to Create a Rule vs. a Doc

| Use a Rule | Use a Doc |
|------------|-----------|
| Must be enforced every time | Reference material |
| Short (under 50 lines) | Detailed guidance |
| Applies to specific file types | Applies broadly |
| Contains DO/DON'T constraints | Contains how-to instructions |

---

## Writing Documentation

### Doc Structure

Docs live in `.claude/docs/` and are referenced from `CLAUDE.md`:

```markdown
# Topic Name

## Overview
Brief description

## Section 1
Content

---

## Related Documentation
- Links to related files
```

### Guidelines

- **One topic per file** — don't mix testing with database work
- **Reference other docs** — link instead of duplicating
- **Keep actionable** — docs should help the agent do something
- **Update CLAUDE.md** — add new docs to the Documentation section

### Naming Conventions

- Use kebab-case: `testing-guidelines.md`, `database-work.md`
- Descriptive names: readers should know the topic from the filename
- Consistent suffixes: `-guidelines`, `-best-practices`, `-work`

---

## Updating CLAUDE.md

`CLAUDE.md` is what the agent reads at the start of every session. When making changes:

- **Add new docs** to the appropriate section (Architecture, Development, Testing, API & Agent)
- **Add new rules** to the Rules section
- **Add new skills** to the Skills section under the right category
- **Keep it concise** — CLAUDE.md is an index, not a manual

---

## Checklist for AI Setup Changes

Before submitting changes to `.claude/`:

- [ ] New docs added to CLAUDE.md Documentation section
- [ ] New rules added to CLAUDE.md Rules section
- [ ] New skills added to CLAUDE.md Skills section
- [ ] Skills under 500 lines (move checklists to separate files)
- [ ] No content duplicated between docs
- [ ] Rules reference docs (not duplicate them)
- [ ] File names follow kebab-case convention
- [ ] Tested with agent (invoke skill, verify output)
