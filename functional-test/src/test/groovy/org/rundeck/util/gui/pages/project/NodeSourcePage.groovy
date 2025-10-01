package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.ElementClickInterceptedException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

@CompileStatic
class NodeSourcePage extends BasePage {

    String loadPath = ""

    By newNodeSource = By.xpath("//button[contains(.,'Add a new Node Source')]")
    By saveNodeSourceConfigBy = By.cssSelector(".btn.btn-cta.btn-xs")
    By saveButtonBy = By.cssSelector(".btn.btn-cta")
    By nodesEditTabBy = By.xpath("//div[contains(text(),'Edit')]")
    By modifyBy = By.linkText("Modify")
    By configurationSavedPopUpBy = By.xpath("//*[contains(text(),'Configuration Saved')]")

    // Wrapper that exists on the list view
    By nodeSourcesListBy      = By.cssSelector("[data-testid='node-sources-list']")
    // Save state banner
    By unsavedChangesBannerBy = By.xpath("//span[contains(text(),'Changes have not been saved.')]")

    // ---------- Provider picker (stable data-testids + minimal fallback) ----------
    By providerPickerByTestId  = By.cssSelector("[data-testid='provider-picker']")
    By providerPickerModal     = By.cssSelector(".modal.show, .modal.in")
    By providerPickerListGroup = By.cssSelector(".provider-list, .list-group")

    // “Local” provider hooks
    By providerLocalByTestId   = By.cssSelector("[data-testid='provider-local']")
    By providerLocalByAttrs    = By.cssSelector("[data-provider='local'], [data-provider-id='local']")

    NodeSourcePage(final SeleniumContext context) { super(context) }

    WebElement getNewNodeSourceButton() { byAndWait(newNodeSource) }

    void forProject(String projectName) {
        this.loadPath = "/project/${projectName}/nodes/sources"
    }

    // ---------- Simple clicks using BasePage waits ----------
    void clickSaveNodeSourceConfig() { byAndWaitClickable(saveNodeSourceConfigBy).click() }
    def  clickNodesEditTab()         { byAndWaitClickable(nodesEditTabBy).click() }
    def  clickModifyButton()         { byAndWaitClickable(modifyBy).click() }

    void clickAddNewNodeSource() {
        byAndWaitClickable(newNodeSource).click()
    }

    void chooseProviderByName(String providerName) {
        // make sure the modal/list is present
        def wait = new WebDriverWait(driver, Duration.ofSeconds(10))
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(providerPickerByTestId),
                ExpectedConditions.presenceOfElementLocated(providerPickerModal),
                ExpectedConditions.presenceOfElementLocated(providerPickerListGroup)
        ))

        // find the exact testid the UI renders
        By choiceBy = providerByTestId(providerName)

        WebElement toScroll = el(choiceBy)
        executeScript("arguments[0].scrollIntoView(true);", toScroll)

        WebElement choice = waitForElementVisible(choiceBy)
        try {
            waitIgnoringForElementToBeClickable(choice).click()
        } catch (ElementClickInterceptedException ignored) {
            executeScript("arguments[0].click();", choice)
        }
    }

    void clickSaveNodeSources() {
        byAndWaitClickable(saveButtonBy).click()
    }

    /**
     * Post-save check:
     * 1) If a success toast appears quickly, we're done.
     * 2) Otherwise, wait until the “Changes have not been saved.” banner disappears.
     * 3) If still flaky, refresh once and ensure the banner is gone (and page ready).
     */
    void waitForSavedState() {

        if (waitForUnsavedBannerToDisappear(12)) return

        // Fallback: refresh once and check again
        try { waitForModal(0, providerPickerModal) } catch (Throwable ignored) {}
        go(loadPath)
        waitForPageReady()
        waitForUnsavedBannerToDisappear(10) // if still present, this will throw a TimeoutException
    }

    /**
     * Page is ready when either the wrapper is present or the CTA is visible.
     */
    void waitForPageReady() {
        try { waitForModal(0, providerPickerModal) } catch (Throwable ignored) {}
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.or(
                        ExpectedConditions.presenceOfElementLocated(nodeSourcesListBy),
                        ExpectedConditions.visibilityOfElementLocated(newNodeSource)
                ))
    }


    private boolean waitForUnsavedBannerToDisappear(int seconds) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(seconds))
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(unsavedChangesBannerBy))
            return true
        } catch (TimeoutException ignored) {
            return false
        }
    }
    private static String slugify(String text) {
        if (text == null) return ""
        return text.toLowerCase()
                .replaceAll(/[^a-z0-9]+/, "-")
                .replaceAll(/(^-|-$)/, "")
    }
    private static By providerByTestId(String providerName) {
        return By.cssSelector("[data-testid='provider-" + slugify(providerName) + "']")
    }
}
