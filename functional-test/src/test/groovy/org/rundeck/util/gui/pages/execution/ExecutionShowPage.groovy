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
    By execStatusIcon = By.cssSelector(".exec-status.icon")

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

    String waitForFinalState(){
        WebElement execStatusIconElem = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(execStatusIcon))
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                ExpectedConditions.not(
                        ExpectedConditions.attributeToBe(execStatusIconElem, "data-execstate", "RUNNING")
                )
        )

        execStatusIconElem.getAttribute('data-execstate')
    }

    String getExecutionStatus(){
        WebElement execStatusIconElem = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(execStatusIcon))
        return execStatusIconElem.getAttribute('data-execstate')
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
