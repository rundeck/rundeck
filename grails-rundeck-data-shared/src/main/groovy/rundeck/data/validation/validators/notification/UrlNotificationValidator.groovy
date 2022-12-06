package rundeck.data.validation.validators.notification

import org.rundeck.app.data.model.v1.job.notification.NotificationData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class UrlNotificationValidator implements Validator {
    @Override
    boolean supports(Class<?> clazz) {
        return NotificationData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        NotificationData notif = (NotificationData)target
        Map urlsConfiguration = notif.configuration
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
                        "configuration",
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
                    "configuration",
                    'scheduledExecution.notifications.url.blank.message',
                    'Webhook URL cannot be blank'
            )
        }
    }
}
