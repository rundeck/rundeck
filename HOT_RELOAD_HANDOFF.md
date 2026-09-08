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

---

# Round 3 handoff (source-code audit, no bootRun triggered)

This round did **not** touch the running `bootRun` process at all (explicit
instruction from the user — a live server was already running against a
*different* checkout, `~/dev/rundeckpro/rundeck`, on this same branch/commit,
and was left untouched). Everything below is static analysis of the actual
`asset-pipeline-grails`/`asset-pipeline-core`/`asset-pipeline-gradle` 5.0.35
source (pulled from the already-downloaded `-sources.jar`s in
`~/.gradle/caches/modules-2/files-2.1/cloud.wondrify/...`), plus filesystem
inspection of `~/dev/rundeckpro/rundeck/rundeckapp/build/**`. No code or
config was changed this round — see "Why nothing was changed yet" below.

## Confirmed from source (higher confidence than round 2's inference)

1. **The `useManifest:false`/`bundle:false` combo does route to the intended
   dynamic path.** `AssetPipelineGrailsPlugin.groovy:90-96`: if
   `useManifest` is false, `AssetPipelineConfigHolder.manifest` is never
   populated. `AssetPipelineFilter.groovy:40`
   (`warDeployed = AssetPipelineConfigHolder.manifest ? true : false`) then
   evaluates false, so **every** request falls through to the `else` branch
   at line 278, which calls `AssetPipeline.serveAsset()` — the per-request,
   no-manifest, dynamically-compiled path. This part of round 2's model is
   correct, not just empirically observed.
2. **The runtime `CacheManager` (the thing backing the `.assetcache` file)
   is content-hash-safe on paper.** `CacheManager.findCache()`
   (`CacheManager.groovy:47-73`) recomputes the MD5 of the live file *and*
   of every tracked dependency on every single call, and evicts on any
   mismatch. `AssetHelper.fileForUri()` (`AssetHelper.groovy:51-59`) does a
   fresh resolver lookup on every call too — there is no AssetFile-level
   cache sitting above `CacheManager`. **This means the documented
   cache-invalidation logic, read in isolation, should already handle a
   plain content edit correctly** — which does not match what round 2
   observed. I could not find a bug in this logic by reading it; if it's
   really the culprit, it's something more subtle than "the cache doesn't
   check content" (e.g. a key-collision, a resolver returning the wrong
   file for the same key, or a bug only reachable through the specific
   require-tree/bundling code path `home.css` uses — not verified either
   way).
3. **`FileSystemAssetResolver`'s subdirectory list is fixed at Spring-bean
   construction time** (`FileSystemAssetResolver.groovy:44-55`,
   `baseDirectory.listFiles()` runs once in the constructor). This only
   matters if a *new* subdirectory appears under `grails-app/assets` after
   `bootRun` starts (an existing directory being edited is fine) — noting
   this since it's a real, if narrow, restart-required case that isn't
   about caching at all.

## New finding: multiple stale duplicate `home.css` build artifacts exist

Ran a filesystem sweep in `~/dev/rundeckpro/rundeck/rundeckapp` (the checkout
actually backing the live `bootRun`) for every `home.css`/`home-*.css`.
Found **far more copies than expected**, with wildly different mtimes:

```
grails-app/assets/provided/static/css/pages/home.css          21:15:50  (live source, edited this session)
grails-spa/gradle-build/spa/provided/static/css/pages/home.css 21:15:47  (npm/webpack output, pre-copySpa)
build/assets/static/css/pages/home-15033c2f....css             20:44:46
build/assets/static/css/pages/home-7b08b0b3....css              21:17:00
build/assets/static/css/pages/home-89110725....css              18:20:10
build/assets/static/css/pages/home-f48211b9....css              18:57:24
build/resources/main/assets/static/css/pages/home-15033c2f....css  21:17:05
build/resources/main/assets/static/css/pages/home-4cd052a3....css  Aug 31 16:24  <- orphan, days old
build/resources/main/assets/static/css/pages/home-7b08b0b3....css  21:17:05
build/resources/main/assets/static/css/pages/home-89110725....css  21:17:05
build/resources/main/assets/static/css/pages/home-f48211b9....css  21:17:05
build/bootrun-assets/assets/static/css/pages/home-89110725....css  11:49:48  <- much staler than everything else
build/assets/manifest.properties -> currently points at home-7b08b0b3....css
```

