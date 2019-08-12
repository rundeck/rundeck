/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import org.apache.log4j.Logger
import rundeck.ScheduledExecutionStats

import static org.junit.Assert.*

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRoles
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategy
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.quartz.ListenerManager
import org.quartz.Scheduler
import org.quartz.core.QuartzScheduler
import org.springframework.context.MessageSource
import rundeck.Execution
import rundeck.CommandExec
import rundeck.JobExec
import rundeck.Notification
import rundeck.Option
import rundeck.PluginStep
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.WorkflowStep
import rundeck.ReferencedExecution
import rundeck.controllers.ScheduledExecutionController
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 6/24/15.
 */
@TestFor(ScheduledExecutionService)
@Mock([Workflow, ScheduledExecution, CommandExec, Notification, Option, PluginStep, JobExec,
        WorkflowStep, Execution, ReferencedExecution, ScheduledExecutionStats])
class ScheduledExecutionServiceSpec extends Specification {

    public static final String TEST_UUID1 = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C7'
    public static final String TEST_UUID2 = '490966E0-2E2F-4505-823F-E2665ADC66FB'

    def setupDoValidate(boolean enabled=false){

        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject('testProject') >> true
            isClusterModeEnabled()>>enabled
            getServerUUID()>>TEST_UUID1
            getFrameworkPropertyResolverWithProps(*_)>>Mock(PropertyResolver)
            projectNames(*_)>>[]
        }
        service.pluginService=Mock(PluginService)
        service.executionServiceBean=Mock(ExecutionService)
        service.executionUtilService=Mock(ExecutionUtilService){
            createExecutionItemForWorkflow(_)>>Mock(WorkflowExecutionItem)
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

    @Unroll
    def "should scheduleJob"() {
        given:
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }

        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> 'uuid'
            isClusterModeEnabled() >> clusterEnabled
        }
        service.jobSchedulerService=Mock(JobSchedulerService){
            determineExecNode(*_)>>{args->
                return serverNodeUUID
            }
            scheduleRemoteJob(_)>>false
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
        result == [scheduleDate, serverNodeUUID]

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled | clusterEnabled | serverNodeUUID
        true                | true            | true             | true        | true            | false          | null
        true                | true            | true             | true        | true            | true           | 'uuid'
    }

    @Unroll
    def "should not scheduleAdHocJob if no date/time"() {
        given:
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()

        when:
        service.scheduleAdHocJob(job, "user", null, Mock(Execution), [:], [:], null)

        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> executionsAreActive
        IllegalArgumentException iae = thrown()
        iae.getMessage() == "Scheduled date and time must be present"

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        true                | true            | true             | true        | true
    }

