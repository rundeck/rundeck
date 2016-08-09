/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;

import java.io.File;
import java.io.InputStream;

/**
 * Copy files to a specific destination on a remote node.
 */
public interface DestinationFileCopier extends FileCopier {

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param input   the input stream
     * @param node node
     * @param destination the full path destination for the file
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyFileStream(final ExecutionContext context, InputStream input, INodeEntry node, String destination) throws
            FileCopierException;

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param file    local file tocopy
     * @param node node
     * @param destination the full path destination for the file
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyFile(final ExecutionContext context, File file, INodeEntry node, String destination) throws FileCopierException;

    /**
     * Copy the contents of an input stream to the node
     *
     * @param context context
     * @param script  file content string
     * @param node node
     * @param destination the full path destination for the file
     *
     * @return File path of the file after copying to the node
     *
     * @throws FileCopierException if an error occurs
     */
    public String copyScriptContent(final ExecutionContext context, String script, INodeEntry node, String destination) throws
            FileCopierException;
}
