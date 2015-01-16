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
import com.dtolabs.rundeck.core.execution.service.MissingProviderException;
import com.dtolabs.rundeck.core.execution.service.ProviderLoaderException;
import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PluginManagerService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PluginManagerService implements FrameworkSupportService, ServiceProviderLoader {
    private static final Logger log = Logger.getLogger(PluginManagerService.class.getName());
    public static final String SERVICE_NAME = "PluginManager";
    private static final Map<File, PluginManagerService> managerRegistry = new HashMap<File, PluginManagerService>();

    private File extdir;
    private File cachedir;
    private final PluginCache cache;

    /**
     * Create a PluginManagerService for the given directory and cache directory
     * @param extdir plugin dir
     * @param cachedir cache dir
     */
    public PluginManagerService(final File extdir, final File cachedir) {
        this.extdir = extdir;
        this.cachedir = cachedir;
        final FileCache<ProviderLoader> filecache = new FileCache<ProviderLoader>();
        cache = new FilePluginCache(filecache);
        final int rescanInterval = 5000;//TODO: use framework property to set interval
        cache.addScanner(new JarPluginScanner(extdir, cachedir, filecache, rescanInterval));
        cache.addScanner(new ScriptPluginScanner(extdir, cachedir, filecache, rescanInterval));
        log.debug("Create PluginManagerService");
    }


    public String getName() {
        return SERVICE_NAME;
    }

    public static PluginManagerService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {

            final PluginManagerService instanceForExtDir = getInstanceForExtDir(framework.getLibextDir(),
                framework.getLibextCacheDir());
            framework.setService(SERVICE_NAME, instanceForExtDir);
            return instanceForExtDir;
        }
        return (PluginManagerService) framework.getService(SERVICE_NAME);
    }

    public synchronized static PluginManagerService getInstanceForExtDir(final File libextDir, final File cachedir) {
        if (null == managerRegistry.get(libextDir)) {
            final PluginManagerService service = new PluginManagerService(libextDir, cachedir);
            managerRegistry.put(libextDir, service);
        }

        return managerRegistry.get(libextDir);
    }

    public synchronized List<ProviderIdent> listProviders() {
        return cache.listProviders();
    }

    public synchronized <T> T loadProvider(final PluggableService<T> service, final String providerName) throws ProviderLoaderException {
        final ProviderIdent ident = new ProviderIdent(service.getName(), providerName);
        final ProviderLoader loaderForIdent = cache.getLoaderForIdent(ident);
        if (null == loaderForIdent) {
            throw new MissingProviderException("No matching plugin found", service.getName(), providerName);
        }
        final T load = loaderForIdent.load(service, providerName);
        if (null != load) {
            return load;
        } else {
            throw new ProviderLoaderException(
                "Unable to load provider: " + providerName + ", for service: " + service.getName(), service.getName(),
                providerName);
        }
    }


}
