package rundeck.services.validator

import grails.config.Config
import grails.util.Holders

class CharsValidator {

    String regex
    String chars

    CharsValidator() {
        Config conf = Holders.config
        chars = conf.getProperty('rundeck.specialChars.allowed', '')
        regex = /^[a-zA-Z0-9\p{L}\p{M}\s\.,'${chars}\(\)-]+$/
    }

    String[] validate(String value) {
        if (!value) { return null }
        def matcher = value =~ /${regex}/
        if (matcher.find()) {
            return null
        }
        return ['characters.invalid', value]
    }
}
