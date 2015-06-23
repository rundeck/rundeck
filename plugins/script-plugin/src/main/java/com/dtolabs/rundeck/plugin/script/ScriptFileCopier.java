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

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.execution.service.DestinationFileCopier;
import com.dtolabs.rundeck.core.execution.service.FileCopierException;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.AbstractBaseDescription;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.utils.Streams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * ExternalScriptFileCopier plugin provides the FileCopier service by allowing an external script to handle the
 * particulars of copying a local file to a node. <p> The plugin is enabled by setting the "file-copier" attribute of a
 * node to "script-copy".  The script to use for copying the file is specified via an attribute called "script-copy".
 * </p> <p> The script-copy attribute is used as the entire command to execute to copy the file.  The destination file
 * location should be the only output from the command. </p><p>   In addition to normal Data context references in this
 * attribute, you can include these special data references: </p> <ul>
 * <li><pre>${file-copy.file}</pre>: This is the local filepath of the file that should be copied to the node.</li>
 * <li><pre>${file-copy.dir}</pre>: This is the value of the script-copy-dir attribute</li>
 * <li><pre>${file-copy.filename}</pre>: This is the name of the file without any path</li>
 * <li><pre>${file-copy.destination}</pre>: This is the value of the expected destination filepath for the file</li>
 * </ul>
 * So for example, if you wanted to change the way the script is invoked, you could specify the script-copy
 * like:
 * <pre>
 *         &lt;node name="mynode" ...
 *         remotecopy="/bin/remotecopy"
 *         script-copy="${node.remotecopy} ${script-copy.file} ${file-copy.destination} -- ${node.username}@${node.name}"/&gt;
 * </pre>
 * This would execute /bin/remotecopy and pass the path of the file to copy followed by -- and the node info
 * "username@hostname".
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin(name = "script-copy", service = ServiceNameConstants.FileCopier)
public class ScriptFileCopier implements DestinationFileCopier, Describable {
    public static String SERVICE_PROVIDER_NAME = "script-copy";
    public static String SCRIPT_ATTRIBUTE = "script-copy";
    public static String DIR_ATTRIBUTE = "script-copy-dir";
    public static String SHELL_ATTRIBUTE = "script-copy-shell";
    public static String REMOTE_FILEPATH_ATTRIBUTE = "script-copy-remote-filepath";
    private static final String SCRIPT_COPY_DEFAULT_COMMAND_PROPERTY = "plugin.script-copy.default.command";
    private static final String SCRIPT_COPY_DEFAULT_DIR_PROPERTY = "plugin.script-copy.default.dir";
    private static final String SCRIPT_COPY_DEFAULT_REMOTE_FILEPATH_PROPERTY =
        "plugin.script-copy.default.remote-filepath";
    private static final String SCRIPT_COPY_DEFAULT_REMOTE_SHELL =
        "plugin.script-copy.default.shell";
    public static final String CONFIG_COMMAND = "command";

    public static final String CONFIG_INTERPRETER = "interpreter";

    public static final String CONFIG_DIRECTORY = "directory";
    public static final String CONFIG_FILEPATH = "filepath";

    static final List<Property> properties = new ArrayList<Property>();
    static final Map<String, String> CONFIG_MAPPING;
    static final Map<String, String> CONFIG_MAPPING_FWK;

    static {
        properties.add(PropertyUtil.string(CONFIG_COMMAND, "Command",
                                           "Shell command to execute the file copy. Can include references to these variables:\n" +
                                                   "${file-copy.file}: This is the local path of the source file" +
                                                   " that should be copied to the node.\n" +
                                                   "${file-copy.dir}: Local working directory when executing the command.\n" +
                                                   "${file-copy.filename}: Source file name without path.\n" +
                                                   "${file-copy.destination}: This is the value " +
                                                   "of the expected destination filepath for the file",
                                           true, null));
        properties.add(PropertyUtil.string(CONFIG_FILEPATH, "Remote Filepath",
                                           "Remote filepath destination for copied scripts, can include " +
                                                   "${file-copy.filename} or any node or job attributes. If " +
                                                   "${file-copy.filename} is not specified, then it is assumed the path " +
                                                   "is a directory path, and the script file will be placed within the " +
                                                   "directory.",
                                           false, null));
        properties.add(PropertyUtil.string(CONFIG_INTERPRETER, "Interpreter",
                                           "Shell or interpreter to pass the command string to. Not required.",
                                           false, null));
        properties.add(PropertyUtil.string(CONFIG_DIRECTORY, "Directory",
                                           "Directory to execute within (optional)",
                                           false, null));

        final Map<String, String> mapping = new HashMap<String, String>();
        mapping.put(CONFIG_COMMAND, SCRIPT_COPY_DEFAULT_COMMAND_PROPERTY);
        mapping.put(CONFIG_FILEPATH, SCRIPT_COPY_DEFAULT_REMOTE_FILEPATH_PROPERTY);
        mapping.put(CONFIG_INTERPRETER, SCRIPT_COPY_DEFAULT_REMOTE_SHELL);
        mapping.put(CONFIG_DIRECTORY, SCRIPT_COPY_DEFAULT_DIR_PROPERTY);
        CONFIG_MAPPING = Collections.unmodifiableMap(mapping);

        CONFIG_MAPPING_FWK = Collections.unmodifiableMap(mapping);
    }

