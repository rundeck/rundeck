package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

@CompileStatic
class AdhocPage extends BasePage implements ActivityListTrait {
    String loadPath = ""

    // Node Filter Selectors
    By nodeFilterInputBy = By.cssSelector("input[name='filter'], #schedJobNodeFilter")
    By filterNodeButtonBy = By.cssSelector("button.node_filter__dosearch, button[type='submit']")
    By nodeFilterResultsBy = By.cssSelector(".node_filter_results__matched_nodes")
    By emptyErrorBy = By.id("emptyerror")
    By loadErrorBy = By.id("loaderror2")
    By viewInNodesPageLinkBy = By.xpath("//a[contains(text(), 'View in nodes page')]")

    // Command Form Selectors
    By commandInputBy = By.id("runFormExec")
    By recentCommandsDropdownBy = By.cssSelector(".act_adhoc_history_dropdown")
    By recentCommandsMenuBy = By.cssSelector(".dropdown-menu")
    By recentCommandItemBy = By.cssSelector(".act_fill_cmd")
    By settingsButtonBy = By.cssSelector("button[data-target='#runconfig']")
    By runButtonBy = By.cssSelector(".runbutton, a.runbutton")
    By runConfigPanelBy = By.id("runconfig")
    By threadCountInputBy = By.id("runNodeThreadcount")
    By nodeKeepgoingTrueBy = By.id("nodeKeepgoingTrue")
    By nodeKeepgoingFalseBy = By.id("nodeKeepgoingFalse")

    // Execution Output Selectors
    By runContentBy = By.id("runcontent")
    By runErrorBy = By.id("runerror")
    By runErrorMessageBy = By.cssSelector("#runerror .errormessage")
    By closeOutputBy = By.cssSelector(".closeoutput")

    // Activity Section Selectors
    By activitySectionBy = By.id("activity_section")
    By activityListBy = By.cssSelector("._history_content.vue-project-activity")

    AdhocPage(final SeleniumContext context, final String project, Map params = [:]) {
        super(context)
        loadDashboardForProject(project, params)
    }

    void loadDashboardForProject(String projectName, Map params = [:]) {
        def queryParams = []
        if (params.nextUi) {
            queryParams << "nextUi=true"
        }
        if (params.legacyUi) {
            queryParams << "legacyUi=true"
        }
        def queryString = queryParams ? "?${queryParams.join('&')}" : ""
        this.loadPath = "/project/${projectName}/command/run${queryString}"
    }

    // Node Filter Methods
    WebElement getNodeFilterInput() {
        waitForElementToBeClickable nodeFilterInputBy
        el nodeFilterInputBy
    }

    WebElement getFilterNodeButton() {
        waitForElementToBeClickable filterNodeButtonBy
        el filterNodeButtonBy
    }

    WebElement getNodeFilterResults() {
        waitForElementToBeVisible nodeFilterResultsBy
        el nodeFilterResultsBy
    }

    WebElement getEmptyError() {
        el emptyErrorBy
    }

    WebElement getLoadError() {
        el loadErrorBy
    }

    WebElement getViewInNodesPageLink() {
        waitForElementToBeClickable viewInNodesPageLinkBy
        el viewInNodesPageLinkBy
    }

    void enterNodeFilter(String filter) {
        nodeFilterInput.click()
        nodeFilterInput.clear()
        nodeFilterInput.sendKeys(filter)
    }

    void submitNodeFilter() {
        filterNodeButton.click()
    }

    void filterNodes(String filter) {
        enterNodeFilter(filter)
        submitNodeFilter()
    }

    // Command Form Methods
    WebElement getCommandInput() {
        waitForElementToBeClickable commandInputBy
        el commandInputBy
    }

    WebElement getRecentCommandsDropdown() {
        waitForElementToBeClickable recentCommandsDropdownBy
        el recentCommandsDropdownBy
    }

