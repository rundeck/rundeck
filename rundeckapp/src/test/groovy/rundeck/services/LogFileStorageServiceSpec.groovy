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

import asset.pipeline.grails.LinkGenerator
import com.dtolabs.rundeck.app.internal.logging.FSStreamingLogReader
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageOptions
import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.server.plugins.services.ExecutionFileStoragePluginProviderService
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.config.ConfigService
import org.rundeck.app.data.providers.logstorage.GormLogFileStorageRequestProvider
import org.rundeck.app.services.ExecutionFile
import org.rundeck.app.services.ExecutionFileProducer
import org.springframework.context.ApplicationContext
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.core.task.SimpleAsyncTaskExecutor
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.ScheduledExecution
import rundeck.services.logging.ProducedExecutionFile
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

import static com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState.AVAILABLE
import static com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState.AVAILABLE_PARTIAL
import static com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState.AVAILABLE_REMOTE
import static com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState.AVAILABLE_REMOTE_PARTIAL
import static com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState.NOT_FOUND
import static com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState.PENDING_REMOTE
import static com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileState.WAITING

/**
 * Created by greg on 3/28/16.
 */
class LogFileStorageServiceSpec extends Specification implements ServiceUnitTest<LogFileStorageService>, DataTest {
    File tempDir

    def setupSpec() { mockDomains LogFileStorageRequest, Execution }

    def setup() {
        tempDir = Files.createTempDirectory("LogFileStorageServiceSpec").toFile()
        tempDir.deleteOnExit()
        service.configurationService=Mock(ConfigService){
            _ * getString(LogFileStorageService.RESUME_INCOMPLETE_STRATEGY, _) >> 'delayed'
            _ * getString(LogFileStorageService.FILE_STORAGE_PLUGIN, _) >> 'test1'
        }
        service.logFileStorageRequestProvider = new GormLogFileStorageRequestProvider()
    }

    def "resume incomplete delayed"() {
        given:
        def mockPlugin = Mock(ExecutionFileStoragePlugin){
            1 * initialize({args->
                args.username==testuser
            })
        }
        service.pluginService = Mock(PluginService) {
            1 * configurePlugin('test1', _, _ as PropertyResolverFactory.Factory, PropertyScope.Instance) >> new ConfiguredPlugin(
                    mockPlugin,
                    [:]
            )
        }
        service.frameworkService = Mock(FrameworkService){

            1 * getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
        }
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def e1 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user1',
                               project: 'test',
                               serverNodeUUID: null
        ).save()

