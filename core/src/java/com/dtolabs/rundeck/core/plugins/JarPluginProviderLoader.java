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

import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * JarPluginProviderLoader can load jar plugin files as provider instances.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class JarPluginProviderLoader implements ProviderLoader {
    private static Logger log = Logger.getLogger(JarPluginProviderLoader.class.getName());
    public static final String RUNDECK_PLUGIN_ARCHIVE = "Rundeck-Plugin-Archive";
    public static final String RUNDECK_PLUGIN_CLASSNAMES = "Rundeck-Plugin-Classnames";
    public static final String JAR_PLUGIN_VERSION = "1.0";
    public static final String RUNDECK_PLUGIN_VERSION = "Rundeck-Plugin-Version";
    private final File file;
    private Map<ProviderIdent, Class> pluginProviderDefs =
        new HashMap<ProviderIdent, Class>();

    public JarPluginProviderLoader(final File file) {
        if (null == file) {
            throw new NullPointerException("file");
        }
        if(!file.exists()) {
            throw new IllegalArgumentException("File does not exist: " + file);
        }
        if(!file.isFile()) {
            throw new IllegalArgumentException("Not a file: " + file);
        }
        this.file = file;
    }

    /**
     * Load provider instance for the service
     */
    public synchronized <T> T load(final PluggableService<T> service, final String providerName) throws
        ProviderLoaderException {
        final ProviderIdent ident = new ProviderIdent(service.getName(), providerName);
        debug("loadInstance for " + ident + ": " + file);

        if (null == pluginProviderDefs.get(ident)) {
            final String[] strings = getClassnames();
            for (final String classname : strings) {
                final Class cls;
                try {
                    cls = loadClass(classname, file);
                    if (matchesProviderDeclaration(ident, cls)) {
                        pluginProviderDefs.put(ident, cls);
                    }
                } catch (PluginException e) {
                    log.warn(
                        "Failed to verify class from " + file + ": classname: " + classname + ": " + e.getMessage());
                }
            }
        }
        final Class cls = pluginProviderDefs.get(ident);
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
    static boolean matchesProviderDeclaration(final ProviderIdent ident, final Class cls) throws
        PluginException {
        final Plugin annotation = getPluginMetadata(cls);
        return ident.getFirst().equals(annotation.service()) &&
               ident.getSecond().equals(annotation.name());
    }

    Attributes mainAttributes;

    /**
     * Get the declared list of provider classnames for the file
     */
    public String[] getClassnames() {
        if (null == mainAttributes) {
            mainAttributes = getJarMainAttributes(file);
        }

        final String value = mainAttributes.getValue(RUNDECK_PLUGIN_CLASSNAMES);
        if (null == value) {
            return null;
        }
        return value.split(",");
    }

    /**
     * Get the main attributes for the jar file
     */
    private static Attributes getJarMainAttributes(final File file) {
        debug("getJarMainAttributes: " + file);

        try {
            final JarInputStream jarInputStream;
            final Attributes mainAttributes;
            jarInputStream = new JarInputStream(new FileInputStream(file));
            final Manifest manifest = jarInputStream.getManifest();
            mainAttributes = manifest.getMainAttributes();
            jarInputStream.close();
            return mainAttributes;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Attempt to create an instance of thea provider for the given service
     *
     * @param cls
     */
    @SuppressWarnings ("unchecked")
    static <T> T createProviderForClass(final PluggableService<T> service, final Class cls) throws
        PluginException, ProviderCreationException {
        debug("Try loading provider " + cls.getName());

        final Plugin annotation = getPluginMetadata(cls);

        final String pluginname = annotation.name();

        if (!service.isValidProviderClass(cls)) {
            throw new PluginException(
                "Class " + cls.getName() + " was not a valid plugin class for service: " + service
                    .getName());
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
    @SuppressWarnings ("unchecked")
    static Plugin getPluginMetadata(final Class cls) throws PluginException {
        //try to get plugin provider name
        final String pluginname;
        if (!cls.isAnnotationPresent(Plugin.class)) {
            throw new PluginException(
                "No Plugin annotation was found for the class: " + cls.getName());
        }

        final Plugin annotation = (Plugin) cls.getAnnotation(Plugin.class);
        pluginname = annotation.name();
        if (null == pluginname || "".equals(pluginname)) {
            throw new PluginException(
                "Plugin annotation 'name' cannot be empty for the class: " + cls.getName());
        }
        //get service name from annotation
        final String servicename = annotation.service();
        if (null == servicename || "".equals(servicename)) {
            throw new PluginException(
                "Plugin annotation 'service' cannot be empty for the class: " + cls.getName());
        }
        return annotation;
    }

    private Map<String, Class> classCache = new HashMap<String, Class>();

    /**
     * Load a class from the jar file by name
     */
    private Class loadClass(final String classname, final File file) throws PluginException {
        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        if (null != classCache.get(classname)) {
            debug("(loadClass) " + classname + ": " + file);
            return classCache.get(classname);
        }
        debug("loadClass! " + classname + ": " + file);
        final ClassLoader parent = JarPluginProviderLoader.class.getClassLoader();
        final Class cls;
        try {
            final URL url = file.toURI().toURL();
            final URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[]{url}, parent);
            cls = Class.forName(classname, true, urlClassLoader);
            classCache.put(classname, cls);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Class not found: " + classname, e);
        } catch (MalformedURLException e) {
            throw new PluginException("Error loading class: " + classname, e);
        } catch (Throwable t) {
            throw new PluginException("Error loading class: " + classname, t);
        }
        return cls;
    }

    /**
     * Return true if the file has a class that provides the ident.
     */
    public synchronized boolean isLoaderFor(final ProviderIdent ident) {
        final String[] strings = getClassnames();
        for (final String classname : strings) {
            try {
                if (matchesProviderDeclaration(ident, loadClass(classname, file))) {
                    return true;
                }
            } catch (PluginException e) {
                e.printStackTrace();
            }
        }
        return false;
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
        if (!file.equals(that.file)) {
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
        int result = file.hashCode();
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
            final JarInputStream jarInputStream = new JarInputStream(new FileInputStream(file));
            final Manifest manifest = jarInputStream.getManifest();
            if(null==manifest){
                jarInputStream.close();
                return false;
            }
            final Attributes mainAttributes = manifest.getMainAttributes();
            validateJarManifest(mainAttributes);
            jarInputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace(System.err);
            log.warn(e.getMessage() + ": " + file.getAbsolutePath());
            return false;
        } catch (InvalidManifestException e) {
            e.printStackTrace(System.err);

            log.warn(e.getMessage() + ": " + file.getAbsolutePath());
            return false;
        }
    }

    /**
     * Validate whether the jar file has a valid manifest, throw exception if invalid
     */
    static void validateJarManifest(final Attributes mainAttributes) throws InvalidManifestException {
        final String value1 = mainAttributes.getValue(RUNDECK_PLUGIN_ARCHIVE);
        final String plugvers = mainAttributes.getValue(RUNDECK_PLUGIN_VERSION);
        final String plugclassnames = mainAttributes.getValue(
            RUNDECK_PLUGIN_CLASSNAMES);
        if (null == value1) {
            throw new InvalidManifestException(
                "Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_ARCHIVE);
        } else if (!"true".equals(value1)) {
            throw new InvalidManifestException(
                RUNDECK_PLUGIN_ARCHIVE + " was not 'true': " + value1);
        }
        if (null == plugvers) {
            throw new InvalidManifestException("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_VERSION);
        } else if (!JAR_PLUGIN_VERSION.equals(plugvers)) {
            throw new InvalidManifestException(
                "Unssupported plugin version: " + RUNDECK_PLUGIN_VERSION + ": " + plugvers);
        }
        if (null == plugclassnames) {
            throw new InvalidManifestException(
                "Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_CLASSNAMES);
        }
    }

    static class InvalidManifestException extends Exception {
        public InvalidManifestException(String s) {
            super(s);
        }
    }
}
