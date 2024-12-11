package org.rundeck.util.gui.pages.project

import groovy.transform.CompileStatic
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.BasePage

@CompileStatic
class AdhocPage extends BasePage implements ActivityListTrait {
    String loadPath = ""

    AdhocPage(final SeleniumContext context, final String project) {
        super(context)
        loadDashboardForProject(project)
    }

    void loadDashboardForProject(String projectName) {
        this.loadPath = "/project/${projectName}/command/run"
    }

}
