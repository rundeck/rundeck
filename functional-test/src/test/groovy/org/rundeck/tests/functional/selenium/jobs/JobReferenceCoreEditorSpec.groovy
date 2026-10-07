package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.ExcludePro
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.UiModes
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobReferenceStep
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.StepType
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
@ExcludePro
@UiModeFlag(featureName = "job-reference", status = UiModeStatus.PROMOTED)
class JobReferenceCoreEditorSpec extends SeleniumBase {

    static final UI_MODES = UiModes.defaultAndLegacy()

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "create a job with referenced execution node step by uuid and run it successfully"(){
        setup:
        String projectName = 'JobReferenceUUIDTest'
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/simple-job-ref.xml', client).succeeded.first().id

        when:
        def jobCreatePage = go(JobCreatePage, projectName, [legacyUi: legacyUi])
        JobShowPage jobPage = jobCreatePage
                .withName('parentJob')
                .addStep(new JobReferenceStep([
                        childJobUuid: jobUuid,
                        stepType    : StepType.NODE
                ]))
                .addDefaultTab('output')
                .saveJob()

        ExecutionShowPage executionPage = jobPage.runJob(true)

        then:
        noExceptionThrown()
        verifyAll {
            executionPage.getExecutionStatus() == 'SUCCEEDED'
            executionPage.getLogOutput().first().getText() == 'this is my jobref'
        }
        cleanup:
        deleteProject(projectName)
        where:
        [legacyUi] << UI_MODES
    }

    def "create a job with referenced execution node step by name and run it successfully"(){
        setup:
        String projectName = 'JobReferenceByNameTest'
        setupProject(projectName)

        expect:
        JobUtils.jobImportFile(projectName, '/test-files/simple-job-ref.xml', client).succeeded

        when:
        def jobCreatePage = go(JobCreatePage, projectName, [legacyUi: legacyUi])
        JobShowPage jobPage = jobCreatePage
                .withName('parentJob')
                .addStep(new JobReferenceStep([
                        childJobName   : 'simple-child-job',
                        stepType       : StepType.NODE
                ]))
                .addDefaultTab('output')
                .saveJob()

        ExecutionShowPage executionPage = jobPage.runJob(true)
        def executionId = executionPage.getCurrentExecutionId()
        then:
        noExceptionThrown()
        verifyAll {
            JobUtils.waitForExecution(
                    ExecutionStatus.SUCCEEDED.state,
                    executionId as String,
                    client)

            executionPage.waitForElementAttributeToChange executionPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
            executionPage.getLogOutput().first().getText() == 'this is my jobref'
        }
        cleanup:
        deleteProject(projectName)
        where:
        [legacyUi] << UI_MODES
    }

    def "create a job with referenced execution node step picked from the job name autocomplete and run it successfully"(){
        setup:
        String projectName = 'JobReferenceNameAutocompleteTest'
        setupProject(projectName)

        expect:
        JobUtils.jobImportFile(projectName, '/test-files/simple-job-ref.xml', client).succeeded

        when: "the child job is chosen from the name field suggestions rather than typed in full"
        def jobCreatePage = go(JobCreatePage, projectName, [legacyUi: legacyUi])
        JobShowPage jobPage = jobCreatePage
                .withName('parentJob')
                .addStep(new JobReferenceStep([
                        childJobName        : 'simple-child-job',
                        useNameAutocomplete : true,
                        stepType            : StepType.NODE
                ]))
                .addDefaultTab('output')
                .saveJob()

        ExecutionShowPage executionPage = jobPage.runJob(true)
        def executionId = executionPage.getCurrentExecutionId()

        then: "the reference resolves, proving the suggestion populated the name field"
        noExceptionThrown()
        verifyAll {
            JobUtils.waitForExecution(
                    ExecutionStatus.SUCCEEDED.state,
                    executionId as String,
                    client)

            executionPage.waitForElementAttributeToChange executionPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
            executionPage.getLogOutput().first().getText() == 'this is my jobref'
        }
        cleanup:
        deleteProject(projectName)
        where:
        [legacyUi] << UI_MODES
    }
}
