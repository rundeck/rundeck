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
package com.dtolabs.rundeck.core.execution.orchestrator;


import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkSupportService;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableService;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;

/**
 * OrchestratorService loads OrchestratorPlugins
 *
 * @author Ashley Taylor
 */
public class OrchestratorService extends FrameworkPluggableProviderService<OrchestratorPlugin>
    implements FrameworkSupportService, DescribableService {

    public static final String SERVICE_NAME = ServiceNameConstants.Orchestrator;

    OrchestratorService(final Framework framework) {
        super(SERVICE_NAME, framework, OrchestratorPlugin.class);
    }

    @Override
    public boolean isScriptPluggable() {
        
        return false;
    }

    public Description getDescription(String name){
        for(Description description: listDescriptions()){
            if(description.getName().equals(name)){
                return description;
            }
        }
        return null;
    }
    
    public static OrchestratorService getInstanceForFramework(final Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final OrchestratorService service = new OrchestratorService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (OrchestratorService) framework.getService(SERVICE_NAME);

    }
}
