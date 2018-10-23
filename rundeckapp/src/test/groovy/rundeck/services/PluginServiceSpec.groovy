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

import com.dtolabs.rundeck.core.plugins.CloseableProvider
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * @author greg
 * @since 6/12/17
 */
@TestFor(PluginService)
class PluginServiceSpec extends Specification {
    def "configure plugin does not exist"() {
        given:
        service.rundeckPluginRegistry = Mock(PluginRegistry)
        def name = 'atest'
        def config = [:]
        def project = 'aproject'
        def framework = null
        def providerservice = Mock(PluggableProviderService)

        when:
        def result = service.configurePlugin(name, config, project, framework, providerservice)

        then:
        result == null
        //validatePluginByName returns null
        1 * service.rundeckPluginRegistry.validatePluginByName('atest', providerservice, null, 'aproject', config) >>
                null


    }

    def "get plugin"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
        when:
            def result = service.getPlugin('blah', String)
        then:
            1 * service.rundeckPluginRegistry.createPluggableService(String) >> providerService
            1 * service.rundeckPluginRegistry.loadPluginByName('blah', providerService) >> 'bloo'
            result == 'bloo'
    }

    def "get plugin descriptor"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
            def describedPlugin = new DescribedPlugin(null, null, 'blah')
        when:
            def result = service.getPluginDescriptor('blah', ServiceNameConstants.LogFilter)
        then:
            1 * service.rundeckPluginRegistry.createPluggableService(LogFilterPlugin) >> providerService
            1 * service.rundeckPluginRegistry.loadPluginDescriptorByName('blah', providerService) >> describedPlugin
            result == describedPlugin
    }

    def "get plugin type by service"() {
        when:
            def result = service.getPluginTypeByService(svcname)
        then:
            result == clazz
        where:
            svcname                        | clazz
            ServiceNameConstants.LogFilter | LogFilterPlugin
    }

    def "get plugin type by service DNE"() {
        when:
            def result = service.getPluginTypeByService(svcname)
        then:
            IllegalArgumentException e = thrown()
        where:
            svcname      | _
            'DNEService' | _
    }

    def "retain plugin"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
            def closeable = Mock(CloseableProvider)
        when:
            def result = service.retainPlugin('blah', providerService)
        then:
            1 * service.rundeckPluginRegistry.retainPluginByName('blah', providerService) >> closeable
            result == closeable
    }

    def "configure plugin"() {
        given:
            service.rundeckPluginRegistry = Mock(PluginRegistry)
            def providerService = Mock(PluggableProviderService)
            def configuredPlugin = new ConfiguredPlugin<String>('blah', config)
        when:
            def result = service.configurePlugin(provider, config, String)
        then:
            1 * service.rundeckPluginRegistry.createPluggableService(String) >> providerService
            1 * service.rundeckPluginRegistry.validatePluginByName(provider, providerService, config/*, null*/)
            1 * service.rundeckPluginRegistry.configurePluginByName(provider, providerService, config/*, null*/) >>
            configuredPlugin
            result == configuredPlugin
        where:
            provider    | config
            'aprovider' | [some: 'config']
    }
}
