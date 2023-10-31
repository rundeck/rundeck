package org.rundeck.tests.functional.selenium.pages.appadmin

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * System configuration page
 */
@CompileStatic
class SystemConfigurationPage extends BasePage {

    By pageTitleBy = By.xpath("//*[text()='System Configuration']")

    String loadPath = ""

    SystemConfigurationPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        byAndWait pageTitleBy
    }


}
