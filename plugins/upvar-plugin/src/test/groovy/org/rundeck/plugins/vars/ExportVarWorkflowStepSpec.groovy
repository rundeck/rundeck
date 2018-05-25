package org.rundeck.plugin.vars

import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext
import com.dtolabs.rundeck.plugins.step.PluginStepContext

import spock.lang.Specification


class ExportVarWorkflowStepSpec extends Specification {

    def "empty group or name throws exception"() {
        given:
        def plugin = new ExportVarWorkflowStep()
        def context = Mock(PluginStepContext)
        def config = [:]
        plugin.setProperties(group,name,value)
        when:
        plugin.executeStep(context, config)

        then:
        StepException e = thrown()

        where:
        group    | name     | value
        'grp'    | 'name'   | ''
        'grp'    | ''       | 'val'
        ' '      | 'name'   | 'val'
    }


    def "set value"(){
        given:
        def plugin = new ExportVarWorkflowStep()
        def context = Mock(PluginStepContext)
        def config = [:]
        plugin.setProperties(value, group,name)

        when:
        plugin.executeStep(context, config)

        then:
        1 * context.getOutputContext() >> Mock(SharedOutputContext) {
            1 * addOutput(ContextView.global(),group,name,value)
        }

        where:
        group    | name     | value
        'grp'    | 'name'   | 'value'

    }

}