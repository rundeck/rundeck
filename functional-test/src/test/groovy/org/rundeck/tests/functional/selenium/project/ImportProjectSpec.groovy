package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectImportPage

@SeleniumCoreTest
class ImportProjectSpec extends SeleniumBase{

    /**
     * It goes to project import and with no file selected it tries to import
     * then validates that "No file was uploaded" message appears
     */
    def "import project no file selected"(){
        given:
        def projectName = "noFileImportProject"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectImportPage importPage = page ProjectImportPage, projectName
        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        importPage.go()
        importPage.getImportButton().click()
        then:
        importPage.getImportAlertMessage().getText() == "×\nNo file was uploaded."
        cleanup:
        deleteProject(projectName)
    }
}
