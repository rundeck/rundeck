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

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.authentication.AuthenticationMgrFactory;
import com.dtolabs.rundeck.core.authentication.Authenticator;
import com.dtolabs.rundeck.core.authorization.*;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherException;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcherMgrFactory;
import com.dtolabs.rundeck.core.dispatcher.NoCentralDispatcher;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.ExecutionServiceFactory;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreter;
import com.dtolabs.rundeck.core.execution.commands.CommandInterpreterService;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.plugins.PluginManagerService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.resources.FileResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGenerator;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.input.InputHandler;

import java.io.File;
import java.net.URI;
import java.util.*;


/**
 * Manages the elements of the Ctl framework. Provides access to the various
 * kinds of framework resource managers like
 * {@link FrameworkProjectMgr}, {@link ModuleMgr}, {@link Authenticator}, {@link Authorization}.
 * <p/>
 * User: alexh
 * Date: Jun 4, 2004
 * Time: 8:16:42 PM
 */
public class Framework extends FrameworkResourceParent {
    public static final Logger logger = Logger.getLogger(Framework.class);

    static final String NODEAUTH_CLS_PROP = "framework.nodeauthentication.classname";
    static final String AUTHENT_CLS_PROP = "framework.authentication.class";
    static final String AUTHORIZE_CLS_PROP = "framework.authorization.class";
    public static final String CENTRALDISPATCHER_CLS_PROP = "framework.centraldispatcher.classname";
    public static final String NODES_RESOURCES_FILE_PROP = "framework.nodes.file.name";
    public static final String NODES_FILE_AUTOGEN_PROP = "framework.nodes.file.autogenerate";
    static final String CENTRALDISPATCHER_CLS_DEFAULT = NoCentralDispatcher.class.getName();

    static final String PROJECTMGR_NAME = "projectResourceMgr";
    public static final String FRAMEWORK_LIBEXT_DIR = "framework.libext.dir";
    public static final String FRAMEWORK_LIBEXT_CACHE_DIR = "framework.libext.cache.dir";
    public static final String DEFAULT_LIBEXT_DIR_NAME = "libext";
    public static final String DEFAULT_LIBEXT_CACHE_DIR_NAME = "cache";
    public static final String SYSTEM_PROP_LIBEXT = "rdeck.libext";
    public static final String SYSTEM_PROP_LIBEXT_CACHE = "rdeck.libext.cache";
    public static final String FRAMEWORK_PLUGINS_ENABLED = "framework.plugins.enabled";

    private final IPropertyLookup lookup;
    private final File projectsBase;
    private final File homeDir;
    private FrameworkProjectMgr projectResourceMgr;
    private boolean allowUserInput = true;
    private static final String FRAMEWORK_USERINPUT_DISABLED = "framework.userinput.disabled";

    final HashMap<String,FrameworkSupportService> services = new HashMap<String, FrameworkSupportService>();
    /**
     * This is the root. Does not return a parent.
     *
     * @throws FrameworkResourceException Throws an exception if this is called
     */
    public IFrameworkResourceParent getParent() {
        throw new FrameworkResourceException("Framework has no parent resource.", this);
    }

    public boolean childCouldBeLoaded(String name) {
        return false;
    }

    public IFrameworkResource loadChild(String name) {
        return null;
    }

    @SuppressWarnings("rawtypes")
    public Collection listChildNames() {
        return new ArrayList();
    }

