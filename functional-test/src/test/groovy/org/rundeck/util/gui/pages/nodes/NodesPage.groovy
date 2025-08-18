package org.rundeck.util.gui.pages.nodes

import org.openqa.selenium.By
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class NodesPage extends BasePage {

    String loadPath = ""
    By searchNodesButtonBy = By.cssSelector(".btn.btn-cta.btn-fill.node_filter__dosearch")
    By nodeListTrBy = By.cssSelector(".node_entry.hover-action-holder.ansicolor-on")
    By searchNodeInputBy = By.id("schedJobNodeFilter")
    By nodeFilterInputToggleBy = By.cssSelector("[data-testid='nfi-toggle']")
    By nodeFilterInputDropdownSaveFilterBy = By.partialLinkText("Save Filter")
    By saveNodeFilterModalNameFieldBy = By.id("newFilterName")
    By saveNodeFilterModalSaveButtonBy = By.cssSelector("[data-testid='sfm-button-save']")

    By actionsDropdownToggleBy = By.cssSelector("[data-testid='nc-actions-dropdown-toggle']")
    By actionsDropdownRunCommandBy = By.cssSelector("[data-testid='nc-run-command']")
    By actionsDropdownSaveJobBy = By.cssSelector("[data-testid='nc-save-job']")


    static Closure<String> projectNodesPathResolver = { "/project/${it}/nodes" }
    static Closure<By> nodesTableNodeFilterLinkByResolver = { String filterLinkText ->
        def sanitizedTestId = filterLinkText?.replaceAll(/[^a-zA-Z0-9-_]/, '_')
        By.cssSelector("#nodesTable [data-testid='nfl-$sanitizedTestId']")
    }

    /**
     * Create a new page
     * @param context
     */
    NodesPage(SeleniumContext context) {
        super(context)
    }

    /**
     * Returns the total number of accessible nodes
     */
    int getTotalNodes(){
        (el searchNodeInputBy).clear()
        (el searchNodeInputBy).sendKeys(".*")
        clickSearchNodes()
        return (els nodeListTrBy).size()
    }

    def clickSearchNodes(){
        (el searchNodesButtonBy).click()
    }

    boolean findNodeByName(String nodeName){
        (el searchNodeInputBy).clear()
        (el searchNodeInputBy).sendKeys(nodeName)
        clickSearchNodes()
        return (els nodeListTrBy).size() > 0
    }

    void goToProjectNodesPage(String projectName) {
        driver.get("${context.client.baseUrl}${projectNodesPathResolver(projectName)}")
        waitForUrlToContain(projectName)
    }

    def setNodeInputText(String text){
        (el searchNodeInputBy).clear()
        (el searchNodeInputBy).sendKeys(text)
    }

    /**
     * Returns the count of node rows show on the screen
     */
    int getDisplayedNodesCount(){
        return (els nodeListTrBy).size()
    }

    /**
     * Verifies that a link <a href="...">...${linkPartialText}...</a> is present
     * @param linkPartialText the text to search for
     * @return
     */
    boolean partialLinkTextIsPresent(String linkPartialText) {
        els(By.partialLinkText(linkPartialText))?.isEmpty() == false
    }

    def setSaveNodeFilterModalNameFieldValue(String text){
        def element = el(saveNodeFilterModalNameFieldBy)
        element.clear()
        element.sendKeys(text)
    }
}
