package com.dtolabs.rundeck.core.execution.workflow.steps

import spock.lang.Specification

class StepExecutionResultImplSpec extends Specification {

    def "wrapStepException copies failure data #failureData"() {
        given:
            def e = new StepException('failed', StepFailureReason.Unknown, failureData)

        when:
            def result = StepExecutionResultImpl.wrapStepException(e)

        then:
            !result.success
            result.exception.is(e)
            result.failureReason == StepFailureReason.Unknown
            result.failureMessage == 'failed'
            result.failureData == expected

        where:
            failureData     || expected
            [resultCode: 1] || [resultCode: 1]
            null            || [:]
    }

    def "wrapStepException copies the map so later changes to the exception data do not leak"() {
        given:
            def data = [resultCode: 1]
            def result = StepExecutionResultImpl.wrapStepException(new StepException('failed', StepFailureReason.Unknown, data))

        when:
            data.resultCode = 2

        then:
            result.failureData.resultCode == 1
    }
}
