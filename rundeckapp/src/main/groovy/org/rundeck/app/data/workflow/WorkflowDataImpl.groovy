package org.rundeck.app.data.workflow

import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import grails.util.Holders
import grails.validation.Validateable
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.springframework.validation.Errors
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.data.validation.validators.ValidatorUtils
import rundeck.data.validation.validators.plugin.RundeckPluginValidator
import rundeck.data.validation.validators.workflowstep.WorkflowStepValidatorFactory

/**
 * Implementation of WorkflowData for deserialized workflow data.
 * This class represents workflow data that has been deserialized from JSON storage.
 * It provides a runtime representation without GORM persistence overhead.
 */
@JsonIgnoreProperties(["errors"])
class WorkflowDataImpl implements WorkflowData, Validateable  {
    Integer threadcount = 1
    Boolean keepgoing = false
    List<WorkflowStepData> steps = []
    String strategy = "node-first"
    Map<String, Object> pluginConfigMap

    static constraints = {
        strategy(nullable:false, maxSize: 256)
        steps(validator: { List<WorkflowStepDataImpl> val, WorkflowDataImpl obj, Errors errors ->
            if(!val) {
                errors.rejectValue('steps', 'scheduledExecution.workflow.empty.message', 'Step must not be empty')
                return
            }
            def frameworkService = Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities)
            val.eachWithIndex { step, idx ->
                String propPath = "steps[${idx}]"

                if(!step.validate()) {
                    ValidatorUtils.processErrors(propPath, step, errors)
                    return
                }

                errors.pushNestedPath(propPath)
                def stepValidator = new WorkflowStepValidatorFactory(frameworkService).createValidator(propPath, step)
                stepValidator.validate(step, errors)
                //Validate LogFilter for each step
                if(step.pluginConfig?.containsKey(ServiceNameConstants.LogFilter)) {
                    step.pluginConfig[ServiceNameConstants.LogFilter].eachWithIndex { cfg, lfidx ->
                        def lfvalidator = new RundeckPluginValidator(frameworkService,
                                LogFilterPlugin, "step[${idx}].pluginConfig.LogFilter[${lfidx}].type", "step[${idx}].pluginConfig.LogFilter[${lfidx}].config")
                        lfvalidator.validate(SimplePluginConfiguration.builder().provider(cfg.type).configuration(cfg.config).build(), errors)
                    }
                }
                errors.popNestedPath()
                //validate global LogFilters
                if(obj.pluginConfigMap?.containsKey(ServiceNameConstants.LogFilter)) {
                    obj.pluginConfigMap[ServiceNameConstants.LogFilter].eachWithIndex{ cfg, lfidx ->
                        def lfvalidator = new RundeckPluginValidator(frameworkService,
                                LogFilterPlugin, "workflow.pluginConfigMap.LogFilter[${lfidx}].type", "workflow.pluginConfigMap.LogFilter[${lfidx}].config")
                        lfvalidator.validate(SimplePluginConfiguration.builder().provider(cfg.type).configuration(cfg.config).build(), errors)
                    }
                }

            }
        })
    }

    /**
     * Create a WorkflowDataImpl instance from a Map representation
     * @param workflowMap Map containing workflow data
     * @return WorkflowDataImpl instance
     */
    static WorkflowDataImpl fromMap(Map<String, Object> workflowMap) {
        if (!workflowMap) {
            return null
        }

        def workflow = new WorkflowDataImpl()
        workflow.keepgoing = workflowMap.keepgoing ?: false
        workflow.strategy = workflowMap.strategy ?: "node-first"
        workflow.threadcount = workflowMap.threadcount ?: 1

        // Handle plugin config
        if (workflowMap.pluginConfig) {
            workflow.pluginConfigMap = workflowMap.pluginConfig as Map<String, Object>
        }

        // Parse steps (commands)
        def commands = workflowMap.commands ?: []
        workflow.steps = commands.collect { Map map ->
            WorkflowStepData exec
            if (map.jobref!=null) {
                exec = JobExec.jobExecFromMap(map)
            } else {
                exec = PluginStep.fromMap(map)
            }
            //exec
            //WorkflowStepDataImpl.fromMap(map as Map<String, Object>)
            if(map.errorhandler){
                WorkflowStepData errorHandlerExec
                Map mapErrorHandler = map.errorhandler as Map
                if (mapErrorHandler.jobref!=null) {
                    exec.errorHandler = JobExec.jobExecFromMap(mapErrorHandler)
                } else {
                    exec.errorHandler = PluginStep.fromMap(mapErrorHandler)
                }
            }

            exec
        }

        return workflow
    }

    /**
     * Get the config for a plugin type
     * @param type Plugin type
     * @return available config data, or null
     */
    def getPluginConfigData(String type) {
        def map = getPluginConfigMap()
        return map?.get(type)
    }

    /**
     * Get the config for a plugin type expecting a map, and an entry in the map
     * @param type Plugin type
     * @param name Plugin name
     * @return available map data or empty map
     */
    Map getPluginConfigData(String type, String name) {
        def map = getPluginConfigMap()
        if(!map){
            map=[(type):[:]]
        }else if(!map[type]){
            map[type]=[:]
        }
        map?.get(type)?.get(name)?:[:]
    }

    /**
     * Get plugin config data list for a specific type (e.g., LogFilter)
     * @param type Plugin type
     * @return List of plugin configurations or null
     */
    List getPluginConfigDataList(String type) {
        def map = getPluginConfigMap()
        def val = map?.get(type)
        if (val && !(val instanceof Collection)) {
            val = [val]
        }
        return val
    }

    /**
     * Set plugin config data for a specific type
     * @param type Plugin type
     * @param data Configuration data
     */
    void setPluginConfigData(String type, data) {
        def map = getPluginConfigMap()
        if (!map) {
            map = [:]
        }
        map[type] = data
        if (!data) {
            map.remove(type)
        }
        setPluginConfigMap(map)
    }

    @Override
    String getPluginConfig() {
        def obj = getPluginConfigMap()
        String pluginConfig
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            pluginConfig = mapper.writeValueAsString(obj)
        } else {
            pluginConfig = "{}"
        }

        return pluginConfig
    }

    /**
     * Set plugin config data for a specific type and name
     * @param type Plugin type
     * @param name Plugin name
     * @param data Configuration data
     */
    void setPluginConfigData(String type, String name, data) {
        def map = getPluginConfigMap()
        if (!map) {
            map = [:]
        }
        if (!map[type]) {
            map[type] = [:]
        }
        map[type][name] = data
        setPluginConfigMap(map)
    }

    /**
     * Set the entire plugin config map
     * @param obj Map of plugin configurations
     */
    void setPluginConfigMap(Map obj) {
        this.pluginConfigMap = obj
    }

    /**
     * Get the plugin config map
     * @return Map of plugin configurations
     */
    Map<String, Object> getPluginConfigMap() {
        return this.pluginConfigMap
    }

    /**
     * Get workflow commands (alias for getSteps() for backwards compatibility).
     * The Workflow domain class uses 'commands' field, but WorkflowData interface uses 'steps'.
     * This method provides compatibility for code that accesses .commands on workflow objects.
     * @return List of workflow steps
     */
    List<WorkflowStepDataImpl> getCommands() {
        return steps
    }

    /**
     * Convert to canonical map representation for serialization
     * @return Map representation
     */
    Map toMap() {
        def plugins = pluginConfigMap ? [pluginConfig: pluginConfigMap] : [:]

        // Remove empty WorkflowStrategy config for the strategy
        if (!plugins.pluginConfig?.get('WorkflowStrategy')?.get(strategy)) {
            plugins.pluginConfig?.remove('WorkflowStrategy')
        }
        if (!plugins.pluginConfig) {
            plugins.remove('pluginConfig')
        }

        // Cleanup WorkflowStrategy to only include the current strategy data
        if (plugins.pluginConfig?.get('WorkflowStrategy')) {
            plugins.pluginConfig['WorkflowStrategy'] = [(strategy): plugins.pluginConfig['WorkflowStrategy'][strategy]]
        }

        return [
            keepgoing: keepgoing,
            strategy: strategy,
            commands: steps?.collect { it.toMap() } ?: []
        ] + plugins
    }
}
