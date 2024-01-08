package org.rundeck.tests.functional.selenium.pages.jobs

import groovy.transform.CompileStatic
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.tests.functional.selenium.pages.BasePage
import org.rundeck.util.container.SeleniumContext

/**
 * Job upload page
 */
@CompileStatic
class JobEditPage extends BasePage {

    private static final String secureOptionSelectors = 'optdetail opt-detail-ext autohilite autoedit'
    private static final String secureOptionDefaultValueId = 'opt_defaultValue'
    private static final String secureOptionDefaultValueHelpSelector = 'help-block'

    String loadPath
    By workflowTab = By.cssSelector('#job_edit_tabs > li > a[href=\'#tab_workflow\']')
    By secureOptionSpan = By.xpath("//span[@class='${secureOptionSelectors}']")
    By defaultValueInput = By.id(secureOptionDefaultValueId)
    By secureOptionDefaultValueHelpDiv = By.xpath("//span[@class='${secureOptionDefaultValueHelpSelector}']")

    JobEditPage(final SeleniumContext context) {
        super(context)
    }

    void loadPathToEditByUuid(String projectName, String jobUuid) {
        loadPath = "/project/${projectName}/job/edit/${jobUuid}"
    }

    void validatePage() {
        if (!driver.currentUrl.contains(loadPath)) {
            throw new IllegalStateException("Not on job edit page: " + driver.currentUrl)
        }
    }

    WebElement workflowTabElement(){
        el workflowTab
    }

    WebElement secureOptionSpanElement(){
        el secureOptionSpan
    }

    WebElement defaultValueInputElement(){
        el defaultValueInput
    }

    WebElement secureOptionDefaultValueHelpElement(){
        el secureOptionDefaultValueHelpDiv
    }

}
