package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiMechanism
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectAclCreatePage

/**
 * Selenium coverage for stable project ACL policy creation flows.
 *
 * Excluded from Pro: the Pro {@code ui-acl-manager} plugin replaces the legacy raw YAML editor
 * view with a tabbed Rules/Editor UI on the default path.
 */
@ExcludePro
@SeleniumCoreTest
@UiModeFlag(
    featureName = "menu-create-project-acl-file",
    status = UiModeStatus.NEXT_UI,
    mechanism = UiMechanism.COOKIE,
    jiraTicket = "RUN-0000",
    description = "Vue project ACL creation selected by the nextUi cookie; legacy GSP is the default."
)
class ProjectAclCreateSpec extends SeleniumBase {

    static final String PROJECT_NAME = "ProjectAclCreateSpec-${UUID.randomUUID()}"
    static final UI_MODES = UiModes.nextUiAndDefault()

    ProjectAclCreatePage createPage

    def setupSpec() {
        setupProject(PROJECT_NAME)
    }

    def cleanupSpec() {
        deleteProject(PROJECT_NAME)
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    /**
     * Every feature selects its UI mode with the nextUi cookie; remove it so the mode cannot
     * carry over into the next iteration or a later spec sharing the browser session.
     */
    def cleanup() {
        (page(LoginPage) as LoginPage).deleteNextUiCookie()
    }

    def "admin sees the empty project ACL creation form"() {
        setup:
            createPage = openCreatePage(nextUi)

        expect: "the authorized create form is complete and ready for input"
            createPage.headingText.contains('Create Project ACL Policy')
            createPage.name == ''
            createPage.nameHelpText ==
                    'The policy name without file extension, can contain the characters: a-zA-Z0-9,.+_-'
            createPage.projectValue == PROJECT_NAME
            createPage.editorText == ''
            createPage.editorFocused
            createPage.hasCreateControls()

        where:
            [nextUi] << UI_MODES
    }

    def "admin can inspect ACL policy help links"() {
        setup:
            createPage = openCreatePage(nextUi)

        when: "the help menu is opened"
            createPage.openHelp()

        then: "both documentation links open in a new browsing context"
            createPage.accessControlHelpLink == [
                    text  : 'Access Control Policy',
                    href  : 'https://docs.rundeck.com/docs/administration/security/authorization.html',
                    target: '_new'
            ]
            createPage.aclFormatHelpLink == [
                    text  : 'ACL Policy Format',
                    href  : 'https://docs.rundeck.com/docs/administration/security/authorization.html#example',
                    target: '_new'
            ]

        where:
            [nextUi] << UI_MODES
    }

    def "admin can return to the project ACL list with Back"() {
        setup:
            createPage = openCreatePage(nextUi)

        when:
            createPage.clickBack()

        then:
            createPage.atProjectAclList

        where:
            [nextUi] << UI_MODES
    }

    def "fileText query parameter prefills the ACL editor"() {
        setup:
            createPage = page(ProjectAclCreatePage, PROJECT_NAME)
            createPage.setNextUiCookie(nextUi)

        when:
            createPage.goWithPrefill('description: prefill-probe')

        then:
            createPage.editorText == 'description: prefill-probe'

        where:
            [nextUi] << UI_MODES
    }

    def "blank project ACL submission shows validation errors"() {
        setup:
            createPage = openCreatePage(nextUi)

        when:
            createPage.clickSave()
            createPage.waitForUrlToContain('/admin/acls/save')

        then: "the save is rejected on the creation page"
            createPage.globalErrorText.contains('Policy cannot be blank')
            createPage.globalErrorText.contains('"id" parameter cannot be blank')
            createPage.fileTextErrors.contains('Policy cannot be blank')
            createPage.hasFileTextErrorState()
            !createPage.hasNameErrorState()

        where:
            [nextUi] << UI_MODES
    }

    def "admin can correct validation errors and save a valid project ACL policy"() {
        given:
            String policyName = "valid-policy-${nextUi ? 'next' : 'legacy'}"
            String invalidPolicy = "description: invalid policy\nfor:\n  resource:\n    - allow: read"
            String validPolicy = "description: valid policy\nfor:\n  resource:\n    - allow: read\nby:\n  group: test_group"
            createPage = openCreatePage(nextUi)

        when: "an incomplete policy is submitted first"
            createPage.enterName(policyName)
            createPage.replaceEditorText(invalidPolicy)
            createPage.clickSave()
            createPage.waitForUrlToContain('/admin/acls/save')

        then: "the form re-renders with the validation report and preserves the input"
            createPage.globalErrorText.contains('Validation failed')
            createPage.validationReportText.contains("Required 'by:' or 'notBy:' section was not present")
            createPage.name == policyName
            createPage.editorText == invalidPolicy

        when: "the policy is corrected and resubmitted using the regenerated CSRF token"
            createPage.replaceEditorText(validPolicy)
            createPage.clickSave()
            createPage.waitForProjectAclList()

        then: "the browser lands on the project ACL list and the stored policy matches via API"
            createPage.atProjectAclList
            getProjectAclContents(policyName).trim() == validPolicy

        cleanup:
            deleteProjectAcl(policyName)

        where:
            [nextUi] << UI_MODES
    }

    def "dirty editor warns before navigation but Cancel does not"() {
        given:
            createPage = openCreatePage(nextUi)
            createPage.replaceEditorText('x')

        expect: "the unsaved-change navigation confirmation is armed"
            createPage.hasUnsavedNavigationConfirmation()
            createPage.currentUrl().contains("/project/${PROJECT_NAME}/admin/acls/create")

        when: "the user cancels through the form"
            createPage.clickCancel()

        then:
            createPage.atProjectAclList

        where:
            [nextUi] << UI_MODES
    }

    def "Enter in the policy name does not submit the form"() {
        setup:
            createPage = openCreatePage(nextUi)

        when:
            createPage.enterName('enter_probe')
            createPage.pressEnterInName()

        then:
            createPage.currentUrl().contains("/project/${PROJECT_NAME}/admin/acls/create")
            createPage.name == 'enter_probe'
            !createPage.hasErrors()

        where:
            [nextUi] << UI_MODES
    }

    def "missing project parameter renders the standard error"() {
        setup:
            createPage = page(ProjectAclCreatePage, PROJECT_NAME)
            createPage.setNextUiCookie(nextUi)

        when:
            createPage.goWithoutProject()

        then:
            createPage.bodyText.contains('Project parameter is required')

        where:
            [nextUi] << UI_MODES
    }

    /**
     * Selects a UI mode by cookie and opens the ACL creation page.
     *
     * @param nextUi true for Vue, false for the legacy GSP
     * @return loaded ACL creation page
     */
    private ProjectAclCreatePage openCreatePage(final boolean nextUi) {
        ProjectAclCreatePage result = page(ProjectAclCreatePage, PROJECT_NAME)
        result.setNextUiCookie(nextUi)
        result.go()
        result
    }

    /**
     * Fetches the stored text of a project ACL policy through the API.
     *
     * @param policyName policy name without the {@code .aclpolicy} extension
     * @return stored policy YAML contents
     */
    private String getProjectAclContents(final String policyName) {
        Map response = get("/project/${PROJECT_NAME}/acl/${policyName}.aclpolicy", Map)
        (response.contents as String)?.normalize()
    }

    /**
     * Deletes a project ACL policy through the API if it exists.
     *
     * @param policyName policy name without the {@code .aclpolicy} extension
     */
    private void deleteProjectAcl(final String policyName) {
        try (def response = doDelete("/project/${PROJECT_NAME}/acl/${policyName}.aclpolicy")) {
            assert response.code() in [204, 404] : "Unexpected status deleting ACL ${policyName}: ${response.code()}"
        }
    }
}