    /**
     * Initialize children, the various resource management objects
     */
    public void initialize() {
        final boolean initialize = true; // managers should call their initialize methods
        projectResourceMgr = FrameworkProjectMgr.create(PROJECTMGR_NAME, projectsBase, this);

        if(null==authenticationMgr) {
            authenticationMgr = AuthenticationMgrFactory.create(lookup.getProperty(AUTHENT_CLS_PROP), this)
                .getAuthenticationMgr();
        }
        if(null==authorizationMgr){
            authorizationMgr = AuthorizationMgrFactory.create(lookup.getProperty(AUTHORIZE_CLS_PROP),
                this, getConfigDir()).getAuthorizationMgr();
        }
        if(null==centralDispatcherMgr){
            try {

                String propValue = CENTRALDISPATCHER_CLS_DEFAULT;
                if(lookup.hasProperty(CENTRALDISPATCHER_CLS_PROP)){
                    propValue=lookup.getProperty(CENTRALDISPATCHER_CLS_PROP);
                }
                centralDispatcherMgr = CentralDispatcherMgrFactory.create(propValue, this).getCentralDispatcher();
            } catch (CentralDispatcherException e) {
                System.err.println("unable to load central dispatcher class: "+e.getMessage());
                throw new CoreException(e);
            }
        }

        //plugin manager service inited first.  any pluggable services will then be
        //able to try to load providers via the plugin manager
        if(!hasProperty(FRAMEWORK_PLUGINS_ENABLED) || "true".equals(getProperty(FRAMEWORK_PLUGINS_ENABLED))){
            //enable plugin service only if framework property does not disable them
            PluginManagerService.getInstanceForFramework(this);
        }
        CommandInterpreterService.getInstanceForFramework(this);
        NodeExecutorService.getInstanceForFramework(this);
        FileCopierService.getInstanceForFramework(this);
        NodeDispatcherService.getInstanceForFramework(this);
        ExecutionServiceFactory.getInstanceForFramework(this);
        WorkflowExecutionService.getInstanceForFramework(this);
        StepExecutionService.getInstanceForFramework(this);
        ResourceModelSourceService.getInstanceForFramework(this);
        ResourceFormatParserService.getInstanceForFramework(this);
        ResourceFormatGeneratorService.getInstanceForFramework(this);
    }

    private CentralDispatcher centralDispatcherMgr;

    /**
     * Return CentralDispatcher implementation.
     * @return in implementation
     */
    public CentralDispatcher getCentralDispatcherMgr() {
        return centralDispatcherMgr;
    }
    /**
     * Reference to class instance to authorizationMgr
     */
    private LegacyAuthorization authorizationMgr;

    public LegacyAuthorization getAuthorizationMgr() {
        return authorizationMgr;
    }

    /**
     * Reference to class instance to authenicate
     */
    private Authenticator authenticationMgr;

    /**
     * Gets Authenticator for this framework instance
     *
     * @return returns instance of Authenticator
     */
    public Authenticator getAuthenticationMgr() {
        return authenticationMgr;
    }

    /**
     * Gets DepotMgr for this framework instance
     * @return returns instance of IFrameworkProjectMgr
     */
    public IFrameworkProjectMgr getFrameworkProjectMgr() {
        return projectResourceMgr;
    }

    /**
     * Standard constructor
     *
     * @param rdeck_base_dir path name to the rdeck_base
     * @param module_base_dir  path name to the module_base
     * @param projects_base_dir  path name to the projects base
     */
    private Framework(final String rdeck_base_dir,
                      final String projects_base_dir) {
        this(rdeck_base_dir, projects_base_dir, null, null);
    }

