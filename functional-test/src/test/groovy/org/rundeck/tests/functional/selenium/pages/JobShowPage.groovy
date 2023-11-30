package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class JobShowPage extends BasePage{
    By jobUuidText = By.cssSelector('#subtitlebar > div > div.subtitle-head-item.flex-item-auto > section > div > div')
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

    WebElement getJobUuidText(){
        el jobUuidText
    }
}
