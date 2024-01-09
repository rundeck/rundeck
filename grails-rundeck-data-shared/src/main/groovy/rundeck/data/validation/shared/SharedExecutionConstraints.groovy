package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedExecutionConstraints implements Validateable {
    String jobUuid
    String argString
    String status
    String userRoleList
    Date dateStarted
    Date dateCompleted
    String outputfilepath
    String loglevel
    String failedNodeList
    String succeededNodeList
    String abortedby
    Boolean timedOut=false
    String executionType
    Integer retryAttempt=0
    Boolean willRetry
    String timeout
    String retry
    String retryDelay

    static constraints = {
        jobUuid(nullable:true)
        argString(nullable:true)
        dateStarted(nullable:true)
        dateCompleted(nullable:true)
        userRoleList(nullable: true)
        outputfilepath(nullable:true)
        loglevel(nullable:true)
        status(nullable:true)
        failedNodeList(nullable:true, blank:true)
        succeededNodeList(nullable:true, blank:true)
        abortedby(nullable:true, blank:true)
        timedOut(nullable: true)
        executionType(nullable: true, maxSize: 30)
        retryAttempt(nullable: true)
        willRetry(nullable: true)
        timeout(maxSize: 256, blank: true, nullable: true,)
        retry(maxSize: 256, blank: true, nullable: true,matches: /^\d+$/)
        retryDelay(nullable:true)
    }
}
