package rundeck.services.passwordencrypt

import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypterPlugin
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.server.plugins.RundeckPluginRegistry
import grails.testing.services.ServiceUnitTest
import rundeck.services.FrameworkService
import rundeck.services.PasswordUtilityEncrypterLoaderService
import rundeck.services.PluginService
import spock.lang.Specification

class PasswordUtilityEncrypterLoaderServiceSpec extends Specification implements ServiceUnitTest<PasswordUtilityEncrypterLoaderService> {

    def setup() {
        service.rundeckPluginRegistry = Stub(RundeckPluginRegistry)
        service.frameworkService = Mock(FrameworkService)
        service.frameworkService.getFrameworkPropertyResolver(_,_) >> new PropertyResolver() {
            @Override
            Object resolvePropertyValue(final String name, final PropertyScope scope) {
                return null
            }
        }
    }

    void "listPlugins"() {
        setup:
        TestPasswordEncryptPlugin plugin = new TestPasswordEncryptPlugin()

        service.pluginService = Mock(PluginService){
            configurePlugin(_,_,_,_) >> new ConfiguredPlugin<PasswordUtilityEncrypterPlugin>(plugin,null)
            listPlugins(_,_) >> ["plugin":plugin]
        }

        when:
        def results = service.getPasswordUtilityEncrypters()

        then:
        results.size() == 1
    }

    class TestPasswordEncryptPlugin implements PasswordUtilityEncrypterPlugin {

        @Override
        Map encrypt(Map params) {
            def result = [:]
            result.obfuscate ="ENC(values)"
            return result
        }
    }

}
