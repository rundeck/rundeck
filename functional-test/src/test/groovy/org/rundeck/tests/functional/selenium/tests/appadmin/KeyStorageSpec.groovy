package org.rundeck.tests.functional.selenium.tests.appadmin

import org.rundeck.tests.functional.selenium.pages.appadmin.KeyStoragePage
import org.rundeck.tests.functional.selenium.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class KeyStorageSpec extends SeleniumBase {

    def "add key storage"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            def keyStoragePage = go KeyStoragePage
            keyStoragePage.waitForElementVisible keyStoragePage.addUploadKeyField
            keyStoragePage.addUploadKeyField.click()
            keyStoragePage.waitForModal 1
            keyStoragePage.addPasswordType 'root', 'git', 'git.pass'
            keyStoragePage.checkKeyExists 'git.pass', 'git'
    }

    def "overwrite key storage"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            def keyStoragePage = go KeyStoragePage
            keyStoragePage.clickOverwriteKey 'git', 'git.pass'
            keyStoragePage.waitForModal 1
            keyStoragePage.overwriteKey 'new-root'
    }

    def "delete key storage"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            def keyStoragePage = go KeyStoragePage
            keyStoragePage.deleteKey 'git.pass', 'git'
    }

}
