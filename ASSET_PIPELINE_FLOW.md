# How assets reach a running rundeckpro instance

This maps the full journey of a static asset (JS/CSS/image/etc.) from source
to the browser, across **both** asset sources that make up a rundeckpro
build: the OSS `rundeckapp` SPA/assets, and the commercial `rundeckpro-*`
plugin assets. Written while debugging `bootRun` asset hot-reload — see
`HOT_RELOAD_HANDOFF.md` for the specific bug this was investigating.

Verified against the running app (`rundeckapp.Application`, PID confirmed via
`jcmd`), source-level (`asset-pipeline-{core,grails,gradle}` 5.0.35,
`grails-core` 7.2.2), and this repo's actual `build.gradle` files — not just
inferred from docs.

## The big picture

There is **one running JVM** (`rundeckapp.Application`, launched via
`./gradlew :rundeckapp:bootRun` from the `rundeckpro` root, or packaged as a
WAR for production). `rundeckapp` is *not* wrapped by some separate
"Enterprise" app — it boots directly as the Spring Boot application. All the
commercial `rundeckpro-*` modules (under `rundeckpro/plugins/*`) are added as
plain **jar dependencies on its classpath** — each one is itself a Grails
plugin, auto-discovered as a `BinaryGrailsPlugin` at startup.

This means assets reach the running app via **two independent build
pipelines** that both terminate in the same runtime resolver mechanism:

```
 rundeckapp's own SPA + classic assets  ──┐
                                          ├──> AssetPipelineConfigHolder.resolvers ──> AssetPipelineFilter ──> browser
 each rundeckpro-* plugin's own assets ──┘        (built at app startup)
```

## Pipeline 1: rundeckapp's own assets (OSS)

Source: `rundeckapp/grails-spa/packages/ui-trellis/src/{app,library}` (Vue
SPA) plus classic assets directly under `rundeckapp/grails-app/assets/**`
(js/css/images not part of the Vue SPA).

Gradle tasks, in dependency order (all defined in
`rundeckapp/build.gradle` unless noted):

1. **`:rundeckapp:grails-spa:packages:ui-trellis:runNpmBuild`** — webpack/vue-cli
   build of the SPA. Reads `src/app` + `src/library` (`grails-spa/build.gradle`
   declares both as task `inputs.dir`s — a library-only edit not being
   declared here was the round-1 bug already fixed). Output lands in
   `grails-spa/build/spa/**` (referred to as `gradle-build/spa` in some
   listings).
2. **`:rundeckapp:grails-spa:runNpmBuild`** — the parent module's own task,
   `dependsOn` the ui-trellis one above, re-exposes the same output under
   `grails-spa/build/spa`.
3. **`copySpa`** (`rundeckapp/build.gradle:806-812`) — `Copy` task,
   `from(configurations.spa) { include 'provided/**' }`,
   `into "$projectDir/grails-app/assets"`. This is the hand-off point from
   the SPA build into the classic Grails asset-pipeline world: after this
   runs, the compiled SPA output lives at
   `rundeckapp/grails-app/assets/provided/**`, alongside all the other
   classic (non-SPA) assets already sitting under `grails-app/assets/**`.
4. **`vendorAssetDeps`** — a separate `NpmTask` (`dependsOn copySpa`) that
   builds/copies vendor JS bundles into
   `grails-app/assets/javascripts/_package-manager/**`. Unrelated to the Vue
   SPA but shares the same `grails-app/assets` destination tree.
