/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.ServiceNameConstants
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
}
