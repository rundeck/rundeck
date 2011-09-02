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
* ResourceModelSourceServiceException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/19/11 12:05 PM
* 
*/
package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;

/**
 * ResourceModelSourceServiceException indicates an error from the ResourceModelSourceService.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ResourceModelSourceServiceException extends ExecutionServiceException {
    public ResourceModelSourceServiceException() {
        super(ResourceModelSourceService.SERVICE_NAME);
    }

    public ResourceModelSourceServiceException(String msg) {
        super(msg, ResourceModelSourceService.SERVICE_NAME);
    }

    public ResourceModelSourceServiceException(Throwable cause) {
        super(cause, ResourceModelSourceService.SERVICE_NAME);
    }

    public ResourceModelSourceServiceException(String msg, Throwable cause) {
        super(msg, cause, ResourceModelSourceService.SERVICE_NAME);

    }
}
