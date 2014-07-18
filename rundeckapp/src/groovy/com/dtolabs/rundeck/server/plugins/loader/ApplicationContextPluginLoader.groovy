/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.server.plugins.loader

import org.springframework.context.ApplicationContext

/**
 * ApplicationContextPluginLoader is ...
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-07-18
 */
class ApplicationContextPluginLoader implements PluginManifestSource{
    public static final String PLUGIN_FILE_LIST = 'pluginFileList'
    public static final String MANIFEST_PROPERTIES_FILE = 'manifest.properties'
    public static final String FILE_PREFIX = 'pluginFile.'
    ApplicationContext applicationContext
    String basePath
    Properties pluginsProperties

    ApplicationContextPluginLoader(ApplicationContext applicationContext, String basePath) {
        this.applicationContext = applicationContext
        this.basePath = basePath
    }

    private Properties loadProperties() throws IOException{
        Properties pluginsProperties = new Properties()
        def manifestProps= applicationContext.getResource(basePath+MANIFEST_PROPERTIES_FILE)
        if(manifestProps.exists()){
            pluginsProperties.load(manifestProps.getInputStream())
        }
        return pluginsProperties
    }
    private Properties getPluginsList() throws IOException{
        if(null==pluginsProperties){
            pluginsProperties = loadProperties()
        }
        return pluginsProperties;
    }
    @Override
    List<PluginManifest> listManifests() {
        def result = new ArrayList<PluginManifest>()
        def list = getPluginsList()
        def pluginListStr = list.getProperty(PLUGIN_FILE_LIST)
        if (pluginListStr) {
            def split = pluginListStr.split(/, */)
            split?.each { pluginFileName ->
                result.add(loadManifestForFile(list,pluginFileName))
            }
        }
        return result
    }

    PluginManifest loadManifestForFile(Properties properties, String pluginFileName) {
        return new PropertiesManifest(FILE_PREFIX + pluginFileName + '.', properties)
    }


    @Override
    PluginLoader getLoaderForPlugin(PluginManifest manifest) {
        def pluginRes = applicationContext.getResource(basePath + manifest.fileName)
        if (pluginRes.exists()) {
            return new ResourceLoader(pluginRes)
        } else {
            return null
        }
    }
}
