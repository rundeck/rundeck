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
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodesSelector;
import com.dtolabs.rundeck.core.common.OrchestratorConfig;
import com.dtolabs.rundeck.core.common.PluginControlService;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.data.MultiDataContext;
import com.dtolabs.rundeck.core.execution.workflow.SharedOutputContext;
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionListener;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.logging.LoggingManager;
import com.dtolabs.rundeck.core.nodes.ProjectNodeService;
import com.dtolabs.rundeck.core.storage.StorageTree;

import java.util.Map;

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
     */
    public Framework getFramework();

    /**
     * @return the authorization context
     */
    public AuthContext getAuthContext();
    /**
     * @return the storage service
     */
    public StorageTree getStorageTree();
    /**
     * @return the job service
     */
    public JobService getJobService();

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
}
