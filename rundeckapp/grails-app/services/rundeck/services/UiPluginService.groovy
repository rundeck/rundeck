package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluginException
import com.dtolabs.rundeck.core.plugins.ProviderIdent
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.PluginRegistry
import com.dtolabs.rundeck.server.plugins.services.UIPluginProviderService
import grails.transaction.Transactional

class UiPluginService {
    static boolean transactional = false
    def PluginRegistry rundeckPluginRegistry
    def PluginService pluginService
    def UIPluginProviderService uiPluginProviderService
    Map loadingCache = [:]

    def pluginsForPage(String path) {
        if (loadingCache[path] != null) {
            return loadingCache[path]
        }
        def loaded = []
        def plugins = pluginService.listPlugins(UIPlugin, uiPluginProviderService)
        plugins.each { String name, DescribedPlugin<UIPlugin> plugin ->
            UIPlugin inst = pluginService.getPlugin(plugin.name, uiPluginProviderService)
            if (inst.doesApply(path)) {
                loaded << inst
            }
        }
        loadingCache[path] = loaded
    }

    Map providerProfiles = [:]

    def getProfileFor(String service, String name) {
        def profile = [:]
        def reslist = resourcesForPlugin(service, name)
        if (reslist?.contains("$service:$name:icon.png".toString())) {
            profile['icon'] = "$service:$name:icon.png"
        } else if (reslist?.contains('icon.png')) {
            profile['icon'] = "icon.png"
        }
        profile
    }

    /**
     * List of resource names for the given plugin, or null
     * @param service
     * @param name
     * @return
     */
    def resourcesForPlugin(String service, String name) {
        rundeckPluginRegistry.getResourceLoader(service, name)?.listResources()
    }

    /**
     * open input stream for the resource
     * @param service
     * @param name
     * @param path
     * @return
     */
    def openResourceForPlugin(String service, String name, String path) {
        try {
            return rundeckPluginRegistry.getResourceLoader(service, name)?.openResourceStreamFor(path)
        } catch (IOException | PluginException e) {
            return null
        }
    }
}
