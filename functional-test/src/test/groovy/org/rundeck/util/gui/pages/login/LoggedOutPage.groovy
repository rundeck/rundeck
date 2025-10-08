package org.rundeck.util.gui.pages.login

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

@CompileStatic
class LoggedOutPage extends BasePage {

    String loadPath = "/user/loggedout"
    By loginAgainFieldBy = By.partialLinkText("Log In Again")


    LoggedOutPage(final SeleniumContext context) {
        super(context)
    }

    WebElement getLoginAgainField() {
        el loginAgainFieldBy
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on logged out  page: " + driver.currentUrl)
        }
    }

}
