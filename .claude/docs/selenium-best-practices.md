# Selenium Best Practices

Guidelines for writing maintainable, reliable Selenium functional tests.

## Core Principles

### 1. Page Object Model (Strict)

**REQUIRED**: All Selenium tests MUST follow the Page Object Model pattern.

- Page Objects encapsulate page structure and interactions
- Tests should only call methods on Page Objects, never interact with elements directly
- Each page or component gets its own Page Object class

**Example:**
```groovy
// ❌ BAD - Direct element interaction in test
def "test login"() {
    when:
    driver.findElement(By.id("username")).sendKeys("admin")
    driver.findElement(By.id("password")).sendKeys("password")

    then:
    // assertions
}

// ✅ GOOD - Using Page Object
def "test login"() {
    when:
    loginPage.login("admin", "password")

    then:
    dashboardPage.isDisplayed()
}
```

### 2. No Thread.sleep()

**NEVER** use `Thread.sleep()` in Selenium tests.

**Why:** Makes tests slow and flaky. Wait times are arbitrary and may not match actual conditions.

**Instead:** Use explicit waits from Page Object base classes:
- `waitForElementVisible()`
- `waitForElementClickable()`
- `waitForCondition()`

### 3. Explicit Waits

Always wait for specific conditions, not arbitrary timeouts.

```groovy
// ❌ BAD
Thread.sleep(5000)
element.click()

// ✅ GOOD
waitForElementClickable(element)
element.click()
```

---

## General Testing Principles

These apply to both Selenium and API tests:

### Test Cleanup/Teardown
Clean whatever was turned on to avoid state pollution between tests. This helps prevent tests from interfering with each other and reduces flaky test failures.

```groovy
cleanup:
    // Clean up resources created in this test
    projectService.deleteProject(projectName)
    
cleanupSpec:
    // Clean up resources shared across test suite
    runnerService.cleanupRunners()
```

### Unique Resource Naming
Use UUID-based names for projects/jobs to prevent collisions. Hardcoded names can cause conflicts when tests run in parallel or when cleanup fails.

```groovy
// ✅ GOOD
def projectName = "test-project-${UUID.randomUUID()}"

// ❌ BAD
def projectName = "test-project"
```

### DRY Principle
Go directly to the scenario being tested; assume prerequisite scenarios are covered by other tests. This reduces test execution time and makes tests more focused.

```groovy
// ✅ GOOD - Test focused on one scenario
def "delete job removes it from project"() {
    given: "a job exists"
    def jobId = createTestJob()
    
    when: "job is deleted"
    deleteJob(jobId)
    
    then: "job no longer exists"
    !jobExists(jobId)
}

// ❌ BAD - Testing multiple unrelated scenarios
def "complete job lifecycle"() {
    // Tests creation, update, execution, AND deletion
    // If this fails, hard to know which part broke
}
```

### Archive Reuse
Avoid importing the same archive twice; re-importing deleted jobs can trigger errors. Create unique archives or use API-defined jobs for simpler scenarios.

**When to use archives:**
- 2+ jobs needed
- Testing configs/ACLs
- Complex job definitions

**When to use API:**
- Simple single-job tests
- Quick setup scenarios

### Test Consolidation
Prefer single tests that assert multiple related things over many small separate tests. This reduces overhead and can improve test reliability.

```groovy
// ✅ GOOD - Related assertions in one test
def "job execution records all metadata"() {
    when:
    def execution = executeJob(jobId)
    
    then: "execution has expected metadata"
    execution.id
    execution.status == "succeeded"
    execution.dateStarted
    execution.dateCompleted
    execution.user == "admin"
}

// ❌ BAD - Separate tests that could be one
def "job execution has id"() { /* ... */ }
def "job execution has status"() { /* ... */ }
def "job execution has dates"() { /* ... */ }
```

### Setup Before Testing
Aim to have the app and data already configured before starting assertions. This makes tests faster and more reliable.

```groovy
// ✅ GOOD
def setup() {
    projectName = "test-${UUID.randomUUID()}"
    createProject(projectName)
    jobId = createJob(projectName)
}

def "execute job succeeds"() {
    when:
    def execution = executeJob(jobId)  // App ready, test starts
    
    then:
    execution.status == "succeeded"
}
```

