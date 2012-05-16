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
* ScriptNodeExecutor.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/5/11 10:00 AM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionException;
import com.dtolabs.rundeck.core.plugins.AbstractDescribableScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptDataContextUtil;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.utils.Streams;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * ScriptPluginNodeExecutor wraps the execution of the script and supplies the NodeExecutor interface.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginNodeExecutor extends AbstractDescribableScriptPlugin implements NodeExecutor {

    ScriptPluginNodeExecutor(final ScriptPluginProvider provider, final Framework framework) {
        super(provider, framework);
    }

    @Override
    public boolean isAllowCustomProperties() {
        return false;
    }

    static void validateScriptPlugin(final ScriptPluginProvider plugin) throws PluginException {
    }

    public NodeExecutorResult executeCommand(final ExecutionContext executionContext, final String[] command,
                                             final INodeEntry node) throws ExecutionException {
        final File workingdir = null;
        final ScriptPluginProvider plugin = getProvider();
        final File scriptfile = plugin.getScriptFile();
        final String pluginname = plugin.getName();
        executionContext.getExecutionListener().log(3,
            "[" + pluginname + "] execCommand started, command: " + StringArrayUtil.asString(command, " "));

        final String scriptargs = plugin.getScriptArgs();
        final String scriptinterpreter = plugin.getScriptInterpreter();
        final boolean interpreterargsquoted=plugin.getInterpreterArgsQuoted();
        executionContext.getExecutionListener().log(3,
            "[" + pluginname + "] scriptargs: " + scriptargs + ", interpreter: " + scriptinterpreter);


        /*
        String dirstring = null;
        dirstring = plugin.get
        if (null != node.getAttributes().get(DIR_ATTRIBUTE)) {
            dirstring = node.getAttributes().get(DIR_ATTRIBUTE);
        }
        if (null != dirstring) {
            workingdir = new File(dirstring);
        }*/

        //create mutable version of data context
        final Map<String, Map<String, String>> localDataContext = ScriptDataContextUtil.createScriptDataContextForProject(
            executionContext.getFramework(),
            executionContext.getFrameworkProject());
        localDataContext.get("plugin").putAll(createPluginDataContext());
        localDataContext.putAll(executionContext.getDataContext());

        //add some more data context values to allow templatized args
        final HashMap<String, String> scptexec = new HashMap<String, String>();
        scptexec.put("command", StringArrayUtil.asString(command, " "));
//        if (null != workingdir) {
//            scptexec.put("dir", workingdir.getAbsolutePath());
//        }
        localDataContext.put("exec", scptexec);

        final ArrayList<String> arglist = new ArrayList<String>();
        if(null!= scriptinterpreter) {
            arglist.addAll(Arrays.asList(scriptinterpreter.split(" ")));
        }
        if(null != scriptinterpreter && interpreterargsquoted){
            final StringBuilder sbuf = new StringBuilder(scriptfile.getAbsolutePath());
            if (null != scriptargs) {
                sbuf.append(" ");
                sbuf.append(DataContextUtils.replaceDataReferences(scriptargs, localDataContext));
            }
            arglist.add(sbuf.toString());
        }else{
            arglist.add(scriptfile.getAbsolutePath());
            if(null!=scriptargs) {
                arglist.addAll(Arrays.asList(DataContextUtils.replaceDataReferences(scriptargs.split(" "),
                    localDataContext)));
            }
        }
        final String[] finalargs = arglist.toArray(new String[arglist.size()]);

        //create system environment variables from the data context
        final Map<String, String> envMap = DataContextUtils.generateEnvVarsFromContext(localDataContext);
        final ArrayList<String> envlist = new ArrayList<String>();
        for (final Map.Entry<String, String> entry : envMap.entrySet()) {
            envlist.add(entry.getKey() + "=" + entry.getValue());
        }
        final String[] envarr = envlist.toArray(new String[envlist.size()]);

        int result = -1;
        boolean success = false;
        final Thread errthread;
        final Thread outthread;
        executionContext.getExecutionListener().log(3, "[" + pluginname + "] executing: " + StringArrayUtil.asString(
            finalargs,
            " "));
        final Runtime runtime = Runtime.getRuntime();
        final Process exec;
        try {
            exec = runtime.exec(finalargs, envarr, workingdir);
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
            "[" + pluginname + "]: result code: " + result + ", success: " + success);
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
                return "[" + pluginname + "] success: " + isSuccess() + ", result code: " + getResultCode();
            }
        };
    }

}
