package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

import java.time.Duration

@CompileStatic
class DashboardPage extends BasePage {

    String loadPath = ""
    By projectDescriptionBy = By.className("text-project-description")
    By projectLabelBy = By.xpath("//div[@data-ko-bind='projectHome']")
    By readmeMarkDownBy = By.className("markdown-body")
    By projectSummaryBy = By.id("projectHome-summary")
    By projectSummaryCountLinkBy = By.cssSelector("#projectHome-summary a .summary-count")
    By executionCountBy = By.cssSelector(".summary-count.text-info") // Execution count
    By userCountBy = By.cssSelector("div[data-test-id='user-count'] .text-info") // User count
    By userElementBy = By.cssSelector(".users li")

    DashboardPage(SeleniumContext context) {
        super(context)
    }

    DashboardPage(SeleniumContext context, String project) {
        super(context)
        loadDashboardForProject(project)
    }

    void loadDashboardForProject(String projectName) {
        this.loadPath = "/project/${projectName}/home"
    }

    def expectProjectDescriptionToBe(String projectDescription) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                ExpectedConditions.textToBe(projectDescriptionBy, projectDescription)
        )
    }

    def expectProjectLabelToBe(String projectLabel) {
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(
                ExpectedConditions.textToBePresentInElement(driver.findElement(projectLabelBy).findElement(By.tagName("h3")), projectLabel)
        )
    }

    def getCheckReadme() {
        (el readmeMarkDownBy).getText()
    }

    WebElement getProjectSummary() {
        el projectSummaryBy
    }

    WebElement getProjectSummaryCountLink() {
        el projectSummaryCountLinkBy
    }

    WebElement getExecutionCountElement() {
        el executionCountBy
    }

    WebElement getUserCountElement() {
        el userCountBy
    }

    WebElement getUserElement() {
        el userElementBy
    }
}