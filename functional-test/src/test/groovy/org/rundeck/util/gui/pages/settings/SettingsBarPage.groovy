package org.rundeck.util.gui.pages.settings

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.support.ui.Select
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * Page object for the Settings Bar component in the utility bar.
 * Contains selectors and helper methods for interacting with:
 * - Help button
 * - Settings button (opens modal)
 * - Settings modal (Theme and UI Early Access tabs)
 * - NextUI indicator
 */
class SettingsBarPage extends BasePage {

    // No load path - this is a component that appears on multiple pages
    String loadPath = ""

    // Settings bar selectors
    By settingsBarBy = By.cssSelector(".settings-bar")
    By settingsBarButtonBy = By.cssSelector(".settings-bar__button")
    By supportButtonBy = By.xpath("//button[contains(@class, 'settings-bar__button')]/i[contains(@class, 'fa-life-ring')]/..")
    By settingsCogButtonBy = By.xpath("//button[contains(@class, 'settings-bar__button')]/i[contains(@class, 'fa-cog')]/..")
    
    // Modal selectors
    By settingsModalBy = By.cssSelector(".settings-modal")
    By settingsModalOverlayBy = By.cssSelector(".settings-modal-overlay")
    By settingsModalCloseBy = By.cssSelector(".settings-modal__close")
    By settingsModalTabBy = By.cssSelector(".settings-modal__tab")
    By themeTabBy = By.xpath("//button[contains(@class, 'settings-modal__tab') and contains(text(), 'Theme')]")
    By uiEarlyAccessTabBy = By.xpath("//button[contains(@class, 'settings-modal__tab') and contains(text(), 'UI Early Access')]")
    By activeTabBy = By.cssSelector(".settings-modal__tab--active")
    
    // Theme panel selectors
    By themeSelectBy = By.cssSelector(".settings-modal select")
    By themePanelBy = By.cssSelector(".settings-theme-panel")
    
    // UI Early Access panel selectors
    By uiEarlyAccessPanelBy = By.cssSelector(".settings-nextui-panel")
    By nextUiToggleBy = By.cssSelector(".settings-toggle__input")
    By nextUiToggleLabelBy = By.cssSelector(".settings-toggle__label")
    
    // NextUI indicator selectors
    By nextUiIndicatorBy = By.cssSelector(".settings-bar__nextui-indicator")
    By nextUiIndicatorTextBy = By.cssSelector(".settings-bar__nextui-text")

    SettingsBarPage(SeleniumContext context) {
        super(context)
    }

    /**
     * Gets the settings bar element
     */
    WebElement getSettingsBar() {
        el(settingsBarBy)
    }

    /**
     * Gets all settings bar buttons
     */
    List<WebElement> getSettingsBarButtons() {
        els(settingsBarButtonBy)
    }

    /**
     * Gets the settings button (cog icon)
     */
    WebElement getSettingsCogButton() {
        waitForElementToBeClickable(settingsCogButtonBy)
        el(settingsCogButtonBy)
    }

    /**
     * Clicks the settings button (cog icon) to open the modal
     */
    void clickSettingsButton() {
        settingsCogButton.click()
    }

    /**
     * Gets the support button
     */
    WebElement getSupportButton() {
        waitForElementToBeClickable(supportButtonBy)
        el(supportButtonBy)
    }

    /**
     * Clicks the support button
     */
    void clickSupportButton() {
        supportButton.click()
    }

    /**
     * Gets the settings modal element
     */
    WebElement getSettingsModal() {
        waitForElementVisible(settingsModalBy)
        el(settingsModalBy)
    }

    /**
     * Gets the modal overlay element
     */
    WebElement getSettingsModalOverlay() {
        waitForElementVisible(settingsModalOverlayBy)
        el(settingsModalOverlayBy)
    }

    /**
     * Gets the modal close button
     */
    WebElement getSettingsModalClose() {
        waitForElementToBeClickable(settingsModalCloseBy)
        el(settingsModalCloseBy)
    }

