/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
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
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.common.OrchestratorConfig;
import com.dtolabs.rundeck.core.storage.StorageTree;
import com.dtolabs.rundeck.plugins.orchestrator.OrchestratorPlugin;

import java.io.File;
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
     * Return data context set
     *
     * @return map of data contexts keyed by name
     */
    public Map<String, Map<String, String>> getDataContext();

    /**
     * @return the data context in the private scope
     */
    public Map<String, Map<String, String>> getPrivateDataContext();

    public ExecutionListener getExecutionListener();

    /**
     * @return Specific file to use for nodes source instead of project nodes
     */
    public File getNodesFile();

	public OrchestratorConfig getOrchestrator();
}
