package org.rundeck.app.data.workflow

import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.util.Holders
import grails.validation.Validateable
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
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
     * Create a WorkflowStepDataImpl instance from a Map representation
     * @param stepMap Map containing workflow step data
     * @return WorkflowStepDataImpl instance
     */
    static WorkflowStepDataImpl fromMap(Map<String, Object> stepMap) {
        if (!stepMap) {
            return null
        }

        def step = new WorkflowStepDataImpl()
        step.pluginType = stepMap.type ?: stepMap.exec
        step.nodeStep = stepMap.nodeStep ?: false
        step.description = stepMap.description
        step.keepgoingOnSuccess = stepMap.keepgoingOnSuccess

        // Handle configuration
        if (stepMap.configuration) {
            step.configuration = stepMap.configuration as Map<String, Object>
        }

        // Handle plugin config
        if (stepMap.plugins) {
            step.pluginConfig = stepMap.plugins as Map<String, Object>
        }

        // Handle error handler recursively
        if (stepMap.errorhandler) {
            step.errorHandler = WorkflowStepDataImpl.fromMap(stepMap.errorhandler as Map<String, Object>)
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
        return map
    }
}
