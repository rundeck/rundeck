package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
/**
 * Login page
 */
@CompileStatic
class LoginPage extends BasePage {
    def user = 'admin' //Default user
    def pass = 'admin123' //Default pass

    By loginFieldBy = By.id("login")
    By passwordFieldBy = By.id("password")
    By loginBtnBy = By.id("btn-login")
    By errorBy = By.cssSelector(".alert.alert-danger > span")

    static final String PAGE_PATH = "/user/login"

    LoginPage(final SeleniumContext context) {
        super(context, "/")
    }

    void validatePage() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on login page: " + driver.currentUrl)
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

    WebElement getError() {
        el errorBy
    }

    void login(String username, String password) {
        loginField.sendKeys(username)
        passwordField.sendKeys(password)
        loginBtn.click()
    }

    void login() {
        loginField.sendKeys(user as CharSequence)
        passwordField.sendKeys(pass as CharSequence)
        loginBtn.click()
    }
}
