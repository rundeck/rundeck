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
 * ApplicationContextPluginFileSource reads a list of plugin files embedded in the application resources
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-07-18
 */
class ApplicationContextPluginFileSource implements PluginFileSource{
    public static final String PLUGIN_FILE_LIST = 'pluginFileList'
    public static final String MANIFEST_PROPERTIES_FILE = 'manifest.properties'
    public static final String FILE_PREFIX = 'pluginFile.'
    ApplicationContext applicationContext
    String basePath
    Properties pluginsProperties

    ApplicationContextPluginFileSource(ApplicationContext applicationContext, String basePath) {
        this.applicationContext = applicationContext
        this.basePath = basePath
    }

    private Properties loadProperties(String filePath) throws IOException{
        Properties pluginsProperties = new Properties()
        def manifestProps= applicationContext.getResource(filePath)
        if(manifestProps.exists()){
            pluginsProperties.load(manifestProps.getInputStream())
        }
        return pluginsProperties
    }
    private Properties getPluginsList() throws IOException{
        if(null==pluginsProperties){
            pluginsProperties = loadProperties(basePath + MANIFEST_PROPERTIES_FILE)
        }
        return pluginsProperties;
    }
    @Override
    List<PluginFileManifest> listManifests() {
        def result = new ArrayList<PluginFileManifest>()
        def list = getPluginsList()
        def pluginListStr = list.getProperty(PLUGIN_FILE_LIST)
        if (pluginListStr) {
            def split = pluginListStr.split(/, */)
            split?.each { pluginFileName ->
                def pluginResProps = applicationContext.getResource(basePath + pluginFileName+".properties")
                Properties props=list
                String prefix= FILE_PREFIX + pluginFileName + '.'
                if(pluginResProps.exists()) {
                    //load properties
                    props = loadProperties(basePath + pluginFileName + ".properties")
                    prefix=null
                }
                result.add(new PropertiesManifest(prefix, props))
            }
        }
        return result
    }

    @Override
    PluginFileContents getContentsForPlugin(PluginFileManifest manifest) {
        def pluginRes = applicationContext.getResource(basePath + manifest.fileName)
        if (!pluginRes.exists()) {
            return null
        }
        return new ResourceFileContents(pluginRes)
    }
}
