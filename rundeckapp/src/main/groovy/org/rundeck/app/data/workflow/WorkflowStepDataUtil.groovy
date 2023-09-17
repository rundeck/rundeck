package org.rundeck.app.data.workflow

import com.dtolabs.rundeck.core.plugins.PluginConfiguration
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData

class WorkflowStepDataUtil {

    static String getJobIdentifier(WorkflowStepData step) {
        if(!step.configuration.jobref.useName && step.configuration.jobref.uuid){
            return step.configuration.jobref.uuid
        }
        return (null==step.configuration.jobref.jobGroup?'':step.configuration.jobref.jobGroup+"/")+step.configuration.jobref.jobName;
    }

    static List<Map<String,Object>> getPluginConfigListForType(WorkflowStepData step, String pluginType) {
        if(!step.pluginConfig || !step.pluginConfig.containsKey(pluginType)) return []
        def config = step.pluginConfig[pluginType]
        config && !(config instanceof Collection) ? [config] : config
    }

    static List<PluginConfiguration> createLogFilterConfigs(Object configurations) {
        List<PluginConfiguration> configs = []
        if (configurations && configurations instanceof Collection) {
            configurations.each { conf ->
                if (conf && conf instanceof Map) {
                    String name = conf['type']
                    if (conf['config'] instanceof Map) {
                        Map pluginconfig = conf['config']
                        configs << new SimplePluginConfiguration(ServiceNameConstants.LogFilter, name, pluginconfig)
                    }
                }
            }
        }
        return configs;
    }
}
