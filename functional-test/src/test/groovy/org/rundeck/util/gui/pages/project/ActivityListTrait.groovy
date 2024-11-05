package org.rundeck.util.gui.pages.project

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

/**
 * Common selectors for Activity List section which is included on multiple pages
 */
trait ActivityListTrait {
    abstract SeleniumContext getContext()

    abstract WebElement el(By by)

    abstract List<WebElement> els(By by)
    static By activitySectionBy = By.id('activity_section')
    static By activityBulkDeleteBtnBy = By
        .cssSelector('#activity_section .activity-list > section button[data-testid="activity-list-bulk-delete"]')

    WebElement getActivitySection() {
        el activitySectionBy
    }
    WebElement getActivityBulkDeleteBtn() {
        el activityBulkDeleteBtnBy
    }
}