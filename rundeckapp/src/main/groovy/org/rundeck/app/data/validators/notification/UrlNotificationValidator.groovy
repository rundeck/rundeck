package org.rundeck.app.data.validators.notification

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.NotificationConstants
import org.rundeck.app.data.model.v1.job.notification.NotificationData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class UrlNotificationValidator implements Validator {
    static ObjectMapper mapper = new ObjectMapper()
    @Override
    boolean supports(Class<?> clazz) {
        return NotificationData.class.isAssignableFrom(clazz)
    }

    Map toUrlConfiguration(String content) {
        if (content?.startsWith('{') && content?.endsWith('}')) {
            //parse as json
            try {
                return mapper.readValue(content, HashMap)
            } catch(Exception e) {
                log.error("Invalid json configuration",e)
                return null
            }
        }
        return [urls: content]
    }

    @Override
    void validate(Object target, Errors errors) {
        NotificationData notif = (NotificationData)target
        def fieldNamesUrl = NotificationConstants.NOTIFICATION_FIELD_NAMES_URL
        Map urlsConfiguration = toUrlConfiguration(notif.content)
        String trigger = notif.eventTrigger
        String urls = urlsConfiguration.urls
        def arr = urls?.split(",")
        def validCount=0
        arr?.each { String url ->
            boolean valid = false
            try {
                new URL(url)
                valid = true
            } catch (MalformedURLException e) {
                valid = false
            }
            if (url && !valid) {
                errors.rejectValue(
                        fieldNamesUrl[trigger],
                        'scheduledExecution.notifications.invalidurl.message',
                        [url] as Object[],
                        'Invalid URL: {0}'
                )
            }else if(url && valid){
                validCount++
            }
        }
        if(urls.isBlank()){
            errors.rejectValue(
                    fieldNamesUrl[trigger],
                    'scheduledExecution.notifications.url.blank.message',
                    'Webhook URL cannot be blank'
            )
        }
    }
}
