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

import com.dtolabs.rundeck.app.internal.logging.DefaultLogEvent
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.data.BaseDataContext
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.configuration.RuntimePropertyResolver
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import grails.plugins.mail.MailMessageBuilder
import grails.plugins.mail.MailService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.web.mapping.LinkGenerator
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import rundeck.services.logging.ExecutionLogReader
import rundeck.services.logging.ExecutionLogState
import spock.lang.Specification

/**
 * Created by greg on 7/12/16.
 */
@TestFor(NotificationService)
@Mock([Execution, ScheduledExecution, Notification, Workflow, CommandExec, User, ScheduledExecutionStats])
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

    def "custom plugin notification"() {
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
                        type: 'HttpNotificationPlugin',
                        content: '{"method":"","url":""}',
                        configuration: '{"method":"","url":""}'
                )
        ]
        job.save()
        service.frameworkService = Mock(FrameworkService) {
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService()
            }
            _* getPluginControlService(_) >> Mock(PluginControlService)

        }

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.pluginService = Mock(PluginService)


        def config = [method:null, url:null]

        when:
        service.triggerJobNotification('start', job, content)

        then:
        1 * service.frameworkService.getFrameworkPropertyResolver(_, config)

    }

    def "custom plugin notification no configuration"() {
        given:
        def (job, execution) = createTestJob()
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) 
        ]

        job.notifications = [
                new Notification(
                        eventTrigger: 'onstart',
                        type: 'SimpleNotificationPlugin'
                )
        ]
        job.save()
        service.frameworkService = Mock(FrameworkService) {
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService()
            }
            _* getPluginControlService(_) >> Mock(PluginControlService)

        }

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.pluginService = Mock(PluginService)

        when:
        service.triggerJobNotification('start', job, content)

        then:
        1 * service.frameworkService.getFrameworkPropertyResolver(_, null)

    }

    def "custom plugin notification job context map"() {
        given:
        def (job, execution) = createTestJob()
        job.scheduled=true
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    getDataContext() >> new BaseDataContext([globals: [testmail: 'bob@example.com']])
                }
        ]

        job.notifications = [
                new Notification(
                        eventTrigger: 'onstart',
                        type: 'TestPlugin',
                        content: '{"method":"","url":""}',
                        configuration: '{"method":"","url":""}'
                )
        ]
        job.save()
        service.frameworkService = Mock(FrameworkService) {
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService()
            }
            _* getPluginControlService(_) >> Mock(PluginControlService)

        }

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.pluginService = Mock(PluginService)

        def mockPlugin = Mock(NotificationPlugin){
        }


        def config = [method:null, url:null]

        when:
        def ret = service.triggerJobNotification('start', job, content)

        then:
        1 * service.frameworkService.getFrameworkPropertyResolver(_, config)
        1 * service.pluginService.configurePlugin(_,_,_,_)>>new ConfiguredPlugin(
                mockPlugin,
                [:]
        )
        1 * mockPlugin.postNotification(_, _, _)>>{ trigger, data, allConfig ->
            println(data)
            System.err.println(data.job.schedule)
            if(data?.job?.schedule=='0 0 0 ? * * *'){
                return true
            }
            return false
        }
        ret

    }


    def "email notification default attaching"() {
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
                        eventTrigger: 'onsuccess',
                        type: 'email',
                        content: '{"recipients":"mail@example.com","subject":"test","attachLog":true}'
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

        def reader = new ExecutionLogReader(state: ExecutionLogState.AVAILABLE)
        reader.reader = new TestReader(logs:
                [
                        new DefaultLogEvent(
                                eventType: LogUtil.EVENT_TYPE_LOG,
                                datetime: new Date(),
                                message: "log",
                                metadata: [:],
                                loglevel: LogLevel.NORMAL
                        ),
                ]
        )
        service.loggingService=Mock(LoggingService)

        when:
        def result = service.triggerJobNotification('success', job, content)

        then:
        1 * service.loggingService.getLogReader(_) >> reader
        1 * service.mailService.sendMail(_)
        result
    }


    class TestReader implements StreamingLogReader {
        List<LogEvent> logs;
        int index = -1;

        @Override
        void openStream(Long offset) throws IOException {
            index = offset;
        }

        @Override
        long getTotalSize() {
            return logs.size()
        }

        @Override
        Date getLastModified() {
            return null
        }

        @Override
        void close() throws IOException {
            index = -1
        }

        @Override
        boolean isComplete() {
            return index > logs.size()
        }

        @Override
        long getOffset() {
            return index
        }

        @Override
        boolean hasNext() {
            return index < logs.size()
        }

        @Override
        LogEvent next() {
            return logs[index++]
        }

        @Override
        void remove() {

        }
    }
}
