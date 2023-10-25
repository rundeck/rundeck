package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class JobsListPage extends BasePage{

    static final String PAGE_PATH = "/jobs"
    By createJobButton = By.partialLinkText('New Job')

    String loadPath = PAGE_PATH

    JobsListPage(SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(PAGE_PATH)) {
            throw new IllegalStateException("Not on jobs list page: " + driver.currentUrl)
        }
    }

    WebElement getCreateJobButton(){
        el createJobButton
    }
}
