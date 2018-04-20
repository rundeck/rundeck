package rundeck.interceptors

import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.PluginService


class WorkflowInterceptor {

    @Autowired
    PluginService pluginService

    Map<String, Class> requiredPluginTypes = [logFilterPlugins: LogFilterPlugin]

    boolean before() { true }

    boolean after() {
        if(model) {
            for (Map.Entry<String, Class> entry : getRequiredPluginTypes()?.entrySet()) {
                model[entry.key] = pluginService.listPlugins(entry.value)
            }
        }
        true
    }

    void afterView() {
        // no-op
    }
}
