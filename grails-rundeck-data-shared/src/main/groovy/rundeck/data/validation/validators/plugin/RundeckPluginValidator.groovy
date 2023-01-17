package rundeck.data.validation.validators.plugin

import com.dtolabs.rundeck.core.plugins.PluginProviderConfiguration
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import org.rundeck.app.core.FrameworkServiceCapabilities
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class RundeckPluginValidator implements Validator {

    private FrameworkServiceCapabilities frameworkService
    private Class pluginType
    private String pluginTypeField
    private String pluginConfigField

    RundeckPluginValidator(FrameworkServiceCapabilities frameworkService, Class pluginType, String pluginTypeField, String pluginConfigField) {
        this.pluginConfigField = pluginConfigField
        this.pluginTypeField = pluginTypeField
        this.pluginType = pluginType
        this.frameworkService = frameworkService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return PluginProviderConfiguration.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        PluginProviderConfiguration plugin = (PluginProviderConfiguration)target
        def described = frameworkService.pluginService.getPluginDescriptor(plugin.provider,pluginType)
        if(!described) {
            errors.reject( "plugin.error.missing.type", [pluginTypeField, pluginType, plugin.provider] as Object[], "{0} : Missing plugin {2} for service {2}")
            return
        }

        def validation = frameworkService.validateDescription(
                described.description,
                '',
                plugin.configuration,
                null,
                PropertyScope.Instance,
                PropertyScope.Project
        )
        if(!validation.valid) {
            errors.reject("plugin.configuration.invalid",
                    [pluginConfigField, pluginType.simpleName, plugin.provider, validation.report.toString()].toArray(),
                    "{0} : The {1} - {2} plugin has an invalid configuration. {3}")
        }

    }

}
