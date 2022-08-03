package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.plugins.PluginRegistryComponent
import com.dtolabs.rundeck.core.plugins.SpringPluginRegistryComponent
import groovy.transform.CompileStatic
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * uses the main application context to source provider beans, stores mapping of service:providername to bean name
 *  New providers can be registered using the {@link SpringPluginRegistryComponent} interface..
 */
@CompileStatic
class RundeckSpringPluginRegistryComponent extends BaseSpringPluginRegistryComponent
    implements PluginRegistryComponent, SpringPluginRegistryComponent, ApplicationContextAware {
    /**
     * Registry of spring bean plugin providers, "service:providername"->"beanname"
     */
    Map<String, String> pluginRegistryMap = [:]
    def ApplicationContext applicationContext


    void registerPlugin(String type, String name, String beanName) {
        pluginRegistryMap.putIfAbsent(type + ":" + name, beanName)
    }

    String getProviderBeanName(final String type, final String name) {
        pluginRegistryMap["${type}:${name}".toString()] ?: pluginRegistryMap[name]
    }

    @Override
    Map<String, Object> getProviderBeans() {
        return pluginRegistryMap.values().collectEntries { String name ->
            [name, applicationContext.getBean(name)]
        }
    }

    @Override
    def Object findProviderBean(final String type, final String name) {
        def name1 = getProviderBeanName(type, name)
        return name1 ? applicationContext.getBean(name1) : null
    }
}
