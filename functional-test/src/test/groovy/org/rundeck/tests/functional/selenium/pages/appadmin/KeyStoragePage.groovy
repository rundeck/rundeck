package org.rundeck.tests.functional.selenium.pages.appadmin

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.Select
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * Key Storage page
 */
@CompileStatic
class KeyStoragePage extends BasePage {

    By addUploadKey = By.linkText("Add or Upload a Key")
    By uploadKeyType = By.name("uploadKeyType")
    By uploadPassword = By.id("uploadpasswordfield")
    By uploadPath = By.id("uploadResourcePath2")
    By uploadResourceName = By.id("uploadResourceName2")
    By save = By.xpath("//input[@type='submit']")

    String loadPath = "/menu/storage"

    KeyStoragePage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on key storage page: " + driver.currentUrl)
        }
    }

    def goToKey(String name, String storagePath) {
        By storageBy = By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$storagePath')]")
        waitForElementVisible storageBy
        def storageEl = el storageBy
        storageEl.click()

        By nameBy = By.xpath("//*[@id=\"page_storage\"]//*[@class=\"action\"]//*[contains(.,'$name')]")
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
        saveField.click()
    }

    void deleteKey(String name, String storagePath) {
        goToKey name, storagePath

        def actionLink = byAndWait By.xpath("//*[@class=\"btn-group\"]//button[contains(.,'Action')]")
        actionLink.click()

        def delete = byAndWait By.linkText("Delete Selected Item")
        delete.click()

        def deleteConfirm = byAndWait By.xpath("//*[@class=\"modal-content\"]//button[contains(.,'Delete')]")
        deleteConfirm.click()
    }

    void clickOverwriteKey(String storagePath, String name) {
        goToKey name, storagePath
        By overBy = By.linkText("Overwrite Key")
        waitForElementVisible overBy
        def overwriteButton = el overBy
        overwriteButton.click()
    }

    void overwriteKey(String newKey) {
        uploadPasswordField.sendKeys(newKey)
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
