import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.services.ExecutionService
import rundeck.services.ExecutionServiceException
import rundeck.services.FrameworkService
import rundeck.services.JobStateService
import rundeck.services.ReportService
import rundeck.services.StorageService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 2/17/15.
 */
@TestFor(ExecutionService)
@Mock([Execution,ScheduledExecution,Workflow,CommandExec,Option])
class ExecutionServiceSpec extends Specification {

    void "retry execution otherwise running"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry:'1'
        )
        job.save()
        def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'userB',
                    project: 'AProject'
        ).save()
        def exec2 = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: 'AProject'
        ).save()
        service.frameworkService=Stub(FrameworkService){
            getServerUUID() >> null
        }
        when:
        Execution e2=service.createExecution(job,"user1",['extra.option.test':'12'],true,exec2.id)

        then:
        ExecutionServiceException e=thrown()
        e.code=='conflict'
        e.message ==~ /.*is currently being executed.*/
    }
    void "retry execution new execution"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                retry:'1'
        )
        job.save()
        def exec = new Execution(
                    scheduledExecution: job,
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user:'user',
                    project: 'AProject'
        ).save()
        service.frameworkService=Stub(FrameworkService){
            getServerUUID() >> null
        }
        when:
        Execution e2=service.createExecution(job,"user1",['extra.option.test':'12'],true,exec.id)

        then:
        e2!=null
    }

    @Unroll
    def "log execution state"(String statusString, String resultStatus, boolean issuccess,boolean iscancelled,boolean istimedout,boolean willretry) {
        given:
        def params = [:]
        service.reportService = Stub(ReportService) {
            reportExecutionResult(_) >> { args ->
                params = args[0]
            }
        }
        when:
        service.logExecution(
                null,
                'test1',
                'user1',
                issuccess,
                statusString,
                11,
                new Date(),
                'abc',
                'job1',
                'blah',
                iscancelled,
                istimedout,
                willretry,
                '1/1/1',
                null
        )

        then:
        params.jcExecId == 11
        params.jcJobId == 'abc'
        params.reportId == 'job1'
        params.adhocExecution == false
        params.ctxProject == 'test1'
        params.author == 'user1'
        params.title == 'blah'
        params.status == resultStatus
        params.node == '1/1/1'
        params.message == "Job status ${statusString}"
        params.dateStarted != null
        params.dateCompleted != null

        where:
        statusString        | resultStatus | issuccess | iscancelled | istimedout | willretry
        'succeeded'         | 'succeed'    | true      | false       | false      | false
        'true'              | 'succeed'    | true      | false       | false      | false
        'custom'            | 'other'      | false     | false       | false      | false
        'other status'      | 'other'      | false     | false       | false      | false
        'false'             | 'fail'       | false     | false       | false      | false
        'failed'            | 'fail'       | false     | false       | false      | false
        'aborted'           | 'cancel'     | false     | true        | false      | false
        'timedout'          | 'timeout'    | false     | false       | true       | false
        'failed-with-retry' | 'retry'      | false     | false       | false      | true
    }

    def "createJobReferenceContext secure opts blank values"(){
        given:
        def context = ExecutionContextImpl.builder()
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'secureOption':[:],'job':['execid':'123']])
                                          .privateDataContext(['option':[:],])
                                          .user('aUser')
                                          .build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                )
        null != se
        def opt1 = new Option(name: 'test1',  enforced: false, required: false, secureInput: true)
        def opt2 = new Option(name: 'test2',  enforced: false, required: false, secureInput: true, secureExposed: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService=Mock(FrameworkService){
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        2
        when:

        def newCtxt=service.createJobReferenceContext(
                se,
                context,
                ['-test1','${option.test1}','-test2','${option.test2}'] as String[],
                null,null,null, null, null,false
        )

        then:
        newCtxt.dataContext['secureOption'] == ['test2': '']
        newCtxt.dataContext['option'] == ['test2': '']
        newCtxt.privateDataContext['option'] == ['test1': '']
    }
    def "createJobReferenceContext secure opts replacement values"(){
        given:
        def context = ExecutionContextImpl.builder()
                                          .threadCount(1)
                                          .keepgoing(false)
                                          .dataContext(['option':['monkey':'wakeful'],'secureOption':['test2':'zimbo'],'job':['execid':'123']])
                                          .privateDataContext(['option':['zilch':'phoenix'],])
                                          .user('aUser')
                                          .build()
        ScheduledExecution se = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(keepgoing: true, commands: [new CommandExec([adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle'])]),
                )
        null != se
        def opt1 = new Option(name: 'test1',  enforced: false, required: false, secureInput: true)
        def opt2 = new Option(name: 'test2',  enforced: false, required: false, secureInput: true, secureExposed: true)
        assertTrue(opt1.validate())
        assertTrue(opt2.validate())
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        null != se.save()

        service.frameworkService=Mock(FrameworkService){
            1 * filterNodeSet(null, 'AProject')
            1 * filterAuthorizedNodes(*_)
            0 * _(*_)
        }

        service.storageService = Mock(StorageService)
        service.jobStateService = Mock(JobStateService)
        2
        when:

        def newCtxt=service.createJobReferenceContext(
                se,
                context,
                ['-test1','${option.zilch}','-test2','${option.test2}'] as String[],
                null,null,null, null, null,false
        )

        then:

        newCtxt.dataContext['secureOption'] == ['test2': 'zimbo']
        newCtxt.dataContext['option'] == ['test2': 'zimbo']
        newCtxt.privateDataContext['option'] == ['test1': 'phoenix']
    }
}
