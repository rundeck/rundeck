package rundeck.data.validation

import grails.validation.Validateable
import org.springframework.context.MessageSource

class ValidationResponse {
    boolean valid = true
    List<String> errors = []

    ValidationResponse createFrom(MessageSource messageSource, Validateable obj) {
        valid = obj.errors.errorCount == 0
        obj.errors.globalErrors.each { err ->
            errors.add(messageSource.getMessage(err.code, err.arguments, err.defaultMessage, Locale.getDefault()))
        }
        obj.errors.fieldErrors.each { err ->
            errors.add(err.field + " : "+ messageSource.getMessage(err.code, err.arguments, err.defaultMessage, Locale.getDefault()))
        }
        this
    }
}
