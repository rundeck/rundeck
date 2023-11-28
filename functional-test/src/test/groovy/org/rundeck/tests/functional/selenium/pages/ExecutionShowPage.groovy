package org.rundeck.tests.functional.selenium.pages

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

@CompileStatic
class ExecutionShowPage extends BasePage{

    static final String PAGE_PATH = "/execution/show/37"
    String loadPath = PAGE_PATH

    By logOutputBtn = By.id('btn_view_output')
    By followBtn = By.partialLinkText('Follow')

    ExecutionShowPage(SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(PAGE_PATH)) {
            throw new IllegalStateException("Not on execution page: " + driver.currentUrl)
        }
    }

    WebElement getLogOutputBtn(){
        el logOutputBtn
    }

    WebElement getFollowBtn(){
        el followBtn
    }

}
