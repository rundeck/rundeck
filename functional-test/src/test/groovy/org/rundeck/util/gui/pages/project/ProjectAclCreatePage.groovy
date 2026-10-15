package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.Keys
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

/**
 * Page object for the project ACL policy creation page.
 */
@CompileStatic
class ProjectAclCreatePage extends BasePage {

    private static final By PAGE_UI_META_BY = By.id("pageUiMeta")
    private static final By NAME_INPUT_BY = By.cssSelector("[name='name']")
    private static final By NAME_GROUP_BY =
            By.xpath("//input[@name='name']/ancestor::div[contains(@class,'form-group')][1]")
    private static final By PROJECT_VALUE_BY = By.cssSelector(".form-control-static.text-info")
    private static final By EDITOR_BY = By.cssSelector(".ace_editor")
    private static final By EDITOR_INPUT_BY = By.cssSelector(".ace_editor .ace_text-input")
    private static final By FILE_TEXT_INPUT_BY = By.cssSelector("textarea[name='fileText']")
    private static final By FILE_TEXT_GROUP_BY =
            By.xpath("//textarea[@name='fileText']/ancestor::div[contains(@class,'form-group')][1]")
    private static final By HELP_TOGGLE_BY = By.id("aclHelp")
    private static final By HELP_MENU_BY =
            By.xpath("//*[@id='aclHelp']/ancestor::div[contains(@class,'dropdown')][1]" +
                    "//ul[contains(@class,'dropdown-menu')]")
    private static final By HELP_ACCESS_CONTROL_LINK_BY = By.cssSelector(
            "a[href='https://docs.rundeck.com/docs/administration/security/authorization.html']"
    )
    private static final By HELP_FORMAT_LINK_BY = By.cssSelector(
            "a[href='https://docs.rundeck.com/docs/administration/security/authorization.html#example']"
    )
    private static final By CANCEL_BUTTON_BY = By.cssSelector("[name='cancel']")
    private static final By SAVE_BUTTON_BY = By.cssSelector("[name='save']")
    private static final By GLOBAL_ERROR_BY = By.cssSelector(".alert.alert-danger")
    private static final By BODY_BY = By.tagName("body")

    final String project
    private final String basePath

    ProjectAclCreatePage(final SeleniumContext context, final String project) {
        super(context)
        this.project = project
        this.basePath = "/project/${project}/admin/acls/create"
    }

    /**
     * Returns the page URL for the selected UI mode.
     */
    @Override
    String getLoadPath() {
        basePath
    }

    /**
     * Returns whether the Vue UI is selected.
     */
    boolean getNextUi() {
        isFlagEnabled('nextUi')
    }

    /**
     * Selects whether the Vue UI should be loaded.
     *
     * @param value true for Vue, false for the legacy GSP
     */
    void setNextUi(final boolean value) {
        withFlag('nextUi', value)
    }

    /**
     * Validates the requested URL, rendered UI mode, and form readiness.
     */
    @Override
    void validatePage() {
        super.validatePage()
        validateUiMode()
        waitForReady()
    }

    /**
     * Validates an explicitly supplied URL, rendered UI mode, and form readiness.
     *
     * @param path expected URL path
     */
    @Override
    void validatePage(final String path) {
        super.validatePage(path)
        validateUiMode()
        waitForReady()
    }

    /**
     * Waits until the selected ACL creation form and editor are visible.
     */
    void waitForReady() {
        waitForElementVisible(pageBy)
        waitForElementVisible(EDITOR_BY)
    }

    /**
     * Loads the create page with editor text supplied through the query string.
     *
     * @param fileText policy text to prefill
     */
    void goWithPrefill(final String fileText) {
        go("${basePath}?fileText=${URLEncoder.encode(fileText, 'UTF-8')}")
    }

    /**
     * Loads the controller action without a project and waits for its standard error page.
     */
    void goWithoutProject() {
        String path = "/menu/createProjectAclFile"
        driver.get(context.client.baseUrl + path)
        waitForUrlToContain("/menu/createProjectAclFile")
        validateUiMode()
        WebElement body = waitForElementVisible(BODY_BY)
        waitForTextToBePresentInElement(body, 'Project parameter is required')
    }

    /**
     * Returns all visible text on the current page.
     */
    String getBodyText() {
        waitForElementVisible(BODY_BY).text
    }

    /**
     * Returns the create form heading.
     */
    String getHeadingText() {
        waitForElementVisible(headingBy).text.trim()
    }

    /**
     * Returns the current policy name.
     */
    String getName() {
        waitForElementVisible(NAME_INPUT_BY).getDomProperty("value")
    }

    /**
     * Enters a policy name.
     *
     * @param name policy name
     */
    void enterName(final String name) {
        WebElement input = waitForElementVisible(NAME_INPUT_BY)
        input.clear()
        input.sendKeys(name)
    }

    /**
     * Presses Enter in the policy name field.
     */
    void pressEnterInName() {
        waitForElementVisible(NAME_INPUT_BY).sendKeys(Keys.ENTER)
    }

    /**
     * Returns the project displayed by the form.
     */
    String getProjectValue() {
        waitForElementVisible(PROJECT_VALUE_BY).text.trim()
    }

    /**
     * Returns the policy-name help text.
     */
    String getNameHelpText() {
        waitForElementVisible(NAME_GROUP_BY).findElement(By.cssSelector(".help-block")).text.trim()
    }

    /**
     * Returns the synchronized editor value submitted by the form.
     */
    String getEditorText() {
        waitForElementVisible(pageBy)
        el(FILE_TEXT_INPUT_BY).getDomProperty("value")
    }

