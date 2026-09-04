# Build Commands Reference

Quick reference for common build and test commands in the rundeck OSS repo.

## Common Build Tasks

```bash
# Full build
./gradlew build

# Build without tests
./gradlew build -x test

# Build without tests or quality checks (fastest — 4-8 min)
./gradlew build -x check

# Run in dev mode (-Dgrails.env=development is REQUIRED for asset hot-reload
# to work without a bootRun restart — see "Hot-reloading SPA assets" below.
# In IntelliJ, add it to the Gradle run config's "Script parameters" / VM options.)
./gradlew bootRun -Dgrails.env=development

# Clean build artifacts
./gradlew clean

# Code formatting
./gradlew spotlessCheck        # Check formatting
./gradlew spotlessApply        # Auto-fix formatting
```

## Backend Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.example.MySpec"

# Run tests for specific module
./gradlew :rundeckapp:test
```

## Functional Tests

```bash
# API functional tests
./gradlew :functional-test:apiTest

# Selenium functional tests
./gradlew :functional-test:seleniumTest
```

## Frontend Tests

```bash
# Core UI tests
UI=rundeckapp/grails-spa/packages/ui-trellis
npm run --prefix "$UI" ci:test:unit           # Run tests
npm run --prefix "$UI" dev:test:watch         # Watch mode
```

## Build Verification

```bash
# Quick build verification (4-8 minutes)
./gradlew build -x check

# Compiles code and builds artifacts without running:
# - Test suite (can take 1+ hour)
# - Code quality checks (spotless, checkstyle)
# CI will run the full test suite
```

## Hot-Reloading SPA Assets During `bootRun`

> **⚠️ WORK IN PROGRESS — not fully working yet.** The `useManifest`/`bundle`
> config below is confirmed to help pick up changes on a `bootRun` restart,
> but no-restart hot-reload (editing a file, running `copySpa`, and seeing it
> without restarting) is **not yet working** — see `HOT_RELOAD_HANDOFF.md` at
> the repo root for the full investigation and next steps. Don't rely on the
> "no restart needed" claim below until that's resolved.

By default, `bootRun` serves assets through a precompiled digest manifest that
is loaded into memory once at JVM startup — recompiling SCSS/JS on disk does
nothing until the JVM restarts. Running with `-Dgrails.env=development` (see
above) activates a dev-scoped `grails.assets.useManifest: false` override
(`rundeckapp/grails-app/conf/application.yml`), which makes asset-pipeline
serve assets dynamically instead: it recompiles only the files whose content
actually changed (content-hash + dependency tracked, not a full project
rebuild) on each request.

With `-Dgrails.env=development` set, the loop to see a Vue/SCSS/JS change
under `rundeckapp/grails-spa/packages/ui-trellis/src` is:

```bash
# 1. Edit a file under src/app or src/library

# 2. Recompile + copy into the Grails asset tree
./gradlew :rundeckapp:copySpa

# 3. Hard-reload the browser — no bootRun restart needed
```

No `assetClean`/`assetCompile`/`copyCompiledAssets` dance is required — those
still run as part of `bootRun`'s normal dependency chain, but their output
(the digest manifest) is simply unused for serving in dev mode. Production/WAR
builds (`./gradlew build`) are unaffected — the `useManifest: false` override
is scoped to `environments: development:` only.

If you forget `-Dgrails.env=development`, `bootRun` falls back to the default
manifest-based serving and you'll need a full restart to see asset changes,
same as before this change.

## Troubleshooting

### Build Fails with "Cannot find Java 17"

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
./gradlew build -x check
```

### Frontend Build Fails

```bash
rm -rf node_modules package-lock.json
npm install
```

### Gradle Daemon Issues

```bash
./gradlew --stop
rm -rf ~/.gradle/caches
```