package com.dtolabs.rundeck.core.plugins.configuration

import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification
import spock.lang.Unroll

import static com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.FeatureFlag
import static com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Framework
import static com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Instance
import static com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.InstanceOnly
import static com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Project
import static com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.ProjectOnly
import static com.dtolabs.rundeck.core.plugins.configuration.PropertyScope.Unspecified

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

    def "isPropertyScopeIgnored"() {
        expect:
            expect == Validator.isPropertyScopeIgnored(propScope, ignored)
        where:
            propScope    | ignored      | expect
            null         | null         | false
            Instance     | null         | false
            InstanceOnly | null         | false
            Framework    | null         | false
            Project      | null         | false
            ProjectOnly  | null         | false
            Unspecified  | null         | false
            FeatureFlag  | null         | true

            Instance     | Instance     | true
            InstanceOnly | Instance     | true
            ProjectOnly  | Instance     | true
            Project      | Instance     | true
            Framework    | Instance     | true
            Unspecified  | Instance     | false
            FeatureFlag  | Instance     | true

            Instance     | InstanceOnly | false
            InstanceOnly | InstanceOnly | true
            ProjectOnly  | InstanceOnly | true
            Project      | InstanceOnly | true
            Framework    | InstanceOnly | true
            Unspecified  | InstanceOnly | false
            FeatureFlag  | InstanceOnly | true

            Instance     | Project      | false
            InstanceOnly | Project      | false
            ProjectOnly  | Project      | true
            Project      | Project      | true
            Framework    | Project      | true
            Unspecified  | Project      | false
            FeatureFlag  | Project      | true

            Instance     | ProjectOnly  | false
            InstanceOnly | ProjectOnly  | false
            ProjectOnly  | ProjectOnly  | true
            Project      | ProjectOnly  | false
            Framework    | ProjectOnly  | true
            Unspecified  | ProjectOnly  | false
            FeatureFlag  | ProjectOnly  | true

            Instance     | Framework    | false
            InstanceOnly | Framework    | false
            ProjectOnly  | Framework    | false
            Project      | Framework    | false
            Framework    | Framework    | true
            FeatureFlag  | Framework    | true

    }
}
