package com.dtolabs.rundeck.core.execution.service

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.impl.local.LocalNodeExecutor
import com.dtolabs.rundeck.core.execution.impl.local.NewLocalNodeExecutor
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import spock.lang.Shared
import spock.lang.Specification

class NodeExecutorServiceSpec extends Specification {
    @Shared NodeExecutorServiceProfile defaultProfile

    def setupSpec() {
        defaultProfile = Mock(NodeExecutorServiceProfile){
            _ * getDefaultLocalProvider() >> LocalNodeExecutor.SERVICE_PROVIDER_TYPE
            _ * getDefaultRemoteProvider() >> 'test-default-remote'
            _ * getLocalRegistry() >> [(LocalNodeExecutor.SERVICE_PROVIDER_TYPE): LocalNodeExecutor]
        }

    }

    def "test use local node executor when it is local node and another isnt specified"() {

        given:

        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        Framework framework =  Mock(Framework){
            isLocalNode(_) >> true
            getProjectManager() >> Mock(ProjectManager){
                loadProjectConfig(_) >> Mock(IRundeckProjectConfig){
                    getProperty(_) >> null
                }
            }
        }

        NodeExecutorService service = new NodeExecutorService(framework, defaultProfile)


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        NodeExecutor provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof LocalNodeExecutor

    }

    def "test use dummy node executor when it is remote node and executor is not specified as something else"() {

        given:

        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        Framework framework =  Mock(Framework){
            isLocalNode(_) >> false
            getProjectManager() >> Mock(ProjectManager){
                loadProjectConfig(_) >> Mock(IRundeckProjectConfig)
            }
        }
        NodeExecutorService service = Spy(new NodeExecutorService(framework, defaultProfile)){
            providerOfType('test-default-remote') >> new DummyNodeExecutor()
        }


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        NodeExecutor provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof DummyNodeExecutor

    }

    def "test use dummy node executor when it is local node and executor is specified as something else"() {

        given:

        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        test1.setAttributes(new HashMap<String, String>());
        test1.getAttributes().put(NodeExecutorService.LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE, "testProvider")
        Framework framework =  Mock(Framework){
            isLocalNode(_) >> true
            getProjectManager() >> Mock(ProjectManager){
                loadProjectConfig(_) >> Mock(IRundeckProjectConfig)
            }
        }
        NodeExecutorService service = Spy(new NodeExecutorService(framework, defaultProfile)){
            providerOfType("testProvider") >> new DummyNodeExecutor()
        }


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        NodeExecutor provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof DummyNodeExecutor

    }

    def "test use local node executor when it is not local node and executor is specified as local"() {

        given:

        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        test1.setAttributes(new HashMap<String, String>())
        test1.getAttributes().put(NodeExecutorService.NODE_SERVICE_SPECIFIER_ATTRIBUTE, "local")
        Framework framework =  Mock(Framework){
            isLocalNode(_) >> false
            getProjectManager() >> Mock(ProjectManager){
                loadProjectConfig(_) >> Mock(IRundeckProjectConfig)
            }
        }
        NodeExecutorService service = Spy(new NodeExecutorService(framework, defaultProfile)){
            providerOfType("local") >> new LocalNodeExecutor(framework)
        }


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        NodeExecutor provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof LocalNodeExecutor

    }
    def "get provider name for node"(){
        given:

            final NodeEntryImpl test1 = new NodeEntryImpl("test1");
            test1.setAttributes(new HashMap<String, String>())
            test1.getAttributes().put(NodeExecutorService.NODE_SERVICE_SPECIFIER_ATTRIBUTE, "local")
            Framework framework = Mock(Framework) {
                isLocalNode(_) >> false
                getProjectManager() >> Mock(ProjectManager) {
                    loadProjectConfig(_) >> Mock(IRundeckProjectConfig)
                }
            }
            def profile = Mock(NodeExecutorServiceProfile){
                _ * getDefaultLocalProvider() >> 'test-default-local'
                _ * getDefaultRemoteProvider() >> 'test-default-remote'
                _ * getLocalRegistry() >> [(LocalNodeExecutor.SERVICE_PROVIDER_TYPE): LocalNodeExecutor]
            }
            NodeExecutorService service = Spy(new NodeExecutorService(framework, profile)) {
                providerOfType("local") >> new LocalNodeExecutor(framework)
            }
            def config = Mock(IRundeckProjectConfig){
                _*getProperty('service.NodeExecutor.default.provider') >> pDefaultRemote
                _*getProperty('service.NodeExecutor.default.local.provider') >> pDefaultLocal
            }

        when:
            def result = service.getProviderNameForNode(isLocal,config)
        then:
            result == expect
        where:
            isLocal |  pDefaultLocal   | pDefaultRemote | expect
            false   |  null            | null           | 'test-default-remote'
            true    |  null            | null           | 'test-default-local'
            false   |  null            | 'remote'       | 'remote'
            true    |  'project-local' | null           | 'project-local'
    }

}