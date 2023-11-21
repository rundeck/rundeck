package org.rundeck.tests.functional.selenium.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

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

    WebElement checkBoxLabel(String checkBoxId) {
        el By.xpath("//label[@for='${checkBoxId}']")
    }
}
