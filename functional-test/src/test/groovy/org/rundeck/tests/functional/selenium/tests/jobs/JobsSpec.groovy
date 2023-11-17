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
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
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
            jobCreatePage.fillBasicJob 'duplicate options'
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

    def "create job with dispatch to nodes"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.fillBasicJob 'jobs with nodes'
            jobCreatePage.tab JobTab.NODES click()
            jobCreatePage.nodeDispatchTrueCheck.click()
            jobCreatePage.waitForElementVisible jobCreatePage.nodeFilterLinkButton
            jobCreatePage.nodeFilterLinkButton.click()
            jobCreatePage.nodeFilterSelectAllLinkButton.click()
            jobCreatePage.waitForElementVisible jobCreatePage.nodeMatchedCountField
            jobCreatePage.nodeMatchedCountField.getText() == '1 Node Matched'
            jobCreatePage.excludeFilterTrueCheck.click()
            jobCreatePage.editableFalseCheck.click()
            jobCreatePage.schedJobNodeThreadCountField.clear()
            jobCreatePage.schedJobNodeThreadCountField.sendKeys '3'
            jobCreatePage.schedJobNodeRankAttributeField.clear()
            jobCreatePage.schedJobNodeRankAttributeField.sendKeys 'arank'
            jobCreatePage.nodeRankOrderDescendingField.click()
            jobCreatePage.nodeKeepGoingTrueCheck.click()
            jobCreatePage.successOnEmptyNodeFilterTrueCheck.click()
            jobCreatePage.nodesSelectedByDefaultFalseCheck.click()
            jobCreatePage.createJobButton.click()
        expect:
            def jobShowPage = page JobShowPage
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.nodeFilterSectionMatchedNodesLabel.getText() == 'Include nodes matching: name: .*'
            jobShowPage.threadCountLabel.getText() == 'Execute on up to 3 Nodes at a time.'
            jobShowPage.nodeKeepGoingLabel.getText() == 'If a node fails: Continue running on any remaining nodes before failing the step.'
            jobShowPage.nodeRankOrderAscendingLabel.getText() == 'Sort nodes by arank in descending order.'
            jobShowPage.nodeSelectedByDefaultLabel.getText() == 'Node selection: The user has to explicitly select target nodes'
    }

    def "rename job with orchestrator"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.fillBasicJob 'job with node orchestrator'
            jobCreatePage.tab JobTab.NODES click()
            jobCreatePage.nodeDispatchTrueCheck.click()
            jobCreatePage.waitForElementVisible jobCreatePage.nodeFilterLinkButton
            jobCreatePage.nodeFilterLinkButton.click()
            jobCreatePage.nodeFilterSelectAllLinkButton.click()
            jobCreatePage.nodeMatchedCountField.isDisplayed()
            jobCreatePage.nodeMatchedCountField.getText() == '1 Node Matched'
            jobCreatePage.executor "window.location.hash = '#orchestrator-edit-type-dropdown'"
            jobCreatePage.orchestratorDropdownButton.click()
            jobCreatePage.orchestratorChoiceLink 'rankTiered' click()
            jobCreatePage.createJobButton.click()
        expect:
            def jobShowPage = page JobShowPage
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.orchestratorNameLabel.getText() == 'Rank Tiered'
            jobShowPage.closeDefinitionModalButton.click()
            jobShowPage.jobActionDropdownButton.click()
            jobShowPage.editJobLink.click()
            jobCreatePage.jobNameInput.clear()
            jobCreatePage.jobNameInput.sendKeys 'renamed job with node orchestrator'
            jobCreatePage.tab JobTab.NODES click()
            jobCreatePage.updateJobButton.click()
            jobShowPage.jobLinkTitleLabel.getText() == 'renamed job with node orchestrator'
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.orchestratorNameLabel.getText() == 'Rank Tiered'
    }

    def "job options config - check usage session"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.fillBasicJob 'a job with options'
            jobCreatePage.optionButton.click()
            jobCreatePage.waitForElementVisible(jobCreatePage.optionName 0)
            jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.createJobButton.click()
    }

    def "job options config - check storage session"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
            jobCreatePage.fillBasicJob 'job with node orchestrator'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.executor "window.location.hash = '#opt_sec_nexp_disabled'"
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.createJobButton.click()
    }

    def "job option simple redo"() {
        setup:
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
        when:
        def jobCreatePage = go JobCreatePage, "project/SeleniumBasic"
        then:
        jobCreatePage.fillBasicJob 'job with node orchestrator'
        jobCreatePage.optionButton.click()
        jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.executor "window.location.hash = '#opt_sec_nexp_disabled'"
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.saveOptionButton.click()
    }

}
