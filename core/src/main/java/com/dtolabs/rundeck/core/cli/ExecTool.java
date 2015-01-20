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

package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.*;
import com.dtolabs.rundeck.core.cli.project.ProjectToolException;
import com.dtolabs.rundeck.core.cli.queue.ConsoleExecutionFollowReceiver;
import com.dtolabs.rundeck.core.cli.queue.QueueTool;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.dispatcher.QueuedItem;
import com.dtolabs.rundeck.core.dispatcher.QueuedItemResult;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.utils.*;
import org.apache.commons.cli.*;
import org.apache.log4j.PropertyConfigurator;
import org.apache.tools.ant.Project;

import java.io.*;
import java.util.*;

/**
 * Main class for <code>dispatch</code> command line tool. This command will dispatch the command either locally or
 * remotely.
 */
public class ExecTool implements CLITool, IDispatchedScript, CLILoggerParams {
    /**
     * Short option value for filter exclude precedence option
     */
    static final String FILTER_EXCLUDE_PRECEDENCE_OPT = "Z";

    /**
     * Long option for filter exclude precedence option
     */
    static final String FILTER_EXCLUDE_PRECEDENCE_LONG = "filter-exclude-precedence";

    private boolean argVerbose = false;

    private boolean argDebug = false;

    private boolean argQuiet = false;

    private boolean argKeepgoing;

    private Integer nodeThreadcount = 1;

    private String argProject;

    private boolean argFollow;
    private boolean argProgress;

    private boolean shouldExit = false;

    protected PrintStream err = System.err;
    protected PrintStream out = System.out;

    /**
     * Reference to command line params
     */
    protected CommandLine cli;


    /**
     * reference to the command line {@link org.apache.commons.cli.Options} instance.
     */
    protected static final Options options = new Options();


    static {
        options.addOption("h", "help", false, "print usage");
        options.addOption("v", "verbose", false, "verbose mode");
        options.addOption("V", "debug", false, "Debug output mode");
        options.addOption("q", "quiet", false, "quiet mode");
        options.addOption("K", "keepgoing", false, "keep going if there are errors");
        options.addOption("C", "threadcount", true, "number of threads");
        options.addOption("F", "filter", true, "node filter string");
        options.addOption("I", "nodes", true, "include node list (deprecated, use --filter)");
        options.addOption("X", "xnodes", true, "exclude node list (deprecated, use --filter)");
        options.addOption("p", "project", true, "project name");
        options.addOption(FILTER_EXCLUDE_PRECEDENCE_OPT,
                FILTER_EXCLUDE_PRECEDENCE_LONG,
                true,
                "true/false. if true, exclusion filters have precedence over inclusion filters");
        options.addOption("s", "scriptfile", true, "scriptfile script file");
        options.addOption("u", "url", true, "script URL");
        options.addOption("S", "stdin", false, "read script from stdin");
        options.addOption("f", "follow", false, "Follow queued execution output");
        options.addOption("r", "progress", false, "In follow mode, print progress indicator chars");
    }

    /**
     * Reference to the framework instance
     */
    private Framework framework;
    private String baseDir;
    protected NodeFormatter nodeFormatter;
    private String nodeFilter;

    void setFramework(Framework framework) {
        this.framework = framework;
    }

    private boolean nodeExcludePrecedence = true;
    private String scriptpath;
    private InputStream scriptAsStream;
    private boolean inlineScript;
    private String inlineScriptContent;
    private String scriptURLString;

    /**
     * Create a new ExecTool initialized at the RDECK_BASE location via System property
     */
    ExecTool() {
        this(Constants.getSystemBaseDir());
    }

    /**
     * Create a new ExecTool with the given RDECK_BASE location
     *
     * @param baseDir path to RDECK_BASE
     */
    ExecTool(String baseDir) {
        this(Framework.getInstanceWithoutProjectsDir(baseDir));
    }

    /**
     * Create a new ExecTool with the given framework instance.
     *
     * @param framework framework instance
     */
    public ExecTool(Framework framework) {
        this.framework = framework;
        this.nodeFormatter = new NodeYAMLFormatter();
    }

