/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* ScriptFileProviderLoader.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/13/11 10:07 AM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.metadata.ProviderDef;
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.ZipUtil;
import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ScriptPluginProviderLoader can load a provider instance for a service from a script plugin zip file.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginProviderLoader implements ProviderLoader, FileCache.Expireable {

    private static final Logger log = Logger.getLogger(ScriptPluginProviderLoader.class.getName());
    public static final String VERSION_1_0 = "1.0";
    public static final String VERSION_1_1 = "1.1";
    public static final List<String> SUPPORTED_PLUGIN_VERSIONS;
    static {
        SUPPORTED_PLUGIN_VERSIONS = Collections.unmodifiableList(Arrays.asList(
                VERSION_1_0,
                VERSION_1_1
        ));
    }
    private final File file;
    final File cachedir;
    /**
     * Dir of expanded zip contents
     */
    private File fileExpandedDir;
    /**
     * Metadata from the plugin.yaml file
     */
    private PluginMeta metadata;
    /**
     * cache of ident to scriptplugin def mapping
     */
    private Map<ProviderIdent, ScriptPluginProvider> pluginProviderDefs =
        new HashMap<ProviderIdent, ScriptPluginProvider>();

    public ScriptPluginProviderLoader(final File file, final File cachedir) {
        this.file = file;
        this.cachedir = cachedir;
    }

    /**
     * Load a provider instance for the service by name
     */
    public synchronized <T> T load(final PluggableService<T> service, final String providerName) throws
        ProviderLoaderException {
        if (!service.isScriptPluggable()) {
            return null;
        }
        final ProviderIdent ident = new ProviderIdent(service.getName(), providerName);

        if (null == pluginProviderDefs.get(ident)) {
            //look for plugin def
            final PluginMeta pluginMeta;
            try {
                pluginMeta = getPluginMeta();
            } catch (IOException e) {
                throw new ProviderLoaderException(e, service.getName(), providerName);
            }
            if (null == pluginMeta) {
                throw new ProviderLoaderException("Unable to load plugin metadata for file: " + file, service.getName(),
                    providerName);
            }
            for (final ProviderDef pluginDef : pluginMeta.getPluginDefs()) {
                if (matchesProvider(ident, pluginDef)) {
                    final ScriptPluginProvider provider;
                    try {
                        provider = getPlugin(pluginMeta, file, pluginDef, ident);
                    } catch (PluginException e) {
                        throw new ProviderLoaderException(e, service.getName(), providerName);
                    }
                    pluginProviderDefs.put(ident, provider);
                    break;
                }
            }
        }
        final ScriptPluginProvider scriptPluginProvider = pluginProviderDefs.get(ident);

        if (null != scriptPluginProvider) {
            try {
                return service.createScriptProviderInstance(scriptPluginProvider);
            } catch (PluginException e) {
                throw new ProviderLoaderException(e, service.getName(), providerName);
            }
        }
        return null;
    }


    /**
     * Get the plugin metadata, loading from the file if necessary
     *
     * @return loaded metadata or null if not found
     *
     * @throws IOException if an error occurs trying to load from the file
     */
    private PluginMeta getPluginMeta() throws IOException {
        if (null != metadata) {
            return metadata;
        }
        metadata = loadMeta(file);
        return metadata;
    }

    /**
     * Get the ScriptPluginProvider definition from the file for the given provider def and ident
     */
    private ScriptPluginProvider getPlugin(
            final PluginMeta pluginMeta,
            final File file,
            final ProviderDef pluginDef,
            final ProviderIdent ident
    ) throws
            ProviderLoaderException, PluginException
    {
        if (null == fileExpandedDir) {
            final File dir;
            try {
                dir = expandScriptPlugin(file);
            } catch (IOException e) {
                throw new ProviderLoaderException(e, ident.getService(), ident.getProviderName());
            }
            fileExpandedDir = dir;
            final File script = new File(fileExpandedDir, pluginDef.getScriptFile());
            //set executable bit for script-file of the provider
            try {
                ScriptfileUtils.setExecutePermissions(script);
            } catch (IOException e) {
                log.warn("Unable to set executable bit for script file: " + script + ": " + e.getMessage());
            }
            debug("expanded plugin dir! " + fileExpandedDir);
        } else {
            debug("expanded plugin dir: " + fileExpandedDir);
        }

        final File script = new File(fileExpandedDir, pluginDef.getScriptFile());
        if (!script.exists() || !script.isFile()) {
            throw new PluginException("Script file was not found: " + script.getAbsolutePath());
        }
        return new ScriptPluginProviderImpl(pluginMeta, pluginDef, file, fileExpandedDir);
    }

    /**
     * Return true if the ident matches the provider def metadata
     */
    private boolean matchesProvider(final ProviderIdent ident, final ProviderDef pluginDef) {
        return ident.getService().equals(pluginDef.getService()) && ident.getProviderName().equals(pluginDef.getName());
    }

    /**
     * Return true if the plugin file can loade a provider for the ident
     */
    public synchronized boolean isLoaderFor(final ProviderIdent ident) {

        final PluginMeta pluginMeta;
        try {
            pluginMeta = getPluginMeta();
        } catch (IOException e) {
            log.warn("Unable to load file meta: " + e.getMessage());
            return false;
        }
        if (null == pluginMeta) {
            return false;
        }
        for (final ProviderDef pluginDef : pluginMeta.getPluginDefs()) {
            if (matchesProvider(ident, pluginDef)) {
                return true;
            }
        }
        return false;
    }

    public List<ProviderIdent> listProviders() {
        final ArrayList<ProviderIdent> providerIdents = new ArrayList<ProviderIdent>();
        PluginMeta pluginMeta=null;
        try {
            pluginMeta = getPluginMeta();
        } catch (IOException e) {
            debug("Unable to load file meta: " + e.getMessage());
        }
        if (null == pluginMeta) {
            return providerIdents;
        }
        for (final ProviderDef pluginDef : pluginMeta.getPluginDefs()) {
            providerIdents.add(new ProviderIdent(pluginDef.getService(), pluginDef.getName()));
        }
        return providerIdents;
    }


    /**
     * Get plugin metadatat from a zip file
     */
    static PluginMeta loadMeta(final File jar) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(jar);
        try{
            final ZipInputStream zipinput = new ZipInputStream(fileInputStream);
            final PluginMeta metadata = ScriptPluginProviderLoader.loadMeta(jar, zipinput);
            return metadata;
        }finally {
            fileInputStream.close();
        }
    }

    /**
     * Load plugin metadata for a file and zip inputstream
     * @param jar the file
     * @param zipinput zip input stream
     * @return loaded metadata, or null if it is invalid or not found
     */
    static PluginMeta loadMeta(final File jar, final ZipInputStream zipinput) throws IOException {
        final String basename = basename(jar);
        PluginMeta metadata = null;
        boolean topfound = false;
        boolean found = false;
        boolean dirfound = false;
        ZipEntry nextEntry = zipinput.getNextEntry();
        while (null != nextEntry) {
            if (!topfound && nextEntry.getName().startsWith(basename + "/")) {
                topfound = true;
            }
            if (!dirfound && (nextEntry.getName().startsWith(basename + "/contents/")
                              || nextEntry.isDirectory() && nextEntry.getName().equals(
                basename + "/contents"))) {

//                debug("Found contents dir: " + nextEntry.getName());
                dirfound = true;
            }
            if (!found && !nextEntry.isDirectory() && nextEntry.getName().equals(basename + "/plugin.yaml")) {
//                debug("Found metadata: " + nextEntry.getName());
                try {
                    metadata = loadMetadataYaml(zipinput);
                    found = true;
                } catch (Throwable e) {
                    log.error("Error parsing metadata file plugin.yaml: " + e.getMessage(), e);
                }
            }
            if (dirfound && found) {
                break;
            }
            nextEntry = zipinput.getNextEntry();
        }
        if (!topfound) {
            log.error("Plugin not loaded: Found no " + basename + "/ dir within file: " + jar.getAbsolutePath());
        }
        if (!found) {
            log.error("Plugin not loaded: Found no " + basename + "/plugin.yaml within: " + jar.getAbsolutePath());
        }
        if (!dirfound) {
            log.error("Plugin not loaded: Found no " + basename + "/contents dir within: " + jar.getAbsolutePath());
        }
        if (found && dirfound) {
            return metadata;
        }
        return null;
    }

    /**
     * return loaded yaml plugin metadata from the stream
     */
    static PluginMeta loadMetadataYaml(final InputStream stream) {
        final Yaml yaml = new Yaml();

        return yaml.loadAs(stream, PluginMeta.class);
    }

    /**
     * Return true if loaded metadata about the plugin file is valid.
     */
    static boolean validatePluginMeta(final PluginMeta pluginList, final File file) {
        boolean valid = true;
        if (null == pluginList.getName()) {
            log.error("name not found in metadata: " + file.getAbsolutePath());
            valid = false;
        }
        if (null == pluginList.getVersion()) {
            log.error("version not found in metadata: " + file.getAbsolutePath());
            valid = false;
        }
        if (null == pluginList.getRundeckPluginVersion()) {
            log.error("rundeckPluginVersion not found in metadata: " + file.getAbsolutePath());
            valid = false;
        } else if (!SUPPORTED_PLUGIN_VERSIONS.contains(pluginList.getRundeckPluginVersion())) {
            log.error("rundeckPluginVersion: " + pluginList.getRundeckPluginVersion() + " is not supported: " + file
                    .getAbsolutePath());
            valid = false;
        }
        final List<ProviderDef> pluginDefs = pluginList.getPluginDefs();
        for (final ProviderDef pluginDef : pluginDefs) {
            try {
                validateProviderDef(pluginDef);
            } catch (PluginException e) {
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Expand zip file into plugin cache dir
     *
     * @param file zip file
     *
     * @return cache dir for the contents of the plugin zip
     */
    private File expandScriptPlugin(final File file) throws IOException {
        if (!cachedir.exists()) {
            if (!cachedir.mkdirs()) {
                log.warn("Unable to create cache dir: " + cachedir.getAbsolutePath());
            }
        }
        final File jardir = getFileCacheDir();
        if (!jardir.exists()) {
            if (!jardir.mkdir()) {
                log.warn("Unable to create cache dir for plugin: " + jardir.getAbsolutePath());
            }
        }
        final String prefix = getFileBasename() + "/contents";

        debug("Expand zip " + file.getAbsolutePath() + " to dir: " + jardir + ", prefix: " + prefix);
        ZipUtil.extractZip(file.getAbsolutePath(), jardir, prefix, prefix + "/");

        return jardir;
    }

    /**
     * Remove any cache dir for the file
     */
    private synchronized boolean removeScriptPluginCache() {
        if (null != fileExpandedDir && fileExpandedDir.exists()) {
            debug("removeScriptPluginCache: " + fileExpandedDir);
            return FileUtils.deleteDir(fileExpandedDir);
        }
        return true;
    }

    /**
     * Basename of the file
     */
    String getFileBasename() {
        return basename(file);
    }

    /**
     * Get basename of a file
     */
    private static String basename(final File file) {
        final String name = file.getName();
        return name.substring(0, name.lastIndexOf("."));
    }

    /**
     * Get the cache dir for use for this file
     */
    File getFileCacheDir() {
        return new File(cachedir, getFileBasename());
    }


    /**
     * Validate provider def
     */
    private static void validateProviderDef(final ProviderDef pluginDef) throws PluginException {

        if (null == pluginDef.getPluginType() || "".equals(pluginDef.getPluginType())) {
            throw new PluginException("Script plugin missing plugin-type");
        }
        if ("script".equals(pluginDef.getPluginType())) {
            validateScriptProviderDef(pluginDef);
        } else {
            throw new PluginException("plugin missing has invalid plugin-type: " + pluginDef.getPluginType());
        }
    }

    /**
     * Validate script provider def
     */
    private static void validateScriptProviderDef(final ProviderDef pluginDef) throws PluginException {
        if (null == pluginDef.getName() || "".equals(pluginDef.getName())) {
            throw new PluginException("Script plugin missing name");
        }
        if (null == pluginDef.getService() || "".equals(pluginDef.getService())) {
            throw new PluginException("Script plugin missing service");
        }
        if (null == pluginDef.getScriptFile() || "".equals(pluginDef.getScriptFile())) {
            throw new PluginException("Script plugin missing script-file");
        }

        //make sure service is pluggable service and is script pluggable
        /* final FrameworkSupportService service = framework.getService(pluginDef.getService());
        if (!(service instanceof PluggableService)) {
            throw new PluginException(
                "Service '" + pluginDef.getService() + "' specified for script plugin '" + pluginDef.getName()
                + "' is not valid: unsupported");
        }
        final PluggableService pservice = (PluggableService) service;
        if (!pservice.isScriptPluggable()) {
            throw new PluginException(
                "Service '" + pluginDef.getService() + "' specified for script plugin '" + pluginDef.getName()
                + "' is not valid: unsupported");
        }*/
    }

    private static void debug(final String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    /**
     * Expire the loader cache item
     */
    public void expire() {
        removeScriptPluginCache();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ScriptPluginProviderLoader that = (ScriptPluginProviderLoader) o;

        if (cachedir != null ? !cachedir.equals(that.cachedir) : that.cachedir != null) {
            return false;
        }
        if (file != null ? !file.equals(that.file) : that.file != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = file != null ? file.hashCode() : 0;
        result = 31 * result + (cachedir != null ? cachedir.hashCode() : 0);
        return result;
    }


    /**
     * Return the version string metadata value for the plugin file, or null if it is not available or could not
     * loaded
     * @param file file
     * @return version string
     */
    static String getVersionForFile(final File file)  {
        try {
            final PluginMeta pluginMeta = loadMeta(file);
            return pluginMeta.getVersion();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Return default value for "mergeEnvironment" based on plugin type version
     * @param pluginMeta
     * @return
     */
    public static boolean getDefaultMergeEnvVars(final PluginMeta pluginMeta) {
        if (VERSION_1_0.equals(pluginMeta.getRundeckPluginVersion())) {
            return false;
        }
        return true;
    }
}
