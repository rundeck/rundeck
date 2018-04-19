/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.server.plugins

import com.dtolabs.rundeck.server.plugins.loader.ApplicationContextPluginFileSource
import com.dtolabs.rundeck.server.plugins.loader.PluginFileManifest
import com.dtolabs.rundeck.server.plugins.loader.PluginFileSource
import com.dtolabs.utils.Streams
import grails.spring.BeanBuilder
import org.apache.log4j.Logger
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowire
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * @author greg
 * @since 3/7/17
 */
class RundeckEmbeddedPluginExtractor implements ApplicationContextAware, InitializingBean {
    public static Logger log = Logger.getLogger(RundeckEmbeddedPluginExtractor.class.name)

    ApplicationContext applicationContext
    File pluginTargetDir
    RundeckPluginRegistry rundeckPluginRegistry
    @Autowired
    Collection<PluginFileSource> pluginFileSources = []

    RundeckEmbeddedPluginExtractor() {
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if (!pluginFileSources) {
            pluginFileSources = [
                    new ApplicationContextPluginFileSource(applicationContext, '/WEB-INF/rundeck/plugins/')
            ]
        }
    }

    /**
     * Install all the embedded plugins, will not overwrite existing plugin files with the same name
     * @param grailsApplication
     * @return
     */
    def extractEmbeddedPlugins() {
        def result = [success: true, logs: [], errors: []]
        for (PluginFileSource loader : pluginFileSources) {

            def pluginList
            try {
                pluginList = loader.listManifests()
            } catch (IOException e) {
                log.error("Could not list plugins from loader: ${e}", e)
                result.errors << "Could not list plugins from loader: ${e}"
                result.success = false
                continue
            }

            pluginList.each { PluginFileManifest pluginmf ->
                try {
                    if (installPlugin(pluginTargetDir, loader, pluginmf, false)) {
                        result.logs << "Extracted bundled plugin ${pluginmf.fileName}"
                    } else {
                        result.logs << "Skipped existing plugin: ${pluginmf.fileName}"
                    }
                } catch (Exception e) {
                    log.error("Failed extracting bundled plugin ${pluginmf}", e)
                    result.success = false
                    result.errors << "Failed extracting bundled plugin ${pluginmf}: ${e}"
                }
                if (pluginmf.fileName.endsWith(".groovy")) {
                    //initialize groovy plugin as spring bean if it does not exist
                    def bean = loadGroovyScriptPluginBean(new File(pluginTargetDir, pluginmf.fileName))
                    result.logs << "Loaded groovy plugin ${pluginmf.fileName} as ${bean.class} ${bean}"
                }
            }
        }

        return result
    }

    def loadGroovyScriptPluginBean(File file) {
        String beanName = file.name.replace('.groovy', '')
        def testBean
        try {
            testBean = applicationContext.getBean(beanName)
            log.debug("Groovy plugin bean already exists in main context: $beanName: $testBean")
        } catch (NoSuchBeanDefinitionException e) {
            log.debug("Bean not found: $beanName")
        }
        if (testBean) {
            return testBean
        }

        def builder = new BeanBuilder(applicationContext)
        builder.beans {
            xmlns lang: 'http://www.springframework.org/schema/lang'
            lang.groovy(id: beanName, 'script-source': file.toURI().toString(), 'customizer-ref': 'pluginCustomizer')
        }

        def context = builder.createApplicationContext()
        def result = context.getBean(beanName)

        log.debug("Loaded groovy plugin bean; type: ${result.class} ${result}")
        rundeckPluginRegistry.registerDynamicPluginBean(beanName, context)

        return result
    }

    /**
     * Install a plugin from a source
     * @param pluginsDir destination directory
     * @param loader source
     * @param pluginmf plugin to install
     * @param overwrite true to overwrite existing file
     * @return true if the plugin file was written, false otherwise
     * @throws IOException
     */
    public boolean installPlugin(
            File pluginsDir, PluginFileSource loader, PluginFileManifest pluginmf,
            boolean overwrite
    ) throws IOException
    {
        File destFile = new File(pluginsDir, pluginmf.fileName)
        if (!overwrite && destFile.exists()) {
            return false
        }
        def pload = loader.getContentsForPlugin(pluginmf)
        if (!pload) {
            throw new Exception("Failed to load plugin: ${pluginmf}")
        }
        destFile.withOutputStream { os ->
            Streams.copyStream(pload.contents, os)
        }

        return true
    }
}