    WebElement getRecentCommandsMenu() {
        waitForElementToBeVisible recentCommandsMenuBy
        el recentCommandsMenuBy
    }

    List<WebElement> getRecentCommandItems() {
        els recentCommandItemBy
    }

    WebElement getSettingsButton() {
        waitForElementToBeClickable settingsButtonBy
        el settingsButtonBy
    }

    WebElement getRunButton() {
        waitForElementToBeClickable runButtonBy
        el runButtonBy
    }

    WebElement getRunConfigPanel() {
        el runConfigPanelBy
    }

    WebElement getThreadCountInput() {
        waitForElementToBeClickable threadCountInputBy
        el threadCountInputBy
    }

    WebElement getNodeKeepgoingTrue() {
        waitForElementToBeClickable nodeKeepgoingTrueBy
        el nodeKeepgoingTrueBy
    }

    WebElement getNodeKeepgoingFalse() {
        waitForElementToBeClickable nodeKeepgoingFalseBy
        el nodeKeepgoingFalseBy
    }

    void enterCommand(String command) {
        commandInput.click()
        waitForElementAttributeToChange(commandInput, 'disabled', null)
        commandInput.clear()
        commandInput.sendKeys(command)
    }

    void clickRecentCommands() {
        recentCommandsDropdown.click()
        waitForElementToBeVisible recentCommandsMenuBy
    }

    void selectRecentCommand(int index) {
        clickRecentCommands()
        def items = recentCommandItems
        if (items && items.size() > index) {
            items[index].click()
        }
    }

    void openSettings() {
        if (!runConfigPanel.displayed) {
            settingsButton.click()
            waitForElementToBeVisible runConfigPanelBy
        }
    }

    void setThreadCount(int count) {
        openSettings()
        threadCountInput.clear()
        threadCountInput.sendKeys(count.toString())
    }

    void setNodeKeepgoing(boolean keepgoing) {
        openSettings()
        if (keepgoing) {
            nodeKeepgoingTrue.click()
        } else {
            nodeKeepgoingFalse.click()
        }
    }

    void clickRun() {
        runButton.click()
    }

    // Execution Output Methods
    WebElement getRunContent() {
        waitForElementToBeVisible runContentBy
        el runContentBy
    }

    WebElement getRunError() {
        el runErrorBy
    }

    String getRunErrorMessage() {
        el runErrorMessageBy getText()
    }

    WebElement getCloseOutputButton() {
        el closeOutputBy
    }

    void closeOutput() {
        if (closeOutputButton.displayed) {
            closeOutputButton.click()
        }
    }

    boolean isRunContentVisible() {
        try {
            return runContent.displayed
        } catch (Exception e) {
            return false
        }
    }

    boolean isRunErrorVisible() {
        try {
            return runError.displayed && runError.getAttribute("class").contains("show")
        } catch (Exception e) {
            return false
        }
    }

    // Activity Section Methods
    WebElement getActivitySection() {
        el activitySectionBy
    }

    WebElement getActivityList() {
        el activityListBy
    }

    boolean isActivitySectionVisible() {
        try {
            return activitySection.displayed
        } catch (Exception e) {
            return false
        }
    }

    // Combined Actions
    def runCommandAndWaitToBe(String command, String state) {
        // Ensure nodes are filtered first
        if (!nodeFilterResults.displayed) {
            filterNodes(".*")
            waitForElementToBeVisible nodeFilterResultsBy
        }
        
        enterCommand(command)
        clickRun()
        
        // Wait for execution to reach desired state
        if (state) {
            waitForElementToBeVisible runContentBy
            // Additional wait logic for execution state can be added here
        }
    }

    def runCommandWithSettings(String command, int threadCount, boolean keepgoing) {
        filterNodes(".*")
        waitForElementToBeVisible nodeFilterResultsBy
        
        setThreadCount(threadCount)
        setNodeKeepgoing(keepgoing)
        
        enterCommand(command)
        clickRun()
        
        waitForElementToBeVisible runContentBy
    }
}
