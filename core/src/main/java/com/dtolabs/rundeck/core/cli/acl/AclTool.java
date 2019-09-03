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

package com.dtolabs.rundeck.core.cli.acl;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.*;
import com.dtolabs.rundeck.core.authorization.providers.*;
import com.dtolabs.rundeck.core.cli.*;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.security.auth.Subject;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by greg on 4/2/15.
 */
public class AclTool extends BaseTool {
    /**
     * log4j
     */
    public static final Logger log4j = Logger.getLogger(AclTool.class);

    /**
     * short option string for file path
     */
    public static final String FILE_OPTION = "f";

    /**
     * long option string for file path
     */
    public static final String FILE_OPTION_LONG = "file";
    /**
     * short option string for dir
     */
    public static final String DIR_OPTION = "d";

    /**
     * long option string for dir
     */
    public static final String DIR_OPTION_LONG = "dir";
    public static final String ALLOW_LONG_OPT = "allow";
    public static final String ALLOW_OPT = "a";
    public static final String GROUPS_LONG_OPT = "groups";
    public static final String GROUPS_OPT = "g";
    public static final String USER_OPT = "u";
    public static final String USER_LONG_OPT = "user";
    public static final String PROJECT_OPT = "p";
    public static final String PROJECT_LONG_OPT = "project";
    public static final String PROJECT_ACL_OPT = "P";
    public static final String PROJECT_ACL_LONG_OPT = "projectacl";
    public static final String JOB_OPT = "j";
    public static final String JOB_LONG_OPT = "job";
    public static final String JOB_UUID_OPT = "I";
    public static final String JOB_UUID_LONG_OPT = "jobUuid";
    public static final String CONTEXT_OPT = "c";
    public static final String CONTEXT_LONG_OPT = "context";
    public static final String ADHOC_OPT = "A";
    public static final String ADHOC_LONG_OPT = "adhoc";
    public static final String NODE_OPT = "n";
    public static final String NODE_LONG_OPT = "node";
    public static final String TAGS_OPT = "t";
    public static final String TAGS_LONG_OPT = "tags";
    public static final String DENY_OPT = "D";
    public static final String DENY_LONG_OPT = "deny";
    public static final String VERBOSE_OPT = "v";
    public static final String VERBOSE_LONG_OPT = "verbose";
    public static final String VALIDATE_OPT = "V";
    public static final String VALIDATE_LONG_OPT = "validate";
    public static final String STORAGE_OPT = "s";
    public static final String STORAGE_LONG_OPT = "storage";
    public static final String GENERIC_OPT = "G";
    public static final String GENERIC_LONG_OPT = "generic";
    public static final String RESOURCE_OPT = "R";
    public static final String RESOURCE_LONG_OPT = "resource";
    public static final String INPUT_OPT = "i";
    public static final String INPUT_OPT_LONG = "input";
    public static final String REGEX_OPT = "r";
    public static final String REGEX_OPT_LONG = "regex";
    public static final String ATTRS_OPT = "b";
    public static final String ATTRS_OPT_LONG = "attributes";
    public static final String LIST_OPT = "l";
    public static final String LIST_OPT_LONG = "list";

    final CLIToolLogger clilogger;

    private Actions action = null;
    private static Comparator<Decision> comparator = new Comparator<Decision>() {
        @Override
        public int compare(final Decision o1, final Decision o2) {
            return o1.getAction().compareTo(o2.getAction());
        }
    };

    public AclTool(final CLIToolLogger cliToolLogger)
            throws IOException, PoliciesParseException
    {
        this(cliToolLogger, System.getProperty("rdeck.base"));
    }

    private String configDir;

    public AclTool(final CLIToolLogger cliToolLogger, final String rdeckBase)
            throws IOException, PoliciesParseException
    {
        if (null == cliToolLogger) {
            PropertyConfigurator.configure(Constants.getLog4jPropertiesFile().getAbsolutePath());
            clilogger = new Log4JCLIToolLogger(log4j);
        } else {
            this.clilogger = cliToolLogger;
        }
        configDir = System.getProperty("rdeck.config", rdeckBase + "/" + "etc");


        final TestOptions testOptions = new TestOptions();
        addToolOptions(testOptions);

    }

    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args command line arg vector
     *
     * @throws Exception action error
     */
    public static void main(final String[] args) throws Exception {
        final AclTool tool = new AclTool(new DefaultCLIToolLogger());
        tool.setShouldExit(true);
        int exitCode = 1; //pessimistic initial value

        try {
            tool.run(args);
            exitCode = 0;
        } catch (OptionsPrompt e) {
            exitCode = 2;
            tool.error(e.getMessage());
            tool.error(e.getPrompt());
            if (tool.argVerbose) {
                e.printStackTrace();
            }
        } catch (CLIToolOptionsException e) {
            exitCode = 2;
            tool.error(e.getMessage());
            if (tool.argVerbose) {
                e.printStackTrace();
            }
        } catch (Throwable e) {
            if (e.getMessage() == null || tool.argVerbose) {
                e.printStackTrace();
            }
            tool.error("Error: " + e.getMessage());
        }
        tool.exit(exitCode);
    }

    /**
     * list action identifier
     */
    public static final String ACTION_TEST = "test";
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_LIST = "list";
    public static final String ACTION_VALIDATE= "validate";

    static enum Actions {

        /**
         * List action
         */
        test(ACTION_TEST),
        create(ACTION_CREATE),
        list(ACTION_LIST),
        validate(ACTION_VALIDATE);
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

    @Override
    protected boolean isUseHelpOption() {
        return true;
    }

    private boolean argVerbose;
    private boolean argValidate;
    private boolean argList;
    private File argFile;
    private File argDir;
    private String argDenyAction;
    private List<String> actionsDenyList;
    private String argAllowAction;
    private List<String> actionsAllowList;
    private String argGroups;
    private List<String> groupsList;
    private String argUser;

    static enum Context {
        project,
        application
    }

    private Context argContext;
    private String argProject;
    private String argProjectAcl;
    private String argProjectJob;
    private String argProjectJobUUID;
    private String argProjectNode;
    private String argTags;
    private List<String> tagsSet;
    private boolean argProjectAdhoc;

    private String argAppStorage;
    private String argGenericType;
    private String argResource;
    private String argInput;
    private boolean argRegex;
    private Map<String, String> attrsMap;
    private boolean attrHelp;

