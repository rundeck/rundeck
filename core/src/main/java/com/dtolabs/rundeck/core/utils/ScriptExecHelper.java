package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecArgList;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * Helper interface for ScriptExec utility.
 * Created by greg on 1/23/15.
 */
public interface ScriptExecHelper {

    /**
     * Run a command with environment variables in a working dir, and copy the streams
     *
     * @param osFamily     local node os family
     * @param execArgList  the ExecArgList to run
     * @param workingdir   optional working dir location (or null)
     * @param outputStream stream for stdout
     * @param errorStream  stream for stderr
     * @param dataContext  data
     *
     * @return the exit code of the command
     *
     * @throws java.io.IOException  if any IO exception occurs
     * @throws InterruptedException if interrupted while waiting for the command to finish
     */
    public int runLocalCommand(
            final String osFamily,
            final ExecArgList execArgList,
            final Map<String, Map<String, String>> dataContext,
            final File workingdir,
            final OutputStream outputStream,
            final OutputStream errorStream
    )
            throws IOException, InterruptedException;

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
    public int runLocalCommand(
            final String[] command,
            final Map<String, String> envMap,
            final File workingdir,
            final OutputStream outputStream,
            final OutputStream errorStream
    )
            throws IOException, InterruptedException;

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
    public String[] createScriptArgs(
            final Map<String, Map<String, String>> localDataContext,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted,
            final String filepath
    );

    /**
     * Generate argument array for a script file invocation on a specific node
     *
     * @param localDataContext      data context properties to expand among the args
     * @param node                  node to use for os-family argument quoting.
     * @param scriptargs            arguments to the script file
     * @param scriptargsarr         arguments to the script file as an array
     * @param scriptinterpreter     interpreter invocation for the file, or null to invoke it directly
     * @param interpreterargsquoted if true, pass the script file and args as a single argument to the interpreter
     * @param filepath              remote filepath for the script
     *
     * @return args
     */
    public String[] createScriptArgs(
            final Map<String, Map<String, String>> localDataContext,
            final INodeEntry node,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted,
            final String filepath
    );

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
    public ExecArgList createScriptArgList(
            final String filepath,
            final String scriptargs,
            final String[] scriptargsarr,
            final String scriptinterpreter,
            final boolean interpreterargsquoted
    );

    public Map<String,String> loadLocalEnvironment();

}
