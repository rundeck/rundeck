package org.rundeck.tests.functional.selenium.pages.profile

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * User profile page
 */
@CompileStatic
class UserProfilePage extends BasePage {

    String loadPath = "/user/profile"

    By languageBy = By.cssSelector("#layoutBody div.form-inline label[for=language]")

    UserProfilePage(final SeleniumContext context) {
        super(context)
    }

    WebElement getLanguageLabel() {
        el languageBy
    }
}
