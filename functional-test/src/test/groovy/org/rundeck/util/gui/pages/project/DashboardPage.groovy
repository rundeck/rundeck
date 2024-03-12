package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

@CompileStatic
class DashboardPage extends BasePage{

    String loadPath = ""
    By projectDescriptionBy = By.className("text-project-description")
    By projectLabelBy = By.xpath("//div[@data-ko-bind='projectHome']")

    DashboardPage(SeleniumContext context) {
        super(context)
    }

    def expectProjectDescriptionToBe(String projectDescription){
        new WebDriverWait(driver,  Duration.ofSeconds(10)).until(
                ExpectedConditions.textToBe(projectDescriptionBy, projectDescription)
        )
    }

    def expectProjectLabelToBe(String projectLabel){
        new WebDriverWait(driver,  Duration.ofSeconds(10)).until(
                ExpectedConditions.textToBePresentInElement(driver.findElement(projectLabelBy).findElement(By.tagName("h3")), projectLabel)
        )
    }
}
