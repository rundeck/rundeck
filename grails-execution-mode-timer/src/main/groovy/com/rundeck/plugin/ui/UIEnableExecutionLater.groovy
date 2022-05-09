package com.rundeck.plugin.ui

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import groovy.transform.CompileStatic

@Plugin(name = UIEnableExecutionLater.PROVIDER_NAME, service = ServiceNameConstants.UI)
@PluginDescription(title = UIEnableExecutionLater.PLUGIN_TITLE, description = UIEnableExecutionLater.PLUGIN_DESC)
@CompileStatic
class UIEnableExecutionLater  implements UIPlugin, PluginResourceLoader {

    static final String PROVIDER_NAME = 'ui-execution-mode-later'
    static final String PLUGIN_TITLE = "Execution Mode Later UI Plugin"
    static final String PLUGIN_DESC = "Enable/Disable execution/schedule later."

    /** JS scripts */
    private static final List<String> SCRIPTS = [
            "lib/knockout.unobtrusive.min.js",
            "lib/support.js",
            "js/init.js",
            "js/executionMode.js",
            "js/editProject.js",
    ]

    /** css stylesheets */
    private static final List<String> STYLES = [
            "css/execution-mode-later-ui-styles.css",
    ]

    private static final List<String> HTML = [
            "html/execution-mode.html"
    ]

    /** Full Resource list */
    private static final List<String> ALL_RESOURCES = SCRIPTS + STYLES + HTML

    private static final List<String> PAGES = ["menu/projectHome","framework/editProject","menu/projectHome","menu/home","menu/executionMode"]

    @Override
    List<String> listResources() throws PluginException, IOException {
        return ALL_RESOURCES
    }

    @Override
    InputStream openResourceStreamFor(String name) throws PluginException, IOException {
        return this.getClass().getResourceAsStream("/" + name)
    }

    @Override
    boolean doesApply(String path) {
        return PAGES.contains(path)
    }

    @Override
    List<String> resourcesForPath(String path) {
        return ALL_RESOURCES
    }

    @Override
    List<String> scriptResourcesForPath(String path) {
        return SCRIPTS
    }

    @Override
    List<String> styleResourcesForPath(String path) {
        return STYLES
    }

    @Override
    List<String> requires(String path) {
        return null
    }
}
