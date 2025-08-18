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

    def "validate default token duration and modification"() {
        when: "User navigates to the User Profile Page and generates a token"
        def userProfilePage = go UserProfilePage
        userProfilePage.waitForElementToBeClickable(userProfilePage.genTokenButton)
        userProfilePage.genTokenButton.click()
        userProfilePage.waitForElementVisible(userProfilePage.modalGenerateNewTokenHeader)

        then: "The default token duration should be '0'"
        userProfilePage.waitForTextToBePresentInElement(userProfilePage.tokenTimeInput, "0")

        and: "User generates a token with name 'TEST_TOKEN_1'"
        userProfilePage.tokenNameInput.clear()
        userProfilePage.tokenNameInput.sendKeys("TEST_TOKEN_1")
        userProfilePage.modalGenerateTokenBtnBy.click()
        userProfilePage.waitForElementToBeClickable(userProfilePage.modalCloseBtn)
        userProfilePage.modalCloseBtn.click()

        and: "The token's time until expiration should be shown as '29d'"
        userProfilePage.waitForElementVisible(userProfilePage.timeUntil)
        assert userProfilePage.timeUntil.getText().contains("29d")

        and: "User deletes the generated token"
        userProfilePage.deleteTokenButton.click()
        userProfilePage.waitForElementToBeClickable(userProfilePage.modalDeleteInput)
        userProfilePage.modalDeleteInput.submit()

        when: "User navigates to the User Profile Page and generates another token"
        userProfilePage.genTokenButton.click()
        userProfilePage.waitForElementVisible(userProfilePage.modalGenerateNewTokenHeader)

        then: "User modifies the default token duration to '50m'"
        userProfilePage.tokenNameInput.clear()
        userProfilePage.tokenNameInput.sendKeys("TEST_TOKEN_2")
        userProfilePage.waitForTextToBePresentInElement(userProfilePage.tokenTimeInput, "0")
        userProfilePage.tokenTimeInput.clear() // Clear the input before setting new value
        userProfilePage.tokenTimeInput.sendKeys("50m")
        userProfilePage.modalGenerateTokenBtnBy.click()
        userProfilePage.waitForElementToBeClickable(userProfilePage.modalCloseBtn)
        userProfilePage.modalCloseBtn.click()

        and: "The token's time until expiration should show 'm' (minutes)"
        userProfilePage.waitForElementVisible(userProfilePage.timeUntil)
        assert userProfilePage.timeUntil.getText().contains("m")

        and: "User deletes the modified token"
        userProfilePage.deleteTokenButton.click()
        userProfilePage.waitForElementToBeClickable(userProfilePage.modalDeleteInput)
        userProfilePage.modalDeleteInput.submit()
    }
}
