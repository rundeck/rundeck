package rundeck.services.passwordencrypt

import com.dtolabs.rundeck.core.encrypter.EncryptorResponse
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

    void "run encrypt"(){
        setup:
        TestPasswordEncryptPlugin plugin = new TestPasswordEncryptPlugin()

        service.pluginService = Mock(PluginService){
            configurePlugin(_,_,_,_) >> new ConfiguredPlugin<PasswordUtilityEncrypterPlugin>(plugin,null)
            listPlugins(_,_) >> ["plugin":plugin]
        }

        when:
        def params = ["valueToEncrypt":"123", "valid": valid ]
        def encoder = service.getPasswordEncoder("TestPasswordEncryptPlugin", params)
        EncryptorResponse response = encoder.instance.encrypt(params)

        then:
        response.isValid() == isValid
        response.error == error
        response.outputs == outputs

        where:
        valid      | isValid | error              | outputs
        true       | true    | null                 | ["encrypt": "ENC(values)"]
        false      | false   | "error encrypting"   | null

    }

    class TestPasswordEncryptPlugin implements PasswordUtilityEncrypterPlugin {

        @Override
        EncryptorResponse encrypt(Map config) {
            EncryptorResponseImp  responseImp = new EncryptorResponseImp()
            def valid = config.get("valid")
            if (valid){
                responseImp.isValid = true
                responseImp.outputs = ["encrypt": "ENC(values)"]
            }else{
                responseImp.isValid = false
                responseImp.error =  "error encrypting"
            }

            return responseImp
        }
    }

    class EncryptorResponseImp implements EncryptorResponse{
        Boolean isValid
        String error
        Map outputs


        @Override
        boolean isValid() {
            return isValid
        }

        @Override
        String getError() {
            return error
        }

        @Override
        Map<String, String> getOutputs() {
            return outputs
        }
    }

}