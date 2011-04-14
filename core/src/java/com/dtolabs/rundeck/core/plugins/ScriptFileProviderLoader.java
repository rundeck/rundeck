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
import com.dtolabs.rundeck.core.plugins.metadata.PluginDef;
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.ZipUtil;
import com.dtolabs.rundeck.core.utils.cache.Expireable;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.JavaBeanLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ScriptFileProviderLoader is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptFileProviderLoader implements FileProviderLoader, Expireable {

    private static final Logger log = Logger.getLogger(ScriptFileProviderLoader.class.getName());
    public static final String SCRIPT_PLUGIN_VERSION = "1.0";
    private final File file;
    final File cachedir;
    private File fileExpandedDir;
    private PluginMeta metadata;
    private Map<ProviderIdent, ScriptPluginProvider> pluginProviderDefs =
        new HashMap<ProviderIdent, ScriptPluginProvider>();

    public ScriptFileProviderLoader(final File file, final File cachedir) {
        this.file = file;
        this.cachedir = cachedir;
    }

    public synchronized <T> T load(final PluggableService<T> service, final String providerName) throws
        ProviderLoaderException {
        if (!service.isScriptPluggable()) {
            return null;
        }
        final ProviderIdent ident = new ProviderIdent(service.getName(), providerName);

        if (null == pluginProviderDefs.get(ident)) {
            //look for plugin def
            final PluginMeta pluginMeta = getPluginMeta(file, ident);
            for (final PluginDef pluginDef : pluginMeta.getPluginDefs()) {
                if (matchesProvider(ident, pluginDef)) {
                    final ScriptPluginProvider provider;
                    try {
                        provider = getPlugin(file, pluginDef, ident);
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


    private PluginMeta getPluginMeta(final File file, final ProviderIdent ident) throws
        ProviderLoaderException {
        if (null != metadata) {
            return metadata;
        }
        try {
            metadata = loadMeta(file);
        } catch (IOException e) {
            throw new ProviderLoaderException(e, ident.getService(), ident.getProviderName());
        }
        return metadata;
    }

    private ScriptPluginProvider getPlugin(final File file, final PluginDef pluginDef, final ProviderIdent ident) throws
        ProviderLoaderException, PluginException {
        if (null == fileExpandedDir) {
            final File dir;
            try {
                dir = expandScriptPlugin(file);
            } catch (IOException e) {
                throw new ProviderLoaderException(e, ident.getService(), ident.getProviderName());
            }
            fileExpandedDir = dir;
            final File script = new File(fileExpandedDir, pluginDef.getScriptFile());
            if (null == pluginDef.getScriptInterpreter()) {
                try {
                    ScriptfileUtils.setExecutePermissions(script);
                } catch (IOException e) {
                    warn("Unable to set executable bit for script file: " + script + ": " + e.getMessage());
                }
            }
            debug("expanded plugin dir! " + fileExpandedDir);
        } else {

            debug("expanded plugin dir: " + fileExpandedDir);
        }

        //set executable bit for script-file of the provider
        final File script = new File(fileExpandedDir, pluginDef.getScriptFile());
        if (!script.exists() || !script.isFile()) {
            throw new PluginException("Script file was not found: " + script.getAbsolutePath());
        }
        return new ScriptPluginProviderImpl(pluginDef, file, fileExpandedDir);
    }

    private boolean matchesProvider(final ProviderIdent ident, final PluginDef pluginDef) {
        return ident.getService().equals(pluginDef.getService()) && ident.getProviderName().equals(pluginDef.getName());
    }

    public synchronized boolean isLoaderFor(final ProviderIdent ident) {

        final PluginMeta pluginMeta;
        try {
            pluginMeta = getPluginMeta(file, ident);
        } catch (ProviderLoaderException e) {
            warn("Unable to load file meta: " + e.getMessage());
            return false;
        }
        if (null == pluginMeta) {
            return false;
        }
        for (final PluginDef pluginDef : pluginMeta.getPluginDefs()) {
            if (matchesProvider(ident, pluginDef)) {
                return true;
            }
        }
        return false;
    }


    static PluginMeta loadMeta(final File jar) throws IOException {
        final ZipInputStream zipinput = new ZipInputStream(new FileInputStream(jar));
        final PluginMeta metadata = ScriptFileProviderLoader.loadMeta(jar, zipinput);
        zipinput.close();
        return metadata;
    }

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
                metadata = loadMetadataYaml(zipinput);
                found = true;
            }
            if (dirfound && found) {
                break;
            }
            nextEntry = zipinput.getNextEntry();
        }
        if (!topfound) {
            warn("Plugin not loaded: Found no " + basename + "/ dir within file: " + jar.getAbsolutePath());
        }
        if (!found) {
            warn("Plugin not loaded: Found no " + basename + "/plugin.yaml within: " + jar.getAbsolutePath());
        }
        if (!dirfound) {
            warn("Plugin not loaded: Found no " + basename + "/contents dir within: " + jar.getAbsolutePath());
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
        final JavaBeanLoader<PluginMeta> yaml = new JavaBeanLoader<PluginMeta>(PluginMeta.class);

        return yaml.load(stream);
    }

    /**
     * Return true if loaded metadata about the plugin file is valid.
     */
    static boolean validatePluginMeta(final PluginMeta pluginList, final File file) {
        boolean valid = true;
        if (null == pluginList.getName()) {
            warn("name not found in metadata: " + file.getAbsolutePath());
            valid = false;
        }
        if (null == pluginList.getVersion()) {
            warn("version not found in metadata: " + file.getAbsolutePath());
            valid = false;
        }
        if (null == pluginList.getRundeckPluginVersion()) {
            warn("rundeckPluginVersion not found in metadata: " + file.getAbsolutePath());
            valid = false;
        } else if (!SCRIPT_PLUGIN_VERSION.equals(pluginList.getRundeckPluginVersion())) {
            warn("rundeckPluginVersion: " + pluginList.getRundeckPluginVersion() + " is not supported: " + file
                .getAbsolutePath());
            valid = false;
        }
        final List<PluginDef> pluginDefs = pluginList.getPluginDefs();
        for (final PluginDef pluginDef : pluginDefs) {
            try {
                validatePluginDef(pluginDef);
            } catch (PluginException e) {
                valid = false;
            }
        }
        return valid;
    }

    /**
     * Expand jar file into plugin cache dir
     *
     * @param file jar file
     *
     * @return cache dir for the contents of the plugin zip
     */
    private File expandScriptPlugin(final File file) throws IOException {
        if (!cachedir.exists()) {
            if (!cachedir.mkdirs()) {
                warn("Unable to create cache dir: " + cachedir.getAbsolutePath());
            }
        }
        final File jardir = getFileCacheDir();
        if (!jardir.exists()) {
            if (!jardir.mkdir()) {
                warn("Unable to create cache dir for plugin: " + jardir.getAbsolutePath());
            }
        }
        final String prefix = getFileBasename() + "/contents";

        debug("Expand zip " + file.getAbsolutePath() + " to dir: " + jardir + ", prefix: " + prefix);
        ZipUtil.extractZip(file.getAbsolutePath(), jardir, prefix, prefix + "/");

        return jardir;
    }

    private synchronized boolean removeScriptPluginCache() {
        if (null != fileExpandedDir && fileExpandedDir.exists()) {
            debug("removeScriptPluginCache: " + fileExpandedDir);
            return FileUtils.deleteDir(fileExpandedDir);
        }
        return true;
    }

    String getFileBasename() {
        return basename(file);
    }

    private static String basename(final File file) {
        final String name = file.getName();
        return name.substring(0, name.lastIndexOf("."));
    }

    File getFileCacheDir() {
        return new File(cachedir, getFileBasename());
    }


    private static void validatePluginDef(final PluginDef pluginDef) throws PluginException {

        if (null == pluginDef.getPluginType() || "".equals(pluginDef.getPluginType())) {
            throw new PluginException("Script plugin missing plugin-type");
        }
        if ("script".equals(pluginDef.getPluginType())) {
            validateScriptPluginDef(pluginDef);
        } else {
            throw new PluginException("plugin missing has invalid plugin-type: " + pluginDef.getPluginType());
        }
    }

    private static void validateScriptPluginDef(final PluginDef pluginDef) throws PluginException {
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

    private static void warn(final String msg) {
        log.warn(msg);
    }

    private static void error(final String msg) {
        log.error(msg);
    }

    public void expire() {
        removeScriptPluginCache();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ScriptFileProviderLoader that = (ScriptFileProviderLoader) o;

        if (!cachedir.equals(that.cachedir)) {
            return false;
        }
        if (!file.equals(that.file)) {
            return false;
        }
        if (fileExpandedDir != null ? !fileExpandedDir.equals(that.fileExpandedDir) : that.fileExpandedDir != null) {
            return false;
        }
        if (metadata != null ? !metadata.equals(that.metadata) : that.metadata != null) {
            return false;
        }
        if (pluginProviderDefs != null ? !pluginProviderDefs.equals(that.pluginProviderDefs)
                                       : that.pluginProviderDefs != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = file.hashCode();
        result = 31 * result + cachedir.hashCode();
        result = 31 * result + (fileExpandedDir != null ? fileExpandedDir.hashCode() : 0);
        result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
        result = 31 * result + (pluginProviderDefs != null ? pluginProviderDefs.hashCode() : 0);
        return result;
    }
}