5. **`assetCompile`** (from the `asset-pipeline-gradle` plugin,
   `AssetForkedCompileTask`, `dependsOn copySpa, vendorAssetDeps`) — this is
   where `asset-pipeline` itself takes over. It walks every registered
   `AssetResolver` (by default just `grails-app/assets` for this module),
   compiles/minifies/digests every asset, and writes the result to
   `rundeckapp/build/assets/**`, plus `rundeckapp/build/assets/manifest.properties`
   (maps `static/css/pages/home.css` → `static/css/pages/home-<md5>.css` for
   every asset). The **runtime `CacheManager`** (`.assetcache`) used *during
   this compile* lives at `rundeckapp/build/.assetcache`
   (`AssetPipelinePlugin.groovy:53`,
   `config.cacheLocation = project.layout.buildDirectory.dir('.assetcache')`) —
   this is a Gradle-build-time cache, keyed by content MD5 + dependency MD5s,
   there purely to skip re-processing unchanged files across compiles.
6. **`copyAssetManifest`** + **`copyCompiledAssets`** (`rundeckapp/build.gradle:850-863`,
   both `dependsOn assetCompile`) — copy the compiled output into the
   classpath-visible location:
   - `build/assets/manifest.properties` → `build/resources/main/META-INF/assets/manifest.properties`
   - `build/assets/**` (minus the manifest) → `build/resources/main/assets/**`
7. **`processResources`** `dependsOn` both of the above; **`bootRun`**
   `dependsOn processResources` (and directly `dependsOn 'copySpa',
   copyRuntimeLibs, 'vendorAssetDeps', 'assetCompile'` too,
   `rundeckapp/build.gradle:479`) — so a plain `bootRun` invocation always
   re-runs the whole chain above (subject to normal Gradle up-to-date
   checks per task).
8. For a **production `war`/`bootJar`** build, `asset-pipeline-gradle`'s own
   wiring (`AssetPipelinePlugin.groovy`, the non-`packagePlugin` branch)
   bundles `assetCompile`'s output directly into the jar/war under
   `assets/**`, so the same compiled tree ends up on the classpath either
   way — `build/resources/main` in dev, inside the jar in prod.

## Pipeline 2: rundeckpro-\* plugin assets (commercial)

Source: each plugin under `rundeckpro/plugins/*` that has its own SPA/assets
(e.g. `rundeckpro-spa-ui`, `ui-job-list`, `ui-job-metrics`, ...) — each is
its own Gradle module with its own `assets { ... }` block
(`asset-pipeline-gradle` applied per-module) and, in `rundeckpro-spa-ui`'s
case, its own `copySpa` task copying its own SPA build into
`<module>/grails-app/assets/provided`.

The key difference from Pipeline 1: these modules set
**`packagePlugin = project.findProperty('packagePlugin') ?: false`**
(`rundeckpro-spa-ui/build.gradle:135`). When that property is true (e.g. on
a normal build/publish, verified against the actual built jar), the
`assetPluginPackage` task runs instead of the normal `assetCompile`-into-jar
path (`AssetPipelinePlugin.groovy`'s `if (extension.packagePlugin...)`
branch), and packages the plugin's compiled assets into
**`META-INF/assets/**`** inside that module's own jar — e.g. verified in
`rundeckpro-spa-ui-6.3.0-SNAPSHOT.jar`:
```
META-INF/assets/pro/ui-job-metrics/lib/jobMetricsWorker.js
META-INF/assets/pro/ui-components/ProJobData.umd.min.js
...
```
plus a `META-INF/assets.list` manifest file listing every packaged asset
path.

These jars then become plain dependencies on `rundeckapp`'s classpath
(visible directly in the running JVM's classpath, e.g.
`.../plugins/rundeckpro-spa-ui/build/libs/rundeckpro-spa-ui-6.3.0-SNAPSHOT.jar`).
**No copy step is needed at the `rundeckapp` level** — the assets travel
inside the jar itself.

## Where both pipelines converge: runtime resolution

At Spring context startup, `AssetPipelineGrailsPlugin.doWithApplicationContext()`
(`asset-pipeline-grails`) registers, **in this order**:

1. `'application'` — a `FileSystemAssetResolver` over
   `${BuildSettings.BASE_DIR}/grails-app/assets` — i.e. `rundeckapp`'s own
   live `grails-app/assets` directory (Pipeline 1's source location, *not*
   `build/resources/main`).
