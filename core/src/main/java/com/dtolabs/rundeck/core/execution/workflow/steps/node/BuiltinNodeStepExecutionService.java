/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/*
* BuiltinNodeStepExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/12/12 5:07 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ExecNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptFileNodeStepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.impl.ScriptURLNodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;


/**
 * BuiltinNodeStepExecutionService provides built in NodeStepExecutor implementations
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class BuiltinNodeStepExecutionService extends BaseProviderRegistryService<NodeStepExecutor>{

    String name;

    BuiltinNodeStepExecutionService(final Framework framework, final String name) {
        super(framework);
        this.name = name;
        resetDefaultProviders();
    }

    public void resetDefaultProviders() {
        registry.put(ExecNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ExecNodeStepExecutor.class);
        registry.put(ScriptFileNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ScriptFileNodeStepExecutor.class);
        registry.put(ScriptURLNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME, ScriptURLNodeStepExecutor.class);
        instanceregistry.remove(ExecNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME);
        instanceregistry.remove(ScriptFileNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME);
        instanceregistry.remove(ScriptURLNodeStepExecutor.SERVICE_IMPLEMENTATION_NAME);
    }
    @Override
    public String getName() {
        return name;
    }
}
