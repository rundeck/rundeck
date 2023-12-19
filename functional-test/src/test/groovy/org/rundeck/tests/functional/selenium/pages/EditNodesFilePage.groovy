package org.rundeck.tests.functional.selenium.pages

import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import org.rundeck.util.container.SeleniumContext

class EditNodesFilePage extends BasePage{

    static final String PAGE_PATH = "/nodes/source"
    static final String editSuffix = "edit"
    int index
    String project

    By aceEditorGutter = By.className("ace_gutter")

    /**
     * Create a new page
     * @param context
     */
    EditNodesFilePage(SeleniumContext context) {
        super(context)
    }

    @Override
    String getLoadPath() {
        if(!project){
            throw new IllegalStateException("project is not set, cannot load nodes.")
        }
        if( !index ){
            throw new IllegalStateException("No node index specified, cannot load node file edit page.")
        }
        return "/project/${project}${PAGE_PATH}/${index}/${editSuffix}"
    }

    WebElement aceGutterElement(){
        el aceEditorGutter
    }

}