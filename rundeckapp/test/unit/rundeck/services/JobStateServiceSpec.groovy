package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.dispatcher.ExecutionState
import com.dtolabs.rundeck.core.jobs.JobNotFound
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.Workflow
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(JobStateService)
@Mock([Execution,ScheduledExecution,Workflow,CommandExec])
class JobStateServiceSpec extends Specification {

    def setup() {

        service.frameworkService=Stub(FrameworkService){
            authorizeProjectJobAll(null,!null,!null,!null) >>> [true,true]
        }

    }

    def cleanup() {
    }
    void "job ref not found by uuid"() {
        def jobName = 'abc'
        def groupPath = null

        def projectName = 'testProj'

        def jobUuid = '123'
        def dnejobUuid = '456'
        given:
        def job = new ScheduledExecution(
                jobName: jobName,
                groupPath: groupPath,
                project: projectName,
                uuid: jobUuid,
                workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
        ).save()

        when:
        def ref = service.jobForID(null,dnejobUuid, projectName)

        then:
        JobNotFound e = thrown()
        e.jobId==dnejobUuid
        e.project==projectName
    }

    void "job ref by uuid"() {
        def jobName = 'abc'
        def groupPath = null

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()

        when:
        def ref = service.jobForID(null,jobUuid, projectName)

        then:
        ref!=null
        ref.id==jobUuid
        ref.jobName==jobName
        ref.groupPath==groupPath
        ref.getJobAndGroup()==jobName
    }
    void "job ref by name and group"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()

        when:
        def ref = service.jobForName(null,groupPath,jobName, projectName)

        then:
        ref!=null
        ref.id==jobUuid
        ref.jobName==jobName
        ref.groupPath==groupPath
        ref.getJobAndGroup()==groupPath+'/'+jobName
    }
    void "job ref by name and group not found"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()

        when:
        def ref = service.jobForName(null,'bogus',jobName, projectName)

        then:
        JobNotFound e = thrown()
        e.jobName==jobName
        e.groupPath=='bogus'
        e.project==projectName
    }
    void "job ref by group/name string"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()

        when:
        def ref = service.jobForName(null,groupPath+'/'+jobName, projectName)

        then:
        ref!=null
        ref.id==jobUuid
        ref.jobName==jobName
        ref.groupPath==groupPath
        ref.getJobAndGroup()==groupPath+'/'+jobName
    }
    void "job ref by group/name string not found"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()

        when:
        def ref = service.jobForName(null,'wrong/'+jobName, projectName)

        then:
        JobNotFound e = thrown()
        e.jobName==jobName
        e.groupPath=='wrong'
        e.project==projectName
    }
    void "job ref by group/name string empty group"() {
        def jobName = 'abc'
        def groupPath = null

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()

        when:
        def ref = service.jobForName(null,jobName, projectName)

        then:
        ref!=null
        ref.id==jobUuid
        ref.jobName==jobName
        ref.groupPath==groupPath
        ref.getJobAndGroup()==jobName
    }
    void "job state no executions"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()
            def reference=service.jobForID(null,jobUuid,projectName)

        when:
        def state = service.getJobState(null,reference)

        then:
        !state.running
        state.runningExecutionIds.size()==0
        state.previousExecutionState==null
    }
    void "job state single running execution"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: projectName
            ).save()
            def reference=service.jobForID(null,jobUuid,projectName)

        when:
        def state = service.getJobState(null,reference)

        then:
        job!=null
        exec!=null
        state.running
        state.runningExecutionIds.size()==1
        state.runningExecutionIds[0]==exec.id.toString()
        state.previousExecutionState==null
    }
    void "job state multiple running executions"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: projectName
            ).save()
            def exec2 = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user2',
                    project: projectName
            ).save()
            def reference=service.jobForID(null,jobUuid,projectName)

        when:
        def state = service.getJobState(null,reference)

        then:
        job!=null
        exec!=null
        state.running
        state.runningExecutionIds.size()==2
        state.runningExecutionIds==new HashSet<String>([exec.id.toString(),exec2.id.toString()])
        state.previousExecutionState==null
    }
    void "job ref not authorized"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:


            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: projectName
            ).save()

        service.frameworkService=Stub(FrameworkService){
            authorizeProjectJobAll(null,job,[AuthConstants.ACTION_READ],projectName) >> false
        }

        when:
        def reference=service.jobForID(null,jobUuid,projectName)

        then:
        JobNotFound e = thrown()
        e.jobId==jobUuid
    }
    void "job state not authorized"() {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:


            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: projectName
            ).save()

        service.frameworkService=Stub(FrameworkService){
            authorizeProjectJobAll(null,job,[AuthConstants.ACTION_READ],projectName) >>> [true,false]
        }

        when:
        def reference=service.jobForID(null,jobUuid,projectName)
        def state = service.getJobState(null,reference)

        then:
        JobNotFound e = thrown()
        e.jobId==jobUuid
    }
    void "job state completed state"(String status, boolean cancelled, boolean timedOut, boolean retried, ExecutionState result) {
        def jobName = 'abc'
        def groupPath = 'elf'

        def projectName = 'testProj'

        def jobUuid = '123'
        given:
            def job = new ScheduledExecution(
                    jobName: jobName,
                    groupPath: groupPath,
                    project: projectName,
                    uuid: jobUuid,
                    workflow: new Workflow(commands:[new CommandExec(adhocRemoteString: 'echo hi')])
            ).save()
            def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    user:'user',
                    project: projectName,
                    status: status,
                    cancelled: cancelled,
                    abortedby: null,
                    timedOut: timedOut,
                    willRetry: retried,
            ).save()
            def reference=service.jobForID(null,jobUuid,projectName)

        def state = service.getJobState(null,reference)

        expect:
        job!=null
        exec!=null
        !state.running
        state.runningExecutionIds.size()==0
        state.previousExecutionState==result

        where:
        status | cancelled | timedOut | retried | result
        'true' | false | false | false | ExecutionState.succeeded
        'false' | false | false | false | ExecutionState.failed
        'false' | true | false | false | ExecutionState.aborted
        'false' | false | true | false | ExecutionState.timedout
        'false' | false | false | true | ExecutionState.failed_with_retry
    }
}
