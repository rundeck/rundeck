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
* PluginManagerService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 2:00 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * PluginManagerService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginManagerService implements FrameworkSupportService {
    private static final String SERVICE_NAME = "PluginManager";
    public static final String RUNDECK_PLUGIN_ARCHIVE = "Rundeck-Plugin-Archive";
    public static final String RUNDECK_PLUGIN_CLASSNAMES = "Rundeck-Plugin-Classnames";
    private Framework framework;

    public PluginManagerService(Framework framework) {
        this.framework = framework;
    }

    public String getName() {
        return SERVICE_NAME;
    }

    public static PluginManagerService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final PluginManagerService service = new PluginManagerService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (PluginManagerService) framework.getService(SERVICE_NAME);
    }

    public void loadPlugins() {
//        System.err.println("loading plugins for services...");
        loadExplicitPluginConfig();
        scanAndLoadJarPlugins();
    }

    /**
     * Load any jars in the libext dir of RDECK_BASE and look for jar metadata to load classes as plugins
     */
    private void scanAndLoadJarPlugins() {
        final File libext = new File(framework.getBaseDir(), "libext");
//        System.err.println("libext dir: "+libext.getAbsolutePath());
        if (!libext.exists() || !libext.isDirectory()) {
            return;
        }
        final String[] list = libext.list(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.endsWith(".jar");
            }
        });
//        System.err.println("libext dir: " + list.length);
        HashMap<File, String[]> jartoload = new HashMap<File, String[]>();
        for (int i = 0 ; i < list.length ; i++) {
            String s = list[i];
            //try reading jar
            File jar = new File(libext, s);
//            System.err.println("test file dir: " + jar.getAbsolutePath());
            try {
                final JarInputStream jarInputStream = new JarInputStream(new FileInputStream(jar));
                final Manifest manifest = jarInputStream.getManifest();
                final Attributes mainAttributes = manifest.getMainAttributes();
                if (null != mainAttributes.getValue(RUNDECK_PLUGIN_ARCHIVE) && null != mainAttributes.getValue(
                    RUNDECK_PLUGIN_CLASSNAMES)) {

                    final String value = mainAttributes.getValue(RUNDECK_PLUGIN_CLASSNAMES);
//                    System.err.println("Has metadata: " + RUNDECK_PLUGIN_ARCHIVE);
//                    System.err.println("Has metadata: " + RUNDECK_PLUGIN_CLASSNAMES + ": " + value);
                    jartoload.put(jar, value.split(","));
                } else {
//                    System.err.println("has no metadata: " + mainAttributes);
                }
                jarInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
//        System.err.println("libext dir: " + libext.getAbsolutePath());
//        System.err.println("Will load plugin jars: " + jartoload);
        loadJarPlugins(jartoload);
    }

    /**
     * Load each jar file and register plugin classnames specified
     */
    private void loadJarPlugins(final HashMap<File, String[]> jartoload) {
        final ClassLoader parent = getClass().getClassLoader();
        for (final File file : jartoload.keySet()) {
            final String[] strings = jartoload.get(file);
            try {
                final URL url = file.toURI().toURL();
                final URLClassLoader urlClassLoader = URLClassLoader.newInstance(new URL[]{url}, parent);
                for (int i = 0 ; i < strings.length ; i++) {
                    String classname = strings[i];
                    try {
                        loadPluginByClassname(classname, urlClassLoader);
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                    }
                }
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadExplicitPluginConfig() {
        //look for plugin classnames with simple framework property
        //iterate through the services, try to load plugins if the service is a PluggableService
        if (framework.getPropertyLookup().hasProperty("framework.plugins.classnames")) {
//                    System.err.println("Has classnames: " + pservice.getName());
            final String property = framework.getProperty("framework.plugins.classnames");
            for (int i = 0 ; i < property.split(",").length ; i++) {
                final String classname = property.split(",")[i];
                try {
                    loadPluginByClassname(classname, this.getClass().getClassLoader());
                } catch (Exception e) {
//                            System.err.println("Failed to load plugin for " + name + " with classname: " + classname);
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Attempt to load the classname and register the class as a provider for the given service
     *
     * @param classname   classname to load
     * @param classLoader classloader to user
     */
    void loadPluginByClassname(final String classname, final ClassLoader classLoader) throws PluginException {
//        System.err.println("Try loading plugin " + classname + " for service: " + service.getName());

        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        final Object auth;
        final Class cls;
        try {
            cls = Class.forName(classname, true, classLoader);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Class not found: " + classname, e);
        } catch (Throwable t) {
            throw new PluginException("Error loading class: " + classname, t);
        }
        //try to get plugin provider name
        String pluginname = null;
        if (!cls.isAnnotationPresent(Plugin.class)) {
            throw new PluginException(
                "No Plugin annotation was found for the class: " + classname);
        }

        final Plugin annotation = (Plugin) cls.getAnnotation(Plugin.class);
        pluginname = annotation.name();
        if (null == pluginname || "".equals(pluginname)) {
            throw new PluginException(
                "Plugin annotation 'name' cannot be empty for the class: " + classname);
        }
        //get service name from annotation
        final String servicename = annotation.service();
        if (null == servicename || "".equals(servicename)) {
            throw new PluginException(
                "Plugin annotation 'service' cannot be empty for the class: " + classname);
        }
        final FrameworkSupportService service1 = framework.getService(servicename);
        if (null == service1) {
            throw new PluginException(
                "Class " + classname + " did not specify a valid service name: " + servicename + ": no such service");
        }
        if (!(service1 instanceof PluggableService)) {
            throw new PluginException(
                "Class " + classname + " did not specify a valid service name: " + servicename + ": not allowed");
        }
        PluggableService usedservice = (PluggableService) service1;

        if (!usedservice.isValidPluginClass(cls)) {
            throw new PluginException(
                "Class " + classname + " was not a valid plugin class for service: " + usedservice.getName());
        }
        usedservice.registerPluginClass(cls, pluginname);

        //System.err.println("Succeeded loading plugin " + classname + " for service: " + usedservice.getName());

    }
}
