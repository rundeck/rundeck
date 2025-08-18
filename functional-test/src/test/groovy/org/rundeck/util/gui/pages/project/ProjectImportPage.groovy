package org.rundeck.util.gui.pages.project

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class ProjectImportPage extends BasePage{


    String loadPath = ""
    By importButtonBy = By.id("uploadFormUpload")
    By importAlertMessageBy = By.cssSelector(".alert")
    By fileInputBy = By.id("uploadFormFileInput")

    ProjectImportPage(SeleniumContext context) {
        super(context)
    }

    ProjectImportPage(final SeleniumContext context, String projectName) {
        super(context)
        this.loadPath = "/project/${projectName}/import"
    }

    /**
     * It locates the import button using "importButtonBy"
     * @return WebElement
     */
    WebElement getImportButton(){
        (el importButtonBy)
    }

    /**
     *
     * @return WebElement for the alert message shown at the top of the screen
     */
    WebElement getImportAlertMessage(){
        (el importAlertMessageBy)
    }

    /** Return WebElement for the file input */
    WebElement getFileInput() {
        (el fileInputBy)
    }
}
