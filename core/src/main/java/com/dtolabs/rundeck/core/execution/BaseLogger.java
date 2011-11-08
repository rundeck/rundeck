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
* BaseLogger.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/30/11 7:03 PM
* 
*/
package com.dtolabs.rundeck.core.execution;

/**
 * BaseLogger defines a base set of logging methods.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface BaseLogger {
    /**
     * Logs message via implementation specific log facility
     *
     * @param message message to log
     */
    void log(String message);

    /**
     * Logs error message via implementation specific log facility
     *
     * @param message message to log
     */
    void error(String message);

    /**
     * Logs warning message via implementation specific log facility
     *
     * @param message message to log
     */
    void warn(String message);

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     */
    void verbose(String message);

    /**
     * Logs debug message via implementation specific log facility
     *
     * @param message message to log
     */
    void debug(String message);
}
