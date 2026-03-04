package rundeck.data.job

import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import grails.util.Holders
import groovy.transform.CompileDynamic
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.workflow.WorkflowStepData
import org.springframework.validation.Errors
import rundeck.data.validation.validators.ValidatorUtils
import rundeck.data.validation.validators.plugin.RundeckPluginValidator
import rundeck.data.validation.validators.workflowstep.WorkflowStepValidatorFactory

@JsonIgnoreProperties(["errors"])
class RdWorkflow implements WorkflowData, Validateable {
    Integer threadcount=1;
    Boolean keepgoing=false;
    List<RdWorkflowStep> steps;
    String strategy="node-first";
    Map<String,Object> pluginConfigMap;

    static constraints = {
        strategy(nullable:false, maxSize: 256)
        steps(validator: { List<RdWorkflowStep> val, RdWorkflow obj, Errors errors ->
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

    @Override
    List<WorkflowStepData> getCommands() {
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

    @Override
    void setPluginConfigData(String type, Object data) {

    }

    @Override
    void setPluginConfigData(String type, String name, Object data) {

    }
/**
     * Construct an RdWorkflow from a canonical map representation (inverse of toMap).
     *
     * Accepts top-level keys:
     *  - keepgoing (Boolean)
     *  - strategy (String)
     *  - commands or steps (List of step maps)
     *  - pluginConfig or pluginConfigMap (Map)
     *
     * @param m Map representation of a workflow
     * @return RdWorkflow instance or null if map is null/empty
     */
    @CompileDynamic
    static RdWorkflow fromMap(Map m) {
        if (!m) return null
        def wf = new RdWorkflow()
        if (m.containsKey('keepgoing')) {
            wf.keepgoing = m.keepgoing as Boolean
        }
        wf.strategy = m.strategy ?: wf.strategy

        def commands = m.commands ?: m.steps
        if (commands instanceof List) {
            wf.steps = commands.collect { entry ->
                (entry instanceof Map) ? RdWorkflowStep.fromMap((Map) entry) : null
            }.findAll { it != null }
        }

        wf.pluginConfigMap = (m.pluginConfig ?: m.pluginConfigMap) as Map<String, Object>

        return wf
    }
}