package com.dtolabs.rundeck.core.execution

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import spock.lang.Specification

class NodeExecutorUtilsSpec extends Specification {

    def "getExportedVariablesForNode without exclude"() {
        given:
        def nodea = new NodeEntryImpl("nodea.host", "nodea")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN, "export {key} = '{value}'")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN_SEPARATOR, ";")

        def orig = ExecutionContextImpl.builder()
                .stepContext([1, 2])
                .stepNumber(3)
                .dataContext(DataContextUtils.context('b', [c: 'd']))
                .build()
        orig.getDataContext()
        def commandList = ["ls"]
        when:
        def result = NodeExecutorUtils.getExportedVariablesForNode(nodea, orig, commandList)

        then:
        result.size() == 2
        result[0] == 'export RD_B_C = \'d\';'
        result[1] == 'ls'
    }

    def "getExportedVariablesForNode with exclude node attributes"() {
        given:
        def nodea = new NodeEntryImpl("nodea.host", "nodea")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN, "export {key} = '{value}'")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN_SEPARATOR, ";")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN_EXCLUDE_NODES, "true")

        def orig = ExecutionContextImpl.builder()
                .stepContext([1, 2])
                .stepNumber(3)
                .dataContext(DataContextUtils.context('node', [c: 'd']))
                .build()
        orig.getDataContext()
        def commandList = ["ls"]
        when:
        def result = NodeExecutorUtils.getExportedVariablesForNode(nodea, orig, commandList)

        then:
        result.size() == 1
        result[0] == 'ls'
    }

    def "getExportedVariablesForNode with exclude node attributes and addional attributes added"() {
        given:
        def nodea = new NodeEntryImpl("nodea.host", "nodea")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN, "export {key} = '{value}'")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN_SEPARATOR, ";")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN_EXCLUDE_NODES, "true")
        def attributes = ['node':[c:'d']]
        attributes << ['job':[e:'f']]
        def orig = ExecutionContextImpl.builder()
                .stepContext([1, 2])
                .stepNumber(3)
                .dataContext(DataContextUtils.context(attributes))
                .build()
        orig.getDataContext()
        def commandList = ["ls"]
        when:
        def result = NodeExecutorUtils.getExportedVariablesForNode(nodea, orig, commandList)

        then:
        result.size() == 2
        result[0] == 'export RD_JOB_E = \'f\';'
        result[1] == 'ls'
    }
}
