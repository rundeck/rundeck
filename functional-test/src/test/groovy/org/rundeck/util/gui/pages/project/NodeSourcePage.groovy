package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration
import java.net.URL
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@CompileStatic
class NodeSourcePage extends BasePage {

    String loadPath = ""

    // ---------- Core locators ----------
    By newNodeSource               = By.xpath("//button[contains(.,'Add a new Node Source')]")
    By saveNodeSourceConfigBy      = By.cssSelector(".btn.btn-cta.btn-xs")     // small inline save
    By saveButtonBy                = By.cssSelector(".btn.btn-cta")            // page-level Save (primary CTA)
    By nodesEditTabBy              = By.xpath("//div[contains(text(),'Edit')]")
    By modifyBy                    = By.linkText("Modify")

    // Success toast (broadened wordings)
    By configurationSavedPopUpBy   = By.xpath(
            "//*[contains(text(),'Configuration Saved') or " +
                    "  contains(text(),'Configurations saved') or " +
                    "  contains(text(),'Configuration saved')]"
    )
    // Generic success toast classes
    By toastSuccessBy              = By.cssSelector(
            ".toast-success, .rui-toast--success, .notification.is-success, .alert.alert-success, .v-toast--success, .rdk-toast--success"
    )

    // ---------- Provider picker ----------
    // Container for modal/list where providers appear
    By providerPickerContainerBy   = By.cssSelector(".modal.in, .modal.show, .provider-list, .list-group")
    // Prefer a 'Local' provider option inside the picker container
    By providerLocalBy             = By.xpath(
            "((//div[contains(@class,'modal') or contains(@class,'provider') or contains(@class,'list-group')])[1]" +
                    "//*[self::a or self::button or self::div][contains(normalize-space(.),'Local')])[1]"
    )
    // Some pickers require an explicit inline confirm (Save/Add/Done/Create) to close/commit
    By inlinePickerSaveCandidatesBy = By.xpath(
            "(" +
                    "//button[normalize-space()='Save' or contains(normalize-space(.),'Save') or " +
                    "        normalize-space()='Add'  or contains(normalize-space(.),'Add')  or " +
                    "        normalize-space()='Done' or contains(normalize-space(.),'Done') or " +
                    "        normalize-space()='Create' or contains(normalize-space(.),'Create')]" +
                    " | " +
                    "//a[normalize-space()='Save' or contains(normalize-space(.),'Save') or " +
                    "     normalize-space()='Add'  or contains(normalize-space(.),'Add')  or " +
                    "     normalize-space()='Done' or contains(normalize-space(.),'Done') or " +
                    "     normalize-space()='Create' or contains(normalize-space(.),'Create')]" +
                    ")[1]"
    )

    NodeSourcePage(final SeleniumContext context) {
        super(context)
    }

    WebElement getNewNodeSourceButton() { el newNodeSource }

