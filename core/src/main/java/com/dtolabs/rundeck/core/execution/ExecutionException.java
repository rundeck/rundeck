/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* ExecutionException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 3, 2010 11:34:10 AM
* $Id$
*/
package com.dtolabs.rundeck.core.execution;

/**
 * ExecutionException thrown when an error occurs during execution. 
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class ExecutionException extends Exception {
    public ExecutionException() {
        super();
    }

    public ExecutionException(String msg) {
        super(msg);
    }

    public ExecutionException(Exception cause) {
        super(cause);
    }

    public ExecutionException(String msg, Exception cause) {
        super(msg, cause);
    }
}
