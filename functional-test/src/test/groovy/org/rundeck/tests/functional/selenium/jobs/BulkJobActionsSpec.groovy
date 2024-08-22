package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage

@SeleniumCoreTest
class BulkJobActionsSpec extends SeleniumBase {

    /**
     *  A cron schedule that will not execute for a long time, but is valid and can be enabled
     *  in a job
     */
    final static String CRON_SCHEDULE_THAT_WONT_EXECUTE = "*/4 * * ? * * 2099"

    String jobId1
    String jobId2

    def setupSpec() {
        setupProject(SELENIUM_BASIC_PROJECT)
    }

    def setup() {
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def cleanup() {
        if (jobId1) {
            JobUtils.deleteJob(jobId1, client)
        }
        if (jobId2) {
            JobUtils.deleteJob(jobId2, client)
        }
    }

    def "bulk edit screen is shown"() {
        given:
        JobListPage jobsListPage = page JobListPage
        jobsListPage.loadPathToNextUI SELENIUM_BASIC_PROJECT
        jobsListPage.go()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.jobsActionsButton).click()
        def bulkEdit = jobsListPage.bulkEditButton

        when:
        jobsListPage.waitIgnoringForElementToBeClickable(bulkEdit).click()

        then:
        def bulkPerformActionButton = jobsListPage.getBulkPerformActionButton()
        bulkPerformActionButton.isDisplayed()
        !bulkPerformActionButton.isEnabled()
    }

