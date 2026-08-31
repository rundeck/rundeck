package com.dtolabs.rundeck.core.execution

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.component.SshExportQuotingConfig
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

    def "getExportedVariablesForNode shell-quotes exported values to prevent injection (RUN-4579)"() {
        given: 'a node with a bare {value} export pattern and a malicious option value'
        def nodea = new NodeEntryImpl("nodea.host", "nodea")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN, 'export {key}={value}')
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN_SEPARATOR, ";")

        def builder = ExecutionContextImpl.builder()
                .stepContext([1, 2])
                .stepNumber(3)
                .dataContext(DataContextUtils.context('b', [c: '; id']))
        if (quotingComponent != null) {
            builder.addComponent(SshExportQuotingConfig.COMPONENT_NAME, quotingComponent, SshExportQuotingConfig)
        }
        def orig = builder.build()
        def commandList = ["ls"]

        when:
        def result = NodeExecutorUtils.getExportedVariablesForNode(nodea, orig, commandList)

        then: 'when quoting is enabled the ; is inside single quotes and cannot separate commands; default/off is legacy'
        result[0] == expected

        where:
        quotingComponent                  || expected
        null                              || "export RD_B_C=; id;"     // default (opt-in off): legacy unquoted
        new SshExportQuotingConfig(false) || "export RD_B_C=; id;"     // off: legacy unquoted
        new SshExportQuotingConfig(true)  || "export RD_B_C='; id';"   // opt-in on: quoted, injection neutralized
    }

    def "getExportedVariablesForNode leaves benign values unquoted (quoting is a no-op)"() {
        given: 'a benign value contains no shell metacharacters'
        def nodea = new NodeEntryImpl("nodea.host", "nodea")
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN, 'export {key}={value}')
        nodea.setAttribute(NodeExecutorUtils.RD_VARIABLE_PATTERN_SEPARATOR, ";")

        def orig = ExecutionContextImpl.builder()
                .stepContext([1, 2])
                .stepNumber(3)
                .dataContext(DataContextUtils.context('b', [c: 'd']))
                .build()
        def commandList = ["ls"]

        when:
        def result = NodeExecutorUtils.getExportedVariablesForNode(nodea, orig, commandList)

        then: 'quoteUnixShellArg adds no quotes, so ordinary configurations are unaffected'
        result[0] == 'export RD_B_C=d;'
    }
}
