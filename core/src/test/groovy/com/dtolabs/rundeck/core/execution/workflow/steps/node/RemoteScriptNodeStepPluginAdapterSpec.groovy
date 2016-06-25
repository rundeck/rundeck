package com.dtolabs.rundeck.core.execution.workflow.steps.node

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.step.FileExtensionGeneratedScript
import com.dtolabs.rundeck.plugins.step.GeneratedScript
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 5/31/16.
 */
class RemoteScriptNodeStepPluginAdapterSpec extends Specification {
    public static final String PROJECT_NAME = 'RemoteScriptNodeStepPluginAdapterSpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def cleanup() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    def "basic command"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
        }
        def node = new NodeEntryImpl('node')
        def script = Mock(FileExtensionGeneratedScript)

        when:
        def result = RemoteScriptNodeStepPluginAdapter.executeRemoteScript(context, node, script, 'test', '123')

        then:
        _ * script.getCommand() >> ['a', 'cmd'].toArray()
        1 * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * executeCommand(_, _, node)
        }

    }

    def "basic script"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def script = Mock(FileExtensionGeneratedScript) {
            getArgs() >> ['someargs'].toArray()
        }

        when:
        def result = RemoteScriptNodeStepPluginAdapter.executeRemoteScript(context, node, script, 'test', '123')

        then:
        _ * script.getScript() >> 'a script'

        _ * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * fileCopyScriptContent(_, _, node, _) >> { args -> args[3] }
            3 * executeCommand(_, _, node) >> Mock(NodeExecutorResult) {
                isSuccess() >> true
            }
        }
    }

    @Unroll
    def "basic file based script ext #defaultFileExt"() {
        given:
        File tempFile = File.createTempFile("test", ".script");
        tempFile.deleteOnExit()
        framework.frameworkServices = Mock(IFrameworkServices)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        node.setOsFamily(osFamily)
        def script = Mock(FileBasedGeneratedScript) {
            getArgs() >> ['someargs'].toArray()
            getScriptFile() >> tempFile
        }

        when:
        def result = RemoteScriptNodeStepPluginAdapter.executeRemoteScript(context, node, script, 'test', '123')

        then:

        _ * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * fileCopyFile(_, _, node, { it.endsWith(defaultFileExt) }) >> { args -> args[3] }
            count * executeCommand(_, _, node) >> Mock(NodeExecutorResult) {
                isSuccess() >> true
            }
        }
        where:
        osFamily  | defaultFileExt | count
        'unix'    | '.sh'          | 3
        'windows' | '.bat'         | 2
    }

    def "file based script with file extension"() {
        given:
        File tempFile = File.createTempFile("test", "script");
        tempFile.deleteOnExit()
        framework.frameworkServices = Mock(IFrameworkServices)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def script = Mock(FileBasedGeneratedScript) {
            getArgs() >> ['someargs'].toArray()
            getFileExtension() >> 'myext'
            getScriptFile() >> tempFile
        }

        when:
        def result = RemoteScriptNodeStepPluginAdapter.executeRemoteScript(context, node, script, 'test', '123')

        then:

        _ * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * fileCopyFile(_, _, node, { it.endsWith('.myext') }) >> { args -> args[3] }
            3 * executeCommand(_, _, node) >> Mock(NodeExecutorResult) {
                isSuccess() >> true
            }
        }
    }

    def "script does not define command or script"() {
        given:
        File tempFile = File.createTempFile("test", "script");
        tempFile.deleteOnExit()
        framework.frameworkServices = Mock(IFrameworkServices)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def script = Mock(GeneratedScript) {
            getArgs() >> ['someargs'].toArray()
        }

        when:
        def result = RemoteScriptNodeStepPluginAdapter.executeRemoteScript(context, node, script, 'test', '123')

        then:

        result != null
        !result.isSuccess()
        result.failureReason.toString() == 'ConfigurationFailure'
        result.failureMessage == 'Generated script must have a command or script defined'
    }
}
