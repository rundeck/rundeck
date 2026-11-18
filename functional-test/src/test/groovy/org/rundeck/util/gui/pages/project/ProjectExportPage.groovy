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

/**
 * Project Export page, plus the export wait page it redirects to
 * ({@code /project/<p>/exportWait/<token>}). The wait page is served by Vue behind the {@code nextUi}
 * cookie, legacy KO/GSP otherwise. Call {@code withFlag('nextUi', bool)} before {@code go()} (or use
 * {@code page(ProjectExportPage, project, [nextUi: nextUi])}) to set the cookie explicitly; pages
 * without the flag leave the cookie untouched. Both versions share the download button id, the
 * panel classes and the refresh checkbox id, so most locators here are shared.
 */
@CompileStatic
class ProjectExportPage extends BasePage {

    By submitExportBy = By.xpath("//button[@type='submit']")
    By downloadArchiveBy = By.xpath("//*[@id='export-download-btn']")
    By errorPanelBy = By.cssSelector(".panel-danger")
    By stripJobRefBy = By.name("stripJobRef")
    By stripNameBy = By.xpath("//label[contains(@for,'trip')]")
    By checkBoxBy = By.xpath("//*[@type='checkbox' and not(@id='preserveuuid')]")
    By checkBoxDisabledBy = By.cssSelector("#exportInputs .export_select_list .checkbox.disabled")
    By allCheckboxBy = By.id('exportAll')
    By exportJobsCheckboxBy = By.id('exportJobs')
    By exportExecutionsCheckboxBy = By.id('exportExecutions')
    By exportConfigsCheckboxBy = By.id('exportConfigs')
    By exportReadmesCheckboxBy = By.id('exportReadmes')
    By exportAclsCheckboxBy = By.id('exportAcls')
    By exportScmCheckboxBy = By.id('exportScm')
    By authTokensCheckboxBy = By.xpath("//*[contains(text(), 'Include Webhook Auth Tokens')]")
    By exportArchiveButtonBy = By.xpath("//button[contains(text(),'Export Archive')]")
    By btnCtaButtonBy = By.cssSelector('a.btn.btn-cta')
    By exportFormFooter = By.cssSelector('#exportform .card-footer')
    By exportToOtherInstanceButtonBy = By.cssSelector('#exportform button[type="button"][data-target="#exportModal"]')

    String loadPath = ""
    String project = ""

    /** Heading panel of the ready state (same {@code .panel-heading} in both versions). */
    By readyHeadingBy = By.cssSelector(".panel-heading")
    /** Return link in the ready panel footer (shared markup: panel-footer anchor). */
    By returnLinkBy = By.cssSelector(".panel-footer a.btn")
    /** Auto refresh checkbox of the in-progress state. */
    By refreshCheckboxBy = By.id("dorefresh")
    /** Vue mount point of the wait page; absent in the legacy version and on the error page. */
    By vueMountBy = By.id("menuWaitPage")
    /** Title of the generic error page (/common/error). */
    By errorTitleBy = By.cssSelector("h2.panel-title")
    By errorBodyBy = By.cssSelector(".panel-body.text-danger")

    ProjectExportPage(final SeleniumContext context, String project) {
        super(context)
        this.project = project
        this.loadPath = "/project/${project}/export"
    }

    /** True when the nextUi flag was set on this page object. */
    boolean getNextUi() { isFlagEnabled('nextUi') }

    /**
     * Sets the nextUi cookie (when the flag was given) before loading the page, so the mode survives
     * the export form POST and its redirect to the wait page.
     */
    @Override
    void go() {
        if (pageFlags.containsKey('nextUi')) {
            setNextUiCookie(nextUi)
        }
        super.go()
    }

    /** Absolute-path URL of the wait page for a token. */
    String exportWaitPath(String token) {
        "/project/${project}/exportWait/${token}"
    }

    /**
     * Wait for the redirect from the export form to the wait page and return its token.
     * Waits on the exact destination, not the shared {@code /export} prefix.
     */
    String waitForWaitPageToken() {
        waitForUrlToContain("/project/${project}/exportWait/")
        String path = URI.create(driver.currentUrl).path
        path.substring(path.lastIndexOf('/') + 1)
    }

