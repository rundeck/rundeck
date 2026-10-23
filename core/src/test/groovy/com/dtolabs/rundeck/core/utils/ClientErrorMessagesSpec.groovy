package com.dtolabs.rundeck.core.utils

import spock.lang.Specification

class ClientErrorMessagesSpec extends Specification {

    def "sql and hibernate failures become a save message"() {
        expect:
        ClientErrorMessages.publicMessage(new RuntimeException("could not execute statement: SQL [n/a]")) ==
                ClientErrorMessages.SAVE_FAILED
        ClientErrorMessages.publicMessage(new GenericJDBCException("could not execute statement")) ==
                ClientErrorMessages.SAVE_FAILED
    }

    def "validation exception text is not returned"() {
        when:
        def leaked = """Validation Error(s) occurred during save():
- Field error in object 'rundeck.db.login.LoginRole' on field 'authority'"""
        def message = ClientErrorMessages.publicMessage(new FakeValidationException(leaked))

        then:
        message == ClientErrorMessages.VALIDATION_FAILED
        !message.contains("LoginRole")
    }

    def "short intentional messages are kept"() {
        expect:
        ClientErrorMessages.publicMessage(new IllegalArgumentException("Schedule not found")) == "Schedule not found"
    }

    def "null and blank failures stay generic"() {
        expect:
        ClientErrorMessages.publicMessage(null) == ClientErrorMessages.REQUEST_FAILED
        ClientErrorMessages.publicMessage(new IllegalStateException((String) null)) == ClientErrorMessages.REQUEST_FAILED
    }

    /** Class name is what the sanitizer classifies; no Grails or Hibernate dependency. */
    static class FakeValidationException extends RuntimeException {
        FakeValidationException(String message) {
            super(message)
        }
    }

    static class GenericJDBCException extends RuntimeException {
        GenericJDBCException(String message) {
            super(message)
        }
    }
}
