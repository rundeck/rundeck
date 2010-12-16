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
* FailedNodesListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 4, 2010 2:02:17 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import java.util.Collection;

/**
 * FailedNodesListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface FailedNodesListener {
    /**
     * Called with a list of node names if the execution failed.  The nodes will be the failed nodes.
     *
     * @param names node names
     */
    public void nodesFailed(Collection<String> names);

    /**
     * Called if no nodes failed during execution.
     */
    public void nodesSucceeded();

    /**
     * Called with full list of matched nodes prior to execution.
     */
    public void matchedNodes(Collection<String> names);
}
