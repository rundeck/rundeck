package org.rundeck.util.gui.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

/**
 * Job list page
 */
@CompileStatic
class JobListPage extends BasePage {

    String loadPath = "/jobs"
    By createJobLink = By.partialLinkText('New Job')
    By jobsActionsButton = By.cssSelector('#project_job_actions')
    By jobsHeader = By.partialLinkText('All Jobs')
    By activitySectionLink = By.partialLinkText('Executions')
    By activityHeader = By.cssSelector('h3.card-title')
    By bodyNextUIBy = By.cssSelector('body.ui-type-next')
    By runJobButtonDisabled = By.cssSelector(".btn.btn-default.btn-xs.disabled")

    JobListPage(final SeleniumContext context) {
        super(context)
    }

    void loadPathToShowJob(String projectName, String jobId) {
        loadPath = "/project/${projectName}/job/show/${jobId}"
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on job list page: " + driver.currentUrl)
        }
    }

    /**
     * It validates this by looking for the run job button to be disabled
     */
    def expectExecutionsDisabled(){
        new WebDriverWait(context.driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.numberOfElementsToBeMoreThan(runJobButtonDisabled, 0))
    }

    void loadPathToNextUI(String projectName) {
        loadPath = "/project/${projectName}/jobs?nextUi=true"
    }

    WebElement getCreateJobLink(){
        el createJobLink
    }

    WebElement getJobsActionsButton(){
        waitPresent(jobsActionsButton, 5)
    }

    private WebElement waitPresent(By selector, Integer seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(
                ExpectedConditions.presenceOfElementLocated(selector)
        )
    }

    WebElement getJobsHeader(){
        el jobsHeader
    }
    WebElement getActivitySectionLink(){
        el activitySectionLink
    }
    WebElement getActivityHeader(){
        el activityHeader
    }
    WebElement getLink(String text){
        el By.partialLinkText(text)
    }

    WebElement getBodyNextUI(){
        el bodyNextUIBy
    }

}
