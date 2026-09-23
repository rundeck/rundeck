package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ConfiguredStepExecutionItem
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.DataOutput
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginOutput
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

    def "captures resolved @PluginOutput property value into the step output context"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: [:]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        def outputContext = new DataOutput(ContextView.step(3))
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getOutputContext() >> outputContext
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 3
        }
        def plugin = Mock(StepPlugin)
        def wrap = new Test4Plugin(impl: plugin)
        def adapter = new StepPluginAdapter(wrap)
        def config = [environmentName: 'production']
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
        wrap.environmentName == 'production'
        outputContext.getSharedContext().getData(ContextView.step(3)).getData() == [data: [environmentName: 'production']]
    }

    def "does not write to output context when property has no @PluginOutput"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: [:]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        def outputContext = new DataOutput(ContextView.step(3))
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getOutputContext() >> outputContext
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 3
        }
        def plugin = Mock(StepPlugin)
        def wrap = new Test2Plugin(impl: plugin)
        def adapter = new StepPluginAdapter(wrap)
        def config = [test: 'somevalue']
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: config,
                label: 'a label'
        )
        when:
        def result = adapter.executeWorkflowStep(context, item)

        then:
        result.isSuccess()
        outputContext.getSharedContext().getData(ContextView.step(3)) == null
    }

    def "captures a computed output-only @PluginOutput value after executeStep runs"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: [:]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        def outputContext = new DataOutput(ContextView.step(4))
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getOutputContext() >> outputContext
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 4
        }
        def plugin = Mock(StepPlugin)
        def wrap = new Test5Plugin(impl: plugin)
        def adapter = new StepPluginAdapter(wrap)
        def config = [environmentName: 'production']
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
        wrap.environmentName == 'production'
        wrap.outputResult == 'PROD_READY'
        // only outputResult (the @PluginOutput-only field) is captured; environmentName has no
        // @PluginOutput and is therefore not exposed
        outputContext.getSharedContext().getData(ContextView.step(4)).getData() == [data: [outputResult: 'PROD_READY']]
    }

    def "does not capture output when the step execution throws"() {
        given:
        framework.frameworkServices = Mock(IFrameworkServices)
        def optionContext = new BaseDataContext([option: [:]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), optionContext)
        def outputContext = new DataOutput(ContextView.step(4))
        StepExecutionContext context = Mock(StepExecutionContext) {
            getFramework() >> framework
            getDataContext() >> optionContext
            getSharedDataContext() >> shared
            getOutputContext() >> outputContext
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 4
            getExecutionListener() >> Mock(ExecutionListener)
        }
        def wrap = new Test6Plugin()
        def adapter = new StepPluginAdapter(wrap)
        def item = new TestExecItem(
                type: 'atype',
                stepConfiguration: [:],
                label: 'a label'
        )
        when:
        def result = adapter.executeWorkflowStep(context, item)

        then:
        !result.isSuccess()
        // the field was set to a value before the plugin threw, but since the step failed,
        // nothing should have been captured into the output context
        wrap.outputResult == 'SET_BEFORE_FAILURE'
        outputContext.getSharedContext().getData(ContextView.step(4)) == null
    }

    @Plugin(name = "test5", service = ServiceNameConstants.WorkflowNodeStep)
    static class Test5Plugin implements StepPlugin {
        StepPlugin impl

        @PluginProperty(title = "Environment Name")
        private String environmentName

        // Output-only: computed inside executeStep(), not settable via job configuration.
        @PluginOutput(name = "outputResult", description = "Computed result exposed for conditional logic")
        private String outputResult

        @Override
        void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
            outputResult = "production".equals(environmentName) ? "PROD_READY" : "NOT_READY"
            impl.executeStep(context, configuration)
        }
    }

    @Plugin(name = "test6", service = ServiceNameConstants.WorkflowNodeStep)
    static class Test6Plugin implements StepPlugin {
        @PluginOutput(name = "outputResult", description = "desc")
        private String outputResult

        @Override
        void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
            outputResult = "SET_BEFORE_FAILURE"
            throw new StepException("boom", StepFailureReason.Unknown)
        }
    }

    @Plugin(name = "test4", service = ServiceNameConstants.WorkflowNodeStep)
    static class Test4Plugin implements StepPlugin {
        StepPlugin impl

        @PluginProperty(title = "Environment Name",
                description = "test",
                defaultValue = "test")
        @PluginOutput(name = "environmentName", description = "Exposed for conditional logic")
        private String environmentName

        @Override
        void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {
            impl.executeStep(context, configuration)
        }
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