    def "bulk enable executions action enables execution"() {
        given:
        jobId1 = generateJob(["execution-enabled": "false"])
        jobId2 = generateJob(["execution-enabled": "false"])

        JobListPage jobsListPage = page JobListPage
        jobsListPage.loadPathToNextUI SELENIUM_BASIC_PROJECT
        jobsListPage.go()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.jobsActionsButton).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkEditButton).click()

        when:
        jobsListPage.waitForNumberOfElementsToBeMoreThan(jobsListPage.bulkJobRowItemsBy, 1)
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkSelectAllButton).click()

        then:
        def bulkPerformActionButton = jobsListPage.getBulkPerformActionButtonOnceClickable()
        jobsListPage.waitIgnoringForElementToBeClickable(bulkPerformActionButton).click()

        when:
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkEnableExecutionAction).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkConfirmActionYesButton).click()

        then:
        jobRespondsTrueTo(jobId1) { it.executionEnabled }
    }

    def "bulk disable executions action disables execution"() {
        given:
        jobId1 = generateJob()
        jobId2 = generateJob()

        JobListPage jobsListPage = page JobListPage
        jobsListPage.loadPathToNextUI SELENIUM_BASIC_PROJECT
        jobsListPage.go()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.jobsActionsButton).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkEditButton).click()

        when:
        jobsListPage.waitForNumberOfElementsToBeMoreThan(jobsListPage.bulkJobRowItemsBy, 1)
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkSelectAllButton).click()

        then:
        def bulkPerformActionButton = jobsListPage.getBulkPerformActionButtonOnceClickable()
        jobsListPage.waitIgnoringForElementToBeClickable(bulkPerformActionButton).click()

        when:
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkDisableExecutionAction).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkConfirmActionYesButton).click()

        then:
        jobRespondsTrueTo(jobId1) { !it.executionEnabled }
    }

    def "bulk enable schedule action enables the schedule"() {
        given:
        jobId1 = generateJob(["schedule-enabled": "false", "schedule-crontab": CRON_SCHEDULE_THAT_WONT_EXECUTE], "api-test-executions-running-scheduled.xml")
        jobId2 = generateJob(["schedule-enabled": "false", "schedule-crontab": CRON_SCHEDULE_THAT_WONT_EXECUTE], "api-test-executions-running-scheduled.xml")

        JobListPage jobsListPage = page JobListPage
        jobsListPage.loadPathToNextUI SELENIUM_BASIC_PROJECT
        jobsListPage.go()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.jobsActionsButton).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkEditButton).click()

        when:
        jobsListPage.waitForNumberOfElementsToBeMoreThan(jobsListPage.bulkJobRowItemsBy, 1)
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkSelectAllButton).click()

        then:
        def bulkPerformActionButton = jobsListPage.getBulkPerformActionButtonOnceClickable()
        jobsListPage.waitIgnoringForElementToBeClickable(bulkPerformActionButton).click()

        when:
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkEnableSchedulesAction).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkConfirmActionYesButton).click()

        then:
        jobRespondsTrueTo(jobId1.toString()) { it.scheduleEnabled }
    }

    def "bulk disable schedule action disables the schedule"() {
        given:
        jobId1 = generateJob(["schedule-enabled": "true", "schedule-crontab": CRON_SCHEDULE_THAT_WONT_EXECUTE], "api-test-executions-running-scheduled.xml")
        jobId2 = generateJob(["schedule-enabled": "true", "schedule-crontab": CRON_SCHEDULE_THAT_WONT_EXECUTE], "api-test-executions-running-scheduled.xml")

        JobListPage jobsListPage = page JobListPage
        jobsListPage.loadPathToNextUI SELENIUM_BASIC_PROJECT
        jobsListPage.go()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.jobsActionsButton).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkEditButton).click()

        when:
        jobsListPage.waitForNumberOfElementsToBeMoreThan(jobsListPage.bulkJobRowItemsBy, 1)
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkSelectAllButton).click()

        then:
        def bulkPerformActionButton = jobsListPage.getBulkPerformActionButtonOnceClickable()
        jobsListPage.waitIgnoringForElementToBeClickable(bulkPerformActionButton).click()

        when:
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkDisableSchedulesAction).click()
        jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.bulkConfirmActionYesButton).click()

        then:
        jobRespondsTrueTo(jobId1.toString()) { !it.scheduleEnabled }
    }

    def "job can be executed from the Job List Page modal"() {
        given:
        String jobToExecute = generateJob(["opt1-required": "false", "opt2-required": "false"])

        JobListPage jobsListPage = page JobListPage

        // TODO: Determine why the run button fails to open a modal in NextUI
        //        jobsListPage.loadPathToNextUI SELENIUM_BASIC_PROJECT

        jobsListPage.loadJobListForProject SELENIUM_BASIC_PROJECT
        jobsListPage.go()

        when:
        def job1RunButton = jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.getExecuteJobInModalButton(jobToExecute))
        job1RunButton.click()

        def runJobNowButton = jobsListPage.waitIgnoringForElementToBeClickable(jobsListPage.getExecuteJobModalRunJobNowButton())
        runJobNowButton.click()
        jobsListPage.waitForUrlToContain('/execution/show/')

        then:
        ExecutionShowPage executionShowPage = page ExecutionShowPage
        executionShowPage.validatePage()
    }

    /**
     * Should be in JobUtils, but it's going through a refactor, thus it's here for now
     * @param jobId
     * @param c
     * @param approximateWaitInSeconds
     * @return
     */
    private boolean jobRespondsTrueTo(String jobId, Closure<Boolean> c, int approximateWaitInSeconds = 5) {
        def job = JobUtils.getJobDetailsById(jobId, MAPPER, client)
        def result = c(job)

        for (int i = 0; !result && i < approximateWaitInSeconds; i++) {
            sleep(1000)
            job = JobUtils.getJobDetailsById(jobId, MAPPER, client)
            result = c(job)
        }

        return result
    }

    private def generateJob(Map argsOverrides = [:], String jobFile = "job-template-common.xml") {
        argsOverrides = ["job-group-name": ""] + argsOverrides
        def path = JobUtils.updateJobFileToImport(jobFile, SELENIUM_BASIC_PROJECT, argsOverrides)
        JobUtils.jobImportFile(SELENIUM_BASIC_PROJECT, path, client).succeeded[0].id
    }
}
