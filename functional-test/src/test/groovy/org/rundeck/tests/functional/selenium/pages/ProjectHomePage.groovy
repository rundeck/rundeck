package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import org.rundeck.util.container.SeleniumContext

import java.time.Duration

class ProjectHomePage extends BasePage {

    static final String PAGE_PATH = '/project/$PROJECT/home'
    String loadPath = PAGE_PATH

    ProjectHomePage(final SeleniumContext context) {
        super(context)
    }

    void goProjectHome(String projectName){
        def path = PAGE_PATH.replaceAll('\\$PROJECT', projectName)
        driver.get(context.client.baseUrl + path)
        new WebDriverWait(driver, Duration.ofSeconds(30)).until(
            ExpectedConditions.urlToBe(context.client.baseUrl + path)
        )
    }
}