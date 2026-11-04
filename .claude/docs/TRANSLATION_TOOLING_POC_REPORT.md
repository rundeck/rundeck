# Translation migration feasibility report

This report covers three feasibility prototypes only. It does not move all
production catalogs or define translator PR/pipeline policy.

## POC 1: canonical extraction

Reproduce the complete first-party inventory from the Rundeck worktree:

```sh
./scripts/i18n inventory --output translations/poc-inventory.json \
  rundeckapp/grails-app/i18n \
  rundeckapp/grails-spa/packages/ui-trellis/src \
  /Users/smartinellibenedetti/dev/rundeckpro2-translation-poc/plugins
```

The verified inventory contains 120 catalogs (71 properties and 49 Vue),
20,482 parsed entries, 217 duplicate-key groups, and no parse failures.
Inventory is read-only for source catalogs. The report retains duplicate line
numbers before any key collapse and records locale aliases, placeholder sets,
plural/markup indicators, source-to-target namespace, intended consumer, and
archive destination.

The proposed public layout is:

- `translations/core/rundeckapp/grails-app/i18n/` for Grails message bundles.
- `translations/core/rundeckapp/grails-spa/packages/ui-trellis/src/app/`
  for Core Vue feature catalogs.
- `translations/rundeckpro/plugins/<plugin>/` for Pro plugin bundles.
- `translations/rundeckpro/plugins/rundeckpro-spa-ui/rundeckpro-ui-components/src/<feature>/` for Pro Vue catalogs.

Observed locale names include `de`, `de_DE`, `en_US`, `es`, `es_419`, `fr_FR`,
`ja`, `ja_JP`, `nl`, `nl_NL`, `pt_BR`, `zh_CN`, and `zh_cn`. The report flags
known aliases such as `ja_JP` to `ja` and `zh_CN` to `zh_Hans`; it does not
silently rename them.

Known exceptions: dynamically constructed/non-static Vue expressions are
reported only when their surrounding static catalog is supported; arbitrary
application JavaScript and non-i18n properties are intentionally excluded.
Unsupported or malformed candidate catalogs appear in `parseFailures` instead
of being executed.

## POC 2: representative consumers

Core Vue imports
`translations/core/rundeckapp/grails-spa/packages/ui-trellis/src/app/components/readme-motd/i18n.ts`
and deep-merges it over the original component catalog. Core backend resources
package
`translations/core/rundeckapp/grails-app/i18n/public-messages.properties`
and register it between administrator overrides and the original `messages`
bundle.

The Pro Schedule Forecast backend reads its mirrored
`rundeckpro/plugins/schedule-forecast/grails-app/i18n` path, while its Vue
consumer reads the mirrored `rundeckpro/plugins/rundeckpro-spa-ui` path. Both
are consumed directly through the existing Rundeck submodule relationship.
The public key wins, original keys and locales remain per-key fallbacks, and
the plugin resource archive still receives a normal message bundle. No npm
package or synchronization copy is introduced.

## POC 3: lossless/idempotent CLI

```sh
./scripts/i18n inventory <roots...>
./scripts/i18n prepare --source-locale en_US --target-locale es_419 \
  --output batch.json <roots...>
./scripts/i18n validate --input batch.json
./scripts/i18n apply --input batch.json
./scripts/i18n validate --source-locale en_US --target-locale es_419 <roots...>
```

Properties parsing preserves source bytes and appends only missing keys; it
handles duplicates, continuations, escapes, comments, ordering, CRLF, and
missing final newlines without `java.util.Properties`. Vue catalogs use a
narrow static parser and deterministic object insertion; repository modules
are never evaluated. Batch metadata and source hashes are protected, stale
sources are rejected, target-side races are skipped, and repeated
prepare/apply runs are no-ops until a source key is added.

The historical Groovy conversion scripts deserialize through
`java.util.Properties` or perform one-way restructuring. That loses duplicate
occurrences, comments, ordering, and formatting and cannot safely patch Vue
catalogs or enforce insert-only races, integrity, and idempotence. They remain
historical utilities, not a production migration foundation.

## Limits and risks

- The static Vue parser supports object-literal catalogs, static strings, and
  non-interpolated templates. Spreads, computed keys, imported fragments, and
  runtime expressions require an explicit future adapter.
- Locale alias policy remains a product decision; the POC reports aliases but
  preserves source names.
- The backend basename wiring is feature-specific POC code. A rollout should
  centralize registration and define archive ownership.
- The duplicate count includes existing source debt; the POC detects it and
  does not collapse or repair it.
