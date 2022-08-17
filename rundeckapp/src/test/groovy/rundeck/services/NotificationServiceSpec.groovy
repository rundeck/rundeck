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
import com.dtolabs.rundeck.core.data.SharedDataContextUtils
import com.dtolabs.rundeck.core.dispatcher.ContextView
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext
import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.logging.LogLevel
import com.dtolabs.rundeck.core.logging.LogUtil
import com.dtolabs.rundeck.core.logging.StreamingLogReader
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.AcceptsServices
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import grails.plugins.mail.MailMessageBuilder
import grails.plugins.mail.MailService
import grails.test.hibernate.HibernateSpec
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import grails.testing.web.GrailsWebUnitTest
import grails.util.Holders
import grails.web.mapping.LinkGenerator
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.rundeck.app.spi.Services
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import rundeck.services.logging.ExecutionLogReader
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import rundeck.services.logging.WorkflowStateFileLoader
import spock.lang.Specification
import spock.lang.Unroll
import testhelper.RundeckHibernateSpec

/**
 * Created by greg on 7/12/16.
 */
class NotificationServiceSpec extends RundeckHibernateSpec implements ServiceUnitTest<NotificationService>, GrailsWebUnitTest {

    List<Class> getDomainClasses() { [Execution, ScheduledExecution, Notification, Workflow, CommandExec, User, ScheduledExecutionStats] }


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
        def globalContext = new BaseDataContext([globals: globals])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), globalContext)
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    1 * getSharedDataContext() >> shared
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
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }
        service.configurationService = Mock(ConfigurationService)
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
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }
        service.configurationService = Mock(ConfigurationService)

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
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }


        def config = [method:null, url:null]

        when:
        service.triggerJobNotification('start', job, content)

        then:
        1 * service.frameworkService.getProjectPropertyResolver(_)

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
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }

        when:
        service.triggerJobNotification('start', job, content)

        then:
        1 * service.frameworkService.getProjectPropertyResolver(_)

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
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }

        def mockPlugin = Mock(NotificationPlugin){
        }


        def config = [method:null, url:null]

        when:
        def ret = service.triggerJobNotification('start', job, content)

        then:
        1 * service.pluginService.configurePlugin(_,_,_,_,_)>>new ConfiguredPlugin(
                mockPlugin,
                [:]
        )
        1 * mockPlugin.postNotification(_, _, _)>>{ trigger, data, allConfig ->
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
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }
        service.configurationService = Mock(ConfigurationService)

        def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
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

    def "email notification attach unsanitized html"() {
        given:
        ExpandoMetaClass.disableGlobally()
        File tmpTemplate = File.createTempFile("tmp","tmplate")
        tmpTemplate << 'My Email template <span style="color: red;">${logoutput.data}</span>'

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
                        content: '{"recipients":"mail@example.com","subject":"test","attachLog":true,"attachLogInline":true}'
                )
        ]
        job.save()
        service.metaClass.checkAllowUnsanitized = { final String project -> true }
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
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }
        service.configurationService = Mock(ConfigurationService){
            getString("mail.template.file")>>tmpTemplate.getAbsolutePath()
        }

        def reader = new ExecutionLogReader(state: ExecutionFileState.AVAILABLE)
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
        def messageBuilder = Mock(MailMessageBuilder)

        when:
        def result = service.triggerJobNotification('success', job, content)

        then:
        1 * service.loggingService.getLogReader(_) >> reader
        1 * service.mailService.sendMail(_) >> { Closure callable ->
            callable.delegate = messageBuilder
            callable.resolveStrategy = Closure.DELEGATE_FIRST
            callable.call(messageBuilder)
        }
        1 * messageBuilder.html('My Email template <span style="color: red;">log\n</span>')

        result
    }

    def "plugin notification shows fixed success node list"() {
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
        execution.succeededNodeList='a,b,c'
        execution.save()
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

        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>['a']
        }

        def config = [method:null, url:null]

        when:
        def ret = service.triggerJobNotification('start', job, content)

        then:
        1 * service.pluginService.configurePlugin(_,_,_,_,_)>>new ConfiguredPlugin(
                mockPlugin,
                [:]
        )
        1 * mockPlugin.postNotification(_, _, _)>>{ trigger, data, allConfig ->
            if(data.succeededNodeListString=='a'){
                return true
            }
            return false
        }
        ret

    }

    def "plugin notification replace execution context variables config"() {
        given:
        def (job, execution) = createTestJob()
        job.scheduled=true

        def globalContext = new BaseDataContext([globals: [testmail: 'bob@example.com'], job:[name: job.jobName, project: job.project, id: job.uuid]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), globalContext)
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    1 * getSharedDataContext() >> shared
                }
        ]

        job.notifications = [
                new Notification(
                        eventTrigger: 'onstart',
                        type: 'TestPlugin',
                        content: contentData
                )
        ]
        job.save()
        execution.failedNodeList='a,b,c'
        execution.succeededNodeList='f,g'
        execution.save()

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

        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>['f']
        }
        def testResult=configResult.collectEntries{[it.key,it.value.replaceAll('__',execution.id.toString())]}


        when:
        def ret = service.triggerJobNotification('start', job, content)

        then:
        1 * service.frameworkService.getProjectPropertyResolver(_)
        1 * service.pluginService.configurePlugin(_,_,_,_,_)>>new ConfiguredPlugin(
                mockPlugin,
                [:]
        )
        1 * mockPlugin.postNotification(_, _, _)>>{ trigger, data, allConfig ->
            return true
        }

        ret

        where:
        contentData                                                                                     | configResult
        '{"info":"Execution ID: ${execution.id}", "failedNodes":"${execution.failedNodeListString}"}'   | ['failedNodes':'a,b,c', 'info':'Execution ID: __']
        '{"body":"Execution ID: ${execution.id}, Job Name: ${job.name}, succeededNode: ${execution.succeededNodeListString}"}' | ['body':'Execution ID: __, Job Name: red color, succeededNode: f']

    }

    def "generate notification context test"() {
        given:
        def (job, execution) = createTestJob()
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }
        when:
        def globalContext = new BaseDataContext([globals: [testmail: 'bob@example.com'], job:[name: job.jobName, project: job.project, id: job.uuid]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), globalContext)
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    1 * getSharedDataContext() >> shared
                }
        ]

        def contentData = "{'data':'value'}"

        job.notifications = [
                new Notification(
                        eventTrigger: 'onstart',
                        type: 'TestPlugin',
                        content: contentData
                )
        ]
        job.save()

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.executionService=Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>['a','b']
        }


        def execMap = null
        Map context = null
        (context, execMap) = service.generateNotificationContext(execution, content, job)

        then:
        execMap != null
        context != null
        execMap.job !=null
        execMap.context !=null
        execMap.projectHref !=null
        context.execution !=null
        context.globals !=null
        context.job !=null
        execMap.succeededNodeList == ['a','b']
        execMap.succeededNodeListString == 'a,b'
    }

    def "generate notification context with options test"() {
        given:
        def (job, execution) = createTestJob()
        service.executionService = Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>[]
        }
        when:
        def globalContext = new BaseDataContext(
                [
                        globals: [testmail: 'bob@example.com'],
                        options: [version: 1, opt1: "value1"],
                        job:[name: job.jobName, project: job.project, id: job.uuid]
                ])

        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), globalContext)

        def sharedDataContext = WFSharedContext.with(shared)

        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    1 * getSharedDataContext() >> sharedDataContext
                }
        ]

        def contentData = "{'data':'value'}"

        job.notifications = [
                new Notification(
                        eventTrigger: 'onstart',
                        type: 'TestPlugin',
                        content: contentData
                )
        ]
        job.save()

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.executionService=Mock(ExecutionService){
            getEffectiveSuccessNodeList(_)>>['a','b']
        }


        def execMap = null
        Map context = null
        (context, execMap) = service.generateNotificationContext(execution, content, job)

        then:
        execMap != null
        context != null
        execMap.job !=null
        execMap.context !=null
        execMap.projectHref !=null
        context.execution !=null
        context.globals !=null
        context.options !=null
        context.job !=null
        execMap.succeededNodeList == ['a','b']
        execMap.succeededNodeListString == 'a,b'
    }

    @Unroll
    def "notification webhook format #format"() {
        setup:
        def (job, execution) = createTestJob()
        def globalContext = new BaseDataContext([globals: [:]])
        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), globalContext)
        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    getSharedDataContext() >> shared
                }
        ]
        service.frameworkService = Mock(FrameworkService) {
            _ * getRundeckFramework() >> Mock(Framework) {
                _ * getWorkflowStrategyService()
            }
            _ * getPluginControlService(_) >> Mock(PluginControlService)

        }

        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"))
        String endpoint = httpServer.url("hook/endpoint").toString()
        String chkBody = null
        service.metaClass.createJsonNotificationPayload = { String trigger, Execution exec ->
            chkBody = jsonBody
        }
        service.metaClass.createXmlNotificationPayload = { String trigger, Execution exec ->
            chkBody = xmlBody
        }

        job.notifications = [new Notification(eventTrigger:"onsuccess",type: "url",format:format,content:endpoint)]
        job.save()


        when:
        service.triggerJobNotification("success",job,content)
        RecordedRequest rq = httpServer.takeRequest()

        then:
        rq.body.readUtf8() == chkBody

        where:
        format  | jsonBody   | xmlBody
        "json"  | "{}"       | null
        "xml"   | null       | "<xml></xml>"
        null    | null       | "<xml></xml>"
    }

    @Unroll
    def "postDataUrl tests"() {
        setup:
        if(securityKey) Holders.config.put("rundeck.notification.webhookSecurityKey",securityKey)
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"))
        String endpoint = httpServer.url("hook/endpoint").toString()

        when:
        def result = NotificationService.postDataUrl(endpoint,format,payload,"success","suceeded","1234")
        RecordedRequest rq = httpServer.takeRequest()

        then:
        result.success
        (rq.getHeader("X-RunDeck-Notification-SHA256-Digest") != null) == generatedDigestExists
        rq.getHeader("Content-Type") == expectedContentType
        rq.body.readUtf8() == payload

        where:
        format | expectedContentType               | payload                                              | securityKey | generatedDigestExists
        "json" | "application/json; charset=UTF-8" | '{"job":"1234"}'                                     | null        | false
        "json" | "application/json; charset=UTF-8" | '{"job":"1234"}'                                     | "MySecret"  | true
        "xml"  | "text/xml; charset=UTF-8"         | '<xml><notifcation job="1234"></notification></xml>' | null        | true
        "xml"  | "text/xml; charset=UTF-8"         | '<xml><notifcation job="1234"></notification></xml>' | "MySecret"  | true
    }

    @Unroll
    def "postDataUrl tests with credentials"() {
        setup:
        if(securityKey) Holders.config.put("rundeck.notification.webhookSecurityKey",securityKey)
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"))
        String endpoint = httpServer.url("hook/endpoint").toString()
        endpoint = endpoint.replaceAll('localhost', 'user:pswd@localhost')

        when:
        def result = NotificationService.postDataUrl(endpoint,format,payload,"success","suceeded","1234")
        RecordedRequest rq = httpServer.takeRequest()

        then:
        result.success
        (rq.getHeader("X-RunDeck-Notification-SHA256-Digest") != null) == generatedDigestExists
        rq.getHeader("Content-Type") == expectedContentType
        rq.getHeader("Authorization")?.startsWith("Basic")
        rq.body.readUtf8() == payload

        where:
        format | expectedContentType               | payload                                              | securityKey | generatedDigestExists
        "json" | "application/json; charset=UTF-8" | '{"job":"1234"}'                                     | null        | true
        "json" | "application/json; charset=UTF-8" | '{"job":"1234"}'                                     | "MySecret"  | true
        "xml"  | "text/xml; charset=UTF-8"         | '<xml><notifcation job="1234"></notification></xml>' | null        | true
        "xml"  | "text/xml; charset=UTF-8"         | '<xml><notifcation job="1234"></notification></xml>' | "MySecret"  | true
    }

    @Unroll
    def "postDataUrl using GET method"() {
        setup:
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"))
        String endpoint = httpServer.url("hook/endpoint").toString()
        String format = "json"
        String payload = '{"job":"1234"}'

        when:
        def result = NotificationService.postDataUrl(endpoint,format,payload,"success","suceeded","1234", NotificationService.GET)
        RecordedRequest rq = httpServer.takeRequest()
        rq.getMethod() == "GET"

        then:
        result.success
    }

    @Unroll
    def "postDataUrl using GET method with credentials"() {
        setup:
        MockWebServer httpServer = new MockWebServer()
        httpServer.start()
        httpServer.enqueue(new MockResponse().setResponseCode(200).setBody("ok"))
        String endpoint = httpServer.url("hook/endpoint").toString()
        endpoint = endpoint.replaceAll('localhost', 'user:pswd@localhost')
        String format = "json"
        String payload = '{"job":"1234"}'

        when:
        def result = NotificationService.postDataUrl(endpoint,format,payload,"success","suceeded","1234", NotificationService.GET)
        RecordedRequest rq = httpServer.takeRequest()
        rq.getMethod() == "GET"
        rq.getHeader("Authorization")?.startsWith("Basic")

        then:
        result.success
    }

    @Unroll
    def "get dynamic properties notification plugins tests"() {
        given:

        def project = "TestProject"

        def fakePluginDesc1 = new PluginApiServiceSpec.FakePluginDescription()
        fakePluginDesc1.name = 'XYZfake'

        service.pluginService = Mock(PluginService){
            listPlugins(_,_)>>[
                    XYZfake: new DescribedPlugin<NotificationPlugin>(null, fakePluginDesc1, 'XYZfake', null, null),
            ]
        }
        service.frameworkService = Mock(FrameworkService) {
            _ * getRundeckFramework() >> Mock(Framework)
        }

        Services services = Mock(Services)

        when:
        service.listNotificationPluginsDynamicProperties(project, services)

        then:
        1* service.pluginService.getDynamicProperties(_,ServiceNameConstants.Notification,_,project,services)

    }

    def "generate notification with globals context"() {
        given:
        def (job, execution) = createTestJob()

        def globalContext = new BaseDataContext([globals: [testmail: 'bob@example.com'], job:[name: job.jobName, project: job.project, id: job.uuid]])

        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), globalContext)

        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    1 * getSharedDataContext() >> shared
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
            _ * getPluginControlService(_) >> Mock(PluginControlService)

        }

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.pluginService = Mock(PluginService)
        service.executionService = Mock(ExecutionService) {
            getEffectiveSuccessNodeList(_) >> []
        }

        when:
        service.triggerJobNotification('start', job, content)

        then:
        0 * service.frameworkService.getProjectGlobals("Test")

    }

    def "generate notification without globals context"() {
        given:
        def (job, execution) = createTestJob()

        def content = [
                execution: execution,
                context  : null
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
            _ * getPluginControlService(_) >> Mock(PluginControlService)

        }

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.pluginService = Mock(PluginService)
        service.executionService = Mock(ExecutionService) {
            getEffectiveSuccessNodeList(_) >> []
        }

        when:
        service.triggerJobNotification('start', job, content)

        then:
        1 * service.frameworkService.getProjectGlobals("Test")

    }

    def "export variables replace values in webhook url"() {
        given:
        def (job, execution) = createTestJob()
        def exportMap = [export:[export:[var:"testVar"], job:[id:222,name: "testName"], execution:[user:"fakeUser", status: "failed"]]]

        when:
        String url = 'http://test.com?id=${job.id}&user=${execution.user}&status=${execution.status}&exportVar=${export.var}&jobName=${job.name}'
        String updatedUrl = service.expandWebhookNotificationUrl(url, execution, job,"trigger", exportMap.export )

        then:
        updatedUrl.equals("http://test.com?id=test1&user=bob&status=succeeded&exportVar=testVar&jobName=red+color")
        job.jobName.toString().encodeAsURL() == "red+color"

        when:
        url = 'http://test.com?user=${execution.user}&status=${execution.status}&jobName=${job.name}'
        updatedUrl = service.expandWebhookNotificationUrl(url, execution, job,"trigger", null )

        then:
        updatedUrl.equals("http://test.com?user=bob&status=succeeded&jobName=red+color")
        job.jobName.toString().encodeAsURL() == "red+color"

    }

    def "generate notification with export context"() {
        given:
        def (job, execution) = createTestJob()

        def globalContext = new BaseDataContext([globals: [testmail: 'bob@example.com'], job:[name: job.jobName, project: job.project, id: job.uuid],
                                                 ])

        def shared = SharedDataContextUtils.sharedContext()
        shared.merge(ContextView.global(), globalContext)

        def content = [
                execution: execution,
                context  : Mock(ExecutionContext) {
                    1 * getSharedDataContext() >> shared
                },
                export: [exportVarGroup1:[testName: 'test', description: 'test description'],
                         exportVarGroup2:[testSubject: 'test123', testTitle: 'test2']]

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
            _ * getPluginControlService(_) >> Mock(PluginControlService)

        }

        service.grailsLinkGenerator = Mock(LinkGenerator) {
            _ * link(*_) >> 'alink'
        }
        service.pluginService = Mock(PluginService)
        service.executionService = Mock(ExecutionService) {
            getEffectiveSuccessNodeList(_) >> []
        }

        when:

        def execMap = null
        Map context = null
        (context, execMap) = service.generateNotificationContext(execution, content, job)

        then:
        context != null
        context.exportVarGroup1 != null
        context.exportVarGroup2 != null
        context.exportVarGroup1.description == 'test description'
        context.exportVarGroup1.testName == 'test'
        context.exportVarGroup2.testSubject == 'test123'
        context.exportVarGroup2.testTitle == 'test2'

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
