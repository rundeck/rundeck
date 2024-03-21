package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.common.navigation.NavProjectSettings
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

@CompileStatic
class ProjectEditPage extends BasePage {

    String loadPath = ""
    By nodeExecutorBy = By.partialLinkText("Default Node Executor")
    By nodeExecutorDropdown = By.cssSelector(".btn.btn-primary.dropdown-toggle")
    By openedNodeExecutorDropdown = By.cssSelector(".btn-group.btn-group-lg.open")
    By defaultKeyFilePathBy = By.name("nodeexec.default.config.keypath")
    By defaultKeyStoragePathBy = By.name("nodeexec.default.config.keystoragepath")
    By defaultPasswordStoragePathBy = By.name("nodeexec.default.config.passwordstoragepath")
    By defaultPassphraseStoragePathBy = By.name("nodeexec.default.config.passphrasestoragepath")
    By retryEnableBy = By.name("nodeexec.default.config.retryEnable")
    By retryCounterBy = By.name("nodeexec.default.config.retryCounter")
    By alwaysSetPtyBy = By.name("nodeexec.default.config.always-set-pty")
    By connectionTimeoutBy = By.name("nodeexec.default.config.ssh-connection-timeout")
    By sshCommandTimeoutBy = By.name("nodeexec.default.config.ssh-command-timeout")
    By sshBindAddressBy = By.name("nodeexec.default.config.ssh-bind-address")
    By passRdEnvBy = By.name("nodeexec.default.config.ssh-send-env")
    By scriptExecutorConfigCommandBy = By.name("nodeexec.default.config.command")
    By scriptExecutorConfigInterpreterBy = By.name("nodeexec.default.config.interpreter")
    By scriptExecutorConfigDirectoryBy = By.name("nodeexec.default.config.directory")
    By descriptionInput = By.id("description")
    By labelInput = By.id("label")
    By saveButton = By.id("save")
    By editConfigurationFile = By.linkText("Edit Configuration File")
    By aceEditor = By.className("ace_text-input")
    By successMessageDiv = By.cssSelector(".alert.alert-info")
    By motdIdSelect = By.id("extraConfig.menuService.motdDisplay")
    By readmeSelect = By.id("extraConfig.menuService.readmeDisplay")
    By execModeCheck = By.name("extraConfig.scheduledExecutionService.disableExecution")
    By scheduleModeCheck = By.name("extraConfig.scheduledExecutionService.disableSchedule")
    By aceEditorText = By.cssSelector(".ace_layer.ace_text-layer")
    By dangerMessageContainer = By.cssSelector(".alert.alert-danger")

    ProjectEditPage(SeleniumContext context) {
        super(context)
    }

    void loadProjectEditForProject(String projectName){
        this.loadPath = "/project/${projectName}/configure"
    }

    def setProjectDescription(String projectDescription){
        (el descriptionInput).clear()
        (el descriptionInput).sendKeys(projectDescription)
    }

    def clearProjectDescription(){
        (el descriptionInput).clear()
    }

    def clearProjectLabel(){
        (el labelInput).clear()
    }

    def clearProjectDescriptionInput(){
        (el descriptionInput).clear()
    }

    def setProjectLabel(String projectLabel){
        (el labelInput).clear()
        (el labelInput).sendKeys(projectLabel)
    }

    def clearProjectLabelInput(){
        (el labelInput).clear()
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

    def getDefaultNodeExecutorLink(){
        el nodeExecutorBy
    }

    def getNodeExecutorDropdown(){
        el nodeExecutorDropdown
    }

    List<WebElement> getExecutors(){
        driver.findElement(By.id("tab_svc_NodeExecutor")).findElement(By.cssSelector(".form-group.spacing-lg")).findElements(By.tagName("a"))
    }

    def getSshListItem(){
        driver.findElement(By.id("tab_svc_NodeExecutor")).findElement(By.cssSelector(".form-group.spacing-lg")).findElement(By.linkText("SSH"))
    }

    def getLocalExecutorListItem(){
        driver.findElement(By.id("tab_svc_NodeExecutor")).findElement(By.cssSelector(".form-group.spacing-lg")).findElement(By.linkText("Local"))
    }

    def getScriptExecutorListItem(){
        driver.findElement(By.id("tab_svc_NodeExecutor")).findElement(By.cssSelector(".form-group.spacing-lg")).findElement(By.linkText("Script Execution"))
    }

    def getDefaultConfigKeyStoragePathInput(){
        el defaultKeyStoragePathBy
    }

    def getDefaultPasswordStoragePath(){
        el defaultPasswordStoragePathBy
    }

    def getDefaultPassphraseStoragePath(){
        el defaultPassphraseStoragePathBy
    }

    def getAlwaysSetPty(){
        el alwaysSetPtyBy
    }

    def getConnectionTimeout(){
        el connectionTimeoutBy
    }

    def getSshCommandTimeout(){
        el sshCommandTimeoutBy
    }

    def getSshBindAddress(){
        el sshBindAddressBy
    }

    def getPassRdEnv(){
        el passRdEnvBy
    }

    def getDefaultKeyFilepath(){
        el defaultKeyFilePathBy
    }

    def getDangerMessages(){
        driver.findElements(dangerMessageContainer)
    }

    def getScriptExecutorConfigCommandInput(){
        el scriptExecutorConfigCommandBy
    }

    def getScriptExecutorConfigInterpreter(){
        el scriptExecutorConfigInterpreterBy
    }

    def getScriptExecutorConfigDirectory(){
        el scriptExecutorConfigDirectoryBy
    }

    def getRetryEnableInput(){
        el retryEnableBy
    }

    def getRetryCounterInput(){
        el retryCounterBy
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

    /**
     * It clicks on "Disable Execution" check
     * it does not validate for it to be enabled or disabled
     */
    def clickExecutionMode(){
        (el execModeCheck).click()
    }

    /**
     * It clicks on "Disable Schedule" check
     * it does not validate for it to be enabled or disabled
     */
    def clickScheduleMode() {
        (el scheduleModeCheck).click()
    }

    def changeConfigValue(String configToChange, String replacement){
        def configLines = (el aceEditorText).text

        def newLine = "${configToChange}=${replacement}"
        def newConfigString = configLines.split('\n').collect { line ->
            def parts = line.split('=')
            if (parts.size() == 2) {
                def key = parts[0].trim()
                if (key == configToChange) {
                    return newLine
                }
            }
            return line
        }.join('\n')

        (el aceEditor).sendKeys(Keys.chord(Keys.CONTROL, "a"))
        (el aceEditor).sendKeys(Keys.BACK_SPACE)
        (el aceEditor).sendKeys(newConfigString)
    }
}
