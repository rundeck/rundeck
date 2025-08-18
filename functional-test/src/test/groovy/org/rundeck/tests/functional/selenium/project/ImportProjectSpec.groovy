package org.rundeck.tests.functional.selenium.project


import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectImportPage
import spock.lang.Unroll

@SeleniumCoreTest
class ImportProjectSpec extends SeleniumBase {

    /**
     * It goes to project import and with no file selected it tries to import
     * then validates that "No file was uploaded" message appears
     */
    def "import project no file selected"() {
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

    def "import a simple project file"() {
        given:
        def projectName = "fileImportProject"
        setupProject(projectName)
        updateConfigurationProject(projectName, [
                "project.gui.readme.display": "projectList,projectHome",
                "project.disable.executions": "false",
                "project.disable.schedule"  : "false"
        ])
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectImportPage importPage = page ProjectImportPage, projectName
        File testArchive = createArchiveJarFile("fileImportProject",
                new File(getClass().getResource("/projects-import/import-project-test/minimal-archive").getPath()))

        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        importPage.go()
        def fileInput = importPage.getFileInput()
        fileInput.sendKeys(testArchive.getAbsolutePath())
        importPage.getImportButton().click()

        then:
        importPage.getImportAlertMessage().getText() == "×\nArchive successfully imported"

        when:
        homePage.go()

        then:
        homePage.getReadmeMessage() == "Project Import Project Readme File\nA test project readme.md file"

        cleanup:
        deleteProject(projectName)
    }

    @Unroll
    def "import invalid files as archive should fail: #archiveName"() {
        given:
        def projectName = "fileImportProject"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        ProjectImportPage importPage = page ProjectImportPage, projectName

        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        importPage.go()
        File testArchive = new File(getClass().getResource("/projects-import/import-project-test/${archiveName}").getPath())
        def fileInput = importPage.getFileInput()
        fileInput.sendKeys(testArchive.getAbsolutePath())
        importPage.getImportButton().click()

        then:
        importPage.getImportAlertMessage().getText().contains("There were errors in the import process")
        importPage.getImportAlertMessage().getText().contains(alertMessage)

        cleanup:
        deleteProject(projectName)

        where:
        archiveName                 | alertMessage
        "empty-archive.zip"         | "Empty or corrupted archive file"
        "wrong-content-archive.zip" | "Nothing to import found in archive"
        "corrupted-archive.jar"     | "Empty or corrupted archive file"
    }

}
