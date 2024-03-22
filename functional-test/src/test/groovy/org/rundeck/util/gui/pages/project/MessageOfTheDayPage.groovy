package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

@CompileStatic
class MessageOfTheDayPage extends BasePage {

    String loadPath = ""
    By motdAceEditor = By.cssSelector(".ace_text-input")
    By motdSaveButton = By.cssSelector(".btn.btn-cta.reset_page_confirm")
    By motdToolTip = By.cssSelector(".vue-project-motd.motd-indicator")
    By motdMessage = By.cssSelector(".motd-content.full")
    By motdMessageHome = By.className("markdown-body")

    MessageOfTheDayPage(SeleniumContext context) {
        super(context)
    }

    def setMessageOfTheDay(String motd){
        (el motdAceEditor).sendKeys(motd)
    }

    def save(){
        (el motdSaveButton).click()
    }

    def clickMOTD(){
        (el motdToolTip).click()
    }

    def waitForMessageShownInProject(String message){
        new WebDriverWait(driver,  Duration.ofSeconds(10)).until(
                ExpectedConditions.textToBe(motdMessage, message)
        )
    }

    def waitForMessageShownInHome(String message){
        new WebDriverWait(driver,  Duration.ofSeconds(30)).until(
                ExpectedConditions.numberOfElementsToBeMoreThan(motdMessageHome, 0)
        )
        new WebDriverWait(driver,  Duration.ofSeconds(30)).until(
                ExpectedConditions.textToBe(motdMessageHome, message)
        )
    }
}
