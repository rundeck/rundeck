package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.common.navigation.NavProjectSettings
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import org.rundeck.util.gui.pages.project.MessageOfTheDayPage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.SideBarPage

@SeleniumCoreTest
class MotdSpec extends SeleniumBase {

    /**
     * Adds motd and checks for sanitization
     * In home, project home and navbar
     */
    def "add motd navbar-projectHome-projectList in configuration file"(){
        given:
            def projectName = "motdProjectAdv"
            def motdMessage = "a simple <script>alert(\"hi\");</script> message"
            def loginPage = page LoginPage
            def projectEditPage = page ProjectEditPage
            def motdPage = page MessageOfTheDayPage
            def sideBarPage = page SideBarPage
            def homePage = page HomePage
            def projectDashboard = page DashboardPage
            setupProject(projectName)
        when: "add the motd and enable motd in the navbar"
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.validatePage()
            homePage.goProjectHome projectName
            sideBarPage.goTo NavLinkTypes.MOTD
            motdPage.setMessageOfTheDay(motdMessage)
            motdPage.save()
            projectEditPage.go("/project/${projectName}/configure")

            projectEditPage.clickEditConfigurationFile()
            projectEditPage.addConfigurationValue("project.gui.motd.display=projectList,projectHome,navbar\n")
            projectEditPage.save()
            projectEditPage.validateConfigFileSave()
            motdPage.clickMOTD()
        then: "validate that motd is shown in the navbar and it has the right value"
            motdPage.waitForMessageShownInProject("a simple message")
        when:
            projectDashboard.go("/project/${projectName}/home")
        then: "validate motd shown on project dashboard"
            motdPage.waitForMessageShownInProject("a simple message")
        when:
            homePage.setLoadPath("/menu/home")
            homePage.go()
        then:
            motdPage.waitForMessageShownInHome("a simple message")
        cleanup:
            deleteProject(projectName)

    }

    /**
     * Add motd and select places where to show using the UI
     */
    def "add motd navbar-projectHome-projectList ui"(){
        given:
            def projectName = "motdProject"
            def motdMessage = "a simple <script>alert(\"hi\");</script> message"
            def loginPage = page LoginPage
            def projectEditPage = page ProjectEditPage
            def motdPage = page MessageOfTheDayPage
            def sideBarPage = page SideBarPage
            def homePage = page HomePage
            def projectDashboard = page DashboardPage
            setupProject(projectName)
        when: "add the motd and enable motd in the navbar"
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.validatePage()
            homePage.goProjectHome projectName
            sideBarPage.goTo NavLinkTypes.MOTD
            motdPage.setMessageOfTheDay(motdMessage)
            motdPage.save()
            projectEditPage.go("/project/${projectName}/configure")
            projectEditPage.clickNavLink(NavProjectSettings.USER_INTERFACE)
            projectEditPage.selectAllMotdPlaces()
            projectEditPage.save()
        then: "validate that motd is shown in the navbar and it has the right value"
            motdPage.waitForMessageShownInProject("a simple message")
        when:
            projectDashboard.go("/project/${projectName}/home")
        then: "validate motd shown on project dashboard"
            motdPage.waitForMessageShownInProject("a simple message")
        when:
            homePage.setLoadPath("/menu/home")
            homePage.go()
        then:
            motdPage.waitForMessageShownInHome("a simple message")
        cleanup:
            deleteProject(projectName)
    }

}
