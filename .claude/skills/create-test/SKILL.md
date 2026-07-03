---
name: create-test
description: Create backend tests for Rundeck (unit, API, functional). Auto-loads testing guidelines. For frontend unit tests use write-jest-tests.
---

# Create Test Skill

## When to Use

- Adding backend unit tests (Spock)
- Implementing test-first workflow
- Adding API tests for new/modified endpoints
- Writing functional/integration tests
- Fixing bugs (write failing test first, then fix)

**For frontend unit tests**: Use `write-jest-tests` skill

## Process

### Phase 1: Load Context

**Automatically read:**
- `.claude/docs/testing-guidelines.md` - Complete testing guide (all test types)
- `.claude/docs/development-guidelines.md` - Code standards, testing philosophy

### Phase 2: Identify Test Type

Determine which test type(s) are needed:

**Unit Tests** - Functions, classes, services
- Spock (Groovy)
- Mock dependencies
- Test in isolation

**API Tests** - REST endpoints
- Testcontainers + Spock
- Test API version requirements
- Test request/response formats
- Located in module tests

**Functional Tests** - Feature integration with Docker
- Complete workflows with Rundeck in Docker
- Use API for setup/validation
- Test plugins, cluster, runners
- Located in `functional-test`

**Integration Tests** - External systems
- Docker containers (Testcontainers)
- LDAP, Email, Ansible, etc.
- Test external integrations

### Phase 3: Write Unit Tests (Spock)

#### Spock Framework

**Structure**: Use `given/when/then` blocks

```groovy
class UserServiceSpec extends Specification {
    
    def userService = new UserService()
    
    def "should return user when valid ID provided"() {
        given: "a valid user ID"
        def userId = 123
        
        when: "finding user by ID"
        def user = userService.findById(userId)
        
        then: "user is returned with correct ID"
        user != null
        user.id == userId
    }
    
    def "should throw exception when user not found"() {
        given: "an invalid user ID"
        def userId = 999
        
        when: "finding user by ID"
        userService.findById(userId)
        
        then: "exception is thrown"
        thrown(UserNotFoundException)
    }
}
```

**Mocking with Spock**:
```groovy
def "should call external service"() {
    given: "a mocked external service"
    def mockService = Mock(ExternalService)
    def userService = new UserService(mockService)
    
    when: "getting user data"
    userService.getUserData(123)
    
    then: "external service is called once"
    1 * mockService.fetchData(123) >> "user data"
}
```

**Data-Driven Tests** with `@Unroll`:
```groovy
@Unroll
def "should validate #scenario"() {
    expect:
    validator.isValid(input) == expected
    
    where:
    scenario        | input    | expected
    "valid email"   | "a@b.c"  | true
    "invalid email" | "abc"    | false
}
```

### Phase 4: Write API Tests

**Framework**: Testcontainers + Spock

**Purpose**: Test Rundeck server API behavior with real server

**CRITICAL**: Test API version requirements

```groovy
class ProjectApiSpec extends Specification {
    
    @Shared
    def container = new RundeckContainer()
    
    def "should return projects for API v44"() {
        when: "calling API v44"
        def response = client.get("/api/44/projects")
        
        then: "projects are returned"
        response.status == 200
        response.json.size() > 0
    }
    
    def "should reject for API v43"() {
        when: "calling API v43"
        def response = client.get("/api/43/projects")
        
        then: "request is rejected"
        response.status == 404
    }
}
```

**Test Requirements**:
1. ✅ New behavior works with NEW API version
2. ❌ New behavior does NOT work with OLD API version
3. ✅ Error conditions return correct responses
4. ✅ Success conditions work in all call patterns
5. ✅ Test multiple content types (JSON, XML)
6. ✅ Test multiple input formats

### Phase 5: Write Functional Tests

**Purpose**: Test complete features/workflows with Rundeck running in Docker

Functional tests run in `functional-test` module using Testcontainers to launch Rundeck instances and verify functionality through APIs.

#### Test Annotations

**@APITest** - Standard functional tests
- Extends `BaseContainer`
- Single Rundeck instance in Docker
- Tests features via API calls

