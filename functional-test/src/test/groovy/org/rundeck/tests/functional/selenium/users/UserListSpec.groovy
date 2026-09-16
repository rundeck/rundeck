package org.rundeck.tests.functional.selenium.users

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.users.UserListPage

/**
 * Covers the vueUserList conversion (/user/list). Only covers stories that don't need a
 * second, non-app-admin account — those are covered by gsp-validate's browser-driven pass,
 * which provisions that fixture; this spec sticks to what TEST_USER (an app-admin) can do alone.
 *
 * NOTE: vueUserList has no per-request override (unlike the uiType/nextUi/legacyUi query-param
 * system @UiModeFlag targets — confirmed via FeatureTagLib.groovy, it reads app-wide config
 * only), so there is no where: [legacyUi] << UI_MODES here: this spec always exercises whichever
 * path is the server's current default.
 */
@SeleniumCoreTest
class UserListSpec extends SeleniumBase {

    def "new profile link navigates to user create"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go UserListPage
        when:
            userListPage.waitForElementVisible userListPage.newProfileLinkBy
            userListPage.newProfileLink.click()
        then:
            userListPage.waitForUrlToContain("/user/create")
    }

    def "edit link navigates to user edit for the correct login"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go UserListPage
        when:
            userListPage.waitForElementVisible userListPage.editLinkBy(TEST_USER)
            userListPage.getEditLink(TEST_USER).click()
        then:
            userListPage.waitForUrlToContain("/user/edit?login=${TEST_USER}")
    }

    def "row expander reveals and hides the detail panel with a groups column for the logged-in user's own row"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go UserListPage
        when: "expanding the current user's own row"
            userListPage.clickRowExpander(TEST_USER)
        then: "the groups header and its help tooltip icon are visible"
            userListPage.waitForElementVisible(userListPage.groupsHeaderBy)
            userListPage.waitForElementVisible(userListPage.groupsHelpIconBy)
        when: "collapsing it again"
            userListPage.clickRowExpander(TEST_USER)
        then: "the groups header is no longer present"
            !userListPage.isElementDisplayedIgnoringStale(userListPage.groupsHeaderBy)
    }

    def "expanded detail panel shows NOT SET for blank profile fields"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go UserListPage
        when:
            userListPage.clickRowExpander(TEST_USER)
        then:
            userListPage.waitForElementVisible(userListPage.notSetBadgeBy)
    }
}
