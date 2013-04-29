/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
* ScriptExecUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 12/13/12 5:05 PM
* 
*/
package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.cli.CLIUtils;
import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.utils.Streams;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;


/**
 * Provides methods for running scripts/commands.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptExecUtil {

    /**
     * Run a command with environment variables in a working dir, and copy the streams
     *
     * @param command      the command array to run
     * @param envMap       the environment variables to pass in
     * @param workingdir   optional working dir location (or null)
     * @param outputStream stream for stdout
     * @param errorStream  stream for stderr
     *
     * @return the exit code of the command
     *
     * @throws IOException          if any IO exception occurs
     * @throws InterruptedException if interrupted while waiting for the command to finish
     */
    public static int runLocalCommand(final String[] command,
                                      final Map<String, String> envMap, final File workingdir,
                                      final OutputStream outputStream, final OutputStream errorStream)
        throws IOException, InterruptedException {
        final String[] envarr = createEnvironmentArray(envMap);

        final Runtime runtime = Runtime.getRuntime();
        final Process exec = runtime.exec(command, envarr, workingdir);
        final Streams.StreamCopyThread errthread = Streams.copyStreamThread(exec.getErrorStream(), errorStream);
        final Streams.StreamCopyThread outthread = Streams.copyStreamThread(exec.getInputStream(), outputStream);
        errthread.start();
        outthread.start();
        exec.getOutputStream().close();
        final int result = exec.waitFor();
        errthread.join();
        outthread.join();
        if (null != outthread.getException()) {
            throw outthread.getException();
        }
        if (null != errthread.getException()) {
            throw errthread.getException();
        }
        return result;
    }

    /**
     * Create the environment array for executing via {@link Runtime}.
     */
    private static String[] createEnvironmentArray(final Map<String, String> envMap) {
        final ArrayList<String> envlist = new ArrayList<String>();
        for (final Map.Entry<String, String> entry : envMap.entrySet()) {
            envlist.add(entry.getKey() + "=" + entry.getValue());
        }
        return envlist.toArray(new String[envlist.size()]);
    }

    /**
     * Generate argument array for a script file invocation
     *
     * @param localDataContext      data context properties to expand among the args
     * @param scriptargs            arguments to the script file
     * @param scriptargsarr         arguments to the script file as an array
     * @param scriptinterpreter     interpreter invocation for the file, or null to invoke it directly
     * @param interpreterargsquoted if true, pass the script file and args as a single argument to the interpreter
     * @param filepath              remote filepath for the script
     */
    public static String[] createScriptArgs(final Map<String, Map<String, String>> localDataContext,
                                            final String scriptargs,
                                            final String[] scriptargsarr,
                                            final String scriptinterpreter,
                                            final boolean interpreterargsquoted, final String filepath) {
        return createScriptArgs(localDataContext, null, scriptargs, scriptargsarr, scriptinterpreter,
                interpreterargsquoted, filepath);
    }

    /**
     * Generate argument array for a script file invocation
     *
     * @param localDataContext      data context properties to expand among the args
     * @param node                  node to use for os-type argument quoting.
     * @param scriptargs            arguments to the script file
     * @param scriptargsarr         arguments to the script file as an array
     * @param scriptinterpreter     interpreter invocation for the file, or null to invoke it directly
     * @param interpreterargsquoted if true, pass the script file and args as a single argument to the interpreter
     * @param filepath              remote filepath for the script
     */
    public static String[] createScriptArgs(final Map<String, Map<String, String>> localDataContext,
            final INodeEntry node,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted, final String filepath) {
        final ArrayList<String> arglist = new ArrayList<String>();
        if (null != scriptinterpreter) {
            arglist.addAll(Arrays.asList(scriptinterpreter.split(" ")));
        }
        if (null != scriptinterpreter && interpreterargsquoted) {
            final StringBuilder sbuf = new StringBuilder(filepath);
            if (null != scriptargs) {
                sbuf.append(" ");
                sbuf.append(DataContextUtils.replaceDataReferences(scriptargs, localDataContext));
            } else if (null != scriptargsarr) {

                final String[] strings = DataContextUtils.replaceDataReferences(scriptargsarr, localDataContext);
                for (final String string : strings) {
                    sbuf.append(" ");
                    sbuf.append(string);
                }
            }
            arglist.add(sbuf.toString());
        } else {
            arglist.add(filepath);
            if (null != scriptargs) {
                arglist.addAll(Arrays.asList(DataContextUtils.replaceDataReferences(scriptargs.split(" "),
                        localDataContext)));
            } else if (null != scriptargsarr) {
                final String[] newargs = DataContextUtils.replaceDataReferences(scriptargsarr, localDataContext);
                Converter<String, String> quote;
                if (null != node) {
                    quote = CLIUtils.argumentQuoteForOperatingSystem(node.getOsFamily());
                } else {
                    quote = CLIUtils.argumentQuoteForOperatingSystem(null);
                }
                //quote args that have substituted context input, or have whitespace
                //allow other args to be used literally
                for (int i = 0; i < newargs.length; i++) {
                    String replaced = newargs[i];
                    if (!replaced.equals(scriptargsarr[i]) || CLIUtils.containsSpace(replaced)) {
                        arglist.add(quote.convert(replaced));
                    } else {
                        arglist.add(replaced);
                    }
                }
            }
        }
        return arglist.toArray(new String[arglist.size()]);
    }
}
