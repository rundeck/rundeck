/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification

import java.text.SimpleDateFormat

class PluginApiServiceSpec extends Specification implements ServiceUnitTest<PluginApiService> {
    String fakePluginId = "Fake Plugin".encodeAsSHA256().substring(0,12)

    void "list plugins"() {
        given:
        messageSource.addMessage("framework.service.Notification.description",Locale.ENGLISH,"Triggered when a Job starts, succeeds, or fails.")
        def fwksvc = Mock(FrameworkService)
        def fwk = Mock(Framework)
        fwksvc.getRundeckFramework() >> fwk
        fwk.getPluginManager() >> Mock(ServiceProviderLoader)
        service.frameworkService = fwksvc

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
        def fakeMeta = new FakePluginMetadata()

        service.metaClass.listPluginsDetailed = { -> pluginData }
        service.metaClass.getLocale = { -> Locale.ENGLISH }

        when:
        1 * service.frameworkService.rundeckFramework.pluginManager.getPluginMetadata(_,_) >> fakeMeta
        def response = service.listPlugins()
        def service = response[0]
        def entry = service.providers[0]

        then:
        response.size() == 1
        service.service == "Notification"
        service.desc == "Triggered when a Job starts, succeeds, or fails."
        service.providers.size() == 1
        entry.pluginId == fakePluginId
        entry.name == "fake"
        entry.title == "Fake Plugin"
        entry.description == "This is the best fake plugin"
        entry.builtin == false
        entry.pluginVersion == "1.0"
        entry.pluginDate == 1534253342000
        entry.enabled == true

    }

    def "list installed plugin ids"() {
        given:
        messageSource.addMessage("framework.service.Notification.description",Locale.ENGLISH,"Triggered when a Job starts, succeeds, or fails.")
        def fwksvc = Mock(FrameworkService)
        def fwk = Mock(Framework)
        fwksvc.getRundeckFramework() >> fwk
        fwk.getPluginManager() >> Mock(ServiceProviderLoader)
        service.frameworkService = fwksvc
        service.pluginService = Mock(PluginService)

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
        def fakeMeta = new FakePluginMetadata()

        service.metaClass.listPluginsDetailed = { -> pluginData }
        service.metaClass.getLocale = { -> Locale.ENGLISH }

        when:
        String uipluginid = PluginUtils.generateShaIdFromName("oneuiplugin")
        def uiplugin = Stub(UIPlugin)
        def uiplugindesc = Stub(Description)
        def uiMeta = Stub(PluginMetadata) {
            getPluginId() >> uipluginid
        }
        1 * service.pluginService.listPlugins(_,_) >> ["oneuiplugin":new FakeUIDescribedPlugin(uiplugin,uiplugindesc,"oneuiplugin")]
        1 * service.frameworkService.rundeckFramework.pluginManager.getPluginMetadata('Notification',_) >> fakeMeta
        1 * service.frameworkService.rundeckFramework.pluginManager.getPluginMetadata('UI',_) >> uiMeta
        def idList = service.listInstalledPluginIds()

        then:
        idList.size() == 2
        idList.keySet().contains(uipluginid)
    }

    def "plugin Property Map"() {
        given:
            service.uiPluginService = Mock(UiPluginService)
            def prop = PropertyBuilder.builder()
                                      .name("prop1")
                                      .title("Property 1")
                                      .description("A fake property for the fake plugin")
                                      .required(true)
                                      .defaultValue("alpha")
                                      .values("alpha", "beta", "gamma")
                                      .type(Property.Type.Select)
            .renderingOptions([(StringRenderingConstants.DISPLAY_TYPE_KEY):StringRenderingConstants.DisplayType.CODE])
                                      .build()

            service.metaClass.getLocale = { -> Locale.ENGLISH }
        when:
            def result = service.pluginPropertyMap('svc', 'provider', prop)
        then:
            result != null
            1 * service.uiPluginService.getPluginMessage(
                'svc',
                'provider',
                "property.prop1.title",
                _,
                _
            ) >> 'title.message'
            1 * service.uiPluginService.getPluginMessage(
                'svc',
                'provider',
                "property.prop1.defaultValue",
                _,
                _
            ) >> 'defaultValue.message'
            result.name == 'prop1'
            result.desc == 'A fake property for the fake plugin'
            result.title == 'title.message'
            result.defaultValue == 'alpha'
            result.staticTextDefaultValue == 'defaultValue.message'
            result.required == true
            result.type == 'Select'
            result.allowed == ['alpha', 'beta', 'gamma']
            result.selectLabels == null
            result.scope == null
            result.options == ['displayType':'CODE']

    }

    class FakeUIDescribedPlugin extends DescribedPlugin<UIPlugin> {

        FakeUIDescribedPlugin(
                final UIPlugin instance,
                final Description description,
                final String name
        ) {
            super(instance, description, name)
        }
    }

    class FakePluginMetadata implements PluginMetadata {

        @Override
        String getFilename() {
            return null
        }

        @Override
        File getFile() {
            return null
        }

        @Override
        String getPluginArtifactName() {
            return "Fake Plugin"
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
            return "Fake Plugin"
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
        String getPluginDocsLink() {
            return null
        }

        @Override
        String getPluginType() {
            return null
        }

    }


    class FakePluginDescription implements Description {
        String name = 'fake'

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

