package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
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

    // ---------- Core locators ----------
    By newNodeSource          = By.xpath("//button[contains(.,'Add a new Node Source')]")
    By saveNodeSourceConfigBy = By.cssSelector(".btn.btn-cta.btn-xs")  // inline Save
    By saveButtonBy           = By.cssSelector(".btn.btn-cta")         // page-level Save
    By nodesEditTabBy         = By.xpath("//div[contains(text(),'Edit')]")
    By modifyBy               = By.linkText("Modify")

    // Wrapper that exists on the list view
    By nodeSourcesListBy      = By.cssSelector("[data-testid='node-sources-list']")

    // Success feedback (class-based toast, allow text fallback)
    By toastSuccessBy = By.cssSelector(".p-toast-message-success, .rdk-toast--success")
    By configurationSavedPopUpBy = By.xpath(
            "//*[contains(translate(normalize-space(.),'SAVED','saved'),'configuration saved') " +
                    "or contains(translate(normalize-space(.),'SAVED','saved'),'configurations saved')]"
    )

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

    void chooseProviderPreferLocal() {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(10))
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(providerPickerByTestId),
                    ExpectedConditions.presenceOfElementLocated(providerPickerModal),
                    ExpectedConditions.presenceOfElementLocated(providerPickerListGroup)
            ))
        } catch (Throwable ignored) {
            byAndWaitClickable(newNodeSource).click()
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(providerPickerByTestId),
                    ExpectedConditions.presenceOfElementLocated(providerPickerModal),
                    ExpectedConditions.presenceOfElementLocated(providerPickerListGroup)
            ))
        }

        // Prefer the stable testid for Local
        List<WebElement> locals = els(providerLocalByTestId)
        WebElement choice = locals.find { it.displayed && it.enabled }

        // Fallback: attribute-based hooks that still identify "local"
        if (!choice) {
            def fallback = els(providerLocalByAttrs)
            choice = fallback.find { it.displayed && it.enabled }
        }

        assert choice != null : "No 'Local' provider option found"
        waitIgnoringForElementToBeClickable(choice).click()
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
        if (waitForToast(6)) return

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

    // ---------- small helpers (reuse BasePage waits) ----------
    private boolean waitForToast(int seconds) {
        def wait = new WebDriverWait(driver, Duration.ofSeconds(seconds))
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(toastSuccessBy),
                    ExpectedConditions.visibilityOfElementLocated(configurationSavedPopUpBy)
            ))
            return true
        } catch (TimeoutException ignored) {
            return false
        }
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
}
