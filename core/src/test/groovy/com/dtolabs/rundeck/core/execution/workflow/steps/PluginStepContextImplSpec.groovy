package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.workflow.DataOutput
import com.dtolabs.rundeck.core.execution.workflow.FlowControl
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import spock.lang.Specification

class PluginStepContextImplSpec extends Specification {
    def "create from step execution context"() {
        given:
            def iframework = Mock(IFramework)
            def mockFramework = Mock(Framework)
            def mockListener = Mock(ExecutionListener)
            def nodeSet = new NodeSetImpl()
            def outputContext = new DataOutput(ContextView.global())
            def dataContext = new BaseDataContext([a: [b: 'c']])
            def flowControl = Mock(FlowControl)
            def context = Mock(StepExecutionContext) {
                1 * getDataContextObject() >> dataContext
                1 * getExecutionListener() >> mockListener
                1 * getFlowControl() >> flowControl
                1 * getFramework() >> mockFramework
                1 * getFrameworkProject() >> 'aProject'
                1 * getIFramework() >> iframework
                1 * getNodes() >> nodeSet
                1 * getOutputContext() >> outputContext
                1 * getStepContext() >> [1]
                1 * getStepNumber() >> 1
                0 * _(*_)
            }
        when:
            def pluginContext = PluginStepContextImpl.from(context)
        then:
            pluginContext != null
            pluginContext.getDataContext() == dataContext
            pluginContext.getExecutionContext() == context
            pluginContext.getFlowControl() == flowControl
            pluginContext.getFramework() == mockFramework
            pluginContext.getFrameworkProject() == 'aProject'
            pluginContext.getIFramework() == iframework
            pluginContext.getLogger() == mockListener
            pluginContext.getNodes() == nodeSet
            pluginContext.getOutputContext() == outputContext
            pluginContext.getStepContext() == [1]
            pluginContext.getStepNumber() == 1
    }
}
