package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.common.navigation.NavLinkTypes
import org.rundeck.util.gui.common.navigation.NavProjectSettings
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.DashboardPage
import org.rundeck.util.gui.pages.project.ProjectEditPage
import org.rundeck.util.gui.pages.project.ReadmePage
import org.rundeck.util.gui.pages.project.SideBarPage

@SeleniumCoreTest
class ReadmeSpec extends SeleniumBase{

    /**
     * Add readme to project and then validates that it is shown in dashboard an project list
     */
    def "add readme"(){
        given:
            def projectName = "projectReadme"
            def readmeText = "this is my readme text"
            setupProject(projectName)
            def loginPage = page LoginPage
            def projectEditPage = page ProjectEditPage
            def readmePage = page ReadmePage
            def sideBarPage = page SideBarPage
            def homePage = page HomePage
            def projectDashboard = page DashboardPage
        when:
            loginPage.go()
            loginPage.login(TEST_USER, TEST_PASS)
            homePage.validatePage()
            homePage.goProjectHome projectName
            sideBarPage.goTo NavLinkTypes.README
            readmePage.setReadmeMessage(readmeText)
            readmePage.save()
            sideBarPage.goTo NavLinkTypes.PROJECT_CONFIG
            projectEditPage.clickNavLink(NavProjectSettings.USER_INTERFACE)
            projectEditPage.selectReadmeAllPlaces()
            projectEditPage.save()
        then:
            projectDashboard.getCheckReadme() == readmeText
        when:
            homePage.go()
        then:
            homePage.getReadmeMessage() == readmeText
        cleanup:
            deleteProject(projectName)
    }
}
