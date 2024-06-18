package org.rundeck.tests.functional.selenium.webhook

import org.openqa.selenium.WebElement
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectExportPage
import org.rundeck.util.gui.pages.project.SideBarPage
import org.rundeck.util.gui.pages.webhooks.WebhooksPage

@SeleniumCoreTest
class WebhooksSpec extends SeleniumBase {

    static final String WEBHOOK_RESOURCES_DIR = '/projects-import/webhooks-test'

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

    /**
     * Create webhook in a project
     */
    void "create project with webhook"() {
        given:
        String projectName = 'CreateWebhookProject'
        String webhookName = 'CreateWebhook'
        setupProject(projectName)
        WebhooksPage webhookPage = page WebhooksPage
        LoginPage loginPage = go LoginPage
        HomePage homePage = page HomePage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        webhookPage.loadPageForProject(projectName)
        createSimpleWebhook(webhookPage, webhookName)
        webhookPage.waitForNumberOfElementsToBe(webhookPage.alertInfoBy, 0)
        removeWebhook(webhookPage, webhookName)
        webhookPage.waitForNumberOfElementsToBe(webhookPage.alertInfoBy, 0)

        then:
        !containsName(webhookPage, webhookName)

        cleanup:
        deleteProject(projectName)
    }


    /**
     * Exports a project with a simple webhook
     */
    void "export project with webhook"() {
        given:
        String projectName = 'exportProjectWithWebhook'
        String webhookName = 'exportWebhook'
        setupProject(projectName)
        WebhooksPage webhookPage = page WebhooksPage
        LoginPage loginPage = go LoginPage
        HomePage homePage = page HomePage
        SideBarPage sideBarPage = page SideBarPage
        ProjectExportPage projectExportPage = page ProjectExportPage, projectName

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()

        webhookPage.loadPageForProject(projectName)
        createSimpleWebhook(webhookPage, webhookName)
        exportProjectWithWebhooks(projectExportPage, sideBarPage, false)

        then:
        projectExportPage.btnCtaButton.text == projectName + '.rdproject.jar'

        cleanup:
        deleteProject(projectName)
    }

    /**
     * Imports a project with a simple webhook
     */
    void "import project with webhook"() {
        given:
        String projectName = 'importProjectWithWebhook'
        String webhookName = 'importWebhook'
        setupProjectArchiveDirectory(
                projectName,
                new File(getClass().getResource(WEBHOOK_RESOURCES_DIR).path),
                ['importWebhooks': 'true']
        )
        WebhooksPage webhookPage = page WebhooksPage
        LoginPage loginPage = go LoginPage
        HomePage homePage = page HomePage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        webhookPage.loadPageForProject(projectName)
        webhookPage.go()
        webhookPage.validatePage()
        webhookPage.waitForElementToBeClickable(webhookPage.createWebhookButton)

        then:
        containsName(webhookPage, webhookName)

        cleanup:
        deleteProject(projectName)
    }

    /**
     * Create a simple webhook
     * @param webhookPage webhook page object
     * @param webhookName webhook name to create
     */
    void createSimpleWebhook(WebhooksPage webhookPage, String webhookName) {
        webhookPage.go()
        webhookPage.validatePage()
        webhookPage.waitForElementToBeClickable(webhookPage.createWebhookButton)
        if (!containsName(webhookPage, webhookName)) {
            webhookPage.createWebhookButton.click()
            webhookPage.nameFormField.clear()
            webhookPage.nameFormField.sendKeys(webhookName)
            webhookPage.handlerConfigTab.click()
            webhookPage.chooseWebhookPlugin.click()
            webhookPage.waitForNumberOfElementsToBe(webhookPage.logEventsBy, 1)
            webhookPage.logEventsPlugin.click()
            webhookPage.saveButton.click()
        }
    }

    /**
     * Exports a project with a webhook
     * @param projectExportPage project name to export
     * @param sideBarPage page where is called export archive
     * @param doExport if it is true, export the project; if it is false, cancel the export
     */
    void exportProjectWithWebhooks(ProjectExportPage projectExportPage, SideBarPage sideBarPage, boolean doExport) {
        // Goes to the export archive section
        sideBarPage.projectSettingsField.click()
        sideBarPage.projectSettingsExportArchiveField.click()
        // Deselect all non-webhook related parameters for the archive to be exported
        projectExportPage.allCheckbox.click()
        projectExportPage.exportJobsCheckbox.click()
        projectExportPage.exportExecutionsCheckbox.click()
        projectExportPage.exportConfigsCheckbox.click()
        projectExportPage.exportReadmesCheckbox.click()
        projectExportPage.exportAclsCheckbox.click()
        projectExportPage.exportScmCheckbox.click()
        // Includes auth token for webhooks
        projectExportPage.authTokensCheckbox.click()
        // Do export
        projectExportPage.exportArchiveButton.click()
        // Waits until the archive is ready for download
        projectExportPage.waitForElementVisible(projectExportPage.btnCtaButtonBy)

        if (doExport) {
            projectExportPage.btnCtaButton.click();
        }
    }

    /**
     * Removes a webhook
     * @param webhookPage current page
     * @param name webhook name to remove
     */
    void removeWebhook(WebhooksPage webhookPage, String name) {
        webhookPage.waitForElementVisible(webhookPage.webhookSelectItem)
        WebElement el = getWebhookFromSideBar(webhookPage.webhookSelectItems, name)
        el.click()
        webhookPage.waitForElementVisible(webhookPage.deleteButtonBy)
        webhookPage.deleteButton.click()
        webhookPage.waitForNumberOfElementsToBe(webhookPage.okButtonBy, 1)
        webhookPage.okButton.click()
    }

    /**
     * Checks if a name is contained in web element
     * @param webhookPage web element page
     * @param name string to look for
     * @return true if is contained or false if not
     */
    boolean containsName(WebhooksPage webhookPage, String name) {
        WebElement el = getWebhookFromSideBar(webhookPage.webhookSelectItems, name)
        return el ? true : false
    }

    /**
     * Gets webhooks from side menu bar
     * @param elements webhooks at side menu bar
     * @param name webhook name
     * @return the element if it exists; otherwise, return null.
     */
    WebElement getWebhookFromSideBar(List<WebElement> elements, String name) {
        return elements.stream()
                .filter(e ->  e.text.contains(name))
                .findFirst()
                .orElse(null)
    }

}
