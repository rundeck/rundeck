package org.rundeck.tests.functional.selenium.tests.appadmin

import org.rundeck.tests.functional.selenium.pages.appadmin.AccessControlPage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase

@SeleniumCoreTest
class AccessControlSpec extends SeleniumBase {

    def "upload requires file input"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            def aclPage = go AccessControlPage
            aclPage.uploadButton.click()
            aclPage.waitForModal 1
            aclPage.uploadSubmitButton.click()
        expect:
            aclPage.alertsFields.size() == 3
            aclPage.fileRequiredLabel.getText() == 'File is required'
            aclPage.namePolicyLabel.getText() == 'The policy name without file extension, can contain the characters: a-zA-Z0-9,.+_-'
            aclPage.nameRequiredLabel.getText() == 'Name is required'
    }

}
