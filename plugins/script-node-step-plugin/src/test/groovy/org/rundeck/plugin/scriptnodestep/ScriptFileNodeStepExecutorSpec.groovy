package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

class ScriptFileNodeStepExecutorSpec extends Specification {
    def "basic execute test"() {
        given:
            def scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                "scriptInterpreter",
                true,
                "fileExtension",
                "argString",
                null,
                "adhocLocalString",
                true,
                null
            )
            scriptFileNodeStepExecutor.scriptUtils = Mock(ScriptFileNodeStepUtils)
            def context = Mock(PluginStepContext) {
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * getPropertyLookup() >> Mock(PropertyLookup) {
                        _ * hasProperty('execution.script.tokenexpansion.enabled') >> { expansionEnabled != null }
                        _ * getProperty('execution.script.tokenexpansion.enabled') >> expansionEnabled.toString()
                    }
                }
                _ * getExecutionContext() >> Mock(ExecutionContext)
            }
            def node = Mock(INodeEntry)

        when:
            scriptFileNodeStepExecutor.executeScriptFile(context, node, null)

        then:
            1 * scriptFileNodeStepExecutor.scriptUtils.executeScriptFile(
                _,
                node,
                'adhocLocalString',
                null,
                null,
                'fileExtension',
                ['argString'].toArray(),
                'scriptInterpreter',
                null,
                true,
                _,
                expectedEnabled,
                null
            ) >> Mock(NodeStepResult) {
                isSuccess() >> true
            }
        where:
            expansionEnabled | expectedEnabled
            null             | true
            true             | true
            false            | false
    }
    def "execute with input stream"() {
        given:
            def scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                "scriptInterpreter",
                true,
                "fileExtension",
                "argString",
                null,
                "adhocLocalString",
                true,
                null
            )
            scriptFileNodeStepExecutor.scriptUtils = Mock(ScriptFileNodeStepUtils)
            def context = Mock(PluginStepContext) {
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * getPropertyLookup() >> Mock(PropertyLookup) {

                    }
                }
                _ * getExecutionContext() >> Mock(ExecutionContext)
            }
            def node = Mock(INodeEntry)

        when:
            scriptFileNodeStepExecutor.executeScriptFile(context, node, new ByteArrayInputStream(input.bytes))

        then:
            1 * scriptFileNodeStepExecutor.scriptUtils.executeScriptFile(
                _,
                node,
                'adhocLocalString',
                null,
                null,
                'fileExtension',
                ['argString'].toArray(),
                'scriptInterpreter',
                { it.text == input },
                true,
                _,
                true,
                null
            ) >> Mock(NodeStepResult) {
                isSuccess() >> true
            }
            0 * scriptFileNodeStepExecutor.scriptUtils.executeScriptFile(*_)
        where:
            input ='some data'
    }
    def "execute with modifier"() {
        given:
            def scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                "scriptInterpreter",
                true,
                "fileExtension",
                "argString",
                null,
                "adhocLocalString",
                true,
                Mock(FileCopierUtil.ContentModifier)
            )
            scriptFileNodeStepExecutor.scriptUtils = Mock(ScriptFileNodeStepUtils)
            def context = Mock(PluginStepContext) {
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * getPropertyLookup() >> Mock(PropertyLookup) {

                    }
                }
                _ * getExecutionContext() >> Mock(ExecutionContext)
            }
            def node = Mock(INodeEntry)

        when:
            scriptFileNodeStepExecutor.executeScriptFile(context, node, null)

        then:
            1 * scriptFileNodeStepExecutor.scriptUtils.executeScriptFile(
                _,
                node,
                'adhocLocalString',
                null,
                null,
                'fileExtension',
                ['argString'].toArray(),
                'scriptInterpreter',
                null,
                true,
                _,
                true,
                !null
            ) >> Mock(NodeStepResult) {
                isSuccess() >> true
            }
            0 * scriptFileNodeStepExecutor.scriptUtils.executeScriptFile(*_)
    }

    def "file path"() {
        given:
            def scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                "scriptInterpreter",
                true,
                "fileExtension",
                "argString",
                'some/path',
                null,
                expandTokens,
                null
            )
            scriptFileNodeStepExecutor.scriptUtils = Mock(ScriptFileNodeStepUtils)
            def context = Mock(PluginStepContext) {
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * getPropertyLookup() >> Mock(PropertyLookup) {

                    }
                }
                _ * getExecutionContext() >> Mock(ExecutionContext)
            }
            def node = Mock(INodeEntry)

        when:
            scriptFileNodeStepExecutor.executeScriptFile(context, node, null)

        then:
            1 * scriptFileNodeStepExecutor.scriptUtils.executeScriptFile(
                _,
                node,
                null,
                'some/path',
                null,
                'fileExtension',
                ['argString'].toArray(),
                'scriptInterpreter',
                null,
                true,
                _,
                expectedEnabled,
                null
            ) >> Mock(NodeStepResult) {
                isSuccess() >> true
            }
        where:
            expandTokens | expectedEnabled
            true         | true
            false        | false
    }

    def "file path shared data variable expansion"() {
        given:
            def scriptFileNodeStepExecutor = new ScriptFileNodeStepExecutor(
                "scriptInterpreter",
                true,
                "fileExtension",
                "argString",
                filePath,
                null,
                true,
                null
            )
            scriptFileNodeStepExecutor.scriptUtils = Mock(ScriptFileNodeStepUtils)
            def dataContext = WFSharedContext.with(ContextView.global(), new BaseDataContext(contextData))
            def context = Mock(PluginStepContext) {
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * getPropertyLookup() >> Mock(PropertyLookup) {

                    }
                }
                _ * getExecutionContext() >> Mock(ExecutionContext) {
                    getSharedDataContext() >> dataContext
                }
            }
            def node = Mock(INodeEntry)

        when:
            scriptFileNodeStepExecutor.executeScriptFile(context, node, null)

        then:
            1 * scriptFileNodeStepExecutor.scriptUtils.executeScriptFile(
                _,
                node,
                null,
                result,
                null,
                'fileExtension',
                ['argString'].toArray(),
                'scriptInterpreter',
                null,
                true,
                _,
                true,
                null
            ) >> Mock(NodeStepResult) {
                isSuccess() >> true
            }
        where:
            filePath                  | contextData                | result
            'some/path'               | [:]                        | 'some/path'
            'some/path${option.key1}' | [:]                        | 'some/path'
            'some/path${option.key1}' | [option: [key1: '/bingo']] | 'some/path/bingo'
            'some/path${data.key1}'   | [data: [key1: '/bingo']]   | 'some/path/bingo'
    }

}
