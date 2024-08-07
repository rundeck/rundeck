package org.rundeck.util.gui.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.Select
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

/**
 * Job create page
 */
@CompileStatic
class JobCreatePage extends BasePage {

    By nodeFilterInputBy = By.id("schedJobNodeFilter")
    By selectNode = By.cssSelector(".glyphicon-circle-arrow-right")
    By lastNodeInList = By.cssSelector(".col-xs-6:nth-child(3) span:nth-child(2)")
    By refreshNodesBy = By.cssSelector(".refresh_nodes")
    By numberOfStepsBy = By.cssSelector(".autohilite.autoedit.wfitem.exectype")
    By notificationModalBy = By.cssSelector('#job-notifications-edit-modal')
    By notificationDropDownBy = By.cssSelector('#notification-edit-type-dropdown > button')
    By notificationSaveBy = By.id("job-notifications-edit-modal-btn-save")
    By updateJob = By.id("jobUpdateSaveButton")
    By jobNameInputBy = By.cssSelector("form input[name=\"jobName\"]")
    By groupPathInputBy = By.cssSelector("form input[name=\"groupPath\"]")
    By groupChooseButton = By.id("groupChooseModalBtn")
    By groupChooseModal = By.cssSelector('#groupChooseModal')
    By groupNameOption = By.cssSelector("span.groupname.jobgroupexpand")
    By descriptionTextareaBy = By.cssSelector("form textarea[name='description']")
    By jobGroupBy = By.cssSelector("input#schedJobGroup")
    By scheduleRunYesBy = By.cssSelector('input#scheduledTrue')
    By scheduleEveryDayCheckboxBy = By.cssSelector('input#everyDay')
    By scheduleDaysCheckboxDivBy = By.cssSelector('div#DayOfWeekDialog')
    By executionPluginsRows = By.xpath('//*[@id="tab_execution_plugins"]//*[@class="list-group-item"]')
    By killHandlerPluginPreviousRow = By.xpath('//input[@value="killhandler"]/preceding-sibling::div[1]')
    By killHandlerPluginCheckbox = By.xpath('//*[@value="killhandler"]//following-sibling::div[1]//input[@type="checkbox"]')
    By killHandlerPluginKillSpawnedCheckbox = By.xpath('//*[@value="killhandler"]//following-sibling::div[1]//div[2]//input[@type="checkbox"]')
    By multiExecFalseBy = By.cssSelector('input#multipleFalse')
    By multiExecTrueBy = By.cssSelector('input#multipleTrue')
    By workFlowStrategyBy = By.xpath('//*[@id="workflow.strategy"]')
    By deleteStepBy = By.xpath('//*[@title="Delete this step"]')
    By strategyPluginParallelBy = By.xpath('//*[@id="strategyPluginparallel"]')
    By strategyPluginParallelMsgBy = By.xpath('//*[@id="strategyPluginparallel"]/span/span')
    By strategyPluginSequentialBy = By.xpath('//*[@id="strategyPluginsequential"]')
    By strategyPluginSequentialMsgBy = By.xpath('//*[@id="strategyPluginsequential"]/span/span')
    By adhocRemoteStringBy = By.name("pluginConfig.adhocRemoteString")
    By floatBy = By.className("floatr")
    By createJobBy = By.id("Create")
    By cancelBy = By.id('createFormCancelButton')
    By optionBy = By.cssSelector("#optnewbutton > span")
    By nodeStepSectionActiveBy = By.cssSelector(".node_step_section.tab-pane.active")

