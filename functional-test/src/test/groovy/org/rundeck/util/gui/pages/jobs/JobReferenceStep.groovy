package org.rundeck.util.gui.pages.jobs

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.common.WaitingTime

import java.time.Duration

class JobReferenceStep implements JobStep {
    final String STEP_NAME = 'job'
    StepType stepType

    private static final By useNameBox = By.id("useNameTrue")
    private static final By jobChooseBtn = By.xpath("//*[starts-with(@id, 'jobChooseBtn')]")
    private static final By jobNameFieldBy = By.xpath("//*[starts-with(@id, 'jobNameField')]")
    String childJobUuid
    String childJobName
    boolean useChooseAJobButton = false


    @Override
    void configure(JobCreatePage jobCreatePage) {
        if(childJobName && useChooseAJobButton){
            jobCreatePage.driver.findElement(jobChooseBtn).click()
            WebElement jobItem = new WebDriverWait(jobCreatePage.driver, Duration.ofSeconds(5)).until(
                ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".jobname.job_list_row[data-job-name='${childJobName}']"))
            )

            jobItem.findElement(By.cssSelector(".glyphicon.glyphicon-book")).click()
        }

        if(childJobName && !useChooseAJobButton){
            jobCreatePage.driver.findElement(useNameBox).click()
            jobCreatePage.waitForElementToBeClickable(jobNameFieldBy)
            WebElement jobNameField = jobCreatePage.driver.findElement(jobNameFieldBy)
            jobNameField.click()
            jobNameField.sendKeys(childJobName)
        }

        if (childJobUuid) {
            jobCreatePage.waitForElementVisible(By.className("_wfiedit"))
            jobCreatePage.driver.findElement(By.className("_wfiedit")).findElement(By.name("uuid")).sendKeys(childJobUuid)
        }

        Thread.sleep(WaitingTime.LOW.duration.toMillis())
    }
}
