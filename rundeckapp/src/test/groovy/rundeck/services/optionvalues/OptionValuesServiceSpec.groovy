package rundeck.services.optionvalues

import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.plugins.option.OptionValue
import com.dtolabs.rundeck.plugins.option.OptionValuesPlugin
import grails.testing.services.ServiceUnitTest
import rundeck.services.FrameworkService
import rundeck.services.PluginService
import rundeck.services.StorageService
import spock.lang.Specification

class OptionValuesServiceSpec extends Specification implements ServiceUnitTest<OptionValuesService>{

    def setup() {
        service.rundeckServerServiceProviderLoader = Stub(ServiceProviderLoader)
        service.frameworkService = Mock(FrameworkService)
        service.frameworkService.getFrameworkPropertyResolver(_,_) >> new PropertyResolver() {
            @Override
            Object resolvePropertyValue(final String name, final PropertyScope scope) {
                return null
            }
        }
        service.storageService = Mock(StorageService)
    }

    def cleanup() {
    }

    void "get Options"() {
        setup:
        service.pluginService = Mock(PluginService)

        when:
        TestOptionValuesPlugin plugin = new TestOptionValuesPlugin()
        1 * service.pluginService.configurePlugin(_,_,_,_,_) >> new ConfiguredPlugin<OptionValuesPlugin>(plugin,null)
        def results = service.getOptions("AProject","optValProvider",null)

        then:
        results.size() == 1

    }

    void "listOptionValuesPlugins"() {
        setup:
        service.pluginService = Mock(PluginService)

        when:
        TestOptionValuesPlugin plugin = new TestOptionValuesPlugin()
        1 * service.pluginService.listPlugins(_,_) >> ["plugin":plugin]
        def results = service.listOptionValuesPlugins()

        then:
        results.size() == 1
    }

    class TestOptionValuesPlugin implements OptionValuesPlugin {

        @Override
        List<OptionValue> getOptionValues(final Map config) {
            return [[name:"opt1",value:"o1"]]
        }
    }
}
