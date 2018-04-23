/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
 */

package com.dtolabs.rundeck.core.execution.impl.local;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.ExecTask;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author greg
 * @since 6/12/17
 */
public class NewLocalNodeExecutor implements NodeExecutor {
    public static final String SERVICE_PROVIDER_TYPE = "newlocal";

    @Override
    public NodeExecutorResult executeCommand(
            final ExecutionContext context, final String[] command, final INodeEntry node
    )
    {

        StringBuilder preview = new StringBuilder();

        for (final String aCommand : command) {
            preview.append("'").append(aCommand).append("'");
        }
        context.getExecutionLogger().log(
                5,
                "NewLocalNodeExecutor, running command (" + command.length + "): " + preview.toString()
        );
        Map<String, String> env = DataContextUtils.generateEnvVarsFromContext(context.getDataContext());

        final int result;
        try {
            result = ScriptExecUtil.runLocalCommand(command, env, null, System.out, System.err);
            if (result != 0) {
                return NodeExecutorResultImpl.createFailure(NodeStepFailureReason.NonZeroResultCode,
                                                            "Result code was " + result, node, result
                );
            }
        } catch (IOException e) {
            return NodeExecutorResultImpl.createFailure(
                    StepFailureReason.IOFailure,
                    e.getMessage(),
                    node
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            return NodeExecutorResultImpl.createFailure(
                    StepFailureReason.Interrupted,
                    e.getMessage(),
                    node
            );
        }

        if (null != context.getOutputContext()) {
            context.getOutputContext().addOutput("exec", "exitCode", String.valueOf(result));
        }
        return NodeExecutorResultImpl.createSuccess(node);
    }
}
