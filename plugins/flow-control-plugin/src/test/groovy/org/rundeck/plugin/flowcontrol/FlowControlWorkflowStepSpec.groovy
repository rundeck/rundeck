package org.rundeck.plugin.flowcontrol

import com.dtolabs.rundeck.core.execution.workflow.FlowControl
import com.dtolabs.rundeck.plugins.PluginLogger
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

class FlowControlWorkflowStepSpec extends Specification{


    def "correct halt function"() {
        given:
        def plugin = new FlowControlWorkflowStep()
        plugin.halt = halt
        plugin.fail = fail

        def flowCtrl = Mock(FlowControl)


        def context = Mock(PluginStepContext) {
            getLogger() >> Mock(PluginLogger)
            getFlowControl() >> flowCtrl
        }


        when:
        plugin.executeStep(context, [:])

        then:
        if(halt) {
            1 * flowCtrl.Halt(suceess)
            0 * flowCtrl.Continue()
        }else {
            0 * flowCtrl.Halt(_)
            1 * flowCtrl.Continue()

        }
        where:
        halt    | fail          | suceess
        true    | true          | false
        true    | false         | true
        false   | true          | _
        false   | false         | _

    }


}
