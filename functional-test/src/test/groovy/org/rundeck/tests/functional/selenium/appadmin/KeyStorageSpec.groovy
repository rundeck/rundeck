package org.rundeck.tests.functional.selenium.appadmin

import org.rundeck.util.gui.pages.appadmin.KeyStoragePage
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.common.WaitingTime
import org.rundeck.util.container.SeleniumBase
import spock.lang.Stepwise

@SeleniumCoreTest
@Stepwise
class KeyStorageSpec extends SeleniumBase {

    def "add key storage"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def keyStoragePage = go KeyStoragePage
        when:
            keyStoragePage.waitForElementVisible keyStoragePage.addUploadKeyField
            keyStoragePage.addUploadKeyField.click()
        then:
            keyStoragePage.waitForModal 1
            keyStoragePage.addPasswordType 'root', 'git', 'git.pass'
            sleep WaitingTime.MODERATE.duration.toMillis()
            keyStoragePage.waitForModal 0
            keyStoragePage.checkKeyExists 'git.pass', 'git'
    }

    def "overwrite key storage"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
            def keyStoragePage = go KeyStoragePage
        when:
            keyStoragePage.refresh()
            keyStoragePage.clickOverwriteKey 'git', 'git.pass'
            keyStoragePage.waitForModal 1
        then:
            keyStoragePage.overwriteKey 'new-root'
    }

    def "delete key storage"() {
        setup:
            def loginPage = go LoginPage
            loginPage.login(TEST_USER, TEST_PASS)
        when:
            def keyStoragePage = go KeyStoragePage
        then:
            keyStoragePage.refresh()
            keyStoragePage.deleteKey 'git.pass', 'git'
    }

}
