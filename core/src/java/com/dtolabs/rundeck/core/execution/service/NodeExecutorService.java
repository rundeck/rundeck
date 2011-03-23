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
* CommandExecutorFactory.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 3:28 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.impl.jsch.JschNodeExecutor;
import com.dtolabs.rundeck.core.execution.impl.local.LocalNodeExecutor;

/**
 * CommandExecutorFactory is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeExecutorService extends NodeSpecifiedService<NodeExecutor> {
    private static final String SERVICE_NAME = "NodeExecutor";
    private static final String SERVICE_FILECOPIER_DEFAULT_TYPE = "service.nodeexec.default.type";
    private static final String NODE_SERVICE_SPECIFIER_ATTRIBUTE = "exec-service";
    private static final String LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE = "local-exec-service";

    public String getName() {
        return SERVICE_NAME;
    }

    NodeExecutorService(Framework framework) {
        super(framework);

        //TODO: use plugin framework to configure available FileCopier implementations.
        registry.put(JschNodeExecutor.SERVICE_PROVIDER_TYPE, JschNodeExecutor.class);
        registry.put(LocalNodeExecutor.SERVICE_PROVIDER_TYPE, LocalNodeExecutor.class);

    }

    @Override
    protected String getDefaultProviderNameForNode(INodeEntry node) {
        if (framework.isLocalNode(node)) {
            return LocalNodeExecutor.SERVICE_PROVIDER_TYPE;
        }
        return framework.getPropertyLookup().hasProperty(SERVICE_FILECOPIER_DEFAULT_TYPE)
               ? framework.getPropertyLookup().getProperty(SERVICE_FILECOPIER_DEFAULT_TYPE)
               : JschNodeExecutor.SERVICE_PROVIDER_TYPE;
    }

    public static NodeExecutorService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final NodeExecutorService service = new NodeExecutorService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (NodeExecutorService) framework.getService(SERVICE_NAME);
    }

    @Override
    protected String getServiceProviderNodeAttributeForNode(INodeEntry node) {
        if (framework.isLocalNode(node)) {
            return LOCAL_NODE_SERVICE_SPECIFIER_ATTRIBUTE;
        }
        return NODE_SERVICE_SPECIFIER_ATTRIBUTE;
    }
}
