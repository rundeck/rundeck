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
    private static final By jobUuidFieldBy = By.xpath("//*[@data-testid='jobUuidField']//input");
    /** Suggestion rows of the PrimeVue autocomplete used by the default UI. */
    private static final By nameSuggestionBy = By.cssSelector(".p-autocomplete-option")
    /** Suggestion rows of the jQuery autocomplete still used by the legacy UI. */
    private static final By legacyNameSuggestionBy = By.cssSelector(".autocomplete-suggestions .autocomplete-suggestion")
    String childJobUuid
    String childJobName
    boolean useChooseAJobButton = false
    /** Type only a prefix of the job name and pick it from the suggestions. */
    boolean useNameAutocomplete = false


    @Override
    void configure(JobCreatePage jobCreatePage, Boolean nextUi = false) {
        if(childJobName && useChooseAJobButton){
            jobCreatePage.driver.findElement(jobChooseBtn).click()
            WebElement jobItem
            By jobSelector = By.cssSelector(".jobname.job_list_row[data-job-name='${childJobName}']")
            jobItem = new WebDriverWait(jobCreatePage.driver, Duration.ofSeconds(15)).until(
                ExpectedConditions.visibilityOfElementLocated(jobSelector)
            )
            jobItem.findElement(By.cssSelector(".glyphicon.glyphicon-book")).click()
        }

        if(childJobName && !useChooseAJobButton && useNameAutocomplete){
            selectJobFromNameSuggestions(jobCreatePage, nextUi)
        }

        if(childJobName && !useChooseAJobButton && !useNameAutocomplete){
            jobCreatePage.driver.findElement(useNameBox).click()
            jobCreatePage.waitForElementToBeClickable(jobNameFieldBy)
            WebElement jobNameField = jobCreatePage.driver.findElement(jobNameFieldBy)
            jobNameField.click()
            jobNameField.sendKeys(childJobName)
        }

        if (childJobUuid) {
            if(nextUi) {
                WebElement jobUuidField = jobCreatePage.driver.findElement(jobUuidFieldBy)
                jobUuidField.click()
                jobUuidField.sendKeys(childJobUuid)
            } else {
                jobCreatePage.waitForElementVisible(By.className("_wfiedit"))
                jobCreatePage.driver.findElement(By.className("_wfiedit")).findElement(By.name("uuid")).sendKeys(childJobUuid)
            }

        }

        Thread.sleep(WaitingTime.LOW.toMillis())
    }

    /**
     * Type a prefix of the job name and pick the matching entry from the name
     * autocomplete, which populates the name, group and uuid fields at once.
     *
     * @param jobCreatePage page under test
     * @param nextUi true for the default (Vue) UI, false for the legacy UI
     */
    void selectJobFromNameSuggestions(JobCreatePage jobCreatePage, Boolean nextUi) {
        jobCreatePage.driver.findElement(useNameBox).click()
        jobCreatePage.waitForElementToBeClickable(jobNameFieldBy)
        WebElement jobNameField = jobCreatePage.driver.findElement(jobNameFieldBy)
        jobNameField.click()
        jobNameField.sendKeys(childJobName.substring(0, Math.min(3, childJobName.length())))

        By suggestionBy = nextUi ? nameSuggestionBy : legacyNameSuggestionBy
        By exactSuggestion = By.xpath(
                nextUi
                        ? "//*[contains(concat(' ', @class, ' '), ' p-autocomplete-option ')][contains(., '${childJobName}')]"
                        : "//*[contains(concat(' ', @class, ' '), ' autocomplete-suggestion ')][contains(., '${childJobName}')]"
        )
        jobCreatePage.waitForElementVisible(suggestionBy)
        jobCreatePage.waitForElementToBeClickable(exactSuggestion)
        jobCreatePage.driver.findElement(exactSuggestion).click()
    }
}
