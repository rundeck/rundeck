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

package com.dtolabs.rundeck.core.execution.impl.common;

import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;

import java.io.File;
import java.io.InputStream;

/**
 * Created by greg on 7/15/16.
 */
public interface FileCopierUtil {
    /**
     * Copy a script file, script source stream, or script string into a temp file, and replace \
     * embedded tokens with values from the dataContext for the latter two. Marks the file as
     * executable and delete-on-exit. This will not rewrite any content if the input is originally a
     * file.
     *
     * @param context  execution context
     * @param original local system file, or null
     * @param input    input stream to write, or null
     * @param script   file content string, or null
     * @param node     destination node entry, to provide node data context
     * @param expandTokens if true, expand tokens in the stream or string
     *
     * @return file where the script was stored, this file should later be cleaned up by calling
     * {@link com.dtolabs.rundeck.core.execution.script.ScriptfileUtils#releaseTempFile(java.io.File)}
     *
     *
     * @throws com.dtolabs.rundeck.core.execution.service.FileCopierException
     *          if an IO problem occurs
     */
    File writeScriptTempFile(
            ExecutionContext context,
            File original,
            InputStream input,
            String script,
            INodeEntry node,
            boolean expandTokens
    ) throws FileCopierException;

    /**
     * Copy a script file, script source stream, or script string into a temp file, and replace \
     * embedded tokens with values from the dataContext for the latter two. Marks the file as
     * executable and delete-on-exit. This will not rewrite any content if the input is originally a
     * file.
     *
     * @param context  execution context
     * @param original local system file, or null
     * @param input    input stream to write, or null
     * @param script   file content string, or null
     * @param node     destination node entry, to provide node data context
     * @param destination destination file, or null to generate a new temp file
     *                    @param expandTokens if true, expand tokens in the stream or string
     *
     * @return file where the script was stored
     *
     * @throws FileCopierException
     *          if an IO problem occurs
     */
    File writeScriptTempFile(
            ExecutionContext context,
            File original,
            InputStream input,
            String script,
            INodeEntry node,
            File destination,
            boolean expandTokens
    ) throws FileCopierException;

    /**
     * @return the default file extension for a temp file based on the type of node
     * @param node node
     */
    String defaultRemoteFileExtensionForNode(INodeEntry node);

    /**
     * @return a string with a file extension appended if it is not already on the file path
     * provided.
     *
     * @param filepath the file path string
     * @param fileext  the file extension, if it does not start with a "." one will be prepended
     *                 first. If null, the unmodified filepath will be returned.
     */
    String appendRemoteFileExtension(String filepath, String fileext);

    /**
     * Return a remote destination temp dir path for the given node.  If specified, the node attribute named {@value
     * #FILE_COPY_DESTINATION_DIR} is used, otherwise a temp directory appropriate for the os-family of the node is
     * returned.
     *
     * @param node the node entry
     *
     * @return a path to destination dir for the node
     */
    String getRemoteDirForNode(INodeEntry node);

    /**
     * Return a remote destination temp dir path for the given node.  If specified, the node attribute named {@value
     * #FILE_COPY_DESTINATION_DIR} is used, otherwise a temp directory appropriate for the os-family of the node is
     * returned.
     *
     * @param node the node entry
     * @param project project
     * @param framework framework
     *
     * @return a path to destination dir for the node
     */
    String getRemoteDirForNode(
            INodeEntry node,
            IRundeckProject project,
            IFramework framework
    );

    /**
     * Return a temporary filepath for a file to be copied to the node, given the input filename (without directory
     * path)
     *
     * @param node           the destination node
     * @param scriptfileName the name of the file to copy
     *
     * @return a filepath specifying destination of the file to copy that should be unique for the node and current
     *         date.
     */
    String generateRemoteFilepathForNode(INodeEntry node, String scriptfileName);

    /**
     * Return a temporary filepath for a file to be copied to the node, given the input filename (without directory
     * path)
     *
     * @param node           the destination node
     * @param scriptfileName the name of the file to copy
     * @param fileExtension  optional extension to use for the temp file, or null for default
     *
     * @return a filepath specifying destination of the file to copy that should be unique
     */
    String generateRemoteFilepathForNode(
            INodeEntry node,
            String scriptfileName,
            String fileExtension
    );

    /**
     * Return a temporary filepath for a file to be copied to the node, given the input filename (without directory
     * path)
     *
     * @param node           the destination node
     * @param scriptfileName the name of the file to copy
     * @param fileExtension  optional extension to use for the temp file, or null for default
     * @param identity       unique identifier, or null to include a random string
     *
     * @return a filepath specifying destination of the file to copy that should be unique
     * @deprecated use {@link #generateRemoteFilepathForNode(INodeEntry, IRundeckProject, IFramework, String, String, String)}
     */
    String generateRemoteFilepathForNode(
            INodeEntry node,
            String scriptfileName,
            String fileExtension,
            String identity
    );

    /**
     * Return a temporary filepath for a file to be copied to the node, given the input filename (without directory
     * path)
     *
     * @param node           the destination node
     * @param project        project
     * @param framework      framework
     * @param scriptfileName the name of the file to copy
     * @param fileExtension  optional extension to use for the temp file, or null for default
     * @param identity       unique identifier, or null to include a random string
     *
     * @return a filepath specifying destination of the file to copy that should be unique
     */
    String generateRemoteFilepathForNode(
            INodeEntry node,
            IRundeckProject project,
            IFramework framework,
            String scriptfileName,
            String fileExtension,
            String identity
    );

    /**
     * Write the file, stream, or text to a local temp file and return the file
     * @param context context
     * @param original source file, or null
     * @param input source inputstream or null
     * @param script source text, or null
     * @return temp file, this file should later be cleaned up by calling
     * {@link com.dtolabs.rundeck.core.execution.script.ScriptfileUtils#releaseTempFile(File)}
     * @throws FileCopierException if IOException occurs
     */
    File writeTempFile(
            ExecutionContext context, File original, InputStream input,
            String script
    ) throws FileCopierException;

    /**
     *
     * @param original source file
     * @param input source stream
     * @param script source string
     * @param destinationFile destination
     * @return local file
     * @throws FileCopierException on error
     */
    File writeLocalFile(
            File original,
            InputStream input,
            String script,
            File destinationFile
    ) throws FileCopierException;
}
