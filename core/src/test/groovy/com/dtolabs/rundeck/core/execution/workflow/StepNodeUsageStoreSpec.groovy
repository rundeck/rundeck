package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.execution.workflow.StepNodeUsageWorkflowListener.StepNodeUsageEntry
import spock.lang.Specification

class StepNodeUsageStoreSpec extends Specification {

    def store = StepNodeUsageStore.getInstance()

    def "record then take round-trips the breakdown map"() {
        given:
        def executionId = 201L
        def breakdown = ["1": new StepNodeUsageEntry(42L, "exec-command", true)]

        when:
        store.recordFinishedBreakdown(executionId, breakdown)

        then:
        def result = store.takeFinishedBreakdown(executionId)
        result["1"].seconds == 42L
        result["1"].pluginType == "exec-command"
    }

    def "takeFinishedBreakdown on an absent id returns null"() {
        expect:
        store.takeFinishedBreakdown(202L) == null
    }

    def "takeFinishedBreakdown removes the entry, so a second call returns null"() {
        given:
        def executionId = 203L
        store.recordFinishedBreakdown(executionId, ["1": new StepNodeUsageEntry(7L, "exec-command", true)])

        when:
        def first = store.takeFinishedBreakdown(executionId)
        def second = store.takeFinishedBreakdown(executionId)

        then:
        first["1"].seconds == 7L
        second == null
    }

    def "recordFinishedBreakdown with a null executionId is a no-op"() {
        when:
        store.recordFinishedBreakdown(null, ["1": new StepNodeUsageEntry(99L, "exec-command", true)])

        then:
        noExceptionThrown()
    }

    def "takeFinishedBreakdown with a null executionId returns null"() {
        expect:
        store.takeFinishedBreakdown(null) == null
    }
}
