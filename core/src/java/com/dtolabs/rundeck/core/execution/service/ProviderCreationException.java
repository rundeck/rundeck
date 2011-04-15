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
* ProviderCreationError.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 3/22/11 3:15 PM
* 
*/
package com.dtolabs.rundeck.core.execution.service;

/**
 * ProviderCreationError is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ProviderCreationException extends ProviderLoaderException {
    public ProviderCreationException(String serviceName, String providerName) {
        super(serviceName, providerName);
    }

    public ProviderCreationException(String msg, String serviceName, String providerName) {
        super(msg, serviceName, providerName);
    }

    public ProviderCreationException(Exception cause, String serviceName, String providerName) {
        super(cause, serviceName, providerName);
    }

    public ProviderCreationException(String msg, Exception cause, String serviceName, String providerName) {
        super(msg, cause, serviceName, providerName);
    }
}
