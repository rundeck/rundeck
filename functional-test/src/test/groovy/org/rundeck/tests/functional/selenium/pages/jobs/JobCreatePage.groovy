package org.rundeck.tests.functional.selenium.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * Job create page
 */
@CompileStatic
class JobCreatePage extends BasePage {

    By notificationModalBy = By.cssSelector('#job-notifications-edit-modal')
    By notificationDropDownBy = By.cssSelector('#notification-edit-type-dropdown > button')
    By notificationSaveBy = By.id("job-notifications-edit-modal-btn-save")
    By updateJob = By.id("jobUpdateSaveButton")
    By jobNameInputBy = By.cssSelector("form input[name=\"jobName\"]")
    By groupPathInputBy = By.cssSelector("form input[name=\"groupPath\"]")
    By descriptionTextareaBy = By.cssSelector("form textarea[name='description']")
    By jobGroupBy = By.cssSelector("input#schedJobGroup")
    By scheduleRunYesBy = By.cssSelector('input#scheduledTrue')
    By scheduleEveryDayCheckboxBy = By.cssSelector('input#everyDay')
    By scheduleDaysCheckboxDivBy = By.cssSelector('div#DayOfWeekDialog')
    By multiExecFalseBy = By.cssSelector('input#multipleFalse')
    By multiExecTrueBy = By.cssSelector('input#multipleTrue')
    By workFlowStrategyBy = By.xpath('//*[@id="workflow.strategy"]')
    By strategyPluginParallelBy = By.xpath('//*[@id="strategyPluginparallel"]')
    By strategyPluginParallelMsgBy = By.xpath('//*[@id="strategyPluginparallel"]/span/span')
    By strategyPluginSequentialBy = By.xpath('//*[@id="strategyPluginsequential"]')
    By strategyPluginSequentialMsgBy = By.xpath('//*[@id="strategyPluginsequential"]/span/span')
    By adhocRemoteStringBy = By.name("pluginConfig.adhocRemoteString")
    By floatBy = By.className("floatr")
    By createJobBy = By.id("Create")
    By cancelBy = By.id('createFormCancelButton')
    By optionBy = By.cssSelector("#optnewbutton > span")

    static class NextUi {
        static By optionBy = By.cssSelector("#optnewbutton > button")
        static By separatorOptionBy = By.cssSelector("#option_preview")
        static By optionCloseKeyStorageBy = By.cssSelector("#storage-file.modal .modal-footer > button.btn-default")
        static By optionOpenKeyStorageBy = By.cssSelector(".opt_sec_enabled div.input-group > .input-group-btn > button")
        static By optionUndoBy = By.cssSelector("[data-test=options_undo_redo] > button:nth-child(1)")
        static By optionRedoBy = By.cssSelector("[data-test=options_undo_redo] > button:nth-child(2)")
        static By defaultValueInput=By.cssSelector("[data-test='option.value'] input[name=defaultValue]")
    }

