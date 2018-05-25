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

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.execution.ExecutionContext
import grails.plugins.mail.MailMessageBuilder
import grails.plugins.mail.MailService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.web.mapping.LinkGenerator
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.Workflow
import spock.lang.Specification

/**
 * Created by greg on 7/12/16.
 */
@TestFor(NotificationService)
@Mock([Execution, ScheduledExecution, Notification, Workflow, CommandExec, User])
class NotificationServiceSpec extends Specification {

    private List createTestJob() {

        def job = new ScheduledExecution(
                uuid: 'test1',
                jobName: 'red color',
                project: 'Test',
                groupPath: 'some',
                description: 'a job',
                argString: '-a b -c d',
                user: 'bob',
                workflow: new Workflow(
                        keepgoing: true,
                        commands: [new CommandExec(
                                [adhocRemoteString: 'test buddy', argString: '-delay 12 -monkey cheese -particle']
                        )]
                ).save(),
                ).save()
        def execution = new Execution(
                project: 'Test',
                scheduledExecution: job,
                user: 'bob',
                status: 'succeeded',
                dateStarted: new Date(),
                dateCompleted: new Date()
        ).save()
        [job, execution]
    }

    def "testsetup"() {
        given:
        def (job, execution) = createTestJob()
        when:
        job.validate()
        execution.validate()
        job = job.save()
        execution = execution.save()

        then:
        job != null
        execution != null
        !job.errors.hasErrors()
        !execution.errors.hasErrors()
        job.id != null
        execution.id != null
    }

    def "mail recipients in context var"() {
        given:
        def (job, execution) = createTestJob()
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    getDataContext() >> new BaseDataContext([globals: globals])
                }
        ]
        job.notifications = [
                new Notification(
                        eventTrigger: 'onstart',
                        type: 'email',
                        content: recipients
                )
        ]
        job.save()
        service.mailService = Mock(MailService)
        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.frameworkService = Mock(FrameworkService) {
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService()
            }
            _ * getPluginControlService(_) >> Mock(PluginControlService)

        }
        service.orchestratorPluginService = Mock(OrchestratorPluginService)
        service.pluginService = Mock(PluginService)
        def mailbuilder = Mock(MailMessageBuilder)

        when:
        def result = service.triggerJobNotification('start', job, content)

        then:
        result
        (count) * service.mailService.sendMail(_) >> { args ->
            args[0].delegate = mailbuilder
            args[0].call()
        }
        shouldSend.each {
            1 * mailbuilder.to(it)
        }

        where:
        globals                                                  | recipients                              |
                count                                                                                          |
                shouldSend
        [testmail: 'bob@example.com']                            | '${globals.testmail}, mail@example.com' |
                2                                                                                              |
                ['bob@example.com', 'mail@example.com']
        [testmail: 'bob@example.com, alice@example.com']         | '${globals.testmail}, mail@example.com' |
                3                                                                                              |
                ['bob@example.com', 'alice@example.com', 'mail@example.com']
        [testmail: 'bob@example.com', test2: 'fred@example.com'] | '${globals.testmail}, ${globals.test2}' |
                2                                                                                              |
                ['bob@example.com', 'fred@example.com']
    }

    def "mail recipients missing context var"() {
        given:
        def (job, execution) = createTestJob()
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    getDataContext() >> new BaseDataContext([globals: [testmail: 'bob@example.com']])
                }
        ]
        job.notifications = [
                new Notification(
                        eventTrigger: 'onstart',
                        type: 'email',
                        content: '${globals.testmail2}, mail@example.com'
                )
        ]
        job.save()
        service.frameworkService = Mock(FrameworkService) {
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService()
            }
            _ * getPluginControlService(_) >> Mock(PluginControlService)

        }
        service.mailService = Mock(MailService)
        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }


        when:
        def result = service.triggerJobNotification('start', job, content)

        then:
        result
        1 * service.mailService.sendMail(_)
    }
}
