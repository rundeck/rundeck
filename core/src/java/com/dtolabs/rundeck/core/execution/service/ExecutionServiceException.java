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
* ExecutionServiceException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/21/11 6:03 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

/**
 * ExecutionServiceException is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ExecutionServiceException extends Exception {
    protected String serviceName;

    public ExecutionServiceException(String serviceName) {
        super();
        this.serviceName = serviceName;
    }

    public ExecutionServiceException(String msg, String serviceName) {
        super(msg + " for Service: " + serviceName);
        this.serviceName = serviceName;
    }

    public ExecutionServiceException(Throwable cause, String serviceName) {
        super(cause);
        this.serviceName = serviceName;
    }

    public ExecutionServiceException(String msg, Throwable cause, String serviceName) {
        super(msg + " for Service: " + serviceName, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
