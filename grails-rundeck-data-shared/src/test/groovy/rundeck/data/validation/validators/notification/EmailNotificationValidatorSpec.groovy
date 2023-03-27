package rundeck.data.validation.validators.notification

import rundeck.data.constants.NotificationConstants
import rundeck.data.job.RdNotification
import spock.lang.Specification
import spock.lang.Unroll

class EmailNotificationValidatorSpec extends Specification {

    //source - services/ScheduledExecutionServiceSpec."validate notifications email data any domain #trigger for #content"
    @Unroll
    def "validate notifications email data any domain #trigger for #content"() {
        given:
        EmailNotificationValidator validator = new EmailNotificationValidator()

        when:
        RdNotification n = new RdNotification(eventTrigger: trigger,
                type: type,
                configuration: [recipients: content])
        validator.validate(n, n.errors)

        then:
        n.errors.errorCount == 0

        where:
        trigger                                             | type    | content
        NotificationConstants.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd'
        NotificationConstants.ONSUCCESS_TRIGGER_NAME | 'email' | '${job.user.name}@something.org'
        NotificationConstants.ONFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'
        NotificationConstants.ONFAILURE_TRIGGER_NAME | 'email' | '${job.user.email}'
        NotificationConstants.ONSTART_TRIGGER_NAME   | 'email' | 'monkey@internal'
        NotificationConstants.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'user@test'
        NotificationConstants.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'
    }

    //source - services/ScheduledExecutionServiceSpec."invalid notifications data"
    @Unroll
    def "invalid notifications data"() {
        given:
        EmailNotificationValidator validator = new EmailNotificationValidator()

        when:
        RdNotification n = new RdNotification(eventTrigger: trigger,
                type: type,
                configuration: [recipients: content])
        validator.validate(n, n.errors)

        then:
        n.errors.hasErrors()
        n.errors.fieldErrors[0].code == code

        where:
        trigger                                             | type    | content  | code
        NotificationConstants.ONSUCCESS_TRIGGER_NAME | 'email' | '' | 'scheduledExecution.notifications.email.blank.message'
        NotificationConstants.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd@example.com'|'scheduledExecution.notifications.invalidemail.message'
    }
}