    /**
     * Standard constructor
     *
     * @param rdeck_base_dir path name to the rdeck_base
     * @param module_base_dir  path name to the module_base
     * @param projects_base_dir  path name to the projects base
     */
    private Framework(final String rdeck_base_dir,
                      final String projects_base_dir,
                      final Authenticator authentication,
                      final LegacyAuthorization authorization) {
        super("framework", new File(null == rdeck_base_dir ? Constants.getSystemBaseDir() : rdeck_base_dir), null);
        if(null==getBaseDir()) {
            throw new NullPointerException(
                "rdeck_base_dir was not set in constructor and system property rdeck.base was not defined");
        }

        final String projectsBaseDir = null == projects_base_dir ? getBaseDir() + Constants.FILE_SEP + "projects"
                                     : projects_base_dir;
        if (null == projectsBaseDir) {
            throw new CoreException("projects base dir could not be determined.");
        }
        logger.debug("creating new Framework instance."
                     + "  rdeck_base_dir=" + getBaseDir()
                     + ", projects_base_dir=" + projectsBaseDir
        );
        if (!getBaseDir().exists())
            throw new IllegalArgumentException("rdeck_base directory does not exist. "
                    + rdeck_base_dir);

        projectsBase = new File(projectsBaseDir);
        if (!projectsBase.exists())
            throw new IllegalArgumentException("project base directory does not exist. "
                    + projectsBaseDir);
        File propertyFile = new File(getConfigDir(), "framework.properties");

        PropertyLookup lookup1 = PropertyLookup.create(propertyFile);
        lookup1.expand();

        lookup = lookup1;
        if (!lookup.hasProperty(AUTHENT_CLS_PROP)) {
            throw new IllegalArgumentException("\"" + AUTHENT_CLS_PROP + "\" property not set");
        }
        if (!lookup.hasProperty(AUTHORIZE_CLS_PROP)) {
            throw new IllegalArgumentException("\"" + AUTHORIZE_CLS_PROP + "\" property not set");
        }
        if (!lookup.hasProperty(CENTRALDISPATCHER_CLS_PROP)) {
            logger.warn("\"" + CENTRALDISPATCHER_CLS_PROP + "\" property not set, using default");
        }

        if(lookup.hasProperty("rdeck.home")) {
            homeDir = new File(lookup.getProperty("rdeck.home"));

        }else {
            final String ctlhome = Constants.getSystemHomeDir();
            if (null==ctlhome) throw new FrameworkResourceException("failed looking up value for rdeck.home ",this);
            homeDir = new File(ctlhome);
        }

        this.authenticationMgr = authentication;
        this.authorizationMgr=authorization;
        long start = System.currentTimeMillis();
        initialize();
        long end = System.currentTimeMillis();
        if(logger.isDebugEnabled()) {
            logger.debug("Framework.initialize() time: " + (end - start) + "ms");
        }
    }
    /**
     * Return a service by name
     */
    public FrameworkSupportService getService(String name) {
        return services.get(name);
    }
    /**
     * Set a service by name
     */
    public void setService(final String name, final FrameworkSupportService service){
        synchronized (services){
            if(null==services.get(name) && null!=service) {
                services.put(name, service);
            }else if(null==service) {
                services.remove(name);
            }
        }
    }
    public ExecutionService getExecutionService() {
        return ExecutionServiceFactory.getInstanceForFramework(this);
    }
    public WorkflowExecutionService getWorkflowExecutionService() {
        return WorkflowExecutionService.getInstanceForFramework(this);
    }
    public StepExecutionService getStepExecutionService() {
        return StepExecutionService.getInstanceForFramework(this);
    }

    public FileCopier getFileCopierForNodeAndProject(INodeEntry node, final String project) throws ExecutionServiceException {
        return getFileCopierService().getProviderForNodeAndProject(node, project);
    }

    public FileCopierService getFileCopierService() {
        return FileCopierService.getInstanceForFramework(this);
    }

    public NodeExecutor getNodeExecutorForNodeAndProject(INodeEntry node, final String project) throws ExecutionServiceException {
        return getNodeExecutorService().getProviderForNodeAndProject(node, project);
    }
    public NodeExecutorService getNodeExecutorService() throws ExecutionServiceException {
        return NodeExecutorService.getInstanceForFramework(this);
    }
    public CommandInterpreter getCommandInterpreterForItem(ExecutionItem item) throws ExecutionServiceException {
        return CommandInterpreterService.getInstanceForFramework(this).getInterpreterForExecutionItem(item);
    }
    public NodeDispatcher getNodeDispatcherForContext(ExecutionContext context) throws ExecutionServiceException {
        return NodeDispatcherService.getInstanceForFramework(this).getNodeDispatcher(context);
    }
    public ResourceModelSourceService getResourceModelSourceService() {
        return ResourceModelSourceService.getInstanceForFramework(this);
    }
    public ResourceFormatParserService getResourceFormatParserService() {
        return ResourceFormatParserService.getInstanceForFramework(this);
    }

