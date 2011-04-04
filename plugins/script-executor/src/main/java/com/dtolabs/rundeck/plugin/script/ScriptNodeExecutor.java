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
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.utils.Streams;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * <p>ExternalScriptExecutor is a {@link NodeExecutor} that delegates execution to an external script. The external
 * script should be specified for a node via the {@value #SCRIPT_ATTRIBUTE} attribute on a node. This should be the full
 * command to execute </p><p>   In addition to normal Data context references in this attribute, you can include these
 * special data references: </p> <ul>
 * <li><pre>${script-exec.command}</pre>: This is the user-entered command to execute</li>
 * <li><pre>${script-exec.dir}</pre>: This is the value of the script-exec-dir attribute</li>
 * </ul> <p> So for example, if you wanted to change the way the script is invoked, you could specify the
 * script-exec-args like:
 * <pre>
 *         &lt;node name="mynode" ...
 *         myscript-file="/some/script.sh"
 *         script-exec="/bin/zsh ${node.myscript-file} ${script-exec.command} -- ${node.username}@${node.name}"/>
 * </pre>
 * This would execute /bin/zsh and pass the value of the script-exec-file attribute followed by the command, followed by
 * -- and the node "username@hostname". </p>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "script-exec", service = "NodeExecutor")
public class ScriptNodeExecutor implements NodeExecutor {
    public static String SCRIPT_ATTRIBUTE = "script-exec";
    public static String DIR_ATTRIBUTE = "script-exec-dir";
    private static final String SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY = "script-exec.default.command";
    private static final String SCRIPT_EXEC_DEFAULT_DIR_PROPERTY = "script-exec.default.dir";

    public NodeExecutorResult executeCommand(final ExecutionContext executionContext, final String[] command,
                                             final INodeEntry node) throws ExecutionException {
        File workingdir = null;
        String scriptargs = null;
        String dirstring = null;

        //get project or framework property for script-exec args
        final Framework framework = executionContext.getFramework();
        //look for specific property
        scriptargs = framework.getProjectProperty(executionContext.getFrameworkProject(),
            SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY);


        if (null != node.getAttributes().get(SCRIPT_ATTRIBUTE)) {
            scriptargs = node.getAttributes().get(SCRIPT_ATTRIBUTE);
        }
        if (null == scriptargs) {
            throw new ExecutionException(
                "[script-exec node executor] no script-exec attribute " + SCRIPT_ATTRIBUTE + " was found on node: "
                + node
                    .getNodename() + ", and no " + SCRIPT_EXEC_DEFAULT_COMMAND_PROPERTY
                + " property was configured for the project or framework.");
        }

        dirstring = framework.getProjectProperty(executionContext.getFrameworkProject(),
            SCRIPT_EXEC_DEFAULT_DIR_PROPERTY);
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

        //expand data references within the command to execute
        final String[] expandCommand = DataContextUtils.replaceDataReferences(command,
            nodeContext);


        //add some more data context values to allow templatized script-exec
        final HashMap<String, String> scptexec = new HashMap<String, String>();
        scptexec.put("command", StringArrayUtil.asString(expandCommand, " "));
        if (null != workingdir) {
            scptexec.put("dir", workingdir.getAbsolutePath());
        }
        final Map<String, Map<String, String>> newDataContext = DataContextUtils.addContext("script-exec", scptexec,
            nodeContext);

        //use script-exec attribute and replace datareferences
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
        String errmsg;
        executionContext.getExecutionListener().log(3, "[script-exec] executing: " + StringArrayUtil.asString(args,
            " "));
        final Runtime runtime = Runtime.getRuntime();
        Process exec = null;
        try {
            exec = runtime.exec(args, envarr, workingdir);
        } catch (IOException e) {
            throw new ExecutionException(e);
        }
        try {
            errthread = Streams.copyStreamThread(exec.getErrorStream(), System.err);
            outthread = Streams.copyStreamThread(exec.getInputStream(), System.out);
            errthread.start();
            outthread.start();
            exec.getOutputStream().close();
            result = exec.waitFor();
            errthread.join();
            outthread.join();
            success = 0 == result;
        } catch (InterruptedException e) {
            e.printStackTrace(System.err);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        executionContext.getExecutionListener().log(3,
            "[script-exec]: result code: " + result + ", success: " + success);
        final int returnresult = result;
        final boolean returnsuccess = success;
        return new NodeExecutorResult() {
            public int getResultCode() {
                return returnresult;
            }

            public boolean isSuccess() {
                return returnsuccess;
            }

            @Override
            public String toString() {
                return "[script-exec] success: " + isSuccess() + ", result code: " + getResultCode();
            }
        };
    }
}
