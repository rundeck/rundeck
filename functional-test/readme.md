# Rundeck functional tests

This uses docker compose test containers to spin up a docker container running Rundeck, and
runs Spock based API tests.

## Running against a docker image

This runs the tests against the `rundeck/rundeck:SNAPSHOT` docker image by default.
```
./gradlew :functional-test:{AVAILABLE_TEST_TASK}
```
If additional information is needed when running the task, you can the gradle flag "-i"
Available tasks listed below

Env vars to control docker image usage:

- `TEST_IMAGE`: The docker image to use (default: `rundeck/rundeck:SNAPSHOT`)
- `TEST_RUNDECK_CONTAINER_SERVICE`: The docker service to expose (default: `rundeck`)
- `TEST_RUNDECK_CONTAINER_PORT`: The port to expose on the host (e.g. 8080) (default: `4440`)
- `TEST_RUNDECK_CONTAINER_CONTEXT`: The context path to use for the container (e.g. `/rundeck`). Must start with `/`. (
  default: blank)
- `TEST_RUNDECK_CONTAINER_TOKEN`: The API token to use for authentication. (default: `admintoken`)
- `TEST_RUNDECK_GRAILS_URL`: This value is used as `RUNDECK_GRAILS_URL` (default: `http://localhost:4440`)
- `TEST_TARGET_PLATFORM`: The target platform for the rundeck container  (default: `linux/amd64`)
- `TEST_WAR_FILE_LOCATION`: (optional for tomcatTest task) If set, it will use this path to get the war file and run the tomcatTest
  task, if not it will try to get the war from the build process

## Running against external URL

You can run the tests against an external Rundeck instance by setting these env vars:

* `TEST_RUNDECK_URL`: The URL of the Rundeck instance
    * This should be the base URL of the server, but not include the `/api` path
* `TEST_RUNDECK_TOKEN`: The API token to use for authentication
* `RUNDECK_TEST_USER`: The username to use for authentication (default: `admin`)
* `RUNDECK_TEST_PASS`: The password to use for authentication (default: `admin123`)

## Test using local docker compose

By default the testdeck framework will attempt to use the local docker engine and docker compose applications
to start the docker-compose files. If you want to revert to using containerized docker-compose, you can set the
following environment variable. Note that this may require to manually pull some images before running the tests.

* `USE_LOCAL_DOCKER_COMPOSE`: Set to `false` to use containerized docker-compose

## Selenium Tests headless mode

By default, testdeck will run selenium tests in headless mode, preventing the automated browser to pop up and steal
focus. If you want to see the browser running the tests, you can set the following environment variable:

* `TEST_SELENIUM_HEADLESS_MODE`: Set to `false` to run the tests in a visible browser.


## Adding a new test task

Create a new gradle test task where you must specify the docker compose file to use and the spock configuration

* `COMPOSE_PATH`: Relative path to the docker compose file
* `TEST_IMAGE`: The docker image to use (default: `rundeck/rundeck:SNAPSHOT`)
* `spock.configuration`: Relative path to the spock configuration file (spock-configs path)

## Adding a test

Create a new Spock "*Spec" class in the `src/test/groovy` directory.

Extend the `BaseContainer` class to share the same Rundeck container instance with all the tests.

All tests must be annotated so the spock configuration used for the test task knows which tests to run

# Current Rundeck Test tasks

* `apiTest`: It runs all tests annotated with @APITest
* `seleniumCoreTest`: It runs all tests annotated with @SeleniumCoreTest
* `tomcatTest`: It runs all tests annotated with @APITest using a rundeck running on tomcat

## Good Practices when writing functional tests

Following these practices helps reduce test flakiness and improves maintainability of functional tests.

### General Testing Principles (applies to both Selenium and API tests)

- **Test cleanup/teardown**: Clean whatever was turned on to avoid state pollution between tests. This helps prevent tests from interfering with each other and reduces flaky test failures.
- **Unique resource naming**: Use UUID-based names for projects/jobs to prevent collisions. Hardcoded names can cause conflicts when tests run in parallel or when cleanup fails.
- **DRY principle**: Go directly to the scenario being tested; assume prerequisite scenarios are covered by other tests. This reduces test execution time and makes tests more focused.
- **Archive reuse**: Avoid importing the same archive twice; re-importing deleted jobs can trigger errors. Create unique archives or use API-defined jobs for simpler scenarios.
- **Test consolidation**: Prefer single tests that assert multiple related things over many small separate tests. This reduces overhead and can improve test reliability.
- **Setup before testing**: Aim to have the app and data already configured before starting assertions. This makes tests faster and more reliable.

### Selenium Testing Guidelines (applies to Selenium tests only)

#### Wait Strategies

