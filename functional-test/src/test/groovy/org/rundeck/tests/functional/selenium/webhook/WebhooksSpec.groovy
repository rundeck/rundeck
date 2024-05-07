package org.rundeck.tests.functional.selenium.webhook

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.webhooks.WebhooksPage

@SeleniumCoreTest
class WebhooksSpec extends SeleniumBase {

    /**
     * Checks if the webhooks are enabled in a project.
     *
     */
    def "Webhooks enabled"(){
        given:
        def projectName = "CheckWebhookEnabled"
        setupProject(projectName)
        def webhookPage = page WebhooksPage
        def loginPage = go LoginPage
        HomePage homePage = page HomePage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        webhookPage.loadPageForProject(projectName)
        webhookPage.go()
        webhookPage.validatePage()

        then:
        noExceptionThrown()
        webhookPage.createWebhookButton.displayed

        cleanup:
        deleteProject(projectName)
    }

}
