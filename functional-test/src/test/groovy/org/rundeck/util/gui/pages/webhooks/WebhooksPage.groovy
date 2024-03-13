package org.rundeck.util.gui.pages.webhooks

import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

class WebhooksPage extends BasePage{

    String loadPath

    WebhooksPage(SeleniumContext context) {
        super(context)
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
