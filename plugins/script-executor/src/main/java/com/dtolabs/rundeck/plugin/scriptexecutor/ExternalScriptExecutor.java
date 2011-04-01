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
package com.dtolabs.rundeck.plugin.scriptexecutor;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.impl.common.AntSupport;
import com.dtolabs.rundeck.core.execution.service.NodeExecutor;
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.utils.Streams;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ExternalScriptExecutor is a {@link NodeExecutor} that delegates execution to an external script. The external script
 * should be specified for a node via the {@value #SCRIPT_ATTRIBUTE} attribute on a node. This should be the local
 * filepath for a script to execute.  If using Windows, this should be a .bat file.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
@Plugin (name = "script-exec", service = "NodeExecutor")
public class ExternalScriptExecutor implements NodeExecutor {
    public static String SCRIPT_ATTRIBUTE = "script-exec-file";
    public static String ARGS_ATTRIBUTE = "script-exec-args";
    public static String DIR_ATTRIBUTE = "script-exec-dir";

    public NodeExecutorResult executeCommand(final ExecutionContext executionContext, final String[] command,
                                             final INodeEntry node) throws ExecutionException {
        File scriptfile;
        File workingdir = null;
        String scriptargs = null;
        if (null == node.getAttributes() || null == node.getAttributes().get(SCRIPT_ATTRIBUTE)) {
            throw new ExecutionException(
                "[script-exec node executor] Node was missing the " + SCRIPT_ATTRIBUTE + " attribute: " + node.getNodename());
        }
        scriptfile = new File(node.getAttributes().get(SCRIPT_ATTRIBUTE));

        if (null != node.getAttributes().get(ARGS_ATTRIBUTE)) {
            scriptargs = node.getAttributes().get(ARGS_ATTRIBUTE);
        }
        if (null != node.getAttributes().get(DIR_ATTRIBUTE)) {
            workingdir = new File(node.getAttributes().get(DIR_ATTRIBUTE));
        }

        //expand data references within the command to execute
        final String[] expandCommand = DataContextUtils.replaceDataReferences(command,
            executionContext.getDataContext());


        //add some more data context values to allow templatized script-exec-args
        final HashMap<String, String> scptexec = new HashMap<String, String>();
        scptexec.put("command", StringArrayUtil.asString(expandCommand, " "));
        scptexec.put("file", scriptfile.getAbsolutePath());
        if (null != workingdir) {
            scptexec.put("dir", workingdir.getAbsolutePath());
        }
        final Map<String, Map<String, String>> newDataContext = DataContextUtils.addContext("script-exec", scptexec,
            executionContext.getDataContext());

        //generate args that we are going to execute
        final ArrayList<String> argslist = new ArrayList<String>();
        if (null != scriptargs) {
            //use script-exec-args attribute and replace datareferences
            final String[] split = scriptargs.split(" ");
            argslist.addAll(Arrays.asList(DataContextUtils.replaceDataReferences(split, newDataContext)));
        } else {
            //execute the scriptfile with the command as the arguments
            argslist.add(scriptfile.getAbsolutePath());
            argslist.add(node.getNodename());
            argslist.addAll(Arrays.asList(expandCommand));
        }

        /**
         * final args array
         */
        final String[] args = argslist.toArray(new String[argslist.size()]);

        //create system environment variables from the data context
        final Map<String, String> envMap = DataContextUtils.generateEnvVarsFromContext(
            executionContext.getDataContext());
        final ArrayList<String> envlist = new ArrayList<String>();
        for (final String key : envMap.keySet()) {
            final String envval = envMap.get(key);
            envlist.add(key + "=" + envval);
        }
        final String[] envarr = envlist.toArray(new String[envlist.size()]);


        int result = -1;
        boolean success = false;
        try {
            final Runtime runtime = Runtime.getRuntime();
            final Process exec = runtime.exec(args, envarr, workingdir);
            System.err.println("exec started: " + StringArrayUtil.asString(args, " "));
            //TODO: bind process output/error to System.out/System.err

            Thread errthread = Streams.copyStreamThread(exec.getErrorStream(), System.err);
            Thread outthread = Streams.copyStreamThread(exec.getInputStream(), System.out);
            errthread.start();
            outthread.start();
            exec.getOutputStream().close();
            result = exec.waitFor();
            System.err.println("exec result: " + result);
            success = 0 == result;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final int returnresult = result;
        final boolean returnsuccess = success;
        return new NodeExecutorResult() {
            public int getResultCode() {
                return returnresult;
            }

            public boolean isSuccess() {
                return returnsuccess;
            }
        };
    }
}
