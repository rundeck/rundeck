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
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import org.rundeck.app.spi.AppService;

import java.io.File;
import java.io.InputStream;

public interface NodeExecutionService
        extends AppService
{

    /**
     * Copy stream as a file to the node to a specific path
     *
     * @param context         context
     * @param input           input stream
     * @param node            node
     * @param destinationPath destination path
     * @return filepath on the node for the destination file.
     * @throws FileCopierException on error
     */
    public String fileCopyFileStream(
            final ExecutionContext context,
            InputStream input,
            INodeEntry node,
            String destinationPath
    ) throws FileCopierException, ExecutionException;

    /**
     * Copy file to the node to a specific path
     *
     * @param context         context
     * @param file            input file
     * @param node            node
     * @param destinationPath destination path
     * @return filepath
     * @throws FileCopierException on error
     */
    public String fileCopyFile(
            final ExecutionContext context,
            File file,
            INodeEntry node,
            String destinationPath
    ) throws FileCopierException, ExecutionException;

    /**
     * Execute a command within the context on the node.
     *
     * @param context context
     * @param command command
     * @param node    node
     * @return result
     */
    NodeExecutorResult executeCommand(ExecutionContext context, ExecArgList command, INodeEntry node)
            throws ExecutionException;
}
