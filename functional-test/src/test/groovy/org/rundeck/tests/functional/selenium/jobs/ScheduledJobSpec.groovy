package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.By
import org.openqa.selenium.Keys
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobListPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.activity.ActivityPage

@SeleniumCoreTest
class ScheduledJobSpec extends SeleniumBase{

    /**
     * Schedules a job and checks if with time passing, executions reflects in activity section.
     *
     */
    def "Scheduled job"(){
        given:
        def projectName = "scheduled-job-test"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        ActivityPage activityPage = page ActivityPage
        JobCreatePage jobCreatePage = page JobCreatePage
        JobListPage jobListPage = page(JobListPage)
        jobListPage.loadJobListForProject(projectName)
        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        jobListPage.go()
        jobListPage.validatePage()
        jobListPage.getCreateJobLink().click()
        jobCreatePage.validatePage()
        jobCreatePage.withName("scheduledJob")
        jobCreatePage.tab(JobTab.WORKFLOW).click()
        jobCreatePage.waitForElementToBeClickable(jobCreatePage.stepFilterInput)
        jobCreatePage.stepFilterInput.sendKeys("command")
        jobCreatePage.stepFilterSearchButton.click()
        jobCreatePage.addSimpleCommandStep("echo 'asd'", 0)
        jobCreatePage.tab(JobTab.SCHEDULE).click()
        jobCreatePage.getScheduleRunYesField().click()
        jobCreatePage.getSchedulesCrontab().click()
        jobCreatePage.getSchedulesCrontabStringInput().sendKeys(Keys.chord(Keys.CONTROL, "a"))
        jobCreatePage.getSchedulesCrontabStringInput().sendKeys(Keys.BACK_SPACE)
        jobCreatePage.driver.findElement(By.name("crontabString"))
        jobCreatePage.getSchedulesCrontabStringInput().sendKeys("*/5 * * ? * * *")
        jobCreatePage.clickTimeZone() //This is to lose focus in crontab string
        jobCreatePage.getCreateJobButton().click()
        jobCreatePage.waitForUrlToContain('/job/show')
        activityPage.loadActivityPageForProject(projectName)
        activityPage.go()
        hold(10)
        activityPage.go()

        then:
        Integer.parseInt(activityPage.executionCount.text) > 0

        cleanup:
        deleteProject(projectName)

    }

}