    /**
     * array containing args to the Cli instance (e,g args after the "--" )
     */
    private String[] argsDeferred;

    private Map excludeMap = new HashMap();
    private Map includeMap = new HashMap();


    /**
     * Reads the argument vector and constructs a {@link org.apache.commons.cli.CommandLine} object containing params
     *
     * @param args the cli arg vector
     *
     * @return a new instance of CommandLine
     */
    public CommandLine parseArgs(String[] args) {
        int lastArg = -1;
        for (int i = 0; i < args.length; i++) {
            if ("--".equals(args[i])) {
                lastArg = i;
                break;
            }
        }
        if (lastArg >= 0) {
            final int argslen = args.length - (lastArg + 1);
            argsDeferred = new String[argslen];
            System.arraycopy(args, lastArg + 1, argsDeferred, 0, argslen);
        }

        final CommandLineParser parser = new PosixParser();
        try {
            cli = parser.parse(options, args);
        } catch (ParseException e) {
            help();
            throw new ProjectToolException(e);
        }
        if (cli.hasOption("K")) {
            argKeepgoing = true;
        }

        if (cli.hasOption("v")) {
            argVerbose = true;
        }

        if (cli.hasOption("V")) {
            argDebug = true;
        }

        if (cli.hasOption("q")) {
            argQuiet = true;
        }

        if (cli.hasOption("S") && cli.hasOption("s")) {
            throw new CoreException("-s and -S options are mutually exclusive");
        }

        if (cli.hasOption("s")) {
            scriptpath = new File(cli.getOptionValue("s")).getAbsolutePath();
        }


        if (cli.hasOption("u")) {
            scriptURLString = cli.getOptionValue("u");
        }

        if (cli.hasOption("S")) {
            inlineScript = true;
        }
        if (cli.hasOption("f")) {
            argFollow = true;
            if (cli.hasOption("r")) {
                argProgress = true;
            }
        }

        if (cli.hasOption("F")) {
            setNodeFilter(cli.getOptionValue("F"));
        }

        if (cli.hasOption('p')) {
            argProject = cli.getOptionValue("p");
        } else if (!cli.hasOption("p") &&
                framework.getFrameworkProjectMgr().listFrameworkProjects().size() == 1) {
            final FrameworkProject project =
                    (FrameworkProject) framework.getFrameworkProjectMgr().listFrameworkProjects().iterator().next();
            argProject = project.getName();
        }

        if (cli.hasOption("C")) {
            try {
                setNodeThreadcount(Integer.valueOf(cli.getOptionValue("C")));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("threadcount must be an integer");
            }
        }

        if (cli.hasOption("h")) {
            help();
            exit(1);
        }

        //if failedNodes file exists, parse it for a list of node names to include.

        final String[] keys = NodeSet.FILTER_KEYS_LIST.toArray(new String[NodeSet.FILTER_KEYS_LIST.size()]);
        excludeMap = parseExcludeArgs(keys);
        includeMap = parseIncludeArgs(keys);

        setNodeExcludePrecedence(determineExclusionPrecedenceForArgs(args, cli));
        if (null == argProject) {
            throw new IllegalArgumentException("project parameter not specified");
        }
        return cli;
    }

    /**
     * Generates the commandline execution string used.  If filterArgs are specified, they are used in place of the
     * commandline filter args when the -F option is not present.
     *
     * @param filterArgs set of filter commandline arguments to insert
     *
     * @return String of the previously executed commandline, modified with the given filterArgs if present
     */
    public String generateExecLine(Map<String, String> filterArgs) {
        ArrayList<String> list = new ArrayList<String>();
        if (argKeepgoing) {
            list.add("-K");
        }
        if (argVerbose) {
            list.add("-v");
        }
        if (argDebug) {
            list.add("-V");
        }
        if (argQuiet) {
            list.add("-q");
        }
        if (null != scriptpath) {
            list.add("-s");
            list.add(scriptpath);
        }
        if (null != scriptURLString) {
            list.add("-u");
            list.add(scriptURLString);
        }
        if (inlineScript) {
            list.add("-S");
        }
        if (null != argProject) {
            list.add("-p");
            list.add(argProject);
        }
        if (1 != getNodeThreadcount()) {
            list.add("-C");
            list.add(Integer.toString(getNodeThreadcount()));
        }
        if (null != filterArgs) {
            for (final Map.Entry<String, String> entry : filterArgs.entrySet()) {
                list.add(entry.getKey());
                list.add(entry.getValue());
            }
        }
        if (null != argsDeferred && argsDeferred.length > 0) {
            list.add("--");
            list.addAll(Arrays.asList(argsDeferred));
        }

        //generate string
        list.add(0, "dispatch");
        return StringArrayUtil.asString(list.toArray(new String[list.size()]), " ");
    }

