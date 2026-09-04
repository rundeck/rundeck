# Handoff: bootRun asset hot-reload investigation (round 2)

Branch: `hot-reload-fix`. Jira: **RUN-4892**.

This supersedes the previous version of this file. That version's root-cause
analysis (`bundle`/`enableDigests` vs `useManifest`) was partially wrong and
has been corrected below based on live testing against a running `bootRun`.
Read this whole file before touching code — the earlier "simple fix" (Tier A)
looked right on paper and in source-reading, but failed live testing in a way
that surfaced a second, undocumented caching layer.

## Current repo state (uncommitted, on `hot-reload-fix`)

```
 M .claude/docs/build-commands.md
 M rundeckapp/grails-app/conf/application.yml
 M rundeckapp/grails-spa/build.gradle
?? HOT_RELOAD_HANDOFF.md   (this file)
```

None of this has been committed. Do not commit until the remaining problem
(below) is resolved or you've deliberately decided to ship a partial fix.

### 1. `rundeckapp/grails-spa/build.gradle` — confirmed working, safe to keep

Added `inputs.dir 'packages/ui-trellis/src/library'` to the `runNpmBuild` task
(it only declared `src/app` before). **Verified live**: after this fix,
`./gradlew :rundeckapp:copySpa` correctly re-runs (not `UP-TO-DATE`) after a
library-only SCSS edit, without needing `--rerun-tasks`. This part is solid,
independent of everything else in this file — keep it regardless of how the
rest shakes out.

### 2. `rundeckapp/grails-app/conf/application.yml` — necessary but insufficient

```yaml
---
spring:
    config:
        activate:
            on-profile: development
grails:
    assets:
        useManifest: false
        bundle: false
---
```
This is a **new YAML document**, gated by `spring.config.activate.on-profile:
development` (only merges when `bootRun` runs with `-Dgrails.env=development`).
The original `grails.assets` block above it (`bundle: true`, `enableDigests:
true`, etc., ~line 209) is untouched, so production/WAR builds and any
non-`development`-profile run are unaffected — this part is confirmed safe.

**Why both keys are needed** (this took two live-test round-trips to find):
- `useManifest: false` alone did nothing when tested live, even with
  `-Dgrails.env=development` confirmed active on the JVM (checked via `ps`
  showing `-Dgrails.env=development` in the process args). Root cause: read
  `asset.pipeline.grails.AssetsTagLib.groovy` (source jar
  `cloud.wondrify:asset-pipeline-grails:5.0.35-sources.jar`) — the `element()`
  method computes:
  ```groovy
  final def nonBundledMode = uniqMode || (!AssetPipelineConfigHolder.manifest && bundle != true && attrs.remove('bundle') != 'true')
  if (! nonBundledMode) {
      output(src, '', attrs, '', true)   // useManifest HARDCODED true
  }
  ```
  With `bundle: true` (left at its default), `nonBundledMode` is always
  `false`, so the taglib **always** renders asset URLs with `useManifest=true`
  baked in, regardless of the `grails.assets.useManifest` config value. You
  need `bundle: false` too for `nonBundledMode` to ever become `true`, which
  is the only branch where `useManifest = !nonBundledMode` (i.e. `false`) is
  actually used for URL/digest generation.
- I initially also suspected the classic Grails `environments: development:`
  nested-key config format might not be honored under Grails 7.2's Spring-Boot
  config loading (this repo just upgraded to Grails 7.2.2). I switched to the
  modern `spring.config.activate.on-profile: development` format defensively.
  I did **not** conclusively prove the old nested-key format was broken —
  I only ever live-tested the *combination* of switching format AND adding
  `bundle: false` together, so it's possible the old format would also have
  worked once `bundle: false` was added. If picking this back up, it may be
  worth testing the classic `environments:` key format in isolation to see if
  it's actually necessary to use `spring.config.activate.on-profile`, or if
  that was a red herring. Low priority — the current format works and is
  arguably more idiomatic for Grails 7 anyway.

**Live-tested and confirmed working, for a restart**: after restarting
`bootRun` with `-Dgrails.env=development` and both `useManifest: false` +
`bundle: false` in place, editing `HomeHeader.vue`'s `.card` style and
restarting picked up the new color, with a freshly-computed (different)
asset filename hash. Verified via direct `curl` of the served CSS (not just
browser screenshot, to rule out browser caching).

## Where it breaks: no-restart hot-reload still does not work

This is the actual goal (see "Ideal outcome" below) and it's still broken.
Sequence that reproduces the failure (all done against the same already-running
`bootRun`, PID/start-time confirmed unchanged via `ps` before/after each step
— i.e. definitely no restart happened):

1. With `bootRun` up (post-restart, config above active), edit
   `rundeckapp/grails-spa/packages/ui-trellis/src/app/components/home/HomeHeader.vue`,
   changing `.homeHeader .card { background-color: ... !important; }` to a
   new color (e.g. green → blue).
