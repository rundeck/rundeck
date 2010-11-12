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
* ExecutionListener.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 4:41:48 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

import org.apache.tools.ant.BuildListener;

/**
 * ExecutionListener is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public interface ExecutionListener {
    /**
     * Return true if output should be terse and not prefixed
     * @return
     */
    public boolean isTerse();
    /**
     * Log a message
     *
     * @param level   the log level
     * @param message Message being logged. <code>null</code> messages are not
     *                logged, however, zero-length strings are.
     */
    public void log(final int level, final String message);

    /**
     * Return a listener for failed node list
     * @return listener
     */
    public FailedNodesListener getFailedNodesListener();

    /**
     * Return a build listener
     * @return build listener
     */
    public BuildListener getBuildListener();
}
