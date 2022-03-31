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
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.data.DataContext
import com.dtolabs.rundeck.core.data.MultiDataContext
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.ExecArgList
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepUtils
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.step.FileExtensionGeneratedScript
import com.dtolabs.rundeck.plugins.step.GeneratedScript
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.RemoteScriptNodeStepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
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

    static class TestExecItem implements NodeStepExecutionItem, ConfiguredStepExecutionItem {
        Map<String, Object> stepConfiguration
        String type
        String label
        String nodeStepType
    }

    static class TestPlugin implements Describable, RemoteScriptNodeStepPlugin {


        //Description description =
        @Override
        Description getDescription() {
            def builder = DescriptionBuilder.builder().name("testPlugin")
            pluginPropNames.each {
                builder.stringProperty(it, null, false, null, null)
            }
            builder.build()
        }
        List<String> pluginPropNames
        GeneratedScript script
        String version = "1.0"

        @Override
        GeneratedScript generateScript(PluginStepContext context, Map<String, Object> configuration, INodeEntry entry) throws NodeStepException {
            script
        }

        @Override
        boolean hasAdditionalConfigVarGroupName() {
            if(version=="2.0"){
                true
            }else{
                false
            }
        }

        void setVersion(String version){
            this.version=version
        }


    }


    def "step config does not replace missing configs with blank"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        DataContext dataContext = DataContextUtils.context()
        WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()

        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
            getDataContextObject() >> dataContext
            getSharedDataContext() >> sharedContext
        }
        def node = new NodeEntryImpl('node')
        def script = Mock(FileExtensionGeneratedScript) {
            getArgs() >> ['someargs'].toArray()
        }
        def plugin = new TestPlugin(script: script, pluginPropNames: ['abc', 'def', 'xyz'])
        def adapter = new RemoteScriptNodeStepPluginAdapter(plugin)
        adapter.scriptUtils = Mock(ScriptFileNodeStepUtils)

        def instanceConfig = [
                abc: 'buddy',
                def: 'shampoo/${config.dne}/asdf',
                xyz: 'test/${config.abc}',
        ]
        def item = new TestExecItem(stepConfiguration: instanceConfig)


        when:
        def result = adapter.executeNodeStep(context, item, node)

        then:
        _ * script.getScript() >> 'a script'
        1 * adapter.scriptUtils.executeRemoteScript({ ExecutionContext ctx ->
            ctx.getDataContext().get('config') == instanceConfig
        }, _, node, ['someargs'].toArray(), _) >>
                Mock(NodeStepResult) {
                    isSuccess() >> true
                }
        _ * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * fileCopyScriptContent(_, _, node, _) >> { args -> args[3] }

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

        def plugin = Mock(RemoteScriptNodeStepPlugin)
        def adapter = new RemoteScriptNodeStepPluginAdapter(plugin)
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

    def "nodestep variable names for 2.x plugin"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        DataContext dataContext = DataContextUtils.context()
        WFSharedContext sharedContext = SharedDataContextUtils.sharedContext()

        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getFrameworkProject() >> PROJECT_NAME
            getDataContextObject() >> dataContext
            getSharedDataContext() >> sharedContext
        }
        def node = new NodeEntryImpl('node')
        def script = Mock(FileExtensionGeneratedScript) {
            getArgs() >> ['someargs'].toArray()
        }
        def plugin = new TestPlugin(script: script, pluginPropNames: ['abc', 'def', 'xyz'])
        plugin.setVersion("2.0")
        def adapter = new RemoteScriptNodeStepPluginAdapter(plugin)
        adapter.scriptUtils = Mock(ScriptFileNodeStepUtils)

        def instanceConfig = [
                abc: 'buddy',
                def: 'shampoo/${config.dne}/asdf',
                xyz: 'test/${config.abc}',
        ]
        def item = new TestExecItem(stepConfiguration: instanceConfig)


        when:
        def result = adapter.executeNodeStep(context, item, node)

        then:
        _ * script.getScript() >> 'a script'
        1 * adapter.scriptUtils.executeRemoteScript({ ExecutionContext ctx ->
            ctx.getDataContext().get('nodestep') == instanceConfig
        }, _, node, ['someargs'].toArray(), _) >>
                Mock(NodeStepResult) {
                    isSuccess() >> true
                }
        _ * framework.frameworkServices.getExecutionService() >> Mock(ExecutionService) {
            1 * fileCopyScriptContent(_, _, node, _) >> { args -> args[3] }
        }
    }
}