        def l = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'test1',
                filetype: '*',
                completed: false
        ).save()

        def e2 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user2',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l2 = new LogFileStorageRequest(
                execution: e2,
                pluginName: 'test1',
                filetype: '*',
                completed: false
        ).save()


        service.logFileStorageTaskScheduler = Mock(TaskScheduler)
        when:
        service.resumeIncompleteLogStorage(serverUUID)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        1 * service.logFileStorageTaskScheduler.schedule(*_)
        1 * service.frameworkService.existsFrameworkProject('test') >> true

        where:
        serverUUID                             | testuser
        null                                   | 'user1'
        'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F' | 'user2'
    }

    def "resume incomplete delayed project does not exist"() {
        given:
        service.pluginService = Mock(PluginService) {
            0 * configurePlugin(_, _, _, PropertyScope.Instance)
        }
        service.frameworkService = Mock(FrameworkService) {
            getFrameworkPropertyResolver('test') >> {
                throw new Exception("Project does not exist")
            }
        }
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def e1 = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user1',
            project: 'test',
            serverNodeUUID: null
        ).save()

        def l = new LogFileStorageRequest(
            execution: e1,
            pluginName: 'test1',
            filetype: '*',
            completed: false
        ).save()

        def e2 = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user2',
            project: 'test',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l2 = new LogFileStorageRequest(
            execution: e2,
            pluginName: 'test1',
            filetype: '*',
            completed: false
        ).save()


        service.logFileStorageTaskScheduler = Mock(TaskScheduler)
        when:
        service.resumeIncompleteLogStorage(serverUUID)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        0 * service.logFileStorageTaskScheduler.schedule(*_)
        1 * service.frameworkService.existsFrameworkProject('test') >> false

        where:
        serverUUID                             | testuser
        null                                   | 'user1'
        'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F' | 'user2'
    }

    def "resume incomplete delayed mixed project exist/does not exist"() {
        given:
        def mockPlugin = Mock(ExecutionFileStoragePlugin) {
            1 * initialize(
                { args ->
                    args.username == 'user2'
                }
            )
        }
        def test2PropertyResolverFactory = Mock(PropertyResolverFactory.Factory)
        service.frameworkService = Mock(FrameworkService) {
            1 * getFrameworkPropertyResolverFactory('test2') >> test2PropertyResolverFactory
        }
        service.pluginService = Mock(PluginService) {
            1 * configurePlugin('test1', _, test2PropertyResolverFactory, PropertyScope.Instance) >> new ConfiguredPlugin(
                mockPlugin,
                [:]
            )
        }
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def e1 = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user1',
            project: 'test',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()

        def l = new LogFileStorageRequest(
            execution: e1,
            pluginName: 'test1',
            filetype: '*',
            completed: false
        ).save()

        def e2 = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user2',
            project: 'test2',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l2 = new LogFileStorageRequest(
            execution: e2,
            pluginName: 'test1',
            filetype: '*',
            completed: false
        ).save()


        service.logFileStorageTaskScheduler = Mock(TaskScheduler)
        when:
        service.resumeIncompleteLogStorage(serverUUID)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        1 * service.logFileStorageTaskScheduler.schedule(*_)
        1 * service.frameworkService.existsFrameworkProject('test') >> false
        1 * service.frameworkService.existsFrameworkProject('test2') >> true

        where:
        serverUUID                             | _
        'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F' | _
    }

    @Unroll
    def "resume incomplete periodic using serverUuid #serverUUID"() {
        given:
        service.configurationService=Mock(ConfigService){
            _ * getString(LogFileStorageService.RESUME_INCOMPLETE_STRATEGY, _) >> 'periodic'
            _ * getString(LogFileStorageService.FILE_STORAGE_PLUGIN, _) >> 'test1'
        }

        service.frameworkService = Mock(FrameworkService)
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def e1 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user1',
                               project: 'test',
                               serverNodeUUID: null
        ).save()

        def l = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'test1',
                filetype: '*',
                completed: false
        ).save()

        def e2 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user2',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l2 = new LogFileStorageRequest(
                execution: e2,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save()


        service.logFileStorageTaskScheduler = Mock(TaskScheduler)
        when:
        service.resumeIncompleteLogStorage(serverUUID)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        0 * service.logFileStorageTaskScheduler.schedule(*_)
        1 == service.retryIncompleteRequests.size()

        where:
        serverUUID                             | testuser
        null                                   | 'user1'
        'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F' | 'user2'
    }

    @Unroll
    def "dequeueIncompleteLogStorage"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def mockPlugin = Mock(ExecutionFileStoragePlugin) {
            1 * initialize(_)
        }
        def test2PropertyResolverFactory = Mock(PropertyResolverFactory.Factory)
        service.frameworkService = Mock(FrameworkService) {
            1 * getFrameworkPropertyResolverFactory('test') >> test2PropertyResolverFactory
        }
        service.pluginService = Mock(PluginService) {
            1 * configurePlugin('test1', _, test2PropertyResolverFactory, PropertyScope.Instance) >> new ConfiguredPlugin(
                mockPlugin,
                [:]
            )
        }
        def e1 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user1',
                               project: 'test',
                               serverNodeUUID: null
        ).save()

        def l = new LogFileStorageRequest(
            execution: e1,
            pluginName: 'blah',
            filetype: '*',
            completed: false
        ).save()

        def e2 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user2',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l2 = new LogFileStorageRequest(
            execution: e2,
            pluginName: 'blah',
            filetype: '*',
            completed: false
        ).save()
        def reqs = [l, l2]
        def keys = reqs.collect { it.execution.id + ':' + it.filetype }
        def toStore = queue.collect { keys[it] }

        service.logFileStorageTaskScheduler = Mock(TaskScheduler)

        service.retryIncompleteRequests.addAll(queue.collect { reqs[it].id })
        when:

        LogFileStorageRequest.withSession { session ->
            session.flush()

        }
        service.dequeueIncompleteLogStorage()
        then:
        1 * service.frameworkService.existsFrameworkProject('test') >> true
        service.retryIncompleteRequests.size() == 0
        service.storageRequests*.id == toStore

        where:
        queue | _
        [0]   | _
        [1]   | _
    }
    @Unroll
    def "dequeueIncompleteLogStorage project not found"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def test2PropertyResolver = Mock(PropertyResolver)
        service.frameworkService = Mock(FrameworkService) {
            0 * getFrameworkPropertyResolver('test') >> test2PropertyResolver
        }
        service.pluginService = Mock(PluginService) {
            0 * configurePlugin(_, _, test2PropertyResolver, PropertyScope.Instance)
        }
        def e1 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user1',
                               project: 'test',
                               serverNodeUUID: null
        ).save()

        def l = new LogFileStorageRequest(
            execution: e1,
            pluginName: 'blah',
            filetype: '*',
            completed: false
        ).save()

        def e2 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
                               user: 'user2',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l2 = new LogFileStorageRequest(
            execution: e2,
            pluginName: 'blah',
            filetype: '*',
            completed: false
        ).save()
        def reqs = [l, l2]

        service.logFileStorageTaskScheduler = Mock(TaskScheduler)

        service.retryIncompleteRequests.addAll(queue.collect { reqs[it].id })
        when:

        LogFileStorageRequest.withSession { session ->
            session.flush()

        }
        service.dequeueIncompleteLogStorage()
        then:
        1 * service.frameworkService.existsFrameworkProject('test') >> false
        service.retryIncompleteRequests.size() == 0
        service.storageRequests*.id == []

        where:
        queue | _
