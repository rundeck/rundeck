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

/*
* ScriptFileProviderLoader.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/13/11 10:07 AM
*
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.VersionConstants;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.plugins.metadata.ProviderDef;
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.ZipUtil;
import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.dtolabs.rundeck.core.plugins.JarPluginProviderLoader.RESOURCES_DIR_DEFAULT;

/**
 * ScriptPluginProviderLoader can load a provider instance for a service from a script plugin zip file.
 *
 * Services that want to use this loader need to implement {@link ScriptPluginProviderLoadable}
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptPluginProviderLoader implements ProviderLoader, FileCache.Expireable, PluginResourceLoader, PluginMetadata {

    private static final Logger         log                               = LoggerFactory.getLogger(ScriptPluginProviderLoader.class.getName());
    public static final  String         VERSION_1_0                       = "1.0";
    public static final  String         VERSION_1_1                       = "1.1";
    public static final  String         VERSION_1_2                       = "1.2";
    public static final  VersionCompare SUPPORTS_RESOURCES_PLUGIN_VERSION = VersionCompare.forString(VERSION_1_2);
    public static final  String         VERSION_2_0                       = "2.0";
    public static final  List<String>   SUPPORTED_PLUGIN_VERSIONS;
    static {
        SUPPORTED_PLUGIN_VERSIONS = Collections.unmodifiableList(Arrays.asList(
                VERSION_1_0,
                VERSION_1_1,
                VERSION_1_2,
                VERSION_2_0
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
     * Metadata from the plugin.yaml file
     */
    private PluginResourceLoader resourceLoader;
    /**
     * cache of ident to scriptplugin def mapping
     */
    private Map<ProviderIdent, ScriptPluginProvider> pluginProviderDefs =
        new HashMap<ProviderIdent, ScriptPluginProvider>();
    private List<String> pluginResourcesList;

    public ScriptPluginProviderLoader(final File file, final File cachedir) {
        this.file = file;
        this.cachedir = cachedir;
    }

    @Override
    public boolean canLoadForService(final FrameworkSupportService service) {
        return service instanceof ScriptPluginProviderLoadable;
    }

    private PluginResourceLoader getResourceLoader() throws PluginException {
        if (null == resourceLoader) {
            synchronized (this) {
                if (null == resourceLoader) {
                    try {
                        ZipResourceLoader loader = new ZipResourceLoader(
                                new File(getFileCacheDir(), "resources"),
                                file,
                                getPluginResourcesList(),
                                getFileBasename() + "/" + getResourcesBasePath()
                        );
                        loader.extractResources();
                        this.resourceLoader = loader;
                    } catch (IOException e) {
                        throw new PluginException("Unable to expand plugin libs: " + e.getMessage(), e);
                    }
                }
            }
        }
        return resourceLoader;
    }

    @Override
    public List<String> listResources() throws PluginException, IOException {
        if (supportsResources(getPluginMeta())) {
            return getResourceLoader().listResources();
        }
        return null;
    }

    @Override
    public InputStream openResourceStreamFor(final String name) throws PluginException, IOException {
        if (supportsResources(getPluginMeta())) {
            return getResourceLoader().openResourceStreamFor(name);
        }
        return null;
    }

    /**
     * Load a provider instance for the service by name
     */
    public synchronized <T> T load(final PluggableService<T> service, final String providerName) throws
        ProviderLoaderException {
        if (!(service instanceof ScriptPluginProviderLoadable)) {
            return null;
        }
        ScriptPluginProviderLoadable<T> loader =(ScriptPluginProviderLoadable<T>) service;
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

        try {
            getResourceLoader().listResources();
        } catch(IOException iex) {
            throw new ProviderLoaderException(iex,service.getName(),providerName);
        }
        catch (PluginException e) {
            throw new ProviderLoaderException(e, service.getName(), providerName);
        }
        if (null != scriptPluginProvider) {
            try {
                return loader.createScriptProviderInstance(scriptPluginProvider);
            } catch (PluginException e) {
                throw new ProviderLoaderException(e, service.getName(), providerName);
            }
        }
        return null;
    }

    @Override
    public <T> CloseableProvider<T> loadCloseable(final PluggableService<T> service, final String providerName)
            throws ProviderLoaderException
    {
        final T load = load(service, providerName);
        if (null == load) {
            return null;
        }
        return Closeables.closeableProvider(load);
    }

    private Date dateLoaded = null;

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
        metadata.setId(PluginUtils.generateShaIdFromName(metadata.getName()));
        dateLoaded = new Date();
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
            if (pluginDef.getPluginType().equals("script")) {

                final File script = new File(fileExpandedDir, pluginDef.getScriptFile());
                //set executable bit for script-file of the provider
                try {
                    ScriptfileUtils.setExecutePermissions(script);
                } catch (IOException e) {
                    log.warn("Unable to set executable bit for script file: " + script + ": " + e.getMessage());
                }
            }
            debug("expanded plugin dir! " + fileExpandedDir);
        } else {
            debug("expanded plugin dir: " + fileExpandedDir);
        }
        if (pluginDef.getPluginType().equals("script")) {
            final File script = new File(fileExpandedDir, pluginDef.getScriptFile());
            if (!script.exists() || !script.isFile()) {
                throw new PluginException("Script file was not found: " + script.getAbsolutePath());
            }
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
        boolean resfound = false;
        ZipEntry nextEntry = zipinput.getNextEntry();
        Set<String> paths = new HashSet<>();
        while (null != nextEntry) {
            paths.add(nextEntry.getName());

            if (!found && !nextEntry.isDirectory() && nextEntry.getName().equals(basename + "/plugin.yaml")) {
//                debug("Found metadata: " + nextEntry.getName());
                try {
                    metadata = loadMetadataYaml(zipinput);
                    found = true;
                } catch (Throwable e) {
                    log.error("Error parsing metadata file plugin.yaml: " + e.getMessage(), e);
                }
            }
            nextEntry = zipinput.getNextEntry();
        }
        if (!found || metadata == null) {
            log.error("Plugin not loaded: Found no " + basename + "/plugin.yaml within: " + jar.getAbsolutePath());
        }
        String resdir = null != metadata ? getResourcesBasePath(metadata) : null;

        for (String path : paths) {
            if (!topfound && path.startsWith(basename + "/")) {
                topfound = true;
            }
            if (!dirfound && (path.startsWith(basename + "/contents/") || path.equals(basename + "/contents"))) {
                dirfound = true;
            }
            if (!resfound
                && resdir != null
                && (path.startsWith(basename + "/" + resdir + "/") || path.equals(basename + "/" + resdir))) {
                resfound = true;
            }
        }
        if (!topfound) {
            log.error("Plugin not loaded: Found no " + basename + "/ dir within file: " + jar.getAbsolutePath());
        }
        if (!dirfound && !resfound) {
            log.error("Plugin not loaded: Found no " +
                      basename +
                      "/contents or " +
                      basename +
                      "/" + resdir + " dir within: " +
                      jar.getAbsolutePath());
        }
        if (found && (dirfound || resfound)) {
            return metadata;
        }
        return null;
    }

    /**
     * define only constructor for single type
     */
    static class SingleTypeConstructor extends Constructor{
        public SingleTypeConstructor(Class<?> clazz) {
            super(clazz, new LoaderOptions());
            this.yamlConstructors.put(null, undefinedConstructor);
            this.yamlConstructors.put(new Tag(clazz), new SubtypeConstructYamlObject());
        }
        //required because ConstructYamlObject is protected
        class SubtypeConstructYamlObject extends ConstructYamlObject{

        }
    }
    /**
     * return loaded yaml plugin metadata from the stream
     */
    static PluginMeta loadMetadataYaml(final InputStream stream) {
        final Yaml yaml = new Yaml(new SingleTypeConstructor(PluginMeta.class));
        return yaml.loadAs(stream, PluginMeta.class);
    }

    /**
     * Return true if loaded metadata about the plugin file is valid.
     */
    static PluginValidation validatePluginMeta(final PluginMeta pluginList, final File file) {
        return validatePluginMeta(pluginList, file, VersionConstants.VERSION);
    }

    /**
     * Return true if loaded metadata about the plugin file is valid.
     */
    static PluginValidation validatePluginMeta(final PluginMeta pluginList, final File file, final String rundeckVersion) {
        PluginValidation.State state = PluginValidation.State.VALID;
        if (pluginList == null) {
            return PluginValidation.builder()
                                   .message("No metadata")
                                   .state(PluginValidation.State.INVALID)
                                   .build();
        }
        List<String> messages = new ArrayList<>();
        if (null == pluginList.getName()) {
            messages.add("'name' not found in metadata");
            state = PluginValidation.State.INVALID;
        }
        if (null == pluginList.getVersion()) {
            messages.add("'version' not found in metadata");
            state = PluginValidation.State.INVALID;
        }
        if (null == pluginList.getRundeckPluginVersion()) {
            messages.add("'rundeckPluginVersion' not found in metadata");
            state = PluginValidation.State.INVALID;
        } else if (!SUPPORTED_PLUGIN_VERSIONS.contains(pluginList.getRundeckPluginVersion())) {
            messages.add("'rundeckPluginVersion': \"" + pluginList.getRundeckPluginVersion() + "\" is not supported");
            state = PluginValidation.State.INVALID;
        }
        if(pluginList.getRundeckPluginVersion().equals(VERSION_2_0)) {
            List<String> validationErrors = new ArrayList<>();

            PluginValidation.State
                hostCompatState =
                PluginMetadataValidator.validateTargetHostCompatibility(
                    validationErrors,
                    pluginList.getTargetHostCompatibility()
                );
            PluginValidation.State
                versCompatState = PluginMetadataValidator.validateRundeckCompatibility(
                validationErrors,
                rundeckVersion,
                pluginList.getRundeckCompatibilityVersion()
            );

            messages.addAll(validationErrors);
            state = state.or(hostCompatState)
                         .or(versCompatState);

        }
        final List<ProviderDef> pluginDefs = pluginList.getPluginDefs();
        for (final ProviderDef pluginDef : pluginDefs) {
            try {
                validateProviderDef(pluginDef);
            } catch (PluginException e) {
                messages.add(e.getMessage());
                state = PluginValidation.State.INVALID;
            }
        }
        return PluginValidation.builder()
                               .state(state)
                               .messages(messages)
                               .build();
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
        if(name.contains(".")) {
            return name.substring(0, name.lastIndexOf("."));
        }
        return name;
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
        } else if ("ui".equals(pluginDef.getPluginType())) {
            validateUIProviderDef(pluginDef);
        } else {
            throw new PluginException("Script plugin has invalid plugin-type: " + pluginDef.getPluginType());
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

    /**
     * Validate script provider def
     */
    private static void validateUIProviderDef(final ProviderDef pluginDef) throws PluginException {
        if (null == pluginDef.getName() || "".equals(pluginDef.getName())) {
            throw new PluginException("UI plugin missing name");
        }
        if (null == pluginDef.getService() || "".equals(pluginDef.getService())) {
            throw new PluginException("UI plugin missing service");
        }
        if (null == pluginDef.getPluginData() || null == pluginDef.getPluginData().get("ui")) {
            throw new PluginException("UI plugin missing ui: definition");
        }

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

    /**
     * Return true if the plugin version supports resources
     *
     * @param pluginMeta
     *
     * @return
     */
    public static boolean supportsResources(final PluginMeta pluginMeta) {
        return VersionCompare.forString(pluginMeta.getRundeckPluginVersion()).atLeast(SUPPORTS_RESOURCES_PLUGIN_VERSION);
    }

    public List<String> getPluginResourcesList() throws IOException {
        return getPluginMeta().getResourcesList();
    }

    public String getResourcesBasePath() throws IOException {
        return getResourcesBasePath(getPluginMeta());
    }

    public static String getResourcesBasePath(PluginMeta metadata) throws IOException {
        String resourcesDir = metadata.getResourcesDir();
        return null != resourcesDir ? resourcesDir : RESOURCES_DIR_DEFAULT;
    }

    @Override
    public String getFilename() {
        return file.getName();
    }

    @Override
    public File getFile() {
        return file;
    }

    @Override
    public String getPluginArtifactName() {
        try {
            return getPluginMeta().getName();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginAuthor() {
        try {
            return getPluginMeta().getAuthor();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginFileVersion() {
        try {
            return getPluginMeta().getVersion();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginVersion() {
        try {
            return getPluginMeta().getRundeckPluginVersion();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginUrl() {
        try {
            return getPluginMeta().getUrl();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public Date getPluginDate() {
        try {
            String date = getPluginMeta().getDate();
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(date);
        } catch (IOException | NullPointerException | ParseException e) {

        }
        return null;
    }

    @Override
    public Date getDateLoaded() {
        return dateLoaded;
    }

    @Override
    public String getPluginName() {
        try {
            return getPluginMeta().getName();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginDescription() {
        try {
            return getPluginMeta().getDescription();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginId() {
        try {
            return getPluginMeta().getId();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getRundeckCompatibilityVersion() {
        try {
            return getPluginMeta().getRundeckCompatibilityVersion();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getTargetHostCompatibility() {
        try {
            return getPluginMeta().getTargetHostCompatibility();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public List<String> getTags() {
        try {
            return getPluginMeta().getTags();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginLicense() {
        try {
            return getPluginMeta().getLicense();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginThirdPartyDependencies() {
        try {
            return getPluginMeta().getThirdPartyDependencies();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginSourceLink() {
        try {
            return getPluginMeta().getSourceLink();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginDocsLink() {
        try {
            return getPluginMeta().getDocsLink();
        } catch (IOException e) {

        }
        return null;
    }

    @Override
    public String getPluginType() {
        return "script";
    }
}
