package com.dtolabs.rundeck.server.plugins


import com.dtolabs.rundeck.core.plugins.PluginRegistryComponent
import groovy.transform.CompileStatic
import org.springframework.context.ApplicationContext

/**
 * Allows using subcontexts to register beans as plugin providers
 */
@CompileStatic
class RundeckDynamicSpringPluginRegistryComponent extends BaseSpringPluginRegistryComponent
    implements PluginRegistryComponent {

    /**
     * Mapping from service:provider to bean name
     */
    Map<String, String> pluginBeanNames = [:]
    /**
     * mapping from bean name to subcontext which contains the bean, groovy plugin sources loaded dynamically will
     * live in a sub context
     */
    def Map<String, ApplicationContext> subContexts = [:]


    def registerDynamicPluginBean(String type, String beanName, ApplicationContext context) {
        subContexts[beanName] = context
        pluginBeanNames[type + ':' + beanName] = beanName
    }

    String getProviderBeanName(final String type, final String name) {
        pluginBeanNames["${type}:${name}"]
    }

    @Override
    Map<String, Object> getProviderBeans() {
        return pluginBeanNames.values().collectEntries { String name ->
            [name, subContexts[name].getBean(name)]
        }
    }

    @Override
    def Object findProviderBean(final String type, final String name) {
        String beanName = getProviderBeanName(type, name)
        subContexts[beanName]?.getBean(beanName)
    }
}
