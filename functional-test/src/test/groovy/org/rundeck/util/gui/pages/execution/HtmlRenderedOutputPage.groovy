package org.rundeck.util.gui.pages.execution

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class HtmlRenderedOutputPage extends BasePage{

    String loadPath = ""
    By logLevelNormalBy = By.cssSelector(".log_line.level_normal")

    /**
     * Create a new page
     * @param context
     */
    HtmlRenderedOutputPage(SeleniumContext context) {
        super(context)
    }

    WebElement getLogLevelNormalLogLine(){
        el logLevelNormalBy
    }

}