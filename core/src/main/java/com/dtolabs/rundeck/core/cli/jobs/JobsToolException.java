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
* CtlJobsException.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 24, 2010 6:20:47 PM
* $Id$
*/
package com.dtolabs.rundeck.core.cli.jobs;

import com.dtolabs.rundeck.core.cli.CLIToolException;

/**
 * CtlJobsException is an exception thrown by the JobsTool
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class JobsToolException extends CLIToolException {
    public JobsToolException() {
        super();
    }

    public JobsToolException(String msg) {
        super(msg);
    }

    public JobsToolException(Exception cause) {
        super(cause);
    }

    public JobsToolException(String msg, Exception cause) {
        super(msg, cause);
    }
}
