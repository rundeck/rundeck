package com.dtolabs.rundeck.core.cli.acl;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;
import com.dtolabs.rundeck.core.authorization.Decision;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.authorization.providers.Policies;
import com.dtolabs.rundeck.core.authorization.providers.PoliciesParseException;
import com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization;
import com.dtolabs.rundeck.core.cli.*;
import com.dtolabs.rundeck.core.cli.jobs.JobsToolException;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkFactory;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
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
    public static final String JOB_OPT = "j";
    public static final String JOB_LONG_OPT = "job";
    public static final String CONTEXT_OPT = "c";
    public static final String CONTEXT_LONG_OPT = "context";
    public static final String ADHOC_OPT = "A";
    public static final String ADHOC_LONG_OPT = "adhoc";
    public static final String NODE_OPT = "n";
    public static final String NODE_LONG_OPT = "node";
    public static final String DENY_OPT = "D";
    public static final String DENY_LONG_OPT = "deny";
    public static final String VERBOSE_OPT = "v";
    public static final String VERBOSE_LONG_OPT = "verbose";
    public static final String STORAGE_OPT = "s";
    public static final String STORAGE_LONG_OPT = "storage";
    public static final String RESOURCE_OPT = "R";
    public static final String RESOURCE_LONG_OPT = "resource";
    public static final String INPUT_OPT = "i";
    public static final String INPUT_OPT_LONG = "input";

    final CLIToolLogger clilogger;

    private Actions action = null;

    public AclTool(final CLIToolLogger cliToolLogger)
            throws IOException, PoliciesParseException
    {
        this(cliToolLogger, Constants.getSystemBaseDir());
    }

    public AclTool(final CLIToolLogger cliToolLogger, final String rdeckBase)
            throws IOException, PoliciesParseException
    {
        this(
                cliToolLogger,
                rdeckBase,
                FrameworkFactory.createFilesystemFramework(new File(rdeckBase)).getPropertyLookup()
        );
    }

    private String configDir;

    public AclTool(final CLIToolLogger cliToolLogger, final String rdeckBase, final IPropertyLookup frameworkProps)
            throws IOException, PoliciesParseException
    {

        if (null == cliToolLogger) {
            clilogger = new Log4JCLIToolLogger(log4j);
        } else {
            this.clilogger = cliToolLogger;
        }
        configDir = frameworkProps.hasProperty("framework.etc.dir") ? frameworkProps.getProperty(
                "framework.etc.dir"
        ) : Constants.getFrameworkConfigDir(rdeckBase);


        final TestOptions testOptions = new TestOptions();
        addToolOptions(testOptions);

    }

    private SAREAuthorization createAuthorization(final File directory) throws IOException, PoliciesParseException {
        return new SAREAuthorization(directory);
    }

    private SAREAuthorization createAuthorizationSingleFile(final File file)
            throws IOException, PoliciesParseException
    {
        return new SAREAuthorization(Policies.loadFile(file));
    }

    /**
     * Creates an instance and executes {@link #run(String[])}.
     *
     * @param args command line arg vector
     *
     * @throws Exception action error
     */
    public static void main(final String[] args) throws Exception {
        PropertyConfigurator.configure(Constants.getLog4jPropertiesFile().getAbsolutePath());
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
            tool.help();
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

    static enum Actions {

        /**
         * List action
         */
        test(ACTION_TEST),
        create(ACTION_CREATE);
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
    private String argProjectJob;
    private String argProjectNode;
    private boolean argProjectAdhoc;

    private String argAppStorage;
    private String argResource;
    private String argInput;

    private class TestOptions implements CLIToolOptions {
        @Override
        public void addOptions(final Options options) {
            options.addOption(VERBOSE_OPT, VERBOSE_LONG_OPT, false, "Verbose output.");

            options.addOption(FILE_OPTION, FILE_OPTION_LONG, true, "File path. Load the specified aclpolicy file.");
            options.addOption(
                    DIR_OPTION,
                    DIR_OPTION_LONG,
                    true,
                    "Directory. Load all policy files in the specified directory."
            );
            options.addOption(
                    ALLOW_OPT,
                    ALLOW_LONG_OPT,
                    true,
                    "Allow actions. Comma-separated list of actions to validate (test command) or allow (create " +
                    "command)."
            );
            options.addOption(
                    DENY_OPT,
                    DENY_LONG_OPT,
                    true,
                    "Deny the specified actions. (create command)"
            );
            options.addOption(
                    GROUPS_OPT,
                    GROUPS_LONG_OPT,
                    true,
                    "Subject Groups names. Comma-separated list of user groups to validate (test command) or for by: clause (create command)."
            );
            options.addOption(
                    USER_OPT,
                    USER_LONG_OPT,
                    true,
                    "Subject User name. Comma-separated list of user names to validate (test command) or for by: clause (create command)."
            );
            options.addOption(
                    PROJECT_OPT,
                    PROJECT_LONG_OPT,
                    true,
                    "Name of project, used in project context or for application resource."
            );
            options.addOption(
                    JOB_OPT,
                    JOB_LONG_OPT,
                    true,
                    "Job name/group. (project context)"
            );
            options.addOption(
                    CONTEXT_OPT,
                    CONTEXT_LONG_OPT,
                    true,
                    "Context: either 'project' or 'application'."
            );
            options.addOption(
                    ADHOC_OPT,
                    ADHOC_LONG_OPT,
                    false,
                    "Adhoc execution (project context)"
            );
            options.addOption(
                    NODE_OPT,
                    NODE_LONG_OPT,
                    true,
                    "Node name. (project context)"
            );
            options.addOption(
                    STORAGE_OPT,
                    STORAGE_LONG_OPT,
                    true,
                    "Storage path/name. (application context)"
            );
            options.addOption(
                    RESOURCE_OPT,
                    RESOURCE_LONG_OPT,
                    true,
                    "Resource type name."
            );
            options.addOption(
                    INPUT_OPT,
                    INPUT_OPT_LONG,
                    true,
                    "Read file for audit log data."
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
            if (cli.hasOption(JOB_OPT)) {
                argProjectJob = cli.getOptionValue(JOB_OPT);
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
            if (cli.hasOption(NODE_OPT)) {
                argProjectNode = cli.getOptionValue(NODE_OPT);
            }
            if (cli.hasOption(STORAGE_OPT)) {
                argAppStorage = cli.getOptionValue(STORAGE_OPT);
            }
            if (cli.hasOption(RESOURCE_OPT)) {
                argResource = cli.getOptionValue(RESOURCE_OPT);
            }
            if (cli.hasOption(INPUT_OPT)) {
                argInput = cli.getOptionValue(INPUT_OPT);
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
        return line;
    }


    /**
     * Call the action
     *
     * @throws com.dtolabs.rundeck.core.cli.jobs.JobsToolException if an error occurs
     */
    protected void go() throws JobsToolException, CLIToolOptionsException {
        if(null==action) {
            throw new CLIToolOptionsException("Command expected. Choose one of: " + Arrays.asList(Actions.values()));
        }
        try {
            switch (action) {
                case test:
                    testAction();
                    break;
                case create:
                    createAction();
                    break;
                default:
                    throw new CLIToolOptionsException("Unrecognized action: " + action);
            }
        } catch (IOException | PoliciesParseException e) {
            throw new JobsToolException(e);
        }
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
                    ACLConstants.TYPE_STORAGE
            )
    );
    static final Set<String> appKinds = new HashSet<>(
            Arrays.asList(
                    ACLConstants.TYPE_PROJECT,
                    ACLConstants.TYPE_SYSTEM,
                    ACLConstants.TYPE_USER
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
                    ACLConstants.ACTION_DELETE_EXECUTION
            );
    static final List<String> appStorageActions =
            Arrays.asList(
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_UPDATE,
                    ACLConstants.ACTION_DELETE
            );
    static final List<String> appProjectKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_CREATE
            );
    static final List<String> appSystemKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ
            );
    static final List<String> appUserKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_ADMIN
            );
    static final List<String> projectJobActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
                    ACLConstants.ACTION_UPDATE,
                    ACLConstants.ACTION_DELETE,
                    ACLConstants.ACTION_RUN,
                    ACLConstants.ACTION_RUNAS,
                    ACLConstants.ACTION_KILL,
                    ACLConstants.ACTION_KILLAS,
                    ACLConstants.ACTION_CREATE
            );
    static final List<String> projectJobKindActions =
            Arrays.asList(
                    ACLConstants.ACTION_CREATE,
                    ACLConstants.ACTION_DELETE
            );
    static final List<String> projectAdhocActions =
            Arrays.asList(
                    ACLConstants.ACTION_READ,
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

    private AuthRequest createAuthRequestFromArgs(boolean prompt)
            throws CLIToolOptionsException, IOException, PoliciesParseException
    {
        //determine context
        if (null == argContext) {
            throw new OptionsPrompt(
                    "-c/--context is required.",
                    "Choose one of: \n" +
                    "  -c " + Context.application + "\n" +
                    "    Access to projects, users, storage, system info.\n" +
                    "  -c " + Context.project + "\n" +
                    "    Access to jobs, nodes, events, within a project."
            );
        }
        if (argContext == Context.project && null == argProject) {
            throw new OptionsPrompt(
                    "-p/--project is required.",
                    "Choose the name of a project, or .*: \n" +
                    "  -p myproject\n" +
                    "  -p '.*'"
            );
        }
        boolean appContext = argContext == Context.application;
        Set<Attribute> environment = appContext ? Framework.RUNDECK_APP_ENV : FrameworkProject.authorizationEnvironment(
                argProject
        );

        //determine subject

        Subject subject;
        if (argGroups != null || argUser != null) {
            Subject t = makeSubject(argUser, groupsList);
            subject = t;
        } else {
            throw new OptionsPrompt(
                    "-g <groups> or -u <user> are required",
                    "  -u user1,user2... \n" +
                    "  -g group1,group2... \n" +
                    "    Groups control access for a set of users, and correspond\n" +
                    "    to authorization roles."
            );
        }

        //determine resource
        Map<String, String> resourceMap = new HashMap<>();

        if (argContext == Context.application && argProject != null) {
            HashMap<String, String> res = new HashMap<>();
            res.put("name", argProject);
            resourceMap = AuthorizationUtil.resource(ACLConstants.TYPE_PROJECT, res);
        } else if (argContext == Context.application && argAppStorage != null) {
            HashMap<String, String> res = new HashMap<>();
            int nx = argAppStorage.lastIndexOf("/");
            if (nx >= 0) {
                res.put("path", argAppStorage.substring(0, nx));
                res.put("name", argAppStorage.substring(nx + 1));
            } else {
                res.put("name", argAppStorage);
            }
            resourceMap = AuthorizationUtil.resource(ACLConstants.TYPE_STORAGE, res);
        } else if (argContext == Context.project && argProjectJob != null) {
            HashMap<String, String> res = new HashMap<>();
            int nx = argProjectJob.lastIndexOf("/");
            if (nx >= 0) {
                res.put("group", argProjectJob.substring(0, nx));
                res.put("name", argProjectJob.substring(nx + 1));
            } else {
                res.put("group", "");
                res.put("name", argProjectJob);
            }
            resourceMap = AuthorizationUtil.resource(ACLConstants.TYPE_JOB, res);
        } else if (argContext == Context.project && argProjectNode != null) {
            HashMap<String, String> res = new HashMap<>();
            res.put("nodename", argProjectNode);
            resourceMap = AuthorizationUtil.resource(ACLConstants.TYPE_NODE, res);
        } else if (argContext == Context.project && argProjectAdhoc) {
            resourceMap = AuthorizationUtil.resource(ACLConstants.TYPE_ADHOC, new HashMap<String, String>());
        } else if (argContext == Context.project && null != argResource) {
            if (!projectKinds.contains(argResource.toLowerCase())) {
                throw new OptionsPrompt(
                        "-R/--resource invalid resource type: " + argResource,
                        "  resource types in this context: " +
                        "    " + StringUtils.join(projectKinds, "\n    ")

                );
            }
            resourceMap = AuthorizationUtil.resourceType(argResource.toLowerCase());
        } else if (argContext == Context.application && null != argResource) {
            if (!appKinds.contains(argResource.toLowerCase())) {

                throw new OptionsPrompt(
                        "-R/--resource invalid resource type: " + argResource,
                        "  resource types in this context: " +
                        "    " + StringUtils.join(appKinds, "\n    ")

                );
            }
            resourceMap = AuthorizationUtil.resourceType(argResource.toLowerCase());
        } else if (argContext == Context.project) {

            throw new OptionsPrompt(
                    "Project-context resource option is required.",
                    "Possible options:\n" +
                    "  Job: -j/--job <jobgroup/name>\n" +
                    "    View, modify, create*, delete*, run, and kill specific jobs.\n" +
                    "    * Create and delete also require -R/--resource level access.\n" +
                    "  Adhoc: -A/--adhoc\n" +
                    "    View, run, and kill adhoc commands.\n" +
                    "  Node: -n/--node <nodename>\n" +
                    "    View and run on specific nodes.\n" +
                    "  Resource: -R/--resource <type>\n" +
                    "    Create and delete jobs.\n" +
                    "    View and manage nodes.\n" +
                    "    View events.\n" +
                    "  resource types in this context: \n" +
                    "    " + StringUtils.join(projectKinds, "\n    ")

            );
        } else {

            throw new OptionsPrompt(
                    "Application-context resource option is required.",
                    "Possible options:\n" +
                    "  Project: -p/--project <projectname>\n" +
                    "    Visibility, import, export, config, and delete executions.\n" +
                    "    *Note: Project create requires -R/--resource access*\n" +
                    "  Storage: -s/--storage <path/name>\n" +
                    "    CRUD access for the key storage system.\n" +
                    "  Resource: -R/--resource <type>\n" +
                    "    Create projects, read system info, manage users.\n" +
                    "  resource types in this context: \n" +
                    "    " + StringUtils.join(appKinds, "\n    ")
            );
        }

        List<String> possibleActions = new ArrayList<>(Arrays.asList("admin", "*"));
        if (argContext == Context.application && argAppStorage != null) {
            //actions for job
            possibleActions.addAll(appStorageActions);
        } else if (argContext == Context.application && argProject != null) {
            //actions for job
            possibleActions.addAll(appProjectActions);
        } else if (argContext == Context.application &&
                   argResource != null &&
                   argResource.equalsIgnoreCase(ACLConstants.TYPE_PROJECT)) {
            //actions for job
            possibleActions.addAll(appProjectKindActions);
        } else if (argContext == Context.application &&
                   argResource != null &&
                   argResource.equalsIgnoreCase(ACLConstants.TYPE_SYSTEM)) {
            //actions for job
            possibleActions.addAll(appSystemKindActions);
        } else if (argContext == Context.application &&
                   argResource != null &&
                   argResource.equalsIgnoreCase(ACLConstants.TYPE_USER)) {
            //actions for job
            possibleActions.addAll(appUserKindActions);
        } else if (argContext == Context.project &&
                   argResource != null &&
                   argResource.equalsIgnoreCase(ACLConstants.TYPE_JOB)) {
            //actions for job
            possibleActions.addAll(projectJobKindActions);
        } else if (argContext == Context.project &&
                   argResource != null &&
                   argResource.equalsIgnoreCase(ACLConstants.TYPE_EVENT)) {
            //actions for job
            possibleActions.addAll(projectEventKindActions);
        } else if (argContext == Context.project &&
                   argResource != null &&
                   argResource.equalsIgnoreCase(ACLConstants.TYPE_NODE)) {
            //actions for job
            possibleActions.addAll(projectNodeKindActions);
        } else if (argContext == Context.project && argProjectJob != null) {
            //actions for job
            possibleActions.addAll(projectJobActions);
        } else if (argContext == Context.project && argProjectAdhoc) {
            //actions for job
            possibleActions.addAll(projectAdhocActions);
        } else if (argContext == Context.project && argProjectNode != null) {
            //actions for job
            possibleActions.addAll(projectNodeActions);
        }
        if (null == argAllowAction && null == argDenyAction) {
            //listing actions
            throw new OptionsPrompt(
                    "-a/--allow or -D/--deny is required.",
                    "  -a action1,action2,...\n" +
                    "  -D action1,action2,...\n" +
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
                        "-a/--allow specified invalid actions.",
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
                        "-D/--deny specified invalid actions.",
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
        return request;
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

    private void createAction() throws CLIToolOptionsException, IOException, PoliciesParseException {
        List<AuthRequest> reqs = new ArrayList<>();
        if (null != argInput) {
            reqs = readRequests(argInput);
        } else {
            reqs.add(createAuthRequestFromArgs(true));
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
                    line = line.substring(res.len);

                    res = parsePart("subject", line, " ", true);
                    if (null == res) {
                        verbose("no subject<: " + line);
                        continue;
                    }
                    Map<String, Object> subjMap = res.resourceMap;
                    Subject subject = createSubject(subjMap);
                    if (null == subject) {
                        verbose("parse subject< failed: "+subjMap+": " + line);
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
                    request.environment =
                            env.equals(
                                    EnvironmentalContext.URI_BASE +
                                    "application:rundeck"
                            ) ?
                            Framework.RUNDECK_APP_ENV :
                            FrameworkProject.authorizationEnvironment(env.substring(env.lastIndexOf(":") + 1));
                    request.actions = new HashSet<>(Arrays.asList(action));
                    request.resourceMap = toStringMap(resourceMap);
                    request.subject = subject;
                    reqs.add(request);
                }else{
                    verbose("did not see start. skip line: " + line);
                }
            }
        }
        return reqs;
    }

    private Map<String, String> toStringMap(final Map<String, Object> resourceMap) {
        Map<String, String> r = new HashMap<>();
        for (String s : resourceMap.keySet()) {
            r.put(s, resourceMap.get(s).toString());
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
        if(group instanceof Collection) {
            groups = (Collection<String>) group;
        }else {
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
        String r1 = test.substring(v + name.length() +1);
        int v2 = r1.indexOf(">");
        if (v2 < 0) {
            return null;
        }
        String restext = r1.substring(0, v2);
        resourceMap = parseMap(restext, delimiter, allowMultiple);
        if (null == resourceMap) {
            return null;
        }
        len = v + (name.length())+1 + v2 + 1;
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
        String r1 = test.substring(v + name.length()+1);
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
        if (authRequest.environment.equals(Framework.RUNDECK_APP_ENV)) {
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

        Map<String, String> resource = new HashMap<>();
        //resource
        String type = authRequest.resourceMap.get("type");
        resource.putAll(authRequest.resourceMap);
        resource.remove("type");

        //project context type
        HashMap<String, Object> s = new HashMap<>();
        ArrayList<Map<String, Object>> maps = new ArrayList<>();
        s.put(type, maps);
        HashMap<String, Object> r = new HashMap<>();
        r.put("equals", resource);
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
        final SAREAuthorization authorization = createAuthorization();
        AuthRequest authRequest = createAuthRequestFromArgs(true);
        HashSet<Map<String, String>> resource = new HashSet<>();
        resource.add(authRequest.resourceMap);

        Set<Decision> result = authorization.evaluate(
                resource,
                authRequest.subject,
                authRequest.actions,
                authRequest.environment
        );
        boolean allowed = true;
        boolean denied = false;
        List<Decision> failed = new ArrayList<>();
        for (Decision decision : result) {
            if (!decision.isAuthorized()) {
                log("Result: "+decision.explain().getCode());
                verbose(decision.toString());
                switch (decision.explain().getCode()){
                    case REJECTED_NO_SUBJECT_OR_ENV_FOUND:
                        log(
                                "Meaning: " +
                                "No rules were found among the aclpolicies that match" +
                                " the subject (user,group) and context (" +
                                (authRequest.isAppContext() ? "application" : "project") +
                                ")"
                        );
                        break;
                    case REJECTED_DENIED:
                        log("Meaning: " +
                            "A matching rule declared that the requested action be DENIED.");
                        denied=true;
                }
                allowed = false;
                failed.add(decision);
            } else {
                if (argVerbose) {
                    log(decision.toString());
                }
            }
        }
        log("The result was: " + (allowed ? "allowed" : denied ? "denied" : "not allowed"));
        if (argVerbose && !allowed&&!denied) {
            log("Policies to allow the requested actions:");
            generateYaml(authRequest, System.out);
        } else if (argVerbose && denied) {
            log(
                    "No new policy can allow the requested action.\n" +
                    "DENY rules will always prevent access, even if ALLOW " +
                    "rules also match. \n" +
                    "To allow it, you must remove the DENY rule."
            );
        }
        if(!allowed){
            exit(2);
        }
    }

    private SAREAuthorization createAuthorization()
            throws IOException, PoliciesParseException, CLIToolOptionsException
    {
        final SAREAuthorization authorization;
        if (null != argFile) {
            authorization = createAuthorizationSingleFile(argFile);
        } else if (null != argDir) {
            authorization = createAuthorization(argDir);
        } else if (null != configDir) {
            log("Using configured Rundeck etc dir: " + configDir);
            authorization = createAuthorization(new File(configDir));
        } else {
            throw new CLIToolOptionsException("-f or -d are required");
        }
        return authorization;
    }

    private class AuthRequest {
        String description;
        Map<String, String> resourceMap;
        Subject subject;
        Set<String> actions;
        Set<Attribute> environment;
        boolean isAppContext(){
            return environment.equals(Framework.RUNDECK_APP_ENV);
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
        public static final String ACTION_UPDATE = "update";
        public static final String ACTION_DELETE = "delete";
        public static final String ACTION_RUN = "run";
        public static final String ACTION_KILL = "kill";
        public static final String ACTION_ADMIN = "admin";
        public static final String ACTION_REFRESH = "refresh";
        public static final String ACTION_RUNAS = "runAs";
        public static final String ACTION_KILLAS = "killAs";
        public static final String ACTION_CONFIGURE = "configure";
        public static final String ACTION_IMPORT = "import";
        public static final String ACTION_EXPORT = "export";
        public static final String ACTION_DELETE_EXECUTION = "delete_execution";

        public static final String TYPE_SYSTEM = "system";
        public static final String TYPE_NODE = "node";
        public static final String TYPE_JOB = JOB_LONG_OPT;
        public static final String TYPE_ADHOC = "adhoc";
        public static final String TYPE_PROJECT = "project";
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
