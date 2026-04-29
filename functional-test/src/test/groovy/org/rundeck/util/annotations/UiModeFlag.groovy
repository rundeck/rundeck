package org.rundeck.util.annotations

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Marks a Spock spec or test method as covering a UI-mode flag feature
 * (nextUi / legacyUi URL param or cookie). Purely informational — never
 * skips or fails tests. Queried on-demand via {@code ./gradlew reportUiFlags}.
 */
@Target([ElementType.TYPE, ElementType.METHOD])
@Retention(RetentionPolicy.RUNTIME)
@interface UiModeFlag {
    /** Short kebab-case name identifying the feature under test, e.g. "workflow-tab". */
    String featureName()

    /** Lifecycle status of this UI flag. Defaults to NEXT_UI. */
    UiModeStatus status() default UiModeStatus.NEXT_UI

    /** Mechanism used to activate the flag. Defaults to URL_PARAM. */
    UiMechanism mechanism() default UiMechanism.URL_PARAM

    /** Optional Jira ticket tracking this flag's lifecycle, e.g. "RUN-4151". */
    String jiraTicket() default ""

    /** Optional free-text description providing context. */
    String description() default ""
}
