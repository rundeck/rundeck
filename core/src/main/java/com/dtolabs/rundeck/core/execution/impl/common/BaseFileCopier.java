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

/*
* BaseFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 2:47 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.common;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.IRundeckProject;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.utils.Streams;
import org.apache.commons.lang.RandomStringUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * BaseFileCopier provides utility methods for a FileCopier class.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class BaseFileCopier {
    public static final String FILE_COPY_DESTINATION_DIR = "file-copy-destination-dir";
    public static final String FRAMEWORK_FILE_COPY_DESTINATION_DIR = "framework." + FILE_COPY_DESTINATION_DIR;
    public static final String PROJECT_FILE_COPY_DESTINATION_DIR = "project." + FILE_COPY_DESTINATION_DIR;
    public static final String DEFAULT_WINDOWS_FILE_EXT = ".bat";
    public static final String DEFAULT_UNIX_FILE_EXT = ".sh";

    private static FileCopierUtil util = new DefaultFileCopierUtil();
    /**
     * create unique strings
     */
    private static AtomicLong counter = new AtomicLong(0);

    /**
     * @return the default file extension for a temp file based on the type of node
     * @param node node
     */
    public static String defaultRemoteFileExtensionForNode(final INodeEntry node){
        if (null != node.getOsFamily() && "windows".equalsIgnoreCase(node.getOsFamily().trim())) {
            return DEFAULT_WINDOWS_FILE_EXT;
        } else {
            return DEFAULT_UNIX_FILE_EXT;
        }
    }

    /**
     * @return a string with a file extension appended if it is not already on the file path
     * provided.
     *
     * @param filepath the file path string
     * @param fileext  the file extension, if it does not start with a "." one will be prepended
     *                 first. If null, the unmodified filepath will be returned.
     */
    public static String appendRemoteFileExtension(final String filepath, final String fileext) {
        return util.appendRemoteFileExtension(filepath, fileext);
    }

    /**
     * Return a remote destination temp dir path for the given node.  If specified, the node attribute named {@value
     * #FILE_COPY_DESTINATION_DIR} is used, otherwise a temp directory appropriate for the os-family of the node is
     * returned.
     *
     * @param node the node entry
     *
     * @return a path to destination dir for the node
     */
    public static String getRemoteDirForNode(final INodeEntry node) {
        return util.getRemoteDirForNode(node);
    }

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
    public static String getRemoteDirForNode(
            final INodeEntry node,
            final IRundeckProject project,
            final IFramework framework
    )
    {
        return util.getRemoteDirForNode(node, project, framework);
    }

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
    public static String generateRemoteFilepathForNode(final INodeEntry node, final String scriptfileName) {
        return generateRemoteFilepathForNode(node, scriptfileName, null);
    }

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
    public static String generateRemoteFilepathForNode(
            final INodeEntry node,
            final String scriptfileName,
            final String fileExtension
    )
    {
        return generateRemoteFilepathForNode(
                node,
                scriptfileName,
                fileExtension,
                null
        );
    }

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
     * @deprecated use {@link #generateRemoteFilepathForNode(com.dtolabs.rundeck.core.common.INodeEntry, com.dtolabs.rundeck.core.common.IRundeckProject, com.dtolabs.rundeck.core.common.IFramework, String, String, String)}
     */
    public static String generateRemoteFilepathForNode(
            final INodeEntry node,
            final String scriptfileName,
            final String fileExtension,
            final String identity
    )
    {
        return generateRemoteFilepathForNode(node, null, null, scriptfileName, fileExtension, identity);
    }

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
    public static String generateRemoteFilepathForNode(
            final INodeEntry node,
            final IRundeckProject project,
            final IFramework framework,
            final String scriptfileName,
            final String fileExtension,
            final String identity
    )
    {
        return util.generateRemoteFilepathForNode(node, project, framework, scriptfileName, fileExtension, identity);
    }

    /**
     * Write the file, stream, or text to a local temp file and return the file
     * @param context context
     * @param original source file, or null
     * @param input source inputstream or null
     * @param script source text, or null
     * @return temp file, this file should later be cleaned up by calling
     * {@link com.dtolabs.rundeck.core.execution.script.ScriptfileUtils#releaseTempFile(java.io.File)}
     * @throws FileCopierException if IOException occurs
     */
    public static File writeTempFile(ExecutionContext context, File original, InputStream input,
            String script) throws FileCopierException {
        return util.writeTempFile(context, original, input, script);
    }

    /**
     *
     * @param original source file
     * @param input source stream
     * @param script source string
     * @param destinationFile destination
     * @return local file
     * @throws FileCopierException on error
     */
    protected static File writeLocalFile(
            File original,
            InputStream input,
            String script,
            File destinationFile
    ) throws FileCopierException
    {
        return util.writeLocalFile(original, input, script, destinationFile);
    }

}
