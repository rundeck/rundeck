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
* ScriptPluginStepPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/19/12 2:07 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow.steps;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;
import com.dtolabs.rundeck.plugins.step.StepPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * ScriptPluginStepPlugin is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginStepPlugin extends BaseScriptPlugin implements StepPlugin {
    ScriptPluginStepPlugin(ScriptPluginProvider provider, Framework framework) {
        super(provider, framework);
    }

    @Override
    public boolean isAllowCustomProperties() {
        return true;
    }

    static void validateScriptPlugin(final ScriptPluginProvider plugin) throws PluginException {
        try {
            createDescription(plugin, true, DescriptionBuilder.builder());
        } catch (ConfigurationException e) {
            throw new PluginException(e);
        }
    }

    @Override
    public void executeStep(final PluginStepContext executionContext, final Map<String, Object> config)
        throws StepException {
        final ScriptPluginProvider plugin = getProvider();
        final String pluginname = plugin.getName();
        executionContext.getLogger()
            .log(3, "[" + pluginname + "] step started, config: " + config);

        //create a new step item containing the resolved properties, which will be used in the script
        // execution context
        int result = -1;
        try {
            result = runPluginScript(executionContext, System.out, System.err, getFramework(), config);
        } catch (IOException e) {
            throw new StepException(e.getMessage(), StepFailureReason.IOFailure);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StepException(e.getMessage(), StepFailureReason.Interrupted);
        }
        executionContext.getLogger().log(3, "[" + pluginname + "]: result code: " + result);
        if (result != 0) {
            Map<String,Object> failureData=new HashMap<>();
            failureData.put(NodeExecutorResultImpl.FAILURE_DATA_RESULT_CODE, result);
            throw new StepException(
                    "Script result code was: " + result,
                    NodeStepFailureReason.NonZeroResultCode,
                    failureData
            );
        }

    }

}