    static class NextUi {
        static By jobNameInputBy = By.cssSelector("form input[id=\"schedJobName\"]")
        static By groupPathInputBy = By.cssSelector("form input[id=\"schedJobGroup\"]")
        static By descriptionTextareaBy = By.cssSelector("form textarea.ace_text-input")
        static By killHandlerPluginPreviousRow = By.xpath('//input[@value="killhandler"]/ancestor::div[@class="list-group-item"]/preceding-sibling::div[@class="list-group-item"][1]')
        static By killHandlerPluginCheckbox = By.xpath('//input[@value="killhandler"]')
        static By killHandlerPluginKillSpawnedCheckbox = By.xpath('//*[@data-prop-name="killChilds"]//input[@type="checkbox"]')
        static By optionBy = By.cssSelector("#optnewbutton > button")
        static By separatorOptionBy = By.cssSelector("#option_preview")
        static By optionCloseKeyStorageBy = By.cssSelector("#storage-file.modal .modal-footer > button.btn-default")
        static By optionOpenKeyStorageBy = By.cssSelector(".opt_sec_enabled div.input-group > .input-group-btn > button")
        static By optionUndoBy = By.cssSelector("[data-test=options_undo_redo] > button:nth-child(1)")
        static By optionRedoBy = By.cssSelector("[data-test=options_undo_redo] > button:nth-child(2)")
        static By optionRevertAllBy = By.cssSelector("[data-test=options_undo_redo] > button:nth-child(3)")
        static By defaultValueInput=By.cssSelector("[data-test='option.value'] input[name=defaultValue]")
        static By optionItemBy(int index) {
            By.cssSelector("#optitem_$index")
        }
        static By storagePathInput = By.name("storagePath")
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
    By notificationListBy = By.cssSelector(".flex-item.flex-grow-1")
    By nofiticationChildsBy = By.className("text-success")
    By updateBtn        = By.name("_action_Update")
    By defaultTabNodes  = By.id("tabSummary")
    By defaultTabOutput = By.id("tabOutput")
    By defaultTabHtml   = By.id("tabHTML")
    By schedulesCronTabBy = By.linkText("Crontab")
    By schedulesCrontabPanel = By.cssSelector("#cronstrtab .panel-body")
    By schedulesCrontabStringBy = By.name("crontabString")
    By stepFilterBy = By.xpath("//*[starts-with(@id, 'stepFilterField')]")
    By stepFilterSearchBy = By.linkText("Search")
    By emptyStepListBy = By.xpath("//*[@data-node-step-type='exec-command'][contains(@style, 'display: none;')]")
    By jobOptionListValuesBy = By.name("valuesList")
    By jobOptionListDelimiterBy = By.name("valuesListDelimiter")
    By jobOptionEnforcedBy = By.id("enforcedType_enforced")
    By jobOptionAllowedValuesRemoteUrlBy = By.xpath("//div[10]/div/div/div[2]/input")
    By jobOptionAllowedValuesRemoteUrlValueBy = By.name("valuesUrl")
    By jobOptionRequiredBy = By.id("option-required-yes")
    By jobOptionMultivaluedBy = By.xpath("//div[15]/div/div/div[2]/input")
    By jobOptionMultivaluedDelimiterBy = By.name("delimiter")
    By jobOptionMultiValuedAllSelectedBy = By.name("multivalueAllSelected")
    By duplicateWfStepBy = By.cssSelector(".glyphicon.glyphicon-duplicate")
    By urlOptionInput = By.xpath("//input[@name='valuesType' and @value='url']")
    By scriptTextAreaBy = By.xpath("//*[contains(@class, 'form-group ') and .//*[contains(text(), 'script to execute')]]")
    By wfItemEditFormBy = By.className("wfitemEditForm")
    By optDetailBy = By.cssSelector(".optdetail.autohilite.autoedit")
    By optionsBy = By.cssSelector(".opt.item")
    By timeZoneBy = By.id("timeZone")
    By optEditFormBy = By.className("optEditForm")

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