---

## Selenium-Specific Guidelines

### Wait Strategies

#### Available Wait Methods

The framework provides explicit wait utilities in base classes:

| Method | Use Case |
|--------|----------|
| `waitForElementVisible(By)` | Element present AND visible |
| `waitForElementToBeClickable(WebElement)` | Element visible AND enabled |
| `waitForElementAttributeToChange(By, String, String)` | Wait for attribute value change |
| `waitForNumberOfElementsToBeMoreThan(By, int)` | Multiple elements present |
| `waitForNumberOfElementsToBe(By, int)` | Exact element count |
| `waitForTextToBePresentInElement(By, String)` | Text appears in element |
| `waitForUrlToContain(String)` | URL contains substring |
| `waitForAttributeContains(WebElement, String, String)` | Attribute contains value |
| `waitIgnoringForElementVisible(By)` | Wait ignoring certain exceptions |
| `waitIgnoringForElementToBeClickable(By)` | Wait ignoring certain exceptions |

#### When to Use Thread.sleep()

**Only use `Thread.sleep()` with `WaitingTime` constants** for special cases like external system initialization where explicit waits cannot be used.

```groovy
// ✅ Acceptable - External system initialization
Thread.sleep(WaitingTime.LONG.milliSeconds)

// ❌ NEVER - Waiting for UI elements
Thread.sleep(2000)
element.click()
```

#### Implicit vs Explicit Waits

- **Implicit waits** are globally configured in `BasePage` constructor and `go()` methods
- **For element interactions, prefer explicit waits** to ensure reliability

```groovy
// Framework sets implicit wait globally
implicitlyWait(2000)  // In BasePage

// But for interactions, be explicit
waitForElementToBeClickable(submitButton)
submitButton.click()
```

### Page Object Model Best Practices

#### Define Selectors as Fields

Keep all selectors at the top of Page Object classes:

```groovy
class CommandPage extends BasePage {
    // Selectors defined as By fields
    By nodeDetailsTableBy = By.cssSelector(".popover-content .node-details-simple")
    By parameterKeyBy = By.cssSelector(".key")
    By submitButtonBy = By.id("submit-button")
    
    // ... methods below
}
```

#### Create Getter Methods with Waits

Encapsulate element access with appropriate waits:

```groovy
WebElement getNodeDetailsTable() {
    waitForPopoverToAppear()
    waitForElementVisible nodeDetailsTableBy
    el nodeDetailsTableBy
}

WebElement getSubmitButton() {
    waitForElementToBeClickable submitButtonBy
    el submitButtonBy
}
```

#### Encapsulate Interactions

Complex operations should be methods, not inline in tests:

```groovy
// ✅ GOOD - In Page Object
class JobEditPage extends BasePage {
    void fillJobDetails(String name, String description) {
        waitForElementVisible(nameInputBy)
        el(nameInputBy).sendKeys(name)
        el(descriptionInputBy).sendKeys(description)
    }
    
    void saveJob() {
        waitForElementToBeClickable(saveButtonBy)
        el(saveButtonBy).click()
        waitForUrlToContain("/job/show")
    }
}

// In test
jobEditPage.fillJobDetails("My Job", "Description")
jobEditPage.saveJob()

// ❌ BAD - Inline in test
driver.findElement(By.id("name")).sendKeys("My Job")
driver.findElement(By.id("description")).sendKeys("Description")
driver.findElement(By.id("save")).click()
```

#### Reuse BasePage Methods

Check `BasePage` for existing wait/interaction methods before creating new ones. This reduces code duplication and maintains consistency.

**Common BasePage methods:**
- `el(By)` - Find element
- `waitForElement(By)` - Wait and find
- `waitForUrlContains(String)` - Wait for URL change
- `waitForPopoverToAppear()` - Wait for popover

### Test Structure

Tests should read like documentation with clear blocks:

