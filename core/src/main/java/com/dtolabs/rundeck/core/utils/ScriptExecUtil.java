/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.ExecArgList;
import com.dtolabs.utils.Streams;
import org.apache.tools.ant.taskdefs.Execute;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Provides methods for running scripts/commands.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptExecUtil {

    /**
     * Time in milliseconds to wait for stream copy threads to drain buffered output after the
     * parent process exits before assuming a background process (started via {@code &}) has
     * inherited the process file descriptors and is keeping the streams open indefinitely.
     * <p>
     * When the shell exits normally, any pending output in the kernel pipe buffer is readable
     * immediately, so legitimate output drains well within this window. If the threads are still
     * alive after this timeout it means a background child process holds the write-end of a
     * pipe open, and the streams are forcibly closed to unblock the threads.
     */
    static final long STREAM_DRAIN_TIMEOUT_MS = 500;

    /**
     * Time in milliseconds to wait after forcibly closing streams for the stream copy threads
     * to notice the close and exit. On Linux, closing the read-end of a pipe from one thread
     * does not reliably interrupt another thread already blocked in a native {@code read()}
     * syscall on that same file descriptor. If threads are still alive after this window,
     * we stop waiting — the parent shell has already exited and we have its exit code.
     * Threads will terminate on their own when the background process eventually closes its
     * inherited pipe descriptors. This matches Ant's PumpStreamHandler behavior.
     */
    static final long BACKGROUND_PROCESS_STOP_WAIT_MS = 1000;

    /**
     * @return instance of the helper interface
     */
    public static ScriptExecHelper helper() {
        return new ScriptExecHelper() {

            @Override
            public int runLocalCommand(
                    final String osFamily,
                    final ExecArgList execArgList,
                    final Map<String, Map<String, String>> dataContext,
                    final File workingdir,
                    final OutputStream outputStream,
                    final OutputStream errorStream
            )
                    throws IOException, InterruptedException {
                return ScriptExecUtil.runLocalCommand(
                        osFamily,
                        execArgList,
                        dataContext,
                        workingdir,
                        outputStream,
                        errorStream
                );
            }

            @Override
            public int runLocalCommand(
                    final String[] command,
                    final Map<String, String> envMap,
                    final File workingdir,
                    final OutputStream outputStream,
                    final OutputStream errorStream
            )
                    throws IOException, InterruptedException {
                return ScriptExecUtil.runLocalCommand(command, envMap, workingdir, outputStream, errorStream);
            }

            @Override
            public String[] createScriptArgs(
                    final Map<String, Map<String, String>> localDataContext,
                    final String scriptargs,
                    final String[] scriptargsarr,
                    final String scriptinterpreter,
                    final boolean interpreterargsquoted,
                    final String filepath
            ) {
                return ScriptExecUtil.createScriptArgs(
                        localDataContext, null, scriptargs, scriptargsarr, scriptinterpreter,
                        interpreterargsquoted, filepath
                );
            }

            @Override
            public String[] createScriptArgs(
                    final Map<String, Map<String, String>> localDataContext,
                    final String scriptargs,
                    final String[] scriptargsarr,
                    final String scriptinterpreter,
                    final boolean interpreterargsquoted
            )
            {
                return ScriptExecUtil.createScriptArgs(
                        localDataContext, null, scriptargs, scriptargsarr, scriptinterpreter,
                        interpreterargsquoted
                );
            }

            @Override
            public String[] createScriptArgs(
                    final Map<String, Map<String, String>> localDataContext,
                    final INodeEntry node,
                    final String scriptargs,
                    final String[] scriptargsarr,
                    final String scriptinterpreter,
                    final boolean interpreterargsquoted,
                    final String filepath
            ) {
                return ScriptExecUtil.createScriptArgs(
                        localDataContext,
                        node,
                        scriptargs,
                        scriptargsarr,
                        scriptinterpreter,
                        interpreterargsquoted,
                        filepath
                );
            }


            @Override
            public ExecArgList createScriptArgList(
                    final String filepath,
                    final String scriptargs,
                    final String[] scriptargsarr,
                    final String scriptinterpreter,
                    final boolean interpreterargsquoted
            ) {
                return ScriptExecUtil.createScriptArgList(
                        filepath,
                        scriptargs,
                        scriptargsarr,
                        scriptinterpreter,
                        interpreterargsquoted
                );
            }

            @Override
            public Map<String, String> loadLocalEnvironment() {
                return ScriptExecUtil.loadLocalEnvironment();
            }

        };
    }

    /**
     * Run a command with environment variables in a working dir, and copy the streams
     *
     * @param osFamily     local node os family
     * @param execArgList  the ExecArgList to run
     * @param workingdir   optional working dir location (or null)
     * @param outputStream stream for stdout
     * @param errorStream  stream for stderr
     * @param dataContext data
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
            throws IOException, InterruptedException {
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
    public static int runLocalCommand(
            final String[] command,
            final Map<String, String> envMap,
            final File workingdir,
            final OutputStream outputStream,
            final OutputStream errorStream
    )
            throws IOException, InterruptedException {
        return runLocalCommand(
                command,
                envMap,
                workingdir,
                outputStream,
                errorStream,
                false,
                ScriptExecUtil::killProcessHandleDescend
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
    public static int runLocalCommand(
            final String[] command,
            final Map<String, String> envMap,
            final File workingdir,
            final OutputStream outputStream,
            final OutputStream errorStream,
            boolean clearEnv,
            Consumer<ProcessHandle> killHandler
    )
            throws IOException, InterruptedException
    {
        return runLocalCommand(
                command,
                envMap,
                workingdir,
                outputStream,
                errorStream,
                clearEnv,
                killHandler,
                null
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
     * @return the exit code of the command
     * @throws IOException          if any IO exception occurs
     * @throws InterruptedException if interrupted while waiting for the command to finish
     */
    public static int runLocalCommand(
            final String[] command,
            final Map<String, String> envMap,
            final File workingdir,
            final OutputStream outputStream,
            final OutputStream errorStream,
            boolean clearEnv,
            Consumer<ProcessHandle> killHandler,
            InputStream inputStream
    )
            throws IOException, InterruptedException {
        final ProcessBuilder processBuilder = new ProcessBuilder().command(command);

        final Map<String, String> environment = processBuilder.environment();
        if(clearEnv) {
            environment.clear();
        }
        environment.putAll(envMap);
        if (null != workingdir) {
            processBuilder.directory(workingdir);
        }

        final Process exec = processBuilder.start();
        final Streams.StreamCopyThread inthread;
        final OutputStream outputStream1 = exec.getOutputStream();
        if (inputStream != null) {
            inthread = Streams.copyStreamThread(inputStream, outputStream1, () -> {
                try {
                    outputStream1.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } else {
            inthread = null;
            outputStream1.close();
        }

        final Streams.StreamCopyThread errthread = Streams.copyStreamThread(exec.getErrorStream(), errorStream);
        final Streams.StreamCopyThread outthread = Streams.copyStreamThread(exec.getInputStream(), outputStream);
        if (null != inthread) {
            inthread.start();
        }
        errthread.start();
        outthread.start();
        try {
            final int result = exec.waitFor();
            outputStream.flush();
            errorStream.flush();
            if (inthread != null) {
                inthread.join();
                outputStream1.close();
            }

            // Give stream threads a brief window to drain any buffered output that
            // remains in the kernel pipe after the shell process exits. When the shell
            // exits normally this drains almost instantly.
            // If threads are still alive after the window it means a background process
            // (started via '&') has inherited the pipe file descriptors and is keeping
            // them open indefinitely. In that case, forcibly close the streams to unblock
            // the threads — this mirrors Ant's PumpStreamHandler behavior, which stopped
            // stream pumping when the parent process exited rather than waiting for all
            // descended processes to exit.
            errthread.join(STREAM_DRAIN_TIMEOUT_MS);
            outthread.join(STREAM_DRAIN_TIMEOUT_MS);

            final boolean backgroundProcessDetected = errthread.isAlive() || outthread.isAlive();
            if (backgroundProcessDetected) {
                // Close streams to release background processes holding inherited FDs.
                // IOExceptions thrown in the stream threads as a result are expected
                // ("Stream closed") and should not be treated as errors.
                try { exec.getInputStream().close(); } catch (IOException ignored) {}
                try { exec.getErrorStream().close(); } catch (IOException ignored) {}
                // On Linux, closing the read-end of a pipe from one thread does not
                // reliably interrupt another thread blocked in a native read() on that
                // same FD. Give threads a brief window to notice the close; if they are
                // still alive we stop waiting — the shell has exited and we have its exit
                // code. The threads will terminate on their own when the background
                // process eventually closes its inherited pipe descriptors.
                errthread.join(BACKGROUND_PROCESS_STOP_WAIT_MS);
                outthread.join(BACKGROUND_PROCESS_STOP_WAIT_MS);
            } else {
                exec.getInputStream().close();
                exec.getErrorStream().close();
            }

            if (null != inthread && null != inthread.getException()) {
                throw inthread.getException();
            }
            // Only propagate stream thread exceptions when they completed naturally.
            // After an intentional close (backgroundProcessDetected == true), the threads
            // will have an expected "Stream closed" IOException that is not a real error.
            if (!backgroundProcessDetected) {
                if (null != outthread.getException()) {
                    throw outthread.getException();
                }
                if (null != errthread.getException()) {
                    throw errthread.getException();
                }
            }
            return result;
        } catch (InterruptedException e) {
            if (null != killHandler) {
                killHandler.accept(exec.toHandle());
            }
            if(exec.isAlive()){
                exec.waitFor();
            }
            throw new InterruptedException("Execution interrupted with code: " + exec.exitValue());
        }
    }

    public static void killProcessHandleDescend(ProcessHandle handle) {
        handle.descendants().forEach(ScriptExecUtil::killProcessHandle);
        handle.destroy();
    }
    public static void killProcessHandle(ProcessHandle handle) {
        handle.destroy();
    }

    /**
     * @return local environment variables
     */
    @SuppressWarnings("unchecked")
    public static Map<String,String> loadLocalEnvironment(){
        return (Map<String,String>)Execute.getEnvironmentVariables();
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
     *
     * @return args
     */
    public static String[] createScriptArgs(
            final Map<String, Map<String, String>> localDataContext,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted, final String filepath
    ) {
        return createScriptArgs(
                localDataContext, null, scriptargs, scriptargsarr, scriptinterpreter,
                interpreterargsquoted, filepath
        );
    }

    /**
     * Generate argument array for a script file invocation
     *
     * @param filepath              remote filepath for the script
     * @param scriptargs            arguments to the script file
     * @param scriptargsarr         arguments to the script file as an array
     * @param scriptinterpreter     interpreter invocation for the file, or null to invoke it directly, can include
     *                              ${scriptfile}
     * @param interpreterargsquoted if true, pass the script file and args as a single argument to the interpreter
     *
     * @return arg list
     */
    public static ExecArgList createScriptArgList(
            final String filepath, final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted
    ) {

        ExecArgList.Builder builder = ExecArgList.builder();
        boolean seenFilepath = false;
        if (null != scriptinterpreter) {
            String[] burst = OptsUtil.burst(scriptinterpreter);
            List<String> args = new ArrayList<String>();
            for (String arg : burst) {
                if (arg.contains("${scriptfile}")) {
                    args.add(
                            arg.replaceAll(
                                    Pattern.quote("${scriptfile}"),
                                    Matcher.quoteReplacement(filepath)
                            )
                    );
                    seenFilepath = true;
                } else {
                    args.add(arg);
                }
            }
            builder.args(args, false, false);
        }
        if (null != scriptinterpreter && interpreterargsquoted) {
            ExecArgList.Builder sub = builder.subList(true);
            addScriptFileArgList(seenFilepath ? null : filepath, scriptargs, scriptargsarr, sub, needsQuoting);
            sub.parent();
        } else {
            addScriptFileArgList(seenFilepath ? null : filepath, scriptargs, scriptargsarr, builder, needsQuoting);
        }
        return builder.build();
    }


    static final Predicate needsQuoting =
            DataContextUtils.stringContainsPropertyReferencePredicate
                    .or(CLIUtils::containsSpace)
                    .or(CLIUtils::containsQuote);


    private static void addScriptFileArgList(
            String filepath, String scriptargs, String[] scriptargsarr,
            ExecArgList.Builder builder, Predicate quoted
    ) {
        if (null != filepath) {
            builder.arg(filepath, false, false);
        }
        if (null != scriptargs) {
            builder.args(OptsUtil.burst(scriptargs), quoted, false);
        } else if (null != scriptargsarr) {
            builder.args(scriptargsarr, quoted, false);
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
     *
     * @return args
     */
    public static String[] createScriptArgs(
            final Map<String, Map<String, String>> localDataContext,
            final INodeEntry node,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted
    )
    {
        return createScriptArgs(
                localDataContext,
                node,
                scriptargs,
                scriptargsarr,
                scriptinterpreter,
                interpreterargsquoted,
                null
        );
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
     *
     * @return args
     */
    public static String[] createScriptArgs(
            final Map<String, Map<String, String>> localDataContext,
            final INodeEntry node,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted,
            final String filepath
    ) {
        final ArrayList<String> arglist = new ArrayList<String>();
        if (null != scriptinterpreter) {
            List<String> c = Arrays.asList(
                    DataContextUtils.replaceDataReferencesInArray(
                            OptsUtil.burst(scriptinterpreter),
                            localDataContext,
                            null,
                            false,
                            true
                    ));
            arglist.addAll(c);
        }
        if (null != scriptinterpreter && interpreterargsquoted) {
            final ArrayList<String> sublist = new ArrayList<String>();
            if (filepath != null) {
                sublist.add(filepath);
            }
            addQuotedArgs(localDataContext, node, scriptargs, scriptargsarr, sublist, true);
            arglist.add(DataContextUtils.join(sublist, " "));
        } else {
            if (filepath != null) {
                arglist.add(filepath);
            }
            addQuotedArgs(localDataContext, node, scriptargs, scriptargsarr, arglist, false);
        }
        return arglist.toArray(new String[arglist.size()]);
    }

    private static void addQuotedArgs(
            Map<String, Map<String, String>> localDataContext, INodeEntry node, String
            scriptargs, String[] scriptargsarr, ArrayList<String> arglist, boolean quoted
    ) {
        if (null != scriptargs) {
            arglist.addAll(
                    Arrays.asList(
                            DataContextUtils.replaceDataReferencesInArray(
                                    scriptargs.split(" "),
                                    localDataContext
                            )
                    )
            );
        } else if (null != scriptargsarr) {
            if (!quoted) {
                arglist.addAll(Arrays.asList(scriptargsarr));
            } else {
                final String[] newargs = DataContextUtils.replaceDataReferencesInArray(scriptargsarr, localDataContext);
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