    public static final Description DESC = new AbstractBaseDescription() {
        public String getName() {
            return SERVICE_PROVIDER_NAME;
        }

        public String getTitle() {
            return "Script Execution";
        }

        public String getDescription() {
            return "Delegates file copying to an external script. Can be configured project-wide or on a per-node basis.";
        }

        public List<Property> getProperties() {
            return properties;
        }

        @Override
        public Map<String, String> getPropertiesMapping() {
            return CONFIG_MAPPING;
        }

        @Override
        public Map<String, String> getFwkPropertiesMapping() {
            return CONFIG_MAPPING_FWK;
        }
    };

    public Description getDescription() {
        return DESC;
    }

    /**
     * Copy inputstream
     */
    public String copyFileStream(final ExecutionContext executionContext, final InputStream inputStream,
                                 final INodeEntry node) throws FileCopierException {

        return copyFile(executionContext, null, inputStream, null, node, null);
    }

    /**
     * Copy existing file
     */
    public String copyFile(final ExecutionContext executionContext, final File file, final INodeEntry node) throws
                                                                                                            FileCopierException {
        return copyFile(executionContext, file, null, null, node,null);
    }

    /**
     * Copy string content
     */
    public String copyScriptContent(final ExecutionContext executionContext, final String s,
                                    final INodeEntry node) throws
                                                           FileCopierException {
        return copyFile(executionContext, null, null, s, node, null);
    }

    static enum Reason implements FailureReason {
        ScriptFileCopierPluginExpectedOutputMissing
    }

    @Override
    public String copyFileStream(ExecutionContext context, InputStream input, INodeEntry node, String destination) throws FileCopierException {
        return copyFile(context, null, input, null, node, destination);
    }

    @Override
    public String copyFile(ExecutionContext context, File file, INodeEntry node, String destination) throws FileCopierException {
        return copyFile(context, file, null, null, node, destination);
    }

    @Override
    public String copyScriptContent(ExecutionContext context, String script, INodeEntry node, String destination) throws FileCopierException {
        return copyFile(context, null, null, script, node, destination);
    }