//        []    | _
        [0]   | _
        [1]   | _
    }


    def "cleanup incomplete all"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = 'blah'
        service.frameworkService = Mock(FrameworkService)
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def e1 = new Execution(dateStarted: new Date(),
                               dateCompleted: new Date(),
                               user: 'user1',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()

        def l = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save()

        def e2 = new Execution(dateStarted: new Date(),
                               dateCompleted: new Date(),
                               user: 'user2',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l2 = new LogFileStorageRequest(
                execution: e2,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save()

        def e3 = new Execution(dateStarted: new Date(),
                               dateCompleted: new Date(),
                               user: 'user3',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def l3 = new LogFileStorageRequest(
                execution: e3,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save()


        service.logFileStorageTaskScheduler = Mock(TaskScheduler)
        service.retryIncompleteRequests.add(l3.id)
        when:
        service.cleanupIncompleteLogStorage('C9CA0A6D-3F85-4F53-A714-313EB57A4D1F')

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        e3 != null
        l3 != null
        0 * service.logFileStorageTaskScheduler.schedule(*_)
        1 == service.retryIncompleteRequests.size()
        1 == LogFileStorageRequest.count()

    }

    def "cleanup incomplete single"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = 'blah'
        service.frameworkService = Mock(FrameworkService)
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def e1 = new Execution(dateStarted: new Date(),
                               dateCompleted: new Date(),
                               user: 'user1',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save(flush: true)

        def l = new LogFileStorageRequest(
                execution: e1,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save(flush: true)

        def e2 = new Execution(dateStarted: new Date(),
                               dateCompleted: new Date(),
                               user: 'user2',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save(flush: true)
        def l2 = new LogFileStorageRequest(
                execution: e2,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save(flush: true)

        def e3 = new Execution(dateStarted: new Date(),
                               dateCompleted: new Date(),
                               user: 'user3',
                               project: 'test',
                               serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save(flush: true)
        def l3 = new LogFileStorageRequest(
                execution: e3,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save(flush: true)


        service.logFileStorageTaskScheduler = Mock(TaskScheduler)
        service.retryIncompleteRequests.add(l3.id)
        when:
        service.cleanupIncompleteLogStorage('C9CA0A6D-3F85-4F53-A714-313EB57A4D1F',l2.id)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        e3 != null
        l3 != null
        0 * service.logFileStorageTaskScheduler.schedule(*_)
        1 == service.retryIncompleteRequests.size()
        2 == LogFileStorageRequest.count()

    }

    @Unroll
    def "generateLocalPathForExecutionFile"() {
        given:
        def exec = new Execution(dateStarted: new Date(),
                                 dateCompleted: null,
                                 user: 'user1',
                                 project: 'test',
                                 serverNodeUUID: null
        ).save()


        expect:
        service.generateLocalPathForExecutionFile(exec, type, partial) == "test/run/logs/${exec.id}.rdlog${partial?'.part':''}"

        where:
        type    | partial
        'rdlog' | false
        'rdlog' | true
    }

    @Unroll
    def "getFileForExecutionFiletype partial"() {
        given:
        def exec = new Execution(dateStarted: new Date(),
                                 dateCompleted: null,
                                 user: 'user1',
                                 project: 'test',
                                 serverNodeUUID: null,
                                 outputfilepath: '/tmp/test/logs/blah/1.rdlog'
        ).save()

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProperties() >> (['framework.logs.dir': '/tmp/test/logs'] as Properties)
        }

        when:
        def result = service.getFileForExecutionFiletype(exec, type, useStored, true)
        then:
        result == new File(dir + (useStored?'1':exec.id) + ext)

        where:
        type         | useStored | dir                                     | ext
        'rdlog'      | false     | '/tmp/test/logs/rundeck/test/run/logs/' | '.rdlog.part'
        'state.json' | false     | '/tmp/test/logs/rundeck/test/run/logs/' | '.state.json.part'
        'rdlog'      | true      | '/tmp/test/logs/blah/'                  | '.rdlog.part'
        'state.json' | true      | '/tmp/test/logs/blah/'                  | '.state.json.part'
    }

    static interface TestFilePlugin extends ExecutionFileStoragePlugin, ExecutionFileStorageOptions {

    }

    @Unroll
    def "getLogFileState"() {
        given:

        def now = new Date()
        def exec = new Execution(dateStarted: new Date(now.time - 20000),
                                 dateCompleted: done ? new Date(now.time - 10000) : null,
                                 user: 'user1',
                                 project: 'test',
                                 serverNodeUUID: null,
                                 outputfilepath: ''
        ).save()
        def outFile = new File(tempDir, "rundeck/test/run/logs/${exec.id}.rdlog")
        def outFilePart = new File(tempDir, "rundeck/test/run/logs/${exec.id}.rdlog.part")
        if (loData) {
            outFile.parentFile.mkdirs()
            outFile.text = 'test-complete'
        }
        if (loPart) {
            outFile.parentFile.mkdirs()
            outFilePart.text = 'test-partial'
        }
        exec.outputfilepath=outFile.absolutePath
        exec.save()
        def plugin = Mock(TestFilePlugin) {
            getRetrieveSupported() >> true
            getPartialRetrieveSupported() >> supports
            isAvailable(filetype) >> reData
            isPartialAvailable(filetype) >> rePart
        }

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProperties() >> (['framework.logs.dir': tempDir.getAbsolutePath()] as Properties)
        }
        service.configurationService = Mock(ConfigService) {
            _ * getTimeDuration(LogFileStorageService.CHECKPOINT_TIME_INTERVAL, '30s', _) >> 30
            _ * getInteger(LogFileStorageService.REMOTE_PENDING_DELAY, _) >> pend
        }
        when:
        def result = service.getLogFileState(exec, filetype, plugin, getPart)
        then:
        result.state == eState

        where:
        filetype | done  | getPart | supports | loData | loPart | reData | rePart | pend | eState
        //not requesting partial check
        'rdlog'  | false | false   | false    | false  | false  | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | true  | false   | false    | false  | false  | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | true  | false   | false    | false  | false  | false  | false  | 1    | NOT_FOUND
        'rdlog'  | false | false   | false    | true   | false  | false  | false  | 120  | AVAILABLE
        'rdlog'  | false | false   | false    | false  | true   | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | false | false   | false    | false  | false  | true   | false  | 120  | AVAILABLE_REMOTE
        'rdlog'  | false | false   | false    | false  | false  | false  | true   | 120  | PENDING_REMOTE
        'rdlog'  | false | false   | false    | false  | false  | true   | true   | 120  | AVAILABLE_REMOTE
        //requesting partial check, not supported
        'rdlog'  | false | true    | false    | false  | false  | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | true  | true    | false    | false  | false  | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | true  | true    | false    | false  | false  | false  | false  | 1    | NOT_FOUND
        'rdlog'  | false | true    | false    | true   | false  | false  | false  | 120  | AVAILABLE
        'rdlog'  | false | true    | false    | false  | true   | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | false | true    | false    | false  | false  | true   | false  | 120  | AVAILABLE_REMOTE
        'rdlog'  | false | true    | false    | false  | false  | false  | true   | 120  | PENDING_REMOTE
        'rdlog'  | false | true    | false    | false  | false  | true   | true   | 120  | AVAILABLE_REMOTE
        //requesting partial check, is supported
        'rdlog'  | false | true    | true     | false  | false  | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | true  | true    | true     | false  | false  | false  | false  | 120  | PENDING_REMOTE
        'rdlog'  | true  | true    | true     | false  | false  | false  | false  | 1    | NOT_FOUND
        'rdlog'  | false | true    | true     | true   | false  | false  | false  | 120  | AVAILABLE
        'rdlog'  | false | true    | true     | false  | true   | false  | false  | 120  | AVAILABLE_PARTIAL
        'rdlog'  | false | true    | true     | false  | false  | true   | false  | 120  | AVAILABLE_REMOTE
        'rdlog'  | false | true    | true     | false  | false  | false  | true   | 120  | AVAILABLE_REMOTE_PARTIAL
        'rdlog'  | false | true    | true     | false  | false  | true   | true   | 120  | AVAILABLE_REMOTE
    }

    def "requestLogFileLoad before output set"() {
        given:
            def exec = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: 'user2',
                    project: 'test',
                    serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
            ).save()
            def filetype = 'rdlog'
            def performLoad = true

            service.frameworkService = Mock(FrameworkService)
            service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, async)

        then:
            result != null
            result.get().state == WAITING

        where:
            async | _
            true  | _
            false | _
    }

    def "requestLogFileLoad with missing file"() {
        given:
        def tempDir = Files.createTempDirectory('test_logs')

        def exec = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user2',
            project: 'test',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F',
            outputfilepath: '/tmp/file'
        ).save()
        def filetype = 'rdlog'
        def performLoad = true

        def scheduledExecution = new ScheduledExecution()
        scheduledExecution.id = 1

        exec.scheduledExecution = scheduledExecution
        exec.save()

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProperties() >> (
                [
                    'framework.logs.dir': tempDir.toAbsolutePath().toString()
                ] as Properties
            )
        }
        service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, async)


        then:
        result != null
            result.get().state == NOT_FOUND
        where:
            async | _
            true  | _
            false | _
    }

    def "requestLogFileLoad with existing file"() {
        given:
        def tempDir = Files.createTempDirectory('test_logs')
        def logsDir = tempDir.resolve('rundeck')

        def exec = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user2',
            project: 'test',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save()
        def filetype = 'rdlog'
        def performLoad = true

        createLogFile(logsDir, exec, filetype)

        exec.outputfilepath = logsDir.resolve("test/run/logs/${exec.id}.${filetype}")
        exec.save()

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProperties() >> (
                [
                    'framework.logs.dir': tempDir.toAbsolutePath().toString()
                ] as Properties
            )
        }
        service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, async)


        then:
        result != null
            result.get().state == AVAILABLE
        where:
            async | _
            true  | _
            false | _
    }

    public void createLogFile(Path logsDir, Execution exec, filetype) {
        def outfilePath = logsDir.resolve("test/run/logs/${exec.id}.${filetype}")
        outfilePath.toFile().parentFile.mkdirs()
        outfilePath.toFile() << 'output'
        outfilePath.toFile().deleteOnExit()
    }

    static class TestEFSPlugin implements ExecutionFileStoragePlugin, ExecutionFileStorageOptions {
        boolean partialRetrieveSupported
        boolean retrieveSupported = true
        boolean storeSupported = false
        Closure<Boolean> retrieve
        boolean available

        @Override
        void initialize(final Map<String, ?> context) {

        }

        @Override
        boolean isAvailable(final String filetype) throws ExecutionFileStorageException {
            return available
        }

        @Override
        boolean store(final String filetype, final InputStream stream, final long length, final Date lastModified)
            throws IOException, ExecutionFileStorageException {
            return false
        }

        @Override
        boolean retrieve(final String filetype, final OutputStream stream)
            throws IOException, ExecutionFileStorageException {
            if (retrieve) {
                return retrieve.call(filetype, stream)
            }
            return false
        }
    }

    def "requestLogFileLoad cluster mode, running, no plugin, no file"() {
        given:
        def tempDir = Files.createTempDirectory('test_logs')
        def exec = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user2',
            project: 'test',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F',
            outputfilepath: '/tmp/file'
        ).save()
        def filetype = 'rdlog'
        def performLoad = true

        def scheduledExecution = new ScheduledExecution()
        scheduledExecution.id = 1

        exec.scheduledExecution = scheduledExecution
        exec.save()

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
            getFrameworkProperties() >> (
                [
                    'framework.logs.dir': tempDir.toAbsolutePath().toString()
                ] as Properties
            )
        }
        service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, async)


        then:
        result != null
            result.get().state == NOT_FOUND

        where:
            async | _
            true  | _
            false | _
    }

    def "requestLogFileLoad running, cluster mode, with plugin, no partial"() {
        given:
        def tempDir = Files.createTempDirectory('test_logs')
        def exec = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user2',
            project: 'test',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F',
            outputfilepath: '/tmp/file'
        ).save()
        def filetype = 'rdlog'
        def performLoad = true

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
            getFrameworkProperties() >> (
                [
                    'framework.logs.dir': tempDir.toAbsolutePath().toString()
                ] as Properties
            )
            1 * getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
        }
        service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
        service.pluginService = Mock(PluginService) {
            1 * configurePlugin('test1', _, _ as PropertyResolverFactory.Factory, PropertyScope.Instance) >> new ConfiguredPlugin<ExecutionFileStoragePlugin>(new TestEFSPlugin(partialRetrieveSupported: false),[:])
        }
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, async)


        then:
        result != null
            result.get().state == PENDING_REMOTE
        where:
            async | _
            true  | _
            false | _
    }

    def "requestLogFileLoad cluster mode with local file no plugin"() {
        given:
        def tempDir = Files.createTempDirectory('test_logs')
        def logsDir = tempDir.resolve('rundeck')

        def exec = new Execution(
            dateStarted: new Date(),
            dateCompleted: null,
            user: 'user2',
            project: 'test',
            serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F',
        ).save()
        def filetype = 'rdlog'
        def performLoad = true

        createLogFile(logsDir, exec, filetype)

        exec.outputfilepath = logsDir.resolve("test/run/logs/${exec.id}.${filetype}")
        exec.save()

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> true
            getServerUUID() >> 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
            getFrameworkProperties() >> (
                [
                    'framework.logs.dir': tempDir.toAbsolutePath().toString()
                ] as Properties
            )
        }
        service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, async)


        then:
        result != null
            result.get().state == AVAILABLE
        where:
            async | _
            true  | _
            false | _
    }

    def "requestLogFileLoad cluster mode with plugin async"() {
        given:
            def tempDir = Files.createTempDirectory('test_logs')
            def logsDir = tempDir.resolve('rundeck')

            def exec = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    user: 'user2',
                    project: 'test',
                    serverNodeUUID: 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
            ).save()
            def filetype = 'rdlog'
            def performLoad = true

