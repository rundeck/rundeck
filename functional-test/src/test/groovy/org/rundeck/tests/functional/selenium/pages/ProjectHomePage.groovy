package org.rundeck.tests.functional.selenium.pages

import org.rundeck.util.container.SeleniumContext

class ProjectHomePage extends BasePage {

    static final String PAGE_PATH = '/project/$PROJECT/home'
    String loadPath = PAGE_PATH

    ProjectHomePage(final SeleniumContext context) {
        super(context)
    }

    void goProjectHome(String projectName){
        driver.get(context.client.baseUrl+loadPath.replaceAll('\\$PROJECT', projectName))
    }
}