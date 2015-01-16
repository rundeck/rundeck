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
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.utils.Streams;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides methods for running scripts/commands.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptExecUtil {

    /**
     * Run a command with environment variables in a working dir, and copy the streams
     *
     *
     * @param osFamily local node os family
     * @param execArgList      the ExecArgList to run
     * @param workingdir   optional working dir location (or null)
     * @param outputStream stream for stdout
     * @param errorStream  stream for stderr
     * @param dataContext data
     *
     *
     * @return the exit code of the command
     *
     * @throws IOException          if any IO exception occurs
     * @throws InterruptedException if interrupted while waiting for the command to finish
     */
    public static int runLocalCommand(
            final String osFamily,
            final ExecArgList execArgList,
            final Map<String, Map<String, String>> dataContext,
            final File workingdir,
            final OutputStream outputStream,
            final OutputStream errorStream
    )
            throws IOException, InterruptedException
    {
        final Map<String, String> envVars =
                DataContextUtils.generateEnvVarsFromContext(dataContext);

        final ArrayList<String> strings =
                execArgList.buildCommandForNode(dataContext, osFamily);

        return runLocalCommand(
                strings.toArray(new String[strings.size()]),
                envVars,
                workingdir,
                outputStream,
                errorStream
        );
    }
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
     *                              @return args
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
     * @param filepath              remote filepath for the script
     * @param scriptargs            arguments to the script file
     * @param scriptargsarr         arguments to the script file as an array
     * @param scriptinterpreter     interpreter invocation for the file, or null to invoke it directly, can include ${scriptfile}
     * @param interpreterargsquoted if true, pass the script file and args as a single argument to the interpreter
     *                              @return arg list
     */
    public static ExecArgList createScriptArgList(final String filepath, final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted) {

        ExecArgList.Builder builder = ExecArgList.builder();
        boolean seenFilepath=false;
        if (null != scriptinterpreter) {
            String[] burst = OptsUtil.burst(scriptinterpreter);
            List<String> args = new ArrayList<String>();
            for (String arg : burst) {
                if (arg.contains("${scriptfile}")) {
                    args.add(arg.replaceAll(Pattern.quote("${scriptfile}"),
                            Matcher.quoteReplacement(filepath)));
                    seenFilepath = true;
                }else{
                    args.add(arg);
                }
            }
            builder.args(args, false);
        }
        if (null != scriptinterpreter && interpreterargsquoted) {
            ExecArgList.Builder sub = builder.subList(true);
            addScriptFileArgList(seenFilepath?null:filepath, scriptargs, scriptargsarr, sub, needsQuoting);
            sub.parent();
        } else {
            addScriptFileArgList(seenFilepath ? null : filepath, scriptargs, scriptargsarr, builder, needsQuoting);
        }
        return builder.build();
    }

    static Predicate any(Predicate... preds) {
        return PredicateUtils.anyPredicate(preds);
    }

    static final Predicate needsQuoting = any(
            DataContextUtils.stringContainsPropertyReferencePredicate,
            CLIUtils.stringContainsWhitespacePredicate,
            CLIUtils.stringContainsQuotePredicate
    );


    private static void addScriptFileArgList(String filepath, String scriptargs, String[] scriptargsarr,
            ExecArgList.Builder builder, Predicate quoted) {
        if(null!=filepath){
            builder.arg(filepath, false);
        }
        if (null != scriptargs) {
            builder.args(OptsUtil.burst(scriptargs), quoted);
        } else if (null != scriptargsarr) {
            builder.args(scriptargsarr, quoted);
        }
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
     *                              @return args
     */
    public static String[] createScriptArgs(final Map<String, Map<String, String>> localDataContext,
            final INodeEntry node,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted, final String filepath) {
        final ArrayList<String> arglist = new ArrayList<String>();
        if (null != scriptinterpreter) {
            arglist.addAll(Arrays.asList(OptsUtil.burst(scriptinterpreter)));
        }
        if (null != scriptinterpreter && interpreterargsquoted) {
            final ArrayList<String> sublist = new ArrayList<String>();
            sublist.add(filepath);
            addQuotedArgs(localDataContext, node, scriptargs, scriptargsarr, sublist, true);
            arglist.add(DataContextUtils.join(sublist, " "));
        } else {
            arglist.add(filepath);
            addQuotedArgs(localDataContext, node, scriptargs, scriptargsarr, arglist, false);
        }
        return arglist.toArray(new String[arglist.size()]);
    }

    private static void addQuotedArgs(Map<String, Map<String, String>> localDataContext, INodeEntry node, String
            scriptargs, String[] scriptargsarr, ArrayList<String> arglist, boolean quoted) {
        if (null != scriptargs) {
            arglist.addAll(Arrays.asList(DataContextUtils.replaceDataReferences(scriptargs.split(" "),
                    localDataContext)));
        } else if (null != scriptargsarr) {
            if (!quoted) {
                arglist.addAll(Arrays.asList(scriptargsarr));
            } else {
                final String[] newargs = DataContextUtils.replaceDataReferences(scriptargsarr, localDataContext);
                Converter<String, String> quote = getQuoteConverterForNode(node);
                //quote args that have substituted context input, or have whitespace
                //allow other args to be used literally
                for (int i = 0; i < newargs.length; i++) {
                    String replaced = newargs[i];
                    if (null != quote && (!replaced.equals(scriptargsarr[i]) || CLIUtils.containsSpace(replaced))) {
                        arglist.add(quote.convert(replaced));
                    } else {
                        arglist.add(replaced);
                    }
                }
            }
        }
    }

    private static Converter<String, String> getQuoteConverterForNode(INodeEntry node) {
        Converter<String, String> quote;
        if (null != node) {
            quote = CLIUtils.argumentQuoteForOperatingSystem(node.getOsFamily());
        } else {
            quote = CLIUtils.argumentQuoteForOperatingSystem(null);
        }
        return quote;
    }
}
