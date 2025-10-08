package org.rundeck.util.gui.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.gui.pages.project.ActivityListTrait

import java.time.Duration

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Job list page
 */
@CompileStatic
class JobListPage extends BasePage implements ActivityListTrait {

    String loadPath = "/jobs"
    By createJobLink = By.partialLinkText('New Job')
    By jobsActionsButtonBy = By.cssSelector('#project_job_actions')

    // Bulk modal elements
    By bulkPerformActionButtonBy = By.cssSelector('#bulk_perform_action_button')
    By bulkEditButtonBy = By.cssSelector('#project_job_actions_bulk_edit')
    By bulkSelectAllButtonBy = By.cssSelector('#bulk_select_all_button')
    By bulkSelectNoneButtonBy = By.cssSelector('#bulk_select_none_button')
    By bulkDeleteJobsActionBy = By.cssSelector('#bulk_delete_jobs_action')
    By bulkEnableSchedulesActionBy = By.cssSelector('#bulk_enable_schedules_action')
    By bulkDisableSchedulesActionBy = By.cssSelector('#bulk_disable_schedules_action')
    By bulkEnableExecutionActionBy = By.cssSelector('#bulk_enable_execution_action')
    By bulkDisableExecutionActionBy = By.cssSelector('#bulk_disable_execution_action')
    By bulkConfirmActionYesButtonBy = By.cssSelector('#bulk_confirm_action_yes_button')
    By bulkConfirmActionNoButtonBy = By.cssSelector('#bulk_confirm_action_no_button')

    By bulkJobRowItemsBy = By.className("job-list-row-item")
    By executeJobModalRunJobNowButtonBy = By.cssSelector('[name="_action_runJobNow"][id="execFormRunButton"]')
    Closure<By> executeJobInModalButtonBy = { By.cssSelector(".act_execute_job[data-job-id=\"$it\"]") }

    By jobsHeader = By.partialLinkText('All Jobs')
    By activitySectionLink = By.partialLinkText('Executions')
    By activityHeader = By.cssSelector('h3.card-title')
    By bodyNextUIBy = By.cssSelector('body.ui-type-next')
    By runJobButtonDisabled = By.cssSelector(".btn.btn-default.btn-xs.disabled")
    By executionPausedIcon = By.cssSelector(".glyphicon.glyphicon-pause")
    By scheduleDisabledIcon = By.cssSelector(".glyphicon.glyphicon-ban-circle")
    By jobRunLinkBy = By.cssSelector(".btn.btn-success.btn-simple.btn-hover.btn-xs.act_execute_job")
    By alertMessageBy = By.cssSelector(".alert.alert-info")
    By jobListGroupTree = By.id("job_group_tree")
    By jobListBy = By.cssSelector("jobslist")
    By alertInfoBy = By.cssSelector(".alert.alert-info")
    By jobGraphBy = By.id("jobgraphtab")
    By dashboardTabBy = By.id("joblisttab")
    static final By SAVE_FILTER_BUTTON = By.cssSelector("button[data-test-id='save-filter-button']")
    static final By SAVE_FILTER_INPUT = By.cssSelector("input.form-control[data-action='auto-focus']")
    static final By OK_BUTTON = By.xpath("/html/body/div[6]/div/div/div[3]/button[2]")
    static final By FILTER_DROPDOWN = By.cssSelector("span.dropdown-toggle.btn.btn-secondary.btn-sm.text-secondary")
    static final By SAVED_FILTER_LINK = By.cssSelector("a[data-test='filter-link']")
    static final By ACTIVITY_ROWS = By.cssSelector("table.activity-list-table tbody tr.link.activity_row")
    static final By EVENT_ICON = By.cssSelector("td.eventicon")
    static final By ANY_TIME_BUTTON = By.cssSelector("span[data-test-id='dropdown-toggle']")
    static final By IN_THE_LAST_WEEK_BUTTON = By.cssSelector("a[data-test-id='period-option']")
    static final By APPLIED_FILTER_NAME = By.cssSelector("span[data-test-id='filter-name']")

    JobListPage clickAnyTimeButton() {
        waitForElementVisible(ANY_TIME_BUTTON).click()
        return this

    }

    JobListPage clickLastWeekButton() {
        waitForElementVisible(IN_THE_LAST_WEEK_BUTTON).click()
        return this

    }
    JobListPage clickSaveFilterButton() {
        waitForElementVisible(SAVE_FILTER_BUTTON).click()
        return this
    }

    JobListPage enterFilterName(String filterName) {
        waitForElementVisible(SAVE_FILTER_INPUT).sendKeys(filterName)
        return this
    }

    JobListPage confirmFilterSave() {
        waitForElementVisible(OK_BUTTON).click()
        return this
    }

    JobListPage openFilterDropdown() {
        waitForElementVisible(FILTER_DROPDOWN).click()
        return this
    }

