package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

class JobCreatePage extends BasePage {

    static final String PAGE_PATH = "/job/create"
    static final String JOB_EDIT_PATH = '/project/$PROJECT/job/edit/$ID'

    By jobNameField = By.id('schedJobName')
    By jobCreateButton = By.id("Create")
    By jobSaveButton = By.id("jobUpdateSaveButton")
    By notificationModal = By.cssSelector('#job-notifications-edit-modal')
    By notificationDropDown = By.cssSelector('#notification-edit-type-dropdown > button')
    By notificationSaveButton = By.id("job-notifications-edit-modal-btn-save")
    By jobDefinitionModal = By.cssSelector('a[href="#job-definition-modal"]')
    By notificationDefinition = By.cssSelector('#detailtable.tab-pane > div.row > div.col-sm-12.table-responsive > table.table.item_details> tbody > tr > td.container > div.row > div.col-sm-12 > div.overflowx')
    By jobAddStepButton = By.cssSelector("#wfnewbutton > .btn")
    By notificationDefinitionBy = By.cssSelector('#detailtable.tab-pane > div.row > div.col-sm-12.table-responsive > table.table.item_details> tbody > tr > td.container > div.row > div.col-sm-12 > div.overflowx')
    By addStepButtonsPanel = By.cssSelector('.add_step_buttons.panel-body')

    String loadPath = PAGE_PATH

    JobCreatePage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(PAGE_PATH)) {
            throw new IllegalStateException("Not on Job Create page: " + driver.currentUrl)
        }
    }

    void goJobEditPage(String projectName, String jobId){
        driver.get(context.client.baseUrl+JOB_EDIT_PATH.replaceAll('\\$PROJECT', projectName).replaceAll('\\$ID', jobId))
    }
    void validateJobEditPage() {
        if (!driver.currentUrl.contains('/job/edit/')) {
            throw new IllegalStateException("Not on Job Edit page: " + driver.currentUrl)
        }
    }


    WebElement getJobNameField(){
        el jobNameField
    }

    WebElement getTab(JobTab tab){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.numberOfElementsToBe(By.linkText(tab.getTabName()), 1))
        el By.linkText(tab.getTabName())
    }

    WebElement getStepByType(StepName stepName, StepType stepType){
        el By.xpath("//*[@${stepType.getStepType()}='${stepName.getStepName()}']")
    }

    WebElement getAddStepButtonsPanel(){
        el addStepButtonsPanel
    }

    WebElement getCreateButton(){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.elementToBeClickable(jobCreateButton))
        el jobCreateButton
    }
    WebElement getJobSaveButton(){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.elementToBeClickable(jobSaveButton))
        el jobSaveButton
    }

    WebElement getAddNotificationButtonByType(NotificationEvent notificationType){
        el notificationType.getNotificationEvent()
    }

    WebElement getNotificationModal(){
        el notificationModal
    }

    WebElement getNotificationDropDown(){
        el notificationDropDown
    }

    WebElement getNotificationByType(NotificationType notificationType){
        el notificationType.getNotificationType()
    }

    WebElement getNotificationConfigByPropName(String propName){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.numberOfElementsToBe(By.cssSelector('#notification-edit-config div.form-group[data-prop-name=\'' + propName + '\']'), 1))
        def groupConfig = el By.cssSelector('#notification-edit-config div.form-group[data-prop-name=\'' + propName + '\']')
        groupConfig.findElement(By.cssSelector('input[type=text]'))
    }

    WebElement getNotificationSaveButton(){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.attributeContains(driver.findElement(notificationSaveButton), "class", "btn btn-primary"))
        el notificationSaveButton
    }

    void waitForJobShow(){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.urlContains("/job/show/"))
    }

    WebElement getJobDefinitionModal(){
        el jobDefinitionModal
    }

    WebElement getNotificationDefinition(){
        el notificationDefinitionBy
    }

    WebElement getSaveStepButton(){
        WebElement stepSaveButton = el By.className("floatr")
        stepSaveButton.findElement(By.cssSelector(".btn.btn-cta.btn-sm"))
    }

    WebElement getJobAddStepButton(){
        el jobAddStepButton
    }

    void waitForSavedStep(Integer stepNumber){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.numberOfElementsToBe(By.id("wfitem_${stepNumber}"), stepNumber+1))
    }

    void waitForStepToBeShown(By stepElementToWait){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.numberOfElementsToBe(stepElementToWait, 1))
    }

    void waitForModal(Integer totalModals){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.numberOfElementsToBe(notificationModal, totalModals))
    }

    void waitForAddNewStepBtn(By jobAddStepButton){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.elementToBeClickable(jobAddStepButton))
    }

    void waitForURL(String url){
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.urlContains(url))
    }
}

enum NotificationType {

    MAIL(By.cssSelector('#notification-edit-type-dropdown > ul > li > a[data-plugin-type=\'email\']')),
    WEBHOOK(By.cssSelector('#notification-edit-type-dropdown > ul > li > a[data-plugin-type=\'url\']'))

    private By notificationType

    NotificationType(By notificationType) {
        this.notificationType = notificationType
    }

    By getNotificationType() {
        return notificationType
    }
}

enum NotificationEvent {
    SUCCESS(By.cssSelector('#job-notifications-onsuccess > .list-group-item:first-child > button')),
    START(By.cssSelector('#job-notifications-onstart > .list-group-item:first-child > button')),
    FAILURE(By.cssSelector('#job-notifications-onfailure > .list-group-item:first-child > button')),
    RETRY(By.cssSelector('#job-notifications-onretryablefailure > .list-group-item:first-child > button')),
    AVERAGE(By.cssSelector('#job-notifications-onavgduration > .list-group-item:first-child > button'))

    private By notificationEvent

    NotificationEvent(By notificationEvent) {
        this.notificationEvent = notificationEvent
    }

    By getNotificationEvent() {
        return notificationEvent
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
    COMMAND("exec-command"),
    SCRIPT("script-inline")

    private String stepName

    StepName(String stepName) {
        this.stepName = stepName
    }

    String getStepName() {
        return stepName
    }
}

public enum JobTab {

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