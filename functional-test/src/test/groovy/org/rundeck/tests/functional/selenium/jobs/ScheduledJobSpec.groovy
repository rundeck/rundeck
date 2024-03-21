package org.rundeck.tests.functional.selenium.jobs

import org.openqa.selenium.Keys
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.home.HomePage
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.JobShowPage
import org.rundeck.util.gui.pages.jobs.JobTab
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.project.ActivityPage

@SeleniumCoreTest
class ScheduledJobSpec extends SeleniumBase{

    def "Scheduled job"(){
        given:
        def projectName = "scheduled-job-test"
        setupProject(projectName)
        LoginPage loginPage = page LoginPage
        HomePage homePage = page HomePage
        JobCreatePage jobCreatePage = page JobCreatePage
        JobShowPage jobShowPage = page JobShowPage
        ActivityPage activityPage = page ActivityPage

        when:
        loginPage.go()
        loginPage.login(TEST_USER, TEST_PASS)
        homePage.validatePage()
        go JobCreatePage, projectName
        jobCreatePage.fillBasicJob 'Run job later'
        jobCreatePage.addSimpleCommandStepButton.click()
        jobCreatePage.addSimpleCommandStep 'echo asd', 1
        jobCreatePage.tab(JobTab.SCHEDULE).click()
        jobCreatePage.scheduleRunYesField.click()
        jobCreatePage.schedulesCrontab.click()
        jobCreatePage.schedulesCrontabPanel.click()
        jobCreatePage.waitForElementVisible(jobCreatePage.schedulesCrontabStringBy)
        jobCreatePage.schedulesCrontabStringInput.click()
        jobCreatePage.schedulesCrontabStringInput.sendKeys(Keys.chord(Keys.CONTROL, "a"))
        jobCreatePage.schedulesCrontabStringInput.sendKeys(Keys.BACK_SPACE)
        jobCreatePage.schedulesCrontabStringInput.sendKeys("*/5 * * ? * * *")
        jobCreatePage.createJobButton.click()
        jobCreatePage.createJobButton.click()
        jobShowPage.waitForElementVisible(jobShowPage.jobUuidBy)
        jobShowPage.validatePage()
        activityPage.loadActivityPageForProject(projectName)
        activityPage.go()
        Thread.sleep(WaitingTime.MODERATE.milliSeconds) // Auto refresh doesn't work
        activityPage.go()

        then:
        Integer.parseInt(activityPage.executionCount.text) > 0

        cleanup:
        deleteProject(projectName)

    }

}
