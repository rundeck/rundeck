package org.rundeck.util.annotations

/**
 * Mechanism by which the UI-mode flag is activated in browser tests.
 */
enum UiMechanism {
    /** Flag activated via query string parameter (?nextUi=true or ?legacyUi=true). */
    URL_PARAM,

    /** Flag activated via browser cookie (driver.manage().addCookie(...)). */
    COOKIE
}
