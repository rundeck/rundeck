package org.rundeck.tests.functional.selenium.jobs

import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.openqa.selenium.support.ui.Select
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.api.responses.jobs.CreateJobResponse
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.activity.ActivityPage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.*
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.profile.UserProfilePage
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Stepwise

import java.util.stream.Collectors

@SeleniumCoreTest
@Stepwise
class JobsSpec extends SeleniumBase {

    def setupSpec() {
        setupProjectArchiveDirectoryResource(SELENIUM_BASIC_PROJECT, "/projects-import/${SELENIUM_BASIC_PROJECT}")
    }

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
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

    def "Duplicate_options - only validations, not save jobs old ui"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi = false
            jobCreatePage.go()
            def optName = 'test'
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew() sendKeys optName
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.executeScript "window.location.hash = '#workflowKeepGoingFail'"
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0

            jobCreatePage.duplicateButton optName click()
            jobCreatePage.waitFotOptLi 1

            jobCreatePage.duplicateButton optName click()
            jobCreatePage.waitFotOptLi 2

        then:
            jobCreatePage.optionNameSaved 1 getText() equals optName + '_1'
            jobCreatePage.optionNameSaved 2 getText() equals optName + '_2'
    }

    def "Duplicate option create form next ui"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=true
            jobCreatePage.go()
            def optName = 'test'
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew() sendKeys optName
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.executeScript "window.location.hash = '#workflowKeepGoingFail'"
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0

            jobCreatePage.duplicateButton( optName, 0) click()


        then: "create form is shown"
            jobCreatePage.optionNameNew() displayed
            jobCreatePage.optionNameNew().getAttribute('value') == optName + '_copy'
            jobCreatePage.saveOptionButton.displayed
        when:
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 1

        then:
            jobCreatePage.optionNameSaved 0 getText() equals optName
            jobCreatePage.optionNameSaved 1 getText() equals optName + '_copy'

        where:
            nextUi = true
    }

    def "Create option form next ui"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=true
            jobCreatePage.go()
            def optName = 'test'
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
            jobCreatePage.optionButton.click()

        then: "create form is shown"
            jobCreatePage.optionNameNew() displayed
            jobCreatePage.saveOptionButton.displayed
        where:
            nextUi = true
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
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
        then:
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.sessionSectionLabel
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
        where:
            nextUi << [false, true]
    }
    def "job options config - check storage session"() {
        given:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
        when:
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
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
        then:
            jobCreatePage.waitForOptionsToBe 1, 0
            jobCreatePage.optionLis 0 isEmpty()
        where:
            nextUi << [false, true]
    }
    def "job option simple redo"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
        then:
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew(1) sendKeys 'seleniumOption2'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 1
            jobCreatePage.waitForElementAttributeToChange jobCreatePage.optionUndoButton, 'disabled', null
            jobCreatePage.optionUndoButton.click()
        expect:
            jobCreatePage.waitForOptionsToBe 1, 0
            jobCreatePage.optionLis 1 isEmpty()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
        where:
            nextUi << [false, true]
    }
    def "No default value field shown in secure job option section"() {
        given:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
        when:
        jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
        jobCreatePage.optionButton.click()
        jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
        jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
        jobCreatePage.sessionSectionLabel.isDisplayed()
        jobCreatePage.secureInputTypeRadio.click()
        jobCreatePage.storagePathInput.sendKeys("test")
        jobCreatePage.secureInputTypeRadio.click()
        jobCreatePage.storagePathInput.clear()
        jobCreatePage.secureInputTypeRadio.click()

        then:
        driver.findElements(jobCreatePage.defaultValueBy).isEmpty() || !jobCreatePage.defaultValueInput.isDisplayed()
        where:
        nextUi << [false, true]
    }
    def "job option revert all"() {
        given:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew(1) sendKeys 'seleniumOption2'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 1
            jobCreatePage.executeScript "window.location.hash = '#optundoredo'"
            jobCreatePage.waitForElementAttributeToChange jobCreatePage.optionUndoButton, 'disabled', null
            jobCreatePage.optionUndoButton
        when:
            jobCreatePage.optionRevertAllButton.click()
            if(!nextUi) {
                jobCreatePage.optionConfirmRevertAllButton.click()
            }
        then:
            jobCreatePage.waitForOptionsToBe 0, 0
            jobCreatePage.waitForOptionsToBe 1, 0
            jobCreatePage.optionLis 0 isEmpty()
            jobCreatePage.optionLis 1 isEmpty()
        where:
            nextUi << [false, true]
    }
    def "job option undo redo"() {
        when:
            def jobCreatePage = page JobCreatePage, SELENIUM_BASIC_PROJECT
            jobCreatePage.nextUi=nextUi
            jobCreatePage.go()
        then:
            jobCreatePage.fillBasicJob specificationContext.currentIteration.name+" ${nextUi ? "next ui" : "old ui"}"
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew() sendKeys 'seleniumOption1'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 0
            jobCreatePage.optionButton.click()
            jobCreatePage.optionNameNew(1) sendKeys 'seleniumOption2'
            jobCreatePage.waitForElementVisible jobCreatePage.separatorOption
            jobCreatePage.sessionSectionLabel.isDisplayed()
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.saveOptionButton
            jobCreatePage.saveOptionButton.click()
            jobCreatePage.waitFotOptLi 1
            jobCreatePage.executeScript "window.location.hash = '#optundoredo'"
            jobCreatePage.waitForElementAttributeToChange jobCreatePage.optionUndoButton, 'disabled', null
            jobCreatePage.optionUndoButton.click()
            jobCreatePage.waitForElementToBeClickable jobCreatePage.optionRedoButton
            sleep 1000
            jobCreatePage.optionRedoButton.click()
        expect:
            !(jobCreatePage.optionLis 0 isEmpty())
            !(jobCreatePage.optionLis 1 isEmpty())
            jobCreatePage.executeScript "arguments[0].scrollIntoView(true);", jobCreatePage.createJobButton
            jobCreatePage.createJobButton.click()
        where:
            nextUi << [false, true]
    }

    def "job workflow step context variables autocomplete"() {
        when:
            def jobCreatePage = go JobCreatePage, SELENIUM_BASIC_PROJECT
        then:
            jobCreatePage.jobNameInput.sendKeys 'job workflow step context variables autocomplete'
            jobCreatePage.tab JobTab.WORKFLOW click()
            jobCreatePage.executeScript "window.location.hash = '#addnodestep'"
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
            jobCreatePage.waitForElementToBeClickable jobCreatePage.wfUndoButtonLink
            jobCreatePage.wfUndoButtonLink.click()
            jobCreatePage.waitForElementToBeClickable jobCreatePage.wfRedoButtonLink
            jobCreatePage.wfRedoButtonLink.click()
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

    def "job timeout should finish job with timeout status and step marked as failed"() {
        setup:
        final String projectName = 'JobTimeOutTest'
        setupProjectArchiveDirectoryResource(projectName, "/projects-import/${projectName}.rdproject")
        JobShowPage jobPage = page(JobShowPage, projectName).forJob('1032a729-c251-4940-86b5-20f99cb5e769')
        jobPage.go()

        when:
        ExecutionShowPage executionPage = jobPage.runJob(true)

        then:
        noExceptionThrown()
        verifyAll {
            executionPage.getExecutionStatus() == 'TIMEDOUT'
            executionPage.getNodesView().expandNode(0).getExecStateForSteps() == ['SUCCEEDED', 'FAILED']
        }

    }

    /**
     * Runs a job via "Run job later" and waits until job its executed.
     *
     */
    def "Run job later"() {
        given:
        def projectName = "run-job-later-test"
        JobShowPage jobShowPage = page JobShowPage
        ActivityPage activityPage = page ActivityPage
        ExecutionShowPage executionShowPage = page ExecutionShowPage
        def mapper = new ObjectMapper()

        when:
        setupProject(projectName)
        def jobName = "test-run-job-later-job"
        def yamlJob = """
                        -
                          project: ${projectName}
                          loglevel: INFO
                          sequence:
                            keepgoing: false
                            strategy: node-first
                            commands:
                            - exec: echo hello there
                          description: ''
                          name: ${jobName}
                        """
        def pathToJob = JobUtils.generateFileToImport(yamlJob, "yaml")
        def multipartBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("xmlBatch", new File(pathToJob).name, RequestBody.create(new File(pathToJob), MultipartBody.FORM))
                .build()
        def response = client.doPostWithMultipart("/project/${projectName}/jobs/import?format=yaml&dupeOption=skip", multipartBody)
        assert response.successful
        def createdJob = mapper.readValue(response.body().string(), CreateJobResponse.class)
        def jobUuid = createdJob.succeeded[0]?.id
        jobShowPage.goToJob(jobUuid as String)
        jobShowPage.validatePage()
        jobShowPage.executionOptionsDropdown.click()
        jobShowPage.runJobLaterOption.click()
        jobShowPage.runJobLaterMinuteArrowUp.click()
        jobShowPage.runJobLaterCreateScheduleButton.click()
        executionShowPage.waitForElementVisible(executionShowPage.jobRunSpinner)
        executionShowPage.waitUntilSpinnerHides()
        executionShowPage.waitForElementVisible(executionShowPage.nodeFlowState)
        activityPage.loadActivityPageForProject(projectName)
        activityPage.go()
        def projectExecutions = Integer.parseInt(activityPage.executionCount.text)

        then:
        projectExecutions > 0

        cleanup:
        deleteProject(projectName)
    }

    /**
     * Using the step filter input, checks if the input brings up the right step to the view.
     *
     */
    def "Filter steps"(){
        given:
        def projectName = "filter-steps-later-test"
        JobCreatePage jobCreatePage = page JobCreatePage
        JobShowPage jobShowPage = page JobShowPage

        when:
        setupProject(projectName)
        go JobCreatePage, projectName
        jobCreatePage.jobNameInput.sendKeys("test")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.waitForElementToBeClickable(jobCreatePage.stepFilterInput)
        jobCreatePage.stepFilterInput.sendKeys("cmd")
        jobCreatePage.stepFilterSearchButton.click()

        then: "Command step is not visible, since the list dont have any steps"
        !jobCreatePage.commandStepVisible()

        when: "We provide a valid filter"
        jobCreatePage.stepFilterInput.sendKeys(Keys.chord(Keys.CONTROL, "a"))
        jobCreatePage.stepFilterInput.sendKeys(Keys.BACK_SPACE)
        jobCreatePage.stepFilterInput.sendKeys("command")
        jobCreatePage.stepFilterSearchButton.click()

        then: "We can create the command step"
        jobCreatePage.addSimpleCommandStep("echo 'asd'", 0)
        jobCreatePage.createJobButton.click()
        jobShowPage.waitForElementVisible(jobShowPage.jobUuid)
        jobShowPage.validatePage()

        cleanup:
        deleteProject(projectName)

    }

    /**
     * Checks if the list of options of type "json file values" are all selected by default when are
     * rendered into the view.
     *
     */
    def "Select all json list options by default"(){
        given:
        def projectName = "select-all-json-test"
        def optionListOfNames = "names"
        def optionListOfValues = "search"
        JobCreatePage jobCreatePage = page JobCreatePage
        JobShowPage jobShowPage = page JobShowPage

        when:
        setupProject(projectName)
        go JobCreatePage, projectName
        jobCreatePage.jobNameInput.sendKeys("test")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.optionButton.click()
        jobCreatePage.optionName(0).sendKeys(optionListOfNames)
        jobCreatePage.jobOptionListValueInput.sendKeys("option1,option2,option3,option4")
        jobCreatePage.jobOptionListDelimiter.sendKeys(",")
        jobCreatePage.jobOptionEnforcedInput.click()
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitForNumberOfElementsToBe(jobCreatePage.optEditFormBy, 0)
        jobCreatePage.waitForElementToBeClickable(jobCreatePage.optionButton)
        jobCreatePage.optionButton.click()
        jobCreatePage.optionName(1).sendKeys(optionListOfValues)
        jobCreatePage.jobOptionAllowedValuesRemoteUrlInput.click()
        jobCreatePage.jobOptionAllowedValuesRemoteUrlValueTextInput.sendKeys("file:/home/\${option.names.value}/saved_searches.json")
        jobCreatePage.jobOptionEnforcedInput.click()
        jobCreatePage.jobOptionRequiredInput.click()
        jobCreatePage.jobOptionMultiValuedInput.click()
        jobCreatePage.waitForElementVisible(jobCreatePage.jobOptionMultivaluedDelimiterBy)
        jobCreatePage.jobOptionMultivaluedDelimiter.sendKeys(",")
        jobCreatePage.jobOptionMultiValuedAllSelectedInput.click()
        jobCreatePage.saveOptionButton.click()

        jobCreatePage.addSimpleCommandStep("echo 'asd'", 0)
        jobCreatePage.createJobButton.click()
        def jobUuid = jobShowPage.jobUuid.text
        jobShowPage.goToJob(jobUuid)

        jobShowPage.waitForElementVisible(jobShowPage.getOptionSelectByName(optionListOfNames))

        jobShowPage.selectOptionFromOptionListByName(optionListOfNames, selection)
        jobShowPage.waitForElementToBeClickable(jobShowPage.getOptionSelectByName(optionListOfValues))
        jobShowPage.waitForNumberOfElementsToBe(By.name("extra.option.search"), Integer.valueOf(selection))
        def flag = true
        (0..(selection-1)).each{
            if(!jobShowPage.getOptionSelectChildren(optionListOfValues)[it].isSelected()) flag = false
        }
        noUnselectedOptions = flag

        then:
        jobShowPage.validatePage()

        cleanup:
        deleteProject(projectName)

        where:
        selection   | noUnselectedOptions
        2           | true
        3           | true
        4           | true

    }

    /**
     * Checks the basic step duplication into the workflow container.
     *
     */
    def "Step duplication"(){
        given:
        def projectName = "step-duplication-test"
        JobCreatePage jobCreatePage = page JobCreatePage
        JobShowPage jobShowPage = page JobShowPage
        ExecutionShowPage executionShowPage = page ExecutionShowPage

        when:
        setupProject(projectName)
        go JobCreatePage, projectName
        jobCreatePage.jobNameInput.sendKeys("test-duplication")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.addSimpleCommandStep "echo 'This is a simple job'", 0
        jobCreatePage.createJobButton.click()
        jobShowPage.waitForElementVisible(jobShowPage.jobActionDropdownButton)
        jobShowPage.jobActionDropdownButton.click()
        jobShowPage.waitForElementToBeClickable(jobShowPage.editJobLink)
        jobShowPage.editJobLink.click()
        jobCreatePage.waitForElementVisible(jobCreatePage.tab(JobTab.WORKFLOW))
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.duplicateWfStepButton.click()
        jobCreatePage.waitForElementVisible(jobCreatePage.getWfStepByListPosition(1))
        jobCreatePage.updateBtn.click()
        jobShowPage.waitForElementVisible(jobShowPage.jobUuid)
        jobShowPage.runJob(true)
        executionShowPage.viewButtonOutput.click()
        def logLines = executionShowPage.logOutput.stream().map {
            it.text
        }.collect(Collectors.toList())

        then:
        logLines.size() == 2
        logLines.forEach {
            it == 'This is a simple job'
        }

        cleanup:
        deleteProject(projectName)

    }

    /**
     * Checks the remote URL options functionality for jobs.
     *
     */
    def "Url job options"(){
        given:
        def projectName = "url-job-options-test"
        def labelToSelect = "Y Label"
        def expectedValue = "y value"
        JobCreatePage jobCreatePage = page JobCreatePage
        JobShowPage jobShowPage = page JobShowPage
        ExecutionShowPage executionShowPage = page ExecutionShowPage

        when:
        setupProject(projectName)
        go JobCreatePage, projectName
        jobCreatePage.jobNameInput.sendKeys("test-url-opts")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.optionButton.click()
        jobCreatePage.optionName(0).sendKeys("remote")
        jobCreatePage.scrollToElement(jobCreatePage.jobOptionAllowedValuesRemoteUrlInput)
        jobCreatePage.jobOptionAllowedValuesRemoteUrlInput.click()
        jobCreatePage.jobOptionAllowedValuesRemoteUrlValueTextInput.sendKeys("http://mock-server/remoteOptions.json")
        jobCreatePage.waitForElementVisible(jobCreatePage.saveOptionButton)
        jobCreatePage.scrollToElement(jobCreatePage.saveOptionButton)
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.addSimpleCommandStep "echo 'This is a simple job'", 0
        jobCreatePage.createJobButton.click()
        jobShowPage.waitForElementVisible(jobShowPage.jobUuid)
        jobShowPage.goToJob(jobShowPage.jobUuid.text)
        jobShowPage.waitForElementToBeClickable(jobShowPage.jobOptionsDropdown)
        def optionsDropdown = new Select(jobShowPage.jobOptionsDropdown)
        optionsDropdown.selectByVisibleText(labelToSelect)
        def selectedElement = optionsDropdown.getFirstSelectedOption()
        def optionValueSelected = selectedElement.getAttribute("value")
        jobShowPage.runJob(true)
        def optionValueExecuted = executionShowPage.optionValueSelected.text

        then:
        optionValueExecuted == optionValueSelected
        optionValueSelected == expectedValue
        optionValueExecuted == expectedValue

        cleanup:
        deleteProject(projectName)

    }

    /**
     * This test creates a job disables the executions and then enables it
     * It only validates via UI that the run button shows up when enabled
     */
    def "job execution disable/enable"(){
        given:
        String projectName = "enableDisableJobSchedule"
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        JobListPage jobListPage = page(JobListPage)
        jobListPage.loadJobListForProject(projectName)
        JobCreatePage jobCreatePage = page JobCreatePage
        when:
        jobShowPage.go()
        then:
        jobShowPage.waitForNumberOfElementsToBe(jobShowPage.jobExecutionDisabledIconBy, 0)
        jobShowPage.waitForNumberOfElementsToBe(jobShowPage.runJobBtnBy, 1)
        when:
        jobShowPage.getJobActionDropdownButton().click()
        jobShowPage.getEditJobLink().click()
        jobCreatePage.tab(JobTab.SCHEDULE).click()
        jobCreatePage.getExecutionEnabledFalse().click()
        jobCreatePage.getUpdateJobButton().click()
        then:
        jobShowPage.waitForNumberOfElementsToBe(jobShowPage.jobExecutionDisabledIconBy, 1)
        jobShowPage.waitForNumberOfElementsToBe(jobShowPage.runJobBtnBy, 0)
        when:
        jobShowPage.getJobActionDropdownButton().click()
        jobShowPage.getEditJobLink().click()
        jobCreatePage.tab(JobTab.SCHEDULE).click()
        jobCreatePage.getExecutionEnabledTrue().click()
        jobCreatePage.getUpdateJobButton().click()
        then:
        jobShowPage.waitForNumberOfElementsToBe(jobShowPage.jobExecutionDisabledIconBy, 0)
        jobShowPage.waitForNumberOfElementsToBe(jobShowPage.runJobBtnBy, 1)

        cleanup:
        deleteProject(projectName)
    }

}