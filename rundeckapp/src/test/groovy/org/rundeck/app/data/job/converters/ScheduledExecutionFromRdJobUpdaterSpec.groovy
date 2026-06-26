package org.rundeck.app.data.job.converters

import grails.testing.gorm.DataTest
import rundeck.CommandExec
import rundeck.Notification
import rundeck.Option
import rundeck.Orchestrator
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.job.RdNodeConfig
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Tests for {@link ScheduledExecutionFromRdJobUpdater#updateNodeConfig}.
 *
 * Verifies that nodesSelectedByDefault is reset to the safe default when
 * a job switches from "Dispatch to Nodes" to "Execute Locally"
 * (RUN-4288).
 */
class ScheduledExecutionFromRdJobUpdaterSpec extends Specification implements DataTest {

    def setupSpec() {
        mockDomains(ScheduledExecution, Workflow, CommandExec, Option, Notification, Orchestrator)
    }

    def "updateNodeConfig resets nodesSelectedByDefault to true when doNodedispatch is false (Execute Locally)"() {
        given: "a node config that switches to Execute Locally but still carries the explicit-selection flag"
        def nodeConfig = new RdNodeConfig(
            doNodedispatch       : false,
            nodesSelectedByDefault: false,  // explicit selection was previously enabled
        )
        def se = new ScheduledExecution()

        when: "the node config is applied"
        ScheduledExecutionFromRdJobUpdater.updateNodeConfig(se, nodeConfig)

        then: "nodesSelectedByDefault is reset to true because local execution never requires explicit node selection"
        se.doNodedispatch == false
        se.nodesSelectedByDefault == true
    }

    @Unroll
    def "updateNodeConfig preserves nodesSelectedByDefault when doNodedispatch is true (Dispatch to Nodes)"() {
        given: "a node config with Dispatch to Nodes and explicit selection set to #explicitSelection"
        def nodeConfig = new RdNodeConfig(
            doNodedispatch        : true,
            nodesSelectedByDefault: explicitSelection,
            filter                : '.*',
        )
        def se = new ScheduledExecution()

        when:
        ScheduledExecutionFromRdJobUpdater.updateNodeConfig(se, nodeConfig)

        then: "nodesSelectedByDefault is preserved exactly as supplied"
        se.doNodedispatch == true
        se.nodesSelectedByDefault == explicitSelection

        where:
        explicitSelection << [true, false]
    }

    def "updateNodeConfig resets nodesSelectedByDefault when switching from dispatch to local (full round-trip)"() {
        given: "a job that was previously configured for dispatch with explicit selection"
        def se = new ScheduledExecution(
            doNodedispatch        : true,
            nodesSelectedByDefault: false,
            filter                : '.*',
        )

        and: "an updated node config that switches to Execute Locally"
        def localConfig = new RdNodeConfig(
            doNodedispatch        : false,
            nodesSelectedByDefault: false,  // stale value carried from UI form submission
        )

        when: "the node config is updated"
        ScheduledExecutionFromRdJobUpdater.updateNodeConfig(se, localConfig)

        then: "the job no longer requires explicit node selection"
        !se.doNodedispatch
        se.nodesSelectedByDefault == true
    }
}
