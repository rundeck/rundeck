package rundeck.services

import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginResourceLoader
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import grails.test.mixin.TestFor
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

class UiPluginServiceSpec extends Specification implements ServiceUnitTest<UiPluginService> {

    void "test pluginsForPage"() {
        given:
        service.pluginService = Mock(PluginService)

        when:
        def result = service.pluginsForPage(path)

        then:
        1 * service.pluginService.listPlugins(UIPlugin, _) >> [
                'b': new DescribedPlugin<UIPlugin>(Mock(UIPlugin), null, 'b', null, null),
                'a': new DescribedPlugin<UIPlugin>(Mock(UIPlugin), null, 'a', null, null)
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

    void "getProfileFor finds icon"() {
        given:
        service.initCaches()
        service.pluginService = Mock(PluginService)
        service.rundeckPluginRegistry = Mock(PluginRegistry)

        when:
        def result = service.getProfileFor('svc', 'plug')

        then:
            result.fileMetadata != null
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

    void "getProfileFor returns provider metadata"() {
        given:
            service.initCaches()
            service.pluginService = Mock(PluginService)
            service.rundeckPluginRegistry = Mock(PluginRegistry)

        when:
            def result = service.getProfileFor('svc', 'plug')

        then:
            result.fileMetadata != null
            2 * service.rundeckPluginRegistry.getPluginMetadata('svc', 'plug') >> Mock(PluginMetadata)
            1 * service.rundeckPluginRegistry.getResourceLoader('svc', 'plug') >> Mock(PluginResourceLoader) {
                listResources() >> []
            }
            1 * service.pluginService.getPluginDescriptor('plug', 'svc') >> new DescribedPlugin<Object>(
                    null,
                    DescriptionBuilder.builder().name('plug').metadata([a: 'b', c: 'd']).build(),
                    'plug',
                    null,
                    null
            )
            result.providerMetadata
            result.providerMetadata == [a: 'b', c: 'd']
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

    void "get plugin message"() {
        given:
            service.initCaches()
            service.pluginService = Mock(PluginService)
            service.rundeckPluginRegistry = Mock(PluginRegistry)

        when:
            def result = service.getPluginMessage('svc', 'plug', 'a', 'somemsg', locale)
        then:
            2 * service.rundeckPluginRegistry.getPluginMetadata('svc', 'plug') >> Mock(PluginMetadata)
            _ * service.rundeckPluginRegistry.getResourceLoader('svc', 'plug') >> Mock(PluginResourceLoader) {
                listResources() >> reslist
                if (expectpath) {
                    openResourceStreamFor(expectpath) >> {
                        return new ByteArrayInputStream(data.bytes)
                    }
                }
            }
            result == expected

        where:
            locale | data                                     | expected
            null   | 'b=b\n'                                  | 'somemsg'
            null   | 'a=b\n'                                  | 'b'
            null   | 'svc.a=c\na=b\n'                         | 'c'
            null   | 'plug.a=d\nsvc.a=c\na=b\n'               | 'd'
            null   | 'svc.plug.a=e\nplug.a=d\nsvc.a=c\na=b\n' | 'e'

            reslist = ['i18n/messages.properties']
            expectpath = 'i18n/messages.properties'
    }
}

