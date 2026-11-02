# Translation tooling proof of concept

Run the tooling directly from the repository root:

```sh
npm ci --prefix scripts/i18n-lib
./scripts/i18n inventory rundeckapp/grails-app/i18n \
  rundeckapp/grails-spa/packages/ui-trellis/src
./scripts/i18n prepare --source-locale en_US --target-locale es_419 \
  --output translations/es_419.json rundeckapp/grails-app/i18n
./scripts/i18n apply --input translations/es_419.json
./scripts/i18n validate --source-locale en_US --target-locale es_419 \
  rundeckapp/grails-app/i18n
```

`inventory` accepts any number of files or directories. It finds properties
catalogs in i18n directories (including feature-prefixed message bundles),
locale modules under `locales/`, and `i18n.ts`/`i18n.js` modules. Use
`--output FILE` for a reproducible JSON report. Each catalog reports its
source-to-target namespace, consumer/archive destination, locale alias,
placeholders, plural/markup indicators, duplicate occurrences, and parse
failures. JavaScript and TypeScript modules are parsed but never imported.

`prepare` writes only source keys absent from the target locale. Fill each
non-null `translation` in that JSON before using `apply`; all other fields are
integrity-protected. Applying is insert-only: an existing key always wins,
unrelated translator edits remain, duplicate batch entries are ignored, and a
repeated apply does nothing. Source hashes reject stale workspaces while
allowing a target translator to win an existing-key race.

`validate --input batch.json` checks protected metadata and source hashes.
Locale validation additionally checks duplicates, missing keys, placeholders,
obvious plural-form changes, and markup tag shape.

The checked-in catalogs are small canonical overlays, not a broad migration.
See `POC_REPORT.md` for the inventory evidence, namespace model, consumer
prototypes, exclusions, and remaining limits.

## Existing conversion scripts

The scripts `scripts/props-to-json.groovy`,
`scripts/structure-i18n-json.groovy`, and
`scripts/convert-messages-json.groovy` remain useful one-way conversion
experiments. They use `java.util.Properties`, which discards comments,
formatting, ordering, and duplicate occurrences. The structure script also
prints diagnostic text into its output, while the directory converter silently
replaces catalogs when two files resolve to the same locale. None of them can
inventory Vue modules, prepare locale deltas, validate duplicates, or safely
patch catalogs. They are therefore documented as predecessors, not used by this
POC.
# Public translation catalog POC

This directory is a deliberately small proof of concept for keeping sparse
Rundeck and Rundeck Pro translation overlays in the public `rundeck` repository.
Consumers merge these files over their existing catalogs, so a key here wins
while keys not present here continue to come from their original location.

The Pro build reads this directory through its existing `rundeck` submodule.
There is no npm package, generated source commit, or manual synchronization
step.

## POC limitations

- Only the core Readme/MOTD UI catalog, the core backend message bundle, and
  the Pro Schedule Forecast catalogs demonstrate the convention.
- The backend POC adds a sparse classpath basename; a production rollout should
  centralize basename registration instead of repeating feature-specific setup.
- Translator-directed public PR and pipeline policy (POC 4) is intentionally
  excluded.
