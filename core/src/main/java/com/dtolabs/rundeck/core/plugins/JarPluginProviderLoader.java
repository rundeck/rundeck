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
 * JarFileProviderLoader.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: 4/12/11 7:29 PM
 * 
 */
package com.dtolabs.rundeck.core.plugins;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

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
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class JarPluginProviderLoader implements ProviderLoader, FileCache.Expireable {
    private static Logger log = Logger.getLogger(JarPluginProviderLoader.class.getName());
    public static final String RUNDECK_PLUGIN_ARCHIVE = "Rundeck-Plugin-Archive";
    public static final String RUNDECK_PLUGIN_CLASSNAMES = "Rundeck-Plugin-Classnames";
    public static final String RUNDECK_PLUGIN_LIBS = "Rundeck-Plugin-Libs";
    public static final String JAR_PLUGIN_VERSION = "1.1";
    public static final VersionCompare LOWEST_JAR_PLUGIN_VERSION = VersionCompare.forString(JAR_PLUGIN_VERSION);
    public static final String RUNDECK_PLUGIN_VERSION = "Rundeck-Plugin-Version";
    public static final String RUNDECK_PLUGIN_FILE_VERSION = "Rundeck-Plugin-File-Version";
    public static final String RUNDECK_PLUGIN_LIBS_LOAD_FIRST = "Rundeck-Plugin-Libs-Load-First";
    public static final String CACHED_JAR_TIMESTAMP_FORMAT = "yyyyMMddHHmmssSSS";
    private final File pluginJar;
    private final File pluginJarCacheDirectory;
    private final File cachedir;
    private final boolean loadLibsFirst;
    private final DateFormat cachedJarTimestampFormatter = new SimpleDateFormat(CACHED_JAR_TIMESTAMP_FORMAT);
    @SuppressWarnings("rawtypes")
    private Map<ProviderIdent, Class> pluginProviderDefs = new HashMap<ProviderIdent, Class>();

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

    /**
     * return the main attributes from the jar manifest
     */
    private Attributes getMainAttributes() {
        if (null == mainAttributes) {
            mainAttributes = getJarMainAttributes(pluginJar);
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

        final Plugin annotation = getPluginMetadata(cls);

        final String pluginname = annotation.name();

        if (!service.isValidProviderClass(cls)) {
            throw new PluginException("Class " + cls.getName() + " was not a valid plugin class for service: "
                    + service.getName());
        }
        debug("Succeeded loading plugin " + cls.getName() + " for service: " + service.getName());
        return service.createProviderInstance(cls, pluginname);
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
     * opened classloaders that need to be closed upon expiration of this loader
     */
    private Map<File, Closeable> classLoaders = new HashMap<>();

    /**
     * @return true if the other jar is a copy of the pluginJar based on names returned by generateCachedJarName
     */
    protected boolean isEquivalentPluginJar(File other) {
        String name = other.getName();
        // length of timestamp + 1 for the dash in generateCachedJarName
        int length = CACHED_JAR_TIMESTAMP_FORMAT.length() + 1;
        if (name.length() <= length) {
            log.warn(String.format("%s does not conform to cached plugin jar naming convention.", other));
            return false;
        } else {
            return other.getName().substring(length).equals(pluginJar.getName());
        }
    }

    /**
     * @return a generated name for the pluginJar using the last modified timestamp
     */
    protected String generateCachedJarName() {
        Date mtime = new Date(pluginJar.lastModified());
        return String.format("%s-%s", cachedJarTimestampFormatter.format(mtime), pluginJar.getName());
    }

    /**
     * Creates a single cached version of the pluginJar located within pluginJarCacheDirectory
     * deleting all existing versions of pluginJar
     */
    protected File createCachedJar() throws PluginException {
        File cachedJar;
        try {
            debug(String.format("Scanning %s for cached versions of %s", pluginJarCacheDirectory, pluginJar));
            File[] files = pluginJarCacheDirectory.listFiles();
            if(files == null) {
                throw new PluginException(
                        String.format("Plugin jar cache dir is not a directory or cannot be read: %s",
                                pluginJarCacheDirectory));
            }
            for (File f : files) {
                if (isEquivalentPluginJar(f)) {
                    debug(String.format("Found %s, deleting...", f));
                    if (!f.delete()) {
                        debug(String.format("Could not delete %s", f));
                    }
                }
            }
            cachedJar = new File(pluginJarCacheDirectory, generateCachedJarName());
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
            debug("(loadClass) " + classname + ": " + pluginJar);
            return classCache.get(classname);
        }

        debug(String.format("Deleting dependency lib cache %s",  getFileCacheDir()));
        FileUtils.deleteDir(getFileCacheDir());
        File cachedJar = createCachedJar();
        debug("loadClass! " + classname + ": " + cachedJar);

        final Class<?> cls;

        final URLClassLoader urlClassLoader = getClassLoader(cachedJar);
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

    private URLClassLoader getClassLoader(final File cachedJar) throws PluginException
    {
        ClassLoader parent = JarPluginProviderLoader.class.getClassLoader();
        // if jar manifest declares secondary lib deps, expand lib into cachedir, and setup classloader to use the libs
        Collection<File> extlibs = null;
        try {
            extlibs = extractDependentLibs();
        } catch (IOException e) {
            throw new PluginException("Unable to expand plugin libs: " + e.getMessage(), e);
        }
        try {
            final URL url = cachedJar.toURI().toURL();
            final URL[] urlarray;
            if (null != extlibs && extlibs.size() > 0) {
                final ArrayList<URL> urls = new ArrayList<URL>();
                urls.add(url);
                for (final File extlib : extlibs) {
                    urls.add(extlib.toURI().toURL());
                }
                urlarray = urls.toArray(new URL[urls.size()]);
            } else {
                urlarray = new URL[]{url};
            }
            URLClassLoader loaded = loadLibsFirst
                                    ? LocalFirstClassLoader.newInstance(urlarray, parent)
                                    : URLClassLoader.newInstance(urlarray, parent);
            classLoaders.put(cachedJar, loaded);
            return loaded;
        } catch (MalformedURLException e) {
            throw new PluginException("Error creating classloader for " + cachedJar, e);
        }
    }

    /**
     * Extract the dependent libs and return the extracted jar files
     *
     * @return the collection of extracted files
     */
    private Collection<File> extractDependentLibs() throws IOException {
        final Attributes attributes = getMainAttributes();
        if (null == attributes) {
            debug("no manifest attributes");
            return null;
        }

        final ArrayList<File> files = new ArrayList<File>();
        final String libs = attributes.getValue(RUNDECK_PLUGIN_LIBS);
        if (null != libs) {
            debug("jar libs listed: " + libs + " for file: " + pluginJar);

            final String[] libsarr = libs.split(" ");
            final File cachedir = getFileCacheDir();
            extractJarContents(libsarr, cachedir);
            for (final String s : libsarr) {
                files.add(new File(cachedir, s));
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
        return name.substring(0, name.lastIndexOf("."));
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

    /**
     * Remove any cache dir for the file
     */
    private synchronized boolean removeScriptPluginCache() {
        final File fileExpandedDir = getFileCacheDir();
        if (null != fileExpandedDir && fileExpandedDir.exists()) {
            debug("removeScriptPluginCache: " + fileExpandedDir);
            return FileUtils.deleteDir(fileExpandedDir);
        }
        return true;
    }

    /**
     * Expire the loader cache item
     */
    public void expire() {
        debug("expire jar provider loader for: " + pluginJar);
        removeScriptPluginCache();
        classCache.clear();
        //close loaders
        for (File file : classLoaders.keySet()) {
            try {
                debug("expire classLoaders for: " + file);
                classLoaders.remove(file).close();
            } catch (IOException e) {
                e.printStackTrace();
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
}
