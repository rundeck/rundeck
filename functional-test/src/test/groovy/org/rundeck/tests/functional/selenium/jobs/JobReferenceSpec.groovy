package org.rundeck.tests.functional.selenium.jobs

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionStatus
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobReferenceStep
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.StepType
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class JobReferenceSpec extends SeleniumBase {

    private static String PROJECT_LOCATION = '/projects-import/jobref'

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    def "import options from parent"(){
        setup:
        String projectName = 'ImportOptionsTest'
        setupProjectArchiveDirectoryResource(projectName, "${PROJECT_LOCATION}/${projectName}.rdproject")
        JobShowPage jobPage = page(JobShowPage, projectName).forJob('ffc04705-13c1-47b7-840e-3f23f0dd4f86')
        jobPage.go()

        when:
        ExecutionShowPage executionPage = jobPage.runJob(true)

        then:
        noExceptionThrown()
        executionPage.getLogOutput().first().getText() == 'Hello'
        cleanup:
        deleteProject(projectName)
    }

    def "pass parent option as an argument to child"(){
        setup:
        String projectName = 'JobReferenceCreationUsingArgTest'
        setupProjectArchiveDirectoryResource(projectName, "${PROJECT_LOCATION}/${projectName}.rdproject")
        JobShowPage jobPage = page(JobShowPage, projectName).forJob('54dc738b-02aa-4d56-8b6b-45f93f47a0d6')
        jobPage.go()

        when:
        ExecutionShowPage executionPage = jobPage.runJob(true)

        then:
        noExceptionThrown()
        executionPage.getLogOutput().first().getText() == 'parent option default value'
        cleanup:
        deleteProject(projectName)
    }

    def "create a job with referenced execution node step by uuid and run it successfully"(){
        setup:
        String projectName = 'JobReferenceUUIDTest'
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/simple-job-ref.xml', client).succeeded.first().id

        when:
        JobShowPage jobPage = go(JobCreatePage, projectName)
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
    }

    def "create a job with referenced execution node step by name and run it successfully"(){
        setup:
        String projectName = 'JobReferenceByNameTest'
        setupProject(projectName)

        expect:
        JobUtils.jobImportFile(projectName, '/test-files/simple-job-ref.xml', client).succeeded

        when:
        JobShowPage jobPage = go(JobCreatePage, projectName)
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
            JobUtils.waitForExecutionToBe(
                    ExecutionStatus.SUCCEEDED.state,
                    executionId as String,
                    new ObjectMapper(),
                    client,
                    WaitingTime.MODERATE,
                    WaitingTime.MODERATE
            )
            executionPage.waitForElementAttributeToChange executionPage.executionStateDisplayLabel, 'data-execstate', 'SUCCEEDED'
            executionPage.getLogOutput().first().getText() == 'this is my jobref'
        }
        cleanup:
        deleteProject(projectName)
    }

    def "create a job with referenced execution workflow step by using 'choose a job' button and run it successfully"() {
        setup:
        String projectName = 'JobReferenceByNameTest'
        setupProject(projectName)

        expect:
        JobUtils.jobImportFile(projectName, '/test-files/simple-job-ref.xml', client).succeeded

        when:
        JobShowPage jobPage = go(JobCreatePage, projectName)
                .withName('parentJob')
                .addStep(new JobReferenceStep([
                        childJobName       : 'simple-child-job',
                        stepType           : StepType.WORKFLOW,
                        useChooseAJobButton: true
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
    }

    def "override node filters thread count"(){
        setup:
        String projectName = 'JobRefThreadCountTest'
        setupProjectArchiveDirectoryResource(projectName, "${PROJECT_LOCATION}/${projectName}.rdproject")
        JobShowPage jobPage = page(JobShowPage, projectName).forJob('9b559a6a-4578-4a8c-9c55-3e7ab2056341')
        jobPage.go()

        when:
        ExecutionShowPage executionPage = jobPage.runJob(true)

        then:
        noExceptionThrown()
        executionPage.getLogOutput().first().getText() == '1'
        cleanup:
        deleteProject(projectName)
    }
}
