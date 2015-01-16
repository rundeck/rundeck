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
* BaseFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 2:47 PM
* 
*/
package com.dtolabs.rundeck.core.execution.impl.common;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.utils.Streams;

import java.io.*;
import java.util.*;

/**
 * BaseFileCopier provides utility methods for a FileCopier class.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class BaseFileCopier {
    public static final String FILE_COPY_DESTINATION_DIR = "file-copy-destination-dir";
    public static final String DEFAULT_WINDOWS_FILE_EXT = ".bat";
    public static final String DEFAULT_UNIX_FILE_EXT = ".sh";

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
     *
     * @return file where the script was stored
     *
     * @throws com.dtolabs.rundeck.core.execution.service.FileCopierException
     *          if an IO problem occurs
     */
    public static File writeScriptTempFile(
            final ExecutionContext context,
            final File original,
            final InputStream input,
            final String script,
            final INodeEntry node
    ) throws FileCopierException
    {
        return writeScriptTempFile(context, original, input, script, node, null);
    }
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
     *
     * @return file where the script was stored
     *
     * @throws com.dtolabs.rundeck.core.execution.service.FileCopierException
     *          if an IO problem occurs
     */
    public static File writeScriptTempFile(
            final ExecutionContext context,
            final File original,
            final InputStream input,
            final String script,
            final INodeEntry node,
            final File destination
    ) throws FileCopierException
    {
        final Framework framework = context.getFramework();

        //create new dataContext with the node data, and write the script (file,
        // content or strea) to a temp file
        //using the dataContext for substitution.
        final Map<String, Map<String, String>> origContext = context.getDataContext();
        final Map<String, Map<String, String>> dataContext = DataContextUtils.addContext(
                "node",
                DataContextUtils.nodeData(node), origContext
        );

        final File tempfile;
        ScriptfileUtils.LineEndingStyle style = ScriptfileUtils.lineEndingStyleForNode(node);

        try {
            if (null == destination) {
                tempfile = ScriptfileUtils.createTempFile(framework);
            } else {
                tempfile = destination;
            }
            if (null != original) {
                //don't replace tokens
                final FileInputStream in = new FileInputStream(original);
                try {
                    final FileOutputStream out = new FileOutputStream(tempfile);
                    try {
                        Streams.copyStream(in, out);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } else if (null != script) {
                DataContextUtils.replaceTokensInScript(
                        script,
                        dataContext,
                        framework,
                        style,
                        tempfile
                );
            } else if (null != input) {
                DataContextUtils.replaceTokensInStream(
                        input,
                        dataContext,
                        framework,
                        style,
                        tempfile
                );
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new FileCopierException(
                    "error writing script to tempfile: " + e.getMessage(),
                    StepFailureReason.IOFailure, e
            );
        }
        try {
            ScriptfileUtils.setExecutePermissions(tempfile);
        } catch (IOException e) {
            System.err.println(
                    "Failed to set execute permissions on tempfile, execution may fail: " +
                    tempfile.getAbsolutePath()
            );
        }
        return tempfile;
    }

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
        if (null == fileext) {
            return filepath;
        }
        String result = filepath;
        String ext = fileext;
        if (!ext.startsWith(".")) {
            ext = "." + fileext;
        }
        result += (filepath.endsWith(ext) ? "" : ext);
        return result;
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
        String pathSeparator = "/";
        String remotedir = "/tmp/";
        if (null != node.getOsFamily() && "windows".equalsIgnoreCase(node.getOsFamily().trim())) {
            pathSeparator = "\\";
            remotedir = "C:\\WINDOWS\\TEMP\\";
        }
        if (null != node.getAttributes() && null != node.getAttributes().get(FILE_COPY_DESTINATION_DIR)) {
            String s = node.getAttributes().get(FILE_COPY_DESTINATION_DIR);
            return s.endsWith(pathSeparator) ? s : s + pathSeparator;
        }

        return remotedir;
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
     * @param fileExtension optional extension to use for the temp file, or null for default
     *
     * @return a filepath specifying destination of the file to copy that should be unique for the node and current
     *         date.
     */
    public static String generateRemoteFilepathForNode(final INodeEntry node, final String scriptfileName, final String fileExtension) {
        String tempfilename = System.currentTimeMillis()
                + "-" + node.getNodename()
                + "-" + scriptfileName;
        String extension = fileExtension;
        if (null == extension) {
            //determine based on node
            extension = defaultRemoteFileExtensionForNode(node);
        }
        final String remoteFilename = appendRemoteFileExtension(
                cleanFileName(tempfilename),
                null != extension ? cleanFileName(extension) : null
        );
        final String remotedir = getRemoteDirForNode(node);

        return remotedir + remoteFilename;
    }

    private static String cleanFileName(String nodename) {
        return nodename.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    /**
     * Write the file, stream, or text to a local temp file and return the file
     * @param context context
     * @param original source file, or null
     * @param input source inputstream or null
     * @param script source text, or null
     * @return temp file
     * @throws FileCopierException if IOException occurs
     */
    public static File writeTempFile(ExecutionContext context, File original, InputStream input,
            String script) throws FileCopierException {
        File tempfile = null;
        try {
            tempfile = ScriptfileUtils.createTempFile(context.getFramework());
        } catch (IOException e) {
            throw new FileCopierException("error writing to tempfile: " + e.getMessage(),
                    StepFailureReason.IOFailure, e);
        }
        return writeLocalFile(original, input, script, tempfile);
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
        try {

            if (null != original) {
                final InputStream in = new FileInputStream(original);
                try {
                    final FileOutputStream out = new FileOutputStream(destinationFile);
                    try {
                        Streams.copyStream(in, out);
                    } finally {
                        out.close();
                    }
                } finally {
                    in.close();
                }
            } else if (null != input) {
                final InputStream in = input;
                final FileOutputStream out = new FileOutputStream(destinationFile);
                try {
                    Streams.copyStream(in, out);
                } finally {
                    out.close();
                }
            } else if (null != script) {
                Reader in = new StringReader(script);
                final FileOutputStream out = new FileOutputStream(destinationFile);
                final Writer write = new OutputStreamWriter(out);
                try {
                    Streams.copyWriterCount(in, write);
                    write.flush();
                } finally {
                    out.close();
                }
            }

            return destinationFile;
        } catch (IOException e) {
            throw new FileCopierException("error writing to tempfile: " + e.getMessage(),
                    StepFailureReason.IOFailure, e);
        }

    }
}
