package org.rundeck.util.gui.pages.webhooks

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class WebhooksPage extends BasePage{

    String loadPath
    By createWebhookButtonBy = By.cssSelector(".btn.btn-primary.btn-full")

    WebhooksPage(SeleniumContext context) {
        super(context)
    }

    WebElement getCreateWebhookButton(){
        el createWebhookButtonBy
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on execution show page: " + driver.currentUrl)
        }
    }

    void loadPageForProject(String projectName){
        this.loadPath = "/webhook/admin?project=${projectName}"
    }
}
