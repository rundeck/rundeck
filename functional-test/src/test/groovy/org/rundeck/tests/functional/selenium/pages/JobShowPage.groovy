package org.rundeck.tests.functional.selenium.pages

import org.rundeck.util.container.SeleniumContext

class JobShowPage extends BasePage{

    static final String PAGE_PATH = "/job/show"
    String loadPath = PAGE_PATH

    JobShowPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on jobs list page: " + driver.currentUrl)
        }
    }
}