    JobListPage selectSavedFilter() {
        waitForElementVisible(SAVED_FILTER_LINK).click()
        return this
    }

    List<WebElement> getActivityRows() {
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfAllElementsLocatedBy(ACTIVITY_ROWS))
    }


    String getAppliedFilterName() {
        return waitForElementVisible(APPLIED_FILTER_NAME).getText()
    }
    String getFirstRowStatus() {
        return getActivityRows().get(0)
                .findElement(EVENT_ICON)
                .getAttribute("title")
    }
    JobListPage(final SeleniumContext context) {
        super(context)
    }

    JobListPage(final SeleniumContext context, String project) {
        super(context)
        loadJobListForProject(project)
    }

    void loadJobListForProject(String projectName){
        this.loadPath = "/project/${projectName}/jobs"
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
        new WebDriverWait(context.driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.numberOfElementsToBe(executionPausedIcon, 1))
    }

    /**
     * It validates this by looking for the run job button to be disabled
     */
    def expectScheduleDisabled(){
        new WebDriverWait(context.driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.numberOfElementsToBe(runJobButtonDisabled, 0))
        new WebDriverWait(context.driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.numberOfElementsToBe(executionPausedIcon, 1))
        new WebDriverWait(context.driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.numberOfElementsToBe(scheduleDisabledIcon, 1))
    }

    void loadPathToNextUI(String projectName) {
        loadPath = "/project/${projectName}/jobs?nextUi=true"
    }

    WebElement getCreateJobLink(){
        el createJobLink
    }

    WebElement getJobsActionsButton(){
        waitPresent(jobsActionsButtonBy)
    }

    private WebElement waitPresent(By selector, Integer seconds = 5) {
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

    def getCountJobList(){
        (els jobRunLinkBy).size()
    }

    def getJobLink(String jobName){
        (el By.linkText(jobName))
    }

    def getDeleteAlertMessage() {
        (el alertMessageBy)

    }

    WebElement getExpandedJobGroupsContainer(){
        (el jobListGroupTree).findElement(By.cssSelector(".expandComponentHolder.expanded"))
    }

    List<WebElement> getExpandedJobGroupsContainerChildren(){
        (el jobListGroupTree).findElements(By.cssSelector(".expandComponentHolder.expanded"))
    }

    List<WebElement> getJobList(){
        els jobListBy
    }

    WebElement getAlertInfo(){
        el alertInfoBy
    }

    WebElement getBulkEditButton(){
        waitPresent(bulkEditButtonBy)
    }

    WebElement getBulkPerformActionButton(){
        waitPresent(bulkPerformActionButtonBy)
    }

    WebElement getBulkPerformActionButtonOnceClickable(){
        byAndWaitClickable(bulkPerformActionButtonBy)
    }

    WebElement getBulkSelectAllButton() {
        waitPresent(bulkSelectAllButtonBy)
    }

    WebElement getBulkSelectNoneButton() {
        waitPresent(bulkSelectNoneButtonBy)
    }

    WebElement getBulkDeleteJobsAction() {
        waitPresent(bulkDeleteJobsActionBy)
    }

    WebElement getBulkEnableSchedulesAction() {
        waitPresent(bulkEnableSchedulesActionBy)
    }

    WebElement getBulkDisableSchedulesAction() {
        waitPresent(bulkDisableSchedulesActionBy)
    }

    WebElement getBulkDisableExecutionAction() {
        waitPresent(bulkDisableExecutionActionBy)
    }

    WebElement getBulkEnableExecutionAction() {
        waitPresent(bulkEnableExecutionActionBy)
    }

    WebElement getBulkConfirmActionYesButton() {
        waitPresent(bulkConfirmActionYesButtonBy)
    }

    WebElement getBulkConfirmActionNoButton() {
        waitPresent(bulkConfirmActionNoButtonBy)
    }

    WebElement getExecuteJobInModalButton(String jobId) {
        waitPresent(executeJobInModalButtonBy(jobId))
    }

    WebElement getExecuteJobModalRunJobNowButton() {
        waitPresent(executeJobModalRunJobNowButtonBy)
    }

    String loopGroups(List<WebElement> groups) {
        List<String> items = new ArrayList<>()
        for (WebElement group : groups) {
            if (group.findElements(By.cssSelector(".glyphicon.glyphicon-chevron-down")).size() > 0) {

                String groupName = (String) ((JavascriptExecutor) driver).executeScript(
                        "return arguments[0].nextSibling.textContent.trim();",
                        group.findElement(By.cssSelector(".glyphicon.glyphicon-chevron-down"))
                )
                items.add(groupName)

                String childText = group.findElement(By.cssSelector("a.hover_show_job_info.link-quiet")).getText()
                items.add(childText)
            }
        }
        return items.stream().collect(Collectors.joining('/'))
    }

}