    void loadEditPath(String projectName, String jobId, Boolean nextUi = false) {
        this.edit=true
        this.projectName=projectName
        this.jobId=jobId
        this.nextUi=nextUi
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

    /**
     * It adds a command with the given string and it verifies that the stepIndexNumber is added
     * @param command
     * @param stepIndexNumber it starts from 0
     * @return
     */
    JobCreatePage addSimpleCommandStep(String command, int stepIndexNumber) {
        executeScript "window.location.hash = '#addnodestep'"
        stepLink 'exec-command', StepType.NODE click()
        byAndWaitClickable adhocRemoteStringBy
        adhocRemoteStringField.click()
        waitForNumberOfElementsToBeOne floatBy
        adhocRemoteStringField.sendKeys command
        saveStep stepIndexNumber

        return this
    }

    JobCreatePage addStep(JobStep step, int stepNumber = 0){
        tab(JobTab.WORKFLOW).click()
        stepLink(step.STEP_NAME, step.stepType).click()

        step.configure(this)

        saveStep stepNumber
        return this
    }

    JobCreatePage withName(String name){
        jobNameInput.sendKeys name
        return this
    }

    JobCreatePage addOption(JobOption option){
        tab(JobTab.WORKFLOW).click()
        option.configure(this)
        return this
    }

    JobCreatePage duplicateOption(String optName, int newIdx){
        tab(JobTab.WORKFLOW).click()
        JobOption.duplicate(this, optName, newIdx)
        return this
    }

    JobCreatePage addDefaultTab(String defaultTab){
        tab(JobTab.OTHER).click()
        driver.findElement(By.xpath("//input[@value='${defaultTab}']")).click();
        return this
    }

    JobShowPage saveJob() {
        if (loadPath.endsWith('create'))
            createJobButton.click()
        else
            updateJobButton.click()

        return new JobShowPage(context)
    }

    boolean commandStepVisible(){
        stepLink 'exec-command', StepType.NODE displayed
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(getLoadPath())) {
            throw new IllegalStateException("Not on job create page: " + driver.currentUrl)
        }
    }

    WebElement tab(JobTab tab) {
        def tabBy = By.partialLinkText(tab.getTabName())
        waitForNumberOfElementsToBeOne tabBy
        el tabBy
    }

    WebElement getLastNodeInListSpan(){
        el lastNodeInList
    }

    WebElement getSelectNodeArrowElement(){
        el selectNode
    }

