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
 * JarFileProviderLoader is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class JarFileProviderLoader implements FileProviderLoader {
    private static Logger log = Logger.getLogger(JarFileProviderLoader.class.getName());
    private final File file;
    private Map<ProviderIdent, Class> pluginProviderDefs =
        new HashMap<ProviderIdent, Class>();

    public JarFileProviderLoader(final File file) {
        this.file = file;
        debug("create JarFileProviderLoader for: " + file);
    }

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


    private static boolean matchesProviderDeclaration(final ProviderIdent ident, final Class cls) throws
        PluginException {
        final Plugin annotation = getPluginMetadata(cls);
        return ident.getFirst().equals(annotation.service()) &&
               ident.getSecond().equals(annotation.name());
    }

    Attributes mainAttributes;

    public String[] getClassnames() {
        if (null == mainAttributes) {
            mainAttributes = getJarMainAttributes(file);
        }

        return mainAttributes.getValue(JarPluginDirScanner.RUNDECK_PLUGIN_CLASSNAMES).split(",");
    }

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
     * Attempt to load the classname and register the class as a provider for the given service
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

    @SuppressWarnings ("unchecked")
    private static Plugin getPluginMetadata(final Class cls) throws PluginException {
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

    private Class loadClass(final String classname, final File file) throws PluginException {
        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        if (null != classCache.get(classname)) {
            debug("(loadClass) " + classname + ": " + file);
            return classCache.get(classname);
        }
        debug("loadClass! " + classname + ": " + file);
        final ClassLoader parent = JarFileProviderLoader.class.getClassLoader();
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

        final JarFileProviderLoader that = (JarFileProviderLoader) o;

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
}
