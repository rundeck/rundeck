package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
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
    private static final String FILE_COPIER_CONFIG_PREFIX = "fcopy.default.config."
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
    By executionHistoryTabBy = By.partialLinkText("Execution History Clean")
    By enableCleanExecutionHistoryBy = By.id("nullenable_cleaner_input")
    By daysToKeepExecsBy = By.id("cleanperiod")
    By minimumExecsToKeepBy = By.id("minimumtokeep")
    By cronScheduleCleanerBy = By.id("cronTextField")
    By fileCopierDivBy = By.id("tab_svc_FileCopier")
    By dropDownButtonBy = By.cssSelector(".btn.btn-primary.dropdown-toggle")

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

    def enableCleanExecutionHistory(){
        (el executionHistoryTabBy).click()
        (el enableCleanExecutionHistoryBy).click()
        waitForElementVisible(el daysToKeepExecsBy)
    }

    def configureCleanExecutionHistory(int daysToKeepExecs, int minimumExecsToKeep, String cronSchedule){
        (el daysToKeepExecsBy).clear()
        (el daysToKeepExecsBy).sendKeys(daysToKeepExecs.toString())
        (el minimumExecsToKeepBy).clear()
        (el minimumExecsToKeepBy).sendKeys(minimumExecsToKeep.toString())
        (el cronScheduleCleanerBy).clear()
        (el cronScheduleCleanerBy).sendKeys(cronSchedule)
        (el minimumExecsToKeepBy).click()
    }

    def getSelectedDefaultFileCopier(){
        (el fileCopierDivBy)
    }

    def clickFileCopierDropDown(){
        (el fileCopierDivBy).findElement(dropDownButtonBy).click()
    }

    List<WebElement> getFileCopierList(){
        (el fileCopierDivBy).findElement(By.cssSelector(".form-group.spacing-lg")).findElements(By.tagName("a"))
    }

    def selectFileCopier(FileCopierEnum fileCopier){
        (el fileCopierDivBy).findElement(By.linkText(fileCopier.getName())).click()
    }

    /**
     * Given input (type text, radio or select) props, sets each value that match with its selector.
     *
     * @param props
     */
    def setFileCopierValues(
            Map<String,String> properties,
            List<String> clickableProps = null,
            Map<String,String> selectableProps = null
    ){
        if( clickableProps ){
            clickableProps.each {
                scrollToElement((el By.name("${FILE_COPIER_CONFIG_PREFIX}${it}")))
                (el By.name("${FILE_COPIER_CONFIG_PREFIX}${it}")).click()
            }
        }
        if( selectableProps ){
            selectableProps.each {
                scrollToElement((el By.name("${FILE_COPIER_CONFIG_PREFIX}${it.key}")))
                new Select((el By.name("${FILE_COPIER_CONFIG_PREFIX}${it.key}"))).selectByValue(it.value)
            }
        }
        properties.each {
            scrollToElement((el By.name("${FILE_COPIER_CONFIG_PREFIX}${it.key}")))
            (el By.name("${FILE_COPIER_CONFIG_PREFIX}${it.key}")).clear()
            (el By.name("${FILE_COPIER_CONFIG_PREFIX}${it.key}")).sendKeys(it.value)
        }
    }

    /**
     * Scroll to a given element.
     *
     * @param el
     */
    void scrollToElement(WebElement el){
        Actions actions = new Actions(driver)
        actions.moveToElement(el)
        actions.perform()
    }

    /**
     * Given input (type text, radio or select) props, returns a map containing
     * the given keys and its value from GUI's input.
     *
     * @param props
     */
    def getFileCopierConfig(
            LinkedHashMap<String, String> props,
            Iterable<String> clickableProps = null,
            LinkedHashMap<String, String> selectableProps = null
    ) {
        def guiConfig = [:]
        props.each {
            guiConfig.put(it.key, (el By.name("${FILE_COPIER_CONFIG_PREFIX}${it.key}")).getAttribute("value"))
        }
        if( clickableProps ){
            clickableProps.each {
                guiConfig.put(it, (el By.name("${FILE_COPIER_CONFIG_PREFIX}${it}")).selected.toString())
            }
        }
        if ( selectableProps ){
            selectableProps.each {
                guiConfig.put(it.key, new Select((el By.name("${FILE_COPIER_CONFIG_PREFIX}${it.key}"))).getFirstSelectedOption().text)
            }
        }
        return guiConfig
    }

    enum FileCopierEnum {
        SCP("SCP"),
        SSHJ_SCP("SSHJ-SCP"),
        STUB("Stub"),
        SCRIPT_EXECUTION("Script Execution"),
        ANSIBLE_FILE_COPIER("Ansible File Copier"),
        WINRM_FILE_COPIER("WinRM Python File Copier"),
        OPENSSH("openssh / file-copier")

        final String name
        FileCopierEnum(String name){
            this.name = name
        }
    }
}
