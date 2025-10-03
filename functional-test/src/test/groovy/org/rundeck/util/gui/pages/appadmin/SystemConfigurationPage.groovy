package org.rundeck.util.gui.pages.appadmin

import groovy.transform.CompileStatic
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

/**
 * System configuration page
 */
@CompileStatic
class SystemConfigurationPage extends BasePage {

    String loadPath = "systemConfig"

    SystemConfigurationPage(final SeleniumContext context) {
        super(context)
    }

}
