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
* FileCopierService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:05 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschScpFileCopier;
import com.dtolabs.rundeck.core.execution.impl.local.LocalFileCopier;
import com.dtolabs.rundeck.core.plugins.PluggableService;
import com.dtolabs.rundeck.core.plugins.PluginException;

/**
 * FileCopierService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileCopierService extends NodeSpecifiedService<FileCopier> implements PluggableService {
    private static final String SERVICE_NAME = "FileCopier";
    private static final String SERVICE_FILECOPIER_DEFAULT_TYPE = "service.filecopier.default.type";
    public static final String REMOTE_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "remote-file-copy-service";
    public static final String LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "local-file-copy-service";

    public String getName() {
        return SERVICE_NAME;
    }

    FileCopierService(Framework framework) {
        super(framework);

        //TODO: use plugin framework to configure available FileCopier implementations.
        registry.put(JschScpFileCopier.SERVICE_PROVIDER_TYPE, JschScpFileCopier.class);
        registry.put(LocalFileCopier.SERVICE_PROVIDER_TYPE, LocalFileCopier.class);

    }

    @Override
    protected String getDefaultProviderNameForNode(INodeEntry node) {
        if (framework.isLocalNode(node)) {
            return LocalFileCopier.SERVICE_PROVIDER_TYPE;
        }
        return framework.getPropertyLookup().hasProperty(SERVICE_FILECOPIER_DEFAULT_TYPE)
               ? framework.getPropertyLookup().getProperty(SERVICE_FILECOPIER_DEFAULT_TYPE)
               : JschScpFileCopier.SERVICE_PROVIDER_TYPE;
    }

    public static FileCopierService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final FileCopierService service = new FileCopierService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (FileCopierService) framework.getService(SERVICE_NAME);
    }

    @Override
    protected String getServiceProviderNodeAttributeForNode(INodeEntry node) {
        if (framework.isLocalNode(node)) {
            return LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE;
        }
        return REMOTE_NODE_SERVICE_SPECIFIER_ATTRIBUTE;
    }

    public boolean isValidPluginClass(final Class clazz) {
        return FileCopier.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
    }

    public void registerPluginClass(Class clazz, String name) throws PluginException {
        if (!isValidPluginClass(clazz)) {
            throw new PluginException("Invalid plugin class: " + clazz.getName());
        }
        final Class<? extends FileCopier> pluginclazz = (Class<FileCopier>) clazz;
        registry.put(name, pluginclazz);
    }
}
