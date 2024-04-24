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
import spock.lang.Specification

class NodeExecutorServiceSpec extends Specification {
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

        NodeExecutorService service = NodeExecutorService.getInstanceForFramework(framework,framework)

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
        NodeExecutorService service = Spy(NodeExecutorService.getInstanceForFramework(framework,framework)){
            providerOfType(NodeExecutorService.DEFAULT_REMOTE_PROVIDER) >> new DummyNodeExecutor()
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
        NodeExecutorService service = Spy(NodeExecutorService.getInstanceForFramework(framework,framework)){
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
        NodeExecutorService service = Spy(NodeExecutorService.getInstanceForFramework(framework,framework)){
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
            NodeExecutorService service = Spy(NodeExecutorService.getInstanceForFramework(framework, framework)) {
                providerOfType("local") >> new LocalNodeExecutor(framework)
            }
            service.setFeatureService(
                Mock(FeatureService) {
                    _ * featurePresent(Features.NEW_LOCAL_NODE_EXECUTOR, false) >> flag
                }
            )
            def config = Mock(IRundeckProjectConfig){
                _*getProperty('service.NodeExecutor.default.provider') >> pDefaultRemote
                _*getProperty('service.NodeExecutor.default.local.provider') >> pDefaultLocal
            }

        when:
            def result = service.getProviderNameForNode(isLocal,config)
        then:
            result == expect
        where:
            isLocal | flag  | pDefaultLocal   | pDefaultRemote | expect
            false   | false | null            | null           | NodeExecutorService.DEFAULT_REMOTE_PROVIDER
            true    | false | null            | null           | NodeExecutorService.DEFAULT_LOCAL_PROVIDER
            true    | true  | null            | null           | NewLocalNodeExecutor.SERVICE_PROVIDER_TYPE
            false   | false | null            | 'remote'       | 'remote'
            true    | false | 'project-local' | null           | 'project-local'
    }

    def "get default local provider with newLocalNodeExecutor feature flag #feature"() {
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
            NodeExecutorService service = Spy(NodeExecutorService.getInstanceForFramework(framework, framework)) {
                providerOfType("local") >> new LocalNodeExecutor(framework)
            }
            service.setFeatureService(
                Mock(FeatureService) {
                    _ * featurePresent(Features.NEW_LOCAL_NODE_EXECUTOR, false) >> feature
                }
            )

        when:
            def result = service.getDefaultLocalProvider()
        then:
            result == expect
        where:
            feature | expect
            false   | LocalNodeExecutor.SERVICE_PROVIDER_TYPE
            true    | NewLocalNodeExecutor.SERVICE_PROVIDER_TYPE
    }
}