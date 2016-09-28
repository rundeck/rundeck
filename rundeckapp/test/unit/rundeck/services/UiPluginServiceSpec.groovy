package rundeck.services

import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
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
}
