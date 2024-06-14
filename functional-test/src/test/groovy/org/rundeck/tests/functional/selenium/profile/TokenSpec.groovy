package org.rundeck.tests.functional.selenium.profile

import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.gui.pages.login.LoginPage
import org.rundeck.util.gui.pages.profile.UserProfilePage

@SeleniumCoreTest
class TokenSpec extends SeleniumBase{

    def setup(){
        def loginPage = go LoginPage
        loginPage.login(TEST_USER, TEST_PASS)
    }

    def "validate default token duration is 0"(){
        when:
        def userProfilePage = go UserProfilePage
        userProfilePage.waitForElementToBeClickable(userProfilePage.genTokenButton)
        userProfilePage.genTokenButton.click()
        userProfilePage.waitForElementVisible(userProfilePage.modalGenerateNewTokenHeader)

        then:
        userProfilePage.waitForTextToBePresentInElement(userProfilePage.tokenTimeInput,"0")
        userProfilePage.tokenNameInput.clear()
        userProfilePage.tokenNameInput.sendKeys("TEST_TOKEN_1")
        userProfilePage.modalGenerateTokenBtnBy.click()
        userProfilePage.waitForElementToBeClickable (userProfilePage.modalCloseBtn)
        userProfilePage.modalCloseBtn.click()
        userProfilePage.waitForElementVisible(userProfilePage.timeUntil)
        userProfilePage.timeUntil.getText().contains("29d")
        userProfilePage.deleteTokenButton.click()
        userProfilePage.waitForElementToBeClickable(userProfilePage.modalDeleteInput)
        userProfilePage.modalDeleteInput.submit()
    }

    def "validate default token duration can be modified"(){
        when:
        def userProfilePage = go UserProfilePage
        userProfilePage.waitForElementToBeClickable(userProfilePage.genTokenButton)
        userProfilePage.genTokenButton.click()
        userProfilePage.waitForElementVisible(userProfilePage.modalGenerateNewTokenHeader)

        then:
        userProfilePage.tokenNameInput.clear()
        userProfilePage.tokenNameInput.sendKeys("TEST_TOKEN_2")
        userProfilePage.waitForTextToBePresentInElement(userProfilePage.tokenTimeInput,"0")
        userProfilePage.tokenTimeInput.sendKeys("50m")
        userProfilePage.modalGenerateTokenBtnBy.click()
        userProfilePage.waitForElementToBeClickable (userProfilePage.modalCloseBtn)
        userProfilePage.modalCloseBtn.click()
        userProfilePage.waitForElementVisible(userProfilePage.timeUntil)
        userProfilePage.timeUntil.getText().contains("m")
    }
}
