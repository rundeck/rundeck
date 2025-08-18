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
    static final By elementSelector = By.xpath("//*[@id='jobInfo_']/span[2]/span")
    static final String loadingFromServerText = "Loading SCM Status..."
    static final String tooltipsAttribute = "title"

    ScmStatusBadge(JobShowPage jobShowPage){
        checkStatusBadge(jobShowPage)
        WebElement statusBadge = jobShowPage.driver.findElement(elementSelector)
        this.tooltips = statusBadge.getAttribute(tooltipsAttribute)
        this.badgeText = statusBadge.getText()
        this.iconClasses = statusBadge.findElement(By.tagName("i")).getAttribute("class").split(" ")
    }

    void checkStatusBadge(JobShowPage jobShowPage, boolean withRetry = true){
        try{
            new WebDriverWait(jobShowPage.driver, Duration.ofSeconds(10)).until(
                    ExpectedConditions.not(
                            ExpectedConditions.textToBe(elementSelector, loadingFromServerText)
                    )
            )
        }catch(Exception e){
            if(withRetry){
                jobShowPage.driver.navigate().refresh()
                checkStatusBadge(jobShowPage, false)
            }
        }
    }
}