    /**
     * Parse the value of the -X option
     *
     * @param keys keys
     *
     * @return parsed args
     */

    protected Map parseExcludeArgs(String[] keys) {
        return parseFilterArgs(keys, cli, "X");
    }

    /**
     * Parse the value of the -X option.
     *
     * @param keys keys
     *
     * @return parsed args
     */
    protected Map parseIncludeArgs(String[] keys) {
        return parseFilterArgs(keys, cli, "I");
    }


    /**
     * The run method carries out the lifecycle of the tool, parsing args, handling exceptions, and exiting with a
     * suitable exit code.
     *
     * @param args the cli arg vector
     */
    public void run(String[] args) {
        int exitCode = 1; //pessimistic initial value
        ThreadBoundOutputStream.bindSystemOut();
        ThreadBoundOutputStream.bindSystemErr();
        out = System.out;
        err = System.err;

        try {
            parseArgs(args);

            configurePrintStream(argQuiet);

            if (!hasNecessaryRunArgs()) {
                listAction();
            } else {
                queueAction();
            }
            exitCode = 0;
        } catch (Throwable t) {
            if (null == t.getMessage() || argVerbose || "true".equals(System.getProperty("rdeck.traceExceptions"))) {
                t.printStackTrace();
            }
            error(t);
            if (t instanceof NodesetEmptyException) {
                exitCode = NODESET_EMPTY_EXIT_CODE;
            }
        }
        exit(exitCode);
    }

    /**
     * Return true if any necessary args for dispatch are present.
     */
    private boolean hasNecessaryRunArgs() {
        return hasArgsDeferred()
                || null != getScript()
                || null != getServerScriptFilePath()
                || isInlineScript()
                || null != getScriptURLString();
    }

    private boolean hasArgsDeferred() {
        return null != argsDeferred && 0 != argsDeferred.length;
    }

    /**
     * Exit code for a Nodeset Empty failure exception
     */
    public static final int NODESET_EMPTY_EXIT_CODE = 3;

