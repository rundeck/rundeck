/*
 * Copyright 2012 DTO Labs, Inc. (http://dtolabs.com)
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
 *
 */

/*
* PluginLogger.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 11/26/12 3:10 PM
* 
*/
package com.dtolabs.rundeck.plugins;

/**
 * PluginLogger provides logging to execution plugins.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface PluginLogger {
    /**
     * Log a message at a given level
     *
     * @param level   the log level, from 0 to 5, where 0 is "error" and 5 is "debug"
     * @param message Message being logged. <code>null</code> messages are not logged, however, zero-length strings
     *                are.
     */
    public void log(final int level, final String message);
}
