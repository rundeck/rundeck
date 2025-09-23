package org.rundeck.tests.functional.selenium.project

import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ProjectExportPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class ProjectExportSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "exports without errors"() {
        when:
            def projectExportPage = go ProjectExportPage, SELENIUM_BASIC_PROJECT
        then:
            projectExportPage.submitExportButton.click()
            projectExportPage.waitForElementVisible projectExportPage.downloadArchiveButton
        expect:
            projectExportPage.errorPanels.collect {it.isDisplayed() } every {it == false}
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
