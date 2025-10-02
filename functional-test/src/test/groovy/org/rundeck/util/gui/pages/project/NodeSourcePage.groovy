package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

@CompileStatic
class NodeSourcePage extends BasePage {

    String loadPath = ""

    By newNodeSource = By.xpath("//button[contains(.,'Add a new Node Source')]")
    By saveNodeSourceConfigBy = By.cssSelector(".btn.btn-cta.btn-xs")
    By saveButtonBy = By.cssSelector(".btn.btn-cta")
    By nodesEditTabBy = By.xpath("//div[contains(text(),'Edit')]")
    By modifyBy = By.linkText("Modify")
    By configurationSavedPopUpBy = By.xpath("//*[contains(text(),'Configuration Saved')]")

    NodeSourcePage(final SeleniumContext context) {
        super(context)
    }

    WebElement getNewNodeSourceButton() {
        el newNodeSource
    }

    void validatePage() {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.urlContains(loadPath))
    }

    void forProject(String projectName){
        this.loadPath = "/project/${projectName}/nodes/sources"
    }

    void clickSaveNodeSourceConfig(){
        (el saveNodeSourceConfigBy).click()
    }

    void clickSaveNodeSources(){
        (el saveButtonBy).click()
    }

    def clickNodesEditTab(){
        (el nodesEditTabBy).click()
    }

    def clickModifyButton(){
        (el modifyBy).click()
    }
}
