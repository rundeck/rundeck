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
* ContextLogger.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/30/11 6:11 PM
* 
*/
package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.execution.BaseLogger;
import com.dtolabs.rundeck.core.logging.LogLevel;

import java.util.Map;

/**
 * ContextLogger extends the BaseLogger to allow logging with extended context information
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ContextLogger extends BaseLogger {

    /**
     * Logs message via implementation specific log facility
     *
     * @param message message to log
     * @param context context
     */
    void log(String message, Map<String, String> context);

    /**
     * Logs error message via implementation specific log facility
     *
     * @param message message to log
     * @param context context
     */
    void error(String message, Map<String, String> context);

    /**
     * Logs warning message via implementation specific log facility
     *
     * @param message message to log
     * @param context context
     */
    void warn(String message, Map<String, String> context);

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     * @param context context
     */
    void verbose(String message, Map<String, String> context);

    /**
     * Logs verbose message via implementation specific log facility
     *
     * @param message message to log
     * @param context context
     */
    void debug(String message, Map<String, String> context);

    /**
     * Emit arbitrary event type
     * @param eventType type
     * @param level level
     * @param message message
     * @param context context
     */
    void emit(String eventType, LogLevel level, String message, Map<String, String> context);
}