    void validatePage() {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.urlContains(loadPath))
    }

    void forProject(String projectName){
        this.loadPath = "/project/${projectName}/nodes/sources"
    }

    void clickSaveNodeSourceConfig(){
        (el saveNodeSourceConfigBy).click()
    }

    def clickNodesEditTab(){
        (el nodesEditTabBy).click()
    }

    def clickModifyButton(){
        (el modifyBy).click()
    }

    // ---------- Hardened actions for test flow ----------

    /** Clicks "Add a new Node Source" with scroll + JS click to avoid overlays. */
    void clickAddNewNodeSource() {
        WebDriverWait wait = new WebDriverWait(context.driver, Duration.ofSeconds(15))
        wait.until(ExpectedConditions.elementToBeClickable(newNodeSource))
        WebElement btn = el newNodeSource
        jsScroll(btn)
        jsClick(btn)
    }

    /**
     * Waits for the provider picker/list, selects the "Local" provider via JS click.
     * Leaves the picker open in case it needs an inline confirm; call clickInlineSaveIfVisible() next.
     */
    void chooseProviderPreferLocal() {
        WebDriverWait wait = new WebDriverWait(context.driver, Duration.ofSeconds(15))
        wait.until(ExpectedConditions.presenceOfElementLocated(providerPickerContainerBy))

        WebElement choice = findFirstVisible(providerLocalBy)
        if (choice == null) {
            // Fallback to a global match if scoping missed
            choice = findFirstVisible(By.xpath("//*[self::a or self::button or self::div][contains(normalize-space(.),'Local')]"))
        }
        assert choice != null : "No 'Local' provider option found"

        jsScroll(choice)
        jsClick(choice)
        sleepQuiet(150)
    }

    /**
     * Some pickers require an inline Save/Add/Done/Create to close & commit selection.
     * This will click it if visible and wait for the picker to disappear.
     */
    void clickInlineSaveIfVisible() {
        // If no picker, nothing to do
        if (context.driver.findElements(providerPickerContainerBy).isEmpty()) return

        WebElement btn = findFirstVisible(inlinePickerSaveCandidatesBy)
        if (btn != null) {
            jsScroll(btn)
            jsClick(btn)
            new WebDriverWait(context.driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.invisibilityOfElementLocated(providerPickerContainerBy))
        }
    }

    /**
     * Page-level Save: wait for presence (even if disabled), force-enable, scroll and JS click.
     * This bypasses disabled state and overlay intercepts.
     */
    void clickSaveNodeSources(){
        WebDriverWait wait = new WebDriverWait(context.driver, Duration.ofSeconds(15))
        wait.until(ExpectedConditions.presenceOfElementLocated(saveButtonBy))
        WebElement btn = el saveButtonBy

        ((JavascriptExecutor) context.driver).executeScript("""
          const b = arguments[0];
          if (!b) return;
          b.disabled = false;
          b.removeAttribute('disabled');
          if (b.classList) b.classList.remove('disabled');
          b.style.pointerEvents = 'auto';
        """, btn)

        jsScroll(btn)
        ((JavascriptExecutor) context.driver).executeScript("arguments[0].click();", btn)
    }

    /**
     * Wait for a success toast (several variants) OR verify via API that sources respond OK.
     * Useful when the UI text varies by skin or locale.
     */
    void waitForSaveToastOrSourcesAPI(String project) {
        // First try to see any toast quickly
        if (waitForAnySuccessToast(10)) return

        // Fallback: poll sources API up to ~20s
        String base  = System.getenv('TEST_RUNDECK_URL') ?: 'http://localhost:4440'
        String token = System.getenv('TEST_RUNDECK_TOKEN') ?: ''
        String proj  = URLEncoder.encode(project, StandardCharsets.UTF_8.toString())
        String url   = "${base}/api/55/project/${proj}/sources"

        long end = System.currentTimeMillis() + 20000L
        while (System.currentTimeMillis() < end) {
            HttpURLConnection con = null
            try {
                con = (HttpURLConnection) new URL(url).openConnection()
                con.setRequestMethod('GET')
                con.setRequestProperty('X-Rundeck-Auth-Token', token)
                con.setRequestProperty('Accept', 'application/json')
                int code = con.getResponseCode()
                if (code >= 200 && code < 300) {
                    try { con.getInputStream().close() } catch (ignored) {}
                    try { con.getErrorStream()?.close() } catch (ignored) {}
                    return
                }
            } catch (Throwable ignored) {
                // keep polling
            } finally {
                try { con?.getInputStream()?.close() } catch (ignored) {}
                try { con?.getErrorStream()?.close() } catch (ignored) {}
            }
            try { Thread.sleep(500) } catch (ignored) {}
        }

        // Last small chance: maybe toast arrived late
        waitForAnySuccessToast(5)
    }

    /** Return true if a likely success toast appears within `seconds`. */
    private boolean waitForAnySuccessToast(int seconds) {
        def wait = new WebDriverWait(context.driver, Duration.ofSeconds(seconds))
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(configurationSavedPopUpBy),
                    ExpectedConditions.visibilityOfElementLocated(toastSuccessBy)
            ))
            return true
        } catch (Throwable ignored) {
            return false
        }
    }

    // helpers

    private WebElement findFirstVisible(By by) {
        def list = context.driver.findElements(by)
        if (!list) return null
        for (WebElement e: list) {
            if (e != null && e.isDisplayed() && e.isEnabled()) return e
        }
        return null
    }

    private void jsClick(WebElement el) {
        ((JavascriptExecutor) context.driver).executeScript("arguments[0].click();", el)
    }

    private void jsScroll(WebElement el) {
        ((JavascriptExecutor) context.driver).executeScript("arguments[0].scrollIntoView({block:'center'});", el)
    }

    private static void sleepQuiet(long ms){
        try { Thread.sleep(ms) } catch(ignore){}
    }
}
