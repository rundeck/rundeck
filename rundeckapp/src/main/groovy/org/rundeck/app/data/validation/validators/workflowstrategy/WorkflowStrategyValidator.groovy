package org.rundeck.app.data.validation.validators.workflowstrategy

import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import grails.util.Holders
import org.rundeck.app.data.job.converters.WorkflowExecutionItemFactory
import org.rundeck.app.data.model.v1.job.JobData
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.services.FrameworkService


class WorkflowStrategyValidator implements Validator {

    FrameworkService frameworkService

    WorkflowStrategyValidator(FrameworkService frameworkService) {
        this.frameworkService = frameworkService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return JobData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        JobData jobData = (JobData)target
        if(!jobData.workflow || !jobData.workflow.strategy) return

        def workflowStrategyService = frameworkService.rundeckFramework.workflowStrategyService
        def frameworkProject = frameworkService.getFrameworkProject(jobData.project)
        def projectProps = frameworkProject.getProperties()
        def strategyConfig = jobData.workflow.pluginConfigMap?.get(ServiceNameConstants.WorkflowStrategy)?.get(jobData.workflow.strategy) ?: [:]

        PropertyResolver resolver = frameworkService.getFrameworkPropertyResolverWithProps(
                projectProps,
                strategyConfig
        )
        //validate input values wrt to property definitions
        def validation = frameworkService.pluginService.validatePlugin(jobData.workflow.strategy,
                workflowStrategyService,
                resolver,
                PropertyScope.Instance
        )
        if(!validation) {
            errors.rejectValue("workflow.strategy",
                    "workflow.strategy.missing.plugin",
                    [jobData.workflow.strategy].toArray(),
                    "Missing workflow strategy plugin {0}"
            )
            return
        }
        def report=validation.report
        if (!report||report.valid) {
            def workflowItem = WorkflowExecutionItemFactory.createExecutionItemForWorkflow(jobData.workflow)
            def workflowStrategy = workflowStrategyService.getStrategyForWorkflow(workflowItem, resolver)

            report = workflowStrategy.validate(workflowItem.workflow)
        }
        if(report && !report.valid) {
            errors.rejectValue("workflow.strategy",
                    "workflow.strategy.invalid.configuration",
                    [jobData.workflow.strategy, report.toString()].toArray(),
                    "The workflow strategy plugin {0} has an invalid configuration. {1}")
        }



    }
}
