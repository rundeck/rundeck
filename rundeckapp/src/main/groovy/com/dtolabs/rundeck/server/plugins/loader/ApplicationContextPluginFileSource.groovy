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

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

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

    /**
     * The archive this application was launched from, opened once and shared, or null when it was
     * not launched from one.
     *
     * Every applicationContext.getResource() for a packaged war resolves through ServletContext,
     * and on Jetty 12.1 that mounts a fresh zipfs filesystem over the whole archive and never
     * closes it. With 87 embedded plugins read three times each that is 264 live mounts, every one
     * of them retaining the archive's central directory: roughly 950MiB of unreclaimable heap, and
     * 264 central-directory parses before the first request is served. One handle does the same
     * work. Jetty 12.0 under Boot 3 did not behave this way.
     */
    private volatile ZipFile enclosingArchive
    private volatile boolean enclosingArchiveResolved

    /**
     * @return the shared archive handle, resolving it on first use, or null when running exploded
     */
    private ZipFile sharedArchive() {
        if (!enclosingArchiveResolved) {
            synchronized (this) {
                if (!enclosingArchiveResolved) {
                    enclosingArchive = resolveEnclosingArchive()
                    enclosingArchiveResolved = true
                }
            }
        }
        return enclosingArchive
    }

    /**
     * Locate the war or jar holding this class.
     *
     * @return an open handle on it, or null when this class is not packaged in an archive, which is
     *         the case in dev mode and for exploded deployments
     */
    private ZipFile resolveEnclosingArchive() {
        try {
            File archive = archiveFromCodeSource()
            if (null == archive) {
                archive = archiveFromClassPath()
            }
            return archive?.isFile() ? new ZipFile(archive) : null
        } catch (Exception ignored) {
            //any failure here simply means the ApplicationContext is used, as it was before
            return null
        }
    }

    /**
     * @return the archive named by this class's code source, or null when it does not name one
     */
    private File archiveFromCodeSource() {
        def location = getClass().protectionDomain?.codeSource?.location
        if (null == location) {
            return null
        }
        //Spring Boot nests classes inside the war, so this is jar:nested:/path/x.war/!WEB-INF/classes/!/
        //on Boot 3.2 and later, and jar:file:/path/x.war!/WEB-INF/classes!/ before that
        String path = location.toString()
        if (path.startsWith('jar:')) {
            path = path.substring('jar:'.length())
        }
        if (path.startsWith('nested:')) {
            path = path.substring('nested:'.length())
        }
        int marker = path.indexOf('/!')
        if (marker < 0) {
            marker = path.indexOf('!')
        }
        if (marker > -1) {
            path = path.substring(0, marker)
        }
        if (path.startsWith('file:')) {
            return new File(URI.create(path))
        }
        return path.startsWith('/') ? new File(path) : null
    }

    /**
     * Fallback for launchers whose code source is not a usable path: java -jar leaves the archive as
     * the sole class path entry, relative to the working directory.
     *
     * @return the archive named by the class path, or null when it names anything else
     */
    private File archiveFromClassPath() {
        String classPath = System.getProperty('java.class.path')
        if (!classPath || classPath.contains(File.pathSeparator)) {
            return null
        }
        if (!classPath.endsWith('.war') && !classPath.endsWith('.jar')) {
            return null
        }
        File named = new File(classPath)
        return named.isAbsolute() ? named : new File(System.getProperty('user.dir') ?: '.', classPath)
    }

    /**
     * @param filePath application-relative path, e.g. /WEB-INF/rundeck/plugins/x.properties
     * @return the matching entry of the shared archive, or null when there is no archive or no
     *         such entry
     */
    private ZipEntry archiveEntry(String filePath) {
        ZipFile archive = sharedArchive()
        if (null == archive) {
            return null
        }
        return archive.getEntry(filePath.startsWith('/') ? filePath.substring(1) : filePath)
    }

    /**
     * @return properties read from the shared archive, or null when the entry is not served from one
     */
    private Properties loadPropertiesFromArchive(String filePath) throws IOException {
        ZipEntry entry = archiveEntry(filePath)
        if (null == entry) {
            return null
        }
        Properties properties = new Properties()
        sharedArchive().getInputStream(entry).withCloseable { properties.load(it) }
        return properties
    }

    private Properties loadProperties(String filePath) throws IOException{
        Properties fromArchive = loadPropertiesFromArchive(filePath)
        if (null != fromArchive) {
            return fromArchive
        }
        if (null != sharedArchive()) {
            //packaged, and the archive has no such entry: same empty result as a missing Resource
            return new Properties()
        }
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
                String propertiesPath = basePath + pluginFileName + ".properties"
                Properties props=list
                String prefix= FILE_PREFIX + pluginFileName + '.'
                Properties pluginProps = loadPropertiesFromArchive(propertiesPath)
                if (null == pluginProps && null == sharedArchive()) {
                    def pluginResProps = applicationContext.getResource(propertiesPath)
                    if (pluginResProps.exists()) {
                        //load properties from the resource already resolved above
                        pluginProps = loadProperties(pluginResProps)
                    }
                }
                if (null != pluginProps) {
                    props = pluginProps
                    prefix=null
                }
                result.add(new PropertiesManifest(prefix, props))
            }
        }
        return result
    }

    @Override
    PluginFileContents getContentsForPlugin(PluginFileManifest manifest) {
        String pluginPath = basePath + manifest.fileName
        if (null != sharedArchive()) {
            ZipEntry entry = archiveEntry(pluginPath)
            return null != entry ? new ArchiveEntryContents(sharedArchive(), entry) : null
        }
        def pluginRes = applicationContext.getResource(pluginPath)
        if (!pluginRes.exists()) {
            return null
        }
        return new ResourceFileContents(pluginRes)
    }

    /**
     * Plugin contents read from the shared archive handle rather than from a per-file mount.
     */
    private static class ArchiveEntryContents implements PluginFileContents {
        private final ZipFile archive
        private final ZipEntry entry

        ArchiveEntryContents(ZipFile archive, ZipEntry entry) {
            this.archive = archive
            this.entry = entry
        }

        @Override
        InputStream getContents() throws IOException {
            return archive.getInputStream(entry)
        }
    }
}
