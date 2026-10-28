package org.rundeck.util.annotations

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Selenium specs that run only in a container with the nextUiMode feature flag enabled
 * (Gradle task nextUiModeSeleniumCoreTest).
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@interface NextUiModeSeleniumCoreTest {

}
