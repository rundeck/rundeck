/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable;
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class NoopExecutionListener
        implements ExecutionListener
{
    @Override
    public void ignoreErrors(final boolean ignore) {

    }

    @Override
    public FailedNodesListener getFailedNodesListener() {
        return null;
    }

    @Override
    public void beginNodeExecution(
            final ExecutionContext context, final String[] command, final INodeEntry node
    )
    {

    }

    @Override
    public void finishNodeExecution(
            final NodeExecutorResult result,
            final ExecutionContext context,
            final String[] command,
            final INodeEntry node
    )
    {

    }

    @Override
    public void beginNodeDispatch(final ExecutionContext context, final StepExecutionItem item) {

    }

    @Override
    public void beginNodeDispatch(final ExecutionContext context, final Dispatchable item) {

    }

    @Override
    public void finishNodeDispatch(
            final DispatcherResult result, final ExecutionContext context, final StepExecutionItem item
    )
    {

    }

    @Override
    public void finishNodeDispatch(
            final DispatcherResult result, final ExecutionContext context, final Dispatchable item
    )
    {

    }

    @Override
    public void beginFileCopyFileStream(
            final ExecutionContext context, final InputStream input, final INodeEntry node
    )
    {

    }

    @Override
    public void beginFileCopyFile(
            final ExecutionContext context, final File input, final INodeEntry node
    )
    {

    }

    @Override
    public void beginFileCopyFile(
            final ExecutionContext context, final List<File> files, final INodeEntry node
    )
    {

    }

    @Override
    public void beginFileCopyScriptContent(
            final ExecutionContext context, final String input, final INodeEntry node
    )
    {

    }

    @Override
    public void finishFileCopy(final String result, final ExecutionContext context, final INodeEntry node) {

    }

    @Override
    public void finishMultiFileCopy(
            final String[] result, final ExecutionContext context, final INodeEntry node
    )
    {

    }

    @Override
    public ExecutionListenerOverride createOverride() {
        return null;
    }

    @Override
    public void log(final int level, final String message) {

    }

    @Override
    public void log(final int level, final String message, final Map eventMeta) {

    }

    @Override
    public void event(final String eventType, final String message, final Map eventMeta) {

    }
}
