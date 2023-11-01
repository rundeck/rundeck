package org.rundeck.tests.functional.selenium.pages.appadmin

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * Access Control page
 */
@CompileStatic
class AccessControlPage extends BasePage {

    By uploadBy = By.cssSelector("#storage_acl_upload_btn")
    By uploadSubmitBy = By.cssSelector("button#aclStorageUpload_submit_btn")
    By alertsBy = By.xpath("//*[@id='aclStorageUpload_content']//*[@class='help-block']")
    By fileRequiredBy = By.xpath("//*[@id=\"aclStorageUpload_content\"]/div[1]/div/span")
    By namePolicyBy = By.xpath("//*[@id=\"aclStorageUpload_content\"]/div[2]/div/span[1]")
    By nameRequiredBy = By.xpath("//*[@id=\"aclStorageUpload_content\"]/div[2]/div/span[2]")

    String loadPath = "/menu/acls"

    AccessControlPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on access control page: " + driver.currentUrl)
        }
    }

    WebElement getUploadButton() {
        el uploadBy
    }

    WebElement getUploadSubmitButton() {
        el uploadSubmitBy
    }

    List<WebElement> getAlertsFields() {
        els alertsBy
    }

    WebElement getFileRequiredLabel() {
        el fileRequiredBy
    }

    WebElement getNamePolicyLabel() {
        el namePolicyBy
    }

    WebElement getNameRequiredLabel() {
        el nameRequiredBy
    }

}
