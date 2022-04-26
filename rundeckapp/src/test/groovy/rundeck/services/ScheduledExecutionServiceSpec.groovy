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

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.plugins.JobLifecyclePluginException
import com.dtolabs.rundeck.core.schedule.SchedulesManager
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.quartz.SchedulerException
import org.rundeck.app.authorization.AppAuthContextProcessor
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.components.jobs.ImportedJob
import org.rundeck.app.components.jobs.JobQuery
import org.rundeck.app.components.jobs.JobQueryInput
import org.rundeck.app.components.schedule.TriggerBuilderHelper
import org.rundeck.app.components.schedule.TriggersExtender
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.schedule.JobScheduleManager
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import org.springframework.context.ConfigurableApplicationContext
import rundeck.Orchestrator
import org.slf4j.Logger
import rundeck.ScheduledExecutionStats
import rundeck.User
import testhelper.RundeckHibernateSpec

import static org.junit.Assert.*

import com.dtolabs.rundeck.core.authorization.AuthContext
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
import org.quartz.ListenerManager
import org.quartz.Scheduler
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
import spock.lang.Unroll

/**
 * Created by greg on 6/24/15.
 */
class ScheduledExecutionServiceSpec extends RundeckHibernateSpec implements ServiceUnitTest<ScheduledExecutionService> {

    public static final String TEST_UUID1 = 'BB27B7BB-4F13-44B7-B64B-D2435E2DD8C7'

    List<Class> getDomainClasses() { [Workflow, ScheduledExecution, CommandExec, Notification, Option, PluginStep, JobExec,
                                      WorkflowStep, Execution, ReferencedExecution, ScheduledExecutionStats, Orchestrator, User] }

    def setupSchedulerService(clusterEnabled = false){
        SchedulesManager rundeckJobSchedulesManager = new LocalJobSchedulesManager()
        rundeckJobSchedulesManager.frameworkService = Mock(FrameworkService){
            getRundeckBase() >> ''
            getServerUUID() >> 'uuid'
            isClusterModeEnabled() >> clusterEnabled
        }
        def quartzScheduler = Mock(Scheduler) {
            getListenerManager() >> Mock(ListenerManager)
        }
        rundeckJobSchedulesManager.quartzScheduler = quartzScheduler
        service.quartzScheduler = quartzScheduler
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> true
        }
    }

    def setupDoValidate(boolean enabled=false){

        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject('testProject') >> true
            isClusterModeEnabled()>>enabled
            getServerUUID()>>TEST_UUID1
            getFrameworkPropertyResolverWithProps(*_)>>Mock(PropertyResolver)
            projectNames(*_)>>[]
            getFrameworkNodeName() >> "testProject"
        }
        service.pluginService=Mock(PluginService)
        service.executionServiceBean=Mock(ExecutionService)
        service.executionUtilService=Mock(ExecutionUtilService){
            createExecutionItemForWorkflow(_)>>Mock(WorkflowExecutionItem)
        }
        service.jobLifecyclePluginService = Mock(JobLifecyclePluginService)
        service.executionLifecyclePluginService = Mock(ExecutionLifecyclePluginService)
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
            updateJob(_,_,_)>>{
                RundeckJobDefinitionManager.importedJob(it[0],[:])
            }
            validateImportedJob(_)>>new RundeckJobDefinitionManager.ReportSet(valid: true, validations:[:])
        }
        TEST_UUID1
    }
    def "blank email notification"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                workflow      : new Workflow(
                        threadcount: 1,
                        keepgoing: true,
                        commands: [new CommandExec(adhocRemoteString: 'a remote string')]
                ),
                (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL):'true',
                (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS):'',

        ]
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'auser'
            getRoles()>>(['a','b'] as Set)
        }
        when:

        def results = service._dovalidate(params, authContext)

        then:
        def ScheduledExecution scheduledExecution = results.scheduledExecution

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
                (ScheduledExecutionController.NOTIFY_SUCCESS_URL):'',
                (ScheduledExecutionController.NOTIFY_ONSUCCESS_URL):'true',

        ]
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'auser'
            getRoles()>>(['a','b'] as Set)
        }
        def results = service._dovalidate(params, authContext)
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
        setupSchedulerService(clusterEnabled)
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

        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: hasSchedule,
                        scheduleEnabled: scheduleEnabled,
                        executionEnabled: executionEnabled,
                        userRoleList: 'a,b'
                )
        ).save()
