package rundeck.data.validation.validators.notification

import org.rundeck.app.data.model.v1.job.notification.NotificationData
import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.data.validation.validators.AnyDomainEmailValidator

class EmailNotificationValidator implements Validator {

    @Override
    boolean supports(Class<?> clazz) {
        return NotificationData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        NotificationData n = (NotificationData)target
        Map conf = n.configuration
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
                        "configuration",
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
                    "configuration",
                    'scheduledExecution.notifications.email.blank.message',
                    'Cannot be blank'
            )
        }
        if(conf.attachLog){
            if(!conf.containsKey("attachLogInFile") &&  !conf.containsKey("attachLogInline")){
                errors.rejectValue(
                        "configuration",
                        'scheduledExecution.notifications.email.attached.blank.message',
                        'You need select one of the options'
                )
            }

            if(conf.attachLogInFile == false && conf.attachLogInline == false){
                errors.rejectValue(
                        "configuration",
                        'scheduledExecution.notifications.email.attached.blank.message',
                        'You need select one of the options'
                )
            }
        }
    }
}
