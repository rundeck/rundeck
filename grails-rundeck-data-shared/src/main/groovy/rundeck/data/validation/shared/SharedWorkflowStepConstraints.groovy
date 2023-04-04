package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedWorkflowStepConstraints implements Validateable {
    Boolean keepgoingOnSuccess
    String description

    static constraints = {
        keepgoingOnSuccess(nullable: true)
        description(nullable: true, maxSize: 1024)
    }
}
