package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiMechanism
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.TopMenuPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectExportPage

/**
 * Test Authorization checks on the Project Export Form page
 * acls in ProjectExportAuthSpec.aclpolicy
 * AuthTest1: project export + read, project_acl read
 * AuthTest2: project export + read, project_acl admin
 * AuthTest3: project export + read, project_acl app_admin
 * AuthTest4: project export + read + configure
 * AuthTest5: project export + read + admin
 * AuthTest6: project export + read + app_admin
 * AuthTest7: project export + read
 * AuthTest8: project export + promote + read
 * AuthTest9: project read (no export access)
 */
@SeleniumCoreTest
class ProjectExportAuthSpec extends SeleniumBase {

    static final String PROJECT_NAME = 'ProjectExportAuthSpec'
    public static final String ACLPOLICY_FILE = PROJECT_NAME + ".aclpolicy"

    static final String USER_PASSWORD = 'password'

    /** The export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default. */
    static final UI_MODES = UiModes.nextUiAndDefault()

    def setupSpec() {
        setupProject(PROJECT_NAME)
        importSystemAcls("/${ACLPOLICY_FILE}", ACLPOLICY_FILE)
    }
    def setup(){
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
        deleteSystemAcl(ACLPOLICY_FILE)
    }

    def "project access #projAuth acl access #aclAuth shows correct export options"() {
        when: "view Project Export form"
            def loginPage = go LoginPage
            loginPage.login(user, USER_PASSWORD)
            sleep(2000)
            def projectExportPage = go ProjectExportPage, PROJECT_NAME
            def boxNames = projectExportPage.checkBoxes*.getAttribute('name')
            def disabledBoxes = projectExportPage.disabledCheckboxes*.text

        then: "expected checkboxes are displayed, or shown as unauthorized"

            verifyAll {
                boxNames.containsAll(expected)
                disabledBoxes.containsAll(expectDisabled)
            }

        cleanup:
            page TopMenuPage logOut()
            sleep(10000)
        where: "user account with specific authorization is used"
            user        | projAuth    | aclAuth       | expected       | expectDisabled
            'AuthTest1' | 'export'    | ['read']      | ['exportAcls'] | ['SCM Configuration (Unauthorized)']
            'AuthTest2' | 'export'    | ['admin']     | ['exportAcls'] | ['SCM Configuration (Unauthorized)']
            'AuthTest3' | 'export'    | ['app_admin'] | ['exportAcls'] | ['SCM Configuration (Unauthorized)']
            'AuthTest4' | 'configure' | []            | ['exportScm']  | ['ACL Policies (Unauthorized)']
            'AuthTest5' | 'admin'     | []            | ['exportScm']  | ['ACL Policies (Unauthorized)']
            'AuthTest6' | 'app_admin' | []            | ['exportScm']  | ['ACL Policies (Unauthorized)']
            'AuthTest7' | 'export'    | []            | []             | ['ACL Policies (Unauthorized)', 'SCM Configuration (Unauthorized)']
    }

    def "project access #projAuth shows promote to other project instance button #expected"() {
        when: "view Project Export form"
            def loginPage = go LoginPage
            loginPage.login(user, USER_PASSWORD)
            sleep(2000)
            def projectExportPage = go ProjectExportPage, PROJECT_NAME

            //scroll footer into view for potential screenshot if an error
            projectExportPage.executeScript "arguments[0].scrollIntoView(true);", projectExportPage.exportFormFooter

        then: "Export to other instance button is shown or hidden"

            if (expected) {
                assert projectExportPage.exportToOtherInstanceButton.isDisplayed()
            } else {
                assert !projectExportPage.els(projectExportPage.exportToOtherInstanceButtonBy)
            }

        where: "user account with specific authorization is used"
            user        | projAuth    | expected
            'AuthTest7' | 'export'    | false
            'AuthTest4' | 'configure' | false
            'AuthTest5' | 'admin'     | true
            'AuthTest6' | 'app_admin' | true
            'AuthTest8' | 'promote'   | true
    }

    def "no export access should show unauthorized"() {
        when: "view Project Export form"
            def loginPage = go LoginPage
            loginPage.login(user, USER_PASSWORD)

            go ProjectExportPage, PROJECT_NAME

        then: "unauthorized message shown"

            pageSource.contains("Not authorized for action \"export\" for Project $PROJECT_NAME")

        where: "user account with specific authorization is used"
            user        | _
            'AuthTest9' | _
    }

    /**
     * AuthTest7 only holds project export + read: it must still be able to complete an export
     * and reach the ready state with the download button on the wait page.
     */
    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "export-only user completes the export and sees the ready state"() {
        given:
            def loginPage = go LoginPage
            loginPage.login('AuthTest7', USER_PASSWORD)
            def projectExportPage = page(ProjectExportPage, PROJECT_NAME, [nextUi: nextUi])
            projectExportPage.go()
        when:
            projectExportPage.exportArchiveButton.click()
            projectExportPage.waitForWaitPageToken()
        then: "the expected UI version renders the wait page and it reaches the ready state"
            projectExportPage.validateWaitPageVersion()
            projectExportPage.waitForExportReady()
            projectExportPage.downloadText == "${PROJECT_NAME}.rdproject.jar".toString()
        cleanup:
            page TopMenuPage logOut()
        where:
            [nextUi] << UI_MODES
    }
}
