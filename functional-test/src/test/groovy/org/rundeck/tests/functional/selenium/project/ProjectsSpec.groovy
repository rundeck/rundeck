package org.rundeck.tests.functional.selenium.project


import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.container.SeleniumBase

@UiModeFlag(
    featureName = "projects-nextui",
    status      = UiModeStatus.NEXT_UI,
    description = "Asserts URL contains nextUi=true on the next-UI projects page; legacy not exercised here"
)
class ProjectsSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }


    def "[NextUi] view projects list page and create project"() {
        when:
            HomePage homePage = page HomePage
            homePage.loadPathToNextUI()
            homePage.go()
        then:
            homePage.validatePage()

            verifyAll {
                driver.currentUrl.contains('nextUi=true')
                driver.pageSource.contains('ui-type-next')
            }

            homePage.bodyNextUI.isDisplayed()
            homePage.getCreateNewProjectField().isDisplayed()
    }


    def "[NextUi] view project actions"() {
        when:
            HomePage homePage = page HomePage
            homePage.loadPathToNextUI()
            homePage.go()

        then:
            homePage.validatePage()

            verifyAll {
                driver.currentUrl.contains('nextUi=true')
                driver.pageSource.contains('ui-type-next')
            }

            homePage.bodyNextUI.isDisplayed()
            def actionsButton = homePage.projectActionsButton
            actionsButton.isDisplayed()
        when:
            actionsButton.click()
        then:
            homePage.getLink('Edit configuration').isDisplayed()
            homePage.getLink('New Job').isDisplayed()
            homePage.getLink('Upload Definition').isDisplayed()
    }
}
