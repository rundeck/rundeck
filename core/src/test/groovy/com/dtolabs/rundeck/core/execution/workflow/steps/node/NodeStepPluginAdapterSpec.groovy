/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.common.*
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.OptsUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginMetadata
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.core.execution.ExecCommand
import org.rundeck.core.execution.ScriptCommand
import org.rundeck.core.execution.ScriptFileCommand
import spock.lang.Specification

/**
 * @author greg
 * @since 6/27/17
 */
class NodeStepPluginAdapterSpec extends Specification {
    public static final String PROJECT_NAME = 'NodeStepPluginAdapterSpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def cleanup() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    static class TestPlugin implements NodeStepPlugin, Describable {
        NodeStepPlugin impl
        Description description

        @Override
        void executeNodeStep(
                final PluginStepContext context,
                final Map<String, Object> configuration,
                final INodeEntry entry
        ) throws NodeStepException
        {
            impl.executeNodeStep(context, configuration, entry)
        }
    }

    @Plugin(name = "test2", service = ServiceNameConstants.WorkflowNodeStep)
    static class Test2Plugin implements NodeStepPlugin {
        NodeStepPlugin impl

        @PluginProperty(title = "test",
                description = "test",
                defaultValue = "test")
        private String test

        @Override
        void executeNodeStep(
                final PluginStepContext context,
                final Map<String, Object> configuration,
                final INodeEntry entry
        ) throws NodeStepException
        {
            impl.executeNodeStep(context, configuration, entry)
        }
    }
    @Plugin(name = "test2", service = ServiceNameConstants.WorkflowNodeStep)
    @PluginMetadata(key = "NodeStepPluginAdapter.DeprecatedConfigurationMode", value = "skip")
    static class TestSkipDeprecatedModePlugin implements NodeStepPlugin {
        NodeStepPlugin impl

        @PluginProperty(title = "test",
                description = "test",
                defaultValue = "test")
        private String test

        @Override
        void executeNodeStep(
                final PluginStepContext context,
                final Map<String, Object> configuration,
                final INodeEntry entry
        ) throws NodeStepException
        {
            impl.executeNodeStep(context, configuration, entry)
        }
    }

    @Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = EXEC_COMMAND_TYPE)
    static class TestCommandPlugin implements ExecCommand, NodeStepPlugin {
        NodeStepPlugin impl
        @PluginProperty(title = "Command",
                description = "Enter the shell command, e.g.: echo this is a test",
                required = true)
        String adhocRemoteString;
        String[] adhocRemoteStringResult;

        @Override
        void executeNodeStep(
                final PluginStepContext context,
                final Map<String, Object> configuration,
                final INodeEntry entry
        ) throws NodeStepException
        {
            def arr = OptsUtil.burst(adhocRemoteString)

            def result = SharedDataContextUtils.replaceDataReferencesInObject(
                    arr as ArrayList,
                    ContextView.node(entry.getNodename()),
                    ContextView::nodeStep,
                    null,
                    context.getExecutionContext().getSharedDataContext(),
                    false,
                    true
            ) as String[]

            configuration.put('result', result)

            impl.executeNodeStep(context, configuration, entry)
        }
    }

    @Plugin(name = "test2", service = ServiceNameConstants.WorkflowNodeStep)
    static class Test3Plugin implements NodeStepPlugin {
        NodeStepPlugin impl

        @PluginProperty(title = "customFieldsTest",
                description = "test")
        @RenderingOption(
                key = StringRenderingConstants.DISPLAY_TYPE_KEY,
                value = "DYNAMIC_FORM"
        )
        private String customFieldsTest

        @Override
        void executeNodeStep(
                final PluginStepContext context,
                final Map<String, Object> configuration,
                final INodeEntry entry
        ) throws NodeStepException
        {
            impl.executeNodeStep(context, configuration, entry)
        }
    }

    static class TestExecItem implements NodeStepExecutionItem, ConfiguredStepExecutionItem {
        String type

        Map<String, Object> stepConfiguration

        String nodeStepType

        String label
    }

    def "expand config vars uses blank for unexpanded"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new TestPlugin(
                impl: plugin,
                description: DescriptionBuilder.builder()
                                               .name('nodetype')
                                               .property(PropertyBuilder.builder().string('a').build())
                                               .property(PropertyBuilder.builder().string('c').build())
                                               .property(PropertyBuilder.builder().string('d').build())
                                               .build()
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: inputconfig,
                nodeStepType: 'nodetype',
                label: 'a label'
        )
        when:
        def result = adapter.executeNodeStep(context, item, node)

        then:
        1 * plugin.executeNodeStep(!null as PluginStepContext, expect, node)
        result.isSuccess()

        where:

        inputconfig = [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"']
        data | expect
        [:] | [a: 'b', c: '', d: 'something "xyzqws"']
        [c: 'q'] | [a: 'b', c: 'q', d: 'something "xyzqws"']
        [p: 'Q'] | [a: 'b', c: '', d: 'something "xyzQqws"']
        [c: 'Z', p: 'Q'] | [a: 'b', c: 'Z', d: 'something "xyzQqws"']
    }

    def "expand config vars for command plugins"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new TestCommandPlugin(
                impl: plugin,
                adhocRemoteString: inputconfig['adhocRemoteString']
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: inputconfig,
                nodeStepType: nodeStepType,
                label: 'a label'
        )
        when:
        def result = adapter.executeNodeStep(context, item, node)

        then:
        1 * plugin.executeNodeStep(!null as PluginStepContext, expect, node)
        result.isSuccess()

        where:

        inputconfig = [adhocRemoteString: 'something ${option.p}']

        data           | expect                                  | nodeStepType
        ["p": "1 2 3"] | [result: ['something', "1 2 3"]]        | ExecCommand.EXEC_COMMAND_TYPE
    }

    def "Not expand config vars for script plugins"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new TestPlugin(
                impl: plugin,
                description: DescriptionBuilder.builder()
                        .name('nodetype')
                        .property(PropertyBuilder.builder().string('a').build())
                        .property(PropertyBuilder.builder().string('c').build())
                        .property(PropertyBuilder.builder().string('d').build())
                        .build()
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: inputconfig,
                nodeStepType: nodeStepType,
                label: 'a label'
        )
        when:
        def result = adapter.executeNodeStep(context, item, node)

        then:
        1 * plugin.executeNodeStep(!null as PluginStepContext, expect, node)
        result.isSuccess()

        where:

        inputconfig = [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"']
        data | expect                                                        | nodeStepType
        [:] | [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"'] | ScriptCommand.SCRIPT_COMMAND_TYPE
        [:] | [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"'] | ScriptFileCommand.SCRIPT_FILE_COMMAND_TYPE
        [:] | [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"'] | ExecCommand.EXEC_COMMAND_TYPE
        [:] | [a: 'b', c: '', d: 'something "xyzqws"']                       | 'someothertype'
    }

    def "create config with custom fields"(){
        given: 'a plugin with custom fields'

        def json = new ObjectMapper()
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new Test3Plugin(
                impl: plugin
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def customFieldsData = [
                [
                        key:'akey',
                        value:'plain',
                        desc:'Adesc'
                ],
                [
                        key:'bkey',
                        value:'some ${option.b} value',
                        label:'Blabel'
                ],
                [
                        key:'ckey',
                        value:'${option.c}',
                        desc:'Cdesc',
                        label:'Clabel'
                ],
                [
                        key:'dkey',
                        value:'${option.d}'
                ],
        ]
        def jsonFieldData=json.writeValueAsString(customFieldsData)
        def config = [customFieldsTest: jsonFieldData]
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: config,
                label: 'a label'
        )
        when: 'create config is called'

        def result = adapter.createConfig(context, item, node)

        then: 'the custom field values inside the json are correct'

        result['customFieldsTest'] instanceof String
        List resData = json.readValue(result['customFieldsTest'].toString(), List)
        resData[0] == [key: 'akey', value: expect['akey'], desc: 'Adesc']
        resData[1] == [key: 'bkey', value: expect['bkey'], label: 'Blabel']
        resData[2] == [key: 'ckey', value: expect['ckey'], desc: 'Cdesc', label: 'Clabel']
        resData[3] == [key: 'dkey', value: expect['dkey']]

        where:
        data | expect
        [:] | [akey: 'plain', bkey: 'some  value', ckey: '',dkey:'']
        [b:'Bval',c:'Cval',d:'Dval\"with quotes\"'] | [akey: 'plain', bkey: 'some Bval value', ckey: 'Cval',dkey:'Dval\"with quotes\"']
    }
    def "expand config vars uses blank from property config"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new TestPlugin(
                impl: plugin,
                description: DescriptionBuilder.builder()
                                               .name('nodetype')
                                               .property(PropertyBuilder.builder().string('a').build())
                                               .property(PropertyBuilder.builder().string('c').blankIfUnexpandable(false).build())
                                               .property(PropertyBuilder.builder().string('d').blankIfUnexpandable(false).build())
                                               .build()
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: inputconfig,
                nodeStepType: 'nodetype',
                label: 'a label'
        )
        when:
        def result = adapter.executeNodeStep(context, item, node)

        then:
        1 * plugin.executeNodeStep(!null as PluginStepContext, expect, node)
        result.isSuccess()

        where:

        inputconfig = [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"']
        data | expect
        [:] | [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"']
        [c: 'q'] | [a: 'b', c: 'q', d: 'something "xyz${option.p}qws"']
        [p: 'Q'] | [a: 'b', c: '${option.c}', d: 'something "xyzQqws"']
        [c: 'Z', p: 'Q'] | [a: 'b', c: 'Z', d: 'something "xyzQqws"']

    }

    def "check expanding values according to blankIfUnexpanded field"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new TestPlugin(
                impl: plugin,
                description: DescriptionBuilder.builder()
                        .name('nodetype')
                        .property(PropertyBuilder.builder().string('a').build())
                        .property(PropertyBuilder.builder().string('c').blankIfUnexpandable(false).build())
                        .build()
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: inputconfig,
                nodeStepType: 'nodetype',
                label: 'a label'
        )

        when:
        def result = adapter.createConfig(context, item, node)

        then:
        result == expect

        where:
        inputconfig = [a: 'b/${config.x}', c: 'a/${config.a}']
        data | expect
        [:] | [a: 'b/', c: 'a/${config.a}']

    }


    def "get plugin variables using PluginProperty"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: [:]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new Test2Plugin(
                impl: plugin
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def config = [test: '123456']
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: config,
                nodeStepType: 'nodetype',
                label: 'a label'
        )
        when:
        def result = adapter.executeNodeStep(context, item, node)

        then:
        1 * plugin.executeNodeStep(!null as PluginStepContext, [:], node)
        result.isSuccess()
        wrap.test == "123456"
    }


    def "skip deprecated configuration mode"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: [:]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new TestSkipDeprecatedModePlugin(
                impl: plugin,
                test: '7890'
        )
        def adapter = new NodeStepPluginAdapter(wrap)
        def config = [test: '123456']
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: config,
                nodeStepType: 'nodetype',
                label: 'a label'
        )
        when:
        def result = adapter.executeNodeStep(context, item, node)

        then: "the plugin would not be re-configured, and the configuration map would be null"
        1 * plugin.executeNodeStep(!null as PluginStepContext, null, node)
        result.isSuccess()
        wrap.test == '7890'
    }


}
