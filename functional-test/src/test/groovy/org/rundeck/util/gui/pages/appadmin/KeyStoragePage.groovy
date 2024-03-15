package org.rundeck.util.gui.pages.appadmin

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * Key Storage page
 */
@CompileStatic
class KeyStoragePage extends BasePage {

    String loadPath = "/menu/storage"

    By addUploadKey = By.xpath("//button[contains(.,'Key')] | //*[contains(@href,'uploadkey')]")
    By uploadKeyType = By.name("uploadKeyType")
    By uploadPassword = By.id("uploadpasswordfield")
    By uploadPath = By.id("uploadResourcePath2")
    By uploadResourceName = By.id("uploadResourceName2")
    By save = By.xpath("//button[contains(.,'Save')] | //button[@class='btn btn-cta'] | //input[contains(@type, 'submit')]")
    By overBy = By.xpath("//button[contains(.,'Overwrite')] | //a[contains(@data-bind, 'actionUploadModify')]")

    KeyStoragePage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on key storage page: " + driver.currentUrl)
        }
    }

    def goToKey(String name, String storagePath) {
        By storageBy = By.xpath("//*[@class=\"action\"]//*[contains(.,'$storagePath')]")
        waitForElementVisible storageBy
        def storageEl = el storageBy
        storageEl.click()

        By nameBy = By.xpath("//*[@class=\"action\"]//*[contains(.,'$name')]")
        waitForElementVisible nameBy
        def nameEl = el nameBy
        nameEl.click()
    }

    void addPasswordType(String keyValue, String storagePath, String name) {
        def select = new Select(uploadKeyTypeField)
        select.selectByValue("password")
        uploadPasswordField.sendKeys(keyValue)
        uploadPathField.sendKeys(storagePath)
        uploadResourceNameField.sendKeys(name)
        waitForElementToBeClickable saveField
        saveField.click()
    }

    void deleteKey(String name, String storagePath) {
        goToKey name, storagePath

        def action = els By.xpath("//*[@class=\"btn-group\"]//button[contains(.,'Action')]")
        if (action.size() == 1) {
            def actionLink = byAndWait By.xpath("//*[@class=\"btn-group\"]//button[contains(.,'Action')]")
            actionLink.click()
        }

        def delete = byAndWait By.xpath("//button[contains(.,'Delete')] | //a[contains(@href, 'storageconfirmdelete')]")
        delete.click()

        def deleteConfirm = byAndWait By.xpath("//*[@class=\"modal-content\"]//button[contains(.,'Delete')]")
        deleteConfirm.click()
    }

    void clickOverwriteKey(String storagePath, String name) {
        goToKey name, storagePath
        waitForElementVisible overBy
        el overBy click()
    }

    void overwriteKey(String newKey) {
        uploadPasswordField.sendKeys(newKey)
        waitForElementToBeClickable saveField
        saveField.click()
    }

    void checkKeyExists(String name, String storagePath) {
        try {
            def storagePathSaved = el By.xpath("//*[contains(.,'keys/${storagePath}/${name}')]")
            waitIgnoringForElementVisible storagePathSaved
        } catch (Exception ignored) {
            def storagePathSaved = el By.xpath("//span[text()='$storagePath']")
            waitIgnoringForElementToBeClickable storagePathSaved
            storagePathSaved.click()
            def keyLocator = el By.xpath("//span[text()='$name']")
            waitIgnoringForElementVisible keyLocator
        }
    }

    WebElement getAddUploadKeyField() {
        el addUploadKey
    }

    WebElement getUploadKeyTypeField() {
        el uploadKeyType
    }

    WebElement getUploadPasswordField() {
        el uploadPassword
    }

    WebElement getUploadPathField() {
        el uploadPath
    }

    WebElement getUploadResourceNameField() {
        el uploadResourceName
    }

    WebElement getSaveField() {
        el save
    }

}
