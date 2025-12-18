package org.rundeck.util.gui.scm

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.gui.pages.jobs.JobShowPage

import java.time.Duration

class ScmStatusBadge {
    final List iconClasses
    final String badgeText
    final String tooltips
    // Use CSS selector targeting the .scm_status span which contains the title attribute
    // The structure is: <span class="scm_status" title="tooltip"><span><i>icon</i>text</span></span>
    static final By elementSelector = By.cssSelector("#jobInfo_ .vue-ui-socket .scm_status")
    static final String loadingFromServerText = "Loading SCM Status..."
    static final String tooltipsAttribute = "title"

    ScmStatusBadge(JobShowPage jobShowPage){
        // Get the element from the wait method to avoid race condition of finding it twice
        WebElement statusBadge = waitForStatusBadgeAndReturn(jobShowPage)
        this.tooltips = statusBadge.getAttribute(tooltipsAttribute)
        this.badgeText = statusBadge.getText()
        this.iconClasses = statusBadge.findElement(By.tagName("i")).getAttribute("class").split(" ")
    }

    /**
     * Waits for the SCM status badge to be present and fully loaded, then returns the WebElement.
     * This avoids the race condition where the element might be removed/re-added between checks.
     *
     * @param jobShowPage The job show page containing the badge
     * @param withRetry Whether to retry by refreshing if the element is not found
     * @return The loaded SCM status badge WebElement
     */
    WebElement waitForStatusBadgeAndReturn(JobShowPage jobShowPage, boolean withRetry = true){
        try{
            // First wait for the element to be present on the page
            new WebDriverWait(jobShowPage.driver, Duration.ofSeconds(15)).until(
                    ExpectedConditions.presenceOfElementLocated(elementSelector)
            )
            // Then wait for it to finish loading (text should not be "Loading SCM Status...")
            new WebDriverWait(jobShowPage.driver, Duration.ofSeconds(10)).until(
                    ExpectedConditions.not(
                            ExpectedConditions.textToBe(elementSelector, loadingFromServerText)
                    )
            )
            // Return the element after it has finished loading to avoid stale element references
            return jobShowPage.driver.findElement(elementSelector)
        }catch(Exception e){
            if(withRetry){
                jobShowPage.driver.navigate().refresh()
                return waitForStatusBadgeAndReturn(jobShowPage, false)
            }
            throw e
        }
    }

    /**
     * @deprecated Use waitForStatusBadgeAndReturn() instead to avoid race conditions
     */
    void checkStatusBadge(JobShowPage jobShowPage, boolean withRetry = true){
        waitForStatusBadgeAndReturn(jobShowPage, withRetry)
    }
}
