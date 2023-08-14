package com.dtolabs.rundeck.core.execution.service

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProjectConfig
import com.dtolabs.rundeck.core.common.IServicesRegistration
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.impl.local.LocalFileCopier
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import org.mockito.Mockito
import spock.lang.Specification

class FileCopierServiceSpec extends Specification {
    def "test use local file copier when it is local node and another isnt specified"() {

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

        FileCopierService service = FileCopierService.getInstanceForFramework(framework,framework)

        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        FileCopier provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof LocalFileCopier

    }

    def "test use dummy file copier when it is remote node and file copier is not specified as something else"() {

        given:

        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        Framework framework =  Mock(Framework){
            isLocalNode(_) >> false
            getProjectManager() >> Mock(ProjectManager){
                loadProjectConfig(_) >> Mock(IRundeckProjectConfig)
            }
        }
        FileCopierService service = Spy(FileCopierService.getInstanceForFramework(framework,framework)){
            providerOfType(FileCopierService.DEFAULT_REMOTE_PROVIDER) >> new DummyFileCopier()
        }


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        FileCopier provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof DummyFileCopier

    }

    def "test use local file copier when it is local node and executor is specified as something else"() {

        given:

        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        test1.setAttributes(new HashMap<String, String>());
        test1.getAttributes().put(FileCopierService.LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE, "testProvider")
        Framework framework =  Mock(Framework){
            isLocalNode(_) >> true
            getProjectManager() >> Mock(ProjectManager){
                loadProjectConfig(_) >> Mock(IRundeckProjectConfig)
            }
        }
        FileCopierService service = Spy(FileCopierService.getInstanceForFramework(framework,framework)){
            providerOfType("testProvider") >> new DummyFileCopier()
        }


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        FileCopier provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof DummyFileCopier

    }

    def "test use local file copier when it is not local node and executor is specified as something else"() {

        given:

        final NodeEntryImpl test1 = new NodeEntryImpl("test1");
        test1.setAttributes(new HashMap<String, String>())
        test1.getAttributes().put(FileCopierService.REMOTE_NODE_SERVICE_SPECIFIER_ATTRIBUTE, "local")
        Framework framework =  Mock(Framework){
            isLocalNode(_) >> false
            getProjectManager() >> Mock(ProjectManager){
                loadProjectConfig(_) >> Mock(IRundeckProjectConfig)
            }
        }
        FileCopierService service = Spy(FileCopierService.getInstanceForFramework(framework,framework)){
            providerOfType("local") >> new LocalFileCopier(framework)
        }


        final StepExecutionContext context = ExecutionContextImpl.builder()
                .frameworkProject("testProject")
                .framework(framework)
                .user("blah")
                .threadCount(1)
                .build();

        when:
        FileCopier provider = service.getProviderForNodeAndProject(test1, context);

        then:
        provider instanceof LocalFileCopier

    }
}
