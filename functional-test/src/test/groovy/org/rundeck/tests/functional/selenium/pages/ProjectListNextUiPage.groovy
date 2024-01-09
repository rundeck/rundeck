package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class ProjectListNextUiPage extends ProjectListPage {

    ProjectListNextUiPage(final SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        return super.getLoadPath()+"?nextUi=true"
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(loadPath)) {
            throw new IllegalStateException("Not on project list page: " + driver.currentUrl)
        }
    }

    @Override
    WebElement getProjectCountSection() {
        return el(By.cssSelector('#projectCount'));
    }
}
