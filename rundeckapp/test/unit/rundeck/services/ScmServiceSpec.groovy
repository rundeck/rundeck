package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import grails.test.mixin.TestFor
import rundeck.services.scm.ScmPluginConfigData
import spock.lang.Specification

/**
 * Created by greg on 10/15/15.
 */
@TestFor(ScmService)
class ScmServiceSpec extends Specification {


    def "disablePlugin"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        ScmPluginConfigData config = Mock(ScmPluginConfigData)

        when:
        service.disablePlugin(integration, 'test1', null)

        then:
        1 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-${integration}.properties",
                'scm.' + integration
        ) >> config
        1 * config.setEnabled(false)
        1 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-${integration}.properties")
        1 * service.jobEventsService.removeListener(_)


        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _
    }

    def "removePluginConfiguration"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        ScmPluginConfigData config = Mock(ScmPluginConfigData)

        when:
        service.removePluginConfiguration(integration, 'test1', null)
        then:
        1 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-${integration}.properties",
                'scm.' + integration
        ) >> config
        1 * config.setEnabled(false)
        1 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-${integration}.properties")
        1 * service.jobEventsService.removeListener(_)
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-${integration}.properties")

        where:
        integration       | _
        ScmService.EXPORT | _
        ScmService.IMPORT | _
    }

    def "removeAllPluginConfiguration"() {
        given:
        service.frameworkService = Mock(FrameworkService)
        service.pluginConfigService = Mock(PluginConfigService)
        service.jobEventsService = Mock(JobEventsService)
        ScmPluginConfigData config = Mock(ScmPluginConfigData)
        ScmPluginConfigData config2 = Mock(ScmPluginConfigData)

        when:
        service.removeAllPluginConfiguration('test1', null)
        then:
        1 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-import.properties",
                'scm.import'
        ) >> config
        1 * service.pluginConfigService.loadScmConfig(
                'test1',
                "etc/scm-export.properties",
                'scm.export'
        ) >> config2
        1 * config.setEnabled(false)
        1 * config2.setEnabled(false)
        1 * service.pluginConfigService.storeConfig(config, 'test1', "etc/scm-import.properties")
        1 * service.pluginConfigService.storeConfig(config2, 'test1', "etc/scm-export.properties")
        2 * service.jobEventsService.removeListener(_)
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-import.properties")
        1 * service.pluginConfigService.removePluginConfiguration('test1', "etc/scm-export.properties")

    }
}
