package org.rundeck.tests.functional.selenium.tests.navigation

import org.rundeck.tests.functional.selenium.pages.TopMenuPage
import org.rundeck.tests.functional.selenium.pages.home.HomePage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.tests.functional.selenium.pages.project.SideBarPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.setup.NavLinkTypes
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class NavigationSpec extends SeleniumBase {

    def setupSpec(){
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def setup(){
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        def homePage = page HomePage
        homePage.goProjectHome"SeleniumBasic"
    }

    def "visits jobs"() {
        when:
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.JOBS
        then:
            sideBarPage.waitForUrlToContain('/jobs')
    }

    def "visits nodes"() {
        when:
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.NODES
        then:
            sideBarPage.waitForUrlToContain('/nodes')
    }

    def "visits commands"() {
        when:
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.COMMANDS
        then:
            sideBarPage.waitForUrlToContain('/command')
    }

    def "visits activity"() {
        when:
            def sideBarPage = page SideBarPage
            sideBarPage.goTo NavLinkTypes.ACTIVITY
        then:
            sideBarPage.waitForUrlToContain('/activity')
    }

    def "visits System Configuration"() {
        when:
            def topMenuPage = page TopMenuPage
        then:
            topMenuPage.navigateToSystemConfiguration()
            topMenuPage.waitForUrlToContain('/config')
    }
}
