/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.execution.workflow.steps.node

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
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
        def adapter = new RemoteScriptNodeStepPluginAdapter(null)
        adapter.scriptUtils = Mock(ScriptFileNodeStepUtils)

        when:
        def result = adapter.executeRemoteScript(context, node, script, 'test', '123')

        then:
        _ * script.getCommand() >> ['a', 'cmd'].toArray()
        1 * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * executeCommand(_, {
                it instanceof ExecArgList && ((ExecArgList) it).asFlatStringList() == ['a', 'cmd']
            }, node
            )
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
        def adapter = new RemoteScriptNodeStepPluginAdapter(null)
        adapter.scriptUtils = Mock(ScriptFileNodeStepUtils)


        when:
        def result = adapter.executeRemoteScript(context, node, script, 'test', '123')

        then:
        _ * script.getScript() >> 'a script'
        1 * adapter.scriptUtils.executeRemoteScript(context, framework, node, ['someargs'].toArray(), _) >>
                Mock(NodeStepResult) {
                    isSuccess() >> true
                }
        _ * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * fileCopyScriptContent(_, _, node, _) >> { args -> args[3] }

        }
    }

    @Unroll
    def "basic file based script various osfamily"() {
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
        def adapter = new RemoteScriptNodeStepPluginAdapter(null)
        adapter.scriptUtils = Mock(ScriptFileNodeStepUtils)

        when:
        def result = adapter.executeRemoteScript(context, node, script, 'test', '123')

        then:

        1 * adapter.scriptUtils.executeScriptFile(
                context,
                node,
                null,
                tempFile.getAbsolutePath(),
                null,
                null,
                ['someargs'].toArray(),
                null,
                false,
                _,
                true
        ) >>
                Mock(NodeStepResult) {
                    isSuccess() >> true
                }
        where:
        osFamily  | _
        'unix'    | _
        'windows' | _
    }

    def "file based script with options"() {
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
            getFileExtension() >> fileExt
            getScriptFile() >> tempFile
            isInterpreterArgsQuoted() >> quoted
            getScriptInterpreter() >> scriptinterpreter
        }
        def adapter = new RemoteScriptNodeStepPluginAdapter(null)
        adapter.scriptUtils = Mock(ScriptFileNodeStepUtils)

        when:
        def result = adapter.executeRemoteScript(context, node, script, 'test', '123')

        then:
        1 * adapter.scriptUtils.executeScriptFile(
                context,
                node,
                null,
                tempFile.getAbsolutePath(),
                null,
                fileExt,
                args.toArray(),
                scriptinterpreter,
                quoted,
                _,
                true
        ) >>
                Mock(NodeStepResult) {
                    isSuccess() >> true
                }
        where:
        fileExt | args         | scriptinterpreter | quoted
        'myext' | ['someargs'] | null              | false
        null    | ['someargs'] | '/bin/bash'       | false
        null    | ['someargs'] | '/bin/bash'       | true
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
        def result = new RemoteScriptNodeStepPluginAdapter(null).executeRemoteScript(
                context,
                node,
                script,
                'test',
                '123'
        )

        then:

        result != null
        !result.isSuccess()
        result.failureReason.toString() == 'ConfigurationFailure'
        result.failureMessage == 'Generated script must have a command or script defined'
    }
}
