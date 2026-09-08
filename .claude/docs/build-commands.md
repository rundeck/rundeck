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

By default, `bootRun` serves assets through a precompiled digest manifest
(`asset.pipeline.AssetPipelineConfigHolder.manifest`) that's loaded into
memory once at JVM startup — recompiling SCSS/JS on disk does nothing until
the JVM restarts.

The actual blocker turned out **not** to be the `grails.assets.useManifest`
config flag (several attempts to flip it — YAML, `.groovy` environments
closure, JVM system property — had no effect; full investigation in
`HOT_RELOAD_HANDOFF.md`). The real cause: `bootRun`'s own `classpath +=
files(..., "$buildDir/resources/main/META-INF")` line (added to expose
`META-INF/services/*` ServiceLoader files) has the side effect of exposing
`META-INF/assets/manifest.properties` at the exact classpath path
`AssetPipelineGrailsPlugin` checks as a fallback — so the manifest loads on
every `bootRun` regardless of any config. The fix (`rundeckapp/build.gradle`)
is a small `Delete` task, `removeDevManifestForBootRun`, that removes that
one file after it's produced and before `bootRun` launches, wired in via
`bootRun.dependsOn removeDevManifestForBootRun`. `grails.assets.bundle=false`
(also set as a `bootRun` JVM arg) is still required alongside it — `AssetsTagLib`
forces digest-style URLs into rendered HTML unless `bundle=false`, which
would otherwise 404 against the live (non-digest-named) source files the
now-active dynamic filter path actually serves.

With this in place, the loop to see a Vue/SCSS/JS change under
`rundeckapp/grails-spa/packages/ui-trellis/src` is:

```bash
# 1. Edit a file under src/app or src/library

# 2. Recompile + copy into the Grails asset tree
./gradlew :rundeckapp:copySpa

# 3. Hard-reload the browser — no bootRun restart needed
```

Verified end-to-end: editing `HomeHeader.vue`, running `copySpa`, and
curling the already-running server picked up the change with zero restart
(see `HOT_RELOAD_HANDOFF.md` round 7).

No `assetClean`/`assetCompile`/`copyCompiledAssets` dance is required — those
still run as part of `bootRun`'s normal dependency chain, but their manifest
output is simply removed before it can be used for serving in dev mode.
Production/WAR builds (`./gradlew build`) are unaffected —
`removeDevManifestForBootRun` only runs as part of the `bootRun` task, and
`processResources`/packaging tasks depend on `copyAssetManifest` directly.

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