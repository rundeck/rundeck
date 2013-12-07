package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger

import java.lang.reflect.Constructor

/**
 * Builder for rundeck plugin DSL support
 * User: greg
 * Date: 4/16/13
 * Time: 3:55 PM
 */
abstract class ScriptPluginBuilder implements GroovyObject, PluginBuilder{
    static Logger logger = Logger.getLogger(ScriptPluginBuilder)
    private Map pluginAttributes=[:]
    /**
     * internal builder for the plugin Description
     */
    def DescriptionBuilder descriptionBuilder
    /**
     * Registry of valid Rundeck plugin classes and associated groovy DSL builders
     */
    private static Map<Class,Class<? extends ScriptPluginBuilder>> clazzBuilderRegistry=[
            (NotificationPlugin):ScriptNotificationPluginBuilder,
            (StreamingLogWriterPlugin):StreamingLogWriterPluginBuilder,
            (StreamingLogReaderPlugin):StreamingLogReaderPluginBuilder,
            (ExecutionFileStoragePlugin): ExecutionFileStoragePluginBuilder,
    ]
    private static Map<Class,String> disabledPluginClasses=[
            (LogFileStoragePlugin):"The LogFileStoragePlugin interface is no longer supported, use a plugin that" +
                    " implements ExecutionFileStoragePlugin"
    ]
    /**
     * Return a ScriptPluginBuilder for the given plugin class and plugin name
     * @param clazz
     * @param name
     * @return
     */
    public static ScriptPluginBuilder forPluginClass(Class clazz, String name){
        Class subClazz = clazzBuilderRegistry[clazz]
        if(subClazz){
            def Constructor constructor = subClazz.getDeclaredConstructor(String)
            return constructor.newInstance(name)
        }else if(disabledPluginClasses[clazz]){
            logger.error("Plugin '${name}' not loaded: "+disabledPluginClasses[clazz])
            return null
        }else{
            throw new IllegalArgumentException("Unsupported plugin type: ${clazz.name}")
        }
    }
    /**
     * Create a builder for a plugin with the specified plugin/provider name.
     * @param name the name of the plugin/provider
     */
    ScriptPluginBuilder(String name) {
        descriptionBuilder=DescriptionBuilder.builder()
        descriptionBuilder.name(name)
    }

    /**
     * allow title, description properties to be set within the DSL
     * @param property
     * @param newValue
     */
//    @Override
    def propertyMissing(String property, Object newValue) {
        if(property in ['title','description'] && newValue instanceof String){
            pluginAttributes[property]=newValue
            if(property=='title'){
                descriptionBuilder.title((String)newValue)
            }else{
                descriptionBuilder.description((String) newValue)
            }
        }else{
            super.setProperty(property, newValue)
        }
    }

    /**
     * Configuration dsl declaration, uses the ScriptPluginConfigBuilder class
     * @param clos
     */
    void configuration(Closure clos){
        def builder = new ScriptPluginConfigBuilder(descriptionBuilder)
        clos.delegate = builder
        clos.resolveStrategy=Closure.DELEGATE_ONLY
        clos.call(builder)
    }

}
