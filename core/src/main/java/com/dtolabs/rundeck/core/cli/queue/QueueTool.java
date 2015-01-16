/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* QueueTool.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Feb 22, 2010 1:18:08 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli.queue;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.*;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.BaseLogger;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.PrintStream;
import java.util.Collection;

/**
 * QueueTool is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class QueueTool extends BaseTool implements CLIToolLogger {
    static final long WAIT_BASE_DELAY = Long.getLong(QueueTool.class.getName() + ".WAIT_BASE_DELAY", 1000L);
    static final long WAIT_MAX_DELAY = Long.getLong(QueueTool.class.getName() + ".WAIT_MAX_DELAY", 5000L);
    /**
     * log4j
     */
    public static final Logger log4j = Logger.getLogger(QueueTool.class);

    /**
     * list action identifier
     */
    public static final String ACTION_LIST = "list";
    /**
     * kill action identifier
     */
    public static final String ACTION_KILL = "kill";
    /**
     * kill action identifier
     */
    public static final String ACTION_FOLLOW = "follow";

    /**
     * Get action
     * @return action
     */
    public Actions getAction() {
        return action;
    }

    /**
     * Set action
     * @param action the action
     */
    public void setAction(final Actions action) {
        this.action = action;
    }

    /**
     * Get jobId for use with Kill action
     * @return execution ID
     */
    public String getExecid() {
        return execid;
    }

    /**
     * Set execid for use with kill action
     * @param execid execution ID
     */
    public void setExecid(final String execid) {
        this.execid = execid;
    }

    /**
     * Return verbose
     * @return is verbose
     */
    public boolean isArgVerbose() {
        return argVerbose;
    }

    /**
     * Set verbose
     * @param argVerbose is verbose
     */
    public void setArgVerbose(final boolean argVerbose) {
        this.argVerbose = argVerbose;
    }

    /**
     * Enumeration of available actions
     */
    public static enum Actions {
        /**
         * List action
         */
        list(ACTION_LIST),
        /**
         * kill action
         */
        kill(ACTION_KILL),
        /**
         * kill action
         */
        follow(ACTION_FOLLOW);
        private String name;

        Actions(final String name) {
            this.name = name;
        }

        /**
         * Return the name
         * @return name
         */
        public String getName() {
            return name;
        }
    }

    /**
     * reference to the command line {@link org.apache.commons.cli.Options} instance.
     */
    private Actions action = Actions.list;
    private String execid;
    private boolean argVerbose;
    private boolean argQuiet;
    private boolean argProgress;
    private boolean argRestart;
    private CLIToolLogger clilogger;
    String argProject;


    /**
     * Reference to the Framework instance
     */
    private final Framework framework;
    SingleProjectResolver internalResolver;

    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args command line arg vector
     *
     * @throws Exception action error
     */
    public static void main(final String[] args) throws Exception {
        
        PropertyConfigurator.configure(Constants.getLog4jPropertiesFile().getAbsolutePath());
        final QueueTool tool = new QueueTool(new DefaultCLIToolLogger());
        tool.setShouldExit(true);
        int exitCode = 1; //pessimistic initial value

        try {
            tool.run(args);
            exitCode = 0;
        } catch (CLIToolOptionsException e) {
            exitCode = 2;
            tool.error(e.getMessage());
            tool.help();
        } catch (Throwable e) {
            if (e.getMessage() == null || tool.argVerbose) {
                e.printStackTrace();
            }
            tool.error("Error: " + e.getMessage());
        }
        tool.exit(exitCode);
    }

    /**
     * Create QueueTool with default Framework instances located by the system rdeck.base property.
     */
    public QueueTool() {
        this(Framework.getInstanceWithoutProjectsDir(Constants.getSystemBaseDir()), new Log4JCLIToolLogger(log4j));
    }

    protected boolean isUseHelpOption() {
        return true;
    }

    public String getHelpString() {
        return "rd-queue <action> : list the executions running in the queue or kill a running execution\n"
               + "rd-queue [list] : list the executions running in the queue [default]\n"
               + "rd-queue follow --eid <id> [--restart] : follow the output of an execution\n"
               + "rd-queue kill --eid <id> : kill an execution running in the queue\n";
    }

    /**
     * Create QueueTool specifying the logger
     *
     * @param logger the logger
     */
    public QueueTool(final CLIToolLogger logger) {
        this(Framework.getInstanceWithoutProjectsDir(Constants.getSystemBaseDir()), logger);
    }
    
    /**
     * Create QueueTool specifying the framework
     *
     * @param framework framework
     */
    public QueueTool(final Framework framework) {
        this(framework, null);
    }

    /**
     * Create QueueTool with the framework.
     *
     * @param framework the framework
     * @param logger    the logger
     */
    public QueueTool(final Framework framework, final CLIToolLogger logger) {
        this.framework = framework;
        internalResolver=new FrameworkSingleProjectResolver(framework);
        this.clilogger = logger;
        if (null == clilogger) {
            clilogger = new Log4JCLIToolLogger(log4j);
        }
        toolOptions =new Options();
        addToolOptions(toolOptions);
    }

    private LoglevelOptions loglevelOptions;
    private Options toolOptions;

    private class Options implements CLIToolOptions{
        public static final String RESTART_OPTION = "t";
        public static final String RESTART_OPTION_LONG = "restart";
        public static final String QUIET_OPTION = "q";
        public static final String QUIET_OPTION_LONG = "quiet";
        public static final String PROGRESS_OPTION = "r";
        public static final String PROGRESS_OPTION_LONG = "progress";
        public static final String EXECID_OPTION = "e";
        public static final String EXECID_OPTION_LONG = "eid";
        /**
         * short option string for verbose
         */
        public static final String VERBOSE_OPTION = "v";
        /**
         * long option string for verbose
         */
        public static final String VERBOSE_OPTION_LONG = "verbose";
        /**
         * short option string for project
         */
        public static final String PROJECT_OPTION = "p";

        public void addOptions(final org.apache.commons.cli.Options options) {

            options.addOption(EXECID_OPTION, EXECID_OPTION_LONG, true, "Execution ID");
            options.addOption(VERBOSE_OPTION, VERBOSE_OPTION_LONG, false, "Enable verbose output");
            options.addOption(QUIET_OPTION, QUIET_OPTION_LONG, false, "Just wait until execution ends (follow only)");
            options.addOption(PROGRESS_OPTION, PROGRESS_OPTION_LONG, false, "Progress mark output (follow only)");
            options.addOption(PROJECT_OPTION, null, true, "Project name (list action only)");
            options.addOption(RESTART_OPTION, RESTART_OPTION_LONG, false, "Restart log output from beginning (follow action only)");
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (cli.hasOption(EXECID_OPTION)) {
                execid = cli.getOptionValue(EXECID_OPTION);
            }
            if (cli.hasOption(VERBOSE_OPTION)) {
                argVerbose = true;
            }
            if (cli.hasOption(PROJECT_OPTION)) {
                argProject = cli.getOptionValue(PROJECT_OPTION);
            }
            if (cli.hasOption(RESTART_OPTION)) {
                argRestart = true;
            }
            if (cli.hasOption(QUIET_OPTION)) {
                argQuiet = true;
            }
            if (cli.hasOption(PROGRESS_OPTION)) {
                argProgress = true;
            }
        }

        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (Actions.list == action) {
                validateListAction();
            }else if(Actions.kill==action){
                validateKillAction();
            }else if(Actions.follow==action){
                validateFollowAction();
            }
        }

        private void validateListAction() throws CLIToolOptionsException {
            if (null != execid) {
                warn("-"+ EXECID_OPTION +"/--"+ EXECID_OPTION_LONG +" argument only valid with kill/follow actions");
            }
            if(null==argProject){
                if (internalResolver.hasSingleProject()) {
                    argProject = internalResolver.getSingleProjectName();
                    debug("# No project specified, defaulting to: " + argProject);
                } else {
                    throw new CLIToolOptionsException("-" + PROJECT_OPTION + " argument is required with list action");
                }
            }
        }

        private void validateKillAction() throws CLIToolOptionsException {
            if (null == execid) {
                throw new CLIToolOptionsException("-" + EXECID_OPTION + "/--" + EXECID_OPTION_LONG +" argument required");
            }
            if (null != argProject) {
                warn("-" + PROJECT_OPTION + " argument only valid with list action");
            }
        }

        private void validateFollowAction() throws CLIToolOptionsException {
            if (null == execid) {
                throw new CLIToolOptionsException("-" + EXECID_OPTION + "/--" + EXECID_OPTION_LONG +" argument required");
            }
            if (null != argProject) {
                warn("-" + PROJECT_OPTION + " argument only valid with list action");
            }
        }
        public String getJobid() {
            return execid;
        }
    }

    /**
     * Reads the argument vector and constructs a {@link org.apache.commons.cli.CommandLine} object containing params
     *
     * @param args the cli arg vector
     *
     * @return a new instance of CommandLine
     * @throws CLIToolOptionsException if arguments are incorrect
     */
    public CommandLine parseArgs(final String[] args) throws CLIToolOptionsException {
        final CommandLine line = super.parseArgs(args);
        if (args.length > 0 && !args[0].startsWith("-")) {
            try {
                action = Actions.valueOf(args[0]);
            } catch (IllegalArgumentException e) {
                throw new CLIToolOptionsException("Invalid action: " + args[0]);
            }
        }
        return line;
    }


    /**
     * Call the action
     *
     * @throws QueueToolException if an error occurs
     */
    protected void go() throws QueueToolException, CLIToolOptionsException {
        switch (action) {
            case list:
                listAction();
                break;
            case kill:
                killAction(execid);
                break;
            case follow:
                followAction(execid);
                break;
            default:
                throw new CLIToolOptionsException("Unrecognized action: " + action);
        }
    }

    /**
     * Perform the kill action on an execution, and print the result.
     *
     * @param execid the execution id
     *
     * @throws QueueToolException if an error occurs
     */
    private void killAction(final String execid) throws QueueToolException {
        final DispatcherResult result;
        try {
            result = framework.getCentralDispatcherMgr().killDispatcherExecution(execid);
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to kill the execution: " + e.getMessage();
            throw new QueueToolException(msg, e);
        }
        if (result.isSuccessful()) {
            log("rd-queue kill: success. [" + execid + "] " + result.getMessage());
        } else {
            error("rd-queue kill: failed. [" + execid + "] " + result.getMessage());
        }
    }

    /**
     * Perform the list action and print the results.
     *
     * @throws QueueToolException if an error occurs
     */
    private void listAction() throws QueueToolException {
        final Collection<QueuedItem> result;
        try {
            result = framework.getCentralDispatcherMgr().listDispatcherQueue(argProject);
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to list the queue: " + e.getMessage();
            throw new QueueToolException(msg, e);
        }
        if (null != result) {
            log("Queue: " + result.size() + " items");
            for (final QueuedItem item : result) {
                final String url = item.getUrl();
                log("[" + item.getId() + "] " + item.getName() + " <" + url + ">");
            }
        } else {
            throw new QueueToolException("List request returned null");
        }


    }

    private void followAction(final String execid) throws QueueToolException {
        ConsoleExecutionFollowReceiver.Mode mode = ConsoleExecutionFollowReceiver.Mode.output;
        if(argQuiet ) {
            mode = ConsoleExecutionFollowReceiver.Mode.quiet;
        }else if(argProgress){
            mode= ConsoleExecutionFollowReceiver.Mode.progress;
        }
        try {
            followAction(execid, argRestart, mode, framework, System.out, this);
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to follow the execution: " + e.getMessage();
            throw new QueueToolException(msg, e);
        }
    }

    /**
     * Perform the Follow action for an Execution
     *
     * @param mode follow mode
     * @param framework framework
     * @param out output for progress marks
     * @param logger logger for output of log lines
     * @param execid the execution id
     * @param restart true to restart the output
     *
     * @throws CentralDispatcherException if any error occurs
     * @return true if execution was successful
     */
    public static boolean followAction(final String execid,
                              final boolean restart,
                              final ConsoleExecutionFollowReceiver.Mode mode,
                              final Framework framework,
                              final PrintStream out, final BaseLogger logger)
        throws CentralDispatcherException {

        final ExecutionFollowResult result;
        ExecutionDetail execution = framework.getCentralDispatcherMgr().getExecution(execid);
        final long averageDuration;
        if(null!=execution.getExecutionJob()){
            averageDuration=execution.getExecutionJob().getAverageDuration();
        }else{
            averageDuration=-1;
        }
        final ExecutionFollowRequest request = new ExecutionFollowRequest() {
            public boolean isResume() { return !restart; }
        };
        final ConsoleExecutionFollowReceiver receiver = new ConsoleExecutionFollowReceiver(averageDuration,
                                                                                           mode,
                                                                                           out,
                                                                                           logger);
        result = framework.getCentralDispatcherMgr().followDispatcherExecution(execid,
                                                                                request,
                                                                                receiver);
        if (mode != ConsoleExecutionFollowReceiver.Mode.output) {
            out.println();
        }
        ExecutionState state = result.getState();
        if (null != state) {
            if (state == ExecutionState.running) {
                state = awaitExecutionCompletion(framework, execid);
            }
            if (mode != ConsoleExecutionFollowReceiver.Mode.quiet) {
                logger.warn("[" + execid + "] execution status: " + state);
            }
            switch (state) {
                case failed:
                case aborted:
                    return false;
            }
        }
        return true;
    }

    private static ExecutionState awaitExecutionCompletion(Framework framework,String execid) throws CentralDispatcherException {
        ExecutionState state = executionStatus(framework, execid);
        long delay= WAIT_BASE_DELAY;
        while (state == ExecutionState.running) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                return state;
            }
            if (delay < WAIT_MAX_DELAY) {
                delay += WAIT_BASE_DELAY;
            }
            state = executionStatus(framework, execid);
        }
        return state;
    }

    private static ExecutionState executionStatus(Framework framework, String execid) throws CentralDispatcherException {
        ExecutionDetail execution = framework.getCentralDispatcherMgr().getExecution(execid);
        return execution.getStatus();
    }


    public void log(final String output) {
        if (null != clilogger) {
            clilogger.log(output);
        }
    }

    public void error(final String output) {
        if (null != clilogger) {
            clilogger.error(output);
        }
    }


    public void warn(final String output) {
        if (null != clilogger) {
            clilogger.warn(output);
        }
    }

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     */
    public void verbose(final String message) {
        if (null != clilogger && argVerbose) {
            clilogger.verbose(message);
        }
    }

    public void debug(final String message) {
        if (null != clilogger) {
            clilogger.debug(message);
        }
    }


}
