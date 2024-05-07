package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

class StepPluginAdapterSpec extends Specification {
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
        def plugin = Mock(StepPlugin)
        def wrap = new Test2Plugin(
                impl: plugin
        )
        def adapter = new StepPluginAdapter(wrap)
        def config = [test: '123456']
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: config,
                label: 'a label'
        )
        when:
        def result = adapter.executeWorkflowStep(context, item)

        then:
        1 * plugin.executeStep(!null as PluginStepContext, [:])
        result.isSuccess()
        wrap.test == "123456"

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
        def plugin = Mock(StepPlugin)
        def wrap = new TestPlugin(
                impl: plugin,
                description: DescriptionBuilder.builder()
                        .name('stepplugin')
                        .property(PropertyBuilder.builder().string('a').build())
                        .property(PropertyBuilder.builder().string('c').build())
                        .property(PropertyBuilder.builder().string('d').build())
                        .build()
        )
        def adapter = new StepPluginAdapter(wrap)
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: inputconfig,
                label: 'a label'
        )
        when:
        def result = adapter.executeWorkflowStep(context, item)

        then:
        1 * plugin.executeStep(!null as PluginStepContext, expect)
        result.isSuccess()

        where:

        inputconfig = [a: 'b', c: '${option.c}', d: 'something "xyz${option.p}qws"']
        data | expect
        [:] | [a: 'b', c: '', d: 'something "xyzqws"']
        [c: 'q'] | [a: 'b', c: 'q', d: 'something "xyzqws"']
        [p: 'Q'] | [a: 'b', c: '', d: 'something "xyzQqws"']
        [c: 'Z', p: 'Q'] | [a: 'b', c: 'Z', d: 'something "xyzQqws"']
    }

    def "create config with custom fields"(){
        given: 'context with a plugin using custom field values'
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
        def plugin = Mock(StepPlugin)
        def wrap = new Test3Plugin(
                impl: plugin
        )
        def adapter = new StepPluginAdapter(wrap)
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
        def result = adapter.createConfig(context, item)

        then: 'customFieldsTest value is expanded with correct values inside the json'
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

    static class TestPlugin implements StepPlugin, Describable {
        StepPlugin impl
        Description description

        @Override
        void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
            impl.executeStep(context, configuration)
        }
    }

    @Plugin(name = "test2", service = ServiceNameConstants.WorkflowNodeStep)
    static class Test2Plugin implements StepPlugin {
        StepPlugin impl

        @PluginProperty(title = "test",
                description = "test",
                defaultValue = "test")
        private String test

        @Override
        void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
            impl.executeStep(context, configuration)
        }
    }

    @Plugin(name = "test3", service = ServiceNameConstants.WorkflowNodeStep)
    static class Test3Plugin implements StepPlugin {
        StepPlugin impl

        @PluginProperty(title = "customFieldsTest",
                description = "test")
        @RenderingOption(
                key = StringRenderingConstants.DISPLAY_TYPE_KEY,
                value = "DYNAMIC_FORM"
        )
        private String customFieldsTest

        @Override
        void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
            impl.executeStep(context, configuration)
        }
    }

    static class TestExecItem implements StepExecutionItem, ConfiguredStepExecutionItem {
        String type

        Map<String, Object> stepConfiguration

        String label
    }
}
