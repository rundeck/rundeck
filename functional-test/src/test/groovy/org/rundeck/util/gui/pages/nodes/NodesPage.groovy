package org.rundeck.util.gui.pages.nodes

import org.openqa.selenium.By
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class NodesPage extends BasePage {

    String loadPath = ""
    By searchNodesButtonBy = By.cssSelector(".btn.btn-cta.btn-fill.node_filter__dosearch")
    By nodeListTrBy = By.cssSelector(".node_entry.hover-action-holder.ansicolor-on")
    By searchNodeInputBy = By.id("schedJobNodeFilter")

    /**
     * Create a new page
     * @param context
     */
    NodesPage(SeleniumContext context) {
        super(context)
    }

    /**
     * it returns the total number of nodes found
     */
    def getTotalNodes(){
        (el searchNodeInputBy).clear()
        (el searchNodeInputBy).sendKeys(".*")
        clickSearchNodes()
        return (els nodeListTrBy).size()
    }

    def clickSearchNodes(){
        (el searchNodesButtonBy).click()
    }

    /**
     *
     */
    boolean findNodeByName(String nodeName){
        (el searchNodeInputBy).clear()
        (el searchNodeInputBy).sendKeys(nodeName)
        clickSearchNodes()
        return (els nodeListTrBy).size() > 0
    }
}
