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
    By uploadFileBy = By.cssSelector("#aclStorageUploadForm input#aclStorageUpload_input_file")
    By uploadNameBy = By.cssSelector("#aclStorageUploadForm input#aclStorageUpload_input_name")
    By alertBy = By.cssSelector(".alert.alert-danger")
    By uploadedPolicyValidationTitleBy = By.cssSelector("#uploadedPolicyValidation .alert h4")
    By policiesTitleBy = By.cssSelector("span.h4 > span[data-bind=\"text: name\"]")
    By aclUploadOverwriteBy = By.cssSelector("#aclStorageUploadForm input#acl_upload_overwrite")
    By storedPoliciesListBy = By.cssSelector("#storedPolicies_list")
    By storedPoliciesHeaderBy = By.cssSelector("#storedPolicies_header")
    By deleteModalBy = By.cssSelector("#deleteStorageAclPolicy")

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

    WebElement getUploadFileField() {
        el uploadFileBy
    }

    WebElement getUploadNameField() {
        el uploadNameBy
    }

    WebElement getAlertField() {
        el alertBy
    }

    List<WebElement> getAlertFields() {
        els alertBy
    }

    WebElement getUploadedPolicyValidationTitleField() {
        el uploadedPolicyValidationTitleBy
    }

    List<WebElement> getPoliciesTitleList() {
        el storedPoliciesListBy findElements policiesTitleBy
    }

    WebElement getActionDropdown() {
        el storedPoliciesListBy findElement By.cssSelector("div:nth-of-type(1)") findElement By.cssSelector(" a[data-toggle='dropdown']")
    }

    WebElement getDeleteButton() {
        actionDropdown.findElement By.xpath('./..') findElement By.cssSelector("a.acl_menu__action_delete")
    }

    List<WebElement> getOverwriteHelpField() {
        el aclUploadOverwriteBy findElement By.xpath("./../..") findElements By.cssSelector("span.help-block")
    }

    WebElement getCountBadge() {
        el storedPoliciesHeaderBy findElement By.cssSelector("h3 .badge")
    }

    WebElement getDeleteModal() {
        el deleteModalBy
    }

    WebElement getDeleteButtonConfirm() {
        deleteModal.findElement By.cssSelector("#deleteStorageAclPolicy_btn_0")
    }

}
