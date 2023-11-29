package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

class JobShowPage extends BasePage{

    static final String PAGE_PATH = "/job/show"
    String loadPath = PAGE_PATH
    By runJobBtn = By.id("execFormRunButton")
    By logOutputBtn = By.id('btn_view_output')

    JobShowPage(final SeleniumContext context) {
        super(context)
    }

    WebElement getRunJobBtn(){
        el runJobBtn
    }

    WebElement getLogOutputBtn(){
        el logOutputBtn
    }

    void waitForLogOutput (By logOutput, Integer number, Integer seconds){
        new WebDriverWait(driver, Duration.ofSeconds(seconds)).until(ExpectedConditions.numberOfElementsToBeMoreThan(logOutput,number))

    }

    void validatePage() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on job show page: " + driver.currentUrl)
        }
    }
}