    /**
     * Internal copy method accepting file, inputstream or string
     */
    String copyFile(
            final ExecutionContext executionContext,
            final File file,
            final InputStream input,
            final String content,
            final INodeEntry node,
            String remotePath
    ) throws
      FileCopierException
    {
        File workingdir = null;
        String scriptargs;
        String dirstring;

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
                + " property was configured for the project or framework.",
                StepFailureReason.ConfigurationFailure
            );
        }

        dirstring = framework.getProjectProperty(executionContext.getFrameworkProject(),
                                                 SCRIPT_COPY_DEFAULT_DIR_PROPERTY);
        if (null != node.getAttributes().get(DIR_ATTRIBUTE)) {
            dirstring = node.getAttributes().get(DIR_ATTRIBUTE);
        }
        if (null != dirstring && !"".equals(dirstring)) {
            workingdir = new File(dirstring);
        }

        final File srcFile =
                null != file ?
                        file :
                        BaseFileCopier.writeTempFile(executionContext, file, input, content);


        //create context data with node attributes
        Map<String, Map<String, String>> newDataContext;
        final Map<String, Map<String, String>> nodeContext =
                DataContextUtils.addContext("node", DataContextUtils.nodeData(node), executionContext.getDataContext());

        final HashMap<String, String> scptexec = new HashMap<String, String>(){{
            //set filename of source file
            put("filename", srcFile.getName());
            put("destfilename", srcFile.getName());
            put("file", srcFile.getAbsolutePath());
            //add file, dir, destination to the file-copy data
        }};
	
        if(null != remotePath && !remotePath.endsWith("/")) {
            scptexec.put("destfilename", new File(remotePath).getName());
        }

        if (null != workingdir) {
            //set up the data context to include the working dir
            scptexec.put("dir", workingdir.getAbsolutePath());
        }

        newDataContext = DataContextUtils.addContext("file-copy", scptexec, nodeContext);

        //expand remote filepath if we are copying a script
        String copiedFilepath;
        if (null == remotePath) {
            copiedFilepath = framework.getProjectProperty(executionContext.getFrameworkProject(),
                    SCRIPT_COPY_DEFAULT_REMOTE_FILEPATH_PROPERTY);
            if (null != node.getAttributes().get(REMOTE_FILEPATH_ATTRIBUTE)) {
                copiedFilepath = node.getAttributes().get(REMOTE_FILEPATH_ATTRIBUTE);
            }
            if (null != copiedFilepath) {
                if (!(copiedFilepath.contains("${file-copy.filename}")
                        || copiedFilepath.contains("${file-copy.file}"))
                        && !copiedFilepath.endsWith("/")) {
                    copiedFilepath += "/";
                }
                copiedFilepath = DataContextUtils.replaceDataReferences(copiedFilepath, newDataContext);
            }
        } else {
            //we are copying to a specific destination
            copiedFilepath = remotePath;
        }

        //put file in a directory
        if (null != copiedFilepath && copiedFilepath.endsWith("/")) {
            copiedFilepath += srcFile.getName();
        }

        scptexec.put("destination", null != copiedFilepath ? copiedFilepath : "");

        newDataContext = DataContextUtils.addContext("file-copy", scptexec, nodeContext);

        final Process exec;

        String remoteShell = framework.getProjectProperty(executionContext.getFrameworkProject(),
                                                          SCRIPT_COPY_DEFAULT_REMOTE_SHELL);
        if (null != node.getAttributes().get(SHELL_ATTRIBUTE)) {
            remoteShell = node.getAttributes().get(SHELL_ATTRIBUTE);
        }

        try {
            if (null != remoteShell) {
                exec = ScriptUtil.execShellProcess(executionContext.getExecutionListener(), workingdir, scriptargs,
                                                   nodeContext, newDataContext, remoteShell, "script-copy");
            } else {
                exec = ScriptUtil.execProcess(executionContext.getExecutionListener(), workingdir, scriptargs,
                                              nodeContext,
                                              newDataContext, "script-copy");
            }
        } catch (IOException e) {
            throw new FileCopierException(e.getMessage(), StepFailureReason.IOFailure, e);
        }finally {
            if(null == file) {
                if (!ScriptfileUtils.releaseTempFile(srcFile)) {
                    executionContext.getExecutionListener().log(
                            Constants.WARN_LEVEL,
                            "Unable to remove local temp file: " + srcFile.getAbsolutePath()
                    );
                }
            }
        }

        final Thread errthread;
        Thread outthread = null;
        final int result;
        final boolean success;


        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            exec.getOutputStream().close();
            errthread = Streams.copyStreamThread(exec.getErrorStream(), System.err);
            if (null == copiedFilepath) {
                outthread = Streams.copyStreamThread(exec.getInputStream(), byteArrayOutputStream);
            }
            errthread.start();
            if (null != outthread) {
                outthread.start();
            }
            result = exec.waitFor();
            System.err.flush();
            byteArrayOutputStream.flush();
            errthread.join();
            if (null != outthread) {
                outthread.join();
            }
            exec.getErrorStream().close();
            exec.getInputStream().close();
            success = 0 == result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileCopierException(e.getMessage(), StepFailureReason.Interrupted, e);
        } catch (IOException e) {
            throw new FileCopierException(e.getMessage(), StepFailureReason.IOFailure, e);
        }
        if (!success) {
            if (scriptargs.contains("${file-copy.destination}") && null == copiedFilepath) {
                executionContext.getExecutionListener().log(0,
                        "[script-copy]: ${file-copy.destination} is referenced in the file-copy script, but its value " +
                                "could not be determined. " +
                                "The node " + node.getNodename() + " may need a " + REMOTE_FILEPATH_ATTRIBUTE
                                + " attribute.");
            }
            throw new FileCopierException("[script-copy]: external script failed with exit code: " + result,
                                          NodeStepFailureReason.NonZeroResultCode);
        }

        if (null != copiedFilepath) {
            return copiedFilepath;
        }
        //load string of output from outputstream
        final String output = byteArrayOutputStream.toString();
        if (null == output || output.length() < 1) {
            throw new FileCopierException("[script-copy]: No output from external script",
                                          Reason.ScriptFileCopierPluginExpectedOutputMissing);
        }
        //TODO: require any specific format for the data?
        //look for first line of output
        final String[] split1 = output.split("(\\r?\\n)");
        if (split1.length < 1) {
            throw new FileCopierException("[script-copy]: No output from external script",
                                          Reason.ScriptFileCopierPluginExpectedOutputMissing);
        }
        final String remotefilepath = split1[0];

        executionContext.getExecutionListener().log(3, "[script-copy]: result filepath: " + remotefilepath);

        return remotefilepath;
    }


}