    private class TestOptions implements CLIToolOptions {
        @Override
        public void addOptions(final Options options) {
            options.addOption(VERBOSE_OPT, VERBOSE_LONG_OPT, false, "Verbose output.");
            options.addOption(VALIDATE_OPT, VALIDATE_LONG_OPT, false, "Validate all input files.");

            options.addOption(
                    OptionBuilder.withArgName("file")
                                 .withLongOpt(FILE_OPTION_LONG)
                                 .hasArg()
                                 .withDescription("File path. Load the specified aclpolicy file.")
                                 .create(FILE_OPTION)
            );
            options.addOption(
                    OptionBuilder.withArgName("dir")
                                 .withLongOpt(DIR_OPTION_LONG)
                                 .hasArg()
                                 .withDescription(
                                         "Directory. Load all policy files in the specified directory."
                                 )
                                 .create(DIR_OPTION)
            );
            options.addOption(
                    OptionBuilder.withArgName("action,...")
                                 .withLongOpt(ALLOW_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Actions to test are allowed (test command) or to allow (create command)."
                                 )
                                 .create(ALLOW_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("action,...")
                                 .withLongOpt(DENY_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Actions to test are denied (test command) or to deny (create command)."
                                 )
                                 .create(DENY_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("group,...")
                                 .withLongOpt(GROUPS_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Subject Groups names to validate (test command) or for by: " +
                                         "clause (create command)."
                                 )
                                 .create(GROUPS_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("user,...")
                                 .withLongOpt(USER_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Subject User names to validate (test command) or for by: " +
                                         "clause (create command)."
                                 )
                                 .create(USER_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("project")
                                 .withLongOpt(PROJECT_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Name of project, used in project context or for application resource."
                                 )
                                 .create(PROJECT_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("projectacl")
                                 .withLongOpt(PROJECT_ACL_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Project name for ACL policy access, used in application context."
                                 )
                                 .create(PROJECT_ACL_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("group/name")
                                 .withLongOpt(JOB_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Job group/name. (project context)"
                                 )
                                 .create(JOB_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("uuid")
                            .withLongOpt(JOB_UUID_LONG_OPT)
                            .hasArg()
                            .withDescription(
                                    "Job uuid. (project context)"
                            )
                            .create(JOB_UUID_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("application | project")
                                 .withLongOpt(CONTEXT_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Context: either 'project' or 'application'."
                                 )
                                 .create(CONTEXT_OPT)
            );
            options.addOption(
                    OptionBuilder
                            .withLongOpt(ADHOC_LONG_OPT)
                            .withDescription(
                                    "Adhoc execution (project context)"
                            )
                            .create(ADHOC_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("nodename")
                                 .withLongOpt(NODE_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Node name. (project context)"
                                 )
                                 .create(NODE_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("tag,..")
                                 .withLongOpt(TAGS_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Node tags. If specified, the resource match will be defined using " +
                                         "'contains'. (project context)"
                                 )
                                 .create(TAGS_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("path/file")
                                 .withLongOpt(STORAGE_LONG_OPT)
                                 .hasArg()
                                 .withDescription(
                                         "Storage path/name. (application context)"
                                 )
                                 .create(STORAGE_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("kind")
                                 .withLongOpt(GENERIC_LONG_OPT)
                                 .hasArg()
                                 .withDescription("Generic resource kind.")
                                 .create(GENERIC_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("type")
                                 .withLongOpt(RESOURCE_LONG_OPT)
                                 .hasArg()
                                 .withDescription("Resource type name.")
                                 .create(RESOURCE_OPT)
            );
            options.addOption(
                    OptionBuilder.withArgName("file | -")
                                 .withLongOpt(INPUT_OPT_LONG)
                                 .hasArg()
                                 .withDescription("Read file or stdin for audit log data. (create command)")
                                 .create(INPUT_OPT)
            );
            options.addOption(
                    REGEX_OPT,
                    REGEX_OPT_LONG,
                    false,
                    "Match the resource using regular expressions. (create command)."
            );
            options.addOption(
                    LIST_OPT,
                    LIST_OPT_LONG,
                    false,
                    "List all permissions for the group or user. (test command)."
            );
            options.addOption(
                    OptionBuilder.withArgName("key=value ...")
                                 .withDescription(
                                         "Attributes for the resource. A sequence of key=value pairs, multiple pairs " +
                                         "can follow with a space. Use a value of '?' to see suggestions."
                                 )
                                 .withLongOpt(ATTRS_OPT_LONG)
                                 .withValueSeparator()
                                 .hasArgs()
                                 .create(ATTRS_OPT)
            );

        }

        @Override
        public void parseArgs(final CommandLine cli, final String[] original) throws CLIToolOptionsException {
            if (cli.hasOption(FILE_OPTION)) {
                argFile = new File(cli.getOptionValue(FILE_OPTION));
            } else if (cli.hasOption(DIR_OPTION)) {
                argDir = new File(cli.getOptionValue(DIR_OPTION));
            }
            if (cli.hasOption(ALLOW_OPT)) {
                argAllowAction = cli.getOptionValue(ALLOW_OPT);
                actionsAllowList = Arrays.asList(argAllowAction.split(", *"));
            }
            if (cli.hasOption(DENY_OPT)) {
                argDenyAction = cli.getOptionValue(DENY_OPT);
                actionsDenyList = Arrays.asList(argDenyAction.split(", *"));
            }
            if (cli.hasOption(GROUPS_OPT)) {
                argGroups = cli.getOptionValue(GROUPS_OPT);
                groupsList = Arrays.asList(argGroups.split(", *"));
            }
            if (cli.hasOption(USER_OPT)) {
                argUser = cli.getOptionValue(USER_OPT);
            }
            if (cli.hasOption(PROJECT_OPT)) {
                argProject = cli.getOptionValue(PROJECT_OPT);
            }
            if (cli.hasOption(PROJECT_ACL_OPT)) {
                argProjectAcl = cli.getOptionValue(PROJECT_ACL_OPT);
            }
            if (cli.hasOption(JOB_OPT)) {
                argProjectJob = cli.getOptionValue(JOB_OPT);
            }
            if (cli.hasOption(JOB_UUID_OPT)) {
                argProjectJobUUID = cli.getOptionValue(JOB_UUID_OPT);
            }
            if (cli.hasOption(CONTEXT_OPT)) {
                argContext = Context.valueOf(cli.getOptionValue(CONTEXT_OPT).toLowerCase());
            }
            if (cli.hasOption(ADHOC_OPT)) {
                argProjectAdhoc = cli.hasOption(ADHOC_OPT);
            }
            if (cli.hasOption(VERBOSE_OPT)) {
                argVerbose = cli.hasOption(VERBOSE_OPT);
            }
            if (cli.hasOption(VALIDATE_OPT)) {
                argValidate = cli.hasOption(VALIDATE_OPT);
            }
            if (cli.hasOption(NODE_OPT)) {
                argProjectNode = cli.getOptionValue(NODE_OPT);
            }
            if (cli.hasOption(STORAGE_OPT)) {
                argAppStorage = cli.getOptionValue(STORAGE_OPT);
            }
            if (cli.hasOption(GENERIC_OPT)) {
                argGenericType = cli.getOptionValue(GENERIC_OPT);
            }
            if (cli.hasOption(INPUT_OPT)) {
                argInput = cli.getOptionValue(INPUT_OPT);
            }
            if (cli.hasOption(REGEX_OPT)) {
                argRegex = cli.hasOption(REGEX_OPT);
            }
            if (cli.hasOption(LIST_OPT)) {
                argList = cli.hasOption(LIST_OPT);
            }
            if (cli.hasOption(TAGS_OPT)) {
                argTags = cli.getOptionValue(TAGS_OPT);
                tagsSet = Arrays.asList(argTags.split(", *"));
            }
            if (cli.hasOption(RESOURCE_OPT)) {
                argResource = cli.getOptionValue(RESOURCE_OPT);
            }
            if (cli.hasOption(ATTRS_OPT)) {
                attrsMap = new HashMap<>();
                cli.getOptionValues(ATTRS_OPT);
                String key = null;
                for (String s : cli.getOptionValues(ATTRS_OPT)) {
                    if (key == null) {
                        key = s;
                    } else if (s.equals("")) {
                        warn("Extraneous attribute key with no value: " + key);
                        attrHelp = true;
                        key = null;
                    } else if (!s.equals("")) {
                        attrsMap.put(key, s);
                        key = null;
                    }
                }
                if (key != null) {
                    attrHelp = true;
                    warn("Extraneous attribute key with no value: " + key);
                }
            }
        }

        @Override
        public void validate(final CommandLine cli, final String[] original) throws CLIToolOptionsException {

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
                throw new CLIToolOptionsException(
                        "Invalid action: " + args[0] + ", must be one of: " + Arrays.toString(
                                Actions.values()
                        )
                );
            }
        }
        if (line.hasOption("h")) {
            help();
            exit(1);
        }
        return line;
    }


    /**
     * Call the action
     *
     * @throws CLIToolOptionsException if an error occurs
     */
    protected void go() throws CLIToolOptionsException {
        if (null == action) {
            throw new CLIToolOptionsException("Command expected. Choose one of: " + Arrays.asList(Actions.values()));
        }
        try {
            switch (action) {
                case list:
                    listAction();
                    break;
                case test:
                    testAction();
                    break;
                case create:
                    createAction();
                    break;
                case validate:
                    validateAction();
                    break;
                default:
                    throw new CLIToolOptionsException("Unrecognized action: " + action);
            }
        } catch (IOException | PoliciesParseException e) {
            throw new CLIToolOptionsException(e);
        }
    }

    private void listAction() throws CLIToolOptionsException, IOException, PoliciesParseException {

        if (applyArgValidate()) {
            return;
        }

        final RuleEvaluator authorization = createAuthorization();
        Subject subject = createSubject();
        String subjdesc = null != argGroups ? "group " + argGroups : "username " + argUser;

        log("# Application Context access for "+subjdesc+"\n");
        //iterate app context resources, test actions
        if(null!=argProject){
            HashMap<String, Object> res = new HashMap<>();
            res.put("name", argProject);
            Map<String,Object> resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_PROJECT, res);

            logDecisions(
                    "project named \"" + argProject+"\"",
                    authorization,
                    subject,
                    resources(resourceMap),
                    new HashSet<>(appProjectActions),
                    createAppEnv()
            );
        }else{
            log("\n(No project (-p) specified, skipping Application context actions for a specific project.)\n");
        }
        if(null!=argProjectAcl){
            HashMap<String, Object> res = new HashMap<>();
            res.put("name", argProjectAcl);
            Map<String,Object> resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_PROJECT_ACL, res);

            logDecisions(
                    "project_acl for Project named \"" + argProjectAcl+"\"",
                    authorization,
                    subject,
                    resources(resourceMap),
                    new HashSet<>(appProjectAclActions),
                    createAppEnv()
            );
        }else{
            log("\n(No project_acl (-P) specified, skipping Application context actions for a ACLs for a specific project.)\n");
        }
        if(null!=argAppStorage){
            Map<String,Object> resourceMap = createStorageResource();
            logDecisions(
                    "storage path \"" + argAppStorage+"\"",
                    authorization,
                    subject,
                    resources(resourceMap),
                    new HashSet<>(appStorageActions),
                    createAppEnv()
            );
        }else{
            log("\n(No storage path (-s) specified, skipping Application context actions for a specific storage " +
                "path.)\n"
            );
        }
        for (String kind : appKindActionsByType.keySet()) {
            logDecisions(
                    kind,
                    authorization,
                    subject,
                    resources(AuthorizationUtil.resourceTypeRule(kind)),
                    new HashSet<>(appKindActionsByType.get(kind)),
                    createAppEnv()
            );
        }



        if (null == argProject) {
            log("\n(No project (-p) specified, skipping Project context listing.)");
            return;
        }
        Set<Attribute> projectEnv = createAuthEnvironment(argProject);

        log("\n# Project \"" + argProject + "\" access for " + subjdesc + "\n");
        //adhoc
        logDecisions(
                "Adhoc executions",
                authorization,
                subject,
                resources(createProjectAdhocResource()),
                new HashSet<>(projectAdhocActions),
                projectEnv
        );
        //job
        if(null!=argProjectJob){

            Map<String,Object> resourceMap = createProjectJobResource();
            logDecisions(
                    "Job \""+argProjectJob+"\"",
                    authorization,
                    subject,
                    resources(resourceMap),
                    new HashSet<>(projectJobActions),
                    projectEnv
            );
        }else if(null!=argProjectJobUUID) {
            Map<String,Object> resourceMap = createProjectJobUUIDResource();
            logDecisions(
                    "Job UUID\""+argProjectJobUUID+"\"",
                    authorization,
                    subject,
                    resources(resourceMap),
                    new HashSet<>(projectJobActions),
                    projectEnv
            );
        }else{
            log("\n(No job name(-j) or uuid (-I) specified, skipping Project context actions for a specific job.)\n");
        }
        //node

        if (null != argProjectNode || null != argTags) {
            Map<String, Object> resourceMap = createProjectNodeResource();
            logDecisions(
                    "Node " + (null!=argProjectNode?("\""+argProjectNode+"\""):"")  +
                    (null!=argTags?" tags: "+argTags:"")
                    ,
                    authorization,
                    subject,
                    resources(resourceMap),
                    new HashSet<>(projectNodeActions),
                    projectEnv
            );
        } else {
            log("\n(No node (-n) or tags (-t) specified, skipping Project context actions for a specific node or" +
                    " node tags.)\n");
        }

        //kinds

        for (String kind : projKindActionsByType.keySet()) {
            logDecisions(
                    kind,
                    authorization,
                    subject,
                    resources(AuthorizationUtil.resourceTypeRule(kind)),
                    new HashSet<>(projKindActionsByType.get(kind)),
                    projectEnv
            );
        }


    }

    private static Set<Attribute> createAppEnv() {
        return Framework.RUNDECK_APP_ENV;
    }

    private Set<Attribute> createAuthEnvironment(final String argProject) {
        return FrameworkProject.authorizationEnvironment(argProject);
    }

    /**
     * If argValidate is specified, validate the input, exit 2 if invalid. Print validation report if argVerbose
     * @return true if validation check failed
     * @throws CLIToolOptionsException
     */
    private boolean applyArgValidate() throws CLIToolOptionsException {
        if(argValidate) {
            Validation validation = validatePolicies();
            if(argVerbose && !validation.isValid()) {
                reportValidation(validation);
            }
            if(!validation.isValid()){
                log("The validation " + (validation.isValid() ? "passed" : "failed"));
                exit(2);
                return true;
            }
        }
        return false;
    }

    private HashSet<Map<String, String>> resources(final Map<String, Object>... resourceMap) {
        HashSet<Map<String, String>> resource = new HashSet<>();
        for (Map<String, Object> stringObjectMap : resourceMap) {
            resource.add(toStringMap(stringObjectMap));
        }
        return resource;
    }

    private void logDecisions(
            final String title,
            final RuleEvaluator authorization,
            final Subject subject,
            final HashSet<Map<String, String>> resource,
            final HashSet<String> actions,
            final Set<Attribute> env
    )
    {
        Set<Decision> evaluate = authorization.evaluate(
                resource,
                subject,
                actions,
                env
        );
        for (Decision decision : sortByAction(evaluate)) {
            log(
                    (decision.isAuthorized()
                     ? "+"
                     : decision.explain().getCode() == Explanation.Code.REJECTED_DENIED ? "!" : "-") +
                    " " +
                    decision.getAction() +
                    ": " +
                    title +
                    (decision.isAuthorized() ? "" : (" [" + decision.explain().getCode() + "]"))
            );
            if(!decision.isAuthorized() && decision.explain().getCode() == Explanation.Code.REJECTED_DENIED) {
                verbose(
                        "  " + decision.explain().toString()
                );
            }
        }
    }

    private Set<Decision> sortByAction(final Set<Decision> evaluate) {
        TreeSet<Decision> sorted = new TreeSet<>(comparator);
        sorted.addAll(evaluate);
        return sorted;
    }


    static final Set<String> projectTypes = new HashSet<>(
            Arrays.asList(
                    ACLConstants.TYPE_ADHOC,
                    ACLConstants.TYPE_JOB,
                    ACLConstants.TYPE_NODE
            )
    );
    static final Set<String> projectKinds = new HashSet<>(
            Arrays.asList(
                    ACLConstants.TYPE_JOB,
                    ACLConstants.TYPE_NODE,
                    ACLConstants.TYPE_EVENT
            )
    );
    static final Set<String> appTypes = new HashSet<>(
            Arrays.asList(
                    ACLConstants.TYPE_PROJECT,
                    ACLConstants.TYPE_PROJECT_ACL,
                    ACLConstants.TYPE_STORAGE,
                    ACLConstants.TYPE_APITOKEN
            )
    );
    static final Set<String> appKinds = new HashSet<>(
            Arrays.asList(
                    ACLConstants.TYPE_PROJECT,
                    ACLConstants.TYPE_SYSTEM,
                    ACLConstants.TYPE_SYSTEM_ACL,
                    ACLConstants.TYPE_USER,
                    ACLConstants.TYPE_JOB,
                    ACLConstants.TYPE_APITOKEN,
                    ACLConstants.TYPE_PLUGIN
            )
    );

    static final List<String> appProjectActions =
            Arrays.asList(
                    ACLConstants.ACTION_ADMIN,
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_CONFIGURE,
                    ACLConstants.ACTION_DELETE,
                    ACLConstants.ACTION_IMPORT,
                    ACLConstants.ACTION_EXPORT,
                    ACLConstants.ACTION_DELETE_EXECUTION,
                    ACLConstants.ACTION_SCM_IMPORT,
                    ACLConstants.ACTION_SCM_EXPORT
            );
    static final List<String> appProjectAclActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_UPDATE,
                    ACLConstants.ACTION_DELETE,
                    ACLConstants.ACTION_ADMIN
            );
    static final List<String> appStorageActions =
            Arrays.asList(
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_UPDATE,
                    ACLConstants.ACTION_DELETE
            );
    static final List<String> appApitokenActions =
            Arrays.asList(
                    ACLConstants.ACTION_CREATE
            );
    static final List<String> appProjectKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_CREATE
            );
    static final List<String> appSystemKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_ENABLE_EXECUTIONS,
                    ACLConstants.ACTION_DISABLE_EXECUTIONS,
                    ACLConstants.ACTION_ADMIN
            );
    static final List<String> appSystemAclKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_UPDATE,
                    ACLConstants.ACTION_DELETE,
                    ACLConstants.ACTION_ADMIN
            );
    static final List<String> appUserKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_ADMIN
            );
    static final List<String> appJobKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_ADMIN
            );
    static final List<String> appApitokenKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_ADMIN,
                    ACLConstants.ACTION_GENERATE_USER_TOKEN,
                    ACLConstants.ACTION_GENERATE_SERVICE_TOKEN
            );
    static final Map<String, List<String>> appResActionsByType;
    static final Map<String, List<String>> appResAttrsByType;

    static final List<String> appPluginActions = Arrays.asList(ACLConstants.ACTION_READ,
                                                            ACLConstants.ACTION_INSTALL,
                                                            ACLConstants.ACTION_UNINSTALL,
                                                            ACLConstants.ACTION_ADMIN);

    static {
        appResActionsByType = new HashMap<>();
        appResActionsByType.put(ACLConstants.TYPE_PROJECT, appProjectActions);
        appResActionsByType.put(ACLConstants.TYPE_PROJECT_ACL, appProjectAclActions);
        appResActionsByType.put(ACLConstants.TYPE_STORAGE, appStorageActions);
        appResActionsByType.put(ACLConstants.TYPE_APITOKEN, appApitokenActions);
    }

    static {
        appResAttrsByType = new HashMap<>();
        appResAttrsByType.put(ACLConstants.TYPE_PROJECT, Collections.singletonList("name"));
        appResAttrsByType.put(ACLConstants.TYPE_PROJECT_ACL, Collections.singletonList("name"));
        appResAttrsByType.put(ACLConstants.TYPE_STORAGE, Arrays.asList("path", "name"));
        appResAttrsByType.put(ACLConstants.TYPE_APITOKEN, Arrays.asList("username", "roles"));

    }

    static final Map<String, List<String>> appKindActionsByType;

    static {

        appKindActionsByType = new HashMap<>();
        appKindActionsByType.put(ACLConstants.TYPE_PROJECT, appProjectKindActions);
        appKindActionsByType.put(ACLConstants.TYPE_SYSTEM, appSystemKindActions);
        appKindActionsByType.put(ACLConstants.TYPE_SYSTEM_ACL, appSystemAclKindActions);
        appKindActionsByType.put(ACLConstants.TYPE_USER, appUserKindActions);
        appKindActionsByType.put(ACLConstants.TYPE_JOB, appJobKindActions);
        appKindActionsByType.put(ACLConstants.TYPE_APITOKEN, appApitokenKindActions);
        appKindActionsByType.put(ACLConstants.TYPE_PLUGIN, appPluginActions);
    }


    static final List<String> projectJobActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_VIEW,
                    ACLConstants.ACTION_UPDATE,
                    ACLConstants.ACTION_DELETE,
                    ACLConstants.ACTION_RUN,
                    ACLConstants.ACTION_RUNAS,
                    ACLConstants.ACTION_KILL,
                    ACLConstants.ACTION_KILLAS,
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_TOGGLE_EXECUTION,
                    ACLConstants.ACTION_TOGGLE_SCHEDULE,
                    ACLConstants.ACTION_SCM_UPDATE,
                    ACLConstants.ACTION_SCM_CREATE,
                    ACLConstants.ACTION_SCM_DELETE
            );
    static final List<String> projectJobKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_DELETE
            );
    static final List<String> projectAdhocActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_VIEW,
                    ACLConstants.ACTION_RUN,
                    ACLConstants.ACTION_RUNAS,
                    ACLConstants.ACTION_KILL,
                    ACLConstants.ACTION_KILLAS
            );
    static final List<String> projectNodeActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_RUN
            );
    static final Map<String, List<String>> projResActionsByType;
    static final Map<String, List<String>> projResAttrsByType;