    @Unroll
    def "should not scheduleAdHocJob with time in past"() {
        given:
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }

        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b',
                        crontabString: "42 2 1 1 1 2 1999",
                        year: "1999",
                        month: "1",
                        dayOfMonth: "1",
                        hour: "1",
                        minute: "2",
                        seconds: "42"
                )
        ).save()

        Date startTime = new Date()
        startTime.set(year: 1999, month: 1, dayOfMonth: 1, hourOfDay: 1, minute: 2, seconds: 42)

        when:
        service.scheduleAdHocJob(job, "user", null, Mock(Execution), [:], [:], startTime)

        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> executionsAreActive
        IllegalArgumentException iae = thrown()
        iae.getMessage() == "Cannot schedule a job in the past"

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
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }

        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
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
        result == [null, null]

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

    def "validate workflow step log filter"() {
        given:
        def step = new CommandExec([
                adhocRemoteString: 'test buddy',
                pluginConfig     : [
                        LogFilter: [
                                [
                                        type  : 'abc',
                                        config: [a: 'b']
                                ]
                        ]
                ]]
        )
        service.pluginService = Mock(PluginService)
        service.frameworkService = Mock(FrameworkService)
        when:
        def valid = service.validateWorkflowStep(step)

        then:
        valid
        !step.hasErrors()
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: true
        ]

    }

    def "validate workflow step missing plugin type"() {
        given:
        def step = new PluginStep([
                type         : 'atype',
                configuration: [
                        a: 'b'
                ]]
        )
        service.pluginService = Mock(PluginService)
        service.frameworkService = Mock(FrameworkService)
        when:
        def valid = service.validateWorkflowStep(step)

        then:
        !valid
        step.hasErrors()
        step.errors.hasFieldErrors('type')
        service.frameworkService.getStepPluginDescription('atype') >> {
            throw new MissingProviderException('NodeStep', 'atype')
        }

    }

    def "validate workflow step log filter invalid"() {
        given:
        def step = new CommandExec([
                adhocRemoteString: 'test buddy',
                pluginConfig     : [
                        LogFilter: [
                                [
                                        type  : 'abc',
                                        config: [a: 'b']
                                ]
                        ]
                ]]
        )
        service.pluginService = Mock(PluginService)
        service.frameworkService = Mock(FrameworkService)
        when:
        def valid = service.validateWorkflowStep(step)

        then:
        !valid
        step.hasErrors()
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: 'bogus'
        ]

    }

    def "do validate step log filter"() {
        given:
        setupDoValidate()
        def params = baseJobParams()
        params.workflow.strategy = 'node-first'
        params = params + [
                '_sessionwf'          : 'true',
                '_sessionEditWFObject': new Workflow(
                        keepgoing: true,
                        strategy: 'node-first',
                        commands: [new CommandExec([
                                adhocRemoteString: 'test buddy',
                                pluginConfig     : [
                                        LogFilter: [
                                                [
                                                        type  : 'abc',
                                                        config: [a: 'b']
                                                ]
                                        ]
                                ]]
                        )]
                ),
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        !results.failed
        !results.scheduledExecution.workflow.commands[0].hasErrors()
        results.scheduledExecution.workflow.commands[0].pluginConfig.LogFilter != null
        results.scheduledExecution.workflow.commands[0].pluginConfig.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: true
        ]

    }

    def "do validate step log filter invalid"() {
        given:
        setupDoValidate()
        def params = baseJobParams()
        params.workflow.strategy = 'node-first'
        params = params + [
                '_sessionwf'          : 'true',
                '_sessionEditWFObject': new Workflow(
                        keepgoing: true,
                        strategy: 'node-first',
                        commands: [new CommandExec([
                                adhocRemoteString: 'filter test',
                                pluginConfig     : [
                                        LogFilter: [
                                                [
                                                        type  : 'abc',
                                                        config: [a: 'b']
                                                ]
                                        ]
                                ]]
                        )]
                ),
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        results.failed
        results.scheduledExecution.workflow.commands[0].hasErrors()

        results.scheduledExecution.workflow.commands[0].pluginConfig.LogFilter != null
        results.scheduledExecution.workflow.commands[0].pluginConfig.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: 'bogus'
        ]

    }
    def "do validate workflow log filters"() {
        given:
        setupDoValidate()
        def params = baseJobParams()
        params.workflow.globalLogFilters = [
                '0': [
                        type  : 'abc',
                        config: [a: 'b']
                ]
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        !results.failed
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: true
        ]

    }

    def "do validate workflow log filters invalid"() {
        given:
        setupDoValidate()
        def params = baseJobParams()
        params.workflow.globalLogFilters = [
                '0': [
                        type  : 'abc',
                        config: [a: 'b']
                ]
        ]
        when:
        def results = service._dovalidate(params, Mock(UserAndRoles))

        then:
        results.failed
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        results.scheduledExecution.errors.hasFieldErrors('workflow')
        params['logFilterValidation']["0"] == 'bogus'
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: 'bogus'
        ]

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
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }
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
        ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'c@example.com,d@example.com'
        ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'c@example.com,d@example.com'
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
        ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'user@test'
        ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'
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
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | ''
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'c@example.com d@example.com'
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'url' | ''
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'url' | 'c@example.com d@example.com'
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | ''
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'monkey@ example.com'
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'url' | ''
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'url' | 'monkey@ example.com'
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
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'c@example.com d@example.com'
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'url' | 'c@example.com d@example.com'
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'monkey@ example.com'
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'url' | 'monkey@ example.com'
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
        'onavgduration'|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL | ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS | 'c@example.com,d@example.com'
        'onretryablefailure'|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL | ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS | 'c@example.com,d@example.com'
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
        'onavgduration'|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL | ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS | 'c@example.'
        'onretryablefailure'|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL | ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS | '@example.com'
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
        service.fileUploadService = Mock(FileUploadService)
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
        service.fileUploadService = Mock(FileUploadService)
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
        service.fileUploadService = Mock(FileUploadService)
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
        service.fileUploadService = Mock(FileUploadService)
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
        service.fileUploadService = Mock(FileUploadService)
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
        results.scheduledExecution.options[0].optionValues==['val1','val2','val3'] as List
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
        service.fileUploadService = Mock(FileUploadService)
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
    def setupDoUpdate(enabled=false, serverUUID = null){
        def uuid=serverUUID?:UUID.randomUUID().toString()
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.frameworkService=Mock(FrameworkService){
            authorizeProjectJobAll(*_)>>true
            authorizeProjectResourceAll(*_)>>true
            authorizeProjectResourceAny(*_)>>true
            existsFrameworkProject('AProject')>>true
            existsFrameworkProject('BProject')>>true
            getAuthContextWithProject(_,_)>>{args->
                return args[0]
            }
            isClusterModeEnabled()>>enabled
            getServerUUID()>>uuid
            getRundeckFramework()>>Mock(Framework){
                getWorkflowStrategyService()>>Mock(WorkflowStrategyService){
                    getStrategyForWorkflow(*_)>>Mock(WorkflowStrategy)
                }
            }
            getFrameworkProject(_) >> projectMock
        }
        service.rundeckJobScheduleManager=Mock(JobScheduleManager){
            determineExecNode(*_)>>{args->
                return uuid
            }
        }
        service.executionServiceBean=Mock(ExecutionService){
            executionsAreActive()>>false
        }
        service.pluginService=Mock(PluginService)

        service.executionUtilService=Mock(ExecutionUtilService){
            createExecutionItemForWorkflow(_)>>Mock(WorkflowExecutionItem)
        }
        service.quartzScheduler = Mock(Scheduler)
        uuid
    }

    def setupDoUpdateJob(enabled = false) {
        def uuid = UUID.randomUUID().toString()

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
            getProjectProperties() >> [:]
        }
        service.frameworkService = Mock(FrameworkService) {
            _ * authorizeProjectJobAll(*_) >> true
            _ * authorizeProjectResourceAll(*_) >> true
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> projectMock
            _ * existsFrameworkProject('BProject') >> true
            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
            _ * projectNames(_ as AuthContext) >> ['AProject', 'BProject']
            _ * isClusterModeEnabled() >> enabled
            _ * getServerUUID() >> uuid
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    _ * getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy) {
                        _ * validate(_)
                    }
                }
            }
            _ * getFrameworkPropertyResolverWithProps(_, _)
        }
        service.executionServiceBean = Mock(ExecutionService) {
            _ * getExecutionsAreActive() >> false
        }
        service.pluginService = Mock(PluginService) {
            _ * validatePlugin('node-first', _ as WorkflowStrategyService, _, _)
        }

        service.executionUtilService = Mock(ExecutionUtilService) {
            _ * createExecutionItemForWorkflow(_) >> Mock(WorkflowExecutionItem) {
                _ * getWorkflow()
            }
        }
        service.quartzScheduler = Mock(Scheduler)
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
        service.fileUploadService = Mock(FileUploadService)
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }

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

    def "do update empty command"() {
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams()).save()
        def newjob = new ScheduledExecution(createJobParams(inparams))

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }

        when:
        def results = service._doupdateJob(se.id, newjob, mockAuth())


        then:
        !results.success
        results.scheduledExecution.errors.hasFieldErrors(fieldName)

        where:
        inparams | fieldName
        [workflow: new Workflow(commands: [])] | 'workflow'
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

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
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
        service.fileUploadService = Mock(FileUploadService)

        when:
        def results = service._doupdateJob(se.id,newjob, mockAuth())


        then:
        !results.success
        results.scheduledExecution.errors.hasFieldErrors('options')
        results.scheduledExecution.options[0].errors.hasFieldErrors('delimiter')
        results.scheduledExecution.options[1].errors.hasErrors() //TODO: Need to be investigated.

    }
    def "do update valid"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams(orig)).save()
        service.fileUploadService = Mock(FileUploadService)

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
        service.frameworkService.getNodeStepPluginDescription('asdf') >> Mock(Description)
        service.frameworkService.validateDescription(_, '', _, _, _, _) >> [valid: true]



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
        'onavgduration'|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL | ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS | 'c@example.com,d@example.com'
        'onretryablefailure'|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL | ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS | 'c@example.com,d@example.com'
    }
    @Unroll
    def "do update options modify"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(options:[
                new Option(name: 'test1', defaultValue: 'a', enforced: true, values: ['a', 'b', 'c']),
                new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
        ])).save()
        service.fileUploadService = Mock(FileUploadService)

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
        service.fileUploadService = Mock(FileUploadService)
        when:
        def results = service._doupdate([id:se.id.toString()]+params, mockAuth())


        then:
        results.scheduledExecution.errors.hasErrors()
        !results.success

        results.scheduledExecution.errors.hasFieldErrors('options')
        results.scheduledExecution.options[0].errors.hasFieldErrors('delimiter')
        results.scheduledExecution.options[1].errors.hasErrors() //TODO: Need to be investigated.
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
        service.fileUploadService = Mock(FileUploadService)

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

    def "do update job remove retry/timeout"() {
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(doNodedispatch: true,
                                                        nodeInclude: "hostname",
                                                        retry: '3',
                                                        timeout: '2h'
        )
        ).save()
        def newJob = new ScheduledExecution(createJobParams(
                doNodedispatch: true,
                nodeInclude: "hostname",
                nodeThreadcount: null,
                retry: null,
                timeout: null
        )
        )



        when:
        def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
        results.success

        results.scheduledExecution.retry == null
        results.scheduledExecution.timeout == null
    }

    def "do update job should replace options"() {

        def projectName = 'testProject'
        given:
        def se = new ScheduledExecution(
                jobName: 'monkey1',
                project: projectName,
                description: 'blah2',
                adhocExecution: false,
                name: 'aResource',
                type: 'aType',
                command: 'aCommand'
        )
        def opt1 = new Option(name: 'test1', defaultValue: 'val1', enforced: false, valuesUrl: "http://test.com/test")
        def opt2 = new Option(name: 'test2', defaultValue: 'val2', enforced: true, values: ['a', 'b', 'c'])
        se.addToOptions(opt1)
        se.addToOptions(opt2)
        se.save()

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
        }

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
            existsFrameworkProject(projectName) >> true
            getFrameworkProject(projectName) >> projectMock

            getRundeckFramework()>>Mock(Framework){
                getWorkflowStrategyService()>>Mock(WorkflowStrategyService){
                    getStrategyForWorkflow(*_)>>Mock(WorkflowStrategy)
                }
            }
        }
        service.fileUploadService = Mock(FileUploadService)
        service.pluginService = Mock(PluginService)
        service.executionUtilService=Mock(ExecutionUtilService){
            createExecutionItemForWorkflow(_)>>Mock(WorkflowExecutionItem)
        }



        def params = new ScheduledExecution(jobName: 'monkey1', project: projectName, description: 'blah2',
                                            workflow: new Workflow(
                                                    commands: [new CommandExec(
                                                            adhocRemoteString: 'test command',
                                                            adhocExecution: true
                                                    )]
                                            ),
                                            options: [
                                                    new Option(
                                                            name: 'test3',
                                                            defaultValue: 'val3',
                                                            enforced: false,
                                                            valuesUrl: "http://test.com/test3"
                                                    ),
                                            ]
        )
        when:
        def results = service._doupdateJob(se.id, params, null)
        def succeeded = results.success
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
            }
        }
        then:
        assertTrue succeeded
        assertNotNull(scheduledExecution)
        assertTrue(scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        assertNotNull(execution)
        assertNotNull(execution.errors)
        assertFalse(execution.errors.hasErrors())
        assertNotNull execution.options
        execution.options.size() == 1
        final Iterator iterator = execution.options.iterator()
        assert iterator.hasNext()
        final Option next = iterator.next()
        assertNotNull(next)
        assertEquals("wrong option name", "test3", next.name)
        assertEquals("wrong option name", "val3", next.defaultValue)
        assertNotNull("wrong option name", next.realValuesUrl)
        assertEquals("wrong option name", "http://test.com/test3", next.realValuesUrl.toExternalForm())
        assertFalse("wrong option name", next.enforced)
    }


    def "do update  remove retry/timeout"() {
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams([retry: '1', timeout: '2h'])).save()

        when:
        def results = service._doupdate([id: se.id.toString()], mockAuth())


        then:
        results.success
        results.scheduledExecution.retry == null
        results.scheduledExecution.timeout == null


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
        service.jobSchedulerService = Mock(JobSchedulerService)

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (enabled?uuid:null)

        where:
        inparams                               | enabled
        [jobName: 'newName']                   | true
        [jobName: 'newName']                   | false
        [jobName: 'newName', scheduled: false] | true
        [jobName: 'newName', scheduled: false] | false
    }

    @Unroll
    def "do update workflow log filters"() {
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams()).save()
        def passparams = [id: se.id.toString()] + inparams
        when:
        def results = service._doupdate(passparams, mockAuth())


        then:
        results.success


        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        !results.scheduledExecution.errors.hasFieldErrors('workflow')
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: true,
        ]
        where:
        inparams | orig | expect
        [workflow: [globalLogFilters: [
                '0': [
                        type  : 'abc',
                        config: [a: 'b']
                ]
        ]]]      | _    | _
    }

    @Unroll
    def "do update workflow log filters invalid"() {
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams()).save()
        def passparams = [id: se.id.toString()] + inparams
        when:
        def results = service._doupdate(passparams, mockAuth())


        then:
        !results.success


        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        results.scheduledExecution.errors.hasFieldErrors('workflow')
        passparams['logFilterValidation']["0"] == 'bogus'
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: 'bogus'
        ]
        where:
        inparams | orig | expect
        [workflow: [globalLogFilters: [
                '0': [
                        type  : 'abc',
                        config: [a: 'b']
                ]
        ]]]      | _    | _
    }

    @Unroll
    def "do update job workflow log filters"() {
        given:
        setupDoUpdateJob()
        def se = new ScheduledExecution(createJobParams()).save()
        def newJob = new ScheduledExecution(
                createJobParams(
                        [
                                workflow: new Workflow(
                                        keepgoing: true,
                                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])],
                                        pluginConfigMap: pluginConfigMap
                                )
                        ]
                )
        )
        def pluginService = service.pluginService
        1 * pluginService.getPluginDescriptor('abc', LogFilterPlugin) >> new DescribedPlugin(null, null, 'abc', null)
        0 * pluginService.getPluginDescriptor(_, LogFilterPlugin)
        1 * service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: true,
        ]
        0 * service.frameworkService.validateDescription(*_)
        0 * _
        when:
        def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
        results.success
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == pluginConfigMap.LogFilter
        !results.scheduledExecution.errors.hasFieldErrors('workflow')


        where:
        pluginConfigMap                                | _
        [LogFilter: [type: 'abc', config: [a: 'b']]]   | _
        [LogFilter: [[type: 'abc', config: [a: 'b']]]] | _
    }


    @Unroll
    def "do update job workflow log filters invalid"() {
        given:
        setupDoUpdateJob()
        def se = new ScheduledExecution(createJobParams()).save()
        def newJob = new ScheduledExecution(
                createJobParams(
                        [
                                workflow: new Workflow(
                                        keepgoing: true,
                                        commands: [new CommandExec([adhocRemoteString: 'test buddy'])],
                                        pluginConfigMap: pluginConfigMap
                                )
                        ]
                )
        )

        def pluginService = service.pluginService

        1 * pluginService.getPluginDescriptor('abc', LogFilterPlugin) >> new DescribedPlugin(null, null, 'abc', null)
        0 * pluginService.getPluginDescriptor(_, LogFilterPlugin)
        1 * service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: 'bogus'
        ]
        0 * service.frameworkService.validateDescription(*_)
        0 * _
        when:
        def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
        !results.success
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == pluginConfigMap.LogFilter
        results.scheduledExecution.errors.hasFieldErrors('workflow')

        where:
        pluginConfigMap                                 | _
        [LogFilter: [type: 'abc', config: [a: 'b']]]    | _
        [LogFilter: [[type: 'abc', config: [a: 'b']]]]  | _
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
        [scheduled: false]                                                          | true
        [scheduled: false]                                                          | false
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
        service.jobSchedulerService = Mock(JobSchedulerService){
            getRundeckJobScheduleManager()>>Mock(JobScheduleManager){
                determineExecNode(*_)>>{args->
                    return uuid
                }
            }
        }

        when:
        def results = service._doupdateJob(se.id,newJob, auth)


        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (enabled?uuid:null)
        results.scheduledExecution.jobName == 'newName'

        where:
        inparams                               | enabled
        [jobName: 'newName']                   | true
        [jobName: 'newName']                   | false
        [jobName: 'newName', scheduled: false] | true
        [jobName: 'newName', scheduled: false] | false
    }

    def "load jobs with error handlers"(){
        given:
        setupDoUpdate()
        service.frameworkService.authorizeProjectJobAny(_,_,_,_) >> true
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
        //scm update setup
        service.frameworkService.authorizeProjectJobAny(_,_,_,project) >> true
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
        1 * service.frameworkService.authorizeProjectJobAny(_,_,_,_) >> true
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

    def "reschedule scheduled jobs"() {
        given:
        def job1 = new ScheduledExecution(createJobParams(userRoleList: 'a,b', user: 'bob')).save()
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler)
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject(_) >> projectMock
        }
        when:
        def result = service.rescheduleJobs(null)

        then:
        job1.shouldScheduleExecution()
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        1 * service.frameworkService.getRundeckBase() >> ''
        1 * service.frameworkService.isClusterModeEnabled() >> false
        1 * service.quartzScheduler.checkExists(*_) >> false
        1 * service.quartzScheduler.scheduleJob(_, _) >> new Date()
    }

    def "reschedule adhoc executions"() {
        given:
        def job1 = new ScheduledExecution(createJobParams(userRoleList: 'a,b', user: 'bob', scheduled: false)).save()
        def exec1 = new Execution(
                scheduledExecution: job1,
                status: 'scheduled',
                dateStarted: new Date() + 2,
                dateCompleted: null,
                project: job1.project,
                user: 'bob',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        ).save(flush: true)
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler)
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject(_) >> projectMock
        }
        service.fileUploadService = Mock(FileUploadService)
        service.jobSchedulerService = Mock(JobSchedulerService)
        when:
        def result = service.rescheduleJobs(null)

        then:
        result.failedJobs.size() == 0
        result.failedExecutions.size() == 0
        result.jobs.size() == 0
        result.executions.size() == 1
        exec1 != null
        !exec1.hasErrors()
        !job1.shouldScheduleExecution()
        job1.user == 'bob'
        job1.userRoles == ['a', 'b']
        1 * service.frameworkService.getAuthContextForUserAndRolesAndProject('bob', ['a', 'b'],job1.project) >> Mock(UserAndRolesAuthContext)
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        1 * service.frameworkService.getRundeckBase() >> ''
        1 * service.jobSchedulerService.scheduleJob(_, _, _, exec1.dateStarted) >> exec1.dateStarted
    }


        def "reschedule onetime executions method"() {
        given:
        def job1 = new ScheduledExecution(createJobParams(userRoleList: 'a,b', user: 'bob', scheduled: false)).save()
        def exec1 = new Execution(
                scheduledExecution: job1,
                status: 'scheduled',
                dateStarted: new Date() + 2,
                dateCompleted: null,
                project: job1.project,
                user: 'bob',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        ).save(flush: true)
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler)
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject(_) >> projectMock
        }
        service.fileUploadService = Mock(FileUploadService)
        service.jobSchedulerService = Mock(JobSchedulerService)

        when:
        def result = service.rescheduleOnetimeExecutions(Arrays.asList(exec1))

        then:
        result.failedExecutions.size() == 0
        result.executions.size() == 1
        exec1 != null
        !exec1.hasErrors()
        !job1.shouldScheduleExecution()
        job1.user == 'bob'
        job1.userRoles == ['a', 'b']
        1 * service.frameworkService.getAuthContextForUserAndRolesAndProject('bob', ['a', 'b'],job1.project) >> Mock(UserAndRolesAuthContext)
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        1 * service.frameworkService.getRundeckBase() >> ''
        1 * service.jobSchedulerService.scheduleJob(_, _, _, exec1.dateStarted) >> exec1.dateStarted
    }

    def "reschedule adhoc execution getAuthContext error"() {
        given:
        def job1 = new ScheduledExecution(createJobParams(userRoleList: 'a,b', user: 'bob', scheduled: false)).save()
        def exec1 = new Execution(
                scheduledExecution: job1,
                status: 'scheduled',
                dateStarted: new Date() + 2,
                dateCompleted: null,
                project: job1.project,
                user: 'bob',
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
        ).save(flush: true)
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler)
        service.frameworkService = Mock(FrameworkService)
        when:
        def result = service.rescheduleJobs(null)

        then:
        result.failedJobs.size() == 0
        result.failedExecutions.size() == 1
        result.jobs.size() == 0
        result.executions.size() == 0
        exec1 != null
        !exec1.hasErrors()
        !job1.shouldScheduleExecution()
        job1.user == 'bob'
        job1.userRoles == ['a', 'b']
        1 * service.frameworkService.getAuthContextForUserAndRolesAndProject('bob', ['a', 'b'],job1.project) >> {
            throw new RuntimeException("getAuthContextForUserAndRoles failure")
        }
        0 * service.executionServiceBean.getExecutionsAreActive() >> true
        0 * service.frameworkService.getRundeckBase() >> ''
        0 * service.quartzScheduler.scheduleJob(_, _) >> new Date()
    }
    def "update execution flags change node ownership"() {
        given:
        setupDoValidate(true)
        def uuid = setupDoUpdate(true)

        def se = new ScheduledExecution(createJobParams()).save()
        service.jobSchedulerService=Mock(JobSchedulerService)
        when:
        def params = baseJobParams()+[

        ]
        //def results = service._dovalidate(params, Mock(UserAndRoles))
        def results = service._doUpdateExecutionFlags(
                [id: se.id.toString(), executionEnabled: executionEnabled, scheduleEnabled: scheduleEnabled],
                null,
                null,
                null,
                null,
                null
        )
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution
        if(scheduleEnabled != null && executionEnabled != null){
            scheduledExecution.serverNodeUUID == uuid
        }else{
            scheduledExecution.serverNodeUUID == null
        }

        where:
        scheduleEnabled | executionEnabled
        true            | true
        null            | false
        false           | true
        true            | null
        null            | null
    }

    def 'getJobIdent'() {
        given:
        def job = isjob ? new ScheduledExecution(createJobParams(jobName: 'ajobname',
                                                                 project: 'AProject',
                                                                 groupPath: 'some/path',
                                                                 scheduled: jobscheduled
        )
        ).save() : null
        def exec = new Execution(
                scheduledExecution: job,
                status: estatus,
                dateStarted: new Date(),
                dateCompleted: null,
                project: job?.project ?: 'testproject',
                user: 'bob',
                executionType: etype,
                workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")]),
                retryAttempt: retry
        ).save(flush: true)
        def id = exec.id
        when:
        def result = service.getJobIdent(job, exec)

        then:
        result.jobname == jobname.replaceAll('_ID_', "$id").replaceAll('_JID_', "${job?.id}")
        result.groupname == groupname.replaceAll('_ID_', "$id").replaceAll('_JID_', "${job?.id}")

        where:
        isjob | estatus     | jobscheduled | etype            |retry| jobname               | groupname
        false | null        | null         | 'user'           |0| 'TEMP:bob:_ID_'       | 'bob:run'
        true  | null        | false        | 'user'           |0| 'TEMP:bob:_JID_:_ID_' | 'bob:run:_JID_'
        true  | null        | true         | 'user'           |0| 'TEMP:bob:_JID_:_ID_' | 'bob:run:_JID_'
        true  | 'scheduled' | true         | 'user'           |0| 'TEMP:bob:_JID_:_ID_' | 'bob:run:_JID_'
        true  | 'running'   | true         | 'user'           |0| 'TEMP:bob:_JID_:_ID_' | 'bob:run:_JID_'
        true  | null        | true         | 'scheduled'      |0| '_JID_:ajobname'      | 'AProject:ajobname:some/path'
        true  | 'scheduled' | true         | 'scheduled'      |0| '_JID_:ajobname'      | 'AProject:ajobname:some/path'
        true  | 'running'   | true         | 'scheduled'      |0| '_JID_:ajobname'      | 'AProject:ajobname:some/path'
        true  | 'running'   | true         | 'scheduled'      |1| 'TEMP:bob:_JID_:_ID_' | 'bob:run:_JID_'
        true  | 'scheduled' | true         | 'user-scheduled' |0| 'TEMP:bob:_JID_:_ID_' | 'bob:run:_JID_'
        true  | 'running'   | true         | 'user-scheduled' |0| 'TEMP:bob:_JID_:_ID_' | 'bob:run:_JID_'

    }

    @Unroll
    def "interrupt job"() {
        given:
        def name = 'a'
        def group = 'b'
        service.quartzScheduler = Mock(Scheduler)

        when:
        def result = service.interruptJob(instanceId, name, group, unschedule)

        then:
        result == expect
        if (instanceId) {
            1 * service.quartzScheduler.interrupt(instanceId) >> success
        }
        if (unschedule && !success) {
            1 * service.quartzScheduler.deleteJob(_) >> diddelete
        }

        where:
        instanceId | unschedule | success | diddelete | expect
        null       | true       | false   | true      | true
        null       | true       | false   | false     | false
        null       | false      | _       | _         | false
        'xyz'      | true       | true    | true      | true
        'xyz'      | true       | false   | false     | false
        'xyz'      | false      | true    | true      | true
        'xyz'      | false      | false   | false     | false
    }


    def "timezone validations on save"() {
        given:
        setupDoValidate()
        def params = baseJobParams() +[scheduled: true,
                                       crontabString: '0 1 2 3 4 ? *',
                                       useCrontabString: 'true',
                                        timeZone: timezone]
        when:

        def results = service._dovalidate(params, mockAuth())

        then:

        results.failed == expectFailed

        where:
        timezone    | expectFailed
        null        | false
        ''          | false
        'America/Los_Angeles'   |false
        'GMT-8:00'  | false
        'PST'       | false
        'XXXX'      |true
        'AAmerica/Los_Angeles' | true

    }

    def "timezone validations on update"(){
        given:
        setupDoUpdate()
        def params = baseJobParams() +[scheduled: true,
                                       crontabString: '0 1 2 3 4 ? *',
                                       useCrontabString: 'true',
                                       timeZone: timezone]
        def se = new ScheduledExecution(createJobParams()).save()
        service.fileUploadService = Mock(FileUploadService)

        when:
        def results = service._doupdate([id: se.id.toString()] + params, mockAuth())

        then:
        results.success == expectSuccess

        where:
        timezone    | expectSuccess
        null        | true
        ''          | true
        'America/Los_Angeles'   |true
        'GMT-8:00'  | true
        'PST'       | true
        'XXXX'      |false
        'AAmerica/Los_Angeles' | false
    }

    @Unroll
    def "scheduleJob with or without TimeZone shouldn't fail"() {
        given:
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }
        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
            getFrameworkProject('AProject') >> projectMock
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b',
                        crontabString: '0 0 10 0 0 ? *',
                        useCrontabString: 'true',
                        timeZone: timezone
                )
        ).save()
        def scheduleDate = new Date()

        when:
        def result = service.scheduleJob(job, null, null, true)

        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> executionsAreActive
        1 * service.quartzScheduler.scheduleJob(_, _) >> scheduleDate
        result == [scheduleDate, null]

        where:
        executionsAreActive | timezone
        true                | 'America/Los_Angeles'
        true                | null
        true                | ''
    }



    def "project passive mode execution"() {
        given:
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> ['project.disable.executions':property]
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject(_) >> projectMock
        }
        when:
        def result = service.isProjectExecutionEnabled(null)

        then:
        null != result
        result == expect

        where:
        property   | expect
        null       | true
        ''         | true
        'true'     | false
        'false'    | true
    }
    def "isRundeckProjectExecutionEnabled"() {
        given:
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> ['project.disable.executions':property]
        }
        when:
        def result = service.isRundeckProjectExecutionEnabled(projectMock)

        then:
        null != result
        result == expect

        where:
        property   | expect
        null       | true
        ''         | true
        'true'     | false
        'false'    | true
    }

    def "project passive mode schedule"() {
        given:
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> ['project.disable.schedule':property]
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject(_) >> projectMock
        }
        when:
        def result = service.isProjectScheduledEnabled(null)

        then:
        null != result
        result == expect

        where:
        property   | expect
        null       | true
        ''         | true
        'true'     | false
        'false'    | true
    }

    def "isRundeckProjectScheduleEnabled"() {
        given:
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> ['project.disable.schedule':property]
        }
        when:
        def result = service.isRundeckProjectScheduleEnabled(projectMock)

        then:
        null != result
        result == expect

        where:
        property   | expect
        null       | true
        ''         | true
        'true'     | false
        'false'    | true
    }

    def "project passive mode should schedule"() {
        given:
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> ['project.disable.schedule':disableSchedule,
                                       'project.disable.executions':disableExecution]
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject(_) >> projectMock
        }
        when:
        def result = service.shouldScheduleInThisProject('proj')

        then:
        null != result
        result == expect

        where:
        disableSchedule   |disableExecution   | expect
        null              |null               | true
        ''                |''                 | true
        'true'            |'true'             | false
        'true'            |'false'            | false
        'false'           |'false'            | true
        'false'           |'true'             | false



    }

    @Unroll
    def "nextExecutionTime on remote Cluster"() {
        given:
        setupDoValidate(true)
        service.quartzScheduler = Mock(Scheduler)
        service.quartzScheduler.getTrigger(_) >> null

        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b',
                        serverNodeUUID: TEST_UUID2
                )
        ).save()

        when:
        def result = service.nextExecutionTime(job)

        then:
        if(expectScheduled){
            result != null
        }else{
            result == null
        }


        where:
        scheduleEnabled | executionEnabled | hasSchedule | expectScheduled
        true            | true             | true        | true
        false           | true             | true        | false
        true            | false            | true        | false
        false           | false            | true        | false
    }

    @Unroll
    def "do save job with dynamic threadcount"(){
        given:
        setupDoUpdate()

        def job = new ScheduledExecution(createJobParams(doNodedispatch: true,
                nodeInclude: "hostname",
                nodeThreadcountDynamic: "\${option.threadcount}",
                retry: null,
                timeout: null,
                options:[new Option(name: 'threadcount', defaultValue: '30', enforced: true)]
                ))

        when:
        job.save()
        then:
        job.nodeThreadcount == 30
    }

    @Unroll
    def "do update job dynamic nodethreadcount"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(doNodedispatch: true, nodeInclude: "hostname",
                nodeThreadcountDynamic: "\${option.threadcount}",
                options:[new Option(name: 'threadcount', defaultValue: '30', enforced: true)]
        )).save()
        def newJob = new ScheduledExecution(createJobParams(
                doNodedispatch: true, nodeInclude: "hostname",
                nodeThreadcount: null,
                nodeThreadcountDynamic: null
        ))



        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())

        then:
        results.success
        results.scheduledExecution.nodeThreadcountDynamic=="\${option.threadcount}"
    }

    def "do update job options with label field"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(options:[
                new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c']),
                new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
        ])).save()
        def newJob = new ScheduledExecution(createJobParams(
                options: input
        ))
        service.fileUploadService = Mock(FileUploadService)

        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())


        then:
        results.success

        results.scheduledExecution.options?.size() == input?.size()

        for(def i=0;i<input?.size();i++){
            for(def prop:['name','label']){
                results.scheduledExecution.options[0]."$prop"==input[i]."$prop"
            }
        }

        where:
        input|_
        [new Option(name: 'test1', label: 'label1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"),
         new Option(name: 'test3', label: 'label2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd']),
        ] |  _
        null |  _

    }



    @Unroll
    def "do update job on cluster"(){
        given:
        def serverUuid = '8527d81a-49cd-42e3-a853-43b956b77600'
        def jobOwnerUuid = '5e0e96a0-042a-426a-80a4-488f7f6a4f13'
        def uuid=setupDoUpdate(true, serverUuid)
        def se = new ScheduledExecution(createJobParams([serverNodeUUID:jobOwnerUuid])).save()
        service.jobSchedulerService = Mock(JobSchedulerService)

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (shouldChange?serverUuid:jobOwnerUuid)
        if(shouldChange) {
            1 * service.jobSchedulerService.updateScheduleOwner(_, _, _) >> true
        }

        where:
        inparams                                        | shouldChange
        [jobName: 'newName']                            | false
        [jobName: 'newName', scheduled: false]          | true
        [jobName: 'newName', scheduled: true]           | false
        [jobName: 'newName', timeZone: 'GMT+1']         | true
        [jobName: 'newName', dayOfMonth: '10']          | true
        [jobName: 'newName', scheduleEnabled: true]     | false
        [jobName: 'newName', scheduleEnabled: false]    | true
        [jobName: 'newName', executionEnabled: true]     | false
        [jobName: 'newName', executionEnabled: false]    | true

    }


    @Unroll
    def "do update list of jobs on cluster"(){
        given:
        def serverUUID = '802d38a5-0cd1-44b3-91ff-824d495f8105'
        def currentOwner = '05b604ed-9a1e-4cb4-8def-b17a071afec9'
        def uuid = setupDoUpdate(true,serverUUID)
        service.jobSchedulerService = Mock(JobSchedulerService){
            getRundeckJobScheduleManager()>>Mock(JobScheduleManager){
                determineExecNode(*_)>>{args->
                    return uuid
                }
            }
        }

        def orig = [serverNodeUUID: currentOwner]

        def se = new ScheduledExecution(createJobParams(orig)).save()
        def newJob = new ScheduledExecution(createJobParams(inparams)).save()
        service.frameworkService.getNodeStepPluginDescription('asdf') >> Mock(Description)
        service.frameworkService.validateDescription(_, '', _, _, _, _) >> [valid: true]

        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())

        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (shouldChange?serverUUID:currentOwner)
        if(shouldChange) {
            1 * service.jobSchedulerService.updateScheduleOwner(_, _, _) >> true
        }

        where:
        inparams                                        | shouldChange
        [jobName: 'newName']                            | false
        [jobName: 'newName', scheduled: false]          | true
        [jobName: 'newName', scheduled: true]           | false
        [jobName: 'newName', timeZone: 'GMT+1']         | true
        [jobName: 'newName', dayOfMonth: '10']          | true
        [jobName: 'newName', scheduleEnabled: true]     | false
        [jobName: 'newName', scheduleEnabled: false]    | true
        [jobName: 'newName', executionEnabled: true]     | false
        [jobName: 'newName', executionEnabled: false]    | true
    }

    @Unroll
    def "scm update job using right update or scm_update permission"() {
        given:
        setupDoUpdate()
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams([:]) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams([description: 'milk duds']))

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
        def result = service.loadJobs([upload], 'update', null, [method: 'scm-import'], mockAuth())

        then:

        result.jobs.size() == 1
        result.jobs[0].properties.description == 'milk duds'
        1 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE, AuthConstants.SCM_UPDATE],_) >> true
    }

    def "not check scm_update permission if isnt a scm-import"() {
        given:
        setupDoUpdate()
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams([:]) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams([description: 'milk duds']))

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
        def result = service.loadJobs([upload], 'update', null, [method: 'x'], mockAuth())

        then:

        result.jobs.size() == 1
        result.jobs[0].properties.description == 'milk duds'
        1 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE],_) >> true
        0 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE, AuthConstants.SCM_UPDATE],_) >> true
    }

    @Unroll
    def "scm update job without update or scm_update permission"() {
        given:
        setupDoUpdate()
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams([:]) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams([description: 'milk duds']))

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
        def result = service.loadJobs([upload], 'update', null, [method: 'scm-import'], mockAuth())

        then:

        result.jobs.size() == 0
        result.errjobs.size() == 1
        result.errjobs[0].errmsg.startsWith("Unauthorized: Update Job")
        1 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE, AuthConstants.SCM_UPDATE],_) >> false
    }


    @Unroll
    def "scm delete scheduledExecution By Id"(){
        given:
        setupDoUpdate()
        service.frameworkService.authorizeProjectResource(*_)>>false
        service.fileUploadService = Mock(FileUploadService)
        service.jobSchedulerService = Mock(JobSchedulerService)
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams([:]) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams([description: 'milk duds'])).save()

        when:
        def result = service.deleteScheduledExecutionById(upload.id, mockAuth(), false, 'user', 'scm-import')

        then:
        result
        result.success?.job
        1 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_DELETE,AuthConstants.SCM_DELETE],_) >> true
    }

    @Unroll
    def "not scm delete scheduledExecution By Id"(){
        given:
        setupDoUpdate()
        service.frameworkService.authorizeProjectResource(*_)>>false
        service.fileUploadService = Mock(FileUploadService)
        service.jobSchedulerService = Mock(JobSchedulerService)
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams([:]) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams([description: 'milk duds'])).save()

        when:
        def result = service.deleteScheduledExecutionById(upload.id, mockAuth(), false, 'user', 'non-scm')

        then:
        result
        !result.sucess
        result.error
        0 * service.frameworkService.authorizeProjectJobAll(_,_,
                [AuthConstants.SCM_DELETE],_) >> true
    }

    @Unroll
    def "scm create jobs using scm_create"(){
        given:
        setupDoUpdate()
        //scm create setup

        def  uuid=UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams(jobName:'job1',groupPath:'path1',project:'AProject')+[uuid:uuid]).save()
        def upload = new ScheduledExecution(
                createJobParams(jobName:'job1',groupPath:'path1',project:'AProject')
        )

        when:
        def result = service.loadJobs([upload], 'create','remove', [method: 'scm-import'],  mockAuth())

        then:
        result.jobs.size()==1
        1 * service.frameworkService.authorizeProjectResourceAny(_,AuthConstants.RESOURCE_TYPE_JOB,
                [AuthConstants.ACTION_CREATE,AuthConstants.SCM_CREATE],'AProject') >> true
        1 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_CREATE,AuthConstants.SCM_CREATE],_) >> true

    }

    @Unroll
    def "scm create jobs not using scm_create"(){
        given:
        setupDoUpdate()
        //scm create setup

        def  uuid=UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams(jobName:'job1',groupPath:'path1',project:'AProject')+[uuid:uuid]).save()
        def upload = new ScheduledExecution(
                createJobParams(jobName:'job1',groupPath:'path1',project:'AProject')
        )

        when:
        def result = service.loadJobs([upload], 'create','remove', [method: 'create'],  mockAuth())

        then:
        result.jobs.size()==1
        1 * service.frameworkService.authorizeProjectResourceAny(_,AuthConstants.RESOURCE_TYPE_JOB,
                [AuthConstants.ACTION_CREATE],'AProject') >> true
        0 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.SCM_CREATE],_) >> false
        1 * service.frameworkService.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_CREATE],_) >> true

    }

    def "blank email notification attached options"() {
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
                                configuration     : [recipients : 'a@example.com,z@example.com', attachLog: true]
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
        scheduledExecution.errors.hasFieldErrors(ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH)

    }


    def "log change on flags change"() {
        given:
        def jobChangeLogger = Mock(Logger)
        setupDoValidate(true)
        def uuid = setupDoUpdate(true)

        def se = new ScheduledExecution(createJobParams()).save()
        service.jobSchedulerService=Mock(JobSchedulerService)
        and:
        service.jobChangeLogger = jobChangeLogger
        def expectedLog = user+' MODIFY [1] AProject "some/where/blue" (update)'
        when:
        def params = baseJobParams()+[

        ]
        //def results = service._dovalidate(params, Mock(UserAndRoles))
        def results = service._doUpdateExecutionFlags(
                [id: se.id.toString(), executionEnabled: executionEnabled, scheduleEnabled: scheduleEnabled],
                null,
                null,
                null,
                null,
                [method: 'update', change: 'modify', user: user]
        )


        then:
        1 * service.jobChangeLogger.info(expectedLog)

        where:
        scheduleEnabled | executionEnabled  | user
        true            | true              | 'admin'
        null            | false             | 'test'
        false           | true              | 'dev'
        true            | null              | 'qa'
        null            | null              |' user'
    }


    @Unroll
    def "cluster, should not scheduleJob because the remote policy"() {
        given:
        service.executionServiceBean = Mock(ExecutionService)
        service.quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        def projectMock = Mock(IRundeckProject) {
            getProjectProperties() >> [:]
        }

        def serverNodeUUID = "uuid"
        def clusterEnabled = true

        service.frameworkService = Mock(FrameworkService) {
            getRundeckBase() >> ''
            getFrameworkProject(_) >> projectMock
            getServerUUID() >> serverNodeUUID
            isClusterModeEnabled() >> clusterEnabled
        }
        service.jobSchedulerService=Mock(JobSchedulerService){
            determineExecNode(*_)>>{args->
                return serverNodeUUID
            }
            scheduleRemoteJob(_)>>true
        }
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b'
                )
        ).save()

        when:
        def result = service.scheduleJob(job, null, null)

        then:
        0 * service.quartzScheduler.scheduleJob(_, _)
        result == [null, null]
    }

}
