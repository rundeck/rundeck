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

/*
* ExecutionContext.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 4:32 PM
*
*/
package com.dtolabs.rundeck.core.execution;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import com.dtolabs.rundeck.core.common.*;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.execution.component.ContextComponent;
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.logging.LoggingManager;
import com.dtolabs.rundeck.core.nodes.ProjectNodeService;
import com.dtolabs.rundeck.core.storage.StorageTree;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * ExecutionContext is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ExecutionContext {

    /**
     * Get the framework project name
     *
     * @return project name
     */
    public String getFrameworkProject();

    /**
     * @return the framework
     * @deprecated use {@link #getIFramework()}
     */
    public Framework getFramework();

    /**
     *
     * @return the framework
     */
    public IFramework getIFramework();

    /**
     * @return the authorization context
     * @deprecated use {@link #getUserAndRolesAuthContext()}
     */
    public AuthContext getAuthContext();

    public UserAndRolesAuthContext getUserAndRolesAuthContext();
    /**
     * @return the storage service
     */
    public StorageTree getStorageTree();
    /**
     * @return the job service
     */
    public JobService getJobService();

    /**
     * @return context components
     */
    public List<ContextComponent<?>> getComponentList();

    /**
     * @return the node service
     */
    public ProjectNodeService getNodeService();

    /**
     * @return username
     */
    public String getUser();

    /**
     * Return the node selector
     *
     * @return nodeset
     */
    NodesSelector getNodeSelector();
    /**
     * Return the node selector
     *
     * @return nodeset
     */
    INodeSet getNodes();
    /**
     * @return node dispatch threadcount
     */
    int getThreadCount();

    /**
     * @return the node rank attribute to use for ranking
     */
    public String getNodeRankAttribute();

    /**
     * @return true if the node rank order is ascending
     */
    public boolean isNodeRankOrderAscending();

    /**
     * @return node dispatch keepgoing
     */
    boolean isKeepgoing();

    /**
     * Return the loglevel value, using the Ant equivalents: DEBUG=1,
     *
     * @return log level from 0-4: ERR,WARN,INFO,VERBOSE,DEBUG
     */
    int getLoglevel();

    /**
     *
     * @return the charset encoding to use for handling output, or null for default
     */
    String getCharsetEncoding();

    /**
     * Return data context set
     *
     * @return map of data contexts keyed by name
     */
    public Map<String, Map<String, String>> getDataContext();
    public DataContext getDataContextObject();

    /**
     * @param type
     * @param <T>
     * @return collection of components of specified type
     */
    <T> Collection<T> componentsForType(Class<T> type);

    /**
     * apply the consumer to components of the given type, and remove "useOnce" components after use
     *
     * @param type
     * @param consumer
     * @param <T>
     */
    <T> int useAllComponentsOfType(Class<T> type, Consumer<T> consumer);

    /**
     * @param <T>
     * @param type
     * @return a single component for the given type
     */
    <T> Optional<T> componentForType(Class<T> type);

    /**
     * apply the consumer to a single component of the given type, and remove the component if it is "useOnce"
     *
     * @param <T>
     * @param type
     * @return a single component for the given type
     */
    <T> boolean useSingleComponentOfType(Class<T> type, Consumer<Optional<T>> consumer);

    /**
     * apply the consumer to a single component of the given type, and remove the component if it is "useOnce"
     *
     * @param type
     * @param <T>
     * @return optional component object
     */
    <T> Optional<T> useSingleComponentOfType(Class<T> type);

    /**
     * @return the node specific context data keyed by node name
     */
    public MultiDataContext<ContextView, DataContext> getSharedDataContext();

    /**
     * @return the data context in the private scope
     */
    public Map<String, Map<String, String>> getPrivateDataContext();
    public DataContext getPrivateDataContextObject();

    public ExecutionListener getExecutionListener();

    public WorkflowExecutionListener getWorkflowExecutionListener();

    public ExecutionLogger getExecutionLogger();

	public OrchestratorConfig getOrchestrator();
    /**
     * @return context for emitting new data
     */
    public SharedOutputContext getOutputContext();

    /**
     * @return manager for capturing logs
     */
    public LoggingManager getLoggingManager();

    public PluginControlService getPluginControlService();
    
    /**
     * Gets a reference to the execution being processed.
     * @return An {@link ExecutionReference} to the execution, or null if doesn't apply.
     */
    public ExecutionReference getExecution();
}