#### Base Test Structure

```groovy
package org.rundeck.functional.api.myfeature

import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.annotations.APITest
import org.rundeck.util.container.BaseContainer

@APITest
class MyFeatureSpec extends BaseContainer {
    
    static final String TEST_PROJECT = "test-project"
    
    def setupSpec() {
        startEnvironment()  // Starts Rundeck in Docker
        setupProject(TEST_PROJECT)  // Creates test project
        
        // Optional: Configure feature flags
        def config = [
            [
                "key": "rundeck.feature.myFeature.enabled",
                "value": "true",
                "strata": "default"
            ]
        ]
        client.post("/config/save", config, Map)
        waitFeatureFlag("rundeck.feature.myFeature.enabled")
    }
    
    def cleanupSpec() {
        deleteProject(TEST_PROJECT)
    }
    
    def "feature works as expected"() {
        given: "test data setup"
        // Setup via API
        
        when: "performing action"
        def response = client.get("/api/endpoint", Map)
        
        then: "expected result"
        response.status == "success"
    }
}
```

#### Key Methods

**Environment Setup:**
- `startEnvironment()` - Starts Rundeck container
- `setupProject(projectName)` - Creates project
- `deleteProject(projectName)` - Deletes project (cleanup)

**API Calls:**
- `client.get(path, Type)` - GET request, parse as Type
- `client.post(path, body, Type)` - POST request
- `get(path, Type)` - Shorthand for client.get
- `doGet(path)` - Raw HTTP GET
- `doPost(path, body)` - Raw HTTP POST

**Utilities:**
- `waitFeatureFlag(key)` - Wait for config to propagate
- `loadKey(path, value, type)` - Create key storage entry
- `hold(seconds)` - Wait fixed time (use sparingly)
- `jsonValue(body)` - Parse JSON response

**Job Operations (JobUtils):**
- `JobUtils.createJob(project, yaml, client)` - Import job
- `JobUtils.waitForExecution(status, execId, client, timeout)` - Wait for execution
- `JobUtils.getExecutionOutputText(execId, client)` - Get logs

#### Example: Basic API Test

```groovy
@APITest
class SystemInfoSpec extends BaseContainer {
    
    def setupSpec() {
        startEnvironment()
    }
    
    def "system info returns valid response"() {
        when:
        def data = get("/system/info", Map)
        
        then:
        !data.error
        data.system.rundeck.apiversion.toInteger() >= 14
    }
}
```

#### Example: Plugin Integration Test

```groovy
@APITest
class JiraPluginSpec extends BaseContainer {
    
    static final String TEST_PROJECT = "jira-test"
    
    def setupSpec() {
        startEnvironment()
        setupProject(TEST_PROJECT)
        
        // Configure Jira URL at system level
        def config = [
            ["key": "jira.url", "value": "https://jira.example.com", "strata": "default"]
        ]
        client.post("/config/save", config, Map)
        waitFeatureFlag("jira.url", "https://jira.example.com")
        
        // Store credentials
        loadKey("jira.pass", "test-password", "password")
    }
    
    def cleanupSpec() {
        deleteProject(TEST_PROJECT)
    }
    
    def "jira plugin reads system config"() {
        given: "a job using Jira plugin"
        def jobXml = """<joblist>
          <job>
            <name>jira test</name>
            <sequence>
              <command>
                <node-step-plugin type='jira-create-issue'>
                  <configuration>
                    <entry key='summary' value='Test Issue' />
                    <entry key='password' value='keys/jira.pass' />
                  </configuration>
                </node-step-plugin>
              </command>
            </sequence>
          </job>
        </joblist>"""
        
        when: "importing and running the job"
        def importResult = JobUtils.createJob(TEST_PROJECT, jobXml, client)
        def jobId = importResult.succeeded[0].id
        def response = doPost("/job/${jobId}/executions", ["loglevel": "DEBUG"])
        def execId = jsonValue(response.body()).id as String
        
        then: "job starts successfully"
        execId != null
        
        when: "waiting for completion"
        def exec = JobUtils.waitForExecution(
            ExecutionStatus.FAILED.state,  // Expected to fail (no real Jira)
            execId,
            client,
            WaitingTime.EXCESSIVE
        )
        
        then: "execution attempted to use system config URL"
        exec.status == 'failed'
        def fullLog = JobUtils.getExecutionOutputText(execId, client)
        fullLog.contains("jira.example.com")
    }
}
```

