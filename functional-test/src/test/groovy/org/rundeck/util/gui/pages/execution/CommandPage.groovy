package org.rundeck.util.gui.pages.execution

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * Command page
 */
@CompileStatic
class CommandPage extends BasePage {

    String loadPath = ""

    By nodeFilterTextBy = By.xpath("//*[@id=\"schedJobNodeFilter\"]")
    By filterNodeBy = By.xpath("//button[contains(@class, 'node_filter__dosearch')]")
    By commandInputTextBy = By.xpath("//input[@id='runFormExec']")
    By runBy = By.xpath("//a[@onclick=\"runFormSubmit('runbox');\"]")
    By saveAsJobBy = By.xpath("//a[contains(@href, '/job/createFromExecution?executionId=')]")
    By runningExecutionStateBy = By.cssSelector("#runcontent .executionshow .execution-action-links a .execstate[data-execstate]")
    By abortBy = By.cssSelector("span[data-bind\$='killExecAction']")
    By runContentBy = By.id("runcontent")
    By runArgumentBy = By.className("argString")
    By execLogGutterBy = By.className("execution-log_gutter-entry")
    By execLogContentBy = By.className("execution-log__content-text")
    By runningExecStateBy = By.cssSelector(".execstate.execstatedisplay.overall")
    By activityRowAdhocBy = By.cssSelector(".link.activity_row.autoclickable.succeed.adhoc")
    
    // Node popover selectors
    By nodeElementsBy = By.cssSelector(".node_ident.embedded_node.tight")
    By popoverContainerBy = By.cssSelector(".popover")
    By popoverContentBy = By.cssSelector(".popover-content")
    By popoverParameterRowsBy = By.cssSelector(".popover-content .node-details-simple tbody tr")
    By parameterKeyBy = By.cssSelector(".key")
    By parameterValueBy = By.cssSelector(".value")

    CommandPage(final SeleniumContext context) {
        super(context)
    }

    CommandPage(final SeleniumContext context, String project) {
        super(context)
        this.loadPath = "/project/${project}/command/run"
    }

    WebElement getNodeFilterTextField() {
        waitForElementToBeClickable nodeFilterTextBy
        el nodeFilterTextBy
    }

    WebElement getFilterNodeButton() {
        waitForElementToBeClickable filterNodeBy
        el filterNodeBy
    }

    WebElement getCommandTextField() {
        waitForElementToBeClickable commandInputTextBy
        el commandInputTextBy
    }

    WebElement getRunButton() {
        waitForElementToBeClickable runBy
        el runBy
    }

    WebElement getSaveAsJobButton() {
        waitForElementToBeClickable saveAsJobBy
        el saveAsJobBy
    }

    WebElement getRunningExecutionStateButton() {
        waitForElementToBeClickable runningExecutionStateBy
        el runningExecutionStateBy
    }

    WebElement getAbortButton() {
        el abortBy
    }

    WebElement runningButtonLink() {
        waitForElementToBeClickable runningExecutionStateButton
        runningExecutionStateButton.findElement By.xpath("./..")
    }

    WebElement getActivityRowAdhoc() {
        waitForElementToBeClickable activityRowAdhocBy
        el activityRowAdhocBy findElement By.className("timeabs")
    }

    WebElement getRunContent() {
        el runContentBy
    }

    WebElement getRunArgument() {
        el runArgumentBy
    }

    List<WebElement> getExecLogGutters() {
        els execLogGutterBy
    }

    WebElement getExecLogContent() {
        el execLogContentBy
    }

    String getRunningExecState() {
        el runningExecStateBy getAttribute("data-execstate")
    }

    def runCommandAndWaitToBe(String command, String state) {
        nodeFilterTextField.click()
        nodeFilterTextField.sendKeys".*"
        filterNodeButton.click()
        waitForElementToBeClickable commandTextField
        commandTextField.click()
        waitForElementAttributeToChange commandTextField, 'disabled', null
        commandTextField.sendKeys command
        runButton.click()
        waitForElementAttributeToChange runningExecutionStateButton, 'data-execstate', state
    }

    List<WebElement> getNodeElements() {
        waitForNumberOfElementsToBeMoreThan(nodeElementsBy, 0)
        els nodeElementsBy
    }

    void clickNode(int index) {
        def nodes = getNodeElements()
        if (nodes.size() <= index) {
            throw new IndexOutOfBoundsException("Node index ${index} is out of bounds. Only ${nodes.size()} nodes available.")
        }
        waitForElementToBeClickable nodes[index]
        nodes[index].click()
        // Wait for popover container to start appearing (popover animation)
        waitForElementVisible popoverContainerBy
    }

    void waitForPopoverToAppear() {
        waitForElementVisible popoverContainerBy
        waitForElementVisible popoverContentBy
    }

    WebElement getPopoverContent() {
        waitForPopoverToAppear()
        el popoverContentBy
    }

    List<WebElement> getPopoverParameterRows() {
        waitForPopoverToAppear()
        // Wait for at least one parameter row to be present, ensuring content is fully loaded
        waitForNumberOfElementsToBeMoreThan(popoverParameterRowsBy, 0)
        els popoverParameterRowsBy
    }

    String getParameterKey(WebElement row) {
        try {
            def keyElement = row.findElement(parameterKeyBy)
            return keyElement.text.trim()
        } catch (Exception e) {
            return null
        }
    }

    String getParameterValue(WebElement row) {
        try {
            def valueElement = row.findElement(parameterValueBy)
            return valueElement.text.trim()
        } catch (Exception e) {
            // Try to get text from the row itself if value cell doesn't exist
            return row.text.trim()
        }
    }

    boolean isParameterRowVisible(WebElement row) {
        try {
            return row.isDisplayed()
        } catch (Exception e) {
            return false
        }
    }

}
