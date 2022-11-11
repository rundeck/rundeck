package org.rundeck.app.data.validators.notification

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.job.notification.NotificationData
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.services.FrameworkService
import rundeck.services.NotificationService

@Slf4j
class PluginNotificationValidator implements Validator {
    static ObjectMapper mapper = new ObjectMapper()
    NotificationService notificationService
    FrameworkService frameworkService
    String project
    Map grailsParams = [:]
    Map validationMap = [:]

    PluginNotificationValidator(NotificationService notificationService, FrameworkService frameworkService, String project, Map grailsParams, Map validationMap) {
        this.notificationService = notificationService
        this.frameworkService = frameworkService
        this.project = project
        this.grailsParams = grailsParams
        this.validationMap = validationMap
    }

    @Override
    boolean supports(Class<?> clazz) {
        return NotificationData.class.isAssignableFrom(clazz)
    }

    Map toConfig(NotificationData notif) {
        if(!notif.content) return null
        try{
            return mapper.readValue(notif.content, Map.class)
        }catch (JsonParseException e){
            log.error("Invalid notification config", e)
            return null
        }
    }

    @Override
    void validate(Object target, Errors errors) {
        NotificationData notif = (NotificationData)target
        IRundeckProject frameworkProject = frameworkService.getFrameworkProject(project)
        Map<String,String> projectProps = frameworkProject.getProjectProperties()
        String trigger = notif.eventTrigger
        //plugin type
        def failed = false
        def pluginDesc = notificationService.getNotificationPluginDescriptor(notif.type)
        if (!pluginDesc) {
            errors.rejectValue(
                    'notificationSet',
                    'scheduledExecution.notifications.pluginTypeNotFound.message',
                    [notif.type] as Object[],
                    'Notification Plugin type "{0}" was not found or could not be loaded'
            )
        }
        def validation = notificationService.validatePluginConfig(notif.type, projectProps, toConfig(notif))
        if (!validation.valid) {
            failed = true

            if (grailsParams instanceof Map) {
                if (!grailsParams['notificationValidation']) {
                    grailsParams['notificationValidation'] = [:]
                }

                if (!validationMap['notificationValidation']) {
                    validationMap['notificationValidation'] = [:]
                }
                if (!grailsParams['notificationValidation'][trigger]) {
                    grailsParams['notificationValidation'][trigger] = [:]
                }
                if (!validationMap['notificationValidation'][trigger]) {
                    validationMap['notificationValidation'][trigger] = [:]
                }
                grailsParams['notificationValidation'][trigger][notif.type] = validation
                validationMap['notificationValidation'][trigger][notif.type] = validation.report.errors
            }
            errors.rejectValue(
                    'notificationSet',
                    'scheduledExecution.notifications.invalidPlugin.message',
                    [notif.type] as Object[],
                    'Invalid Configuration for plugin: {0}'
            )
        }
    }
}