    By separatorOptionBy = By.xpath("//*[@id[contains(.,'preview_')]]//span[contains(.,'The option values will be available to scripts in these forms')]")
    By saveOptionBy = By.xpath("//*[@title[contains(.,'Save the new option')]]")
    By nodeDispatchTrueBy = By.id("doNodedispatchTrue")
    By nodeFilterLinkBy = By.cssSelector("#job_edit__node_filter_include .job_edit__node_filter__filter_select_dropdown")
    By nodeFilterSelectAllLinkBy = By.cssSelector("#job_edit__node_filter_include .job_edit__node_filter__filter_select_all")
    By nodeMatchedCountBy = By.xpath("//span[@class='text-info node_filter_results__matched_nodes_count']")
    By excludeFilterTrueBy = By.xpath("//*[@id='excludeFilterTrue']")
    By editableFalseBy = By.xpath("//*[@id='editableFalse']")
    By schedJobNodeThreadCountBy = By.xpath("//*[@id='schedJobnodeThreadcount']")
    By schedJobNodeRankAttributeBy = By.xpath("//*[@id='schedJobnodeRankAttribute']")
    By nodeRankOrderDescendingBy = By.xpath("//*[@id='nodeRankOrderDescending']")
    By nodeKeepGoingTrueBy = By.xpath("//*[@id='nodeKeepgoingTrue']")
    By successOnEmptyNodeFilterTrueBy = By.xpath("//*[@id='successOnEmptyNodeFilterTrue']")
    By nodesSelectedByDefaultFalseBy = By.xpath("//*[@id='nodesSelectedByDefaultFalse']")
    By orchestratorDropdownBy = By.cssSelector('#orchestrator-edit-type-dropdown > button')
    By errorAlertBy = By.cssSelector('#error')
    By formValidationAlertBy = By.cssSelector('#page_job_edit > div.list-group-item > div.alert.alert-danger')
    By sessionSectionBy = By.xpath("//div[contains(@class, 'opt_sec_nexp_disabled')]")
    By secureInputTypeBy = By.xpath("//input[contains(@value, 'secureExposed')]")
    By storagePathInput = By.name("defaultStoragePath")
    By defaultValueInput = By.id("opt_defaultValue")
    By optionOpenKeyStorageBy = By.cssSelector(".btn.btn-default.obs-select-storage-path")
    By optionCloseKeyStorageBy = By.xpath("//button[@class='btn btn-sm btn-default']")
    By optionUndoBy = By.xpath("//*[@id='optundoredo']/div/span[1]")
    By optionRedoBy = By.xpath("//*[@id='optundoredo']/div/span[2]")
    By optionRevertAllBy = By.xpath("//*[starts-with(@id,'revertall')]")
    By optionConfirmRevertAllBy = By.cssSelector("div[class='popover-content'] span[class*='confirm']")
    By workFlowStepBy = By.linkText("Workflow Steps")
    By ansibleBinariesPathBy = By.name("pluginConfig.ansible-binaries-dir-path")
    By autocompleteSuggestionsBy = By.cssSelector("div[class='autocomplete-suggestions']")
    By wfUndoButtonBy = By.xpath("//*[@id='wfundoredo']/div/span[1]")
    By wfUndoButtonLinkBy = By.xpath("//*[@class='btn btn-xs btn-default act_undo flash_undo']")
    By wfRedoButtonBy = By.xpath("//*[@id='wfundoredo']/div/span[2]")
    By wfRedoButtonLinkBy = By.xpath("//*[@class='btn btn-xs btn-default act_redo flash_undo']")
    By wfRevertAllButtonBy = By.xpath("//*[@id='wfundoredo']/div/span[3]")
    By revertWfConfirmBy = By.xpath('//*[starts-with(@id,"popover")]/div[2]/span[2]')
    By listWorkFlowItemBy = By.xpath("//*[starts-with(@id,'wfitem_')]")
    By addSimpleCommandStepBy = By.xpath("//span[contains(@onclick, 'wfnewbutton')]")

    private String loadPath = "/job/create"
    String projectName
    String jobId
    boolean edit=false
    @Override
    String getLoadPath() {
        if(edit && projectName && jobId){
            return "/project/${projectName}/job/edit/${jobId}${nextUi?'?nextUi=true':''}"
        }else if(projectName && !edit){
            return "/project/${projectName}/job/create${nextUi?'?nextUi=true':''}"
        }else{
            return loadPath
        }
    }
    boolean nextUi = false

    JobCreatePage(final SeleniumContext context) {
        super(context)
    }

    JobCreatePage(final SeleniumContext context, String projectName) {
        super(context)
        loadCreatePath(projectName)
    }

    void loadEditPath(String projectName, String jobId) {
        this.edit=true
        this.projectName=projectName
        this.jobId=jobId
    }

    void loadCreatePath(String projectName) {
        this.projectName=projectName
        this.edit=false
    }

