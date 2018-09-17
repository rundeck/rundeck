/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogReaderPlugin
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import com.dtolabs.rundeck.plugins.logs.ContentConverterPlugin
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.server.plugins.services.PluginBuilder
import org.apache.log4j.Logger

import java.lang.reflect.Constructor
import java.text.ParseException
import java.text.SimpleDateFormat

/**
 * Builder for rundeck plugin DSL support
 * User: greg
 * Date: 4/16/13
 * Time: 3:55 PM
 */
abstract class ScriptPluginBuilder implements GroovyObject, PluginBuilder, PluginMetadata {
    static Logger logger = Logger.getLogger(ScriptPluginBuilder)
    private Map pluginAttributes = [:]
    /**
     * internal builder for the plugin Description
     */
    def DescriptionBuilder descriptionBuilder

    private Class pluginClass
    /**
     * Registry of valid Rundeck plugin classes and associated groovy DSL builders
     */
    private static Map<Class, Class<? extends ScriptPluginBuilder>> clazzBuilderRegistry = [
            (NotificationPlugin)        : ScriptNotificationPluginBuilder,
            (StreamingLogWriterPlugin)  : StreamingLogWriterPluginBuilder,
            (StreamingLogReaderPlugin)  : StreamingLogReaderPluginBuilder,
            (ExecutionFileStoragePlugin): ExecutionFileStoragePluginBuilder,
            (LogFilterPlugin)           : LogFilterPluginBuilder,
            (ContentConverterPlugin)    : ContentConverterPluginBuilder,
    ]
    private static Map<Class, String> disabledPluginClasses = [
            (LogFileStoragePlugin): "The LogFileStoragePlugin interface is no longer supported, use a plugin that" +
                    " implements ExecutionFileStoragePlugin"
    ]
    /**
     * Return a ScriptPluginBuilder for the given plugin class and plugin name
     * @param clazz
     * @param name
     * @return
     */
    public static ScriptPluginBuilder forPluginClass(Class clazz, String name) {
        Class subClazz = clazzBuilderRegistry[clazz]
        if (subClazz) {
            def Constructor constructor = subClazz.getDeclaredConstructor(Class,String)
            return constructor.newInstance(clazz,name)
        } else if (disabledPluginClasses[clazz]) {
            logger.error("Plugin '${name}' not loaded: " + disabledPluginClasses[clazz])
            return null
        } else {
            throw new IllegalArgumentException("Unsupported plugin type: ${clazz.name}")
        }
    }
    /**
     * Create a builder for a plugin with the specified plugin/provider name.
     * @param name the name of the plugin/provider
     */
    ScriptPluginBuilder(Class clazz,String name) {
        descriptionBuilder = DescriptionBuilder.builder()
        descriptionBuilder.name(name)
        this.pluginClass=clazz
        this.filename = name + '.groovy'
    }
    private String filename

    Class getPluginClass() {
        return pluginClass
    }
    /**
     * allow title, description properties to be set within the DSL
     * @param property
     * @param newValue
     */
    def propertyMissing(String property, Object newValue) {
        println "set prop: $property to $newValue"
        if (property in ['title', 'description', 'version', 'url', 'author', 'date','rundeckPluginVersion','rundeckVersion','license','thirdPartyDependencies','sourceLink'] && newValue instanceof String) {
            pluginAttributes[property] = newValue
            if (property == 'title') {
                pluginAttributes['id'] = PluginUtils.generateShaIdFromName((String) newValue)
                descriptionBuilder.title((String) newValue)
            } else if (property == 'description') {
                descriptionBuilder.description((String) newValue)
            }
        } else if(property == "pluginTags") {
            if(newValue instanceof Collection) {
                pluginAttributes['tags'] = newValue
            } else {
                logger.error("Tags property of plugin script: ${filename} must be a list. Tags property ignored.")
            }
        } else {
            super.setProperty(property, newValue)
        }
    }

    @Override
    String getFilename() {
        filename
    }

    @Override
    File getFile() {
        return null
    }

    @Override
    String getPluginAuthor() {
        return pluginAttributes['author']
    }

    @Override
    String getPluginFileVersion() {
        return pluginAttributes['version']
    }

    @Override
    String getPluginVersion() {
        return pluginAttributes['rundeckPluginVersion']
    }

    @Override
    String getPluginUrl() {
        return pluginAttributes['url']
    }

    @Override
    Date getPluginDate() {
        def val = pluginAttributes['date']
        if (val instanceof Date) {
            return val
        } else if (val instanceof String) {
            try {
                (new SimpleDateFormat().parse(val))
            } catch (ParseException e) {
            }
        }
        return null
    }

    private Date dateLoaded = new Date()

    @Override
    Date getDateLoaded() {
        return dateLoaded
    }
    /**
     * Configuration dsl declaration, uses the ScriptPluginConfigBuilder class
     * @param clos
     */
    void configuration(Closure clos) {
        def builder = new ScriptPluginConfigBuilder(descriptionBuilder)
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_ONLY
        clos.call(builder)
    }

    @Override
    String getPluginName() {
        return pluginAttributes['title']
    }

    @Override
    String getPluginDescription() {
        return descriptionBuilder.build().description
    }

    @Override
    String getPluginId() {
        return pluginAttributes['id']
    }

    @Override
    String getRundeckCompatibilityVersion() {
        return pluginAttributes['rundeckVersion']
    }

    @Override
    String getTargetHostCompatibility() {
        return "all"
    }

    @Override
    List<String> getTags() {
        return pluginAttributes['tags']
    }

    @Override
    String getPluginLicense() {
        return pluginAttributes['license']
    }

    @Override
    String getPluginThirdPartyDependencies() {
        return pluginAttributes['thirdPartyDependencies']
    }

    @Override
    String getPluginSourceLink() {
        return pluginAttributes['sourceLink']
    }

    @Override
    String getPluginType() {
        return "groovy"
    }

}
