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
* CtlProjectInvalidArgumentsException.java
* 
* User: greg
* Created: Oct 17, 2007 3:10:32 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli.project;


/**
 * DepotSetupInvalidArgumentsException specifies that an argument to the CLI tool was invalid.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class InvalidArgumentsException extends ProjectToolException {
    public InvalidArgumentsException(String message) {
        super(message);
    }

    public InvalidArgumentsException(String message, Throwable t) {
        super(message, t);
    }

    public InvalidArgumentsException(Throwable t) {
        super(t);
    }
}
