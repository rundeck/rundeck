package org.rundeck.app.data.validation.validators.notification

import org.rundeck.app.data.job.RdNotification
import rundeck.controllers.ScheduledExecutionController
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
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd'
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | '${job.user.name}@something.org'
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | '${job.user.email}'
        ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'monkey@internal'
        ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'user@test'
        ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'
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
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | ''| 'scheduledExecution.notifications.email.blank.message'
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd@example.com'|'scheduledExecution.notifications.invalidemail.message'
    }
}
