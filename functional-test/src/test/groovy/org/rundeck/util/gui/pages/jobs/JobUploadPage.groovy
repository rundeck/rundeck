package org.rundeck.util.gui.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * Job upload page
 */
@CompileStatic
class JobUploadPage extends BasePage {

    private static final String fileInputName = "xmlBatch"
    private static final String formUploadButtonId = "uploadFormUpload"
    private static final String jobUploadInfoSelectors = 'alert alert-info'

    String loadPath
    By fileInput = By.name(fileInputName)
    By formUploadButton = By.id(formUploadButtonId)
    By jobUploadInfoDiv = By.xpath("//div[@class='${jobUploadInfoSelectors}']")
    By headerTextSuccessBy = By.cssSelector(".card-header.text-success")

    JobUploadPage(final SeleniumContext context) {
        super(context)
    }

    void loadPathToUploadPage(String projectName) {
        loadPath = "/project/${projectName}/job/upload"
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on job upload page: " + driver.currentUrl)
        }
    }

    WebElement fileInputElement(){
        el fileInput
    }

    WebElement fileUploadButtonElement(){
        el formUploadButton
    }

    WebElement jobUploadInfoDivElement(){
        el jobUploadInfoDiv
    }

    WebElement getHeaderTextSuccess(){
        waitForElementVisible headerTextSuccessBy
        el headerTextSuccessBy
    }

}
