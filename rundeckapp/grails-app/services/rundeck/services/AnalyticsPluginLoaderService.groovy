package rundeck.services

import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope

class AnalyticsPluginLoaderService {

    def rundeckPluginRegistry
    def pluginService
    def frameworkService

    def getAnalyticsListener(String provider, Map params) {
        return getPlugin(provider,frameworkService.getFrameworkPropertyResolverWithProps(params))
    }

    private def getPlugin(String provider, PropertyResolver resolver){
        //load plugin and configure with config values
        def result = pluginService.configurePlugin(provider, getAnalyticsService(), resolver, PropertyScope.Instance)
        if (!result?.instance) {
            log.error("Plugin '${provider}' not found")
            return []
        }
        result
    }


    PluggableProviderService<WorkflowExecutionListener> getAnalyticsService() {
        rundeckPluginRegistry.createPluggableService(WorkflowExecutionListener)
    }
}