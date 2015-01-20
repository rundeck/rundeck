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
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * JobsTool commandline tool (rd-jobs), which provides actions for listing stored jobs from the server, and loading XML
 * definitions to the server. Server communication happens through the {@link com.dtolabs.rundeck.core.dispatcher.CentralDispatcher}
 * server layer.
 * <p> 'list' action: list stored
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
    /**
     * purge action identifier
     */
    public static final String ACTION_PURGE = "purge";
    private StoredJobsRequestDuplicateOption duplicateOption = StoredJobsRequestDuplicateOption.update;
    private boolean uuidOptionRemove;

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

    public String getProjectFilter() {
        return getArgProject();
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

    public String getProject() {
        return getArgProject();
    }

    public StoredJobsRequestUUIDOption getUUIDOption() {
        return uuidOptionRemove ? StoredJobsRequestUUIDOption.remove : StoredJobsRequestUUIDOption.preserve;
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
        load(ACTION_LOAD),
        /**
         * load action
         */
        purge(ACTION_PURGE);
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
    String argProject;
    private boolean argVerbose;
    private File argFile;
    private JobDefinitionFileFormat format= JobDefinitionFileFormat.xml;
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
     * long option to remove UUIDs when importing jobs
     */
    public static final String REMOVE_UUID_OPTION_SHORT = "r";
    /**
     * long option to remove UUIDs when importing jobs
     */
    public static final String REMOVE_UUID_OPTION_LONG = "remove-uuids";

    /**
     * short option string for load option: format
     */
    public static final String FORMAT_OPTION = "F";

    /**
     * long option string for load option: format
     */
    public static final String FORMAT_OPTION_LONG = "format";


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
        PropertyConfigurator.configure(new File(Constants.getFrameworkConfigFile(),
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
        this(Framework.getInstanceWithoutProjectsDir(Constants.getSystemBaseDir()), new Log4JCLIToolLogger(log4j));
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
        this(Framework.getInstanceWithoutProjectsDir(Constants.getSystemBaseDir()), logger);
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
        internalResolver = new FrameworkSingleProjectResolver(framework);
        this.clilogger = logger;
        if (null == clilogger) {
            clilogger = new Log4JCLIToolLogger(log4j);
        }
        final CommonOptions commonOptions = new CommonOptions();
        final LoadOptions loadOptions = new LoadOptions();
        final ListOptions listOptions = new ListOptions();
        final PurgeOptions purgeOptions = new PurgeOptions();
        addToolOptions(commonOptions);
        addToolOptions(loadOptions);
        addToolOptions(listOptions);
        addToolOptions(purgeOptions);
    }

    private class CommonOptions implements CLIToolOptions {
        public void addOptions(final Options options) {
            options.addOption(FILE_OPTION, FILE_OPTION_LONG, true,
                "File path. For list action, path to store the job definitions found in XML.  For load action, path to an XML file to upload.");
            options.addOption(VERBOSE_OPTION, VERBOSE_OPTION_LONG, false, "Enable verbose output");
            options.addOption(FORMAT_OPTION, FORMAT_OPTION_LONG, true,
                "Format for input/output file. One of: " + Arrays.toString(JobDefinitionFileFormat.values()));
            options.addOption(PROJECT_OPTION, PROJECT_OPTION_LONG, true,
                    "Project name. List jobs within this project, or import jobs to this project.");
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (cli.hasOption(VERBOSE_OPTION)) {
                argVerbose = true;
            }
            if (cli.hasOption(FILE_OPTION)) {
                argFile = new File(cli.getOptionValue(FILE_OPTION));
            }
            if(cli.hasOption(FORMAT_OPTION)) {
                try {
                    format = JobDefinitionFileFormat.valueOf(cli.getOptionValue(FORMAT_OPTION));
                } catch (IllegalArgumentException e) {
                    throw new CLIToolOptionsException(
                        "Invalid format: " + cli.getOptionValue(FORMAT_OPTION) + ", must be one of: " + Arrays.toString(
                            JobDefinitionFileFormat.values()));
                }
            }
        }

        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
        }
    }

    private class LoadOptions implements CLIToolOptions {
        public void addOptions(final Options options) {
            options.addOption(DUPLICATE_OPTION, DUPLICATE_OPTION_LONG, true,
                "Duplicate job behavior option. When loading jobs, treat definitions that already exist on the server in the given manner: 'update' existing jobs,'skip' the uploaded definitions, or 'create' them anyway. (load action. default: update)");
            options.addOption(REMOVE_UUID_OPTION_SHORT, REMOVE_UUID_OPTION_LONG, false,
                "When loading jobs, remove any UUIDs while importing. (load action. default: false)");
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
            uuidOptionRemove = cli.hasOption(REMOVE_UUID_OPTION_SHORT);
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
                "Job Name. List jobs matching this name. (list/purge action)");
            options.addOption(GROUP_OPTION, GROUP_OPTION_LONG, true,
                "Group name. List jobs within this group or sub-group (list/purge action)");
            options.addOption(IDLIST_OPTION, IDLIST_OPTION_LONG, true,
                "Job ID List. List Jobs with these IDs explicitly. Comma-separated, e.g.: 1,2,3. (list/purge action)");

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
            if (Actions.list == action) {
                if(null==argProject){
                    if(internalResolver.hasSingleProject()) {
                        argProject = internalResolver.getSingleProjectName();
                        debug("# No project specified, defaulting to: " + argProject);
                    }else {
                        throw new CLIToolOptionsException(
                            ACTION_LIST + " action: -" + PROJECT_OPTION + "/--" + PROJECT_OPTION_LONG
                            + " option is required");
                    }
                }
            }
        }
    }

    /**
     * CLIToolOptions class for the purge action
     */
    private class PurgeOptions implements CLIToolOptions {
        public void addOptions(final org.apache.commons.cli.Options options) {
        }

        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            //todo: purge history option
        }

        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (Actions.purge == action) {
                if(null==argProject){
                    if(internalResolver.hasSingleProject()) {
                        argProject = internalResolver.getSingleProjectName();
                        debug("# No project specified, defaulting to: " + argProject);
                    }else {
                        throw new CLIToolOptionsException(
                            ACTION_PURGE + " action: -" + PROJECT_OPTION + "/--" + PROJECT_OPTION_LONG
                            + " option is required");
                    }
                }
                if (null == argGroup && null == argIdlist && null == argName) {
                    throw new CLIToolOptionsException(
                        ACTION_PURGE + " action: Some filter option is required");
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
            case purge:
                purgeAction();
                break;
            default:
                throw new CLIToolOptionsException("Unrecognized action: " + action);
        }
    }

    public String getHelpString() {
        return "rd-jobs [<action>] [options...]: list or delete Jobs on the server, or upload Jobs to the server from a file\n"
               + "\tList action (default):\n"
               + "rd-jobs [list] [query options] : list jobs matching the query, or all available\n"
               + "rd-jobs [list] --name <name> : Match jobs with the given name\n"
               + "rd-jobs [list] [query options] --file <output> : Save matched Jobs to output file as XML\n"
               + "rd-jobs [list] [query options] --file <output> --format <xml|yaml> : Save matched Jobs to output file as XML or YAML\n"
               + "\tPurge action:\n"
               + "rd-jobs purge -p <project> [query options] : Delete jobs from the project matching the options\n"
               + "rd-jobs purge -p <project> --file <file> [query options] : Delete jobs from the project matching the options, after saving them to a file\n"
               + "\tLoad action:\n"
               + "rd-jobs load --file <file> : load jobs stored in XML file, require each to define its project\n"
               + "rd-jobs load -p <project> --file <file> : load jobs stored in XML file to specific project\n"
               + "rd-jobs load --file <file> -F yaml : load jobs stored in YAML file";
    }

    /**
     * Perform the kill action on a job, and print the result.
     *
     * @throws JobsToolException if an error occurs
     */
    private void loadAction() throws JobsToolException {
        final Collection<IStoredJobLoadResult> result;
        try {
            result = framework.getCentralDispatcherMgr().loadJobs(this, argFile,format);
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to load jobs: " + e.getMessage();
            throw new JobsToolException(msg, e);
        }
        if (null == result) {
            throw new JobsToolException("Upload request returned null");
        }
        log("# Total Jobs Uploaded: " + result.size() + " jobs");
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
            log("# Failed to add " + failed.size() + " Jobs:");
            final ArrayList list = genJobDetailList(failed);
            HashMap map = new HashMap();
            map.put("failed", list);
            logYaml(map);
        }
        //list skipped jobs
        if (skipped.size() > 0) {
            log("# Skipped " + skipped.size() + " Jobs:");
            final ArrayList list = genJobDetailList(skipped);
            HashMap map = new HashMap();
            map.put("skipped", list);
            logYaml(map);
        }
        //list succeeded jobs
        if (succeeded.size() > 0) {
            log("# Succeeded creating/updating " + succeeded.size() + "  Jobs:");
            final ArrayList list = genJobDetailList(succeeded);
            HashMap map = new HashMap();
            map.put("succeeded", list);
            logYaml(map);
        }
        if(failed.size()>0) {
            throw new JobsToolException("Failed to load " + failed.size() + " Jobs");
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
            final FileOutputStream output = null != argFile ? new FileOutputStream(
                argFile) : null;
            try {
                result = framework.getCentralDispatcherMgr().listStoredJobs(this, output, format);
            } finally {
                if(null!=output){
                    output.close();
                }
            }
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to list the queue: " + e.getMessage();
            throw new JobsToolException(msg, e);
        } catch (IOException e) {
            final String msg = "Failed request to list the queue: " + e.getMessage();
            throw new JobsToolException(msg, e);
        }
        if (null != result) {
            log("# Found " + result.size() + " jobs:");
            logYaml(genJobDetailList(result));
        } else {
            throw new JobsToolException("List request returned null");
        }
        if (null != argFile) {
            log("Wrote "+ format+" to file: " + argFile.getAbsolutePath());
        }


    }

    /**
     * Perform the purge action and print the results.
     *
     * @throws JobsToolException if an error occurs
     */
    private void purgeAction() throws JobsToolException {
        final Collection<IStoredJob> result;
        final Collection<DeleteJobResult> deleteresult;
        final FileOutputStream output;
        try {
            output = null != argFile ? new FileOutputStream(
                    argFile) : null;
        } catch (FileNotFoundException e) {
            final String msg = "Failed to open output file for writing: " + argFile + ": " + e.getMessage();
            throw new JobsToolException(msg, e);
        }
        try {
            try {
                result = framework.getCentralDispatcherMgr().listStoredJobs(this, output, format);
            } finally {
                if (null != output) {
                    output.close();
                }
            }

        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to list stored jobs: " + e.getMessage();
            throw new JobsToolException(msg, e);
        } catch (IOException e) {
            final String msg = "Failed to close output file: " + argFile + ": " + e.getMessage();
            throw new JobsToolException(msg, e);
        }
        if (null != argFile) {
            log("Wrote " + format + " to file: " + argFile.getAbsolutePath());
        }

        ArrayList<String> jobIds = new ArrayList<String>();
        for (final IStoredJob job : result) {
            jobIds.add(job.getJobId());
        }
        if (jobIds.size() == 0) {
            log("# Found 0 matching jobs");
            return;
        }
        if (argVerbose) {
            log("# Deleting " + result.size() + " jobs...");
        }

        try{
            deleteresult = framework.getCentralDispatcherMgr().deleteStoredJobs(jobIds);
        } catch (CentralDispatcherException e) {
            final String msg = "Failed request to delete jobs: " + e.getMessage();
            throw new JobsToolException(msg, e);
        }
        
        List<DeleteJobResult> successful = new ArrayList<DeleteJobResult>();
        List<DeleteJobResult> failed = new ArrayList<DeleteJobResult>();
        for (final DeleteJobResult jobResult : deleteresult) {
            if(!jobResult.isSuccessful()) {
                failed.add(jobResult);
            }else {
                successful.add(jobResult);
            }
        }
        //list skipped jobs
        if (successful.size() > 0) {
            log("# Deleted " + successful.size() + " Jobs:");
            final List list = genJobDetailList(successful);
            final Map map = new HashMap();
            map.put("deleted", list);
            logYaml(map);
        }
        //list failed jobs
        if (failed.size() > 0) {
            log("# Failed to delete " + failed.size() + " Jobs:");
            final List list = genJobDetailList(failed);
            final Map map = new HashMap();
            map.put("failed", list);
            logYaml(map);
            throw new JobsToolException("Failed to delete "+failed.size()+" jobs");
        }
    }

    private List<Map<String, String>> genJobDetailList(List<DeleteJobResult> result) {
        ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
        for (final DeleteJobResult item : result) {
            list.add(genJobDetail(item));
        }
        return list;
    }

    private Map<String, String> genJobDetail(DeleteJobResult item) {
        final HashMap<String, String> map = new HashMap<String, String>();
        map.put("id", item.getId());
        if (null != item.getMessage()) {
            map.put("message", item.getMessage());
        }
        if (null != item.getErrorCode()) {

            map.put("errorCode", item.getErrorCode());
        }
        return map;
    }

    private void logYaml(final Object list) {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        log(yaml.dump(list));

    }

    private ArrayList genJobDetailList(Collection<? extends IStoredJob> result) {
        ArrayList list = new ArrayList();
        for (final IStoredJob item : result) {
            list.add(genJobDetail(item));
        }
        return list;
    }

    private Object genJobDetail(final IStoredJob item) {
        final HashMap<String, String> map = new HashMap<String, String>();

        if(isArgVerbose()) {
            map.put("name", item.getName());
            if(null!=item.getDescription() && !"".equals(item.getDescription())){
                map.put("description", item.getDescription());
            }
            if(item instanceof IStoredJobLoadResult) {
                final IStoredJobLoadResult load = (IStoredJobLoadResult) item;
                if(null!=load.getMessage()) {
                    map.put("message", load.getMessage());
                }
            }
            if (null != item.getGroup()) {
                map.put("group", item.getGroup());
            }
            if(null != item.getJobId()){
                map.put("id", item.getJobId());
            }
            if(null!=item.getUrl()){
                map.put("url", item.getUrl());
            }
            if(null!=item.getProject()){
                map.put("project", item.getProject());
            }
        }else {
            final String ident = (null != item.getGroup() ? item.getGroup() + "/" : "") + item.getName();

            if (item instanceof IStoredJobLoadResult) {
                final IStoredJobLoadResult load = (IStoredJobLoadResult) item;
                if(!load.isSuccessful()){
                    map.put("job", ident);
                    if (null != load.getMessage()) {
                        map.put("reason", load.getMessage());
                    }
                    return map;
                }
            }
            final String desc = null != item.getDescription() && !"".equals(item.getDescription()) ? " - '" + item
                .getDescription()
                                                                                               + "'" : "";

            return ident + desc;
        }
        return map;
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
