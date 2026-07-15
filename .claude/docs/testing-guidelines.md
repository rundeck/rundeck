# Testing Guidelines

## Overview

All code changes MUST be accompanied by test code to verify behavior changes.

---

## Testing Philosophy

### How do you know you fixed the problem?

1. **Write a test that exemplifies the problem**
2. **Test should FAIL before implementing fix**
3. **After fix, test should PASS**

### What if code has no existing tests?

**CRITICAL**: Add missing tests for existing code FIRST

1. Write tests for existing functionality
2. Verify code works as expected
3. Establish baseline — future breakage will be caught

---

## Unit Tests (Required for All Code Changes)

### Backend: Spock + Groovy

**Framework**: [Spock Framework](https://spockframework.org/)

- Use Spock for all Groovy/Java tests
- Convert existing JUnit tests to Spock when modifying
- Delete replaced JUnit test cases

**Example**:
```groovy
def "should return user when valid ID provided"() {
    given: "a valid user ID"
    def userId = 123
    
    when: "finding user by ID"
    def user = userService.findById(userId)
    
    then: "user is returned"
    user != null
    user.id == userId
}
```

### Frontend: Jest

All JavaScript/TypeScript changes require Jest unit tests.

**Complete guide**: **`.claude/docs/jest-testing-guidelines.md`**

---

## API Tests (Required for API Changes)

**Framework**: Testcontainers + Spock

**CRITICAL**: Test required API version of change

1. ✅ New behavior works with new API version
2. ❌ New behavior does NOT work with older API version
3. ✅ Error conditions return correct responses

---

## UI Tests

### Vue Components (Unit)

- **Location**: `tests/` directory alongside components
- **File Name**: `Component.spec.ts`

### Selenium Tests (End-to-End)

- **Framework**: Testcontainers + Spock + Selenium
- **Location**: `functional-test/`
- **Complete guide**: **`.claude/docs/selenium-best-practices.md`**

---

## Functional Tests

- **Location**: `functional-test/`
- **When to add**: New plugins, modified plugin behavior, new application features

---

## Integration Tests (3rd Party Components)

Purpose: Test integration between Rundeck and LDAP, Email, Ansible, etc.

Use Docker containers (Testcontainers) alongside Rundeck.

---

## Best Practices

✅ Test edge cases and error conditions  
✅ Use clear test names that describe behavior  
✅ Convert JUnit tests to Spock when modifying backend code  

❌ Skip tests for "simple" changes  
❌ Test implementation details (test behavior)  
❌ Create flaky tests  

---

## Resources

- **`.claude/docs/jest-testing-guidelines.md`** — Frontend testing (Jest + Vue)
- **`.claude/docs/selenium-best-practices.md`** — E2E testing (Selenium + Page Objects)
- **`.claude/docs/build-commands.md`** — All test execution commands
- [Spock Framework](https://spockframework.org/)
- [Vue Test Utils](https://test-utils.vuejs.org/guide/)
- [Testcontainers](https://testcontainers.com/)