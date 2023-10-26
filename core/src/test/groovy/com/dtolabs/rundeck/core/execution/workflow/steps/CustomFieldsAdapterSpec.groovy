package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import spock.lang.Specification

class CustomFieldsAdapterSpec extends Specification {

    def "plain field no effect"() {
        given:
        def fieldName = 'somefield'
        def description = DescriptionBuilder.builder().name('APlugin')
                .property(PropertyBuilder.builder().string(fieldName).build())
                .build()
        def adapter = CustomFieldsAdapter.create(description)
        when:
        def result = adapter.convertInput(fieldName, inputVal)

        then:
        result == inputVal
        when:
        def out = adapter.convertOutput(fieldName, outputVal)

        then:
        out == outputVal

        where:
        inputVal  | outputVal
        'someval' | 'outval'
    }

    def "field with DYNAMIC_FORM converts the input from Array of dynamic form entries in JSON"() {
        given:
        def fieldName = 'somefield'
        def description = DescriptionBuilder.builder().name('APlugin')
                .property(PropertyBuilder.builder().string(fieldName)
                        .renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType.DYNAMIC_FORM)
                        .build())
                .build()
        def adapter = CustomFieldsAdapter.create(description)
        when:
        def result = adapter.convertInput(fieldName, inputVal)

        then:
        result == convertedInput

        when:
        def outresult = adapter.convertOutput(fieldName, newOutput)

        then:
        outresult == convertedOutput


        where:
        inputVal = '[{"key":"akey","value":"avalue"},{"key":"bkey","value":"bvalue"}]'
        convertedInput = [akey: 'avalue', bkey: 'bvalue']
        newOutput = [akey: 'avalue2', bkey: 'bvalue2 "quoted"']
        convertedOutput = '[{"key":"akey","value":"avalue2"},{"key":"bkey","value":"bvalue2 \\\"quoted\\\""}]'
    }
}
