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
package com.dtolabs.rundeck.core.cli.run;

import com.dtolabs.client.services.DispatcherConfig;
import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.*;
import com.dtolabs.rundeck.core.cli.queue.ConsoleExecutionFollowReceiver;
import com.dtolabs.rundeck.core.cli.queue.QueueTool;
import com.dtolabs.rundeck.core.common.FrameworkFactory;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.NodeSet;
import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class RunTool extends BaseTool {
    /**
     * log4j
     */
    public static final Logger log4j = Logger.getLogger(RunTool.class);

    /**
     * run action identifier
     */
    public static final String ACTION_RUN = "run";

    /**
     * Get action
     *
     * @return action
     */
    public Actions getAction() {
        return action;
    }

    /**
     * Set action
     *
     * @param action the action
     */
    public void setAction(final Actions action) {
        this.action = action;
    }

    public String getSingleProjectName() {
        try {
            List<String> strings = getCentralDispatcher().listProjectNames();
            if(strings.size()==1) {
                return strings.get(0);
            }
        } catch (CentralDispatcherException e) {
            debug(org.apache.tools.ant.util.StringUtils.getStackTrace(e));
            error("Couldn't list projects: " + e.getMessage());
        }
        return null;
    }

    /**
     * Enumeration of available actions
     */
    public static enum Actions {
        /**
         * run action
         */
        run(ACTION_RUN);
        private String name;

        Actions(final String name) {
            this.name = name;
        }

        /**
         * Return the name
         *
         * @return name
         */
        public String getName() {
            return name;
        }
    }


    private Actions action = Actions.run;
    private CLIToolLogger clilogger;


    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args command line arg vector
     *
     * @throws Exception action error
     */
    public static void main(final String[] args) throws Exception {
        PropertyConfigurator.configure(Constants.getLog4jPropertiesFile().getAbsolutePath());
        final RunTool tool = new RunTool(createDefaultDispatcherConfig(),new DefaultCLIToolLogger());
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
            if (e.getMessage() == null || tool.runOptions.argVerbose) {
                e.printStackTrace();
            }
            tool.error("Error: " + e.getMessage());
        }
        tool.exit(exitCode);
    }

    /**
     * Create QueueTool with default framework properties located by the system rdeck.base property.
     */
    public RunTool() {
        this(FrameworkFactory.createFilesystemFramework(new File(Constants.getSystemBaseDir())).getPropertyLookup(),
             new Log4JCLIToolLogger(log4j));
    }

    /**
     * Create QueueTool specifying the logger
     *
     * @param logger the logger
     */
    public RunTool(final CLIToolLogger logger) {
        this(FrameworkFactory.createFilesystemFramework(new File(Constants.getSystemBaseDir())).getPropertyLookup(), logger);
    }

    /**
     * Create QueueTool specifying the framework
     *
     */
    public RunTool(final IPropertyLookup frameworkProps) {
        this(frameworkProps, null);
    }

    /**
     * Create QueueTool with the framework.
     *
     * @param logger    the logger
     */
    public RunTool(final IPropertyLookup frameworkProps, final CLIToolLogger logger) {
        this(FrameworkFactory.createDispatcherConfig(frameworkProps), logger);
    }
    public RunTool(final DispatcherConfig dispatcherConfig, final CLIToolLogger logger) {
        setCentralDispatcher(FrameworkFactory.createDispatcher(dispatcherConfig));
        this.clilogger = logger;
        if (null == clilogger) {
            clilogger = new Log4JCLIToolLogger(log4j);
        }
        nodefilterOptions = new NodeFilterOptions(false);
        loglevelOptions = new LoglevelOptions();
        extendedOptions = new ExtendedOptions();
        followOptions = new FollowOptions();
        runOptions = new Options();
        addToolOptions(extendedOptions);
        addToolOptions(loglevelOptions);
        addToolOptions(nodefilterOptions);
        addToolOptions(followOptions);
        addToolOptions(runOptions);
    }

    private NodeFilterOptions nodefilterOptions;
    private LoglevelOptions loglevelOptions;
    private ExtendedOptions extendedOptions;
    private FollowOptions followOptions;
    Options runOptions;

    class FollowOptions implements CLIToolOptions{
        /**
         * short option string for run option :follow
         */
        public static final String FOLLOW_OPTION = "f";

        /**
         * long option string for run option: follow
         */
        public static final String FOLLOW_OPTION_LONG = "follow";
        /**
         * short option string for run option :quiet
         */
        public static final String QUIET_OPTION = "q";
        /**
         * long option string for run option: quiet
         */
        public static final String QUIET_OPTION_LONG = "quiet";
        /**
         * short option string for run option :quiet
         */
        public static final String PROGRESS_OPTION = "r";
        /**
         * long option string for run option: quiet
         */
        public static final String PROGRESS_OPTION_LONG = "progress";
        boolean argFollow;
        boolean argQuiet;
        boolean argProgress;

        public void addOptions(final org.apache.commons.cli.Options options) {
            options.addOption(FOLLOW_OPTION, FOLLOW_OPTION_LONG, false, "Follow Job output on commandline");
            options.addOption(QUIET_OPTION, QUIET_OPTION_LONG, false, "Quiet mode for following output.");
            options.addOption(PROGRESS_OPTION, PROGRESS_OPTION_LONG, false, "Following Job progress");
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            argFollow = cli.hasOption(FOLLOW_OPTION);
            argQuiet = cli.hasOption(QUIET_OPTION);
            argProgress = cli.hasOption(PROGRESS_OPTION);
        }

        public void validate(CommandLine cli, String[] original) throws CLIToolOptionsException {
            if((argQuiet || argProgress) && !argFollow){

            }
        }
    }
    /**
     * CLIToolOptions class for the run tool
     */
    class Options implements CLIToolOptions {

        /**
         * short option string for query parameter: idlist
         */
        public static final String ID_OPTION = "i";

        /**
         * short option string for verbose
         */
        public static final String VERBOSE_OPTION = "v";

        /**
         * long option string for query parameter: idlist
         */
        public static final String ID_OPTION_LONG = "id";

        /**
         * long option string for verbose
         */
        public static final String VERBOSE_OPTION_LONG = "verbose";

        /**
         * short option string for run option: job
         */
        public static final String JOB_OPTION = "j";

        /**
         * long option string for run option: job
         */
        public static final String JOB_OPTION_LONG = "job";

        /**
         * short option string for run option: job
         */
        public static final String PROJECT_OPTION = "p";

        /**
         * long option string for run option: job
         */
        public static final String PROJECT_OPTION_LONG = "project";
        String argIdlist;
        String argJob;
        boolean argVerbose;
        String argProject;

        public void addOptions(final org.apache.commons.cli.Options options) {

            options.addOption(ID_OPTION, ID_OPTION_LONG, true,
                "Job ID. Run the Job with this ID. ");
            options.addOption(JOB_OPTION, JOB_OPTION_LONG, true,
                "Job identifier (group and name).  Run a Job specified by Job name and optional group, e.g: 'Group Name/Job Name'. ");
            options.addOption(PROJECT_OPTION, PROJECT_OPTION_LONG, true,
                "Project name. Required if not specifying Job ID.");
            options.addOption(VERBOSE_OPTION, VERBOSE_OPTION_LONG, false,
                "Enable verbose output");
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {

            if (cli.hasOption(ID_OPTION)) {
                argIdlist = cli.getOptionValue(ID_OPTION);
            }
            if (cli.hasOption(VERBOSE_OPTION)) {
                argVerbose = true;
            }
            if (cli.hasOption(JOB_OPTION)) {
                argJob = cli.getOptionValue(JOB_OPTION);
            }
            if (cli.hasOption(PROJECT_OPTION)) {
                argProject = cli.getOptionValue(PROJECT_OPTION);
            }
        }

        public void validate(CommandLine cli, String[] original) throws CLIToolOptionsException {
            /* does nothing by without action knowing the action.*/
            switch (action) {
                case run:
                    validateRunAction();
                    break;
                default:
                    throw new CLIToolOptionsException("Unexpected action: " + action);
            }
        }

        public void validateRunAction() throws CLIToolOptionsException {

            if (null == argJob && null == argIdlist) {
                throw new CLIToolOptionsException(
                        "run action: -" + Options.JOB_OPTION + "/--" + Options.JOB_OPTION_LONG + " option or -"
                        + Options.ID_OPTION + "/--"
                        + Options.ID_OPTION_LONG + " is required"
                );
            }
            if (null != argJob && null != argIdlist) {
                throw new CLIToolOptionsException(
                        "run action: -" + Options.JOB_OPTION + "/--" + Options.JOB_OPTION_LONG + " option and -"
                        + Options.ID_OPTION + "/--"
                        + Options.ID_OPTION_LONG + " cannot be combined, please specify only one."
                );
            }
            if (null != argJob && null == argProject) {
                argProject = getSingleProjectName();
                if (null != argProject) {
                    debug("# No project specified, defaulting to: " + argProject);
                } else {
                    throw new CLIToolOptionsException(
                            "run action: -" +
                            Options.JOB_OPTION +
                            "/--" +
                            Options.JOB_OPTION_LONG +
                            " option requires -"
                            +
                            Options.PROJECT_OPTION +
                            "/--"
                            +
                            Options.PROJECT_OPTION_LONG +
                            " option"
                    );
                }
            }
        }
    }


    /**
     * Reads the argument vector and constructs a {@link org.apache.commons.cli.CommandLine} object containing params
     *
     * @param args the cli arg vector
     *
     * @return a new instance of CommandLine
     *
     * @throws CLIToolOptionsException if arguments are incorrect
     */
    public CommandLine parseArgs(final String[] args) throws CLIToolOptionsException {
        CommandLine cli = super.parseArgs(args);
        if (args.length > 0 && !args[0].startsWith("-")) {
            try {
                action = Actions.valueOf(args[0]);
            } catch (IllegalArgumentException e) {
                throw new CLIToolOptionsException("Invalid action: " + args[0] + ", must be one of: " + Arrays.toString(
                    Actions.values()));
            }
        }
        return cli;
    }


    /**
     * Call the action
     *
     * @throws com.dtolabs.rundeck.core.cli.run.RunToolException
     *          if an error occurs
     */
    protected void go() throws RunToolException, CLIToolOptionsException {
        switch (action) {
            case run:
                jobrunAction();
                break;
            default:
                throw new CLIToolOptionsException("Unrecognized action: " + action);
        }
    }

    private void jobrunAction() throws RunToolException {
        final QueuedItemResult result;
        final String jobname;
        final String jobgroup;
        final String project;

        if (null != runOptions.argJob && runOptions.argJob.indexOf("/") >= 0) {
            //separate group and job name
            final Matcher m = Pattern.compile("^(/?(.*)/)?(.+)$").matcher(runOptions.argJob);
            if (m.matches()) {
                final String gmatch= m.group(2);
                jobgroup = "".equals(gmatch)?null:gmatch;
                jobname = m.group(3);
            } else {
                jobname = runOptions.argJob;
                jobgroup = null;
            }
        } else if (null != runOptions.argJob) {
            jobname = runOptions.argJob;
            jobgroup = null;
        } else {
            jobname = null;
            jobgroup = null;
        }
        project=runOptions.argProject;
        final boolean argFollow=followOptions.argFollow;
        final boolean argProgress=followOptions.argProgress;
        final boolean argQuiet=followOptions.argQuiet;

        if (nodefilterOptions.getArgIncludeNodes()!=null || nodefilterOptions.getArgExcludeNodes()!=null) {
            warn("-I/-X are deprecated, use -F with a filter string. This option will be removed in a future version.");
        }
        final String nodeFilter = null != nodefilterOptions.getArgNodeFilter() ? nodefilterOptions.getArgNodeFilter() :
                null != nodefilterOptions.getNodeSet() ? NodeSet.generateFilter(nodefilterOptions.getNodeSet()) : null;
        final Boolean argKeepgoing = nodefilterOptions.isKeepgoingSet() ? nodefilterOptions.isArgKeepgoing() : null;
        final Boolean argExcludePrecedence= nodefilterOptions.isArgExcludePrecedence();
        final int nodeThreadcount = nodefilterOptions.getArgThreadCount();
        final int loglevel = loglevelOptions.getLogLevel();
        final String[] extraOpts = extendedOptions.getExtendedOptions();

        try {
            result = getCentralDispatcher().queueDispatcherJob(new IDispatchedJob() {
                public String[] getArgs() {
                    return extraOpts;
                }

                public int getLoglevel() {
                    return loglevel;
                }

                public Map<String, Map<String, String>> getDataContext() {
                    return null;
                }

                @Override
                public String getNodeFilter() {
                    return nodeFilter;
                }

                public Boolean getNodeExcludePrecedence() {
                    return argExcludePrecedence;
                }

                @Override
                public int getNodeThreadcount() {
                    return nodeThreadcount;
                }

                public IStoredJobRef getJobRef() {
                    return new IStoredJobRef() {
                        public String getJobId() {
                            return runOptions.argIdlist;
                        }

                        public String getName() {
                            return jobname;
                        }

                        public String getGroup() {
                            return jobgroup;
                        }

                        public String getProject() {
                            return project;
                        }
                    };
                }

                public Boolean isKeepgoing() {
                    return argKeepgoing;
                }
            });
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to run a job: " + e.getMessage();
            throw new RunToolException(msg, e);
        }
        if (null == result || !result.isSuccessful()) {
            throw new RunToolException(
                "Queued job request failed: " + (null != result ? result.getMessage() : "Result was null"));
        }
        final QueuedItem queuedItem = result.getItem();
        log("Job execution started:");
        log("[" + queuedItem.getId() + "] " + queuedItem.getName() + " <" + queuedItem.getUrl() + ">");
        if(!argFollow){
            return;
        }
        followOutput(queuedItem, argQuiet, argProgress);
    }

    /**
     * Perform follow action for the execution, using QueueTool implementation.
     */
    private void followOutput(QueuedItem item, final boolean quiet, final boolean progress) throws RunToolException {
        boolean successful = false;
        try {
            ConsoleExecutionFollowReceiver.Mode mode = ConsoleExecutionFollowReceiver.Mode.output;
            if(quiet){
                mode= ConsoleExecutionFollowReceiver.Mode.quiet;
            }else if(progress){
                mode= ConsoleExecutionFollowReceiver.Mode.progress;
            }
            successful = QueueTool.followAction(item.getId(), true, mode,
                                                System.out, this,
                                                getCentralDispatcher()
            );
        } catch (CentralDispatcherException e) {
            throw new RunToolException("Failed following output for execution: " + item.getId(), e);
        }
        if(!successful){
           exit(3);
        }
    }


    protected boolean isUseHelpOption() {
        return true;
    }

    public String getHelpString() {
        return "run : start running a Job on the server, and optionally follow the output\n"
               + "run -i <id>: Run a job by ID immediately\n"
               + "run -j <group/name>: Run a job by Name immediately. Group must be specified if name is not unique.\n"
               + "run -F <node filter> [-i/-j ...]: Specify node filters and run a Job\n"
               + "run -i <id> -- <arguments...>: Specify commandline arguments to the Job\n"
               + "run -i <id> --follow -- <arguments...>: Print output of the job as it is received\n"
               + "run -i <id> --follow --progress -- <arguments...>: Follow progress of the job\n"
               + "run -i <id> --follow --quiet -- <arguments...>: Wait until job is done and exit with job status\n"
            ;
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
    public void debug(final String message) {
        if (null != clilogger) {
            clilogger.debug(message);
        }
    }


}
