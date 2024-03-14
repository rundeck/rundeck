package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.common.navigation.NavProjectSettings
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

@CompileStatic
class ProjectEditPage extends BasePage {

    String loadPath = ""
    By descriptionInput = By.id("description")
    By labelInput = By.id("label")
    By saveButton = By.id("save")
    By editConfigurationFile = By.linkText("Edit Configuration File")
    By aceEditor = By.className("ace_text-input")
    By successMessageDiv = By.cssSelector(".alert.alert-info")
    By motdIdSelect = By.id("extraConfig.menuService.motdDisplay")
    By readmeSelect = By.id("extraConfig.menuService.readmeDisplay")

    ProjectEditPage(SeleniumContext context) {
        super(context)
    }

    def setProjectDescription(String projectDescription){
        (el descriptionInput).clear()
        (el descriptionInput).sendKeys(projectDescription)
    }

    def setProjectLabel(String projectLabel){
        (el labelInput).clear()
        (el labelInput).sendKeys(projectLabel)
    }

    def save(){
        (el saveButton).click()
    }

    def clickEditConfigurationFile(){
        (el editConfigurationFile).click()
    }

    def addConfigurationValue(String configurationValue){
        (el aceEditor).sendKeys(configurationValue)
    }

    def clickNavLink(NavProjectSettings navProjectSettings){
        (el By.linkText(navProjectSettings.getTabLink())).click()
    }

    /**
     * It clicks all of the checkboxes for the motd places
     */
    def selectAllMotdPlaces(){
        (els motdIdSelect).each {
            it.click()
        }
    }

    /**
     * When it saves it shows a message using css "alert alert-info"
     * When it fails to save, it shows a message using "alert alert-info"
     */
    def validateConfigFileSave(){
        new WebDriverWait(driver,  Duration.ofSeconds(10)).until(
                ExpectedConditions.textToBePresentInElementLocated(successMessageDiv, "configuration file saved")
        )
    }

    /**
     * It replaces the @original string by the @replacement string and it must be in edit configuration file screen
     * @param original
     * @param replacement
     */
    def replaceConfiguration(String original, String replacement){
        ((JavascriptExecutor) context.driver).executeScript("ace.edit('_id0').session.replace(ace.edit('_id0').find('${original}',{wrap: true, wholeWord: true }), '${replacement}');")
    }

    /**
     * This select all of the readme places checkboxes
     */
    def selectReadmeAllPlaces(){
        (els readmeSelect).each {it.click()}
    }
}
