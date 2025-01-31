package org.rundeck.tests.functional.selenium.appadmin

import org.openqa.selenium.By
import org.rundeck.util.gui.pages.appadmin.AccessControlPage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class AccessControlSpec extends SeleniumBase {

    def validPolicyData = '{context: {"application":"rundeck"}, description: "test",for:{resource: [ { deny:["xyz"]}]}, by: {group: "DNE_test"}}'
    def validPolicyName = 'test-valid-policy-name'

    def "upload requires file input"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def aclPage = go AccessControlPage
        when:
            aclPage.uploadButton.click()
            aclPage.waitForModal 1, By.cssSelector(".modal.in")
            aclPage.uploadSubmitButton.click()
        then:
            aclPage.alertsFields.size() == 3
            aclPage.fileRequiredLabel.getText() == 'File is required'
            aclPage.namePolicyLabel.getText() == 'The policy name without file extension, can contain the characters: a-zA-Z0-9,.+_-'
            aclPage.nameRequiredLabel.getText() == 'Name is required'
    }

    def "upload invalid acl content"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def aclPage = go AccessControlPage
        when:
            aclPage.uploadButton.click()
            aclPage.waitForModal 1, By.cssSelector(".modal.in")
            aclPage.uploadSubmitButton.click()
            aclPage.uploadNameField.sendKeys 'some-file-name'
            aclPage.uploadFileField.sendKeys createTempYamlFile('invalid acl content test data')
            aclPage.uploadSubmitButton.click()
        then:
            aclPage.validatePage()
            aclPage.alertField.getText().contains 'Validation failed'
            aclPage.uploadedPolicyValidationTitleField.getText() == 'Uploaded File failed ACL Policy Validation:'
    }

    def "upload valid acl content succeeds"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def aclPage = go AccessControlPage
        when:
            aclPage.uploadButton.click()
            aclPage.waitForModal 1, By.cssSelector(".modal.in")
            aclPage.uploadSubmitButton.click()
            aclPage.uploadFileField.sendKeys createTempYamlFile(validPolicyData)
            aclPage.uploadNameField.clear()
            aclPage.uploadNameField.sendKeys validPolicyName
            aclPage.uploadSubmitButton.click()
        then:
            aclPage.validatePage()
            aclPage.policiesTitleList.size() == 1
            aclPage.policiesTitleList.get 0 getText() equals validPolicyName
    }

    def "upload form warns of duplicate name"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def aclPage = go AccessControlPage
        when:
            aclPage.uploadButton.click()
            aclPage.waitForModal 1, By.cssSelector(".modal.in")
            aclPage.uploadSubmitButton.click()
            aclPage.uploadFileField.sendKeys createTempYamlFile(validPolicyData)
            aclPage.uploadNameField.clear()
            aclPage.uploadNameField.sendKeys validPolicyName
            aclPage.uploadSubmitButton.click()
        then:
            aclPage.overwriteHelpField.size() == 1
            aclPage.overwriteHelpField.get 0 getText() equals "A Policy already exists with the specified name"
    }

    def "delete acl policy"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def aclPage = go AccessControlPage
        when:
            aclPage.actionDropdown.click()
            aclPage.deleteButton.click()
            aclPage.deleteModal.isDisplayed()
            aclPage.deleteButtonConfirm.click()
        then:
            aclPage.alertFields.size() == 0
            aclPage.countBadge.getText() == "0"
            aclPage.policiesTitleList.size() == 0
    }

    private def createTempYamlFile(String text) {
        def tempFile = File.createTempFile("temp", ".yaml")
        tempFile.text = text
        tempFile.deleteOnExit()
        return tempFile.absolutePath
    }

}
