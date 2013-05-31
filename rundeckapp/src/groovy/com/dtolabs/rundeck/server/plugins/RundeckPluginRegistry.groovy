package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.execution.service.MissingProviderException
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException
import com.dtolabs.rundeck.core.execution.workflow.steps.PluginAdapterUtility
import com.dtolabs.rundeck.core.execution.workflow.steps.PropertyResolver
import com.dtolabs.rundeck.core.execution.workflow.steps.PropertyResolverFactory
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.ProviderIdent
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
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
 */
class RundeckPluginRegistry implements ApplicationContextAware{
    public static Logger log = Logger.getLogger(RundeckPluginRegistry.class.name)
    HashMap pluginRegistryMap
    def ApplicationContext applicationContext
    def ServiceProviderLoader rundeckServerServiceProviderLoader
    def File pluginDirectory
    def File pluginCacheDirectory

    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return
     */
    public Object configurePluginByName(String name, PluggableProviderService service, Map configuration) {
        Map pluginDesc = loadPluginDescriptorByName(name, service)
        Object plugin=pluginDesc['instance']

//        if (null != configuration) {
//            configuration = DataContextUtils.replaceDataReferences(configuration, context.getDataContext());
//        }
        def description = pluginDesc['description']
        if(description && description instanceof Description){
            final PropertyResolver resolver = PropertyResolverFactory.createInstanceResolver(configuration);
            final Map<String, Object> config = PluginAdapterUtility.configureProperties(resolver, description, plugin);
        }
        plugin
    }
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return
     */
    public Object configurePluginByName(String name, PluggableProviderService service, Framework framework, String project) {
        Map pluginDesc = loadPluginDescriptorByName(name, service)
        Object plugin=pluginDesc['instance']

//        if (null != configuration) {
//            configuration = DataContextUtils.replaceDataReferences(configuration, context.getDataContext());
//        }
        def description = pluginDesc['description']
        if(description && description instanceof Description) {
            final PropertyResolver resolver = PropertyResolverFactory.createFrameworkProjectRuntimeResolver(framework, project, null, name, service.getName());
            final Map<String, Object> config = PluginAdapterUtility.configureProperties(resolver, description, plugin);
        }
        plugin
    }
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return
     */
    public Object loadPluginByName(String name, PluggableProviderService service){
        return loadPluginDescriptorByName(name,service)?.get('instance')
    }
    /**
     * Load a plugin instance with the given bean or provider name
     * @param name name of bean or provider
     * @param service provider service
     * @return map containing [instance:(plugin instance), description: (map or Description),
     */
    public Map loadPluginDescriptorByName(String name, PluggableProviderService service){
        def beanDesc=[:]
        try {
            def beanName = pluginRegistryMap[name]
            if(beanName){
                def bean = applicationContext.getBean(beanName)
                if(bean instanceof PluginBuilder){
                    bean=((PluginBuilder)bean).buildPlugin()
                }
                beanDesc=[instance:bean,
                        description: [
                                name: name,
                        ],
                        file: new File(pluginDirectory, name + ".groovy")
                ]
                //try to check annotations
                if (bean instanceof Describable) {
                    beanDesc['description'] = ((Describable) bean).description
                } else if (PluginAdapterUtility.canBuildDescription(bean)) {
                    beanDesc['description'] = PluginAdapterUtility.buildDescription(bean, DescriptionBuilder.builder())
                }
                return beanDesc
            }
        } catch (NoSuchBeanDefinitionException e) {
            log.error("plugin Spring bean does not exist: ${name}")
        }
        //try loading via ServiceProviderLoader
        if(rundeckServerServiceProviderLoader && service){
            try {
                def instance = rundeckServerServiceProviderLoader.loadProvider(service, name)
                beanDesc= [
                        instance: instance,
                        description: [
                                name: name
                        ]
                ]
                def d = service.listDescriptions().find {it.name==name}
                if(d){
                    beanDesc.description=d
                }
                return beanDesc
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
        def list=listPluginDescriptors(groovyPluginType,service)
        def Map map=[:]
        list.each {k,v->
            map[k]=v['instance']
        }
        map
    }
    /**
     * List all plugin type definitions that are either ServiceProvider plugins of the given service name,
     * or are groovy plugins of the given type
     * @param providerServiceName
     * @param groovyPluginType
     * @return
     */
    public Map<String,Object> listPluginDescriptors(Class groovyPluginType, PluggableProviderService service){
        def list=[:]
        pluginRegistryMap.each { k, String v ->
            try {
                def bean = applicationContext.getBean(v)
                if (bean instanceof PluginBuilder) {
                    bean = ((PluginBuilder) bean).buildPlugin()
                }
                if(bean!=null && groovyPluginType.isAssignableFrom(bean.class)){
                    list[k]=[instance:bean,
                            description:[
                                name:k,
                            ],
                            file: new File(pluginDirectory, k + ".groovy")
                    ]
                    //try to check annotations
                    if(bean instanceof Describable){
                        list[k]['description'] = ((Describable)bean).description
                    }else if (PluginAdapterUtility.canBuildDescription(bean)){
                        list[k]['description']= PluginAdapterUtility.buildDescription(bean,DescriptionBuilder.builder())
                    }
                }else{
                    log.debug("bean not right type: ${bean}, class: ${bean.class.name}, assignable: ${groovyPluginType.isAssignableFrom(bean.class)}")
                }
            } catch (NoSuchBeanDefinitionException e) {
                log.error("No such bean: ${v}")
            }catch (BeanNotOfRequiredTypeException e) {
                log.error("Not a valid NotificationPlugin: ${v}")
            }
        }
        if(rundeckServerServiceProviderLoader && service){
            service.listProviders().each { ProviderIdent ident ->
                def instance = rundeckServerServiceProviderLoader.loadProvider(service, ident.providerName)
                if(!groovyPluginType.isAssignableFrom(instance.class)){
                    return
                }
                if (!list[ident.providerName]) {
                    list[ident.providerName] = [:]
                }
                list[ident.providerName] += [
                        instance: instance,
                        description: [
                                name:ident.providerName
                        ]
                ]
            }
            service.listDescriptions().each {Description d->
                if (!list[d.name]) {
                    list[d.name] = [:]
                }
                list[d.name].description=d
            }
        }

        list
    }
}
