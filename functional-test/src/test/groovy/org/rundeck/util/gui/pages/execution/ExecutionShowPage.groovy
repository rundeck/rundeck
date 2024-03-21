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

    By executionStateDisplayBy = By.cssSelector("#subtitlebar summary > span.execution-summary-status > span.execstate.execstatedisplay.overall")
    By abortButtonBy = By.cssSelector('#subtitlebar section.execution-action-links span.btn-danger[data-bind="click: killExecAction"]')
    By viewContentNodesBy = By.cssSelector("#nodes")
    By viewButtonNodesBy = By.cssSelector("#btn_view_nodes")
    By viewContentOutputBy = By.cssSelector("#output")
    By viewButtonOutputBy = By.id("btn_view_output")
    By logOutputBy = By.className("execution-log__content-text")
    By nodeFlowStateBy = By.id("nodeflowstate")
    By execStatusIcon = By.cssSelector(".exec-status.icon")
    By autoCaretBy = By.cssSelector(".auto-caret.text-muted")
    By execLogNodeBy = By.className("execution-log__node-chunk")
    By execLogGutterEntryBy = By.className("execution-log_gutter-entry")
    By execLogGutterBy = By.className("execution-log__gutter")
    By execLogLineBy = By.className("execution-log__line")
    By execLogSettingsBy = By.className("execution-log__settings")
    By execRedColorTextBy = By.className("ansi-fg-red")
    By settingsOptionsBy = By.cssSelector(".rd-drawer.rd-drawer--left.rd-drawer--active")
    By maskSettingsOptionsBy = By.cssSelector(".ant-drawer.ant-drawer-left.ant-drawer-open.no-mask")
    By popUpSettingsBy = By.cssSelector(".execution-log.execution-log--light")
    By logNodeSettingsBy = By.className("execution-log__node-badge")
    By logContentTextBy = By.className("execution-log__content-text")
    By logContentTextOverflowBy = By.cssSelector(".execution-log__content-text.execution-log__content-text--overflow")
    By gutterLineNumberBy = By.cssSelector(".gutter.line-number")

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

    String waitForFinalState(){
        WebElement execStatusIconElem = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(execStatusIcon))
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                ExpectedConditions.not(
                        ExpectedConditions.attributeToBe(execStatusIconElem, "data-execstate", "RUNNING")
                )
        )

        execStatusIconElem.getAttribute('data-execstate')
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

    List<WebElement> getTextContains(String text) {
        els By.xpath("//*[contains(normalize-space(text()), '$text')]")
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
            expandedNode.findElements(By.cssSelector(".wfnodestep")).collect { it.findElement(By.cssSelector('.execstatedisplay')).getAttribute('data-execstate') }
        }
    }
}
