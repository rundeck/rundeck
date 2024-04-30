package org.rundeck.util.gui.pages.usersummary

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

class UserSummaryPage extends BasePage{

    String loadPath = "/menu/userSummary"
    By userCountFieldBy = By.className("text-info")

    UserSummaryPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on user summary page: " + driver.currentUrl)
        }
    }

    WebElement getUserCountField() {
        el userCountFieldBy
    }
}