//            createLogFile(logsDir, exec, filetype)

            service.frameworkService = Mock(FrameworkService) {
                isClusterModeEnabled() >> true
                getServerUUID() >> 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
                getFrameworkProperties() >> (
                        [
                                'framework.logs.dir': tempDir.toAbsolutePath().toString()
                        ] as Properties
                )
                getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
            }
            boolean retrieved = false
            def plugin = new TestEFSPlugin(
                    partialRetrieveSupported: false, available: true, retrieve: { type, stream ->
                stream.write('data'.bytes)
                retrieved = true
                true
            }
            )
            service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
            service.pluginService = Mock(PluginService) {
                1 * configurePlugin(
                        'test1',
                        _,
                        _ as PropertyResolverFactory.Factory,
                        PropertyScope.Instance
                ) >> new ConfiguredPlugin<ExecutionFileStoragePlugin>(plugin, [:])
            }
            service.logFileTaskExecutor = new SimpleAsyncTaskExecutor()
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, async)
            service.runRetrievalRequestTask(service.retrievalRequests.take())


        then:
            result != null
//            service.retrievalRequests.size() == 1
            result.get(10, TimeUnit.SECONDS).state == AVAILABLE
            retrieved == async
        where:
            async | _
            true  | _
    }

    def "requestLogFileLoad cluster mode with plugin async and partial support so ruuid is null"() {
        given:
            def tempDir = Files.createTempDirectory('test_logs.part')
            def logsDir = tempDir.resolve('rundeck.part')

            def exec = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    user: 'user2',
                    project: 'test',
                    serverNodeUUID: 'blabla'
            )

            exec.save()
            def filetype = 'rdlog'
            def performLoad = true

            service.frameworkService = Mock(FrameworkService) {
                isClusterModeEnabled() >> true
                getServerUUID() >> 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
                getFrameworkProperties() >> (
                        [
                                'framework.logs.dir': tempDir.toAbsolutePath().toString()
                        ] as Properties
                )


                1 * getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
            }
            boolean retrieved = false
            def plugin = new TestEFSPlugin(
                    partialRetrieveSupported: true, available: true, retrieve: { type, stream ->
                stream.write('data'.bytes)
                retrieved = true
                true
            }
            )
            service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
            service.pluginService = Mock(PluginService) {
                configurePlugin(
                        'test1',
                        _,
                        _ as PropertyResolverFactory.Factory,
                        PropertyScope.Instance
                ) >> new ConfiguredPlugin<ExecutionFileStoragePlugin>(plugin, [:])
            }
            service.logFileTaskExecutor = Mock(AsyncListenableTaskExecutor)


        when:
            service.requestLogFileLoadAsync(exec, filetype, performLoad, async)
            Map task = service.retrievalRequests.take()
            task.ruuid = null
            def result = service.runRetrievalRequestTask(task)

        then:
            noExceptionThrown()
            result != null
            !result.isDone()
        where:
            async | _
            true  | _
    }

    def "requestLogFileLoad not cluster mode force partial check with plugin async and partial support so ruuid is null"() {
        given:
        def tempDir = Files.createTempDirectory('test_logs.part')
        def logsDir = tempDir.resolve('rundeck.part')

        def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user2',
                project: 'test',
                serverNodeUUID: 'blabla'
        )

        exec.save()
        def filetype = 'rdlog'
        def performLoad = true

        service.frameworkService = Mock(FrameworkService) {
            isClusterModeEnabled() >> false
            getServerUUID() >> 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
            getFrameworkProperties() >> (
                    [
                            'framework.logs.dir': tempDir.toAbsolutePath().toString()
                    ] as Properties
            )


            1 * getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
        }
        boolean retrieved = false
        def plugin = new TestEFSPlugin(
                partialRetrieveSupported: true, available: true, retrieve: { type, stream ->
            stream.write('data'.bytes)
            retrieved = true
            true
        }
        )
        service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
        service.configurationService = Mock(ConfigurationService) {
            getString(LogFileStorageService.FILE_STORAGE_PLUGIN, null) >> 'testplugin'
            getBoolean(LogFileStorageService.FORCE_PARTIAL_CHECKING, false) >> true
        }
        service.pluginService = Mock(PluginService) {
            configurePlugin(
                    'testplugin',
                    _,
                    _ as PropertyResolverFactory.Factory,
                    PropertyScope.Instance
            ) >> new ConfiguredPlugin<ExecutionFileStoragePlugin>(plugin, [:])
        }
        service.logFileTaskExecutor = Mock(AsyncListenableTaskExecutor)


        when:
        service.requestLogFileLoadAsync(exec, filetype, performLoad, async)
        Map task = service.retrievalRequests.take()
        task.ruuid = null
        def result = service.runRetrievalRequestTask(task)

        then:
        noExceptionThrown()
        result != null
        !result.isDone()
        where:
        async | _
        true  | _
    }

    def "requestLogFileLoad cluster mode with plugin async with error"() {
        given:
            def tempDir = Files.createTempDirectory('test_logs')
            def logsDir = tempDir.resolve('rundeck')

            def exec = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: new Date(),
                    user: 'user2',
                    project: 'test',
                    serverNodeUUID: 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
            ).save()
            def filetype = 'rdlog'
            def performLoad = true