    static {
        projResActionsByType = new HashMap<>();
        projResActionsByType.put(ACLConstants.TYPE_JOB, projectJobActions);
        projResActionsByType.put(ACLConstants.TYPE_ADHOC, projectAdhocActions);
        projResActionsByType.put(ACLConstants.TYPE_NODE, projectNodeActions);
    }


    static {
        projResAttrsByType = new HashMap<>();
        projResAttrsByType.put(ACLConstants.TYPE_JOB, Arrays.asList("group", "name", "uuid"));
        projResAttrsByType.put(ACLConstants.TYPE_ADHOC, new ArrayList<String>());
        List<String> nodeAttributeNames = Arrays.asList(
                "nodename",
                "rundeck_server",
                "username",
                "hostname",
                "osFamily",
                "osVersion",
                "(etc. any node attribute)"
        );
        projResAttrsByType.put(ACLConstants.TYPE_NODE, nodeAttributeNames);
    }

    static final List<String> projectNodeKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_UPDATE,
                    ACLConstants.ACTION_REFRESH
            );
    static final List<String> projectEventKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_CREATE
            );
    static final Map<String, List<String>> projKindActionsByType;

    static {

        projKindActionsByType = new HashMap<>();
        projKindActionsByType.put(ACLConstants.TYPE_JOB, projectJobKindActions);
        projKindActionsByType.put(ACLConstants.TYPE_NODE, projectNodeKindActions);
        projKindActionsByType.put(ACLConstants.TYPE_EVENT, projectEventKindActions);
    }

    private AuthRequest createAuthRequestFromArgs()
            throws CLIToolOptionsException, IOException, PoliciesParseException
    {
        //determine context
        if (null == argContext) {
            throw new OptionsPrompt(
                    optionDisplayString(CONTEXT_OPT, false) + " is required.",
                    "Choose one of: \n" +
                    "  -c " + Context.application + "\n" +
                    "    Access to projects, users, storage, system info, execution management.\n" +
                    "  -c " + Context.project + "\n" +
                    "    Access to jobs, nodes, events, within a project."
            );
        }
        if (argContext == Context.project && null == argProject) {
            throw new OptionsPrompt(
                    optionDisplayString(PROJECT_OPT, false) + " is required.",
                    "Choose the name of a project, or .*: \n" +
                    "  -p myproject\n" +
                    "  -p '.*'"
            );
        }
        boolean appContext = argContext == Context.application;
        Set<Attribute> environment = appContext ? createAppEnv() : createAuthEnvironment(argProject);

        //determine subject

        Subject subject = createSubject();

        //determine resource
        Map<String, Object> resourceMap = new HashMap<>();

        if (argContext == Context.application && argResource != null) {
            if (!appTypes.contains(argResource.toLowerCase())) {

                throw new OptionsPrompt(
                        optionDisplayString(RESOURCE_OPT, false) + " invalid resource type: " + argResource,
                        "  resource types in application context: " +
                        "    " + StringUtils.join(appTypes, "\n    ")

                );
            }
            resourceMap = AuthorizationUtil.resourceRule(argResource.toLowerCase(), null);
        } else if (argContext == Context.project && argResource != null) {
            if (!projectTypes.contains(argResource.toLowerCase())) {

                throw new OptionsPrompt(
                        optionDisplayString(RESOURCE_OPT, false) + " invalid resource type: " + argResource,
                        "  resource types in project context: " +
                        "    " + StringUtils.join(projectTypes, "\n    ")

                );
            }
            resourceMap = AuthorizationUtil.resourceRule(argResource.toLowerCase(), null);
        } else if (argContext == Context.application && argProject != null) {
            HashMap<String, Object> res = new HashMap<>();
            res.put("name", argProject);
            resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_PROJECT, res);
        } else if (argContext == Context.application && argProjectAcl != null) {
            HashMap<String, Object> res = new HashMap<>();
            res.put("name", argProjectAcl);
            resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_PROJECT_ACL, res);
        } else if (argContext == Context.application && argAppStorage != null) {
            resourceMap = createStorageResource();
        } else if (argContext == Context.project && argProjectJob != null) {
            resourceMap = createProjectJobResource();
        } else if (argContext == Context.project && argProjectJobUUID != null) {
            resourceMap = createProjectJobUUIDResource();
        } else if (argContext == Context.project && (argProjectNode != null || argTags != null)) {
            resourceMap = createProjectNodeResource();
        } else if (argContext == Context.project && argProjectAdhoc) {
            resourceMap = createProjectAdhocResource();
        } else if (argContext == Context.project && null != argGenericType) {
            if (!projectKinds.contains(argGenericType.toLowerCase())) {
                throw new OptionsPrompt(
                        optionDisplayString(GENERIC_OPT, false) + " invalid generic kind: " + argGenericType,
                        "  generic kinds in this context: " +
                        "    " + StringUtils.join(projectKinds, "\n    ")

                );
            }
            resourceMap = AuthorizationUtil.resourceTypeRule(argGenericType.toLowerCase());
        } else if (argContext == Context.application && null != argGenericType) {
            if (!appKinds.contains(argGenericType.toLowerCase())) {

                throw new OptionsPrompt(
                        optionDisplayString(GENERIC_OPT, false) + " invalid generic kind: " + argGenericType,
                        "  generic kind in this context: " +
                        "    " + StringUtils.join(appKinds, "\n    ")

                );
            }
            resourceMap = AuthorizationUtil.resourceTypeRule(argGenericType.toLowerCase());
        } else if (argContext == Context.project) {

            throw new OptionsPrompt(
                    "Project-context resource option is required.",
                    "Possible options:\n" +
                    "  Job: " +
                    optionDisplayString(JOB_OPT) +
                    "\n" +
                    "    View, modify, create*, delete*, run, and kill specific jobs,\n" +
                    "    and toggle whether schedule and/or execution are enabled.\n" +
                    "    * Create and delete also require additional " +
                    optionDisplayString(GENERIC_OPT) +
                    " level access.\n" +
                    "  Adhoc: " +
                    optionDisplayString(ADHOC_OPT) +
                    "\n" +
                    "    View, run, and kill adhoc commands.\n" +
                    "  Node: " +
                    optionDisplayString(NODE_OPT) +
                    "\n" +
                    "      : " +
                    optionDisplayString(TAGS_OPT) +
                    "\n" +
                    "    View and run on specific nodes by name or tag.\n" +
                    "  Resource: " +
                    optionDisplayString(RESOURCE_OPT) +
                    "\n" +
                    "    Specify the resource type directly. " +
                    optionDisplayString(ATTRS_OPT) +
                    " should also be used.\n" +
                    "    resource types in this context: \n" +
                    "    " +
                    StringUtils.join(projectTypes, "\n    ") +
                    "\n" +
                    "  Generic: " +
                    optionDisplayString(GENERIC_OPT) +
                    "\n" +
                    "    Create and delete jobs.\n" +
                    "    View and manage nodes.\n" +
                    "    View events.\n" +
                    "    generic kinds in this context: \n" +
                    "    " +
                    StringUtils.join(projectKinds, "\n    ")

            );
        } else {

            throw new OptionsPrompt(
                    "Application-context resource option is required.",
                    "Possible options:\n" +
                    "  Project: " +
                    optionDisplayString(PROJECT_OPT) +
                    "\n" +
                    "    Visibility, import, export, config, and delete executions.\n" +
                    "    *Note: Project create requires additional " +
                    optionDisplayString(GENERIC_OPT) +
                    " level access.\n" +
                    "  Project ACLs: " +
                    optionDisplayString(PROJECT_ACL_OPT) +
                    "\n" +
                    "    CRUD access for the project ACLs.\n" +
                    "  Storage: " +
                    optionDisplayString(STORAGE_OPT) +
                    "\n" +
                    "    CRUD access for the key storage system.\n" +
                    "  Resource: " +
                    optionDisplayString(RESOURCE_OPT) +
                    "\n" +
                    "    Specify the resource type directly. " +
                    optionDisplayString(ATTRS_OPT) +
                    " should also be used.\n" +
                    "    resource types in this context: \n" +
                    "    " +
                    StringUtils.join(appTypes, "\n    ") +
                    "\n" +
                    "  Generic: " +
                    optionDisplayString(GENERIC_OPT) +
                    "\n" +
                    "    Create projects, read system info, manage system ACLs, manage users, change\n" +
                    "      execution mode, manage plugins.\n" +
                    "    generic kinds" +
                    " in this context: \n" +
                    "    " +
                    StringUtils.join(appKinds, "\n    ")
            );
        }
        if (null != attrsMap && attrsMap.size() > 0) {
            resourceMap.putAll(attrsMap);
        } else if (attrHelp && null != argResource
                   && !argResource.equalsIgnoreCase(ACLConstants.TYPE_ADHOC)) {
            List<String> possibleAttrs =
                    (argContext == Context.application ? appResAttrsByType : projResAttrsByType)
                            .get(argResource.toLowerCase());
            throw new OptionsPrompt(
                    optionDisplayString(ATTRS_OPT) +
                    " should be specified when " +
                    optionDisplayString(RESOURCE_OPT) +
                    " is used",
                    "Possible attributes for resource type "+argResource+" in this context:\n" +
                    "  " + StringUtils.join(possibleAttrs, "\n  ")
            );
        }

        List<String> possibleActions = new ArrayList<>(Arrays.asList("*"));

        if (argContext == Context.application && null != argResource) {
            //actions for resources for application context
            possibleActions.addAll(appResActionsByType.get(argResource));
        } else if (argContext == Context.project && null != argResource) {
            //actions for resources for project context
            possibleActions.addAll(projResActionsByType.get(argResource));
        } else if (argContext == Context.application && argAppStorage != null) {
            //actions for job
            possibleActions.addAll(appStorageActions);
        } else if (argContext == Context.application && argProject != null) {
            //actions for job
            possibleActions.addAll(appProjectActions);
        }else if (argContext == Context.application && argProjectAcl != null) {
            //actions for project_acl
            possibleActions.addAll(appProjectAclActions);
        } else if (argContext == Context.application && argGenericType != null) {
            //actions for job
            possibleActions.addAll(appKindActionsByType.get(argGenericType.toLowerCase()));
        } else if (argContext == Context.project && argGenericType != null) {
            //actions for job
            possibleActions.addAll(projKindActionsByType.get(argGenericType.toLowerCase()));
        } else if (argContext == Context.project && (argProjectJob != null || argProjectJobUUID != null)) {
            //actions for job
            possibleActions.addAll(projectJobActions);
        } else if (argContext == Context.project && argProjectAdhoc) {
            //actions for job
            possibleActions.addAll(projectAdhocActions);
        } else if (argContext == Context.project && (argProjectNode != null || argTags != null)) {
            //actions for job
            possibleActions.addAll(projectNodeActions);
        }
        if (null == argAllowAction && null == argDenyAction) {
            //listing actions
            throw new OptionsPrompt(
                    optionDisplayString(ALLOW_OPT) + " or " +
                    optionDisplayString(DENY_OPT) + " is required.",
                    "Possible actions in this context: \n" +
                    "  " + StringUtils.join(possibleActions, "\n  ")
            );
        }
        if (null != argAllowAction) {
            //validate actions
            List<String> invalid = new ArrayList<>();
            for (String s : actionsAllowList) {
                if (!possibleActions.contains(s)) {
                    invalid.add(s);
                }
            }
            if (invalid.size() > 0) {
                throw new OptionsPrompt(
                        optionDisplayString(ALLOW_OPT, false) + " specified invalid actions.",
                        "These actions are not valid for the context:"
                        + "  " + StringUtils.join(invalid, "\n  ") +
                        "Possible actions in this context: \n" +
                        "  " + StringUtils.join(possibleActions, "\n  ")
                );
            }
        }
        if (null != argDenyAction) {
            //validate actions
            List<String> invalid = new ArrayList<>();
            for (String s : actionsDenyList) {
                if (!possibleActions.contains(s)) {
                    invalid.add(s);
                }
            }
            if (invalid.size() > 0) {
                throw new OptionsPrompt(
                        optionDisplayString(DENY_OPT, false) + " specified invalid actions.",
                        "These actions are not valid for the context:\n"
                        + "  " + StringUtils.join(invalid, "\n  ") + "\n\n" +
                        "Possible actions in this context:\n" +
                        "  " + StringUtils.join(possibleActions, "\n  ")
                );
            }
        }


        AuthRequest request = new AuthRequest();
        request.resourceMap = resourceMap;
        request.subject = subject;
        if (null != actionsAllowList) {
            request.actions = new HashSet<>(actionsAllowList);
        }
        request.environment = environment;
        if (null != actionsDenyList) {
            request.denyActions = new HashSet<>(actionsDenyList);
        }
        request.regexMatch = argRegex;
        request.containsMatch = argContext == Context.project && argTags != null;
        return request;
    }

    private Map<String, Object> createProjectNodeResource() {
        final Map<String, Object> resourceMap;HashMap<String, Object> res = new HashMap<>();
        if(null!=argProjectNode) {
            res.put("nodename", argProjectNode);
        }
        if(null!=argTags) {
            res.put("tags", tagsSet);
        }
        resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_NODE, res);
        return resourceMap;
    }

    private Map<String, Object> createProjectJobResource() {
        final Map<String, Object> resourceMap;HashMap<String, Object> res = new HashMap<>();
        int nx = argProjectJob.lastIndexOf("/");
        if (nx >= 0) {
            res.put("group", argProjectJob.substring(0, nx));
            res.put("name", argProjectJob.substring(nx + 1));
        } else {
            res.put("group", "");
            res.put("name", argProjectJob);
        }
        resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_JOB, res);
        return resourceMap;
    }

    private Map<String, Object> createProjectJobUUIDResource() {
        final Map<String, Object> resourceMap;HashMap<String, Object> res = new HashMap<>();
        res.put("uuid", argProjectJobUUID);
        resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_JOB, res);
        return resourceMap;
    }
    
    private Map<String, Object> createProjectAdhocResource() {
        return AuthorizationUtil.resourceRule(ACLConstants.TYPE_ADHOC, new HashMap<String, Object>());
    }

    private Map<String, Object> createStorageResource() {
        final Map<String, Object> resourceMap;HashMap<String, Object> res = new HashMap<>();
        int nx = argAppStorage.lastIndexOf("/");
        if (nx >= 0) {
            res.put("path", argAppStorage);
            res.put("name", argAppStorage.substring(nx + 1));
        } else {
            res.put("path", argAppStorage);
            res.put("name", argAppStorage);
        }
        resourceMap = AuthorizationUtil.resourceRule(ACLConstants.TYPE_STORAGE, res);
        return resourceMap;
    }

    private Subject createSubject() throws OptionsPrompt {
        final Subject subject;
        if (argGroups != null || argUser != null) {
            Subject t = makeSubject(argUser, groupsList);
            subject = t;
        } else {
            throw new OptionsPrompt(
                    optionDisplayString(GROUPS_OPT) + " or " +
                    optionDisplayString(USER_OPT) + " are required",
                    "  -u user1,user2... \n" +
                    "  -g group1,group2... \n" +
                    "    Groups control access for a set of users, and correspond\n" +
                    "    to authorization roles."
            );
        }
        return subject;
    }

    private Subject makeSubject(final String argUser1user, final Collection<String> groupsList1) {
        Subject t = new Subject();
        String user = argUser1user != null ? argUser1user : "user";
        t.getPrincipals().add(new Username(user));
        if (null != groupsList1) {
            for (String s : groupsList1) {
                t.getPrincipals().add(new Group(s));
            }
        }
        return t;
    }
    private void validateAction() throws CLIToolOptionsException, IOException, PoliciesParseException {
        if (null == argFile && null == argDir && null != configDir) {
            log("Using configured Rundeck etc dir: " + configDir);
        }
        final Validation validation = validatePolicies();
        reportValidation(validation);
        log("The validation " + (validation.isValid() ? "passed" : "failed"));
        if (!validation.isValid()) {
            exit(2);
        }
    }

    private void reportValidation(final Validation validation) {
        for (Map.Entry<String, List<String>> entry : validation.getErrors().entrySet()) {
            String ident = entry.getKey();
            List<String> value = entry.getValue();
            System.err.println(ident + ":");
            for (String s : value) {
                System.err.println("\t" + s);
            }
        }
    }

    private Validation validatePolicies() throws CLIToolOptionsException {
        final Validation validation;
        ValidationSet validationSet = new ValidationSet();
        if (null != argFile) {
            if(!argFile.isFile()) {
                throw new CLIToolOptionsException("File: " + argFile + ", does not exist or is not a file");
            }
            validation = YamlProvider.validate(YamlProvider.sourceFromFile(argFile, validationSet), validationSet);
        } else if (null != argDir) {
            if(!argDir.isDirectory()) {
                throw new CLIToolOptionsException("File: " + argDir + ", does not exist or is not a directory");
            }
            validation = YamlProvider.validate(YamlProvider.asSources(argDir), validationSet);
        } else if (null != configDir) {
            File directory = new File(configDir);
            if(!directory.isDirectory()) {
                throw new CLIToolOptionsException("File: " + directory + ", does not exist or is not a directory");
            }
            validation = YamlProvider.validate(YamlProvider.asSources(directory), validationSet);
        } else {
            throw new CLIToolOptionsException("-f or -d are required");
        }
        return validation;
    }

    private void createAction() throws CLIToolOptionsException, IOException, PoliciesParseException {
        List<AuthRequest> reqs = new ArrayList<>();
        if (null != argInput) {
            reqs = readRequests(argInput);
        } else {
            reqs.add(createAuthRequestFromArgs());
        }
        //generate yaml
        for (AuthRequest req : reqs) {
            generateYaml(req, System.out);
        }
    }

    private List<AuthRequest> readRequests(final String argInput) throws IOException {
        List<AuthRequest> reqs = new ArrayList<>();
        final Reader input;
        if (argInput.equals("-")) {
            input = new InputStreamReader(System.in);
        } else {
            input = new FileReader(new File(argInput));
        }
        try (BufferedReader reader = new BufferedReader(input)) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Decision for:")) {
                    int i = line.indexOf("authorized: false");
                    if (i <= 0) {
                        verbose("skip line: " + line);
                        continue;
                    }
                    ParsePart res = parsePart("res", line, ", ", false);
                    if (null == res) {
                        verbose("no res< " + line);
                        continue;
                    }
                    Map<String, Object> resourceMap = res.resourceMap;
                    if(resourceMap.containsKey("tags") && resourceMap.get("tags").toString().contains(",")) {
                        //split tags
                        List<String> tags = Arrays.asList(resourceMap.get("tags").toString().split(","));
                        resourceMap.put("tags", tags);
                    }
                    line = line.substring(res.len);

                    res = parsePart("subject", line, " ", true);
                    if (null == res) {
                        verbose("no subject<: " + line);
                        continue;
                    }
                    Map<String, Object> subjMap = res.resourceMap;
                    Subject subject = createSubject(subjMap);
                    if (null == subject) {
                        verbose("parse subject< failed: " + subjMap + ": " + line);
                        continue;
                    }
                    line = line.substring(res.len);


                    res = parseString("action", line);
                    if (null == res) {
                        verbose("no action<: " + line);
                        continue;
                    }
                    String action = res.value;
                    line = line.substring(res.len);

                    res = parseString("env", line);
                    if (null == res) {
                        verbose("no env<: " + line);
                        continue;
                    }
                    String env = res.value;
                    line = line.substring(res.len);
                    if (env.lastIndexOf(":") < 0 || env.lastIndexOf(":") >= env.length()) {
                        verbose("env parse failed: " + line);
                        continue;
                    }

                    AuthRequest request = new AuthRequest();
                    boolean isAppContext = env.equals(
                            EnvironmentalContext.URI_BASE +
                            "application:rundeck"
                    ) || env.equals(
                            //backwards compatibility for old audit logs
                            "http://dtolabs.com/rundeck/auth/env/" +
                            "application:rundeck"
                    );
                    request.environment =
                            isAppContext ?
                            createAppEnv() :
                            createAuthEnvironment(env.substring(env.lastIndexOf(":") + 1));
                    request.actions = new HashSet<>(Arrays.asList(action));
                    request.resourceMap = resourceMap;
                    request.subject = subject;
                    reqs.add(request);
                } else {
                    verbose("did not see start. skip line: " + line);
                }
            }
        }
        return reqs;
    }

    private Map<String, String> toStringMap(final Map<String, Object> resourceMap) {
        Map<String, String> r = new HashMap<>();
        for (String s : resourceMap.keySet()) {
            Object value = resourceMap.get(s);
            if(s.equals("tags") && value instanceof Collection) {
                r.put(s, StringUtils.join((Collection) value, ","));
            }else {
                r.put(s, value.toString());
            }
        }
        return r;
    }

    private Subject createSubject(final Map<String, Object> subjMap) {
        String user;
        if (null == subjMap.get("Username") || !(subjMap.get("Username") instanceof String)) {
            return null;
        }
        if (null == subjMap.get("Group") || !(
                subjMap.get("Group") instanceof Collection
                || subjMap.get("Group") instanceof String
        )) {
            return null;
        }
        Object group = subjMap.get("Group");
        Collection<String> groups;
        if (group instanceof Collection) {
            groups = (Collection<String>) group;
        } else {
            groups = Arrays.asList((String) group);
        }
        return makeSubject(subjMap.get("Username").toString(), groups);
    }

    private class ParsePart {
        int len;
        Map<String, Object> resourceMap;
        String value;
    }

    private ParsePart parsePart(String name, String line, final String delimiter, final boolean allowMultiple) {
        Map<String, Object> resourceMap = new HashMap<>();
        int len = 0;
        String test = line;
        int v = test.indexOf(name + "<");
        if (v < 0 || v > test.length() - (name.length() + 1)) {
            return null;
        }
        String r1 = test.substring(v + name.length() + 1);
        int v2 = r1.indexOf(">");
        if (v2 < 0) {
            return null;
        }
        String restext = r1.substring(0, v2);
        resourceMap = parseMap(restext, delimiter, allowMultiple);
        if (null == resourceMap) {
            return null;
        }
        len = v + (name.length()) + 1 + v2 + 1;
        ParsePart parsePart = new ParsePart();
        parsePart.len = len;
        parsePart.resourceMap = resourceMap;
        return parsePart;
    }

    private ParsePart parseString(String name, String line) {
        Map<String, String> resourceMap = new HashMap<>();
        int len = 0;
        String test = line;
        int v = test.indexOf(name + "<");
        if (v < 0 || v > test.length() - (name.length() + 1)) {
            return null;
        }
        String r1 = test.substring(v + name.length() + 1);
        int v2 = r1.indexOf(">");
        if (v2 < 0) {
            return null;
        }
        String restext = r1.substring(0, v2);

        len = v + (name.length() + 1) + v2 + 1;
        ParsePart parsePart = new ParsePart();
        parsePart.value = restext;
        parsePart.len = len;
        return parsePart;
    }

    private Map<String, Object> parseMap(final String restext, final String delimiter, final boolean allowMultiple) {
        String[] split = restext.split(Pattern.quote(delimiter));
        if (split.length < 1) {
            return null;
        }
        HashMap<String, Object> result = new HashMap<>();
        for (final String aSplit : split) {
            String[] s = aSplit.split(":", 2);
            if (s.length < 2) {
                return null;
            }
            if (result.containsKey(s[0]) && allowMultiple) {
                if (result.get(s[0]) instanceof Collection) {
                    ((Collection<String>) result.get(s[0])).add(s[1]);
                } else if (result.get(s[0]) instanceof String) {
                    ArrayList<String> strings = new ArrayList<>();
                    strings.add((String) result.get(s[0]));
                    strings.add(s[1]);
                    result.put(s[0], strings);
                }
            } else {
                result.put(s[0], s[1]);
            }
        }
        return result;
    }

    private void generateYaml(final AuthRequest authRequest, final PrintStream out) {
        Map<String, ?> data = toDataMap(authRequest);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions);
        out.println("# create or append this to a .aclpolicy file");
        out.println("---");
        yaml.dump(data, new OutputStreamWriter(out));
    }

    /**
     * Create the map structure corresponding to yaml
     *
     * @param authRequest request
     *
     * @return data map
     */
    public static Map<String, ?> toDataMap(final AuthRequest authRequest) {
        HashMap<String, Object> stringHashMap = new HashMap<>();
        //context
        Map<String, Object> ruleMap = new HashMap<>();
        if (authRequest.environment.equals(createAppEnv())) {
            //app context
            HashMap<String, String> s = new HashMap<>();
            s.put("application", "rundeck");
            stringHashMap.put(CONTEXT_LONG_OPT, s);
        } else {
            String project = authRequest.environment.iterator().next().value;
            HashMap<String, String> s = new HashMap<>();
            s.put("project", project);
            stringHashMap.put(CONTEXT_LONG_OPT, s);
        }

        //subject
        Set<Username> principals = authRequest.subject.getPrincipals(Username.class);
        if (principals.iterator().next().getName().equals("user")) {
            //use groups
            HashMap<String, Object> s = new HashMap<>();
            ArrayList<String> strings = new ArrayList<>();
            for (Group group : authRequest.subject.getPrincipals(Group.class)) {
                strings.add(group.getName());
            }
            s.put("group", strings.size() > 1 ? strings : strings.iterator().next());
            stringHashMap.put("by", s);
        } else {
            HashMap<String, String> s = new HashMap<>();
            s.put("username", principals.iterator().next().getName());
            stringHashMap.put("by", s);
        }

        Map<String, Object> resource = new HashMap<>();
        //resource
        String type = authRequest.resourceMap.get("type").toString();
        resource.putAll(authRequest.resourceMap);
        resource.remove("type");

        //project context type
        HashMap<String, Object> s = new HashMap<>();
        ArrayList<Map<String, Object>> maps = new ArrayList<>();
        s.put(type, maps);
        HashMap<String, Object> r = new HashMap<>();
        if (resource.size() > 0) {
            r.put(authRequest.regexMatch ? "match" : authRequest.containsMatch ? "contains" : "equals", resource);
        }
        if (authRequest.actions != null && authRequest.actions.size() > 0) {

            r.put(
                    "allow",
                    authRequest.actions.size() > 1
                    ? new ArrayList<>(authRequest.actions)
                    : authRequest.actions.iterator().next()
            );
        }
        if (authRequest.denyActions != null && authRequest.denyActions.size() > 0) {
            r.put(
                    "deny",
                    authRequest.denyActions.size() > 1
                    ? new ArrayList<>(authRequest.denyActions)
                    : authRequest.denyActions.iterator().next()
            );
        }
        maps.add(r);
        ruleMap.putAll(s);

        stringHashMap.put("for", ruleMap);
        stringHashMap.put("description", authRequest.description != null ? authRequest.description : "generated");


        return stringHashMap;
    }

    private void testAction() throws CLIToolOptionsException, IOException, PoliciesParseException {
        if (applyArgValidate()) {
            return;
        }
        final RuleEvaluator authorization = createAuthorization();
        AuthRequest authRequest = createAuthRequestFromArgs();
        HashSet<Map<String, String>> resource = resources(authRequest.resourceMap);

        boolean expectAuthorized=true;
        boolean expectDenied=false;
        Set<Decision> testResult=null;
        if (null != authRequest.actions && authRequest.actions.size() > 0) {
            testResult =
                    authorization.evaluate(
                            resource,
                            authRequest.subject,
                            authRequest.actions,
                            authRequest.environment
                    );
        }else if (null != authRequest.denyActions && authRequest.denyActions.size() > 0) {
            expectAuthorized=false;
            expectDenied=true;
            testResult = authorization.evaluate(
                    resource,
                    authRequest.subject,
                    authRequest.denyActions,
                    authRequest.environment
            );
        }else {
            error(optionDisplayString(ALLOW_OPT) + " or " + optionDisplayString(DENY_OPT) + " is required");
            exit(2);
        }
        boolean testPassed = true;
        boolean wasAllowed = true;
        boolean wasDenied = false;

        for (Decision decision : testResult) {
            if(!decision.isAuthorized()){
                wasAllowed=false;
            }
            if (expectAuthorized && !decision.isAuthorized() ||
                    expectDenied && decision.isAuthorized() ) {
                log("Result: " + decision.explain().getCode());
                verbose(decision.toString());
                switch (decision.explain().getCode()) {
                    case REJECTED_NO_SUBJECT_OR_ENV_FOUND:
                        log(
                                "Meaning: " +
                                "No rules were found among the aclpolicies that match" +
                                " the subject (user,group) and context (" +
                                (authRequest.isAppContext() ? "application" : "project") +
                                ")" +
                                " and resource (" + authRequest.resourceMap + ")"
                        );
                        break;
                    case REJECTED_DENIED:
                        log(
                                "Meaning: " +
                                "A matching rule declared that the requested action be DENIED."
                        );
                        wasDenied = true;
                }
                testPassed = false;
            } else {
                switch (decision.explain().getCode()) {
                    case REJECTED_DENIED:
                        wasDenied = true;
                }
                if (argVerbose) {
                    log(decision.toString());
                }
            }
        }
        log("The decision was: " + (wasAllowed ? "allowed" : wasDenied ? "denied" : "not allowed"));
        if (argVerbose && !testPassed ) {
            log("Policies to allow the requested actions:");
            generateYaml(authRequest, System.out);
        } else if (argVerbose && !testPassed && expectAuthorized && wasDenied) {
            log(
                    "No new policy can allow the requested action.\n" +
                    "DENY rules will always prevent access, even if ALLOW " +
                    "rules also match. \n" +
                    "To allow it, you must remove the DENY rule."
            );
        }
        log("The test " + (testPassed ? "passed" : "failed"));
        if (!testPassed) {
            exit(2);
        }
    }

    private RuleEvaluator createAuthorization()
            throws IOException, PoliciesParseException, CLIToolOptionsException
    {
        return RuleEvaluator.createRuleEvaluator(createPolicies());
    }
    private Policies createPolicies()
            throws IOException, PoliciesParseException, CLIToolOptionsException
    {
        final Policies policies;
        if (null != argFile) {
            if(!argFile.isFile()) {
                throw new CLIToolOptionsException("File: " + argFile + ", does not exist or is not a file");
            }
            policies = Policies.loadFile(argFile);
        } else if (null != argDir) {
            if(!argDir.isDirectory()) {
                throw new CLIToolOptionsException("File: " + argDir + ", does not exist or is not a directory");
            }
            policies = Policies.load(argDir);
        } else if (null != configDir) {
            log("Using configured Rundeck etc dir: " + configDir);
            File directory = new File(configDir);
            if(!directory.isDirectory()) {
                throw new CLIToolOptionsException("File: " + directory + ", does not exist or is not a directory");
            }
            policies = Policies.load(directory);
        } else {
            throw new CLIToolOptionsException("-f or -d are required");
        }
        return policies;
    }

    private class AuthRequest {
        String description;
        Map<String, Object> resourceMap;
        boolean regexMatch;
        boolean containsMatch;
        Subject subject;
        Set<String> actions;
        Set<Attribute> environment;

        boolean isAppContext() {
            return environment.equals(createAppEnv());
        }

        Set<String> denyActions;
    }

    @Override
    public String getHelpString() {
        return "rd-acl <command> [options...]: test [options]\n"
               + "\tTest action:\n"
               + "rd-acl test [options] : Test existing aclpolicy files\n"
               + "rd-acl test --dir <path> [options] : Test all aclpolicy files in specific dir\n"
               + "rd-acl test --file <file> [options] : Test specific aclpolicy file\n"
               + "rd-acl test -v [options] : Verbose output, including policy definitions to resolve failing tests\n"
               + "\tCreate action:\n"
               + "rd-acl create [options] : Generate aclpolicy definition based on input options\n"
               + "rd-acl create -i <audit.log> : Generate aclpolicy definitions to resolve rejected access requests\n"
               + "rd-acl create -i - : Read audit log entries from stdin\n"

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
        if (argVerbose && null != clilogger) {
            clilogger.verbose(message);
        }
    }

    public void debug(final String message) {
        if (null != clilogger) {
            clilogger.debug(message);
        }
    }

    class ACLConstants {
        public static final String ACTION_CREATE = "create";
        public static final String ACTION_READ = "read";
        public static final String ACTION_VIEW = "view";
        public static final String ACTION_UPDATE = "update";
        public static final String ACTION_DELETE = "delete";
        public static final String ACTION_RUN = "run";
        public static final String ACTION_KILL = "kill";
        public static final String ACTION_ADMIN = "admin";
        public static final String ACTION_GENERATE_USER_TOKEN = "generate_user_token";
        public static final String ACTION_GENERATE_SERVICE_TOKEN = "generate_service_token";
        public static final String ACTION_REFRESH = "refresh";
        public static final String ACTION_RUNAS = "runAs";
        public static final String ACTION_KILLAS = "killAs";
        public static final String ACTION_CONFIGURE = "configure";
        public static final String ACTION_IMPORT = "import";
        public static final String ACTION_EXPORT = "export";
        public static final String ACTION_INSTALL = "install";
        public static final String ACTION_UNINSTALL = "uninstall";
        public static final String ACTION_DELETE_EXECUTION = "delete_execution";
        public static final String ACTION_ENABLE_EXECUTIONS = "enable_executions";
        public static final String ACTION_DISABLE_EXECUTIONS = "disable_executions";
        public static final String ACTION_TOGGLE_SCHEDULE = "toggle_schedule";
        public static final String ACTION_TOGGLE_EXECUTION = "toggle_execution";
        public static final String ACTION_SCM_UPDATE="scm_update";
        public static final String ACTION_SCM_CREATE="scm_create";
        public static final String ACTION_SCM_DELETE="scm_delete";
        public static final String ACTION_SCM_IMPORT = "scm_import";
        public static final String ACTION_SCM_EXPORT = "scm_export";

        public static final String TYPE_SYSTEM = "system";
        public static final String TYPE_SYSTEM_ACL = "system_acl";
        public static final String TYPE_NODE = "node";
        public static final String TYPE_JOB = "job";
        public static final String TYPE_APITOKEN = "apitoken";
        public static final String TYPE_ADHOC = "adhoc";
        public static final String TYPE_PROJECT = "project";
        public static final String TYPE_PROJECT_ACL = "project_acl";
        public static final String TYPE_PLUGIN = "plugin";
        public static final String TYPE_EVENT = "event";
        public static final String TYPE_USER = "user";
        public static final String TYPE_STORAGE = "storage";

        private Map<String, String> resType(String type) {
            return Collections.unmodifiableMap(AuthorizationUtil.resourceType(type));
        }

        public final Map<String, String> RESOURCE_TYPE_SYSTEM = resType(TYPE_SYSTEM);
        public final Map<String, String> RESOURCE_TYPE_NODE = resType(TYPE_NODE);
        public final Map<String, String> RESOURCE_TYPE_JOB = resType(TYPE_JOB);
        public final Map<String, String> RESOURCE_TYPE_EVENT = resType(TYPE_EVENT);
        public final Map<String, String> RESOURCE_ADHOC = Collections.unmodifiableMap(
                AuthorizationUtil
                        .resource(TYPE_ADHOC)
        );
    }

    class OptionsPrompt extends CLIToolOptionsException {
        private String prompt;

        public OptionsPrompt(final String msg, final String prompt) {
            super(msg);
            this.prompt = prompt;
        }

        public String getPrompt() {
            return prompt;
        }
    }
}
