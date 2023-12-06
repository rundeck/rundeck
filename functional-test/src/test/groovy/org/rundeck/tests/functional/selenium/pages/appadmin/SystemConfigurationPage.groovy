package org.rundeck.tests.functional.selenium.pages.appadmin

import groovy.transform.CompileStatic
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * System configuration page
 */
@CompileStatic
class SystemConfigurationPage extends BasePage {

    String loadPath = ""

    SystemConfigurationPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.containsIgnoreCase("config")) {
            throw new IllegalStateException("Not on key system configuration page: " + driver.currentUrl)
        }
    }

}
