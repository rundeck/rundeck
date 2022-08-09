package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import spock.lang.Specification
import spock.lang.Unroll

class LoadingPluginRegistryComponentSpec extends Specification {


    @Unroll
    def "get resource loader"() {
        given:

            ServiceProviderLoader serviceProviderLoader = Mock(ServiceProviderLoader)
            def sut = new LoadingPluginRegistryComponent(serviceProviderLoader)

            def loader = Mock(PluginResourceLoader)
        when:
            def result = sut.getResourceLoader('svc', 'plugin2')

        then:

            1 * serviceProviderLoader.getResourceLoader('svc', 'plugin2') >> loader
            result == loader
    }

    @Unroll
    def "get plugin metadata"() {
        given:

            ServiceProviderLoader serviceProviderLoader = Mock(ServiceProviderLoader)
            def sut = new LoadingPluginRegistryComponent(serviceProviderLoader)

            def meta = Mock(PluginMetadata)

        when:
            def result = sut.getPluginMetadata('svc', 'plugin2')

        then:

            1 * serviceProviderLoader.getPluginMetadata('svc', 'plugin2') >> meta
            result == meta
    }

    @Unroll
    def "load plugin descriptor by name undescribable"() {
        given:

            ServiceProviderLoader serviceProviderLoader = Mock(ServiceProviderLoader)
            def sut = new LoadingPluginRegistryComponent(serviceProviderLoader)

            def svc = Mock(PluggableProviderService) {
                getName() >> 'aSvc'
                1 * providerOfType(_) >> null
            }
            def obj = new Object()

        when:
            def result = sut.loadPluginDescriptorByName('provider', svc)

        then:
            1 * serviceProviderLoader.loadProvider(svc, 'provider') >> obj
            result.instance == obj
            result.description == null
    }

    @Unroll
    def "load plugin descriptor by name describable"() {
        given:

            ServiceProviderLoader serviceProviderLoader = Mock(ServiceProviderLoader)
            def sut = new LoadingPluginRegistryComponent(serviceProviderLoader)

            def svc = Mock(PluggableProviderService) {
                getName() >> 'aSvc'
                1 * providerOfType(_) >> null
            }
            def description = Mock(Description)
            def obj = Mock(Describable) {
                getDescription() >> description
            }

        when:
            def result = sut.loadPluginDescriptorByName('provider', svc)

        then:
            1 * serviceProviderLoader.loadProvider(svc, 'provider') >> obj
            result.instance == obj
            result.description == description
    }


}
