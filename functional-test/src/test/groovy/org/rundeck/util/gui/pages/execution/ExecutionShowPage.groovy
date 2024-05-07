package org.rundeck.util.gui.pages.execution

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage
import java.time.Duration

/**
 * Execution Show page
 */
@CompileStatic
class ExecutionShowPage extends BasePage {

    String loadPath = "execution/show"

    static final String execStateAttribute = 'data-execstate'

    static final By executionStateDisplayBy = By.cssSelector("#subtitlebar summary > span.execution-summary-status > span.execstate.execstatedisplay.overall")
    static final By abortButtonBy = By.cssSelector('#subtitlebar section.execution-action-links span.btn-danger[data-bind="click: killExecAction"]')
    static final By viewContentNodesBy = By.cssSelector("#nodes")
    static final By viewButtonNodesBy = By.cssSelector("#btn_view_nodes")
    static final By viewContentOutputBy = By.cssSelector("#output")
    static final By viewButtonOutputBy = By.id("btn_view_output")
    static final By logOutputBy = By.className("execution-log__content-text")
    static final By nodeFlowStateBy = By.id("nodeflowstate")
    static final By execStatusIcon = By.cssSelector('#jobInfo_ .exec-status.icon')
    static final By execCompletedIcon = By.cssSelector('#subtitlebar .fas.fa-flag-checkered')
    static final By optionValueSelected = By.cssSelector(".optvalue:nth-child(3)")
    static final By jobRunSpinner = By.cssSelector(".loading-spinner")
    static final By autoCaretBy = By.cssSelector(".auto-caret.text-muted")
    static final By execLogNodeBy = By.className("execution-log__node-chunk")
    static final By execLogGutterEntryBy = By.className("execution-log_gutter-entry")
    static final By execLogGutterBy = By.className("execution-log__gutter")
    static final By execLogLineBy = By.className("execution-log__line")
    static final By execLogSettingsBy = By.className("execution-log__settings")
    static final By execRedColorTextBy = By.className("ansi-fg-red")
    static final By settingsOptionsBy = By.cssSelector(".rd-drawer.rd-drawer--left.rd-drawer--active")
    static final By maskSettingsOptionsBy = By.cssSelector(".ant-drawer.ant-drawer-left.ant-drawer-open.no-mask")
    static final By popUpSettingsBy = By.cssSelector(".execution-log.execution-log--light")
    static final By logNodeSettingsBy = By.className("execution-log__node-badge")
    static final By logContentTextBy = By.className("execution-log__content-text")
    static final By logContentTextOverflowBy = By.cssSelector(".execution-log__content-text.execution-log__content-text--overflow")
    static final By gutterLineNumberBy = By.cssSelector(".gutter.line-number")
    By runAgainLink = By.linkText("Run Again")

    ExecutionShowPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on execution show page: " + driver.currentUrl)
        }
    }

    WebElement getExecutionStateDisplayLabel() {
        el executionStateDisplayBy
    }

    WebElement getAbortButton() {
        el abortButtonBy
    }

    WebElement getViewContentNodes() {
        el viewContentNodesBy
    }

    WebElement getViewButtonNodes() {
        el viewButtonNodesBy
    }

    WebElement getViewContentOutput() {
        el viewContentOutputBy
    }

    WebElement getViewButtonOutput() {
        el viewButtonOutputBy
    }

    List<WebElement> getLogOutput(){
        els logOutputBy
    }

    WebElement getNodeFlowState(){
        el nodeFlowStateBy
    }

    WebElement getOptionValueSelected(){
        el optionValueSelected
    }

    void waitUntilSpinnerHides(){
        new WebDriverWait(driver, Duration.ofMinutes(2)).until(
                ExpectedConditions.invisibilityOf((el jobRunSpinner))
        )
    }

    String waitForFinalState(){
        WebElement execStatusIconElem = waitForElementVisible(execStatusIcon)
        waitForElementVisible(execCompletedIcon)

        return execStatusIconElem.getAttribute(execStateAttribute)
    }

    /**
     * It wait for the run again link to be shown
     */
    void waitForRunAgainLink(){
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.numberOfElementsToBe(runAgainLink, 1))
    }

    String getExecutionStatus(){
        WebElement execStatusIconElem = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(execStatusIcon))
        return execStatusIconElem.getAttribute(execStateAttribute)
    }

    NodesView getNodesView(){
        if(getCurrentView() != NodesView.VIEW_NAME)
            switchToView(NodesView.VIEW_NAME)

        return new NodesView(this)
    }

    void switchToView(String viewName){
        driver.get(driver.getCurrentUrl().replace("#${getCurrentView()}", "#${viewName}"))
    }

    String getCurrentView(){
        String currentUrl = driver.getCurrentUrl()
        return currentUrl.substring(currentUrl.lastIndexOf('#') + 1)
    }

    class NodesView {
        static final String VIEW_NAME = 'nodes'
        final ExecutionShowPage execPage
        WebElement expandedNode
        NodesView(ExecutionShowPage execPage){
            this.execPage = execPage
        }

        NodesView expandNode(int nodeOrder){
            expandedNode = execPage.driver.findElements(By.className('wfnodestate')).get(nodeOrder)
            expandedNode.click()
            return this
        }

        List<String> getExecStateForSteps(){
            expandedNode.findElements(By.cssSelector(".wfnodestep")).collect { it.findElement(By.cssSelector('.execstatedisplay')).getAttribute(ExecutionShowPage.execStateAttribute) }
        }
    }

    List<WebElement> getAutoCaret() {
        els autoCaretBy
    }

    WebElement getExecLogNode() {
        el execLogNodeBy
    }

    String getExecLogGutterEntryAttribute() {
        el execLogGutterEntryBy getAttribute("pseudo-content")
    }

    String getExecLogGutterEntryAttribute(int index) {
        els execLogGutterEntryBy get index getAttribute("pseudo-content")
    }

    List<WebElement> getExecLogEntryGutters() {
        els execLogGutterEntryBy
    }

    WebElement getExecLogEntryGutter() {
        el execLogGutterEntryBy
    }

    List<WebElement> getExecLogGutters() {
        els execLogGutterBy
    }

    WebElement getExecLogGutter() {
        el execLogGutterBy
    }

    List<WebElement> getExecLogLines() {
        els execLogLineBy
    }

    WebElement getExecLogSettings() {
        el execLogSettingsBy isDisplayed()
        el execLogSettingsBy findElement(By.tagName("button"))
    }

    WebElement getExecRedColorText() {
        el execRedColorTextBy
    }

    List<WebElement> getSettingsInputOptions() {
        waitForElementVisible settingsOptionsBy
        el settingsOptionsBy findElements(By.tagName("input"))
    }

    WebElement getSettingsOption() {
        el settingsOptionsBy
    }

    WebElement getClosePopupSettingsButton() {
        el popUpSettingsBy findElement By.cssSelector(".btn.btn-default.btn-link")
    }

    WebElement getLogNodeSetting() {
        el logNodeSettingsBy
    }

    List<WebElement> getLogNodeSettings() {
        els logNodeSettingsBy
    }

    List<WebElement> waitForLineTobeShownNumberOfTimes(String text, int times) {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.numberOfElementsToBe(By.xpath("//*[contains(normalize-space(text()), '$text')]"), times)
        )
    }

    WebElement getGutterLineNumber() {
        el gutterLineNumberBy
    }

    List<WebElement> getGutterLineNumbers() {
        els gutterLineNumberBy
    }

    WebElement getLogContentText() {
        el logContentTextBy
    }

    WebElement getStat() {
        el By.className("stats")
    }

    List<WebElement> getStats() {
        els By.className("stats")
    }

    List<WebElement> getLogContentTextOverflows() {
        els logContentTextOverflowBy
    }

    def validateStatus(String status) {
        waitForElementAttributeToChange executionStateDisplayLabel, 'data-execstate', status
    }
}
