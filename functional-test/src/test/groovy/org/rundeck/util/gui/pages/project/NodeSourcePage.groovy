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

    // ---------- Provider picker (stable data-testids + minimal fallback) ----------
    By providerPickerByTestId  = By.cssSelector("[data-testid='provider-picker']")
    By providerPickerModal     = By.cssSelector(".modal.show, .modal.in")
    By providerPickerListGroup = By.cssSelector(".provider-list, .list-group")

    // “Local” provider hooks
    By providerLocalByTestId   = By.cssSelector("[data-testid='provider-local']")
    By providerLocalByAttrs    = By.cssSelector("[data-provider='local'], [data-provider-id='local'], a[href*='local' i]")

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
        // After clicking the CTA, wait for ANY of: data-testid picker, a visible modal (.show or .in),
        // or the list-group itself. Use presence-of-element to be tolerant of animation timing.
        def wait = new WebDriverWait(driver, Duration.ofSeconds(10))
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(providerPickerByTestId),
                    ExpectedConditions.presenceOfElementLocated(providerPickerModal),
                    ExpectedConditions.presenceOfElementLocated(providerPickerListGroup)
            ))
        } catch (Throwable firstTry) {
            // The click might not have registered if the page was mid-render.
            // Click "Add a new Node Source" again, then wait once more.
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
     * Wait for a success toast; if none appears, refresh/navigate and wait for the page
     * to be ready using BasePage utilities.
     */
    void waitForSaveToastOrRefresh() {
        if (waitForToast(10)) return

        // Ensure no modal is covering, then reload and wait for ready state
        try { waitForModal(0, providerPickerModal) } catch (Throwable ignored) {}
        go(loadPath)  // uses BasePage.go + validatePage()
        waitForPageReady()
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
}
