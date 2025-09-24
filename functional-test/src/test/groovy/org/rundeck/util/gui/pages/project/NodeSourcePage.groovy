package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.*
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.nio.charset.StandardCharsets

import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.interactions.MoveTargetOutOfBoundsException
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

@CompileStatic
class NodeSourcePage extends BasePage {

    String loadPath = ""

    // ---------- Core locators ----------
    By newNodeSource               = By.xpath("//button[contains(.,'Add a new Node Source')]")
    By saveNodeSourceConfigBy      = By.cssSelector(".btn.btn-cta.btn-xs")     // small inline save
    By saveButtonBy                = By.cssSelector(".btn.btn-cta")            // page-level Save (primary CTA)
    By nodesEditTabBy              = By.xpath("//div[contains(text(),'Edit')]")
    By modifyBy                    = By.linkText("Modify")
    By nodeSourcesListBy = By.cssSelector("[data-testid='node-sources-list']")

    // Success toast/text
    By configurationSavedPopUpBy = By.xpath(
            "//*[contains(translate(normalize-space(.),'SAVED','saved'),'configuration saved') " +
                    "or contains(translate(normalize-space(.),'SAVED','saved'),'configurations saved')]"
    )
    By toastSuccessBy = By.cssSelector(".p-toast-message-success, .rdk-toast--success")

    // ---------- Provider picker (testids + fallbacks) ----------
    By providerPickerByTestId   = By.cssSelector("[data-testid='provider-picker']")
    By providerPickerModal      = By.cssSelector(".modal.show")
    By providerPickerListGroup  = By.cssSelector(".provider-list, .list-group")

    // Preferred “Local” hooks
    By providerLocalByTestId    = By.cssSelector("[data-testid='provider-local']")
    By providerLocalByAttrs     = By.cssSelector("[data-provider='local'], [data-provider-id='local'], a[href*='local' i]")

    NodeSourcePage(final SeleniumContext context) { super(context) }

    WebElement getNewNodeSourceButton() { el newNodeSource }

    void validatePage() {
        new WebDriverWait(context.driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.urlContains(loadPath))
    }

    void forProject(String projectName) {
        this.loadPath = "/project/${projectName}/nodes/sources"
    }

    // ---------- Simple clicks ----------
    void clickSaveNodeSourceConfig(){ (el saveNodeSourceConfigBy).click() }
    def  clickNodesEditTab()       { (el nodesEditTabBy).click() }
    def  clickModifyButton()       { (el modifyBy).click() }

    // ---------- Hardened actions for the current test flow ----------

    void clickAddNewNodeSource() {
        safeClick(newNodeSource)              // instead of locating + jsClick
    }

    void chooseProviderPreferLocal() {
        WebDriverWait wait = new WebDriverWait(context.driver, Duration.ofSeconds(15))

        // 0) Briefly wait for the modal to appear (id or visible modal)
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("#add-new-modal")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector(".modal.show"))
            ))
        } catch (Throwable ignored) { /* modal might be non-blocking; continue */ }

        // 1) Fast path: find the Local provider by its test id globally (covers append-to-body)
        try {
            WebElement choice = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-testid='provider-local']")
            ))
            jsScroll(choice)
            jsClick(choice)
            sleepQuiet(100)
            return
        } catch (TimeoutException ignored) {
            // fall through to scoped fallbacks
        }

        // 2) Scoped fallbacks: locate the picker container, then look inside it
        WebElement picker = null
        try {
            picker = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("[data-testid='provider-picker']")))
        } catch (TimeoutException ignored) {
            try {
                picker = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".modal.show")))
            } catch (TimeoutException ignored2) {
                picker = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".provider-list, .list-group")))
            }
        }

        WebElement choice = findFirstVisibleWithin(picker,
                By.cssSelector("[data-testid='provider-local']"))
        if (choice == null) {
            choice = findFirstVisibleWithin(picker,
                    By.cssSelector("[data-provider='local'], [data-provider-id='local'], a[href*='local' i]"))
        }
        if (choice == null) {
            WebElement textNode = findFirstVisibleWithin(picker,
                    By.xpath(".//*[contains(translate(normalize-space(.),'LOCAL','local'),'local')]"))
            if (textNode != null) {
                try {
                    choice = textNode.findElement(By.xpath("ancestor::*[self::a or self::button][1]"))
                } catch (NoSuchElementException ignored) { }
            }
        }

        assert choice != null : "No 'Local' provider option found"
        jsScroll(choice)
        safeClick(choice)
        sleepQuiet(100)
    }



    void clickSaveNodeSources() {
        safeClick(saveButtonBy)
    }

    void waitForSaveToastOrRefresh() {
        if (waitForAnySuccessToast(10)) return

        WebDriverWait wait = new WebDriverWait(context.driver, Duration.ofSeconds(30))
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".modal.show, .modal.in")
            ))
        } catch (Throwable ignored) {}

        try {
            this.go(loadPath)   // hard navigate to the route
        } catch (Throwable ignored) {
            context.driver.navigate().refresh()
        }

        // URL contains the route, then page ready (list or CTA)
        wait.until(ExpectedConditions.urlContains(loadPath))
        waitForPageReady()
        try { Thread.sleep(200) } catch (ignored) {}
    }


    private boolean waitForAnySuccessToast(int seconds) {
        def wait = new WebDriverWait(context.driver, Duration.ofSeconds(seconds))
        try {
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.visibilityOfElementLocated(toastSuccessBy),
                    ExpectedConditions.visibilityOfElementLocated(configurationSavedPopUpBy)
            ))
            return true
        } catch (Throwable ignored) {
            return false
        }
    }

    // ---------- helpers ----------
    private static WebElement findFirstVisibleWithin(WebElement root, By by) {
        if (root == null) return null
        def list = root.findElements(by)
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

    private void safeClick(By by) {
        WebDriverWait wait = new WebDriverWait(context.driver, Duration.ofSeconds(15))
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(by))
        safeClick(element)
    }

    private void safeClick(WebElement element) {
        jsScroll(element)
        try {
            element.click() // prefer native click
        } catch (ElementClickInterceptedException | ElementNotInteractableException | MoveTargetOutOfBoundsException ignored) {
            // try Actions click
            try {
                new Actions(context.driver).moveToElement(element, 1, 1).click().perform()
            } catch (Throwable ignored2) {
                // final fallback: JS click
                jsClick(element)
            }
        }
    }
    void waitForPageReady() {
        WebDriverWait wait = new WebDriverWait(context.driver, Duration.ofSeconds(30))
        // make sure no modal is covering
        try {
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector(".modal.show, .modal.in")
            ))
        } catch (Throwable ignored) {}

        // Either the list wrapper is present OR the CTA button is visible
        wait.until(ExpectedConditions.or(
                ExpectedConditions.presenceOfElementLocated(nodeSourcesListBy),
                ExpectedConditions.visibilityOfElementLocated(newNodeSource)
        ))
    }
}