    /**
     * Assert the wait page was rendered by the expected UI version: the Vue mount only exists in
     * nextUi, and #pageUiMeta says which template the server picked.
     */
    void validateWaitPageVersion() {
        String expected = nextUi ? 'next' : 'current'
        if (pageUiType != expected) {
            throw new AssertionError("Wait page rendered uiType '${pageUiType}', expected '${expected}'" as Object)
        }
        if (nextUi) {
            waitForElementVisible(vueMountBy)
        } else if (!els(vueMountBy).isEmpty()) {
            throw new AssertionError("Legacy wait page unexpectedly contains #menuWaitPage" as Object)
        }
    }

    /** Waits until the polled/ready state is shown (download button visible). */
    void waitForExportReady() {
        waitForElementVisible(downloadArchiveBy)
    }

    /** Waits until the tab title carries the [READY] prefix and returns it. */
    String waitForReadyTitle() {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(
                ExpectedConditions.titleContains('[READY]'))
        driver.title
    }

    String getReadyHeadingText() {
        waitForElementVisible(readyHeadingBy).text
    }

    String getDownloadHref() {
        downloadArchiveButton.getAttribute('href')
    }

    String getDownloadText() {
        downloadArchiveButton.text.trim()
    }

    WebElement getReturnLink() {
        waitForElementVisible(returnLinkBy)
    }

    /** Whether the Vue mount point or the legacy wait panels are present at all (error page check). */
    boolean isVueMountPresent() {
        !els(vueMountBy).isEmpty()
    }

    /** Open the wait page for an arbitrary token, keeping the current nextUi cookie. */
    void goToWaitPage(String token) {
        if (pageFlags.containsKey('nextUi')) {
            setNextUiCookie(nextUi)
        }
        go(exportWaitPath(token))
    }

    String getErrorTitleText() {
        waitForElementVisible(errorTitleBy).text
    }

    String getErrorBodyText() {
        waitForElementVisible(errorBodyBy).text
    }

    /**
     * Fetch the download URL from inside the browser session and report status and
     * Content-Disposition. Uses the page's own session cookie; consumes the one-shot token.
     * @return [status, contentDisposition]
     */
    List<Object> fetchDownloadHeaders() {
        Object result = ((JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[arguments.length - 1];" +
                "fetch(arguments[0], {credentials: 'same-origin'})" +
                ".then(r => done([r.status, r.headers.get('content-disposition')]))" +
                ".catch(e => done([-1, String(e)]));",
                downloadHref)
        (List<Object>) result
    }

    /** Is the refresh checkbox in the in-progress state checked? */
    boolean isRefreshChecked() {
        el(refreshCheckboxBy).isSelected()
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
    List<WebElement> getDisabledCheckboxes() {
        els checkBoxDisabledBy
    }

    WebElement checkBoxLabel(String checkBoxId) {
        el By.xpath("//label[@for='${checkBoxId}']")
    }

    WebElement getAllCheckbox() {
        return el(allCheckboxBy)
    }

    WebElement getExportJobsCheckbox() {
        return el(exportJobsCheckboxBy)
    }

    WebElement getExportExecutionsCheckbox() {
        return el(exportExecutionsCheckboxBy)
    }

    WebElement getExportConfigsCheckbox() {
        return el(exportConfigsCheckboxBy)
    }

    WebElement getExportReadmesCheckbox() {
        return el(exportReadmesCheckboxBy)
    }

    WebElement getExportAclsCheckbox() {
        return el(exportAclsCheckboxBy)
    }

    WebElement getExportScmCheckbox() {
        return el(exportScmCheckboxBy)
    }

    WebElement getAuthTokensCheckbox() {
        return els(authTokensCheckboxBy).get(0)
    }

    WebElement getExportArchiveButton() {
        return el(exportArchiveButtonBy)
    }

    WebElement getBtnCtaButton() {
        return el(btnCtaButtonBy)
    }
    WebElement getExportFormFooter() {
        return el(exportFormFooter)
    }
    WebElement getExportToOtherInstanceButton() {
        return el(exportToOtherInstanceButtonBy)
    }
}
