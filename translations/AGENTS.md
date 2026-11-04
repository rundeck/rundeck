# Agent instructions

- Place each catalog beneath `core/` or `rundeckpro/` using the directory path
  of the consumer it mirrors; do not introduce shorthand namespace folders.
- Register every catalog family in `catalog-registry.json`.
- Use `./scripts/i18n`; do not replace this workflow with a Gradle task.
- Run `inventory` and `validate` before editing translation batches.
- Only populate `translation` fields emitted by `prepare`; never add guessed keys.
- Treat `apply` as insert-only. Do not overwrite existing translator work or
  broadly rewrite production catalogs.
- Run `npm test --prefix scripts/i18n-lib` after tooling changes.
