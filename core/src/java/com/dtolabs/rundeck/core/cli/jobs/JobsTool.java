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
package com.dtolabs.rundeck.core.cli.jobs;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.*;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * JobsTool commandline tool (rd-jobs), which provides actions for listing stored jobs from the server, and loading XML
 * definitions to the server. Server communication happens through the {@link com.dtolabs.rundeck.core.dispatcher.CentralDispatcher}
 * server layer. </p> <p> 'list' action: list stored
 * jobs matching query input, or all jobs if no query options are provided.  Optionally write the XML content to a file
 * indicated with the -f/--file option. </p> <p> 'load' action: load XML content from a file indicated with -f/--file
 * option to the server, listing the server's response about success/failure/skipped status for each Job defined.
 * Behavior when duplicate jobs already exist on the server can be specified using the </p>
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class JobsTool extends BaseTool implements IStoredJobsQuery, ILoadJobsRequest {
    /**
     * log4j
     */
    public static final Logger log4j = Logger.getLogger(JobsTool.class);

    /**
     * list action identifier
     */
    public static final String ACTION_LIST = "list";
    /**
     * load action identifier
     */
    public static final String ACTION_LOAD = "load";
    private StoredJobsRequestDuplicateOption duplicateOption = StoredJobsRequestDuplicateOption.update;

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

    /**
     * Get Name for use with list action
     *
     * @return name argument
     */
    public String getArgName() {
        return argName;
    }

    /**
     * Set name for use with list action
     *
     * @param argName job ID
     */
    public void setArgName(final String argName) {
        this.argName = argName;
    }

    /**
     * Return verbose
     *
     * @return is verbose
     */
    public boolean isArgVerbose() {
        return argVerbose;
    }

    /**
     * Set verbose
     *
     * @param argVerbose is verbose
     */
    public void setArgVerbose(final boolean argVerbose) {
        this.argVerbose = argVerbose;
    }

    public String getNameMatch() {
        return getArgName();
    }

    public String getGroupMatch() {
        return getArgGroup();
    }

    public String getIdlist() {
        return getArgIdlist();
    }

    public String getCommand() {
        return null;
    }

    public String getType() {
        return null;
    }

    public String getProjectFilter() {
        return getArgProject();
    }

    public String getResource() {
        return null;
    }

    /**
     * Return file argument
     *
     * @return file argument
     */
    public File getArgFile() {
        return argFile;
    }

    /**
     * Set the file argument
     *
     * @param argFile file for output or input depending on the action
     */
    public void setArgFile(final File argFile) {
        this.argFile = argFile;
    }

    public StoredJobsRequestDuplicateOption getDuplicateOption() {

        return duplicateOption;
    }

    /**
     * Return group option value
     *
     * @return option value
     */
    public String getArgGroup() {
        return argGroup;
    }

    /**
     * Set group option value
     *
     * @param argGroup group value
     */
    public void setArgGroup(final String argGroup) {
        this.argGroup = argGroup;
    }

    /**
     * Return idlist option value
     *
     * @return option value
     */
    public String getArgIdlist() {
        return argIdlist;
    }

    /**
     * Set idlist option value
     *
     * @param argIdlist group value
     */
    public void setArgIdlist(final String argIdlist) {
        this.argIdlist = argIdlist;
    }

    /**
     * Return project option value
     *
     * @return option value
     */
    public String getArgProject() {
        return argProject;
    }

    /**
     * Set project option value
     *
     * @param argProject group value
     */
    public void setArgProject(final String argProject) {
        this.argProject = argProject;
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
         * load action
         */
        load(ACTION_LOAD);
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

    private Actions action = Actions.list;
    private String argName;
    private String argGroup;
    private String argIdlist;
    private String argProject;
    private boolean argVerbose;
    private File argFile;
    private CLIToolLogger clilogger;
    /**
     * short option string for query parameter: group
     */
    public static final String GROUP_OPTION = "g";

    /**
     * short option string for query parameter: name
     */
    public static final String NAME_OPTION = "n";

    /**
     * short option string for query parameter: idlist
     */
    public static final String IDLIST_OPTION = "i";

    /**
     * short option string for file input/output path
     */
    public static final String FILE_OPTION = "f";

    /**
     * short option string for verbose
     */
    public static final String VERBOSE_OPTION = "v";

    /**
     * long option string for query parameter: name
     */
    public static final String NAME_OPTION_LONG = "name";

    /**
     * long option string for query parameter: group
     */
    public static final String GROUP_OPTION_LONG = "group";

    /**
     * long option string for query parameter: idlist
     */
    public static final String IDLIST_OPTION_LONG = "idlist";

    /**
     * long option string for file input/output path
     */
    public static final String FILE_OPTION_LONG = "file";

    /**
     * long option string for verbose
     */
    public static final String VERBOSE_OPTION_LONG = "verbose";

    /**
     * short option string for query parameter: project
     */
    public static final String PROJECT_OPTION = "p";

    /**
     * long option string for query parameter: project
     */
    public static final String PROJECT_OPTION_LONG = "project";

    /**
     * short option string for load option: duplicate
     */
    public static final String DUPLICATE_OPTION = "d";

    /**
     * long option string for load option: duplicate
     */
    public static final String DUPLICATE_OPTION_LONG = "duplicate";


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
        final JobsTool tool = new JobsTool(new DefaultCLIToolLogger());
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
    public JobsTool() {
        this(Framework.getInstance(Constants.getSystemBaseDir()), new Log4JCLIToolLogger(log4j));
    }

    protected boolean isUseHelpOption() {
        return true;
    }

    /**
     * Create QueueTool specifying the logger
     *
     * @param logger the logger
     */
    public JobsTool(final CLIToolLogger logger) {
        this(Framework.getInstance(Constants.getSystemBaseDir()), logger);
    }

    /**
     * Create QueueTool specifying the framework
     *
     * @param framework framework
     */
    public JobsTool(final Framework framework) {
        this(framework, null);
    }

    /**
     * Create QueueTool with the framework.
     *
     * @param framework the framework
     * @param logger    the logger
     */
    public JobsTool(final Framework framework, final CLIToolLogger logger) {
        this.framework = framework;
        this.clilogger = logger;
        if (null == clilogger) {
            clilogger = new Log4JCLIToolLogger(log4j);
        }
        final CommonOptions commonOptions = new CommonOptions();
        final LoadOptions loadOptions = new LoadOptions();
        final ListOptions listOptions = new ListOptions();
        addToolOptions(commonOptions);
        addToolOptions(loadOptions);
        addToolOptions(listOptions);
    }

    private class CommonOptions implements CLIToolOptions {
        public void addOptions(final Options options) {
            options.addOption(FILE_OPTION, FILE_OPTION_LONG, true,
                "File path. For list action, path to store the job definitions found in XML.  For load action, path to an XML file to upload.");
            options.addOption(VERBOSE_OPTION, VERBOSE_OPTION_LONG, false, "Enable verbose output");
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (cli.hasOption(VERBOSE_OPTION)) {
                argVerbose = true;
            }
            if (cli.hasOption(FILE_OPTION)) {
                argFile = new File(cli.getOptionValue(FILE_OPTION));
            }
        }

        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
        }
    }

    private class LoadOptions implements CLIToolOptions {
        public void addOptions(final Options options) {
            options.addOption(DUPLICATE_OPTION, DUPLICATE_OPTION_LONG, true,
                "Duplicate job behavior option. When loading jobs, treat definitions that already exist on the server in the given manner: 'update' existing jobs,'skip' the uploaded definitions, or 'create' them anyway. (load action. default: update)");
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (cli.hasOption(DUPLICATE_OPTION)) {
                final String dupeopt = cli.getOptionValue(DUPLICATE_OPTION);
                try {
                    duplicateOption = StoredJobsRequestDuplicateOption.valueOf(dupeopt);
                } catch (IllegalArgumentException e) {
                    throw new CLIToolOptionsException(
                        "Illegal value for --" + DUPLICATE_OPTION_LONG + ": '" + dupeopt + "', must be one of: "
                        + Arrays.toString(StoredJobsRequestDuplicateOption.values()));
                }
            }
        }

        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (Actions.load == action) {
                if (null != argName) {
                    warn("load action: -" + NAME_OPTION + "/--" + NAME_OPTION_LONG
                         + " option only valid with list action");
                }
                if (null != argGroup) {
                    warn("load action: -" + GROUP_OPTION + "/--" + GROUP_OPTION_LONG
                         + " option only valid with list action");
                }
                if (null != argIdlist) {
                    warn("load action: -" + IDLIST_OPTION + "/--" + IDLIST_OPTION_LONG
                         + " option only valid with list action");
                }
                if (null != argProject) {
                    warn("load action: -" + PROJECT_OPTION + "/--" + PROJECT_OPTION_LONG
                         + " option only valid with list action");
                }
                if (null == argFile) {
                    throw new CLIToolOptionsException(
                        "load action: -" + FILE_OPTION + "/--" + FILE_OPTION_LONG + " option is required");
                }
                if (null != argFile && !argFile.exists()) {
                    throw new CLIToolOptionsException(
                        "load action: -" + FILE_OPTION + "/--" + FILE_OPTION_LONG + " option: File does not exist: "
                        + argFile
                            .getAbsolutePath());
                }
            }

        }
    }

    /**
     * CLIToolOptions class for the list action
     */
    private class ListOptions implements CLIToolOptions {
        public void addOptions(final org.apache.commons.cli.Options options) {
            options.addOption(NAME_OPTION, NAME_OPTION_LONG, true,
                "Job Name. List jobs matching this name. (list action)");
            options.addOption(GROUP_OPTION, GROUP_OPTION_LONG, true,
                "Group name. List jobs within this group or sub-group (list action)");
            options.addOption(IDLIST_OPTION, IDLIST_OPTION_LONG, true,
                "Job ID List. List Jobs with these IDs explicitly. Comma-separated, e.g.: 1,2,3. (list action)");
            options.addOption(PROJECT_OPTION, PROJECT_OPTION_LONG, true,
                "Project name. List jobs within this project. (list action)");

        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (cli.hasOption(NAME_OPTION)) {
                argName = cli.getOptionValue(NAME_OPTION);
            }
            if (cli.hasOption(GROUP_OPTION)) {
                argGroup = cli.getOptionValue(GROUP_OPTION);
            }
            if (cli.hasOption(IDLIST_OPTION)) {
                argIdlist = cli.getOptionValue(IDLIST_OPTION);
            }
            if (cli.hasOption(PROJECT_OPTION)) {
                argProject = cli.getOptionValue(PROJECT_OPTION);
            }
        }

        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
        }
    }

    /**
     * Commandline
     */
    protected CommandLine cli;

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
        final CommandLine line = super.parseArgs(args);
        if (args.length > 0 && !args[0].startsWith("-")) {
            try {
                action = Actions.valueOf(args[0]);
            } catch (IllegalArgumentException e) {
                throw new CLIToolOptionsException("Invalid action: " + args[0] + ", must be one of: " + Arrays.toString(
                    Actions.values()));
            }
        }
        return line;
    }


    /**
     * Call the action
     *
     * @throws JobsToolException if an error occurs
     */
    protected void go() throws JobsToolException, CLIToolOptionsException {
        switch (action) {
            case list:
                listAction();
                break;
            case load:
                loadAction();
                break;
            default:
                throw new CLIToolOptionsException("Unrecognized action: " + action);
        }
    }

    public String getHelpString() {
        return "rd-jobs [<action>] [options...]: list Jobs on the server, or upload Jobs to the server from a file\n"
               + "\tList action (default):\n"
               + "rd-jobs [list] [query options] : list jobs matching the query, or all available\n"
               + "rd-jobs [list] --name <name> : Match jobs with the given name\n"
               + "rd-jobs [list] [query options] --file <output> : Save matched Jobs to output file as XML\n"
               + "\tLoad action:\n"
               + "rd-jobs load --file <file> : load jobs stored in XML file";
    }

    /**
     * Perform the kill action on a job, and print the result.
     *
     * @throws JobsToolException if an error occurs
     */
    private void loadAction() throws JobsToolException {
        final Collection<IStoredJobLoadResult> result;
        try {
            result = framework.getCentralDispatcherMgr().loadJobs(this, argFile);
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to load jobs: " + e.getMessage();
            throw new JobsToolException(msg, e);
        }
        if (null == result) {
            throw new JobsToolException("Upload request returned null");
        }
        log("Total Jobs Uploaded: " + result.size() + " jobs");
        //list failed jobs
        final ArrayList<IStoredJobLoadResult> failed = new ArrayList<IStoredJobLoadResult>();
        final ArrayList<IStoredJobLoadResult> skipped = new ArrayList<IStoredJobLoadResult>();
        final ArrayList<IStoredJobLoadResult> succeeded = new ArrayList<IStoredJobLoadResult>();
        for (final IStoredJobLoadResult item : result) {
            if (!item.isSuccessful()) {
                failed.add(item);
            } else if (item.isSkippedJob()) {
                skipped.add(item);
            } else {
                succeeded.add(item);
            }
        }
        //list failed jobs
        if (failed.size() > 0) {
            log("Failed to add " + failed.size() + " Jobs:");
            for (final IStoredJobLoadResult item : failed) {
                logStoredJobItem(item, item.getMessage());
            }
        }
        //list skipped jobs
        if (skipped.size() > 0) {
            log("Skipped " + skipped.size() + " Jobs:");
            for (final IStoredJobLoadResult item : skipped) {
                logStoredJobItem(item, null);
            }
        }
        //list succeeded jobs
        if (succeeded.size() > 0) {
            log("Succeeded creating/updating " + succeeded.size() + "  Jobs:");
            for (final IStoredJobLoadResult item : succeeded) {
                logStoredJobItem(item, null);
            }
        }

    }

    /**
     * Perform the list action and print the results.
     *
     * @throws JobsToolException if an error occurs
     */
    private void listAction() throws JobsToolException {
        final Collection<IStoredJob> result;
        try {
            result = framework.getCentralDispatcherMgr().listStoredJobs(this, null != argFile ? new FileOutputStream(
                argFile) : null);
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to list the queue: " + e.getMessage();
            throw new JobsToolException(msg, e);
        } catch (FileNotFoundException e) {
            final String msg = "Failed request to list the queue: " + e.getMessage();
            throw new JobsToolException(msg, e);
        }
        if (null != result) {
            log("Found " + result.size() + " jobs:");
            int i = 1;
            for (final IStoredJob item : result) {
                logStoredJobItem(item);
                i++;
            }
        } else {
            throw new JobsToolException("List request returned null");
        }
        if (null != argFile) {
            log("Wrote XML to file: " + argFile.getAbsolutePath());
        }


    }

    private void logStoredJobItem(final IStoredJob item) {
        log(MessageFormat.format("\t{0} {1} [{2}] <{3}>",
            "-",
            item.getName(),
            item.getJobId(),
            item.getUrl()));
        if (isArgVerbose() && null != item.getGroup() && !"".equals(item.getGroup())) {
            log("\t- " + item.getGroup() + "/ ");
        }
        if (isArgVerbose() && null != item.getDescription() && !"".equals(item.getDescription())) {
            log("\t- " + item.getDescription());
        }
    }

    private void logStoredJobItem(final IStoredJobLoadResult item, final String message) {
        log(MessageFormat.format("\t{0} {1}{2}{3}{4}{5}",
            "-",
            isArgVerbose() && null != item.getGroup() ? item.getGroup() + "/" : "",
            item.getName(),
            null != item.getJobId() ? " [" + item.getJobId() + "]" : "",
            null != item.getUrl() ? " <" + item.getUrl() + ">" : "",
            null != message ? " : " + message : ""));
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