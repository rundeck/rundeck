package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
@ExcludePro
class BasicJobsCoreEditorSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "create valid job basic workflow"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.fillBasicJob 'a valid job with basic workflow'
            jobCreatePage.createJobButton.click()
        expect:
            jobShowPage.validatePage()
            jobShowPage.jobLinkTitleLabel.getText() == 'a valid job with basic workflow'
    }
}