//            createLogFile(logsDir, exec, filetype)

            service.frameworkService = Mock(FrameworkService) {
                isClusterModeEnabled() >> true
                getServerUUID() >> 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F'
                getFrameworkProperties() >> (
                        [
                                'framework.logs.dir': tempDir.toAbsolutePath().toString()
                        ] as Properties
                )
                1 * getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
            }
            boolean retrieved = false
            def plugin = new TestEFSPlugin(
                    partialRetrieveSupported: false, available: true, retrieve: { type, stream ->
//                stream.write('data'.bytes)
                retrieved = true
                if(err){
                    throw new ExecutionFileStorageException(err)
                }
                false
            }
            )
            service.grailsLinkGenerator = Mock(grails.web.mapping.LinkGenerator)
            service.pluginService = Mock(PluginService) {
                configurePlugin(
                        'test1',
                        _,
                        _ as PropertyResolverFactory.Factory,
                        PropertyScope.Instance
                ) >> new ConfiguredPlugin<ExecutionFileStoragePlugin>(plugin, [:])
            }
            service.logFileTaskExecutor = new SimpleAsyncTaskExecutor()
        when:

            def result = service.requestLogFileLoadAsync(exec, filetype, performLoad, true)
            service.runRetrievalRequestTask(service.retrievalRequests.take())


        then:
            result != null
