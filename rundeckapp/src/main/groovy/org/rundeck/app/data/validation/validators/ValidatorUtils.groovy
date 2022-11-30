package org.rundeck.app.data.validation.validators

import grails.util.Holders
import grails.validation.Validateable
import org.springframework.context.MessageSource
import org.springframework.validation.Errors

class ValidatorUtils {

    static Closure nestedValidator = { val, obj, errors ->
        if (!val) return true
        if (!val.validate()) {
            processErrors(propertyName, val, errors)
            return false
        }
        return true
    }

    static void processErrors(String propertyName, Validateable val, Errors errors) {
        MessageSource messageSource = Holders.applicationContext
        val.errors.fieldErrors.each { err ->
            def fieldName = err.arguments ? err.arguments[0] : err.properties['field']
            if (fieldName != null) {
                String errorCode = "${propertyName}.${err.code}"
                if (val.hasProperty(fieldName)) {
                    errorCode = "${propertyName}.$fieldName}.${err.code}"
                }
                def args = err.arguments ?: [err.rejectedValue] as Object[]
                String msg = err.defaultMessage ?: messageSource.getMessage(err.code, err.arguments, Locale.getDefault()) ?: "Invalid value for {0}"
                errors.rejectValue("${propertyName}.${err.properties['field']}", errorCode, args, msg)
            }
        }
        val.errors.globalErrors.each {err ->
             errors.reject(err.code, err.arguments, err.defaultMessage)
        }
    }
}
