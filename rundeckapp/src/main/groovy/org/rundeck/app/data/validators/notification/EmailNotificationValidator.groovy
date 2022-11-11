package org.rundeck.app.data.validators.notification

import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.NotificationConstants
import org.rundeck.app.data.job.RdJob
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.services.AnyDomainEmailValidator

class EmailNotificationValidator implements Validator {
    static ObjectMapper mapper = new ObjectMapper()

    @Override
    boolean supports(Class<?> clazz) {
        return RdJob.RdNotificationData.class.isAssignableFrom(clazz)
    }

    Map toMailConfiguration(String content) {
        if (content.startsWith('{') && content.endsWith('}')) {
            try {
                return mapper.readValue(content, HashMap)
            } catch(Exception e) {
                log.error("Invalid json configuration",e)
                return null
            }
        }
        return [recipients: content]
    }

    @Override
    void validate(Object target, Errors errors) {
        RdJob.RdNotificationData n = (RdJob.RdNotificationData)target
        def fieldNames = NotificationConstants.NOTIFICATION_FIELD_NAMES
        def fieldAttachedNames = NotificationConstants.NOTIFICATION_FIELD_ATTACHED_NAMES
        Map conf = toMailConfiguration(n.content)
        String trigger = n.eventTrigger
        def arr = conf.recipients?.split(",")
        def validator = new AnyDomainEmailValidator()
        def validcount=0
        arr.each { email ->
            if(email && email.indexOf('${')>=0){
                //don't reject embedded prop refs
                validcount++
            }else if (email && !validator.isValid(email)) {
                errors.rejectValue(
                        fieldNames[trigger],
                        'scheduledExecution.notifications.invalidemail.message',
                        [email] as Object[],
                        'Invalid email address: {0}'
                )
            }else if(email){
                validcount++
            }
        }
        if(conf.recipients.isBlank()){
            errors.rejectValue(
                    fieldNames[trigger],
                    'scheduledExecution.notifications.email.blank.message',
                    'Cannot be blank'
            )
        }
        if(conf.attachLog){
            if(!conf.containsKey("attachLogInFile") &&  !conf.containsKey("attachLogInline")){
                errors.rejectValue(
                        fieldAttachedNames[trigger],
                        'scheduledExecution.notifications.email.attached.blank.message',
                        'You need select one of the options'
                )
            }

            if(conf.attachLogInFile == false && conf.attachLogInline == false){
                errors.rejectValue(
                        fieldAttachedNames[trigger],
                        'scheduledExecution.notifications.email.attached.blank.message',
                        'You need select one of the options'
                )
            }
        }
    }
}
