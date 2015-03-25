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
* LocalExecNodeStepPlugin.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/13/12 4:54 PM
* 
*/
package com.dtolabs.rundeck.plugin.localexec;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.core.utils.OptsUtil;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.step.NodeStepPlugin;
import com.dtolabs.rundeck.plugins.step.PluginStepContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * LocalExecNodeStepPlugin is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin(service = ServiceNameConstants.WorkflowNodeStep, name = LocalExecNodeStepPlugin.PROVIDER_NAME)
@PluginDescription(title = "Local Command", description = "Run a command locally on the server")
public class LocalExecNodeStepPlugin implements NodeStepPlugin {
    public static final String PROVIDER_NAME = "localexec";

    @PluginProperty(title = "Command", description = "The command (runs locally)", required = true)
    private String command;

    @Override
    public void executeNodeStep(PluginStepContext context, Map<String, Object> map, INodeEntry entry)
        throws NodeStepException {
        if(null==command || "".equals(command.trim())) {
            throw new NodeStepException("Command is not set",
                                        StepFailureReason.ConfigurationFailure,
                                        entry.getNodename());
        }
        String[] split = OptsUtil.burst(command);
        Map<String, Map<String, String>> nodeData = DataContextUtils.addContext("node",
                                                                                DataContextUtils.nodeData(entry),
                                                                                context.getDataContext());

        final String[] finalCommand = DataContextUtils.replaceDataReferences(split, nodeData);
        StringBuilder preview=new StringBuilder();

        for (int i = 0; i < finalCommand.length; i++) {
            preview.append("'").append(finalCommand[i]).append("'");
        }
        context.getLogger().log(5, "LocalExecNodeStepPlugin, running command ("+split.length+"): " + preview.toString());
        Map<String, String> env = DataContextUtils.generateEnvVarsFromContext(nodeData);
        final int result;
        try {
            result = ScriptExecUtil.runLocalCommand(finalCommand, env, null, System.out, System.err);
            if(result!=0) {
                Map<String,Object> failureData=new HashMap<>();
                failureData.put(NodeExecutorResultImpl.FAILURE_DATA_RESULT_CODE, result);
                throw new NodeStepException("Result code was " + result,
                                            NodeStepFailureReason.NonZeroResultCode,
                                            failureData,
                                            entry.getNodename());
            }
        } catch (IOException e) {
            throw new NodeStepException(e, StepFailureReason.IOFailure, entry.getNodename());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeStepException(e, StepFailureReason.Interrupted, entry.getNodename());
        }
    }
}
