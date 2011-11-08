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
* ValidationException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/27/11 5:22 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

/**
 * ValidationException indicates an error validting a property value
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ValidationException extends Exception {
    public ValidationException() {
        super();
    }

    public ValidationException(String msg) {
        super(msg);
    }

    public ValidationException(Exception cause) {
        super(cause);
    }

    public ValidationException(String msg, Exception cause) {
        super(msg, cause);
    }
}
