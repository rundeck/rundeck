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
* CommandInterpreterService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:06 PM
* 
*/
package com.dtolabs.rundeck.core.execution.commands;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;

/**
 * CommandInterpreterService is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class CommandInterpreterService extends BaseProviderRegistryService<CommandInterpreter> {
    public static final String SERVICE_NAME = "CommandInterpreter";

    public CommandInterpreterService(final Framework framework) {
        super(framework);

        //TODO: use plugin framework to configure available FileCopier implementations.
        resetDefaultProviders();
    }
    public void resetDefaultProviders(){
        registry.put(ExecCommandInterpreter.SERVICE_IMPLEMENTATION_NAME, ExecCommandInterpreter.class);
        registry.put(ScriptFileCommandInterpreter.SERVICE_IMPLEMENTATION_NAME, ScriptFileCommandInterpreter.class);
        instanceregistry.remove(ExecCommandInterpreter.SERVICE_IMPLEMENTATION_NAME);
        instanceregistry.remove(ScriptFileCommandInterpreter.SERVICE_IMPLEMENTATION_NAME);
    }

    public CommandInterpreter getInterpreterForExecutionItem(final ExecutionItem item) throws
        ExecutionServiceException {
        return providerOfType(item.getType());
    }

    public static CommandInterpreterService getInstanceForFramework(Framework framework) {
        if (null == framework.getService(SERVICE_NAME)) {
            final CommandInterpreterService service = new CommandInterpreterService(framework);
            framework.setService(SERVICE_NAME, service);
            return service;
        }
        return (CommandInterpreterService) framework.getService(SERVICE_NAME);
    }


    public String getName() {
        return SERVICE_NAME;
    }
}
