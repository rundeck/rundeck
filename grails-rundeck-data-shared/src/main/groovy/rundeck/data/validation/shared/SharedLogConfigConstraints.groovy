package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedLogConfigConstraints implements Validateable {
    String logOutputThreshold
    String logOutputThresholdAction
    String logOutputThresholdStatus

    static constraints = {
        logOutputThreshold(maxSize: 256, blank:true, nullable: true)
        logOutputThresholdAction(maxSize: 256, blank:true, nullable: true,inList: ['halt','truncate'])
        logOutputThresholdStatus(maxSize: 256, blank:true, nullable: true)
    }
}
