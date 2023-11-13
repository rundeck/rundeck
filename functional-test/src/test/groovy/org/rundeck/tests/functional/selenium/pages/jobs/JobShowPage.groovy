package org.rundeck.tests.functional.selenium.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

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

    String loadPath = "/job/show"

    JobShowPage(final SeleniumContext context) {
        super(context)
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

}
