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
* ExternalScriptExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/31/11 6:18 PM
* 
*/
package com.dtolabs.rundeck.plugin.script;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepFailureReason;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.AbstractBaseDescription;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.utils.Streams;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * <p>ExternalScriptExecutor is a {@link NodeExecutor} that delegates execution to an external script. The external
 * script should be specified for a node via the {@value #SCRIPT_ATTRIBUTE} attribute on a node. This should be the full
 * command to execute </p><p>   In addition to normal Data context references in this attribute, you can include these
 * special data references: </p> <ul>
 * <li><pre>${exec.command}</pre>: This is the user-entered command to execute</li>
 * <li><pre>${exec.dir}</pre>: This is the value of the script-exec-dir attribute</li>
 * </ul>
 * So for example, if you wanted to change the way the script is invoked, you could specify the
 * script-exec-args like:
 * <pre>
 *         &lt;node name="mynode" ...
 *         myscript-file="/some/script.sh"
 *         script-exec="/bin/zsh ${node.myscript-file} ${script-exec.command} -- ${node.username}@${node.name}"/&gt;
 * </pre>
 * This would execute /bin/zsh and pass the value of the script-exec-file attribute followed by the command, followed by
 * -- and the node "username@hostname".
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin(name = "script-exec", service = ServiceNameConstants.NodeExecutor)
public class ScriptNodeExecutor implements NodeExecutor, Describable {
    public static final String SERVICE_PROVIDER_NAME = "script-exec";
    public static final String SCRIPT_ATTRIBUTE = "script-exec";
    public static final String DIR_ATTRIBUTE = "script-exec-dir";
    public static final String SHELL_ATTRIBUTE = "script-exec-shell";
    private static final String SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY = "plugin.script-exec.default.command";
    private static final String SCRIPT_EXEC_DEFAULT_DIR_PROPERTY = "plugin.script-exec.default.dir";
    private static final String SCRIPT_EXEC_DEFAULT_REMOTE_SHELL =
        "plugin.script-exec.default.shell";

    static final List<Property> properties = new ArrayList<Property>();

    public static final String CONFIG_COMMAND = "command";

    public static final String CONFIG_INTERPRETER = "interpreter";

    public static final String CONFIG_DIRECTORY = "directory";
    static final Map<String, String> CONFIG_MAPPING;
    static final Map<String, String> CONFIG_MAPPING_FWK;

    static {
        properties.add(PropertyUtil.string(CONFIG_COMMAND, "Command",
                                           "Shell command to execute",
                                           true, null));
        properties.add(PropertyUtil.string(CONFIG_INTERPRETER, "Interpreter",
                                           "Shell or interpreter to pass the command string to. Not required.",
                                           false, null));
        properties.add(PropertyUtil.string(CONFIG_DIRECTORY, "Directory",
                                           "Directory to execute within",
                                           false, null));


        final Map<String, String> mapping = new HashMap<String, String>();
        mapping.put(CONFIG_COMMAND, SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY);
        mapping.put(CONFIG_INTERPRETER, SCRIPT_EXEC_DEFAULT_REMOTE_SHELL);
        mapping.put(CONFIG_DIRECTORY, SCRIPT_EXEC_DEFAULT_DIR_PROPERTY);
        CONFIG_MAPPING = Collections.unmodifiableMap(mapping);

        final Map<String, String> mapping2 = new HashMap<String, String>();
        mapping2.put(CONFIG_COMMAND, SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY);
        mapping2.put(CONFIG_INTERPRETER, SCRIPT_EXEC_DEFAULT_REMOTE_SHELL);
        mapping2.put(CONFIG_DIRECTORY, SCRIPT_EXEC_DEFAULT_DIR_PROPERTY);

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
            return "Delegates command execution to an external script. Can be configured project-wide or on a per-node basis.";
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

    public NodeExecutorResult executeCommand(final ExecutionContext executionContext, final String[] command,
                                             final INodeEntry node)  {
        File workingdir = null;
        String scriptargs;
        String dirstring;

        //get project or framework property for script-exec args
        final Framework framework = executionContext.getFramework();
        //look for specific property
        scriptargs = framework.getProjectProperty(executionContext.getFrameworkProject(),
                                                  SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY);


        if (null != node.getAttributes().get(SCRIPT_ATTRIBUTE)) {
            scriptargs = node.getAttributes().get(SCRIPT_ATTRIBUTE);
        }
        if (null == scriptargs) {
            return NodeExecutorResultImpl.createFailure(StepFailureReason.ConfigurationFailure,
                                                        "[script-exec node executor] no script-exec attribute "
                                                        + SCRIPT_ATTRIBUTE + " was found on node: "
                                                        + node
                                                            .getNodename() + ", and no "
                                                        + SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY
                                                        + " property was configured for the project or framework.",
                                                        node);
        }

        dirstring = framework.getProjectProperty(executionContext.getFrameworkProject(),
                                                 SCRIPT_EXEC_DEFAULT_DIR_PROPERTY);
        if (null != node.getAttributes().get(DIR_ATTRIBUTE)) {
            dirstring = node.getAttributes().get(DIR_ATTRIBUTE);
        }
        if (null != dirstring && !"".equals(dirstring)) {
            workingdir = new File(dirstring);
        }

        final Map<String, Map<String, String>> dataContext = executionContext.getDataContext();

        //add some more data context values to allow templatized script-exec
        final HashMap<String, String> scptexec = new HashMap<String, String>();
        scptexec.put("command", StringArrayUtil.asString(command, " "));
        if (null != workingdir) {
            scptexec.put("dir", workingdir.getAbsolutePath());
        }
        final Map<String, Map<String, String>> newDataContext = DataContextUtils.addContext("exec", scptexec,
                                                                                            dataContext);

        final Process exec;

        String remoteShell = framework.getProjectProperty(executionContext.getFrameworkProject(),
                                                          SCRIPT_EXEC_DEFAULT_REMOTE_SHELL);
        if (null != node.getAttributes().get(SHELL_ATTRIBUTE)) {
            remoteShell = node.getAttributes().get(SHELL_ATTRIBUTE);
        }
        try {
            if (null != remoteShell) {
                exec = ScriptUtil.execShellProcess(executionContext.getExecutionListener(), workingdir, scriptargs,
                                                   dataContext, newDataContext, remoteShell, "script-exec");
            } else {
                exec = ScriptUtil.execProcess(executionContext.getExecutionListener(), workingdir, scriptargs,
                                              dataContext,
                                              newDataContext, "script-exec");
            }
        } catch (IOException e) {
            return NodeExecutorResultImpl.createFailure(StepFailureReason.IOFailure, e.getMessage(), e, node, -1);
        }

        int result = -1;
        boolean success = false;
        Thread errthread;
        Thread outthread;
        FailureReason reason;
        String message;
        try {
            exec.getOutputStream().close();
            errthread = Streams.copyStreamThread(exec.getErrorStream(), System.err);
            outthread = Streams.copyStreamThread(exec.getInputStream(), System.out);
            errthread.start();
            outthread.start();
            result = exec.waitFor();
            System.err.flush();
            System.out.flush();
            errthread.join();
            outthread.join();
            exec.getErrorStream().close();
            exec.getInputStream().close();
            success = 0 == result;
            executionContext.getExecutionListener().log(3,
                                                        "[script-exec]: result code: " + result + ", success: "
                                                        + success);
            if (success) {
                return NodeExecutorResultImpl.createSuccess(node);
            }
            reason = NodeStepFailureReason.NonZeroResultCode;
            message = "Result code was " + result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            reason = StepFailureReason.Interrupted;
            message = e.getMessage();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            reason = StepFailureReason.IOFailure;
            message = e.getMessage();
        }
        executionContext.getExecutionListener().log(3,
                                                    "[script-exec]: result code: " + result + ", success: " + success);
        return NodeExecutorResultImpl.createFailure(reason, message, node, result);
    }

    public Description getDescription() {
        return DESC;
    }
}
