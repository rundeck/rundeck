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

    public static final String TEST_USER = System.getenv("RUNDECK_TEST_USER") ?: "admin"
    public static final String TEST_PASS = System.getenv("RUNDECK_TEST_PASS") ?: "admin123"

    def loginPage = page LoginPage
    def homePage = page HomePage
    def keyStoragePage = page KeyStoragePage

    def "1 add key storage"() {
        when:
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            homePage.goToKeyStorage()
            keyStoragePage.waitForElementVisible keyStoragePage.addUploadKeyField
            keyStoragePage.addUploadKeyField.click()
            keyStoragePage.waitForModal 1
            keyStoragePage.addPasswordType 'root', 'git', 'git.pass'
            keyStoragePage.checkKeyExists 'git.pass', 'git'
    }

    def "2 overwrite key storage"() {
        when:
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            homePage.goToKeyStorage()
            keyStoragePage.clickOverwriteKey 'git', 'git.pass'
            keyStoragePage.waitForModal 1
            keyStoragePage.overwriteKey 'new-root'
            homePage.navHome
    }

    def "3 delete key storage"() {
        when:
            loginPage.login(TEST_USER, TEST_PASS)
        then:
            homePage.goToKeyStorage()
            keyStoragePage.deleteKey 'git.pass', 'git'
    }

}
