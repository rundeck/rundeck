---
globs: ["functional-test/src/test/groovy/**/*.groovy"]
alwaysApply: false
applyTo: "functional-test/src/test/groovy/**/*.groovy"
---

# Selenium Rules

Reference: `.claude/docs/selenium-best-practices.md`

## Mandatory

1. **Page Object Model**: Tests MUST NOT call `driver.findElement()` or interact with `By.*` locators directly. All element access goes through a Page Object method.
2. **Avoid `Thread.sleep()`**: Do not use `Thread.sleep()` except with `WaitingTime` constants for special cases (e.g., external system initialization). Replace UI waits with `waitForElementVisible()`, `waitForElementToBeClickable()`, or `waitForCondition()`.
3. **Explicit waits only**: Every interaction that depends on dynamic state MUST be preceded by an explicit wait for the specific condition (visible, clickable, URL change, etc.).
4. **Inherit base classes**: Page Objects MUST extend `BasePage` — do not re-implement wait utilities.
5. **No sleep-based flake fixes**: If a test is flaky, find the race condition. Adding any form of arbitrary delay is not an acceptable fix.

## Before Completing
Verify all 5 rules are satisfied in the file. Fix any violations before responding.