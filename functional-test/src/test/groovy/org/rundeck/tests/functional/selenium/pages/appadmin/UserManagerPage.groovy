package org.rundeck.tests.functional.selenium.pages.appadmin

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * User manager page
 */
@CompileStatic
class UserManagerPage extends BasePage {

    String loadPath = ""
    By manageLocalUsersTabBy = By.xpath("//a[text()='Manage Local Users']")
    By addUserButtonBy = By.xpath("//button[text()='Add User']")
    By userNameInputBy = By.xpath("//input[@placeholder='Username']")
    By passwordInputBy = By.xpath("//input[@placeholder='Password']")
    By confirmPasswordInputBy = By.xpath("//input[@placeholder='Confirm Password']")
    By firstNameInputBy = By.xpath("//input[@placeholder='First Name']")
    By lastNameInputBy = By.xpath("//input[@placeholder='Last Name']")
    By emailInputBy = By.xpath("//input[@placeholder='Email']")
    By saveUserButtonBy = By.xpath("//button[text()='Save']")

    UserManagerPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.containsIgnoreCase("userManager")) {
            throw new IllegalStateException("Not on user manager page: " + driver.currentUrl)
        }
    }

    void addLocalUser(String userName, String password, String firstName, String lastName, String email) {
        el manageLocalUsersTabBy click()
        el addUserButtonBy click()
        el userNameInputBy sendKeys(userName)
        el passwordInputBy sendKeys(password)
        el confirmPasswordInputBy sendKeys(password)
        el firstNameInputBy sendKeys(firstName)
        el lastNameInputBy sendKeys(lastName)
        el emailInputBy sendKeys(email)
        el saveUserButtonBy click()
    }

    boolean userNameExists(String userName) {
        el By.xpath('//*[@class="column-data"]//*[text()="' + userName + '"]')
    }
}
