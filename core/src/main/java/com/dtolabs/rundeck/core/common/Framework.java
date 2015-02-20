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

import com.dtolabs.client.services.RundeckAPICentralDispatcher;
import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.CoreException;
import com.dtolabs.rundeck.core.authorization.*;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.dispatcher.CentralDispatcher;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.ExecutionServiceFactory;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
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
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;
import org.apache.log4j.Logger;

import java.io.File;
import java.net.URI;
import java.util.*;


/**
 * Manages the elements of the Ctl framework. Provides access to the various
 * kinds of framework resource managers like
 * {@link FrameworkProjectMgr}, {@link Authorization}.
 * <br>
 * User: alexh
 * Date: Jun 4, 2004
 * Time: 8:16:42 PM
 */
public class Framework implements IFramework, IFilesystemFramework{
    public static final Logger logger = Logger.getLogger(Framework.class);

    public static final String CENTRALDISPATCHER_CLS_PROP = "framework.centraldispatcher.classname";
    public static final String NODES_RESOURCES_FILE_PROP = "framework.nodes.file.name";

    /**
     * Environmental attribute for the rundeck app
     */
    public static final Attribute RUNDECK_APP_CONTEXT = new Attribute(URI.create(EnvironmentalContext.URI_BASE +
            "application"), "rundeck");
    /**
     * the rundeck app environment for authorization
     */
    public static final Set<Attribute> RUNDECK_APP_ENV = Collections.singleton(RUNDECK_APP_CONTEXT);

    static final String PROJECTMGR_NAME = "frameworkProjectMgr";
    public static final String FRAMEWORK_PLUGINS_ENABLED = "framework.plugins.enabled";

    private final FilesystemFramework filesystemFramework;
    private final IPropertyLookup lookup;
    private IFrameworkProjectMgr frameworkProjectMgr;

    final HashMap<String,FrameworkSupportService> services = new HashMap<String, FrameworkSupportService>();

    /**
     * Initialize children, the various resource management objects
     */
    public void initialize() {
        //plugin manager service inited first.  any pluggable services will then be
        //able to try to load providers via the plugin manager
        if(!hasProperty(FRAMEWORK_PLUGINS_ENABLED) || "true".equals(getProperty(FRAMEWORK_PLUGINS_ENABLED))){
            //enable plugin service only if framework property does not disable them
            PluginManagerService.getInstanceForFramework(this);
        }
        NodeStepExecutionService.getInstanceForFramework(this);
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

    /**
     * Gets DepotMgr for this framework instance
     * @return returns instance of IFrameworkProjectMgr
     */
    @Override
    public ProjectManager getFrameworkProjectMgr() {
        return frameworkProjectMgr;
    }


    /**
     * Standard constructor
     */
    Framework(
            final FilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr frameworkProjectMgr,
            final PropertyLookup lookup
    )
    {
        this.filesystemFramework=filesystemFramework;
        filesystemFramework.setFramework(this);

        setFrameworkProjectMgr(frameworkProjectMgr);

        this.lookup = lookup;

        initialize();
    }

    public static PropertyRetriever createPropertyRetriever(File basedir) {
        return FilesystemFramework.createPropertyRetriever(basedir);
    }
    public static Framework getInstance(String basedir, String projectsdir) {
        return FrameworkFactory.createForFilesystem(basedir);
    }

    public IFrameworkProjectMgr getFilesystemFrameworkProjectManager() {
        return frameworkProjectMgr;
    }

    /**
     * @return a service by name
     * @param name service name
     */
    public FrameworkSupportService getService(String name) {
        return services.get(name);
    }
    /**
     * Set a service by name
     * @param name name
     * @param service service
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
    @Override
    public ExecutionService getExecutionService() {
        return ExecutionServiceFactory.getInstanceForFramework(this);
    }
    @Override
    public WorkflowExecutionService getWorkflowExecutionService() {
        return WorkflowExecutionService.getInstanceForFramework(this);
    }
    @Override
    public StepExecutionService getStepExecutionService() {
        return StepExecutionService.getInstanceForFramework(this);
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(INodeEntry node, final String project) throws ExecutionServiceException {
        return getFileCopierService().getProviderForNodeAndProject(node, project);
    }

    @Override
    public FileCopierService getFileCopierService() {
        return FileCopierService.getInstanceForFramework(this);
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(INodeEntry node, final String project) throws ExecutionServiceException {
        return getNodeExecutorService().getProviderForNodeAndProject(node, project);
    }
    @Override
    public NodeExecutorService getNodeExecutorService() throws ExecutionServiceException {
        return NodeExecutorService.getInstanceForFramework(this);
    }
    @Override
    public NodeStepExecutionService getNodeStepExecutorService() throws ExecutionServiceException {
        return NodeStepExecutionService.getInstanceForFramework(this);
    }
    @Override
    public NodeStepExecutor getNodeStepExecutorForItem(NodeStepExecutionItem item) throws ExecutionServiceException {
        return NodeStepExecutionService.getInstanceForFramework(this).getExecutorForExecutionItem(item);
    }
    @Override
    public NodeDispatcher getNodeDispatcherForContext(ExecutionContext context) throws ExecutionServiceException {
        return NodeDispatcherService.getInstanceForFramework(this).getNodeDispatcher(context);
    }
    @Override
    public ResourceModelSourceService getResourceModelSourceService() {
        return ResourceModelSourceService.getInstanceForFramework(this);
    }
    @Override
    public ResourceFormatParserService getResourceFormatParserService() {
        return ResourceFormatParserService.getInstanceForFramework(this);
    }

    @Override
    public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return ResourceFormatGeneratorService.getInstanceForFramework(this);
    }

    @Override
    public ServiceProviderLoader getPluginManager(){
        if(null!=getService(PluginManagerService.SERVICE_NAME)) {
            return PluginManagerService.getInstanceForFramework(this);
        }
        return null;
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
     * @return a PropertyRetriever interface for framework-scoped properties
     */
    public PropertyRetriever getPropertyRetriever() {
        return PropertyLookup.safePropertyRetriever(lookup);
    }

    /**
     * @return true if the property exists
     * @param key property key
     */
    public boolean hasProperty(final String key) {
        return lookup.hasProperty(key);
    }

    /**
     * @return true if the property is set for the project or the framework
     * @param project project
     * @param key property name
     */
    public boolean hasProjectProperty(final String key, final String project) {
        final IRundeckProject frameworkProject = getFrameworkProjectMgr().getFrameworkProject(project);
        return frameworkProject.hasProperty(key) || hasProperty(key);
    }
    /**
     * @return the property value for the key from the project or framework properties if it exists, otherwise
     * return null.
     * @param project project
     * @param key property name
     */
    public String getProjectProperty(final String project, final String key) {
        final IRundeckProject frameworkProject = getFrameworkProjectMgr().getFrameworkProject(project);
        if(frameworkProject.hasProperty(key)) {
            return frameworkProject.getProperty(key);
        }else if(hasProperty(key)) {
            return getProperty(key);
        }
        return null;
    }


    @Override
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
                "}";
    }

