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

/*
* NodesetFailureException.java
* 
* User: greg
* Created: Aug 10, 2009 6:01:05 PM
* $Id$
*/
package com.dtolabs.rundeck.core;

import java.util.Collection;
import java.util.Map;

/**
 * NodesetFailureException indicates some nodes failed during a multi-node dispatch.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class NodesetFailureException extends CoreException {
    /**
     * Exit code for a Nodeset failure exception
     */
    public static final int EXIT_CODE =9;

    /**
     * Construct a NodesetFailureException
     * @param nodeset Collection of node names for failed nodes
     * @param retryMsg commandline for retrying the command for the failed nodes.
     */
    private Collection<String> nodeset;
    private Map<String,Object> nodeFailures;

    /**
     * Create NodesetFailureException
     * @param nodeset node names
     */
    public NodesetFailureException(final Collection<String> nodeset) {
        super("Execution failed on the following " + (null != nodeset ? nodeset.size() : 0) + " nodes: " + nodeset);
        this.nodeset = nodeset;
    }
    /**
     * Create NodesetFailureException
     * @param nodeset node names
     */
    public NodesetFailureException(final Map<String,Object> failures) {
        super("Execution failed on the following " + (null != failures ? failures.size() : 0) + " nodes: " + failures);
        this.nodeFailures = failures;
    }

    /**
     * Get the collection of node names
     * @return collection of node names
     */
    public Collection<String> getNodeset() {
        return nodeset;
    }

    /**
     * Set the nodeset
     * @param nodeset node names collection
     */
    public void setNodeset(final Collection<String> nodeset) {
        this.nodeset = nodeset;
    }

    public Map<String, Object> getNodeFailures() {
        return nodeFailures;
    }

    public void setNodeFailures(Map<String, Object> nodeFailures) {
        this.nodeFailures = nodeFailures;
    }
}
