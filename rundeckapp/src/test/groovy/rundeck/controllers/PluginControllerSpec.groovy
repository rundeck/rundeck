package rundeck.controllers

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import com.dtolabs.rundeck.server.plugins.DescribedPlugin
import grails.testing.web.controllers.ControllerUnitTest
import rundeck.services.FrameworkService
import rundeck.services.PluginApiService
import rundeck.services.PluginService
import spock.lang.Specification

import java.text.SimpleDateFormat

class PluginControllerSpec extends Specification implements ControllerUnitTest<PluginController> {

    String fakePluginId = "fake".encodeAsSHA256().substring(0,12)

    def setup() {
    }

    def cleanup() {
    }

    void "list plugins"() {
        given:
        messageSource.addMessage("framework.service.Notification.description",Locale.ENGLISH,"Triggered when a Job starts, succeeds, or fails.")
        controller.pluginApiService = Mock(PluginApiService)
        def fwksvc = Mock(FrameworkService)
        def fwk = Mock(Framework)
        fwksvc.getRundeckFramework() >> fwk
        fwk.getPluginManager() >> Mock(ServiceProviderLoader)
        controller.frameworkService = fwksvc
        def pluginDescs = [
                "Notification": [new FakePluginDescription()]
        ]
        def pluginData = [
                descriptions        : pluginDescs,
                serviceDefaultScopes: [],
                bundledPlugins      : [],
                embeddedFilenames   : [],
                specialConfiguration: [],
                specialScoping      : [],
                uiPluginProfiles    : []
        ]
        def fakeMeta = getFakeMetadata()

        when:
        1 * controller.pluginApiService.listPlugins() >> pluginData
        1 * controller.frameworkService.rundeckFramework.pluginManager.getPluginMetadata(_,_) >> fakeMeta
        controller.listPlugins()
        def service = response.json[0]
        def entry = service.providers[0]

        then:
        response.json.size() == 1
        service.service == "Notification"
        service.desc == "Triggered when a Job starts, succeeds, or fails."
        service.providers.size() == 1
        entry.id == fakePluginId
        entry.name == "fake"
        entry.title == "Fake Plugin"
        entry.description == "This is the best fake plugin"
        entry.builtin == false
        entry.pluginVersion == "1.0"
        entry.pluginDate == 1534253342000
        entry.enabled == true

    }

    void "plugin detail"() {
        given:
        controller.pluginService = Mock(PluginService)
        def fwksvc = Mock(FrameworkService)
        def fwk = Mock(Framework)
        fwksvc.getRundeckFramework() >> fwk
        fwk.getPluginManager() >> Mock(ServiceProviderLoader)
        controller.frameworkService = fwksvc

        when:
        def fakePluginDesc = new FakePluginDescription()
        params.name = "fake"
        params.service = "Notification"
        1 * controller.pluginService.getPluginDescriptor("fake", NotificationPlugin.class) >> new DescribedPlugin(null,fakePluginDesc,"fake")
        controller.pluginDetail()
        def rj = response.json
        def rp1 = rj.props.find { it.name == "prop1" }
        def rp2 = rj.props.find { it.name == "password" }

        then:
        rj.id == fakePluginId
        rj.name == "fake"
        rj.title == "Fake Plugin"
        rj.desc == "This is the best fake plugin"
        rp1.name == "prop1"
        rp1.title == "Property 1"
        rp1.desc == "A fake property for the fake plugin"
        rp1.defaultValue == "alpha"
        rp1.required == true
        rp1.allowed == ["alpha","beta","gamma"]
        rp2.name == "password"
        rp2.title == "Password"
        rp2.desc == "The password to the fake plugin"
        rp2.defaultValue == null
        rp2.required == false
        rp2.allowed == null
    }

    PluginMetadata getFakeMetadata() {
        return new PluginMetadata() {
            @Override
            String getFilename() {
                return null
            }

            @Override
            File getFile() {
                return null
            }

            @Override
            String getPluginAuthor() {
                return null
            }

            @Override
            String getPluginFileVersion() {
                return "1.0"
            }

            @Override
            String getPluginVersion() {
                return "1.0"
            }

            @Override
            String getPluginUrl() {
                return null
            }

            @Override
            Date getPluginDate() {
                return  new SimpleDateFormat("EEE MMM dd hh:mm:ss Z yyyy").parse("Tue Aug 14 08:29:02 CDT 2018")
            }

            @Override
            Date getDateLoaded() {
                return new Date()
            }

            @Override
            String getPluginName() {
                return null
            }

            @Override
            String getPluginDescription() {
                return null
            }

            @Override
            String getPluginId() {
                return null
            }

            @Override
            String getRundeckCompatibilityVersion() {
                return null
            }

            @Override
            String getTargetHostCompatibility() {
                return null
            }

            @Override
            List<String> getTags() {
                return null
            }

            @Override
            String getPluginLicense() {
                return null
            }

            @Override
            String getPluginThirdPartyDependencies() {
                return null
            }

            @Override
            String getPluginSourceLink() {
                return null
            }

            @Override
            String getPluginType() {
                return null
            }
        }
    }


    private class FakePluginDescription implements Description {

        @Override
        String getName() {
            return "fake"
        }

        @Override
        String getTitle() {
            return "Fake Plugin"
        }

        @Override
        String getDescription() {
            return "This is the best fake plugin"
        }

        @Override
        List<Property> getProperties() {
            def p1 = PropertyBuilder.builder()
                                    .name("prop1")
                                    .title("Property 1")
                                    .description("A fake property for the fake plugin")
                                    .required(true)
                                    .defaultValue("alpha")
                                    .values("alpha","beta","gamma")
                                    .type(Property.Type.Select)
                                    .build()
            def p2 = PropertyBuilder.builder()
                                    .name("password")
                                    .title("Password")
                                    .description("The password to the fake plugin")
                                    .required(false)
                                    .type(Property.Type.String)
                                    .renderingAsPassword()
                                    .build()

            return [ p1, p2]
        }

        @Override
        Map<String, String> getPropertiesMapping() {
            return [:]
        }

        @Override
        Map<String, String> getFwkPropertiesMapping() {
            return [:]
        }
    }
}