    /**
     * Gets the value of "framework.server.hostname" property
     *
     * @return Returns value of framework.server.hostname property
     */
    @Override
    public String getFrameworkNodeHostname() {
        String hostname = getProperty("framework.server.hostname");
        if (null!=hostname) {
            return hostname.trim();
        } else {
            return hostname;
        }
    }

   /**
     * Gets the value of "framework.server.name" property
     *
     * @return Returns value of framework.server.name property
     */
    @Override
    public String getFrameworkNodeName() {
        String name = getProperty("framework.server.name");
       if (null!=name) {
           return name.trim();
       } else {
           return name;
       }
    }

    /**
     * @return Generate a node entry for the framework with default values
     */
    @Override
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
     * @param nodesFile optional file to read nodes from
     *
     * @return filtered set  of nodes
     *
     * @throws NodeFileParserException on error
     */
    @Override
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
     * @return the nodeset consisting only of the input nodes where the specified actions are all authorized
     * @param project project name
     * @param actions action set
     * @param unfiltered nodes
     * @param authContext authoriziation
     */
    @Override
    public INodeSet filterAuthorizedNodes(
            final String project, final Set<String> actions, final INodeSet unfiltered,
            AuthContext authContext
    ) {
        if (null == actions || actions.size() <= 0) {
            return unfiltered;
        }
        final HashSet<Map<String, String>> resources = new HashSet<Map<String, String>>();
        for (final INodeEntry iNodeEntry : unfiltered.getNodes()) {
            HashMap<String, String> resdef = new HashMap<String, String>(iNodeEntry.getAttributes());
            resdef.put("type", "node");
            resdef.put("rundeck_server", Boolean.toString(isLocalNode(iNodeEntry)));
            resources.add(resdef);
        }
        final Set<Decision> decisions = authContext.evaluate(resources,
                actions,
                Collections.singleton(new Attribute(URI.create(EnvironmentalContext.URI_BASE + "project"),
                        project)));
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




    /**
     * References the {@link INodeDesc} instance representing the framework node.
     */
    private INodeDesc nodedesc;
    /**
     * Gets the {@link INodeDesc} value describing the framework node
     * @return the singleton {@link INodeDesc} object for this framework instance
     */
    @Override
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
    @Override
    public boolean isLocalNode(INodeDesc node) {
        final String fwkNodeName = getFrameworkNodeName();
        return fwkNodeName.equals(node.getNodename());
    }

    public void setFrameworkProjectMgr(final IFrameworkProjectMgr frameworkProjectMgr) {
        this.frameworkProjectMgr = frameworkProjectMgr;
    }

    public FilesystemFramework getFilesystemFramework() {
        return filesystemFramework;
    }

    @Override
    public File getConfigDir() {
        return getFilesystemFramework().getConfigDir();
    }

    @Override
    public File getFrameworkProjectsBaseDir() {
        return getFilesystemFramework().getFrameworkProjectsBaseDir();
    }

    @Override
    public File getLibextDir() {
        return getFilesystemFramework().getLibextDir(this);
    }

    @Override
    public File getLibextCacheDir() {
        return getFilesystemFramework().getLibextCacheDir(this);
    }

    @Override
    public File getBaseDir() {
        return getFilesystemFramework().getBaseDir();
    }
}
