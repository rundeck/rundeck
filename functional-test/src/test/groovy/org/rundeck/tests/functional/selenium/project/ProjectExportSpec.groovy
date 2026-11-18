package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.annotations.UiMechanism
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectExportPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class ProjectExportSpec extends SeleniumBase {

    /** The export wait page (/project/<p>/exportWait/<token>) is served by Vue behind the nextUi cookie. */
    static final UI_MODES = UiModes.nextUiAndDefault()

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    /** Export the basic project and land on the wait page in the requested UI version. */
    private ProjectExportPage startExport(boolean nextUi) {
        def projectExportPage = page(ProjectExportPage, SELENIUM_BASIC_PROJECT, [nextUi: nextUi])
        projectExportPage.go()
        projectExportPage.submitExportButton.click()
        projectExportPage.waitForWaitPageToken()
        return projectExportPage
    }

    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "exports without errors"() {
        when:
            def projectExportPage = startExport(nextUi)
        then: "the expected UI version rendered the wait page"
            projectExportPage.validateWaitPageVersion()
        and: "it polls through to the ready state without an error panel"
            projectExportPage.waitForExportReady()
            projectExportPage.errorPanels.collect {it.isDisplayed() }.every {it == false}
        where:
            [nextUi] << UI_MODES
    }

    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "wait page switches to ready on its own and the tab title shows READY"() {
        when: "an export is started and the page is left alone"
            def projectExportPage = startExport(nextUi)
        then: "the title changes to the ready title without a manual reload"
            projectExportPage.waitForReadyTitle() == '[READY] Export archive'
        and: "the ready heading names the project"
            projectExportPage.readyHeadingText.contains("Download an archive of project ${SELENIUM_BASIC_PROJECT}".toString())
        where:
            [nextUi] << UI_MODES
    }

    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "ready state shows the download button and the return link"() {
        when:
            def projectExportPage = startExport(nextUi)
            projectExportPage.waitForExportReady()
        then: "the download button points at the one-shot download URL and names the archive"
            projectExportPage.downloadText == "${SELENIUM_BASIC_PROJECT}.rdproject.jar".toString()
            projectExportPage.downloadHref.contains("/project/${SELENIUM_BASIC_PROJECT}/exportWait/")
            projectExportPage.downloadHref.endsWith("download=true")
        and: "the return link goes back to the export configuration"
            projectExportPage.returnLink.getAttribute('href').endsWith("/project/${SELENIUM_BASIC_PROJECT}/export")
        where:
            [nextUi] << UI_MODES
    }

    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "download link serves the archive as an attachment"() {
        when: "the ready state is reached and the download URL is requested with the page's session"
            def projectExportPage = startExport(nextUi)
            projectExportPage.waitForExportReady()
            def (status, disposition) = projectExportPage.fetchDownloadHeaders()
        then:
            status == 200
            disposition.toString().contains('attachment')
            disposition.toString().contains("${SELENIUM_BASIC_PROJECT}".toString())
            disposition.toString().contains('.rdproject.jar')
        where:
            [nextUi] << UI_MODES
    }

    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "clicking the download button keeps the page on the ready state"() {
        when:
            def projectExportPage = startExport(nextUi)
            projectExportPage.waitForExportReady()
            projectExportPage.downloadArchiveButton.click()
        then: "the attachment download does not navigate away or raise an error panel"
            projectExportPage.waitForExportReady()
            projectExportPage.errorPanels.collect {it.isDisplayed() }.every {it == false}
        where:
            [nextUi] << UI_MODES
    }

    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "return link goes back to the export configuration page"() {
        when:
            def projectExportPage = startExport(nextUi)
            projectExportPage.waitForExportReady()
            projectExportPage.returnLink.click()
        then:
            projectExportPage.waitForUrlToNotContain("/exportWait/")
            projectExportPage.waitForElementVisible(projectExportPage.exportArchiveButtonBy)
        where:
            [nextUi] << UI_MODES
    }

    @UiModeFlag(featureName = 'menu-wait', status = UiModeStatus.NEXT_UI, mechanism = UiMechanism.COOKIE,
        description = 'Export wait page is Vue behind the nextUi cookie; legacy KO/GSP is the default.')
    def "unknown token shows the generic not found error page"() {
        given:
            def projectExportPage = page(ProjectExportPage, SELENIUM_BASIC_PROJECT, [nextUi: nextUi])
        when:
            projectExportPage.goToWaitPage('bogus-token')
        then: "the server-side error page renders, not the wait page"
            projectExportPage.errorTitleText == 'Not Found'
            projectExportPage.errorBodyText.contains('bogus-token')
            !projectExportPage.vueMountPresent
        where:
            [nextUi] << UI_MODES
    }

    def "form radio inputs have proper name"() {
        when:
            def projectExportPage = go ProjectExportPage, SELENIUM_BASIC_PROJECT
        then:
            projectExportPage.stripJobRefRadios.any {it.isSelected() }
            projectExportPage.stripJobRefRadios.size() == 3
    }

    def "form radio inputs have labels"() {
        when:
            def projectExportPage = go ProjectExportPage, SELENIUM_BASIC_PROJECT
        then:
            projectExportPage.stripNameLabels.size() == 3
    }

    def "form checkboxes are checked by default"() {
        when:
            def projectExportPage = go ProjectExportPage, SELENIUM_BASIC_PROJECT
        then:
            projectExportPage.checkBoxes.size() >= 9
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == "true" } >= 8
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == null } == 2
    }

    def "form checkbox labels work"() {
        when:
            def projectExportPage = go ProjectExportPage, SELENIUM_BASIC_PROJECT
        then:
            projectExportPage.checkBoxes.each { checkbox ->
                String checkBoxId = checkbox.getAttribute("id")
                projectExportPage.checkBoxLabel checkBoxId click()
            }
        expect:
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == "true" } >= 8
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == null } <= 2
    }

    def "clicking all checkbox when selected deselects all checkboxes"() {
        when:
            def projectExportPage = go ProjectExportPage, SELENIUM_BASIC_PROJECT
        then:
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == "true" } >= 8
        when:
            projectExportPage.getAllCheckbox().click()
        then:
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == null } >= 8
    }

}
