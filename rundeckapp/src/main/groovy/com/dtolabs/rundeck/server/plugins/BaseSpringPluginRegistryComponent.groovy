package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.plugins.*
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.PluginAdapterUtility
import com.dtolabs.rundeck.plugins.ServiceTypes
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import groovy.transform.CompileStatic
import groovy.transform.PackageScope
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.NoSuchBeanDefinitionException

/**
 * base registry component that uses a Spring context, with a mapping between
 * provider identity (service+provider name) to bean name.
 */
@CompileStatic
@Slf4j
abstract class BaseSpringPluginRegistryComponent
    implements PluginRegistryComponent {

    def File pluginDirectory

    @Override
    <T> CloseableDescribedPlugin<T> retainPluginDescriptorByName(
        final String name,
        final PluggableProviderService<T> service
    ) {
        DescribedPlugin<T> beanPlugin = loadBeanDescriptor(name, service.name) as DescribedPlugin<T>
        if (null == beanPlugin) {
            return null
        }
        return new CloseableDescribedPlugin<T>(beanPlugin)
    }

    @Override
    <T> DescribedPlugin<T> loadPluginDescriptorByName(
        final String name,
        final PluggableProviderService<T> service
    ) {
        return loadBeanDescriptor(name, service.name) as DescribedPlugin<T>
    }

    private DescribedPlugin<?> loadBeanDescriptor(String name, String type) {
        try {
            def bean = findProviderBean(type, name)
            if (!bean) {
                return null
            }
            if (bean instanceof PluginBuilder) {
                bean = ((PluginBuilder) bean).buildPlugin()
            }
            if (!bean) {
                return null
            }

            final Plugin annotation1 = bean.getClass().getAnnotation(Plugin.class);
            if (type && annotation1 && annotation1.service() != type) {
                return null
            }

            Description desc = null
            if (bean instanceof Describable) {
                desc = ((Describable) bean).description
            } else if (PluginAdapterUtility.canBuildDescription(bean)) {
                desc = PluginAdapterUtility.buildDescription(bean, DescriptionBuilder.builder())
            }
            if (!desc) {
                return null
            }
            return new DescribedPlugin<?>(bean, desc, name, new File(pluginDirectory, name + ".groovy"))
        } catch (NoSuchBeanDefinitionException e) {
            log.error("plugin Spring bean does not exist: ${name}")
        }
        return null
    }
    /**
     * Look for specified bean in subcontexts if present, or in applicationContext
     * @param beanName
     * @return
     */
    @PackageScope
    abstract Object findProviderBean(String type, String name)

    /**
     *
     * @return map of spring bean name to object for registered plugins
     */
    abstract Map<String, Object> getProviderBeans()

    @Override
    def <T> Map<String, DescribedPlugin<T>> listPluginDescriptors(
        final Class<T> pluginType,
        final PluggableProviderService<T> service
    ) {
        def Map<String, DescribedPlugin<T>> map = [:]
        getProviderBeans().each { String pluginName, Object bean ->
            try {
                if (bean instanceof PluginBuilder) {
                    bean = ((PluginBuilder) bean).buildPlugin()
                }
                if (bean != null && pluginType.isAssignableFrom(bean.class)) {
                    def file = new File(pluginDirectory, pluginName + ".groovy")
                    //try to check annotations
                    Description desc = null
                    if (bean instanceof Describable) {
                        desc = ((Describable) bean).description
                    } else if (PluginAdapterUtility.canBuildDescription(bean)) {
                        desc = PluginAdapterUtility.buildDescription(bean, DescriptionBuilder.builder())
                    }
                    map[pluginName] = new DescribedPlugin(bean, desc, pluginName, file.exists() ? file : null)
                }
            } catch (NoSuchBeanDefinitionException e) {
                log.error("No such bean: ${pluginName}")
            } catch (BeanNotOfRequiredTypeException e) {
                log.error("Not a valid instance: ${pluginName}")
            }
        }
        return map
    }

    private String extractPluginName(String key) {
        List k = Arrays.asList(key?.split(':'))
        if (k?.size() > 1) {
            return k.get(1)
        }
        return key
    }

    @Override
    PluginResourceLoader getResourceLoader(final String service, final String provider) throws ProviderLoaderException {
        // check groovy bean plugins
        try {
            def bean = findProviderBean(service, provider)
            if (bean instanceof PluginResourceLoader) {
                return bean
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.debug("No such plugin found: ${service},${provider}")
        }

        return null
    }

    @Override
    PluginMetadata getPluginMetadata(final String service, final String provider) throws ProviderLoaderException {
        Class clazz = ServiceTypes.getPluginType(service)
        try {
            def bean = findProviderBean(service, provider)
            if (bean instanceof PluginBuilder) {
                if (((PluginBuilder)bean).pluginClass == clazz) {
                    if (bean instanceof PluginMetadata) {
                        def metadata = bean as PluginMetadata
                        return metadata
                    }
                }
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.debug("No such bean: ${service},${provider}")
        }
        return null
    }
}