2. One `FileSystemAssetResolver` per exploded `BinaryGrailsPlugin` found on
   the classpath, pointed at `<plugin-project-dir>/grails-app/assets`.
   (Not relevant in this repo's normal bootRun/WAR setup, since the
   `rundeckpro-*` plugins ship compiled jars, not exploded plugin
   directories — but this is the mechanism that *would* apply if you ran a
   plugin's own module in isolation.)
3. `ClasspathAssetResolver('classpath', 'META-INF/assets', 'META-INF/assets.list')` —
   **this is how Pipeline 2's assets are actually discovered**: it reads the
   `META-INF/assets.list` file baked into every jar on the classpath and
   resolves asset paths from `META-INF/assets/**` inside those jars.
4. `ClasspathAssetResolver('classpath', 'META-INF/static')`,
   `ClasspathAssetResolver('classpath', 'META-INF/resources')` — standard
   Spring Boot static-resource classpath conventions, lower priority.

`fileForUri()` (`AssetHelper.groovy`) returns the **first** match across
this list, checked fresh on every single request — no resolver-level
caching.

## Request-time serving: two very different code paths

`AssetPipelineFilter.doFilterInternal()` branches on
`AssetPipelineConfigHolder.manifest` (populated only if
`grails.assets.useManifest` was true at startup):

- **Manifest/production mode** (`warDeployed = true`): looks up the
  requested path in the manifest to get the digest-named file, serves it
  through a static `ProductionAssetCache`, with a digest-embedded `ETag`
  and long-lived `Cache-Control` for hashed paths. Fast, but **the manifest
  is loaded once at startup — a code/asset change after boot is invisible
  until a restart**, regardless of resolver freshness.
- **Dynamic/dev mode** (`warDeployed = false`, i.e. `useManifest: false`):
  no manifest, no `ProductionAssetCache`. Every request re-resolves the file
  fresh via the resolver chain above and recompiles it through
  `AssetPipeline.serveAsset()` (backed by the same `CacheManager`
  `.assetcache`, but this one is content-MD5-checked per request, so it
  **does** pick up on-disk changes without a restart). Headers explicitly
  set `Cache-Control: no-cache, no-store, must-revalidate` and no `ETag`.

**This is the crux of the current bug** (see `HOT_RELOAD_HANDOFF.md` round
4): `rundeckapp/grails-app/conf/application.yml` has a block intended to
force dynamic mode when `bootRun` runs with `-Dgrails.env=development`, but
it was gated on the wrong mechanism (`spring.config.activate.on-profile`,
which needs a Spring profile that's never set) instead of the classic
Grails `environments: development:` key (which correctly reads
`-Dgrails.env`, confirmed via `grails-core`'s
`EnvironmentAwarePropertySource`). As a result, **every `bootRun` so far has
always been running in manifest/production mode**, which is why no-restart
edits never appeared — not a caching bug in the dynamic path itself, but the
dynamic path never being reached at all. Fixed in that file; not yet
verified live (needs a `bootRun` restart to re-test).

## Rendering: how a GSP/view actually emits the URL

`asset.pipeline.grails.AssetsTagLib`'s `javascript`/`stylesheet` tags (used
in GSPs, e.g. `<asset:stylesheet src="pages/home.css"/>`) build the final
`<script>`/`<link>` URL. Per its `element()` method: if `bundle:true`
(the project's non-gated default, `application.yml:210`, "Force use of
compiled/hashed assets even in development"), it **always** renders with
`useManifest=true` baked into the URL-generation logic regardless of the
`grails.assets.useManifest` config value — this is why round 1's fix needed
`bundle:false` *in addition to* `useManifest:false`: one config controls
what the tag renders as a URL, the other controls how the filter serves
whatever URL comes in.
