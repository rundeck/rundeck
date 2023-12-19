package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class EditNodesPage extends BasePage{

    static final String PAGE_PATH = "/nodes/sources"
    String project

    By modifyResourceButton = By.partialLinkText("Modify")
    By aceEditorGutter = By.xpath("//div[contains(@class, 'ace_layer') and contains(@class, 'ace_gutter-layer') and contains(@class, 'ace_folding-enabled')]")

    /**
     * Create a new page
     * @param context
     */
    EditNodesPage(SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        if(!project){
            throw new IllegalStateException("project is not set, cannot load nodes.")
        }
        return "/project/${project}${PAGE_PATH}"
    }

    WebElement modifyResourceButtonElement(){
        el modifyResourceButton
    }

    WebElement aceGutterElement(){
        el aceEditorGutter
    }

}