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

    /**
     * Copy a script file, script source stream, or script string into a temp file, and replace \
     * embedded tokens with values from the dataContext for the latter two. Marks the file as executable and delete-on-exit. This will not
     * rewrite any content if the input is originally a file.
     *
     * @param context  execution context
     * @param original local system file, or null
     * @param input    input stream to write, or null
     * @param script   file content string, or null
     * @param node     destination node entry, to provide node data context
     *
     * @return temp file path
     *
     * @throws com.dtolabs.rundeck.core.execution.ExecutionException
     *          if an IO problem occurs
     */
    public static File writeScriptTempFile(final ExecutionContext context, final File original, final InputStream input,
                                           final String script, final INodeEntry node) throws
        FileCopierException {
        final Framework framework = context.getFramework();

        //create new dataContext with the node data, and write the script (file,content or strea) to a temp file
        //using the dataContext for substitution.
        final Map<String, Map<String, String>> origContext = context.getDataContext();
        final Map<String, Map<String, String>> dataContext = DataContextUtils.addContext("node",
            DataContextUtils.nodeData(node), origContext);

        File tempfile = null;
        try {
            if (null != original) {
                //don't replace tokens
                tempfile = ScriptfileUtils.createTempFile(framework);
                final FileInputStream in = new FileInputStream(original);
                try{
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
                tempfile = DataContextUtils.replaceTokensInScript(script,
                    dataContext, framework);
            } else if (null != input) {
                tempfile = DataContextUtils.replaceTokensInStream(input,
                    dataContext, framework);
            } else {
                return null;
            }
        } catch (IOException e) {
            throw new FileCopierException("error writing script to tempfile: " + e.getMessage(),
                                          StepFailureReason.IOFailure, e);
        }
//        System.err.println("Wrote script content to file: " + tempfile);
        try {
            ScriptfileUtils.setExecutePermissions(tempfile);
        } catch (IOException e) {
            System.err.println(
                "Failed to set execute permissions on tempfile, execution may fail: " + tempfile.getAbsolutePath());
        }
        return tempfile;
    }

    /**
     * Return a string with an appropriate script file extension appended if it is not already on the file path
     * provided. The OS-family of the node determines the appropriate extension to use.
     *
     * @param node     node destination
     * @param filepath the file path string
     */
    public static String appendRemoteFileExtensionForNode(final INodeEntry node, final String filepath) {
        String result = filepath;
        if (null != node.getOsFamily() && "windows".equalsIgnoreCase(node.getOsFamily().trim())) {
            result += (filepath.endsWith(".bat") ? "" : ".bat");
        } else {
            result += (filepath.endsWith(".sh") ? "" : ".sh");
        }
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
        if (null != node.getAttributes() && null != node.getAttributes().get(FILE_COPY_DESTINATION_DIR)) {
            return node.getAttributes().get(FILE_COPY_DESTINATION_DIR);
        }
        String remotedir = "/tmp/";
        if (null != node.getOsFamily() && "windows".equalsIgnoreCase(node.getOsFamily().trim())) {
            remotedir = "C:/WINDOWS/TEMP/";
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
        final String remoteFilename = appendRemoteFileExtensionForNode(node,
            System.currentTimeMillis() + "-" + node.getNodename() + "-" + scriptfileName);
        final String remotedir = getRemoteDirForNode(node);

        return remotedir + remoteFilename;
    }
}
