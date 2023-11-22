package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.rundeck.util.container.SeleniumContext

class JobsList_nextui_Page extends JobsListPage{


    JobsList_nextui_Page(final SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        return super.getLoadPath()+"?nextUi=true"
    }

    void validatePage() {
        if (!driver.currentUrl.endsWith(loadPath)) {
            throw new IllegalStateException("Not on jobs list page: " + driver.currentUrl)
        }
    }
}
