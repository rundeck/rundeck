package org.rundeck.util.gui.pages.project

import org.openqa.selenium.By
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class ReadmePage extends BasePage{

    String loadPath = ""
    By readmeAceEditor = By.cssSelector(".ace_text-input")
    By readmeSaveButton = By.cssSelector(".btn.btn-cta.reset_page_confirm")

    ReadmePage(SeleniumContext context) {
        super(context)
    }

    def setReadmeMessage(String readmeMessage){
        (el readmeAceEditor).sendKeys(readmeMessage)
    }

    def save(){
        (el readmeSaveButton).click()
    }

}
