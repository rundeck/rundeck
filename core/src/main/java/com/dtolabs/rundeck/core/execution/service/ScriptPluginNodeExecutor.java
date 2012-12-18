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
* ScriptNodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/5/11 10:00 AM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/**
 * ScriptPluginNodeExecutor wraps the execution of the script and supplies the NodeExecutor interface.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginNodeExecutor extends BaseScriptPlugin implements NodeExecutor {

    ScriptPluginNodeExecutor(final ScriptPluginProvider provider, final Framework framework) {
        super(provider, framework);
    }

    @Override
    public boolean isAllowCustomProperties() {
        return false;
    }

    static void validateScriptPlugin(final ScriptPluginProvider plugin) throws PluginException {
    }

    public NodeExecutorResult executeCommand(final ExecutionContext executionContext, final String[] command,
                                             final INodeEntry node)  {
        final ScriptPluginProvider plugin = getProvider();
        final String pluginname = plugin.getName();
        executionContext.getExecutionListener().log(3,
                                                    "[" + pluginname + "] execCommand started, command: "
                                                    + StringArrayUtil.asString(command, " "));

        final Map<String, Map<String, String>> localDataContext = createScriptDataContext(
            executionContext.getFramework(),
                                                                                          executionContext.getFrameworkProject(),
                                                                                          executionContext.getDataContext());
        final HashMap<String, String> scptexec = new HashMap<String, String>();
        scptexec.put("command", StringArrayUtil.asString(command, " "));
        localDataContext.put("exec", scptexec);

        final String[] finalargs = createScriptArgs(localDataContext);

        executionContext.getExecutionListener().log(3, "[" + getProvider().getName() + "] executing: " + Arrays.asList(
            finalargs));

        int result = -1;
        try {
            result = ScriptExecUtil.runLocalCommand(finalargs,
                                                    DataContextUtils.generateEnvVarsFromContext(localDataContext),
                                                    null,
                                                    System.out,
                                                    System.err
            );
            executionContext.getExecutionListener().log(3,
                                                        "[" + pluginname + "]: result code: " + result + ", success: "
                                                        + (0 == result));
            if(0!=result){
                return NodeExecutorResultImpl.createFailure(NodeStepFailureReason.NonZeroResultCode,
                                                            "Result code: " + result, node, result);
            }else {
                return NodeExecutorResultImpl.createSuccess(node);
            }
        } catch (IOException e) {
            return NodeExecutorResultImpl.createFailure(StepFailureReason.IOFailure,
                                                        e.getMessage(),
                                                        e,
                                                        node,
                                                        result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return NodeExecutorResultImpl.createFailure(StepFailureReason.Interrupted,
                                                        e.getMessage(),
                                                        e,
                                                        node,
                                                        result);
        }
    }

}
