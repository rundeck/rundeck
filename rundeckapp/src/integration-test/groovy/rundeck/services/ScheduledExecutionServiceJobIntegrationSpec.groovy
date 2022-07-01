package rundeck.services

import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.schedule.SchedulesManager
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.server.plugins.BaseSpringPluginRegistryComponent
import com.dtolabs.rundeck.server.plugins.RundeckDynamicSpringPluginRegistryComponent
import grails.spring.BeanBuilder
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.*
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import rundeck.CommandExec
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Shared
import spock.lang.Specification

import javax.security.auth.Subject

@Integration
@Rollback
class ScheduledExecutionServiceJobIntegrationSpec extends Specification {
    NotificationService notificationService
    StorageService storageService
    PluginService pluginService
    RundeckDynamicSpringPluginRegistryComponent rundeckDynamicSpringPluginRegistryComponent

    def grailsApplication

    @Shared
    ScheduledExecutionService service = new ScheduledExecutionService()

    def setup() {
        def builder = new BeanBuilder(grailsApplication.mainContext)
        builder.beans {
            dummyNotificationPlugin(DummyNotificationPlugin){
            }
        }

        def context = builder.createApplicationContext()
        rundeckDynamicSpringPluginRegistryComponent.registerDynamicPluginBean(ServiceNameConstants.Notification, DummyNotificationPlugin.BEAN_NAME, context)

        ScheduledExecution job = new ScheduledExecution(createJobParams())
        job.addToOptions(new Option(name: 'optvals', optionValuesPluginType: 'test', required: true, enforced: false))
        job.save(flush: true, failOnError: true)

    }

    def cleanup() {
        def scheduledExecutions = ScheduledExecution.getAll()
        def jobDef = createJobParams()
        def job = scheduledExecutions.find{it-> it.jobName == jobDef.get("jobName") && it.groupPath == jobDef.get("groupPath")}
        job.delete(flush: true, failOnError: true)
    }

    private Map createJobParams(Map overrides=[:]){
        [
                jobName: 'notification plugin dynamic properties',
                project: 'AProject',
                groupPath: 'integration/test',
                description: 'handle dynamic properties issue',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy'])]),
                serverNodeUUID: null,
                scheduled: true
        ]+overrides
    }

    Decision createDecision(String action, boolean isAuthorized) {
        return new Decision() {
            @Override
            boolean isAuthorized() {
                return isAuthorized
            }

            @Override
            Explanation explain() {
                return null
            }

            @Override
            long evaluationDuration() {
                return 0
            }

            @Override
            Map<String, String> getResource() {
                return null
            }

            @Override
            String getAction() {
                return action
            }

            @Override
            Set<Attribute> getEnvironment() {
                return null
            }

            @Override
            Subject getSubject() {
                return null
            }
        }
    }

    void "test prepare create/edit Job"() {

        def project = "AProject"

        def jobDef = createJobParams()
        def scheduledExecutions = ScheduledExecution.getAll()
        def job = scheduledExecutions.find{it-> it.jobName == jobDef.get("jobName") && it.groupPath == jobDef.get("groupPath")}

        given: "params for job and request"
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            evaluate(_,_,_) >> { createDecision("action", true) }
        }

        NodeSetImpl testNodeSetB = new NodeSetImpl()
        testNodeSetB.putNode(new NodeEntryImpl("nodea"))

        def iRundeckProject = Mock(IRundeckProject){
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        def frameworkService  = Mock(FrameworkService){
            filterNodeSet({ NodesSelector selector->
                selector.acceptNode(new NodeEntryImpl("nodea")) &&
                        selector.acceptNode(new NodeEntryImpl("nodec xyz")) &&
                        !selector.acceptNode(new NodeEntryImpl("nodeb"))

            },_)>>testNodeSetB
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
                getFrameworkProjectMgr()>> Mock(ProjectManager) {
                    existsFrameworkProject(project) >> true
                    getFrameworkProject(_) >> iRundeckProject
                }
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
            getProjectGlobals(_) >> [:]
        }

        KeyStorageTree storageTree = storageService.storageTreeWithContext(auth)

        def services = Mock(Services){
            getService(KeyStorageTree)>>storageTree
        }

        notificationService.frameworkService = frameworkService
        service.frameworkService = frameworkService
        service.pluginService = Mock(PluginService){
            listPlugins() >> []
        }
        service.jobSchedulesService = Mock(SchedulesManager){
        }

        service.rundeckAuthorizedServicesProvider = Mock(AuthorizedServicesProvider){
            getServicesWith(_)>>services
        }

        service.notificationService = notificationService
        service.orchestratorPluginService=Mock(OrchestratorPluginService)
        service.executionLifecyclePluginService = Mock(ExecutionLifecyclePluginService)
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager)
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeProjectJobAll(_, _, ['update'], _) >> true
        }
        service.configurationService=Mock(ConfigurationService)
        when: "get plugins with dynamic props"

        Map params = [id: job.id, project: project]

        def model = service.prepareCreateEditJob(params, job, "update", auth)

        then: "model is correct"

        model.scheduledExecution != null
        model.scheduledExecution.options != null
        model.scheduledExecution.workflow != null
        model.notificationPluginsDynamicProperties !=null
        model.notificationPluginsDynamicProperties.size() == 3
    }
}