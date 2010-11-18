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
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException;
import com.dtolabs.rundeck.core.dispatcher.DispatcherResult;
import com.dtolabs.rundeck.core.dispatcher.QueuedItem;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.Collection;

/**
 * QueueTool is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class QueueTool extends BaseTool implements CLIToolLogger {
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
        kill(ACTION_KILL);
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
    private CLIToolLogger clilogger;


    /**
     * Reference to the Framework instance
     */
    private final Framework framework;

    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args command line arg vector
     *
     * @throws Exception action error
     */
    public static void main(final String[] args) throws Exception {
        final File basedir = new File(Constants.getSystemBaseDir());
        PropertyConfigurator.configure(new File(new File(basedir, "etc"),
            "log4j.properties").getAbsolutePath());
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
        this(Framework.getInstance(Constants.getSystemBaseDir()), new Log4JCLIToolLogger(log4j));
    }

    protected boolean isUseHelpOption() {
        return true;
    }

    public String getHelpString() {
        return "rd-queue <action> : list the executions running in the queue or kill a running execution\n"
               + "rd-queue [list] : list the executions running in the queue [default]\n"
               + "rd-queue kill --eid <id> : kill an execution running in the queue\n";
    }

    /**
     * Create QueueTool specifying the logger
     *
     * @param logger the logger
     */
    public QueueTool(final CLIToolLogger logger) {
        this(Framework.getInstance(Constants.getSystemBaseDir()), logger);
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

        public void addOptions(final org.apache.commons.cli.Options options) {

            options.addOption(EXECID_OPTION, EXECID_OPTION_LONG, true, "Execution ID");
            options.addOption(VERBOSE_OPTION, VERBOSE_OPTION_LONG, true, "Enable verbose output");
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (cli.hasOption(EXECID_OPTION)) {
                execid = cli.getOptionValue(EXECID_OPTION);
            }
            if (cli.hasOption(VERBOSE_OPTION)) {
                argVerbose = true;
            }
        }

        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (Actions.list == action) {
                validateListAction();
            }else if(Actions.kill==action){
                validateKillAction();
            }
        }

        private void validateListAction() throws CLIToolOptionsException {
            if (null != execid) {
                warn("-"+ EXECID_OPTION +"/--"+ EXECID_OPTION_LONG +" argument only valid with kill action");
            }
        }

        private void validateKillAction() throws CLIToolOptionsException {
            if (null == execid) {
                throw new CLIToolOptionsException("-" + EXECID_OPTION + "/--" + EXECID_OPTION_LONG +" argument required");
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
            result = framework.getCentralDispatcherMgr().listDispatcherQueue();
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
        if (null != clilogger) {
            clilogger.verbose(message);
        }
    }


}
