package rundeck.data.validation.validators.notification

import rundeck.data.constants.NotificationConstants
import rundeck.data.job.RdNotification
import spock.lang.Specification
import spock.lang.Unroll

class UrlNotificationValidatorSpec extends Specification {

    //source - services/ScheduledExecutionServiceSpec."invalid notifications data"
    @Unroll
    def "invalid notifications data"() {
        given:
        UrlNotificationValidator validator = new UrlNotificationValidator()

        when:
        RdNotification n = new RdNotification(eventTrigger: trigger,
                type: type,
                configuration: [urls: content])
        validator.validate(n, n.errors)

        then:
        n.errors.hasErrors()
        n.errors.fieldErrors[0].code == code

        where:
        trigger                                             | type    | content  | code
        NotificationConstants.ONSUCCESS_TRIGGER_NAME | 'url' | '' | 'scheduledExecution.notifications.url.blank.message'
        NotificationConstants.ONSUCCESS_TRIGGER_NAME | 'url' | 'htttp://localhost'|'scheduledExecution.notifications.invalidurl.message'
    }
}
