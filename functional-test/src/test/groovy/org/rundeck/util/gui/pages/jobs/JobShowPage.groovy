package org.rundeck.util.gui.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * Job show page
 */
@CompileStatic
class JobShowPage extends BasePage{

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
    By jobLinkTitleBy = By.xpath("//a[contains(@class, 'job-header-link')]")
    By autocompleteJobStepDefinitionBy = By.cssSelector("#wfitem_0 > span > div > div > span > span > span.text-success")
    By runFormBy = By.cssSelector("#execDiv #exec_options_form #formbuttons #execFormRunButton")
    By optionValidationWarningBy = By.cssSelector("#execDiv #exec_options_form #optionSelect #_commandOptions div.form-group.has-warning p.text-warning")
    By jobRowBy = By.cssSelector("#job_group_tree .jobname.job_list_row[data-job-id] > a[data-job-id]")
    By jobSearchBy = By.xpath("//span[@title='Click to modify filter']")
    By jobSearchNameBy = By.cssSelector('#jobs_filters form input[name="jobFilter"]')
    By jobSearchGroupBy = By.cssSelector('#jobs_filters form input[name="groupPath"]')
    By jobSearchSubmitBy = By.cssSelector('#jobs_filters form #jobs_filters_footer input[type="submit"][name="_action_jobs"]')

    String loadPath = "/job/show"

    JobShowPage(final SeleniumContext context) {
        super(context)
    }

    JobShowPage(final SeleniumContext context, String project) {
        super(context)
        this.loadPath = "/project/$project/jobs"
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
        el descriptionText
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

}
