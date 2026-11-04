# Canonical translation catalogs

This directory is the public proof of concept for Rundeck and Rundeck Pro
translation catalogs. Its structure mirrors the source tree each catalog
relates to, beneath an explicit product owner:

```text
translations/
├── catalog-registry.json
├── core/
│   ├── rundeckapp/
│   │   ├── grails-app/i18n/
│   │   └── grails-spa/packages/ui-trellis/src/app/components/
│   └── plugins/
│       └── flow-control-plugin/src/main/resources/resources/i18n/
└── rundeckpro/
    └── plugins/
        ├── schedule-forecast/grails-app/i18n/
        └── rundeckpro-spa-ui/rundeckpro-ui-components/src/
```

The mirrored paths make ownership, packaging destinations, and related source
locations visible without consulting a second naming convention. Runtime
helpers live under `tools/i18n`; generated evidence lives under
`.claude/docs/translation-reports`.

Consumers overlay canonical values over their original catalogs while
unmigrated keys continue to use the original locations as fallback. Rundeck
Pro reads its catalogs through the existing Rundeck submodule; no package
publication or synchronization copy is required.

## Tooling

Run the direct CLI from the repository root:

```sh
npm ci --prefix scripts/i18n-lib
./scripts/i18n inventory --scope all --output build/i18n-inventory.json
./scripts/i18n prepare --source-locale en_US --target-locale es_419 \
  --scope canonical --output build/es_419.json
./scripts/i18n validate --input build/es_419.json
./scripts/i18n apply --input build/es_419.json
./scripts/i18n migrate --scope canonical --source-locale en_US \
  --target-locale es_419 --output build/i18n-migration.json
```

`inventory` reports unsupported candidates rather than silently omitting them.
`prepare` emits only missing values. `apply` is insert-only and validates all
planned writes before changing any target. Literal dotted keys remain distinct
from nested Vue paths, and source integrity is tracked per key.

See `.claude/docs/TRANSLATION_TOOLING_POC_REPORT.md` for feasibility evidence
and `.claude/docs/CONSUMER_ADAPTER_POC_REPORT.md` in Rundeck Pro for consumer
and packaging evidence.

## POC boundary

- Schedule Forecast backend and Vue are complete representative families.
- Core application, Readme/MOTD, and Flow Control catalogs are bounded
  consumer examples.
- Original catalogs remain fallback until a family reaches generated parity
  and deletion eligibility.
- Translator-directed PR automation remains outside these POCs.
