package org.rundeck.util.gui.pages.profile

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * User profile page
 */
@CompileStatic
class UserProfilePage extends BasePage {

    String loadPath = "/user/profile"

    By languageBy = By.cssSelector("#layoutBody div.form-inline label[for=language]")
    By editBy = By.linkText("Edit")
    By userLoginBy = By.cssSelector(".form-control-static")
    By genTokenButtonBy = By.xpath("//a[@class='btn btn-default btn-xs' and @data-toggle='modal' and @href='#gentokenmodal']")
    By modalGenerateNewTokenHeaderBy = By.xpath("//h4[@class='modal-title' and @id='gentokenLabel']")
    By tokenTimeInputBy = By.xpath("//input[@type='number' and @name='tokenTime' and @class='form-control']")
    By tokenNameInputBy = By.xpath("//input[@type='text' and @name='tokenName' and @class='form-control']")
    By modalGenerateTokenBtnBy = By.xpath("//a[@class='genusertokenbtn btn btn-cta' and normalize-space()='Generate New Token']")
    By timeUntilBy = By.xpath("//span[@class='timeuntil']")
    By modalCloseBtnBy = By.xpath("//button[@class='btn btn-danger' and @type='button' and normalize-space()='Close']")
    By deleteTokenButtonBy = By.xpath("//a[@title='Delete']")
    By modalDeleteInputBy = By.xpath("//input[@type='submit' and @class='btn btn-danger yes' and @value='Delete']")



    UserProfilePage(final SeleniumContext context) {
        super(context)
    }

    WebElement getLanguageLabel() {
        el languageBy
    }

    WebElement getEditLink() {
        el editBy
    }

    WebElement getUserLogin() {
        el userLoginBy
    }

    WebElement getGenTokenButton(){
        el genTokenButtonBy
    }

    WebElement getModalGenerateNewTokenHeader(){
        el modalGenerateNewTokenHeaderBy
    }

    WebElement getTokenTimeInput(){
        el tokenTimeInputBy
    }

    WebElement getModalGenerateTokenBtnBy(){
        el modalGenerateTokenBtnBy
    }

    WebElement getTimeUntil(){
        el timeUntilBy
    }

    WebElement getModalCloseBtn(){
        el modalCloseBtnBy
    }

    WebElement getDeleteTokenButton(){
        el deleteTokenButtonBy
    }

    WebElement  getModalDeleteInput(){
        el modalDeleteInputBy
    }

    WebElement getTokenNameInput(){
        el tokenNameInputBy
    }


}
