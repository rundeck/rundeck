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
import rundeck.Notification
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

    void "update with invalid email notification does not delete existing notifications or options"() {
        given: "the persisted job from setup() already has one Option and one Notification"
        def job = findSetupJob()
        def existingNotification = Notification.fromMap('onsuccess', [recipients: 'valid@example.com'])
        existingNotification.scheduledExecution = job
        job.addToNotifications(existingNotification)
        job.save(flush: true, failOnError: true)
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }
        mockUpdateCollaborators()

        when: "an update is submitted with a new notification containing an invalid email address"
        def params = [
            id                : job.id,
            jobNotificationsJson: '[{"type":"email","trigger":"onsuccess","config":{"recipients":"not-an-email"}}]'
        ]
        def results = service._doupdate(params, auth)
        // Force a flush: the production bug only manifests when the surrounding transaction
        // commits (flushing any pending, unflushed delete actions queued earlier in the
        // pipeline). @Rollback test transactions never commit, so without an explicit flush
        // here we'd only be observing in-memory session state, not what would actually be
        // persisted in production.
        ScheduledExecution.withSession { it.flush() }

        then: "the update fails validation on the invalid notification"
        !results.success

        and: "the original notification is still present in the database, unreplaced"
        Notification.findAllByScheduledExecution(job)*.eventTrigger == ['onsuccess']
        Notification.findAllByScheduledExecution(job)[0].mailConfiguration().recipients == 'valid@example.com'

        and: "the original option is unaffected, since this update didn't submit any option input"
        Option.findAllByScheduledExecution(job)*.name == ['optvals']
    }

    void "successful update fully replaces the notification set"() {
        given: "the persisted job from setup() already has one Option and one Notification"
        def job = findSetupJob()
        // setup()'s 'optvals' Option has no defaultValue, which is only valid because it's
        // never independently revalidated by the other tests in this file (they replace it
        // with a fully-valid Option first). Since this test intentionally leaves options
        // untouched, give it a valid defaultValue here so the full-job revalidation this
        // update triggers isn't rejected for an unrelated, pre-existing reason.
        Option.findAllByScheduledExecution(job)[0].with {
            defaultValue = 'default'
            save(flush: true, failOnError: true)
        }
        def existingNotification = Notification.fromMap('onsuccess', [recipients: 'valid@example.com'])
        existingNotification.scheduledExecution = job
        job.addToNotifications(existingNotification)
        job.save(flush: true, failOnError: true)
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }
        mockUpdateCollaborators()

        when: "a valid update replaces the notification set"
        def params = [
            id                  : job.id,
            jobNotificationsJson: '[{"type":"email","trigger":"onfailure","config":{"recipients":"other@example.com"}}]'
        ]
        def results = service._doupdate(params, auth)

        then: "the update succeeds"
        results.success

        and: "the database reflects only the new notification definition"
        def savedJob = ScheduledExecution.get(job.id)
        Notification.countByScheduledExecution(savedJob) == 1
        Notification.findAllByScheduledExecution(savedJob)*.eventTrigger == ['onfailure']
    }

    void "successful update that removes all options actually deletes them from the database"() {
        given: "the persisted job from setup() already has one Option"
        def job = findSetupJob()
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }
        mockUpdateCollaborators()

        when: "a valid update replaces the session-edited option set with an empty one"
        def params = [
            id                     : job.id,
            _sessionopts           : true,
            _sessionEditOPTSObject : [:]
        ]
        def results = service._doupdate(params, auth)

        then: "the update succeeds"
        results.success

        and: "the previously-persisted option is actually gone from the database"
        def savedJob = ScheduledExecution.get(job.id)
        Option.countByScheduledExecution(savedJob) == 0
    }

    void "create with job-queue enabled and a secure option is rejected"() {
        given: "a brand-new job definition with a secure option, and a component validator that rejects it"
        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> (['test'] as Set)
        }
        mockUpdateCollaborators()
        service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager) {
            updateJob(_, _, _) >> { args -> RundeckJobDefinitionManager.importedJob(args[0], args[1]?.associations) }
            validateImportedJob(_) >> {
                // Simulate the rundeckpro job-queue component's own validation: it inspects
                // the real scheduledExecution.options directly (via hasSecureOptions()), so
                // this only rejects correctly if that property reflects the submitted
                // candidate options DURING validation, not just after a successful save.
                ScheduledExecution se = (ScheduledExecution) it[0].job
                if (se.hasSecureOptions()) {
                    def report = new Validator.Report()
                    report.errors.put('job-queue', 'Job Queueing is not supported in jobs with secure options.')
                    return new Validator.ReportSet(false, ['job-queue': report])
                }
                return new Validator.ReportSet(true, [:])
            }
        }

        def newJob = new ScheduledExecution(createJobParams(
            jobName: 'job queue secure options test',
            options: []
        ))
        newJob.addToOptions(new Option(name: 'secureopt', secureInput: true, defaultValue: '/keys/something'))
        def importedJob = RundeckJobDefinitionManager.importedJob(newJob, [:])

        when: "the job is created"
        def results = service._docreateJobOrParams(importedJob, [:], auth)

        then: "the create fails validation"
        !results.success
        results.validation?.containsKey('job-queue')

        and: "nothing was persisted"
        !ScheduledExecution.findByJobName('job queue secure options test')
    }
}