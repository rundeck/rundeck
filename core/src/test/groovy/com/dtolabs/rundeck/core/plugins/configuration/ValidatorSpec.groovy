package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification
import spock.lang.Unroll

class ValidatorSpec extends Specification {
    static class TestValidator implements PropertyObjectValidator {
        @Override
        boolean isValid(final Object value) throws ValidationException {
            return value instanceof List && value.size() == 2
        }

        @Override
        boolean isValid(final String value) throws ValidationException {
            return isValid(value.split(",").toList())
        }
    }

    @Unroll
    def "validate values allows PropertyObjectValidator"() {
        given:
            def props = [
                PropertyBuilder.builder().
                    name("aprop").
                    type(Property.Type.Options).
                    validator(new TestValidator()).
                    build()
            ]
            Map<String, Object> values = new HashMap<>(input)

        when:
            def result = Validator.validateValues(values, props)
        then:
            result.valid == expect
        where:
            input                           | expect
            [aprop: 'asdf']                 | false
            [aprop: 'asdf,xyz']             | true
            [aprop: ['asdf']]               | false
            [aprop: ['asdf', 'xyz']]        | true
            [aprop: ['asdf', 'xyz', 'abc']] | false
    }
}
