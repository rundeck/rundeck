package org.rundeck.util.gui.pages.users

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * User List page (/user/list) — behind the vueUserList feature flag.
 * This flag has no per-request override (confirmed via FeatureTagLib.groovy — it reads
 * app-wide config only), so this page object always drives whichever rendering path is
 * currently the server's active default; there is no nextUi/legacyUi param to toggle here.
 */
@CompileStatic
class UserListPage extends BasePage {

    String loadPath = "/user/list"

    By newProfileLinkBy = By.linkText("+ New Profile …")
    By rowExpanderBy(String login) {
        By.xpath("//tr[.//*[normalize-space(text())='${login}']]//button[contains(@class,'expander') or @aria-expanded]")
    }
    By editLinkBy(String login) {
        By.xpath("//a[contains(@href,'/user/edit?login=${login}')]")
    }
    By groupsHeaderBy = By.xpath("//th[contains(.,'Groups')]")
    By groupsHelpIconBy = By.cssSelector("[data-testid='groups-help-icon']")
    By notSetBadgeBy = By.xpath("//*[contains(text(),'NOT SET') or contains(text(),'Not set')]")

    UserListPage(final SeleniumContext context) {
        super(context)
    }

    WebElement getNewProfileLink() {
        el newProfileLinkBy
    }

    WebElement getEditLink(String login) {
        el editLinkBy(login)
    }

    void clickRowExpander(String login) {
        def expander = el rowExpanderBy(login)
        expander.click()
    }
}
