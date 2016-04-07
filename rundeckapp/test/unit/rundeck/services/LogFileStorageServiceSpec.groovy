package rundeck.services

import asset.pipeline.grails.LinkGenerator
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import com.dtolabs.rundeck.server.plugins.ConfiguredPlugin
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.Execution
import rundeck.LogFileStorageRequest
import spock.lang.Specification

import java.util.concurrent.ScheduledExecutorService

/**
 * Created by greg on 3/28/16.
 */
@Mock([LogFileStorageRequest, Execution])
@TestFor(LogFileStorageService)
class LogFileStorageServiceSpec extends Specification {

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
}
