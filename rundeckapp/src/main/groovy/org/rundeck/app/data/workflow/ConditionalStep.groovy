package org.rundeck.app.data.workflow

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.util.Holders
import grails.validation.Validateable
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.workflow.ConditionalSet
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.rundeck.app.data.model.v1.job.workflow.ConditionalOperator
import rundeck.JobExec
import rundeck.PluginStep
import rundeck.data.validation.shared.SharedWorkflowStepConstraints
import rundeck.data.validation.validators.workflowstep.WorkflowStepValidatorFactory

/**
 * Implementation of WorkflowStepData for conditional steps.
 * A conditional step contains a ConditionalSet and a list of sub-steps that will be executed
 * when the conditions are met.
 */
@JsonIgnoreProperties(["errors"])
class ConditionalStep implements WorkflowStepData, Validateable {
    ConditionalSet conditionSet
    List<WorkflowStepData> subSteps
    WorkflowStepData errorHandler
    Boolean keepgoingOnSuccess
    String description
    Boolean nodeStep

    static constraints = {
        importFrom SharedWorkflowStepConstraints
        conditionSet(nullable: false, validator: { val, obj, errors ->
            if (!val) {
                errors.rejectValue("conditionSet", 'WorkflowStep.conditional.conditionSet.required', [errors.nestedPath] as Object[], "Step {0}: Conditional step must have a conditionSet")
                return
            }
            // Validate that if conditionSet is present, subSteps must be non-empty
            if (!obj.subSteps || obj.subSteps.isEmpty()) {
                errors.rejectValue("subSteps", 'WorkflowStep.conditional.subSteps.required', [errors.nestedPath] as Object[], "Step {0}: Conditional steps must have at least one sub-step")
            }
            // Validate ConditionalSet structure
            if (val.conditionGroups == null || val.conditionGroups.isEmpty()) {
                errors.rejectValue("conditionSet", 'WorkflowStep.conditional.conditionGroups.required', [errors.nestedPath] as Object[], "Step {0}: ConditionalSet must have at least one condition group")
            } else {
                def validOperators = ConditionalOperator.getAllSymbols()
                val.conditionGroups.eachWithIndex { group, groupIndex ->
                    if (group == null || group.isEmpty()) {
                        errors.rejectValue("conditionSet", 'WorkflowStep.conditional.conditionGroup.empty', [errors.nestedPath, groupIndex] as Object[], "Step {0}: Condition group {1} must not be empty")
                    } else {
                        group.eachWithIndex { condDef, condIndex ->
                            if (!condDef.key || condDef.key.trim().isEmpty()) {
                                errors.rejectValue("conditionSet", 'WorkflowStep.conditional.condition.key.required', [errors.nestedPath, groupIndex, condIndex] as Object[], "Step {0}: Condition group {1}, condition {2}: key must be non-empty")
                            }
                            if (!condDef.operator || !ConditionalOperator.isValidOperator(condDef.operator)) {
                                errors.rejectValue("conditionSet", 'WorkflowStep.conditional.condition.operator.invalid', [errors.nestedPath, groupIndex, condIndex, condDef.operator] as Object[], "Step {0}: Condition group {1}, condition {2}: operator '{3}' is not valid. Valid operators: ${validOperators.join(', ')}")
                            }
                        }
                    }
                }
            }
        })
        subSteps(nullable: false, validator: { val, obj, errors ->
            // If conditionSet is present, subSteps must be non-empty
            if (obj.conditionSet && (!val || val.isEmpty())) {
                errors.rejectValue("subSteps", 'WorkflowStep.conditional.subSteps.required', [errors.nestedPath] as Object[], "Step {0}: Conditional steps must have at least one sub-step")
            }
            // Validate nesting depth (v1: max 1 level deep)
            if (val) {
                val.eachWithIndex { subStep, index ->
                    if (subStep instanceof ConditionalStep) {
                        errors.rejectValue("subSteps", 'WorkflowStep.conditional.nesting.depth', [errors.nestedPath, index] as Object[], "Step {0}: Sub-step {1} cannot be a conditional step (nesting limited to 1 level in v1)")
                    }
                }
            }
        })
        errorHandler(nullable: true, validator: { val, obj, errors ->
            if(!val) return
            new WorkflowStepValidatorFactory(Holders.grailsApplication.mainContext.getBean(FrameworkServiceCapabilities)).createValidator("${errors.nestedPath}.errorHandler", val).validate(val, errors)
        })
    }

    /**
     * Create a ConditionalStep instance from a Map representation
     * @param stepMap Map containing conditional step data
     * @return ConditionalStep instance
     */
    static ConditionalStep fromMap(Map<String, Object> stepMap) {
        if (!stepMap || !stepMap.conditionGroups) {
            return null
        }

        if (!stepMap) {
            return null
        }

        def step = new ConditionalStep()
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
        if (stepMap.conditionGroups) {
            step.conditionSet = new ConditionalSetImpl().fromMap(stepMap as Map<String, Object>)
        }
        // Handle subSteps - can be any WorkflowStepData type
        if (stepMap.conditionGroups && stepMap.subSteps) {
            def subStepsList = []
            stepMap.subSteps.each { Map subStepMap ->
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
    String summarize() {
        return description ?: "Conditional Step"
    }

    @Override
    Map<String, Object> getConfiguration() {
//        Map cfg = this.toMap()
//        cfg.remove("plugins")
//        cfg.remove("errorHandler")
//        cfg.remove("keepgoingOnSuccess")
//        return cfg
        return null
    }

    @Override
    String getPluginType() {
        return "conditional" // Conditional steps don't have a plugin type
    }

    @Override
    Map<String, Object> getPluginConfig() {
        return null // Conditional steps don't have plugin config
    }

    @Override
    Object getPluginConfigForType(String type) {
        return null
    }

    @Override
    List getPluginConfigListForType(String type) {
        return null
    }

    @Override
    void storePluginConfigForType(String key, Object obj) {
        // No-op for conditional steps
    }

    @Override
    void setPluginConfig(Map<String, Object> obj) {
        // No-op for conditional steps
    }

    @Override
    Map toDescriptionMap() {
        return toMap()
    }

    @Override
    String getRunnerNode() {
        return null // Conditional steps don't have a runner node
    }

    Class getClassType() {
        return ConditionalStep
    }

    /**
     * Convert to canonical map representation for serialization
     * @return Map representation
     */
    Map toMap() {
        def map = [type: pluginType, nodeStep: nodeStep]

        if (configuration) {
            map.configuration = configuration
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
            map.conditionGroups = conditionSet.toMap().conditionGroups
        }
        if(subSteps){
            map.subSteps = subSteps.collect { it.toMap() }
        }
        return map
    }

    boolean instanceOf(Class classT){
        return classT != null && classT.isAssignableFrom(ConditionalStep);
    }
}

