package rundeck.services

import com.dtolabs.rundeck.core.authorization.UserAndRoles
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.quartz.ListenerManager
import org.quartz.Scheduler
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Notification
import rundeck.Option
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.controllers.ScheduledExecutionController
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 6/24/15.
 */
@TestFor(ScheduledExecutionService)
@Mock([Workflow, ScheduledExecution, CommandExec, Notification, Option,PluginStep,JobExec,WorkflowStep])
class ScheduledExecutionServiceSpec extends Specification {

    public static final String TEST_UUID1 = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C7'
    public static final String TEST_UUID2 = '490966E0-2E2F-4505-823F-E2665ADC66FB'

    def setupDoValidate(boolean enabled=false){

        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject('testProject') >> true
            isClusterModeEnabled()>>enabled
            getServerUUID()>>TEST_UUID1
        }
        TEST_UUID1
    }
    def "blank email notification"() {
        given:
        setupDoValidate()

        when:
        def params = baseJobParams()+[
                workflow      : new Workflow(
                        threadcount: 1,
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'a remote string')]
                ),
                      notifications : [
                              [
                                      eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                                      type        : ScheduledExecutionController.EMAIL_NOTIFICATION_TYPE,
                                      content     : ''
                              ]
                      ]
        ]
        def results = service._dovalidate(params, Mock(UserAndRoles))
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:

        results.failed
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution

        scheduledExecution.errors != null
        scheduledExecution.errors.hasErrors()
        scheduledExecution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS)

    }

    def "blank webhook notification"() {
        given:
        setupDoValidate()

        when:
        def params = baseJobParams()+[
                      notifications : [
                              [
                                      eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME,
                                      type        : ScheduledExecutionController.WEBHOOK_NOTIFICATION_TYPE,
                                      content     : ''
                              ]
                      ]
        ]
        def results = service._dovalidate(params, Mock(UserAndRoles))
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:

        results.failed
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution

        scheduledExecution.errors != null
        scheduledExecution.errors.hasErrors()
        scheduledExecution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_URL)

    }

    private Map createJobParams(Map overrides = [:]) {
        [
                jobName       : 'blue',
                project       : 'AProject',
                groupPath     : 'some/where',
                description   : 'a job',
                argString     : '-a b -c d',
                workflow      : new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])]
                ),
                serverNodeUUID: null,
                scheduled     : true
        ] + overrides
    }

    def "claim all scheduled jobs"() {
        given:
        def targetserverUUID = UUID.randomUUID().toString()
        def serverUUID1 = UUID.randomUUID().toString()
        def serverUUID2 = UUID.randomUUID().toString()
        ScheduledExecution job1 = new ScheduledExecution(
                createJobParams(jobName: 'blue1', project: 'AProject', serverNodeUUID: null)
        ).save()
        ScheduledExecution job2 = new ScheduledExecution(
                createJobParams(jobName: 'blue2', project: 'AProject2', serverNodeUUID: serverUUID1)
        ).save()
        ScheduledExecution job3 = new ScheduledExecution(
                createJobParams(jobName: 'blue3', project: 'AProject2', serverNodeUUID: serverUUID2)
        ).save()
        ScheduledExecution job3x = new ScheduledExecution(
                createJobParams(jobName: 'blue3', project: 'AProject2', serverNodeUUID: targetserverUUID)
        ).save()
        ScheduledExecution job4 = new ScheduledExecution(
                createJobParams(jobName: 'blue4', project: 'AProject2', scheduled: false)
        ).save()
        def jobs = [job1, job2, job3, job3x, job4]
        when:
        def resultMap = service.claimScheduledJobs(targetserverUUID, null, true)

        ScheduledExecution.withSession { session ->
            session.flush()
            jobs*.refresh()
        }
        then:

        [job1, job2, job3, job3x] == jobs.findAll { it.serverNodeUUID == targetserverUUID }
        [job1, job2, job3]*.extid == resultMap.keySet() as List
    }

    def "claim all scheduled jobs in a project"(
            String targetProject,
            String targetServerUUID,
            String serverUUID1,
            List<Map> dataList,
            List<String> resultList
    )
    {
        setup:
        def jobs = dataList.collect {
            new ScheduledExecution(createJobParams(it)).save()
        }

        when:
        def resultMap = service.claimScheduledJobs(targetServerUUID, null, true, targetProject)

        ScheduledExecution.withSession { session ->
            session.flush()
            jobs*.refresh()
        }
        then:

        resultList == resultMap.keySet() as List

        where:
        targetProject | targetServerUUID |
                serverUUID1 |
                dataList |
                resultList
        'AProject'    | TEST_UUID1       |
                TEST_UUID2  |
                [[uuid: 'job3', project: 'AProject', serverNodeUUID: TEST_UUID1], [uuid: 'job1', serverNodeUUID: TEST_UUID2], [project: 'AProject2', uuid: 'job2']] |
                ['job1']
        'AProject2'   | TEST_UUID1       |
                TEST_UUID2  |
                [[uuid: 'job3', project: 'AProject2', serverNodeUUID: TEST_UUID1], [uuid: 'job1', serverNodeUUID: TEST_UUID2], [project: 'AProject2', uuid: 'job2']] |
                ['job2']
    }

    @Unroll
    def "should scheduleJob"() {
        given:
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()
        def scheduleDate = new Date()

        when:
        def result = service.scheduleJob(job, null, null)

        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> executionsAreActive
        1 * service.quartzScheduler.scheduleJob(_, _) >> scheduleDate
        result == scheduleDate

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        true                | true            | true             | true        | true
    }

    @Issue('https://github.com/rundeck/rundeck/issues/1475')
    @Unroll
    def "should not scheduleJob when executionsAreActive=#executionsAreActive scheduleEnabled=#scheduleEnabled executionEnabled=#executionEnabled and hasSchedule=#hasSchedule"() {
        given:
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()
        def scheduleDate = new Date()

        when:
        def result = service.scheduleJob(job, null, null)

        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> executionsAreActive
        0 * service.quartzScheduler.scheduleJob(_, _)
        result == null

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule
        false               | true            | true             | true
        true                | false           | true             | true
        true                | true            | false            | true
        true                | true            | true             | false
    }



    def "do validate adhoc ok"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      workflow: [threadcount: 1, keepgoing: true, "commands[0]": cmd],
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        !results.failed

        where:
        cmd                                                                              | _
        [adhocExecution: true, adhocRemoteString: 'test what']                           | _
        [adhocExecution: true, adhocFilepath: 'test what']                               | _
        [adhocExecution: true, adhocLocalString: 'test what']                            | _
        [adhocExecution: true, adhocFilepath: 'test file', argString: 'test args']       | _
        [adhocExecution: true, adhocRemoteString: 'test remote', argString: 'test args'] | _
        [adhocExecution: true, adhocLocalString: 'test local', argString: 'test args']   | _
    }
    def "do validate workflow ok"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      workflow: [threadcount: 1, keepgoing: true]+cmds,
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        !results.failed
        results.scheduledExecution.workflow.commands.size()==3
        results.scheduledExecution.workflow.commands[0] instanceof CommandExec
        results.scheduledExecution.workflow.commands[0].adhocRemoteString=='do something'
        results.scheduledExecution.workflow.commands[1] instanceof CommandExec
        results.scheduledExecution.workflow.commands[1].adhocLocalString=='test dodah'
        results.scheduledExecution.workflow.commands[2] instanceof JobExec
        results.scheduledExecution.workflow.commands[2].jobName=='test1'
        results.scheduledExecution.workflow.commands[2].jobGroup=='a/test'


        where:
        cmds                                                                     | _
        ["commands[0]": [adhocExecution: true, adhocRemoteString: "do something"],
         "commands[1]": [adhocExecution: true, adhocLocalString: "test dodah"],
         "commands[2]": [jobName: 'test1', jobGroup: 'a/test']] | _

    }
    @Unroll
    def "do validate node-first strategy error handlers"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      workflow: new Workflow([threadcount: 1, keepgoing: true, strategy: strategy]+cmds),
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        results.scheduledExecution.errors.hasErrors()==expectFail
        results.failed==expectFail
        if(expectFail){
            results.scheduledExecution.workflow.commands[0].errors.hasErrors()
            results.scheduledExecution.workflow.commands[0].errors.hasFieldErrors('errorHandler')
        }


        where:
        cmds | expectFail | strategy

        ["commands": [new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                new CommandExec(adhocRemoteString: 'test command2', adhocExecution: true)
        ),]] | false | 'node-first'

        ["commands": [new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                new CommandExec(adhocRemoteString: 'test command2', adhocExecution: true)
        ),]] | false | 'step-first'

        ["commands": [new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                new JobExec(jobGroup: 'test1', jobName: 'blah')
        ),]] | true | 'node-first'

        ["commands": [new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true, errorHandler:
                new JobExec(jobGroup: 'test1', jobName: 'blah')
        ),]] | false | 'step-first'

        ["commands": [new JobExec(jobGroup: 'test1', jobName: 'blah', errorHandler:
                new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true)
        ),]] | false |  'node-first'

        ["commands": [new JobExec(jobGroup: 'test1', jobName: 'blah', errorHandler:
                new CommandExec(adhocRemoteString: 'test command1', adhocExecution: true)
        ),]] | false |  'step-first'

        ["commands": [new JobExec(jobGroup: 'test1', jobName: 'blah', errorHandler:
                new JobExec(jobGroup: 'test1', jobName: 'blah')
        ),]] | false | 'node-first'

        ["commands": [new JobExec(jobGroup: 'test1', jobName: 'blah', errorHandler:
                new JobExec(jobGroup: 'test1', jobName: 'blah')
        ),]] | false | 'step-first'

    }
    def "do validate adhoc invalid"() {
        given:
        setupDoValidate()
        def params = baseJobParams() + [
                workflow: [threadcount: 1, keepgoing: true, "commands[0]": cmd],
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        results.failed
        results.scheduledExecution.errors.hasFieldErrors('workflow')
        results.scheduledExecution.workflow.commands[0].errors.hasFieldErrors(fieldName)

        where:
        cmd                                           | fieldName
        [adhocExecution: true, adhocRemoteString: ''] | 'adhocExecution'
        [adhocExecution: true, adhocFilepath: '']     | 'adhocExecution'
        [adhocExecution: true, adhocLocalString: '']  | 'adhocExecution'
        [adhocExecution: true, adhocRemoteString: 'test1', adhocLocalString: 'test2']  | 'adhocRemoteString'
    }
    def "do validate empty input is invalid"() {
        given:
        setupDoValidate()
        def params = [:]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        results.failed
        for(String prop:['workflow','project','jobName']){
            results.scheduledExecution.errors.hasFieldErrors(prop)
        }

    }
    def "do validate job name"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+inparams
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        results.failed
        results.scheduledExecution.errors.hasFieldErrors(fieldName)

        where:
        inparams                 | fieldName
        [jobName: 'test/monkey'] | 'jobName'
    }

    def "do validate node dispatch threadcount blank"() {
        given:
        setupDoValidate()
        def params = baseJobParams() + inparams
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        !results.failed
        results.scheduledExecution.nodeThreadcount == expectCount

        where:
        inparams                                                             | expectCount
        [doNodedispatch: 'true', nodeInclude: 'blah', nodeThreadcount: "1"]  | 1
        [doNodedispatch: 'true', nodeInclude: 'blah', nodeThreadcount: ""]   | 1
        [doNodedispatch: 'true', nodeInclude: 'blah', nodeThreadcount: null] | 1
        [doNodedispatch: 'true', nodeInclude: 'blah', nodeThreadcount: "2"]  | 2
        [doNodedispatch: 'true', nodeInclude: 'blah', nodeThreadcount: 2]    | 2
    }
    def "do validate old node filter params"() {
        given:
        setupDoValidate()
        def params = baseJobParams() + inparams
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        !results.failed
        for(String key: expect.keySet()){
            results.scheduledExecution[key] == expect[key]
        }

        where:
        inparams                                                             | expect
        [doNodedispatch: 'true', nodeIncludeName: "bongo",
                                 nodeExcludeOsFamily: "windows",
                                 nodeIncludeTags: "spaghetti"]  | [filter:'name: bongo tags: spaghetti !os-family: windows']
    }

    def "validate notifications email data"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      notifications:
                              [
                                      [
                                              eventTrigger: trigger,
                                              type        : type,
                                              content     : content
                                      ]
                              ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.notifications[0].eventTrigger == trigger
        results.scheduledExecution.notifications[0].type == type
        results.scheduledExecution.notifications[0].configuration == [recipients:content]

        where:
        trigger                                             | type    | content
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.com,d@example.com'
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'c@example.com,d@example.com'
        ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'c@example.com,d@example.com'
    }
    def "validate notifications email data any domain"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      notifications:
                              [
                                      [
                                              eventTrigger: trigger,
                                              type        : type,
                                              content     : content
                                      ]
                              ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.notifications[0].eventTrigger == trigger
        results.scheduledExecution.notifications[0].type == type
        results.scheduledExecution.notifications[0].configuration == [recipients:content]

        where:
        trigger                                             | type    | content
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd'
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | '${job.user.name}@something.org'
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | '${job.user.email}'
        ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'monkey@internal'
    }
    def "invalid notifications data"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      notifications:
                              [
                                      [
                                              eventTrigger: trigger,
                                              type        : type,
                                              content     : content
                                      ]
                              ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        results.failed
        results.scheduledExecution.errors.hasErrors()
        results.scheduledExecution.errors.hasFieldErrors(contentField)

        where:
        contentField|trigger                                             | type    | content
        ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | ''
        ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd@example.com'
        ScheduledExecutionController.NOTIFY_SUCCESS_URL|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'url' | ''
        ScheduledExecutionController.NOTIFY_SUCCESS_URL|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'url' | 'c@example.comd@example.com'
        ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | ''
        ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'monkey@ example.com'
        ScheduledExecutionController.NOTIFY_FAILURE_URL|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'url' | ''
        ScheduledExecutionController.NOTIFY_FAILURE_URL|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'url' | 'monkey@ example.com'
        ScheduledExecutionController.NOTIFY_START_RECIPIENTS|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | ''
        ScheduledExecutionController.NOTIFY_START_RECIPIENTS|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'c@example.com d@example.com'
        ScheduledExecutionController.NOTIFY_START_URL|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'url' | ''
        ScheduledExecutionController.NOTIFY_START_URL|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'url' | 'c@example.com d@example.com'
    }
    def "do update job invalid notifications"() {
        given:
        setupDoValidate()
        def se = new ScheduledExecution(createJobParams()).save()
        def newjob = new ScheduledExecution(createJobParams(
                notifications:
                        [
                                new Notification(
                                        eventTrigger: trigger,
                                        type: type,
                                        content: content
                                )
                        ]
        )).save()

        when:
        def results = service._doupdateJob(se.id,newjob, mockAuth())

        then:
        !results.success
        results.scheduledExecution.errors.hasErrors()
        results.scheduledExecution.errors.hasFieldErrors(contentField)

        where:
        contentField|trigger                                             | type    | content
        ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd@example.com'
        ScheduledExecutionController.NOTIFY_SUCCESS_URL|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'url' | 'c@example.comd@example.com'
        ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'monkey@ example.com'
        ScheduledExecutionController.NOTIFY_FAILURE_URL|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'url' | 'monkey@ example.com'
        ScheduledExecutionController.NOTIFY_START_RECIPIENTS|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'c@example.com d@example.com'
        ScheduledExecutionController.NOTIFY_START_URL|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'url' | 'c@example.com d@example.com'
    }
    def "validate notifications email form fields"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      (enablefield): 'true',
                      (contentField): content,
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.notifications[0].eventTrigger == trigger
        results.scheduledExecution.notifications[0].type == 'email'
        results.scheduledExecution.notifications[0].configuration == [recipients:content]

        where:
        trigger|enablefield                                             | contentField | content
        'onsuccess'|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL | ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS | 'c@example.com,d@example.com'
        'onfailure'|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL | ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS | 'c@example.com,d@example.com'
        'onstart'|ScheduledExecutionController.NOTIFY_ONSTART_EMAIL | ScheduledExecutionController.NOTIFY_START_RECIPIENTS | 'c@example.com,d@example.com'
    }
    def "invalid notifications email form fields"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      (enablefield): 'true',
                      (contentField): content,
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        results.failed
        results.scheduledExecution.errors.hasErrors()
        results.scheduledExecution.errors.hasFieldErrors(contentField)

        where:
        trigger|enablefield                                             | contentField | content
        'onsuccess'|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL | ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS | 'c@example.'
        'onfailure'|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL | ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS | '@example.com'
        'onstart'|ScheduledExecutionController.NOTIFY_ONSTART_EMAIL | ScheduledExecutionController.NOTIFY_START_RECIPIENTS | 'c@example.'
    }


    def "do validate crontabstring"() {
        given:
        setupDoValidate()
        def params = baseJobParams() +[scheduled: true, crontabString: '0 1 2 3 4 ? *', useCrontabString: 'true']
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.scheduled
        results.scheduledExecution.seconds=='0'
        results.scheduledExecution.minute=='1'
        results.scheduledExecution.hour=='2'
        results.scheduledExecution.dayOfMonth=='3'
        results.scheduledExecution.month=='4'
        results.scheduledExecution.dayOfWeek=='?'
        results.scheduledExecution.year=='*'
    }

    private LinkedHashMap<String, Serializable> baseJobParams() {
        [jobName : 'monkey1', project: 'AProject', description: 'blah',
         workflow: [threadcount  : 1, keepgoing: true,
                    "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command']],
        ]
    }

    def "validate options data"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[options: [
                              "options[0]":
                                      [
                                              name: 'test3',
                                              defaultValue: 'val3',
                                              enforced: false,
                                              valuesUrl: "http://test.com/test3"
                                      ]
                      ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.options.size()==1
        results.scheduledExecution.options[0].name == 'test3'
        results.scheduledExecution.options[0].defaultValue == 'val3'
        !results.scheduledExecution.options[0].enforced
        results.scheduledExecution.options[0].realValuesUrl.toExternalForm() == 'http://test.com/test3'

    }
    def "validate scheduled job with required option without default"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      scheduled: true, crontabString: '0 1 2 3 4 ? *', useCrontabString: 'true',
                      options: [
                              'options[0]':
                                      [
                                              name: 'test3',
                                              required: true,
                                              enforced:false,
                                              defaultValue: null
                                      ]
                      ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        results.failed
        results.scheduledExecution.options.size()==1
        results.scheduledExecution.options[0].errors.hasFieldErrors('defaultValue')
        results.scheduledExecution.errors.hasFieldErrors('options')
    }
    def "validate options multivalued"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      options: [
                              "options[0]":
                                      [
                                              name: 'test3',
                                              defaultValue: 'val3',
                                              enforced: false,
                                              multivalued:true,
                                              delimiter: ','
                                      ]
                      ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.options.size()==1
        results.scheduledExecution.options[0].name == 'test3'
        results.scheduledExecution.options[0].defaultValue == 'val3'
        !results.scheduledExecution.options[0].enforced
        results.scheduledExecution.options[0].multivalued
        results.scheduledExecution.options[0].delimiter==','
    }
    def "invalid options data"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      options: [
                              "options[0]":
                                      [
                                              name: name,
                                              defaultValue: defval,
                                              enforced: enforced,
                                              valuesUrl: valuesUrl
                                      ]
                      ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        results.failed
        results.scheduledExecution.errors.hasFieldErrors('options')
        results.scheduledExecution.errors.getFieldError('options').getRejectedValue()[0].errors.hasFieldErrors(fieldName)

        where:
        name| defval     | enforced | valuesUrl               | fieldName
        null | 'val3'    | false    | 'http://test.com/test3' | 'name'
        'test1' | 'val3' | false    | 'hzzp://test.com/test3' | 'valuesUrl'
    }
    def "validate options multivalued with multiple defaults"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      options: [
                              "options[0]":
                                      [
                                              name: 'test3',
                                              defaultValue: 'val1,val2',
                                              enforced: true,
                                              multivalued:true,
                                              delimiter: ',',
                                              values:['val1','val2','val3']
                                      ]
                      ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.options.size()==1
        results.scheduledExecution.options[0].name == 'test3'
        results.scheduledExecution.options[0].defaultValue == 'val1,val2'
        results.scheduledExecution.options[0].enforced
        results.scheduledExecution.options[0].multivalued
        results.scheduledExecution.options[0].delimiter==','
        results.scheduledExecution.options[0].values==['val1','val2','val3'] as Set
    }
    def "invalid options multivalued"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      options: [
                              "options[0]":
                                      [
                                              name: 'test3',
                                              defaultValue: 'val1,val2',
                                              enforced: true,
                                              multivalued:true,
                                              delimiter: ',',
                                              values:['val1','val2','val3']
                                      ]+data
                      ]
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        results.failed
        results.scheduledExecution.errors.hasFieldErrors('options')
        results.scheduledExecution.errors.getFieldError('options').getRejectedValue()[0].errors.hasFieldErrors(fieldName)

        where:
        data                             | fieldName
        [delimiter: null]                | 'delimiter'
        [defaultValue: 'val1,val2,val4'] | 'defaultValue'
        [secureInput: true]              | 'multivalued'
    }
    def setupDoUpdate(enabled=false){
        def uuid=UUID.randomUUID().toString()
        service.frameworkService=Mock(FrameworkService){
            authorizeProjectJobAll(*_)>>true
            authorizeProjectResourceAll(*_)>>true
            existsFrameworkProject('AProject')>>true
            existsFrameworkProject('BProject')>>true
            getAuthContextWithProject(_,_)>>{args->
                return args[0]
            }
            isClusterModeEnabled()>>enabled
            getServerUUID()>>uuid
        }
        service.executionServiceBean=Mock(ExecutionService){
            executionsAreActive()>>false
        }
        uuid
    }

    private UserAndRolesAuthContext mockAuth() {
        Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }
    }

    def "do update invalid"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams(orig)).save()

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        !results.success
        results.scheduledExecution.errors.hasFieldErrors(fieldName)

        where:
        inparams | fieldName | orig
        //invalid job name
        [jobName:'test/blah']|'jobName' | [:]
        //invalid workflow step
        [ workflow: [threadcount: 1, keepgoing: true, "commands[0]": [adhocExecution: true, adhocRemoteString: '']],
          _workflow_data: true,]|'workflow' | [:]
        //required option must have default when job is scheduled
        [scheduled: true,
         options: ["options[0]": [name: 'test', required:true, enforced: false, ]],
         crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true'] | 'options' | [:]
        //existing option job now scheduled
        [scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true'] | 'options' | [options:[new Option(name: 'test', required:true, enforced: false)]]
    }
    def "do update job invalid"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams()).save()
        def newjob=new ScheduledExecution(createJobParams(inparams))

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }


        when:
        def results = service._doupdateJob(se.id,newjob, mockAuth())


        then:
        !results.success
        results.scheduledExecution.errors.hasFieldErrors(fieldName)

        where:
        inparams | fieldName
        [jobName:'test/blah']|'jobName'
        [ workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: '', adhocExecution: true)])]|'workflow'
    }
    @Unroll("invalid crontab value for #reason")
    def "do update job invalid crontab"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams()).save()
        def newjob=new ScheduledExecution(createJobParams(scheduled:true,crontabString:crontabString))


        when:
        def results = service._doupdateJob(se.id,newjob, mockAuth())


        then:
        !results.success
        results.scheduledExecution.errors.hasFieldErrors('crontabString')

        where:
        crontabString                   | reason
        '0 0 2 32 */6 ?'                | 'day of month'
        '0 0 2 ? 12 8'                  | 'day of week'
        '0 21 */4 */4 */6 3 2010-2040'  | 'day of month and week set'
        '0 21 */4 ? */6 ? 2010-2040'    | 'day of month and week ?'
        '0 0 25 */4 */6 ?'              | 'hour'
        '0 70 */4 */4 */6 ?'            | 'minute'
        '0 0 2 3 13 ?'                  | 'month'
        '70 21 */4 */4 */6 ?'           | 'seconds'
        '0 21 */4 */4 */6 ? z2010-2040' | 'year char'
        '0 21 */4 */4 */6'              | 'too few components'
    }
    def "do update job options invalid"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams(options:[
                new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c']),
                new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
        ])).save()
        def newjob=new ScheduledExecution(createJobParams(
                options: [
                        new Option(name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3", multivalued: true),
                        new Option(name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd'], multivalued: true, delimiter: "testdelim")
                ]
        ))

        when:
        def results = service._doupdateJob(se.id,newjob, mockAuth())


        then:
        !results.success
        results.scheduledExecution.errors.hasFieldErrors('options')
        results.scheduledExecution.options[0].errors.hasFieldErrors('delimiter')
        !results.scheduledExecution.options[1].errors.hasErrors()

    }
    def "do update valid"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams(orig)).save()

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        if(expect){
            for(String key:expect.keySet()){
                results.scheduledExecution[key]==expect[key]
            }
        }

        where:
        inparams | orig                                                                                                                                                                                                      | expect
        [ workflow: [threadcount: 1, keepgoing: true, strategy:'node-first', "commands[0]": [adhocExecution: true, adhocLocalString: 'test local']],
          _workflow_data: true,]|[:]                                                                                                                                                                                         |[:]
        [nodeThreadcount: '',doNodedispatch: true,nodeInclude:'aname']|[[nodeThreadcount: 3,doNodedispatch: true,nodeInclude:'aname']]                                                                                       |[nodeThreadcount: 1]
        [scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true'] | [:] | [scheduled: true, seconds:'0', minute:'21', hour:'*/4', dayOfMonth:'*/4', month:'*/6', dayOfWeek:'?', year:'2010-2040']
        [scheduled: true,
         options: ["options[0]": [name: 'test', required:true, enforced: false,defaultValue:'abc' ]],
         crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true'] | [:] | [scheduled: true]
    }
    @Unroll
    def "do update workflow"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams(orig)).save()

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        results.scheduledExecution.workflow.strategy==inparams.workflow.strategy
        results.scheduledExecution.workflow.keepgoing==inparams.workflow.keepgoing in [true,'true']
        if(inparams.workflow.threadcount) {
            results.scheduledExecution.workflow.threadcount == inparams.workflow.threadcount
        }
        if(expect){
            results.scheduledExecution.workflow.commands.size()==expect.size()
            for(def i=0;i<expect.size();i++){
                def map = expect[i]
                for(String key:map.keySet()){
                    results.scheduledExecution.workflow.commands[i][key]==map[key]
                }
            }
        }

        where:
        inparams  | orig |  expect
        [workflow: [threadcount: 1, keepgoing: true, strategy: 'node-first', "commands[0]": [adhocExecution: true, adhocLocalString: 'test local']], _workflow_data: true,] |
                [:] |
                [[adhocLocalString:'test local']]
        [workflow: [threadcount: 1, keepgoing: "false", strategy: 'step-first', "commands[0]": [adhocExecution: true, adhocRemoteString: 'test command2']], _workflow_data: true,]                                                  |
                [:]                                                                                                                                                                                                                       |
                [[adhocRemoteString: 'test command2']]
        [workflow: [strategy: 'step-first', keepgoing: 'false']]                                                                                                                                                                    | [:] | []
        //update via session workflow
        ['_sessionwf':true, '_sessionEditWFObject':new Workflow(keepgoing: true, strategy: 'node-first', commands: [new CommandExec([adhocRemoteString: 'test buddy'])]), workflow: [strategy: 'step-first', keepgoing: 'false']] |
                [:] |
                [[adhocRemoteString: 'test buddy']]
    }

    @Unroll
    def "do update job valid"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(orig)).save()
        def newJob = new ScheduledExecution(createJobParams(inparams))



        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())


        then:
        results.success
        if(inparams.workflow) {
            results.scheduledExecution.workflow.commands.size() == inparams.workflow.commands.size()
            results.scheduledExecution.workflow.commands[0].adhocRemoteString == 'test command'
            if (inparams.workflow.commands[0].errorHandler) {
                results.scheduledExecution.workflow.commands[0].errorHandler.properties == inparams.workflow.commands[0].errorHandler.properties
            } else {
                results.scheduledExecution.workflow.commands[0].errorHandler == null
            }
        }
        if(expect){
            for(String prop:expect.keySet()){
                results.scheduledExecution[prop]==inparams[prop]
            }
        }

        where:
        inparams                                                                                                                                                                                                          | expect | orig
        [description: 'new job', jobName: 'monkey',
         workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', adhocExecution: true)])]                                                                                                   | [description: 'new job', jobName: 'monkey'] | [:]
        [workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', errorHandler: new CommandExec(adhocRemoteString: 'err command'))])]                                                        | [:]| [:]
        [workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test command', errorHandler: new PluginStep(keepgoingOnSuccess: true, type: 'asdf', nodeStep: true, configuration: ["blah": "value"]))])] | [:]| [:]
        [workflow: new Workflow(commands: [
                new CommandExec(adhocRemoteString: 'test command'),
                new CommandExec(adhocRemoteString: 'another command'),
        ])] | [:]                                                                                                                  | [:]
        [doNodedispatch: true, nodeIncludeName: "nodename",] |[doNodedispatch: true, nodeIncludeName: "nodename",nodeInclude:null] | [doNodedispatch: true, nodeInclude: "hostname",]
        [doNodedispatch: true, nodeInclude: "hostname",] |[doNodedispatch: true, nodeInclude: "hostname",nodeThreadcount: 3]       | [:]
        [scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true'] |
                [scheduled:true,seconds:'0',minute:'21',hour:'*/4',dayOfMonth:'*/4',month:'*/6',dayOfWeek:'?',year:'2010-2040']                                                                                                  | [:]

    }
    @Unroll
    def "do update job valid notifications"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(notifications: [new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com'),
                                                                        new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
        ])).save()
        def newJob = new ScheduledExecution(createJobParams(notifications: [new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'spaghetti@nowhere.com'),
                                                                            new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'milk@store.com')
        ]))



        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())


        then:
        !results.scheduledExecution.errors.hasErrors()
        results.success
        results.scheduledExecution.notifications.size()==2
        results.scheduledExecution.notifications.find{it.eventTrigger==ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME}.configuration==[recipients:'spaghetti@nowhere.com']
        results.scheduledExecution.notifications.find{it.eventTrigger==ScheduledExecutionController.ONFAILURE_TRIGGER_NAME}.configuration==[recipients:'milk@store.com']

    }
    @Unroll
    def "do update valid notifications"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(notifications: [new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com'),
                                                                        new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com')
        ])).save()



        when:
        def results = service._doupdate([id:se.id.toString()]+inparams, mockAuth())


        then:
        !results.scheduledExecution.errors.hasErrors()
        results.success
        results.scheduledExecution.notifications.size()==expect.size()
        for(String trig: expect.keySet()){
            results.scheduledExecution.notifications.find{it.eventTrigger==trig}.configuration==expect[trig]
        }

        where:
        inparams|expect
        [notifications:[
                [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'spaghetti@nowhere.com'],
                [eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'milk@store.com']
        ]] | [(ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [recipients: 'spaghetti@nowhere.com'], (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME): [recipients: 'milk@store.com']]

        [notifications:[
                [eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'url', content: 'http://monkey.com'],
        ]] | [(ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [url: 'http://monkey.com']]

        [notified: 'false',(ScheduledExecutionController.NOTIFY_ONSUCCESS_URL): 'true',(ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://example.com'] | [:]
        [notified: 'true',(ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://example.com'] | [:]


    }

    @Unroll
    def "do update notifications form fields"() {
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(notifications: [new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'a@example.com,z@example.com') ]
        )
        ).save()
        def params = baseJobParams() + [
                notified: 'true',
                (enablefield): 'true',
                (contentField): content,
        ]
        when:
        def results = service._doupdate([id:se.id.toString()]+params, mockAuth())

        then:
        results.success
        !results.scheduledExecution.errors.hasErrors()
        results.scheduledExecution.notifications.size()==1
        results.scheduledExecution.notifications[0].eventTrigger == trigger
        results.scheduledExecution.notifications[0].type == 'email'
        results.scheduledExecution.notifications[0].configuration == [recipients:content]

        where:
        trigger|enablefield                                             | contentField | content
        'onsuccess'|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL | ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS | 'c@example.com,d@example.com'
        'onfailure'|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL | ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS | 'c@example.com,d@example.com'
        'onstart'|ScheduledExecutionController.NOTIFY_ONSTART_EMAIL | ScheduledExecutionController.NOTIFY_START_RECIPIENTS | 'c@example.com,d@example.com'
    }
    @Unroll
    def "do update options modify"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(options:[
                new Option(name: 'test1', defaultValue: 'a', enforced: true, values: ['a', 'b', 'c']),
                new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
        ])).save()

        def params = baseJobParams()+input
        when:
        def results = service._doupdate([id:se.id.toString()]+params, mockAuth())


        then:
        !results.scheduledExecution.errors.hasErrors()
        results.success

        results.scheduledExecution.options?.size() == expectSize

        for(def i=0;i<input?.options?.size();i++){
            for(def prop:['name','defaultValue','enforced','realValuesUrl','values']){
                results.scheduledExecution.options[0]."$prop"==input.options["options[$i]"]."$prop"
            }
        }

        where:
        input|expectSize
        //modify existing options
        [options: ["options[0]": [name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"],
                   "options[1]": [name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd']]]] |  2
        //replace with a new option
        [options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]] |  1
        //remove all options
        [_nooptions: true] | null
        [_sessionopts: true, _sessionEditOPTSObject: [:] ] | null //empty session opts clears options
        //don't modify options
        [:] | 2

    }
    @Unroll
    def "do update options invalid"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(options:[
                new Option(name: 'test1', defaultValue: 'a', enforced: true, values: ['a', 'b', 'c']),
                new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
        ])).save()

        def params = baseJobParams()+input
        when:
        def results = service._doupdate([id:se.id.toString()]+params, mockAuth())


        then:
        results.scheduledExecution.errors.hasErrors()
        !results.success

        results.scheduledExecution.errors.hasFieldErrors('options')
        results.scheduledExecution.options[0].errors.hasFieldErrors('delimiter')
        !results.scheduledExecution.options[1].errors.hasErrors()
        results.scheduledExecution.options[1].delimiter=='testdelim'

        where:
        input|_
        //invalid test1 option delimiter
        [options: ["options[0]": [name: 'test1', defaultValue: 'val3', enforced: false, multivalued: true],
                   "options[1]": [name: 'test2', defaultValue: 'val2', enforced: false, values: ['a', 'b', 'c', 'd'], multivalued: true, delimiter: "testdelim"]]] |  _

    }
    def "do update job valid options"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(options:[
                new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c']),
                new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
        ])).save()
        def newJob = new ScheduledExecution(createJobParams(
                options: input
        ))

        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())


        then:
        results.success

        results.scheduledExecution.options?.size() == input?.size()

        for(def i=0;i<input?.size();i++){
            for(def prop:['name','defaultValue','enforced','realValuesUrl','values']){
                results.scheduledExecution.options[0]."$prop"==input[i]."$prop"
            }
        }

        where:
        input|_
        [new Option(name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"),
                new Option(name: 'test3', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd']),
        ] |  _
        null |  _

    }
    def "do update job nodethreadcount default 1"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(doNodedispatch: true, nodeInclude: "hostname",
                                                        nodeThreadcount: 1)).save()
        def newJob = new ScheduledExecution(createJobParams(
                doNodedispatch: true, nodeInclude: "hostname",
                nodeThreadcount: null
        ))



        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())


        then:
        results.success

        results.scheduledExecution.nodeThreadcount==1
    }

    def "do update job add error handlers verify strategy matches"() {
        "in node-first strategy, node steps cannot have workflow step error handler"
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(
                workflow: new Workflow(strategy: strategy,
                                       commands: [
                                               new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                               new CommandExec(adhocRemoteString: 'test command', adhocExecution: true),
                                               new JobExec(jobName: 'test1', jobGroup: 'test'),
                                               new JobExec(jobName: 'test1', jobGroup: 'test'),
                                       ]
                )
        )
        ).save()


        def eh1 = new CommandExec(adhocRemoteString: 'err command')
        def eh2 = new CommandExec(adhocRemoteString: 'err command')
        def eh3 = new JobExec(jobGroup: 'eh', jobName: 'eh1')
        def eh4 = new JobExec(jobGroup: 'eh', jobName: 'eh2')

        def newJob = new ScheduledExecution(createJobParams(
                workflow: new Workflow(strategy: strategy,
                                       commands: [
                                               new CommandExec(
                                                       adhocRemoteString: 'test command',
                                                       adhocExecution: true,
                                                       errorHandler: eh1
                                               ),
                                               new CommandExec(
                                                       adhocRemoteString: 'test command',
                                                       adhocExecution: true,
                                                       errorHandler: eh3
                                               ),
                                               new JobExec(jobName: 'test1', jobGroup: 'test', errorHandler: eh2),
                                               new JobExec(jobName: 'test1', jobGroup: 'test', errorHandler: eh4),
                                       ]
                )
        )
        )


        when:
        def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
        results.success == issuccess
        if (issuccess) {
            results.scheduledExecution.jobName == 'monkey'
            results.scheduledExecution.description == 'new job'
            results.scheduledExecution.workflow.commands.size() == 4
            results.scheduledExecution.workflow.commands[0].errorHandler != null
            results.scheduledExecution.workflow.commands[1].errorHandler != null
            results.scheduledExecution.workflow.commands[2].errorHandler != null
            results.scheduledExecution.workflow.commands[3].errorHandler != null
        } else {

            !results.scheduledExecution.workflow.commands[0].errors.hasErrors()
            results.scheduledExecution.workflow.commands[1].errors.hasErrors()
            results.scheduledExecution.workflow.commands[1].errors.hasFieldErrors('errorHandler')
            !results.scheduledExecution.workflow.commands[2].errors.hasErrors()
            !results.scheduledExecution.workflow.commands[3].errors.hasErrors()
        }

        where:
        strategy     | issuccess
        'step-first' | true
        'node-first' | false

    }
    def "do update cluster mode sets serverNodeUUID when enabled"(){
        given:
        def uuid=setupDoUpdate(enabled)
        def se = new ScheduledExecution(createJobParams()).save()

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (enabled?uuid:null)

        where:
        inparams            | enabled
        [jobName: 'newName']| true
        [jobName: 'newName']| false
    }
    def "do validate cluster mode sets serverNodeUUID when enabled"(){
        given:
        def uuid=setupDoValidate(enabled)
        when:
        def results = service._dovalidate(baseJobParams()+inparams, mockAuth())
        then:
        !results.failed
        results.scheduledExecution.serverNodeUUID == (enabled?uuid:null)
        where:
        inparams                                                                    | enabled
        [scheduled: true, crontabString: '0 1 1 1 * ? *', useCrontabString: 'true'] | true
        [scheduled: true, crontabString: '0 1 1 1 * ? *', useCrontabString: 'true'] | false
    }
    def "do update job cluster mode sets serverNodeUUID when enabled"(){
        given:
        def uuid=setupDoUpdate(enabled)
        def se = new ScheduledExecution(createJobParams()).save()


        def newJob = new ScheduledExecution(createJobParams(inparams))

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }


        when:
        def results = service._doupdateJob(se.id,newJob, auth)


        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (enabled?uuid:null)
        results.scheduledExecution.jobName == 'newName'

        where:
        inparams            | enabled
        [jobName: 'newName']| true
        [jobName: 'newName']| false
    }

    def "load jobs with error handlers"(){
        given:
        setupDoUpdate()
        def upload = new ScheduledExecution(
                jobName: 'testUploadErrorHandlers',
                groupPath: "testgroup",
                project: 'AProject',
                description: 'desc',
                workflow: new Workflow(commands: [
                        new CommandExec(adhocExecution: true, adhocRemoteString: "echo test",
                                        errorHandler: new CommandExec(adhocExecution: true,
                                                                      adhocRemoteString: "echo this is an errorhandler")),
                        new CommandExec(argString: "blah blah", adhocLocalString: "test2",
                                        errorHandler: new CommandExec(argString: "blah blah err",
                                                                      adhocLocalString: "test2err")),
                        new CommandExec(argString: "blah3 blah3", adhocFilepath: "test3",
                                        errorHandler: new CommandExec(argString: "blah3 blah3 err",
                                                                      adhocFilepath: "test3err")),
                        new JobExec(jobGroup: "group", jobName: "test",
                                    errorHandler: new JobExec(jobName: "testerr", jobGroup: "grouperr", argString: "line err")),

                ])
        )

        when:
        def result = service.loadJobs([upload], 'update',null, [:],  mockAuth())

        ScheduledExecution job=result.jobs[0]
        then:
        result!=null
        result.jobs!=null
        result.errjobs!=null
        result.skipjobs!=null
        result.skipjobs.size()==0
        result.errjobs.size()==0
        result.jobs.size()==1
        result.jobs[0].id!=null
        job.workflow.commands.size()==4
        for(def cmd:job.workflow.commands) {
            cmd.errorHandler!=null
            cmd.id!=null
        }
        job.workflow.commands[0] instanceof CommandExec
        job.workflow.commands[0].errorHandler instanceof CommandExec
        job.workflow.commands[1] instanceof CommandExec
        job.workflow.commands[1].errorHandler instanceof CommandExec
        job.workflow.commands[2] instanceof CommandExec
        job.workflow.commands[2].errorHandler instanceof CommandExec
        job.workflow.commands[3] instanceof JobExec
        job.workflow.commands[3].errorHandler instanceof JobExec

    }
    def "load jobs cannot load job with same uuid in different project"(){
        given:
        setupDoUpdate()
        def  uuid=UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams()+[uuid:uuid]).save()
        def upload = new ScheduledExecution(
                createJobParams() + [project: 'BProject', description: 'new desc', uuid: uuid]
        )

        when:
        def result = service.loadJobs([upload], option,uuidOption, [:],  mockAuth())

        then:
        if(success){

            result.jobs.size()==1
        }else {
            result.errjobs.size() == 1
            result.errjobs[0].scheduledExecution.errors.hasErrors()
            result.errjobs[0].scheduledExecution.errors.hasFieldErrors('uuid')
        }

        where:
        option   | uuidOption | success
        'update' | null       | false
        'update' | 'preserve' | false
        'update' | 'remove'   | true
        'create' | null       | false
        'create' | 'preserve' | false
        'create' | 'remove'   | true
    }
    @Unroll
    def "load jobs should match updated jobs based on name,group,and project"(){
        given:
        setupDoUpdate()
        def  uuid=UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams(jobName:'job1',groupPath:'path1',project:'AProject')+[uuid:uuid]).save()
        def upload = new ScheduledExecution(
                createJobParams(jobName:name,groupPath:group,project:project)
        )

        when:
        def result = service.loadJobs([upload], option,'remove', [:],  mockAuth())

        then:

        result.jobs.size()==1
        if(issame){
            result.jobs[0].id==orig.id
        }else{
            result.jobs[0].id!=orig.id
        }


        where:
        name   | group   | project    | option   | issame
        'job1' | 'path1' | 'AProject' | 'update' | true
        'job1' | 'path1' | 'AProject' | 'create' | false
        'job1' | 'path1' | 'AProject' | 'update' | false
        'job2' | 'path2' | 'AProject' | 'update' | false
        'job1' | 'path1' | 'BProject' | 'update' | false
    }
    @Unroll
    def "load jobs should update job"() {
        given:
        setupDoUpdate()
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams(origprops) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams(inparams))

        def testmap=[
                doNodedispatch: true,
                nodeThreadcount: 4,
                nodeKeepgoing: true,
                nodeExcludePrecedence: true,
                nodeInclude: 'asuka',
                nodeIncludeName: 'test',
                nodeExclude: 'testo',
                nodeExcludeTags: 'dev',
                nodeExcludeOsFamily: 'windows',
                nodeIncludeTags: 'something',
                description: 'blah'
        ]

        when:
        def result = service.loadJobs([upload], 'update', null, [:], mockAuth())

        then:

        result.jobs.size() == 1
        result.jobs[0].properties.subMap(expect.keySet()) == expect

        where:
        origprops | inparams                   | expect
        //basic fields updated
        [:]  | [description: 'milk duds'] | [description: 'milk duds']
        //remove node filters
        [doNodedispatch: true, nodeInclude: "monkey.*", nodeExcludeOsFamily: 'windows', nodeIncludeTags: 'something',]|
                [:]|
                [doNodedispatch: false, nodeInclude: null, nodeExcludeOsFamily: null, nodeIncludeTags: null,]
        //override filters
        [doNodedispatch: true, nodeInclude: "monkey.*", nodeExcludeOsFamily: 'windows', nodeIncludeTags: 'something',]|[doNodedispatch: true,
                                                                                                                        nodeThreadcount: 1,
                                                                                                                        nodeKeepgoing: true,
                                                                                                                        nodeExcludePrecedence: true,
                                                                                                                        nodeInclude: 'asuka',
                                                                                                                        nodeIncludeName: 'test',
                                                                                                                        nodeExclude: 'testo',
                                                                                                                        nodeExcludeTags: 'dev']|[doNodedispatch: true,
                                                                                                                                                 nodeThreadcount: 1,
                                                                                                                                                 nodeKeepgoing: true,
                                                                                                                                                 nodeExcludePrecedence: true,
                                                                                                                                                 nodeInclude: 'asuka',
                                                                                                                                                 nodeIncludeName: 'test',
                                                                                                                                                 nodeExclude: 'testo',
                                                                                                                                                 nodeExcludeTags: 'dev']
        //
        [doNodedispatch: true,nodeInclude: 'test',nodeThreadcount: 1] |
                [nodeThreadcount: 4,
                 nodeKeepgoing: true,
                 nodeExcludePrecedence: true,
                 nodeInclude: 'asuka',
                 nodeIncludeName: 'test',
                 nodeExclude: 'testo',
                 nodeExcludeTags: 'dev']|
                [
                        nodeThreadcount: 4,
                        nodeKeepgoing: true,
                        nodeExcludePrecedence: true,
                        nodeInclude: 'asuka',
                        nodeIncludeName: 'test',
                        nodeExclude: 'testo',
                        nodeExcludeTags: 'dev']
    }
}