    /**
     * List the nodes
     */
    void listAction() {
        try {
            log((argVerbose ? getNodeFormatter() : new DefaultNodeFormatter()).formatNodes(filterNodes(false)
                    .getNodes()).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    NodeFormatter getNodeFormatter() {
        return nodeFormatter;
    }

    /**
     * Call the CentralDispatcherMgr to submit the execution params to the central dispatch queue instead of executing
     * locally.
     */
    private void queueAction() {
        final QueuedItemResult result;
        try {
            if (inlineScript && null == inlineScriptContent) {
                //pass input stream as the script stream so that the Queue action can serialize it for the remote
                // request
                setScriptAsStream(instream);
            }
            result = framework.getCentralDispatcherMgr().queueDispatcherScript(this);
        } catch (CentralDispatcherException e) {
            throw new CoreException("Unable to queue the execution: " + e.getMessage(), e);
        }
        if (null == result || !result.isSuccessful()) {
            throw new CoreException(
                    "Queued job request failed: " + (null != result ? result.getMessage() : "Result was null"));
        }
        if (null != result.getMessage()) {
            out.println(result.getMessage());
        }
        out.println("Queued Execution ID: " + result.getItem().getId() + " <" + result.getItem().getUrl() + ">");

        if (argFollow) {
            followOutput(result.getItem(), argQuiet, argProgress);
        }
    }

    /**
     * Perform follow action for the execution, using QueueTool implementation.
     */
    private void followOutput(QueuedItem item, final boolean quiet, final boolean progress) throws CoreException {
        boolean successful = false;
        try {
            ConsoleExecutionFollowReceiver.Mode mode = ConsoleExecutionFollowReceiver.Mode.output;
            if (quiet) {
                mode = ConsoleExecutionFollowReceiver.Mode.quiet;
            } else if (progress) {
                mode = ConsoleExecutionFollowReceiver.Mode.progress;
            }
            successful = QueueTool.followAction(item.getId(), true, mode, framework, System.out, this);
        } catch (CentralDispatcherException e) {
            throw new CoreException("Failed following output for execution: " + item.getId(), e);
        }
        if (!successful) {
            exit(3);
        }
    }

    public static final String DEFAULT_LOG_FORMAT = "[%user@%node %command][%level] %message";


    /**
     * Crete a NodeSet using the parsed argument values
     *
     * @return NodeSet
     */
    protected NodeSet createFilterNodeSelector() {
        if(null!=nodeFilter){
            NodeSet nodeSet = NodeSet.fromFilter(nodeFilter);
            nodeSet.setKeepgoing(isKeepgoing());
            nodeSet.setThreadCount(getNodeThreadcount());
            nodeSet.getInclude().setDominant(!getNodeExcludePrecedence());
            nodeSet.getExclude().setDominant(getNodeExcludePrecedence());
            return nodeSet;
        }
        return createNodeSet(includeMap, excludeMap, getNodeExcludePrecedence(), getNodeThreadcount(), argKeepgoing, null);
    }

    /**
     * Create a NodeSet using the included maps, where exclusion has precedence
     *
     * @param includeMap include map
     * @param excludeMap exclude map
     *
     * @return NodeSet
     */
    protected NodeSet createNodeSet(final Map includeMap, final Map excludeMap) {
        return createNodeSet(includeMap, excludeMap, true, getNodeThreadcount(), argKeepgoing, null);
    }

    /**
     * Create a NodeSet using the included maps, and boolean exclude value
     *
     * @param includeMap        include map
     * @param excludeMap        exclude map
     * @param excludePrecedence if true, exclusion has precedence
     * @param threadCount       the threadcount
     * @param keepgoing         keepgoing boolean
     * @param failedNodesfile   file indicating list of failed nodes
     *
     * @return NodeSet
     */
    protected static NodeSet createNodeSet(final Map includeMap, final Map excludeMap, final boolean excludePrecedence,
            final Integer threadCount, final boolean keepgoing,
            final File failedNodesfile) {
        final NodeSet nodeset = new NodeSet();
        nodeset.createExclude(excludeMap).setDominant(excludePrecedence);
        nodeset.createInclude(includeMap).setDominant(!excludePrecedence);
        nodeset.setThreadCount(threadCount);
        nodeset.setKeepgoing(keepgoing);
        nodeset.setFailedNodesfile(failedNodesfile);

        return nodeset;
    }

    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args args
     *
     * @throws Exception on error
     */
    public static void main(final String[] args) throws Exception {

        /**
         * Initialize the log4j logger
         */
        File configDir = Constants.getFrameworkConfigFile();
        PropertyConfigurator.configure(new File(configDir,
                "log4j.properties").getAbsolutePath());
        File systemBaseDir = new File(Constants.getSystemBaseDir());
        final ExecTool ExecTool = new ExecTool(systemBaseDir.getAbsolutePath());
        ExecTool.shouldExit = true;
        ExecTool.run(args);
    }

    /**
     * Calls the exit method
     *
     * @param exitcode return code to exit with
     */
    public void exit(int exitcode) {
        if (shouldExit) {
            System.exit(exitcode);
        } else if (0 != exitcode) {
            warn("ExecTool.exit() called while in embedded usage, not exitting.");
        }
    }

    /**
     * Writes help message to implementation specific output channel.
     */
    public void help() {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(80,
                "dispatch [-h] [-v] [-V] [-q] [-p project] " +
                        "[-F node-filter] " +
                        "[--threadcount <1>] [--keepgoing] " +
                        "[[-S] | [-s <>] | [-u <url>] | [-- command-args]]",
                null,
                options,
                "Examples:\n"
                        + "| dispatch\n | => Prints all nodes\n"
                        + "| dispatch -p default -f -- whoami\n | => Runs the whoami command on all nodes\n"
                        + "| dispatch -F '!name: node1' -f -- uptime\n | => Runs the uptime command on all nodes except node1\n"
                        + "| dispatch -s myscript.sh -f\n | => Copies and then runs myscript.sh to matching nodes\n"
                        + "| dispatch -u http://server/script.sh\n | => Downloads script URL, then runs on matching nodes\n"
                        + "\n"
                        + "[RUNDECK version " + VersionConstants.VERSION + " (" + VersionConstants.BUILD + ")]");
    }

    /**
     * Logs message via implementation specific log facility
     *
     * @param message message to log
     */
    public void log(String message) {
        System.out.println(message);
    }

    /**
     * Logs warning message via implementation specific log facility
     *
     * @param message message to log
     */
    public void warn(String message) {
        System.err.println("warn: " + message);
    }


    /**
     * Logs error message via implementation specific log facility
     *
     * @param message message to log
     */
    public void error(String message) {
        System.err.println("error: " + message);
    }

    private void error(final Throwable t) {
        error(t.getMessage());
    }

    public void debug(String message) {
        if (argDebug) {
            log("debug: " + message);
        }
    }

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     */
    public void verbose(String message) {
        if (argVerbose) {
            log("verbose: " + message);
        }
    }



    /**
     * Reconfigures the System.out PrintStream to write to a file output stream.
     *
     * @param quiet If true, configures to write to a file.
     */
    protected void configurePrintStream(boolean quiet) {
        if (quiet) {
            final String logpath = framework.getProperty("framework.logs.dir");
            if (null == logpath || "".equals(logpath)) {
                throw new CoreException("Cannot configure print stream to a file. "
                        + "framework.logs.dir property not set");
            }
            final File logsdir = new File(logpath);
            if (!logsdir.isDirectory() || !logsdir.exists()) {
                throw new CoreException("Cannot configure print stream to a file. " +
                        "Path does not exist or is not a directory: " + logpath);
            }
            try {
                final File logfile = File.createTempFile("dispatch-", ".log", logsdir);
                ThreadBoundOutputStream.bindSystemOut().installThreadStream(new FileOutputStream(logfile));
            } catch (IOException e) {
                throw new CoreException("Cannot configure print stream to a file. " +
                        "Failed created log file: " + e.getMessage());
            }
        }
    }


    public String getArgFrameworkProject() {
        return argProject;
    }

    public String[] getArgsDeferred() {
        return null != argsDeferred ? argsDeferred.clone() : null;
    }

    public String getScriptpath() {
        return scriptpath;
    }

    public boolean isInlineScript() {
        return inlineScript;
    }

    public String getInlineScriptContent() {
        return inlineScriptContent;
    }

    public Framework getFramework() {
        return framework;
    }

    public NodesSelector getNodeSelector() {
        return createFilterNodeSelector().nodeSelectorWithDefault(framework.getFrameworkNodeName());
    }

    public INodeSet getNodes() {
        return filterNodes(true);
    }

    public int getThreadCount() {
        return getNodeThreadcount();
    }

    public String getNodeRankAttribute() {
        return null;
    }

    public boolean isNodeRankOrderAscending() {
        return true;
    }

    public Boolean isKeepgoing() {
        return argKeepgoing;
    }

    public String getFrameworkProject() {
        return getArgFrameworkProject();
    }

    public String getScript() {
        return getInlineScriptContent();
    }

    public InputStream getScriptAsStream() {
        return scriptAsStream;
    }

    public void setScriptAsStream(final InputStream scriptAsStream) {
        this.scriptAsStream = scriptAsStream;
    }

    public String getServerScriptFilePath() {
        return getScriptpath();
    }

    public NodeSet getNodeSet() {
        return createFilterNodeSelector();
    }

    public String[] getArgs() {
        return getArgsDeferred();
    }

    public int getLoglevel() {
        if (argDebug) {
            return Constants.DEBUG_LEVEL;
        }
        if (argQuiet && argVerbose) {
            return Constants.WARN_LEVEL;
        }
        if (argVerbose) {
            return Constants.VERBOSE_LEVEL;
        }
        if (argQuiet) {
            return Constants.ERR_LEVEL;
        }
        return Constants.INFO_LEVEL;
    }

    public Map<String, Map<String, String>> getDataContext() {
        return null;
    }

    /**
     * Return true if exclusion should have precedence in node filter args
     *
     * @param args all commandline args
     * @param cli  parsed CommandLine
     *
     * @return true if --filter-exclusion-precedence is true, or -I is not specified before -X
     */
    static boolean determineExclusionPrecedenceForArgs(String[] args, final CommandLine cli) {
        if (cli.hasOption(FILTER_EXCLUDE_PRECEDENCE_OPT)) {
            return "true".equals(cli.getOptionValue(FILTER_EXCLUDE_PRECEDENCE_OPT));
        } else {
            //determine if -X or -I appears first in args list, and set precendence for first item
            for (int i = 0; i < args.length; i++) {
                String option = args[i];
                if ("-X".equals(option) || "--xnodes".equals(option)) {
                    return true;
                } else if ("-I".equals(option) || "--nodes".equals(option)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Parse the values as key=value pairs, using the set of allowed keys.  If there is only one entry in the values
     * array without a key, then the first key of the allowed keys is used as the default
     *
     * @param keys   allowed keys for the key=value strings, the first key is used as the default key
     * @param values array of key=value strings, or merely 1 value string if the array is size 1
     *
     * @return map of the key to values
     */
    protected static Map<String, String> parseMultiNodeArgs(String[] keys, String[] values) {
        HashMap<String, String> map = new HashMap<String, String>();

        if (null != values && values.length > 0) {
            for (String exclude : values) {
                int i1 = exclude.indexOf("=");
                if (i1 > 0 && i1 <= exclude.length() - 1) {
                    String k = exclude.substring(0, i1);
                    String v = exclude.substring(i1 + 1);
                    map.put(k, v);
                } else if (i1 < 0) {
                    map.put(keys[0], exclude);
                }
            }
        }
        return map;
    }

    protected static Map<String, String> parseFilterArgs(String[] keys, CommandLine cli, String opt) {
        String[] strings = cli.getOptionValues(opt);
        if (null == strings || strings.length == 0) {
            if (null != cli.getOptionValue(opt)) {
                strings = new String[]{
                        cli.getOptionValue(opt)
                };
            }
        }
        return parseMultiNodeArgs(keys, strings);
    }


    /**
     * Looks up node registrations in nodes.properties
     *
     * @return Nodes object
     */
    private INodeSet readNodesFile() {
        FrameworkProject project = framework.getFrameworkProjectMgr().getFrameworkProject(argProject);

        try {
            return project.getNodeSet();
        } catch (NodeFileParserException e) {
            throw new CoreException("Error parsing nodes resource file: " + e.getMessage(), e);
        }
    }



    INodeSet filterNodes() {
        return filterNodes(false);
    }

    INodeSet filterNodes(final boolean singleNodeDefault) {
        /**
         * Read the nodes.properties file
         */
        final INodeSet n = readNodesFile();
        debug("total unfiltered nodes=" + n.getNodeNames().size());
        final NodeSet filterNodeSelector = createFilterNodeSelector();
        if (0 == n.getNodeNames().size()) {
            verbose("Empty node list");
        } else {
            /**
             * Apply the include/exclude filters to the list
             */
            debug("applying nodeset filter... " + getNodeSelector().toString());
            /**
             * Reset collection to filter results
             */
            return NodeFilter.filterNodes(singleNodeDefault ? filterNodeSelector.nodeSelectorWithDefault(
                    framework.getFrameworkNodeName()) : filterNodeSelector.nodeSelectorWithDefaultAll(), n);
        }

        /**
         * Retrieve the complete list of node entries
         */

        return n;
    }

    void setNodeFormatter(final NodeFormatter nodeFormatter) {
        this.nodeFormatter = nodeFormatter;
    }

    public String getScriptURLString() {
        return scriptURLString;
    }

    public String getNodeFilter() {
        if(null!=nodeFilter){
            return nodeFilter;
        }
        NodeSet nodeSet = getNodeSet();
        if(null!= nodeSet) {
            return NodeSet.generateFilter(nodeSet);
        }
        return null;
    }

    public void setNodeFilter(String nodeFilter) {
        this.nodeFilter = nodeFilter;
    }

    /**
     * boolean value specifying that exclusion node filters have precedence over inclusion filters
     */
    public Boolean getNodeExcludePrecedence() {
        return nodeExcludePrecedence;
    }

    public void setNodeExcludePrecedence(boolean nodeExcludePrecedence) {
        this.nodeExcludePrecedence = nodeExcludePrecedence;
    }

    public int getNodeThreadcount() {
        return nodeThreadcount;
    }

    public void setNodeThreadcount(Integer nodeThreadcount) {
        this.nodeThreadcount = nodeThreadcount;
    }

    /**
     * Action to display matching nodes
     */
    static class DefaultNodeFormatter implements NodeFormatter {

        public StringBuffer formatNodes(Collection nodes) throws Exception {
            return formatResults(nodes);
        }

        StringBuffer formatResults(Collection c) {
            StringBuffer sb = new StringBuffer();
            int i = 0;
            for (Object aC : c) {
                INodeEntry node = (INodeEntry) aC;
                sb.append(node.getNodename());
                if (i < c.size() - 1) {
                    sb.append(" ");
                }
                i++;
            }

            return sb;
        }
    }

    public static interface NodeFormatter {
        public StringBuffer formatNodes(Collection nodes) throws Exception;
    }

    /**
     * Action to display matching nodes
     */
    static class NodeYAMLFormatter implements NodeFormatter {

        public StringBuffer formatNodes(final Collection nodes) throws Exception {
            return generate(nodes);
        }


        StringBuffer generate(final Collection c) throws NodesGeneratorException, IOException {
            final StringWriter writer = new StringWriter();
            NodesYamlGenerator gen = new NodesYamlGenerator(writer);
            for (Object aC : c) {
                gen.addNode((INodeEntry) aC);
            }
            gen.generate();
            return writer.getBuffer();
        }
    }

    InputStream instream = System.in;


    /**
     * Return the Ant loglevel equivalent to the input flags (verbose,debug,quiet).
     *
     * @return loglevel
     */
    public int getAntLoglevel() {
        if (argDebug) {
            return Project.MSG_DEBUG;
        }
        if (argQuiet && argVerbose) {
            return Project.MSG_WARN;
        }
        if (argVerbose) {
            return Project.MSG_VERBOSE;
        }
        if (argQuiet) {
            return Project.MSG_ERR;
        }
        return Project.MSG_INFO;
    }

    public boolean isDebug() {
        return argDebug;
    }

    public boolean isVerbose() {
        return argVerbose;
    }

    public boolean isQuiet() {
        return argQuiet;
    }

    public void log(String message, Map<String, String> context) {
        if (getAntLoglevel() >= Project.MSG_INFO) {
            log(message);
        }
    }

    public void error(String message, Map<String, String> context) {
        if (getAntLoglevel() >= Project.MSG_ERR) {
            log(message);
        }
    }

    public void warn(String message, Map<String, String> context) {
        if (getAntLoglevel() >= Project.MSG_WARN) {
            log(message);
        }
    }

    public void verbose(String message, Map<String, String> context) {
        if (getAntLoglevel() >= Project.MSG_VERBOSE) {
            log(message);
        }
    }

    public void debug(String message, Map<String, String> context) {
        if (getAntLoglevel() >= Project.MSG_DEBUG) {
            log(message);
        }
    }
}
