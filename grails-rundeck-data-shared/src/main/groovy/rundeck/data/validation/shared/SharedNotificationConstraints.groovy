package rundeck.data.validation.shared

import grails.validation.Validateable
import rundeck.data.constants.NotificationConstants

class SharedNotificationConstraints implements Validateable {
    String eventTrigger
    String type
    String format

    static constraints = {
        eventTrigger(nullable: false, blank: false, inList: new ArrayList<String>(NotificationConstants.TRIGGER_NAMES))
        type(nullable:false,blank:false)
        format(nullable:true,blank:true)
    }
}