    /**
     * Checks if the settings modal is open
     */
    boolean isModalOpen() {
        els(settingsModalBy).size() > 0
    }

    /**
     * Waits for the modal to be visible
     */
    void waitForModalVisible() {
        waitForElementVisible(settingsModalBy)
    }

    /**
     * Waits for the modal to be closed
     */
    void waitForModalClosed() {
        waitForNumberOfElementsToBe(settingsModalBy, 0)
    }

    /**
     * Closes the modal by clicking the close button
     */
    void closeModal() {
        settingsModalClose.click()
        waitForModalClosed()
    }

    /**
     * Closes the modal by clicking the overlay
     */
    void closeModalByOverlayClick() {
        def overlay = settingsModalOverlay
        // Click on the top-left corner of the overlay (outside the modal)
        new Actions(driver)
                .moveToElement(overlay, 10, 10)
                .click()
                .perform()
        waitForModalClosed()
    }

    /**
     * Gets the Theme tab element
     */
    WebElement getThemeTab() {
        waitForElementToBeClickable(themeTabBy)
        el(themeTabBy)
    }

    /**
     * Gets the UI Early Access tab element
     */
    WebElement getUiEarlyAccessTab() {
        waitForElementToBeClickable(uiEarlyAccessTabBy)
        el(uiEarlyAccessTabBy)
    }

    /**
     * Clicks the Theme tab
     */
    void clickThemeTab() {
        themeTab.click()
    }

    /**
     * Clicks the UI Early Access tab
     */
    void clickUiEarlyAccessTab() {
        uiEarlyAccessTab.click()
    }

    /**
     * Gets the currently active tab element
     */
    WebElement getActiveTab() {
        waitForElementVisible(activeTabBy)
        el(activeTabBy)
    }

    /**
     * Checks if Theme tab is active
     */
    boolean isThemeTabActive() {
        themeTab.getAttribute("class").contains("active")
    }

    /**
     * Checks if UI Early Access tab is active
     */
    boolean isUiEarlyAccessTabActive() {
        uiEarlyAccessTab.getAttribute("class").contains("active")
    }

    /**
     * Gets the theme select element
     */
    WebElement getThemeSelect() {
        waitForElementToBeClickable(themeSelectBy)
        el(themeSelectBy)
    }

    /**
     * Gets the current theme value
     */
    String getCurrentTheme() {
        new Select(themeSelect).firstSelectedOption.getAttribute("value")
    }

    /**
     * Sets the theme
     */
    void setTheme(String theme) {
        new Select(themeSelect).selectByValue(theme)
    }

    /**
     * Gets the NextUI toggle input element
     */
    WebElement getNextUiToggle() {
        waitForElementVisible(nextUiToggleBy)
        el(nextUiToggleBy)
    }

    /**
     * Gets the NextUI toggle label (clickable area)
     */
    WebElement getNextUiToggleLabel() {
        waitForElementToBeClickable(nextUiToggleLabelBy)
        el(nextUiToggleLabelBy)
    }

    /**
     * Checks if the NextUI toggle is enabled
     */
    boolean isNextUiToggleEnabled() {
        nextUiToggle.isSelected()
    }

    /**
     * Toggles the NextUI switch
     */
    void toggleNextUi() {
        nextUiToggleLabel.click()
    }

    /**
     * Checks if the NextUI indicator is visible
     */
    boolean isNextUiIndicatorVisible() {
        els(nextUiIndicatorBy).size() > 0
    }

    /**
     * Gets the NextUI indicator element
     */
    WebElement getNextUiIndicator() {
        waitForElementToBeClickable(nextUiIndicatorBy)
        el(nextUiIndicatorBy)
    }

    /**
     * Gets the NextUI indicator text element
     */
    WebElement getNextUiIndicatorText() {
        waitForElementVisible(nextUiIndicatorTextBy)
        el(nextUiIndicatorTextBy)
    }

    /**
     * Clicks the NextUI indicator to open the modal
     */
    void clickNextUiIndicator() {
        nextUiIndicator.click()
    }
}