- **Prefer explicit waits over `Thread.sleep()`**: Using explicit waits helps reduce timing-related flakiness. Only use `Thread.sleep()` with `WaitingTime` constants for special cases like external system initialization where explicit waits cannot be used.
- **Implicit waits are globally configured**: The framework sets a global implicit wait in the BasePage constructor and uses `implicitlyWait(2000)` in `go()` methods. This is an established pattern. However, for specific element interactions, **prefer explicit waits** to ensure reliability and avoid unpredictable behavior.
- **Use explicit waits** provided by the Page Object base classes:
  - `waitForElementVisible()` - Wait for element to be present AND visible
  - `waitForElementToBeClickable()` - Wait for element to be visible AND enabled
  - `waitForElementAttributeToChange()` - Wait for specific attribute changes
  - `waitForNumberOfElementsToBeMoreThan()` - Wait for multiple elements to be present
  - `waitForNumberOfElementsToBe()` - Wait for the number of elements to match a specific count
  - `waitForTextToBePresentInElement()` - Wait for specific text to appear in an element
  - `waitForUrlToContain()` - Wait for the current URL to contain a specific substring
  - `waitForAttributeContains()` - Wait for an element's attribute to contain a specific value
  - `waitIgnoringForElementVisible()` - Wait for element to be visible, ignoring certain exceptions
  - `waitIgnoringForElementToBeClickable()` - Wait for element to be clickable, ignoring certain exceptions

#### Page Object Model

- **Keep selectors in Page Object classes**: Avoid putting CSS/XPath selectors directly in test files. All selectors belong in Page Object classes to improve maintainability.
- **Define selectors as `By` fields** at the top of Page Object classes:
  ```groovy
  By nodeDetailsTableBy = By.cssSelector(".popover-content .node-details-simple")
  By parameterKeyBy = By.cssSelector(".key")
  ```
- **Create getter methods** that include appropriate waits:
  ```groovy
  WebElement getNodeDetailsTable() {
      waitForPopoverToAppear()
      waitForElementVisible nodeDetailsTableBy
      el nodeDetailsTableBy
  }
  ```
- **Encapsulate interactions**: Complex operations should be methods in Page Objects, not inline in tests. This improves readability and reusability.
- **Reuse BasePage methods**: Check if BasePage already has the wait/interaction method you need before creating new ones. This reduces code duplication and maintains consistency.

#### Test Structure

- **Clear test structure**: Tests should read like documentation with clear when/then/expect blocks. This makes tests easier to understand and maintain.
- **Document methods**: Use Groovydoc comments to explain the purpose and behavior of Page Object methods.

#### Examples

✅ **Correct**:
```groovy
// In CommandPage.groovy
By nodeDetailsTableBy = By.cssSelector(".popover-content .node-details-simple")

WebElement getNodeDetailsTable() {
    waitForPopoverToAppear()
    waitForElementVisible nodeDetailsTableBy
    el nodeDetailsTableBy
}

// In CommandSpec.groovy
def nodeDetailsTable = commandPage.getNodeDetailsTable()
assert nodeDetailsTable.isDisplayed()
```

❌ **Incorrect**:
```groovy
// In test file - DON'T DO THIS
Thread.sleep(2000)  // Never use sleep!
def table = driver.findElement(By.cssSelector(".popover-content .node-details-simple"))  // Selector in test!
```

### Resource Management (applies to both Selenium and API tests)

- **Project cleanup preparation**: Before deleting a project, ensure all jobs have finished or disable schedules and executions. This prevents errors during cleanup and ensures proper test isolation.
- **Unique naming everywhere**: Use `UUID.randomUUID().toString()` for all resources created in tests. This prevents naming conflicts and makes tests more reliable.
- **Archive usage patterns**: Prefer archives when 2+ jobs are needed or when testing configs/ACLs; use API-defined jobs for simple single-job tests. Archives are more efficient for complex setups.
- **Runner cleanup**: Clean up runners using the API; avoid repeating tags for runners/jobs. This prevents resource leaks and test interference.
- **Feature flag handling**: After enabling feature flags, wait for them to take effect before proceeding with tests. This ensures tests run against the expected configuration.

### API Testing Guidelines (applies to API tests only)

- **API version management**: Bump API versions as needed when testing new endpoints or features. This ensures tests use the correct API version and catch version-specific issues.
- **Error code validation**: Verify the API returns correct HTTP status codes and handles cleanup properly. This helps catch API bugs and ensures proper error handling.
- **Cleanup strategies**: Use `cleanup:` blocks for per-test cleanup or `cleanupSpec:` for suite-level cleanup. Choose based on test dependencies and resource sharing needs.
- **No temporary test annotations**: Tests shouldn't rely on annotations for eventual removal; they should fail when their dependencies are removed. This ensures tests remain valid as the codebase evolves.