    void fillBasicJob(String name) {
        jobNameInput.sendKeys name
        tab JobTab.WORKFLOW click()
        addSimpleCommandStep 'echo selenium test', 0
    }

    void addSimpleCommandStep(String command, int number) {
        executeScript "window.location.hash = '#addnodestep'"
        stepLink 'exec-command', StepType.NODE click()
        waitForElementToBeClickable adhocRemoteStringField
        adhocRemoteStringField.click()
        waitForNumberOfElementsToBeOne floatBy
        adhocRemoteStringField.sendKeys command
        saveStep number
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(getLoadPath())) {
            throw new IllegalStateException("Not on job create page: " + driver.currentUrl)
        }
    }

    WebElement tab(JobTab tab) {
        def tabBy = By.linkText(tab.getTabName())
        waitForNumberOfElementsToBeOne tabBy
        el tabBy
    }

    WebElement addNotificationButtonByType(NotificationEvent notificationType) {
        el notificationType.notificationEvent
    }

    WebElement getNotificationDropDown() {
        el notificationDropDownBy
    }

    WebElement notificationByType(NotificationType notificationType) {
        el notificationType.notificationType
    }

    WebElement notificationConfigByPropName(String propName) {
        def popBy = By.cssSelector('#notification-edit-config div.form-group[data-prop-name=\'' + propName + '\']')
        waitForNumberOfElementsToBeOne popBy
        el popBy findElement By.cssSelector('input[type=text]')
    }

    WebElement getNotificationSaveButton() {
        el notificationSaveBy
    }

    WebElement getJobGroupField() {
        waitForElementVisible jobGroupBy
        el jobGroupBy
    }

    WebElement getUpdateJobButton() {
        el updateJob
    }

    void waitNotificationModal(Integer totalNotificationModals) {
        waitForNumberOfElementsToBe notificationModalBy, totalNotificationModals
    }

    WebElement getJobNameInput() {
        el jobNameInputBy
    }

    WebElement getGroupPathInput() {
        el groupPathInputBy
    }

    WebElement getDescriptionTextarea() {
        def element = el descriptionTextareaBy
        String js = 'jQuery(\'form textarea[name="description"]\').show()'
        ((JavascriptExecutor) driver).executeScript(js, element)
        waitForElementVisible element
        element
    }

    WebElement getScheduleRunYesField() {
        el scheduleRunYesBy
    }

    WebElement getScheduleEveryDayCheckboxField() {
        el scheduleEveryDayCheckboxBy
    }

    WebElement getScheduleDaysCheckboxDivField() {
        el scheduleDaysCheckboxDivBy
    }

    WebElement getMultiExecFalseField() {
        el multiExecFalseBy
    }

    WebElement getMultiExecTrueField() {
        el multiExecTrueBy
    }

    WebElement getWorkFlowStrategyField() {
        el workFlowStrategyBy
    }

    WebElement getStrategyPluginParallelField() {
        el strategyPluginParallelBy
    }

    WebElement getStrategyPluginParallelMsgField() {
        el strategyPluginParallelMsgBy
    }

    WebElement stepLink(String dataNodeStepType, StepType stepType) {
        el By.xpath("//*[@${stepType.getStepType()}='$dataNodeStepType']")
    }

    WebElement getAdhocRemoteStringField() {
        el adhocRemoteStringBy
    }

    WebElement getCreateJobButton() {
        el createJobBy
    }

    WebElement getCancelButton() {
        el cancelBy
    }

    WebElement getOptionButton() {
        el nextUi ? NextUi.optionBy : optionBy
    }

    WebElement optionNameNew(int index=0) {
        if(nextUi){
            return byAndWait (By.cssSelector("#optitem_new input[type=text][name=name]"))
        }else{
            return optionName(index)
        }
    }
    WebElement optionName(int index) {
        byAndWait nextUi?
                  By.cssSelector("#optitem_$index div.optEditForm input[type=text][name=name]"):
                  By.cssSelector("#optvis_$index > div.optEditForm input[type=text][name=name]")
    }

    WebElement getSeparatorOption() {
        el nextUi ? NextUi.separatorOptionBy : separatorOptionBy
    }

    WebElement getSaveOptionButton() {
        el saveOptionBy
    }

    void waitFotOptLi(int index) {
        waitForElementVisible nextUi?
                              By.cssSelector("#optitem_$index"):
                              By.cssSelector("#optli_$index")
    }

    List<WebElement> optionLis(int index) {
        els By.cssSelector("#optli_$index")
    }

    void waitForOptionsToBe(int index, int total) {
        waitForNumberOfElementsToBe By.cssSelector("#optli_$index"), total
    }

    WebElement optionNameSaved(int index) {
        el nextUi?
           By.cssSelector("#optitem_${index} .option-item .option-item-content .optdetail_name")
           :By.xpath("//*[@id=\"optli_$index\"]/div/div/span[2]/span/span[1]/span[1]")
    }

    WebElement duplicateButton(String nameOpt, int optNum = 0) {
        el nextUi?
           By.cssSelector("#optitem_${optNum} .option-item-content+.btn-group> .btn + .btn")
           :By.xpath("//*[@id='optctrls_$nameOpt']/span[2]")
    }

    WebElement getNodeDispatchTrueCheck() {
        el nodeDispatchTrueBy
    }

    WebElement getNodeFilterLinkButton() {
        el nodeFilterLinkBy
    }

    WebElement getNodeFilterSelectAllLinkButton() {
        el nodeFilterSelectAllLinkBy
    }

    WebElement getNodeMatchedCountField() {
        el nodeMatchedCountBy
    }

    WebElement getExcludeFilterTrueCheck() {
        el excludeFilterTrueBy
    }

    WebElement getEditableFalseCheck() {
        el editableFalseBy
    }

    WebElement getSchedJobNodeThreadCountField() {
        el schedJobNodeThreadCountBy
    }

    WebElement getSchedJobNodeRankAttributeField() {
        el schedJobNodeRankAttributeBy
    }

    WebElement getNodeRankOrderDescendingField() {
        el nodeRankOrderDescendingBy
    }

    WebElement getNodeKeepGoingTrueCheck() {
        el nodeKeepGoingTrueBy
    }

    WebElement getSuccessOnEmptyNodeFilterTrueCheck() {
        el successOnEmptyNodeFilterTrueBy
    }

    WebElement getNodesSelectedByDefaultFalseCheck() {
        el nodesSelectedByDefaultFalseBy
    }
    
    WebElement getOrchestratorDropdownButton() {
        el orchestratorDropdownBy
    }

    WebElement getSessionSectionLabel() {
        el sessionSectionBy
    }

    WebElement getSecureInputTypeRadio() {
        el secureInputTypeBy
    }

    WebElement getStoragePathInput(){
        el storagePathInput
    }

    WebElement getDefaultValueInput(){
        el nextUi? NextUi.defaultValueInput:defaultValueInput
    }

    WebElement getOptionOpenKeyStorageButton() {
        el nextUi?
           NextUi.optionOpenKeyStorageBy
           :optionOpenKeyStorageBy
    }

    WebElement getOptionCloseKeyStorageButton() {
        el nextUi?
           NextUi.optionCloseKeyStorageBy
           :optionCloseKeyStorageBy
    }

    WebElement getOptionUndoButton() {
        el nextUi ? NextUi.optionUndoBy : optionUndoBy
    }

    WebElement getOptionRedoButton() {
        el nextUi ? NextUi.optionRedoBy : optionRedoBy
    }

    WebElement getOptionRevertAllButton() {
        el optionRevertAllBy
    }

    WebElement getOptionConfirmRevertAllButton() {
        el optionConfirmRevertAllBy
    }

    WebElement orchestratorChoiceLink(String variable) {
        el By.cssSelector("#orchestrator-edit-type-dropdown > ul > li > a[role=button][data-plugin-type=$variable]")
    }

    WebElement getErrorAlert() {
        el errorAlertBy
    }

    WebElement getFormValidationAlert() {
        el formValidationAlertBy
    }

    WebElement getWorkFlowStepLink() {
        el workFlowStepBy
    }

    WebElement getAnsibleBinariesPathField() {
        el ansibleBinariesPathBy
    }

    WebElement getWfUndoButton() {
        el wfUndoButtonBy
    }

    WebElement getWfUndoButtonLink() {
        el wfUndoButtonLinkBy
    }

    WebElement getWfRedoButton() {
        el wfRedoButtonBy
    }

    WebElement getWfRedoButtonLink() {
        el wfRedoButtonLinkBy
    }

    WebElement getWfRevertAllButton() {
        el wfRevertAllButtonBy
    }

    WebElement getRevertWfConfirmYes() {
        el revertWfConfirmBy
    }

    List<WebElement> getWorkFlowList() {
        els listWorkFlowItemBy
    }

    WebElement getAddSimpleCommandStepButton() {
        el addSimpleCommandStepBy
    }

    WebElement getAutocompleteSuggestions() {
        def autoAux = els autocompleteSuggestionsBy
        WebElement autoDivAux
        for (WebElement auto : autoAux) {
            if (auto.isDisplayed()) {
                autoDivAux = auto
                break
            }
        }
        autoDivAux.findElement By.cssSelector("div[class='autocomplete-suggestion']")
    }

    void saveStep(Integer stepNumber) {
        def button = el floatBy findElement By.cssSelector(".btn.btn-cta.btn-sm")
        executeScript "arguments[0].scrollIntoView(true);", button
        button?.click()
        waitForElementVisible By.id("wfitem_${stepNumber}")
    }

}

