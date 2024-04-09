package org.rundeck.util.gui.pages.jobs

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class CreateDuplicatedJobPage extends BasePage{

    String loadPath
    By jobGroupBy = By.cssSelector("input#schedJobGroup")
    By createJobBy = By.id("Create")
    By jobNameInputBy = By.cssSelector("form input[name=\"jobName\"]")

    /**
     * Create a new page
     * @param context
     */
    CreateDuplicatedJobPage(SeleniumContext context) {
        super(context)
    }

    void loadDuplicatedJobPath(String projectName, String existentJobUuid){
        this.loadPath = "/project/${projectName}/job/copy/${existentJobUuid}"
    }

    WebElement getJobGroupField() {
        waitForElementVisible jobGroupBy
        el jobGroupBy
    }

    WebElement getCreateJobButton() {
        el createJobBy
    }

    WebElement getJobNameInput() {
        el jobNameInputBy
    }
}
