package org.rundeck.tests.functional.selenium.execution

import org.openqa.selenium.WebElement
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.CommandPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.login.LoginPage
import spock.lang.Shared

@SeleniumCoreTest
class ExecutionShowAuthSpec extends SeleniumBase {
    static final String TEST_PROJECT = "ExecutionShowAuth"
    public static final String ACLPOLICY_FILE = "ExecutionShowAuthSpec.aclpolicy"

    @Shared
    String executionPath

    def setupSpec() {
        setupProject(TEST_PROJECT)

        //import acls for authorization checks
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)

        //create an execution
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def commandPage = go CommandPage, TEST_PROJECT
        commandPage.runCommandAndWaitToBe("echo 'Hello world'", "SUCCEEDED")

        //retrieve show path
        def exec = ExecutionUtils.Retrievers.executionsForProject(client, TEST_PROJECT).get().first()
        if (exec.permalink.startsWith(client.baseUrl)) {
            //remove the base url
            executionPath = exec.permalink.substring(client.baseUrl.length())
        } else {
            executionPath = exec.permalink
        }
    }

    def cleanupSpec() {
        deleteProject(TEST_PROJECT)
        deleteSystemAcl(ACLPOLICY_FILE)
    }

    static Map AUTH_USERS = [
        admin           : 'AuthTest1',
        read            : 'AuthTest2',
        delete_execution: 'AuthTest3',
        app_admin       : 'AuthTest4'
    ]
    static final String AUTH_USER_PASS = "password"

    def "execution action menu contains #expected not #notExpected with #authorization authorization"() {
        given: "specific user logs in with limited authorization"
            def loginPage = go LoginPage
            loginPage.login(AUTH_USERS[authorization], AUTH_USER_PASS)
            assert !driver.currentUrl.contains('/user/error')
        when: "view execution page for given execution"
            def showPage = page ExecutionShowPage, executionPath
            showPage.go()
            showPage.waitForActionMenuButton()
            showPage.executionActionMenuButton.click()
            showPage.waitForActionMenuVisible()
            List<WebElement> links = showPage.actionMenuLinks
            def linksText = links*.text

        then: "action menu has expected links, and no unexpected links"

        verifyAll {
            assert linksText.containsAll(expected), "${linksText} does not contain all of ${expected}"
            assert linksText.intersect(notExpected)==[], "${linksText} should not contain any of ${notExpected}"
        }

        where:
            authorization      | expected                                | notExpected
            'read'             | ['Definition']                          | ['Delete this Execution…']
            'admin'            | ['Delete this Execution…', 'Definition'] | []
            'app_admin'        | ['Delete this Execution…', 'Definition'] | []
            'delete_execution' | ['Delete this Execution…', 'Definition'] | []

    }
}
