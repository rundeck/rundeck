package org.rundeck.tests.functional.selenium

import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.annotations.SeleniumCoreTest
import org.rundeck.util.setup.BaseSpec
import org.rundeck.util.setup.NavLinkTypes

import java.time.Duration

//@SeleniumCoreTest
class JobsSpec extends BaseSpec {

    def "edit job and set groups"() {
        when:
            def description = "description demo"
            doLogin()
            createProject(toCamelCase(specificationContext.currentFeature.name), null)
            intoProjectGoTo(NavLinkTypes.JOBS)
        then:
            createSimpleJob('simpleJob', 'echo ${job.id}')
            driver.findElement(By.linkText("Action")).click()
            driver.findElement(By.linkText("Edit this Job…")).click()
            new WebDriverWait(driver, Duration.ofSeconds(10)).until {
                ExpectedConditions.visibilityOfElementLocated {
                    By.className("ace_text-input")}
            }
            sleep(5000)
            driver.findElement(By.className("ace_text-input")).sendKeys(description)
            driver.findElement(By.id("jobUpdateSaveButton")).click()
            description == driver.findElement(By
                    .xpath("//*[@class=\"section-space\"]//*[@class=\"h5 text-strong\"]")).getText()
        cleanup:
            deleteProject(true)
    }

}
