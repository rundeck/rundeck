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
* WorkflowExecutionItem.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 16, 2010 9:43:26 AM
* $Id$
*/
package com.dtolabs.rundeck.execution;

import com.dtolabs.rundeck.core.execution.ExecutionItem;
import com.dtolabs.rundeck.core.utils.NodeSet;

import java.util.Map;

/**
 * WorkflowExecutionItem is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface WorkflowExecutionItem extends ExecutionItem {
    /**
     * Return the workflow definition
     * @return workflow
     */
    public IWorkflow getWorkflow();

    /**
     * Return the project name
     * @return project name
     */
    public String getProject();
    /**
     * Return the nodeset
     * @return nodeset
     */
    public NodeSet getNodeSet();

    /**
     * Return user executing the item
     * @return username
     */
    public String getUser();

    /**
     * Return the input options
     * @return the option
     */
    public Map<String,Map<String,String>> getDataContext();

    /**
     * Return log level
     * @return log level
     */
    public int getLoglevel();
    public boolean isKeepgoing();
}
