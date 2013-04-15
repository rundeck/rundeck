package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluggableService
import com.dtolabs.rundeck.core.plugins.ProviderIdent
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import org.apache.log4j.Logger
import org.springframework.beans.factory.BeanNotOfRequiredTypeException
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Manages a registry for two kinds of plugins: groovy plugins loaded via spring,
 * and Rundeck-core style jar/zip plugins, loaded via {@link ServiceProviderLoader}
 * User: greg
 * Date: 4/11/13
 * Time: 7:07 PM
 * To change this template use File | Settings | File Templates.
 */
class RundeckPluginRegistry implements ApplicationContextAware{
    public static Logger log = Logger.getLogger(RundeckPluginRegistry.class.name)
    HashMap pluginRegistryMap
    def ApplicationContext applicationContext
    def ServiceProviderLoader rundeckServerServiceProviderLoader

    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return
     */
    public Object loadPluginByName(String name, PluggableService service){
        try {
            def beanName = pluginRegistryMap[name]
            if(beanName){
                return applicationContext.getBean(beanName)
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.error("plugin Spring bean does not exist: ${name}")
        }
        //try loading via ServiceProviderLoader
        if(rundeckServerServiceProviderLoader && service){
            try {
                return rundeckServerServiceProviderLoader.loadProvider(service,name)
            } catch (MissingProviderException exception) {
                log.error("plugin Provider does not exist: ${name}")
            } catch (ProviderLoaderException exception) {
                log.error("Failure loading Rundeck provider instance: ${name}",exception)
            }
        }
        null
    }

    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    public Map<String,Object> listPlugins(Class groovyPluginType, PluggableProviderService service){
        def list=[:]
        log.error("pluginRegistryMap: ${pluginRegistryMap}")
        pluginRegistryMap.each { k, String v ->
            try {
                def bean = applicationContext.getBean(v)
//                def test = groovyPluginType.cast(bean)
                if(bean){
                    list[k]=bean
                }else{
                    log.error("bean not right type: ${bean}, class: ${bean.class.name}, assignable: ${groovyPluginType.isAssignableFrom(bean.class)}")
                }
            } catch (NoSuchBeanDefinitionException e) {
                log.error("No such bean: ${v}")
            }catch (BeanNotOfRequiredTypeException e) {
                log.error("Not a valid NotificationPlugin: ${v}")
            }
        }
        if(rundeckServerServiceProviderLoader && service){
            service.listProviders().each{ ProviderIdent ident->
                list[ident.providerName]=rundeckServerServiceProviderLoader.loadProvider(service,ident.providerName)
            }
        }

        list
    }
}
