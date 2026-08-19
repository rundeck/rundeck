package com.dtolabs.rundeck.core.data

import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import spock.lang.Specification

class UnexpandableBehaviorSpec extends Specification {

    def "parse accepts known values"() {
        expect:
        UnexpandableBehavior.parse(input) == expected

        where:
        input            | expected
        'blank'          | UnexpandableBehavior.BLANK
        'preserve'       | UnexpandableBehavior.PRESERVE
        'preserveBash'   | UnexpandableBehavior.PRESERVE_BASH
        'preserve_bash'  | UnexpandableBehavior.PRESERVE_BASH
        'preserve-bash'  | UnexpandableBehavior.PRESERVE_BASH
        'PRESERVEBASH'   | UnexpandableBehavior.PRESERVE_BASH
        null             | null
        ''               | null
        '  '             | null
        'unknown'        | null
    }

    def "fromBlankIfUnexpandable maps classic boolean"() {
        expect:
        UnexpandableBehavior.fromBlankIfUnexpandable(true) == UnexpandableBehavior.BLANK
        UnexpandableBehavior.fromBlankIfUnexpandable(false) == UnexpandableBehavior.PRESERVE
    }

    def "shouldBlankUnresolved preserveBash blanks dotted only"() {
        expect:
        SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.BLANK, 'option.op1')
        SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.BLANK, 'MYVAR')
        !SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.PRESERVE, 'option.op1')
        !SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.PRESERVE, 'MYVAR')
        SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.PRESERVE_BASH, 'option.op1')
        SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.PRESERVE_BASH, 'node.hostname')
        !SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.PRESERVE_BASH, 'MYVAR')
        !SharedDataContextUtils.shouldBlankUnresolved(UnexpandableBehavior.PRESERVE_BASH, 'MYVAR:-default')
    }

    def "preserveBash expands present option, blanks missing option, keeps bash vars"() {
        given:
        WFSharedContext shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), new BaseDataContext('option', [op1: '1,2']))

        when:
        String result = SharedDataContextUtils.replaceDataReferences(
                'echo ${option.op1} ${option.missing} ${MYVAR} ${OTHER:-x}',
                shared,
                ContextView.global(),
                ContextView.&nodeStep,
                null,
                false,
                UnexpandableBehavior.PRESERVE_BASH
        )

        then:
        result == 'echo 1,2  ${MYVAR} ${OTHER:-x}'
    }

    def "blank blanks all unresolved including bash"() {
        given:
        WFSharedContext shared = SharedDataContextUtils.sharedContext()

        when:
        String result = SharedDataContextUtils.replaceDataReferences(
                'echo ${option.missing} ${MYVAR}',
                shared,
                ContextView.global(),
                ContextView.&nodeStep,
                null,
                false,
                UnexpandableBehavior.BLANK
        )

        then:
        result == 'echo  '
    }

    def "preserve leaves all unresolved intact"() {
        given:
        WFSharedContext shared = SharedDataContextUtils.sharedContext()

        when:
        String result = SharedDataContextUtils.replaceDataReferences(
                'echo ${option.missing} ${MYVAR}',
                shared,
                ContextView.global(),
                ContextView.&nodeStep,
                null,
                false,
                UnexpandableBehavior.PRESERVE
        )

        then:
        result == 'echo ${option.missing} ${MYVAR}'
    }
}