    WebElement getNodeFilterInput(){
        el nodeFilterInputBy
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

    void waitGroupModal(Integer totalGroupModals) {
        waitForNumberOfElementsToBe groupChooseModal, totalGroupModals
    }

    WebElement getJobNameInput() {
        el nextUi ? NextUi.jobNameInputBy : jobNameInputBy
    }

    WebElement getGroupPathInput() {
        el nextUi ? NextUi.groupPathInputBy :groupPathInputBy
    }

    WebElement getGroupChooseButton() {
        el groupChooseButton
    }

    WebElement getGroupNameOption() {
        el groupNameOption
    }

    WebElement getDescriptionTextarea() {
        if(nextUi) {
            el NextUi.descriptionTextareaBy
        } else {
            def element = el descriptionTextareaBy
            String js = 'jQuery(\'form textarea[name="description"]\').show()'
            ((JavascriptExecutor) driver).executeScript(js, element)
            waitForElementVisible element
            element
        }
    }

    WebElement getRefreshNodesButton(){
        el refreshNodesBy
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

    List<WebElement> getExecutionPluginsRows() {
        driver.findElements(executionPluginsRows)
    }

    WebElement getKillHandlerPluginPreviousRow() {
        if(nextUi){
            new WebDriverWait(driver,  Duration.ofSeconds(50)).until(
                    ExpectedConditions.presenceOfElementLocated(NextUi.killHandlerPluginPreviousRow)
            )
            el NextUi.killHandlerPluginPreviousRow
        } else {
            el killHandlerPluginPreviousRow
        }
    }

    WebElement getKillHandlerPluginCheckbox() {
        if(nextUi){
            new WebDriverWait(driver,  Duration.ofSeconds(50)).until(
                    ExpectedConditions.presenceOfElementLocated(NextUi.killHandlerPluginCheckbox)
            )
            el NextUi.killHandlerPluginCheckbox
        } else {
            el killHandlerPluginCheckbox
        }
    }

    WebElement getKillHandlerPluginKillSpawnedCheckbox() {
        el nextUi ? NextUi.killHandlerPluginKillSpawnedCheckbox : killHandlerPluginKillSpawnedCheckbox
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
        if(stepType == StepType.WORKFLOW)
            workFlowStepLink.click()
        el By.xpath("//*[@${stepType.getStepType()}='$dataNodeStepType']")
    }

    WebElement getAdhocRemoteStringField() {
        el adhocRemoteStringBy
    }

    WebElement getCreateJobButton() {
        el createJobBy
    }

    void clickTimeZone(){
        (el timeZoneBy).click()
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
    By optionItemBy(int index) {
        nextUi ? NextUi.optionItemBy(index) : By.cssSelector("#optli_$index")
    }
    void waitFotOptLi(int index) {
        waitForElementVisible optionItemBy(index)
    }

    List<WebElement> optionLis(int index) {
        els optionItemBy(index)
    }

    void waitForOptionsToBe(int index, int total) {
        waitForNumberOfElementsToBe optionItemBy(index), total
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
        el nextUi ? NextUi.storagePathInput : storagePathInput
    }

    By getDefaultValueBy(){
        nextUi? NextUi.defaultValueInput:defaultValueInput
    }
    WebElement getDefaultValueInput(){
        el defaultValueBy
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
        el nextUi ? NextUi.optionRevertAllBy : optionRevertAllBy
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

    WebElement getUpdateBtn() {
        return el(updateBtn)
    }

    WebElement getDefaultTabNodes() {
        (el defaultTabNodes)
    }

    WebElement getDefaultTabOutput() {
        (el defaultTabOutput)
    }

    WebElement getDefaultTabHtml() {
        (el defaultTabHtml)
    }

    WebElement getSchedulesCrontab(){
        el schedulesCronTabBy
    }

    WebElement getSchedulesCrontabPanel(){
        el schedulesCrontabPanel
    }

    WebElement getSchedulesCrontabStringInput(){
        el schedulesCrontabStringBy
    }

    WebElement getStepFilterInput(){
        el stepFilterBy
    }

    WebElement getStepFilterSearchButton(){
        el stepFilterSearchBy
    }

    WebElement getEmptyStepList(){
        el emptyStepListBy
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

    WebElement getJobOptionListValueInput(){
        el jobOptionListValuesBy
    }

    WebElement getJobOptionListDelimiter(){
        el jobOptionListDelimiterBy
    }

    WebElement getJobOptionEnforcedInput(){
        el jobOptionEnforcedBy
    }

    WebElement getJobOptionAllowedValuesRemoteUrlInput(){
        el jobOptionAllowedValuesRemoteUrlBy
    }

    void scrollToElement(WebElement el){
        Actions actions = new Actions(driver);
        actions.moveToElement(el);
        actions.perform();
    }

    WebElement getJobOptionAllowedValuesRemoteUrlValueTextInput(){
        el jobOptionAllowedValuesRemoteUrlValueBy
    }

    WebElement getJobOptionRequiredInput(){
        el jobOptionRequiredBy
    }

    WebElement getJobOptionMultiValuedInput(){
        el jobOptionMultivaluedBy
    }

    WebElement getJobOptionMultivaluedDelimiter(){
        el jobOptionMultivaluedDelimiterBy
    }

    WebElement getJobOptionMultiValuedAllSelectedInput(){
        el jobOptionMultiValuedAllSelectedBy
    }

    WebElement getDuplicateWfStepButton(){
        el duplicateWfStepBy
    }

    WebElement getWfStepByListPosition(int position){
        (el By.id("wfitem_${position}"))
    }

    void saveStep(Integer stepNumber) {
        def button = el floatBy findElement By.cssSelector(".btn.btn-cta.btn-sm")
        executeScript "arguments[0].scrollIntoView(true);", button
        button?.click()
        waitForElementVisible By.id("wfitem_${stepNumber}")
    }

    def removeStepByIndex(int stepIndex){
        (els deleteStepBy).get(stepIndex).click()
    }

    def expectNumberOfStepsToBe(int numberSteps){
        new WebDriverWait(driver,  Duration.ofSeconds(5)).until(
                ExpectedConditions.numberOfElementsToBe(numberOfStepsBy, numberSteps)
        )
    }

    /**
     * It gets the list of notification elements added to the job
     */
    def getNotificationList(){
        (els notificationListBy)
    }

    /**
     * It gets the list of notification childs so it can validate each config value
     */
    def getNotificationChilds(WebElement notificationParent){
        notificationParent.findElements(nofiticationChildsBy)
    }

    void addScriptStep(String jobName, List<String> params, Integer waitTime = null) {
        jobNameInput.sendKeys jobName
        tab JobTab.WORKFLOW click()
        executeScript "window.location.hash = '#addnodestep'"
        stepLink 'script-inline', StepType.NODE click()
        waitForElementVisible scriptTextAreaBy
        params.each {scriptTextAreaInput.sendKeys(it)}
        wfItemEditForm.click()
        getWfItemIndex(0).isDisplayed()

        waitTime?.with {
            el(By.id("wfnewbutton")).findElement(By.cssSelector(".btn.btn-default.btn-sm.ready")).click()
            addSimpleCommandStep("sleep $it", 1)
            el(By.id("wfnewbutton")).findElement(By.cssSelector(".btn.btn-default.btn-sm.ready")).click()
            addSimpleCommandStep("echo \"after wait\"", 2)

            getWfItemIndex(1).isDisplayed()
            getWfItemIndex(2).isDisplayed()
        }

        createJobButton.click()
    }

    WebElement getScriptTextAreaInput() {
        el scriptTextAreaBy findElement By.tagName("textarea")
    }

    WebElement getWfItemEditForm() {
        el wfItemEditFormBy findElement By.cssSelector(".btn.btn-cta.btn-sm")
    }

    WebElement getWfItemIndex(int number) {
        el By.id("wfitem_${number}")
    }

    List<WebElement> getOptions(){
        els optionsBy
    }

    List<WebElement> getOptDetails(){
        els optDetailBy
    }

    def getTotalFoundPlugins(String pluginName){
        return (el nodeStepSectionActiveBy).findElements(By.name(pluginName)).size()
    }

}


class JobOption {
    static final String defaultOptType = 'Text'
    String optionType
    String inputType
    String name
    int optNumber

    void configure(JobCreatePage jobCreatePage){
        final By usageSectionLocator = By.xpath("//*[contains(@id,'${notUseDefaultType() ? optionType.toLowerCase() + "_" : ""}preview_')]//span[contains(.,' will be available to scripts in these forms')]")

        jobCreatePage.optionButton.click()
        jobCreatePage.optionName(optNumber).sendKeys(name)

        if(notUseDefaultType()){
            jobCreatePage.driver.findElement(By.name("optionType")).click();
            Select typeSelector = new Select(jobCreatePage.driver.findElement(By.name("optionType")))
            typeSelector.selectByVisibleText(optionType)
        }

        if(inputType)
            jobCreatePage.driver.findElement(By.cssSelector("input[value='${inputType}']")).click()

        jobCreatePage.waitForElementVisible(usageSectionLocator)
        jobCreatePage.executeScript("window.location.hash = '#workflowKeepGoingFail'")
        jobCreatePage.saveOptionButton.click()
        jobCreatePage.waitFotOptLi(optNumber)
    }

    boolean notUseDefaultType(){
        optionType && optionType != defaultOptType
    }

    static void duplicate(JobCreatePage jobCreatePage, String optName, int newIdx){
        jobCreatePage.duplicateButton(optName).click()
        jobCreatePage.waitFotOptLi(newIdx)
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