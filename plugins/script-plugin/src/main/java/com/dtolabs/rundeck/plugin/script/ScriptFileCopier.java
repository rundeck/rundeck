/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* ExternalScriptFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/4/11 10:57 AM
* 
*/
package com.dtolabs.rundeck.plugin.script;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.utils.Streams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * ExternalScriptFileCopier plugin provides the FileCopier service by allowing an external script to handle the
 * particulars of copying a local file to a node. <p> The plugin is enabled by setting the "file-copier" attribute of a
 * node to "script-copy".  The script to use for copying the file is specified via an attribute called "script-copy".
 * </p> <p> The script-copy attribute is used as the entire command to execute to copy the file.  The destination file
 * location should be the only output from the command. </p><p>   In addition to normal Data context references in this
 * attribute, you can include these special data references: </p> <ul>
 * <li><pre>${script-copy.file}</pre>: This is the local filepath of the file that should be copied to the node.</li>
 * <li><pre>${script-copy.dir}</pre>: This is the value of the script-copy-dir attribute</li>
 * </ul> <p> So for example, if you wanted to change the way the script is invoked, you could specify the script-copy
 * like:
 * <pre>
 *         &lt;node name="mynode" ...
 *         remotecopy="/bin/remotecopy"
 *         script-copy="${node.remotecopy} ${script-copy.file} -- ${node.username}@${node.name}"/>
 * </pre>
 * This would execute /bin/remotecopy and pass the path of the file to copy followed by -- and the node info
 * "username@hostname". </p>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "script-copy", service = "FileCopier")
public class ScriptFileCopier implements FileCopier {
    public static String SCRIPT_ATTRIBUTE = "script-copy";
    public static String DIR_ATTRIBUTE = "script-copy-dir";
    private static final String SCRIPT_COPY_DEFAULT_COMMAND_PROPERTY = "plugin.script-copy.default.command";
    private static final String SCRIPT_COPY_DEFAULT_DIR_PROPERTY = "plugin.script-copy.default.dir";

    /**
     * Copy inputstream
     */
    public String copyFileStream(final ExecutionContext executionContext, final InputStream inputStream,
                                 final INodeEntry node) throws FileCopierException {

        return copyFile(executionContext, null, inputStream, null, node);
    }

    /**
     * Copy existing file
     */
    public String copyFile(final ExecutionContext executionContext, final File file, final INodeEntry node) throws
        FileCopierException {
        return copyFile(executionContext, file, null, null, node);
    }

    /**
     * Copy string content
     */
    public String copyScriptContent(final ExecutionContext executionContext, final String s,
                                    final INodeEntry node) throws
        FileCopierException {
        return copyFile(executionContext, null, null, s, node);
    }


    /**
     * Internal copy method accepting file, inputstream or string
     */
    String copyFile(final ExecutionContext executionContext, final File file, final InputStream input,
                    final String content, final INodeEntry node) throws
        FileCopierException {

        File workingdir = null;
        String scriptargs = null;
        String dirstring = null;

        //get project or framework property for script-exec args
        final Framework framework = executionContext.getFramework();
        //look for specific property
        scriptargs = framework.getProjectProperty(executionContext.getFrameworkProject(),
            SCRIPT_COPY_DEFAULT_COMMAND_PROPERTY);


        if (null != node.getAttributes().get(SCRIPT_ATTRIBUTE)) {
            scriptargs = node.getAttributes().get(SCRIPT_ATTRIBUTE);
        }
        if (null == scriptargs) {
            throw new FileCopierException(
                "[script-copy file copier] no attribute " + SCRIPT_ATTRIBUTE + " was found on node: "
                + node
                    .getNodename() + ", and no " + SCRIPT_COPY_DEFAULT_COMMAND_PROPERTY
                + " property was configured for the project or framework.");
        }

        dirstring = framework.getProjectProperty(executionContext.getFrameworkProject(),
            SCRIPT_COPY_DEFAULT_DIR_PROPERTY);
        if (null != node.getAttributes().get(DIR_ATTRIBUTE)) {
            dirstring = node.getAttributes().get(DIR_ATTRIBUTE);
        }
        if (null != dirstring) {
            workingdir = new File(dirstring);
        }

        final Map<String, Map<String, String>> origDataContext = executionContext.getDataContext();

        //add node context data
        final Map<String, Map<String, String>> nodeContext =
            DataContextUtils.addContext("node", DataContextUtils.nodeData(node), origDataContext);


        //write the temp file and replace tokens in the script with values from the dataContext
        final File tempfile = BaseFileCopier.writeScriptTempFile(executionContext, file, input, content, node);


        //add some more data context values to allow templatized script-copy attribute
        final HashMap<String, String> scptexec = new HashMap<String, String>();
        //set up the data context to include the local temp file
        scptexec.put("file", tempfile.getAbsolutePath());
        if (null != workingdir) {
            //set up the data context to include the working dir
            scptexec.put("dir", workingdir.getAbsolutePath());
        }
        final Map<String, Map<String, String>> newDataContext = DataContextUtils.addContext("file-copy", scptexec,
            nodeContext);

        //use script-copy attribute and replace datareferences
        final String[] args = DataContextUtils.replaceDataReferences(scriptargs.split(" "), newDataContext);

        //create system environment variables from the data context
        final Map<String, String> envMap = DataContextUtils.generateEnvVarsFromContext(nodeContext);
        final ArrayList<String> envlist = new ArrayList<String>();
        for (final String key : envMap.keySet()) {
            final String envval = envMap.get(key);
            envlist.add(key + "=" + envval);
        }
        final String[] envarr = envlist.toArray(new String[envlist.size()]);


        int result = -1;
        boolean success = false;
        Thread errthread = null;
        Thread outthread = null;
        executionContext.getExecutionListener().log(3, "[script-copy] executing: " + StringArrayUtil.asString(args,
            " "));
        final Runtime runtime = Runtime.getRuntime();
        Process exec = null;
        try {
            exec = runtime.exec(args, envarr, workingdir);
        } catch (IOException e) {
            throw new FileCopierException(e);
        }
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            errthread = Streams.copyStreamThread(exec.getErrorStream(), System.err);
            outthread = Streams.copyStreamThread(exec.getInputStream(), byteArrayOutputStream);
            errthread.start();
            outthread.start();
            exec.getOutputStream().close();
            result = exec.waitFor();
            errthread.join();
            outthread.join();
            success = 0 == result;
        } catch (InterruptedException e) {
            throw new FileCopierException(e);
        } catch (IOException e) {
            throw new FileCopierException(e);
        }
        if (!success) {
            throw new FileCopierException("[script-copy]: external script failed with exit code: " + result);
        }

        //load string of output from outputstream
        final String output = byteArrayOutputStream.toString();
        if (null == output || output.length() < 1) {
            throw new FileCopierException("[script-copy]: No output from external script");
        }
        //TODO: require any specific format for the data?
        //look for first line of output
        final String[] split1 = output.split("(\\r?\\n)");
        if (split1.length < 1) {
            throw new FileCopierException("[script-copy]: No output from external script");
        }
        final String remotefilepath = split1[0];

        executionContext.getExecutionListener().log(3, "[script-copy]: result filepath: " + remotefilepath);

        return remotefilepath;
    }

}
