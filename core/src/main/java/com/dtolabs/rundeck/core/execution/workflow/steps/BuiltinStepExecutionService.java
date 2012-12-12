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
* BuiltinStepExecutionService.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/13/12 5:50 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.BaseProviderRegistryService;

import java.util.*;


/**
 * BuiltinStepExecutionService provides the builtin StepExecutors
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class BuiltinStepExecutionService extends BaseProviderRegistryService<StepExecutor> {

    String name;

    BuiltinStepExecutionService(final String name, final Framework framework) {
        super(framework);
        this.name=name;
        resetDefaultProviders();
    }

    public void resetDefaultProviders() {
        registry.put(NodeDispatchStepExecutor.STEP_EXECUTION_TYPE, NodeDispatchStepExecutor.class);
    }

    @Override
    public String getName() {
        return name;
    }
}
