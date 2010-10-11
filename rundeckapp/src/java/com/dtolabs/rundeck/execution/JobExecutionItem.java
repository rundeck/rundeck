/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
* JobExecutionItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 22, 2010 4:28:34 PM
* $Id$
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.utils.NodeSet;

import java.util.Map;

/**
 * This interface represents an execution of a particular stored Job definition, identified by a string, and including
 * node filters (NodeSet), CLI arguments (args), loglevel.  The Executor for this interface is the grails
 * ExecutionService, which will look up the stored job by the identifier string, and then create and submit the
 * appropriate ExecutionItem for that job to the Execution Service.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface JobExecutionItem extends ExecutionItem {
    public String getJobIdentifier();

    /**
     * Return the nodeset configuration
     *
     * @return nodeset
     */
    public NodeSet getNodeSet();


    /**
     * Get the argument line definition
     *
     * @return the arg
     */
    public String[] getArgs();

    /**
     * Return the loglevel value, using the Ant equivalents: DEBUG=1,
     *
     * @return log level from 0-4: ERR,WARN,INFO,VERBOSE,DEBUG
     */
    public int getLoglevel();

    /**
     * Return username of executing user
     * @return user name
     */
    public String getUser();

    /**
     * Return the data context for the Job, should include "option" values at least
     * @return
     */
    public Map<String, Map<String, String>> getDataContext();
}