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

}
