package org.rundeck.util.gui.pages.activity

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

@CompileStatic
class ActivityPage extends BasePage {

    String loadPath = "activity"

    By activityRowBy = By.cssSelector(".link.activity_row.autoclickable.succeed.job")
    By timeAbs = By.className("timeabs")

    ActivityPage(final SeleniumContext context) {
        super(context)
    }

    List<WebElement> getActivityRows() {
        els activityRowBy
    }

    WebElement getTimeAbs() {
        el timeAbs
    }

}