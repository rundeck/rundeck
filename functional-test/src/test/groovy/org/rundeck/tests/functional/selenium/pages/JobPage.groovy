package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.setup.StepType

class JobPage extends BasePage {

    By jobNameBy = By.id("schedJobName")
    By workflowBy = By.linkText("Workflow")
    By commandBy = By.id("adhocRemoteStringField")
    By createBy = By.id("Create")
    By floatBy = By.className("floatr")
    By btnBy = By.cssSelector(".btn.btn-cta.btn-sm")
    By action = By.linkText("Action")
    By editJob = By.linkText("Edit this Job…")
    By description = By.cssSelector(".ace_layer.ace_text-layer")
    By updateJob = By.id("jobUpdateSaveButton")
    By descriptionText = By
            .xpath("//*[@class=\"section-space\"]//*[@class=\"h5 text-strong\"]")

    static final String PAGE_PATH = "/job/create"

    JobPage(final SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        PAGE_PATH
    }

    void validatePage() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on all jobs page: " + driver.currentUrl)
        }
    }

    void createSimpleJob(String jobName, String command) {
        jobNameField.click()
        jobNameField.sendKeys(jobName)
        workflowTab.click()
        waitForElementVisible By.cssSelector(".add_step_buttons.panel-body")
        selectStep "command", StepType.NODE
        waitForElementVisible commandBy
        commandField.click()
        commandField.sendKeys(command ?: "echo \"This is a Sample Job\"")
        saveStep 0
        createField.click()
    }

    void selectStep(String dataNodeStepType, StepType stepType) {
        def step = el By.xpath("//*[@${stepType.getStepType()}='$dataNodeStepType']")
        step.click()
        waitForNumberOfElementsToBe floatBy
    }

    void saveStep(Integer stepNumber) {
        def aux = floatField.findElement btnBy
        aux.click()
        waitForElementVisible By.id("wfitem_" + stepNumber)
    }

    WebElement getJobNameField() {
        el jobNameBy
    }

    WebElement getWorkflowTab() {
        el workflowBy
    }

    WebElement getCommandField() {
        el commandBy
    }

    WebElement getCreateField() {
        el createBy
    }

    WebElement getFloatField() {
        el floatBy
    }

    WebElement getActionField() {
        el action
    }

    WebElement getEditJobField() {
        el editJob
    }

    WebElement getDescriptionField() {
        el description
    }

    WebElement getUpdateJobButton() {
        el updateJob
    }

    WebElement getDescriptionTextLabel() {
        el descriptionText
    }

}
