package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.activity.ActivityPage

@SeleniumCoreTest
class ScheduledJobSpec extends SeleniumBase{

    def setup() {
        go(LoginPage).login(TEST_USER, TEST_PASS)
    }

    /**
     * Schedules a job and checks if with time passing, executions reflects in activity section.
     *
     */
    def "scheduled job"(){
        given:
        def projectName = "scheduled-job-test"
        setupProject(projectName)
        ActivityPage activityPage = page ActivityPage
        JobListPage jobListPage = page(JobListPage)
        def jobXml = JobUtils.generateScheduledJobsXml("scheduledJob", "<time hour='*' seconds='*/2' minute='*' />")
        JobUtils.createJob(projectName, jobXml, client)

        when:
        jobListPage.loadJobListForProject(projectName)
        jobListPage.go()
        jobListPage.validatePage()

        activityPage.loadActivityPageForProject(projectName)
        activityPage.go()

        def refreshActivityPageAndGetExecutionCount = {
            activityPage.refresh()
            Integer.parseInt(activityPage.executionCount.text)
        }

        then:
        assert waitFor(refreshActivityPageAndGetExecutionCount, { Integer executionCount -> executionCount  > 0 }, WaitingTime.EXCESSIVE) > 0

        cleanup:
        deleteProject(projectName)
    }

    /**
     * This test creates a job, schedules it, disables the schedule and then enables it
     * It doesnt validate for the schedule to actually run
     */
    def "disable-enable job schedule"(){
        given:
        String projectName = "enableDisableJobSchedule"
        setupProject(projectName)
        String jobUuid = JobUtils.jobImportFile(projectName, '/test-files/test.xml', client).succeeded.first().id
        JobShowPage jobShowPage = page(JobShowPage, projectName).forJob(jobUuid)
        JobCreatePage jobCreatePage = page JobCreatePage
        when:
        jobShowPage.go()
        jobShowPage.getJobActionDropdownButton().click()
        jobShowPage.getEditJobLink().click()
        jobCreatePage.tab(JobTab.SCHEDULE).click()
        jobCreatePage.getScheduleEnabledFalse().click()
        jobCreatePage.getScheduleRunYesField().click()
        jobCreatePage.getUpdateJobButton().click()
        then:
        jobShowPage.els(jobShowPage.scheduleTimeBy).size() == 0
        jobShowPage.el(jobShowPage.jobInfoSectionBy).text.contains("DISABLED")
        when:
        jobShowPage.getJobActionDropdownButton().click()
        jobShowPage.getJobEnableScheduleButtonBy().click()
        jobShowPage.el(jobShowPage.jobScheduleToggleModalBy).findElement(jobShowPage.buttonDangerBy).click()
        then:
        jobShowPage.els(jobShowPage.scheduleTimeBy).size() == 1
        jobShowPage.el(jobShowPage.scheduleTimeBy).getText().contains("in")

        cleanup:
        deleteProject(projectName)
    }

}
