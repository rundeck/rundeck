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
* ScriptUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/13/11 5:15 PM
* 
*/
package com.dtolabs.rundeck.plugin.script;

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.utils.StringArrayUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

/**
 * ScriptUtil is used by the script plugin to execute processes
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptUtil {

    /**
     * Execute a process by invoking a specified shell command and passing the arguments to the shell.
     *
     * @param logger         logger
     * @param workingdir     working dir
     * @param scriptargs     arguments to the shell
     * @param envContext     Environment variable context
     * @param newDataContext context data to replace in the scriptargs
     * @param remoteShell    the remote shell script, which will be split on whitespace
     * @param logName        name of plugin to use in logging
     */
    static Process execShellProcess(final ExecutionListener logger, final File workingdir,
                                    final String scriptargs,
                                    final Map<String, Map<String, String>> envContext,
                                    final Map<String, Map<String, String>> newDataContext,
                                    final String remoteShell,
                                    final String logName) throws IOException {

        ArrayList<String> shells = new ArrayList<String>(Arrays.asList(remoteShell.split(" ")));

        //use script-copy attribute and replace datareferences
        final String newargs = DataContextUtils.replaceDataReferences(scriptargs, newDataContext);
        shells.add(newargs);

        final ProcessBuilder processBuilder = new ProcessBuilder(shells).directory(workingdir);
        final Map<String, String> environment = processBuilder.environment();
        //create system environment variables from the data context
        environment.putAll(DataContextUtils.generateEnvVarsFromContext(envContext));


        logger.log(3, "[" + logName + "] executing: " + remoteShell + " " + newargs);
        return processBuilder.start();
    }

    /**
     * Execute a process directly with some arguments
     *
     * @param logger         logger
     * @param workingdir     working dir
     * @param scriptargs     arguments to the shell
     * @param envContext     Environment variable context
     * @param newDataContext context data to replace in the scriptargs
     * @param logName        name of plugin to use in logging
     */
    static Process execProcess(final ExecutionListener logger, final File workingdir, final String scriptargs,
                               final Map<String, Map<String, String>> envContext,
                               final Map<String, Map<String, String>> newDataContext,
                               final String logName) throws IOException {
        //use script-exec attribute and replace datareferences
        final String[] args = DataContextUtils.replaceDataReferences(scriptargs.split(" "), newDataContext);

        //create system environment variables from the data context
        final Map<String, String> envMap = DataContextUtils.generateEnvVarsFromContext(envContext);
        final ArrayList<String> envlist = new ArrayList<String>();
        for (final String key : envMap.keySet()) {
            final String envval = envMap.get(key);
            envlist.add(key + "=" + envval);
        }
        final String[] envarr = envlist.toArray(new String[envlist.size()]);


        logger.log(3, "[" + logName + "] executing: " + StringArrayUtil.asString(args, " "));
        final Runtime runtime = Runtime.getRuntime();
        return runtime.exec(args, envarr, workingdir);
    }
}
