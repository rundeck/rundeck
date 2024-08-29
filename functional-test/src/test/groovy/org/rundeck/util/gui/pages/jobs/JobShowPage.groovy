package org.rundeck.util.gui.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.gui.pages.execution.ExecutionShowPage
import org.rundeck.util.gui.scm.ScmStatusBadge

import java.time.Duration

/**
 * Job show page
 */
@CompileStatic
class JobShowPage extends BasePage{

    By jobUuidBy = By.xpath("//*[@class='rd-copybox__content']")
    By stepsInJobDefinitionBy = By.cssSelector(".pflowitem.wfctrlholder")
    By jobDefinitionModalBy = By.cssSelector('a[href="#job-definition-modal"]')
    By notificationDefinitionBy = By.cssSelector('#detailtable.tab-pane > div.row > div.col-sm-12.table-responsive > table.table.item_details> tbody > tr > td.container > div.row > div.col-sm-12 > div.overflowx')
    By closeJobDefinitionModalBy = By.xpath("//*[contains(@id,'job-definition-modal_footer')]//*[@type='submit']")
    By jobInfoGroupBy = By.cssSelector('div.jobInfoSection a.text-secondary')
    By descriptionText = By
            .xpath("//*[@class=\"section-space\"]//*[@class=\"h5 text-strong\"]")
    By cronBy = By.xpath("//*[@class='cronselected']")
    By scheduleTimeBy = By.xpath("//*[@class='scheduletime']")
    By multipleExecBy = By.xpath("//*[@id=\"detailtable\"]//td[text()='Multiple Executions?']")
    By multipleExecYesBy = By.xpath("//*[@id=\"detailtable\"]//td[contains(text(),'Yes')]")
    By workflowDetailBy = By.xpath("//*[@id='workflowstrategydetail']//*[@class='col-sm-12']")
    By nodeFilterSectionMatchedNodesBy = By.cssSelector("#detailtable.tab-pane  tr#exec_detail_nodes  .exec_detail__matchednodes")
    By threadCountBy = By.cssSelector("div[class\$='threadcount'] span[class*='text-strong']")
    By nodeKeepGoingBy = By.cssSelector("div[class='exec_detail__nodeKeepgoing'] span[class^='text-strong']")
    By nodeRankOrderAscendingBy = By.cssSelector("div[class\$='nodeRankOrderAscending'] span[class^='text-strong']")
    By nodeSelectedByDefaultBy = By.cssSelector("div[class\$='nodeSelectedByDefault'] span[class^='text-strong']")
    By orchestratorNameBy = By.xpath("//details[contains(@id, 'exec_detail__orchestrator')]")
    By closeDefinitionModalBy = By.cssSelector("div[id='job-definition-modal_footer'] button[data-dismiss='modal']")
    By jobActionBy = By.xpath("//div[contains(@class, 'job-action-button')]")
    By jobActionEditBy = By.xpath("//a[@title='Edit this Job']")
    By nodeFilterInputBy = By.cssSelector("#doReplaceFilters")
    By nodeFilterOverrideBy = By.cssSelector("#filterradio")
    By schedJobNodeFilterBy = By.cssSelector("div[class='input-group nodefilters multiple-control-input-group']")
    By jobLinkTitleBy = By.xpath("//a[contains(@class, 'job-header-link')]")
    By autocompleteJobStepDefinitionBy = By.cssSelector("#wfitem_0 > span > div > div > span > span > span.text-success")
    By runFormBy = By.cssSelector("#execDiv #exec_options_form #formbuttons #execFormRunButton")
    By optionValidationWarningBy = By.cssSelector("#execDiv #exec_options_form #optionSelect #_commandOptions div.form-group.has-warning p.text-warning")
    By jobRowBy = By.cssSelector("#job_group_tree .jobname.job_list_row[data-job-id] > a[data-job-id]")
    By jobSearchBy = By.xpath("//span[@title='Click to modify filter']")
    By jobSearchNameBy = By.cssSelector('#jobs_filters form input[name="jobFilter"]')
    By jobSearchGroupBy = By.cssSelector('#jobs_filters form input[name="groupPath"]')
    By jobSearchSubmitBy = By.cssSelector('#jobs_filters form #jobs_filters_footer input[type="submit"][name="_action_jobs"]')
    By runJobBtnBy = By.id("execFormRunButton")
    By logOutputBtn = By.id('btn_view_output')
    By jobActionsListButtonBy = By.linkText("Action")
    By jobDeleteButtonBy = By.linkText("Delete this Job")
    By jobDeleteConfirmBy = By.xpath("//*[@value=\"Delete\"]")
    By jobDeleteModalBy = By.id("jobdelete")
    By runJobLaterBy = By.linkText("Run Job Later...")
    By runJobLaterMinuteArrowUpBy = By.cssSelector("td:nth-child(3) .glyphicon-chevron-up")
    By runJobLaterScheduleCreateButtonBy = By.id("scheduler_buttons")
    By jobStatusBarBy = By.className("job-stats-value")
    By jobOptionValuesBy = By.cssSelector(".optionvalues")
    By jobOptionValueInputBy = By.cssSelector(".optionvalues > option:nth-child(6)")
    By jobDisableScheduleButtonBy = By.linkText("Disable Schedule")
    By jobEnableScheduleButtonBy = By.linkText("Enable Schedule")
    By jobInfoSectionBy = By.id("jobInfo_")
    By jobDisableScheduleModalButtonBy = By.cssSelector("[value='Disable Schedule']")
    By jobExecutionDisabledIconBy = By.cssSelector(".glyphicon.glyphicon-ban-circle")
    By jobOptionsDropdownBy = By.cssSelector(".optionvalues")

