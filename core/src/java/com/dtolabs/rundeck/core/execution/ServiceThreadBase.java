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
* ServiceThreadBase.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/29/11 12:03 PM
* 
*/
package com.dtolabs.rundeck.core.execution;

import java.util.*;

/**
 * ServiceThreadBase is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ServiceThreadBase extends Thread {
    volatile boolean success = false;
    private volatile boolean aborted = false;
    volatile Throwable thrown;
    volatile Object resultObject;

    public void abort() {
        if (isAlive()) {
            aborted = true;
            interrupt();
        }
    }

    public boolean isSuccessful() {
        return success;
    }

    public Throwable getThrowable() {
        return thrown;
    }

    public boolean isAborted() {
        return aborted;
    }

    public Object getResultObject() {
        return resultObject;
    }
}
