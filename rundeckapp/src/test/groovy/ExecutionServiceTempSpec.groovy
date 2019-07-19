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


import com.dtolabs.rundeck.app.support.QueueQuery
import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.common.SelectorUtils
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.execution.ExecutionItemFactory
import com.dtolabs.rundeck.execution.JobRefCommand
import com.dtolabs.rundeck.server.authorization.AuthConstants
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import grails.test.mixin.Mock
import org.grails.plugins.metricsweb.MetricService
import org.junit.Assert
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.StorageException
import org.springframework.context.MessageSource
import rundeck.*
import rundeck.services.*
import spock.lang.Specification
import spock.lang.Unroll

import java.time.ZoneId

/**
 * Created by greg on 2/17/15.
 */
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, Option, ExecReport, LogFileStorageRequest, ReferencedExecution])
class ExecutionServiceTempSpec extends Specification{
    ExecutionService service
    def setup(){
        service = new ExecutionService()
        service.jobPluginService = Mock(JobPluginService)
    }

    def "loadSecureOptionStorageDefaults replace job vars"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec(
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                )
                        ]
                ),
                options: [
                        new Option(
                                name: 'pass',
                                secureInput: true,
                                secureExposed: false,
                                defaultStoragePath: 'keys/${job.username}/pass'
                        )
                ]
        )
        job.save()

        Map secureOptsExposed = [:]
        Map secureOpts = [:]
        def authContext = Mock(AuthContext)
        Map<String, String> args = FrameworkService.parseOptsFromString(job.argString)
        service.storageService = Mock(StorageService)

        def jobcontext = [:]
        jobcontext.id = job.extid
        jobcontext.name = job.jobName
        jobcontext.group = job.groupPath
        jobcontext.project = job.project
        jobcontext.username = username
        jobcontext['user.name'] = jobcontext.username


        when:
        service.loadSecureOptionStorageDefaults(job, secureOptsExposed, secureOpts, authContext,false,
                args,jobcontext)

        then:
        service.storageService.storageTreeWithContext(authContext) >> Mock(KeyStorageTree) {
            hasPassword('keys/admin/pass') >> true
            readPassword('keys/admin/pass') >> {
                return 'pass1'.bytes
            }
            hasPassword('keys/dev/pass') >> true
            readPassword('keys/dev/pass') >> {
                return 'pass2'.bytes
            }
            hasPassword('keys/op/pass') >> true
            readPassword('keys/op/pass') >> {
                return 'pass3'.bytes
            }
            hasPassword(_) >> true
            readPassword(_) >> {
                return ''.bytes
            }
        }
        expectedpass == secureOpts['pass']
        where:
        expectedpass    | username
        'pass1'         | 'admin'
        'pass2'         | 'dev'
        'pass3'         | 'op'
        ''              | 'user'

    }


    def "loadSecureOptionStorageDefaults node deferred variables"() {
        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [
                                new CommandExec(
                                        adhocRemoteString: 'test buddy',
                                        argString: '-delay 12 -monkey cheese -particle'
                                )
                        ]
                ),
                options: [
                        new Option(
                                name: 'pass',
                                secureInput: true,
                                secureExposed: false,
                                defaultStoragePath: securepath
                        )
                ]
        )
        job.save()

        Map secureOptsExposed = [:]
        Map secureOpts = [:]
        def authContext = Mock(AuthContext)
        Map<String, String> args = FrameworkService.parseOptsFromString(job.argString)
        service.storageService = Mock(StorageService)

        def jobcontext = [:]
        jobcontext.id = job.extid
        jobcontext.name = job.jobName
        jobcontext.group = job.groupPath
        jobcontext.project = job.project
        jobcontext.username = 'admin'
        jobcontext['user.name'] = jobcontext.username

        def secureOptionNodeDeferred = [:]


        when:
        service.loadSecureOptionStorageDefaults(job, secureOptsExposed, secureOpts, authContext,false,
                args,jobcontext,secureOptionNodeDeferred)

        then:
        secureOptionNodeDeferred
        secureOptionNodeDeferred.size()==1
        secureOptionNodeDeferred['pass']
        secureOptionNodeDeferred['pass']==securepath

        where:
        securepath                         | _
        'keys/${node.hostname}/pass'       | _
        'keys/${node.username}/pass'       | _

    }

    def "max multiple executions with error"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                multipleExecutions: multipleExecutions,
                maxMultipleExecutions: max
        )
        job.save()
        def exec = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'userB',
                project: 'AProject'
        ).save()
        def exec2 = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user',
                project: 'AProject'
        ).save()
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        Execution e2 = service.createExecution(job, authContext, null, ['extra.option.test': '12', executionType: 'user'])

        then:
        ExecutionServiceException e = thrown()
        e.code == 'conflict'
        e.message ==~ /.*is currently being executed.*/


        where:
        max     | multipleExecutions
        2       | true
        1       | true
        null    | false
    }

    def "max multiple executions without error"() {

        given:
        ScheduledExecution job = new ScheduledExecution(
                jobName: 'blue',
                project: 'AProject',
                groupPath: 'some/where',
                description: 'a job',
                argString: '-a b -c d',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ),
                retry: '1',
                multipleExecutions: true,
                maxMultipleExecutions: max
        )
        job.save()
        def exec = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'userB',
                project: 'AProject'
        ).save()
        def exec2 = new Execution(
                scheduledExecution: job,
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user',
                project: 'AProject'
        ).save()
        service.frameworkService = Stub(FrameworkService) {
            getServerUUID() >> null
        }
        def authContext = Mock(UserAndRolesAuthContext) {
            getUsername() >> 'user1'
        }
        when:
        Execution e2 = service.createExecution(job, authContext, null, ['extra.option.test': '12', executionType: 'user'])

        then:
        e2


        where:
        max     | _
        0       | _
        null    | _
        3       | _
    }
}
