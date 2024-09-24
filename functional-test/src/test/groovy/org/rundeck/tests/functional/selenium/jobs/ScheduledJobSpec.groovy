package org.rundeck.tests.functional.selenium.jobs

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.common.execution.ExecutionUtils
import org.rundeck.util.common.jobs.JobUtils
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.activity.ActivityPage

@SeleniumCoreTest
class ScheduledJobSpec extends SeleniumBase{

    /**
     * Schedules a job and checks if with time passing, executions reflects in activity section.
     *
     */
    def "scheduled job"(){
        given:
        def projectName = "scheduled-job-test"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        ActivityPage activityPage = page ActivityPage
        JobListPage jobListPage = page(JobListPage)
        jobListPage.loadJobListForProject(projectName)
        def jobXml = JobUtils.generateScheduledJobsXml("scheduledJob", "<time hour='*' seconds='*/5' minute='*' />")
        def job1CreatedResponse = JobUtils.createJob(projectName, jobXml, client)
        assert job1CreatedResponse.successful
        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        jobListPage.go()
        jobListPage.validatePage()
        waitFor(ExecutionUtils.Retrievers.executionsForProject(client, projectName), {it.size() >= 1}, WaitingTime.EXCESSIVE )
        activityPage.loadActivityPageForProject(projectName)
        activityPage.go()

        then:
        Integer.parseInt(activityPage.executionCount.text) > 0

        cleanup:
        deleteProject(projectName)

    }

}
