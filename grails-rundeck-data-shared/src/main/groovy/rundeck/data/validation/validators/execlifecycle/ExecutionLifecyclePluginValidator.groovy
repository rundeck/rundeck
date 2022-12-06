package rundeck.data.validation.validators.execlifecycle

import com.dtolabs.rundeck.core.common.FrameworkServiceCapabilities
import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import org.rundeck.app.data.model.v1.job.JobData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class ExecutionLifecyclePluginValidator implements Validator {

    FrameworkServiceCapabilities frameworkService

    ExecutionLifecyclePluginValidator(FrameworkServiceCapabilities frameworkService) {
        this.frameworkService = frameworkService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return JobData.class.isAssignableFrom(clazz);
    }

    @Override
    void validate(Object target, Errors errors) {
        JobData jobData = (JobData)target
        def pluginConfigSet = frameworkService.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(jobData)
        if (pluginConfigSet && pluginConfigSet.pluginProviderConfigs?.size()>0) {
            //validate execution life cycle plugins
            pluginConfigSet.pluginProviderConfigs.each { PluginProviderConfiguration providerConfig ->
                def pluginType = providerConfig.provider
                Map config = providerConfig.configuration
                def validation = frameworkService.pluginService.pluginRegistry.validatePluginByName(pluginType,
                        frameworkService.getPluginService().createPluggableService(ExecutionLifecyclePlugin),
                        frameworkService.getRundeckFramework(),
                        jobData.project,
                        config)
                if(!validation) {
                    errors.rejectValue("pluginConfigMap",
                    "scheduledExecution.executionLifecyclePlugins.pluginTypeNotFound.message",
                            [pluginType] as Object[],
                            'Execution Life Cycle plugin type "{0}" was not found or could not be loaded'
                    )
                    return
                }

                if (!validation.valid) {
                    errors.rejectValue(
                            'pluginConfigMap',
                            'scheduledExecution.executionLifecyclePlugins.invalidPlugin.message',
                            [pluginType, validation.report.toString()] as Object[],
                            'Invalid Configuration for Execution Life Cycle plugin: {0} - {1}'
                    )
                }
            }
        }
    }
}
