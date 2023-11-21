package org.rundeck.tests.functional.selenium.tests.project

import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.tests.functional.selenium.pages.project.ProjectExportPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class ProjectExportSpec extends SeleniumBase {

    def setupSpec() {
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def "exports without errors"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def projectExportPage = go ProjectExportPage, "SeleniumBasic"
        then:
            projectExportPage.submitExportButton.click()
            projectExportPage.waitForElementVisible projectExportPage.downloadArchiveButton
        expect:
            projectExportPage.errorPanels.collect {it.isDisplayed() } every {it == false}
    }

    def "form radio inputs have proper name"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def projectExportPage = go ProjectExportPage, "SeleniumBasic"
        then:
            projectExportPage.stripJobRefRadios.any {it.isSelected() }
            projectExportPage.stripJobRefRadios.size() == 3
    }

    def "form radio inputs have labels"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def projectExportPage = go ProjectExportPage, "SeleniumBasic"
        then:
            projectExportPage.stripNameLabels.size() == 3
    }

    def "form checkboxes are checked by default"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def projectExportPage = go ProjectExportPage, "SeleniumBasic"
        then:
            projectExportPage.checkBoxes.size() == 9
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == "true" } == 8
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == null } == 1
    }

    def "form checkbox labels work"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def projectExportPage = go ProjectExportPage, "SeleniumBasic"
        then:
            projectExportPage.checkBoxes.each { checkbox ->
                String checkBoxId = checkbox.getAttribute("id")
                projectExportPage.checkBoxLabel checkBoxId click()
            }
        expect:
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == "true" } == 1
            projectExportPage.checkBoxes.count {it.getAttribute("checked") == null } == 8
    }

}
