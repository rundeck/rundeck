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

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.Authorization;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.ExecutionService;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcher;
import com.dtolabs.rundeck.core.execution.dispatch.NodeDispatcherService;
import com.dtolabs.rundeck.core.execution.orchestrator.OrchestratorService;
import com.dtolabs.rundeck.core.execution.service.*;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowStrategyService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionService;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutor;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatGeneratorService;
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


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
    public static final Logger logger = LoggerFactory.getLogger(FrameworkBase.class);

    public static final String NODES_RESOURCES_FILE_PROP = "framework.nodes.file.name";
    public static final String FRAMEWORK_GLOBALS_PROP = "framework.globals.";
    public static final String PROJECT_GLOBALS_PROP = "project.globals.";


    private final IPropertyLookup lookup;
    private ProjectManager projectManager;
    private IFrameworkServices frameworkServices;
    private IFrameworkNodes frameworkNodes;


    /**
     * Standard constructor
     */
    public FrameworkBase(
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

    @Override
    public void initialize(final Framework framework) {
        frameworkServices.initialize(framework);
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
    public void overrideService(final String name, final FrameworkSupportService service) {
        frameworkServices.overrideService(name,service);
    }

    @Override
    public void setService(final String name, final FrameworkSupportService service) {
        frameworkServices.setService(name, service);
    }

    @Override
    public StepExecutor getStepExecutorForItem(final StepExecutionItem item, final String project)
            throws ExecutionServiceException
    {
        return frameworkServices.getStepExecutorForItem(item, project);
    }

    @Override
    public NodeDispatcherService getNodeDispatcherService() {
        return frameworkServices.getNodeDispatcherService();
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
    public WorkflowStrategyService getWorkflowStrategyService() {
        return frameworkServices.getWorkflowStrategyService();
    }

    @Override
    public StepExecutionService getStepExecutionService() {
        return frameworkServices.getStepExecutionService();
    }

    @Override
    public FileCopier getFileCopierForNodeAndProject(final INodeEntry node, ExecutionContext context)
            throws ExecutionServiceException
    {
        return frameworkServices.getFileCopierForNodeAndProject(node, context);
    }

    @Override
    public FileCopierService getFileCopierService() {
        return frameworkServices.getFileCopierService();
    }

    @Override
    public NodeExecutor getNodeExecutorForNodeAndProject(final INodeEntry node, ExecutionContext context)
            throws ExecutionServiceException
    {
        return frameworkServices.getNodeExecutorForNodeAndProject(node, context);
    }

    @Override
    public NodeExecutorService getNodeExecutorService()  {
        return frameworkServices.getNodeExecutorService();
    }

    @Override
    public NodeStepExecutionService getNodeStepExecutorService()  {
        return frameworkServices.getNodeStepExecutorService();
    }

    @Override
    public NodeStepExecutor getNodeStepExecutorForItem(final NodeStepExecutionItem item, ExecutionContext context, INodeEntry node)
            throws ExecutionServiceException
    {
        return frameworkServices.getNodeStepExecutorForItem(item, context,node);
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

    /**
     *
     * @param basedir
     * @param projectsdir
     * @return
     * @Deprecated used in tests
     */
    @Deprecated
    public static Framework getInstance(String basedir, String projectsdir) {
        ServiceSupport serviceSupport = new ServiceSupport();
        BaseFrameworkExecutionServices executionServices = new BaseFrameworkExecutionServices();
        serviceSupport.setExecutionServices(executionServices);
        Framework framework = FrameworkFactory.createForFilesystem(basedir, serviceSupport);
        executionServices.setNodeExecutorService(new NodeExecutorService(framework));
        executionServices.setFramework(framework);
        return framework;
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


    /**
    * Retrieves the global properties defined for the specified project.
    * This variables are defined either in the framework (framework.globals.*) or in the
    * project (project.globals.*). The prefix (xxx.globals.) will be stripped from the
    * property name.
    * <p></p>
    * For variables defines both in framework and project contexts, the variable defined in the project
    * will have priority.
    * @param project The project identifier.
    * @return Map with global variables.
    */
    public Map<String, String> getProjectGlobals(final String project) {

        // Final property map.
        Map<String, String> projectGlobalsMap = new HashMap<>();

        // Transitional map for project global variables.
        Map<String, String> projectGlobs = new HashMap<>();

        // Project full properties (framework + project).
        Map<String, String> projectFullProps = getFrameworkProjectMgr().getFrameworkProject(project).getProperties();

        // Search properties for globals entries.
        for(Map.Entry<String, String> propEntry: projectFullProps.entrySet()) {

            Map<String, String> curMap;
            String varName;

            if(propEntry.getKey().startsWith(FRAMEWORK_GLOBALS_PROP)) {
                // Search for framework globals and extract var name.
                curMap = projectGlobalsMap;
                varName = propEntry.getKey().substring(FRAMEWORK_GLOBALS_PROP.length());
            }
            else if(propEntry.getKey().startsWith(PROJECT_GLOBALS_PROP)) {
                // Search for project globals and extract var name.
                curMap = projectGlobs;
                varName = propEntry.getKey().substring(PROJECT_GLOBALS_PROP.length());
            }
            else
                continue;
            if("".equals(varName)){
                continue;
            }
            // Copy value into new map.
            curMap.put(varName, propEntry.getValue());
        }

        // Merge and replace props.
        projectGlobalsMap.putAll(projectGlobs);
        return projectGlobalsMap;
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
