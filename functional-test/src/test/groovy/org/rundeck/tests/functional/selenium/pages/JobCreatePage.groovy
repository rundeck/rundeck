package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

import org.rundeck.util.container.SeleniumContext

import java.time.Duration

class JobCreatePage extends BasePage {

    By jobName = By.id('schedJobName')
    By jobCreateButtonBy = By.id("Create")
    By commandBy = By.id("adhocRemoteStringField")
    By floatBy = By.className("floatr")
    By btnBy = By.cssSelector(".btn.btn-cta.btn-sm")
    By notificationModalBy = By.cssSelector('#job-notifications-edit-modal')
    By notificationDropDownBy = By.cssSelector('#notification-edit-type-dropdown > button')
    By notificationSaveBy = By.id("job-notifications-edit-modal-btn-save")
    By updateJob = By.id("jobUpdateSaveButton")
    By jobNameInputBy = By.cssSelector("form input[name=\"jobName\"]")
    By groupPathInputBy = By.cssSelector("form input[name=\"groupPath\"]")
    By descriptionTextareaBy = By.cssSelector("form textarea[name='description']")
    By descriptionText = By
            .xpath("//*[@class=\"section-space\"]//*[@class=\"h5 text-strong\"]")
    By jobGroupBy = By.cssSelector("input#schedJobGroup")
    By jobInfoGroupBy = By.cssSelector('div.jobInfoSection a.text-secondary')
    By scheduleRunYesBy = By.cssSelector('input#scheduledTrue')
    By scheduleEveryDayCheckboxBy = By.cssSelector('input#everyDay')
    By scheduleDaysCheckboxDivBy = By.cssSelector('div#DayOfWeekDialog')
    By multiExecFalseBy = By.cssSelector('input#multipleFalse')
    By multiExecTrueBy = By.cssSelector('input#multipleTrue')

    String loadPath = "/job/create"


    JobCreatePage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(loadPath)) {
            throw new IllegalStateException("Not on jobs list page: " + driver.currentUrl)
        }
    }

    void createSimpleJob(String jobName, String command) {
        jobNameField.click()
        jobNameField.sendKeys(jobName)
        tab JobTab.WORKFLOW click()
        selectStep StepName.COMMAND, StepType.NODE
        waitForElementVisible commandBy
        commandField.click()
        commandField.sendKeys(command ?: "echo \"This is a Sample Job\"")
        saveStep 0
    }

    void selectStep(StepName stepName, StepType stepType) {
        def step = el By.xpath("//*[@${stepType.getStepType()}='${stepName.getStepName()}']")
        step.click()
        waitForNumberOfElementsToBe floatBy
    }

    void saveStep(Integer stepNumber) {
        def aux = floatField.findElement btnBy
        aux.click()
        waitForElementVisible By.id("wfitem_" + stepNumber)
    }

    WebElement getJobNameField() {
        el jobName
    }

    WebElement tab(JobTab tab) {
        def tabBy = By.linkText(tab.getTabName())
        waitForNumberOfElementsToBe tabBy
        el tabBy
    }

    WebElement getCreateButton() {
        new WebDriverWait(driver, Duration.ofSeconds(5)).until(ExpectedConditions.elementToBeClickable(jobCreateButtonBy))
        el jobCreateButtonBy
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

    WebElement getFloatField() {
        el floatBy
    }

    WebElement notificationConfigByPropName(String propName) {
        def popBy = By.cssSelector('#notification-edit-config div.form-group[data-prop-name=\'' + propName + '\']')
        waitForNumberOfElementsToBe popBy
        el popBy findElement By.cssSelector('input[type=text]')
    }

    WebElement getNotificationSaveButton() {
        el notificationSaveBy
    }

    WebElement getCommandField() {
        el commandBy
    }

    WebElement getJobGroupField() {
        waitForElementVisible jobGroupBy
        el jobGroupBy
    }

    WebElement getJobInfoGroupLabel() {
        waitForElementVisible jobInfoGroupBy
        el jobInfoGroupBy
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

    WebElement getDescriptionTextLabel() {
        el descriptionText
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
