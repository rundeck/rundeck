package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import com.dtolabs.rundeck.server.plugins.PluginRegistry
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(UiPluginService)
class UiPluginServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test pluginsForPage"() {
        given:
        service.pluginService = Mock(PluginService)

        when:
        def result = service.pluginsForPage(path)

        then:
        1 * service.pluginService.listPlugins(UIPlugin, _) >> [
                'b': new DescribedPlugin<UIPlugin>(Mock(UIPlugin), null, 'b'),
                'a': new DescribedPlugin<UIPlugin>(Mock(UIPlugin), null, 'a')
        ]
        1 * service.pluginService.getPlugin('a', _) >> Mock(UIPlugin) {
            1 * doesApply(path) >> ('a' in applies)
        }
        1 * service.pluginService.getPlugin('b', _) >> Mock(UIPlugin) {
            1 * doesApply(path) >> ('b' in applies)
        }

        result.keySet() == applies as Set

        where:
        path             | applies    | _
        'menu/something' | ['a', 'b'] | _
        'menu/d'         | ['a']      | _
        'menu/x'         | ['b']      | _
    }

    void "getProfileFor"() {
        given:
        service.initCaches()
        service.pluginService = Mock(PluginService)
        service.rundeckPluginRegistry = Mock(PluginRegistry)

        when:
        def result = service.getProfileFor('svc', 'plug')

        then:
        result.metadata != null
        result.icon == icon
        2 * service.rundeckPluginRegistry.getPluginMetadata('svc', 'plug') >> Mock(PluginMetadata)
        1 * service.rundeckPluginRegistry.getResourceLoader('svc', 'plug') >> Mock(PluginResourceLoader) {
            listResources() >> reslist
        }

        where:
        reslist                           | icon
        null                              | null
        []                                | null
        ['icon.png']                      | 'icon.png'
        ['icon.gif']                      | 'icon.gif'
        ['svc.plug.icon.png', 'icon.png'] | 'svc.plug.icon.png'
        ['svc.icon.png', 'icon.png']      | 'svc.icon.png'
        ['plug.icon.png', 'icon.png']     | 'plug.icon.png'
    }

    void "getMessagesFor"() {
        given:
        service.initCaches()
        service.pluginService = Mock(PluginService)
        service.rundeckPluginRegistry = Mock(PluginRegistry)

        when:
        def result = service.getMessagesFor('svc', 'plug', locale)

        then:
        result == messages
        2 * service.rundeckPluginRegistry.getPluginMetadata('svc', 'plug') >> Mock(PluginMetadata)
        _ * service.rundeckPluginRegistry.getResourceLoader('svc', 'plug') >> Mock(PluginResourceLoader) {
            listResources() >> reslist
            if (expectpath) {
                openResourceStreamFor(expectpath) >> {
                    return new ByteArrayInputStream('a=b\n'.bytes)
                }
            }
        }

        where:
        locale        | reslist                                                     | messages | expectpath
        null          | null                                                        | [:]      | null
        null          | []                                                          | [:]      | null
        null          | ['i18n/messages.properties']                                |
                [a: 'b']                                                                       |
                'i18n/messages.properties'
        Locale.FRENCH | ['i18n/messages.properties']                                |
                [a: 'b']                                                                       |
                'i18n/messages.properties'
        Locale.FRENCH | ['i18n/messages_fr.properties', 'i18n/messages.properties'] |
                [a: 'b']                                                                       |
                'i18n/messages_fr.properties'
        Locale.FRENCH | ['i18n/svc.messages_fr.properties', 'i18n/messages_fr.properties', 'i18n/messages.properties'] |
                [a: 'b']                                                                       |
                'i18n/svc.messages_fr.properties'
        Locale.FRENCH | ['i18n/plug.messages_fr.properties', 'i18n/messages_fr.properties', 'i18n/messages.properties'] |
                [a: 'b']                                                                       |
                'i18n/plug.messages_fr.properties'
        Locale.FRENCH | ['i18n/svc.plug.messages_fr.properties', 'i18n/messages_fr.properties', 'i18n/messages.properties'] |
                [a: 'b']                                                                       |
                'i18n/svc.plug.messages_fr.properties'
        Locale.FRENCH | ['i18n/svc.plug.messages_fr.properties', 'i18n/svc.messages_fr.properties', 'i18n/messages_fr.properties', 'i18n/messages.properties'] |
                [a: 'b']                                                                       |
                'i18n/svc.plug.messages_fr.properties'
    }
}
