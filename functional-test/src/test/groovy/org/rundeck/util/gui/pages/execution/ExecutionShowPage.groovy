package org.rundeck.util.gui.pages.execution

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

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
    By logOutputSwitchBy = By.id("btn_view_output")
    By nodeFlowStateBy = By.id("nodeflowstate")

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

    WebElement getLogOutputSwitch(){
        el logOutputSwitchBy
    }

    WebElement getNodeFlowState(){
        el nodeFlowStateBy
    }

}
