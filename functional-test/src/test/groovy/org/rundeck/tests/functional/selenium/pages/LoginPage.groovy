package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

/**
 * Login page
 */
@CompileStatic
class LoginPage extends BasePage {
    By loginFieldBy = By.id("login")
    By passwordFieldBy = By.id("password")
    By loginBtnBy = By.id("btn-login")

    public static final String PAGE_PATH = "/user/login"

    LoginPage(final WebDriver driver) {
        super(driver)
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on login page")
        }
    }

    WebElement getLoginField() {
        el loginFieldBy
    }

    WebElement getPasswordField() {
        el passwordFieldBy
    }

    WebElement getLoginBtn() {
        el loginBtnBy
    }

    void login(String username, String password) {
        loginField.sendKeys(username)
        passwordField.sendKeys(password)
        loginBtn.click()
    }
}
