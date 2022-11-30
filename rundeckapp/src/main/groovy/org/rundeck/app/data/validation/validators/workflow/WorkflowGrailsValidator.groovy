package org.rundeck.app.data.validation.validators.workflow

import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import grails.util.Holders
import org.rundeck.app.data.job.RdWorkflow
import org.rundeck.app.data.job.RdWorkflowStep
import org.rundeck.app.data.validation.validators.ValidatorUtils
import org.rundeck.app.data.validation.validators.plugin.RundeckPluginValidator
import org.rundeck.app.data.validation.validators.workflowstep.WorkflowStepValidatorFactory
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.services.FrameworkService

class WorkflowGrailsValidator {

    static Closure validator = { List<RdWorkflowStep> val, RdWorkflow obj, Errors errors ->
        if(!val) {
            errors.rejectValue('steps', 'scheduledExecution.workflow.empty.message')
            return
        }
        val.eachWithIndex { step, idx ->
            String propPath = "steps[${idx}]"

            if(!step.validate()) {
                ValidatorUtils.processErrors(propPath, step, errors)
                return
            }

            errors.pushNestedPath(propPath)
            def stepValidator = new WorkflowStepValidatorFactory().createValidator(propPath, step)
            stepValidator.validate(step, errors)
            //Validate LogFilter for each step
            if(step.pluginConfig?.containsKey(ServiceNameConstants.LogFilter)) {
                step.pluginConfig[ServiceNameConstants.LogFilter].eachWithIndex { cfg, lfidx ->
                    def lfvalidator = new RundeckPluginValidator(Holders.grailsApplication.mainContext.getBean(FrameworkService),
                            LogFilterPlugin, "step[${idx}].pluginConfig.LogFilter[${lfidx}].type", "step[${idx}].pluginConfig.LogFilter[${lfidx}].config")
                   lfvalidator.validate(SimplePluginConfiguration.builder().provider(cfg.type).configuration(cfg.config).build(), errors)
                }
            }
            errors.popNestedPath()
            //validate global LogFilters
            if(obj.pluginConfigMap?.containsKey(ServiceNameConstants.LogFilter)) {
                obj.pluginConfigMap[ServiceNameConstants.LogFilter].eachWithIndex{ cfg, lfidx ->
                    def lfvalidator = new RundeckPluginValidator(Holders.grailsApplication.mainContext.getBean(FrameworkService),
                            LogFilterPlugin, "workflow.pluginConfigMap.LogFilter[${lfidx}].type", "workflow.pluginConfigMap.LogFilter[${lfidx}].config")
                    lfvalidator.validate(SimplePluginConfiguration.builder().provider(cfg.type).configuration(cfg.config).build(), errors)
                }
            }

        }
    }
}
