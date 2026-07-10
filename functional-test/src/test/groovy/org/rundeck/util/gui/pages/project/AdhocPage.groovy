package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

@CompileStatic
class AdhocPage extends BasePage implements ActivityListTrait {
    String loadPath = ""
    boolean nextUi = false
    String projectName

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

    // NextUI Toggle Selectors
    By nextUiToggleBy = By.id("nextUi")
    By nextUiToggleLabelBy = By.cssSelector("label[for='nextUi']")

    // Vue App Mount Point Selector (for adhocNext.gsp)
    By vueAppMountBy = By.cssSelector(".adhoc-page-vue")

    AdhocPage(final SeleniumContext context, final String project) {
        super(context)
        this.projectName = project
        loadDashboardForProject(project)
    }

    void loadDashboardForProject(String projectName) {
        this.projectName = projectName
        this.loadPath = "/project/${projectName}/command/run${nextUi ? '?nextUi=true' : ''}"
    }

    @Override
    void go() {
        // Rebuild loadPath with current nextUi value before navigating
        if (projectName) {
            loadDashboardForProject(projectName)
        }
        super.go()
    }

    /**
     * Wait for Vue app to mount if using next UI version
     * This ensures Vue components are ready before interacting with them
     */
    void waitForVueAppIfNeeded() {
        if (nextUi) {
            // Wait for Vue mount point to be present
            waitForElementVisible(vueAppMountBy)
            // Wait for Vue app to render command input (indicates app is ready)
            waitForElementToBeClickable(commandInputBy)
        }
    }

    /**
     * Check if this page instance is using the Vue (next) UI
     */
    boolean isNextUi() {
        return nextUi
    }

    // Node Filter Methods
    WebElement getNodeFilterInput() {
        waitForVueAppIfNeeded()
        waitForElementToBeClickable nodeFilterInputBy
        el nodeFilterInputBy
    }

    WebElement getFilterNodeButton() {
        waitForElementToBeClickable filterNodeButtonBy
        el filterNodeButtonBy
    }

    WebElement getNodeFilterResults() {
        waitForElementVisible nodeFilterResultsBy
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
        waitForVueAppIfNeeded()
        waitForElementToBeClickable commandInputBy
        el commandInputBy
    }

    WebElement getRecentCommandsDropdown() {
        waitForElementToBeClickable recentCommandsDropdownBy
        el recentCommandsDropdownBy
    }

    WebElement getRecentCommandsMenu() {
        waitForElementVisible recentCommandsMenuBy
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
        waitForVueAppIfNeeded()
        commandInput.click()
        // Wait for input to be enabled (not disabled)
        waitForElementAttributeToChange(commandInput, 'disabled', null)
        commandInput.clear()
        commandInput.sendKeys(command)
    }

    void clickRecentCommands() {
        recentCommandsDropdown.click()
        waitForElementVisible recentCommandsMenuBy
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
            waitForElementVisible runConfigPanelBy
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
        waitForElementVisible runContentBy
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

    // NextUI Toggle Methods
    WebElement getNextUiToggle() {
        el nextUiToggleBy
    }

    WebElement getNextUiToggleLabel() {
        el nextUiToggleLabelBy
    }

    boolean isNextUiToggleDisplayed() {
        try {
            return nextUiToggle.displayed
        } catch (Exception e) {
            return false
        }
    }

    // Combined Actions
    def runCommandAndWaitToBe(String command, String state) {
        // Ensure nodes are filtered first
        if (!nodeFilterResults.displayed) {
            filterNodes(".*")
            waitForElementVisible nodeFilterResultsBy
        }
        
        enterCommand(command)
        clickRun()
        
        // Wait for execution to reach desired state
        if (state) {
            waitForElementVisible runContentBy
            // Additional wait logic for execution state can be added here
        }
    }

    def runCommandWithSettings(String command, int threadCount, boolean keepgoing) {
        filterNodes(".*")
        waitForElementVisible nodeFilterResultsBy
        
        setThreadCount(threadCount)
        setNodeKeepgoing(keepgoing)
        
        enterCommand(command)
        clickRun()
        
        waitForElementVisible runContentBy
    }
}
