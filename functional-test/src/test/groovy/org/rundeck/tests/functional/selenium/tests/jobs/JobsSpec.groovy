package org.rundeck.tests.functional.selenium.tests.jobs

import org.rundeck.tests.functional.selenium.pages.jobs.JobCreatePage
import org.rundeck.tests.functional.selenium.pages.jobs.JobEditPage
import org.rundeck.tests.functional.selenium.pages.jobs.JobListPage
import org.rundeck.tests.functional.selenium.pages.jobs.JobShowPage
import org.rundeck.tests.functional.selenium.pages.jobs.JobTab
import org.rundeck.tests.functional.selenium.pages.jobs.JobUploadPage
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
        setupProject(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}.zip")
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "change workflow strategy"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.jobNameInput.sendKeys 'jobs workflow strategy'
            jobCreatePage.tab JobTab.WORKFLOW click()
            jobCreatePage.workFlowStrategyField.sendKeys 'Parallel'
            jobCreatePage.waitIgnoringForElementVisible jobCreatePage.strategyPluginParallelField
            jobCreatePage.strategyPluginParallelMsgField.getText() == 'Run all steps in parallel'

            jobCreatePage.executeScript "window.location.hash = '#addnodestep'"
            jobCreatePage.stepLink 'exec-command', StepType.NODE click()
            jobCreatePage.waitForElementVisible jobCreatePage.adhocRemoteStringField
            jobCreatePage.adhocRemoteStringField.click()
            jobCreatePage.waitForNumberOfElementsToBeOne jobCreatePage.floatBy
            jobCreatePage.adhocRemoteStringField.sendKeys 'echo selenium test'
            jobCreatePage.saveStep 0
            jobCreatePage.createJobButton.click()
        expect:
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.workflowDetailField.getText() == 'Parallel Run all steps in parallel'
    }

    def "cancel job create with default lang"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobListPage = page JobListPage
        then:
            jobCreatePage.cancelButton.click()
        expect:
            jobListPage.validatePage()
    }

    def "change UI lang fr_FR and cancel job create"() {
        setup:
            def userProfilePage = page UserProfilePage
            def jobCreatePage = page JobCreatePage
            def jobListPage = page JobListPage
        when:
            userProfilePage.loadPath += "?lang=fr_FR"
            userProfilePage.go()
            jobCreatePage.loadCreatePath SELENIUM_BASIC_PROJECT
        then:
            userProfilePage.languageLabel.getText() == 'Langue:'
            jobCreatePage.go()
            jobCreatePage.cancelButton.click()
        expect:
            jobListPage.validatePage()
    }

    def "change UI lang ja_JP and cancel job create"() {
        setup:
            def userProfilePage = page UserProfilePage
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobListPage = page JobListPage
        when:
            userProfilePage.loadPath += "?lang=ja_JP"
            userProfilePage.go()
            jobCreatePage.loadCreatePath SELENIUM_BASIC_PROJECT
        then:
            userProfilePage.languageLabel.getText() == '言語:'
            jobCreatePage.go()
            jobCreatePage.cancelButton.click()
        expect:
            jobListPage.validatePage()
    }

    def "Duplicate_options - only validations, not save jobs"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def optName = 'test'
        then:
            jobCreatePage.fillBasicJob 'duplicate options'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys optName
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.executeScript "window.location.hash = '#workflowKeepGoingFail'"
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0

            jobCreatePage.duplicateButton optName click()
            jobCreatePage.waitFotOptLi 1

            jobCreatePage.duplicateButton optName click()
            jobCreatePage.waitFotOptLi 2

        expect:
            jobCreatePage.optionNameSaved 1 getText() equals optName + '_1'
            jobCreatePage.optionNameSaved 2 getText() equals optName + '_2'
    }

    def "create job with dispatch to nodes"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.fillBasicJob 'jobs with nodes'
            jobCreatePage.tab JobTab.NODES click()
            jobCreatePage.nodeDispatchTrueCheck.click()
            jobCreatePage.waitForElementVisible jobCreatePage.nodeFilterLinkButton
            jobCreatePage.nodeFilterLinkButton.click()
            jobCreatePage.nodeFilterSelectAllLinkButton.click()
            jobCreatePage.waitForTextToBePresentInElement jobCreatePage.nodeMatchedCountField, '1 Node Matched'
            jobCreatePage.excludeFilterTrueCheck.click()
            jobCreatePage.editableFalseCheck.click()
            jobCreatePage.schedJobNodeThreadCountField.clear()
            jobCreatePage.schedJobNodeThreadCountField.sendKeys '3'
            jobCreatePage.schedJobNodeRankAttributeField.clear()
            jobCreatePage.schedJobNodeRankAttributeField.sendKeys 'arank'
            jobCreatePage.executeScript "window.location.hash = '#nodeRankOrderDescending'"
            jobCreatePage.nodeRankOrderDescendingField.click()
            jobCreatePage.nodeKeepGoingTrueCheck.click()
            jobCreatePage.successOnEmptyNodeFilterTrueCheck.click()
            jobCreatePage.nodesSelectedByDefaultFalseCheck.click()
            jobCreatePage.createJobButton.click()
            jobShowPage.jobDefinitionModal.click()
        expect:
            jobShowPage.nodeFilterSectionMatchedNodesLabel.getText() == 'Include nodes matching: name: .*'
            jobShowPage.threadCountLabel.getText() == 'Execute on up to 3 Nodes at a time.'
            jobShowPage.nodeKeepGoingLabel.getText() == 'If a node fails: Continue running on any remaining nodes before failing the step.'
            jobShowPage.nodeRankOrderAscendingLabel.getText() == 'Sort nodes by arank in descending order.'
            jobShowPage.nodeSelectedByDefaultLabel.getText() == 'Node selection: The user has to explicitly select target nodes'
    }

    def "rename job with orchestrator"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
            def jobShowPage = page JobShowPage
        then:
            jobCreatePage.fillBasicJob 'job with node orchestrator'
            jobCreatePage.tab JobTab.NODES click()
            jobCreatePage.nodeDispatchTrueCheck.click()
            jobCreatePage.waitForElementVisible jobCreatePage.nodeFilterLinkButton
            jobCreatePage.nodeFilterLinkButton.click()
            jobCreatePage.nodeFilterSelectAllLinkButton.click()
            jobCreatePage.waitForTextToBePresentInElement jobCreatePage.nodeMatchedCountField, '1 Node Matched'
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.orchestratorDropdownButton
            jobCreatePage.waitForElementToBeClickable jobCreatePage.orchestratorDropdownButton
            jobCreatePage.orchestratorDropdownButton.click()
            jobCreatePage.orchestratorChoiceLink 'rankTiered' click()
            jobCreatePage.createJobButton.click()
            jobShowPage.jobDefinitionModal.click()
        expect:
            jobShowPage.orchestratorNameLabel.getText() == 'Rank Tiered'
            jobShowPage.closeDefinitionModalButton.click()
            jobShowPage.jobActionDropdownButton.click()
            jobShowPage.editJobLink.click()
            jobCreatePage.jobNameInput.clear()
            jobCreatePage.jobNameInput.sendKeys 'renamed job with node orchestrator'
            jobCreatePage.tab JobTab.NODES click()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.updateJobButton
            jobCreatePage.updateJobButton.click()
            jobShowPage.jobLinkTitleLabel.getText() == 'renamed job with node orchestrator'
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.orchestratorNameLabel.getText() == 'Rank Tiered'
    }

    def "job options config - check usage session"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with options'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.sessionSectionLabel
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
    }

    def "job options config - check storage session"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with option secure'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.secureInputTypeRadio.click()
            jobCreatePage.optionOpenKeyStorageButton.click()
            jobCreatePage.optionCloseKeyStorageButton.click()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
    }

    def "job option simple redo"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with options undo test'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 1 sendKeys 'seleniumOption2'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 1
            jobCreatePage.optionUndoButton.click()
        expect:
            jobCreatePage.waitForOptionsToBe 1, 0
            jobCreatePage.optionLis 1 isEmpty()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
    }

    def "No default value field shown in secure job option section"() {
        when:
        def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
        jobCreatePage.fillBasicJob 'a job with option secure'
        jobCreatePage.optionButton.click()
        jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.secureInputTypeRadio.click()
        jobCreatePage.storagePathInput.sendKeys("test")
        jobCreatePage.secureInputTypeRadio.click()
        jobCreatePage.storagePathInput.clear()
        jobCreatePage.secureInputTypeRadio.click()

        expect:
        !jobCreatePage.defaultValueInput.isDisplayed()
    }

    def "Upload job definition with secure options + default values, get info"() {
        setup:
        def uploadJobDefinitionPage = page JobUploadPage
        uploadJobDefinitionPage.loadPathToUploadPage(SELENIUM_BASIC_PROJECT)
        def tempYamlText = "- defaultTab: nodes\n  description: ''\n  executionEnabled: true\n  id: c19d5338-770b-493f-8125-bfa4634515bb\n  loglevel: INFO\n  name: test\n  nodeFilterEditable: false\n  options:\n  - name: secure_opt\n    secure: true\n    value: anything\n    valueExposed: true\n  plugins:\n    ExecutionLifecycle: {}\n  scheduleEnabled: true\n  schedules: []\n  sequence:\n    commands:\n    - exec: echo 'asd'\n    keepgoing: false\n    strategy: node-first\n  uuid: c19d5338-770b-493f-8125-bfa4634515bb"
        def tempFile = File.createTempFile("temp", ".yaml")
        tempFile.text = tempYamlText
        tempFile.deleteOnExit()

        when:
        uploadJobDefinitionPage.go()
        uploadJobDefinitionPage.fileInputElement().sendKeys(tempFile.getAbsolutePath())
        uploadJobDefinitionPage.fileUploadButtonElement().click()
        uploadJobDefinitionPage.waitForElementVisible(uploadJobDefinitionPage.jobUploadInfoDivElement())

        then:
        uploadJobDefinitionPage.jobUploadInfoDivElement().isDisplayed()
    }

    def "Secure options with default values, hint in the definition form"() {
        setup:
        def uploadJobDefinitionPage = page JobUploadPage
        uploadJobDefinitionPage.loadPathToUploadPage(SELENIUM_BASIC_PROJECT)

        def tempYamlText = "- defaultTab: nodes\n  description: ''\n  executionEnabled: true\n  id: c19d5338-770b-493f-8125-bfa4634515bb\n  loglevel: INFO\n  name: test\n  nodeFilterEditable: false\n  options:\n  - name: secure_opt\n    secure: true\n    value: anything\n    valueExposed: true\n  plugins:\n    ExecutionLifecycle: {}\n  scheduleEnabled: true\n  schedules: []\n  sequence:\n    commands:\n    - exec: echo 'asd'\n    keepgoing: false\n    strategy: node-first\n  uuid: c19d5338-770b-493f-8125-bfa4634515bb"
        def tempFile = File.createTempFile("temp", ".yaml")
        tempFile.text = tempYamlText
        tempFile.deleteOnExit()

        def jobEditPage = page JobEditPage
        jobEditPage.loadPathToEditByUuid(SELENIUM_BASIC_PROJECT, 'c19d5338-770b-493f-8125-bfa4634515bb')

        when:
        uploadJobDefinitionPage.go()
        uploadJobDefinitionPage.fileInputElement().sendKeys(tempFile.getAbsolutePath())
        uploadJobDefinitionPage.fileUploadButtonElement().click()
        jobEditPage.go()
        jobEditPage.workflowTabElement().click()
        jobEditPage.secureOptionSpanElement().click()

        then:
        jobEditPage.defaultValueInputElement().isDisplayed()
        jobEditPage.secureOptionDefaultValueHelpElement().isDisplayed()

    }

    def "job option revert all"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with options revert all test'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 1 sendKeys 'seleniumOption2'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 1
            jobCreatePage.executeScript "window.location.hash = '#optundoredo'"
            jobCreatePage.optionUndoButton
            jobCreatePage.optionRevertAllButton.click()
            jobCreatePage.optionConfirmRevertAllButton.click()
        expect:
            jobCreatePage.waitForOptionsToBe 0, 0
            jobCreatePage.waitForOptionsToBe 1, 0
            jobCreatePage.optionLis 0 isEmpty()
            jobCreatePage.optionLis 1 isEmpty()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
    }

    def "job option undo redo"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with options undo-redo test'
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 0 sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.optionButton.click()
            jobCreatePage.optionName 1 sendKeys 'seleniumOption2'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 1
            jobCreatePage.executeScript "window.location.hash = '#optundoredo'"
            jobCreatePage.optionUndoButton.click()
            jobCreatePage.waitForElementToBeClickable jobCreatePage.optionRedoButton
            jobCreatePage.optionRedoButton.click()
        expect:
            !(jobCreatePage.optionLis 0 isEmpty())
            !(jobCreatePage.optionLis 1 isEmpty())
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
    }

    def "job workflow step context variables autocomplete"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.jobNameInput.sendKeys 'job workflow step context variables autocomplete'
            jobCreatePage.tab JobTab.WORKFLOW click()
            jobCreatePage.executeScript "window.location.hash = '#addnodestep'"
            jobCreatePage.workFlowStepLink.click()
            jobCreatePage.stepLink 'com.batix.rundeck.plugins.AnsiblePlaybookInlineWorkflowStep', StepType.WORKFLOW click()
            jobCreatePage.ansibleBinariesPathField.clear()
            jobCreatePage.ansibleBinariesPathField.sendKeys '${job.id'
            jobCreatePage.autocompleteSuggestions.click()
            jobCreatePage.saveStep 0
            jobCreatePage.createJobButton.click()
        expect:
            def jobShowPage = page JobShowPage
            jobShowPage.jobDefinitionModal.click()
            jobShowPage.autocompleteJobStepDefinitionLabel.getText() == '${job.id}'
    }

    def "job workflow simple undo"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with workflow undo test'
            jobCreatePage.addSimpleCommandStepButton.click()
            jobCreatePage.addSimpleCommandStep 'echo selenium test 2', 1
            jobCreatePage.wfUndoButton.click()
            jobCreatePage.waitForNumberOfElementsToBe jobCreatePage.listWorkFlowItemBy, 1
        expect:
            jobCreatePage.workFlowList.size() == 1
            jobCreatePage.createJobButton.click()
    }

    def "job workflow undo redo"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with workflow undo-redo test'
            jobCreatePage.addSimpleCommandStepButton.click()
            jobCreatePage.addSimpleCommandStep 'echo selenium test 2', 1
            jobCreatePage.wfUndoButton.click()
            jobCreatePage.waitForElementToBeClickable jobCreatePage.wfRedoButton
            jobCreatePage.wfRedoButton.click()
            jobCreatePage.waitForNumberOfElementsToBe jobCreatePage.listWorkFlowItemBy, 2
        expect:
            jobCreatePage.workFlowList.size() == 2
            jobCreatePage.createJobButton.click()
    }

    def "job workflow revert all"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.fillBasicJob 'a job with workflow revert all test'
            jobCreatePage.addSimpleCommandStepButton.click()
            jobCreatePage.addSimpleCommandStep 'echo selenium test 2', 1
            jobCreatePage.wfRevertAllButton.click()
            jobCreatePage.revertWfConfirmYes.click()
            jobCreatePage.waitForNumberOfElementsToBe jobCreatePage.listWorkFlowItemBy, 0
        expect:
            jobCreatePage.workFlowList.size() == 0
            jobCreatePage.addSimpleCommandStepButton.click()
            jobCreatePage.addSimpleCommandStep 'echo selenium test', 0
            jobCreatePage.createJobButton.click()
    }
}