2. Run `./gradlew :rundeckapp:copySpa`. Confirms it re-ran (not
   `UP-TO-DATE`), and `grails-app/assets/provided/static/css/pages/home.css`
   on disk **does** contain the new color (checked via `grep` directly on
   that file, fresh mtime confirmed).
3. `curl http://localhost:4440/assets/static/css/pages/home.css` (also tried
   the specific digest-named URL) still returns the **old** color. Confirmed
   via direct `curl`, not browser, so this isn't a browser cache artifact.
4. Ran `./gradlew :rundeckapp:assetCompile :rundeckapp:copyCompiledAssets`
   (the full compile+classpath-copy chain, still no `bootRun` restart).
   `rundeckapp/build/resources/main/assets/static/css/pages/home-<newhash>.css`
   gets produced with a **new** digest hash — but its **content** contains
   neither the old nor the new color; the `background-color` rule is missing
   entirely. This means the Gradle `assetCompile` task itself produced
   *wrong* output on this incremental re-run — not just "stale", but content
   that doesn't match either edit.
5. `curl` to that new-hash URL still returns the previous (stale, wrong)
   content from step 3, not even the wrong-but-different content from step 4.
   This is unexplained — worth re-checking with fresh eyes, may indicate an
   additional response-level cache (Spring `ResourceHttpRequestHandler`
   caching resolved `Resource` objects by path?) on top of everything else.

### Suspected cause: a third, undocumented cache

`AssetCompiler`'s Gradle task invocation
(`rundeckapp/build.gradle`'s `assetCompile` task, backed by
`cloud.wondrify`'s `AssetCompile`/`AssetForkedCompileTask` Gradle plugin
classes) passes a `cacheLocation` pointing at
`rundeckapp/build/.assetcache` (visible in the task's own command-line args
when run with output shown — it's a base64-encoded JSON blob, decode it to
see `"cacheLocation":".../rundeckapp/build/.assetcache"`). This is a
**separate, Gradle-build-time** content-hash cache, distinct from:
- the plugin's own runtime `CacheManager` (used by `AssetPipelineFilter`'s
  dynamic-serving path, keyed under a *different* `.assetcache` location
  resolved via `AssetPipelineGrailsPlugin.groovy` at Spring context init —
  confirmed via reading `AssetPipelineConfigHolder.config.cacheLocation =
  new File(BuildSettings.TARGET_DIR, CacheManager.CACHE_LOCATION)`), and
- whatever Spring/servlet mechanism actually serves `/assets/**` at runtime
  (not yet confirmed to be `AssetPipelineFilter` at all for this app's setup —
  see next section).

It's plausible the Gradle-side `.assetcache` is returning a stale/incorrect
cached compilation result for `home.css` on the incremental re-run in step 4,
independent of everything we changed in `application.yml`. This has **not**
been proven, only inferred from the wrong-content symptom. Next step here
would be to `rm -rf rundeckapp/build/.assetcache` and re-run `assetCompile`
to see if that's the culprit, then work out how (or whether) to invalidate
just the changed file's cache entry as part of a dev workflow instead of
nuking the whole cache each time (which would be slow and defeat the point).

### Also unconfirmed: what actually serves `/assets/**` at runtime

We never conclusively proved `AssetPipelineFilter` is even the thing serving
these requests in this app's configuration, versus Spring Boot's own static
resource handling serving directly from the `build/resources/main` classpath
directory that `bootRun` adds (`rundeckapp/build.gradle:499`,
`classpath += files("$buildDir/resources/main", ...)`). If it's the latter,
then `useManifest`/`bundle` config changes affect *URL generation* (taglib)
but might be irrelevant to *serving* — and the real fix might need to focus
entirely on making sure `build/resources/main/assets/**` gets refreshed
correctly and isn't cached by Spring's resource resolution. This needs
runtime introspection (e.g. temporarily adding a log statement inside
`AssetPipelineFilter` via a local jar patch, or attaching a debugger to the
running `bootRun` JVM, or checking Spring Boot's resource handler
registration for `/assets/**` in this app's config — grep for
`addResourceHandlers`, `ResourceHandlerRegistry`, `WebMvcConfigurer` in
`rundeckapp/grails-app`).

## Ideal outcome (unchanged from original ask)

Edit a file under `src/app` or `src/library` → run `./gradlew
:rundeckapp:copySpa` (or nothing at all, in a stretch "Tier B" with webpack
`--watch` piping straight into `grails-app/assets/provided`) → hard-refresh
browser → see the change. **No `bootRun` restart, ever**, for any number of
edits in a session.

## Suggested next steps, in order

