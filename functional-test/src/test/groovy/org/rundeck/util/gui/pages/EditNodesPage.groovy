package org.rundeck.util.gui.pages


import org.rundeck.util.container.SeleniumContext

class EditNodesPage extends BasePage{

    static final String PAGE_PATH = "/nodes/sources"
    String project

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

}