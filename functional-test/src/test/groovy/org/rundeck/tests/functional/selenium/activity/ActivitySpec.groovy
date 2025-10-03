package org.rundeck.tests.functional.selenium.activity

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.execution.CommandPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class ActivitySpec extends SeleniumBase {
    static final String TEST_PROJECT = "ActivitySpec"

    def setupSpec() {
        setupProject(TEST_PROJECT)

        //create an execution
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def commandPage = go CommandPage, TEST_PROJECT
        commandPage.runCommandAndWaitToBe("echo 'Hello world'", "SUCCEEDED")
        commandPage.runCommandAndWaitToBe("echo 'Hello world2'", "SUCCEEDED")
    }

    def "activity page with default max shows all executions"() {
        given: "specific user logs in with limited authorization"
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            loginPage.waitForUrlToContain('/menu/home')
        when: "view activity page"
            def activityPage = go ActivityPage, TEST_PROJECT
            activityPage
                .waitForActivityRowsPresent(ActivityPage.ActivityType.ADHOC, ActivityPage.ActivityState.SUCCEEDED)
        then: "activity page shows 2 rows"
            activityPage
                .getActivityRows(ActivityPage.ActivityType.ADHOC, ActivityPage.ActivityState.SUCCEEDED).size() == 2
    }

    def "activity page with default max #max shows #expect rows"() {
        given: "specific user logs in with limited authorization"
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            loginPage.waitForUrlToContain('/menu/home')
        when: "view activity page"
            def activityPage = page ActivityPage
            activityPage.params = "?max=${max}"
            activityPage.loadActivityPageForProject(TEST_PROJECT)
            activityPage.go()
            activityPage
                .waitForActivityRowsPresent(ActivityPage.ActivityType.ADHOC, ActivityPage.ActivityState.SUCCEEDED)
        then: "activity page shows 2 rows"
            activityPage
                .getActivityRows(ActivityPage.ActivityType.ADHOC, ActivityPage.ActivityState.SUCCEEDED).size() == expect
        where:
            max | expect
            1   | 1
            2   | 2
            100 | 2
    }
}