//            service.retrievalRequests.size() == 1
            result.get().state == status
            retrieved
        where:
            err             | status
            null            | ExecutionFileState.NOT_FOUND
            'error message' | ExecutionFileState.ERROR
    }

    static class TestExecFileProducer1 implements ExecutionFileProducer {
        String executionFileType = 'test1'
        boolean executionFileGenerated = false
        boolean checkpointable = false
        ExecutionFile storageFile
        ExecutionFile storageFileCheckpoint

        @Override
        ExecutionFile produceStorageFileForExecution(final ExecutionReference e) {
            storageFile
        }

        @Override
        ExecutionFile produceStorageCheckpointForExecution(final ExecutionReference e) {
            storageFileCheckpoint
        }
    }

    def "get execution file, isShouldBeStored allows optional file storage"() {
        given:
            def execution = new Execution(
                    dateStarted: new Date(),
                    dateCompleted: null,
                    user: 'user2',
                    project: 'test',
                    serverNodeUUID: 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F',
                    outputfilepath: '/tmp/file'
            ).save()
            ExecutionFile file1 = Mock(ExecutionFile) {
                isShouldBeStored() >> true
            }
            ExecutionFile file2 = Mock(ExecutionFile) {
                isShouldBeStored() >> false
            }
            defineBeans {
                testFileProducer1(TestExecFileProducer1) {
                    executionFileType = 'test1'
                    storageFile = file1
                }
                testFileProducer2(TestExecFileProducer1) {
                    executionFileType = 'test2'
                    storageFile = file2
                }
            }
            service.applicationContext = applicationContext
        when:
            def result = service.getExecutionFiles(execution, [], false)
        then:
            result.size() == 1
            result['test1'] == file1
            !result['test2']
    }
    def "submit for storage duplicate"(){
        given:
            def exec = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user2',
                project: 'test',
                serverNodeUUID: 'D0CA0A6D-3F85-4F53-A714-313EB57A4D1F',
                outputfilepath: '/tmp/file'
            ).save()
            service.frameworkService = Mock(FrameworkService){

                2 * getFrameworkPropertyResolverFactory(_) >> Mock(PropertyResolverFactory.Factory)
            }
            def instance = Mock(ExecutionFileStoragePlugin)
            service.pluginService=Mock(PluginService){
                2 * configurePlugin('test1', _, _ as PropertyResolverFactory.Factory, PropertyScope.Instance)>>new ConfiguredPlugin<ExecutionFileStoragePlugin>(
                    instance,
                    [:]
                )
            }
            service.grailsLinkGenerator=Stub(grails.web.mapping.LinkGenerator)
            service.submitForStorage(exec)
        when:
            service.submitForStorage(exec)
        then:
            service.storageRequests.size()==1

    }

    def "countIncompleteStorageRequests"() {
        given:
        def serveruuid = 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        service.frameworkService = Mock(FrameworkService){

            serverUUID >> serveruuid
        }
        def e2 = new Execution(dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user3',
                project: 'test',
                serverNodeUUID: serveruuid
        ).save(flush: true)
        def l2 = new LogFileStorageRequest(
                execution: e2,
                pluginName: 'blah',
                filetype: '*',
                completed: true
        ).save(flush: true)
        def e3 = new Execution(dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user3',
                project: 'test',
                serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save(flush: true)
        def l3 = new LogFileStorageRequest(
                execution: e3,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save(flush: true)

        when:
        int count = service.countIncompleteLogStorageRequests()

        then:
        count == 1
    }

    def "count IncompleteStorageRequests"() {
        given:
        def serveruuid = 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        service.frameworkService = Mock(FrameworkService){

            serverUUID >> serveruuid
        }
        def e = new Execution(dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user1',
                project: 'test',
                serverNodeUUID: 'some-other-server-uuid'
        ).save(flush: true)
        def l = new LogFileStorageRequest(
                execution: e,
                pluginName: 'blah',
                filetype: '*',
                completed: false
        ).save(flush: true)
        def e2 = new Execution(dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user2',
                project: 'test',
                serverNodeUUID: serveruuid
        ).save(flush: true)
        def l2 = new LogFileStorageRequest(
                execution: e2,
                pluginName: 'blah2',
                filetype: '*',
                completed: true
        ).save(flush: true)
        def e3 = new Execution(dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user3',
                project: 'test',
                serverNodeUUID: 'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F'
        ).save(flush: true)
        def l3 = new LogFileStorageRequest(
                execution: e3,
                pluginName: 'blah3',
                filetype: '*',
                completed: false
        ).save(flush: true)

        when:
        def results = service.listIncompleteRequests(serveruuid)

        then:
        results.size() == 1
        results[0].pluginName == 'blah3'

    }

    def "should return correctly expanded path given a path template and execution"(){
        given:
        def e = new Execution(dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user1',
                project: projectName,
                serverNodeUUID: serverUUID
        )
        e.id = execId
        service.grailsLinkGenerator = Mock(LinkGenerator)

        when:
        String expandedPath = service.getRemotePathForExecutionFromPathTemplate(e, pathTemplate)

        then:
        expectedExpandedPath == expandedPath

        where:

        execId | projectName      | serverUUID               | pathTemplate                                      | expectedExpandedPath
        67     | 'projectExample' | 'some-other-server-uuid' | 'rootDir/logs/${job.project}/${job.execid}.log'   | "rootDir/logs/${projectName}/${execId}.log"
        67     | 'projectExample' | 'some-other-server-uuid' |'rootDir/logs/${job.execid}/${job.serverUUID}.log' | "rootDir/logs/${execId}/${serverUUID}.log"

    }

    def "should expand the path template correctly"(){
        given:
        def e = new Execution(dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user1',
                project: projectName,
                serverNodeUUID: serverUUID
        )
        e.id = execId

        Map<String, String> execCtx = ExecutionService.exportContextForExecution(e, Mock(LinkGenerator))

        when:
        String expandedPath = LogFileStorageService.expandPath(pathTemplate, execCtx)

        then:
        expectedExpandedPath == expandedPath

        where:

        execId | projectName      | serverUUID               | pathTemplate                                      | expectedExpandedPath
        67     | 'projectExample' | 'some-other-server-uuid' | 'rootDir/logs/${job.project}/${job.execid}.log'   | "rootDir/logs/${projectName}/${execId}.log"
        67     | 'projectExample' | 'some-other-server-uuid' |'rootDir/logs/${job.execid}/${job.serverUUID}.log' | "rootDir/logs/${execId}/${serverUUID}.log"
    }

    def "should bring the path property from the defined log storage plugin"(){
        given:
        Execution e = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user1',
                project: projectName,
                serverNodeUUID: 'some-other-server-uuid'
        )


        service.grailsLinkGenerator = Mock(LinkGenerator)

        service.configurationService=Mock(ConfigurationService){
            getString(LogFileStorageService.FILE_STORAGE_PLUGIN,_) >> pluginName
        }

        Description pluginDescription = Mock(Description) {
            getProperties() >> [PropertyUtil.string('path','path','path',true,logStoragePluginPath)]
        }

        PropertyResolver propertyResolver = Mock(PropertyResolver){
            resolvePropertyValue('path', _) >> logStoragePluginPath
        }
        PropertyResolverFactory.Factory propertyResolverFactory = Mock(PropertyResolverFactory.Factory){
            create(_, pluginDescription) >> propertyResolver
        }
        service.frameworkService = Mock(FrameworkService){
            getFrameworkPropertyResolverFactory(projectName) >> propertyResolverFactory
        }
        service.pluginService = Mock(PluginService) {
            configurePlugin(pluginName, _, propertyResolverFactory, PropertyScope.Instance) >> Mock(ConfiguredPlugin){
                instance >> Mock(ExecutionFileStoragePlugin){
                    getConfiguredPathTemplate() >> logStoragePluginPath
                }
            }
        }

        service.executionFileStoragePluginProviderService = Mock(ExecutionFileStoragePluginProviderService)

        when:
        String returnedStoragePath = service.getStorePathTemplateForExecution(e)

        then:
        logStoragePluginPath == returnedStoragePath

        where:
        pluginName = 'log-storage-name-example'
        projectName = 'projectExample'
        logStoragePluginPath = 'rootDir/logs/${job.project}/${job.execid}.log'
    }
    def "default getConfiguredStorageRetryCount"(){
        expect:
            1==service.getConfiguredStorageRetryCount()
    }
    def "default getConfiguredStorageRetryDelay"(){
        expect:
            60==service.getConfiguredStorageRetryDelay()
    }
    def "default getConfiguredRetrievalRetryCount"(){
        expect:
            3==service.getConfiguredRetrievalRetryCount()
    }
    def "default getConfiguredRetrievalRetryDelay"(){
        expect:
            60==service.getConfiguredRetrievalRetryDelay()
    }

    def testIsCachedItemFresh() {

        expect:
            service.isResultCacheItemFresh([time: new Date(), count: 0])
            service.isResultCacheItemAllowedRetry([time: new Date(), count: 0])
            !service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (61 * 1000)), count: 0])
            !service.isResultCacheItemAllowedRetry([time: new Date(), count: 3])
    }

    def "test cache refreshness with retry delay"() {

        given:
            service.configurationService = Mock(ConfigService) {
                _ * getInteger(LogFileStorageService.RETRIEVAL_RETRY_DELAY, _) >> 30
            }
        expect:
            service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0])
            !service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0])
            !service.isResultCacheItemAllowedRetry([time: new Date(), count: 3])
    }

    def "test cache refreshness with retry delay count"() {

        given:
            service.configurationService = Mock(ConfigService) {
                _ * getInteger(LogFileStorageService.RETRIEVAL_RETRY_DELAY, _) >> 30
                _ * getInteger(LogFileStorageService.RETRIEVAL_RETRY_COUNT, _) >> 10
            }
        expect:

            service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (25 * 1000)), count: 0])
            !service.isResultCacheItemFresh([time: new Date(System.currentTimeMillis() - (31 * 1000)), count: 0])
            service.isResultCacheItemAllowedRetry([time: new Date(), count: 3])
            !service.isResultCacheItemAllowedRetry([time: new Date(), count: 10])
    }


    def "testCacheResult"(){
        when:
            def result = service
                .cacheRetrievalState("1", LogFileState.NOT_FOUND, 0, 'error', 'errorCode', ['errorData'])
            Map result1 = service.getRetrievalCacheResult("1")
        then:
            result != null
            "1" == result.id
            0 == result.count
            'errorCode' == result.errorCode
            ['errorData'] == result.errorData
            'error' == result.error
            'test1' == result.name
            LogFileState.NOT_FOUND == result.state
            1 == service.getCurrentRetrievalResults().size()

            result1 != null
    }

    def testCacheResultDefaults() {
        when:
            def result = service.cacheRetrievalState("1", LogFileState.NOT_FOUND, 0, 'error', null, null)
            Map result1 = service.getRetrievalCacheResult("1")
        then:
            result != null
            "1" == result.id
            0 == result.count
            'execution.log.storage.retrieval.ERROR' == result.errorCode
            ['test1', 'error'] == result.errorData
            'error' == result.error
            'test1' == result.name
            LogFileState.NOT_FOUND == result.state
            1 == service.getCurrentRetrievalResults().size()
    }

    def testRunStorageRequestFailureWithRetry(){
        given:

            def testPlugin = Mock(ExecutionFileStoragePlugin)

            boolean queued=false

            service.logFileStorageTaskScheduler = Mock(TaskScheduler)
            service.configurationService=Mock(ConfigService){
                1*getInteger(LogFileStorageService.STORAGE_RETRY_DELAY,_)>>30
                1*getInteger(LogFileStorageService.STORAGE_RETRY_COUNT,_)>>2
                0 * getInteger(_, _)
                _*getString(LogFileStorageService.FILE_STORAGE_PLUGIN,_)>>'test1'
            }
            def e = new Execution(dateStarted: new Date(),
                                   dateCompleted: null,
                                   user: 'user1',
                                   project: 'test',
                                   serverNodeUUID: null
            ).save()

            def testfile = File.createTempFile("LogFileStorageServiceTests", ".txt")
            testfile<<'some content'
            testfile.deleteOnExit()
            long length=testfile.length()
            Date modified=new Date(testfile.lastModified())

            service.executorService=Mock(ThreadPoolTaskScheduler){
                0 * execute(_)
            }
            Map<String,ExecutionFileProducer> loggingBeans=[:]
            loggingBeans[filetype] = Mock(ExecutionFileProducer){
                _*getExecutionFileType()>>filetype
                1 *  produceStorageFileForExecution(_)>> {
                    new ProducedExecutionFile(localFile: testfile, fileDeletePolicy: ExecutionFile.DeletePolicy.NEVER)
                }
                _*produceStorageCheckpointForExecution(_)>> {
                    new ProducedExecutionFile(localFile: testfile, fileDeletePolicy: ExecutionFile.DeletePolicy.NEVER)
                }
            }


            def appmock = Mock(ApplicationContext){
              _*  getBeansOfType(ExecutionFileProducer)>>loggingBeans
            }
            service.applicationContext=appmock


            LogFileStorageRequest request = new LogFileStorageRequest(filetype: filetype,execution: e,pluginName:'test1',completed: false)
            request.validate()
            request.save(flush:true)
            def task = [file: testfile, storage: testPlugin, filetype: filetype,request:request,requestId:request.id]
            if(useExecId) {
                task['execId'] = e.id.toString()
            } else {
                task['executionUuid'] = e.uuid
            }

        when:
            def result=service.runStorageRequest(task)


        then:
            1*testPlugin.store(filetype,_, length, {it== modified })>>storeSuccess

            task.count == 1
//            !svc.executorService.executeCalled
            service.getCurrentRequests().size() == 0
            1 * service.logFileStorageTaskScheduler.schedule(_, _ as Date)>>{
                assert it[1]>new Date()
            }
        cleanup:
            testfile.delete()
        where:
            useExecId | filetype | storeSuccess
            true      | 'rdlog'  | false
            false     | 'rdlog'  | false


    }

    def testSubmitForStorage_plugin_storeSupported() {
        given:
            def test = new LogFileStorageServiceTests.testOptionsStoragePlugin()
            test.storeSupported=true

            def execution = new Execution(
                dateStarted: new Date(),
                dateCompleted: null,
                user: 'user1',
                project: 'test',
                serverNodeUUID: null
            ).save()


            service.frameworkService = Mock(FrameworkService) {
                1 * getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
            }
            service.pluginService = Mock(PluginService) {
                _ * configurePlugin('test1', _, _ as PropertyResolverFactory.Factory, PropertyScope.Instance) >>
                new ConfiguredPlugin(test, [:])
            }
            service.executorService = Mock(ExecutionService)
            service.grailsLinkGenerator=Mock(grails.web.mapping.LinkGenerator){

            }
        when:
            service.submitForStorage(execution)
        then:
            1 == service.storageRequests.size()

    }

    def testRequestLogFileReaderFileExists(){

        given:
            def testfile = File.createTempFile("LogFileStorageServiceTests", ".txt")
            testfile<<'some content'
            testfile.deleteOnExit()

            def execution = new Execution(
                dateStarted: new Date(),
                dateCompleted: new Date(),
                user: 'user1',
                project: 'test',
                serverNodeUUID: null
            ).save()

            service.frameworkService = Mock(FrameworkService) {
                _ * isClusterModeEnabled() >> false
                _ * getServerUUID() >> UUID.randomUUID()

                1 * getFrameworkPropertyResolverFactory('test') >> Mock(PropertyResolverFactory.Factory)
                _ * getFrameworkProperties() >> (['framework.logs.dir': '/tmp/dir'] as Properties)
            }
            service.pluginService = Mock(PluginService) {
                0*configurePlugin('test1', _, _ as PropertyResolverFactory.Factory, PropertyScope.Instance)
            }
            List useStoredValues = [true, true]
            List partialValues = [false, false]
            int useStoredNdx=0
            service.metaClass.getFileForExecutionFiletype = {
                Execution e2, String filetype, boolean useStored, boolean partial ->
                    assert execution == e2
                    assert "rdlog"==filetype
                    assert useStored==useStoredValues[useStoredNdx]
                    assert partial == partialValues[useStoredNdx]
                    useStoredNdx++
                    testfile
            }


            service.grailsLinkGenerator=Mock(grails.web.mapping.LinkGenerator){

            }
            service.configurationService=Mock(ConfigService)

        when:
            def reader =  service.requestLogFileReader(execution, LoggingService.LOG_FILE_FILETYPE, false)
        then:
             null != (reader)
             ExecutionFileState.AVAILABLE == reader.state
            null != (reader.reader)
            reader.reader instanceof FSStreamingLogReader
        cleanup:
            testfile.delete()

    }

}
