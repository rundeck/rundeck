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
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameterGenerator;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameterGeneratorImpl;
import com.dtolabs.rundeck.core.execution.script.ExecTaskParameters;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.core.utils.ScriptExecUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.dtolabs.rundeck.core.execution.impl.local.LocalNodeExecutor.getDisableLocalExecutorEnv;

/**
 * @author greg
 * @since 6/12/17
 */
@Plugin(name = NewLocalNodeExecutor.SERVICE_PROVIDER_TYPE, service = ServiceNameConstants.NodeExecutor)
@PluginDescription(title = "Local (New)", description = "Beta - Executes commands locally on the Rundeck server")
public class NewLocalNodeExecutor
        implements NodeExecutor, DescriptionBuilder.Collaborator
{
    public static final String SERVICE_PROVIDER_TYPE = "newlocal";
    public static final String PROP_MERGE_ENV = "mergeEnv";
    private ExecTaskParameterGenerator parameterGenerator = new ExecTaskParameterGeneratorImpl();
    private boolean disableLocalExecutor = false;

    @PluginProperty(
            title = "Merge Environment",
            description = "Merge the environment variables from the Rundeck server with the local environment",
            defaultValue = "true",
            scope = PropertyScope.Framework
    )
    Boolean mergeEnv;

    @Override
    public void buildWith(final DescriptionBuilder builder) {
        builder.frameworkMapping(
                PROP_MERGE_ENV,
                String.join(".", "framework", ServiceNameConstants.NodeExecutor, SERVICE_PROVIDER_TYPE, PROP_MERGE_ENV)
        );
    }

    public NewLocalNodeExecutor() {
        this.disableLocalExecutor = getDisableLocalExecutorEnv();
    }

    @Override
    public NodeExecutorResult executeCommand(
            final ExecutionContext context,
            final String[] command,
            final INodeEntry node
    )
    {
        return executeCommand(context, command, null, node);
    }

    @Override
    public NodeExecutorResult executeCommand(
            final ExecutionContext context, final String[] command,
            final InputStream inputStream,
            final INodeEntry node
    )
    {
        if (disableLocalExecutor) {
            return NodeExecutorResultImpl.createFailure(
                    StepFailureReason.ConfigurationFailure,
                    "Local Executor is disabled",
                    node
            );
        }

        List<String> commandList = new ArrayList<>();
        try {
            ExecTaskParameters
                    taskParameters =
                    parameterGenerator.generate(node, true, null, command);
            commandList.add(taskParameters.getCommandexecutable());
            commandList.addAll(Arrays.asList(taskParameters.getCommandArgs()));
        } catch (ExecutionException e) {
            return NodeExecutorResultImpl.createFailure(
                    StepFailureReason.ConfigurationFailure,
                    e.getMessage(),
                    node
            );
        }

        StringBuilder preview = new StringBuilder();
        for (final String aCommand : commandList) {
            preview.append("'").append(aCommand).append("'");
        }
        context.getExecutionLogger().log(
                5,
                "NewLocalNodeExecutor, running command (" + commandList.size() + "): " + preview
        );
        Map<String, String> env = DataContextUtils.generateEnvVarsFromContext(context.getDataContext());
        final int result;
        try {
            result =
                    ScriptExecUtil.runLocalCommand(
                            commandList.toArray(new String[]{}),
                            env,
                            null,
                            System.out,
                            System.err,
                            !mergeEnv,
                            ScriptExecUtil::killProcessHandleDescend,
                            inputStream
                    );
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
        if (result != 0) {
            return NodeExecutorResultImpl.createFailure(
                    NodeStepFailureReason.NonZeroResultCode,
                    "Result code was " + result, node, result
            );
        }
        return NodeExecutorResultImpl.createSuccess(node);
    }
}
