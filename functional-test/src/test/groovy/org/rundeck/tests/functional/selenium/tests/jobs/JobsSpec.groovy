package org.rundeck.tests.functional.selenium.tests.jobs

import org.rundeck.tests.functional.selenium.pages.jobs.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.jobs.JobListPage
import org.rundeck.tests.functional.selenium.pages.jobs.JobShowPage
import org.rundeck.tests.functional.selenium.pages.jobs.JobTab
import org.rundeck.tests.functional.selenium.pages.jobs.StepType
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.tests.functional.selenium.pages.profile.UserProfilePage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class JobsSpec extends SeleniumBase {

    def setupSpec() {
        setupProject("SeleniumBasic", "/projects-import/SeleniumBasic.zip")
    }

    def "change workflow strategy"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.jobNameInput.sendKeys 'jobs workflow strategy'
            jobCreatePage.tab JobTab.WORKFLOW click()
            jobCreatePage.workFlowStrategyField.sendKeys 'Parallel'
            jobCreatePage.waitIgnoringForElementVisible jobCreatePage.strategyPluginParallelField
            jobCreatePage.strategyPluginParallelMsgField.getText() == 'Run all steps in parallel'

            jobCreatePage.executor "window.location.hash = '#addnodestep'"
            jobCreatePage.stepLink 'command', StepType.NODE click()
            jobCreatePage.waitForElementVisible jobCreatePage.adhocRemoteStringField
            jobCreatePage.adhocRemoteStringField.click()
            jobCreatePage.waitForNumberOfElementsToBe jobCreatePage.floatBy
            jobCreatePage.adhocRemoteStringField.sendKeys 'echo selenium test'
            jobCreatePage.saveStep 0
            jobCreatePage.createJobButton.click()
        expect:
            def jobShowPage = page JobShowPage
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.workflowDetailField.getText() == 'Parallel Run all steps in parallel'
    }

    def "cancel job create with default lang"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.cancelButton.click()
        expect:
            def jobListPage = page JobListPage
            jobListPage.validatePage()
    }

    def "change UI lang fr_FR and cancel job create"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userProfilePage = page UserProfilePage
            userProfilePage.loadPath += "?lang=fr_FR"
            userProfilePage.go()
            userProfilePage.languageLabel.getText() == 'Langue:'
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.cancelButton.click()
        expect:
            def jobListPage = page JobListPage
            jobListPage.validatePage()
    }

    def "change UI lang ja_JP and cancel job create"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def userProfilePage = page UserProfilePage
            userProfilePage.loadPath += "?lang=ja_JP"
            userProfilePage.go()
            userProfilePage.languageLabel.getText() == '言語:'
            userProfilePage.go()
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.cancelButton.click()
        expect:
            def jobListPage = page JobListPage
            jobListPage.validatePage()
    }

    def "Duplicate_options - only validations, not save jobs"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.jobNameInput.sendKeys 'duplicate options'
            jobCreatePage.tab JobTab.WORKFLOW click()
            jobCreatePage.executor "window.location.hash = '#addnodestep'"
            jobCreatePage.stepLink 'command', StepType.NODE click()
            jobCreatePage.waitForElementVisible jobCreatePage.adhocRemoteStringField
            jobCreatePage.adhocRemoteStringField.click()
            jobCreatePage.waitForNumberOfElementsToBe jobCreatePage.floatBy
            jobCreatePage.adhocRemoteStringField.sendKeys 'echo selenium test'
            jobCreatePage.saveStep 0

            def optName = 'test'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys optName
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0

            jobCreatePage.duplicateButton optName click()
            jobCreatePage.waitFotOptLi 1

            jobCreatePage.duplicateButton optName click()
            jobCreatePage.waitFotOptLi 2

        expect:
            jobCreatePage.optionNameSaved 1 getText() equals optName + '_1'
            jobCreatePage.optionNameSaved 2 getText() equals optName + '_2'
            jobCreatePage.createJobButton.click()
    }

}
