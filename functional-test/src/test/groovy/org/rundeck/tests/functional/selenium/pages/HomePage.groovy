package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.FindBy
import org.rundeck.util.container.SeleniumContext

@CompileStatic
class HomePage extends BasePage {

    By appAdmin = By.id("appAdmin")
    By keyStorage = By.linkText("Key Storage")
    By navHome = By.id("nav-rd-home")

    HomePage(final SeleniumContext context) {
        super(context, "/")
    }

    void goToKeyStorage() {
        waitForElementVisible appAdminField
        appAdminField.click()
        keyStorageField.click()
    }

    WebElement getAppAdminField() {
        el appAdmin
    }

    WebElement getKeyStorageField() {
        el keyStorage
    }

    WebElement getNavHome() {
        waitForElementVisible navHome
        el navHome
    }

}