//        def scheduleDate = new Date()
        def data=["project": job.project,
                  "jobId":job.uuid,
                  "oldQuartzJobName": originalJobName,
                  "oldQuartzGroupName": originalGroupName]

        service.jobSchedulerService=Mock(JobSchedulerService){
            determineExecNode(*_)>>{args->
                return serverNodeUUID
            }
            scheduleRemoteJob(data)>>false
        }

        when:
        def result = service.scheduleJob(job, originalJobName, originalGroupName, false, remoteAssgined)

        then:
        1 * service.executionServiceBean.getExecutionsAreActive() >> executionsAreActive
        (remoteAssgined ? 0 : 1) * service.jobSchedulesService.handleScheduleDefinitions(_, _) >> [nextTime: scheduleDate]
        result == [scheduleDate, serverNodeUUID]

        where:
        executionsAreActive | scheduleEnabled | executionEnabled | hasSchedule | expectScheduled | clusterEnabled | serverNodeUUID | remoteAssgined | scheduleDate | originalJobName | originalGroupName
        true                | true            | true             | true        | true            | false          | null           | false          | new Date()   | null            | null
        true                | true            | true             | true        | true            | true           | 'uuid'         | false          | new Date()   | null            | null
        true                | true            | true             | true        | true            | false          | null           | false          | new Date()   | "aJobName"      | "aGroupName"
        true                | true            | true             | true        | true            | true           | 'uuid'         | false          | new Date()   | "aJobName"      | "aGroupName"
        true                | true            | true             | true        | true            | false          | null           | false          | null         | null            | null
        true                | true            | true             | true        | true            | true           | null           | true           | null         | null            | null
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
        service.scheduleAdHocJob(job, "user", null, Mock(Execution), [:], [:], null, false)

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
        service.scheduleAdHocJob(job, "user", null, Mock(Execution), [:], [:], startTime, false)

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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> (executionsAreActive && scheduleEnabled && executionEnabled && hasSchedule)
        }
        service.jobSchedulerService = Mock(JobSchedulerService){
            scheduleRemoteJob(_) >> false
        }
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
        0 * service.jobSchedulesService.handleScheduleDefinitions(_, _)
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
                      workflow: new Workflow([threadcount: 1, keepgoing: true, commands: new CommandExec(cmd)]),
        ]
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'auser'
            getRoles()>>(['a','b'] as Set)
        }
        when:
        def results = service._dovalidate(params, authContext)

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
                _sessionEditWFObject: new Workflow(threadcount: 1, keepgoing: true, commands:cmds),
        ]
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'auser'
            getRoles()>>(['a','b'] as Set)
        }
        when:
        def results = service._dovalidate(params, authContext)

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
        [new CommandExec(adhocExecution: true, adhocRemoteString: "do something"),
         new CommandExec(adhocExecution: true, adhocLocalString: "test dodah"),
          new JobExec(jobName: 'test1', jobGroup: 'a/test')] | _

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
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'auser'
            getRoles()>>(['a','b'] as Set)
        }
        when:
        def results = service._dovalidate(params, authContext)

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

            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

        then:
        results.failed
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        results.scheduledExecution.errors.hasFieldErrors('workflow')
        params['logFilterValidation']["0"]!=null
        params['logFilterValidation']["0"] instanceof Validator.Report
        !params['logFilterValidation']["0"].valid
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: Validator.errorReport('a','bogus')
        ]

    }

    def "parse job plugins params"() {
        given:
            def params = [keys: keys] + other
        when:
            def result = ScheduledExecutionService.parseExecutionLifecyclePluginsParams(params)
        then:
            result
            result.service == ServiceNameConstants.ExecutionLifecycle
            result.pluginProviderConfigs!=null
            result.pluginProviderConfigs.size()==expectSize
            if(expectSize>0) {
                result.pluginProviderConfigs.find { it.provider == 'aType' } != null
                result.pluginProviderConfigs.find { it.provider == 'aType' }.configuration == [a: 'b']
            }
            if(expectSize>1){
                result.pluginProviderConfigs.find{it.provider=='bType'}!=null
                result.pluginProviderConfigs.find{it.provider=='bType'}.configuration==[b:'c']
            }

        where:
            keys              | other  | expectSize
            'asdf'            | [type: [asdf: 'aType'], asdf: [configMap: [a:'b']], enabled:[asdf:'true']] | 1
            'asdf'            | [type: [asdf: 'aType'], asdf: [configMap: [a:'b',z:'']], enabled:[asdf:'true']] | 1
            'asdf'            | [type: [asdf: 'aType'], asdf: [configMap: [a:'b',_z:'asdf']], enabled:[asdf:'true']] | 1
            'asdf'            | [type: [asdf: 'aType'], asdf: [configMap: [a:'b']], enabled:[asdf:'false']] | 0
            ['asdf']          | [type: [asdf: 'aType'], asdf: [configMap: [a:'b']], enabled:[asdf:'true']] | 1
            ['asdf']          | [type: [asdf: 'aType'], asdf: [configMap: [a:'b']], enabled:[asdf:'false']] | 0
            ['asdf', 'asdf2'] | [type: [asdf: 'aType', asdf2: 'bType'], asdf: [configMap: [a:'b']], asdf2: [configMap: [b:'c']], enabled:[asdf:'true']] | 1
            ['asdf', 'asdf2'] | [type: [asdf: 'aType', asdf2: 'bType'], asdf: [configMap: [a:'b']], asdf2: [configMap: [b:'c']], enabled:[asdf:'true',asdf2:'false']] | 1
            ['asdf', 'asdf2'] | [type: [asdf: 'aType', asdf2: 'bType'], asdf: [configMap: [a:'b']], asdf2: [configMap: [b:'c']], enabled:[asdf:'true',asdf2:'true']] | 2

    }

    def "do validate job plugins"() {
        given:
            setupDoValidate()
            def params = baseJobParams()
            params.executionLifecyclePlugins = [
                    keys:['a','b'],
                    enabled:['a':'true','b':'true'],
                    type:[a:'aType',b:'bType'],
                    'a': [
                            configMap: [a: 'b']
                    ],
                    'b': [
                            configMap: [b: 'c']
                    ]
            ]
            service.frameworkService.getFrameworkProject(_)>>Mock(IRundeckProject){
                getProperties()>>[:]
            }

            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

        then:
            1 * service.pluginService.validatePluginConfig('aType', ExecutionLifecyclePlugin,'AProject',[a:'b'])>>new ValidatedPlugin(valid:true)
            1 * service.pluginService.validatePluginConfig('bType', ExecutionLifecyclePlugin,'AProject',[b:'c'])>>new ValidatedPlugin(valid:true)
            1 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(_)>>{
                PluginConfigSet.with ServiceNameConstants.ExecutionLifecycle, [
                        SimplePluginConfiguration.builder().provider('aType').configuration([a:'b']).build(),
                        SimplePluginConfiguration.builder().provider('bType').configuration([b:'c']).build(),
                ]
            }
            1 * service.executionLifecyclePluginService.setExecutionLifecyclePluginConfigSetForJob(_,_)
            !results.failed

    }
    def "do validate job plugins invalid"() {
        given:
            setupDoValidate()
            def params = baseJobParams()
            params.executionLifecyclePlugins = [
                    keys:['a','b'],
                    enabled:['a':'true','b':'true'],
                    type:[a:'aType',b:'bType'],
                    'a': [
                            configMap: [a: 'b']
                    ],
                    'b': [
                            configMap: [b: 'c']
                    ]
            ]
            service.frameworkService.getFrameworkProject(_)>>Mock(IRundeckProject){
                getProperties()>>[:]
            }
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

        then:
            1 * service.pluginService.validatePluginConfig('aType',ExecutionLifecyclePlugin,'AProject',[a:'b'])>>new ValidatedPlugin(valid:false,report:Validator.errorReport('a','wrong'))
            1 * service.pluginService.validatePluginConfig('bType',ExecutionLifecyclePlugin,'AProject',[b:'c'])>>new ValidatedPlugin(valid:true)
            1 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(_)>>{
                PluginConfigSet.with ServiceNameConstants.ExecutionLifecycle, [
                        SimplePluginConfiguration.builder().provider('aType').configuration([a:'b']).build(),
                        SimplePluginConfiguration.builder().provider('bType').configuration([b:'c']).build(),
                ]
            }
            1 * service.executionLifecyclePluginService.setExecutionLifecyclePluginConfigSetForJob(_,_)
            results.failed
            results.scheduledExecution.errors.hasFieldErrors('pluginConfig')
    }
    @Unroll
    def "do validate node-first strategy error handlers"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                _sessionEditWFObject: new Workflow([threadcount: 1, keepgoing: true, strategy: strategy]+cmds),
        ]
        params.workflow.strategy=strategy
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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
                _sessionEditWFObject: new Workflow([threadcount: 1, keepgoing: true, strategy: 'sequential', commands: [new CommandExec(cmd)]]),
        ]
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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
            def authContext = Mock(UserAndRolesAuthContext){
                getUsername()>>'auser'
                getRoles()>>(['a','b'] as Set)
            }
        when:
            def results = service._dovalidate(params, authContext)

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

    @Unroll
    def "validate notifications email data for #trigger"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                (field):content,
                (flag):'true',

        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.notifications
        results.scheduledExecution.notifications.size()==1
        results.scheduledExecution.notifications[0].eventTrigger == trigger
        results.scheduledExecution.notifications[0].type == type
        results.scheduledExecution.notifications[0].configuration == [recipients:content]

        where:
        trigger                                             | type    | content | field | flag
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.com,d@example.com'|ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'c@example.com,d@example.com'|ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL
        ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'c@example.com,d@example.com'|ScheduledExecutionController.NOTIFY_START_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONSTART_EMAIL
        ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'c@example.com,d@example.com'|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL
        ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'c@example.com,d@example.com'|ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL
    }
    @Unroll
    def "validate notifications email data any domain #trigger for #content"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                (field):content,
                (flag):'true',
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        !results.failed
        results.scheduledExecution.notifications[0].eventTrigger == trigger
        results.scheduledExecution.notifications[0].type == type
        results.scheduledExecution.notifications[0].configuration == [recipients:content]

        where:
        trigger                                             | type    | content|field|flag
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd'|ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL
        ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | '${job.user.name}@something.org'|ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'|ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL
        ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | '${job.user.email}'|ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL
        ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'monkey@internal'|ScheduledExecutionController.NOTIFY_START_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONSTART_EMAIL
        ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'user@test'|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL
        ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'example@any.domain'|ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL
    }
    @Unroll
    def "invalid notifications data"() {
        given:
        setupDoValidate()
        def params = baseJobParams()+[
                      (contentField):content,
                      (flag):'true'
        ]
        when:
        def results = service._dovalidate(params, mockAuth())

        then:
        results.failed
        results.scheduledExecution.errors.hasErrors()
        results.scheduledExecution.errors.hasFieldErrors(contentField)

        where:
        contentField|trigger                                             | type    | content  |flag
        ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | ''|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL
        ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'email' | 'c@example.comd@example.com'|ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL
        ScheduledExecutionController.NOTIFY_SUCCESS_URL|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'url' | ''|ScheduledExecutionController.NOTIFY_ONSUCCESS_URL
        ScheduledExecutionController.NOTIFY_SUCCESS_URL|ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME | 'url' | 'c@example.comd@example.com'|ScheduledExecutionController.NOTIFY_ONSUCCESS_URL
        ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | ''|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL
        ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'email' | 'monkey@ example.com'|ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL
        ScheduledExecutionController.NOTIFY_FAILURE_URL|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'url' | ''|ScheduledExecutionController.NOTIFY_ONFAILURE_URL
        ScheduledExecutionController.NOTIFY_FAILURE_URL|ScheduledExecutionController.ONFAILURE_TRIGGER_NAME | 'url' | 'monkey@ example.com'|ScheduledExecutionController.NOTIFY_ONFAILURE_URL
        ScheduledExecutionController.NOTIFY_START_RECIPIENTS|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | ''|ScheduledExecutionController.NOTIFY_ONSTART_EMAIL
        ScheduledExecutionController.NOTIFY_START_RECIPIENTS|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'email' | 'c@example.com d@example.com'|ScheduledExecutionController.NOTIFY_ONSTART_EMAIL
        ScheduledExecutionController.NOTIFY_START_URL|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'url' | ''|ScheduledExecutionController.NOTIFY_ONSTART_URL
        ScheduledExecutionController.NOTIFY_START_URL|ScheduledExecutionController.ONSTART_TRIGGER_NAME   | 'url' | 'c@example.com d@example.com'|ScheduledExecutionController.NOTIFY_ONSTART_URL
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | ''|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_RECIPIENTS|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'email' | 'c@example.com d@example.com'|ScheduledExecutionController.NOTIFY_OVERAVGDURATION_EMAIL
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'url' | ''|ScheduledExecutionController.NOTIFY_ONOVERAVGDURATION_URL
        ScheduledExecutionController.NOTIFY_OVERAVGDURATION_URL|ScheduledExecutionController.OVERAVGDURATION_TRIGGER_NAME   | 'url' | 'c@example.com d@example.com'|ScheduledExecutionController.NOTIFY_ONOVERAVGDURATION_URL
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | ''|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_RECIPIENTS|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'email' | 'monkey@ example.com'|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_EMAIL
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'url' | ''|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_URL
        ScheduledExecutionController.NOTIFY_RETRYABLEFAILURE_URL|ScheduledExecutionController.ONRETRYABLEFAILURE_TRIGGER_NAME | 'url' | 'monkey@ example.com'|ScheduledExecutionController.NOTIFY_ONRETRYABLEFAILURE_URL
    }
    @Unroll
    def "do update job invalid notifications"() {
        given:
        setupDoUpdateJob()
        def se = new ScheduledExecution(createJobParams()).save()
        def newjob = new ScheduledExecution(createJobParams()).save()
        newjob.addToNotifications(new Notification(
                                        eventTrigger: trigger,
                                        type: type,
                                        content: content
                                ))
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newjob, associations: [:])
        service.jobSchedulesService = Mock(SchedulesManager)

        when:
        def results = service._doupdateJob(se.id,importedJob, mockAuth())

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
        results.scheduledExecution.crontabString
        results.scheduledExecution.seconds=='0'
        results.scheduledExecution.minute=='1'
        results.scheduledExecution.hour=='2'
        results.scheduledExecution.dayOfMonth=='3'
        results.scheduledExecution.month=='4'
        results.scheduledExecution.dayOfWeek=='?'
        results.scheduledExecution.year=='*'
    }

    private Map<String, Object> baseJobParams() {
        [jobName : 'monkey1', project: 'AProject', description: 'blah',
                workflow:[threadcount: 1, keepgoing: true,strategy: 'sequential'],
         _sessionwf:'true',
         _sessionEditWFObject: new Workflow(threadcount: 1, keepgoing: true, commands:[ new CommandExec(adhocExecution: true, adhocRemoteString: 'test command')]),
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
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeProjectJobAll(*_)>>true
            authorizeProjectResourceAll(*_)>>true
            authorizeProjectResourceAny(*_)>>true
            authorizeProjectJobAny(_,_,['update'],_)>>true
            getAuthContextWithProject(_,_)>>{args->
                return args[0]
            }
        }
        service.frameworkService=Mock(FrameworkService){
            existsFrameworkProject('AProject')>>true
            existsFrameworkProject('BProject')>>true
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
        service.executionLifecyclePluginService = Mock(ExecutionLifecyclePluginService)
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager){
            updateJob(_,_,_)>>{ RundeckJobDefinitionManager.importedJob(it[0],it[1]?.associations)}
            validateImportedJob(_)>>new RundeckJobDefinitionManager.ReportSet(valid:true, validations:[:])
        }
        uuid
    }

    def setupDoUpdateJob(enabled = false) {
        def uuid = UUID.randomUUID().toString()

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
            getProjectProperties() >> [:]
            _*_
        }
        service.frameworkService = Mock(FrameworkService) {
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> projectMock
            _ * existsFrameworkProject('BProject') >> true
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
            _ * frameworkNodeName () >> null
            _ * getFrameworkPropertyResolverWithProps(_, _)
            _ * filterNodeSet(*_) >> null
        }

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            _ * authorizeProjectJobAll(*_) >> true
            _ * authorizeProjectResourceAll(*_) >> true
            _ * filterAuthorizedNodes(*_) >> null
            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
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
        service.executionLifecyclePluginService = Mock(ExecutionLifecyclePluginService)
        service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager){
            updateJob(_,_,_)>>{
                RundeckJobDefinitionManager.importedJob(it[0],it[1]?.associations?:[:])
            }
            validateImportedJob(_)>>new RundeckJobDefinitionManager.ReportSet(valid:true, validations:[:])
        }
        uuid
    }

    private UserAndRolesAuthContext mockAuth() {
        Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }
    }

    @Unroll
    def "do update invalid"(){
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams(orig)).save()
        service.fileUploadService = Mock(FileUploadService)
        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }
        service.jobSchedulesService = Mock(SchedulesManager)

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
        [ workflow: new Workflow([threadcount: 1, keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocRemoteString: '')]])]|'workflow' | [:]
        //required option must have default when job is scheduled
        [scheduled: true,
         options: ["options[0]": [name: 'test', required:true, enforced: false, ]],
         crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true'] | 'options' | [:]
        //existing option job now scheduled
        [scheduled: true, crontabString: '0 21 */4 */4 */6 ? 2010-2040', useCrontabString: 'true',options:[new Option(name: 'test', required:true, enforced: false)]] | 'options' | [options:[new Option(name: 'test', required:true, enforced: false)]]
    }

    def "do update empty command"() {
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams()).save()
        def newjob = new ScheduledExecution(createJobParams(inparams))
        def importedJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newjob, associations: [:])

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }
        service.jobSchedulesService = Mock(SchedulesManager)

        when:
        def results = service._doupdateJob(se.id, importedJob, mockAuth())


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
        newjob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newjob, associations: [:])

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
            getRoles() >> new HashSet<String>(['test'])
        }

        service.messageSource = Mock(MessageSource) {
            getMessage(_, _) >> { it[0].toString() }
        }
        service.jobSchedulesService = Mock(SchedulesManager)
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
        service.jobSchedulesService = Mock(SchedulesManager)
        newjob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newjob, associations: [:])
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
        '0 0 2 ? 12 1975'               | 'will never fire'
    }

    @Unroll("doupdate invalid crontab value for #reason")
    def "do update invalid crontab"(){
        given:
            setupDoUpdate()
            def se = new ScheduledExecution(createJobParams()).save()
            def params = [id: se.id, scheduled: true, crontabString: crontabString, useCrontabString: 'true']
            service.jobSchedulesService = Mock(SchedulesManager)
        when:
            def results = service._doupdate(params, mockAuth())


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
            '0 0 2 ? 12 1975'               | 'will never fire'
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
        newjob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newjob, associations: [:])
        service.jobSchedulesService = Mock(SchedulesManager)
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
        setupSchedulerService()
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
        [ workflow: new Workflow([threadcount: 1, keepgoing: true, strategy:'node-first', commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test local')]])]|[:]                                                                                                                                                                                         |[:]
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
        setupSchedulerService(false)
        def se = new ScheduledExecution(createJobParams(orig)).save()

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        results.scheduledExecution.workflow.strategy==inparams._sessionEditWFObject.strategy
        results.scheduledExecution.workflow.keepgoing==inparams._sessionEditWFObject.keepgoing in [true,'true']
        if(inparams._sessionEditWFObject.threadcount) {
            results.scheduledExecution.workflow.threadcount == inparams._sessionEditWFObject.threadcount
        }else{
            results.scheduledExecution.workflow.threadcount == 1
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
        ['_sessionwf':'true', '_sessionEditWFObject': new Workflow([threadcount: 1, keepgoing: true, strategy:'node-first', commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test local')]])] |
                [:] |
                [[adhocLocalString:'test local']]
        ['_sessionwf':'true', '_sessionEditWFObject': new Workflow([threadcount: 1, keepgoing: false, strategy:'step-first', commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test command2')]])]                                                  |
                [:]                                                                                                                                                                                                                       |
                [[adhocRemoteString: 'test command2']]
        ['_sessionwf':'true', '_sessionEditWFObject': new Workflow(strategy: 'step-first', keepgoing: false, commands: [new CommandExec(adhocExecution: true, adhocLocalString: 'test command2')])]                                                                                                                                                                    | [:] | []
        //update via session workflow
        ['_sessionwf':'true', '_sessionEditWFObject':new Workflow(keepgoing: true, strategy: 'node-first', commands: [new CommandExec([adhocRemoteString: 'test buddy'])])] |
                [:] |
                [[adhocRemoteString: 'test buddy']]
    }

    @Unroll
    def "do update job valid"(){
        given:
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(orig)).save()
        def newJob = new ScheduledExecution(createJobParams(inparams))
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
        service.frameworkService.getNodeStepPluginDescription('asdf') >> Mock(Description)
        service.frameworkService.validateDescription(_, '', _, _, _, _) >> [valid: true]
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> newJob.job.scheduled
        }


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
        setupSchedulerService()
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams()).save();
        se.addToNotifications(new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com'))
        se.addToNotifications(new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com'))

        def newJob = new ScheduledExecution(createJobParams())
        newJob.addToNotifications(new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'spaghetti@nowhere.com'))
        newJob.addToNotifications(new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'milk@store.com'))

        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])



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
        setupSchedulerService(false)
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams()).save();
        se.addToNotifications(new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'c@example.com,d@example.com'))
        se.addToNotifications(new Notification(eventTrigger: ScheduledExecutionController.ONFAILURE_TRIGGER_NAME, type: 'email', content: 'monkey@example.com'))

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
        [
                (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL):'true',
                (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS):'spaghetti@nowhere.com',
                (ScheduledExecutionController.NOTIFY_ONFAILURE_EMAIL):'true',
                (ScheduledExecutionController.NOTIFY_FAILURE_RECIPIENTS):'milk@store.com',
                ] | [(ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [recipients: 'spaghetti@nowhere.com'], (ScheduledExecutionController.ONFAILURE_TRIGGER_NAME): [recipients: 'milk@store.com']]

        [

                (ScheduledExecutionController.NOTIFY_ONSUCCESS_URL):'true',
                (ScheduledExecutionController.NOTIFY_SUCCESS_URL):'http://monkey.com',

        ] | [(ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME): [url: 'http://monkey.com']]

        [notified: 'false',(ScheduledExecutionController.NOTIFY_ONSUCCESS_URL): 'true',(ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://example.com'] | [:]
        [notified: 'true',(ScheduledExecutionController.NOTIFY_SUCCESS_URL): 'http://example.com'] | [:]


    }

    @Unroll
    def "do update notifications form fields"() {
        given:
        setupSchedulerService(false)
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams()).save()
        se.addToNotifications(new Notification(eventTrigger: ScheduledExecutionController.ONSUCCESS_TRIGGER_NAME, type: 'email', content: 'a@example.com,z@example.com'))

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
        setupSchedulerService(false)
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
        results.scheduledExecution.errors.hasErrors() == !isSuccess
        results.success == isSuccess

        results.scheduledExecution.options?.size() == expectSize

        def inputOptions = input?.options ?: input?._sessionEditOPTSObject

        for(def i=0;i<inputOptions?.size();i++){
            for(def prop:['name','defaultValue','enforced','realValuesUrl','values']){
                results.scheduledExecution.options[i]."$prop"==inputOptions["options[$i]"]."$prop"
            }
        }

        where:
        input|expectSize|isSuccess
        //modify existing options
        [options: ["options[0]": [name: 'test1', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"],
                   "options[1]": [name: 'test2', defaultValue: 'd', enforced: true, values: ['a', 'b', 'c', 'd']]]] |  2 | true
        //replace with a new option
        [options: ["options[0]": [name: 'test3', defaultValue: 'val3', enforced: false, valuesUrl: "http://test.com/test3"]]] |  1  | true
        //remove all options
        [_sessionopts: true, _sessionEditOPTSObject: [:] ] | null | true //empty session opts clears options
        [_sessionopts: true, _sessionEditOPTSObject: ['options[0]': new Option(name: 'test1', defaultValue: 'val3Changed', enforced: false, valuesUrl: new URL("http://test.com/test3"))], useCrontabString: 'true',crontabString: "X 48 09 ? * * *" ] | 1 | false
        //don't modify options
//        [:] | 2 | true

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

        service.jobSchedulesService = Mock(SchedulesManager)
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
        setupSchedulerService()
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(options:[
                new Option(name: 'test1', defaultValue: 'val1', enforced: true, values: ['a', 'b', 'c']),
                new Option(name: 'test2', enforced: false, valuesUrl: "http://test.com/test2")
        ])).save()
        def newJob = new ScheduledExecution(createJobParams(
                options: input
        ))
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
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
        setupSchedulerService()
        setupDoUpdate()

        def se = new ScheduledExecution(createJobParams(doNodedispatch: true, nodeInclude: "hostname",
                                                        nodeThreadcount: 1)).save()
        def newJob = new ScheduledExecution(createJobParams(
                doNodedispatch: true, nodeInclude: "hostname",
                nodeThreadcount: null
        ))
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])



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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])



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
        setupSchedulerService()
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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }

        def projectMock = Mock(IRundeckProject) {
            getProperties() >> [:]
        }

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
            existsFrameworkProject(projectName) >> true
            getFrameworkProject(projectName) >> projectMock

            getRundeckFramework() >> Mock(Framework) {
                getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy)
                }
            }
        }
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeProjectJobAny(_,_,['update'],_)>>true
        }
        service.fileUploadService = Mock(FileUploadService)
        service.pluginService = Mock(PluginService)
        service.executionUtilService=Mock(ExecutionUtilService){
            createExecutionItemForWorkflow(_)>>Mock(WorkflowExecutionItem)
        }
        service.executionLifecyclePluginService = Mock(ExecutionLifecyclePluginService)
        service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager){
            updateJob(_,_,_)>>{ RundeckJobDefinitionManager.importedJob(it[0],it[1]?.associations)}
            validateImportedJob(_)>>new RundeckJobDefinitionManager.ReportSet(valid: true,validations:[:])

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
        params = new RundeckJobDefinitionManager.ImportedJobDefinition(job:params, associations: [:])
        when:
        def results = service._doupdateJob(se.id, params, mockAuth())
        def scheduledExecution = results.scheduledExecution
        if (scheduledExecution && scheduledExecution.errors.hasErrors()) {
            scheduledExecution.errors.allErrors.each {
            }
        }
        then:
        !results.scheduledExecution.errors.hasErrors()
        results.success
        scheduledExecution!=null
        (scheduledExecution instanceof ScheduledExecution)
        final ScheduledExecution execution = scheduledExecution
        null!=(execution)
        null!=(execution.errors)
        assertFalse(execution.errors.hasErrors())
        null!= execution.options
        execution.options.size() == 1
        final Iterator iterator = execution.options.iterator()
        assert iterator.hasNext()
        final Option next = iterator.next()
        null!=(next)
        next.name=="test3"
        next.defaultValue=="val3"
        null!= next.realValuesUrl
        next.realValuesUrl.toExternalForm()=="http://test.com/test3"
        !next.enforced
    }


    def "do update  remove retry/timeout"() {
        given:
        setupDoUpdate()
        def se = new ScheduledExecution(createJobParams([retry: '1', timeout: '2h'])).save()
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }
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
        setupSchedulerService()
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
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])


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
        setupSchedulerService(enabled)
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
        setupSchedulerService()
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
        service.jobSchedulesService = Mock(SchedulesManager)
        when:
        def results = service._doupdate(passparams, mockAuth())


        then:
        !results.success


        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == [
                [config: [a: 'b'], type: 'abc']
        ]
        results.scheduledExecution.errors.hasFieldErrors('workflow')
        passparams['logFilterValidation']["0"] !=null
        passparams['logFilterValidation']["0"] instanceof Validator.Report
        !passparams['logFilterValidation']["0"].valid
        1 * service.pluginService.getPluginDescriptor('abc', LogFilterPlugin) >>
                new DescribedPlugin(null, null, 'abc', null)
        service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: Validator.errorReport('a','bogus')
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
        setupSchedulerService()
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
        service.jobSchedulesService = Mock(SchedulesManager){
            1 * isScheduled(_)
            1 * shouldScheduleExecution(_)
        }
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
        def pluginService = service.pluginService
        1 * pluginService.getPluginDescriptor('abc', LogFilterPlugin) >> new DescribedPlugin(null, null, 'abc', null)
        0 * pluginService.getPluginDescriptor(_, LogFilterPlugin)
        1 * service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: true,
        ]
        0 * service.frameworkService.validateDescription(*_)
        0 * service.jobLifecyclePluginService.beforeJobSave(_,_)
        1 * service.frameworkService.getFrameworkNodeName()
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,['update'],'AProject')>>true
        2 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(_)
        1 * service.executionLifecyclePluginService.setExecutionLifecyclePluginConfigSetForJob(_,_)
        1 * service.rundeckJobDefinitionManager.persistComponents(_,_)
        1 * service.rundeckJobDefinitionManager.waspersisted(_,_)
        0 * _
        when:
        def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
        !results.scheduledExecution.errors.hasErrors()
        results.success
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == expect
        !results.scheduledExecution.errors.hasFieldErrors('workflow')


        where:
        pluginConfigMap                                | expect
        [LogFilter: [type: 'abc', config: [a: 'b']]]   | [[type: 'abc', config: [a: 'b']]]
        [LogFilter: [[type: 'abc', config: [a: 'b']]]] | [[type: 'abc', config: [a: 'b']]]
    }

    @Unroll
    def "do update job job plugin valid"() {
        given:
            setupSchedulerService()
            setupDoUpdateJob()
            def se = new ScheduledExecution(createJobParams()).save()
            def newJob = new ScheduledExecution(createJobParams())
            newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
            def pluginService = service.pluginService
            0 * pluginService.getPluginDescriptor(_, LogFilterPlugin)
            def pluginConfigSet = PluginConfigSet.with(
                    ServiceNameConstants.ExecutionLifecycle,
                    [
                            SimplePluginConfiguration.builder().
                                    provider('aPlugin').
                                    configuration([some: 'config']).
                                    build()
                    ]
            )
            1 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(newJob.job) >> pluginConfigSet
            1 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(se) >> pluginConfigSet
            1 * service.frameworkService.getFrameworkNodeName()
            1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,['update'],_)>>true
            0 * service.jobLifecyclePluginService.beforeJobSave(_,_)
            1 * service.rundeckJobDefinitionManager.persistComponents(_,_)
            1 * service.rundeckJobDefinitionManager.waspersisted(_,_)
            service.jobSchedulesService = Mock(SchedulesManager){
                1 * isScheduled(_)
                1 * shouldScheduleExecution(_)
            }
            0 * _
        when:
            def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
            results.success

            1 * service.pluginService.validatePluginConfig('aPlugin', ExecutionLifecyclePlugin, 'AProject', [some: 'config']) >>
            new ValidatedPlugin(valid: true)
            1 * service.executionLifecyclePluginService.setExecutionLifecyclePluginConfigSetForJob(_, _)

    }

    @Unroll
    def "do update job job plugin invalid"() {
        given:
            setupDoUpdateJob()
            def se = new ScheduledExecution(createJobParams()).save()
            def newJob = new ScheduledExecution(createJobParams())
            newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
            def pluginService = service.pluginService
            0 * pluginService.getPluginDescriptor(_, LogFilterPlugin)
            def configSet = PluginConfigSet.with(
                    ServiceNameConstants.ExecutionLifecycle,
                    [
                            SimplePluginConfiguration.builder().
                                    provider('aPlugin').
                                    configuration([some: 'config']).
                                    build()
                    ]
            )
            1 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(newJob.job) >> configSet
            1 * service.executionLifecyclePluginService.setExecutionLifecyclePluginConfigSetForJob(_, _)
            1 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(se) >> configSet
            0 * service.frameworkService.getFrameworkNodeName()
            0 * service.jobLifecyclePluginService.beforeJobSave(_,_)
            0 * service.rundeckJobDefinitionManager.persistComponents(_,_)>>true
            service.jobSchedulesService = Mock(SchedulesManager){
                1 * isScheduled(_)
            }
            0 * _
        when:
            def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
            !results.success

            1 * service.pluginService.validatePluginConfig('aPlugin', ExecutionLifecyclePlugin, 'AProject', [some: 'config']) >>
            new ValidatedPlugin(valid: false,report: Validator.errorReport('a','wrong'))

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
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])

        def pluginService = service.pluginService

        1 * pluginService.getPluginDescriptor('abc', LogFilterPlugin) >> new DescribedPlugin(null, null, 'abc', null)
        0 * pluginService.getPluginDescriptor(_, LogFilterPlugin)
        1 * service.frameworkService.validateDescription(_, '', [a: 'b'], _, _, _) >> [
                valid: false, report: Validator.errorReport('a','wrong')
        ]
        0 * service.frameworkService.validateDescription(*_)
        0 * service.jobLifecyclePluginService.beforeJobSave(_,_)
        0 * service.frameworkService.getFrameworkNodeName()
        2 * service.executionLifecyclePluginService.getExecutionLifecyclePluginConfigSetForJob(_)
        1 * service.executionLifecyclePluginService.setExecutionLifecyclePluginConfigSetForJob(_,_)

        0 * service.rundeckJobDefinitionManager.persistComponents(newJob,_)
        service.jobSchedulesService = Mock(SchedulesManager){
            1 * isScheduled(_)
        }
        0 * _
        when:
        def results = service._doupdateJob(se.id, newJob, mockAuth())


        then:
        !results.success
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter != null
        results.scheduledExecution.workflow.pluginConfigMap.LogFilter == expect
        results.scheduledExecution.errors.hasFieldErrors('workflow')

        where:
        pluginConfigMap                                 | expect
        [LogFilter: [type: 'abc', config: [a: 'b']]]    | [[type: 'abc', config: [a: 'b']]]
        [LogFilter: [[type: 'abc', config: [a: 'b']]]]  | [[type: 'abc', config: [a: 'b']]]
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
        setupSchedulerService()
        def uuid=setupDoUpdate(enabled)
        def se = new ScheduledExecution(createJobParams()).save()


        def newJob = new ScheduledExecution(createJobParams(inparams))
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])

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
        service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,_,_) >> true
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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> upload.scheduled
        }

            upload = new RundeckJobDefinitionManager.ImportedJobDefinition(job:upload, associations: [:])
        service.rundeckJobDefinitionManager.validateImportedJob(upload)>>true
        when:
        def result = service.loadImportedJobs([upload], 'update',null, [:],  mockAuth())

        then:
        result!=null
        result.jobs!=null
        result.errjobs!=null
        result.skipjobs!=null
        result.skipjobs.size()==0
        result.errjobs.size()==0
        result.jobs.size()==1
        result.jobs[0].id!=null
        ScheduledExecution job=result.jobs[0]
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
        service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,_,project) >> true
        def  uuid=UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams(jobName:'job1',groupPath:'path1',project:'AProject')+[uuid:uuid]).save()
        def upload = new ScheduledExecution(
                createJobParams(jobName:name,groupPath:group,project:project)
        )
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> upload.scheduled
        }
        upload = new RundeckJobDefinitionManager.ImportedJobDefinition(job:upload, associations: [:])

        service.rundeckJobDefinitionManager.validateImportedJob(upload)>>true
        when:
        def result = service.loadImportedJobs([upload], option,'remove', [:],  mockAuth())

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
    def "load jobs should set user and roles"(){
        given:
        setupDoUpdate()
        //scm update setup
        service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,_,project) >> true
        def  uuid=UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams(jobName:'job1',groupPath:'path1',project:'AProject')+[uuid:uuid]).save()
        def upload = new ScheduledExecution(
                createJobParams(jobName:name,groupPath:group,project:project,scheduled:false)
        )
        upload = new RundeckJobDefinitionManager.ImportedJobDefinition(job:upload, associations: [:])

        service.rundeckJobDefinitionManager.validateImportedJob(upload)>>true
        service.jobSchedulesService=Mock(JobSchedulesService){
            1 * shouldScheduleExecution(_)>>true
        }
        when:
        def result = service.loadImportedJobs([upload], option,'remove', [:],  mockAuth())

        then:

        result.jobs.size()==1
        result.jobs[0].user=='test'
        result.jobs[0].userRoles==['test']


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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> upload.scheduled
        }

        when:
        def result = service.loadJobs([upload], 'update', null, [:], mockAuth())

        then:

        result.jobs.size() == 1
        result.jobs[0].properties.subMap(expect.keySet()) == expect
        2 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,_,_) >> true
        where:
        origprops | inparams                   | expect
        //basic fields updated
        [:]  | [description: 'milk duds'] | [description: 'milk duds']
        //remove node filters
        [doNodedispatch: true, filter: 'something',]|
                [:]|
                [doNodedispatch: false, filter: null,]
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
                                                                                                                                                 nodeInclude: null,
                                                                                                                                                 nodeIncludeName: null,
                                                                                                                                                 nodeExclude: null,
                                                                                                                                                 nodeExcludeTags: null]
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
                        nodeInclude: null,
                        nodeIncludeName: null,
                        nodeExclude: null,
                        nodeExcludeTags: null]
    }

    def "load jobs cluster mode should set server UUID"(){
        given:
            def  serverUUID=UUID.randomUUID().toString()
            setupDoUpdate(true,serverUUID)
            service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,_,'AProject') >> true

            def  uuid=UUID.randomUUID().toString()
            def orig = new ScheduledExecution(createJobParams(jobName:'job1',groupPath:'path1',project:'AProject',scheduled:false)+[uuid:uuid]).save()
            def upload = new ScheduledExecution(
                    createJobParams(jobName:'job1',groupPath:'path1',project:'AProject',uuid:uuid,scheduled:false)
            )
            service.jobSchedulesService=Mock(JobSchedulesService){
                1 * shouldScheduleExecution(uuid)>>true
            }
            upload = new RundeckJobDefinitionManager.ImportedJobDefinition(job:upload, associations: [:])

            service.rundeckJobDefinitionManager.validateImportedJob(upload)>>true
        when:
            def result = service.loadImportedJobs([upload], 'update',null, [:],  mockAuth())

        then:
            result.jobs.size()==1
            result.jobs[0].serverNodeUUID==serverUUID
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
        service.jobSchedulesService = Mock(JobSchedulesService){
            getAllScheduled(_,_) >> [job1]
            shouldScheduleExecution(_) >> job1.shouldScheduleExecution()
        }
        when:
        def result = service.rescheduleJobs(null)

        then:
        job1.shouldScheduleExecution()
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        1 * service.frameworkService.getRundeckBase() >> ''
        1 * service.frameworkService.isClusterModeEnabled() >> false
        1 * service.quartzScheduler.checkExists(*_) >> false
        1 * service.jobSchedulesService.handleScheduleDefinitions(_, _) >> new Date()
    }

    def "reschedule adhoc executions"() {
        given:
        setupSchedulerService(false)
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
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * service.rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject('bob', ['a', 'b'],job1.project) >> Mock(UserAndRolesAuthContext)
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        1 * service.frameworkService.getRundeckBase() >> ''
        1 * service.jobSchedulerService.scheduleJob(_, _, _, exec1.dateStarted, false) >> exec1.dateStarted
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
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * service.rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject('bob', ['a', 'b'],job1.project) >> Mock(UserAndRolesAuthContext)
        1 * service.executionServiceBean.getExecutionsAreActive() >> true
        1 * service.frameworkService.getRundeckBase() >> ''
        1 * service.jobSchedulerService.scheduleJob(_, _, _, exec1.dateStarted, false) >> exec1.dateStarted
    }

    def "reschedule adhoc execution getAuthContext error"() {
        given:
        setupSchedulerService()
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
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor)
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
        1 * service.rundeckAuthContextProcessor.getAuthContextForUserAndRolesAndProject('bob', ['a', 'b'],job1.project) >> {
            throw new RuntimeException("getAuthContextForUserAndRoles failure")
        }
        0 * service.executionServiceBean.getExecutionsAreActive() >> true
        0 * service.frameworkService.getRundeckBase() >> ''
        0 * service.jobSchedulesService.handleScheduleDefinitions(_, _) >> new Date()
    }
    def "update execution flags change node ownership"() {
        given:
        setupDoValidate(true)
        def uuid = setupDoUpdate(true)

        def se = new ScheduledExecution(createJobParams()).save()
        service.jobSchedulerService=Mock(JobSchedulerService)
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }
        when:
        def params = baseJobParams()+[

        ]
        //def results = service._dovalidate(params, Mock(UserAndRolesAuthContext))
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


    @Unroll
    def "timezone validations on save for #timezone"() {
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
            timezone               | expectFailed
            null                   | false
            ''                     | false
            'America/Los_Angeles'  | false
            'GMT-08:00'            | false
            'GMT-8:00'             | false
            'PST'                  | false
            'XXXX'                 | true
            'AAmerica/Los_Angeles' | true

    }

    def "timezone validations on update"(){
        given:
        setupSchedulerService()
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
        setupSchedulerService(false)
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
        1 * service.jobSchedulesService.handleScheduleDefinitions(_, _) >> [nextTime:scheduleDate]
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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])



        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())

        then:
        results.success
        results.scheduledExecution.nodeThreadcountDynamic==null
        results.scheduledExecution.nodeThreadcount==1
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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
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
        setupSchedulerService(true)
        def serverUuid = '8527d81a-49cd-42e3-a853-43b956b77600'
        def jobOwnerUuid = '5e0e96a0-042a-426a-80a4-488f7f6a4f13'
        def uuid=setupDoUpdate(true, serverUuid)

        service.jobSchedulerService = Mock(JobSchedulerService)
        service.executionServiceBean=Mock(ExecutionService){
            getExecutionsAreActive()>>true
        }

        service.quartzScheduler = Mock(Scheduler){
            checkExists(_) >> false
        }

        def se = new ScheduledExecution(createJobParams([serverNodeUUID:jobOwnerUuid])).save()
        service.jobSchedulerService = Mock(JobSchedulerService)

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (shouldChange?serverUuid:jobOwnerUuid)
        if(shouldChange) {
            1 * service.jobSchedulerService.updateScheduleOwner(_) >> true
            if(inparams.scheduled && inparams.scheduleEnabled){
                1 * service.jobSchedulesService.handleScheduleDefinitions(_, _)
            }
        }

        where:
        inparams                                                                         | shouldChange
        [jobName: 'newName', scheduled: true, scheduleEnabled: true]                     | false
        [jobName: 'newName', scheduled: false, scheduleEnabled: true]                    | true
        [jobName: 'newName', scheduled: true, scheduleEnabled: true]                     | false
        [jobName: 'newName', timeZone: 'GMT+1', scheduled: true,scheduleEnabled: true]   | true
        [jobName: 'newName', dayOfMonth: '10', scheduled: true,scheduleEnabled: true]    | true
        [jobName: 'newName', scheduleEnabled: true, scheduled: true]                     | false
        [jobName: 'newName', scheduleEnabled: false, scheduled: true]                    | true
        [jobName: 'newName', executionEnabled: true, scheduled: true]                    | false
        [jobName: 'newName', executionEnabled: false, scheduled: true]                   | true

    }

    @Unroll
    def "do update job with job lifecycle plugin, nominal"(){
        given:
        setupSchedulerService()
        def serverUuid = '8527d81a-49cd-42e3-a853-43b956b77600'
        def jobOwnerUuid = '5e0e96a0-042a-426a-80a4-488f7f6a4f13'
        def uuid=setupDoUpdate(true, serverUuid)
        def se = new ScheduledExecution(createJobParams([serverNodeUUID:jobOwnerUuid])).save()
        service.jobSchedulerService = Mock(JobSchedulerService)
        service.jobLifecyclePluginService=Mock(JobLifecyclePluginService)

        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())


        then:
        results.success
        1 * service.jobLifecyclePluginService.beforeJobSave(se,_)>>lfresult

        where:
        inparams                                        | lfresult
        [jobName: 'newName']                            | null
        [jobName: 'newName']                            | Mock(JobLifecycleStatus){ isSuccessful()>>true }
    }

    @Unroll
    def "do update with job lifecycle plugin, error thrown"(){
        given:
        def serverUuid = '8527d81a-49cd-42e3-a853-43b956b77600'
        def jobOwnerUuid = '5e0e96a0-042a-426a-80a4-488f7f6a4f13'
        def uuid=setupDoUpdate(true, serverUuid)
        def se = new ScheduledExecution(createJobParams([serverNodeUUID:jobOwnerUuid])).save()
        service.jobSchedulerService = Mock(JobSchedulerService)
        service.jobLifecyclePluginService=Mock(JobLifecyclePluginService)

        service.jobSchedulesService = Mock(SchedulesManager)
        when:
        def results = service._doupdate([id: se.id.toString()] + inparams, mockAuth())

        then:
        !results.success
        se.errors.hasErrors()
        se.errors.hasGlobalErrors()
        1 * service.jobLifecyclePluginService.beforeJobSave(se,_) >> {
            throw new JobLifecyclePluginException('an error')
        }

        where:
        inparams                                        | _
        [jobName: 'newName']                            | _
    }

    @Unroll
    def "do save with job lifecycle plugin, error thrown"(){
        given:
        def TEST_UUID1=setupDoValidate()
            def jobparams = baseJobParams()+[

                    workflow: new Workflow(threadcount: 1, keepgoing: true, commands: [new CommandExec(adhocExecution: true, adhocRemoteString: 'test what')]),
            ]
        service.jobSchedulerService = Mock(JobSchedulerService)
        service.jobLifecyclePluginService=Mock(JobLifecyclePluginService)

        service.frameworkService = Stub(FrameworkService) {
            existsFrameworkProject('testProject') >> true
            isClusterModeEnabled() >> false
            getServerUUID() >> TEST_UUID1
            getFrameworkPropertyResolverWithProps(*_) >> Mock(PropertyResolver)
            projectNames(*_) >> []
            getFrameworkNodeName() >> "testProject"
        }
        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            authorizeProjectJobAny(_,_,_,_)>>true
        }
        service.rundeckJobDefinitionManager.jobFromWebParams(_,_)>>{
            new RundeckJobDefinitionManager.ImportedJobDefinition(job:it[0],associations: [:])
        }
        service.rundeckJobDefinitionManager.validateImportedJob(_)>>true
        ImportedJob<ScheduledExecution> importedJob = service.updateJobDefinition(null, jobparams, mockAuth(), new ScheduledExecution())
        when:
        def results = service._dosave(jobparams,importedJob , mockAuth())

        then:
        !results.success
        results.scheduledExecution.errors.hasErrors()
        results.scheduledExecution.errors.hasGlobalErrors()
        results.scheduledExecution.errors.globalErrors.any{it.code=='scheduledExecution.plugin.error.message'}
        1 * service.jobLifecyclePluginService.beforeJobSave(_,_) >> {
            throw new JobLifecyclePluginException('an error')
        }

    }

    @Unroll
    def "do save updated job, renamed with null jobName should fail at validation"() {
        def params = [:]
        def oldJob = new OldJob(oldjobname: 'other name')
        def auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'bob'
            getRoles()>>['a']
        }
        setupDoUpdateJob()
        service.frameworkService = Mock(FrameworkService) {
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> Mock(IRundeckProject) {
                getProperties() >> [:]
                getProjectProperties() >> [:]
            }
            _ * existsFrameworkProject('BProject') >> true

            _ * projectNames(_ as AuthContext) >> ['AProject', 'BProject']
            _ * isClusterModeEnabled() >> false
            _ * getServerUUID() >> null
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    _ * getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy) {
                        _ * validate(_)
                    }
                }
            }
            _ * frameworkNodeName () >> null
            _ * getFrameworkPropertyResolverWithProps(_, _)
            _ * filterNodeSet(*_) >> null
        }

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            0 * authorizeProjectJobAll(_,_,_,_)
            _ * authorizeProjectResourceAll(*_) >> true
            _ * filterAuthorizedNodes(*_) >> null
            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
        }
        given: "a job name set to null"
            def job = new ScheduledExecution(createJobParams())
            job.jobName = null
            def importedJob = RundeckJobDefinitionManager.importedJob(job, [:])

        when: "save the updated the job"
            def results = service._dosaveupdated(params, importedJob, oldJob, auth)

        then: "validation error results in failure"
            !results.success
            results.error.contains('Validation failed')
    }

    @Unroll
    def "do save updated job, renaming a job with cluster enabled should keep original job name on job reference"() {
        def params = [:]
        def job = new ScheduledExecution(createJobParams())
        def oldJob = new OldJob(oldjobname: 'blue', originalRef: ScheduledExecutionService.jobEventRevRef(job))
        job.jobName = 'other name'
        def auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'bob'
            getRoles()>>['a']
        }
        setupDoUpdateJob()
        service.frameworkService = Mock(FrameworkService) {
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> Mock(IRundeckProject) {
                getProperties() >> [:]
                getProjectProperties() >> [:]
            }
            _ * existsFrameworkProject('BProject') >> true

            _ * projectNames(_ as AuthContext) >> ['AProject', 'BProject']
            _ * isClusterModeEnabled() >> true
            _ * getServerUUID() >> null
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    _ * getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy) {
                        _ * validate(_)
                    }
                }
            }
            _ * frameworkNodeName () >> null
            _ * getFrameworkPropertyResolverWithProps(_, _)
            _ * filterNodeSet(*_) >> null
        }

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeProjectJobAll(_,_,_,_) >> true
            _ * authorizeProjectResourceAll(*_) >> true
            _ * authorizeProjectJobAny(*_) >> true
            _ * filterAuthorizedNodes(*_) >> null
            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
        }


        JobReferenceImpl jobReferenceImpl = new JobReferenceImpl(
                id: job.extid,
                jobName: job.jobName,
                groupPath: job.groupPath,
                project: job.project,
                serverUUID: job.serverNodeUUID,
                originalQuartzJobName: oldJob.oldjobname,
                originalQuartzGroupName: oldJob.oldjobgroup
        )

        service.jobSchedulerService = Mock(JobSchedulerService){
            1 * updateScheduleOwner(_) >> { arguments ->
                JobReferenceImpl jobReference = arguments[0];
                jobReference.getOriginalQuartzJobName() == jobReferenceImpl.getOriginalQuartzJobName()
                jobReference.getOriginalQuartzGroupName() == jobReferenceImpl.getOriginalQuartzGroupName()
                return false
            }
        }

        service.jobSchedulesService=Mock(SchedulesManager){
            1 * shouldScheduleExecution(_) >> false
        }

        def importedJob = RundeckJobDefinitionManager.importedJob(job, [:])

        when: "save the updated the job"
            def results = service._dosaveupdated(params, importedJob, oldJob, auth)

        then: "validation error results in failure"
            results.success
    }

    @Unroll
    def "do save updated job, check if remote scheduling was changed"() {
        def params = [:]
        def job = new ScheduledExecution(createJobParams())
        def oldJob = new OldJob(oldjobname: 'blue', originalRef: ScheduledExecutionService.jobEventRevRef(job))
        job.jobName = 'other name'
        def auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'bob'
            getRoles()>>['a']
        }
        setupDoUpdateJob()
        service.frameworkService = Mock(FrameworkService) {
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> Mock(IRundeckProject) {
                getProperties() >> [:]
                getProjectProperties() >> [:]
            }
            _ * existsFrameworkProject('BProject') >> true

            _ * projectNames(_ as AuthContext) >> ['AProject', 'BProject']
            _ * isClusterModeEnabled() >> true
            _ * getServerUUID() >> null
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    _ * getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy) {
                        _ * validate(_)
                    }
                }
            }
            _ * frameworkNodeName () >> null
            _ * getFrameworkPropertyResolverWithProps(_, _)
            _ * filterNodeSet(*_) >> null
        }

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            1 * authorizeProjectJobAll(_,_,_,_) >> true
            _ * authorizeProjectResourceAll(*_) >> true
            _ * authorizeProjectJobAny(*_) >> true
            _ * filterAuthorizedNodes(*_) >> null
            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
        }

        service.jobSchedulerService = Mock(JobSchedulerService){
            1 * updateScheduleOwner(_) >> false
        }

        service.jobSchedulesService=Mock(SchedulesManager){
            1 * shouldScheduleExecution(_) >> remoteSchedulingChanged
        }

        def importedJob = RundeckJobDefinitionManager.importedJob(job, [:])

        when: "save the updated the job"
        def results = service._dosaveupdated(params, importedJob, oldJob, auth)

        then: "validation error results in failure"
        results.success
        results.remoteSchedulingChanged == remoteSchedulingChanged

        where:
        remoteSchedulingChanged |_
        true                    |_
        false                   |_
    }

    @Unroll
    def "do update job, disabled execution should delete quartz"() {
        def params = [:]

        def se = new ScheduledExecution(createJobParams()).save()
        assert se.id!=null
        def oldQuartzJob=se.generateJobScheduledName()
        def oldQuartzGroup=se.generateJobGroupName()

        def auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'bob'
            getRoles()>>['a']
        }
        setupDoUpdateJob()
        service.frameworkService = Mock(FrameworkService) {
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> Mock(IRundeckProject) {
                getProperties() >> [:]
                getProjectProperties() >> [:]
            }
            _ * existsFrameworkProject('BProject') >> true
            _ * projectNames(_ as AuthContext) >> ['AProject', 'BProject']
            _ * isClusterModeEnabled() >> false
            _ * getServerUUID() >> null
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    _ * getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy) {
                        _ * validate(_)
                    }
                }
            }
            _ * frameworkNodeName () >> null
            _ * getFrameworkPropertyResolverWithProps(_, _)
            _ * filterNodeSet(*_) >> null
        }

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            0 * authorizeProjectJobAll(_,_,_,_)
            _ * authorizeProjectResourceAll(*_) >> true
            _ * filterAuthorizedNodes(*_) >> null
            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
        }
        service.jobSchedulesService=Mock(SchedulesManager)
        service.jobSchedulerService=Mock(JobSchedulerService)
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_, _, ['update'], 'AProject') >> true
        1 * service.jobSchedulesService.shouldScheduleExecution(_) >> false
        1 * service.jobSchedulesService.isScheduled(_) >> true

        given: "modify job to disable execution or schedule"
            def job = new ScheduledExecution(createJobParams(jobparams))
            def importedJob = RundeckJobDefinitionManager.importedJob(job, [:])

        when: "save the updated the job"
            def results = service._doupdateJobOrParams(se.id, importedJob, params, auth)

        then: "job should be deleted from quartz"
            results.success
            1 * service.jobSchedulerService.deleteJobSchedule(oldQuartzJob, oldQuartzGroup)
        where:
            jobparams                 | _
            [scheduleEnabled: false]  | _
            [executionEnabled: false] | _
    }

    @Unroll
    def "do update job with external schedule, should not change owner"() {
        def params = [:]

        def se = new ScheduledExecution(createJobParams(scheduled:false)).save()
        assert se.id!=null
        def oldQuartzJob=se.generateJobScheduledName()
        def oldQuartzGroup=se.generateJobGroupName()

        def auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'bob'
            getRoles()>>['a']
        }
        setupDoUpdateJob()
        service.frameworkService = Mock(FrameworkService) {
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> Mock(IRundeckProject) {
                getProperties() >> [:]
                getProjectProperties() >> [:]
            }
            _ * existsFrameworkProject('BProject') >> true
            _ * projectNames(_ as AuthContext) >> ['AProject', 'BProject']
            _ * isClusterModeEnabled() >> true
            _ * getServerUUID() >> null
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    _ * getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy) {
                        _ * validate(_)
                    }
                }
            }
            _ * frameworkNodeName () >> null
            _ * getFrameworkPropertyResolverWithProps(_, _)
            _ * filterNodeSet(*_) >> null
        }

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            0 * authorizeProjectJobAll(_,_,_,_)
            _ * authorizeProjectResourceAll(*_) >> true
            _ * filterAuthorizedNodes(*_) >> null
            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
        }
        service.jobSchedulesService=Mock(SchedulesManager)
        service.jobSchedulerService=Mock(JobSchedulerService)
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_, _, ['update'], 'AProject') >> true
        1 * service.jobSchedulesService.shouldScheduleExecution(_) >> true
        1 * service.jobSchedulesService.isScheduled(_) >> true

        given: "modify job without modifying scheduling params"
            def job = new ScheduledExecution(createJobParams([scheduled:false,description:'a new description']))
            def importedJob = RundeckJobDefinitionManager.importedJob(job, [:])

        when: "save the updated the job"
            def results = service._doupdateJobOrParams(se.id, importedJob, params, auth)

        then: "job should not have schedule owner changed"
            results.success
            0 * service.jobSchedulerService.deleteJobSchedule(oldQuartzJob, oldQuartzGroup)
            0 * service.jobSchedulerService.updateScheduleOwner(_)
    }
    @Unroll
    def "do update job, enabled execution should register quartz"() {
        def params = [:]

        def se = new ScheduledExecution(createJobParams(scheduleEnabled: false, executionEnabled: false)).save()
        assert se.id!=null
        def oldQuartzJob=se.generateJobScheduledName()
        def oldQuartzGroup=se.generateJobGroupName()

        def auth = Mock(UserAndRolesAuthContext){
            getUsername()>>'bob'
            getRoles()>>['a']
        }
        setupDoUpdateJob()
        service.frameworkService = Mock(FrameworkService) {
            _ * existsFrameworkProject('AProject') >> true
            _ * getFrameworkProject('AProject') >> Mock(IRundeckProject) {
                getProperties() >> [:]
                getProjectProperties() >> [:]
            }
            _ * existsFrameworkProject('BProject') >> true
            _ * projectNames(_ as AuthContext) >> ['AProject', 'BProject']
            _ * isClusterModeEnabled() >> false
            _ * getServerUUID() >> null
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService() >> Mock(WorkflowStrategyService) {
                    _ * getStrategyForWorkflow(*_) >> Mock(WorkflowStrategy) {
                        _ * validate(_)
                    }
                }
            }
            _ * frameworkNodeName () >> null
            _ * getFrameworkPropertyResolverWithProps(_, _)
            _ * filterNodeSet(*_) >> null
        }

        service.rundeckAuthContextProcessor=Mock(AppAuthContextProcessor){
            0 * authorizeProjectJobAll(_,_,_,_)
            _ * authorizeProjectResourceAll(*_) >> true
            _ * filterAuthorizedNodes(*_) >> null

            _ * getAuthContextWithProject(_, _) >> { args ->
                return args[0]
            }
        }
        service.jobSchedulesService=Mock(SchedulesManager)
        service.jobSchedulerService=Mock(JobSchedulerService)
        service.executionServiceBean=Mock(ExecutionService){
            _* getExecutionsAreActive()>>true
        }
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_, _, ['update'], 'AProject') >> true
        2 * service.jobSchedulesService.shouldScheduleExecution(_) >> true
        1 * service.jobSchedulerService.deleteJobSchedule(oldQuartzJob, oldQuartzGroup)
        1 * service.jobSchedulesService.isScheduled(_) >> false
        1 * service.quartzScheduler.checkExists(_)>>exists

        given: "modify job to enable executions and schedule"
            def job = new ScheduledExecution(createJobParams(jobparams))
            def importedJob = RundeckJobDefinitionManager.importedJob(job, [:])

        when: "save the updated the job"
            def results = service._doupdateJobOrParams(se.id, importedJob, params, auth)

        then: "job should be added to quartz"
            results.success
            1 * service.jobSchedulesService.handleScheduleDefinitions(_,exists)>>[nextTime:new Date()+1]
        where:
            jobparams                                       | exists
            [scheduleEnabled: true, executionEnabled: true] | true
            [scheduleEnabled: true, executionEnabled: true] | false
    }


    @Unroll
    def "do update job with job lifecycle plugin, error thrown"(){
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
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
        service.frameworkService.getNodeStepPluginDescription('asdf') >> Mock(Description)
        service.frameworkService.validateDescription(_, '', _, _, _, _) >> [valid: true]
        service.jobLifecyclePluginService=Mock(JobLifecyclePluginService)

        service.jobSchedulesService = Mock(SchedulesManager)
        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())

        then:
            !results.success
            se.errors.hasErrors()
            se.errors.hasGlobalErrors()
            se.errors.globalErrors.any{it.code=='scheduledExecution.plugin.error.message'}
            1 * service.jobLifecyclePluginService.beforeJobSave(se,_) >> {
                throw new JobLifecyclePluginException('an error')
            }


        where:
        inparams                                        | _
        [jobName: 'newName']                            | _
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
        newJob = new RundeckJobDefinitionManager.ImportedJobDefinition(job:newJob, associations: [:])
        service.frameworkService.getNodeStepPluginDescription('asdf') >> Mock(Description)
        service.frameworkService.validateDescription(_, '', _, _, _, _) >> [valid: true]

        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }

        when:
        def results = service._doupdateJob(se.id,newJob, mockAuth())

        then:
        results.success
        results.scheduledExecution.serverNodeUUID == (shouldChange?serverUUID:currentOwner)
        if(shouldChange) {
            1 * service.jobSchedulerService.updateScheduleOwner(_) >> true
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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> upload.scheduled
        }

        when:
        def result = service.loadJobs([upload], 'update', null, [method: 'scm-import'], mockAuth())

        then:

        result.jobs.size() == 1
        result.jobs[0].properties.description == 'milk duds'
        2 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_SCM_UPDATE],_) >> true
    }

    def "not check scm_update permission if isnt a scm-import"() {
        given:
        setupDoUpdate()
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams([:]) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams([description: 'milk duds']))

        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> upload.scheduled
        }

        when:
        def result = service.loadJobs([upload], 'update', null, [method: 'x'], mockAuth())

        then:

        result.jobs.size() == 1
        result.jobs[0].properties.description == 'milk duds'
        2 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE],_) >> true
        0 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_SCM_UPDATE],_) >> true
    }

    @Unroll
    def "scm update job without update or scm_update permission"() {
        given:
        setupDoUpdate()
        def uuid = UUID.randomUUID().toString()
        def orig = new ScheduledExecution(createJobParams([:]) + [uuid: uuid]).save()
        def upload = new ScheduledExecution(createJobParams([description: 'milk duds']))

        when:
        def result = service.loadJobs([upload], 'update', null, [method: 'scm-import'], mockAuth())

        then:

        result.jobs.size() == 0
        result.errjobs.size() == 1
        result.errjobs[0].errmsg.startsWith("Unauthorized: Update Job")
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_UPDATE, AuthConstants.ACTION_SCM_UPDATE],_) >> false
    }


    @Unroll
    def "scm delete scheduledExecution By Id"(){
        given:
        setupDoUpdate()
        service.rundeckAuthContextProcessor.authorizeProjectResource(*_)>>false
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
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_DELETE,AuthConstants.ACTION_SCM_DELETE],_) >> true
    }

    @Unroll
    def "not scm delete scheduledExecution By Id"(){
        given:
        setupDoUpdate()
        service.rundeckAuthContextProcessor.authorizeProjectResource(*_)>>false
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
        0 * service.rundeckAuthContextProcessor.authorizeProjectJobAll(_,_,
                [AuthConstants.ACTION_SCM_DELETE],_) >> true
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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> upload.scheduled
        }
        service.rundeckJobDefinitionManager.validateImportedJob(_)>>true

        when:
        def result = service.loadJobs([upload], 'create','remove', [method: 'scm-import'],  mockAuth())

        then:
        result.jobs.size()==1
        1 * service.rundeckAuthContextProcessor.authorizeProjectResourceAny(_,AuthConstants.RESOURCE_TYPE_JOB,
                [AuthConstants.ACTION_CREATE,AuthConstants.ACTION_SCM_CREATE],'AProject') >> true
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_CREATE,AuthConstants.ACTION_SCM_CREATE],_) >> true

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
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> upload.scheduled
        }
        service.rundeckJobDefinitionManager.validateImportedJob(_)>>true

        when:
        def result = service.loadJobs([upload], 'create','remove', [method: 'create'],  mockAuth())

        then:
        result.jobs.size()==1
        1 * service.rundeckAuthContextProcessor.authorizeProjectResourceAny(_,AuthConstants.RESOURCE_TYPE_JOB,
                [AuthConstants.ACTION_CREATE],'AProject') >> true
        0 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_SCM_CREATE],_) >> false
        1 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_CREATE],_) >> true

    }

    def "blank email notification attached options defaults to inline"() {
        given:
        setupDoValidate()

        def params = baseJobParams()+[
                (ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL):'true',
                (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS):'a@example.com,z@example.com',
                (ScheduledExecutionController.NOTIFY_SUCCESS_ATTACH):'true',

        ]
        def authContext = Mock(UserAndRolesAuthContext){
            getUsername()>>'auser'
            getRoles()>>(['a','b'] as Set)
        }

        when:
        def results = service._dovalidate(params, authContext)
        def ScheduledExecution scheduledExecution = results.scheduledExecution

        then:

        !results.failed
        scheduledExecution != null
        scheduledExecution instanceof ScheduledExecution

        !scheduledExecution.errors.hasErrors()
        scheduledExecution.notifications[0].mailConfiguration().recipients=='a@example.com,z@example.com'
        scheduledExecution.notifications[0].mailConfiguration().attachLog
        scheduledExecution.notifications[0].mailConfiguration().attachLogInFile

    }


    def "log change on flags change"() {
        given:
        def jobChangeLogger = Mock(Logger)
        setupDoValidate(true)
        def uuid = setupDoUpdate(true)

        def se = new ScheduledExecution(createJobParams()).save()
        service.jobSchedulesService = Mock(JobSchedulesService){
            shouldScheduleExecution(_) >> se.scheduled
        }
        service.jobSchedulerService=Mock(JobSchedulerService)
        and:
        service.jobChangeLogger = jobChangeLogger
        def expectedLog = user+" MODIFY [${se.id}] AProject \"some/where/blue\" (update)"
        when:
        def params = baseJobParams()+[

        ]
        //def results = service._dovalidate(params, Mock(UserAndRolesAuthContext))
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
            0 * service.jobSchedulesService.handleScheduleDefinitions(_, _)
            result == [null, null]
    }

    @Unroll
    def "list workflows uses query components"() {
        given: "job query bean"
            def testJobQuery = Mock(JobQuery)
            defineBeans {
                testJobQueryBean(InstanceFactoryBean, testJobQuery)
            }
            def params = [test: 'test2']
            def input = Mock(JobQueryInput){
                getMax()>>queryMax
            }
            service.applicationContext = applicationContext
        when: "called with queryMax parameter #queryMax"
            def result = service.listWorkflows(input, params)
        then: "component extends search critera and optionally count criteria if max is set"
            result != null
            (queryMax>0?2:1) * testJobQuery.extendCriteria(_, _, _)
        where:
            queryMax << [0,20]

    }


    @Unroll
    def "list workflows paging parameters total"() {
        given:
            def job1 = new ScheduledExecution(createJobParams()).save()
            def job2 = new ScheduledExecution(createJobParams(jobName:'another job')).save()
            def job3 = new ScheduledExecution(createJobParams(jobName:'another job2',groupPath:'another/group')).save()
            def input = Mock(JobQueryInput){
                getMax()>>queryMax
                getProjFilter()>>'AProject'
                getJobFilter()>>jobFilter
            }
            service.applicationContext = applicationContext
        when:
            def result = service.listWorkflows(input, [:])
        then:
            result != null
            result.schedlist.size()==count
            result.total==total

        where:
            queryMax | jobFilter | groupPath | total | count
            0        | null      | null      | 3     | 3
            0        | 'another' | null      | 2     | 2
            1        | 'another' | null      | 2     | 1
    }

    @Unroll
    def "list workflows group path - total"() {
        given:
            def job1 = new ScheduledExecution(createJobParams()).save()
            def job2 = new ScheduledExecution(createJobParams(jobName:'another job',groupPath: '')).save()
            def job3 = new ScheduledExecution(createJobParams(jobName:'another job2',groupPath:'')).save()
            def input = Mock(JobQueryInput){
                getMax()>>queryMax
                getProjFilter()>>'AProject'
                getGroupPath()>>groupPath
            }
            service.applicationContext = applicationContext
        when:
            def result = service.listWorkflows(input, [:])
        then:
            result != null
            result.schedlist.size()==count
            result.total == total

        where:
            queryMax |  groupPath    | total | count
            0        |  'some/where' | 1     | 1
            0        |  '-'          | 2     | 2
            1        |  '-'          | 2     | 1
    }

    @Unroll
    def "delete scheduled execution"() {
        given:
            def job = new ScheduledExecution(createJobParams()).save()
            def authContext = Mock(AuthContext)
            def username = 'bob'
            def id = job.id
            service.executionServiceBean = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService)
            service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager)
            service.jobSchedulerService = Mock(JobSchedulerService)
        when:
            def result = service.deleteScheduledExecution(job, deleteExecutions, authContext, username)
        then:
            result.success
            !ScheduledExecution.get(id)
            1 * service.fileUploadService.deleteRecordsForScheduledExecution(job)
            1 * service.rundeckJobDefinitionManager.beforeDelete(job, authContext)
            1 * service.rundeckJobDefinitionManager.afterDelete(job, authContext)
            1 * service.jobSchedulerService.deleteJobSchedule(_, _)

        where:
            deleteExecutions << [true, false]
    }

    @Unroll
    def "delete scheduled execution running"() {
        given:
            def job = new ScheduledExecution(createJobParams()).save()
            def exec1 = new Execution(
                    scheduledExecution: job,
                    status: 'running',
                    dateStarted: new Date() + 2,
                    dateCompleted: null,
                    project: job.project,
                    user: 'bob',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
            ).save(flush: true)
            def authContext = Mock(AuthContext)
            def username = 'bob'
            def id = job.id
            service.executionServiceBean = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService)
            service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager)
            service.jobSchedulerService = Mock(JobSchedulerService)
        when:
            def result = service.deleteScheduledExecution(job, deleteExecutions, authContext, username)
        then:
            !result.success
            result.error =~ /it is currently being executed/
            null != ScheduledExecution.get(id)
            0 * service.fileUploadService.deleteRecordsForScheduledExecution(job)
            0 * service.rundeckJobDefinitionManager.beforeDelete(job, authContext)
            0 * service.rundeckJobDefinitionManager.afterDelete(job, authContext)
            0 * service.jobSchedulerService.deleteJobSchedule(_, _)

        where:
            deleteExecutions << [true, false]
    }


    @Unroll
    def "delete scheduled execution also executions"() {
        given:
            def job = new ScheduledExecution(createJobParams()).save()
            def exec1 = new Execution(
                    scheduledExecution: job,
                    status: 'running',
                    dateStarted: new Date(100),
                    dateCompleted: new Date(10000),
                    project: job.project,
                    user: 'bob',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
            ).save(flush: true)

            def authContext = Mock(AuthContext)
            def username = 'bob'
            def id = job.id
            def execid = exec1.id
            service.executionServiceBean = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService)
            service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager)
            service.jobSchedulerService = Mock(JobSchedulerService)
        when:
            def result = service.deleteScheduledExecution(job, deleteExecutions, authContext, username)
        then:
            !ScheduledExecution.get(id)
            (deleteExecutions ? 1 : 0) * service.
                    executionServiceBean.
                    deleteBulkExecutionIds([execid], authContext, username)>>{
                Execution.get(it[0].first()).delete(flush:true)
            }
            if (!deleteExecutions) {
                def exec2 = Execution.get(execid)
                exec2 != null
                exec2.scheduledExecution == null
            }
            1 * service.fileUploadService.deleteRecordsForScheduledExecution(job)
            1 * service.rundeckJobDefinitionManager.beforeDelete(job, authContext)
            1 * service.rundeckJobDefinitionManager.afterDelete(job, authContext)
            1 * service.jobSchedulerService.deleteJobSchedule(_, _)

        where:
            deleteExecutions << [true, false]
    }

    @Unroll
    def "delete scheduled execution also deletes job stats and job refs"() {
        given:
            def job = new ScheduledExecution(createJobParams()).save()
            def stats = new ScheduledExecutionStats(se: job, content: '{}').save()
            def exec1 = new Execution(
                    status: 'running',
                    dateStarted: new Date(100),
                    dateCompleted: new Date(10000),
                    project: job.project,
                    user: 'bob',
                    workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: "test exec")])
            ).save(flush: true)
            def ref = new ReferencedExecution(scheduledExecution: job, status: 'success', execution: exec1).save()

            def authContext = Mock(AuthContext)
            def username = 'bob'
            def id = job.id
            def statid = stats.id
            def refid = ref.id
            service.executionServiceBean = Mock(ExecutionService)
            service.fileUploadService = Mock(FileUploadService)
            service.rundeckJobDefinitionManager = Mock(RundeckJobDefinitionManager)
            service.jobSchedulerService = Mock(JobSchedulerService)
        when:
            def result = service.deleteScheduledExecution(job, deleteExecutions, authContext, username)
        then:
            result.success
            !ScheduledExecution.get(id)
            !ScheduledExecutionStats.get(statid)
            !ReferencedExecution.get(refid)
            1 * service.fileUploadService.deleteRecordsForScheduledExecution(job)
            1 * service.rundeckJobDefinitionManager.beforeDelete(job, authContext)
            1 * service.rundeckJobDefinitionManager.afterDelete(job, authContext)
            1 * service.jobSchedulerService.deleteJobSchedule(_, _)

        where:
            deleteExecutions << [true, false]
    }

    def "job definition basic should retain uuid"() {
        given:
            def uuid = UUID.randomUUID().toString()
            def job = new ScheduledExecution(createJobParams(uuid: uuid))
            def jobInput = input ? new ScheduledExecution(createJobParams(input)) : null

            def auth = Mock(UserAndRolesAuthContext)

        when:
            service.jobDefinitionBasic(job, jobInput, params, auth)
        then:
            job.uuid == uuid
        where:
            input                 | params
            null                  | [:]
            [jobName: 'zocaster'] | null
    }

    @Unroll
    def "job definition basic blank #propName should be set to #expect"() {
        given:
            def job = new ScheduledExecution(createJobParams())
            def jobInput =  new ScheduledExecution(createJobParams())
            jobInput?."$propName" = value

            def auth = Mock(UserAndRolesAuthContext)

        when:
            service.jobDefinitionBasic(job, jobInput, null, auth)
        then:
            job."$propName" == expect

        where:
            propName    | value | expect
            'jobName'   | ''    | ''
            'jobName'   | null  | ''
            'groupPath' | ''    | null
            'groupPath' | null  | null
    }

    @Unroll
    def "job definition basic null map #propName should be set to #expect"() {
        given:
            def job = new ScheduledExecution(createJobParams())

            def params = [(propName): value]

            def auth = Mock(UserAndRolesAuthContext)

        when:
            service.jobDefinitionBasic(job, null, params, auth)
        then:
            job."$propName" == expect

        where:
            propName    | value | expect
            'jobName'   | ''    | ''
            'jobName'   | null  | ''
            'groupPath' | ''    | null
            'groupPath' | null  | null
    }

    def "job definition workflow should have not null workflow"() {
        given: "new job"
            def job = new ScheduledExecution()
            def auth = Mock(UserAndRolesAuthContext)
        when: "define the workflow from empty input"
            service.jobDefinitionWorkflow(job, null, [:], auth)
        then: "workflow is not null"
            job.workflow != null

    }

    def "job definition workflow from input job"() {
        given: "new job"
            def job = new ScheduledExecution()
            def input = new ScheduledExecution(createJobParams())
            def auth = Mock(UserAndRolesAuthContext)
        when: "define the workflow from input job"
            service.jobDefinitionWorkflow(job, input, [:], auth)
        then: "workflow is the same"
            job.workflow != null
            job.workflow.toMap() == input.workflow.toMap()

    }

    def "job definition workflow from workflow param"() {
        given: "new job"
            def job = new ScheduledExecution()
            def params = [workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test')])]
            def auth = Mock(UserAndRolesAuthContext)
        when: "define the workflow from params"
            service.jobDefinitionWorkflow(job, null, params, auth)
        then: "workflow is the same"
            job.workflow != null
            job.workflow.toMap() == params.workflow.toMap()

    }
    def "job definition workflow from map params"() {
        given: "existing job workflow"
            def job = new ScheduledExecution(workflow: new Workflow(commands: [new CommandExec(adhocRemoteString: 'test')]))
            def params = [workflow: [strategy:'parallel',keepgoing: 'true']]
            def auth = Mock(UserAndRolesAuthContext)
        when: "workflow attributes modified"
            service.jobDefinitionWorkflow(job, null, params, auth)
        then: "workflow is modified"
            job.workflow != null
            job.workflow.toMap().keepgoing == true
            job.workflow.toMap().strategy == 'parallel'
            job.workflow.toMap().commands == [[exec: 'test']]

    }
    def "job definition workflow strategy config from map params"() {
        given: "existing job workflow"
            def job = new ScheduledExecution(workflow: new Workflow(strategy:'aplugin', commands: [new CommandExec(adhocRemoteString: 'test')]))
            def params = [workflow: [
                strategy:'aplugin',
                strategyPlugin:[
                    aplugin:[
                        config:[a:'b']
                    ]
                ]
            ]]
            def auth = Mock(UserAndRolesAuthContext)
        when: "workflow strategy config input"
            service.jobDefinitionWFStrategy(job, null, params, auth)
        then: "workflow strategy plugin config is modified"
            job.workflow != null
            job.workflow.toMap().strategy == 'aplugin'
            job.workflow.toMap().pluginConfig == [WorkflowStrategy:[aplugin:[a:'b']]]

    }

    def "job definition workflow strategy config from input"() {
        given: "existing job workflow"
            def job = new ScheduledExecution(workflow: new Workflow(strategy:'ruleset', commands: [new CommandExec(adhocRemoteString: 'test')]))
            def jobInput = new ScheduledExecution(workflow: new Workflow(strategy:'ruleset',
                    pluginConfig: "{\"WorkflowStrategy\":{\"ruleset\":{\"rules\":\"[*] run-in-sequence\\r\\n[5] if:option.env==QA\\r\\n[6] unless:option.env==PRODUCTION\"}}}",
                    commands: [new CommandExec(adhocRemoteString: 'test')]))

            def auth = Mock(UserAndRolesAuthContext)
        when: "workflow strategy config input"
            service.jobDefinitionWFStrategy(job, jobInput, null, auth)
        then: "workflow strategy plugin config is modified"
            job.workflow != null
            job.workflow.toMap().strategy == 'ruleset'
            job.workflow.toMap().pluginConfig == [WorkflowStrategy:[ruleset:[rules:'[*] run-in-sequence\r\n[5] if:option.env==QA\r\n[6] unless:option.env==PRODUCTION']]]

    }
    def "job definition workflow strategy config from input unmatched strategy"() {
        given: "existing job workflow"
            def job = new ScheduledExecution(workflow: new Workflow(strategy:'other', commands: [new CommandExec(adhocRemoteString: 'test')]))
            def jobInput = new ScheduledExecution(workflow: new Workflow(strategy:'other',
                    pluginConfig: "{\"WorkflowStrategy\":{\"ruleset\":{\"rules\":\"[*] run-in-sequence\\r\\n[5] if:option.env==QA\\r\\n[6] unless:option.env==PRODUCTION\"}}}",
                    commands: [new CommandExec(adhocRemoteString: 'test')]))

            def auth = Mock(UserAndRolesAuthContext)
        when: "workflow strategy config input"
            service.jobDefinitionWFStrategy(job, jobInput, null, auth)
        then: "workflow strategy plugin config is modified"
            job.workflow != null
            job.workflow.toMap().strategy == 'other'
            job.workflow.toMap().pluginConfig == null

    }

    def "job definition workflow from session params"() {
        given: "new job"
            def job = new ScheduledExecution()
            def params = [
                    _sessionwf          : 'true',
                    _sessionEditWFObject: new Workflow(
                            commands: [
                                    new CommandExec(adhocRemoteString: 'test')
                            ]
                    )
            ]
            def auth = Mock(UserAndRolesAuthContext)
        when: "define the workflow from params"
            service.jobDefinitionWorkflow(job, null, params, auth)
        then: "workflow is the same"
            job.workflow != null
            job.workflow.toMap() == params._sessionEditWFObject.toMap()
    }

    @Unroll
    def "validateDefinitionWorkflow empty workflow and commands"() {
        given:
            def job = new ScheduledExecution(inputMap)
            def auth = Mock(UserAndRolesAuthContext)
            service.frameworkService = Mock(FrameworkService)
        when:
            def failed = service.validateDefinitionWorkflow(job, auth, false)
        then:
            failed
            job.errors.hasFieldErrors('workflow')
            job.errors.getFieldError('workflow').code == 'scheduledExecution.workflow.empty.message'

        where:
            inputMap                               | _
            [:]                                    | _
            [workflow: new Workflow()]             | _
            [workflow: new Workflow(commands: [])] | _
    }

    def "job definition options from session opts"() {
        given:
            def job = new ScheduledExecution(options:[new Option(name:'optX')])
            def params = [
                _sessionopts          : 'true',
                _sessionEditOPTSObject: [
                    opt1: new Option(name: 'opt1'),
                    opt2: new Option(name: 'opt2', required: true, description: 'monkey'),
                ]
            ]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionOptions(job, null, params, auth)
        then:
            job.options!=null
            job.options.size()==2
            job.options[0].toMap()==params._sessionEditOPTSObject['opt1'].toMap()
            job.options[1].toMap()==params._sessionEditOPTSObject['opt2'].toMap()
    }

    def "job definition options from params.options list"() {
        given:
            def job = new ScheduledExecution(options:[new Option(name:'optX')])
            def opt1 = new Option(name: 'opt1')
            def opt2 = new Option(name: 'opt2', required: true, description: 'monkey')
            def params = [
                options:[
                    opt1,
                    opt2,
                ]
            ]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionOptions(job, null, params, auth)
        then:
            job.options!=null
            job.options.size()==2
            job.options[0].toMap()==opt1.toMap()
            job.options[1].toMap()==opt2.toMap()
    }
    def "job definition options from params.options map"() {
        given:
            def job = new ScheduledExecution(options:[new Option(name:'optX')])
            def opt1 = [name: 'opt1']
            def opt2 = [name: 'opt2', required: true, description: 'monkey']
            def params = [
                options:[
                    'options[0]': opt1,
                    'options[1]': opt2,
                ]
            ]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionOptions(job, null, params, auth)
        then:
            job.options!=null
            job.options.size()==2
            job.options[0].toMap()==opt1
            job.options[1].toMap()==opt2
    }
    def "job definition options from input job"() {
        given:
            def job = new ScheduledExecution(options:[new Option(name:'optX')])
            def job2 = new ScheduledExecution(options:[new Option(name: 'opt1'),new Option(name: 'opt2', required: true, description: 'monkey')])
            def params = [:]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionOptions(job, job2, params, auth)
        then:
            job.options!=null
            job.options.size()==2
            job.options[0].toMap()==job2.options[0].toMap()
            job.options[1].toMap()==job2.options[1].toMap()
    }
    def "job definition options without input"() {

        given:

            def opt1 = new Option(name: 'optX')
            def opt1map=opt1.toMap()
            def job = new ScheduledExecution(options:[opt1])

            def params = [:]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionOptions(job, null, params, auth)
        then:
            job.options!=null
            job.options.size()==1
            job.options[0].toMap()==opt1map
    }

    def "validate definition options should not have errors for unvalidated scheduledExecution"() {
        given:
            def opt2 = new Option(name: 'opt2', required: false, description: 'monkey', enforced: false)
            def params = [options: [opt2,]]
            def job = new ScheduledExecution()
            def auth = Mock(UserAndRolesAuthContext)
            service.fileUploadService = Mock(FileUploadService)
            service.jobDefinitionOptions(job, null, params, auth)
        when:
            def failed = service.validateDefinitionOptions(job, params)
        then:
            !failed
            !job.errors.hasErrors()
    }

    def "validate definition options should have errors for option"() {
        given:
            def opt2 = new Option(name: 'opt2', required: true, description: 'monkey', enforced: false)
            def params = [options: [opt2,]]
            def job = new ScheduledExecution(scheduled: true, crontabString: '0 0 0 0 * * ?')
            job.validate()
            def auth = Mock(UserAndRolesAuthContext)
            service.fileUploadService = Mock(FileUploadService)
            service.jobDefinitionOptions(job, null, params, auth)
        when:
            def failed = service.validateDefinitionOptions(job, params)
        then:
            failed
            def erropt = job.options.find{it.name=='opt2'}
            erropt.errors.hasFieldErrors('defaultValue')
            !erropt.errors.hasFieldErrors('scheduledExecution.name')
            job.errors.hasErrors()
            job.errors.hasFieldErrors('options')
            job.errors.hasFieldErrors('jobName')
    }

    def "validate definition options should not have errors for invalid scheduledExecution"() {
        given:
            def opt2 = new Option(name: 'opt2', required: false, description: 'monkey', enforced: false)
            def params = [options: [opt2,]]
            def job = new ScheduledExecution(scheduled: true, crontabString: '0 0 0 0 * * ?')
            job.validate()
            def auth = Mock(UserAndRolesAuthContext)
            service.fileUploadService = Mock(FileUploadService)
            service.jobDefinitionOptions(job, null, params, auth)
        when:
            def failed = service.validateDefinitionOptions(job, params)
        then:
            !failed
            def erropt = job.options.find{it.name=='opt2'}
            !erropt.hasErrors()
            !erropt.errors.hasFieldErrors('defaultValue')
            !erropt.errors.hasFieldErrors('scheduledExecution.name')
            job.errors.hasErrors()
            !job.errors.hasFieldErrors('options')
            job.errors.hasFieldErrors('jobName')
    }

    def "job definition notifications from input job"() {

        given:

            def job = new ScheduledExecution(notifications:[])
            def job2 = new ScheduledExecution(notifications:[
                new Notification(type:'email',content:'blah',eventTrigger: 'onsuccess'),
                new Notification(type:'aplugin',content:'{}',eventTrigger: 'onfailure')
            ])

            def params = [:]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, job2, params, auth)
        then:
            job.notifications.size()==2
            job.notifications.find{it.toMap()== job2.notifications[0].toMap()}!=null
            job.notifications.find{it.toMap()== job2.notifications[1].toMap()}!=null
    }
    def "job definition notifications from old params"() {
        given:
            def job = new ScheduledExecution(notifications:[])
            def params = [(ScheduledExecutionController.NOTIFY_ONSUCCESS_EMAIL): 'true',
                          (ScheduledExecutionController.NOTIFY_SUCCESS_RECIPIENTS): 'c@example.com,d@example.com']
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, null, params, auth)
        then:
            job.notifications.size() == 1
            job.notifications[0].type == 'email'
            job.notifications[0].eventTrigger == 'onsuccess'
            job.notifications[0].configuration == [recipients:  'c@example.com,d@example.com']
    }
    def "job definition notifications from jobNotificationsJson email"() {
        given:
            def job = new ScheduledExecution(notifications:[])
            def json='[{"type":"email","trigger":"onsuccess","config":{"recipients":"c@example.com,d@example.com"}}]'
            def params = [jobNotificationsJson: json]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, null, params, auth)
        then:
            job.notifications.size() == 1
            job.notifications[0].type == 'email'
            job.notifications[0].eventTrigger == 'onsuccess'
            job.notifications[0].configuration == [recipients:  'c@example.com,d@example.com']
    }
    def "job definition notifications from jobNotificationsJson url"() {
        given:
            def job = new ScheduledExecution(notifications:[])
            def json='[{"type":"url","trigger":"onsuccess","config":{"urls":"aurl","format":"'+formatin+'"}}]'
            def params = [jobNotificationsJson: json]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, null, params, auth)
        then:
            job.notifications.size() == 1
            job.notifications[0].type == 'url'
            job.notifications[0].eventTrigger == 'onsuccess'
            job.notifications[0].urlConfiguration().urls=='aurl'
            job.notifications[0].format==formatin
        where:
            formatin | _
            'xml'    | _
            'json'   | _
    }
    def "job definition notifications from jobNotificationsJson plugin"() {
        given:
            def job = new ScheduledExecution(notifications:[])
            def json='[{"type":"aplugin","trigger":"onsuccess","config":{"blah":"blee","bloo":123}}]'
            def params = [jobNotificationsJson: json]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, null, params, auth)
        then:
            job.notifications.size() == 1
            job.notifications[0].type == 'aplugin'
            job.notifications[0].eventTrigger == 'onsuccess'
            job.notifications[0].configuration==[blah:'blee',bloo:123]
    }
    def "job definition notifications from jobNotificationsJson multi replaces all"() {
        given:
            def job = new ScheduledExecution(notifications:[
                new Notification(eventTrigger: 'onfailure',type:'aplugin',configuration:[bloop:'blep'])
            ])
            def json='[{"type":"aplugin","trigger":"onsuccess","config":{"blah":"blee","bloo":123}}]'
            def params = [jobNotificationsJson: json]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, null, params, auth)
        then:
            job.notifications.size() == 1
            job.notifications[0].type == 'aplugin'
            job.notifications[0].eventTrigger == 'onsuccess'
            job.notifications[0].configuration==[blah:'blee',bloo:123]
    }
    def "job definition notifications from jobNotificationsJson multi replaces all 2"() {
        given:
            def job = new ScheduledExecution(notifications:[
                new Notification(eventTrigger: 'onfailure',type:'aplugin',configuration:[bloop:'blep']),
                new Notification(eventTrigger: 'onsuccess',type:'aplugin',configuration:[blap:'jkd'])
            ])
            def json='[{"type":"aplugin","trigger":"onsuccess","config":{"blah":"blee","bloo":123}}]'
            def params = [jobNotificationsJson: json]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, null, params, auth)
        then:
            job.notifications.size() == 1
            job.notifications[0].type == 'aplugin'
            job.notifications[0].eventTrigger == 'onsuccess'
            job.notifications[0].configuration==[blah:'blee',bloo:123]
    }

    def "job definition notifications from jobNotificationsJson allow multiple with same trigger and type"() {
        given:
            def job = new ScheduledExecution(notifications:[
                new Notification(eventTrigger: 'onfailure',type:'aplugin',configuration:[bloop:'blep'])
            ])
            def json='[{"type":"aplugin","trigger":"onsuccess","config":{"blah":"blee","bloo":123}},' +
                     '{"type":"aplugin","trigger":"onsuccess","config":{"blem":"blee","beef":456}}]'
            def params = [jobNotificationsJson: json]
            def auth = Mock(UserAndRolesAuthContext)
        when:
            service.jobDefinitionNotifications(job, null, params, auth)
        then:
            job.notifications.size() == 2
            job.notifications.find{it.configuration==[blah:'blee',bloo:123]}!=null
            job.notifications.find{it.configuration==[blem:'blee',beef:456]}!=null
    }
    def "scm create jobs using scm_create without permission"(){
        given:
        setupDoUpdate()
        //scm create setup

        def  uuid=UUID.randomUUID().toString()
        def upload = new ScheduledExecution(
                createJobParams(jobName:'job1',groupPath:'path1',project:'AProject', uuid: uuid)
        )

        when:
        def result = service.loadJobs([upload], 'create','remove', [method: 'scm-import'],  mockAuth())

        then:
        result.jobs.size()==0
        1 * service.rundeckAuthContextProcessor.authorizeProjectResourceAny(_,AuthConstants.RESOURCE_TYPE_JOB,
                [AuthConstants.ACTION_CREATE,AuthConstants.ACTION_SCM_CREATE],'AProject') >> false
        0 * service.rundeckAuthContextProcessor.authorizeProjectJobAny(_,_,
                [AuthConstants.ACTION_CREATE,AuthConstants.ACTION_SCM_CREATE],_)

    }

    def "applyTriggerComponents"(){
        given:
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b'
                )
        ).save()
        service.applicationContext = Mock(ConfigurableApplicationContext){
            getBeansOfType(_) >> ["componentName":new TriggersExtenderImpl(job)]
        }
        service.afterPropertiesSet()
        when:
        def result = service.applyTriggerComponents(null, [])
        then:
        !result.isEmpty()

    }

    def "registerOnQuartz"(){
        given:
        def job = new ScheduledExecution(
                createJobParams(
                    jobName: 'testJob',
                    groupPath: 'a/group',
                    project:'aProject',
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b'
                )
        ).save()
        service.applicationContext = Mock(ConfigurableApplicationContext){
            getBeansOfType(_) >> ["componentName":new TriggersExtenderImpl(job)]
        }
        service.afterPropertiesSet()
        service.quartzScheduler=Mock(Scheduler)
        when:
        def result = service.registerOnQuartz(null, [], temp, job)
        then:
        result
        count * service.
            quartzScheduler.
            deleteJob({ it.name == "${job.id}:testJob" && it.group == 'aProject:testJob:a/group' })
        count * service.quartzScheduler.scheduleJob(_,!null,true)
        where:
        temp  | count
        true  | 0
        false | 1

    }

    def "registerOnQuartz handles exception"(){
        given:
        def job = new ScheduledExecution(
                createJobParams(
                    jobName: 'testJob',
                    groupPath: 'a/group',
                    project:'aProject',
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b'
                )
        ).save()
        service.applicationContext = Mock(ConfigurableApplicationContext){
            getBeansOfType(_) >> ["componentName":new TriggersExtenderImpl(job)]
        }
        service.afterPropertiesSet()
        service.quartzScheduler=Mock(Scheduler)
        when:
        def result = service.registerOnQuartz(null, [], false, job)
        then:
        result==null
        1 * service.
            quartzScheduler.
            deleteJob({ it.name == "${job.id}:testJob" && it.group == 'aProject:testJob:a/group' })
        1 * service.quartzScheduler.scheduleJob(_,!null,true)>>{
            throw new SchedulerException("test error")
        }

    }

    @Unroll
    def "do not allow orchestrator that has an execution to be deleted hasLinkedExecutions: #hasExecutionsLinked"() {
        setup:
        Orchestrator orch = new Orchestrator(type: "rankTiered")
        orch.save()
        when:
        Long orchId = orch.id
        ScheduledExecution se = new ScheduledExecution(
                createJobParams(
                        jobName: 'testJob',
                        groupPath: 'orch/group',
                        project: 'orchProject',
                        executionEnabled: true,
                        orchestrator: orch
                )
        )
        se.save()

        if (hasExecutionsLinked) {
            Execution e = new Execution(scheduledExecution: se, orchestrator: orch, project:"orchProject", user:"auser")
            e.save()
        }
        service.jobDefinitionOrchestrator(se,new ScheduledExecution(),[:],null)

        then:
        (Orchestrator.get(orchId) == null) == orchestratorDeleted

        where:
        hasExecutionsLinked | orchestratorDeleted
        true                | false
        false               | true

    }

    @Unroll
    def "parseParamOrchestrator config params input #inparams"() {
        given:
            def type = 'rankTiered'
        when:
            def orch = service.parseParamOrchestrator(inparams, type)

        then:
            orch != null
            orch.type == type
            orch.configuration == expect

        where:
            inparams                                               | expect
            [:]                                                    | null
            [orchestratorPlugin: 'x']                              | null
            [orchestratorPlugin: ['x']]                            | null
            [orchestratorPlugin: [a: 'x']]                         | null
            [orchestratorPlugin: [rankTiered: 'x']]                | null
            [orchestratorPlugin: [rankTiered: ['x']]]              | null
            [orchestratorPlugin: [rankTiered: [z: 'x']]]           | null
            [orchestratorPlugin: [rankTiered: [config: [a: 'b']]]] | [a: 'b']

    }

    def "jobDefinitionGlobalLogFilters add logFilter"(){
        given:
        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b'
                )
        )
        job.setUuid("testUUID")
        job.save()

        def params = baseJobParams()
        params.workflow.globalLogFilters = [
                '0': [
                        type  : 'abc',
                        config: [a: 'b']
                ]
        ]

        when:
        service.jobDefinitionGlobalLogFilters(job, null, params, null)
        def se = service.getByIDorUUID(job.uuid)
        then:
        se.workflow.pluginConfig == '{"LogFilter":[{"type":"abc","config":{"a":"b"}}]}'
    }

    def "jobDefinitionGlobalLogFilters modify logFilter"(){
        given:

        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b',
                )
        )
        job.setUuid("testUUID")
        job.workflow.pluginConfig = '{"LogFilter":[{"type":"abc","config":{"a":"b"}}]}'
        job.save()

        def params = baseJobParams()
        params.workflow.globalLogFilters = [
                '0': [
                        type  : 'abcd',
                        config: [a: 'b', d: 'e']
                ]
        ]

        when:
        service.jobDefinitionGlobalLogFilters(job, null, params, null)
        def se = service.getByIDorUUID(job.uuid)
        then:
        se.workflow.pluginConfig == '{"LogFilter":[{"type":"abcd","config":{"a":"b","d":"e"}}]}'
    }

    def "jobDefinitionGlobalLogFilters delete logFilter"(){
        given:

        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b',
                )
        )
        job.setUuid("testUUID")
        job.workflow.pluginConfig = '{"LogFilter":[{"type":"abc","config":{"a":"b"}}]}'
        job.save()

        def params = baseJobParams()
        params.workflow.globalLogFilters = [:]

        when:
        service.jobDefinitionGlobalLogFilters(job, null, params, null)
        def se = service.getByIDorUUID(job.uuid)
        then:
        se.workflow.getPluginConfigDataList('LogFilter') == null
    }

    def "jobDefinitionGlobalLogFilters logFilter modified by job input"(){
        given:

        def job = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b',
                )
        )
        job.setUuid("testUUID")
        job.save()

        def job2 = new ScheduledExecution(
                createJobParams(
                        scheduled: true,
                        scheduleEnabled: true,
                        executionEnabled: true,
                        userRoleList: 'a,b',
                )
        )
        job2.setUuid("testUUID2")
        job2.workflow.pluginConfig = pluginConfigString
        job2.save()

        def params = baseJobParams()
        params.workflow.globalLogFilters = [:]

        when:
        service.jobDefinitionGlobalLogFilters(job, job2, params, null)
        def se = service.getByIDorUUID(job.uuid)
        then:
        se.workflow.pluginConfig == result

        where:
        pluginConfigString                                  | result
        '{"LogFilter":[{"type":"abc","config":{"a":"b"}}]}' | '{"LogFilter":[{"type":"abc","config":{"a":"b"}}]}'
        '{}'                                                | '{}'
    }

    def "getWorkflowDescriptionTree"() {
        setup:
        println "get workflow description tree"
        String project = "AProject"
        def job1 = new ScheduledExecution(
                jobName: 'test',
                groupPath: "group",
                project: project,
                description: 'test job',
                workflow: new Workflow(commands: [
                        new CommandExec(argString: "blah blah", adhocLocalString: "test2")
                ])
        )
        job1.save()
        def schedEx = new ScheduledExecution(
                jobName: 'testUploadErrorHandlers',
                groupPath: "testgroup",
                project: project,
                description: 'desc',
                workflow: new Workflow(commands: [
                        new CommandExec(adhocExecution: true, adhocRemoteString: "echo test",
                                        errorHandler: new CommandExec(adhocExecution: true,
                                                                      adhocRemoteString: "echo this is an errorhandler")),
                        new CommandExec(argString: "blah blah", adhocLocalString: "test2"),
                        new CommandExec(argString: "blah3 blah3", adhocFilepath: "test3"),
                        new JobExec(jobGroup: "group", jobName: "test"),

                ])
        )
        schedEx.save()

        when:
        def output = service.getWorkflowDescriptionTree(project,schedEx.workflow,false)

        then:
        output[0].exec == "exec"
        output[0].errorhandler.exec == "exec"
        output[1].script == "script"
        output[2].scriptfile == "scriptfile"
        output[2].expandTokenInScriptFile == false
        output[3].jobref.group == "group"
        output[3].jobref.name == "test"
        output[3].jobId
        output[3].workflow == [[script:"script"]]

    }

    def "load remote options timeout configuration"(){
        given:
            def fwkservice=Mock(FrameworkService)
            defineBeans{
                frameworkService(InstanceFactoryBean,fwkservice)
            }
            service.configurationService = Mock(ConfigurationService)
            service.frameworkService = fwkservice
            def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2')
            se.addToOptions(new Option(
                name:'test',
                realValuesUrl: new URL('file://test')
            ))
            se.save()
            def input=[:]
            ScheduledExecutionController.metaClass.static.getRemoteJSON={String url,int vtimeout, int vcontimeout, int vretry, boolean disableRemoteJsonCheck->
                input.url=url
                input.timeout=vtimeout
                input.contimeout=vcontimeout
                input.retry=vretry
                [
                    json: [
                        'some', 'option', 'values'
                    ]
                ]
            }
        when:
            def result = service.loadOptionsRemoteValues(se,[option:'test'],'auser')
        then:
            input.url=='file://test'
            input.timeout==expectTimeout
            input.contimeout==expectConTimeout
            input.retry==expectRetry
            result.values==['some','option','values']
            1 * service.configurationService.getString("jobs.options.remoteUrlTimeout")>>timeout?.toString()
            _ * service.configurationService.getInteger("jobs.options.remoteUrlTimeout", null) >> timeout
            1 * service.configurationService.getString("jobs.options.remoteUrlConnectionTimeout")>>conTimeout?.toString()
            _ * service.configurationService.getInteger("jobs.options.remoteUrlConnectionTimeout", null) >> conTimeout
            1 * service.configurationService.getString("jobs.options.remoteUrlRetry")>>retry?.toString()
            _ * service.configurationService.getInteger("jobs.options.remoteUrlRetry", null) >> retry
            1 * service.frameworkService.getRundeckFramework()>>Mock(IFramework){
                1 * getFrameworkProjectMgr()>>Mock(ProjectManager){
                    1 * loadProjectConfig('testProject')
                }
            }

        where:
            timeout | conTimeout | retry | expectTimeout | expectConTimeout | expectRetry
            1       | 1          | 1     | 1             | 1                | 1
            2       | 2          | 2     | 2             | 2                | 2
            null    | null       | null  | 10            | 0                | 5
    }
}

