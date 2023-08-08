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
import com.dtolabs.rundeck.core.data.DataContext
import com.dtolabs.rundeck.core.data.MultiDataContextImpl
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.RuntimePropertyResolverTest
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
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

    static class TestExecItem implements NodeStepExecutionItem, ConfiguredStepExecutionItem {
        String type

        Map<String, Object> stepConfiguration

        String nodeStepType

        String label

        boolean keepgoingOnSuccess
    }

    def "expand config vars uses blank for unexpanded"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getIFramework() >> Mock(IFramework) {
                getPropertyRetriever() >> Mock(PropertyRetriever)
            }
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

    def "expand config vars uses blank from property config"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getIFramework() >> Mock(IFramework) {
                getPropertyRetriever() >> Mock(PropertyRetriever)
            }
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
            getIFramework() >> Mock(IFramework) {
                getPropertyRetriever() >> Mock(PropertyRetriever)
            }
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
            getIFramework() >> Mock(IFramework) {
                getPropertyRetriever() >> Mock(PropertyRetriever)
            }
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

    static class mapRetriever implements PropertyRetriever {
        private Map<String, String> map;

        mapRetriever(Map<String, String> map) {
            this.map = map;
        }

        @Override
        String getProperty(String name) {
            return map.get(name);
        }
    }

    def "check expanding values according to blankIfUnexpanded field with propertie in true"() {

        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: data])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getIFramework() >> Mock(IFramework) {
                getPropertyRetriever() >> new mapRetriever(['rundeck.plugins.nixy-local-script.blank': isBlank])
            }
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getFrameworkProject() >> PROJECT_NAME
        }
        def node = new NodeEntryImpl('node')
        def plugin = Mock(NodeStepPlugin)
        def wrap = new TestPlugin(
                impl: plugin,
                description: DescriptionBuilder.builder()
                        .name('localhost')
                        .property(PropertyBuilder.builder().string('script').blankIfUnexpandable(false).build())
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
        inputconfig = [script: 'echo \"step1: ${option.myData1}\"\necho \"step2: ${option.myData2}\"\necho \"step3: ${option.myData3}\"']

        isBlank | data | expect
        'false'   | [:] | ['script': 'echo \"step1: ${option.myData1}\"\necho \"step2: ${option.myData2}\"\necho \"step3: ${option.myData3}\"']
        'true'    | ['myData1': 'chickens', 'myData2': ''] | ['script': 'echo \"step1: chickens\"\necho "step2: \"\necho "step3: \"']
    }

}
