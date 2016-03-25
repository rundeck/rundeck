package com.dtolabs.rundeck.core.execution.dispatch

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.FrameworkSupportService
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.OrchestratorConfig
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.PluginManagerService
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin
import spock.lang.Specification

/**
 * Created by greg on 2/15/16.
 */
class OrchestratorNodeDispatcherSpec extends Specification {
    public static final String PROJECT_NAME = 'OrchestratorNodeDispatcherSpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def teardown() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    @Plugin(name = 'test', service = 'Orchestrator')
    class TestPlugin1 implements OrchestratorPlugin {
        @PluginProperty()
        int intTest

        @PluginProperty
        String stringTest

        @Override
        Orchestrator createOrchestrator(final StepExecutionContext context, final Collection<INodeEntry> nodes) {
            return null
        }
    }

    interface TestProviderLoader extends FrameworkSupportService, ServiceProviderLoader {

    }

    def "expand property refs in config"() {
        given:
        Map<String, Map<String, String>> dataContext = [:] + idataContext
        OrchestratorService orchestratorService = OrchestratorService.getInstanceForFramework(framework)
        OrchestratorConfig oconfig = new OrchestratorConfig('test', inputConfig)
        OrchestratorPlugin plugin = new TestPlugin1()
        TestProviderLoader providerLoader = Mock(TestProviderLoader) {
            1 * loadProvider(orchestratorService, 'test') >> plugin
        }
        INodeSet nodeSet = new NodeSetImpl()
        IFrameworkServices frameworkServices = Mock(IFrameworkServices) {
            getOrchestratorService() >> orchestratorService
            getPluginManager() >> providerLoader
            0 * _(*_)
        }

        framework.setFrameworkServices(frameworkServices)


        OrchestratorNodeDispatcher dispatcher = new OrchestratorNodeDispatcher(framework)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getOrchestrator() >> oconfig
            getNodes() >> nodeSet
            getExecutionListener() >> Mock(ExecutionListener)
            getThreadCount() >> 1
            getDataContext() >> dataContext
        }

        NodeStepExecutionItem item = Mock(NodeStepExecutionItem)

        when:
        def result = dispatcher.dispatch(context, item, null)


        then:
        result.success
        1 == plugin.intTest
        'beans' == plugin.stringTest

        where:
        inputConfig                                         | idataContext
        [intTest: '1', stringTest: 'beans']                 | [:]
        [intTest: '${option.intTest}', stringTest: 'beans'] | [option: [intTest: '1']]
        [intTest: '1', stringTest: '${option.blah}']        | [option: [blah: 'beans']]
    }
    def "don't expand property refs for empty config"() {
        given:
        Map<String, Map<String, String>> dataContext = [:] + idataContext
        OrchestratorService orchestratorService = OrchestratorService.getInstanceForFramework(framework)
        OrchestratorConfig oconfig = new OrchestratorConfig('test', inputConfig)
        OrchestratorPlugin plugin = new TestPlugin1()
        TestProviderLoader providerLoader = Mock(TestProviderLoader) {
            1 * loadProvider(orchestratorService, 'test') >> plugin
        }
        INodeSet nodeSet = new NodeSetImpl()
        IFrameworkServices frameworkServices = Mock(IFrameworkServices) {
            getOrchestratorService() >> orchestratorService
            getPluginManager() >> providerLoader
            0 * _(*_)
        }

        framework.setFrameworkServices(frameworkServices)


        OrchestratorNodeDispatcher dispatcher = new OrchestratorNodeDispatcher(framework)
        StepExecutionContext context = Mock(StepExecutionContext) {
            getOrchestrator() >> oconfig
            getNodes() >> nodeSet
            getExecutionListener() >> Mock(ExecutionListener)
            getThreadCount() >> 1
            getDataContext() >> dataContext
        }

        NodeStepExecutionItem item = Mock(NodeStepExecutionItem)

        when:
        def result = dispatcher.dispatch(context, item, null)


        then:
        result.success
        0 == plugin.intTest
        null == plugin.stringTest

        where:
        inputConfig | idataContext
        null        | [:]

    }
}
