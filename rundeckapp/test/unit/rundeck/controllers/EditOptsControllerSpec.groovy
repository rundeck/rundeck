package rundeck.controllers

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Option
import spock.lang.Specification

/**
 * Created by greg on 2/11/16.
 */
@TestFor(EditOptsController)
@Mock(Option)
class EditOptsControllerSpec extends Specification {
    def "validate opt required scheduled job with default storage path"() {
        given:
        Option opt = new Option(required: true, defaultValue: defval, defaultStoragePath: defstorageval)

        when:
        EditOptsController._validateOption(opt, params, true)
        then:
        iserr == opt.errors.hasFieldErrors('defaultValue')

        where:
        iserr | defval | defstorageval
        true  | null   | null
        false | 'abc'  | null
        false | null   | 'abc'

    }
}
