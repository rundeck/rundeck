package com.dtolabs.rundeck.core.execution

import spock.lang.Specification

/**
 * @author greg
 * @since 8/1/17
 */
class ExecutionContextImplSpec extends Specification {
    def "original step context not modified"() {
        given:
        def orig = ExecutionContextImpl.builder()
                                       .stepContext([1, 2])
                                       .stepNumber(3)
                                       .build()


        when:
        def newctx = ExecutionContextImpl.builder(orig)
                                         .pushContextStep(4)
                                         .build()

        then:
        orig.stepContext == [1, 2]
        orig.stepNumber == 3

        newctx.stepContext == [1, 2, 3]
        newctx.stepNumber == 4
    }
}
