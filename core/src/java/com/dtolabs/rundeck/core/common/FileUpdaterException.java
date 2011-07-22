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
* FileUpdaterException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/21/11 5:10 PM
* 
*/
package com.dtolabs.rundeck.core.common;

/**
 * FileUpdaterException  is thrown by FileUpdater if an error occurs.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileUpdaterException extends Exception {
    public FileUpdaterException() {
        super();
    }

    public FileUpdaterException(String msg) {
        super(msg);
    }

    public FileUpdaterException(Exception cause) {
        super(cause);
    }

    public FileUpdaterException(String msg, Exception cause) {
        super(msg, cause);
    }
}
