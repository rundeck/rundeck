package org.rundeck.tests.functional.selenium.webhook

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.webhooks.WebhooksPage

@SeleniumCoreTest
class WebhooksSpec extends SeleniumBase {

    def "Webhooks enabled"(){
        given:
        def projectName = "CheckWebhookEnabled"
        setupProject(projectName)
        def webhookPage = page WebhooksPage
        def loginPage = go LoginPage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
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
