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
* IDispatchedExecution.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 30, 2010 3:46:26 PM
* $Id$
*/
package com.dtolabs.rundeck.core.dispatcher;

import com.dtolabs.rundeck.core.utils.NodeSet;

import java.util.Map;

/**
 * IDispatchedExecution defines common properties of dispatched execution requests (script, command or job)
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface IDispatchedExecution {
    /**
     * Return the node exclude precedence
     * @return nodeset
     */
    Boolean getNodeExcludePrecedence();

    /**
     * @return the node dispatch threadcount
     */
    int getNodeThreadcount();

    /**
     * @return true or false for node dispatch keepgoing option, or null if unspecified.
     *
     */
    public Boolean isKeepgoing();

    /**
     *
     * @return node filter string
     */
    String getNodeFilter();

    /**
     * Get the argument line definition
     * @return the arg
     */
    String[] getArgs();

    /**
     * Return the loglevel value, using the Ant equivalents: DEBUG=1,
     * @return log level from 0-4: ERR,WARN,INFO,VERBOSE,DEBUG
     */
    int getLoglevel();

    /**
     * Return data context set
     *
     * @return map of data contexts keyed by name
     */
    public Map<String, Map<String, String>> getDataContext();
}
