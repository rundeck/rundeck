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
import com.dtolabs.rundeck.core.logging.ExecutionFileStorageOptions
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Execution
import rundeck.LogFileStorageRequest
import rundeck.services.logging.ExecutionLogState
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.util.concurrent.ScheduledExecutorService

import static rundeck.services.logging.ExecutionLogState.AVAILABLE
import static rundeck.services.logging.ExecutionLogState.AVAILABLE_PARTIAL
import static rundeck.services.logging.ExecutionLogState.AVAILABLE_REMOTE
import static rundeck.services.logging.ExecutionLogState.AVAILABLE_REMOTE_PARTIAL
import static rundeck.services.logging.ExecutionLogState.NOT_FOUND
import static rundeck.services.logging.ExecutionLogState.PENDING_REMOTE

/**
 * Created by greg on 3/28/16.
 */
@Mock([LogFileStorageRequest, Execution])
@TestFor(LogFileStorageService)
class LogFileStorageServiceSpec extends Specification {
    File tempDir

    def setup() {
        tempDir = Files.createTempDirectory("LogFileStorageServiceSpec").toFile()
    }

    def cleanup() {


    }

    def "resume incomplete delayed"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = 'blah'
        grailsApplication.config.rundeck.logFileStorageService.resumeIncomplete.strategy = 'delayed'
        service.configurationService=Mock(ConfigurationService){
            getString('logFileStorageService.resumeIncomplete.strategy',_)>>'delayed'
            getString('execution.logs.fileStoragePlugin',_)>>'blah'
        }
        def mockPlugin = Mock(ExecutionFileStoragePlugin){
            1 * initialize({args->
                args.username==testuser
            })
        }
        service.pluginService = Mock(PluginService) {
            1 * configurePlugin('blah', _, _, PropertyScope.Instance) >> new ConfiguredPlugin(
                    mockPlugin,
                    [:]
            )
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


        service.scheduledExecutor = Mock(ScheduledExecutorService)
        when:
        service.resumeIncompleteLogStorage(serverUUID)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        1 * service.scheduledExecutor.schedule(*_)

        where:
        serverUUID                             | testuser
        null                                   | 'user1'
        'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F' | 'user2'
    }
    def "resume incomplete periodic"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = 'blah'
        service.configurationService=Mock(ConfigurationService){
            getString('logFileStorageService.resumeIncomplete.strategy',_)>>'periodic'
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


        service.scheduledExecutor = Mock(ScheduledExecutorService)
        when:
        service.resumeIncompleteLogStorage(serverUUID)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        0 * service.scheduledExecutor.schedule(*_)
        1 == service.retryIncompleteRequests.size()

        where:
        serverUUID                             | testuser
        null                                   | 'user1'
        'C9CA0A6D-3F85-4F53-A714-313EB57A4D1F' | 'user2'
    }
    def "resume incomplete periodic single"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = 'blah'
        service.configurationService=Mock(ConfigurationService){
            getString('logFileStorageService.resumeIncomplete.strategy',_)>>'periodic'
        }
        service.frameworkService = Mock(FrameworkService)
        service.grailsLinkGenerator = Mock(LinkGenerator)
        def e1 = new Execution(dateStarted: new Date(),
                               dateCompleted: null,
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


        service.scheduledExecutor = Mock(ScheduledExecutorService)
        when:
        service.resumeIncompleteLogStorage('C9CA0A6D-3F85-4F53-A714-313EB57A4D1F',l2.id)

        then:
        e1 != null
        l != null
        e2 != null
        l2 != null
        0 * service.scheduledExecutor.schedule(*_)
        1 == service.retryIncompleteRequests.size()
        service.retryIncompleteRequests.contains(l2.id)

    }


    def "cleanup incomplete all"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = 'blah'
        service.configurationService=Mock(ConfigurationService){
            getString('logFileStorageService.resumeIncomplete.strategy',_)>>'periodic'
        }
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


        service.scheduledExecutor = Mock(ScheduledExecutorService)
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
        0 * service.scheduledExecutor.schedule(*_)
        1 == service.retryIncompleteRequests.size()
        1 == LogFileStorageRequest.count()

    }

    def "cleanup incomplete single"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStoragePlugin = 'blah'
        service.configurationService=Mock(ConfigurationService){
            getString('logFileStorageService.resumeIncomplete.strategy',_)>>'periodic'
        }
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


        service.scheduledExecutor = Mock(ScheduledExecutorService)
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
        0 * service.scheduledExecutor.schedule(*_)
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
        result == service.generateLocalPathForExecutionFile(exec, type, partial)

        where:
        type    | partial | result
        'rdlog' | false   | 'test/run/logs/1.rdlog'
        'rdlog' | true    | 'test/run/logs/1.rdlog.part'
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
        result == new File(expected)

        where:
        type         | useStored | expected
        'rdlog'      | false     | '/tmp/test/logs/rundeck/test/run/logs/1.rdlog.part'
        'state.json' | false     | '/tmp/test/logs/rundeck/test/run/logs/1.state.json.part'
        'rdlog'      | true      | '/tmp/test/logs/blah/1.rdlog.part'
        'state.json' | true      | '/tmp/test/logs/blah/1.state.json.part'
    }

    static interface TestFilePlugin extends ExecutionFileStoragePlugin, ExecutionFileStorageOptions {

    }

    @Unroll
    def "getLogFileState"() {
        given:
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.execution.logs.fileStorage.remotePendingDelay = pend
        def outFile = new File(tempDir, "rundeck/test/run/logs/1.rdlog")
        def outFilePart = new File(tempDir, "rundeck/test/run/logs/1.rdlog.part")
        if (loData) {
            outFile.parentFile.mkdirs()
            outFile.text = 'test-complete'
        }
        if (loPart) {
            outFile.parentFile.mkdirs()
            outFilePart.text = 'test-partial'
        }
        def now = new Date()
        def exec = new Execution(dateStarted: new Date(now.time - 20000),
                                 dateCompleted: done ? new Date(now.time - 10000) : null,
                                 user: 'user1',
                                 project: 'test',
                                 serverNodeUUID: null,
                                 outputfilepath: outFile.absolutePath
        ).save()
        def plugin = Mock(TestFilePlugin) {
            getRetrieveSupported() >> true
            getPartialRetrieveSupported() >> supports
            isAvailable(filetype) >> reData
            isPartialAvailable(filetype) >> rePart
        }

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProperties() >> (['framework.logs.dir': tempDir.getAbsolutePath()] as Properties)
        }
        service.configurationService = Mock(ConfigurationService) {
            getTimeDuration('execution.logs.fileStorage.checkpoint.time.interval', '30s', _) >> 30
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
}
