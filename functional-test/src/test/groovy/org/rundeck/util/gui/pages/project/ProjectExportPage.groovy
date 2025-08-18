package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * Project Export page
 */
@CompileStatic
class ProjectExportPage extends BasePage {

    By submitExportBy = By.xpath("//button[@type='submit']")
    By downloadArchiveBy = By.xpath("//*[@id='export-download-btn']")
    By errorPanelBy = By.cssSelector(".panel-danger")
    By stripJobRefBy = By.name("stripJobRef")
    By stripNameBy = By.xpath("//label[contains(@for,'trip')]")
    By checkBoxBy = By.xpath("//*[@type='checkbox' and not(@id='preserveuuid')]")
    By checkBoxDisabledBy = By.cssSelector("#exportInputs .export_select_list .checkbox.disabled")
    By allCheckboxBy = By.id('exportAll')
    By exportJobsCheckboxBy = By.id('exportJobs')
    By exportExecutionsCheckboxBy = By.id('exportExecutions')
    By exportConfigsCheckboxBy = By.id('exportConfigs')
    By exportReadmesCheckboxBy = By.id('exportReadmes')
    By exportAclsCheckboxBy = By.id('exportAcls')
    By exportScmCheckboxBy = By.id('exportScm')
    By authTokensCheckboxBy = By.xpath("//*[contains(text(), 'Include Webhook Auth Tokens')]")
    By exportArchiveButtonBy = By.xpath("//button[contains(text(),'Export Archive')]")
    By btnCtaButtonBy = By.cssSelector('a.btn.btn-cta')
    By exportFormFooter = By.cssSelector('#exportform .card-footer')
    By exportToOtherInstanceButtonBy = By.cssSelector('#exportform button[type="button"][data-target="#exportModal"]')

    String loadPath = ""

    ProjectExportPage(final SeleniumContext context, String project) {
        super(context)
        this.loadPath = "/project/${project}/export"
    }

    WebElement getSubmitExportButton() {
        el submitExportBy
    }

    WebElement getDownloadArchiveButton() {
        el downloadArchiveBy
    }

    List<WebElement> getErrorPanels() {
        els errorPanelBy
    }

    List<WebElement> getStripJobRefRadios() {
        els stripJobRefBy
    }

    List<WebElement> getStripNameLabels() {
        els stripNameBy
    }

    List<WebElement> getCheckBoxes() {
        els checkBoxBy
    }
    List<WebElement> getDisabledCheckboxes() {
        els checkBoxDisabledBy
    }

    WebElement checkBoxLabel(String checkBoxId) {
        el By.xpath("//label[@for='${checkBoxId}']")
    }

    WebElement getAllCheckbox() {
        return el(allCheckboxBy)
    }

    WebElement getExportJobsCheckbox() {
        return el(exportJobsCheckboxBy)
    }

    WebElement getExportExecutionsCheckbox() {
        return el(exportExecutionsCheckboxBy)
    }

    WebElement getExportConfigsCheckbox() {
        return el(exportConfigsCheckboxBy)
    }

    WebElement getExportReadmesCheckbox() {
        return el(exportReadmesCheckboxBy)
    }

    WebElement getExportAclsCheckbox() {
        return el(exportAclsCheckboxBy)
    }

    WebElement getExportScmCheckbox() {
        return el(exportScmCheckboxBy)
    }

    WebElement getAuthTokensCheckbox() {
        return els(authTokensCheckboxBy).get(0)
    }

    WebElement getExportArchiveButton() {
        return el(exportArchiveButtonBy)
    }

    WebElement getBtnCtaButton() {
        return el(btnCtaButtonBy)
    }
    WebElement getExportFormFooter() {
        return el(exportFormFooter)
    }
    WebElement getExportToOtherInstanceButton() {
        return el(exportToOtherInstanceButtonBy)
    }
}