```groovy
def "execute job with node filter succeeds"() {
    given: "a job with node filter exists"
    def jobId = createJob(projectName)
    def nodeFilter = "tags: web"
    
    when: "job is executed with node filter"
    commandPage.go(projectName)
    commandPage.selectNodes(nodeFilter)
    commandPage.executeCommand("echo test")
    
    then: "execution succeeds on matching nodes"
    def execution = commandPage.getLastExecution()
    execution.status == "succeeded"
    execution.nodeCount == expectedNodeCount
    
    and: "output contains expected text"
    execution.output.contains("test")
}
```

### Document Methods

Use Groovydoc to explain Page Object methods:

```groovy
/**
 * Waits for the node details popover to appear and returns the details table element.
 * 
 * @return WebElement representing the node details table
 * @throws TimeoutException if popover doesn't appear within timeout
 */
WebElement getNodeDetailsTable() {
    waitForPopoverToAppear()
    waitForElementVisible nodeDetailsTableBy
    el nodeDetailsTableBy
}
```

---

## Resource Management

### Project Cleanup Preparation

Before deleting a project, ensure all jobs have finished or disable schedules and executions. This prevents errors during cleanup and ensures proper test isolation.

```groovy
cleanup:
    // Disable schedules before cleanup
    disableAllSchedules(projectName)
    
    // Wait for running executions
    waitForExecutionsToComplete(projectName)
    
    // Now safe to delete
    deleteProject(projectName)
```

### Runner Cleanup

Clean up runners using the API; avoid repeating tags for runners/jobs. This prevents resource leaks and test interference.

```groovy
cleanupSpec:
    // Clean runners created during tests
    runnerService.listRunners().each { runner ->
        if (runner.tags.contains(TEST_TAG)) {
            runnerService.deleteRunner(runner.id)
        }
    }
```

### Feature Flag Handling

After enabling feature flags, wait for them to take effect before proceeding with tests. This ensures tests run against the expected configuration.

```groovy
given: "feature flag is enabled"
featureFlagService.enable("new-feature")
Thread.sleep(WaitingTime.SHORT.milliSeconds)  // Allow flag to propagate

when: "using the new feature"
// ... test code
```

---

## Additional Resources

### Related Documentation
- **Testing Guidelines**: `.claude/docs/testing-guidelines.md`
- **Good Practices Draft**: https://github.com/rundeck/rundeck/compare/good-practices-functional-tests

### Page Object Base Classes

Our Page Objects inherit common wait methods:
- `BasePage` - Common functionality for all pages
- `BaseComponent` - Reusable UI components

Check existing Page Objects in `functional-test/src/test/groovy/pages/` for patterns.

## Common Patterns

### Waiting for AJAX/Dynamic Content

```groovy
// Wait for element to appear
waitForElementVisible(By.id("dynamicContent"))

// Wait for element to be clickable
waitForElementClickable(submitButton)

// Wait for custom condition
waitForCondition { driver.findElements(By.className("item")).size() > 0 }
```

### Handling Modals

```groovy
// Wait for modal to appear
waitForElementVisible(modalDialog)

// Interact with modal
modalDialog.fillForm(data)
modalDialog.clickSubmit()

// Wait for modal to close
waitForElementNotVisible(modalDialog)
```

### Verifying Test State

```groovy
// Wait for page transition
waitForUrlContains("/dashboard")

// Verify element state
assert waitForElementVisible(successMessage).isDisplayed()
```

## Test Organization

### Test File Location
- **Path**: `functional-test/src/test/groovy/`
- **Naming**: `*Spec.groovy` (Spock convention)

### Page Object Location
- **Path**: `functional-test/src/test/groovy/pages/`
- **Naming**: `*Page.groovy` or `*Component.groovy`

## Debugging Failing Tests

When a Selenium test fails:

1. **Check screenshots** - Tests automatically capture on failure
2. **Review logs** - Look for JavaScript errors or network issues
3. **Verify waits** - Are explicit waits long enough?
4. **Reproduce locally** - Run with visible browser (not headless)
5. **Check timing** - Is there a race condition?

**Never** add arbitrary sleeps to "fix" flaky tests. Find and fix the root cause.
