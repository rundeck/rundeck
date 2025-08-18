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
    By searchButtonBy = By.xpath("//button[@class='btn btn-default btn btn-secondary btn-sm' and @type='button' and normalize-space()='Search...']")
    By loginInputBy = By.xpath("//input[@class='form-control' and @placeholder='Login']")
    By modalSearchButtonBy = By.xpath("//button[contains(@class, 'btn btn-cta') and text()='Search']")

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

    WebElement getSearchButton(){
        el searchButtonBy
    }

    WebElement getLoginInput(){
        el loginInputBy
    }

    WebElement getModalSearchButton(){
        el modalSearchButtonBy
    }
}
