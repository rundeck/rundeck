package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

@CompileStatic
class ProjectListPage extends BasePage {
    static final String PAGE_PATH = "/menu/home"
    By projectCountBy = By.cssSelector('#layoutBody span.text-h3 > span')
    By firstRunCreateBtnBy = By.cssSelector('#firstRun .splash-screen .btn.btn-primary')

    ProjectListPage(final SeleniumContext context) {
        super(context)
    }
    String loadPath = PAGE_PATH

    @Override
    void validate() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on Project List page: " + driver.currentUrl)
        }
    }

    WebElement getProjectCountSection() {
        el projectCountBy
    }
    WebElement getFirstRunCreateBtn() {
        el firstRunCreateBtnBy
    }
}
