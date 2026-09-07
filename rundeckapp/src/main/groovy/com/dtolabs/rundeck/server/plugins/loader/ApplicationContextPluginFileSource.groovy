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
import org.springframework.core.io.Resource
import org.springframework.context.ApplicationContextAware

/**
 * ApplicationContextPluginFileSource reads a list of plugin files embedded in the application resources
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-07-18
 */
class ApplicationContextPluginFileSource implements PluginFileSource, ApplicationContextAware {
    public static final String PLUGIN_FILE_LIST = 'pluginFileList'
    public static final String MANIFEST_PROPERTIES_FILE = 'manifest.properties'
    public static final String FILE_PREFIX = 'pluginFile.'
    ApplicationContext applicationContext
    String basePath
    Properties pluginsProperties

    ApplicationContextPluginFileSource(String basePath) {
        this.basePath = basePath
    }

    ApplicationContextPluginFileSource(ApplicationContext applicationContext, String basePath) {
        this.applicationContext = applicationContext
        this.basePath = basePath
    }

    private Properties loadProperties(String filePath) throws IOException{
        return loadProperties(applicationContext.getResource(filePath))
    }

    /**
     * Loads from an already-resolved resource, so callers that have tested exists() do not pay a
     * second resolution for the same path.
     */
    private static Properties loadProperties(Resource resource) throws IOException{
        Properties pluginsProperties = new Properties()
        if(resource.exists()){
            resource.getInputStream().withCloseable { pluginsProperties.load(it) }
        }
        return pluginsProperties
    }
    private Properties getPluginsList() throws IOException{
        if(null==pluginsProperties){
            pluginsProperties = loadProperties(basePath + MANIFEST_PROPERTIES_FILE)
        }
        return pluginsProperties;
    }
    /**
     * Manifests of the embedded plugins, which are fixed for the life of the deployment.
     *
     * Building this list resolves two application-context resources per embedded plugin, and under
     * Spring Boot those resolutions walk the nested jars of the war. Sampling /api/57/plugin/list
     * under load put 40% of request time in here on Grails 8 against 2% on Grails 7, so the list is
     * built once and reused. Callers still receive their own copy, as they did when every call
     * returned a fresh list.
     */
    private volatile List<PluginFileManifest> manifests

    @Override
    List<PluginFileManifest> listManifests() {
        if (null == manifests) {
            manifests = buildManifests().asImmutable()
        }
        return new ArrayList<PluginFileManifest>(manifests)
    }

    private List<PluginFileManifest> buildManifests() throws IOException {
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
                    //load properties from the resource already resolved above
                    props = loadProperties(pluginResProps)
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
