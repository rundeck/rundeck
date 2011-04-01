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

import java.util.*;

/**
 * PluginManagerService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginManagerService implements FrameworkSupportService {
    private static final String SERVICE_NAME = "PluginManager";
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

    public void loadPlugins(Collection<String> names) {
//        System.err.println("loading plugins for services...");

        //look for plugin classnames with simple framework property
        //iterate through the services, try to load plugins if the service is a PluggableService
        for (final String name : names) {
            final FrameworkSupportService service = framework.getService(name);
            if (service instanceof PluggableService) {
                final PluggableService pservice = (PluggableService) service;
//                System.err.println("check service: " + pservice.getName());
                if (framework.getPropertyLookup().hasProperty("framework.plugins." + name + ".classnames")) {
//                    System.err.println("Has classnames: " + pservice.getName());
                    final String property = framework.getProperty("framework.plugins." + name + ".classnames");
                    for (int i = 0 ; i < property.split(",").length ; i++) {
                        final String classname = property.split(",")[i];
                        try {
                            loadPluginForServiceByClassname(pservice, classname);
                        } catch (Exception e) {
//                            System.err.println("Failed to load plugin for " + name + " with classname: " + classname);
                            e.printStackTrace(System.err);
                        }
                    }
                }
            }
        }
    }

    /**
     * Attempt to load the classname and register the class as a provider for the given service
     */
    void loadPluginForServiceByClassname(final PluggableService service, final String classname) throws
        PluginException {
//        System.err.println("Try loading plugin " + classname + " for service: " + service.getName());

        if (null == classname) {
            throw new IllegalArgumentException("A null java class name was specified.");
        }
        final Object auth;
        final Class cls;
        try {
            cls = Class.forName(classname);
        } catch (ClassNotFoundException e) {
            throw new PluginException("Class not found: " + classname, e);
        } catch (Throwable t) {
            throw new PluginException("Error loading class: " + classname, t);
        }
        if(!service.isValidPluginClass(cls)) {
            throw new PluginException(
                "Class " + classname + " was not a valid plugin class for service: " + service.getName());
        }
        //try to get plugin provider name
        String pluginname=null;
        if(cls.isAnnotationPresent(Plugin.class)){
            final Plugin annotation = (Plugin) cls.getAnnotation(Plugin.class);
            pluginname=annotation.name();
            if(null==pluginname || "".equals(pluginname)){
                throw new PluginException(
                    "Plugin annotation name cannot be empty for the class: " + classname + ", for service " + service
                        .getName());
            }
        }else{
            throw new PluginException(
                "No Plugin annotation was found for the class: " + classname + ", for service " + service
                    .getName());
        }
        service.registerPluginClass(cls, pluginname);

//        System.err.println("Succeeded loading plugin " + classname + " for service: " + service.getName());

    }
}
