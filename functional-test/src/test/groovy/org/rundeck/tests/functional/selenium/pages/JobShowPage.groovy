package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class JobShowPage extends BasePage{

    By jobDefinitionModalBy = By.cssSelector('a[href="#job-definition-modal"]')
    By notificationDefinitionBy = By.cssSelector('#detailtable.tab-pane > div.row > div.col-sm-12.table-responsive > table.table.item_details> tbody > tr > td.container > div.row > div.col-sm-12 > div.overflowx')
    By closeJobDefinitionModalBy = By.xpath("//*[contains(@id,'job-definition-modal_footer')]//*[@type='submit']")

    String loadPath = "/job/show"

    JobShowPage(final SeleniumContext context) {
        super(context)
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on job show selected page: " + driver.currentUrl)
        }
    }

    WebElement getJobDefinitionModal(){
        el jobDefinitionModalBy
    }

    WebElement getNotificationDefinition(){
        el notificationDefinitionBy
    }

    WebElement getCloseJobDefinitionModalButton() {
        el closeJobDefinitionModalBy
    }
}
