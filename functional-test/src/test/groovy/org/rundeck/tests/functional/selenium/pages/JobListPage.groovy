package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class JobListPage extends BasePage {

    By newJob = By.partialLinkText('New Job')

    String loadPath = "/jobs"

    JobListPage(final SeleniumContext context) {
        super(context)
    }

    void loadPathToEditJob(String projectName, String jobId) {
        loadPath = "/project/${projectName}/job/edit/${jobId}"
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on job list page: " + driver.currentUrl)
        }
    }

    WebElement getNewJobButton() {
        el newJob
    }
}
