package org.rundeck.util.gui.pages.project

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

class ActivityPage extends BasePage{

    String loadPath = ""
    By executionCount = By.className("summary-count")

    /**
     * Create a new page
     * @param context
     */
    ActivityPage(SeleniumContext context) {
        super(context)
    }

    void loadActivityPageForProject(String projectName){
        this.loadPath = "/project/${projectName}/activity"
    }

    WebElement getExecutionCount(){
        el executionCount
    }
}
