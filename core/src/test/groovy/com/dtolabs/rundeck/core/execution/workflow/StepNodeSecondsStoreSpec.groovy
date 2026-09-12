package com.dtolabs.rundeck.core.execution.workflow

import spock.lang.Specification

class StepNodeSecondsStoreSpec extends Specification {

    def store = StepNodeSecondsStore.getInstance()

    def "record then take round-trips the value"() {
        given:
        def executionId = 201L

        when:
        store.recordFinishedTotal(executionId, 42L)

        then:
        store.takeFinishedTotal(executionId) == 42L
    }

    def "takeFinishedTotal on an absent id returns null"() {
        expect:
        store.takeFinishedTotal(202L) == null
    }

    def "takeFinishedTotal removes the entry, so a second call returns null"() {
        given:
        def executionId = 203L
        store.recordFinishedTotal(executionId, 7L)

        when:
        def first = store.takeFinishedTotal(executionId)
        def second = store.takeFinishedTotal(executionId)

        then:
        first == 7L
        second == null
    }

    def "recordFinishedTotal with a null executionId is a no-op"() {
        when:
        store.recordFinishedTotal(null, 99L)

        then:
        noExceptionThrown()
    }

    def "takeFinishedTotal with a null executionId returns null"() {
        expect:
        store.takeFinishedTotal(null) == null
    }
}