enum NotificationType {

    MAIL(By.cssSelector('#notification-edit-type-dropdown > ul > li > a[data-plugin-type=\'email\']')),
    WEBHOOK(By.cssSelector('#notification-edit-type-dropdown > ul > li > a[data-plugin-type=\'url\']'))

    private By notificationTypeBy

    NotificationType(By notificationTypeBy) {
        this.notificationTypeBy = notificationTypeBy
    }

    By getNotificationType() {
        notificationTypeBy
    }
}

enum NotificationEvent {
    SUCCESS(By.cssSelector('#job-notifications-onsuccess > .list-group-item:first-child > button')),
    START(By.cssSelector('#job-notifications-onstart > .list-group-item:first-child > button')),
    FAILURE(By.cssSelector('#job-notifications-onfailure > .list-group-item:first-child > button')),
    RETRY(By.cssSelector('#job-notifications-onretryablefailure > .list-group-item:first-child > button')),
    AVERAGE(By.cssSelector('#job-notifications-onavgduration > .list-group-item:first-child > button'))

    private By notificationEventBy

    NotificationEvent(By notificationEventBy) {
        this.notificationEventBy = notificationEventBy
    }

    By getNotificationEvent() {
        notificationEventBy
    }
}

enum StepType {
    NODE("data-node-step-type"),
    WORKFLOW("data-step-type")

    private String stepType

    StepType(String stepType) {
        this.stepType = stepType
    }

    String getStepType() {
        return stepType
    }
}

enum StepName {
    COMMAND("command"),
    SCRIPT("script")

    private String stepName

    StepName(String stepName) {
        this.stepName = stepName
    }

    String getStepName() {
        return stepName
    }
}

enum JobTab {

    DETAILS("Details"),
    WORKFLOW("Workflow"),
    NODES("Nodes"),
    SCHEDULE("Schedule"),
    NOTIFICATIONS("Notifications"),
    EXECUTION_PLUGINS("Execution Plugins"),
    OTHER("Other")

    private String tabName

    JobTab(String tabName) {
        this.tabName = tabName
    }

    String getTabName() {
        return tabName
    }
}
