/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
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
* ExecutionFollowReceiver.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/3/12 5:14 PM
* 
*/
package com.dtolabs.rundeck.core.dispatcher;

/**
 * ExecutionFollowReceiver receives execution log entries and status.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ExecutionFollowReceiver {
    /**
     * Receive updated log output status
     * @return true to continue receiving output.
     *
     * @param offset     offset location
     * @param totalSize  total log file size
     * @param duration   millisecond duration of the execution
     */
    public boolean receiveFollowStatus(long offset, long totalSize, long duration);

    /**
     * Receive a log entry
     * @return true to continue receiving output.
     *
     * @param timeStr  time string
     * @param loglevel log level string
     * @param user     username
     * @param command  command context
     * @param nodeName node name
     * @param message  log message
     */
    public boolean receiveLogEntry(final String timeStr, final String loglevel, final String user, final String command,
                                   final String nodeName, final String message);
}
