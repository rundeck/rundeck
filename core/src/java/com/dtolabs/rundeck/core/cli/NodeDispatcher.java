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
* NodeDispatcher.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jun 3, 2010 1:31:26 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.FailedNodesListener;
import org.apache.tools.ant.Project;

import java.util.Collection;

/**
 * NodeDispatcher interface for a way to execute a dispatched command
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface NodeDispatcher {
    /**
     * Execute a node dispatch request, in serial with parallel threads.
     *
     * @param project        Ant project
     * @param nodes          node set to iterate over
     * @param threadcount    max number of parallel threads
     * @param keepgoing      if true, continue execution even if a node fails
     * @param failedListener listener for results of failed nodes (when keepgoing is true)
     * @param factory        factory to produce executable items given input nodes
     */
    public void executeNodedispatch(final Project project, final Collection<INodeEntry> nodes,
                                    final int threadcount, final boolean keepgoing,
                                    final FailedNodesListener failedListener,
                                    final NodeCallableFactory factory);
}