    /**
     * Waits for the editor to contain the expected text.
     *
     * @param expected expected editor value
     */
    void waitForEditorText(final String expected) {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until {
            getEditorText() == expected
        }
    }

    /**
     * Replaces the current editor contents.
     *
     * @param text new editor text
     */
    void replaceEditorText(final String text) {
        waitForElementVisible(EDITOR_BY)
        WebElement editor = el(EDITOR_INPUT_BY)
        editor.sendKeys(Keys.chord(Keys.CONTROL, "a"))
        editor.sendKeys(Keys.BACK_SPACE)
        editor.sendKeys(text)
        waitForEditorText(text)
    }

    /**
     * Returns whether keyboard focus is inside the ACE editor.
     */
    boolean isEditorFocused() {
        new WebDriverWait(driver, Duration.ofSeconds(30)).until {
            driver.switchTo().activeElement().getAttribute("class")?.contains("ace_text-input")
        }
    }

    /**
     * Opens the help menu.
     */
    void openHelp() {
        clickElementSafely(HELP_TOGGLE_BY)
        waitForElementVisible(HELP_MENU_BY)
    }

    /**
     * Returns the access-control documentation link attributes.
     */
    Map<String, String> getAccessControlHelpLink() {
        linkAttributes(HELP_ACCESS_CONTROL_LINK_BY)
    }

    /**
     * Returns the ACL format documentation link attributes.
     */
    Map<String, String> getAclFormatHelpLink() {
        linkAttributes(HELP_FORMAT_LINK_BY)
    }

    /**
     * Navigates back to the project ACL list.
     */
    void clickBack() {
        clickElementSafely(backLinkBy)
        waitForUrlToContain("/project/${project}/admin/acls")
    }

    /**
     * Submits the form using Save.
     */
    void clickSave() {
        clickElementSafely(SAVE_BUTTON_BY)
    }

    /**
     * Cancels creation and waits for the project ACL list.
     */
    void clickCancel() {
        clickElementSafely(CANCEL_BUTTON_BY)
        waitForUrlToContain("/project/${project}/admin/acls")
    }

    /**
     * Returns whether the page's beforeunload handler is armed for unsaved changes.
     *
     * Selenium sessions automatically dismiss native beforeunload prompts, so this dispatches
     * a cancelable synthetic event and observes the same handler result without navigating.
     */
    boolean hasUnsavedNavigationConfirmation() {
        (Boolean) ((JavascriptExecutor) driver).executeScript(
                """
                const event = new Event('beforeunload', {cancelable: true});
                const legacyResult = typeof window.onbeforeunload === 'function'
                    ? window.onbeforeunload.call(window, event)
                    : undefined;
                window.dispatchEvent(event);
                return Boolean(legacyResult) || event.defaultPrevented || event.returnValue === false;
                """
        )
    }

    /**
     * Returns global validation error text.
     */
    String getGlobalErrorText() {
        waitForElementVisible(GLOBAL_ERROR_BY).text
    }

    /**
     * Returns field-level editor errors.
     */
    List<String> getFileTextErrors() {
        els(fileTextErrorBy)*.text
    }

    /**
     * Returns whether the policy-name field is styled as invalid.
     */
    boolean hasNameErrorState() {
        waitForElementVisible(NAME_GROUP_BY).getAttribute("class").contains("has-error")
    }

    /**
     * Returns whether the editor field is styled as invalid.
     */
    boolean hasFileTextErrorState() {
        waitForElementVisible(FILE_TEXT_GROUP_BY).getAttribute("class").contains("has-error")
    }

    /**
     * Returns whether any field or global errors are displayed.
     */
    boolean hasErrors() {
        !els(GLOBAL_ERROR_BY).isEmpty() || hasNameErrorState() || hasFileTextErrorState()
    }

    /**
     * Returns whether the create form's primary controls are visible.
     */
    boolean hasCreateControls() {
        waitForElementVisible(NAME_INPUT_BY).isDisplayed() &&
                waitForElementVisible(EDITOR_BY).isDisplayed() &&
                waitForElementVisible(CANCEL_BUTTON_BY).isDisplayed() &&
                waitForElementVisible(SAVE_BUTTON_BY).isDisplayed()
    }

    private By getPageBy() {
        nextUi ? By.cssSelector("[data-testid='acl-create-page']") : By.id("createform")
    }

    private By getHeadingBy() {
        nextUi
                ? By.cssSelector("[data-testid='acl-file-card'] .card-title")
                : By.cssSelector("#createform > .card-header .card-title")
    }

    private By getFileTextErrorBy() {
        nextUi
                ? By.cssSelector("[data-testid='acl-file-text-error']")
                : By.cssSelector("#acl_text_editor_content .help-block")
    }

    private By getBackLinkBy() {
        nextUi
                ? By.cssSelector("[data-testid='acl-back-link']")
                : By.cssSelector("#createform > .card-header .card-title > a")
    }

    private void validateUiMode() {
        el(PAGE_UI_META_BY)
        Map<String, Object> metadata = (Map<String, Object>) ((JavascriptExecutor) driver).executeScript(
                "return JSON.parse(document.getElementById('pageUiMeta').textContent)"
        )
        String expectedUiType = nextUi ? 'next' : 'current'
        if (metadata.uiType != expectedUiType) {
            throw new IllegalStateException(
                    "Expected UI type '${expectedUiType}', but pageUiMeta reported '${metadata.uiType}'"
            )
        }
        if (metadata.nextUiCapable != true) {
            throw new IllegalStateException("Expected createProjectAclFile to be nextUi-capable")
        }
    }

    private Map<String, String> linkAttributes(final By locator) {
        WebElement link = waitForElementVisible(locator)
        [text: link.text.trim(), href: link.getAttribute("href"), target: link.getAttribute("target")]
    }
}
