package com.rundeck.plugins.migwiz

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import groovy.transform.CompileStatic

/**
 * Migration Wizazrd UI Plugin adapter.
 */
@Plugin(name = MigWIzUIPlugin.PROVIDER_NAME, service = ServiceNameConstants.UI)
@PluginDescription(title = MigWIzUIPlugin.PLUGIN_TITLE, description = MigWIzUIPlugin.PLUGIN_DESC)
@CompileStatic
class MigWIzUIPlugin implements UIPlugin, PluginResourceLoader {

    static final String PROVIDER_NAME = 'migration-wizard'
    static final String PLUGIN_TITLE = "Runbook Automation Migration Wizard"
    static final String PLUGIN_DESC = "Take your existing projects to the cloud with the Runbook Automation Migration Wizard!"

    /** JS scripts */
    private static final List<String> SCRIPTS = [
//            "lib/jquery.migwiz.js",
//            "js/init.js"
    ]

    /** css stylesheets */
    private static final List<String> STYLES = [
            "css/ui-migwiz-plugin-styles.css"
    ]

    /** Full Resource list */
    private static final List<String> ALL_RESOURCES = SCRIPTS + STYLES


    @Override
    boolean doesApply(String path) {
        // TODO use AntPathMatcher
        return true
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
        return ["ui-common-platform"]
    }

    @Override
    List<String> listResources() throws PluginException, IOException {
        return ALL_RESOURCES
    }

    /**
     * Gets a resource from the classpath resources bundled in the jar.
     */
    @Override
    InputStream openResourceStreamFor(String name) throws PluginException, IOException {
        return this.getClass().getResourceAsStream("/" + name)
    }
}
