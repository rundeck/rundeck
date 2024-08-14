package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFilesystemFramework
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.DataContext
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.impl.common.FileCopierUtil
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import spock.lang.Specification

class ScriptURLNodeStepExecutorSpec extends Specification {
    def "executeScriptURL"() {
        given:
            def tmpbase = File.createTempDir("ScriptURLNodeStepExecutorSpec")
            def tmpFile = File.createTempFile("download", ".tmp", tmpbase)

            def context = Mock(PluginStepContext) {
                _*getFramework() >> Mock(Framework) {
                    getFilesystemFramework() >> Mock(IFilesystemFramework) {
                        getBaseDir() >> tmpbase
                    }
                }
                _ * getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    _ * getPropertyLookup() >> Mock(PropertyLookup) {

                    }
                }
                _ * getExecutionContext() >> Mock(ExecutionContext)
                _ * getDataContextObject() >> new BaseDataContext([:])
            }
            def node = Mock(INodeEntry)
            def scriptURLNodeStepExecutor = new ScriptURLNodeStepExecutor(
                context,
                "scriptInterpreter",
                false,
                "fileExtension",
                "argString",
                'http://example.com',
                true,
                Mock(FileCopierUtil.ContentModifier)
            )
            scriptURLNodeStepExecutor.scriptUtils = Mock(ScriptFileNodeStepUtils)
            scriptURLNodeStepExecutor.downloader = Mock(ScriptURLNodeStepExecutor.URLDownloader)
        when:
            scriptURLNodeStepExecutor.executeScriptURL(node, null)
        then:
            1 * scriptURLNodeStepExecutor.downloader.downloadURLToTempFile('http://example.com', _,context,node) >> tmpFile
            1 * scriptURLNodeStepExecutor.scriptUtils.executeScriptFile(
                _,
                node,
                null,
                tmpFile.absolutePath,
                null,
                'fileExtension',
                ['argString'].toArray(),
                'scriptInterpreter',
                null,
                false,
                _,
                true,
                !null
            ) >> Mock(NodeStepResult) {
                isSuccess() >> true
            }
            0 * scriptURLNodeStepExecutor.scriptUtils.executeScriptFile(*_)
    }
}
