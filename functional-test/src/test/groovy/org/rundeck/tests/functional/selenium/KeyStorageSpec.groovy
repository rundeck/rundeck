package org.rundeck.tests.functional.selenium

import org.rundeck.tests.functional.selenium.pages.HomePage
import org.rundeck.tests.functional.selenium.pages.KeyStoragePage
import org.rundeck.tests.functional.selenium.pages.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class KeyStorageSpec extends SeleniumBase {

    def "1 add key storage"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            def homePage = page HomePage
            homePage.goToKeyStorage()
            def keyStoragePage = page KeyStoragePage
            keyStoragePage.waitForElementVisible keyStoragePage.addUploadKeyField
            keyStoragePage.addUploadKeyField.click()
            keyStoragePage.waitForModal 1
            keyStoragePage.addPasswordType 'root', 'git', 'git.pass'
            keyStoragePage.checkKeyExists 'git.pass', 'git'
    }

    def "2 overwrite key storage"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            def homePage = page HomePage
            homePage.goToKeyStorage()
            def keyStoragePage = page KeyStoragePage
            keyStoragePage.clickOverwriteKey 'git', 'git.pass'
            keyStoragePage.waitForModal 1
            keyStoragePage.overwriteKey 'new-root'
            homePage.navHome
    }

    def "3 delete key storage"() {
        when:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            def homePage = page HomePage
            homePage.goToKeyStorage()
            def keyStoragePage = page KeyStoragePage
            keyStoragePage.deleteKey 'git.pass', 'git'
    }

}
