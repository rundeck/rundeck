# UI Mode Flag POC — Friction Log & Migration Playbook

Branch: `poc-ui-mode-flag` (OSS submodule + parent repo)
Generated: 2026-04-24

---

## Friction Log

| Scenario | Friction | Severity | Agent action |
|----------|----------|----------|--------------|
| Setup    | `poc/ui-mode-flag` branch name conflicts with existing `POC` file in `.git/logs/refs/heads/` on case-insensitive macOS HFS+ filesystem | Low | Used `poc-ui-mode-flag` (hyphen not slash) |
| Setup    | `./gradlew :functional-test:unitTest` picks up all API tests (which need a live Rundeck server) because they don't live under `selenium/` | Medium | Added `exclude '**/tests/functional/**'` in addition to `exclude '**/selenium/**'` |
| Setup    | Gradle wrapper tried to download on first invocation — sandbox network restriction | Low | Used `required_permissions: ["all"]` for all Gradle invocations |
| Setup    | `evaluationDependsOn(":rundeckapp")` in `functional-test/build.gradle` requires Java 17 (Grails 7) but default JVM was Java 11 | Medium | Prefixed all `./gradlew` calls with `JAVA_HOME=$(/usr/libexec/java_home -v 17)` |
| Scenario A | `visitSpec(null)` correctly triggers the `catch(Exception)` block — confirms fail-soft works for NPE | None | Used as the fail-soft test case |
| Scenario B | Gradle `--tests` flag does not support comma-separated patterns (`*SpecA*,*SpecB*`) — runs only the first or fails | Low | Used multiple `--tests` flags: `--tests "*UiModesBindingSpec*" --tests "*UiModesDefaultOnlySpec*"` |
| Scenario C | `Stub(RdClient)` fails with `CannotCreateMockException` — `RdClient` is a concrete class and Objenesis is not on the classpath | Medium | Used `getClient() >> null` (RdClient is never called in URL-path tests) |
| Scenario C | Deprecated `loadEditPath` call-site inventory: 2 remaining boolean-overload call-sites surface `@Deprecated` warnings at compile time — `rundeck/functional-test/.../ExportImportSpec.groovy:109` (4-arg form) and `pro-functional-test/.../ConditionalStepSpec.groovy:262` (3-arg form). All other call-sites already use the new `UiMode` overload (`JobEditSpec` migrated in Scenario H) | Low | Both will be migrated in RUN-4152 Spec 2 — for the POC, the deprecation surface area was confirmed minimal |
| Scenario E | No Pro-side `META-INF/services` directory exists at all (`pro-functional-test/src/test/resources/META-INF/services` does not exist) — the AC requirement "confirmed by removing any such file and re-running" is vacuously satisfied; the OSS extension is reachable purely via `testImplementation(project(':functional-test').sourceSets.test.output)` | None | No action needed; documented for clarity |
| Scenario G | `boolean annotationFound = false` declared as a local in `doLast` cannot be mutated from inside an anonymous `ClassVisitor` inner class body (Groovy/Java variable capture semantics) | Medium | Replaced all mutable scalar state with a single `Map info` (Maps are reference types, safely mutable from inner class methods) |
| Scenario G | `descriptor` parameter name in `visitAnnotation(String descriptor, boolean visible)` conflicts with `visitEnum(String name, String descriptor, String value)` inside the nested AnnotationVisitor — Groovy rejects it | Low | Renamed the `visitEnum` parameter to `desc` |
| Scenario F | After promotion cleanup, `legacyUi` references remain in `JobCreatePage.groovy`, `BasePage.groovy`, and `JobUploadPage.groovy` — these are page-object branches not tagged to any feature | High | Confirms need for `// @UiModeFlag-ref: <featureName>` comment convention on page-object branches |
| All      | Gradle daemons can hang silently between long runs when `evaluationDependsOn` is involved | Medium | Added `--no-daemon` to all test invocations |

---

## Migration Playbook

### Step 1 — Classify the target spec

Run:
```
rg -l "nextUi|legacyUi" rundeck/functional-test/src/test/groovy/org/rundeck/tests/functional/selenium/
rg -l "nextUi|legacyUi" pro-functional-test/src/test/groovy/
```

For each file found, classify by mechanism and status:

| Signal in spec | Mechanism | Initial status |
|---|---|---|
| `?nextUi=true` in URL or `nextUi << [true, false]` | URL_PARAM | NEXT_UI |
| `?legacyUi=true` in URL or `legacyUi << [false, true]` | URL_PARAM | PROMOTED |
| `driver.manage().addCookie(new Cookie("nextUi", ...))` | COOKIE | NEXT_UI |
| `loadEditPath(..., true, legacyUi)` (4-arg) | URL_PARAM | PROMOTED |

### Step 2 — Apply the annotation (class-level first)

```groovy
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.annotations.UiMechanism  // only if COOKIE

@UiModeFlag(
    featureName = "<kebab-case name>",
    status      = UiModeStatus.PROMOTED,        // or NEXT_UI
    jiraTicket  = "RUN-XXXX",
    description = "<brief context>"
)
class MySpec extends SeleniumBase { ... }
```

If different methods have different statuses, annotate methods instead of the class.

### Step 3 — Add `UI_MODES` constant and migrate `where:` blocks

Add at class level:
```groovy
import org.rundeck.util.gui.UiModes

// PROMOTED feature:
static final UI_MODES = UiModes.defaultAndLegacy()   // [[false], [true]]

// NEXT_UI feature:
static final UI_MODES = UiModes.nextUiAndDefault()    // [[true], [false]]
```

Replace every `where:` block:
```groovy
// Before:
where:
    legacyUi << [false, true]

// After:
where:
    [legacyUi] << UI_MODES
```

**Note on `@Stepwise` specs** (`BasicJobsSpec`, `JobsSpec`): `UI_MODES` works the same way, but
`@Stepwise` aborts on first failure so a failing legacy iteration will skip the remaining ones.
Pre-existing behaviour — do not change.

### Step 4 — Migrate `loadEditPath` call-sites

| Old call | New call |
|---|---|
| `loadEditPath(p, id)` | `loadEditPath(p, id, UiMode.DEFAULT)` |
| `loadEditPath(p, id, true)` (3-arg nextUi) | `loadEditPath(p, id, UiMode.NEXT_UI)` |
| `loadEditPath(p, id, true, legacyUi)` (4-arg) | `loadEditPath(p, id, legacyUi ? UiMode.LEGACY : UiMode.DEFAULT)` |
| `loadEditPath(p, id, true, !nextUi)` | `loadEditPath(p, id, nextUi ? UiMode.NEXT_UI : UiMode.LEGACY)` |

Import required: `import org.rundeck.util.gui.pages.jobs.UiMode`

### Step 5 — Tag page-object branches (CRITICAL — surfaced by Scenario F)

For every `if(legacyUi) { ... }` or `if(!legacyUi) { ... }` branch in page objects still used
by an annotated spec, add a comment tag so future cleanup scans can find orphan branches:

```groovy
// @UiModeFlag-ref: workflow-tab
if (legacyUi) {
    addSimpleCommandStepButton.click()
}
```

Grep to verify all branches are tagged:
```
rg "@UiModeFlag-ref:" rundeck/functional-test/src/test/groovy/org/rundeck/util/gui/pages/
```

**Affected page objects (found during Scenario F orphan scan):**
- `JobCreatePage.groovy` — `legacyUi` branches in `fillBasicJob` and step helpers
- `BasePage.groovy` — `withLegacyUi()` / `withNextUi()` helpers
- `JobUploadPage.groovy` — `legacyUi` branch

### Step 6 — Validate

Run in order:
```
./gradlew :functional-test:compileTestGroovy          # must succeed, no new deprecation warnings
./gradlew :functional-test:unitTest                   # must pass
./gradlew :functional-test:reportUiFlags              # spec must appear with correct metadata
./gradlew :functional-test:seleniumCoreTest --tests "*<SpecName>*"   # background + await
```

### Step 7 — Cleanup (when the legacy branch is removed)

1. Switch `UI_MODES` from `defaultAndLegacy()` to `defaultOnly()`
2. Remove all `if(legacyUi)` branches from the spec body
3. Remove the `@UiModeFlag` annotation
4. Find and remove all `// @UiModeFlag-ref: <featureName>` page-object branches:
   ```
   rg "@UiModeFlag-ref: <featureName>" rundeck/functional-test/src/test/groovy/
   ```
5. Re-run Step 6 validation — `reportUiFlags` must no longer list this feature

---

## Summary statistics (Scenario F diff)

| Metric | Value |
|---|---|
| Lines before promotion cleanup | 37 |
| Lines after promotion cleanup | 28 |
| Lines removed | 13 |
| Lines added | 4 |
| Net delta | -9 |
| Files touched | 1 (`SimulatedPromotionSpec.groovy`) |

### Page-object orphan reference count (Scenario F orphan scan)

Run: `rg -c "legacyUi|nextUi" rundeck/functional-test/src/test/groovy/org/rundeck/util/gui/pages`

| File | Reference count |
|---|---:|
| `JobCreatePage.groovy` | 78 |
| `SettingsBarPage.groovy` | 16 |
| `BasePage.groovy` | 7 |
| `JobShowPage.groovy` | 3 |
| `JobUploadPage.groovy` | 2 |
| `JobReferenceStep.groovy` | 2 |
| `JobStep.groovy` | 1 |
| `HomePage.groovy` | 1 |
| `JobListPage.groovy` | 1 |
| **Total page-object references** | **111 across 9 files** |

This is the surface area that requires `// @UiModeFlag-ref: <featureName>` tagging during the
full rollout (RUN-4152 Spec 2). The vast majority is concentrated in `JobCreatePage` (78/111 = 70%).

## reportUiFlags re-verification (after Scenario H)

After `JobEditSpec` migration in Scenario H, re-running `./gradlew :functional-test:reportUiFlags`
produces 4 rows with the `workflow-tab` row showing a real git age (`3 weeks ago`), confirming
the `git log --follow` age-derivation path works end-to-end on a real spec file:

```
Feature       Mechanism  Status    Jira      Spec                          Age
────────────  ─────────  ────────  ────────  ────────────────────────────  ───────────
binding-test  URL_PARAM  PROMOTED  -         UiModesBindingSpec            uncommitted
cookie-test   COOKIE     NEXT_UI   -         CookieModeSmokeSpec           uncommitted
smoke         URL_PARAM  PROMOTED  -         UiModeFlagExtensionSmokeSpec  uncommitted
workflow-tab  URL_PARAM  PROMOTED  RUN-4151  JobEditSpec                   3 weeks ago
```

## Decision

**PROCEED TO FULL ROLLOUT.** All scenarios A–H passed. All design claims validated:
- Extension loads without breaking tests ✓
- Data-driven `UI_MODES` constant works in `where:` blocks ✓
- `UiMode` enum produces correct URL shapes ✓
- `mechanism=COOKIE` threads through to reporting ✓
- Pro classpath inherits OSS extension via `sourceSets.test.output` — no duplication needed ✓
- Promotion diff is measurable and small (−9 lines) ✓
- `reportUiFlags` inline ASM task works and inventories all annotated specs ✓
- `JobEditSpec` selenium run passes both iterations with migrated code ✓ (see Scenario H result)
