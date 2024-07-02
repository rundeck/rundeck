package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import grails.testing.gorm.DataTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.components.jobs.JobDefinitionComponent
import org.rundeck.app.data.model.v1.job.component.JobComponentData
import org.rundeck.app.data.providers.GormJobDataProvider
import org.rundeck.app.events.LogJobChangeEvent
import org.springframework.validation.Errors
import rundeck.CommandExec
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.data.job.RdJob
import rundeck.data.job.RdLogConfig
import rundeck.data.job.RdNodeConfig
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import rundeck.services.data.ScheduledExecutionDataService
import spock.lang.Specification

import javax.security.auth.Subject
import javax.servlet.http.HttpSession

class RdJobServiceSpec extends Specification implements DataTest {
    RdJobService service
    GormJobDataProvider provider = new GormJobDataProvider()

    Closure doWithSpring() {
        { ->
            rundeckJobDefinitionManager(InstanceFactoryBean, Mock(RundeckJobDefinitionManager))
        }
    }

    void setup() {
        def httpSession = Mock(HttpSession) {
            getSubject() >> new Subject()
            getAttribute("user") >> "user"
        }
        provider.metaClass.getSession = {
            httpSession
        }
        mockDomains(ScheduledExecution, Workflow, WorkflowStep, CommandExec)
        mockDataService(ScheduledExecutionDataService)
        provider.scheduledExecutionDataService = applicationContext.getBean(ScheduledExecutionDataService)
        service = new RdJobService()
        service.scheduledExecutionDataService = applicationContext.getBean(ScheduledExecutionDataService)
        service.jobDataProvider = provider
        service.metaClass.getSession = {
            httpSession
        }
    }

    def "SaveJob"() {

        given:
        RdJob rdJob = mockedJob()
        1 * rdJob.validate() >> true
        1 * rdJob.getErrors() >> Mock(Errors) {
            hasErrors() >> false
        }
        service.frameworkService =  Mock(FrameworkService) {
            1 * userAuthContext(_) >> Mock(UserAndRolesAuthContext) {
                getUsername() >> "user"
                getRoles() >> new TreeSet<>(["r1","r2"])
            }
            existsFrameworkProject("one") >> true
            getFrameworkProject("one") >> Mock(IRundeckProject)

        }
        service.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor) {
            1 * authorizeProjectJobAll(_,_,_,_) >> true
        }

        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * rescheduleJob(_,_,_,_,_,_) >> null
        }
        service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager) {
            2 * getJobDefinitionComponents() >> [:]
            1 * validateImportedJob(_) >> Mock(Validator.ReportSet) {
                getValidations() >> [:]
            }
            1 * updateJob(_,_,_) >> null
        }

        when:
        def saved = service.saveJob(rdJob)
        then:
        saved.uuid
    }

    def "RunComponentBeforeSave"() {
        given:
        service.scheduledExecutionService = Mock(ScheduledExecutionService) {
            1 * getNodes(_,_) >> Mock(INodeSet)
        }
        service.jobLifecycleComponentService = Mock(JobLifecycleComponentService) {
            1 * beforeJobSave(_,_) >> Mock(JobLifecycleStatus) {
                isUseNewValues() >> true
                getOptions() >> new TreeSet<JobOption>()
            }
        }
        RdJob job = mockedJob()

        when:
        service.runComponentBeforeSave("user", job)

        then:
        1 * job.validate()

    }

    def "ValidateComponents"() {
        given:
        RdJob job = new RdJob()
        ScheduledExecution se = new ScheduledExecution()
        service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager) {
            2 * getJobDefinitionComponents() >> [:]
            1 * updateJob(_,_,_) >> Mock(ImportedJob)
            1 * validateImportedJob(_) >> Mock(Validator.ReportSet) {
                1 * getValidations() >> validations
            }
        }

        when:
        service.validateComponents(se, job)

        then:
        job.errors.errorCount == errCount

        where:
        errCount | validations
        0        | [:]
        1        | ["component1":new Validator.Report(errors: ["err1":"validation error"])]
    }

    def "ValidateComponentsExist"() {
        given:
        RdJob job = mockedJob()

        service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager) {
            1 * getJobDefinitionComponents() >> ["component1": [:] as JobDefinitionComponent]
        }

        when:
        service.validateComponentsExist(job)

        then:

        1* job.getComponents() >> ["job-queue":[:] as JobComponentData]
        1* job.getErrors() >> Mock(Errors) {
            1 * rejectValue("components","jobData.components.notfound",_,_)
        }


    }

    def "DetectJobChanges"() {
        given:
        RdJob job = new RdJob(uuid: uuid, jobName: newjob, project:"one")
        ScheduledExecution se = new ScheduledExecution( uuid: uuid, jobName: oldjob, project:"one")
        se.id = 1L
        LogJobChangeEvent event = new LogJobChangeEvent()
        service.jobSchedulesService = Mock(JobSchedulesService) {
            isScheduled(_) >> true
        }
        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
        }

        when:
        def actual = service.detectJobChanges(se, job, event)

        then:
        actual.scheduledJobName == uuid
        actual.renamed == renamed
        event.changeinfo.origName == uuid
        if(renamed) event.changeinfo.rename

        where:
        uuid        | renamed  | oldjob      | newjob
        "uuid"      | true     | "oldjob"    | "newjob"
        null        | false    | "job1"      | "job1"

    }

    def "AuthorizeEditAndUpdateJobUserAndRoles"() {
        given:
        RdJob job = new RdJob()
        def authCtx = Mock(UserAndRolesAuthContext) {
            getUsername() >> "user"
            getRoles() >> ["r1","r2"]
        }
        service.rundeckAuthContextProcessor = Mock(AppAuthContextProcessor)
        ScheduledExecution se = new ScheduledExecution()

        when:
        service.authorizeEditAndUpdateJobUserAndRoles(authCtx, se, job)

        then:
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAll(_,_,_,_) >> false
        job.errors.errorCount == 2
        se.user == "user"
        se.userRoles == ["r1","r2"]
    }


    def mockedJob() {
        return Mock(RdJob) {
            getJobName() >> "job1"
            getProject() >> "one"
            getScheduled() >> false
            getNodeConfig() >> new RdNodeConfig()
            getLogConfig() >> new RdLogConfig()
            getComponents() >> [:]
            getWorkflow() >> new RdWorkflow(steps: [new RdWorkflowStep(nodeStep: false,pluginType: "builtin-command",configuration: [exec:"echo hello"])])
        }
    }
}
