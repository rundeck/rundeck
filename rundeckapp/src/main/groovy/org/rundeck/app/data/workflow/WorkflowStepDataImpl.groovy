package org.rundeck.app.data.workflow

import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.util.Holders
import grails.validation.Validateable
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.data.validation.shared.SharedWorkflowStepConstraints
import rundeck.data.validation.validators.workflowstep.WorkflowStepValidatorFactory

import java.util.Collection

/**
 * Implementation of WorkflowStepData for deserialized workflow step data.
 * This class represents workflow step data that has been deserialized from JSON storage.
 * It provides a runtime representation without GORM persistence overhead.
 */
@JsonIgnoreProperties(["errors"])
class WorkflowStepDataImpl implements WorkflowStepData, PluginProviderConfiguration, Validateable  {
    WorkflowStepDataImpl errorHandler
    Boolean keepgoingOnSuccess
    String description
    Map<String, Object> configuration
    Boolean nodeStep
    String pluginType
    Map<String, Object> pluginConfig
    String runnerNode;
    ConditionalSet conditionSet
    List<WorkflowStepData> subSteps

    static constraints = {
        importFrom SharedWorkflowStepConstraints
        errorHandler(nullable: true, validator: { val, obj, errors ->
            if(!val) return
            if(obj.nodeStep && !val.nodeStep) {
                errors.rejectValue("errorHandler",'WorkflowStep.errorHandler.nodeStep.invalid', [errors.nestedPath] as Object[], "Step {0}: Must have a Node Step as an Error Handler")
            }
            new WorkflowStepValidatorFactory(Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities)).createValidator("${errors.nestedPath}.errorHandler", val).validate(val, errors)
        })
    }

    /**
     * Create a WorkflowStepData instance from a Map representation.
     * If conditionSet is present, returns a ConditionalStep; otherwise returns WorkflowStepDataImpl.
     * @param stepMap Map containing workflow step data
     * @return WorkflowStepData instance (ConditionalStep or WorkflowStepDataImpl)
     */
    static WorkflowStepData fromMap(Map<String, Object> map) {
        if (!map) {
            return null
        }

        WorkflowStepData step
        if (map.jobref!=null) {
            step = JobExec.jobExecFromMap(map)
        } else if (map.exec != null || map.script != null || map.scriptfile != null || map.scripturl != null) {
            CommandExec ce = new CommandExec()
            CommandExec.updateFromMap(ce, map)
            step = ce
        } else {
            WorkflowStepData pluginStep = PluginStep.fromMap(map)
            step = pluginStep.createClone()
        }
        //exec
        //WorkflowStepDataImpl.fromMap(map as Map<String, Object>)
        if(map.errorhandler){
            WorkflowStepData errorHandlerExec
            Map mapErrorHandler = map.errorhandler as Map
            if (mapErrorHandler.jobref!=null) {
                step.errorHandler = JobExec.jobExecFromMap(mapErrorHandler)
            } else {
                step.errorHandler = PluginStep.fromMap(mapErrorHandler)
            }
        }

        if (map.conditionGroups) {
            step.conditionSet = new ConditionalSetImpl().fromMap(map as Map<String, Object>)
        }
        // Handle subSteps - can be any WorkflowStepData type
        if (map.conditionGroups && map.subSteps) {
            def subStepsList = []
            map.subSteps.each { Map subStepMap ->
                WorkflowStepData exec
                if (subStepMap.jobref!=null) {
                    exec = JobExec.jobExecFromMap(subStepMap)
                } else {
                    exec = PluginStep.fromMap(subStepMap)
                }
                subStepsList.add(exec)
            }
            step.subSteps = subStepsList
        }

        return step
    }

    @Override
    @JsonIgnore
    String getProvider() {
        return pluginType
    }

    @Override
    @JsonIgnore
    String summarize() {
        return description ?: pluginType ?: "Workflow Step"
    }

    /**
     * Get plugin config for a specific type
     * @param type Plugin type
     * @return Plugin config for the type, or null
     */
    Object getPluginConfigForType(String type) {
        return getPluginConfig()?.get(type)
    }

    /**
     * Get plugin config list for a specific type
     * @param type Plugin type
     * @return List of plugin configurations for the type, or null
     */
    List getPluginConfigListForType(String type) {
        def val = getPluginConfig()?.get(type)
        val && !(val instanceof Collection) ? [val] : val
    }

    /**
     * Store plugin configuration for a type
     * @param key Plugin type key
     * @param obj Configuration object
     */
    void storePluginConfigForType(String key, Object obj) {
        def config = getPluginConfig() ?: [:]
        config.put(key, obj)
        setPluginConfig(config)
    }

    /**
     * Set the entire plugin config map
     * @param obj Map of plugin configurations
     */
    void setPluginConfig(Map<String, Object> obj) {
        this.pluginConfig = obj
    }

    /**
     * Return map representation without details (for backward compatibility)
     * @return Map representation
     */
    Map toDescriptionMap() {
        return toMap()
    }

    /**
     * Convert to canonical map representation for serialization
     * @return Map representation
     */
    Map toMap() {
        def map = [type: pluginType, nodeStep: nodeStep]

        if (configuration) {
            map.put('configuration', configuration)
        }
        if (description) {
            map.description = description
        }
        if (errorHandler) {
            map.errorhandler = errorHandler.toMap()
        } else if (keepgoingOnSuccess) {
            map.keepgoingOnSuccess = keepgoingOnSuccess
        }
        if (pluginConfig) {
            map.plugins = pluginConfig
        }
        if (runnerNode) {
            map.runnerNode = runnerNode
        }
        if(conditionSet){
            map.conditionSet = conditionSet
        }
        if(subSteps){
            map.subSteps = subSteps.collect { it.toMap() }
        }
        return map
    }

    /**
     * Instanteate new WorkflowStepDataImpl from WorkflowStepData source
     * @param source WorkflowStepData source
     * @return WorkflowStepDataImpl instance
     */
    static WorkflowStepDataImpl fromWorkflowStepData(WorkflowStepData source) {
        if (!source) {
            return null
        }
        def step = new WorkflowStepDataImpl()
        step.pluginType = source.pluginType
        step.nodeStep = source.nodeStep
        step.description = source.description
        step.keepgoingOnSuccess = source.keepgoingOnSuccess
        step.configuration = source.configuration
        step.pluginConfig = source.pluginConfig

        if (source.errorHandler) {
            step.errorHandler = fromWorkflowStepData(source.errorHandler)
        }
        return step
    }

    void updateFromMap(Map params) {
        if (params == null) {
            return
        }

        // Common properties for all step types
        if (params.containsKey('description')) {
            this.description = params.description?.toString()
        }
        if (params.containsKey('keepgoingOnSuccess')) {
            this.keepgoingOnSuccess = params.keepgoingOnSuccess != null ?
                    (params.keepgoingOnSuccess instanceof Boolean ? params.keepgoingOnSuccess :
                            Boolean.parseBoolean(params.keepgoingOnSuccess.toString())) : null
        }
        if (params.containsKey('nodeStep')) {
            if (params.nodeStep instanceof String) {
                this.nodeStep = params.nodeStep == 'true'
            } else {
                this.nodeStep = params.nodeStep as Boolean
            }
        }
        if (params.containsKey('pluginType') || params.containsKey('type')) {
            this.pluginType = params.pluginType ?: params.type
        }
        if (params.containsKey('configuration')) {
            this.configuration = params.configuration as Map<String, Object>
        }
        if (params.containsKey('pluginConfig') || params.containsKey('plugins')) {
            this.pluginConfig = (params.pluginConfig ?: params.plugins) as Map<String, Object>
        }
        if (params.containsKey('runnerNode')) {
            this.runnerNode = params.runnerNode?.toString()
        }

        // Note: JobExec and CommandExec specific properties (jobName, jobGroup, adhocRemoteString, etc.)
        // are not stored directly in WorkflowStepDataImpl. They would need to be stored in configuration
        // or handled by converting to/from the appropriate domain class type.
    }
}
