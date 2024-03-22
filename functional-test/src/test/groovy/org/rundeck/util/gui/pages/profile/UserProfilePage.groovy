package org.rundeck.util.gui.pages.profile

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * User profile page
 */
@CompileStatic
class UserProfilePage extends BasePage {

    String loadPath = "/user/profile"

    By languageBy = By.cssSelector("#layoutBody div.form-inline label[for=language]")
    By editBy = By.linkText("Edit")
    By userLoginBy = By.cssSelector(".form-control-static")

    UserProfilePage(final SeleniumContext context) {
        super(context)
    }

    WebElement getLanguageLabel() {
        el languageBy
    }

    WebElement getEditLink() {
        el editBy
    }

    WebElement getUserLogin() {
        el userLoginBy
    }
}
