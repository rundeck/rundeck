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
    static By activityListBy = By.cssSelector('#activity_section .activity-list')
    static By activitySectionLinksBy = By.cssSelector('#activity_section .nav.activity_links')
    static By activitySectionActivityTabLinkBy = By.cssSelector('#activity_section .nav.activity_links a[href="#history"]')
    static By activityBulkDeleteBtnBy = By
        .cssSelector('#activity_section .activity-list > section button[data-testid="activity-list-bulk-delete"]')

    WebElement getActivityList() {
        el activityListBy
    }
    WebElement getActivitySectionSectionLinks() {
        el activitySectionLinksBy
    }
    WebElement getActivitySectionActivityTabLink() {
        el activitySectionActivityTabLinkBy
    }
    WebElement getActivityBulkDeleteBtn() {
        el activityBulkDeleteBtnBy
    }
}