package org.rundeck.app.data.exception

import grails.validation.Validateable

class DataValidationException extends RuntimeException {

    Validateable target

    DataValidationException(Validateable target) {
        this.target = target
    }
}
