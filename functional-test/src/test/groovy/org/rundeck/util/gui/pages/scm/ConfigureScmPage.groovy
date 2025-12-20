package org.rundeck.util.gui.pages.scm

import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

class ConfigureScmPage extends BasePage{

    String loadPath

    ConfigureScmPage(SeleniumContext context, String project) {
        super(context)
        loadPath = "/project/${project}/scm"
    }

    void disableScmExport() {
        toggleSCM(false, true)
    }

    void disableScmImport() {
        toggleSCM(false, false)
    }

    void enableScmImport() {
        toggleSCM(true, false)
    }

    void enableScmExport() {
        toggleSCM(true, true)
    }

    private void toggleSCM(boolean enable, boolean export) {
        String action = enable ? "enable" : "disable"
        String oppositeAction = !enable ? "enable" : "disable"
        String integration = export ? "export" : "import"
        By dataTargetValueBy = By.xpath("//span[@data-target='#${action}Plugin${integration}']")
        By oppositeDataTargetValueBy = By.xpath("//span[@data-target='#${oppositeAction}Plugin${integration}']")

        new WebDriverWait(driver, Duration.ofSeconds(15))
                    .ignoring(StaleElementReferenceException.class)
                    .until(ExpectedConditions.or(
                            ExpectedConditions.elementToBeClickable(dataTargetValueBy),
                            ExpectedConditions.elementToBeClickable(oppositeDataTargetValueBy)
                    ))

        waitForNumberOfElementsToBe(dataTargetValueBy, 1)
        WebElement toggleButton = driver.findElement(dataTargetValueBy)
        toggleButton.click()
        waitForModal(1)
        byAndWait(modalField).findElement(By.xpath(".//input[@type='submit'][@value='Yes']")).click()
        
        waitForModal(0)
        
        String expectedMessageText = enable ? "Plugin enabled for SCM ${integration}" : "Plugin disabled"
        By successBannerBy = By.cssSelector(".alert.alert-info")
        
        WebElement banner = waitForElementVisible(successBannerBy)
        waitForTextToBePresentInElement(banner, expectedMessageText)
    }
}