class TriggersExtenderImpl implements TriggersExtender {

    def job

    TriggersExtenderImpl(job) {
        this.job = job
    }

    @Override
    void extendTriggers(Object jobDetail, List<TriggerBuilderHelper> triggerBuilderHelpers) {
        triggerBuilderHelpers << new TriggerBuilderHelper(){

            LocalJobSchedulesManager schedulesManager = new LocalJobSchedulesManager()
            @Override
            Object getTriggerBuilder() {
                schedulesManager.createTriggerBuilder(this.job).getTriggerBuilder()
            }

            @Override
            Map getParams() {
                return null
            }

            @Override
            Object getTimeZone() {
                return null
            }
        }
    }

    def "test Create Job Exclude InactivePlugins"(){
        given:
        def se = new ScheduledExecution(jobName: 'monkey1', project: 'testProject', description: 'blah2')
        se.save()

        def auth = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'test'
        }

        def iRundeckProject = Mock(IRundeckProject){
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")


        def pluginControlService = Mock(PluginControlService){
            isDisabledPlugin("test",_)>>true
        }

        def frameworkService  = Mock(FrameworkService){
            getRundeckFramework()>>Mock(Framework){
                getFrameworkNodeName()>>'fwnode'
                getFrameworkProjectMgr()>> Mock(ProjectManager) {
                    existsFrameworkProject(se.project) >> true
                    getFrameworkProject(_) >> iRundeckProject
                }
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
            getProjectGlobals(_) >> [:]
            getPluginControlService(_) >> pluginControlService
            getNodeStepPluginDescriptions() >> [[name:'test'],[name:'test2']]
        }

        service.frameworkService = frameworkService
        service.pluginService = Mock(PluginService){
            listPlugins() >> []
        }
        service.jobSchedulesService = Mock(SchedulesManager){
        }

        service.rundeckAuthorizedServicesProvider = Mock(AuthorizedServicesProvider){
            getServicesWith(_)>> Mock(Services)
        }

        service.notificationService = Mock(NotificationService){
        }
        service.orchestratorPluginService=Mock(OrchestratorPluginService)
        service.executionLifecyclePluginService = Mock(ExecutionLifecyclePluginService)
        service.rundeckJobDefinitionManager=Mock(RundeckJobDefinitionManager)

        when:

        Map params = [id: se.id, project: se.project]

        def result = service.prepareCreateEditJob(params, se, "create", auth)

        then:
        result!=null
        result.nodeStepDescriptions !=null
        result.nodeStepDescriptions.size() == 1

    }
}
