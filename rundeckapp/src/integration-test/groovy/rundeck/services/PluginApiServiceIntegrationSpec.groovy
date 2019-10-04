package rundeck.services

import com.dtolabs.rundeck.plugins.ServiceNameConstants
import grails.testing.mixin.integration.Integration
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.grails.web.util.WebUtils
import spock.lang.Specification

@Integration
class PluginApiServiceIntegrationSpec extends Specification {

    PluginApiService pluginApiService

    def setup() {
        def gwr = Stub(GrailsWebRequest) {
            getLocale() >> Locale.ENGLISH
        }
        WebUtils.metaClass.static.retrieveGrailsWebRequest = { -> gwr }
    }

    def cleanup() {
    }

    void "list plugins"() {
        when:
        def pluginList = pluginApiService.listPluginsDetailed()
        pluginList.descriptions.each {
            println it.key
        }

        then:
            pluginList.descriptions*.getKey().containsAll(
                    [
                            ServiceNameConstants.NodeExecutor,
                            ServiceNameConstants.FileCopier,
                            ServiceNameConstants.WorkflowNodeStep,
                            ServiceNameConstants.WorkflowStep,
                            ServiceNameConstants.ResourceModelSource,
                            ServiceNameConstants.NodeEnhancer,
                            ServiceNameConstants.ResourceFormatParser,
                            ServiceNameConstants.ResourceFormatGenerator,
                            ServiceNameConstants.Orchestrator,
                            ServiceNameConstants.ExecutionLifecycle,
                            ServiceNameConstants.Notification,
                            ServiceNameConstants.StreamingLogReader,
                            ServiceNameConstants.StreamingLogWriter,
                            ServiceNameConstants.ExecutionFileStorage,
                            ServiceNameConstants.Storage,
                            ServiceNameConstants.StorageConverter,
                            ServiceNameConstants.ScmExport,
                            ServiceNameConstants.ScmImport,
                            ServiceNameConstants.FileUpload,
                            ServiceNameConstants.LogFilter,
                            ServiceNameConstants.ContentConverter,
                            ServiceNameConstants.TourLoader,
                            ServiceNameConstants.UserGroupSource,
                            ServiceNameConstants.UI,
                            ServiceNameConstants.WebhookEvent,
                            ServiceNameConstants.PasswordUtilityEncrypter,
                    ]
            )
        pluginList.descriptions.size() == 27
        pluginList.serviceDefaultScopes.size() == 2
        pluginList.bundledPlugins.size() == 7
        pluginList.embeddedFilenames != null
        pluginList.specialConfiguration.size() == 7
        pluginList.specialScoping.size() == 2
        pluginList.uiPluginProfiles != null

    }

    void "list plugins option value plugin enabled"() {
        when:
        pluginApiService.featureService.toggleFeature("optionValuesPlugin",true)
        def pluginList = pluginApiService.listPluginsDetailed()

        then:
        pluginList.descriptions.size() == 28
        pluginList.serviceDefaultScopes.size() == 2
        pluginList.bundledPlugins.size() == 7
        pluginList.embeddedFilenames != null
        pluginList.specialConfiguration.size() == 7
        pluginList.specialScoping.size() == 2
        pluginList.uiPluginProfiles != null

    }

    void "list plugins life cycle plugins enabled"() {
        setup:
        pluginApiService.grailsApplication.config.rundeck.feature.jobLifecyclePlugin.enabled=true
        pluginApiService.grailsApplication.config.rundeck.feature.executionLifecyclePlugin.enabled=true

        when:
        def pluginList = pluginApiService.listPluginsDetailed()

        then:
        pluginList.descriptions.size() == 29
        pluginList.serviceDefaultScopes.size() == 2
        pluginList.bundledPlugins.size() == 7
        pluginList.embeddedFilenames != null
        pluginList.specialConfiguration.size() == 7
        pluginList.specialScoping.size() == 2
        pluginList.uiPluginProfiles != null

    }
}