1. `rm -rf rundeckapp/build/.assetcache` and retest the exact repro sequence
   above (steps 1-3, then re-run `assetCompile`/`copyCompiledAssets`, then
   `curl`) to see if that resolves the wrong-content-on-recompile symptom.
   If yes: figure out whether this cache is safe to always bypass in dev
   (e.g. a Gradle property to disable it for `assetCompile` specifically) or
   needs smarter invalidation.
2. Independently confirm what's actually serving `/assets/static/**` at
   runtime — `AssetPipelineFilter`'s dynamic path, or Spring's static
   resource handler reading straight from the classpath directory. This
   determines whether the `useManifest`/`bundle` config changes are even the
   right lever, or a red herring that happened to correlate with the restart
   picking up changes (because a restart *always* refreshes everything,
   masking whether the config change did anything at all for the *serving*
   side specifically).
3. Once the actual serving mechanism is confirmed, retest with a fresh
   `bootRun` restart + a single no-restart edit, checking content via `curl`
   at each step (not just the browser — browser caching and "Next UI" vs
   legacy component confusion both cost real time in this session; see
   below).
4. `.claude/docs/build-commands.md`'s new "Hot-Reloading SPA Assets During
   bootRun" section currently describes the **intended** (not yet working)
   end state as if it were confirmed. Fix or caveat that section once the
   real behavior is nailed down — right now it will mislead anyone who reads
   it before the fix actually works.
5. Once (1)-(4) are resolved and a real no-restart edit is verified via
   `curl`, re-verify in the browser via Chrome DevTools (see gotchas below),
   then do the full `./gradlew build -x check` sanity pass (per this repo's
   critical rules) to confirm packaged WAR/production behavior is unchanged
   before committing anything.

## Gotchas hit this session (save yourself the time)

- **This app has a "Next UI" mode toggle** (visible bottom-right in the
  Rundeck footer). The home page renders different Vue components depending
  on this mode. I edited `HomeHeader.vue` (legacy) and initially got confused
  when the visual change didn't appear — it wasn't a hot-reload failure at
  that point, it was editing a component not used by the active UI mode.
  **Prefer `curl`-ing the compiled CSS/JS directly** over relying on
  screenshots for hot-reload verification; it's faster and avoids this whole
  class of confusion. `HomeHeader.vue`'s `.homeHeader .card` selector is
  *not* scoped (no `data-v-*` attribute), so it does apply globally once
  served — the issue was purely "which component renders in this UI mode",
  not CSS scoping.
- **CSS minifier rewrites colors to keyword names**: `#ff00ff` → `#f0f` (and
  greps for `ff00ff` fail), `#00ff00` → `lime`. Grep for the property name
  (`background-color`) or the minified form, not just your original literal.
- **Any `bootRun` restart requires logging into Rundeck again** in the
  browser session (admin/admin locally) — the snapshot/session doesn't
  survive a JVM restart even though the browser tab/cookies do, because it's
  a new server-side session.
- **Confirm restart vs. no-restart explicitly** every time, via
  `lsof -tiTCP:4440 -sTCP:LISTEN | xargs ps -p ... -o pid,lstart` — don't
  assume from context. This was essential to trust the "no restart happened"
  claim during the failing no-restart repro.
- The very first live `bootRun` attempt from the CLI
  (`./gradlew :rundeckapp:bootRun -Dgrails.env=development` run directly from
  the `rundeck` repo root) failed with `FileNotFoundException:
  .../rundeck/templates` — this is an environment/invocation mismatch, not a
  code issue. **Always launch `bootRun` from the `rundeckpro` root** using
  the wrapper `settings.gradle` (matching the IntelliJ `Enterprise`/`Core`
  run configs, i.e. `-c $PROJECT_DIR$/settings.gradle` where `$PROJECT_DIR$`
  is the `rundeckpro` root), not directly inside the `rundeck` submodule
  checkout.

## Reference: source jars used for investigation

Both already extracted to `/tmp/claude-503/` this session (may not survive
across machines/sessions — re-extract if needed):
- `cloud.wondrify:asset-pipeline-grails:5.0.35-sources.jar` →
  `asset.pipeline.grails.AssetsTagLib`, `AssetProcessorService`,
  `AssetPipelineGrailsPlugin` (~/.gradle/caches/modules-2/files-2.1/cloud.wondrify/asset-pipeline-grails/5.0.35/)
- `org.apache.grails:grails-core:7.2.2-sources.jar` →
  `org.grails.config.EnvironmentAwarePropertySource`,
  `grails.boot.config.GrailsEnvironmentPostProcessor`
  (~/.gradle/caches/modules-2/files-2.1/org.apache.grails/grails-core/7.2.2/)

## Not yet done

- Tier B (webpack `--watch` writing straight into `grails-app/assets/provided`)
  was never attempted — blocked on getting Tier A's no-restart case working
  first, per the original plan's ordering.
- No `./gradlew build -x check` production-build sanity check has been run
  yet against the current `application.yml`/`build.gradle` changes.
