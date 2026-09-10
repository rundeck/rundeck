package rundeck.services

import com.dtolabs.rundeck.core.authorization.Attribute
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.Explanation
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.NodesSelector
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.schedule.SchedulesManager
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import grails.spring.BeanBuilder
import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.*
import org.quartz.Scheduler
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.data.providers.GormJobStatsDataProvider
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.rundeck.app.spi.Services
import org.springframework.context.MessageSource
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
    RundeckPluginRegistry rundeckPluginRegistry

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
        rundeckPluginRegistry.registerDynamicPluginBean(DummyNotificationPlugin.BEAN_NAME, context)

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


        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        def iRundeckProject = Mock(IRundeckProject){
            getProperties() >> properties
        }
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
                getPropertyRetriever()>>PropertyLookup.create(properties)
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
        service.pluginService.frameworkService = Mock(FrameworkService){
            getRundeckFramework()>> Mock(IFramework) {
                getPropertyRetriever() >> PropertyResolverFactory.instanceRetriever([:])
            }
        }

        service.jobSchedulesService = Mock(SchedulesManager){
        }

        service.rundeckAuthorizedServicesProvider = Mock(AuthorizedServicesProvider){
            getServicesWith(_)>>services
        }

        service.notificationService = notificationService
        0 * service.notificationService.listNotificationPluginsDynamicProperties(_,_)
        service.orchestratorPluginService=Mock(OrchestratorPluginService)
        service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager)
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeProjectJobAll(_, _, ['update'], _) >> true
        }
        service.configurationService=Mock(ConfigurationService)
        service.fileUploadService=Mock(FileUploadService){
            _*getPluginType()>>'afileuploadplugin'
        }
        when: "get plugins with dynamic props"

        Map params = [id: job.id, project: project]

        def model = service.prepareCreateEditJob(params, job, "update", auth)

        then: "model is correct"

        model.scheduledExecution != null
        model.scheduledExecution.options != null
        model.scheduledExecution.workflow != null
        model.fileUploadPluginType == 'afileuploadplugin'
    }

    /**
     * Mocks the collaborators needed to drive a real _doupdate()/_doupdateJob() call through
     * the actual GORM-backed ScheduledExecution/Option domain, following the same pattern as
     * ScheduledExecutionServiceSpec#setupDoUpdate() (mock-GORM unit spec), so that we can
     * observe real Hibernate flush/commit behavior for job option/notification updates.
     */
    private void mockUpdateCollaborators() {
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
            authorizeProjectJobAll(*_) >> true
            authorizeProjectResourceAll(*_) >> true
            authorizeProjectResourceAny(*_) >> true
            authorizeProjectJobAny(*_) >> true
            getAuthContextWithProject(_, _) >> { args -> args[0] }
        }
        service.frameworkService = Mock(FrameworkService) {
            existsFrameworkProject(_) >> true
            isClusterModeEnabled() >> false
            getServerUUID() >> UUID.randomUUID().toString()
            projectNames(*_) >> ['AProject']
            getRundeckFramework() >> Mock(Framework) {
                getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy)
                }
            }
            pluginConfigFactory(_, _) >> Mock(PropertyResolverFactory.Factory) {
                create(_, _) >> Mock(PropertyResolver)
            }
            getFrameworkProject(_) >> projectMock
            getNodeStepPluginDescription(_) >> Mock(Description)
            getStepPluginDescription(_) >> Mock(Description)
            validateDescription(_, '', _, _, _, _) >> [valid: true]
        }
        service.pluginService = Mock(PluginService)
        service.executionServiceBean = Mock(ExecutionService) {
            executionsAreActive() >> false
        }
        service.executionUtilService = Mock(ExecutionUtilService) {
            createExecutionItemForWorkflow(_) >> Mock(WorkflowExecutionItem)
        }
        service.quartzScheduler = Mock(Scheduler)
        service.executionLifecycleComponentService = Mock(ExecutionLifecycleComponentService)
        service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager) {
            updateJob(_, _, _) >> { args -> RundeckJobDefinitionManager.importedJob(args[0], args[1]?.associations) }
            validateImportedJob(_) >> new Validator.ReportSet(true, [:])
        }
        service.jobStatsDataProvider = new GormJobStatsDataProvider()
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }
        service.jobSchedulesService = Mock(SchedulesManager) {
            isScheduled(_) >> false
            shouldScheduleExecution(_) >> true
        }
        service.fileUploadService = Mock(FileUploadService)
    }

    private ScheduledExecution findSetupJob() {
        def jobDef = createJobParams()
        ScheduledExecution.getAll().find {
            it.jobName == jobDef.jobName && it.groupPath == jobDef.groupPath
        }
    }

    void "update with invalid enforced option default does not delete existing options"() {
        given: "the persisted job from setup() already has one Option named 'optvals'"
        def job = findSetupJob()
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }
        mockUpdateCollaborators()

        when: "an update is submitted with a new option whose enforced default isn't in its allowed values"
        def params = [
            id    : job.id,
            options: [
                new Option(name: 'badopt', enforced: true, valuesList: 'a,b,c', defaultValue: 'not-in-list')
            ]
        ]
        def results = service._doupdate(params, auth)
        // Force a flush: the production bug only manifests when the surrounding transaction
        // commits (flushing any pending, unflushed delete actions queued earlier in the
        // pipeline). @Rollback test transactions never commit, so without an explicit flush
        // here we'd only be observing in-memory session state, not what would actually be
        // persisted in production.
        ScheduledExecution.withSession { it.flush() }

        then: "the update fails validation on the invalid option"
        !results.success
        results.validation?.options

        and: "the original 'optvals' option is still present in the database, unreplaced"
        Option.findAllByScheduledExecution(job)*.name == ['optvals']
    }

    void "successful update fully replaces the option set"() {
        given: "the persisted job from setup() already has one Option"
        def job = findSetupJob()
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }
        mockUpdateCollaborators()

        when: "a valid update replaces the option set"
        def params = [
            id     : job.id,
            options: [new Option(name: 'newopt', enforced: false, defaultValue: 'ok')]
        ]
        def results = service._doupdate(params, auth)

        then: "the update succeeds"
        results.success

        and: "the database reflects only the new option definition"
        def savedJob = ScheduledExecution.get(job.id)
        Option.countByScheduledExecution(savedJob) == 1
        Option.findAllByScheduledExecution(savedJob)*.name == ['newopt']
    }
}