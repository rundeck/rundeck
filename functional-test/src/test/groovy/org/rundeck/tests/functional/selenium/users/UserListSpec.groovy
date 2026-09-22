package org.rundeck.tests.functional.selenium.users

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.users.UserListPage

/**
 * Covers the user-list conversion (/user/list): Vue rendering is behind the {@code nextUi}
 * URL param (NEXT_UI stage), legacy KO/GSP rendering remains the server default. Only covers
 * stories that don't need a second, non-app-admin account — those are covered by
 * gsp-validate's browser-driven pass, which provisions that fixture; this spec sticks to what
 * TEST_USER (an app-admin) can do alone.
 */
@SeleniumCoreTest
@UiModeFlag(
    featureName = "user-list",
    status      = UiModeStatus.NEXT_UI,
    jiraTicket  = "RUN-0000",
    description = "Vue user list behind ?nextUi=true; legacy KO/GSP rendering is the default."
)
class UserListSpec extends SeleniumBase {

    static final UI_MODES = UiModes.nextUiAndDefault()

    def "new profile link navigates to user create"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go(UserListPage, [nextUi: nextUi])
        when:
            userListPage.waitForElementVisible userListPage.newProfileLinkBy
            userListPage.newProfileLink.click()
        then:
            userListPage.waitForUrlToContain("/user/create")
        where:
            [nextUi] << UI_MODES
    }

    def "edit link navigates to user edit for the correct login"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go(UserListPage, [nextUi: nextUi])
        when:
            userListPage.waitForElementVisible userListPage.editLinkBy(TEST_USER)
            userListPage.getEditLink(TEST_USER).click()
        then:
            userListPage.waitForUrlToContain("/user/edit?login=${TEST_USER}")
        where:
            [nextUi] << UI_MODES
    }

    def "row expander reveals and hides the detail panel with a groups column for the logged-in user's own row"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go(UserListPage, [nextUi: nextUi])
        when: "expanding the current user's own row"
            userListPage.clickRowExpander(TEST_USER)
        then: "the groups header and its help tooltip icon are visible"
            userListPage.waitForElementVisible(userListPage.groupsHeaderBy)
            userListPage.waitForElementVisible(userListPage.groupsHelpIconBy)
        when: "collapsing it again"
            userListPage.clickRowExpander(TEST_USER)
        then: "the groups header is no longer present"
            !userListPage.isElementDisplayedIgnoringStale(userListPage.groupsHeaderBy)
        where:
            [nextUi] << UI_MODES
    }

    def "expanded detail panel shows NOT SET for blank profile fields"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userListPage = go(UserListPage, [nextUi: nextUi])
        when:
            userListPage.clickRowExpander(TEST_USER)
        then:
            userListPage.waitForElementVisible(userListPage.notSetBadgeBy)
        where:
            [nextUi] << UI_MODES
    }
}
