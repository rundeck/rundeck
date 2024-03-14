package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import org.rundeck.util.gui.pages.project.NodeSourcePage
import org.rundeck.util.gui.pages.project.ProjectCreatePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class CreateProjectSpec extends SeleniumBase {

    def "create project has basic fields"() {
        when:
            def loginPage = go LoginPage
            def homePage = page HomePage
            def projectCreatePage = page ProjectCreatePage
        then:
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.createProjectButton()
        expect:
            currentUrl.contains("/resources/createProject")
            projectCreatePage.projectNameInput
            projectCreatePage.labelInput
            projectCreatePage.descriptionInput
    }

    def "Create project with empty name"(){
        given:
        def loginPage = go LoginPage
        def homePage = page HomePage
        def projectCreatePage = page ProjectCreatePage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.createProjectButton()
        currentUrl.contains("/resources/createProject")
        projectCreatePage.createField.click()

        then:
        projectCreatePage.projectCreateDangerAlert.displayed
        projectCreatePage.projectCreateDangerAlertContent.text.contains("Project name is required")

    }

    def "Create simple project | empty label | empty description"(){
        given:
        def projectName = "simple-project"
        def loginPage = go LoginPage
        def homePage = page HomePage
        def projectCreatePage = page ProjectCreatePage
        NodeSourcePage nodeSourcePage = page NodeSourcePage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.createProjectButton()
        currentUrl.contains("/resources/createProject")
        projectCreatePage.projectNameInput.sendKeys(projectName)
        projectCreatePage.createField.click()

        then:
        nodeSourcePage.validatePage()

        cleanup:
        deleteProject(projectName)
    }

    def "Create simple project | label | description"(){
        given:
        def projectName = "simple-project"
        def loginPage = go LoginPage
        def homePage = page HomePage
        def projectCreatePage = page ProjectCreatePage
        NodeSourcePage nodeSourcePage = page NodeSourcePage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.createProjectButton()
        currentUrl.contains("/resources/createProject")
        projectCreatePage.projectNameInput.sendKeys(projectName)
        projectCreatePage.projectDescriptionInput.sendKeys("a description")
        projectCreatePage.labelInput.sendKeys("label")
        projectCreatePage.createField.click()

        then:
        nodeSourcePage.validatePage()

        cleanup:
        deleteProject(projectName)
    }

    def "Create project with invalid characters in description | name"(){
        given:
        def loginPage = go LoginPage
        def homePage = page HomePage
        def projectCreatePage = page ProjectCreatePage

        when: "IC in desc"
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.createProjectButton()
        currentUrl.contains("/resources/createProject")
        projectCreatePage.projectNameInput.sendKeys("name")
        projectCreatePage.descriptionInput.sendKeys("!\$%&")
        projectCreatePage.createField.click()

        then:
        projectCreatePage.projectCreateDangerAlert.displayed
        projectCreatePage.projectCreateDangerAlertContent.text.contains(
                "Project description can only contain these characters: [a-zA-Z0-9p{L}p{M}\\s\\.,\\(\\)_-]."
        )

        when: "IC in name"
        projectCreatePage.go()
        projectCreatePage.projectNameInput.sendKeys("!\$%&")
        projectCreatePage.createField.click()

        then:
        projectCreatePage.projectCreateDangerAlert.displayed
        projectCreatePage.projectCreateDangerAlertContent.text.contains(
                "Project name can only contain these characters: [a-zA-Z0-9_-+.] and cannot start with a '.'."
        )
    }
}