package org.rundeck.util.gui.pages.activity

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.gui.pages.project.ActivityListTrait

@CompileStatic
class ActivityPage extends BasePage implements ActivityListTrait{

    String loadPath = "activity"

    By activityRowBy = By.cssSelector(".link.activity_row.autoclickable.succeed.job")
    By timeAbs = By.className("timeabs")
    By executionCount = By.className("summary-count")

    ActivityPage(final SeleniumContext context) {
        super(context)
    }
    ActivityPage(final SeleniumContext context, String projectName) {
        super(context)
        loadActivityPageForProject(projectName)
    }

    void loadActivityPageForProject(String projectName){
        this.loadPath = "/project/${projectName}/activity"
    }

    WebElement getExecutionCount(){
        el executionCount
    }

    List<WebElement> getActivityRows() {
        els activityRowBy
    }

    WebElement getTimeAbs() {
        el timeAbs
    }

    List<WebElement> getActivityRowsByJobName(String jobName) {
        els By.xpath("//*[contains(text(),'${jobName}')]")
    }

}