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
 * JarFileProviderLoader.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 4/12/11 7:29 PM
 * 
 */
package com.dtolabs.rundeck.core.plugins;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import org.apache.log4j.Logger;

import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.utils.FileUtils;
import com.dtolabs.rundeck.core.utils.ZipUtil;
import com.dtolabs.rundeck.core.utils.cache.FileCache;

/**
 * JarPluginProviderLoader can load jar plugin files as provider instances.
 *
 * Calls to load a provider instance should be synchronized as this class will
 * perform file copy operations.
 *
 * Services that want to use this loader need to implement {@link JavaClassProviderLoadable}
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class JarPluginProviderLoader implements ProviderLoader,
        FileCache.Expireable,
        PluginResourceLoader,
        PluginMetadata,
        Closeable
{
    public static final String RESOURCES_DIR_DEFAULT = "resources";
    private static Logger log = Logger.getLogger(JarPluginProviderLoader.class.getName());
    public static final String RUNDECK_PLUGIN_ARCHIVE = "Rundeck-Plugin-Archive";
    public static final String RUNDECK_PLUGIN_CLASSNAMES = "Rundeck-Plugin-Classnames";
    public static final String RUNDECK_PLUGIN_RESOURCES = "Rundeck-Plugin-Resources";
    public static final String RUNDECK_PLUGIN_RESOURCES_DIR = "Rundeck-Plugin-Resources-Dir";
    public static final String RUNDECK_PLUGIN_LIBS = "Rundeck-Plugin-Libs";
    public static final String JAR_PLUGIN_VERSION = "1.1";
    public static final String JAR_PLUGIN_VERSION_1_2 = "1.2";
    public static final String JAR_PLUGIN_VERSION_2_0 = "2.0";
    public static final VersionCompare SUPPORTS_RESOURCES_PLUGIN_VERSION = VersionCompare.forString(
            JAR_PLUGIN_VERSION_1_2);
    public static final VersionCompare LOWEST_JAR_PLUGIN_VERSION = VersionCompare.forString(JAR_PLUGIN_VERSION);
    public static final String RUNDECK_PLUGIN_VERSION = "Rundeck-Plugin-Version";
    public static final String RUNDECK_PLUGIN_FILE_VERSION = "Rundeck-Plugin-File-Version";
    public static final String RUNDECK_PLUGIN_AUTHOR = "Rundeck-Plugin-Author";
    public static final String RUNDECK_PLUGIN_NAME = "Rundeck-Plugin-Name";
    public static final String RUNDECK_PLUGIN_URL = "Rundeck-Plugin-URL";
    public static final String RUNDECK_PLUGIN_DATE = "Rundeck-Plugin-Date";
    public static final String RUNDECK_PLUGIN_LIBS_LOAD_FIRST = "Rundeck-Plugin-Libs-Load-First";
    public static final String CACHED_JAR_TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";
    //Plugin Version 2 attributes
    public static final String RUNDECK_PLUGIN_RUNDECK_COMPAT_VER = "Rundeck-Plugin-Rundeck-Compatibility-Version";
    public static final String RUNDECK_PLUGIN_DESCRIPTION = "Rundeck-Plugin-Description";
    public static final String RUNDECK_PLUGIN_LICENSE = "Rundeck-Plugin-License";
    public static final String RUNDECK_PLUGIN_TAGS = "Rundeck-Plugin-Tags";
    public static final String RUNDECK_PLUGIN_THIRD_PARTY_DEPS = "Rundeck-Plugin-Third-Party-Dependencies";
    public static final String RUNDECK_PLUGIN_SOURCE_LINK = "Rundeck-Plugin-Source-Link";
    public static final String RUNDECK_PLUGIN_TARGET_HOST_COMPAT = "Rundeck-Plugin-Target-Host-Compatibility";

    //End Plugin Version 2 attributes
    private final File pluginJar;
    private final File pluginJarCacheDirectory;
    private final File cachedir;
    private final boolean loadLibsFirst;
    private final DateFormat cachedJarTimestampFormatter = new SimpleDateFormat(CACHED_JAR_TIMESTAMP_FORMAT);
    @SuppressWarnings("rawtypes")
    private Map<ProviderIdent, Class> pluginProviderDefs = new HashMap<ProviderIdent, Class>();
    private AtomicInteger loadCount = new AtomicInteger();

    public JarPluginProviderLoader(final File pluginJar, final File pluginJarCacheDirectory, final File cachedir) {
        this(pluginJar, pluginJarCacheDirectory, cachedir, true);
    }

    public JarPluginProviderLoader(final File pluginJar, final File pluginJarCacheDirectory, final File cachedir,
            final boolean loadLibsFirst) {
        if (null == pluginJar) {
            throw new NullPointerException("Expected non-null plugin jar argument.");
        }
        if (!pluginJar.exists()) {
            throw new IllegalArgumentException("File does not exist: " + pluginJar);
        }
        if (!pluginJar.isFile()) {
            throw new IllegalArgumentException("Not a file: " + pluginJar);
        }
        this.pluginJar = pluginJar;
        this.pluginJarCacheDirectory = pluginJarCacheDirectory;
        this.cachedir = cachedir;
        this.loadLibsFirst = loadLibsFirst;
    }

    @Override
    public boolean canLoadForService(final FrameworkSupportService service) {
        return service instanceof JavaClassProviderLoadable;
    }

    private boolean supportsResources(final String pluginVersion) {
        return VersionCompare.forString(pluginVersion).atLeast(SUPPORTS_RESOURCES_PLUGIN_VERSION);
    }

    @Override
    public List<String> listResources() throws PluginException, IOException {
        if (supportsResources(getPluginVersion())) {
            return getCachedJar().resourcesLoader.listResources();
        }
        return null;
    }

    @Override
    public InputStream openResourceStreamFor(final String path) throws PluginException, IOException {
        if (supportsResources(getPluginVersion())) {
            return getCachedJar().resourcesLoader.openResourceStreamFor(path);
        }
        return null;
    }

    private final Closeable dereferencer = new Closeable() {
        @Override
        public void close() throws IOException {
            removeReference();
        }
    };

    @Override
    public <T> CloseableProvider<T> loadCloseable(final PluggableService<T> service, final String providerName)
            throws ProviderLoaderException
    {
        addReference();
        return Closeables.closeableProvider(load(service, providerName), Closeables.closeOnce(dereferencer));
    }

    /**
     * Load provider instance for the service
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> T load(final PluggableService<T> service, final String providerName)
            throws ProviderLoaderException {
        final ProviderIdent ident = new ProviderIdent(service.getName(), providerName);
        debug("loadInstance for " + ident + ": " + pluginJar);

        if (null == pluginProviderDefs.get(ident)) {
            final String[] strings = getClassnames();
            for (final String classname : strings) {
                final Class<?> cls;
                try {
                    cls = loadClass(classname);
                    if (matchesProviderDeclaration(ident, cls)) {
                        pluginProviderDefs.put(ident, cls);
                    }
                } catch (PluginException e) {
                    log.error("Failed to load class from " + pluginJar + ": classname: " + classname + ": "
                            + e.getMessage());
                }
            }
        }
        final Class<T> cls = pluginProviderDefs.get(ident);
        if (null != cls) {
            try {
                return createProviderForClass(service, cls);
            } catch (PluginException e) {
                throw new ProviderLoaderException(e, service.getName(), providerName);
            }
        }
        return null;
    }

    /**
     * Return true if the ident matches the Plugin annotation for the class
     */
    static boolean matchesProviderDeclaration(final ProviderIdent ident, final Class<?> cls) throws PluginException {
        final Plugin annotation = getPluginMetadata(cls);
        return ident.getFirst().equals(annotation.service()) && ident.getSecond().equals(annotation.name());
    }

    /**
     * Return true if the ident matches the Plugin annotation for the class
     */
    static ProviderIdent getProviderDeclaration(final Class<?> cls) throws PluginException {
        final Plugin annotation = getPluginMetadata(cls);
        return new ProviderIdent(annotation.service(), annotation.name());
    }

    Attributes mainAttributes;
    String pluginId; //set when main attributes are loaded

    /**
     * Get the declared list of provider classnames for the file
     */
    public String[] getClassnames() {
        final Attributes attributes = getMainAttributes();
        if (null == attributes) {
            return null;
        }
        final String value = attributes.getValue(RUNDECK_PLUGIN_CLASSNAMES);
        if (null == value) {
            return null;
        }
        return value.split(",");
    }

    private String getResourcesBasePath() {
        final Attributes attributes = getMainAttributes();
        if (null != attributes) {
            final String dir = attributes.getValue(RUNDECK_PLUGIN_RESOURCES_DIR);
            if (null != dir) {
                //list resources in the dir of the jar
                return dir;
            }
        }
        return RESOURCES_DIR_DEFAULT;
    }


    private List<String> getPluginResourcesList() {
        final Attributes attributes = getMainAttributes();
        if (null != attributes) {
            final String value = attributes.getValue(RUNDECK_PLUGIN_RESOURCES);
            if (null != value) {
                return Arrays.asList(value.split(" *, *"));
            }
        }
        return null;
    }

    /**
     * Get the version of the plugin, not the file version
     *
     * @return
     */
    public String getPluginVersion() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_VERSION);
    }

    /**
     * return the main attributes from the jar manifest
     */
    private Attributes getMainAttributes() {
        if (null == mainAttributes) {
            mainAttributes = getJarMainAttributes(pluginJar);
            if(mainAttributes.getValue(RUNDECK_PLUGIN_VERSION).equals(JAR_PLUGIN_VERSION_2_0)) {
                String pluginName = mainAttributes.getValue(RUNDECK_PLUGIN_NAME);
                if(pluginName == null) pluginName = pluginJar.getName();
                pluginId = PluginUtils.generateShaIdFromName(pluginName);
            }
        }
        return mainAttributes;
    }

    /**
     * Get the main attributes for the jar file
     */
    private static Attributes getJarMainAttributes(final File file) {
        debug("getJarMainAttributes: " + file);

        try {
            try(final JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file))){
                return jarInputStream.getManifest().getMainAttributes();
            }
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Attempt to create an instance of thea provider for the given service
     *
     * @param cls class
     * @return created instance
     */
    static <T,X extends T> T createProviderForClass(final PluggableService<T> service, final Class<X> cls) throws PluginException,
            ProviderCreationException {
        debug("Try loading provider " + cls.getName());
        if(!(service instanceof JavaClassProviderLoadable)){
            return null;
        }
        JavaClassProviderLoadable<T> loadable = (JavaClassProviderLoadable<T>) service;
        final Plugin annotation = getPluginMetadata(cls);

        final String pluginname = annotation.name();

        if (!loadable.isValidProviderClass(cls)) {
            throw new PluginException("Class " + cls.getName() + " was not a valid plugin class for service: " 
                    + service.getName() + ". Expected class " + cls.getName() + ", with a public constructor with no parameter");
        }
        debug("Succeeded loading plugin " + cls.getName() + " for service: " + service.getName());
        return loadable.createProviderInstance(cls, pluginname);
    }

    private static void debug(final String s) {
        if (log.isDebugEnabled()) {
            log.debug(s);
        }
    }

    /**
     * Get the Plugin annotation for the class
     */
    static Plugin getPluginMetadata(final Class<?> cls) throws PluginException {
        // try to get plugin provider name
        final String pluginname;
        if (!cls.isAnnotationPresent(Plugin.class)) {
            throw new PluginException("No Plugin annotation was found for the class: " + cls.getName());
        }

        final Plugin annotation = (Plugin) cls.getAnnotation(Plugin.class);
        pluginname = annotation.name();
        if (null == pluginname || "".equals(pluginname)) {
            throw new PluginException("Plugin annotation 'name' cannot be empty for the class: " + cls.getName());
        }
        // get service name from annotation
        final String servicename = annotation.service();
        if (null == servicename || "".equals(servicename)) {
            throw new PluginException("Plugin annotation 'service' cannot be empty for the class: " + cls.getName());
        }
        return annotation;
    }

    private Map<String, Class<?>> classCache = new HashMap<String, Class<?>>();

    /**
     * @return true if the other jar is a copy of the pluginJar based on names returned by generateCachedJarName
     */
    protected boolean isEquivalentPluginJar(File other) {
        return other.getName().replaceFirst("\\d+-\\d+-", "").equals(pluginJar.getName());
    }

    static final AtomicLong counter = new AtomicLong(0);
    /**
     * @return a generated name for the pluginJar using the last modified timestamp
     */
    protected String generateCachedJarIdentity() {
        Date mtime = new Date(pluginJar.lastModified());
        return String.format(
                "%s-%d",
                cachedJarTimestampFormatter.format(mtime),
                counter.getAndIncrement()
        );
    }

    /**
     * @return a generated name for the pluginJar using the last modified timestamp
     */
    protected String generateCachedJarName(String ident) {
        return String.format(
                "%s-%s",
                ident,
                pluginJar.getName()
        );
    }

    /**
     * @return a generated name for the pluginJar using the last modified timestamp
     */
    protected File generateCachedJarDir(String ident) {
        File dir = new File(getFileCacheDir(), ident);
        if (!dir.mkdirs()) {
            debug("Could not create dir for cachedjar libs: " + dir);
        }
        return dir;
    }

    /**
     * Creates a single cached version of the pluginJar located within pluginJarCacheDirectory
     * deleting all existing versions of pluginJar
     * @param jarName
     */
    protected File createCachedJar(final File dir, final String jarName) throws PluginException {
        File cachedJar;
        try {
            cachedJar = new File(dir, jarName);
            cachedJar.deleteOnExit();
            FileUtils.fileCopy(pluginJar, cachedJar, true);
        } catch (IOException e) {
            throw new PluginException(e);
        }
        return cachedJar;
    }

    /**
     * Load a class from the jar file by name
     */
    private Class<?> loadClass(final String classname) throws PluginException {
        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        if (null != classCache.get(classname)) {
            return classCache.get(classname);
        }
        CachedJar cachedJar1 = getCachedJar();

        debug("loadClass! " + classname + ": " + cachedJar1.getCachedJar());
        final Class<?> cls;
        final URLClassLoader urlClassLoader = cachedJar1.getClassLoader();
        try {
            cls = Class.forName(classname, true, urlClassLoader);
            classCache.put(classname, cls);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Class not found: " + classname, e);
        } catch (Throwable t) {
            throw new PluginException("Error loading class: " + classname, t);
        }
        return cls;
    }

    private CachedJar cachedJar;
    private Date loadedDate = null;

    private synchronized JarPluginProviderLoader.CachedJar getCachedJar() throws PluginException {
        if (null == cachedJar) {
            synchronized (this) {
                if (null == cachedJar) {
                    this.loadedDate = new Date();
                    String itemIdent = generateCachedJarIdentity();
                    String jarName = generateCachedJarName(itemIdent);
                    File dir = generateCachedJarDir(itemIdent);
                    File cachedJar = createCachedJar(dir, jarName);

                    // if jar manifest declares secondary lib deps, expand lib into cachedir, and setup classloader
                    // to use the libs
                    Collection<File> extlibs = null;
                    try {
                        extlibs = extractDependentLibs(dir);
                    } catch (IOException e) {
                        throw new PluginException("Unable to expand plugin libs: " + e.getMessage(), e);
                    }
                    ZipResourceLoader loader = null;
                    if (supportsResources(getPluginVersion())) {

                        loader = new ZipResourceLoader(
                                new File(dir, "resources"),
                                cachedJar,
                                getPluginResourcesList(),
                                getResourcesBasePath()
                        );
                        try {
                            loader.extractResources();
                        } catch (IOException e) {
                            throw new PluginException("Unable to expand plugin resources: " + e.getMessage(), e);
                        }
                    }
                    this.cachedJar = new CachedJar(dir, cachedJar, extlibs, loader);
                }
            }
        }
        return cachedJar;
    }


    /**
     * Extract the dependent libs and return the extracted jar files
     *
     * @return the collection of extracted files
     */
    private Collection<File> extractDependentLibs(final File cachedir) throws IOException {
        final Attributes attributes = getMainAttributes();
        if (null == attributes) {
            debug("no manifest attributes");
            return null;
        }

        final ArrayList<File> files = new ArrayList<File>();
        final String libs = attributes.getValue(RUNDECK_PLUGIN_LIBS);
        if (null != libs) {
            debug("jar libs listed: " + libs + " for file: " + pluginJar);
            if (!cachedir.isDirectory()) {
                if (!cachedir.mkdirs()) {
                    debug("Failed to create cachedJar dir for dependent libs: " + cachedir);
                }
            }
            final String[] libsarr = libs.split(" ");
            extractJarContents(libsarr, cachedir);
            for (final String s : libsarr) {
                File libFile = new File(cachedir, s);
                libFile.deleteOnExit();
                files.add(libFile);
            }
        } else {
            debug("no jar libs listed in manifest: " + pluginJar);
        }
        return files;
    }

    /**
     * Extract specific entries from the jar to a destination directory. Creates the
     * destination directory if it does not exist
     *
     * @param entries
     *            the entries to extract
     * @param destdir
     *            destination directory
     */
    private void extractJarContents(final String[] entries, final File destdir) throws IOException {
        if (!destdir.exists()) {
            if (!destdir.mkdir()) {
                log.warn("Unable to create cache dir for plugin: " + destdir.getAbsolutePath());
            }
        }

        debug("extracting lib files from jar: " + pluginJar);
        for (final String path : entries) {
            debug("Expand zip " + pluginJar.getAbsolutePath() + " to dir: " + destdir + ", file: " + path);
            ZipUtil.extractZipFile(pluginJar.getAbsolutePath(), destdir, path);
        }

    }

    /**
     * Basename of the file
     */
    String getFileBasename() {
        return basename(pluginJar);
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
     * Return true if the file has a class that provides the ident.
     */
    public synchronized boolean isLoaderFor(final ProviderIdent ident) {
        final String[] strings = getClassnames();
        for (final String classname : strings) {
            try {
                if (matchesProviderDeclaration(ident, loadClass(classname))) {
                    return true;
                }
            } catch (PluginException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public synchronized List<ProviderIdent> listProviders() {
        final ArrayList<ProviderIdent> providerIdents = new ArrayList<ProviderIdent>();
        final String[] strings = getClassnames();
        for (final String classname : strings) {
            try {
                providerIdents.add(getProviderDeclaration(loadClass(classname)));
            } catch (PluginException e) {
                e.printStackTrace();
            }
        }
        return providerIdents;
    }


    private synchronized int addReference() {
        return loadCount.incrementAndGet();
    }

    private synchronized int removeReference() {
        int i = loadCount.decrementAndGet();
        debug(String.format("removeReference for: %s (loadCount: %d)", pluginJar, i));
        if (i <= 0) {
            if (isExpired()) {
                try {
                    close();
                } catch (IOException e) {

                }
            }
        }
        return i;
    }

    /**
     * Close class loaders and delete cached files
     * @throws IOException
     */
    @Override
    public void close() throws IOException {
        debug(String.format("close jar provider loader for: %s", pluginJar));
        synchronized (this) {
            closed = true;
        }
        if (null != cachedJar) {
            cachedJar.close();
            classCache.clear();
            cachedJar = null;
        }
    }

    private boolean closed = false;

    public synchronized boolean isClosed() {
        return closed;
    }

    private boolean expired = false;

    public synchronized boolean isExpired() {
        return expired;
    }

    /**
     * Expire the loader cache item
     */
    public void expire() {
        synchronized (this) {
            expired = true;
        }
        int i = loadCount.get();
        debug(String.format("expire jar provider loader for: %s (loadCount: %d)", pluginJar, i));
        if (i <= 0) {
            try {
                close();
            } catch (IOException e) {

            }
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final JarPluginProviderLoader that = (JarPluginProviderLoader) o;

        if (classCache != null ? !classCache.equals(that.classCache) : that.classCache != null) {
            return false;
        }
        if (!pluginJar.equals(that.pluginJar)) {
            return false;
        }
        if (mainAttributes != null ? !mainAttributes.equals(that.mainAttributes) : that.mainAttributes != null) {
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
        int result = pluginJar.hashCode();
        result = 31 * result + (pluginProviderDefs != null ? pluginProviderDefs.hashCode() : 0);
        result = 31 * result + (mainAttributes != null ? mainAttributes.hashCode() : 0);
        result = 31 * result + (classCache != null ? classCache.hashCode() : 0);
        return result;
    }

    /**
     * Return true if the file is a valid jar plugin file
     */
    public static boolean isValidJarPlugin(final File file) {
        try {
            try (final JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file))) {
                final Manifest manifest = jarInputStream.getManifest();
                if (null == manifest) {
                    return false;
                }
                final Attributes mainAttributes = manifest.getMainAttributes();
                validateJarManifest(mainAttributes);
            }
            return true;
        } catch (IOException | InvalidManifestException e) {
            log.error(file.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Validate whether the jar file has a valid manifest, throw exception if invalid
     */
    static void validateJarManifest(final Attributes mainAttributes) throws InvalidManifestException {
        final String value1 = mainAttributes.getValue(RUNDECK_PLUGIN_ARCHIVE);
        final String plugvers = mainAttributes.getValue(RUNDECK_PLUGIN_VERSION);

        final String plugclassnames = mainAttributes.getValue(RUNDECK_PLUGIN_CLASSNAMES);
        if (null == value1) {
            throw new InvalidManifestException("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_ARCHIVE);
        } else if (!"true".equals(value1)) {
            throw new InvalidManifestException(RUNDECK_PLUGIN_ARCHIVE + " was not 'true': " + value1);
        }
        if (null == plugvers) {
            throw new InvalidManifestException("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_VERSION);
        }
        final VersionCompare pluginVersion = VersionCompare.forString(plugvers);
        if (!pluginVersion.atLeast(LOWEST_JAR_PLUGIN_VERSION)) {
            throw new InvalidManifestException("Unsupported plugin version: " + RUNDECK_PLUGIN_VERSION + ": "
                    + plugvers);
        }
        if (null == plugclassnames) {
            throw new InvalidManifestException("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_CLASSNAMES);
        }
        if(plugvers.equals(JAR_PLUGIN_VERSION_2_0)) {
            String pluginName = mainAttributes.getValue(RUNDECK_PLUGIN_NAME);
            if(pluginName == null) throw new InvalidManifestException("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_NAME);
            String rundeckCompat = mainAttributes.getValue(RUNDECK_PLUGIN_RUNDECK_COMPAT_VER);
            if(rundeckCompat == null) throw new InvalidManifestException("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_RUNDECK_COMPAT_VER);
            ArrayList<String> errors = new ArrayList<>();
            PluginMetadataValidator.validateRundeckCompatibility(errors, rundeckCompat);
            if(!errors.isEmpty()) {
                StringBuilder b = new StringBuilder();
                for(String err : errors) { b.append(err);b.append("\n"); }
                throw new InvalidManifestException(b.toString());
            }
        }
    }

    static class InvalidManifestException extends Exception {
        public InvalidManifestException(String s) {
            super(s);
        }
    }

    /**
     * Return the version string metadata value for the plugin file, or null if it is not available or could not
     * loaded
     *
     * @param file plugin file
     * @return version string
     */
    static String getVersionForFile(final File file) {
        return loadManifestAttribute(file, RUNDECK_PLUGIN_FILE_VERSION);
    }

    /**
     * Return true if the jar attributes declare it should load local dependency classes first.
     *
     * @param file plugin file
     *
     * @return true if plugin libs load first is set
     */
    static boolean getLoadLocalLibsFirstForFile(final File file) {
        Attributes attributes = loadMainAttributes(file);
        if (null == attributes) {
            return false;
        }
        boolean loadFirstDefault=true;
        String loadFirst = attributes.getValue(RUNDECK_PLUGIN_LIBS_LOAD_FIRST);
        if(null!=loadFirst){
            return Boolean.valueOf(loadFirst);
        }
        return loadFirstDefault;
    }

    private static Attributes loadMainAttributes(final File file) {
        Attributes mainAttributes = null;
        try {
            try(final JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file))) {
                final Manifest manifest = jarInputStream.getManifest();
                if (null != manifest) {
                    mainAttributes = manifest.getMainAttributes();
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
            log.warn(e.getMessage() + ": " + file.getAbsolutePath());
        }
        return mainAttributes;
    }

    private static String loadManifestAttribute(final File file, final String attribute) {
        String value = null;
        final Attributes mainAttributes = loadMainAttributes(file);
        if(null!=mainAttributes){
            value = mainAttributes.getValue(attribute);
        }
        return value;
    }

    /**
     * Holds the cached jar file, dir, libs list and class and resource loaders for a jar plugin
     */
    private class CachedJar implements Closeable {
        private File dir;
        private File cachedJar;
        private Collection<File> depLibs;
        private URLClassLoader classLoader;
        private PluginResourceLoader resourcesLoader;

        public File getDir() {
            return dir;
        }

        public File getCachedJar() {
            return cachedJar;
        }

        public CachedJar(
                File dir,
                File cachedJar,
                Collection<File> depLibs,
                PluginResourceLoader resourcesLoader
        )
                throws PluginException
        {
            this.dir = dir;
            this.cachedJar = cachedJar;
            this.depLibs = depLibs;
            this.resourcesLoader = resourcesLoader;
        }

        public Collection<File> getDepLibs() {
            return depLibs;
        }

        public URLClassLoader getClassLoader() throws PluginException{
            if(null==classLoader){
                synchronized (this){
                    if(null==classLoader){
                        classLoader = buildClassLoader();
                    }
                }
            }
            return classLoader;
        }

        private URLClassLoader buildClassLoader() throws PluginException {
            ClassLoader parent = JarPluginProviderLoader.class.getClassLoader();
            try {
                final URL url = getCachedJar().toURI().toURL();
                final URL[] urlarray;
                if (null != getDepLibs() && getDepLibs().size() > 0) {
                    final ArrayList<URL> urls = new ArrayList<URL>();
                    urls.add(url);
                    for (final File extlib : getDepLibs()) {
                        urls.add(extlib.toURI().toURL());
                    }
                    urlarray = urls.toArray(new URL[urls.size()]);
                } else {
                    urlarray = new URL[]{url};
                }
                URLClassLoader loaded = loadLibsFirst
                                        ? LocalFirstClassLoader.newInstance(urlarray, parent)
                                        : URLClassLoader.newInstance(urlarray, parent);
                return loaded;
            } catch (MalformedURLException e) {
                throw new PluginException("Error creating classloader for " + cachedJar, e);
            }
        }

        @Override
        public void close() throws IOException {
            debug(String.format("Jar plugin closing cached jar: %s", cachedJar));
            //close loaders
            if (null != classLoader) {
                try {
                    debug("expire classLoaders for: " + cachedJar);
                    classLoader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //remove cache files
            debug("remove cache dir on exit: " + dir);
            FileUtils.deleteDir(dir);
        }
    }

    @Override
    public String getFilename() {
        return pluginJar.getName();
    }

    @Override
    public File getFile() {
        return pluginJar;
    }

    @Override
    public String getPluginAuthor() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_AUTHOR);
    }

    @Override
    public String getPluginFileVersion() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_FILE_VERSION);
    }

    @Override
    public String getPluginUrl() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_URL);
    }

    @Override
    public Date getPluginDate() {
        Attributes mainAttributes = getMainAttributes();
        String value = mainAttributes.getValue(RUNDECK_PLUGIN_DATE);
        if (null != value) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(value);
            } catch (ParseException e) {

            }
        }
        return null;
    }

    @Override
    public Date getDateLoaded() {
        return loadedDate;
    }

    @Override
    public String getPluginName() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_NAME);
    }

    @Override
    public String getPluginDescription() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_DESCRIPTION);
    }

    @Override
    public String getPluginId() {
        getMainAttributes();
        return pluginId;
    }

    @Override
    public String getRundeckCompatibilityVersion() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_RUNDECK_COMPAT_VER);
    }

    @Override
    public String getTargetHostCompatibility() {
        Attributes mainAttributes = getMainAttributes();
        String hostCompat = mainAttributes.getValue(RUNDECK_PLUGIN_TARGET_HOST_COMPAT);
        if(hostCompat == null) hostCompat = "all";
        return hostCompat;
    }

    @Override
    public List<String> getTags() {
        Attributes mainAttributes = getMainAttributes();
        String tagString = mainAttributes.getValue(RUNDECK_PLUGIN_TAGS);
        List<String> tags = new ArrayList<>();
        if(tagString != null) {
            tags = Arrays.asList(tagString.split(","));
        }
        return tags;
    }

    @Override
    public String getPluginLicense() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_LICENSE);
    }

    @Override
    public String getPluginThirdPartyDependencies() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_THIRD_PARTY_DEPS);
    }

    @Override
    public String getPluginSourceLink() {
        Attributes mainAttributes = getMainAttributes();
        return mainAttributes.getValue(RUNDECK_PLUGIN_SOURCE_LINK);
    }

    @Override
    public String getPluginType() {
        return "jar";
    }
}