    static class NextUi {
        static By descriptionText = By
                .xpath("//*[@class=\"markdown-body\"]")
    }

    static final String PAGE_PATH = "/job/show"
    String loadPath = "/job/show"
    private String project
    boolean nextUi = false

    JobShowPage(final SeleniumContext context) {
        super(context)
    }

    JobShowPage(final SeleniumContext context, String project) {
        super(context)
        this.project = project
        this.loadPath = "/project/$project/jobs"
    }

    JobShowPage forJob(String jobUuid){
        this.loadPath = "/project/$project/job/show/$jobUuid"
        return this
    }

    ExecutionShowPage runJob(boolean waitForFinalState = false){
        getRunJobBtn().click()

        ExecutionShowPage execution = new ExecutionShowPage(context)
        if(waitForFinalState) {
            execution.waitForRunAgainLink()
            execution.waitForFinalState()
        }

        return execution
    }

    ScmStatusBadge getScmStatusBadge(){
        return new ScmStatusBadge(this)
    }

    List<WebElement> getOptionsFields(){
        driver.findElements(By.xpath("//input[contains(@name, 'extra.option.')]"))
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on job show selected page: " + driver.currentUrl)
        }
    }

    WebElement getJobDefinitionModal(){
        el jobDefinitionModalBy
    }

    WebElement getNotificationDefinition(){
        el notificationDefinitionBy
    }

    WebElement getCloseJobDefinitionModalButton() {
        el closeJobDefinitionModalBy
    }

    WebElement getJobInfoGroupLabel() {
        waitForElementVisible jobInfoGroupBy
        el jobInfoGroupBy
    }

    WebElement getDescriptionTextLabel() {
        el nextUi ? NextUi.descriptionText : descriptionText
    }

    List<WebElement> getCronLabel() {
        els cronBy
    }

    WebElement getScheduleTimeLabel() {
        el scheduleTimeBy
    }

    WebElement getMultipleExecField() {
        el multipleExecBy
    }

    WebElement getMultipleExecYesField() {
        el multipleExecYesBy
    }

    WebElement getWorkflowDetailField() {
        el workflowDetailBy
    }

    WebElement getNodeFilterSectionMatchedNodesLabel() {
        el nodeFilterSectionMatchedNodesBy
    }

    WebElement getThreadCountLabel() {
        el threadCountBy
    }

    WebElement getNodeKeepGoingLabel() {
        el nodeKeepGoingBy
    }

    WebElement getNodeRankOrderAscendingLabel() {
        el nodeRankOrderAscendingBy
    }

    WebElement getNodeSelectedByDefaultLabel() {
        el nodeSelectedByDefaultBy
    }

    WebElement getOrchestratorNameLabel() {
        el orchestratorNameBy
    }

    WebElement getCloseDefinitionModalButton() {
        el closeDefinitionModalBy
    }

    WebElement getJobActionDropdownButton() {
        el jobActionBy
    }

    WebElement getEditJobLink() {
        el jobActionEditBy
    }

    WebElement getJobLinkTitleLabel() {
        el jobLinkTitleBy
    }

    WebElement getAutocompleteJobStepDefinitionLabel() {
        el autocompleteJobStepDefinitionBy
    }

    WebElement optionInputText(String name) {
        el By.cssSelector("#optionSelect #_commandOptions input[type=text][name='extra.option.${name}']")
    }

    WebElement runJobLink(String uuid) {
        el By.cssSelector("#job_group_tree a.act_execute_job[data-job-id='${uuid}']")
    }

    WebElement getRunFormButton() {
        el runFormBy
    }

    WebElement getOptionValidationWarningText() {
        el optionValidationWarningBy
    }

    WebElement getNodeFilterInput() {
        el nodeFilterInputBy
    }

    WebElement getNodeFilterOverride() {
        el nodeFilterOverrideBy
    }

    WebElement getSchedJobNodeFilter() {
        el schedJobNodeFilterBy
    }

    List<WebElement> getJobRowLink() {
        els jobRowBy
    }

    WebElement getJobSearchButton() {
        el jobSearchBy
    }

    WebElement getJobSearchNameField() {
        el jobSearchNameBy
    }

    WebElement getJobSearchGroupField() {
        el jobSearchGroupBy
    }

    WebElement getJobSearchSubmitButton() {
        el jobSearchSubmitBy
    }

    WebElement getRunJobBtn(){
        el runJobBtnBy
    }

    WebElement getLogOutputBtn(){
        el logOutputBtn
    }

    WebElement getJobOptionsValuesDropdown(){
        el jobOptionValuesBy
    }

    WebElement getJobOptionValueListItem(String name){
        waitForNumberOfElementsToBeOne(By.xpath("//option[. = '${name}']"))
        driver.findElement(By.xpath("//option[. = '${name}']"))
    }

    WebElement getJobOptionValueInput(){
        el jobOptionValueInputBy
    }

    WebElement getJobOptionsDropdown(){
        el jobOptionsDropdownBy
    }

    /**
     * Waits for the log output to have more than `qtty` log entries containing logLineText
     * @param logLineText text to match log entries
     * @param minimum number of log entries that should contain logLineText
     * @param timeout to be waiting for results
     */
    void waitForLogOutput (String logLineText, Integer minimum = 0, Integer timeout = 10) {
        By logLineSelector = By.xpath("//span[contains(text(),'${logLineText}')]")
        new WebDriverWait(driver, Duration.ofSeconds(timeout)).until(ExpectedConditions.numberOfElementsToBeMoreThan(logLineSelector, minimum))
    }

    WebElement getExecutionOptionsDropdown(){
        driver.findElement(By.id("execOptFormRunButtons")).findElement(By.className("btn-secondary"))
    }

    WebElement getRunJobLaterOption() {
        el runJobLaterBy
    }

    WebElement getRunJobLaterMinuteArrowUp(){
        el runJobLaterMinuteArrowUpBy
    }

    WebElement getRunJobLaterCreateScheduleButton(){
        el runJobLaterScheduleCreateButtonBy
    }

    WebElement getJobStatusBar(){
        el jobStatusBarBy
    }

    void selectOptionFromOptionListByName(String optionListName,int optionNo){
        def select = new Select(getOptionSelectByName(optionListName))
        select.selectByValue("option${optionNo}")
    }

    WebElement getOptionSelectByName(String name){
        driver.findElement(By.name("extra.option.${name}"))
    }

    List<WebElement> getOptionSelectChildren(String name){
        final By optionSelector = By.name("extra.option.${name}")

        waitForElementVisible(optionSelector)
        driver.findElements(optionSelector)
    }

    void waitForLogOutput (By logOutput, Integer number, Integer seconds){
        new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.numberOfElementsToBeMoreThan(logOutput,number))
    }

    void goToJob(String jobUuidText){
        go(PAGE_PATH + "/$jobUuidText")
    }

    def expectNumberOfStepsToBe(int steps){
        new WebDriverWait(driver,  Duration.ofSeconds(10)).until(
                ExpectedConditions.numberOfElementsToBe(stepsInJobDefinitionBy, steps)
        )
    }

    /**
     * It returns the list of "Actions" buttons
     */
    def getJobActionsButtonList(){
        (el jobActionsListButtonBy)
    }

    def getJobDeleteButtons(){
        (el jobDeleteButtonBy)
    }

    def getJobDisableScheduleButtonBy(){
        (el jobDisableScheduleButtonBy)
    }

    def getJobEnableScheduleButtonBy(){
        (el jobEnableScheduleButtonBy)
    }

    def getJobDeleteConfirmBy(){
        (el jobDeleteConfirmBy)
    }

    def waitForJobDeleteModalToBeShown() {
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(
                ExpectedConditions.visibilityOf(el jobDeleteModalBy)
        )
    }

    WebElement getJobUuid(){
        el jobUuidBy
    }

    WebElement getDeleteJobBtn(){
        waitForElementVisible jobDeleteModalBy
        el jobDeleteModalBy findElement(By.cssSelector(".btn.btn-danger.btn-sm"))
    }

    List<WebElement> getExtraOptFirsts(String optionName){
        els By.name("extra.option.$optionName")
    }

}
