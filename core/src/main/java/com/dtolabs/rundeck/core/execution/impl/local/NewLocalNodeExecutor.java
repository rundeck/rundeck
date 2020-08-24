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
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;

import java.io.IOException;
import java.util.Map;

/**
 * @author greg
 * @since 6/12/17
 */
public class NewLocalNodeExecutor implements NodeExecutor {
    private static final String MESSAGE_ERROR_FILE_BUSY_PATTERN = "Cannot run program.+: error=26.*";
    private static final int MAX_TIME_TO_WAIT_BEFORE_TRY_AGAIN = 3000;
    public static final String SERVICE_PROVIDER_TYPE = "newlocal";

    @Override
    public NodeExecutorResult executeCommand(
            final ExecutionContext context, final String[] command, final INodeEntry node
    )
    {
        return executeCommand(context, command, node, true, 500);
    }

    private NodeExecutorResult executeCommand(
            final ExecutionContext context, final String[] command, final INodeEntry node, boolean retryAttempt, int timeToWait
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

        int result = 0;
        try {
            result = ScriptExecUtil.runLocalCommand(command, env, null, System.out, System.err);
            if (result != 0) {
                return NodeExecutorResultImpl.createFailure(NodeStepFailureReason.NonZeroResultCode,
                                                            "Result code was " + result, node, result
                );
            }
        } catch (IOException e) {
            if(retryAttempt && e.getMessage().matches(MESSAGE_ERROR_FILE_BUSY_PATTERN)){
                context.getExecutionLogger().log(
                        5,
                        "File is busy. Retrying..."
                );
                retryAttemptExecuteCommand(context, command, node, timeToWait);
            } else {
                return NodeExecutorResultImpl.createFailure(
                        StepFailureReason.IOFailure,
                        e.getMessage(),
                        node
                );
            }
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

    private void retryAttemptExecuteCommand(ExecutionContext context, String[] command, INodeEntry node, int timeToWait) {
        try{
            timeToWait = timeToWait + 500;
            boolean retryAttempt = timeToWait < MAX_TIME_TO_WAIT_BEFORE_TRY_AGAIN;
            context.getExecutionLogger().log(
                    5,
                    "Waiting " + (timeToWait / 1000) + " seconds before try again"
            );
            Thread.sleep(timeToWait);
            executeCommand(context, command, node, retryAttempt, timeToWait);
        }
        catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }
}
