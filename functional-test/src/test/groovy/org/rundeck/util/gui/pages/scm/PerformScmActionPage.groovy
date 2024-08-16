package org.rundeck.util.gui.pages.scm

import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.StaleElementReferenceException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

import org.rundeck.util.common.scm.ScmIntegration
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.common.scm.ScmActionId
import org.rundeck.util.gui.pages.BasePage
import org.rundeck.util.common.WaitingTime

import java.time.Duration

class PerformScmActionPage extends BasePage {

    String loadPath
    private static final String PERFORM_ACTION_URL_PART = "performAction?actionId="
    private static final ScmIntegration INTEGRATION_FOR_COMMIT = ScmIntegration.EXPORT
    private static final By INFO_MESSAGE_BOX_LOCATOR = By.cssSelector(".alert.alert-info")
    private static final By ERROR_MESSAGE_BOX_LOCATOR = By.cssSelector(".alert.alert-danger")

    PerformScmActionPage(SeleniumContext context, String project) {
        super(context)
        loadPath = "/project/${project}"
    }

    String commitJobChanges(String jobUuid, String commitMessage, boolean shouldWaitAfterCommit = true){
        loadPath += "/job/${jobUuid}/scm/${INTEGRATION_FOR_COMMIT.name}/${PERFORM_ACTION_URL_PART}${ScmActionId.JOB_COMMIT.name}"
        super.go()

        driver.findElement(By.name("pluginProperties.message")).sendKeys(commitMessage)
        driver.findElement(By.name("submit")).click()

        String resultText = waitForResultMessageBox().getText()

        if(shouldWaitAfterCommit)
            Thread.sleep(WaitingTime.MODERATE.duration.toMillis())

        return resultText
    }

    private WebElement waitForResultMessageBox(){
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.or(
                        ExpectedConditions.elementToBeClickable(ERROR_MESSAGE_BOX_LOCATOR),
                        ExpectedConditions.elementToBeClickable(INFO_MESSAGE_BOX_LOCATOR))
                )

        try {
            return driver.findElement(INFO_MESSAGE_BOX_LOCATOR)
        } catch(NoSuchElementException ignored){
            return driver.findElement(ERROR_MESSAGE_BOX_LOCATOR)
        }
    }

    /**
     * Cannot go since it doesn't have a defined loadpath until the action method is called
     */
    @Override
    void go() {
    }

    @Override
    void validatePage() {
        if (!driver.currentUrl.endsWith(loadPath)) {
            throw new IllegalStateException("Couldn't browse Perform SCM Action Page. Expected: ${loadPath} Actual: ${driver.currentUrl}")
        }
    }
}
