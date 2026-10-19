package org.rundeck.util.gui.pages.users

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * User List page (/user/list) — Vue rendering gated behind the {@code nextUi} URL param
 * (NEXT_UI stage; legacy KO/GSP rendering remains the server default).
 */
@CompileStatic
class UserListPage extends BasePage {

    private String loadPath = "/user/list"

    @Override
    String getLoadPath() {
        nextUi ? "${loadPath}?nextUi=true" : loadPath
    }

    boolean getNextUi() { isFlagEnabled('nextUi') }
    void setNextUi(boolean value) { withFlag('nextUi', value) }

    By newProfileLinkBy = By.xpath("//a[contains(@href,'/user/create')]")

    /** Expander markup differs per DOM: Vue button has a data-testid, legacy is a span with a fixed id. */
    By rowExpanderBy(String login) {
        nextUi
            ? By.cssSelector("[data-testid='user-expander-${login}']")
            : By.id("_exp_udetail_${login}")
    }
    By editLinkBy(String login) {
        By.xpath("//a[contains(@href,'/user/edit?login=${login}')]")
    }
    By groupsHeaderBy = By.xpath("//th[contains(.,'Groups')]")

    /** Help-tooltip icon differs per DOM: Vue has a data-testid, legacy's <g:helpTooltip> renders a bare .has_tooltip span. */
    By getGroupsHelpIconBy() {
        nextUi
            ? By.cssSelector("[data-testid='groups-help-icon']")
            : By.cssSelector("th.table-header .has_tooltip")
    }
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
        By locator = rowExpanderBy(login)
        waitForElementToBeClickable(locator)
        el(locator).click()
    }
}
