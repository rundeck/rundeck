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
* ScriptPluginFileCopier.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/5/11 10:11 AM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.impl.common.BaseFileCopier;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.utils.Streams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * ScriptPluginFileCopier wraps the execution of the script and supplies the FileCopier interface.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginFileCopier implements FileCopier {
    private ScriptPluginProvider plugin;

    public ScriptPluginFileCopier(ScriptPluginProvider plugin) {
        this.plugin = plugin;
    }

    static void validateScriptPlugin(final ScriptPluginProvider plugin) throws PluginException {
        if (null == plugin.getScriptArgs()) {
            throw new PluginException(
                "no script-args defined for provider: " + plugin);
        }
    }

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
        final String pluginname = plugin.getName();
        final File scriptfile = plugin.getScriptFile();

        //look for specific property
        scriptargs = plugin.getScriptArgs();
        final String scriptinterpreter = plugin.getScriptInterpreter();


        if (null == scriptargs) {
            throw new FileCopierException(
                "[" + pluginname + " file copier] no script-args defined for plugin");
        }

        /*dirstring = framework.getProjectProperty(executionContext.getFrameworkProject(),
            SCRIPT_COPY_DEFAULT_DIR_PROPERTY);
        if (null != node.getAttributes().get(DIR_ATTRIBUTE)) {
            dirstring = node.getAttributes().get(DIR_ATTRIBUTE);
        }
        if (null != dirstring) {
            workingdir = new File(dirstring);
        }*/

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


        final ArrayList<String> arglist = new ArrayList<String>();
        if (null != scriptinterpreter) {
            arglist.addAll(Arrays.asList(scriptinterpreter.split(" ")));
        }
        arglist.add(scriptfile.getAbsolutePath());
        if (null != scriptargs) {
            arglist.addAll(Arrays.asList(DataContextUtils.replaceDataReferences(scriptargs.split(" "),
                newDataContext)));
        }
        final String[] finalargs = arglist.toArray(new String[arglist.size()]);

        //create system environment variables from the data context
        final Map<String, String> envMap = DataContextUtils.generateEnvVarsFromContext(newDataContext);
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
        executionContext.getExecutionListener().log(3, "[" + pluginname + "] executing: " + StringArrayUtil.asString(
            finalargs,
            " "));
        final Runtime runtime = Runtime.getRuntime();
        Process exec = null;
        try {
            exec = runtime.exec(finalargs, envarr, workingdir);
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
            throw new FileCopierException("[" + pluginname + "]: external script failed with exit code: " + result);
        }

        //load string of output from outputstream
        final String output = byteArrayOutputStream.toString();
        if (null == output || output.length() < 1) {
            throw new FileCopierException("[" + pluginname + "]: No output from external script");
        }
        //TODO: require any specific format for the data?
        //look for first line of output
        final String[] split1 = output.split("(\\r?\\n)");
        if (split1.length < 1) {
            throw new FileCopierException("[" + pluginname + "]: No output from external script");
        }
        final String remotefilepath = split1[0];

        executionContext.getExecutionListener().log(3, "[" + pluginname + "]: result filepath: " + remotefilepath);

        return remotefilepath;
    }
}
