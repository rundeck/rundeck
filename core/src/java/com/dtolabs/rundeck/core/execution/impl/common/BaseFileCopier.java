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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * BaseFileCopier is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class BaseFileCopier {
    /**
     * Copy the embedded script content, or the script source stream, or script string into a temp file, and replace embedded tokens with
     * values from the dataContext. Marks the file as executable and delete-on-exit.
     *
     * @return temp file path
     *
     * @throws com.dtolabs.rundeck.core.execution.ExecutionException
     *          if an IO problem occurs
     */
    protected File writeScriptTempFile(ExecutionContext context, final File original, final InputStream input,
                                       final String script, final INodeEntry node, final Framework framework) throws
        FileCopierException {

        //create new dataContext with the node data, and write the script (file,content or strea) to a temp file
        //using the dataContext for substitution.
        final Map<String, Map<String, String>> origContext = context.getDataContext();
        final Map<String, Map<String, String>> dataContext = DataContextUtils.addContext("node",
            DataContextUtils.nodeData(node), origContext);

        File tempfile = null;
        try {
            if (null != original) {
                tempfile = DataContextUtils.replaceTokensInFile(original, dataContext, framework);
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
            throw new FileCopierException("error writing script to tempfile: " + e.getMessage(), e);
        }
        System.err.println("Wrote script content to file: " + tempfile);
        try {
            ScriptfileUtils.setExecutePermissions(tempfile);
        } catch (IOException e) {
            System.err.println(
                "Failed to set execute permissions on tempfile, execution may fail: " + tempfile.getAbsolutePath());
        }
        return tempfile;
    }

    protected String appendRemoteFileExtensionForNode(INodeEntry node, String remoteFilename) {
        if ("windows".equalsIgnoreCase(node.getOsFamily().trim())) {
            remoteFilename += (remoteFilename.endsWith(".bat") ? "" : ".bat");
        } else {
            remoteFilename += (remoteFilename.endsWith(".sh") ? "" : ".sh");
        }
        return remoteFilename;
    }

    protected String getRemoteDirForNode(INodeEntry node) {
        //TODO: allow set temp dir via node attribute
        String remotedir = "/tmp/";
        if ("windows".equalsIgnoreCase(node.getOsFamily().trim())) {
            remotedir = "C:/WINDOWS/TEMP/";
        }
        return remotedir;
    }
}