    public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return ResourceFormatGeneratorService.getInstanceForFramework(this);
    }

    public ServiceProviderLoader getPluginManager(){
        if(null!=getService(PluginManagerService.SERVICE_NAME)) {
            return PluginManagerService.getInstanceForFramework(this);
        }
        return null;
    }
    /**
     * Returns the singleton instance of Framework object.  If any of the
     * supplied directory paths are null, then the value from {@link Constants} is used.
     *
     * @param rdeck_base_dir     path name to the rdeck_base
     * @param module_base_dir      path name to the modle base
     * @param projects_base_dir path name to the projects base
     * @return a Framework instance
     */
    public static Framework getInstance(final String rdeck_base_dir,
                                        final String projects_base_dir) {

        return new Framework(rdeck_base_dir, projects_base_dir);
    }

    /**
     * Get the Framework instance from an Ant project.
     * @param project
     * @return
     */
    public static Framework getInstance(Project project) {
        return getInstance(project, true);
    }

    /**
     * Get the Framework instance from an Ant project.
     * @param project
     * @return
     */
    public static Framework getInstance(Project project, boolean fail) {
        Object o = project.getReference(Framework.class.getName() + ".instance");
        if (null != o && o instanceof Framework) {
            return (Framework) o;
        }else {
            if(fail){
                throw new IllegalArgumentException("Project does not contain a reference to the Framework instance.");
            }else{
                return null;
            }
        }
    }

    /**
     * Retrieve a Framework from the project in several ways: look for embedded reference, otherwise construct with
     * 'rdeck.base' property value from the project, otherwise create new Framework from system property
     * 'rdeck.base' value.
     *
     * @param project ant project
     *
     * @return existing or new Framework instance
     */
    public static Framework getInstanceOrCreate(final Project project) {
        Framework fw = null;
        if (null != project) {
            fw = getInstance(project, false);
        }
        if (null != project && null == fw && null != project.getProperty("rdeck.base")) {
            fw = Framework.getInstance(project.getProperty("rdeck.base"),
                                       project.getProperty("projects.dir"));
            fw.configureFromProject(project);
            fw.configureProject(project);
        }
        if (null == fw) {
            fw = Framework.getInstance(Constants.getSystemBaseDir());
            fw.configureFromProject(project);
            fw.configureProject(project);
        }
        return fw;
    }
    /**
     * Returns an instance of Framework object.
     * Specify the rdeck_base path and let the projects and modules dir be constructed from it.
     *
     * @param rdeck_base_dir path name to the rdeck_base
     *
     * @return a Framework instance
     */
    public static Framework getInstance(final String rdeck_base_dir) {
        return getInstance(rdeck_base_dir, null, null);
    }

    /**
     * Returns an instance of Framework object. Specify the rdeck_base path and let the projects and modules dir be
     * constructed from it.
     *
     * @param rdeck_base_dir path name to the rdeck_base
     *
     * @return a Framework instance
     */
    public static Framework getInstance(final String rdeck_base_dir,
                                        final Authenticator authenticator,
                                        final LegacyAuthorization authorization) {

        logger.debug("creating new Framework instance."
                     + "  rdeck_base_dir=" + rdeck_base_dir
        );
        if(null==rdeck_base_dir && null==Constants.getSystemBaseDir()) {
            throw new RuntimeException(
                "Unable to determine rdeck base directory: system property rdeck.base is not set");
        }
        Framework instance = new Framework(rdeck_base_dir,
                                           Constants.getFrameworkProjectsDir(rdeck_base_dir),
                                           authenticator,
                                           authorization);
        return instance;
    }

    /**
     * Factory method to getting the singleton instance of the Framework object. Info about the
     * rdeck.base, projects.base and modules.base are retrieved via {@link Constants}.
     *
     * @return returns Framework singleton instance. Creates it using info from
     * {@link Constants} data. Assumes rdeck.home and rdeck.base System props are set
     */
    public static Framework getInstance() {
        logger.debug("creating new Framework using info from com.dtolabs.rundeck.core.Constants");
        return Framework.getInstance(null, null);
    }

    /**
     * Set properties of this Framework instance based on project properties
     *
     * @param project the ant project
     */
    private void configureFromProject(final Project project) {
        if ("true".equals(project.getProperty(FRAMEWORK_USERINPUT_DISABLED))) {
            setAllowUserInput(false);
        } else {
            setAllowUserInput(true);
        }
    }

    /**
     * Configure a project to embed this framework instance as a reference.
     *
     * @param project the ant project
     */
    public void configureProject(final Project project) {
        project.addReference(Framework.class.getName() + ".instance", this);
        if (!isAllowUserInput()) {
            final InputHandler h = project.getInputHandler();
            project.setInputHandler(new FailInputHandler(h));
            project.setProperty(FRAMEWORK_USERINPUT_DISABLED, "true");
        } else {
            final InputHandler h = project.getInputHandler();
            if (h instanceof FailInputHandler) {
                final InputHandler orig = ((FailInputHandler) h).getOriginal();
                project.setInputHandler(orig);
            }
            project.setProperty(FRAMEWORK_USERINPUT_DISABLED, "false");
        }
    }

    /**
     * Set the CentralDispatcherMgr instance
     * @param centralDispatcherMgr the instance
     */
    public void setCentralDispatcherMgr(final CentralDispatcher centralDispatcherMgr) {
        this.centralDispatcherMgr = centralDispatcherMgr;
    }


    /**
     * An InputHandler implementation which simply throws an exception.  It also stores an original implementation that
     * it may have replaced.
     */
    static class FailInputHandler implements InputHandler {
        private InputHandler orig;

        public FailInputHandler(final InputHandler h) {
            this.orig = h;
        }

        public void handleInput(final org.apache.tools.ant.input.InputRequest request) throws BuildException {
            throw new BuildException("User input is not available.");
        }

        public InputHandler getOriginal() {
            return orig;
        }
    }
    /**
     * Return the property value by name
     *
     * @param name Property key
     * @return property value
     */
    public String getProperty(final String name) {
        return lookup.getProperty(name);
    }


    /**
     * Return true if the property exists
     */
    public boolean hasProperty(final String key) {
        return lookup.hasProperty(key);
    }

    /**
     * Return true if the property is set for the project or the framework
     */
    public boolean hasProjectProperty(final String key, final String project) {
        final FrameworkProject frameworkProject = getFrameworkProjectMgr().getFrameworkProject(project);
        return frameworkProject.hasProperty(key) || hasProperty(key);
    }
    /**
     * Return the property value for the key from the project or framework properties if it exists, otherwise
     * return null.
     */
    public String getProjectProperty(final String project, final String key) {
        final FrameworkProject frameworkProject = getFrameworkProjectMgr().getFrameworkProject(project);
        if(frameworkProject.hasProperty(key)) {
            return frameworkProject.getProperty(key);
        }else if(hasProperty(key)) {
            return getProperty(key);
        }
        return null;
    }


    public IPropertyLookup getPropertyLookup() {
        return lookup;
    }

    /**
     * Returns a string with useful information for debugging.
     *
     * @return Formatted string
     */
    public String toString() {
        return "Framework{" +
                "baseDir=" + getBaseDir() +
                ", projectsBaseDir=" + projectsBase +
                "}";
    }

    /**
     * Gets the value of "framework.node.hostname" property
     *
     * @return Returns value of framework.node.hostname property
     */
    public String getFrameworkNodeHostname() {
        String hostname = getProperty("framework.node.hostname");
        if (null!=hostname) {
            return hostname.trim();
        } else {
            return hostname;
        }
    }

   /**
     * Gets the value of "framework.node.name" property
     *
     * @return Returns value of framework.node.name property
     */
    public String getFrameworkNodeName() {
        String name = getProperty("framework.node.name");
       if (null!=name) {
           return name.trim();
       } else {
           return name;
       }
    }

    /**
     * Generate a node entry for the framework with default values
     * @return
     */
    public NodeEntryImpl createFrameworkNode() {
        NodeEntryImpl node = new NodeEntryImpl(getFrameworkNodeHostname(), getFrameworkNodeName());
        node.setUsername(getProperty("framework.ssh.user"));
        node.setDescription("Rundeck server node");
        node.setOsArch(System.getProperty("os.arch"));
        node.setOsName(System.getProperty("os.name"));
        node.setOsVersion(System.getProperty("os.version"));
        //family has to be guessed at
        //TODO: determine cygwin somehow
        final String s = System.getProperty("file.separator");
        node.setOsFamily("/".equals(s) ? "unix" : "\\".equals(s) ? "windows" : "");
        return node;
    }

    /**
     * Read the nodes file for a project and return a filtered set of nodes
     *
     * @param nodeset node filter set
     * @param project project name
     *
     * @return filtered set  of nodes
     *
     * @throws NodeFileParserException
     */
    public Collection<INodeEntry> filterNodes(final NodesSelector nodeset, final String project, final File nodesFile) throws
        NodeFileParserException {
        return filterNodeSet(nodeset, project, nodesFile).getNodes();
    }
    /**
     * Read the nodes file for a project and return a filtered set of nodes
     *
     * @param nodeset node filter set
     * @param project project name
     *
     * @return filtered set  of nodes
     *
     * @throws NodeFileParserException
     */
    public INodeSet filterNodeSet(final NodesSelector nodeset, final String project, final File nodesFile) throws
        NodeFileParserException {
        INodeSet unfiltered=null;

        if (null != nodesFile) {
            try {
                unfiltered = FileResourceModelSource.parseFile(nodesFile, this, project);
            } catch (ResourceModelSourceException e) {
                throw new CoreException(e);
            } catch (ConfigurationException e) {
                throw new CoreException(e);
            }
        } else {
            unfiltered = getFrameworkProjectMgr().getFrameworkProject(project).getNodeSet();
        }
        if(0==unfiltered.getNodeNames().size()) {
            logger.warn("Empty node list");
        }
        INodeSet filtered = NodeFilter.filterNodes(nodeset, unfiltered);
        return filtered;
    }

    /**
     * Return the nodeset consisting only of the input nodes where the specified actions are all authorized
     */
    public INodeSet filterAuthorizedNodes(final String project, final Set<String> actions, final INodeSet unfiltered) {
        if (null != actions && actions.size() > 0) {
            //select only nodes with all allowed actions
            final HashSet<Map<String, String>> resources = new HashSet<Map<String, String>>();
            for (final INodeEntry iNodeEntry : unfiltered.getNodes()) {
                HashMap<String, String> resdef = new HashMap<String, String>(iNodeEntry.getAttributes());
                resdef.put("type", "node");
                resdef.put("rundeck_server", Boolean.toString(isLocalNode(iNodeEntry)));
                resources.add(resdef);
            }
            final Set<Decision> decisions = getAuthorizationMgr().evaluate(resources,
                getAuthenticationMgr().getSubject(),
                actions,
                Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"), project)));
            final NodeSetImpl authorized = new NodeSetImpl();
            HashMap<String, Set<String>> authorizations = new HashMap<String, Set<String>>();
            for (final Decision decision : decisions) {
                if (decision.isAuthorized() && actions.contains(decision.getAction())) {
                    final String nodename = decision.getResource().get("nodename");
                    if(null==authorizations.get(nodename)) {
                        authorizations.put(nodename, new HashSet<String>());
                    }
                    authorizations.get(nodename).add(decision.getAction());
                }
            }
            for (final Map.Entry<String, Set<String>> entry : authorizations.entrySet()) {
                if(entry.getValue().size()==actions.size()) {
                    authorized.putNode(unfiltered.getNode(entry.getKey()));
                }
            }
            return authorized;
        }
        return unfiltered;
    }

    public File getConfigDir() {
        return new File(Constants.getFrameworkConfigDir(getBaseDir().getAbsolutePath()));
    }
    /**
     * Return the directory containing plugins/extensions for the framework.
     */
    public File getLibextDir(){
        if(null!=System.getProperty(SYSTEM_PROP_LIBEXT)) {
            return new File(System.getProperty(SYSTEM_PROP_LIBEXT));
        }else if (hasProperty(FRAMEWORK_LIBEXT_DIR)) {
            return new File(getProperty(FRAMEWORK_LIBEXT_DIR));
        }else {
            return new File(getBaseDir(), DEFAULT_LIBEXT_DIR_NAME);
        }
    }
    /**
     * Return the cache directory used by the plugin system
     */
    public File getLibextCacheDir(){
        if (null != System.getProperty(SYSTEM_PROP_LIBEXT_CACHE)) {
            return new File(System.getProperty(SYSTEM_PROP_LIBEXT_CACHE));
        }else if (hasProperty(FRAMEWORK_LIBEXT_CACHE_DIR)) {
            return new File(getProperty(FRAMEWORK_LIBEXT_CACHE_DIR));
        }else {
            return new File(getLibextDir(), DEFAULT_LIBEXT_CACHE_DIR_NAME);
        }
    }



    public File getFrameworkProjectsBaseDir() {
        return projectsBase;
    }

    public void setAuthorizationMgr(LegacyAuthorization authorizationMgr) {
        this.authorizationMgr = authorizationMgr;
    }

    public void setAuthenticationMgr(Authenticator authenticationMgr) {
        this.authenticationMgr = authenticationMgr;
    }

    public File getHomeDir() {
        return homeDir;
    }

    public boolean isAllowUserInput() {
        return allowUserInput;
    }

    public void setAllowUserInput(boolean allowUserInput) {
        this.allowUserInput = allowUserInput;
    }

    /**
     * References the {@link INodeDesc} instance representing the framework node.
     */
    private INodeDesc nodedesc;
    /**
     * Gets the {@link INodeDesc} value describing the framework node
     * @return the singleton {@link INodeDesc} object for this framework instance
     */
    public INodeDesc getNodeDesc() {
        if (null==nodedesc) {
            nodedesc = NodeEntryImpl.create(getFrameworkNodeHostname(), getFrameworkNodeName());
        }
        return nodedesc;
    }


    /**
     * Return true if the node is the local framework node.  Compares the (logical) node names
     * of the nodes after eliding any embedded 'user@' parts.
     * @param node the node
     * @return true if the node's name is the same as the framework's node name
     */
    public boolean isLocalNode(INodeDesc node) {
        final String fwkNodeName = getFrameworkNodeName();
        return fwkNodeName.equals(node.getNodename());
    }

     /**
     * Check's if this node is the server node. Based on comparing framework.server.name and framework.node.name
     * Assumes framework.server.name property exists.
     * @return Returns true if framework.server.name and framework.node.name match
     */
    public boolean isServerNode() {
        String serverNode = null;
        if (hasProperty("framework.server.name")) {
            serverNode = getProperty("framework.server.name");
        }
        if (null!=serverNode && serverNode.equals(getPropertyLookup().getProperty("framework.node.name"))) {
            return true;
        } else {
            return false;
        }
    }
}
