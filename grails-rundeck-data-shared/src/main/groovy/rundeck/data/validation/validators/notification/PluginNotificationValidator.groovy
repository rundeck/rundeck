package rundeck.data.validation.validators.notification

import com.dtolabs.rundeck.core.common.FrameworkServiceCapabilities
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.job.notification.NotificationData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

@Slf4j
class PluginNotificationValidator implements Validator {
    FrameworkServiceCapabilities frameworkService
    String project

    PluginNotificationValidator(FrameworkServiceCapabilities frameworkService, String project) {
        this.frameworkService = frameworkService
        this.project = project
    }

    @Override
    boolean supports(Class<?> clazz) {
        return NotificationData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        NotificationData notif = (NotificationData)target
        IRundeckProject frameworkProject = frameworkService.getFrameworkProject(project)
        Map<String,String> projectProps = frameworkProject.getProjectProperties()
        def pluginDesc = frameworkService.pluginService.getPluginDescriptor(notif.type, ServiceNameConstants.Notification)
        if (!pluginDesc) {
            errors.rejectValue(
                    'type',
                    'scheduledExecution.notifications.pluginTypeNotFound.message',
                    [notif.type] as Object[],
                    'Notification Plugin type "{0}" was not found or could not be loaded'
            )
            return
        }
        def validation = frameworkService.pluginService.pluginRegistry.validatePluginByName(notif.type, frameworkService.pluginService.createPluggableService(NotificationPlugin),
                frameworkService.getFrameworkPropertyResolverWithProps(projectProps, notif.configuration), PropertyScope.Instance, PropertyScope.Project)
        if (!validation.valid) {

            errors.rejectValue(
                    'configuration',
                    'scheduledExecution.notifications.invalidPlugin.message',
                    [notif.type, validation.report.toString()] as Object[],
                    'Notification plugin: {0} has an invalid configuration: {1}'
            )
        }
    }
}
