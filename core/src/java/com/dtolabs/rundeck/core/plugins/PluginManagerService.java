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
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.plugins.metadata.PluginDef;
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import com.dtolabs.rundeck.core.utils.ZipUtil;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * PluginManagerService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginManagerService implements FrameworkSupportService {
    private static final Logger log = Logger.getLogger(PluginManagerService.class.getName());
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
        debug("loading plugins for services...");
        loadExplicitPluginConfig();
        scanAndLoadJarPlugins();
        scanAndLoadZipScriptPlugins();
    }

    /**
     * Load any jars in the libext dir of RDECK_BASE and look for jar metadata to load classes as plugins
     */
    private void scanAndLoadJarPlugins() {
        final File libext = new File(framework.getBaseDir(), "libext");
        debug("scanAndLoadJarPlugins dir: " + libext.getAbsolutePath());
        if (!libext.exists() || !libext.isDirectory()) {
            return;
        }
        final String[] list = libext.list(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.endsWith(".jar");
            }
        });
        debug("libext dir: " + list.length);
        HashMap<File, String[]> jartoload = new HashMap<File, String[]>();
        for (final String s : list) {
            //try reading jar
            final File jar = new File(libext, s);
            debug("test file dir: " + jar.getAbsolutePath());
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
                    debug("has no metadata: " + mainAttributes);
                }
                jarInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        debug("Will load plugin jars: " + jartoload);
        loadJarPlugins(jartoload);
    }

    private void debug(String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    /**
     * Load any jars in the libext dir of RDECK_BASE and look for jar metadata to load classes as plugins
     */
    private void scanAndLoadZipScriptPlugins() {
        final File libext = new File(framework.getBaseDir(), "libext");
        debug("scanAndLoadZipScriptPlugins dir: " + libext.getAbsolutePath());
        if (!libext.exists() || !libext.isDirectory()) {
            return;
        }
        final String[] list = libext.list(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.endsWith("-plugin.zip");
            }
        });
        debug("found zip plugins: " + list.length);
        final HashMap<File, loadedMeta> ziptoload = new HashMap<File, loadedMeta>();
        for (final String s : list) {
            //try reading jar
            final File jar = new File(libext, s);
            final String basename = s.substring(0, s.lastIndexOf("."));
            debug("test zip: " + jar.getAbsolutePath() + ", basename: " + basename);
            try {
                final ZipInputStream zipinput = new ZipInputStream(new FileInputStream(jar));
                ZipEntry nextEntry = zipinput.getNextEntry();
                boolean found = false;
                while (null != nextEntry) {
                    if (!nextEntry.isDirectory() && (nextEntry.getName().equals("plugin.yaml") || nextEntry.getName()
                        .equals(basename + "/plugin.yaml"))) {
                        debug("Found metadata: " + nextEntry.getName());
                        final PluginMeta metadata = loadMetadataYaml(zipinput);

                        ziptoload.put(jar, new loadedMeta(metadata, nextEntry.getName()
                            .equals(basename + "/plugin.yaml"), basename));
                        found = true;
                        break;
                    }
                    nextEntry = zipinput.getNextEntry();
                }
                if (!found) {
                    debug("Skipping, Found no plugin.yaml within: " + jar.getAbsolutePath());
                }
                zipinput.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        debug("Will load zip plugins: " + ziptoload);
        loadScriptPlugins(ziptoload);
    }

    private static class loadedMeta {
        PluginMeta pluginMeta;
        boolean hasTopdir;
        String topdirname;

        private loadedMeta(PluginMeta pluginMeta, boolean hasTopdir, String topdirname) {
            this.pluginMeta = pluginMeta;
            this.hasTopdir = hasTopdir;
            this.topdirname = topdirname;
        }
    }

    private PluginMeta loadMetadataYaml(final InputStream stream) {
        final Constructor myconst = new Constructor(PluginMeta.class);
        Yaml yaml = new Yaml(myconst);
//        TypeDescription carDescription = new TypeDescription(PluginMeta.class);
//        carDescription.putListPropertyType("wheels", Wheel.class);
//        constructor.addTypeDefinition(carDescription);
        final Object load = yaml.load(stream);

        return (PluginMeta) load;
    }

    /**
     * For each file and list of plugins defined, iterate over plugins, validate definitions, expand the file into a
     * directory, create a ScriptPlugin instance, attempt to register it to the appropriate service
     */
    private void loadScriptPlugins(final Map<File, loadedMeta> scriptPlugins) {
        Map<File, File> expanded = new HashMap<File, File>();
        for (final File file : scriptPlugins.keySet()) {
            final loadedMeta loadedMeta = scriptPlugins.get(file);
            final PluginMeta pluginList = loadedMeta.pluginMeta;
            for (final PluginDef pluginDef : pluginList.getPluginDefs()) {
                //validate plugindef
                try {
                    validateScriptPluginDef(pluginDef);
                    if (!expanded.containsKey(file)) {
                        debug("expanding plugin contents: " + file);
                        final File file1 = expandScriptPlugin(file, loadedMeta);
                        expanded.put(file, file1);
                    }
                    File dir = expanded.get(file);
                    //set executable bit for script-file of the provider
                    File script = new File(dir, pluginDef.getScriptFile());
                    ScriptfileUtils.setExecutePermissions(script);
                    loadPluginDef(pluginDef, dir, file);
                } catch (PluginException e) {
                    System.err.println(
                        "Failed loading plugindef: " + pluginDef.getName() + " for plugin " + file.getAbsolutePath());
                    e.printStackTrace(System.err);
                } catch (IOException e) {
                    System.err.println(
                        "Failed expanding plugin " + file.getAbsolutePath());
                    e.printStackTrace(System.err);
                }
            }
        }

    }

    /**
     * Expand jar file into plugin cache dir
     *
     * @param file jar file
     * @param meta loaded metadata
     *
     * @return cache dir for the contents of the plugin zip
     */
    private File expandScriptPlugin(final File file, final loadedMeta meta) throws IOException {
        File basedir = new File(framework.getBaseDir(), "libext/cache");
        if (!basedir.exists()) {
            basedir.mkdirs();
        }
        final String name = file.getName();
        String basename = name.substring(0, name.lastIndexOf("."));
        String prefix = "contents/";
        if (meta.hasTopdir) {
            prefix = meta.topdirname + "/" + prefix;
        }
        File jardir = new File(basedir, basename);
        if (!jardir.exists()) {
            jardir.mkdir();
        }


        ZipUtil.extractZip(file.getAbsolutePath(), jardir, prefix, prefix);

        return jardir;
    }

    /**
     * Load a plugindef from the basedir specified. create a ScriptPlugin instance, attempt to register it to the
     * appropriate service
     */
    private void loadPluginDef(final PluginDef pluginDef, final File dir, final File file) throws PluginException {
        final ScriptPluginProvider provider = new ScriptPluginProviderImpl(pluginDef, file, dir);
        final PluggableService service = (PluggableService) framework.getService(provider.getService());
        service.registerScriptProvider(provider);
        debug("loaded plugin def" + pluginDef);

    }

    private void validateScriptPluginDef(final PluginDef pluginDef) throws PluginException {
        if (null == pluginDef.getName() || "".equals(pluginDef.getName())) {
            throw new PluginException("Script plugin missing name");
        }
        if (null == pluginDef.getService() || "".equals(pluginDef.getService())) {
            throw new PluginException("Script plugin missing service");
        }
        /*
        //TODO: validate plugin-type when multiple types are supported
        if (null == pluginDef.getPluginType() || "".equals(pluginDef.getPluginType())) {
            throw new PluginException("Script plugin missing plugin-type");
        }
        if (! "script".equals(pluginDef.getPluginType())) {
            throw new PluginException("plugin missing has invalid plugin-type");
        }
        */
        if (null == pluginDef.getScriptFile() || "".equals(pluginDef.getScriptFile())) {
            throw new PluginException("Script plugin missing script-file");
        }

        //make sure service is pluggable service and is script pluggable
        final FrameworkSupportService service = framework.getService(pluginDef.getService());
        if (!(service instanceof PluggableService)) {
            throw new PluginException(
                "Service '" + pluginDef.getService() + "' specified for script plugin '" + pluginDef.getName()
                + "' is not valid: unsupported");
        }
        PluggableService pservice = (PluggableService) service;
        if (!pservice.isScriptPluggable()) {
            throw new PluginException(
                "Service '" + pluginDef.getService() + "' specified for script plugin '" + pluginDef.getName()
                + "' is not valid: unsupported");
        }
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
                        loadProviderByClassname(classname, urlClassLoader);
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
                    loadProviderByClassname(classname, this.getClass().getClassLoader());
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
    void loadProviderByClassname(final String classname, final ClassLoader classLoader) throws PluginException {
        debug("Try loading provider " + classname);

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

        if (!usedservice.isValidProviderClass(cls)) {
            throw new PluginException(
                "Class " + classname + " was not a valid plugin class for service: " + usedservice.getName());
        }
        usedservice.registerProviderClass(cls, pluginname);

        debug("Succeeded loading plugin " + classname + " for service: " + usedservice.getName());

    }
}