#### Important Patterns

**Test Data Setup:**
- Use API to create test data (jobs, keys, config)
- Don't rely on UI or manual setup
- Use `setupProjectArchiveDirectoryResource()` for complex project setup

**Waiting:**
- Use `waitFeatureFlag()` for config propagation
- Use `JobUtils.waitForExecution()` for job completion
- Avoid `hold()` except for cluster sync scenarios

**Assertions:**
- Test through API responses
- Verify job execution logs with `getExecutionOutputText()`
- Check execution status, output, side effects

**Cleanup:**
- Always cleanup in `cleanupSpec()`
- Delete projects, jobs, keys created
- Tests should not leave artifacts

### Phase 6: Run Tests

```bash
# Backend unit tests
./gradlew test

# Specific module
./gradlew :module-name:test

# API tests
./gradlew :functional-test:apiTest

# Functional tests
./gradlew :functional-test:test
```

---

## Testing Philosophy

### How do you know you fixed the problem?

1. **Write a test that exemplifies the problem**
2. **Test should FAIL before implementing fix**
3. **After fix, test should PASS**

### How do you know new behavior works?

**Update existing tests** to cover new behaviors

### What if code has no existing tests?

**CRITICAL**: Add missing tests for existing code FIRST

1. Write tests for existing functionality
2. Verify code works as expected
3. You've now established baseline
4. Future changes that break functionality → tests fail

---

## Checklist

Before considering tests complete:

- [ ] Test type identified (Unit, API, Functional)
- [ ] Testing guidelines loaded and followed
- [ ] Tests written before implementation
- [ ] Tests follow proper patterns (given/when/then, Spock structure)
- [ ] All tests pass locally
- [ ] **Unit tests**: Proper mocking, test isolation
- [ ] **API tests**: Version requirements tested, error conditions covered
- [ ] **Functional tests**: Proper annotation (@APITest), cleanup in cleanupSpec()
- [ ] Edge cases covered
- [ ] Error conditions tested
- [ ] No flaky tests introduced
- [ ] Test names clearly describe what they test

---

## Common Mistakes to Avoid

❌ **Don't**:
- Skip tests for "simple" changes
- Write implementation before tests
- Test implementation details instead of behavior
- Leave commented-out test code
- Create flaky tests (random pass/fail)
- Over-mock (test real behavior when possible)
- **Functional tests**: Forget `cleanupSpec()` - leaves test data
- **Functional tests**: Use `hold()` instead of proper waits
- **Functional tests**: Create test data through UI (slow, brittle)
- **API tests**: Test only new API version (must test old version rejects)

✅ **Do**:
- Write test before implementation
- Test edge cases and error conditions
- Use clear test names that describe behavior
- Keep tests simple and focused
- Convert JUnit tests to Spock when modifying
- Make tests independent (no execution order dependencies)
- **Unit tests**: Mock at system boundaries, not internal interfaces
- **API tests**: Test version requirements (new works, old rejects)
- **Functional tests**: Setup via API, cleanup in cleanupSpec()
- **Functional tests**: Use proper annotations (@APITest)

---

## Integration with Other Skills

- **create-code**: Use this skill when implementing test-first workflow
- **create-api-endpoint**: API endpoints MUST have API tests (test version requirements)
- **create-plugin**: Plugins should have unit tests for functionality

---

## Resources

- `.claude/docs/testing-guidelines.md` - Complete testing guide
- `.claude/docs/development-guidelines.md` - Testing philosophy, code standards
- [Spock Framework](https://spockframework.org/) - Groovy testing framework
- [Testcontainers](https://testcontainers.com/) - Docker containers for testing
- Example tests: `functional-test/src/test/groovy/`
