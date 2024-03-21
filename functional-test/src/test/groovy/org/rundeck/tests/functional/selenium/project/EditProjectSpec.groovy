package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import org.rundeck.util.gui.pages.project.ProjectCreatePage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.SideBarPage

@SeleniumCoreTest
class EditProjectSpec extends SeleniumBase {

    /**
     * It creates a project using de API and then adds a project description using the UI
     * it validates that the description is shown in the project dashboard
     */
    def "add project description"() {
        given:
            def projectName = "addProjectDesc"
            def projectDescription = "custom project description"
            setupProject(projectName)
            def projectEditPage = page ProjectEditPage
            def projectDashboard = page DashboardPage
            def homePage = page HomePage
            def loginPage = go LoginPage
        when:
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.validatePage()
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.setProjectDescription projectDescription
            projectEditPage.save()
        then:
            projectDashboard.expectProjectDescriptionToBe(projectDescription)
        cleanup:
            deleteProject(projectName)
    }

    /**
     * It creates a project using de API and then adds a project label using the UI
     * it validates that the label is shown in the project dashboard
     */
    def "add project label"() {
        given:
            def projectName = "addProjectLabel"
            def projectLabel = "custom project label"
            setupProject(projectName)
            def projectEditPage = page ProjectEditPage
            def projectDashboard = page DashboardPage
            def homePage = page HomePage
            def loginPage = go LoginPage
        when:
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.validatePage()
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.setProjectLabel projectLabel
            projectEditPage.save()
        then:
            projectDashboard.expectProjectLabelToBe(projectLabel)
        cleanup:
            deleteProject(projectName)
    }

    /**
     * It creates a project using de API and then adds a project description using the UI by modifying the configuration file
     * It validates that the description is shown in the project dashboard
     * Then modifies description and label and validates
     * Then it deletes description an label and validates
     */
    def "add-edit-delete project description and label advanced"(){
        given:
            def projectName = "addProjectDescAdv"
            def projectDescription = "SampleDesc"
            def projectLabel = "SampleLabel"
            def projectDescriptionProperty = "project.description=${projectDescription}\nproject.label=${projectLabel}\n"
            setupProject(projectName)
            def projectEditPage = page ProjectEditPage
            def projectDashboard = page DashboardPage
            def topMenu = page TopMenuPage
            def sideBarPage = page SideBarPage
            def homePage = page HomePage
            def loginPage = page LoginPage
        when:
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.validatePage()
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.clickEditConfigurationFile()
            projectEditPage.addConfigurationValue projectDescriptionProperty
            projectEditPage.save()
            sideBarPage.goTo(NavLinkTypes.DASHBOARD)
        then:
            projectDashboard.expectProjectDescriptionToBe(projectDescription)
            projectDashboard.expectProjectLabelToBe(projectLabel)
        when: "go to homepage and check for description and label to be shown"
            topMenu.clickHomeButton()
        then:
            homePage.expectPartialLinkToExist(projectDescription)
            homePage.expectPartialLinkToExist(projectLabel)
        when: "edit project label and verify"
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.setProjectLabel projectLabel+"edited"
            projectEditPage.setProjectDescription projectDescription+"edited"
            projectEditPage.save()
            topMenu.clickHomeButton()
        then: "validate that the label and description are shown with the new values"
            homePage.expectPartialLinkToExist(projectDescription+"edited")
            homePage.expectPartialLinkToExist(projectLabel+"edited")
        when: "remove label and description"
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.setProjectLabel ""
            projectEditPage.setProjectDescription ""
            projectEditPage.save()
            topMenu.clickHomeButton()
        then: "validate that the project name is shown"
            homePage.expectPartialLinkToExist(projectName)
        cleanup:
            deleteProject(projectName)
    }

    def "Change project description"(){
        given:
        def projectName = "changeDescriptionTest"
        def projectDescription = "custom project description"
        def anotherProjectDescription = "another custom project description"
        setupProject(projectName)
        def projectEditPage = page ProjectEditPage
        def projectDashboard = page DashboardPage
        def loginPage = go LoginPage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.setProjectDescription(projectDescription)
        projectEditPage.save()
        then:
        projectDashboard.expectProjectDescriptionToBe(projectDescription)

        when:
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.setProjectDescription(anotherProjectDescription)
        projectEditPage.save()
        then:
        projectDashboard.expectProjectDescriptionToBe(anotherProjectDescription)

        cleanup:
        deleteProject(projectName)
    }

    def "Change project label"(){
        given:
        def projectName = "changeProjectLabel"
        def projectLabel = "custom project label"
        def anotherProjectLabel = "another project label"
        setupProject(projectName)
        def projectEditPage = page ProjectEditPage
        def projectDashboard = page DashboardPage
        def loginPage = go LoginPage

        when:
        loginPage.login(TEST_USER, TEST_PASS)
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.setProjectLabel projectLabel
        projectEditPage.save()
        then:
        projectDashboard.expectProjectLabelToBe(projectLabel)

        when:
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.setProjectLabel anotherProjectLabel
        projectEditPage.save()
        then:
        projectDashboard.expectProjectLabelToBe(anotherProjectLabel)

        cleanup:
        deleteProject(projectName)
    }

    def "delete description | label"(){
        given:
        def projectName = "removeDescriptionAndLabelTest"
        setupProject(projectName)
        def projectEditPage = page ProjectEditPage
        def projectDashboard = page DashboardPage
        def loginPage = go LoginPage

        when: "Give a desc and label first"
        loginPage.login(TEST_USER, TEST_PASS)
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.setProjectLabel "a-label"
        projectEditPage.setProjectDescription "A desc"
        projectEditPage.save()
        then:
        projectDashboard.expectProjectLabelToBe("a-label")
        projectDashboard.expectProjectDescriptionToBe("A desc")

        when: "Delete description"
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.clearProjectDescriptionInput()
        projectEditPage.save()

        then:
        projectDashboard.expectProjectDescriptionToBe("")

        when: "Delete project label"
        projectEditPage.go("/project/${projectName}/configure")
        projectEditPage.clearProjectLabelInput()
        projectEditPage.save()
        then:
        projectDashboard.expectProjectLabelToBe("")

        cleanup:
        deleteProject(projectName)
    }

}