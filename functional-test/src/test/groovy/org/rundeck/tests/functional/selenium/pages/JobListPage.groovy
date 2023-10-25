package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class JobListPage extends BasePage {

    By createJob = By.linkText("Create a new Job")

    static final String PAGE_PATH = "/jobs"

    JobListPage(final SeleniumContext context) {
        super(context, null)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on all jobs page: " + driver.currentUrl)
        }
    }

    WebElement getCreateJobButton() {
        el createJob
    }
}
