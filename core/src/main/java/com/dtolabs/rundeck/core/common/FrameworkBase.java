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

import com.dtolabs.rundeck.core.authorization.*;
import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
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
public class FrameworkBase implements IFramework{
    public static final Logger logger = Logger.getLogger(FrameworkBase.class);

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


    private final IPropertyLookup lookup;
    private ProjectManager projectManager;
    private IFrameworkServices frameworkServices;
    private IFrameworkNodes frameworkNodes;


    /**
     * Standard constructor
     */
    FrameworkBase(
            final ProjectManager projectManager,
            final IPropertyLookup lookup,
            final IFrameworkServices services,
            final IFrameworkNodes iFrameworkNodes
    )
    {
        setProjectManager(projectManager);

        this.lookup = lookup;
        this.frameworkServices=services;
        this.frameworkNodes= iFrameworkNodes;

    }

    /**
     * Gets DepotMgr for this framework instance
     * @return returns instance of IFrameworkProjectMgr
     */
    @Override
    public ProjectManager getFrameworkProjectMgr() {
        return getProjectManager();
    }


    @Override
    public FrameworkSupportService getService(final String name) {
        return frameworkServices.getService(name);
    }

    @Override
    public void setService(final String name, final FrameworkSupportService service) {
        frameworkServices.setService(name, service);
    }

    @Override
    public ExecutionService getExecutionService() {
        return frameworkServices.getExecutionService();
    }

    @Override
    public OrchestratorService getOrchestratorService() {
        return frameworkServices.getOrchestratorService();
    }

    @Override
    public WorkflowExecutionService getWorkflowExecutionService() {
        return frameworkServices.getWorkflowExecutionService();
    }

    @Override
    public StepExecutionService getStepExecutionService() {
        return frameworkServices.getStepExecutionService();
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(final INodeEntry node, final String project)
            throws ExecutionServiceException
    {
        return frameworkServices.getFileCopierForNodeAndProject(node,project);
    }

    @Override
    public FileCopierService getFileCopierService() {
        return frameworkServices.getFileCopierService();
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(final INodeEntry node, final String project)
            throws ExecutionServiceException
    {
        return frameworkServices.getNodeExecutorForNodeAndProject(node, project);
    }

    @Override
    public NodeExecutorService getNodeExecutorService() throws ExecutionServiceException {
        return frameworkServices.getNodeExecutorService();
    }

    @Override
    public NodeStepExecutionService getNodeStepExecutorService() throws ExecutionServiceException {
        return frameworkServices.getNodeStepExecutorService();
    }

    @Override
    public NodeStepExecutor getNodeStepExecutorForItem(final NodeStepExecutionItem item)
            throws ExecutionServiceException
    {
        return frameworkServices.getNodeStepExecutorForItem(item);
    }

    @Override
    public NodeDispatcher getNodeDispatcherForContext(final ExecutionContext context) throws ExecutionServiceException {
        return frameworkServices.getNodeDispatcherForContext(context);
    }

    @Override
    public ResourceModelSourceService getResourceModelSourceService() {
        return frameworkServices.getResourceModelSourceService();
    }

    @Override
    public ResourceFormatParserService getResourceFormatParserService() {
        return frameworkServices.getResourceFormatParserService();
    }

    @Override
    public ResourceFormatGeneratorService getResourceFormatGeneratorService() {
        return frameworkServices.getResourceFormatGeneratorService();
    }

    @Override
    public ServiceProviderLoader getPluginManager() {
        return frameworkServices.getPluginManager();
    }

    public static PropertyRetriever createPropertyRetriever(File basedir) {
        return FilesystemFramework.createPropertyRetriever(basedir);
    }
    public static Framework getInstance(String basedir, String projectsdir) {
        return FrameworkFactory.createForFilesystem(basedir);
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
        return "BaseFramework{" +
                "}";
    }

    @Override
    public String getFrameworkNodeHostname() {
        return getFrameworkNodes().getFrameworkNodeHostname();
    }

    @Override
    public String getFrameworkNodeName() {
        return getFrameworkNodes().getFrameworkNodeName();
    }

    @Override
    public NodeEntryImpl createFrameworkNode() {
        return getFrameworkNodes().createFrameworkNode();
    }

    @Override
    public INodeSet filterAuthorizedNodes(
            final String project, final Set<String> actions, final INodeSet unfiltered, final AuthContext authContext
    )
    {
        return getFrameworkNodes().filterAuthorizedNodes(project, actions, unfiltered, authContext);
    }

    @Override
    public INodeDesc getNodeDesc() {
        return getFrameworkNodes().getNodeDesc();
    }

    @Override
    public boolean isLocalNode(final INodeDesc node) {
        return getFrameworkNodes().isLocalNode(node);
    }

    public IFrameworkServices getFrameworkServices() {
        return frameworkServices;
    }

    public void setFrameworkServices(final IFrameworkServices frameworkServices) {
        this.frameworkServices = frameworkServices;
    }

    public IFrameworkNodes getFrameworkNodes() {
        return frameworkNodes;
    }

    public void setFrameworkNodes(final IFrameworkNodes frameworkNodes) {
        this.frameworkNodes = frameworkNodes;
    }

    public ProjectManager getProjectManager() {
        return projectManager;
    }

    public void setProjectManager(final ProjectManager projectManager) {
        this.projectManager = projectManager;
    }
}
