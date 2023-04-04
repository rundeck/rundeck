package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedNotificationConstraints implements Validateable {
    String eventTrigger
    String type
    String format

    static constraints = {
        eventTrigger(nullable:false,blank:false)
        type(nullable:false,blank:false)
        format(nullable:true,blank:true)
    }
}
