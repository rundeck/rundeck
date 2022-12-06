package rundeck.data.job

import com.dtolabs.rundeck.core.common.FrameworkServiceCapabilities
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.util.Holders
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import grails.validation.Validateable
import org.springframework.validation.Errors
import rundeck.data.validation.validators.ValidatorUtils
import rundeck.data.validation.validators.plugin.RundeckPluginValidator
import rundeck.data.validation.validators.workflowstep.WorkflowStepValidatorFactory

@JsonIgnoreProperties(["errors"])
class RdWorkflow implements WorkflowData, Validateable {
    Long id
    Integer threadcount=1;
    Boolean keepgoing=false;
    List<RdWorkflowStep> steps;
    String strategy="node-first";
    Map<String,Object> pluginConfigMap;

    static constraints = {
        id(nullable: true)
        strategy(nullable:false, maxSize: 256)
        steps(validator: { List<RdWorkflowStep> val, RdWorkflow obj, Errors errors ->
            if(!val) {
                errors.rejectValue('steps', 'scheduledExecution.workflow.empty.message')
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
}