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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A project node source
 */
public interface IProjectNodes {
    /**
     * Returns the set of nodes for the project
     *
     * @return an instance of {@link com.dtolabs.rundeck.core.common.INodeSet}
     */
    INodeSet getNodeSet();

    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    ArrayList<Exception> getResourceModelSourceExceptions();

    /**
     * @return the set of exceptions produced by source name
     */
    Map<String,Exception> getResourceModelSourceExceptionsMap();

    /**
     * list the configurations of resource model providers.
     *
     * @return a list of maps containing:
     * <ul>
     * <li>type - provider type name</li>
     * <li>props - configuration properties</li>
     * </ul>
     */
    List<Map<String, Object>> listResourceModelConfigurations();

    /**
     * Conditionally update the nodes resources file if a URL source is defined for it and return
     * true if the update process was invoked and succeeded
     *
     * @param nodesResourcesFilePath destination file path
     *
     * @return true if the update succeeded, false if it was not performed
     *
     * @throws UpdateUtils.UpdateException if an error occurs while trying to update the resources file
     */
    public boolean updateNodesResourceFile(final String nodesResourcesFilePath) throws UpdateUtils.UpdateException;

    /**
     * Update the nodes resources file from a specific URL, with BASIC authentication as provided or
     * as defined in the URL's userInfo section.
     *
     * @param providerURL           URL to retrieve resources file definition
     * @param username              username or null
     * @param password              or null
     * @param nodesResourceFilePath path of the destination file
     *
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs during the update process
     */
    void updateNodesResourceFileFromUrl(
            String providerURL, String username,
            String password,
            String nodesResourceFilePath
    ) throws UpdateUtils.UpdateException;

    /**
     * Update the resources file given an input Nodes set
     *
     * @param nodeset                nodes
     * @param nodesResourcesFilePath destination file path
     *
     * @throws com.dtolabs.rundeck.core.common.UpdateUtils.UpdateException if an error occurs while trying to update the
     *                                                                     resources file or generate
     *
     *                                                                     nodes
     */
    void updateNodesResourceFile(INodeSet nodeset, final String nodesResourcesFilePath)
            throws UpdateUtils.UpdateException;
}
