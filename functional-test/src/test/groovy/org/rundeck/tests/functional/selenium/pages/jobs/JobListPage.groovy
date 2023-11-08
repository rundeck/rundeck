package org.rundeck.tests.functional.selenium.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * Job list page
 */
@CompileStatic
class JobListPage extends BasePage {

    String loadPath = "/jobs"

    JobListPage(final SeleniumContext context) {
        super(context)
    }

    void loadPathToEditJob(String projectName, String jobId) {
        loadPath = "/project/${projectName}/job/edit/${jobId}"
    }

    void loadPathToShowJob(String projectName, String jobId) {
        loadPath = "/project/${projectName}/job/show/${jobId}"
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on job list page: " + driver.currentUrl)
        }
    }

}
