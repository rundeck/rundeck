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
* ExecutionFollowResult.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/3/12 6:45 PM
* 
*/
package com.dtolabs.rundeck.core.dispatcher;

/**
 * ExecutionFollowResult describes the result of the last log output received
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public interface ExecutionFollowResult {

    /**
     * @return true if the log output was loaded completely
     */
    public boolean isLogComplete();
    /**
     * @return the execution state
     */
    public ExecutionState getState();
    /**
     * @return true if the {@link ExecutionFollowReceiver} halted the follow request by
     * returning false.
     */
    public boolean isReceiverFinished();

}