`build/assets/` and `build/resources/main/assets/` never get pruned of old
digest-named files by `assetCompile`/`copyCompiledAssets` (each run just adds
a new hash file alongside old ones), so **you cannot use "which hash files
exist" as a freshness signal** — always check `build/assets/manifest.properties`
for the currently-active hash, and diff its *content* (not just its
existence) against what you expect.

### `build/bootrun-assets/` — investigated, inconclusive, do not assume it's the culprit

This directory looked like a strong lead at first (it's a **complete parallel
mirror** of the whole compiled asset tree, frozen at 11:49:48 — hours staler
than every other artifact, which would perfectly explain "recompiles happen
but old content keeps being served"). I could not find where this directory
comes from in source: it's not referenced anywhere in
`asset-pipeline-gradle` (`AssetPipelinePlugin.groovy`'s `configureBootRun()`
only adds *configuration classpaths*, not this directory), not in
`grails-gradle-plugins` sources, and not in any `.gradle`/`.groovy`/`.kts`
file anywhere in the `rundeckpro` tree (`grep -rl bootrun-assets` came back
empty everywhere except the directory itself). So its origin is unknown —
possibly an IntelliJ-managed working directory from an unrelated earlier
run config, possibly something else.

I tried to confirm/deny it's actually on the live JVM's classpath by
`ps -p 90068 -o pid,lstart,command`. The output was ambiguous — a single
90KB argv line that contains **both** `org.gradle.launcher.daemon.bootstrap.GradleDaemon`
*and* `rundeckapp.Application` substrings, which doesn't cleanly parse as
"this is process X's actual command line" (it may be the Gradle Daemon's own
line with an embedded description of a task it's running, not the forked
app JVM's own argv — needs a cleaner tool to disambiguate, not text grepping
a concatenated ps line). From what I could parse out, the classpath *does*
explicitly list `.../rundeckapp/build/resources/main` but I did **not** find
`bootrun-assets` in it. That's a point against it being the culprit, but the
parsing was not clean enough to treat as proof.

**Do not delete `build/bootrun-assets/` or anything else under
`~/dev/rundeckpro/rundeck/rundeckapp/build/` while that checkout's bootRun
is live** — I deliberately did not touch it, since deleting build outputs
out from under a running server (even ones that look orphaned) risks
breaking your current session.

### Resolver-shadowing across Grails plugins — mostly ruled out

Round 2 didn't check this; I considered it a strong lead going in
(`AssetPipelineGrailsPlugin.doWithApplicationContext()` registers a
resolver for the primary app's `grails-app/assets`, then one per
`BinaryGrailsPlugin` — order matters, first match wins). But the live
process's own args show `-Dbase.dir=.../rundeckpro/rundeck/rundeckapp` and
main class `rundeckapp.Application` — i.e. `rundeckapp` **is** the primary
Spring Boot app for this `bootRun`, not a plugin wrapped by some other
"Enterprise" module. So the `'application'` resolver already points at the
right live directory, ruling out the specific multi-module shadowing
scenario I'd guessed at in the previous message. **Not fully ruled out**:
whether any *other* Grails plugin on the classpath (there are several
first-party ones under `rundeckpro/plugins/*` and the rundeck submodule's
own `grails-*` modules) ships its own `grails-app/assets/static/css/pages/home.css`
— unchecked, low-probability but cheap to check (see next steps).

## Why nothing was fixed/changed this round

Every theory I could build confident evidence for either (a) turned out to
be already-correct-in-source (the cache invalidation logic), or (b) got
knocked down by a subsequent check (multi-module resolver shadowing;
`bootrun-assets` not conclusively on the classpath). I did not want to make
a speculative code/config change with no live way to verify it actually
fixes the no-restart case, per the instruction not to trigger `bootRun`.
The most responsible use of this round was narrowing the search space, not
guessing.

## Concrete next steps, in priority order, once bootRun is live on this branch

1. **Get a clean, unambiguous read of the live JVM's classpath and open file
   handles**, since the `ps` text-grep approach this round was inconclusive:
   ```bash
   jcmd <pid> VM.command_line          # clean argv/classpath, no concatenation issues
   lsof -p <pid> | grep -i 'assets\|home.css'   # confirms which physical files are actually open
   ```
   This settles definitively whether `build/bootrun-assets/` (or any other
   surprise directory) is in play.
2. **Reproduce the round-2 repro** (edit `HomeHeader.vue`'s color,
   `./gradlew :rundeckapp:copySpa`, `curl` the served CSS) and *before*
   concluding it's stale, check `build/assets/manifest.properties`'s
   `static/css/pages/home.css=` line — confirm whether the manifest hash
   changed at all. If it **didn't** change, the bug is upstream of serving,
   in `assetCompile` itself deciding nothing changed (a Gradle
   inputs/up-to-date problem, same species as the `runNpmBuild` /
   `src/library` bug already fixed in round 1 — check `assetCompile`'s
   declared `inputs`/`outputs` next). If it **did** change, the bug is
   downstream, in serving/resolving — proceed to step 1's `lsof` check to
   see which physical file got opened for that request.
3. **Check the other Grails plugins' asset directories** for a colliding
   `static/css/pages/home.css` (cheap, rules the last open theory in/out):
   ```bash
   find ~/dev/rundeckpro -path '*/grails-app/assets/static/css/pages/home.css' \
     -not -path '*/rundeck/rundeckapp/*'
   ```
4. Only once one of the above pinpoints the actual mechanism: implement the
   fix, then run the full `./gradlew build -x check` sanity pass (per this
   repo's critical rules) before committing anything, and fix/remove the
   `.claude/docs/build-commands.md` WIP banner once no-restart hot-reload is
   confirmed working end-to-end.

---

# Round 4 — root cause found and fixed (live bootRun, no restart yet re-tested)

The user restarted `bootRun` from `~/dev/rundeckpro/rundeck` on this branch.
With it live, I ran the round-3 "next steps" for real:

## Step 1 (clean classpath read) — `bootrun-assets` theory killed

`jcmd <pid> VM.command_line` (works fine live; the earlier `ps` attempt on
the now-dead process was just unreliable) shows the classpath does **not**
contain `build/bootrun-assets` anywhere. That directory is an orphan/red
herring — leave it alone, it's not part of this bug.

## The actual root cause: the dev-profile YAML block never activates

`curl -sD - http://localhost:4440/assets/static/css/pages/home.css` on the
freshly-restarted server returned:

```
ETag: "static/css/pages/home-89110725d402daffc4896d512a63febc.css"
Cache-Control: no-cache
Last-Modified: Fri, 04 Sep 2026 03:56:07 GMT
```

Per `AssetPipelineResponseBuilder.groovy` (asset-pipeline-core 5.0.35), an
ETag containing the **digest-named path** only happens when
`AssetPipelineConfigHolder.manifest.getProperty(manifestPath)` returns a
non-null value — i.e. **the manifest is loaded and populated**. If
`useManifest: false` had actually taken effect, `AssetPipelineFilter`'s
`warDeployed` (line 40) would be false and the dynamic `else` branch (line
278) would run instead, which never builds an ETag or ResponseBuilder at
all. So: **the dev-mode config block added in round 1 has never actually
been active**, on any of the "confirmed working after a restart" tests in
round 2 either — those just happened to look right because a fresh restart
always serves fresh manifest content, coincidentally masking that dev mode
never engaged.

**Why it never activates**: the block was gated with
```yaml
spring:
  config:
    activate:
      on-profile: development
```
which requires the **Spring Boot profile** "development" to be active
(`spring.profiles.active=development`). `jcmd <pid> VM.command_line` on the
live process shows `-Dspring.profiles.active` present with **no value** —
it's never set to `development`. Only `-Dgrails.env=development` is set
(a distinct, older Grails-specific mechanism). I verified in
`org.grails.config.EnvironmentAwarePropertySource` (grails-core 7.2.2
source) that the classic `environments.<name>.*` config key is resolved via
`grails.util.Environment.getCurrent()`, which reads `-Dgrails.env` —
completely independent of Spring profiles. **The gating key and the actual
runtime signal never matched.**

## Fix applied

Changed the block in `rundeckapp/grails-app/conf/application.yml` (both
checkouts, `~/dev/rundeckpro` and `~/dev/rundeckpro2`, kept in sync; **not
committed**, per the submodule rule) from the `spring.config.activate.on-profile`
form to the classic nested form:

```yaml
environments:
    development:
        grails:
            assets:
                useManifest: false
                bundle: false
```

This is gated on `-Dgrails.env=development`, which the IntelliJ run config
already sets correctly and reliably (confirmed via `jcmd`).

## Not yet verified — needs a bootRun restart to test

I have **not** restarted `bootRun` again to confirm this fixes it — that's
a multi-minute, disruptive action and should be a deliberate choice, not
something done silently mid-investigation. Once you restart on this branch:

1. Re-run the `curl` header check above. If the fix worked, the ETag should
   **not** contain a digest-named path, and `Cache-Control` should be
   `no-cache, no-store, must-revalidate` (the dynamic branch's literal
   header set) instead of just `no-cache`.
2. Only then move to the actual no-restart repro: edit a `src/app` or
   `src/library` file, `./gradlew :rundeckapp:copySpa`, `curl` again with no
   restart in between. This is the real test round 2 could never get past —
   now that dev mode will *actually* be engaged for the first time, it's
   possible this just works, or it's possible the original round-2 "wrong
   content" symptom (missing CSS rule after a full recompile) resurfaces for
   a different reason now that the correct code path is finally exercised.
   Don't assume it's fully fixed until this specific case is checked.
3. If step 2 still fails, the investigation continues from a much smaller,
   correctly-scoped starting point: dev mode is now definitely engaged, so
   any remaining staleness is a real bug in the dynamic-serving/CacheManager
   path itself, not a config-activation problem.

---

# Round 5 — round 4's fix didn't work; corrected, needs another restart to verify

User restarted `bootRun` on `~/dev/rundeckpro/rundeck` with round 4's
`environments: development:` YAML fix in place. Re-ran the same `curl`
header check:

```
ETag: "static/css/pages/home-89110725d402daffc4896d512a63febc.css"
Cache-Control: no-cache
```

**Identical to before the fix** — still manifest/production mode. The YAML
`environments:` key form did not activate.

## Why: this Grails 7 app's YAML config doesn't honor `environments:` the way I assumed

Checked whether the classic Grails `environments`-scoping mechanism works
*at all* in this app by looking for an existing, demonstrably-active example.
Found one: `rundeckapp/grails-app/conf/application.groovy` has a
**classic ConfigSlurper-style Groovy closure**,
`environments { development { grails.serverURL = ...; dataSource { url =
"jdbc:h2:file:./db/devDb" }; ... } }`. This is proven active — it's what
sets the dev H2 datasource URL and dev `serverURL` on every local run. So
`-Dgrails.env=development` **does** correctly scope config — but only
through this legacy `.groovy` ConfigSlurper closure form, not through a
`environments:` key written in `application.yml`. In this Grails
7/Spring-Boot config setup, the YAML nested-key form apparently isn't wired
through the same environment-scoping mechanism (`EnvironmentAwarePropertySource`
theoretically should handle it per its source, but empirically, live, it does
not take effect here — possibly because that PropertySource isn't part of
the chain Spring Boot's YAML loader actually builds `grailsApplication.config`
from in this Grails version; not root-caused further since a working
alternative was immediately available).

## Corrected fix

1. **Removed** the non-functional YAML block from
   `rundeckapp/grails-app/conf/application.yml`.
2. **Added** the same two settings into the *existing, proven-active*
   `environments { development { ... } }` closure in
   `rundeckapp/grails-app/conf/application.groovy`, right after the existing
   `spring.h2.console.enabled=true` line:
   ```groovy
   grails.assets.useManifest=false
   grails.assets.bundle=false
   ```
3. Updated `.claude/docs/build-commands.md`'s file/mechanism reference to
   point at `application.groovy`'s closure instead of `application.yml`.

All three files synced between `~/dev/rundeckpro` and `~/dev/rundeckpro2`
checkouts; none committed (submodule rule). YAML re-validated with
`python3 -c "import yaml; yaml.safe_load_all(...)"` after the removal — still
parses cleanly (6 documents).

## Still not verified — needs a third restart

Same verification procedure as round 4: after restart, re-run
```bash
curl -sD - -o /dev/null http://localhost:4440/assets/static/css/pages/home.css \
  | grep -iE 'cache-control|etag'
```
Expect **no** digest-named ETag and `Cache-Control: no-cache, no-store,
must-revalidate` (not just `no-cache`) if this activates correctly this
time. If it still shows the old manifest-mode signature, the next thing to
check is whether `AssetPipelineGrailsPlugin.doWithSpring()`'s
`config.getProperty('grails.assets', Map, [:])` call actually sees
`.groovy`-closure-scoped values merged in at the point it runs (plugin
`doWithSpring` closures execute quite early in context startup — worth
checking if `grailsApplication.config` is fully environment-merged by then,
via a temporary log line if this round's fix also fails).

---

# Round 6 — round 5's fix ALSO didn't work; switched to a JVM system property

User restarted again with round 5's `application.groovy` closure fix in
place. Same `curl` check, **exact same result again**:

```
ETag: "static/css/pages/home-89110725d402daffc4896d512a63febc.css"
Cache-Control: no-cache
```

Two independent environment-scoping mechanisms (`application.yml`'s YAML
`environments:` key, and `application.groovy`'s classic ConfigSlurper
`environments { development { ... } } ` closure) have now both failed to
affect `grails.assets.useManifest`/`bundle`, even though I confirmed the
`.groovy` closure form **is** active for other keys in the very same block
(`spring.h2.console.enabled=true` → verified `/h2-console` returns a live
302, not 404; `dataSource.url` → verified `rundeckapp/db/devDb.mv.db`
exists at the exact configured path).

## Working theory (not fully proven, but well-supported)

Traced `GrailsApplication.getConfig()` (`grails-core` 7.2.2,
`DefaultGrailsApplication.java:326-342`) — it lazily builds a
`PropertySourcesConfig` wrapping Spring's real `PropertySources`, the same
object backing generic `Environment.getProperty(...)` calls (which is what
resolves `spring.h2.console.enabled` correctly). So it's **not** a case of
"legacy Groovy config is a separate, disconnected object" — same underlying
source. That leaves plugin **load/read ordering** as the most likely
explanation: `AssetPipelineGrailsPlugin.doWithSpring()`
(`asset-pipeline-grails`) calls `application.config.getProperty('grails.assets',
Map, [:])` directly inside its `doWithSpring` closure, i.e. at Spring
bean-definition time. If that runs before whatever finalizes the
environment-scoped merge of file-based config (YAML `environments:` key or
`.groovy` `environments{}` closure) into the property sources
`PropertySourcesConfig` wraps, it would silently read the pre-merge,
env-agnostic value every time — explaining why the *unscoped* base
`grails.assets` block (bundle:true etc., always in effect regardless of env)
works fine, but every environment-scoped override of that same map has
failed. This wasn't proven with a debugger/log line (didn't want to burn
another restart on non-actionable instrumentation) — treat it as the
working theory that motivated round 6's fix, not a certainty.

## Round 6 fix: bypass config-merge timing entirely via a JVM system property

Reverted round 5's `application.groovy` closure addition (confirmed
ineffective, removed to avoid dead/misleading config). Instead, added the
override directly as JVM args on the `bootRun` Gradle task itself
(`rundeckapp/build.gradle`, inside the existing `jvmArgs(...)` call, right
after the existing `-XX:MaxMetaspaceSize` line):

```groovy
'-Dgrails.assets.useManifest=false',
'-Dgrails.assets.bundle=false'
```

Rationale: JVM system properties (`-D` flags) are wired into Spring's
`Environment` as one of the **highest-priority, earliest-available**
property sources — present from JVM/`Environment` construction, before any
custom YAML/Groovy file parsing happens. This sidesteps the suspected
ordering bug entirely, regardless of whether the theory above is exactly
right. It's also correctly scoped: this only affects the `bootRun` Gradle
task, so production/WAR builds (which don't invoke `bootRun`) are
unaffected by construction, without needing any `grails.env`/profile
gating at all.

Synced to both checkouts. `application.yml` and `application.groovy` are
now both back to their pre-investigation state (no dev-only asset overrides
left in either) — the *only* active override is the `bootRun` jvmArgs in
`build.gradle`.

## Still not verified — needs a fourth restart

Same check as before:
```bash
curl -sD - -o /dev/null http://localhost:4440/assets/static/css/pages/home.css \
  | grep -iE 'cache-control|etag'
```
If this **still** shows a digest-named ETag, the config-timing theory above
is wrong and the investigation needs to go a level deeper: add a temporary
log line inside a local copy of `AssetPipelineGrailsPlugin.doWithSpring()`
(or attach a debugger) to print `assetsConfig.useManifest` and
`assetsConfig.bundle` at the moment they're read, to see definitively what
value/timing is actually in play — stop guessing at config mechanisms and
get a direct empirical read at the exact point of failure.

---

# Round 7 — round 6 confirmed a JVM system property WAS set, still no effect; found and fixed the actual root cause

User restarted again with round 6's `bootRun` JVM-args fix in place.
`jcmd <pid> VM.command_line` confirmed `-Dgrails.assets.useManifest=false`
and `-Dgrails.assets.bundle=false` were genuinely present on the live
process. **Same result again** — byte-identical ETag/headers to every prior
round.

## Definitive, zero-restart proof of what's actually happening

Rather than form a fourth config theory, ran a test that doesn't depend on
guessing about config internals at all: edited
`grails-app/assets/provided/static/css/pages/home.css` **directly on disk**
(appended a comment marker) and `curl`'d immediately — **no Gradle task,
no restart**. If dynamic serving were active at all, `AssetPipeline.serveAsset()`
reads that exact file fresh on every request, so the marker would appear
instantly. **It did not appear.** This is airtight, mechanism-agnostic proof
that `warDeployed` (manifest mode) is active, independent of ETag/hash
reasoning. (Reverted the marker immediately after.)

Also ruled out two more theories empirically:
- **Wrong `asset-pipeline-grails` version resolved**: `./gradlew
  :rundeckapp:dependencies --configuration runtimeClasspath` confirms 5.0.35
  wins conflict resolution everywhere — the version I'd been reading source
  for the whole time was correct.
- **A Groovy Elvis-operator bug** (`assetsConfig.useManifest ?: true` would
  silently discard an explicit `false` and substitute the `true` default,
  since real Groovy `false` is itself falsy) — a real footgun in the
  library's own code, worth knowing about, but turned out not to be the
  actual blocker here (see below, the `if` never even reaches that
  evaluation in a way that matters once the real cause is fixed).

## Actual root cause, found by re-reading `AssetPipelineGrailsPlugin.doWithSpring()`'s manifest-file lookup, not just the useManifest flag

```groovy
manifestFile = applicationContext.getResource("assets/manifest.properties")
if (!manifestFile.exists()) {
    manifestFile = applicationContext.getResource("classpath:assets/manifest.properties")
}
...
if (useManifest && manifestFile?.exists()) { ... AssetPipelineConfigHolder.manifest = manifestProps }
```

Checked whether `manifestFile?.exists()` is even reachable as `true` in this
project's `bootRun` classpath, independent of `useManifest`. It is — found
in `rundeckapp/build.gradle`'s `bootRun` block:

```groovy
// Full resources/main so classpath:/assets/ resolves; duplicateFileMode=WARN avoids Liquibase failure on duplicate migrations
classpath += files("$buildDir/resources/main", "$buildDir/resources/main/META-INF")
```

This was added (per its own comment) to expose `META-INF/services/*`
ServiceLoader files (`PasswordUtilityEncrypter`, `PreBootstrap`,
`CredentialProvider`, `PluginProviderServices` — confirmed these are the
only other files under `build/resources/main/META-INF/`). Side effect:
adding `build/resources/main/META-INF` **itself** as a classpath root means
its `assets/manifest.properties` subpath (populated by the separate
`copyAssetManifest` task, `build/resources/main/META-INF/assets/manifest.properties`
— confirmed this is the *only* file under that `assets/` subfolder) becomes
directly reachable via a plain classpath lookup for `"assets/manifest.properties"`
— exactly the fallback path `AssetPipelineGrailsPlugin` checks. So
`manifestFile.exists()` is `true` on every single `bootRun`, **completely
independent of the `grails.assets.useManifest` config value** — explaining
why all three prior config-based fix attempts (YAML profile-gate, `.groovy`
environments closure, JVM system property) were each individually correct
in isolation but could never have worked, because they were solving the
wrong half of an `&&` condition. The manifest digest matching the current
file content on every restart (verified: freshly restarted process's
`build/assets/manifest.properties` mtime exactly matches boot time, and its
`home.css` hash matches what's served) also explains why round 2's "confirmed
working after a restart" observation was real and reproducible — manifest
mode correctly reflects current content on a fresh boot, by design; restarts
were never actually broken, only the *dynamic, no-restart* path was
unreachable.

## Round 7 fix

Kept round 6's `-Dgrails.assets.useManifest=false`/`-Dgrails.assets.bundle=false`
JVM args in place (harmless, and correctly scoped even if their effect was
previously masked). Added a new Gradle task in `rundeckapp/build.gradle`,
right after `copyAssetManifest`:

```groovy
task removeDevManifestForBootRun(type: Delete) {
    dependsOn copyAssetManifest
    delete "$buildDir/resources/main/META-INF/assets/manifest.properties"
}
bootRun.dependsOn removeDevManifestForBootRun
```

This deletes the one file that makes `manifestFile.exists()` true, ordered
to run after `copyAssetManifest` produces it and before `bootRun`'s own JVM
launches (via Gradle's task-graph `dependsOn` ordering — no risk of a later
task recreating it, since `copyAssetManifest` is the only writer and nothing
depends on `removeDevManifestForBootRun` in a way that would trigger a
rerun). Scoped to the `bootRun` task only — production/WAR packaging goes
through `processResources`/`copyAssetManifest` directly and is untouched, and
Gradle's own output-tampering detection will correctly re-copy the file on
any subsequent normal `./gradlew build`/`war` run regardless.

Verified the Groovy parses (`./gradlew :rundeckapp:tasks --all -q` lists the
new task with no errors) before asking for another restart — didn't want to
burn a fifth restart on a syntax typo.

Synced to both checkouts.

## Still not verified — needs a fifth restart

Same check as every prior round:
```bash
curl -sD - -o /dev/null http://localhost:4440/assets/static/css/pages/home.css \
  | grep -iE 'cache-control|etag'
```
This time, if `manifestFile.exists()` is genuinely false, `AssetPipelineConfigHolder.manifest`
should stay `null` for the entire life of the process — meaning the earlier
"does useManifest's value even matter" question becomes moot regardless of
the Elvis-operator concern above. If this **still** fails, the next
zero-restart diagnostic to reach for is the direct-file-edit test above
(cheap, decisive, no restart needed) rather than another config theory.

---

# Round 8 — CONFIRMED WORKING, including post-cleanup config, on a fresh restart

Round 7's live test (headers, direct-file-edit, `copySpa` real-workflow —
see above) passed, but only against that round's exact `build.gradle`, which
still had `-Dgrails.assets.useManifest=false` alongside `bundle=false`.
Afterward, while `bootRun` was down for unrelated cleanup, `useManifest` was
removed from the `bootRun` jvmArgs (reasoned from source to be dead weight —
`AssetsTagLib`'s `nonBundledMode` formula never references it, only
`bundle`) and `./gradlew build -x check` was run clean. **That trimmed
config had not actually been booted** — an earlier draft of this section
claimed it was "confirmed working end-to-end" before that restart happened,
which was wrong; corrected after the user caught it.

User then started a fresh `bootRun` specifically for this to be checked
properly. Confirmed via `jcmd <pid> VM.command_line` that only
`-Dgrails.assets.bundle=false` is present now (no `useManifest`) — i.e. this
really is the current file, not a stale process. Re-ran all three checks
against it:

1. **Header check**: `Cache-Control: no-cache, no-store, must-revalidate`,
   `Pragma: no-cache`, `Expires: 1970`, no ETag — same dynamic-mode
   signature as round 7.
2. **`META-INF/assets/`**: confirmed empty (emptied by
   `removeDevManifestForBootRun`).
3. **Direct-file-edit test**: appended a marker to `home.css` on disk,
   curled immediately (no Gradle task, no restart) — marker present.
   Reverted.
4. **Real workflow test**: edited `HomeHeader.vue`'s `.card` margin (`20px`
   → `22px`), ran `./gradlew :rundeckapp:copySpa`, curled the running
   server — `22px` came back immediately, no restart. Reverted the edit,
   re-ran `copySpa`, confirmed `git diff --stat` on that file is empty
   (clean revert).

**This is now genuinely verified for the exact file state currently on
disk** — not reasoned-from-source, not verified against a since-changed
config. The `useManifest` removal was correct.

## Current state

- `rundeckapp/build.gradle`: `removeDevManifestForBootRun` task +
  `bootRun` jvmArgs with only `-Dgrails.assets.bundle=false` (no
  `useManifest`) — this exact state is what was just verified above.
- `.claude/docs/build-commands.md`: WIP banner removed, describes the real
  mechanism.
- `application.yml` / `application.groovy`: back to pre-investigation
  state, no leftover dead config.
- Both checkouts (`~/dev/rundeckpro`, `~/dev/rundeckpro2`) in sync.
- Not yet committed.

## Still not done

- Never loaded a real authenticated Rundeck page through a browser or a
  session-authenticated `curl` to eyeball rendered `<link>`/`<script>` tags
  — all verification hit the asset endpoint directly. Given `bundle=false`
  is confirmed necessary and present, and the raw endpoint behavior is fully
  verified, this is a nice-to-have sanity check rather than a real risk, but
  it hasn't been done.
- Decide whether `HOT_RELOAD_HANDOFF.md` / `ASSET_PIPELINE_FLOW.md` (both
  untracked, repo root) belong in the final commit/PR or should be dropped
  once the fix itself is committed.
