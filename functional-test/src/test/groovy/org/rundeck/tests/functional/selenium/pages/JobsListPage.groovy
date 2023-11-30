package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

class JobsListPage extends BasePage{

    static final String PAGE_PATH = "/jobs"
    By createJobLink = By.partialLinkText('New Job')
    By bulkEditSection = By.cssSelector('#indexMain .bulk_edit_controls')
    By jobsActionsButton = By.cssSelector('#project_job_actions')
    By jobsHeader = By.partialLinkText('All Jobs')
    By activitySectionLink = By.partialLinkText('Executions')
    By activityHeader = By.cssSelector('h3.card-title')
    String project
    String getLoadPath() {
        if(!project){
            throw new IllegalStateException("project is not set, cannot load Jobs List page")
        }
        return "/project/${project}${PAGE_PATH}"
    }

    JobsListPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(PAGE_PATH)) {
            throw new IllegalStateException("Not on jobs list page: " + driver.currentUrl)
        }
    }

    WebElement getCreateJobLink(){
        el createJobLink
    }
    WebElement getBulkEditSection(){
        el bulkEditSection
    }
    WebElement getJobsActionsButton(){
        waitPresent(jobsActionsButton, 5)
//        waitInteractive(jobsActionsButton, 5)
    }

    private WebElement waitPresent(By selector, Integer seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(
            ExpectedConditions.presenceOfElementLocated(selector)
        )
    }
    private WebElement waitInteractive(By selector, Integer seconds) {
        new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(
            ExpectedConditions.elementToBeClickable(selector)
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
}
