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
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException;
import com.dtolabs.rundeck.core.dispatcher.IDispatchedScript;
import com.dtolabs.rundeck.core.dispatcher.QueuedItemResult;
import com.dtolabs.rundeck.core.execution.*;
import com.dtolabs.rundeck.core.execution.script.ScriptfileUtils;
import com.dtolabs.rundeck.core.utils.*;
import com.dtolabs.shared.reports.ReportAgent;
import org.apache.commons.cli.*;
import org.apache.log4j.PropertyConfigurator;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;

import java.io.*;
import java.util.*;

/**
 * Main class for <code>run-exec</code> command line tool. This command will dispatch the command either locally or
 * remotely.
 */
public class ExecTool implements CLITool,IDispatchedScript,CLILoggerParams {

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

    private String argIncludeNodes;

    private String argExcludeNodes;

    private File failedNodes;

    private Integer argThreadCount = 1;

    private String argProject;

    private boolean argQueue;
    private boolean argTerse;

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
        options.addOption("I", "nodes", true, "include node list");
        options.addOption("X", "xnodes", true, "exclude node list");
        options.addOption("F", "failednodes", true, "Filepath to store failed nodes");
        options.addOption("p", "project", true, "project name");
        options.addOption(FILTER_EXCLUDE_PRECEDENCE_OPT,
                FILTER_EXCLUDE_PRECEDENCE_LONG,
                true,
                "true/false. if true, exclusion filters have precedence over inclusion filters");
        options.addOption("s", "scriptfile", true, "scriptfile script file");
        options.addOption("S", "stdin", false, "read script from stdin");
        options.addOption("N", "nodesfile", true, "Path to arbitrary nodes file");
        options.addOption("Q", "queue", false, "Send the execution to the command dispatcher queue");
        options.addOption("z", "terse", false, "leave log messages unadorned");
    }

    /**
     * Reference to the framework instance
     */
    private Framework framework;
    private String baseDir;

    void setFramework(Framework framework) {
        this.framework = framework;
    }

    /**
     * boolean value specifying that exclusion node filters have precedence over inclusion filters
     */
    private boolean argExcludePrecedence = true;
    private String scriptpath;
    private InputStream scriptAsStream;
    private boolean inlineScript;
    private String inlineScriptContent;

    // Set to the value of -N,--nodeslist
    private String argNodesFile;

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
        this(Framework.getInstance(baseDir));
    }

    /**
     * Create a new ExecTool with the given framework instance.
     *
     * @param framework framework instance
     */
    public ExecTool(Framework framework) {
        this.framework = framework;
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
        for (int i = 0 ; i < args.length ; i++) {
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
        if (cli.hasOption('z') || (System.getProperties().containsKey("rdeck.cli.terse") && "true".equalsIgnoreCase(
            System.getProperty("rdeck.cli.terse")))) {
            argTerse = true;
        }

        if (cli.hasOption("Q")) {
            argQueue = true;
        }

        if (cli.hasOption("S") && cli.hasOption("s")) {
            throw new CoreException("-s and -S options are mutually exclusive");
        }

        if (cli.hasOption("s")) {
            scriptpath = cli.getOptionValue("s");
        }

        if (cli.hasOption("S")) {
            inlineScript = true;
        }

        if (cli.hasOption("F")) {
            failedNodes = new File(cli.getOptionValue("F"));
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
                argThreadCount = Integer.valueOf(cli.getOptionValue("C"));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("threadcount must be an integer");
            }
        }

        if (cli.hasOption('N')) {
            if (!new File(cli.getOptionValue('N')).exists()) {
                throw new IllegalArgumentException("specified nodes  file does not exist");
            }
            argNodesFile = cli.getOptionValue('N');
        }
        if (cli.hasOption("h")) {
            help();
            exit(1);
        }

        boolean parsedFailedNodes = false;
        //if failedNodes file exists, parse it for a list of node names to include.
        if (null != failedNodes && failedNodes.exists()) {
            includeMap = FailedNodesFilestore.parseFailedNodes(failedNodes);
            if (includeMap.size() > 0) {
                parsedFailedNodes = true;
            }
        }
        if (!parsedFailedNodes) {
            excludeMap = parseExcludeArgs(NodeSet.FILTER_KEYS);
            includeMap = parseIncludeArgs(NodeSet.FILTER_KEYS);
        }

        argExcludePrecedence = determineExclusionPrecedenceForArgs(args, cli);
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
        if (inlineScript) {
            list.add("-S");
        }
        if (null != failedNodes) {
            list.add("-F");
            list.add(failedNodes.getAbsolutePath());
        }
        if (null != argProject) {
            list.add("-p");
            list.add(argProject);
        }
        if (1 != argThreadCount) {
            list.add("-C");
            list.add(argThreadCount.toString());
        }
        if (null != argNodesFile) {
            list.add("-N");
            list.add(argNodesFile);
        }
        if (null != filterArgs) {
            for (String key : filterArgs.keySet()) {
                String value = filterArgs.get(key);
                list.add(key);
                list.add(value);
            }
        }
        if (null != argsDeferred && argsDeferred.length > 0) {
            list.add("--");
            list.addAll(Arrays.asList(argsDeferred));
        }

        //generate string
        list.add(0, "run-exec");
        return StringArrayUtil.asString(list.toArray(new String[list.size()]), " ");
    }

    /**
     * Parse the value of the -X option
     *
     * @param keys
     *
     * @return
     */

    protected Map parseExcludeArgs(String[] keys) {
        return parseFilterArgs(keys, cli, "X");
    }

    /**
     * Parse the value of the -X option.
     *
     * @param keys
     *
     * @return
     */
    protected Map parseIncludeArgs(String[] keys) {
        return parseFilterArgs(keys, cli, "I");
    }


    private  String[] inputArgs;
    /**
     * The run method carries out the lifecycle of the tool, parsing args, handling exceptions, and exiting with a
     * suitable exit code.
     *
     * @param args the cli arg vector
     */
    public void run(String[] args) {
        inputArgs=args;
        int exitCode = 1; //pessimistic initial value
        ThreadBoundOutputStream.bindSystemOut();
        ThreadBoundOutputStream.bindSystemErr();
        out=System.out;
        err=System.err;

        try {
            parseArgs(args);

            configurePrintStream(argQuiet);

            if ((null == argsDeferred || 0 == argsDeferred.length) && null == getScript()
                && null == getServerScriptFilePath() && !isInlineScript()) {
                listAction();
            } else if (argQueue) {
                queueAction();
            } else {
                runAction();
            }
            exitCode = 0;
        } catch (Throwable t) {
            if (null == t.getMessage() || argVerbose || "true".equals(System.getProperty("rdeck.traceExceptions"))) {
                t.printStackTrace();
            }
            error(t);
            if (t instanceof NodesetFailureException) {
                NodesetFailureException nfe = (NodesetFailureException) t;
                HashMap<String, String> failmap = new HashMap<String, String>();
                if (null != nfe.getNodeset() && nfe.getNodeset().size() > 0 && null == failedNodes) {
                    failmap.put("-I", "name=" + StringArrayUtil.asString(nfe.getNodeset().toArray(
                        new String[nfe.getNodeset().size()]), ","));
                }
                error("Execute this command to retry on the failed nodes:\n\t" + generateExecLine(failmap));
                exitCode = NodesetFailureException.EXIT_CODE;
            }
            if (t instanceof NodesetEmptyException) {
                exitCode = NODESET_EMPTY_EXIT_CODE;
            }
        }
        exit(exitCode);
    }

    /**
     * Exit code for a Nodeset Empty failure exception
     */
    public static final int NODESET_EMPTY_EXIT_CODE = 3;

    private void listAction() {
        /**
                 * List the nodes
         */
        IAction action = createListAction(getNodeSet());
        action.doAction();
    }

    /**
     * Call the CentralDispatcherMgr to submit the execution params to the central dispatch queue instead of executing
     * locally.
     *
     */
    private void queueAction() {
        final QueuedItemResult result;
        try {
            if (inlineScript && null == inlineScriptContent) {
                //pass input stream as the script stream so that the Queue action can serialize it for the remote request
                setScriptAsStream(instream);
            }
            result = framework.getCentralDispatcherMgr().queueDispatcherScript(this);
        } catch (CentralDispatcherException e) {
            error("Unable to queue the execution: " + e.getMessage());
            throw new CoreException("Unable to queue the execution: " + e.getMessage(), e);
        }
        if (null != result && result.isSuccessful()) {
            if (null != result.getMessage()) {
                out.println(result.getMessage());
            }
            out.println("Queued job ID: " + result.getItem().getId() + " <" + result.getItem().getUrl() + ">");
        } else {
            throw new CoreException(
                "Queued job request failed: " + (null != result ? result.getMessage() : "Result was null"));
        }

    }

    /**
     * Execute the script with the ExecutionService layer, using a default build listener
     */
    private void runAction() throws Exception {
        runAction(null);
    }

    public static final String DEFAULT_LOG_FORMAT = "[%user@%node %command][%level] %message";
    public static final String FRAMEWORK_LOG_RUNDECK_EXEC_CONSOLE_FORMAT = "framework.log.run-exec.console.format";
    /**
     *
     * Execute the script with the ExecutionService layer, using a build listener
     *
     * @param listener a build listener
     */
    void runAction(final BuildListener listener) throws Exception {
        ExecutionResult result=null;
        final DispatchedScriptExecutionItem executionItem =
            ExecutionServiceFactory.createDispatchedScriptExecutionItem(this);
        final Date startDate=new Date();
        final String user = getFramework().getAuthenticationMgr().getUserInfoWithoutPrompt().getUsername();
        try {
            //set up build listener
            final BuildListener blistener;
            if(null!=listener){
                final HashMap<Integer,Integer> map = new HashMap<Integer, Integer>();
                //convert all INFO level messages to WARN level.
                //INFO is used by default for all ExecTask messages, and any messages for SSHExec will go out on
                //STDOUT so they are captured later in the stream.
                map.put(Project.MSG_INFO, Project.MSG_WARN);
                blistener=new LogLevelConvertBuildListener(listener, map);
            }else {
                blistener = createExecToolCommandLogger(getAntLoglevel(), null);
            }
            //store inline script content (via STDIN or script property) to a temp file
            if(inlineScript){
                File inlinefile=null;
                if(null!=inlineScriptContent) {
                    inlinefile = writeInlineContentToFile();
                }else {
                    inlinefile = writeStdinInputToFile();
                }
                setScriptpath(inlinefile.getAbsolutePath());
                setScriptAsStream(new FileInputStream(inlinefile));
            }else if(null!=scriptpath){
                setScriptAsStream(new FileInputStream(getScriptpath()));
            }
            final String logformat;
            if (getFramework().existsProperty(ExecTool.FRAMEWORK_LOG_RUNDECK_EXEC_CONSOLE_FORMAT)) {
                logformat = getFramework().getProperty(ExecTool.FRAMEWORK_LOG_RUNDECK_EXEC_CONSOLE_FORMAT);
            }else{
                logformat=null;
            }
            //configure execution listener
            final ExecutionListener executionListener = new CLIExecutionListener(blistener,
                FailedNodesFilestore.createListener(getFailedNodes()), this, this, argTerse, logformat);

            //acquire ExecutionService object
            final ExecutionService service = ExecutionServiceFactory.instance().createExecutionService(framework,
                executionListener);
            
            //submit the execution request to the service layer
            result = service.executeItem(executionItem);
        } catch (ExecutionException e) {
            error("Unable to perform the execution: " + e.getMessage());
        }


        final String tags;
        if (null != executionItem.getDispatchedScript().getNodeSet()
            && null != executionItem.getDispatchedScript().getNodeSet().getInclude()
            && executionItem.getDispatchedScript().getNodeSet().getInclude().getTags() != null) {
            tags = executionItem.getDispatchedScript().getNodeSet().getInclude().getTags();
        } else {
            tags = null;
        }
        final String script = CLIUtils.generateArgline("run-exec", inputArgs);
        if (null != result && result.isSuccess()) {
            debug("Finished execution: " + result.getResultObject());
            final String resultString =
                null != result.getResultObject() ?  result.getResultObject().toString() : "run-exec succeeded" ;
            ReportAgent.logExecInfo(
                executionItem.getDispatchedScript().getFrameworkProject(),
                user,
                "run-exec",
                com.dtolabs.shared.reports.Constants.ActionType.SUCCEED,getFramework().getFrameworkNodeName(),
                null,
                resultString,
                tags,
                script,
                startDate,
                new Date());
            return;
        }
        //log failure

        final String failureString = null!=result?(null != result.getResultObject() ? result.getResultObject().toString()
                                                                      : null != result && result.getException() != null
                                                                        ? result.getException().getMessage() : ""):"action failed: result was null";
        ReportAgent.logExecInfo(
            executionItem.getDispatchedScript().getFrameworkProject(),
            user,
            "run-exec",
            com.dtolabs.shared.reports.Constants.ActionType.FAIL, getFramework().getFrameworkNodeName(),
            null,
            failureString,
            tags,
            script,
            startDate,
            new Date());
        if(null!=result && null!=result.getException()) {
            throw result.getException();
        }else{
            throw new CoreException("action failed: result was null" );
        }

    }


    /**
     * Crete a NodeSet using the parsed argument values
     *
     * @return NodeSet
     */
    protected NodeSet createNodeSet() {
        return createNodeSet(includeMap, excludeMap, argExcludePrecedence, argThreadCount, argKeepgoing, failedNodes);
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
        return createNodeSet(includeMap, excludeMap, true, argThreadCount, argKeepgoing, failedNodes);
    }

    /**
     * Create a NodeSet using the included maps, and boolean exclude value
     *
     * @param includeMap        include map
     * @param excludeMap        exclude map
     * @param excludePrecedence if true, exclusion has precedence
     * @param threadCount       the threadcount
     * @param keepgoing         keepgoing boolean
     *
     * @param failedNodesfile file indicating list of failed nodes
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
     * @param args
     *
     * @throws Exception
     */
    public static void main(final String[] args) throws Exception {
        final File basedir = new File(Constants.getSystemBaseDir());
        /**
         * Initialize the log4j logger
         */
        PropertyConfigurator.configure(new File(new File(basedir, "etc"),
            "log4j.properties").getAbsolutePath());
        final ExecTool ExecTool = new ExecTool(basedir.getAbsolutePath());
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
            "run-exec [-h] [-v] [-V] [-q] [-p project] " +
            "[-I nodes] [-X xnodes] " +
            "[--threadcount <1>] [--keepgoing] " +
            "[--queue] " +
            "[[-S] | [-s <>] | [-- command-args]]",
            null,
            options,
            "Examples:\n"
            + "| run-exec\n | => Prints all nodes\n"
            + "| run-exec -p default -- whoami\n | => Runs the whoami command on all nodes\n"
            + "| run-exec -X node1 -- uptime\n | => Runs the uptime command on all nodes except node1\n"
            + "| run-exec -s myscript.sh\n | => Copies and then runs myscript.sh to matcing nodes\n"
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

    private void debug(String message) {
        if (argDebug) {
            log("debug: " +message);
        }
    }

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     */
    public void verbose(String message) {
        if (argVerbose) {
            log("verbose: "+message);
        }
    }


    /**
     * Creates a command logger appropriate for the message level
     *
     * @param level log level
     *
     * @return Creates a new CommandLogger
     */
    protected BuildListener createExecToolCommandLogger(final int level, final Reformatter gen) {
        // add the build listeners
        final ExecToolCommandLogger logger = new ExecToolCommandLogger(gen);
        logger.setMessageOutputLevel(level);
        logger.setOutputPrintStream(out);
        logger.setErrorPrintStream(err);
        logger.setEmacsMode(true);
        return logger;
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
                final File logfile = File.createTempFile("run-exec-", ".log", logsdir);
                ThreadBoundOutputStream.bindSystemOut().installThreadStream(new FileOutputStream(logfile));
            } catch (IOException e) {
                throw new CoreException("Cannot configure print stream to a file. " +
                                       "Failed created log file: " + e.getMessage());
            }
        }
    }

    public boolean isArgKeepgoing() {
        return argKeepgoing;
    }

    public void setArgKeepgoing(boolean argKeepgoing) {
        this.argKeepgoing = argKeepgoing;
    }

    public boolean isArgQuiet() {
        return argQuiet;
    }

    public void setArgQuiet(boolean argQuiet) {
        this.argQuiet = argQuiet;
    }

    public boolean isArgDebug() {
        return argDebug;
    }

    public void setArgDebug(boolean argDebug) {
        this.argDebug = argDebug;
    }

    public boolean isArgVerbose() {
        return argVerbose;
    }

    public void setArgVerbose(boolean argVerbose) {
        this.argVerbose = argVerbose;
    }

    public String getArgIncludeNodes() {
        return argIncludeNodes;
    }

    public void setArgIncludeNodes(String argIncludeNodes) {
        this.argIncludeNodes = argIncludeNodes;
    }

    public String getArgExcludeNodes() {
        return argExcludeNodes;
    }

    public void setArgExcludeNodes(String argExcludeNodes) {
        this.argExcludeNodes = argExcludeNodes;
    }

    public Integer getArgThreadCount() {
        return argThreadCount;
    }

    public void setArgThreadCount(Integer argThreadCount) {
        this.argThreadCount = argThreadCount;
    }

    public String getArgDepot() {
        return getArgFrameworkProject();
    }

    public void setArgDepot(String argProject) {
        setArgFrameworkProject(argProject);
    }

    public String getArgFrameworkProject() {
        return argProject;
    }

    public void setArgFrameworkProject(String argProject) {
        this.argProject = argProject;
    }

    public Map getExcludeMap() {
        return excludeMap;
    }

    public void setExcludeMap(Map excludeMap) {
        this.excludeMap = excludeMap;
    }

    public Map getIncludeMap() {
        return includeMap;
    }

    public void setIncludeMap(Map includeMap) {
        this.includeMap = includeMap;
    }

    public String[] getArgsDeferred() {
        return argsDeferred;
    }

    public void setArgsDeferred(String[] argsDeferred) {
        this.argsDeferred = argsDeferred;
    }

    public boolean isArgExcludePrecedence() {
        return argExcludePrecedence;
    }

    public void setArgExcludePrecedence(boolean argExcludePrecedence) {
        this.argExcludePrecedence = argExcludePrecedence;
    }

    public String getScriptpath() {
        return scriptpath;
    }

    public void setScriptpath(String scriptpath) {
        this.scriptpath = scriptpath;
    }

    public boolean isInlineScript() {
        return inlineScript;
    }

    public void setInlineScript(boolean inlineScript) {
        this.inlineScript = inlineScript;
    }

    public String getInlineScriptContent() {
        return inlineScriptContent;
    }

    public void setInlineScriptContent(String inlineScriptContent) {
        this.inlineScriptContent = inlineScriptContent;
    }

    public File getFailedNodes() {
        return failedNodes;
    }

    public void setFailedNodes(File failedNodes) {
        this.failedNodes = failedNodes;
    }

    public boolean isArgQueue() {
        return argQueue;
    }

    public void setArgQueue(boolean argQueue) {
        this.argQueue = argQueue;
    }

    Framework getFramework() {
        return framework;
    }

    public NodeSet getNodeSet() {
        return createNodeSet();
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

    public String[] getArgs() {
        return getArgsDeferred();
    }

    public int getLoglevel() {
        return getAntLoglevel();
    }

    public Map<String, Map<String, String>> getDataContext() {
        return null;
    }

    public Map<String, String> getOptions() {
        return null;
    }

    /**
     * Return true if exclusion should have precedence in node filter args
     * @param args all commandline args
     * @param cli parsed CommandLine
     * @return true if --filter-exclusion-precedence is true, or -I is not specified before -X
     */
    static boolean determineExclusionPrecedenceForArgs(String[] args, final CommandLine cli) {
        if (cli.hasOption(FILTER_EXCLUDE_PRECEDENCE_OPT)) {
            return  "true".equals(cli.getOptionValue(FILTER_EXCLUDE_PRECEDENCE_OPT));
        }else{
            //determine if -X or -I appears first in args list, and set precendence for first item
            for (int i = 0; i < args.length; i++) {
                String option = args[i];
                if("-X".equals(option) || "--xnodes".equals(option)) {
                    return true;
                }else if("-I".equals(option) || "--nodes".equals(option)){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Parse the values as key=value pairs, using the set of allowed keys.  If there is only
     * one entry in the values array without a key, then the first key of the allowed keys is used as the default
     *
     * @param keys allowed keys for the key=value strings, the first key is used as the default key
     * @param values array of key=value strings, or merely 1 value string if the array is size 1
     * @return map of the key to values
     */
    protected static Map<String, String> parseMultiNodeArgs(String[] keys, String[] values) {
        HashMap<String,String> map = new HashMap<String,String>();

        if (null != values && values.length > 0) {
            HashSet set = new HashSet();
            set.addAll(Arrays.asList(keys));
            for (int i = 0; i < values.length; i++) {
                String exclude = values[i];
                int i1 = exclude.indexOf("=");
                if (i1 > 0 && i1 <= exclude.length() - 1) {
                    String k = exclude.substring(0, i1);
                    String v = exclude.substring(i1 + 1);
                    map.put(k, v);
                } else if (i1 < 0 ) {
                    map.put(keys[0], exclude);
                }
            }
        }
        return map;
    }

    protected static Map<String, String> parseFilterArgs(String[] keys, CommandLine cli, String opt) {
        String[] strings = cli.getOptionValues(opt);
        if (null == strings || strings.length == 0 ) {
            if(null!= cli.getOptionValue(opt)){
                strings = new String[]{
                    cli.getOptionValue(opt)
                };
            }
        }
        return parseMultiNodeArgs(keys, strings);
    }

    /**
     * Defines generic action methods
     * @deprecated no longer public, only used for relic list code, should be refactored out
     */
    interface IAction  {
        void doAction();

    }

    abstract class AbstractAction implements IAction {
        public String getFrameworkProject() {
            return argProject;
        }

        public InputStream getScriptAsStream() throws IOException {
            return null;
        }

        public String getCommand() {
            return null;
        }

        public boolean hasScript() {
            return false;
        }

        public String getScript() {
            return null;
        }

        public String getServerScriptFilePath() {
            return null;
        }

        public String[] getArgs() {
            return argsDeferred;
        }

        public int getLoglevel() {
            return getAntLoglevel();
        }

        AbstractAction(NodeSet nodeset) {
            this.nodeset = nodeset;
        }

        NodeSet nodeset;

        public NodeSet getNodeSet() {
            return nodeset;
        }

        /**
         * Looks up node registrations in nodes.properties
         *
         * @return Nodes object
         */
        Nodes readNodesFile() {
            FrameworkProject project = framework.getFrameworkProjectMgr().getFrameworkProject(argProject);

            final Nodes n;

            try {
                if (null != argNodesFile) {
                    n = project.getNodes(new File(argNodesFile));
                } else {
                    n = project.getNodes();
                }
            } catch (NodeFileParserException e) {
                throw new CoreException("Error parsing nodes resource file: " + e.getMessage(), e);
            }
            return n;
        }

        public Collection<INodeEntry> filterNodes() {
            /**
             * Read the nodes.properties file
             */
            final Nodes n = readNodesFile();
            debug("total unfiltered nodes=" + n.countNodes());
            if (0 == n.countNodes()) {
                verbose("Empty node list");
            } else if (null != getNodeSet() && !(getNodeSet().getExclude().isBlank() && getNodeSet().getInclude()
                .isBlank())) {
                /**
                 * Apply the include/exclude filters to the list
                 */
                debug("applying nodeset filter... " + getNodeSet().toString());
                /**
                 * Reset collection to filter results
                 */
                return n.filterNodes(getNodeSet());
            }else{
                //list action defaults to listing all nodes when no filter is supplied, so fallthrough to list all nodes
            }
            /**
             * Retrieve the complete list of node entries
             */

            return n.listNodes();
        }

        /**
         * do the action
         */
        public abstract void doAction();
    }

    ListAction createListAction(NodeSet nodeset) {
        return new ListAction(nodeset);
    }

    /**
     * Action to display matching nodes
     */
    class ListAction extends AbstractAction {
        ListAction(NodeSet nodeset) {
            super(nodeset);
        }

        public void doAction() {
            final Collection c = filterNodes();
            log(formatResults(c, argVerbose).toString());
        }


        StringBuffer formatResults(Collection c, boolean verbose) {
            StringBuffer sb = new StringBuffer();
            int i = 0;
            for (Iterator iter = c.iterator() ; iter.hasNext() ;) {
                INodeEntry node = (INodeEntry) iter.next();
                if (verbose) {
                    String lineSep = System.getProperty("line.separator");
                    sb.append(node.getNodename()).append(":").append(lineSep);
                    sb.append("   ").append("hostname: ").append(node.getHostname()).append(lineSep);
                    sb.append("   ").append("os-arch: ").append(node.getOsArch()).append(lineSep);
                    sb.append("   ").append("os-family: ").append(node.getOsFamily()).append(lineSep);
                    sb.append("   ").append("os-name: ").append(node.getOsName()).append(lineSep);
                    sb.append("   ").append("os-version: ").append(node.getOsVersion()).append(lineSep);
                    sb.append("   ").append("tags: ").append(node.getTags());
                    final Map<String, String> attributes = node.getAttributes();
                    if (null != attributes && attributes.size() > 0) {
                        sb.append(lineSep);
                        sb.append("   ---- Attributes ---- ").append(lineSep);
                        for (String attr : attributes.keySet()) {
                            sb.append("   ").append(attr).append(": ").append(attributes.get(attr)).append(lineSep);
                        }
                    }
                    if (i < c.size() - 1) {
                        sb.append(lineSep);
                    }
                } else {
                    sb.append(node.getNodename());
                    if (i < c.size() - 1) {
                        sb.append(" ");
                    }
                }
                i++;
            }

            return sb;
        }
    }

    public static NodeDispatcher createNodeDispatcher() {
        return new DefaultNodeDispatcher();
    }
    /**
     * Execute a node dispatch request, in serial with parallel threads.
     *
     * @param project Ant project
     * @param nodes node set to iterate over
     * @param threadcount max number of parallel threads
     * @param keepgoing if true, continue execution even if a node fails
     * @param failedListener listener for results of failed nodes (when keepgoing is true)
     * @param factory factory to produce executable items given input nodes
     */
    public static void executeNodedispatch(final Project project, final Collection<INodeEntry> nodes,
                                           final int threadcount, final boolean keepgoing,
                                           final FailedNodesListener failedListener,
                                           final NodeCallableFactory factory) {
        createNodeDispatcher().executeNodedispatch(project, nodes, threadcount, keepgoing, failedListener, factory);
    }

    /**
     * This BuildListener repeats all events to a secondary BuildListener, but converts messageLogged events' priorities
     * (log level) based on the values given in the levels Map.
     */
    public static class LogLevelConvertBuildListener implements BuildListener {
        private BuildListener listener;
        private Map<Integer,Integer> levels = new HashMap<Integer, Integer>();

        /**
         * Create a new LogLevelConvertBuildListener
         *
         * @param listener existing BuildListener that will receive all messages
         * @param levels   map of Integer to Integer values, converting one log level into another.  See {@link
         *                 Project}
         */
        public LogLevelConvertBuildListener(final BuildListener listener, final Map<Integer, Integer> levels) {
            this.listener = listener;
            this.levels = levels;
        }

        public void messageLogged(final BuildEvent event) {
            final int prio = event.getPriority();

            if (levels.containsKey(prio)) {
                final int newprio = levels.get(prio);
                event.setMessage(event.getMessage(), newprio);
            }
            listener.messageLogged(event);
        }

        public void buildStarted(BuildEvent event) {
            listener.buildStarted(event);
        }

        public void buildFinished(BuildEvent event) {
            listener.buildFinished(event);
        }

        public void targetStarted(BuildEvent event) {
            listener.targetStarted(event);
        }

        public void targetFinished(BuildEvent event) {
            listener.targetFinished(event);
        }

        public void taskStarted(BuildEvent event) {
            listener.taskStarted(event);
        }

        public void taskFinished(BuildEvent event) {
            listener.taskFinished(event);
        }
    }



    InputStream instream=System.in;

    /**
     * Reads input from the STDIN channel and saves it to a temporary file. Files are stored in framework.var.dir (eg,
     * RDECK_BASE/var)
     *
     * @return File containing the user input from stdin
     */
    protected File writeStdinInputToFile() {
        return writeInputToFile(instream);
    }

    protected File writeInputToFile(final InputStream input) {
        verbose("reading stdin and saving it to file...");
        final File tempfile;
        try {
            tempfile = ScriptfileUtils.writeScriptTempfile(framework, input);
        } catch (IOException e) {
            throw new CoreException("error while reading script input from stdin: " + e.getMessage());
        }
        verbose("Wrote stdin input to file: " + tempfile);
        try {
            ScriptfileUtils.setExecutePermissions(tempfile);
        } catch (IOException e) {
            warn(
                "Failed to set execute permissions on tempfile, execution may fail: " + tempfile.getAbsolutePath());
        }
        return tempfile;
    }

    /**
     * Reads input from the STDIN channel and saves it to a temporary file. Files are stored in framework.var.dir (eg,
     * RDECK_BASE/var)
     *
     * @return File containing the user input from stdin
     */
    protected File writeInlineContentToFile() {
        final File tempfile;
        verbose("writing inline content to temporary file...");
        try {
             tempfile = ScriptfileUtils.writeScriptTempfile(framework, getInlineScriptContent());
        } catch (IOException e) {
            throw new CoreException("error while reading script input from stdin: " + e.getMessage());
        }
        verbose("Wrote inline script content to file: " + tempfile);
        try {
            ScriptfileUtils.setExecutePermissions(tempfile);
        } catch (IOException e) {
            warn(
                "Failed to set execute permissions on tempfile, execution may fail: " + tempfile.getAbsolutePath());
        }
        return tempfile;
    }

    /**
     * Return the Ant loglevel equivalent to the input flags (verbose,debug,quiet).
     * @return loglevel
     */
    public int getAntLoglevel(){
        if(argDebug) {
            return Project.MSG_DEBUG;
        }
        if (argQuiet && argVerbose) {
            return Project.MSG_WARN;
        }
        if(argVerbose) {
            return Project.MSG_VERBOSE;
        }
        if(argQuiet) {
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
}
