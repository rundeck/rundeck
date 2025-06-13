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
    String params=""

    By timeAbs = By.className("timeabs")
    By executionCount = By.className("summary-count")
    enum ActivityType {
        ANY(''),
        JOB('.job'),
        ADHOC('.adhoc')
        String css

        ActivityType(String css) {
            this.css = css
        }
    }

    enum ActivityState {
        ANY(''),
        SUCCEEDED('.succeed'),
        FAILED('.failed'),
        ABORTED('.aborted')
        String css

        ActivityState(String css) {
            this.css = css
        }
    }
    ActivityPage(final SeleniumContext context) {
        super(context)
    }
    ActivityPage(final SeleniumContext context, String projectName) {
        super(context)
        loadActivityPageForProject(projectName)
    }

    void loadActivityPageForProject(String projectName){
        this.loadPath = "/project/${projectName}/activity${params}"
    }

    WebElement getExecutionCount(){
        el executionCount
    }

    static By activityRowBy(ActivityType type, ActivityState state) {
        By.cssSelector(".link.activity_row.autoclickable${state.css}${type.css}")
    }

    List<WebElement> getActivityRows() {
        els(activityRowBy(ActivityType.JOB, ActivityState.SUCCEEDED))
    }

    List<WebElement> getActivityRows(ActivityType type, ActivityState state) {
        els(activityRowBy(type, state))
    }

    def waitForActivityRowsPresent(ActivityType type = ActivityType.ANY, ActivityState state = ActivityState.ANY) {
        waitForNumberOfElementsToBeMoreThan(activityRowBy(type, state), 0)
    }

    WebElement getTimeAbs() {
        el timeAbs
    }

    List<WebElement> getActivityRowsByJobName(String jobName) {
        els By.xpath("//*[contains(text(),'${jobName}')]")
    }

}
