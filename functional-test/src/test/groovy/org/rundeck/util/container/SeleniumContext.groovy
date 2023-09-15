package org.rundeck.util.container

import groovy.transform.CompileStatic
import org.openqa.selenium.WebDriver

@CompileStatic
interface SeleniumContext {
    WebDriver getDriver()

    RdClient getClient()
}
