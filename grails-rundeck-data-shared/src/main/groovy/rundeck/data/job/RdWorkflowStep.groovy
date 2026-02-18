package rundeck.data.job

import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.util.Holders
import grails.validation.Validateable
import groovy.transform.CompileDynamic
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import rundeck.data.validation.shared.SharedWorkflowStepConstraints
import rundeck.data.validation.validators.workflowstep.WorkflowStepValidatorFactory

@JsonIgnoreProperties(["errors"])
class RdWorkflowStep implements WorkflowStepData, PluginProviderConfiguration, Validateable {
    RdWorkflowStep errorHandler;
    Boolean keepgoingOnSuccess;
    String description;
    Map<String, Object> configuration
    Boolean nodeStep;
    String pluginType;
    Map<String,Object> pluginConfig;

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

    @Override
    @JsonIgnore
    String getProvider() {
        return pluginType
    }

    @Override
    @JsonIgnore
    String summarize() {
        return "implement summarization"
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

    @Override
    void storePluginConfigForType(String key, Object obj) {

    }
/**
     * Construct an RdWorkflowStep from a canonical map representation (inverse of toMap).
     *
     * Recognizes keys:
     *  - type or provider -> pluginType
     *  - nodeStep (Boolean)
     *  - configuration or config (Map)
     *  - description (String)
     *  - errorhandler (Map) -> recursive RdWorkflowStep
     *  - keepgoingOnSuccess (Boolean)
     *  - plugins or pluginConfig (Map) -> pluginConfig
     *
     * @param m Map representation of a workflow step
     * @return RdWorkflowStep instance or null if map is null/empty
     */
    @CompileDynamic
    static RdWorkflowStep fromMap(Map m) {
        if (!m) return null
        def step = new RdWorkflowStep()
        step.pluginType = m.type ?: m.provider
        if (m.containsKey('nodeStep')) {
            step.nodeStep = m.nodeStep as Boolean
        }
        step.configuration = (m.configuration ?: m.config) as Map<String, Object>
        step.description = m.description
        if (m.containsKey('keepgoingOnSuccess')) {
            step.keepgoingOnSuccess = m.keepgoingOnSuccess as Boolean
        }
        if (m.errorhandler instanceof Map) {
            step.errorHandler = RdWorkflowStep.fromMap((Map) m.errorhandler)
        }
        step.pluginConfig = (m.plugins ?: m.pluginConfig) as Map<String, Object>

        return step
    }
}
